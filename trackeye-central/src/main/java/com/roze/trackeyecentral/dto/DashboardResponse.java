// src/main/java/com/trackeye/dto/response/DashboardResponse.java
package com.roze.trackeyecentral.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class DashboardResponse {
    private SummaryStats summary;
    private List<ActivitySummary> topActivities;
    private List<Map<String, Object>> recentActivity;
    private List<OnlineUser> onlineUsers;
    private ProductivityScore productivityScore;
    
    @Data
    @Builder
    public static class SummaryStats {
        private long totalActiveUsers;
        private long totalDevices;
        private long totalMinutesTracked;
        private long totalScreenshots;
        private double averageProductivity;
    }
    
    @Data
    @Builder
    public static class ActivitySummary {
        private String appName;
        private long totalMinutes;
        private int userCount;
    }
    
    @Data
    @Builder
    public static class OnlineUser {
        private Long userId;
        private String fullName;
        private String currentApp;
        private long lastActivityAt;
        private boolean isActive;
    }
    
    @Data
    @Builder
    public static class ProductivityScore {
        private double score;
        private String grade;
        private long productiveMinutes;
        private long unproductiveMinutes;
        private long neutralMinutes;
    }
}