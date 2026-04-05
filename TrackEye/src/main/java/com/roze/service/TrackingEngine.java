package com.roze.service;

import com.roze.config.AppConfig;
import com.roze.model.ActivitySession;
import com.roze.model.AfkSession;
import com.roze.model.BrowserActivity;
import com.roze.platform.ActivityMonitor;
import com.roze.repository.SessionRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingEngine {

    private final ActivityMonitor monitor;
    private final AppConfig config;
    private final SessionRepository repository;
    private final BrowserTrackingService browserTrackingService;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private String currentApp = "";
    private String currentTitle = "";
    private String currentProcess = "";
    private Instant sessionStart = Instant.now();

    private boolean isIdle = false;
    private Instant idleStart = null;

    @PostConstruct
    public void start() {
        scheduler.scheduleAtFixedRate(this::track, 0, config.getPollIntervalMs(), TimeUnit.MILLISECONDS);
        log.info("Tracking engine started with interval: {}ms", config.getPollIntervalMs());
    }

    private void track() {
        try {
            long idleMs = monitor.getIdleTimeMillis();
            boolean currentlyIdle = idleMs > (config.getIdleTimeoutSeconds() * 1000L);

            if (currentlyIdle && !isIdle) {
                endCurrentActivity();
                isIdle = true;
                idleStart = Instant.now();
                log.debug("User became idle after {}ms", idleMs);

            } else if (!currentlyIdle && isIdle) {
                if (idleStart != null) {
                    long idleDuration = Instant.now().toEpochMilli() - idleStart.toEpochMilli();
                    repository.saveAfk(new AfkSession(
                            idleStart.toEpochMilli(),
                            Instant.now().toEpochMilli(),
                            idleDuration
                    ));
                    log.debug("AFK session ended: {}ms", idleDuration);
                }
                isIdle = false;
                idleStart = null;
                startNewActivity();

            } else if (!currentlyIdle) {
                String rawWindowTitle = monitor.getActiveWindowTitle();
                String rawProcessName = monitor.getActiveProcessName();

                // Handle null values
                if (rawWindowTitle == null) rawWindowTitle = "";
                if (rawProcessName == null) rawProcessName = "";

                // Dynamically determine the best app name from available data
                AppInfo appInfo = determineBestAppInfo(rawWindowTitle, rawProcessName);

                String appName = appInfo.appName;
                String windowTitle = appInfo.windowTitle;
                String processName = appInfo.processName;

                // Skip tracking for system/desktop processes
                if (isSystemProcess(processName, windowTitle)) {
                    return;
                }

                // Track browser activity if applicable
                if (!windowTitle.isEmpty() && isBrowserProcess(processName)) {
                    BrowserActivity browserActivity = browserTrackingService.getActiveBrowserFromWindow(windowTitle);
                    if (browserActivity != null && browserActivity.getBrowserName() != null) {
                        log.trace("Browser detected: {} - {}", browserActivity.getBrowserName(), browserActivity.getUrl());
                    }
                }

                // Check if we switched to a different application
                if (!appName.equals(currentApp)) {
                    endCurrentActivity();
                    currentApp = appName;
                    currentTitle = windowTitle;
                    currentProcess = processName;
                    sessionStart = Instant.now();
                    log.info("Switched to: {} | Window: {} | Process: {}", appName, windowTitle, processName);
                } else if (!windowTitle.isEmpty() && !windowTitle.equals(currentTitle)) {
                    // Same app, different window
                    currentTitle = windowTitle;
                    log.debug("Window title changed: {}", windowTitle);
                }
            }
        } catch (Exception e) {
            log.error("Error in tracking loop: {}", e.getMessage(), e);
        }
    }

    /**
     * Dynamically determines the best app name from available data
     * This is the key method that makes it work like Hubstaff/Desktime
     */
    private AppInfo determineBestAppInfo(String windowTitle, String processName) {
        // Clean up the inputs
        windowTitle = cleanString(windowTitle);
        processName = cleanString(processName);

        String bestAppName = "";
        String bestWindowTitle = "";
        String bestProcessName = processName;

        // Strategy 1: Use window title if it's meaningful (not a system process name)
        if (!windowTitle.isEmpty() && isMeaningfulWindowTitle(windowTitle)) {
            // Extract app name from window title (remove common patterns)
            bestWindowTitle = windowTitle;

            // Try to extract app name from window title
            // Pattern: "Document.txt - AppName" or "AppName" or "AppName - Something"
            String extractedApp = extractAppFromWindowTitle(windowTitle);
            if (!extractedApp.isEmpty()) {
                bestAppName = extractedApp;
            } else {
                bestAppName = windowTitle;
            }
        }

        // Strategy 2: Use process name if window title wasn't meaningful
        if (bestAppName.isEmpty() && !processName.isEmpty()) {
            bestAppName = formatProcessName(processName);
            if (bestWindowTitle.isEmpty()) {
                bestWindowTitle = bestAppName;
            }
        }

        // Strategy 3: Fallback to unknown
        if (bestAppName.isEmpty()) {
            bestAppName = "Unknown";
            bestWindowTitle = windowTitle.isEmpty() ? "Unknown" : windowTitle;
        }

        return new AppInfo(bestAppName, bestWindowTitle, bestProcessName);
    }

    /**
     * Extract application name from window title dynamically
     * Examples: "Document.txt - Notepad" -> "Notepad"
     *          "Google Chrome" -> "Google Chrome"
     *          "Postman - Workspace" -> "Postman"
     */
    private String extractAppFromWindowTitle(String title) {
        if (title == null || title.isEmpty()) return "";

        // Common patterns: "Title - AppName" or "Title | AppName" or "Title • AppName"
        String[] separators = {" - ", " | ", " • ", " — ", " › "};

        for (String separator : separators) {
            if (title.contains(separator)) {
                String[] parts = title.split(separator);
                // The app name is usually the last part
                String possibleApp = parts[parts.length - 1].trim();
                if (possibleApp.length() > 0 && possibleApp.length() < 50) {
                    return possibleApp;
                }
            }
        }

        // If no separator, the whole title might be the app name
        if (title.length() < 50 && !title.contains(" ") && !title.contains(".")) {
            return title;
        }

        // For browsers, the app name is often at the end
        String[] browsers = {"Firefox", "Chrome", "Brave", "Edge", "Opera", "Safari"};
        for (String browser : browsers) {
            if (title.contains(browser)) {
                return browser;
            }
        }

        return "";
    }

    /**
     * Clean string by removing "Active: " prefix and trimming
     */
    private String cleanString(String str) {
        if (str == null) return "";
        if (str.startsWith("Active: ")) {
            str = str.substring(8);
        }
        return str.trim();
    }

    /**
     * Check if window title is meaningful (not a system process name)
     */
    private boolean isMeaningfulWindowTitle(String title) {
        if (title == null || title.isEmpty()) return false;

        String[] systemNames = {
                "gnome-shell", "kwin", "plasmashell", "xfwm4", "openbox",
                "java", "node", "next-server", "web", "isolated", "ps", "sh", "bash",
                "systemd", "kernel", "init", "dbus", "gjs", "gdbus"
        };

        String lowerTitle = title.toLowerCase();
        for (String systemName : systemNames) {
            if (lowerTitle.equals(systemName) || lowerTitle.startsWith(systemName)) {
                return false;
            }
        }

        // Valid titles usually have length > 2 and contain letters
        return title.length() > 2 && title.matches(".*[a-zA-Z].*");
    }

    /**
     * Check if this is a system/desktop process we should skip
     */
    private boolean isSystemProcess(String processName, String windowTitle) {
        String combined = (processName + " " + windowTitle).toLowerCase();
        String[] systemPatterns = {
                "gnome-shell", "kwin", "plasmashell", "xfwm4", "openbox",
                "systemd", "dbus", "gdbus", "gjs", "trackeye"
        };

        for (String pattern : systemPatterns) {
            if (combined.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Format process name for display (remove paths, extensions, clean up)
     */
    private String formatProcessName(String processName) {
        if (processName == null || processName.isEmpty()) return "";

        // Remove path if present
        if (processName.contains("/")) {
            processName = processName.substring(processName.lastIndexOf("/") + 1);
        }
        if (processName.contains("\\")) {
            processName = processName.substring(processName.lastIndexOf("\\") + 1);
        }

        // Remove extensions
        processName = processName.replace(".exe", "").replace(".app", "").replace(".jar", "");

        // Convert snake_case or kebab-case to spaces
        processName = processName.replace("_", " ").replace("-", " ");

        // Capitalize first letter of each word
        String[] words = processName.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }

        return result.toString().trim();
    }

    /**
     * Check if process is a browser
     */
    private boolean isBrowserProcess(String processName) {
        if (processName == null) return false;
        String lower = processName.toLowerCase();
        return lower.contains("firefox") || lower.contains("chrome") ||
                lower.contains("brave") || lower.contains("edge") ||
                lower.contains("opera") || lower.contains("safari");
    }

    private void startNewActivity() {
        currentApp = "";
        currentTitle = "";
        currentProcess = "";
        sessionStart = Instant.now();
    }

    private void endCurrentActivity() {
        if (currentApp == null || currentApp.isEmpty()) return;

        // Don't save system processes
        if (isSystemProcess(currentProcess, currentTitle)) {
            return;
        }

        Instant now = Instant.now();
        long duration = now.toEpochMilli() - sessionStart.toEpochMilli();

        if (duration >= 1000) {
            repository.saveActivity(new ActivitySession(
                    currentApp,
                    currentTitle != null ? currentTitle : "",
                    currentProcess != null ? currentProcess : "",
                    sessionStart.toEpochMilli(),
                    now.toEpochMilli(),
                    duration
            ));
            log.debug("Saved activity: {} for {}ms", currentApp, duration);
        }
    }

    @PreDestroy
    public void stop() {
        endCurrentActivity();
        if (isIdle && idleStart != null) {
            repository.saveAfk(new AfkSession(
                    idleStart.toEpochMilli(),
                    Instant.now().toEpochMilli(),
                    Instant.now().toEpochMilli() - idleStart.toEpochMilli()
            ));
        }
        scheduler.shutdown();
        log.info("Tracking engine stopped");
    }

    /**
     * Inner class to hold app info
     */
    private static class AppInfo {
        final String appName;
        final String windowTitle;
        final String processName;

        AppInfo(String appName, String windowTitle, String processName) {
            this.appName = appName;
            this.windowTitle = windowTitle;
            this.processName = processName;
        }
    }
}