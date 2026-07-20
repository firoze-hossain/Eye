// src/main/java/com/trackeye/model/AuditLog.java
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
@Table(name = "audit_logs")
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "organization_id")
    private Long organizationId;
    
    @Column(nullable = false)
    private String action;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "user_agent")
    private String userAgent;
    
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> details;
    
    @Column(name = "created_at", nullable = false)
    private Long createdAt;
}