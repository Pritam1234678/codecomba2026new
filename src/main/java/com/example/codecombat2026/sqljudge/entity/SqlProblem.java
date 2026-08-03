package com.example.codecombat2026.sqljudge.entity;

import com.example.codecombat2026.util.TimeUtil;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Metadata for one SQL assessment question. The actual dataset (tables) lives
 * in a dedicated {@code q_<id>} schema on every Neon node; this row stores only
 * what the app needs to judge a candidate's query:
 *
 * <ul>
 *   <li>{@code setupSql} — DDL + seed data, applied to each Neon node at publish.</li>
 *   <li>{@code officialSolutionSql} — reference query used to generate {@code expectedResult}.</li>
 *   <li>{@code expectedResult} — normalized JSON of the official solution output.</li>
 * </ul>
 *
 * {@code enabled} is flipped true only when provisioning succeeded on ALL
 * configured Neon nodes; {@link #getProvisioningStatus()} records per-node
 * state so a partial failure can be retried without a distributed transaction.
 */
@Entity
@Table(name = "sql_problems",
    indexes = {
        @Index(name = "idx_sql_problems_enabled", columnList = "enabled")
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SqlProblem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** e.g. {@code q_17} — schema name on every Neon node. Assigned at provision time. */
    @Column(name = "schema_name")
    private String schemaName;

    @Column(name = "setup_sql", columnDefinition = "TEXT")
    private String setupSql;

    @Column(name = "official_solution_sql", columnDefinition = "TEXT")
    private String officialSolutionSql;

    /** JSON payload produced by SqlResultNormalizer for the official solution. */
    @Column(name = "expected_result", columnDefinition = "TEXT")
    private String expectedResult;

    /** ORDERED | UNORDERED */
    @Column(name = "comparison_mode", nullable = false, length = 16)
    private String comparisonMode = "UNORDERED";

    /** Query timeout in ms, enforced server-side via SET statement_timeout. */
    @Column(name = "time_limit_ms", nullable = false)
    private int timeLimitMs = 2000;

    /** Hard cap on rows read back from Neon (JDBC setMaxRows). */
    @Column(name = "max_result_rows", nullable = false)
    private int maxResultRows = 500;

    /** JSON map nodeId -> {"status": PROVISIONED|FAILED|PENDING, "error": ..., "at": ...}. */
    @Column(name = "provisioning_status", columnDefinition = "TEXT")
    private String provisioningStatus;

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = TimeUtil.now();
        updatedAt = TimeUtil.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = TimeUtil.now();
    }
}
