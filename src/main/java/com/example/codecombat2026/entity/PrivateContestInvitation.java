package com.example.codecombat2026.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing a unique, time-limited invite token for a private contest.
 * 
 * Mapped to the private_contest_invitations table.
 * 
 * Requirements: 4.8, 5.1, 5.2, 5.3
 */
@Entity
@Table(name = "private_contest_invitations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrivateContestInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The contest this invitation belongs to.
     * References contests table (not private_contests) for direct integration.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_id", nullable = false)
    private Contest contest;

    /**
     * Cryptographically random token (32 bytes, base64url-encoded).
     * Must be unique across all invitations.
     */
    @Column(nullable = false, unique = true, length = 64)
    private String token;

    /**
     * Timestamp when the invitation was created.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Timestamp when the invitation expires.
     * Default: 30 days from creation (set by application logic).
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Flag indicating if the token has been invalidated (e.g., when regenerated).
     * Invalidated tokens cannot be used to join contests.
     */
    @Column(nullable = false)
    private Boolean invalidated = false;
}
