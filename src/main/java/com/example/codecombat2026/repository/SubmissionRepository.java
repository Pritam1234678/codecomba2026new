package com.example.codecombat2026.repository;

import com.example.codecombat2026.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    // Real submissions only — test runs are excluded from all user-facing reads.
    @Query("SELECT s FROM Submission s WHERE s.user.id = :userId AND s.testRun = false")
    List<Submission> findByUser_Id(@Param("userId") Long userId);

    /**
     * Fetch only the N most recent submissions for a user — avoids loading
     * hundreds of rows just to show the dashboard table.
     */
    @Query("SELECT s FROM Submission s WHERE s.user.id = :userId AND s.testRun = false ORDER BY s.submittedAt DESC")
    List<Submission> findRecentByUserId(@Param("userId") Long userId,
                                        org.springframework.data.domain.Pageable pageable);

    List<Submission> findByContest_Id(Long contestId);

    @Query("SELECT s FROM Submission s JOIN FETCH s.user WHERE s.contest.id = :contestId AND s.testRun = false")
    List<Submission> findByContestIdWithUser(@Param("contestId") Long contestId);

    List<Submission> findByProblem_Id(Long problemId);

    /**
     * Submissions made by a user against a contest within a closed time
     * window — drives the proctoring admin drill-down's "submissions
     * during this session" panel (Req 19.4). Bounds are inclusive on both
     * ends; callers pass the session's {@code started_at} as
     * {@code start} and either the session's {@code ended_at} or
     * {@code NOW()} (for active sessions) as {@code end}.
     *
     * <p>Resolved Q4: we deliberately do not add a
     * {@code proctoring_session_id} column to {@code submissions} —
     * the unique-per-{@code (contest_id, user_id)} constraint on
     * {@code proctoring_sessions} makes this window-correlated lookup
     * unambiguous, so the join can stay logical without bloating the
     * already-hot {@code submissions} table.
     *
     * <p>Results are ordered ascending by {@code submittedAt} so the
     * admin timeline reads top-to-bottom in chronological order.
     */
    @Query("SELECT s FROM Submission s " +
           "WHERE s.user.id = :userId " +
           "AND s.contest.id = :contestId " +
           "AND s.testRun = false " +
           "AND s.submittedAt BETWEEN :start AND :end " +
           "ORDER BY s.submittedAt ASC")
    List<Submission> findByUser_IdAndContest_IdAndSubmittedAtBetween(
            @Param("userId") Long userId,
            @Param("contestId") Long contestId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Returns the most recent non-test submission for a user+problem.
     * Uses ORDER BY submittedAt DESC + LIMIT 1 to avoid NonUniqueResultException
     * when multiple test-run submissions exist for the same user+problem.
     */
    @Query("SELECT s FROM Submission s WHERE s.user.id = :userId AND s.problem.id = :problemId AND s.testRun = false ORDER BY s.submittedAt DESC")
    List<Submission> findByUser_IdAndProblem_IdOrderBySubmittedAtDesc(
        @Param("userId") Long userId,
        @Param("problemId") Long problemId,
        org.springframework.data.domain.Pageable pageable
    );

    void deleteByUser_Id(Long userId);

    @Query("SELECT COUNT(s) FROM Submission s WHERE s.user.id = :userId AND s.problem.id = :problemId AND s.testRun = true AND s.contest.id IS NOT NULL")
    long countContestRunsByUserAndProblem(@Param("userId") Long userId, @Param("problemId") Long problemId);

    @Query("SELECT COUNT(s) FROM Submission s WHERE s.user.id = :userId AND s.problem.id = :problemId AND s.testRun = false AND s.contest.id IS NOT NULL")
    long countContestSubmitsByUserAndProblem(@Param("userId") Long userId, @Param("problemId") Long problemId);

    @Query("SELECT s.contest.id, COUNT(DISTINCT s.user.id) FROM Submission s WHERE s.contest.id IN :contestIds AND s.testRun = false GROUP BY s.contest.id")
    List<Object[]> countParticipantsByContestIds(@Param("contestIds") List<Long> contestIds);

    // ─── Async worker update methods ──────────────────────────────────────────

    @Modifying
    @Transactional
    @Query("UPDATE Submission s SET s.status = :status WHERE s.id = :id")
    void updateStatus(@Param("id") Long id,
                      @Param("status") Submission.SubmissionStatus status);

    @Modifying
    @Transactional
    @Query("UPDATE Submission s SET " +
           "s.status = :status, " +
           "s.errorMessage = :err, " +
           "s.testCasesPassed = :passed, " +
           "s.totalTestCases = :total, " +
           "s.timeConsumed = :time, " +
           "s.score = :score, " +
           "s.testCaseDetails = :details, " +
           "s.submittedAt = :at " +
           "WHERE s.id = :id AND s.status IN :inflight")
int updateResult(@Param("id") Long id,
                       @Param("inflight") java.util.List<Submission.SubmissionStatus> inflight,
                       @Param("status") Submission.SubmissionStatus status,
                       @Param("err") String err,
                       @Param("passed") int passed,
                       @Param("total") int total,
                       @Param("time") Double time,
                       @Param("score") int score,
                       @Param("details") String details,
                       @Param("at") LocalDateTime at);
}
