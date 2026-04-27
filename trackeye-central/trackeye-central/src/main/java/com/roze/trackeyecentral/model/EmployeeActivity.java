// src/main/java/com/trackeye/model/EmployeeActivity.java
package com.roze.trackeyecentral.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Entity
@Table(name = "employee_activities")
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "device_id", nullable = false)
    private Long deviceId;
    
    @Column(name = "app_name", nullable = false)
    private String appName;
    
    @Column(name = "window_title")
    private String windowTitle;
    
    @Column(name = "process_name")
    private String processName;
    
    @Column(name = "start_time", nullable = false)
    private Long startTime;
    
    @Column(name = "end_time", nullable = false)
    private Long endTime;
    
    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;
    
    @Column(name = "synced_at", nullable = false)
    private Long syncedAt;
}