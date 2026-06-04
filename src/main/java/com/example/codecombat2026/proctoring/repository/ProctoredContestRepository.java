package com.example.codecombat2026.proctoring.repository;

import com.example.codecombat2026.proctoring.entity.ProctoredContest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link ProctoredContest}.
 *
 * <p>The presence of a row keyed by {@code contestId} is the only
 * authoritative signal that a contest runs in proctored mode (Req 1.6) —
 * the {@code Contest} entity itself stays untouched. Both finders here
 * support that derivation: {@link #existsByContestId(Long)} for the
 * cheap boolean used by {@code ContestController} to surface the
 * derived {@code proctored} flag on contest payloads, and
 * {@link #findByContestId(Long)} for admin lifecycle operations that
 * need the row itself (e.g. to read the current {@code consentVersion}).
 */
@Repository
public interface ProctoredContestRepository extends JpaRepository<ProctoredContest, Long> {

    /**
     * Look up the proctored extension row for the given contest.
     *
     * @param contestId parent {@code contests.id}
     * @return the extension row if the contest is proctored, otherwise empty
     */
    Optional<ProctoredContest> findByContestId(Long contestId);

    /**
     * Cheap existence check used to derive the {@code proctored} flag on
     * contest list / detail payloads (Req 1.6) without loading the row.
     *
     * @param contestId parent {@code contests.id}
     * @return {@code true} iff a {@code proctored_contests} row exists
     */
    boolean existsByContestId(Long contestId);

    /**
     * Batch loader for the contest-list path: returns every {@code contest_id}
     * with a proctored extension row in a single query so the caller can
     * enrich a list of {@code Contest} payloads with the derived
     * {@code proctored} flag in O(N) without an existence check per row
     * (Req 1.6, 1.7).
     *
     * <p>Callers should wrap the result in a {@code Set} for O(1) membership
     * checks while iterating contests.
     *
     * @return list of {@code contests.id} values that are proctored
     */
    @Query("SELECT pc.contestId FROM ProctoredContest pc")
    List<Long> findAllContestIds();
}
