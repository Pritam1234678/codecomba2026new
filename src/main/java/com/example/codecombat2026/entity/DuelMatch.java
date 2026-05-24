package com.example.codecombat2026.entity;

import com.example.codecombat2026.util.TimeUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for a Live Duel match.
 *
 * <p>Maps to the {@code duel_matches} table introduced by Flyway migration
 * {@code V3__live_duel_mode.sql}. The PK ({@code match_id}) is a {@link UUID}
 * assigned by the application (in {@code DuelService.pairAndStart} via
 * {@link UUID#randomUUID()}) — there is intentionally no {@code @GeneratedValue}
 * because Postgres's {@code uuid_generate_v4()} extension is not assumed.
 *
 * <p>Hibernate 6 (Spring Boot 3.5.x default) maps {@link UUID} to the native
 * Postgres {@code uuid} type and {@link LocalDateTime} to
 * {@code timestamp(6) without time zone} out of the box, which agrees with the
 * V3 column types — so no {@code @JdbcTypeCode} or {@code AttributeConverter}
 * is required for {@code spring.jpa.hibernate.ddl-auto=validate} to pass.
 *
 * <p>The {@code status} and {@code outcome} columns are {@code varchar(20)} in
 * the schema, so the {@link Enumerated#value()} mapping uses
 * {@link EnumType#STRING} with an explicit {@code length = 20} so Hibernate's
 * validate step matches the DB metadata.
 */
@Entity
@Table(name = "duel_matches")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DuelMatch {

    /**
     * Match identifier. Assigned by the application (NOT by the DB) so the
     * {@code DuelService.pairAndStart} call can return the id to both clients
     * before the INSERT round-trip completes.
     */
    @Id
    @Column(name = "match_id", nullable = false, updatable = false)
    private UUID matchId;

    /** Lower of the two participating user ids (enforced by DB CHECK {@code user_a_id < user_b_id}). */
    @Column(name = "user_a_id", nullable = false)
    private Long userAId;

    /** Higher of the two participating user ids. */
    @Column(name = "user_b_id", nullable = false)
    private Long userBId;

    /** Problem served to both participants — drawn from {@code duel_eligible_problems}. */
    @Column(name = "problem_id", nullable = false)
    private Long problemId;

    /** Lifecycle state. Transitions: {@code WAITING → IN_PROGRESS → FINISHED}. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    /** Final outcome, set together with {@link #endedAt} when the row reaches {@code FINISHED}. */
    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", length = 20)
    private Outcome outcome;

    /**
     * Winner's user id. Must equal {@link #userAId} or {@link #userBId} when
     * {@link #outcome} is {@code USER_A_WIN} / {@code USER_B_WIN}; must be
     * {@code null} for {@code DRAW} / {@code ABANDONED}. Once set, immutable
     * (enforced by the {@code trg_duel_matches_winner_immutable} trigger).
     */
    @Column(name = "winner_user_id")
    private Long winnerUserId;

    /** Set when the row transitions {@code WAITING → IN_PROGRESS}. */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /** Set when the row transitions to {@code FINISHED}. */
    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    /** Insert timestamp — populated by {@link #onCreate()} for parity with the DB {@code DEFAULT now()}. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Difficulty bucket (EASY / MEDIUM / HARD). V4 column. Drives both
     * matchmaking routing (only paired with players in the same bucket) and
     * the per-match {@link #timeLimitSec} chosen at pair time. Stored as a
     * varchar(10) plain string (rather than an enum mapping) because the
     * level value already lives as VARCHAR on the {@code problems.level}
     * column it is sourced from — the simpler comparison wins.
     */
    @Column(name = "difficulty", nullable = false, length = 10)
    private String difficulty;

    /**
     * Per-match time-limit in seconds. V4 column. Replaces the global
     * {@code DUEL_DRAW_TIMEOUT_SEC} value with a row-local field so the
     * EASY / MEDIUM / HARD windows (1200 / 2400 / 3900 s) live alongside
     * the match they describe and the recovery path can rehydrate the
     * draw timer correctly even if the env var changes between deploys.
     */
    @Column(name = "time_limit_sec", nullable = false)
    private Integer timeLimitSec;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = TimeUtil.now();
        }
    }

    /** Lifecycle states for a duel match. */
    public enum Status {
        WAITING,
        IN_PROGRESS,
        FINISHED
    }

    /** Terminal outcomes for a duel match. */
    public enum Outcome {
        USER_A_WIN,
        USER_B_WIN,
        DRAW,
        ABANDONED
    }
}
