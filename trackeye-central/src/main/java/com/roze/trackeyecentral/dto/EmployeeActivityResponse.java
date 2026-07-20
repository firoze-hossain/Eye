// src/main/java/com/trackeye/dto/response/EmployeeActivityResponse.java
package com.roze.trackeyecentral.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeActivityResponse {
    private Long userId;
    private String userFullName;
    private String date;
    private long totalMinutes;
    private long totalActiveMinutes;
    private long totalAfkMinutes;
    private List<ActivityItem> activities;
    private List<Map<String, Object>> topApps;
    private ProductivitySummary productivity;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityItem {
        private String appName;
        private String windowTitle;
        private Long startTime;
        private Long endTime;
        private Long durationMs;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductivitySummary {
        private double productivityScore;
        private String grade;
        private long productiveMinutes;
        private long neutralMinutes;
        private long unproductiveMinutes;
    }
}