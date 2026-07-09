package com.example.codecombat2026.service;

import com.example.codecombat2026.dto.LeaderboardEntry;
import com.example.codecombat2026.entity.Submission;
import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.entity.UserPhoto;
import com.example.codecombat2026.repository.SubmissionRepository;
import com.example.codecombat2026.repository.UserPhotoRepository;
import com.example.codecombat2026.repository.UserRepository;
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

    @Autowired
    private UserPhotoRepository userPhotoRepository;

    @Autowired
    private UserRepository userRepository;

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
            String fullName = sample != null && sample.getUser() != null
                ? sample.getUser().getFullName() : null;
            String username = sample != null && sample.getUser() != null
                ? sample.getUser().getUsername() : null;

            leaderboard.add(new LeaderboardEntry(userId,
                fullName != null ? fullName : (username != null ? username : "Unknown"),
                username != null ? username : "N/A",
                (double) totalScore, solved, 0, null));
        }

        // Populate photo URLs — batch query avoids N+1
        try {
            List<UserPhoto> allPhotos = userPhotoRepository.findAll();
            Map<Long, String> photoMap = new HashMap<>();
            for (UserPhoto up : allPhotos) {
                String url = up.getPhotoUrl();
                if (url != null && !url.isBlank()) {
                    photoMap.put(up.getUserId(), url);
                }
            }
            for (LeaderboardEntry le : leaderboard) {
                String url = photoMap.get(le.getUserId());
                if (url != null) le.setPhotoUrl(url);
            }
        } catch (Exception ignored) {}

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

    /**
     * Batch-fetch user display info (name, roll, photo) for fast-path cache enrichment.
     * Queries both the users and photos tables in bulk to avoid N+1.
     */
    public Map<Long, LeaderboardEntry> batchUserInfo(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Map.of();

        // Fetch all matching users
        List<User> users = userRepository.findAllById(userIds);
        Map<Long, String> nameMap = new HashMap<>();
        Map<Long, String> rollMap = new HashMap<>();
        for (com.example.codecombat2026.entity.User u : users) {
            nameMap.put(u.getId(),
                u.getFullName() != null ? u.getFullName() : u.getUsername());
            rollMap.put(u.getId(), u.getUsername());
        }

        // Fetch photos
        List<UserPhoto> photos = userPhotoRepository.findAll();
        Map<Long, String> photoMap = new HashMap<>();
        for (UserPhoto up : photos) {
            if (up.getPhotoUrl() != null && !up.getPhotoUrl().isBlank()) {
                photoMap.put(up.getUserId(), up.getPhotoUrl());
            }
        }

        Map<Long, LeaderboardEntry> result = new HashMap<>();
        for (Long uid : userIds) {
            LeaderboardEntry e = new LeaderboardEntry();
            e.setUserId(uid);
            e.setUserName(nameMap.getOrDefault(uid, "Unknown"));
            e.setUserRoll(rollMap.getOrDefault(uid, "N/A"));
            e.setPhotoUrl(photoMap.get(uid));
            result.put(uid, e);
        }
        return result;
    }
}
