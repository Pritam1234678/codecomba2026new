package com.example.codecombat2026.sqljudge.dto;

import com.example.codecombat2026.sqljudge.entity.SqlSubmission;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Status response for a single SQL submission, returned by the polling
 * endpoint. For RUN (test) submissions a small sanitized result preview is
 * embedded so the frontend can render the candidate's own output.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SqlSubmissionStatusResponse {
    private Long id;
    private Long problemId;
    private SqlSubmission.Status status;
    private Long executionTimeMs;
    private String selectedNode;
    private String errorMessage;
    private SqlResult resultPreview;
    private boolean testRun;
    private LocalDateTime submittedAt;
    private LocalDateTime completedAt;

    public static SqlSubmissionStatusResponse from(SqlSubmission s, SqlResult preview) {
        return new SqlSubmissionStatusResponse(
            s.getId(), s.getProblemId(), s.getStatus(), s.getExecutionTimeMs(),
            s.getSelectedNode(), s.getErrorMessage(), preview, s.isTestRun(),
            s.getSubmittedAt(), s.getCompletedAt());
    }
}
