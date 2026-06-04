package com.example.codecombat2026.proctoring.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * One per candidate attempt on a proctored contest.
 *
 * <p>Maps to {@code proctoring_sessions} (V7). The
 * {@code (contest_id, user_id)} unique constraint is intentional: a given
 * candidate may have at most one row per contest. Re-attempts after
 * {@code SELF_QUIT} or {@code ADMIN_FORCED} are blocked at the API layer by
 * {@code ProctoringSessionService.isLocked} (Req 13.9, 24.6).
 *
 * <p>Both enum columns are persisted as their string names — see
 * {@link RiskBand} and {@link EndReason} — and the V7 CHECK constraints
 * remain the authoritative whitelist for both. {@code length} on the
 * {@link Column} annotation matches the underlying {@code varchar} width so
 * Hibernate's {@code ddl-auto=validate} passes against V7.
 */
@Entity
@Table(
    name = "proctoring_sessions",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_proctoring_sessions_contest_user",
        columnNames = {"contest_id", "user_id"}
    )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProctoringSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "contest_id", nullable = false)
    private Long contestId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    /** Null while the session is active; set together with {@link #endReason}. */
    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    /** Null while active; one of {@link EndReason} when terminated. */
    @Enumerated(EnumType.STRING)
    @Column(name = "end_reason", length = 32)
    private EndReason endReason;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_band", nullable = false, length = 8)
    private RiskBand riskBand;

    @Column(name = "flagged", nullable = false)
    private Boolean flagged;

    /** IPv4 (max 15) or IPv6 (max 45) text representation. */
    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @Column(name = "consent_version", nullable = false)
    private Integer consentVersion;
}
