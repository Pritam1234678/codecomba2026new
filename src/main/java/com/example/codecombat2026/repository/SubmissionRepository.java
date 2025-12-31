package com.example.codecombat2026.repository;

import com.example.codecombat2026.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByUser_Id(Long userId);

    List<Submission> findByContest_Id(Long contestId);

    // Custom query to fetch submissions with user data for leaderboard
    @Query("SELECT s FROM Submission s JOIN FETCH s.user WHERE s.contest.id = :contestId")
    List<Submission> findByContestIdWithUser(@Param("contestId") Long contestId);

    List<Submission> findByProblem_Id(Long problemId);

    // Find existing submission for a user on a specific problem
    Submission findByUser_IdAndProblem_Id(Long userId, Long problemId);
}
