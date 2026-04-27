// src/main/java/com/trackeye/controller/ScreenshotController.java
package com.roze.trackeyecentral.controller;


import com.roze.trackeyecentral.model.Device;
import com.roze.trackeyecentral.model.EmployeeScreenshot;
import com.roze.trackeyecentral.repository.DeviceRepository;
import com.roze.trackeyecentral.repository.ScreenshotRepository;
import com.roze.trackeyecentral.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/screenshots")
@RequiredArgsConstructor
public class ScreenshotController {

    private final ScreenshotRepository screenshotRepository;
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    
    @Value("${trackeye.storage.screenshots:./data/screenshots}")
    private String screenshotStoragePath;

    /**
     * Get screenshots for a specific user and date
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ScreenshotResponse>> getUserScreenshots(
            @RequestAttribute Long organizationId,
            @PathVariable Long userId,
            @RequestParam String date) {
        
        try {
            // Verify user belongs to organization
            var user = userRepository.findById(userId).orElse(null);
            if (user == null || !user.getOrganizationId().equals(organizationId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // Get user's devices
            var devices = deviceRepository.findByUserId(userId);
            var deviceIds = devices.stream().map(Device::getId).collect(Collectors.toList());
            
            // Parse date
            LocalDate localDate = LocalDate.parse(date);
            long startOfDay = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long endOfDay = localDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            
            // Get screenshots
            var screenshots = screenshotRepository.findByDeviceIdsAndTimeRange(deviceIds, startOfDay, endOfDay);
            
            var response = screenshots.stream()
                .map(s -> new ScreenshotResponse(s))
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting user screenshots: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get screenshots for the organization (admin view)
     */
    @GetMapping("/organization")
    public ResponseEntity<Map<String, Object>> getOrganizationScreenshots(
            @RequestAttribute Long organizationId,
            @RequestParam String date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        try {
            LocalDate localDate = LocalDate.parse(date);
            long startOfDay = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long endOfDay = localDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            
            // Get all screenshots for organization
            var screenshots = screenshotRepository.getOrganizationScreenshots(
                organizationId, startOfDay, endOfDay, 
                org.springframework.data.domain.PageRequest.of(page, size));
            
            // Group by user
            Map<Long, List<ScreenshotResponse>> groupedByUser = screenshots.stream()
                .collect(Collectors.groupingBy(
                    s -> {
                        var device = deviceRepository.findById(s.getDeviceId()).orElse(null);
                        return device != null ? device.getUserId() : null;
                    },
                    Collectors.mapping(ScreenshotResponse::new, Collectors.toList())
                ));
            
            // Get user details
            Map<Long, String> userNames = new HashMap<>();
            for (Long userId : groupedByUser.keySet()) {
                if (userId != null) {
                    userRepository.findById(userId).ifPresent(u -> 
                        userNames.put(userId, u.getFullName()));
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("date", date);
            response.put("screenshots", groupedByUser);
            response.put("userNames", userNames);
            response.put("totalCount", screenshots.size());
            response.put("page", page);
            response.put("size", size);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting organization screenshots: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get screenshot image file
     */
    @GetMapping("/image")
    public ResponseEntity<Resource> getScreenshotImage(@RequestParam String path) {
        try {
            // Security: Verify path is within storage directory
            Path requestedPath = Paths.get(path).normalize();
            Path storagePath = Paths.get(screenshotStoragePath).normalize();
            
            if (!requestedPath.startsWith(storagePath)) {
                log.warn("Attempted to access file outside storage: {}", path);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            File file = requestedPath.toFile();
            if (!file.exists() || !file.canRead()) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(file);
            
            // Determine content type
            String contentType = determineContentType(path);
            
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
                
        } catch (Exception e) {
            log.error("Error serving screenshot: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get screenshot thumbnail
     */
    @GetMapping("/thumbnail")
    public ResponseEntity<Resource> getScreenshotThumbnail(@RequestParam String path) {
        // For production, you would generate and serve thumbnails
        // This is a simplified version
        return getScreenshotImage(path);
    }

    /**
     * Get screenshot metadata
     */
    @GetMapping("/{screenshotId}")
    public ResponseEntity<ScreenshotResponse> getScreenshotMetadata(
            @RequestAttribute Long organizationId,
            @PathVariable Long screenshotId) {
        
        try {
            var screenshot = screenshotRepository.findById(screenshotId).orElse(null);
            if (screenshot == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Verify access
            var device = deviceRepository.findById(screenshot.getDeviceId()).orElse(null);
            if (device == null) {
                return ResponseEntity.notFound().build();
            }
            
            var user = userRepository.findById(device.getUserId()).orElse(null);
            if (user == null || !user.getOrganizationId().equals(organizationId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            return ResponseEntity.ok(new ScreenshotResponse(screenshot));
            
        } catch (Exception e) {
            log.error("Error getting screenshot metadata: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get screenshot statistics for a user
     */
    @GetMapping("/stats/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserScreenshotStats(
            @RequestAttribute Long organizationId,
            @PathVariable Long userId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        
        try {
            // Verify user belongs to organization
            var user = userRepository.findById(userId).orElse(null);
            if (user == null || !user.getOrganizationId().equals(organizationId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // Get user's devices
            var devices = deviceRepository.findByUserId(userId);
            var deviceIds = devices.stream().map(Device::getId).collect(Collectors.toList());
            
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            long startTime = start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long endTime = end.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            
            var screenshots = screenshotRepository.findByDeviceIdsAndTimeRange(deviceIds, startTime, endTime);
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("userId", userId);
            stats.put("userName", user.getFullName());
            stats.put("startDate", startDate);
            stats.put("endDate", endDate);
            stats.put("totalScreenshots", screenshots.size());
            stats.put("averagePerDay", screenshots.size() / (double) (end.toEpochDay() - start.toEpochDay() + 1));
            
            // Group by date
            Map<String, Long> screenshotsByDate = screenshots.stream()
                .collect(Collectors.groupingBy(
                    s -> LocalDate.ofEpochDay(s.getTimestamp() / 86400000).toString(),
                    Collectors.counting()
                ));
            
            stats.put("screenshotsByDate", screenshotsByDate);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Error getting screenshot stats: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String determineContentType(String path) {
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (path.endsWith(".png")) {
            return "image/png";
        } else if (path.endsWith(".gif")) {
            return "image/gif";
        }
        return "application/octet-stream";
    }

    /**
     * Inner class for screenshot response
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ScreenshotResponse {
        private Long id;
        private String screenshotUrl;
        private Long timestamp;
        private String windowTitle;
        private String processName;
        private String userFullName;
        private String deviceName;
        
        public ScreenshotResponse(EmployeeScreenshot screenshot) {
            this.id = screenshot.getId();
            this.screenshotUrl = screenshot.getScreenshotUrl();
            this.timestamp = screenshot.getTimestamp();
            this.windowTitle = screenshot.getWindowTitle();
            this.processName = screenshot.getProcessName();
        }
    }
}