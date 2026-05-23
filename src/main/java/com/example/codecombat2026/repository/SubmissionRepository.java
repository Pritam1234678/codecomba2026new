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
    List<Submission> findByUser_Id(Long userId);

    /**
     * Fetch only the N most recent submissions for a user — avoids loading
     * hundreds of rows just to show the dashboard table.
     */
    @Query("SELECT s FROM Submission s WHERE s.user.id = :userId ORDER BY s.submittedAt DESC")
    List<Submission> findRecentByUserId(@Param("userId") Long userId,
                                        org.springframework.data.domain.Pageable pageable);

    List<Submission> findByContest_Id(Long contestId);

    @Query("SELECT s FROM Submission s JOIN FETCH s.user WHERE s.contest.id = :contestId")
    List<Submission> findByContestIdWithUser(@Param("contestId") Long contestId);

    List<Submission> findByProblem_Id(Long problemId);

    /**
     * Returns the most recent non-test submission for a user+problem.
     * Uses ORDER BY submittedAt DESC + LIMIT 1 to avoid NonUniqueResultException
     * when multiple test-run submissions exist for the same user+problem.
     */
    @Query("SELECT s FROM Submission s WHERE s.user.id = :userId AND s.problem.id = :problemId ORDER BY s.submittedAt DESC")
    List<Submission> findByUser_IdAndProblem_IdOrderBySubmittedAtDesc(
        @Param("userId") Long userId,
        @Param("problemId") Long problemId,
        org.springframework.data.domain.Pageable pageable
    );

    void deleteByUser_Id(Long userId);

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
           "WHERE s.id = :id")
    void updateResult(@Param("id") Long id,
                      @Param("status") Submission.SubmissionStatus status,
                      @Param("err") String err,
                      @Param("passed") int passed,
                      @Param("total") int total,
                      @Param("time") Double time,
                      @Param("score") int score,
                      @Param("details") String details,
                      @Param("at") LocalDateTime at);
}
