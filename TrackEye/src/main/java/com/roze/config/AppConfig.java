package com.roze.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "trackeye")
public class AppConfig {
    private String storagePath;
    private int idleTimeoutSeconds = 180;
    private int screenshotIntervalSeconds = 300;
    private boolean screenshotOnAppSwitch = true;
    private long pollIntervalMs = 1000;
    
    public String getStoragePath() {
        if (storagePath == null) {
            storagePath = System.getProperty("user.home") + "/TrackEyeData";
        }
        return storagePath;
    }
}