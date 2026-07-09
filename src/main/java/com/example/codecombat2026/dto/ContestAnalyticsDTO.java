package com.example.codecombat2026.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for Private Contest Analytics Dashboard.
 * 
 * Contains aggregated metrics about contest participation, submission statistics,
 * per-problem performance, and engagement timeline.
 * 
 * Requirements: 16.1
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContestAnalyticsDTO {
    
    /**
     * Total number of invited participants (count of private_contest_participants rows).
     */
    private Integer totalParticipants;
    
    /**
     * Number of participants with at least one submission.
     */
    private Integer activeParticipants;
    
    /**
     * Total number of submissions across all problems in the contest.
     */
    private Integer totalSubmissions;
    
    /**
     * Per-problem statistics (submission count, acceptance rate, avg solve time).
     */
    private List<ProblemStatDTO> problemStats;
    
    /**
     * Engagement timeline showing submission count per 15-minute interval.
     */
    private List<EngagementTimelineEntryDTO> engagementTimeline;
}
