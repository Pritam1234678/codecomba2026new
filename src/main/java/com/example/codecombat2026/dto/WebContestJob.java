package com.example.codecombat2026.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Job DTO for the web-contest queue. Serialized to JSON and pushed to Valkey.
 * Consumed by WebContestWorkerPool.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebContestJob {
    private Long submissionId;
    private Long userId;
    private Long problemId;
    private Long contestId;
    private Map<String, String> editableFiles;
    private String language;
    private String templatePath;
    private Double timeLimit;
    private Integer memoryLimit;
    private boolean testRun;
}
