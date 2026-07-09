package com.example.codecombat2026.service;

import com.example.codecombat2026.dto.SubmissionJob;
import com.example.codecombat2026.entity.Problem;
import com.example.codecombat2026.entity.Submission;
import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.repository.ProblemRepository;
import com.example.codecombat2026.repository.SubmissionRepository;
import com.example.codecombat2026.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Task 19.1: Update submission verdict handler for points in private contests.
 * 
 * Tests Requirements 35.1, 35.2, 35.3:
 * - Award points when submission status is ACCEPTED in private contest
 * - Use same point calculation as public contests (based on problem difficulty)
 * - Record in user_points or audit log (via totalPoints field update)
 * 
 * Point values:
 * - EASY: 5 points
 * - MEDIUM: 7 points
 * - HARD: 10 points
 */
@SpringBootTest
@TestPropertySource(properties = {
    "JUDGE_WORKERS=0" // Disable auto-start of workers for testing
})
@Transactional
class PrivateContestPointsAwardingTest {

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private SubmissionWorkerPool workerPool;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private Problem easyProblem;
    private Problem mediumProblem;
    private Problem hardProblem;

    @BeforeEach
    void setup() {
        // Clean up Redis keys
        redis.keys("private:*").forEach(key -> redis.delete(key));
        
        // Create test user with 0 initial points
        testUser = new User();
        testUser.setUsername("test_user_" + System.currentTimeMillis());
        testUser.setEmail("test" + System.currentTimeMillis() + "@example.com");
        testUser.setPassword("password");
        testUser.setFullName("Test User");
        testUser.setEnabled(true);
        testUser.setTotalPoints(0);
        testUser = userRepository.save(testUser);

        // Create test problems with different difficulty levels
        easyProblem = createProblem("Easy Problem", "EASY");
        mediumProblem = createProblem("Medium Problem", "MEDIUM");
        hardProblem = createProblem("Hard Problem", "HARD");
    }

    @Test
    void testAwardPointsForEasyProblem() {
        // Given: An AC submission for an EASY problem in a private contest
        Long contestId = 100L;
        Submission submission = createSubmission(testUser.getId(), easyProblem.getId(), contestId);
        
        // Initialize the score cache key
        String scoreKey = "private:score:" + contestId + ":" + testUser.getId() + ":" + easyProblem.getId();
        redis.opsForValue().set(scoreKey, "0");

        // When: finalizeAndNotify is called with AC status
        SubmissionJob job = createPrivateContestJob(submission.getId(), testUser.getId(), 
            easyProblem.getId(), contestId);
        
        workerPool.finalizeAndNotify(job, submission.getId(), 
            Submission.SubmissionStatus.AC, null, 5, 5, 100, 1000L, "[]");

        // Then: User should receive 5 points (EASY)
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(5, updatedUser.getTotalPoints(), "User should receive 5 points for EASY problem");
        
        // Verify points-awarded flag is set in Redis
        String pointsKey = "private:points:awarded:" + contestId + ":" + testUser.getId() + ":" + easyProblem.getId();
        assertTrue(redis.hasKey(pointsKey), "Points-awarded flag should be set");
    }

    @Test
    void testAwardPointsForMediumProblem() {
        // Given: An AC submission for a MEDIUM problem in a private contest
        Long contestId = 101L;
        Submission submission = createSubmission(testUser.getId(), mediumProblem.getId(), contestId);
        
        // Initialize the score cache key
        String scoreKey = "private:score:" + contestId + ":" + testUser.getId() + ":" + mediumProblem.getId();
        redis.opsForValue().set(scoreKey, "0");

        // When: finalizeAndNotify is called with AC status
        SubmissionJob job = createPrivateContestJob(submission.getId(), testUser.getId(), 
            mediumProblem.getId(), contestId);
        
        workerPool.finalizeAndNotify(job, submission.getId(), 
            Submission.SubmissionStatus.AC, null, 5, 5, 100, 1200L, "[]");

        // Then: User should receive 7 points (MEDIUM)
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(7, updatedUser.getTotalPoints(), "User should receive 7 points for MEDIUM problem");
    }

    @Test
    void testAwardPointsForHardProblem() {
        // Given: An AC submission for a HARD problem in a private contest
        Long contestId = 102L;
        Submission submission = createSubmission(testUser.getId(), hardProblem.getId(), contestId);
        
        // Initialize the score cache key
        String scoreKey = "private:score:" + contestId + ":" + testUser.getId() + ":" + hardProblem.getId();
        redis.opsForValue().set(scoreKey, "0");

        // When: finalizeAndNotify is called with AC status
        SubmissionJob job = createPrivateContestJob(submission.getId(), testUser.getId(), 
            hardProblem.getId(), contestId);
        
        workerPool.finalizeAndNotify(job, submission.getId(), 
            Submission.SubmissionStatus.AC, null, 10, 10, 100, 1500L, "[]");

        // Then: User should receive 10 points (HARD)
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(10, updatedUser.getTotalPoints(), "User should receive 10 points for HARD problem");
    }

    @Test
    void testNoPointsAwardedForWrongAnswer() {
        // Given: A WA submission in a private contest
        Long contestId = 103L;
        Submission submission = createSubmission(testUser.getId(), easyProblem.getId(), contestId);
        
        // Initialize the score cache key
        String scoreKey = "private:score:" + contestId + ":" + testUser.getId() + ":" + easyProblem.getId();
        redis.opsForValue().set(scoreKey, "0");

        // When: finalizeAndNotify is called with WA status
        SubmissionJob job = createPrivateContestJob(submission.getId(), testUser.getId(), 
            easyProblem.getId(), contestId);
        
        workerPool.finalizeAndNotify(job, submission.getId(), 
            Submission.SubmissionStatus.WA, null, 3, 5, 60, 1000L, "[]");

        // Then: User should NOT receive any points
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(0, updatedUser.getTotalPoints(), "User should not receive points for WA");
    }

    @Test
    void testNoPointsAwardedForPublicContest() {
        // Given: An AC submission in a PUBLIC contest (not private)
        Long contestId = 104L;
        Submission submission = createSubmission(testUser.getId(), easyProblem.getId(), contestId);
        
        // When: finalizeAndNotify is called for a public contest job
        SubmissionJob job = createPublicContestJob(submission.getId(), testUser.getId(), 
            easyProblem.getId(), contestId);
        
        workerPool.finalizeAndNotify(job, submission.getId(), 
            Submission.SubmissionStatus.AC, null, 5, 5, 100, 1000L, "[]");

        // Then: User should NOT receive points (public contests use different point system)
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(0, updatedUser.getTotalPoints(), "User should not receive points for public contest via this path");
    }

    @Test
    void testPointsAwardedOnlyOncePerProblem() {
        // Given: Multiple AC submissions for the same problem in the same contest
        Long contestId = 105L;
        Submission submission1 = createSubmission(testUser.getId(), easyProblem.getId(), contestId);
        
        // Initialize the score cache key with 0 (first submission)
        String scoreKey = "private:score:" + contestId + ":" + testUser.getId() + ":" + easyProblem.getId();
        redis.opsForValue().set(scoreKey, "0");

        // When: First AC submission
        SubmissionJob job1 = createPrivateContestJob(submission1.getId(), testUser.getId(), 
            easyProblem.getId(), contestId);
        
        workerPool.finalizeAndNotify(job1, submission1.getId(), 
            Submission.SubmissionStatus.AC, null, 5, 5, 100, 1000L, "[]");

        // Then: User should receive 5 points
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(5, updatedUser.getTotalPoints(), "User should receive 5 points on first AC");

        // When: Second AC submission for the same problem (already scored 100)
        Submission submission2 = createSubmission(testUser.getId(), easyProblem.getId(), contestId);
        redis.opsForValue().set(scoreKey, "100"); // Previous score was 100
        
        SubmissionJob job2 = createPrivateContestJob(submission2.getId(), testUser.getId(), 
            easyProblem.getId(), contestId);
        
        workerPool.finalizeAndNotify(job2, submission2.getId(), 
            Submission.SubmissionStatus.AC, null, 5, 5, 100, 900L, "[]");

        // Then: User should still have only 5 points (not 10)
        updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(5, updatedUser.getTotalPoints(), 
            "User should not receive additional points for re-solving the same problem");
    }

    @Test
    void testPointsAccumulateAcrossProblems() {
        // Given: Multiple problems solved in the same contest
        Long contestId = 106L;
        
        // Solve EASY problem
        Submission submission1 = createSubmission(testUser.getId(), easyProblem.getId(), contestId);
        String scoreKey1 = "private:score:" + contestId + ":" + testUser.getId() + ":" + easyProblem.getId();
        redis.opsForValue().set(scoreKey1, "0");
        
        SubmissionJob job1 = createPrivateContestJob(submission1.getId(), testUser.getId(), 
            easyProblem.getId(), contestId);
        workerPool.finalizeAndNotify(job1, submission1.getId(), 
            Submission.SubmissionStatus.AC, null, 5, 5, 100, 1000L, "[]");

        // Solve MEDIUM problem
        Submission submission2 = createSubmission(testUser.getId(), mediumProblem.getId(), contestId);
        String scoreKey2 = "private:score:" + contestId + ":" + testUser.getId() + ":" + mediumProblem.getId();
        redis.opsForValue().set(scoreKey2, "0");
        
        SubmissionJob job2 = createPrivateContestJob(submission2.getId(), testUser.getId(), 
            mediumProblem.getId(), contestId);
        workerPool.finalizeAndNotify(job2, submission2.getId(), 
            Submission.SubmissionStatus.AC, null, 5, 5, 100, 1200L, "[]");

        // Then: User should have 5 + 7 = 12 points
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(12, updatedUser.getTotalPoints(), 
            "User should accumulate points across multiple problems");
    }

    // Helper methods

    private Problem createProblem(String title, String level) {
        Problem problem = new Problem();
        problem.setTitle(title);
        problem.setDescription("Test problem description");
        problem.setLevel(level);
        problem.setActive(true);
        problem.setVisibility("PRIVATE_AVAILABLE");
        problem.setTimeLimit(5.0);
        problem.setMemoryLimit(256);
        return problemRepository.save(problem);
    }

    private Submission createSubmission(Long userId, Long problemId, Long contestId) {
        User user = userRepository.findById(userId).orElseThrow();
        Problem problem = problemRepository.findById(problemId).orElseThrow();
        
        Submission submission = new Submission();
        submission.setUser(user);
        submission.setProblem(problem);
        // Contest is optional for submissions, we don't need to set it for test
        submission.setCode("test code");
        submission.setLanguage(Submission.ProgrammingLanguage.JAVA);
        submission.setStatus(Submission.SubmissionStatus.PENDING);
        submission.setSubmittedAt(LocalDateTime.now());
        return submissionRepository.save(submission);
    }

    private SubmissionJob createPrivateContestJob(Long submissionId, Long userId, Long problemId, Long contestId) {
        return new SubmissionJob(
            submissionId, userId, problemId, contestId, 
            "test code", "JAVA", 5.0, 256, 
            false, null, null, contestId // privateContestId = contestId for private contests
        );
    }

    private SubmissionJob createPublicContestJob(Long submissionId, Long userId, Long problemId, Long contestId) {
        return new SubmissionJob(
            submissionId, userId, problemId, contestId, 
            "test code", "JAVA", 5.0, 256, 
            false, null, null, null // privateContestId = null for public contests
        );
    }
}
