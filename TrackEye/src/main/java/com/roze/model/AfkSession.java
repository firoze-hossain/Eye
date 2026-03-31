package com.roze.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AfkSession {
    private Long id;
    private long startTime;
    private long endTime;
    private long durationMs;
    
    public AfkSession(long startTime, long endTime, long durationMs) {
        this(null, startTime, endTime, durationMs);
    }
}