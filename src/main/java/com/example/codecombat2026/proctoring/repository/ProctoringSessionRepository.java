package com.example.codecombat2026.proctoring.repository;

import com.example.codecombat2026.proctoring.entity.EndReason;
import com.example.codecombat2026.proctoring.entity.ProctoringSession;
import com.example.codecombat2026.proctoring.entity.RiskBand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link ProctoringSession}.
 *
 * <p>Sessions are unique per {@code (contest_id, user_id)} (V7), so the
 * lookup methods below return either {@link Optional} (when uniqueness
 * is enforced) or {@link List} (when filtering by contest only). The
 * {@code endedAt IS NULL} predicate is the canonical "session is still
 * active" filter — it is used both by the candidate-side active session
 * resolver (Req 13.2) and by the admin dashboard's live grid.
 *
 * <p>{@link #existsByUserIdAndContestIdAndEndReasonIn(Long, Long, Collection)}
 * is the lockout primitive (Req 13.9, 24.6): callers pass
 * {@code Set.of(SELF_QUIT, ADMIN_FORCED)} to determine whether a
 * candidate is permanently barred from re-entering this contest's
 * proctoring flow.
 */
@Repository
public interface ProctoringSessionRepository extends JpaRepository<ProctoringSession, Long> {

    /**
     * The unique session for {@code (contest, user)}, regardless of
     * {@code endedAt} state.
     *
     * @param contestId parent {@code contests.id}
     * @param userId    candidate {@code users.id}
     * @return the row if one exists, otherwise empty
     */
    Optional<ProctoringSession> findByContestIdAndUserId(Long contestId, Long userId);

    /**
     * The active (not yet terminated) session for {@code (contest, user)}.
     * Used by the candidate-side resolver to decide whether the heartbeat
     * loop should reattach to an existing row or refuse to start one.
     *
     * @param contestId parent {@code contests.id}
     * @param userId    candidate {@code users.id}
     * @return the active row if {@code endedAt IS NULL}, otherwise empty
     */
    Optional<ProctoringSession> findByContestIdAndUserIdAndEndedAtIsNull(Long contestId, Long userId);

    /**
     * All flagged sessions for a contest — admin dashboard's "flagged
     * candidates" view (Req 21.2).
     *
     * @param contestId parent {@code contests.id}
     * @param flagged   filter on {@code flagged} column
     * @return matching sessions in no particular order
     */
    List<ProctoringSession> findByContestIdAndFlagged(Long contestId, Boolean flagged);

    /**
     * All currently-active sessions for a contest — admin dashboard's
     * live grid and the per-contest sweeps (Req 13.2, 21.3).
     *
     * @param contestId parent {@code contests.id}
     * @return sessions where {@code endedAt IS NULL}
     */
    List<ProctoringSession> findByContestIdAndEndedAtIsNull(Long contestId);

    /**
     * Every currently-active session across all contests — used by the
     * Valkey→Postgres risk-score flusher (Req 18.1, 18.2). Bounded by
     * the MVP concurrency target (≤ 100 active sessions per JVM —
     * Req 18.4) so a full table walk via this derived query is cheap;
     * if that target is ever raised the same call signature can be
     * backed by a paged variant without changing callers.
     *
     * @return sessions where {@code endedAt IS NULL}
     */
    List<ProctoringSession> findByEndedAtIsNull();

    /**
     * Lockout primitive — true iff the candidate has a previous session
     * for this contest that ended with one of the given reasons
     * (typically {@code SELF_QUIT} or {@code ADMIN_FORCED}). Drives the
     * permanent re-entry block in Req 13.9 and 24.6.
     *
     * @param userId      candidate {@code users.id}
     * @param contestId   parent {@code contests.id}
     * @param endReasons  reasons that count as a lockout
     * @return {@code true} iff a matching ended-session row exists
     */
    boolean existsByUserIdAndContestIdAndEndReasonIn(Long userId, Long contestId, Collection<EndReason> endReasons);

    /**
     * Persist a band/flagged transition computed by
     * {@code RiskScoringEngine.applyDelta} or {@code rescore}. Issued as a
     * single conditional UPDATE so concurrent paths (live event ingest,
     * admin rescore, scheduled flush) cannot corrupt the row by reading
     * then writing a stale value. The {@code flagged} parameter is set
     * by the caller to {@code (band == HIGH)} per Req 12.6 — we keep it
     * an explicit argument so the SQL stays a plain UPDATE rather than
     * a CASE expression and so callers can audit the value they wrote.
     *
     * @param id       owning {@code proctoring_sessions.id}
     * @param band     new {@link RiskBand} value
     * @param flagged  {@code true} iff {@code band == HIGH}
     * @return number of rows updated (0 if {@code id} no longer exists)
     */
    @Modifying
    @Transactional
    @Query("UPDATE ProctoringSession s SET s.riskBand = :band, s.flagged = :flagged WHERE s.id = :id")
    int updateBandAndFlag(@Param("id") Long id, @Param("band") RiskBand band, @Param("flagged") boolean flagged);

    /**
     * Persist a fully-recomputed score+band+flagged triple. Used by
     * {@code RiskScoringEngine.rescore} (Req 12.8) which replays the
     * persisted event log and rewrites the durable projection in one
     * shot. Single SQL UPDATE so the (score, band, flagged) trio cannot
     * tear in the DB.
     *
     * <p>The {@code endedAt IS NULL} predicate ensures the scheduled
     * flusher never overwrites a row that has already been terminated
     * by another path (contest sweep, force-end, etc.) — see Bug 10.
     *
     * @param id       owning {@code proctoring_sessions.id}
     * @param score    new {@code risk_score} value
     * @param band     new {@link RiskBand} value
     * @param flagged  {@code true} iff {@code band == HIGH}
     * @return number of rows updated (0 if {@code id} no longer exists
     *         or session is already ended)
     */
    @Modifying
    @Transactional
    @Query("UPDATE ProctoringSession s SET s.riskScore = :score, s.riskBand = :band, s.flagged = :flagged " +
           "WHERE s.id = :id AND s.endedAt IS NULL")
    int updateScoreBandAndFlag(@Param("id") Long id,
                               @Param("score") int score,
                               @Param("band") RiskBand band,
                               @Param("flagged") boolean flagged);

    /**
     * Conditional terminal close — stamps {@code endedAt} and
     * {@code endReason} only when the row is still active
     * ({@code endedAt IS NULL}). Mirrors the duel module's
     * {@code finalizeIfActive} pattern (see {@code DuelMatchRepository})
     * so duplicate close attempts (simultaneous force-end, racing
     * heartbeat-timeout vs self-finish, etc.) collapse to a no-op
     * rather than overwriting the original {@link EndReason}. The
     * affected row count is the authoritative "I won the close race"
     * signal — callers must inspect the return value before pushing
     * any {@code SESSION_TERMINATED} side effects.
     *
     * <p>Validates Req 13.4, 13.5, 13.6, 13.7, 13.8.
     *
     * @param id       owning {@code proctoring_sessions.id}
     * @param endedAt  termination timestamp (typically {@link LocalDateTime#now()})
     * @param reason   one of {@link EndReason}
     * @return {@code 1} iff this caller closed the session, {@code 0} if it was already closed
     */
    @Modifying
    @Transactional
    @Query("UPDATE ProctoringSession s SET s.endedAt = :endedAt, s.endReason = :reason " +
           "WHERE s.id = :id AND s.endedAt IS NULL")
    int closeIfActive(@Param("id") Long id,
                      @Param("endedAt") LocalDateTime endedAt,
                      @Param("reason") EndReason reason);

    /**
     * Bulk close every still-active session — used at JVM startup to
     * finalize sessions that were active when the previous JVM died
     * (Req 13.7 read in conjunction with the {@code @PostConstruct}
     * recovery hook on {@code ProctoringSessionService}). Stamps every
     * row where {@code endedAt IS NULL} with the supplied timestamp and
     * reason, returning the affected row count for the startup log.
     *
     * @param endedAt termination timestamp
     * @param reason  reason to record (typically {@link EndReason#HEARTBEAT_TIMEOUT})
     * @return number of rows finalized
     */
    @Modifying
    @Transactional
    @Query("UPDATE ProctoringSession s SET s.endedAt = :endedAt, s.endReason = :reason " +
           "WHERE s.endedAt IS NULL")
    int closeAllOrphans(@Param("endedAt") LocalDateTime endedAt,
                        @Param("reason") EndReason reason);

    /**
     * Atomically increment {@code resume_count} for an active session, but only
     * while it stays at or below the cap. Returns the affected row count: 1 when
     * the resume was allowed (count incremented), 0 when the session is already
     * ended or the cap has been reached. Doing the check + increment in a single
     * conditional UPDATE prevents two racing refreshes from both being allowed
     * past the cap.
     *
     * @param id     owning {@code proctoring_sessions.id}
     * @param maxResumes the maximum allowed resume_count (e.g. 2)
     * @return 1 if the resume was granted, 0 otherwise
     */
    @Modifying
    @Transactional
    @Query("UPDATE ProctoringSession s SET s.resumeCount = s.resumeCount + 1 " +
           "WHERE s.id = :id AND s.endedAt IS NULL AND s.resumeCount < :maxResumes")
    int incrementResumeIfAllowed(@Param("id") Long id, @Param("maxResumes") int maxResumes);
}
