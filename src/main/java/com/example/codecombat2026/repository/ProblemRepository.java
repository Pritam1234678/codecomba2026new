package com.example.codecombat2026.repository;

import com.example.codecombat2026.entity.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
     * Uses a NOT EXISTS correlated subquery (Hibernate composite-key safe).
     * Search and level filters use COALESCE so PostgreSQL never sees a
     * bare NULL parameter inside LIKE/= — that triggered
     * "function lower(bytea) does not exist" because the JDBC driver
     * binds NULL as bytea by default.
     */
    @Query(value = """
        SELECT p.* FROM problems p
        WHERE NOT EXISTS (
            SELECT 1 FROM contest_problems cp
            WHERE cp.contest_id = :contestId AND cp.problem_id = p.id
        )
        AND (CAST(:level AS text) IS NULL OR p.level = CAST(:level AS text))
        AND (CAST(:search AS text) IS NULL
             OR LOWER(p.title) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')))
        ORDER BY p.id DESC
        """, nativeQuery = true)
    List<Problem> findAvailableForContest(
        @Param("contestId") Long contestId,
        @Param("search")    String search,
        @Param("level")     String level
    );

    /** Null out the legacy {@code contest_id} FK before deleting a contest. */
    @Modifying
    @Query(value = "UPDATE problems SET contest_id = NULL WHERE contest_id = :contestId", nativeQuery = true)
    int nullOutContestId(@Param("contestId") Long contestId);
}
