// src/main/java/com/trackeye/dto/request/ActivitySyncRequest.java
package com.roze.trackeyecentral.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivitySyncRequest {
    
    @NotBlank(message = "App name is required")
    private String appName;
    
    private String windowTitle;
    
    private String processName;
    
    @NotNull(message = "Start time is required")
    @Min(value = 0, message = "Start time must be positive")
    private Long startTime;
    
    @NotNull(message = "End time is required")
    @Min(value = 0, message = "End time must be positive")
    private Long endTime;
    
    @NotNull(message = "Duration is required")
    @Min(value = 0, message = "Duration must be positive")
    private Long durationMs;
}