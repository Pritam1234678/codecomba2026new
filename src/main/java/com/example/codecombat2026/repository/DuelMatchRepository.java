package com.example.codecombat2026.repository;

import com.example.codecombat2026.entity.DuelMatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link DuelMatch}.
 *
 * <p>Combines derived-method queries with a small set of hand-written JPQL
 * queries that back the conditional finalize / cancel / start UPDATEs used by
 * {@code DuelService}. Every mutating query is gated on the current
 * {@code status} (and where applicable {@code winner_user_id IS NULL}) so the
 * UPDATE is idempotent across racing AC submissions, draw timer, reconnect
 * timer, forfeit, and admin-cancel paths — the row count returned by each
 * {@code @Modifying} method is the authoritative "I won the race" signal that
 * the caller must check (Requirements 9.2, 9.3, 9.4).
 *
 * <p>Defense in depth: even if these UPDATEs are issued out of order, the
 * V3 migration's {@code trg_duel_matches_winner_immutable} trigger and the
 * partial-unique-active-match indexes make any incorrect rewrite of a
 * already-decided row impossible at the database level.
 */
@Repository
public interface DuelMatchRepository extends JpaRepository<DuelMatch, UUID> {

    /**
     * Return all currently-active matches (WAITING or IN_PROGRESS) involving
     * the given user. The partial unique indexes
     * {@code ux_duel_active_user_a} and {@code ux_duel_active_user_b}
     * guarantee at most one row, so callers typically use
     * {@code list.stream().findFirst()}.
     *
     * <p>Returning {@code List} (rather than {@code Optional}) keeps the JPQL
     * portable and avoids the {@code NonUniqueResultException} surface area —
     * the partial-unique-index invariant remains the source of truth.
     */
    @Query("SELECT m FROM DuelMatch m " +
           "WHERE (m.userAId = :u OR m.userBId = :u) " +
           "AND m.status IN (com.example.codecombat2026.entity.DuelMatch.Status.WAITING, " +
           "                 com.example.codecombat2026.entity.DuelMatch.Status.IN_PROGRESS)")
    List<DuelMatch> findActiveByUser(@Param("u") Long userId);

    /**
     * Return every match still in {@code IN_PROGRESS}. Used by the JVM-restart
     * recovery path in {@code DuelService} which scans this list on
     * {@code @PostConstruct} and either re-attaches a draw timer or finalizes
     * the row as {@code ABANDONED}.
     */
    @Query("SELECT m FROM DuelMatch m " +
           "WHERE m.status = com.example.codecombat2026.entity.DuelMatch.Status.IN_PROGRESS")
    List<DuelMatch> findAllInProgress();

    /**
     * Conditional finalize: stamp winner / outcome / FINISHED only if the row
     * is still {@code IN_PROGRESS} with no winner yet. Returns the affected
     * row count: 1 means this caller won the win-claim race; 0 means another
     * path (racing AC, draw timer, forfeit, admin-cancel) finalized first and
     * the caller MUST re-read the row instead of overwriting.
     */
    @Modifying
    @Transactional
    @Query("UPDATE DuelMatch m " +
           "SET m.winnerUserId = :winner, " +
           "    m.outcome = :outcome, " +
           "    m.status = com.example.codecombat2026.entity.DuelMatch.Status.FINISHED, " +
           "    m.endedAt = :endedAt " +
           "WHERE m.matchId = :matchId " +
           "AND m.status = com.example.codecombat2026.entity.DuelMatch.Status.IN_PROGRESS " +
           "AND m.winnerUserId IS NULL")
    int finalizeIfActive(@Param("matchId") UUID matchId,
                         @Param("winner") Long winnerUserId,
                         @Param("outcome") DuelMatch.Outcome outcome,
                         @Param("endedAt") LocalDateTime endedAt);

    /**
     * Admin-cancel path: stamp {@code ABANDONED} with a NULL winner only if
     * the match is still active. Same semantics as {@link #finalizeIfActive}:
     * 1 = this caller closed the match, 0 = already finalized by another path.
     */
    @Modifying
    @Transactional
    @Query("UPDATE DuelMatch m " +
           "SET m.outcome = com.example.codecombat2026.entity.DuelMatch.Outcome.ABANDONED, " +
           "    m.status = com.example.codecombat2026.entity.DuelMatch.Status.FINISHED, " +
           "    m.endedAt = :endedAt " +
           "WHERE m.matchId = :matchId " +
           "AND m.status = com.example.codecombat2026.entity.DuelMatch.Status.IN_PROGRESS " +
           "AND m.winnerUserId IS NULL")
    int adminCancelIfActive(@Param("matchId") UUID matchId,
                            @Param("endedAt") LocalDateTime endedAt);

    /**
     * Draw-finalize path: stamp {@code DRAW} with a NULL winner only if the
     * match is still active. Used by the 600-second draw timer (Requirement
     * 6.6) and by the dual-disconnect combined timer (Requirement 7.4).
     * Returns 1 iff this caller closed the match, 0 if another path
     * (racing AC, forfeit, admin-cancel) finalized first.
     */
    @Modifying
    @Transactional
    @Query("UPDATE DuelMatch m " +
           "SET m.outcome = com.example.codecombat2026.entity.DuelMatch.Outcome.DRAW, " +
           "    m.status = com.example.codecombat2026.entity.DuelMatch.Status.FINISHED, " +
           "    m.endedAt = :endedAt " +
           "WHERE m.matchId = :matchId " +
           "AND m.status = com.example.codecombat2026.entity.DuelMatch.Status.IN_PROGRESS " +
           "AND m.winnerUserId IS NULL")
    int finalizeAsDrawIfActive(@Param("matchId") UUID matchId,
                               @Param("endedAt") LocalDateTime endedAt);

    /**
     * Status transition {@code WAITING → IN_PROGRESS}: stamps {@code startedAt}
     * only if the row is still {@code WAITING}. Returns 1 iff this caller
     * performed the transition.
     */
    @Modifying
    @Transactional
    @Query("UPDATE DuelMatch m " +
           "SET m.status = com.example.codecombat2026.entity.DuelMatch.Status.IN_PROGRESS, " +
           "    m.startedAt = :startedAt " +
           "WHERE m.matchId = :matchId " +
           "AND m.status = com.example.codecombat2026.entity.DuelMatch.Status.WAITING")
    int startIfWaiting(@Param("matchId") UUID matchId,
                       @Param("startedAt") LocalDateTime startedAt);

    /** Paged listing for the admin {@code /api/admin/duels} endpoint. */
    Page<DuelMatch> findByStatus(DuelMatch.Status status, Pageable pageable);

    /** Live-count metric — used by the admin dashboard for "active duels". */
    long countByStatusIn(Collection<DuelMatch.Status> statuses);

    /**
     * Count of duels finished at or after the given instant (typically the
     * start of the current day in server local time). Backs the daily
     * "duels finished today" metric.
     */
    @Query("SELECT count(m) FROM DuelMatch m " +
           "WHERE m.status = com.example.codecombat2026.entity.DuelMatch.Status.FINISHED " +
           "AND m.endedAt >= :startOfDay")
    long countFinishedSince(@Param("startOfDay") LocalDateTime startOfDay);

    /**
     * Count of duels abandoned at or after the given instant. Backs the daily
     * "duels abandoned today" metric.
     */
    @Query("SELECT count(m) FROM DuelMatch m " +
           "WHERE m.outcome = com.example.codecombat2026.entity.DuelMatch.Outcome.ABANDONED " +
           "AND m.endedAt >= :startOfDay")
    long countAbandonedSince(@Param("startOfDay") LocalDateTime startOfDay);

    /**
     * Recent duel history for a given user — both finished (most recent
     * first by {@code endedAt}) so the lobby can render their last N
     * matches. We include only FINISHED rows because in-progress matches
     * are surfaced separately via the {@code findActiveByUser} query.
     */
    @Query("SELECT m FROM DuelMatch m " +
           "WHERE (m.userAId = :userId OR m.userBId = :userId) " +
           "AND m.status = com.example.codecombat2026.entity.DuelMatch.Status.FINISHED " +
           "ORDER BY m.endedAt DESC NULLS LAST")
    List<DuelMatch> findFinishedByUser(@Param("userId") Long userId, Pageable pageable);
}
