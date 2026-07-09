package com.example.codecombat2026.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for per-problem analytics in a private contest.
 * 
 * Requirements: 16.1
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProblemStatDTO {
    
    /**
     * Problem ID.
     */
    private Long problemId;
    
    /**
     * Problem title.
     */
    private String problemTitle;
    
    /**
     * Total number of submissions for this problem.
     */
    private Integer submissionCount;
    
    /**
     * Number of submissions with status ACCEPTED.
     */
    private Integer acceptedSubmissions;
    
    /**
     * Acceptance rate as a percentage (0.0 - 100.0).
     * Ratio of ACCEPTED to total submissions.
     */
    private Double acceptanceRate;
    
    /**
     * Average time to solve (in minutes) for participants who solved this problem.
     * Calculated from contest start time to first ACCEPTED submission per user.
     */
    private Double avgSolveTimeMinutes;
}
