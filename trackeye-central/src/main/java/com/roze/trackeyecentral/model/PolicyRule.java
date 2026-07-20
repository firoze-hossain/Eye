package com.roze.trackeyecentral.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * A single "watch for this" rule an organization has configured, e.g.
 *   category=GAMBLING, matchType=URL_DOMAIN, pattern="bet365.com"
 *   category=GAMING,   matchType=APP_NAME,   pattern="steam"
 *
 * No rules ship pre-seeded for adult/gambling content - every org defines its
 * own list under Settings, matching what actually matters to them and avoiding
 * hard-coding a specific site blocklist into the product itself.
 */
@Data
@Entity
@Table(name = "policy_rules")
@NoArgsConstructor
@AllArgsConstructor
public class PolicyRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    // GAMBLING | ADULT | GAMING | SOCIAL_MEDIA | CUSTOM
    @Column(nullable = false)
    private String category;

    // URL_DOMAIN | URL_KEYWORD | APP_NAME | WINDOW_TITLE_KEYWORD
    @Column(name = "match_type", nullable = false)
    private String matchType;

    // Case-insensitive substring/domain to match against, e.g. "steam", "bet365.com"
    @Column(nullable = false)
    private String pattern;

    // LOW | MEDIUM | HIGH - drives how the notification is presented
    @Column(nullable = false)
    private String severity = "MEDIUM";

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @Column(name = "created_by")
    private Long createdBy;
}
