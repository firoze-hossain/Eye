// src/main/java/com/trackeye/model/EmployeeBrowserActivity.java
package com.roze.trackeyecentral.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Entity
@Table(name = "employee_browser_activities")
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeBrowserActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "device_id", nullable = false)
    private Long deviceId;
    
    @Column(name = "browser_name")
    private String browserName;
    
    @Column(length = 2000)
    private String url;
    
    @Column(name = "page_title")
    private String pageTitle;
    
    @Column(name = "start_time", nullable = false)
    private Long startTime;
    
    @Column(name = "end_time", nullable = false)
    private Long endTime;
    
    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;
    
    @Column(name = "synced_at", nullable = false)
    private Long syncedAt;
}