package com.example.codecombat2026.sqljudge.worker;

import com.example.codecombat2026.sqljudge.comparator.SqlResultComparator;
import com.example.codecombat2026.sqljudge.config.SqlJudgeProperties;
import com.example.codecombat2026.sqljudge.dto.SqlExecutionResult;
import com.example.codecombat2026.sqljudge.dto.SqlJob;
import com.example.codecombat2026.sqljudge.dto.SqlResult;
import com.example.codecombat2026.sqljudge.dto.SqlVerdictEvent;
import com.example.codecombat2026.sqljudge.entity.SqlProblem;
import com.example.codecombat2026.sqljudge.entity.SqlSubmission;
import com.example.codecombat2026.sqljudge.executor.SqlQueryExecutor;
import com.example.codecombat2026.sqljudge.repository.SqlProblemRepository;
import com.example.codecombat2026.sqljudge.repository.SqlSubmissionRepository;
import com.example.codecombat2026.sqljudge.router.NeonNode;
import com.example.codecombat2026.sqljudge.router.SqlExecutionRouter;
import com.example.codecombat2026.sqljudge.service.SqlExpectedResultCache;
import com.example.codecombat2026.sqljudge.validator.SqlQueryValidator;
import com.example.codecombat2026.service.SseEmitterRegistry;
import com.example.codecombat2026.util.TimeUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Worker pool that drains the SQL-judge Valkey queue.
 *
 * <p>Durability mirrors the code-judge {@code SubmissionWorkerPool}:
 * <ul>
 *   <li>Producer LPUSHes a {@link SqlJob} onto {@code sqljudge:queue}.</li>
 *   <li>Worker atomically claims via LMOVE onto {@code sqljudge:processing:&lt;instance&gt;:&lt;idx&gt;}.</li>
 *   <li>On crash the janitor re-queues jobs still on the processing list after a grace period.</li>
 * </ul>
 *
 * <p>Bounded concurrency: {@code workers} threads (default 8) run concurrently,
 * and each node's {@code Semaphore} caps per-node in-flight queries. Under a
 * 500–1000 submission burst, submissions pile up in Valkey and drain at the
 * controlled rate — the API never creates 500 threads or 500 connections.
 */
@Component
public class SqlJudgeWorkerPool {

    private static final Logger log = LoggerFactory.getLogger(SqlJudgeWorkerPool.class);

    public static final String QUEUE_KEY = "sqljudge:queue";
    private static final String PROCESSING_KEY_PREFIX = "sqljudge:processing:";
    private static final String PROCESSING_REGISTRY = "sqljudge:processing:registry";
    private static final String CLAIM_KEY_PREFIX = "sqljudge:claim:";

    private final SqlJudgeProperties properties;
    private final SqlSubmissionRepository submissionRepository;
    private final SqlProblemRepository problemRepository;
    private final SqlQueryValidator validator;
    private final SqlQueryExecutor executor;
    private final SqlExecutionRouter router;
    private final SqlExpectedResultCache expectedCache;
    private final SqlResultComparator comparator;
    private final SseEmitterRegistry sseRegistry;
    private final StringRedisTemplate redis;
    private final StringRedisTemplate workerRedis;
    private final ObjectMapper objectMapper;

    private ThreadPoolExecutor pool;
    private volatile boolean shuttingDown = false;
    private final String instanceId;
    private final AtomicInteger activeJobs = new AtomicInteger(0);

    @Autowired
    public SqlJudgeWorkerPool(SqlJudgeProperties properties,
                              SqlSubmissionRepository submissionRepository,
                              SqlProblemRepository problemRepository,
                              SqlQueryValidator validator,
                              SqlQueryExecutor executor,
                              SqlExecutionRouter router,
                              SqlExpectedResultCache expectedCache,
                              SqlResultComparator comparator,
                              SseEmitterRegistry sseRegistry,
                              @Qualifier("stringRedisTemplate") StringRedisTemplate redis,
                              @Qualifier("workerRedisTemplate") StringRedisTemplate workerRedis,
                              ObjectMapper objectMapper) {
        this.properties = properties;
        this.submissionRepository = submissionRepository;
        this.problemRepository = problemRepository;
        this.validator = validator;
        this.executor = executor;
        this.router = router;
        this.expectedCache = expectedCache;
        this.comparator = comparator;
        this.sseRegistry = sseRegistry;
        this.redis = redis;
        this.workerRedis = workerRedis;
        this.objectMapper = objectMapper;
        this.instanceId = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
    }

    @PostConstruct
    public void startWorkers() {
        if (!properties.isEnabled()) {
            log.info("⏸️  SQL judge disabled (sql.judge.enabled=false) — worker pool not started");
            return;
        }
        int workerCount = properties.getWorkers();
        pool = new ThreadPoolExecutor(
            workerCount, workerCount,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            r -> {
                Thread t = new Thread(r);
                t.setName("sqljudge-worker-" + SqlJudgeWorkerPool.WORKER_SEQ.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        );
        for (int i = 0; i < workerCount; i++) {
            final int idx = i;
            pool.submit(() -> workerLoop(idx));
        }
        log.info("✅ SQL judge: started {} workers (queue={}, instance={})", workerCount, QUEUE_KEY, instanceId);
    }

    private static final AtomicInteger WORKER_SEQ = new AtomicInteger();

    @PreDestroy
    public void shutdown() {
        shuttingDown = true;
        if (pool == null) return;
        log.info("SQL judge worker pool shutting down — draining in-flight jobs...");
        pool.shutdown();
        try {
            if (!pool.awaitTermination(15, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private String processingKey(int workerIdx) {
        return PROCESSING_KEY_PREFIX + instanceId + ":" + workerIdx;
    }

    private void workerLoop(int workerIdx) {
        String procKey = processingKey(workerIdx);
        try { redis.opsForSet().add(PROCESSING_REGISTRY, procKey); } catch (Exception ignored) {}

        while (!Thread.currentThread().isInterrupted() && !shuttingDown) {
            try {
                String jobJson = workerRedis.opsForList().move(
                    QUEUE_KEY,
                    org.springframework.data.redis.connection.RedisListCommands.Direction.RIGHT,
                    procKey,
                    org.springframework.data.redis.connection.RedisListCommands.Direction.LEFT,
                    Duration.ofSeconds(3));

                if (jobJson == null) continue;

                SqlJob job = objectMapper.readValue(jobJson, SqlJob.class);
                activeJobs.incrementAndGet();
                // Store claim timestamp so the janitor can compute real age.
                try {
                    redis.opsForValue().set(CLAIM_KEY_PREFIX + job.getSubmissionId(),
                        String.valueOf(System.currentTimeMillis()),
                        java.time.Duration.ofMinutes(10));
                } catch (Exception ignored) {}
                try {
                    processJob(job);
                } finally {
                    activeJobs.decrementAndGet();
                    try {
                        redis.opsForList().remove(procKey, 0, jobJson);
                    } catch (Exception e) {
                        log.warn("Failed to LREM processed SQL job from {}: {}", procKey, e.getMessage());
                    }
                }
            } catch (Exception e) {
                if (Thread.currentThread().isInterrupted() || shuttingDown) break;
                log.error("SQL judge worker error: {}", e.getMessage());
                try { Thread.sleep(500); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * Hot path — keep it dead simple:
     * mark RUNNING → validate → pick node → acquire permit → execute → compare → release → save → SSE.
     */
    private void processJob(SqlJob job) {
        Long submissionId = job.getSubmissionId();
        long start = System.currentTimeMillis();
        boolean finalized = false;

        try {
            // 1. Mark RUNNING (idempotent).
            markRunning(submissionId);

            // 2. Load problem metadata.
            SqlProblem problem = problemRepository.findById(job.getProblemId()).orElse(null);
            if (problem == null) {
                finalize(submissionId, SqlSubmission.Status.INTERNAL_ERROR, null,
                    "Problem not found", null);
                finalized = true;
                return;
            }
            if (!problem.isEnabled()) {
                finalize(submissionId, SqlSubmission.Status.INTERNAL_ERROR, null,
                    "Problem is no longer active", null);
                finalized = true;
                return;
            }

            // 3. Validate the candidate SQL.
            SqlSubmission submission = submissionRepository.findById(submissionId).orElse(null);
            String candidateSql = submission != null ? submission.getSubmittedSql() : null;
            if (candidateSql == null || candidateSql.isBlank()) {
                finalize(submissionId, SqlSubmission.Status.SECURITY_VIOLATION, null,
                    "Empty SQL query", null);
                finalized = true;
                return;
            }
            SqlQueryValidator.ValidationResult validation = validator.validate(candidateSql);
            if (!validation.ok) {
                finalize(submissionId, SqlSubmission.Status.SECURITY_VIOLATION, null,
                    validation.message, null);
                finalized = true;
                return;
            }

            // 4. Execute with controlled concurrency + one alternate-node retry.
            SqlExecutionResult execution = executeWithRetry(job.getProblemId(), candidateSql, problem);
            if (execution == null || "NODE_UNAVAILABLE".equals(execution.getStatus())) {
                finalize(submissionId, SqlSubmission.Status.INTERNAL_ERROR, null,
                    "No Neon node available. Please try again in a moment.", null);
                finalized = true;
                return;
            }

            // 5. For RUN: return the preview. For SUBMIT: compare against expected.
            SqlSubmission.Status status;
            String error = null;
            SqlResult preview = null;

            if (job.isTestRun()) {
                status = "OK".equals(execution.getStatus())
                    ? SqlSubmission.Status.ACCEPTED
                    : mapExecStatus(execution.getStatus());
                if (status == SqlSubmission.Status.ACCEPTED) {
                    preview = execution.getResult();
                } else {
                    error = execution.getErrorMessage();
                }
            } else {
                if ("OK".equals(execution.getStatus())) {
                    SqlResult expected = expectedCache.getExpected(job.getProblemId());
                    if (expected == null) {
                        status = SqlSubmission.Status.INTERNAL_ERROR;
                        error = "Expected result not configured for this problem";
                    } else {
                        boolean match = comparator.matches(expected, execution.getResult(), problem.getComparisonMode());
                        status = match ? SqlSubmission.Status.ACCEPTED : SqlSubmission.Status.WRONG_ANSWER;
                        if (!match && status == SqlSubmission.Status.WRONG_ANSWER) {
                            error = "Output does not match the expected result";
                        }
                    }
                } else {
                    status = mapExecStatus(execution.getStatus());
                    error = execution.getErrorMessage();
                }
            }

            // Persist the RUN preview BEFORE finalizing so the SSE verdict
            // (pushed from the re-loaded submission) carries the preview too.
            if (job.isTestRun() && preview != null) {
                savePreview(submissionId, preview);
            }

            finalize(submissionId, status, execution.getExecutionTimeMs(), error,
                execution.getSelectedNode());

            finalized = true;

            log.info("SQL judge sub {} problem {} {} → {} ({}ms, node={})",
                submissionId, job.getProblemId(), job.isTestRun() ? "RUN" : "SUBMIT",
                status, System.currentTimeMillis() - start, execution.getSelectedNode());

        } catch (Exception e) {
            log.error("SQL judge job {} failed: {}", submissionId, e.getMessage(), e);
            try {
                finalize(submissionId, SqlSubmission.Status.INTERNAL_ERROR, null,
                    e.getMessage() != null ? e.getMessage() : "Internal judge error", null);
                finalized = true;
            } catch (Exception inner) {
                log.error("SQL judge: even INTERNAL_ERROR finalize failed for {}: {}", submissionId, inner.getMessage());
            }
        } finally {
            if (!finalized && submissionId != null) {
                try {
                    finalize(submissionId, SqlSubmission.Status.INTERNAL_ERROR, null,
                        "Judge worker terminated unexpectedly", null);
                } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Pick a node, acquire its permit, execute. On node failure BEFORE the
     * query succeeds, release and retry once on an alternate healthy node.
     */
    private SqlExecutionResult executeWithRetry(Long problemId, String candidateSql, SqlProblem problem) {
        NeonNode first = router.select(List.of());
        if (first == null) {
            // No node available right now. Wait briefly for a slot within the
            // configured max-queue-wait window instead of failing the job.
            long deadline = System.currentTimeMillis() + properties.getMaxQueueWaitSeconds() * 1000L;
            while (System.currentTimeMillis() < deadline) {
                try { Thread.sleep(200); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
                first = router.select(List.of());
                if (first != null) break;
            }
            if (first == null) return null;
        }

        try {
            SqlExecutionResult result = executor.execute(first, problem, candidateSql);
            if ("NODE_UNAVAILABLE".equals(result.getStatus())) {
                // Node failed before the query ran — retry once on an alternate node.
                NeonNode alternate = router.selectExcluding(first.getId());
                if (alternate != null) {
                    try {
                        SqlExecutionResult retry = executor.execute(alternate, problem, candidateSql);
                        return retry;
                    } catch (Exception ex) {
                        alternate.recordFailure();
                        return new SqlExecutionResult("NODE_UNAVAILABLE", null,
                            "All Neon nodes unavailable", 0, alternate.getId());
                    } finally {
                        alternate.release(0);
                    }
                }
                return new SqlExecutionResult("NODE_UNAVAILABLE", null,
                    "No alternate Neon node available", result.getExecutionTimeMs(), first.getId());
            }
            return result;
        } finally {
            if (first != null) {
                first.release(0);
            }
        }
    }

    private SqlSubmission.Status mapExecStatus(String execStatus) {
        return switch (execStatus == null ? "RUNTIME_ERROR" : execStatus) {
            case "TIME_LIMIT_EXCEEDED" -> SqlSubmission.Status.TIME_LIMIT_EXCEEDED;
            case "SECURITY_VIOLATION" -> SqlSubmission.Status.SECURITY_VIOLATION;
            default -> SqlSubmission.Status.RUNTIME_ERROR;
        };
    }

    private void markRunning(Long submissionId) {
        try {
            submissionRepository.updateStatus(submissionId,
                List.of(SqlSubmission.Status.QUEUED), SqlSubmission.Status.RUNNING);
        } catch (Exception e) {
            log.warn("SQL judge: markRunning failed for submission {}: {}", submissionId, e.getMessage());
        }
    }

    private void finalize(Long submissionId, SqlSubmission.Status status, Long timeMs,
                          String error, String node) {
        try {
            submissionRepository.updateFinalized(submissionId,
                List.of(SqlSubmission.Status.QUEUED, SqlSubmission.Status.RUNNING),
                status, timeMs, node, error);
            SqlSubmission s = submissionRepository.findById(submissionId).orElse(null);
            if (s != null) {
                s.setStatus(status);
                s.setExecutionTimeMs(timeMs);
                s.setSelectedNode(node);
                s.setErrorMessage(error);
                s.setCompletedAt(TimeUtil.now());
                pushVerdict(s);
            }
        } catch (Exception e) {
            log.error("SQL judge finalize failed for {}: {}", submissionId, e.getMessage());
        }
    }

    private void savePreview(Long submissionId, SqlResult preview) {
        try {
            int previewRows = properties.getPreviewMaxRows();
            if (preview != null && preview.getRows() != null && preview.getRows().size() > previewRows) {
                preview.setRows(preview.getRows().subList(0, previewRows));
                preview.setTruncated(true);
            }
            String json = objectMapper.writeValueAsString(preview);
            submissionRepository.updateFinalizedPreview(submissionId, json);
        } catch (Exception e) {
            log.warn("SQL judge: failed to store preview for {}: {}", submissionId, e.getMessage());
        }
    }

    private void pushVerdict(SqlSubmission s) {
        try {
            SqlResult preview = null;
            if (s.getResultPreview() != null && !s.getResultPreview().isBlank()) {
                preview = objectMapper.readValue(s.getResultPreview(), SqlResult.class);
            }
            SqlVerdictEvent event = new SqlVerdictEvent(
                s.getId(), s.getStatus(), s.isTestRun(), s.getExecutionTimeMs(),
                s.getSelectedNode(), s.getErrorMessage(), preview, s.getCompletedAt());
            sseRegistry.sendEvent(s.getUserId(), "sql_verdict", event);
        } catch (Exception e) {
            log.warn("SQL judge: SSE push failed for sub {}: {}", s.getId(), e.getMessage());
        }
    }

    /**
     * Janitor — re-queue jobs stuck on processing lists after a grace period
     * (crashed worker / JVM died mid-process). Mirrors the code-judge pattern.
     */
    @Scheduled(fixedDelayString = "${SQL_JUDGE_RECLAIM_INTERVAL_MS:60000}")
    public void reclaimStuckJobs() {
        if (pool == null) return;
        try {
            java.util.Set<String> keys = redis.opsForSet().members(PROCESSING_REGISTRY);
            if (keys == null || keys.isEmpty()) return;

            long maxAgeMs = 5 * 60_000L;
            long now = System.currentTimeMillis();
            int reclaimed = 0;

            for (String procKey : keys) {
                List<String> jobs = redis.opsForList().range(procKey, 0, -1);
                if (jobs == null || jobs.isEmpty()) continue;

                for (String jobJson : new java.util.ArrayList<>(jobs)) {
                    try {
                        SqlJob job = objectMapper.readValue(jobJson, SqlJob.class);
                        SqlSubmission sub = submissionRepository.findById(job.getSubmissionId()).orElse(null);
                        if (sub == null) {
                            redis.opsForList().remove(procKey, 0, jobJson);
                            continue;
                        }
                        long claimMs;
                        try {
                            String raw = redis.opsForValue().get(CLAIM_KEY_PREFIX + job.getSubmissionId());
                            claimMs = (raw != null) ? Long.parseLong(raw) : 0L;
                        } catch (Exception ex) { claimMs = 0L; }
                        if (claimMs == 0L) {
                            claimMs = sub.getSubmittedAt() != null
                                ? java.sql.Timestamp.valueOf(sub.getSubmittedAt()).getTime() : now;
                        }
                        long age = now - claimMs;

                        SqlSubmission.Status s = sub.getStatus();
                        if (s != SqlSubmission.Status.QUEUED && s != SqlSubmission.Status.RUNNING) {
                            redis.opsForList().remove(procKey, 0, jobJson);
                            continue;
                        }
                        if (age > maxAgeMs) {
                            redis.opsForList().remove(procKey, 0, jobJson);
                            redis.opsForList().leftPush(QUEUE_KEY, jobJson);
                            reclaimed++;
                            log.warn("SQL judge: reclaimed stuck job {} (age {}ms)", job.getSubmissionId(), age);
                        }
                    } catch (Exception ex) {
                        log.warn("SQL judge: bad job in {}: {}", procKey, ex.getMessage());
                    }
                }
            }
            if (reclaimed > 0) log.info("SQL judge janitor reclaimed {} stuck job(s)", reclaimed);
        } catch (Exception e) {
            log.warn("SQL judge janitor failed: {}", e.getMessage());
        }
    }

    public int getActiveJobs() { return activeJobs.get(); }

    public Long getQueueDepth() {
        try { return redis.opsForList().size(QUEUE_KEY); }
        catch (Exception e) { return -1L; }
    }
}
