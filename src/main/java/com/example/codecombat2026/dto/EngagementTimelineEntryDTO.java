package com.example.codecombat2026.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for a single entry in the engagement timeline.
 * 
 * Represents submission count for a 15-minute interval.
 * 
 * Requirements: 16.1
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EngagementTimelineEntryDTO {
    
    /**
     * ISO 8601 timestamp marking the start of this 15-minute interval.
     */
    private String timestamp;
    
    /**
     * Number of submissions made during this 15-minute interval.
     */
    private Integer submissionCount;
}
