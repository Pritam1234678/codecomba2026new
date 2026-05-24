package com.example.codecombat2026.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA entity for the {@code duel_eligible_problems} table introduced in
 * Flyway migration V3 (Live Duel Mode).
 *
 * <p>The primary key {@code problem_id} mirrors {@code problems.id} — there
 * is no auto-generated identifier here. Eligibility rows are inserted/removed
 * by admins via {@code DuelEligibleProblemAdminController}, and the
 * {@code added_at} timestamp is set by the application (using
 * {@code TimeUtil.now()}) on insert. The matching SQL column has a
 * {@code DEFAULT now()}, but {@code nullable = false} forces JPA to send a
 * non-null value.
 *
 * @see com.example.codecombat2026.entity.Problem
 */
@Entity
@Table(name = "duel_eligible_problems")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DuelEligibleProblem {

    /**
     * Primary key — equals {@code problems.id}. No {@code @GeneratedValue}
     * because the value is supplied by the caller (the eligible problem must
     * already exist in the {@code problems} table; the FK enforces that).
     */
    @Id
    @Column(name = "problem_id")
    private Long problemId;

    /**
     * When the problem was added to the eligible pool. Set by the application
     * via {@code TimeUtil.now()} prior to {@code save}; the SQL column also
     * has a {@code DEFAULT now()} as a defensive fallback.
     */
    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt;

    /**
     * Optional FK to {@code users.id} — the admin who added the problem to
     * the pool. Nullable so historical / system-seeded rows are allowed.
     */
    @Column(name = "added_by")
    private Long addedBy;
}
