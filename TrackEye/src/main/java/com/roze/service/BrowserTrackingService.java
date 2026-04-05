package com.roze.service;

import com.roze.model.BrowserActivity;
import com.roze.repository.SessionRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrowserTrackingService {

    private final SessionRepository repository;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private Map<String, BrowserInfo> activeBrowsers = new ConcurrentHashMap<>();
    private Map<String, BrowserConfig> detectedBrowsers = new HashMap<>();

    // Browser name mappings for detection
    private static final Map<String, String> BROWSER_NAMES = new HashMap<>();

    static {
        BROWSER_NAMES.put("brave", "Brave");
        BROWSER_NAMES.put("chrome", "Google Chrome");
        BROWSER_NAMES.put("chromium", "Chromium");
        BROWSER_NAMES.put("firefox", "Firefox");
        BROWSER_NAMES.put("mozilla", "Firefox");
        BROWSER_NAMES.put("edge", "Microsoft Edge");
        BROWSER_NAMES.put("microsoft-edge", "Microsoft Edge");
        BROWSER_NAMES.put("opera", "Opera");
        BROWSER_NAMES.put("vivaldi", "Vivaldi");
        BROWSER_NAMES.put("arc", "Arc Browser");
        BROWSER_NAMES.put("safari", "Safari");
    }

    @PostConstruct
    public void start() {
        autoDetectAllBrowsers();

        if (detectedBrowsers.isEmpty()) {
            log.warn("No browsers detected. Will try fallback detection methods.");
            fallbackBrowserDetection();
        }

        scheduler.scheduleAtFixedRate(this::trackBrowserActivity, 0, 3, TimeUnit.SECONDS);
        log.info("Browser tracking service started with {} browsers detected: {}",
                detectedBrowsers.size(), detectedBrowsers.keySet());
    }

    /**
     * Auto-detect all browsers on the system
     */
    private void autoDetectAllBrowsers() {
        String userHome = System.getProperty("user.home");

        log.info("Auto-detecting browsers on system...");

        // Method 1: Search common config directories
        searchCommonConfigDirectories(userHome);

        // Method 2: Search snap packages
        searchSnapPackages(userHome);

        // Method 3: Search flatpak packages
        searchFlatpakPackages(userHome);

        // Method 4: Search system-wide installations
        searchSystemWideInstallations();

        // Method 5: Use 'find' command to locate history files
        searchViaFindCommand(userHome);

        // Method 6: Check running processes for browsers
        detectBrowsersFromProcesses();

        // Log detection results
        if (detectedBrowsers.isEmpty()) {
            log.warn("No browsers found. Please ensure browsers are installed.");
        } else {
            log.info("Detection complete. Found {} browser(s):", detectedBrowsers.size());
            for (Map.Entry<String, BrowserConfig> entry : detectedBrowsers.entrySet()) {
                log.info("  ✓ {} -> {}", entry.getKey(), entry.getValue().getPaths());
            }
        }
    }

    /**
     * Search common config directories for browser history
     */
    private void searchCommonConfigDirectories(String userHome) {
        Path configDir = Paths.get(userHome, ".config");
        if (!Files.exists(configDir)) return;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(configDir)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    String dirName = entry.getFileName().toString().toLowerCase();

                    // Check if this directory looks like a browser config
                    for (Map.Entry<String, String> browser : BROWSER_NAMES.entrySet()) {
                        if (dirName.contains(browser.getKey())) {
                            // Look for History file in common locations
                            List<Path> historyPaths = findHistoryFiles(entry);
                            if (!historyPaths.isEmpty()) {
                                String browserName = browser.getValue();
                                if (!detectedBrowsers.containsKey(browserName)) {
                                    detectedBrowsers.put(browserName, new BrowserConfig(
                                            browserName, historyPaths, browser.getKey()));
                                    log.info("Found {} via .config: {}", browserName, historyPaths);
                                } else {
                                    // Add additional paths
                                    detectedBrowsers.get(browserName).getPaths().addAll(historyPaths);
                                }
                            }
                            break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.debug("Error scanning .config directory: {}", e.getMessage());
        }
    }

    /**
     * Find history files in a browser directory
     */
    private List<Path> findHistoryFiles(Path browserDir) {
        List<Path> historyPaths = new ArrayList<>();

        try {
            // Look for History file (Chrome, Brave, Edge, Opera)
            Path historyFile = browserDir.resolve("Default/History");
            if (Files.exists(historyFile) && Files.isReadable(historyFile)) {
                historyPaths.add(historyFile);
            }

            // Check for Profile directories
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(browserDir)) {
                for (Path entry : stream) {
                    if (Files.isDirectory(entry) && entry.getFileName().toString().matches("Profile\\d+|Default")) {
                        Path profileHistory = entry.resolve("History");
                        if (Files.exists(profileHistory) && Files.isReadable(profileHistory)) {
                            historyPaths.add(profileHistory);
                        }
                    }
                }
            }

            // Check for places.sqlite (Firefox)
            Path placesFile = browserDir.resolve("places.sqlite");
            if (Files.exists(placesFile) && Files.isReadable(placesFile)) {
                historyPaths.add(placesFile);
            }

        } catch (IOException e) {
            log.debug("Error finding history files in {}: {}", browserDir, e.getMessage());
        }

        return historyPaths;
    }

    /**
     * Search snap packages for browser history
     */
    private void searchSnapPackages(String userHome) {
        Path snapDir = Paths.get(userHome, "snap");
        if (!Files.exists(snapDir)) return;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(snapDir)) {
            for (Path snap : stream) {
                String snapName = snap.getFileName().toString().toLowerCase();

                for (Map.Entry<String, String> browser : BROWSER_NAMES.entrySet()) {
                    if (snapName.contains(browser.getKey())) {
                        // Search for history in snap package
                        Path currentDir = snap.resolve("current");
                        if (Files.exists(currentDir)) {
                            List<Path> historyPaths = findHistoryFilesRecursively(currentDir, 3);
                            if (!historyPaths.isEmpty()) {
                                String browserName = browser.getValue();
                                if (!detectedBrowsers.containsKey(browserName)) {
                                    detectedBrowsers.put(browserName, new BrowserConfig(
                                            browserName, historyPaths, browser.getKey()));
                                    log.info("Found {} via Snap: {}", browserName, historyPaths);
                                }
                            }
                        }
                        break;
                    }
                }
            }
        } catch (IOException e) {
            log.debug("Error scanning snap directory: {}", e.getMessage());
        }
    }

    /**
     * Search flatpak packages for browser history
     */
    private void searchFlatpakPackages(String userHome) {
        Path flatpakDir = Paths.get(userHome, ".var", "app");
        if (!Files.exists(flatpakDir)) return;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(flatpakDir)) {
            for (Path app : stream) {
                String appName = app.getFileName().toString().toLowerCase();

                for (Map.Entry<String, String> browser : BROWSER_NAMES.entrySet()) {
                    if (appName.contains(browser.getKey())) {
                        List<Path> historyPaths = findHistoryFilesRecursively(app, 5);
                        if (!historyPaths.isEmpty()) {
                            String browserName = browser.getValue();
                            if (!detectedBrowsers.containsKey(browserName)) {
                                detectedBrowsers.put(browserName, new BrowserConfig(
                                        browserName, historyPaths, browser.getKey()));
                                log.info("Found {} via Flatpak: {}", browserName, historyPaths);
                            }
                        }
                        break;
                    }
                }
            }
        } catch (IOException e) {
            log.debug("Error scanning flatpak directory: {}", e.getMessage());
        }
    }

    /**
     * Search system-wide installations
     */
    private void searchSystemWideInstallations() {
        String[] systemPaths = {
                "/opt", "/usr/share", "/usr/lib", "/var/lib/flatpak/app"
        };

        for (String systemPath : systemPaths) {
            Path path = Paths.get(systemPath);
            if (!Files.exists(path)) continue;

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                for (Path entry : stream) {
                    String entryName = entry.getFileName().toString().toLowerCase();

                    for (Map.Entry<String, String> browser : BROWSER_NAMES.entrySet()) {
                        if (entryName.contains(browser.getKey())) {
                            List<Path> historyPaths = findHistoryFilesRecursively(entry, 4);
                            if (!historyPaths.isEmpty()) {
                                String browserName = browser.getValue();
                                if (!detectedBrowsers.containsKey(browserName)) {
                                    detectedBrowsers.put(browserName, new BrowserConfig(
                                            browserName, historyPaths, browser.getKey()));
                                    log.info("Found {} via system path {}: {}", browserName, systemPath, historyPaths);
                                }
                            }
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                log.debug("Error scanning {}: {}", systemPath, e.getMessage());
            }
        }
    }

    /**
     * Use find command to locate history files
     */
    private void searchViaFindCommand(String userHome) {
        try {
            // Find all History files in user's home
            ProcessBuilder pb = new ProcessBuilder("sh", "-c",
                    "find " + userHome + " -name 'History' -type f 2>/dev/null | head -20");
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Path historyPath = Paths.get(line.trim());
                    if (Files.exists(historyPath) && Files.isReadable(historyPath)) {
                        detectBrowserFromPath(historyPath);
                    }
                }
            }
            process.waitFor(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.debug("Error using find command: {}", e.getMessage());
        }

        // Also find Firefox places.sqlite
        try {
            ProcessBuilder pb = new ProcessBuilder("sh", "-c",
                    "find " + userHome + " -name 'places.sqlite' -type f 2>/dev/null | head -10");
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Path historyPath = Paths.get(line.trim());
                    if (Files.exists(historyPath) && Files.isReadable(historyPath)) {
                        detectBrowserFromPath(historyPath);
                    }
                }
            }
            process.waitFor(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.debug("Error finding places.sqlite: {}", e.getMessage());
        }
    }

    /**
     * Detect browser from file path
     */
    private void detectBrowserFromPath(Path historyPath) {
        String pathStr = historyPath.toString().toLowerCase();

        for (Map.Entry<String, String> browser : BROWSER_NAMES.entrySet()) {
            if (pathStr.contains(browser.getKey())) {
                String browserName = browser.getValue();
                List<Path> paths = new ArrayList<>();
                paths.add(historyPath);

                if (!detectedBrowsers.containsKey(browserName)) {
                    detectedBrowsers.put(browserName, new BrowserConfig(
                            browserName, paths, browser.getKey()));
                    log.info("Found {} via find command: {}", browserName, historyPath);
                } else {
                    // Add as additional path if not already present
                    BrowserConfig config = detectedBrowsers.get(browserName);
                    if (!config.getPaths().contains(historyPath)) {
                        config.getPaths().add(historyPath);
                    }
                }
                break;
            }
        }
    }

    /**
     * Detect browsers from running processes
     */
    private void detectBrowsersFromProcesses() {
        try {
            ProcessBuilder pb = new ProcessBuilder("sh", "-c",
                    "ps -eo comm | grep -E 'brave|chrome|chromium|firefox|mozilla|edge|opera|vivaldi' | sort -u");
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String processName = line.trim().toLowerCase();
                    for (Map.Entry<String, String> browser : BROWSER_NAMES.entrySet()) {
                        if (processName.contains(browser.getKey())) {
                            String browserName = browser.getValue();
                            if (!detectedBrowsers.containsKey(browserName)) {
                                // Create placeholder - will find actual history later
                                detectedBrowsers.put(browserName, new BrowserConfig(
                                        browserName, new ArrayList<>(), browser.getKey()));
                                log.info("Detected running browser: {} (process: {})", browserName, processName);
                            }
                            break;
                        }
                    }
                }
            }
            process.waitFor(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.debug("Error checking running processes: {}", e.getMessage());
        }
    }

    /**
     * Recursively find history files up to a certain depth
     */
    private List<Path> findHistoryFilesRecursively(Path startDir, int maxDepth) {
        List<Path> found = new ArrayList<>();

        try {
            Files.walk(startDir, maxDepth)
                    .filter(path -> {
                        String fileName = path.getFileName().toString();
                        return fileName.equals("History") || fileName.equals("places.sqlite");
                    })
                    .filter(Files::isReadable)
                    .forEach(found::add);
        } catch (IOException e) {
            log.debug("Error walking directory {}: {}", startDir, e.getMessage());
        }

        return found;
    }

    /**
     * Fallback detection using known common paths
     */
    private void fallbackBrowserDetection() {
        String userHome = System.getProperty("user.home");

        // Known common paths for various browsers
        Map<String, List<String>> commonPaths = new HashMap<>();
        commonPaths.put("Brave", Arrays.asList(
                userHome + "/.config/BraveSoftware/Brave-Browser/Default/History",
                userHome + "/.config/Brave-Browser/Default/History"
        ));
        commonPaths.put("Google Chrome", Arrays.asList(
                userHome + "/.config/google-chrome/Default/History",
                userHome + "/.config/chromium/Default/History"
        ));
        commonPaths.put("Firefox", Arrays.asList(
                userHome + "/.mozilla/firefox/*.default/places.sqlite"
        ));
        commonPaths.put("Microsoft Edge", Arrays.asList(
                userHome + "/.config/microsoft-edge/Default/History"
        ));
        commonPaths.put("Opera", Arrays.asList(
                userHome + "/.config/opera/History"
        ));
        commonPaths.put("Vivaldi", Arrays.asList(
                userHome + "/.config/vivaldi/Default/History"
        ));

        for (Map.Entry<String, List<String>> entry : commonPaths.entrySet()) {
            String browserName = entry.getKey();
            List<Path> foundPaths = new ArrayList<>();

            for (String pathPattern : entry.getValue()) {
                if (pathPattern.contains("*")) {
                    // Handle wildcards
                    Path parentDir = Paths.get(pathPattern.substring(0, pathPattern.lastIndexOf('/')));
                    if (Files.exists(parentDir)) {
                        try (DirectoryStream<Path> stream = Files.newDirectoryStream(parentDir)) {
                            for (Path dir : stream) {
                                if (Files.isDirectory(dir)) {
                                    Path historyFile = dir.resolve("places.sqlite");
                                    if (Files.exists(historyFile)) {
                                        foundPaths.add(historyFile);
                                    }
                                }
                            }
                        } catch (IOException e) {
                            log.debug("Error scanning {}: {}", parentDir, e.getMessage());
                        }
                    }
                } else {
                    Path path = Paths.get(pathPattern);
                    if (Files.exists(path) && Files.isReadable(path)) {
                        foundPaths.add(path);
                    }
                }
            }

            if (!foundPaths.isEmpty()) {
                detectedBrowsers.put(browserName, new BrowserConfig(
                        browserName, foundPaths, browserName.toLowerCase().replace(" ", "-")));
                log.info("Found {} via fallback: {}", browserName, foundPaths);
            }
        }
    }

    /**
     * Track active browser activity
     */
    private void trackBrowserActivity() {
        try {
            for (Map.Entry<String, BrowserConfig> entry : detectedBrowsers.entrySet()) {
                BrowserConfig config = entry.getValue();
                BrowserActivity activity = getCurrentBrowserActivity(config);

                if (activity != null && activity.getUrl() != null && !activity.getUrl().isEmpty()) {
                    String key = config.browserName;
                    BrowserInfo current = activeBrowsers.get(key);

                    String currentUrl = (current != null) ? current.url : null;
                    String currentTitle = (current != null) ? current.title : null;

                    boolean isDifferent = (current == null) ||
                            !safeEquals(currentUrl, activity.getUrl()) ||
                            !safeEquals(currentTitle, activity.getPageTitle());

                    if (isDifferent) {
                        if (current != null && current.startTime > 0) {
                            long duration = Instant.now().toEpochMilli() - current.startTime;
                            if (duration >= 1000) {
                                repository.saveBrowserActivity(new BrowserActivity(
                                        key,
                                        safeString(currentUrl),
                                        safeString(currentTitle),
                                        current.startTime,
                                        Instant.now().toEpochMilli(),
                                        duration
                                ));
                                log.info("Saved {} activity: {} - {} ({}ms)",
                                        key, safeString(currentTitle), currentUrl, duration);
                            }
                        }

                        activeBrowsers.put(key, new BrowserInfo(
                                activity.getUrl(),
                                activity.getPageTitle(),
                                Instant.now().toEpochMilli()
                        ));
                        log.info("{} switched to: {} - {}", key, activity.getPageTitle(), activity.getUrl());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error tracking browser activity: {}", e.getMessage(), e);
        }
    }

    private boolean safeEquals(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    private String safeString(String str) {
        return str != null ? str : "";
    }

    private BrowserActivity getCurrentBrowserActivity(BrowserConfig config) {
        for (Path historyPath : config.getPaths()) {
            try {
                if (Files.exists(historyPath) && Files.isReadable(historyPath)) {
                    BrowserActivity activity = readBrowserHistory(historyPath, config);
                    if (activity != null && activity.getUrl() != null && !activity.getUrl().isEmpty()) {
                        return activity;
                    }
                }
            } catch (Exception e) {
                log.trace("Could not read browser history from {}: {}", historyPath, e.getMessage());
            }
        }
        return null;
    }

    private BrowserActivity readBrowserHistory(Path historyPath, BrowserConfig config) {
        Path tempFile = null;
        try {
            boolean isFirefox = config.browserName.equals("Firefox");

            tempFile = Files.createTempFile("browser_history_", ".db");
            Files.copy(historyPath, tempFile, StandardCopyOption.REPLACE_EXISTING);

            String url = "jdbc:sqlite:" + tempFile.toString();
            try (Connection conn = DriverManager.getConnection(url)) {
                String sql;
                if (isFirefox) {
                    sql = "SELECT url, title FROM moz_places WHERE url LIKE 'http%' ORDER BY last_visit_date DESC LIMIT 3";
                } else {
                    sql = "SELECT url, title FROM urls WHERE url LIKE 'http%' ORDER BY last_visit_time DESC LIMIT 3";
                }

                try (Statement stmt = conn.createStatement()) {
                    ResultSet rs = stmt.executeQuery(sql);
                    if (rs.next()) {
                        String urlStr = rs.getString("url");
                        String title = rs.getString("title");

                        if (title == null) title = "";

                        if (urlStr != null && !urlStr.isEmpty() &&
                                !urlStr.startsWith("chrome://") &&
                                !urlStr.startsWith("about:") &&
                                !urlStr.startsWith("edge://") &&
                                !urlStr.startsWith("brave://") &&
                                !urlStr.startsWith("file://")) {

                            if (urlStr.startsWith("http://") || urlStr.startsWith("https://")) {
                                log.debug("Found {} activity: {} - {}", config.browserName, urlStr, title);
                                return new BrowserActivity(config.browserName, urlStr, title, 0, 0, 0);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.trace("Error reading {} history: {}", config.browserName, e.getMessage());
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
        return null;
    }

    public BrowserActivity getActiveBrowserFromWindow(String windowTitle) {
        if (windowTitle == null || windowTitle.isEmpty()) return null;

        String lowerTitle = windowTitle.toLowerCase();

        if (lowerTitle.contains("brave") || lowerTitle.contains(" - brave")) {
            return extractUrlFromTitle(windowTitle, "Brave");
        }
        if (lowerTitle.contains("firefox") || lowerTitle.contains(" - firefox")) {
            return extractUrlFromTitle(windowTitle, "Firefox");
        }
        if (lowerTitle.contains("chrome") || lowerTitle.contains("google chrome")) {
            return extractUrlFromTitle(windowTitle, "Google Chrome");
        }
        if (lowerTitle.contains("chromium")) {
            return extractUrlFromTitle(windowTitle, "Chromium");
        }
        if (lowerTitle.contains("edge") || lowerTitle.contains("microsoft edge")) {
            return extractUrlFromTitle(windowTitle, "Microsoft Edge");
        }
        if (lowerTitle.contains("opera")) {
            return extractUrlFromTitle(windowTitle, "Opera");
        }
        if (lowerTitle.contains("vivaldi")) {
            return extractUrlFromTitle(windowTitle, "Vivaldi");
        }

        return null;
    }

    private BrowserActivity extractUrlFromTitle(String title, String browser) {
        Pattern urlPattern = Pattern.compile("(https?://[^\\s/$.?#].[^\\s]*)");
        Matcher urlMatcher = urlPattern.matcher(title);

        String url = "";
        String pageTitle = title;

        if (urlMatcher.find()) {
            url = urlMatcher.group(1);
            pageTitle = title.replace(url, "").trim();
            if (pageTitle.endsWith("-")) {
                pageTitle = pageTitle.substring(0, pageTitle.length() - 1).trim();
            }
            if (pageTitle.endsWith(browser)) {
                pageTitle = pageTitle.substring(0, pageTitle.length() - browser.length()).trim();
                if (pageTitle.endsWith("-")) {
                    pageTitle = pageTitle.substring(0, pageTitle.length() - 1).trim();
                }
            }
        }

        if (!url.isEmpty()) {
            log.info("Extracted from window title - Browser: {}, URL: {}, Title: {}", browser, url, pageTitle);
            return new BrowserActivity(browser, url, pageTitle, 0, 0, 0);
        }

        return null;
    }

    @PreDestroy
    public void stop() {
        for (Map.Entry<String, BrowserInfo> entry : activeBrowsers.entrySet()) {
            BrowserInfo info = entry.getValue();
            if (info != null && info.startTime > 0) {
                long duration = Instant.now().toEpochMilli() - info.startTime;
                if (duration >= 1000) {
                    repository.saveBrowserActivity(new BrowserActivity(
                            entry.getKey(),
                            safeString(info.url),
                            safeString(info.title),
                            info.startTime,
                            Instant.now().toEpochMilli(),
                            duration
                    ));
                }
            }
        }
        scheduler.shutdown();
        log.info("Browser tracking service stopped");
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class BrowserConfig {
        String browserName;
        List<Path> paths;
        String processName;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class BrowserInfo {
        String url;
        String title;
        long startTime;
    }
}