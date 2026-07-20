package com.example.codecombat2026.repository;

import com.example.codecombat2026.entity.PracticeSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PracticeSubmissionRepository extends JpaRepository<PracticeSubmission, Long> {

    List<PracticeSubmission> findByUserIdOrderBySubmittedAtDesc(Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE PracticeSubmission p SET p.status = :status WHERE p.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") com.example.codecombat2026.entity.Submission.SubmissionStatus status);

    @Modifying
    @Transactional
    @Query("UPDATE PracticeSubmission p SET " +
           "p.status = :status, p.errorMessage = :err, " +
           "p.testCasesPassed = :passed, p.totalTestCases = :total, " +
           "p.timeConsumed = :time, p.score = :score, " +
           "p.testCaseDetails = :details " +
           "WHERE p.id = :id AND p.status IN :inflight")
    int updateResult(@Param("id") Long id,
                     @Param("inflight") List<com.example.codecombat2026.entity.Submission.SubmissionStatus> inflight,
                     @Param("status") com.example.codecombat2026.entity.Submission.SubmissionStatus status,
                     @Param("err") String err,
                     @Param("passed") int passed,
                     @Param("total") int total,
                     @Param("time") Double time,
                     @Param("score") int score,
                     @Param("details") String details);
}
