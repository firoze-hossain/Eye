// src/main/java/com/trackeye/controller/SyncController.java (Updated with DTO imports)
package com.roze.trackeyecentral.controller;


import com.roze.trackeyecentral.dto.ActivitySyncRequest;
import com.roze.trackeyecentral.dto.AfkSessionSyncRequest;
import com.roze.trackeyecentral.dto.ScreenshotSyncRequest;
import com.roze.trackeyecentral.dto.SyncResponse;
import com.roze.trackeyecentral.service.DataSyncService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {

    private final DataSyncService dataSyncService;

    /**
     * Sync activities from client
     */
    @PostMapping("/activities")
    public ResponseEntity<SyncResponse> syncActivities(
            @RequestAttribute Long deviceId,
            @Valid @RequestBody List<ActivitySyncRequest> activities,
            HttpServletRequest request) {
        
        log.info("Syncing {} activities for device {}", activities.size(), deviceId);
        SyncResponse response = dataSyncService.saveActivities(deviceId, activities);
        return ResponseEntity.ok(response);
    }

    /**
     * Sync screenshot file and metadata
     */
    @PostMapping("/screenshot")
    public ResponseEntity<SyncResponse> syncScreenshot(
            @RequestAttribute Long deviceId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("timestamp") Long timestamp,
            @RequestParam(value = "windowTitle", required = false) String windowTitle,
            @RequestParam(value = "processName", required = false) String processName,
            HttpServletRequest request) {
        
        log.info("Syncing screenshot for device {}", deviceId);
        SyncResponse response = dataSyncService.saveScreenshot(deviceId, file, timestamp, windowTitle, processName);
        return ResponseEntity.ok(response);
    }

    /**
     * Sync multiple screenshots (batch)
     */
    @PostMapping("/screenshots")
    public ResponseEntity<SyncResponse> syncScreenshots(
            @RequestAttribute Long deviceId,
            @Valid @RequestBody List<ScreenshotSyncRequest> screenshots,
            HttpServletRequest request) {
        
        log.info("Syncing {} screenshots for device {}", screenshots.size(), deviceId);
        SyncResponse response = dataSyncService.saveScreenshots(deviceId, screenshots);
        return ResponseEntity.ok(response);
    }

    /**
     * Sync browser activities
     */
    @PostMapping("/browser-activities")
    public ResponseEntity<SyncResponse> syncBrowserActivities(
            @RequestAttribute Long deviceId,
            @Valid @RequestBody List<DataSyncService.BrowserActivitySyncRequest> activities,
            HttpServletRequest request) {
        
        log.info("Syncing {} browser activities for device {}", activities.size(), deviceId);
        SyncResponse response = dataSyncService.saveBrowserActivities(deviceId, activities);
        return ResponseEntity.ok(response);
    }

    /**
     * Sync AFK sessions
     */
    @PostMapping("/afk-sessions")
    public ResponseEntity<SyncResponse> syncAfkSessions(
            @RequestAttribute Long deviceId,
            @Valid @RequestBody List<AfkSessionSyncRequest> sessions,
            HttpServletRequest request) {
        
        log.info("Syncing {} AFK sessions for device {}", sessions.size(), deviceId);
        SyncResponse response = dataSyncService.saveAfkSessions(deviceId, sessions);
        return ResponseEntity.ok(response);
    }
}