package com.example.codecombat2026.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AuditLog entity maps to the audit_logs table.
 * 
 * Records all critical actions in the Private Contest lifecycle for compliance and debugging.
 * Tracks admin actions, host actions, and participant actions with full context including
 * IP address, user agent, and detailed JSON metadata.
 * 
 * Business Rules:
 * - Logs are append-only (no updates or deletes via application layer)
 * - Minimum retention: 90 days (enforced by scheduled cleanup job)
 * - All critical private contest actions must be logged
 * 
 * Logged Events:
 * - Hosting request submitted
 * - Hosting request approved/rejected/revoked by admin
 * - Private contest created
 * - Private contest cancelled by host
 * - Private contest deleted by admin
 * - Participant joined via invite link
 * - Participant removed by host
 * - Problem added/removed from contest
 * - Invite link regenerated
 * - Contest host privileges revoked by admin
 * 
 * Requirements: 2.6, 29.1, 29.2, 29.3
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_logs_user_id", columnList = "user_id"),
    @Index(name = "idx_audit_logs_action", columnList = "action"),
    @Index(name = "idx_audit_logs_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_logs_resource", columnList = "resource_type, resource_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user who performed the action.
     * Many-to-One relationship with User entity.
     * Nullable to handle system-triggered actions (e.g., scheduled cleanup).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * The action performed (e.g., "HOSTING_REQUEST_SUBMITTED", "CONTEST_CREATED", "PARTICIPANT_JOINED").
     * Using descriptive action names for clarity in audit queries.
     * Max length: 100 characters
     */
    @Column(nullable = false, length = 100)
    private String action;

    /**
     * The type of resource affected by the action.
     * Examples: "HOSTING_REQUEST", "PRIVATE_CONTEST", "PARTICIPANT", "INVITATION", "PROBLEM"
     * Max length: 50 characters
     */
    @Column(name = "resource_type", nullable = false, length = 50)
    private String resourceType;

    /**
     * The ID of the specific resource affected (e.g., contest_id, request_id, user_id).
     * Nullable for actions that don't target a specific resource.
     */
    @Column(name = "resource_id")
    private Long resourceId;

    /**
     * Timestamp when the action was performed.
     * Automatically set on entity creation.
     */
    @Column(nullable = false)
    private LocalDateTime timestamp;

    /**
     * IP address of the user who performed the action.
     * Extracted from HttpServletRequest.
     * Max length: 45 characters (IPv6 support)
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * User agent string from the HTTP request.
     * Useful for identifying client type (browser, mobile app, API client).
     * Max length: 255 characters
     */
    @Column(name = "user_agent", length = 255)
    private String userAgent;

    /**
     * Additional details about the action in JSON format.
     * Examples:
     * - For approval: {"reason": "Verified educational institution", "previousStatus": "PENDING"}
     * - For cancellation: {"cancellationReason": "Schedule conflict", "participantCount": 35}
     * - For participant join: {"inviteToken": "abc123...", "participantEmail": "user@example.com"}
     * 
     * Stored as TEXT to accommodate large JSON objects.
     */
    @Column(name = "details_json", columnDefinition = "TEXT")
    private String detailsJson;

    /**
     * Pre-persist callback to set timestamp.
     * Ensures timestamp is always set even if not explicitly provided.
     */
    @PrePersist
    protected void onCreate() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }
}
