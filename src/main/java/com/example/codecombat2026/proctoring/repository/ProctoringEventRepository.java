package com.example.codecombat2026.proctoring.repository;

import com.example.codecombat2026.proctoring.entity.ProctoringEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link ProctoringEvent}.
 *
 * <p>Two access patterns are exposed:
 * <ul>
 *   <li>Replay/admin reconstruction via
 *       {@link #findBySessionIdOrderByServerTimestampAsc(Long)} — events
 *       are listed in {@code server_timestamp} order so deterministic
 *       rescoring (Req 12.7) and the admin "session timeline" view see
 *       a stable sequence even when client clocks drift.</li>
 *   <li>Retention sweeps via
 *       {@link #deleteByServerTimestampBefore(LocalDateTime)} — a bulk
 *       JPQL {@code DELETE} that bypasses entity loading and returns the
 *       affected row count for logging (Req 21.2).</li>
 * </ul>
 *
 * <p>The bulk delete is annotated with {@link Modifying} and
 * {@link Transactional} per Spring Data's contract for state-changing
 * queries; without those Hibernate refuses to execute the statement.
 * The cutoff parameter is {@link LocalDateTime} (not {@link java.time.Instant})
 * because the underlying {@code server_timestamp} column is mapped as
 * {@code LocalDateTime}; conversion from {@code Instant} happens at the
 * service boundary.
 */
@Repository
public interface ProctoringEventRepository extends JpaRepository<ProctoringEvent, Long> {

    /**
     * All events for a session, oldest first. Used by deterministic
     * rescoring (Req 12.7) and the admin timeline view.
     *
     * @param sessionId owning {@code proctoring_sessions.id}
     * @return events ordered ascending by {@code server_timestamp}
     */
    List<ProctoringEvent> findBySessionIdOrderByServerTimestampAsc(Long sessionId);

    /**
     * Bulk-delete events older than {@code cutoff}. Returns the number
     * of deleted rows so the retention job (Req 21.2) can log a useful
     * sweep summary. Implemented as a JPQL bulk update so we do not
     * load entities into the persistence context just to evict them.
     *
     * @param cutoff exclusive upper bound on {@code server_timestamp}
     * @return number of deleted rows
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM ProctoringEvent e WHERE e.serverTimestamp < :cutoff")
    int deleteByServerTimestampBefore(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Replay dedup lookup (Req 11.2, 11.3). Returns the existing event row
     * (if any) for {@code (sessionId, clientCorrelationId)}. The matching
     * partial unique index (V8 migration) guarantees at most one row per
     * pair, so the {@code findFirst…} variant is semantically equivalent to
     * {@code findBy…} but avoids an unbounded {@code LIMIT}-less scan in the
     * planner.
     *
     * @param sessionId           owning {@code proctoring_sessions.id}
     * @param clientCorrelationId UUID assigned by the browser at original
     *                            capture time
     * @return existing row, or empty when this is the first time the
     *         correlation id has been seen for the session
     */
    Optional<ProctoringEvent> findFirstBySessionIdAndClientCorrelationId(
            Long sessionId, String clientCorrelationId);
}
