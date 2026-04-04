package com.roze.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BrowserActivity {
    private Long id;
    private String browserName;
    private String url;
    private String pageTitle;
    private long startTime;
    private long endTime;
    private long durationMs;
    
    public BrowserActivity(String browserName, String url, String pageTitle, 
                          long startTime, long endTime, long durationMs) {
        this(null, browserName, url, pageTitle, startTime, endTime, durationMs);
    }
}