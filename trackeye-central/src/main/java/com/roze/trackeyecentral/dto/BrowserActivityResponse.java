// src/main/java/com/roze/trackeyecentral/dto/BrowserActivityResponse.java
package com.roze.trackeyecentral.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrowserActivityResponse {
    private Long id;
    private String browserName;
    private String url;
    private String domain;
    private String pageTitle;
    private Long startTime;
    private Long durationMs;
}
