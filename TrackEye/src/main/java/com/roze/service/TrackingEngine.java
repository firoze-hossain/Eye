package com.roze.service;

import com.roze.config.AppConfig;
import com.roze.model.ActivitySession;
import com.roze.model.AfkSession;
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
                // User became idle
                endCurrentActivity();
                isIdle = true;
                idleStart = Instant.now();
                log.debug("User became idle after {}ms", idleMs);
                
            } else if (!currentlyIdle && isIdle) {
                // User returned from idle
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
                // User is active - track current app
                String windowTitle = monitor.getActiveWindowTitle();
                String processName = monitor.getActiveProcessName();
                String appName = extractAppName(processName, windowTitle);
                
                if (!appName.equals(currentApp)) {
                    endCurrentActivity();
                    currentApp = appName;
                    currentTitle = windowTitle;
                    currentProcess = processName;
                    sessionStart = Instant.now();
                    log.debug("Switched to: {} ({})", appName, processName);
                } else if (!windowTitle.equals(currentTitle)) {
                    // Same app, different window - update title but keep same session
                    currentTitle = windowTitle;
                }
            }
        } catch (Exception e) {
            log.error("Error in tracking loop", e);
        }
    }

    private void startNewActivity() {
        currentApp = "";
        currentTitle = "";
        currentProcess = "";
        sessionStart = Instant.now();
    }

    private void endCurrentActivity() {
        if (currentApp.isEmpty()) return;
        
        Instant now = Instant.now();
        long duration = now.toEpochMilli() - sessionStart.toEpochMilli();
        
        if (duration >= 1000) { // Only save if at least 1 second
            repository.saveActivity(new ActivitySession(
                currentApp,
                currentTitle,
                currentProcess,
                sessionStart.toEpochMilli(),
                now.toEpochMilli(),
                duration
            ));
            log.debug("Saved activity: {} for {}ms", currentApp, duration);
        }
    }
    
    private String extractAppName(String processName, String windowTitle) {
        if (processName != null && !processName.isEmpty()) {
            return processName.replace(".exe", "").replace(".app", "").trim();
        }
        if (windowTitle != null && windowTitle.contains(" - ")) {
            String[] parts = windowTitle.split(" - ");
            return parts[parts.length - 1].trim();
        }
        return windowTitle != null ? windowTitle : "Unknown";
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
}