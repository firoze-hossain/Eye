// src/main/java/com/trackeye/dto/response/LiveActivityResponse.java
package com.roze.trackeyecentral.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveActivityResponse {
    private Long userId;
    private String userFullName;
    private String deviceName;
    private String currentApp;
    private String currentWindowTitle;
    private Long lastActivityAt;
    private boolean isOnline;
    private long idleTimeMs;
    private String lastScreenshotUrl;
}