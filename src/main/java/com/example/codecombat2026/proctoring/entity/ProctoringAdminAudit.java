package com.example.codecombat2026.proctoring.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Audit row for admin-initiated actions on a proctoring session.
 *
 * <p>Maps to {@code proctoring_admin_audit} (V7). The V7 CHECK constraint
 * {@code action IN ('FORCE_END','WARNING')} stays authoritative — we
 * deliberately keep {@link #action} as a {@link String} (rather than a
 * Java enum) so the controller layer can validate the small whitelist
 * without coupling persistence to an enum type that is not used anywhere
 * else (Req 15.7, 21.5).
 */
@Entity
@Table(name = "proctoring_admin_audit")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProctoringAdminAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    /** One of {@code FORCE_END} or {@code WARNING}; enforced by the V7 CHECK constraint. */
    @Column(name = "action", nullable = false, length = 16)
    private String action;

    @Column(name = "acted_at", nullable = false)
    private LocalDateTime actedAt;

    /** Mapped to Postgres {@code text}; arbitrary length. */
    @Column(name = "reason", columnDefinition = "text")
    private String reason;
}
