package com.example.codecombat2026.repository;

import com.example.codecombat2026.entity.DuelEligibleProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link DuelEligibleProblem}.
 *
 * <p>Backs the curated pool of problems that may be served in a Live Duel.
 * The two custom queries below power
 * {@code DuelService.pickProblem(userA, userB)}: the primary path returns
 * eligible problems excluding those already solved by <strong>both</strong>
 * users, with {@link #findAllProblemIds()} as the documented fallback when
 * the exclusion empties the candidate set (Requirement 3.5).
 *
 * <p>Standard {@link JpaRepository} methods provide
 * {@code existsById}, {@code deleteById}, {@code findAll}, and {@code save}
 * for the admin CRUD surface; they are not redeclared here.
 *
 * @see com.example.codecombat2026.entity.DuelEligibleProblem
 */
@Repository
public interface DuelEligibleProblemRepository extends JpaRepository<DuelEligibleProblem, Long> {

    /**
     * Returns the {@code problem_id}s of every eligible problem that is
     * <strong>not</strong> jointly solved by both {@code userA} and
     * {@code userB} (Requirement 3.2 / 3.3).
     *
     * <p>A problem counts as "jointly solved" when there exists a row in
     * {@code user_problem_solved} for {@code userA} <em>and</em> a row for
     * {@code userB} on the same {@code problem_id}. Problems solved by only
     * one of the two users remain eligible — the duel is more interesting
     * when one participant has prior experience.
     *
     * <p>Implemented as a native query against the existing
     * {@code user_problem_solved} join table because no JPA association is
     * declared between {@link DuelEligibleProblem} and
     * {@code UserProblemSolved}.
     *
     * @param userA first participant's id
     * @param userB second participant's id
     * @return problem ids in {@code duel_eligible_problems} not solved by
     *         both users; may be empty (caller should fall back to
     *         {@link #findAllProblemIds()})
     */
    @Query(
        value = "SELECT dep.problem_id FROM duel_eligible_problems dep " +
                "WHERE dep.problem_id NOT IN ( " +
                "    SELECT s1.problem_id FROM user_problem_solved s1 " +
                "    INNER JOIN user_problem_solved s2 ON s1.problem_id = s2.problem_id " +
                "    WHERE s1.user_id = :userA AND s2.user_id = :userB " +
                ")",
        nativeQuery = true
    )
    List<Long> findEligibleNotBothSolved(Long userA, Long userB);

    /**
     * Returns every {@code problem_id} in {@code duel_eligible_problems}.
     *
     * <p>Fallback used by {@code DuelService.pickProblem} when
     * {@link #findEligibleNotBothSolved(Long, Long)} returns an empty list
     * (Requirement 3.5). The caller logs a warning and picks uniformly from
     * this set so the duel can still start.
     *
     * @return all eligible problem ids; empty only if the pool itself is empty
     */
    @Query(
        value = "SELECT problem_id FROM duel_eligible_problems",
        nativeQuery = true
    )
    List<Long> findAllProblemIds();

    /**
     * Returns the {@code problem_id}s of every eligible problem whose
     * {@code problems.level} matches {@code :level}. Used by the V4
     * difficulty-bucketed matchmaker so EASY/MEDIUM/HARD queues each
     * draw from the right slice of the curated pool.
     *
     * <p>No "both-solved" exclusion — the user explicitly dropped that
     * gate (see V4 rework spec). Random selection happens caller-side.
     *
     * @param level uppercase difficulty string ('EASY' / 'MEDIUM' / 'HARD')
     * @return all eligible problem ids of that level; may be empty
     */
    @Query(
        value = "SELECT dep.problem_id FROM duel_eligible_problems dep " +
                "INNER JOIN problems p ON p.id = dep.problem_id " +
                "WHERE p.level = :level",
        nativeQuery = true
    )
    List<Long> findEligibleByLevel(@Param("level") String level);
}
