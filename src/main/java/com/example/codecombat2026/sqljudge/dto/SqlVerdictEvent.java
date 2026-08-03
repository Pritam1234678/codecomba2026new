package com.example.codecombat2026.sqljudge.dto;

import com.example.codecombat2026.sqljudge.entity.SqlSubmission;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Verdict pushed to the user's SSE stream (event name {@code sql_verdict})
 * when the worker finishes a submission. Mirrors VerdictEvent for the code
 * judge but keeps SQL fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SqlVerdictEvent {
    private Long submissionId;
    private SqlSubmission.Status status;
    private boolean testRun;
    private Long executionTimeMs;
    private String selectedNode;
    private String errorMessage;
    private SqlResult resultPreview;
    private LocalDateTime completedAt;
}
