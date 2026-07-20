// src/main/java/com/trackeye/dto/response/ScreenshotResponse.java
package com.roze.trackeyecentral.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreenshotResponse {
    private Long id;
    private String screenshotUrl;
    private Long timestamp;
    private String windowTitle;
    private String processName;
    private String userFullName;
    private String deviceName;
}