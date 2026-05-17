package com.example.codecombat2026.service;

import com.example.codecombat2026.dto.LeaderboardEntry;
import com.example.codecombat2026.entity.Submission;
import com.example.codecombat2026.repository.SubmissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class LeaderboardService {

    @Autowired
    private SubmissionRepository submissionRepository;

    public List<LeaderboardEntry> getContestLeaderboard(Long contestId) {
        // Fetch all submissions for the contest with user data
        List<Submission> submissions = submissionRepository.findByContestIdWithUser(contestId);

        // Group submissions by user and problem, keeping only the best score for each
        // problem
        Map<Long, Map<Long, Integer>> userProblemScores = new HashMap<>();
        Map<Long, Submission> userSubmissionMap = new HashMap<>(); // To get user details

        for (Submission submission : submissions) {
            Long userId = submission.getUserId();
            Long problemId = submission.getProblemId();
            Integer score = submission.getScore() != null ? submission.getScore() : 0;

            // Store submission for user details
            userSubmissionMap.putIfAbsent(userId, submission);

            // Track best score per problem per user
            userProblemScores.putIfAbsent(userId, new HashMap<>());
            Map<Long, Integer> problemScores = userProblemScores.get(userId);

            // Keep the best score for this problem
            problemScores.put(problemId, Math.max(problemScores.getOrDefault(problemId, 0), score));
        }

        // Calculate total scores and create leaderboard entries
        List<LeaderboardEntry> leaderboard = new ArrayList<>();

        for (Map.Entry<Long, Map<Long, Integer>> userEntry : userProblemScores.entrySet()) {
            Long userId = userEntry.getKey();
            Map<Long, Integer> problemScores = userEntry.getValue();

            // Calculate average score across all problems
            int numProblems = problemScores.size();
            int sumScores = problemScores.values().stream().mapToInt(Integer::intValue).sum();
            double averageScore = numProblems > 0 ? (double) sumScores / numProblems : 0.0;

            int problemsSolved = (int) problemScores.values().stream().filter(score -> score == 100).count();

            // Get user details from a submission
            Submission sampleSubmission = userSubmissionMap.get(userId);
            String userName = sampleSubmission.getUser() != null ? sampleSubmission.getUser().getFullName() : "Unknown";
            String userRoll = sampleSubmission.getUser() != null ? sampleSubmission.getUser().getRollNumber() : "N/A";

            LeaderboardEntry entry = new LeaderboardEntry(userId, userName, userRoll, averageScore, problemsSolved, 0);
            leaderboard.add(entry);
        }

        // Sort by total score descending
        leaderboard.sort((a, b) -> b.getTotalScore().compareTo(a.getTotalScore()));

        // Assign ranks
        for (int i = 0; i < leaderboard.size(); i++) {
            leaderboard.get(i).setRank(i + 1);
        }

        return leaderboard;
    }
}
