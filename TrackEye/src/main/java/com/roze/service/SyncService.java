package com.roze.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roze.model.ActivitySession;
import com.roze.model.AfkSession;
import com.roze.model.BrowserActivity;
import com.roze.model.ScreenshotRecord;
import com.roze.repository.SessionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The piece that was missing before: this reads locally-collected rows from the
 * SQLite DB and pushes them to the central server, then marks them as synced.
 *
 * Auth: it sends the X-API-Key and X-Device-ID headers that the central
 * server's ApiKeyAuthenticationFilter expects. Those values come from
 * ~/.trackeye/config.properties, which is written by InstallationService when
 * the device is registered (see SetupController).
 *
 * If the device is not registered yet, sync is skipped quietly.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncService {

    private final SessionRepository repository;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private static final Path CONFIG_FILE =
            Paths.get(System.getProperty("user.home"), ".trackeye", "config.properties");

    // Pushed in small batches so a slow network never blocks tracking.
    private static final int BATCH = 200;
    // How often we try to sync (seconds).
    private static final int SYNC_INTERVAL_SECONDS = 60;

    private volatile String serverUrl;
    private volatile String apiKey;
    private volatile String deviceIdentifier;

    @PostConstruct
    public void start() {
        reloadConfig();
        scheduler.scheduleAtFixedRate(this::syncSafely, 20, SYNC_INTERVAL_SECONDS, TimeUnit.SECONDS);
        log.info("Sync service started (every {}s). Registered: {}", SYNC_INTERVAL_SECONDS, isRegistered());
    }

    /** Re-read the config file. Call this right after a successful registration. */
    public void reloadConfig() {
        try {
            if (Files.exists(CONFIG_FILE)) {
                Properties p = new Properties();
                try (var in = Files.newInputStream(CONFIG_FILE)) {
                    p.load(in);
                }
                this.serverUrl = trimSlash(p.getProperty("trackeye.server.url"));
                this.apiKey = p.getProperty("trackeye.client.api-key");
                this.deviceIdentifier = p.getProperty("trackeye.client.device-id");
            }
        } catch (Exception e) {
            log.warn("Could not read client config: {}", e.getMessage());
        }
    }

    public boolean isRegistered() {
        return serverUrl != null && !serverUrl.isEmpty()
                && apiKey != null && !apiKey.isEmpty()
                && deviceIdentifier != null && !deviceIdentifier.isEmpty();
    }

    private void syncSafely() {
        try {
            if (!isRegistered()) {
                log.debug("Device not registered yet - skipping sync");
                return;
            }
            syncActivities();
            syncAfk();
            syncBrowser();
            syncScreenshots();
        } catch (Exception e) {
            log.error("Sync cycle failed: {}", e.getMessage());
        }
    }

    private void syncActivities() throws Exception {
        List<ActivitySession> rows = repository.getUnsyncedActivities(BATCH);
        if (rows.isEmpty()) return;

        List<Object> payload = new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        for (ActivitySession a : rows) {
            payload.add(new java.util.LinkedHashMap<>() {{
                put("appName", a.getAppName() == null ? "Unknown" : a.getAppName());
                put("windowTitle", a.getWindowTitle());
                put("processName", a.getProcessName());
                put("startTime", a.getStartTime());
                put("endTime", a.getEndTime());
                put("durationMs", a.getDurationMs());
            }});
            ids.add(a.getId());
        }
        if (postJson("/api/sync/activities", payload)) {
            repository.markActivitiesSynced(ids);
            log.info("Synced {} activities", ids.size());
        }
    }

    private void syncAfk() throws Exception {
        List<AfkSession> rows = repository.getUnsyncedAfk(BATCH);
        if (rows.isEmpty()) return;

        List<Object> payload = new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        for (AfkSession s : rows) {
            payload.add(new java.util.LinkedHashMap<>() {{
                put("startTime", s.getStartTime());
                put("endTime", s.getEndTime());
                put("durationMs", s.getDurationMs());
            }});
            ids.add(s.getId());
        }
        if (postJson("/api/sync/afk-sessions", payload)) {
            repository.markAfkSynced(ids);
            log.info("Synced {} AFK sessions", ids.size());
        }
    }

    private void syncBrowser() throws Exception {
        List<BrowserActivity> rows = repository.getUnsyncedBrowser(BATCH);
        if (rows.isEmpty()) return;

        List<Object> payload = new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        for (BrowserActivity b : rows) {
            payload.add(new java.util.LinkedHashMap<>() {{
                put("browserName", b.getBrowserName());
                put("url", b.getUrl());
                put("pageTitle", b.getPageTitle());
                put("startTime", b.getStartTime());
                put("endTime", b.getEndTime());
                put("durationMs", b.getDurationMs());
            }});
            ids.add(b.getId());
        }
        if (postJson("/api/sync/browser-activities", payload)) {
            repository.markBrowserSynced(ids);
            log.info("Synced {} browser activities", ids.size());
        }
    }

    /** Screenshots are uploaded one file at a time as multipart/form-data. */
    private void syncScreenshots() {
        List<ScreenshotRecord> rows = repository.getUnsyncedScreenshots(BATCH);
        for (ScreenshotRecord s : rows) {
            try {
                File file = new File(s.getFilePath());
                if (!file.exists()) {
                    // File was deleted locally - don't retry forever.
                    repository.markScreenshotsSynced(List.of(s.getId()));
                    continue;
                }
                if (uploadScreenshot(file, s)) {
                    repository.markScreenshotsSynced(List.of(s.getId()));
                }
            } catch (Exception e) {
                log.warn("Screenshot upload failed for {}: {}", s.getFilePath(), e.getMessage());
            }
        }
    }

    // ---- HTTP helpers -------------------------------------------------------

    private boolean postJson(String path, Object body) throws Exception {
        String json = mapper.writeValueAsString(body);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + path))
                .header("Content-Type", "application/json")
                .header("X-API-Key", apiKey)
                .header("X-Device-ID", deviceIdentifier)
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 200) return true;

        log.warn("POST {} -> {} : {}", path, resp.statusCode(), truncate(resp.body()));
        return false;
    }

    private boolean uploadScreenshot(File file, ScreenshotRecord s) throws Exception {
        String boundary = "----trackeye" + System.currentTimeMillis();
        var byteStreams = new java.io.ByteArrayOutputStream();
        var w = new java.io.PrintWriter(new java.io.OutputStreamWriter(byteStreams, StandardCharsets.UTF_8), true);

        writePart(w, boundary, "timestamp", String.valueOf(s.getTimestamp()));
        writePart(w, boundary, "windowTitle", s.getWindowTitle() == null ? "" : s.getWindowTitle());
        writePart(w, boundary, "processName", s.getProcessName() == null ? "" : s.getProcessName());

        // file part
        w.append("--").append(boundary).append("\r\n");
        w.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                .append(file.getName()).append("\"\r\n");
        w.append("Content-Type: image/jpeg\r\n\r\n");
        w.flush();
        byteStreams.write(Files.readAllBytes(file.toPath()));
        w.append("\r\n").append("--").append(boundary).append("--").append("\r\n");
        w.flush();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + "/api/sync/screenshot"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("X-API-Key", apiKey)
                .header("X-Device-ID", deviceIdentifier)
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofByteArray(byteStreams.toByteArray()))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 200) {
            log.info("Uploaded screenshot {}", file.getName());
            return true;
        }
        log.warn("Screenshot upload -> {} : {}", resp.statusCode(), truncate(resp.body()));
        return false;
    }

    private void writePart(java.io.PrintWriter w, String boundary, String name, String value) {
        w.append("--").append(boundary).append("\r\n");
        w.append("Content-Disposition: form-data; name=\"").append(name).append("\"\r\n\r\n");
        w.append(value).append("\r\n");
    }

    private static String trimSlash(String url) {
        if (url == null) return null;
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String truncate(String s) {
        if (s == null) return "";
        return s.length() > 300 ? s.substring(0, 300) + "..." : s;
    }
}
