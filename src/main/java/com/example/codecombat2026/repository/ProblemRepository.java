package com.example.codecombat2026.repository;

import com.example.codecombat2026.entity.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, Long> {

    /**
     * Reads through {@code contest_problems} (M:N junction).
     * The legacy {@code problems.contest_id} column is intentionally NOT
     * consulted here.
     */
    @Query("""
        SELECT p FROM Problem p
        JOIN ContestProblem cp ON cp.problemId = p.id
        WHERE cp.contestId = :contestId
        ORDER BY cp.displayOrder ASC, cp.addedAt ASC
        """)
    List<Problem> findByContestId(@Param("contestId") Long contestId);

    /**
     * Standalone-pool query for the "Browse Existing Problems" picker.
     * Returns problems NOT currently attached to the given contest,
     * optionally filtered by level and title search.
     *
     * Uses a LEFT JOIN + IS NULL pattern instead of NOT IN subquery to
     * avoid Hibernate composite-key resolution issues with @IdClass.
     */
    @Query("""
        SELECT p FROM Problem p
        WHERE NOT EXISTS (
            SELECT 1 FROM ContestProblem cp
            WHERE cp.contestId = :contestId AND cp.problemId = p.id
        )
        AND (:level  IS NULL OR p.level = :level)
        AND (:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY p.id DESC
        """)
    List<Problem> findAvailableForContest(
        @Param("contestId") Long contestId,
        @Param("search")    String search,
        @Param("level")     String level
    );
}
