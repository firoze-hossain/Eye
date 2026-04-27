// src/main/java/com/trackeye/service/ReportService.java
package com.roze.trackeyecentral.service;

import com.roze.trackeyecentral.dto.*;
import com.roze.trackeyecentral.model.Device;
import com.roze.trackeyecentral.model.EmployeeActivity;
import com.roze.trackeyecentral.model.EmployeeScreenshot;
import com.roze.trackeyecentral.model.User;
import com.roze.trackeyecentral.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ActivityRepository activityRepository;
    private final ScreenshotRepository screenshotRepository;
    private final BrowserActivityRepository browserActivityRepository;
    private final AfkSessionRepository afkSessionRepository;
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    /**
     * Get dashboard statistics for organization
     */
    @Transactional(readOnly = true)
    public DashboardResponse getDashboardStats(Long organizationId) {
        try {
            // Get all devices in organization
            List<Device> devices = deviceRepository.findByOrganizationId(organizationId);
            List<Long> deviceIds = devices.stream().map(Device::getId).collect(Collectors.toList());
            
            // Get today's timestamp range
            long startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long now = Instant.now().toEpochMilli();
            long last24Hours = now - (24 * 60 * 60 * 1000);
            
            // Calculate statistics
            long totalActiveUsers = userRepository.countActiveUsersByOrganizationId(organizationId);
            long totalDevices = deviceIds.size();
            long totalMinutesTracked = activityRepository.getTotalTimeForDevices(deviceIds, startOfDay, now) / 60000;
            long totalScreenshots = screenshotRepository.count();
            
            // Get top activities
            List<Map<String, Object>> topActivitiesRaw = activityRepository.getTopAppsForDevices(deviceIds, startOfDay, now, Pageable.ofSize(5));
            List<DashboardResponse.ActivitySummary> topActivities = new ArrayList<>();
            for (Map<String, Object> raw : topActivitiesRaw) {
                topActivities.add(DashboardResponse.ActivitySummary.builder()
                    .appName((String) raw.get("app_name"))
                    .totalMinutes(((Number) raw.get("total_ms")).longValue() / 60000)
                    .userCount(((Number) raw.get("session_count")).intValue())
                    .build());
            }
            
            // Get online users
            long activeThreshold = now - (5 * 60 * 1000); // Last 5 minutes
            List<Device> activeDevices = deviceRepository.findOnlineDevices(activeThreshold);
            List<DashboardResponse.OnlineUser> onlineUsers = new ArrayList<>();
            for (Device device : activeDevices) {
                Optional<User> userOpt = userRepository.findById(device.getUserId());
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    // Get current activity
                    List<EmployeeActivity> recentActivities = activityRepository
                        .findByDeviceIdOrderByStartTimeDesc(device.getId())
                        .stream()
                        .limit(1)
                        .collect(Collectors.toList());
                    
                    String currentApp = recentActivities.isEmpty() ? "Idle" : recentActivities.get(0).getAppName();
                    
                    onlineUsers.add(DashboardResponse.OnlineUser.builder()
                        .userId(user.getId())
                        .fullName(user.getFullName())
                        .currentApp(currentApp)
                        .lastActivityAt(device.getLastSeenAt())
                        .isActive(true)
                        .build());
                }
            }
            
            // Calculate productivity score
            long totalTime = activityRepository.getTotalTimeForDevices(deviceIds, startOfDay, now);
            long afkTime = afkSessionRepository.getTotalAfkTimeForDevices(deviceIds, startOfDay, now);
            long productiveTime = totalTime - afkTime;
            double productivityScore = totalTime > 0 ? (productiveTime * 100.0 / totalTime) : 0;
            
            String grade = calculateGrade(productivityScore);
            
            DashboardResponse.ProductivityScore productivity = DashboardResponse.ProductivityScore.builder()
                .score(productivityScore)
                .grade(grade)
                .productiveMinutes(productiveTime / 60000)
                .unproductiveMinutes(afkTime / 60000)
                .neutralMinutes(0)
                .build();
            
            // Get recent activity
            List<Map<String, Object>> recentActivityRaw = activityRepository
                .getOrganizationActivities(organizationId, last24Hours, now, 10);
            
            return DashboardResponse.builder()
                .summary(DashboardResponse.SummaryStats.builder()
                    .totalActiveUsers(totalActiveUsers)
                    .totalDevices(totalDevices)
                    .totalMinutesTracked(totalMinutesTracked)
                    .totalScreenshots(totalScreenshots)
                    .averageProductivity(productivityScore)
                    .build())
                .topActivities(topActivities)
                .recentActivity(recentActivityRaw)
                .onlineUsers(onlineUsers)
                .productivityScore(productivity)
                .build();
                
        } catch (Exception e) {
            log.error("Error getting dashboard stats: {}", e.getMessage(), e);
            return DashboardResponse.builder()
                .summary(DashboardResponse.SummaryStats.builder().build())
                .topActivities(new ArrayList<>())
                .recentActivity(new ArrayList<>())
                .onlineUsers(new ArrayList<>())
                .productivityScore(DashboardResponse.ProductivityScore.builder().build())
                .build();
        }
    }

    /**
     * Get employee activities for a specific date
     */
    @Transactional(readOnly = true)
    public EmployeeActivityResponse getEmployeeActivities(Long organizationId, Long userId, LocalDate date) {
        try {
            // Verify user belongs to organization
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (!user.getOrganizationId().equals(organizationId)) {
                throw new RuntimeException("User does not belong to this organization");
            }
            
            // Get user's devices
            List<Device> devices = deviceRepository.findByUserId(userId);
            List<Long> deviceIds = devices.stream().map(Device::getId).collect(Collectors.toList());
            
            // Get date range
            long startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            
            // Get activities
            List<EmployeeActivity> activities = activityRepository
                .findByDeviceIdsAndTimeRange(deviceIds, startOfDay, endOfDay);
            
            // Calculate totals
            long totalMinutes = 0;
            long totalActiveMinutes = 0;
            List<EmployeeActivityResponse.ActivityItem> activityItems = new ArrayList<>();
            
            for (EmployeeActivity activity : activities) {
                totalMinutes += activity.getDurationMs();
                totalActiveMinutes += activity.getDurationMs();
                
                activityItems.add(EmployeeActivityResponse.ActivityItem.builder()
                    .appName(activity.getAppName())
                    .windowTitle(activity.getWindowTitle())
                    .startTime(activity.getStartTime())
                    .endTime(activity.getEndTime())
                    .durationMs(activity.getDurationMs())
                    .build());
            }
            
            // Get top apps
            List<Map<String, Object>> topAppsRaw = activityRepository
                .getTopAppsForDevices(deviceIds, startOfDay, endOfDay, Pageable.ofSize(10));
            
            // Get AFK time
            long afkTime = afkSessionRepository.getTotalAfkTimeForDevices(deviceIds, startOfDay, endOfDay);
            
            // Calculate productivity
            double productivityScore = totalActiveMinutes > 0 ? 
                ((totalActiveMinutes - afkTime) * 100.0 / totalActiveMinutes) : 0;
            
            String grade = calculateGrade(productivityScore);
            
            EmployeeActivityResponse.ProductivitySummary productivity = 
                EmployeeActivityResponse.ProductivitySummary.builder()
                .productivityScore(productivityScore)
                .grade(grade)
                .productiveMinutes((totalActiveMinutes - afkTime) / 60000)
                .neutralMinutes(0)
                .unproductiveMinutes(afkTime / 60000)
                .build();
            
            return EmployeeActivityResponse.builder()
                .userId(userId)
                .userFullName(user.getFullName())
                .date(date.toString())
                .totalMinutes(totalMinutes / 60000)
                .totalActiveMinutes(totalActiveMinutes / 60000)
                .totalAfkMinutes(afkTime / 60000)
                .activities(activityItems)
                .topApps(topAppsRaw)
                .productivity(productivity)
                .build();
                
        } catch (Exception e) {
            log.error("Error getting employee activities: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get employee activities", e);
        }
    }

    /**
     * Get employee screenshots for a specific date
     */
    @Transactional(readOnly = true)
    public List<ScreenshotResponse> getEmployeeScreenshots(Long organizationId, Long userId, LocalDate date) {
        try {
            // Verify user belongs to organization
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (!user.getOrganizationId().equals(organizationId)) {
                throw new RuntimeException("User does not belong to this organization");
            }
            
            // Get user's devices
            List<Device> devices = deviceRepository.findByUserId(userId);
            List<Long> deviceIds = devices.stream().map(Device::getId).collect(Collectors.toList());
            
            // Get date range
            long startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            
            // Get screenshots
            List<EmployeeScreenshot> screenshots = screenshotRepository
                .findByDeviceIdsAndTimeRange(deviceIds, startOfDay, endOfDay);
            
            return screenshots.stream()
                .map(s -> ScreenshotResponse.builder()
                    .id(s.getId())
                    .screenshotUrl(s.getScreenshotUrl())
                    .timestamp(s.getTimestamp())
                    .windowTitle(s.getWindowTitle())
                    .processName(s.getProcessName())
                    .userFullName(user.getFullName())
                    .deviceName(getDeviceName(devices, s.getDeviceId()))
                    .build())
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("Error getting employee screenshots: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get real-time active users
     */
    @Transactional(readOnly = true)
    public List<LiveActivityResponse> getLiveActivities(Long organizationId) {
        try {
            // Get all devices in organization
            List<Device> devices = deviceRepository.findByOrganizationId(organizationId);
            
            long activeThreshold = System.currentTimeMillis() - (5 * 60 * 1000); // Last 5 minutes
            List<LiveActivityResponse> liveActivities = new ArrayList<>();
            
            for (Device device : devices) {
                if (device.getLastSeenAt() != null && device.getLastSeenAt() > activeThreshold) {
                    Optional<User> userOpt = userRepository.findById(device.getUserId());
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        
                        // Get current activity
                        List<EmployeeActivity> recentActivities = activityRepository
                            .findByDeviceIdOrderByStartTimeDesc(device.getId())
                            .stream()
                            .limit(1)
                            .collect(Collectors.toList());
                        
                        String currentApp = recentActivities.isEmpty() ? "Idle" : recentActivities.get(0).getAppName();
                        String currentWindow = recentActivities.isEmpty() ? "" : recentActivities.get(0).getWindowTitle();
                        
                        // Get latest screenshot
                        List<EmployeeScreenshot> recentScreenshots = screenshotRepository
                            .findByDeviceIdOrderByTimestampDesc(device.getId())
                            .stream()
                            .limit(1)
                            .collect(Collectors.toList());
                        
                        String lastScreenshotUrl = recentScreenshots.isEmpty() ? null : recentScreenshots.get(0).getScreenshotUrl();
                        
                        long idleTime = System.currentTimeMillis() - device.getLastSeenAt();
                        
                        liveActivities.add(LiveActivityResponse.builder()
                            .userId(user.getId())
                            .userFullName(user.getFullName())
                            .deviceName(device.getDeviceName())
                            .currentApp(currentApp)
                            .currentWindowTitle(currentWindow)
                            .lastActivityAt(device.getLastSeenAt())
                            .isOnline(true)
                            .idleTimeMs(idleTime)
                            .lastScreenshotUrl(lastScreenshotUrl)
                            .build());
                    }
                }
            }
            
            return liveActivities;
            
        } catch (Exception e) {
            log.error("Error getting live activities: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get weekly report
     */
    @Transactional(readOnly = true)
    public WeeklyReportResponse getWeeklyReport(Long organizationId, Long userId) {
        try {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(6);
            
            long startTime = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long endTime = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            
            List<Long> deviceIds;
            String userFullName = null;
            
            if (userId != null) {
                // Get specific user
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
                
                if (!user.getOrganizationId().equals(organizationId)) {
                    throw new RuntimeException("User does not belong to this organization");
                }
                
                userFullName = user.getFullName();
                List<Device> devices = deviceRepository.findByUserId(userId);
                deviceIds = devices.stream().map(Device::getId).collect(Collectors.toList());
            } else {
                // Get all organization devices
                List<Device> devices = deviceRepository.findByOrganizationId(organizationId);
                deviceIds = devices.stream().map(Device::getId).collect(Collectors.toList());
            }
            
            // Get daily summaries
            List<WeeklyReportResponse.DailySummary> dailySummaries = new ArrayList<>();
            long totalMinutes = 0;
            long totalProductiveMinutes = 0;
            long totalAfkMinutes = 0;
            int totalScreenshots = 0;
            
            for (int i = 0; i < 7; i++) {
                LocalDate currentDate = startDate.plusDays(i);
                long dayStart = currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                long dayEnd = currentDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                
                long dayTotal = activityRepository.getTotalTimeForDevices(deviceIds, dayStart, dayEnd);
                long dayAfk = afkSessionRepository.getTotalAfkTimeForDevices(deviceIds, dayStart, dayEnd);
                long dayProductive = dayTotal - dayAfk;
                
                int screenshotCount = screenshotRepository
                    .findByDeviceIdsAndTimeRange(deviceIds, dayStart, dayEnd).size();
                
                double productivityScore = dayTotal > 0 ? (dayProductive * 100.0 / dayTotal) : 0;
                
                dailySummaries.add(WeeklyReportResponse.DailySummary.builder()
                    .date(currentDate.toString())
                    .totalMinutes(dayTotal / 60000)
                    .productiveMinutes(dayProductive / 60000)
                    .screenshotCount(screenshotCount)
                    .productivityScore(productivityScore)
                    .build());
                
                totalMinutes += dayTotal;
                totalProductiveMinutes += dayProductive;
                totalAfkMinutes += dayAfk;
                totalScreenshots += screenshotCount;
            }
            
            // Get top activities for the week
            List<Map<String, Object>> topActivities = activityRepository
                .getTopAppsForDevices(deviceIds, startTime, endTime, Pageable.ofSize(10));
            
            double averageProductivity = totalMinutes > 0 ? (totalProductiveMinutes * 100.0 / totalMinutes) : 0;
            
            WeeklyReportResponse.WeeklyTotals totals = WeeklyReportResponse.WeeklyTotals.builder()
                .totalMinutes(totalMinutes / 60000)
                .productiveMinutes(totalProductiveMinutes / 60000)
                .afkMinutes(totalAfkMinutes / 60000)
                .totalScreenshots(totalScreenshots)
                .averageProductivity(averageProductivity)
                .build();
            
            return WeeklyReportResponse.builder()
                .startDate(startDate.toString())
                .endDate(endDate.toString())
                .userId(userId)
                .userFullName(userFullName)
                .dailySummaries(dailySummaries)
                .totals(totals)
                .topActivities(topActivities)
                .build();
                
        } catch (Exception e) {
            log.error("Error getting weekly report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get weekly report", e);
        }
    }
    
    /**
     * Get organization overview report
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getOrganizationOverview(Long organizationId, LocalDate startDate, LocalDate endDate) {
        try {
            long startTime = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long endTime = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            
            // Get all devices in organization
            List<Device> devices = deviceRepository.findByOrganizationId(organizationId);
            List<Long> deviceIds = devices.stream().map(Device::getId).collect(Collectors.toList());
            
            // Get users
            List<User> users = userRepository.findByOrganizationId(organizationId);
            
            Map<String, Object> report = new HashMap<>();
            report.put("organizationId", organizationId);
            report.put("startDate", startDate.toString());
            report.put("endDate", endDate.toString());
            report.put("totalUsers", users.size());
            report.put("activeUsers", users.stream().filter(u -> "active".equals(u.getStatus())).count());
            report.put("totalDevices", devices.size());
            report.put("totalTimeMs", activityRepository.getTotalTimeForDevices(deviceIds, startTime, endTime));
            report.put("totalAfkTimeMs", afkSessionRepository.getTotalAfkTimeForDevices(deviceIds, startTime, endTime));
            report.put("totalScreenshots", screenshotRepository.findByDeviceIdsAndTimeRange(deviceIds, startTime, endTime).size());
            
            // User-wise breakdown
            List<Map<String, Object>> userBreakdown = new ArrayList<>();
            for (User user : users) {
                List<Device> userDevices = deviceRepository.findByUserId(user.getId());
                List<Long> userDeviceIds = userDevices.stream().map(Device::getId).collect(Collectors.toList());
                
                long userTotalTime = activityRepository.getTotalTimeForDevices(userDeviceIds, startTime, endTime);
                long userAfkTime = afkSessionRepository.getTotalAfkTimeForDevices(userDeviceIds, startTime, endTime);
                
                Map<String, Object> userStats = new HashMap<>();
                userStats.put("userId", user.getId());
                userStats.put("fullName", user.getFullName());
                userStats.put("email", user.getEmail());
                userStats.put("totalTimeMs", userTotalTime);
                userStats.put("totalHours", userTotalTime / 3600000.0);
                userStats.put("afkTimeMs", userAfkTime);
                userStats.put("productivity", userTotalTime > 0 ? ((userTotalTime - userAfkTime) * 100.0 / userTotalTime) : 0);
                userBreakdown.add(userStats);
            }
            
            report.put("userBreakdown", userBreakdown);
            
            return report;
            
        } catch (Exception e) {
            log.error("Error getting organization overview: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get organization overview", e);
        }
    }
    
    private String calculateGrade(double score) {
        if (score >= 90) return "A+";
        if (score >= 80) return "A";
        if (score >= 70) return "B";
        if (score >= 60) return "C";
        if (score >= 50) return "D";
        return "F";
    }
    
    private String getDeviceName(List<Device> devices, Long deviceId) {
        return devices.stream()
            .filter(d -> d.getId().equals(deviceId))
            .findFirst()
            .map(Device::getDeviceName)
            .orElse("Unknown");
    }
}