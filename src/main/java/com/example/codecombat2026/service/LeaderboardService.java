package com.example.codecombat2026.service;

import com.example.codecombat2026.dto.LeaderboardEntry;
import com.example.codecombat2026.entity.Submission;
import com.example.codecombat2026.repository.SubmissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Admin batch leaderboard.
 *
 * Contract (matches the live ZSET in {@link LeaderboardCacheService}):
 *   - per (user, problem) → take the best score across all submissions
 *   - per user → totalScore = SUM of those best-per-problem scores
 *   - problemsSolved = number of problems where best score == 100
 *
 * The live ZSET aggregates by INCRBY-ing the score on each AC. Test runs and
 * non-AC submissions don't update the ZSET. To match that semantics here we
 * also drop test runs (handled at the worker level — practice/test rows still
 * land in the table when they save) and only count submissions whose status is
 * a final verdict.
 */
@Service
public class LeaderboardService {

    @Autowired
    private SubmissionRepository submissionRepository;

    public List<LeaderboardEntry> getContestLeaderboard(Long contestId) {
        List<Submission> submissions = submissionRepository.findByContestIdWithUser(contestId);

        // (userId, problemId) → best score
        Map<Long, Map<Long, Integer>> bestPerProblem = new HashMap<>();
        Map<Long, Submission> userSamples = new HashMap<>();

        for (Submission submission : submissions) {
            // Skip in-flight rows; they have no score yet
            Submission.SubmissionStatus s = submission.getStatus();
            if (s == null || s == Submission.SubmissionStatus.PENDING
                          || s == Submission.SubmissionStatus.JUDGING) continue;

            Long userId    = submission.getUserId();
            Long problemId = submission.getProblemId();
            int  score     = submission.getScore() != null ? submission.getScore() : 0;
            if (userId == null || problemId == null) continue;

            userSamples.putIfAbsent(userId, submission);
            bestPerProblem
                .computeIfAbsent(userId, k -> new HashMap<>())
                .merge(problemId, score, Math::max);
        }

        List<LeaderboardEntry> leaderboard = new ArrayList<>();
        for (Map.Entry<Long, Map<Long, Integer>> e : bestPerProblem.entrySet()) {
            Long userId = e.getKey();
            Map<Long, Integer> perProblem = e.getValue();

            // Sum of best-per-problem (matches ZSET aggregation)
            int totalScore = perProblem.values().stream().mapToInt(Integer::intValue).sum();
            int solved     = (int) perProblem.values().stream().filter(v -> v == 100).count();

            Submission sample = userSamples.get(userId);
            String userName = sample != null && sample.getUser() != null
                ? sample.getUser().getFullName() : "Unknown";
            String userRoll = sample != null && sample.getUser() != null
                ? sample.getUser().getUsername() : "N/A";

            leaderboard.add(new LeaderboardEntry(userId, userName, userRoll,
                (double) totalScore, solved, 0));
        }

        // Sort by totalScore desc, tiebreak by problemsSolved desc
        leaderboard.sort((a, b) -> {
            int cmp = Double.compare(b.getTotalScore(), a.getTotalScore());
            if (cmp != 0) return cmp;
            return Integer.compare(b.getProblemsSolved(), a.getProblemsSolved());
        });

        for (int i = 0; i < leaderboard.size(); i++) {
            leaderboard.get(i).setRank(i + 1);
        }

        return leaderboard;
    }
}
