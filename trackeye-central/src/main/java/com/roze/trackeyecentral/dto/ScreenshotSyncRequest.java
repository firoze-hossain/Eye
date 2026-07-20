// src/main/java/com/trackeye/dto/request/ScreenshotSyncRequest.java
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
public class ScreenshotSyncRequest {
    
    @NotBlank(message = "Screenshot URL is required")
    private String screenshotUrl;
    
    @NotNull(message = "Timestamp is required")
    @Min(value = 0, message = "Timestamp must be positive")
    private Long timestamp;
    
    private String windowTitle;
    
    private String processName;
    
    private String screenshotHash;
}