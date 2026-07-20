package com.roze.trackeyecentral.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Entity
@Table(name = "policy_violations")
@NoArgsConstructor
@AllArgsConstructor
public class PolicyViolation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String severity;

    // The actual app name / URL / window title that matched, for context.
    @Column(name = "matched_value", length = 1000)
    private String matchedValue;

    @Column(name = "occurred_at", nullable = false)
    private Long occurredAt;
}
