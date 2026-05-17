package com.example.codecombat2026.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Serialized into Valkey queue as JSON.
 * Consumed by SubmissionWorkerPool.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionJob {
    private Long submissionId;
    private Long userId;
    private Long problemId;
    private Long contestId;   // null if problem has no contest
    private String code;
    private String language;
    private Double timeLimit;
}
