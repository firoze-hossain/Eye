// src/main/java/com/trackeye/dto/response/WeeklyReportResponse.java
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
public class WeeklyReportResponse {
    private String startDate;
    private String endDate;
    private Long userId;
    private String userFullName;
    private List<DailySummary> dailySummaries;
    private WeeklyTotals totals;
    private List<Map<String, Object>> topActivities;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailySummary {
        private String date;
        private long totalMinutes;
        private long productiveMinutes;
        private int screenshotCount;
        private double productivityScore;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeeklyTotals {
        private long totalMinutes;
        private long productiveMinutes;
        private long afkMinutes;
        private int totalScreenshots;
        private double averageProductivity;
    }
}