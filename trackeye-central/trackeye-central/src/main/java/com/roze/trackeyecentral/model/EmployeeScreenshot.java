// src/main/java/com/trackeye/model/EmployeeScreenshot.java
package com.roze.trackeyecentral.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Entity
@Table(name = "employee_screenshots")
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeScreenshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "device_id", nullable = false)
    private Long deviceId;
    
    @Column(name = "screenshot_url", nullable = false)
    private String screenshotUrl;
    
    @Column(name = "screenshot_hash")
    private String screenshotHash;
    
    @Column(name = "timestamp", nullable = false)
    private Long timestamp;
    
    @Column(name = "window_title")
    private String windowTitle;
    
    @Column(name = "process_name")
    private String processName;
    
    @Column(name = "synced_at", nullable = false)
    private Long syncedAt;
}