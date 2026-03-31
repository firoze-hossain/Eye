package com.roze.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivitySession {
    private Long id;
    private String appName;
    private String windowTitle;
    private String processName;
    private long startTime;
    private long endTime;
    private long durationMs;
    
    public ActivitySession(String appName, String windowTitle, String processName, 
                          long startTime, long endTime, long durationMs) {
        this(null, appName, windowTitle, processName, startTime, endTime, durationMs);
    }
}