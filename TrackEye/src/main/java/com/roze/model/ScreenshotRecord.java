package com.roze.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScreenshotRecord {
    private Long id;
    private long timestamp;
    private String filePath;
    private String windowTitle;
    private String processName;
    
    public ScreenshotRecord(long timestamp, String filePath, String windowTitle, String processName) {
        this(null, timestamp, filePath, windowTitle, processName);
    }
}