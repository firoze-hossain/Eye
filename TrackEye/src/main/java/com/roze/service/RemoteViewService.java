package com.roze.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Powers the admin "Watch Live" feature. This is deliberately a cheap poll,
 * not a persistent connection: every ~3s it asks the server "is anyone
 * watching me?" (GET /api/sync/watch-status). While the answer is yes, it
 * captures the screen and uploads a JPEG frame roughly every 1.2s until told
 * to stop. Nothing is written to the local database or disk - frames only
 * ever exist in memory on both ends.
 *
 * This shares its registration config with SyncService (same
 * ~/.trackeye/config.properties) but runs its own lightweight loop so a slow
 * sync cycle (every 60s) never delays how quickly a watch session starts.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RemoteViewService {

    private static final Path CONFIG_FILE =
            Paths.get(System.getProperty("user.home"), ".trackeye", "config.properties");

    private static final int IDLE_POLL_SECONDS = 3;
    private static final int STREAM_FRAME_MILLIS = 1200;
    private static final float JPEG_QUALITY = 0.5f;
    // Cap the longer edge so frames upload fast even on 4K displays.
    private static final int MAX_DIMENSION = 1280;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean streaming = new AtomicBoolean(false);

    private volatile String serverUrl;
    private volatile String apiKey;
    private volatile String deviceIdentifier;

    @jakarta.annotation.PostConstruct
    public void start() {
        reloadConfig();
        scheduler.scheduleWithFixedDelay(this::pollOnce, 10, IDLE_POLL_SECONDS, TimeUnit.SECONDS);
    }

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
            log.warn("RemoteViewService could not read client config: {}", e.getMessage());
        }
    }

    private boolean isRegistered() {
        return serverUrl != null && apiKey != null && deviceIdentifier != null && !deviceIdentifier.isEmpty();
    }

    private void pollOnce() {
        if (!isRegistered() || streaming.get()) return;
        try {
            boolean active = checkStatus();
            if (active) {
                log.info("Watch session started by admin - beginning screen stream");
                streaming.set(true);
                streamUntilStopped();
            }
        } catch (Exception e) {
            log.debug("Watch status poll failed: {}", e.getMessage());
        }
    }

    private void streamUntilStopped() {
        try {
            while (true) {
                long cycleStart = System.currentTimeMillis();
                byte[] jpeg = captureJpeg();
                boolean stillActive = uploadFrame(jpeg);
                if (!stillActive) break;

                long elapsed = System.currentTimeMillis() - cycleStart;
                long sleepMs = Math.max(0, STREAM_FRAME_MILLIS - elapsed);
                Thread.sleep(sleepMs);
            }
        } catch (Exception e) {
            log.warn("Watch stream ended: {}", e.getMessage());
        } finally {
            streaming.set(false);
            log.info("Watch session ended");
        }
    }

    private boolean checkStatus() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + "/api/sync/watch-status"))
                .header("X-API-Key", apiKey)
                .header("X-Device-ID", deviceIdentifier)
                .timeout(Duration.ofSeconds(10))
                .GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        return resp.statusCode() == 200 && resp.body().contains("\"active\":true");
    }

    private boolean uploadFrame(byte[] jpeg) throws Exception {
        String boundary = "----trackeye-watch" + System.currentTimeMillis();
        var out = new ByteArrayOutputStream();
        var w = new java.io.PrintWriter(new java.io.OutputStreamWriter(out, StandardCharsets.UTF_8), true);

        w.append("--").append(boundary).append("\r\n");
        w.append("Content-Disposition: form-data; name=\"file\"; filename=\"frame.jpg\"\r\n");
        w.append("Content-Type: image/jpeg\r\n\r\n");
        w.flush();
        out.write(jpeg);
        w.append("\r\n--").append(boundary).append("--\r\n");
        w.flush();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + "/api/sync/watch-frame"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("X-API-Key", apiKey)
                .header("X-Device-ID", deviceIdentifier)
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray()))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        return resp.statusCode() == 200 && resp.body().contains("\"active\":true");
    }

    private byte[] captureJpeg() throws Exception {
        Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        BufferedImage full = new Robot().createScreenCapture(screenRect);
        BufferedImage scaled = scaleDown(full);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        ImageWriter writer = writers.next();
        ImageWriteParam params = writer.getDefaultWriteParam();
        params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        params.setCompressionQuality(JPEG_QUALITY);
        writer.setOutput(new MemoryCacheImageOutputStream(baos));
        writer.write(null, new IIOImage(scaled, null, null), params);
        writer.dispose();
        return baos.toByteArray();
    }

    private BufferedImage scaleDown(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        int longEdge = Math.max(w, h);
        if (longEdge <= MAX_DIMENSION) return src;
        double scale = MAX_DIMENSION / (double) longEdge;
        int nw = (int) (w * scale), nh = (int) (h * scale);
        BufferedImage resized = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();
        return resized;
    }

    private static String trimSlash(String url) {
        if (url == null) return null;
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
