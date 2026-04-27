// src/main/java/com/trackeye/model/User.java
package com.roze.trackeyecentral.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;
    
    @Column(nullable = false)
    private String email;
    
    @Column(name = "password_hash")
    private String passwordHash;
    
    @Column(name = "full_name", nullable = false)
    private String fullName;
    
    @Column(nullable = false)
    private String role = "employee";
    
    @Column(nullable = false)
    private String status = "active";
    
    @Column(name = "created_at", nullable = false)
    private Long createdAt;
    
    @Column(name = "last_login_at")
    private Long lastLoginAt;
    
    @Column(name = "invited_by")
    private Long invitedBy;
}