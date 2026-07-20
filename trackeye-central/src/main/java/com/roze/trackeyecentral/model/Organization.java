// src/main/java/com/trackeye/model/Organization.java
package com.roze.trackeyecentral.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Data
@Entity
@Table(name = "organizations")
@NoArgsConstructor
@AllArgsConstructor
public class Organization {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String orgName;
    
    @Column(unique = true, nullable = false)
    private String subdomain;
    
    @Column(name = "plan_type")
    private String planType = "basic";
    
    @Column(name = "max_users")
    private Integer maxUsers = 5;
    
    @Column(name = "data_retention_days")
    private Integer dataRetentionDays = 30;
    
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> settings;
    
    @Column(name = "created_at", nullable = false)
    private Long createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
}