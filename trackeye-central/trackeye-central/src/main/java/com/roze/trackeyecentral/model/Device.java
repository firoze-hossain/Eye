// src/main/java/com/trackeye/model/Device.java
package com.roze.trackeyecentral.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Entity
@Table(name = "devices")
@NoArgsConstructor
@AllArgsConstructor
public class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "device_name", nullable = false)
    private String deviceName;
    
    @Column(name = "device_identifier", unique = true, nullable = false)
    private String deviceIdentifier;
    
    @Column(name = "api_key", unique = true)
    private String apiKey;
    
    @Column(name = "api_key_created_at")
    private Long apiKeyCreatedAt;
    
    @Column(name = "os_type")
    private String osType;
    
    @Column(name = "last_seen_at")
    private Long lastSeenAt;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "created_at", nullable = false)
    private Long createdAt;
}