// src/main/java/com/trackeye/model/AfkSession.java
package com.roze.trackeyecentral.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Entity
@Table(name = "employee_afk_sessions")
@NoArgsConstructor
@AllArgsConstructor
public class AfkSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "device_id", nullable = false)
    private Long deviceId;
    
    @Column(name = "start_time", nullable = false)
    private Long startTime;
    
    @Column(name = "end_time", nullable = false)
    private Long endTime;
    
    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;
    
    @Column(name = "synced_at", nullable = false)
    private Long syncedAt;
}