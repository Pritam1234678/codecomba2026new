package com.example.codecombat2026.proctoring.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Per-{@code (user, contest, version)} consent acknowledgment.
 *
 * <p>Maps to {@code proctoring_consent_acks} (V7). The unique constraint
 * {@code (user_id, contest_id, consent_version)} makes the consent flow
 * idempotent — re-clicking "Accept" never inserts a duplicate row (Req
 * 2.5, 2.6). Captured {@link #clientIp} and {@link #userAgent} provide a
 * minimal forensics trail.
 */
@Entity
@Table(
    name = "proctoring_consent_acks",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_pca_user_contest_version",
        columnNames = {"user_id", "contest_id", "consent_version"}
    )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProctoringConsentAck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "contest_id", nullable = false)
    private Long contestId;

    @Column(name = "consent_version", nullable = false)
    private Integer consentVersion;

    @Column(name = "accepted_at", nullable = false)
    private LocalDateTime acceptedAt;

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    /** Mapped to Postgres {@code text}; arbitrary length. */
    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;
}
