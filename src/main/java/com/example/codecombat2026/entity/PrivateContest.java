package com.example.codecombat2026.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * PrivateContest entity maps to the private_contests table.
 * 
 * Extension table that links a Contest to its Contest_Host and hosting metadata.
 * Establishes a 1:1 relationship with the contests table via contest_id unique constraint.
 * 
 * Business Rules:
 * - Monthly Quota: 2 private contests per Contest_Host per calendar month (enforced at service layer)
 * - Participant Limit: Maximum 100 participants per private contest (enforced at service layer)
 * - Duration Limit: Maximum 5 hours (300 minutes) per contest (enforced at service layer)
 * - Cancellation: Host can cancel before contest starts (still counts toward monthly quota)
 * 
 * Requirements: 4.6, 4.7, 18.2
 */
@Entity
@Table(name = "private_contests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrivateContest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 1:1 relationship with Contest entity.
     * Each PrivateContest extends exactly one Contest row.
     * Cascade delete: if the Contest is deleted, this PrivateContest row is also deleted.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_id", nullable = false, unique = true)
    private Contest contest;

    /**
     * Many-to-One relationship with User entity representing the Contest_Host.
     * A Contest_Host (User) can create multiple PrivateContests.
     * Cascade delete: if the User is deleted, their PrivateContests are also deleted.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_user_id", nullable = false)
    private User hostUser;

    /**
     * Flag indicating whether proctoring is enabled for this private contest.
     * When true, a corresponding row should exist in the proctored_contests table.
     * Default: false
     */
    @Column(name = "enable_proctoring", nullable = false)
    private Boolean enableProctoring = false;

    /**
     * Flag indicating whether the Contest_Host has cancelled this contest.
     * Cancellation is only allowed before the contest starts (status = UPCOMING).
     * Cancelled contests still count toward the monthly quota.
     * Default: false
     */
    @Column(nullable = false)
    private Boolean cancelled = false;

    /**
     * Timestamp when the contest was cancelled by the Contest_Host.
     * Null if not cancelled.
     */
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    /**
     * Optional reason provided by the Contest_Host for cancelling the contest.
     * This reason is sent to all participants via email notification.
     */
    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    /**
     * Timestamp when this private contest was created.
     * Automatically set by the database on insert.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Pre-persist callback to set createdAt timestamp.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
