package com.roze.trackeyecentral.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * A notification targeted at a specific dashboard user (an admin or the
 * employee's manager). Kept generic (type + title + body + payload) so it can
 * carry policy violations today and other alert types later (idle overrun,
 * device offline, etc.) without a schema change.
 */
@Data
@Entity
@Table(name = "notifications")
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    // The dashboard user who should see this (an admin or the employee's manager).
    @Column(name = "recipient_user_id", nullable = false)
    private Long recipientUserId;

    // POLICY_VIOLATION | DEVICE_OFFLINE | SYSTEM
    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String body;

    // The employee this notification is about, if any (nullable for system-wide).
    @Column(name = "subject_user_id")
    private Long subjectUserId;

    @Column(nullable = false)
    private Boolean read = false;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;
}
