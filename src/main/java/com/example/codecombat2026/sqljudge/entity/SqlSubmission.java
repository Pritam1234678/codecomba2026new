package com.example.codecombat2026.sqljudge.entity;

import com.example.codecombat2026.util.TimeUtil;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * One candidate SQL submission (RUN or SUBMIT).
 *
 * Created with status {@code QUEUED} by the API, then consumed by
 * {@code SqlJudgeWorkerPool}. The full result set is never persisted — only a
 * small sanitized preview for test runs (RUN), so the polling endpoint can
 * render it after the queue drains.
 */
@Entity
@Table(name = "sql_submissions",
    indexes = {
        @Index(name = "idx_sql_subs_user", columnList = "user_id"),
        @Index(name = "idx_sql_subs_problem", columnList = "problem_id"),
        @Index(name = "idx_sql_subs_user_problem", columnList = "user_id, problem_id")
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SqlSubmission {

    public enum Status {
        QUEUED,
        RUNNING,
        ACCEPTED,
        WRONG_ANSWER,
        TIME_LIMIT_EXCEEDED,
        RUNTIME_ERROR,
        SECURITY_VIOLATION,
        INTERNAL_ERROR
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "problem_id", nullable = false)
    private Long problemId;

    @Column(name = "submitted_sql", columnDefinition = "TEXT", nullable = false)
    private String submittedSql;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.QUEUED;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "selected_node", length = 64)
    private String selectedNode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /** Small sanitized preview for test runs (RUN), as JSON. */
    @Column(name = "result_preview", columnDefinition = "TEXT")
    private String resultPreview;

    @Column(name = "test_run", nullable = false)
    private boolean testRun = false;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        if (submittedAt == null) submittedAt = TimeUtil.now();
    }
}
