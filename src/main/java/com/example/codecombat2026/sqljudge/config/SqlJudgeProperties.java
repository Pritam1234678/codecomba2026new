package com.example.codecombat2026.sqljudge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Ops-control surface for the SQL judge.
 *
 * <p>Loaded by Spring's {@code @ConfigurationProperties} binding from
 * {@code application.properties} (and any env vars the operator has merged
 * into the JVM environment). Relaxed binding handles both kebab-case in
 * property files ({@code sql.judge.default-timeout-ms}) and SCREAMING_SNAKE_CASE
 * env vars ({@code SQL_JUDGE_DEFAULT_TIMEOUT_MS}).
 *
 * <p>The six Neon nodes are configured through {@link #getNodes()}; every node
 * needs its own bounded Hikari connection pool, and each carries its own
 * concurrency cap. Adding a 7th Neon node later requires configuration only —
 * no judge code changes.
 */
@Configuration
@ConfigurationProperties(prefix = "sql.judge")
@Data
public class SqlJudgeProperties {

    /** Master switch — when false the worker pool never starts. */
    private boolean enabled = false;

    /** Server-side query timeout for candidate SQL, in ms (statement_timeout). */
    private int defaultTimeoutMs = 2000;

    /** Hard cap on rows read back from Neon via JDBC setMaxRows. */
    private int maxResultRows = 500;

    /** Cap on the sanitized RUN preview stored per test submission. */
    private int previewMaxRows = 100;

    /** Number of worker threads that drain the Valkey queue (bounded concurrency). */
    private int workers = 8;

    /** Total execution slots across all nodes; per-node Semaphore is the real bound. */
    private int maxInflightQueries = 120;

    /** Max wall-clock seconds a worker waits for a free node before failing a job. */
    private int maxQueueWaitSeconds = 30;

    /** Consecutive failures before a node is marked unhealthy (circuit breaker). */
    private int failureThreshold = 3;

    /** How often (ms) the health sweep probes nodes with SELECT 1. */
    private long healthCheckIntervalMs = 30000;

    /** Max query length accepted, in chars. */
    private int maxSqlLength = 20000;

    /** Per-user rate limit for RUN (test) requests. */
    private int runRateLimit = 15;

    /** Per-user rate limit for SUBMIT requests. */
    private int submitRateLimit = 5;

    /** Rate-limit window in seconds. */
    private int rateWindowSeconds = 30;

    /** How often (ms) the janitor re-queues jobs stuck on processing lists. */
    private long reclaimIntervalMs = 60000;

    /** The six (or more) Neon execution nodes. */
    private List<NodeConfig> nodes = new ArrayList<>();

    @Data
    public static class NodeConfig {
        /** Stable id, e.g. {@code neon-1}. Stored on submissions as selected_node. */
        private String id;

        /** Pooled Neon JDBC URL. Credentials come from env vars via placeholders. */
        private String url;

        private String username;

        private String password;

        /** Per-node execution slot cap — never exceeds this many concurrent queries. */
        private int maxConcurrency = 20;

        /** Optional per-node timeout override; falls back to defaultTimeoutMs. */
        private int timeoutMs = 0;
    }
}
