package com.example.codecombat2026.ws;

import com.example.codecombat2026.dto.ContestAnalyticsDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * WebSocket frame for real-time dashboard updates.
 * 
 * Sent to contest hosts every 5 seconds while they are connected to the
 * dashboard WebSocket endpoint. Provides live updates on:
 * - Current participant count
 * - Total submission count
 * - Top 10 leaderboard entries
 * - Recent submissions (last 10)
 * 
 * This frame is pushed from server to client only. Clients never send
 * this frame type.
 * 
 * Requirements: 32.1, 32.2
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardUpdateFrame {
    
    /**
     * Frame type discriminator. Always "DASHBOARD_UPDATE".
     */
    private String type = "DASHBOARD_UPDATE";
    
    /**
     * Contest ID this update applies to.
     */
    private Long contestId;
    
    /**
     * Server timestamp when this update was generated.
     */
    private LocalDateTime timestamp;
    
    /**
     * Current count of participants (users who joined via invite link).
     */
    private Integer participantCount;
    
    /**
     * Total count of submissions made to this contest.
     */
    private Integer submissionCount;
    
    /**
     * Top 10 leaderboard entries, sorted by rank.
     */
    private List<LeaderboardEntryDTO> topLeaderboard;
    
    /**
     * Last 10 submissions, ordered by submission time descending.
     */
    private List<RecentSubmissionDTO> recentSubmissions;
    
    /**
     * Factory method to create a dashboard update frame.
     */
    public static DashboardUpdateFrame of(
            Long contestId,
            Integer participantCount,
            Integer submissionCount,
            List<LeaderboardEntryDTO> topLeaderboard,
            List<RecentSubmissionDTO> recentSubmissions) {
        DashboardUpdateFrame frame = new DashboardUpdateFrame();
        frame.type = "DASHBOARD_UPDATE";
        frame.contestId = contestId;
        frame.timestamp = LocalDateTime.now();
        frame.participantCount = participantCount;
        frame.submissionCount = submissionCount;
        frame.topLeaderboard = topLeaderboard;
        frame.recentSubmissions = recentSubmissions;
        return frame;
    }
    
    /**
     * Leaderboard entry for dashboard updates.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeaderboardEntryDTO {
        private Integer rank;
        private String username;
        private Integer score;
        private Integer penalty;
        private Integer problemsSolved;
    }
    
    /**
     * Recent submission for dashboard updates.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentSubmissionDTO {
        private Long submissionId;
        private String username;
        private String problemTitle;
        private String status;
        private LocalDateTime submittedAt;
    }
}
