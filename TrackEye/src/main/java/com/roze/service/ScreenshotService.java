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
        try {
            String windowTitle = monitor.getActiveWindowTitle();
            String processName = monitor.getActiveProcessName();
            
            LocalDateTime now = LocalDateTime.now();
            String dateDir = now.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String fileName = String.format("%s_%s.jpg", 
                now.format(DateTimeFormatter.ofPattern("HH-mm-ss")), reason);
            
            Path screenshotDir = Paths.get(config.getStoragePath(), "screenshots", dateDir);
            Files.createDirectories(screenshotDir);
            Path filePath = screenshotDir.resolve(fileName);
            
            // Capture full screen
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage capture = new Robot().createScreenCapture(screenRect);
            ImageIO.write(capture, "jpg", filePath.toFile());
            
            // Save to database
            repository.saveScreenshot(new ScreenshotRecord(
                Instant.now().toEpochMilli(),
                filePath.toString(),
                windowTitle,
                processName
            ));
            
            log.debug("Screenshot captured: {} ({})", fileName, reason);
            
        } catch (AWTException e) {
            log.warn("Robot not available for screenshot: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to capture screenshot", e);
        }
    }
    
    @PreDestroy
    public void stop() {
        scheduler.shutdown();
        log.info("Screenshot service stopped");
    }
}