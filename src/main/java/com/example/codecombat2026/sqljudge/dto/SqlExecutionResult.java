package com.example.codecombat2026.sqljudge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Outcome of one query execution on one Neon node. Carries everything the
 * worker needs to finalize a submission: the verdict, timing, optional
 * normalized result for a RUN preview, and an error message for failures.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SqlExecutionResult {
    private String status;            // ACCEPTED-ish code: e.g. "OK", "SECURITY_VIOLATION", "TIME_LIMIT_EXCEEDED", "RUNTIME_ERROR"
    private SqlResult result;
    private String errorMessage;
    private long executionTimeMs;
    private String selectedNode;
}
