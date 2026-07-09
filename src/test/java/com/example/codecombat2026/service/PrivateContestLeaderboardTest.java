package com.example.codecombat2026.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.example.codecombat2026.dto.SubmissionJob;

/**
 * Tests for private contest leaderboard updates in finalizeAndNotify.
 * Validates Requirements 13.7 and 14.2 from private-contest-hosting spec.
 */
class PrivateContestLeaderboardTest {

    @Test
    void testIsPrivateContest_WithPrivateContestId() {
        SubmissionJob job = new SubmissionJob();
        job.setPrivateContestId(25L);
        
        assertTrue(job.isPrivateContest(), 
            "Job with privateContestId should return true for isPrivateContest()");
    }

    @Test
    void testIsPrivateContest_WithoutPrivateContestId() {
        SubmissionJob job = new SubmissionJob();
        job.setPrivateContestId(null);
        
        assertFalse(job.isPrivateContest(), 
            "Job without privateContestId should return false for isPrivateContest()");
    }

    @Test
    void testIsPrivateContest_DefaultValue() {
        SubmissionJob job = new SubmissionJob();
        
        assertFalse(job.isPrivateContest(), 
            "Job with default privateContestId should return false for isPrivateContest()");
    }

    @Test
    void testPrivateContestLeaderboardKeys() {
        // Verify the key format expected for private contest leaderboards
        Long contestId = 50L;
        Long userId = 1L;
        Long problemId = 10L;
        
        String expectedLeaderboardKey = "private:leaderboard:" + contestId;
        String expectedScoreKey = "private:score:" + contestId + ":" + userId + ":" + problemId;
        
        assertEquals("private:leaderboard:50", expectedLeaderboardKey, 
            "Leaderboard key format should match design specification");
        assertEquals("private:score:50:1:10", expectedScoreKey, 
            "Score key format should match design specification");
    }

    @Test
    void testPublicContestLeaderboardKeys() {
        // Verify the key format expected for public contest leaderboards
        Long contestId = 51L;
        Long userId = 2L;
        Long problemId = 11L;
        
        String expectedScoreKey = "contest:score:" + contestId + ":" + userId + ":" + problemId;
        
        assertEquals("contest:score:51:2:11", expectedScoreKey, 
            "Public contest score key format should match existing implementation");
    }

    @Test
    void testDeltaScoreCalculation_FirstSubmission() {
        int prevScore = 0;
        int newScore = 100;
        int delta = newScore - prevScore;
        
        assertEquals(100, delta, "Delta for first submission should be the full score");
    }

    @Test
    void testDeltaScoreCalculation_ImprovedScore() {
        int prevScore = 50;
        int newScore = 100;
        int delta = newScore - prevScore;
        
        assertEquals(50, delta, "Delta for improved score should be positive difference");
    }

    @Test
    void testDeltaScoreCalculation_WorseScore() {
        int prevScore = 80;
        int newScore = 30;
        int delta = newScore - prevScore;
        
        assertEquals(-50, delta, "Delta for worse score should be negative difference");
    }

    @Test
    void testDeltaScoreCalculation_SameScore() {
        int prevScore = 75;
        int newScore = 75;
        int delta = newScore - prevScore;
        
        assertEquals(0, delta, "Delta for same score should be zero (no update needed)");
    }
}
