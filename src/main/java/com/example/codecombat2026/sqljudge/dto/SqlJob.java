package com.example.codecombat2026.sqljudge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Serialized into the Valkey SQL-judge queue as JSON. Consumed by
 * {@code SqlJudgeWorkerPool}. Deliberately tiny — the worker reloads the
 * problem metadata and submission row from the DB, so queue payloads stay small.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SqlJob {
    private Long submissionId;
    private Long userId;
    private Long problemId;
    /** true = RUN (test, no comparison), false = SUBMIT. */
    private boolean testRun;
}
