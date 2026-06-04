package com.example.codecombat2026.proctoring.entity;

/**
 * Risk-band classification persisted on {@code proctoring_sessions.risk_band}.
 *
 * <p>The DB column is {@code varchar(8)} with a {@code CHECK} constraint
 * pinning the value set to {@code LOW / MEDIUM / HIGH}, so we map this enum
 * via {@link jakarta.persistence.EnumType#STRING} and keep the CHECK
 * constraint as the authoritative whitelist.
 *
 * <p>Band thresholds are not encoded here — they live in
 * {@code ProctoringConfig.bands} (Req 12.3) so operators can tune them
 * without a redeploy.
 */
public enum RiskBand {
    LOW,
    MEDIUM,
    HIGH
}
