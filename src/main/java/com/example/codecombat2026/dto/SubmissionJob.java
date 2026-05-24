package com.example.codecombat2026.dto;

import java.util.UUID;

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
    /** Memory limit in MB (sandbox enforces via prlimit). */
    private Integer memoryLimit;
    /** true = test run (not saved to DB, no leaderboard update) */
    private boolean testRun = false;
    /**
     * Live Duel match id when this job was produced inside a duel.
     * Null for practice and contest submissions. Defaults to null so jobs
     * already on submission:queue at deploy time deserialize cleanly.
     */
    private UUID duelId;
}
