package com.roze.trackeyecentral.service;// src/main/java/com/trackeye/service/DataSyncService.java (Complete)


import com.roze.trackeyecentral.dto.ActivitySyncRequest;
import com.roze.trackeyecentral.dto.AfkSessionSyncRequest;
import com.roze.trackeyecentral.dto.ScreenshotSyncRequest;
import com.roze.trackeyecentral.dto.SyncResponse;
import com.roze.trackeyecentral.model.AfkSession;
import com.roze.trackeyecentral.model.EmployeeActivity;
import com.roze.trackeyecentral.model.EmployeeBrowserActivity;
import com.roze.trackeyecentral.model.EmployeeScreenshot;
import com.roze.trackeyecentral.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataSyncService {

    private final ActivityRepository activityRepository;
    private final ScreenshotRepository screenshotRepository;
    private final BrowserActivityRepository browserActivityRepository;
    private final AfkSessionRepository afkSessionRepository;
    private final DeviceRepository deviceRepository;

    @Value("${trackeye.storage.screenshots:./data/screenshots}")
    private String screenshotStoragePath;

    @Transactional
    public SyncResponse saveActivities(Long deviceId, List<ActivitySyncRequest> activities) {
        long now = System.currentTimeMillis();
        int saved = 0;

        for (ActivitySyncRequest request : activities) {
            EmployeeActivity activity = new EmployeeActivity();
            activity.setDeviceId(deviceId);
            activity.setAppName(request.getAppName());
            activity.setWindowTitle(request.getWindowTitle());
            activity.setProcessName(request.getProcessName());
            activity.setStartTime(request.getStartTime());
            activity.setEndTime(request.getEndTime());
            activity.setDurationMs(request.getDurationMs());
            activity.setSyncedAt(now);

            activityRepository.save(activity);
            saved++;
        }

        // Update device last seen
        deviceRepository.updateLastSeen(deviceId, now);

        log.info("Saved {} activities for device {}", saved, deviceId);

        return SyncResponse.builder()
                .success(true)
                .recordsSaved(saved)
                .lastSyncTimestamp(now)
                .build();
    }

    @Transactional
    public SyncResponse saveScreenshot(Long deviceId, MultipartFile file, Long timestamp,
                                       String windowTitle, String processName) {
        try {
            long now = System.currentTimeMillis();

            // Generate unique filename
            String dateDir = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            String fileName = String.format("%d_%s_%s.jpg",
                    timestamp, deviceId, UUID.randomUUID().toString().substring(0, 8));

            Path storageDir = Paths.get(screenshotStoragePath, dateDir);
            Files.createDirectories(storageDir);

            Path filePath = storageDir.resolve(fileName);

            // Save file
            file.transferTo(filePath.toFile());

            // Calculate file hash for deduplication
            String fileHash = calculateFileHash(file.getBytes());

            // Save metadata
            EmployeeScreenshot screenshot = new EmployeeScreenshot();
            screenshot.setDeviceId(deviceId);
            screenshot.setScreenshotUrl(filePath.toString());
            screenshot.setScreenshotHash(fileHash);
            screenshot.setTimestamp(timestamp);
            screenshot.setWindowTitle(windowTitle);
            screenshot.setProcessName(processName);
            screenshot.setSyncedAt(now);

            screenshotRepository.save(screenshot);

            // Update device last seen
            deviceRepository.updateLastSeen(deviceId, now);

            log.info("Saved screenshot for device {}", deviceId);

            return SyncResponse.builder()
                    .success(true)
                    .recordsSaved(1)
                    .lastSyncTimestamp(now)
                    .build();

        } catch (IOException e) {
            log.error("Failed to save screenshot: {}", e.getMessage());
            return SyncResponse.builder()
                    .success(false)
                    .errorMessage("Failed to save screenshot: " + e.getMessage())
                    .build();
        }
    }

    @Transactional
    public SyncResponse saveScreenshots(Long deviceId, List<ScreenshotSyncRequest> screenshots) {
        long now = System.currentTimeMillis();
        int saved = 0;

        for (ScreenshotSyncRequest request : screenshots) {
            EmployeeScreenshot screenshot = new EmployeeScreenshot();
            screenshot.setDeviceId(deviceId);
            screenshot.setScreenshotUrl(request.getScreenshotUrl());
            screenshot.setTimestamp(request.getTimestamp());
            screenshot.setWindowTitle(request.getWindowTitle());
            screenshot.setProcessName(request.getProcessName());
            screenshot.setSyncedAt(now);

            screenshotRepository.save(screenshot);
            saved++;
        }

        deviceRepository.updateLastSeen(deviceId, now);

        log.info("Saved {} screenshot records for device {}", saved, deviceId);

        return SyncResponse.builder()
                .success(true)
                .recordsSaved(saved)
                .lastSyncTimestamp(now)
                .build();
    }

    @Transactional
    public SyncResponse saveBrowserActivities(Long deviceId, List<BrowserActivitySyncRequest> activities) {
        long now = System.currentTimeMillis();
        int saved = 0;

        for (BrowserActivitySyncRequest request : activities) {
            EmployeeBrowserActivity activity = new EmployeeBrowserActivity();
            activity.setDeviceId(deviceId);
            activity.setBrowserName(request.getBrowserName());
            activity.setUrl(request.getUrl());
            activity.setPageTitle(request.getPageTitle());
            activity.setStartTime(request.getStartTime());
            activity.setEndTime(request.getEndTime());
            activity.setDurationMs(request.getDurationMs());
            activity.setSyncedAt(now);

            browserActivityRepository.save(activity);
            saved++;
        }

        deviceRepository.updateLastSeen(deviceId, now);

        log.info("Saved {} browser activities for device {}", saved, deviceId);

        return SyncResponse.builder()
                .success(true)
                .recordsSaved(saved)
                .lastSyncTimestamp(now)
                .build();
    }

    @Transactional
    public SyncResponse saveAfkSessions(Long deviceId, List<AfkSessionSyncRequest> sessions) {
        long now = System.currentTimeMillis();
        int saved = 0;

        for (AfkSessionSyncRequest request : sessions) {
            AfkSession session = new AfkSession();
            session.setDeviceId(deviceId);
            session.setStartTime(request.getStartTime());
            session.setEndTime(request.getEndTime());
            session.setDurationMs(request.getDurationMs());
            session.setSyncedAt(now);

            afkSessionRepository.save(session);
            saved++;
        }

        deviceRepository.updateLastSeen(deviceId, now);

        log.info("Saved {} AFK sessions for device {}", saved, deviceId);

        return SyncResponse.builder()
                .success(true)
                .recordsSaved(saved)
                .lastSyncTimestamp(now)
                .build();
    }

    private String calculateFileHash(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }

    @lombok.Data
    public static class BrowserActivitySyncRequest {
        private String browserName;
        private String url;
        private String pageTitle;
        private Long startTime;
        private Long endTime;
        private Long durationMs;
    }


}