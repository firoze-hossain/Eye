package com.roze.service;

import com.roze.config.AppConfig;
import com.roze.model.ScreenshotRecord;
import com.roze.platform.ActivityMonitor;
import com.roze.repository.SessionRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScreenshotService {

    private final AppConfig config;
    private final ActivityMonitor monitor;
    private final SessionRepository repository;
    
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private String lastProcess = "";

    // Backoff state for the Wayland/GNOME portal-permission problem (see capture()).
    private int consecutiveFailures = 0;
    private volatile long backoffUntil = 0;
    private static final long[] BACKOFF_MINUTES = {2, 5, 15, 30, 60};

    // Runs the actual Robot capture on its own thread so a blocked OS
    // permission dialog (see capture()) can time out instead of freezing
    // every future scheduled capture.
    private final ExecutorService captureExecutor = Executors.newSingleThreadExecutor();
    private static final int CAPTURE_TIMEOUT_SECONDS = 8;

    @PostConstruct
    public void start() {
        if (GraphicsEnvironment.isHeadless()) {
            log.warn("Headless mode detected - screenshots disabled");
            return;
        }
        
        // Periodic screenshots
        scheduler.scheduleAtFixedRate(
            this::capturePeriodic,
            30,
            config.getScreenshotIntervalSeconds(),
            TimeUnit.SECONDS
        );
        
        // App switch screenshots
        if (config.isScreenshotOnAppSwitch()) {
            scheduler.scheduleAtFixedRate(
                this::captureOnAppSwitch,
                10,
                2,
                TimeUnit.SECONDS
            );
        }
        
        log.info("Screenshot service started (interval: {}s, app switch: {})", 
            config.getScreenshotIntervalSeconds(), config.isScreenshotOnAppSwitch());
    }
    
    private void capturePeriodic() {
        capture("periodic");
    }
    
    private void captureOnAppSwitch() {
        String currentProcess = monitor.getActiveProcessName();
        if (!currentProcess.equals(lastProcess) && !currentProcess.isEmpty()) {
            lastProcess = currentProcess;
            capture("switch");
        }
    }
    
    private void capture(String reason) {
        // FIX: on GNOME/Wayland (and similarly on macOS), Robot's screen
        // capture can go through an OS permission portal that shows an
        // interactive "Share your screen?" dialog - especially right after
        // unlocking the session, when the prior grant is invalidated. Two
        // separate problems follow from this:
        //   1. If denied/pending, retrying on every 5-min interval AND every
        //      app switch nags the employee with a fresh popup repeatedly.
        //   2. Worse: the capture call can BLOCK waiting for the user to
        //      respond to that dialog. Since this runs on the single scheduler
        //      thread, a stuck dialog would silently freeze EVERY future
        //      periodic/app-switch capture until someone deals with it.
        // Fix for (1) is the backoff below. Fix for (2) is running the actual
        // capture on its own thread with a hard timeout, so a stuck dialog
        // can never block anything beyond this one attempt.
        if (System.currentTimeMillis() < backoffUntil) {
            log.debug("Screenshot capture in backoff (permission prompt likely pending/denied) - skipping");
            return;
        }

        Future<?> future = captureExecutor.submit(() -> doCapture(reason));
        try {
            future.get(CAPTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            log.warn("Screenshot capture timed out after {}s - likely a pending OS permission dialog. " +
                    "Backing off instead of retrying immediately.", CAPTURE_TIMEOUT_SECONDS);
            registerFailure();
        } catch (Exception e) {
            log.error("Failed to capture screenshot", e);
            registerFailure();
        }
    }

    private void doCapture(String reason) {
        try {
            String windowTitle = monitor.getActiveWindowTitle();
            String processName = monitor.getActiveProcessName();

            LocalDateTime now = LocalDateTime.now();
            String dateDir = now.format(DateTimeFormatter.ISO_LOCAL_DATE);
            Path screenshotDir = Paths.get(config.getStoragePath(), "screenshots", dateDir);
            Files.createDirectories(screenshotDir);

            // FIX: Toolkit.getDefaultToolkit().getScreenSize() returns ONLY the
            // primary display's dimensions - on a laptop + external monitor
            // setup (very common on Mac), whatever's happening on the SECOND
            // screen was never captured at all, regardless of where the mouse
            // or the active window actually was. GraphicsEnvironment reports
            // every connected display; capture each one as its own screenshot
            // (rather than stitching one combined image, which has known
            // reliability issues across displays with different DPI/scaling,
            // especially on macOS Retina + non-Retina mixes).
            GraphicsDevice[] screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
            int savedCount = 0;

            for (int i = 0; i < screens.length; i++) {
                GraphicsDevice screen = screens[i];
                Rectangle bounds = screen.getDefaultConfiguration().getBounds();
                String monitorSuffix = screens.length > 1 ? "_m" + (i + 1) : "";
                String fileName = String.format("%s_%s%s.jpg",
                        now.format(DateTimeFormatter.ofPattern("HH-mm-ss")), reason, monitorSuffix);
                Path filePath = screenshotDir.resolve(fileName);

                BufferedImage capture = new Robot(screen).createScreenCapture(bounds);
                ImageIO.write(capture, "jpg", filePath.toFile());

                String label = screens.length > 1
                        ? windowTitle + " [Monitor " + (i + 1) + " of " + screens.length + "]"
                        : windowTitle;

                repository.saveScreenshot(new ScreenshotRecord(
                        Instant.now().toEpochMilli(),
                        filePath.toString(),
                        label,
                        processName
                ));
                savedCount++;
            }

            log.debug("Screenshot captured: {} monitor(s) ({})", savedCount, reason);
            consecutiveFailures = 0;
            backoffUntil = 0;

        } catch (AWTException e) {
            log.warn("Robot not available for screenshot: {}", e.getMessage());
            registerFailure();
        } catch (Exception e) {
            log.error("Failed to capture screenshot", e);
            registerFailure();
        }
    }

    private void registerFailure() {
        int idx = Math.min(consecutiveFailures, BACKOFF_MINUTES.length - 1);
        long minutes = BACKOFF_MINUTES[idx];
        consecutiveFailures++;
        backoffUntil = System.currentTimeMillis() + minutes * 60_000;
        if (consecutiveFailures == 2) {
            // Only warn loudly once it looks like a real pattern, not a one-off blip.
            log.warn("Screenshot capture has failed {} times in a row - backing off for {} min. " +
                    "On Linux this is usually the GNOME/Wayland screen-share permission prompt " +
                    "(especially after locking/unlocking). Switching to an X11 session at login " +
                    "avoids this entirely.", consecutiveFailures, minutes);
        }
    }
    
    @PreDestroy
    public void stop() {
        scheduler.shutdown();
        captureExecutor.shutdownNow();
        log.info("Screenshot service stopped");
    }
}