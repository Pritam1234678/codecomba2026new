package com.example.codecombat2026.service;

import com.example.codecombat2026.dto.SubmissionJob;
import com.example.codecombat2026.dto.VerdictEvent;
import com.example.codecombat2026.dto.ExecutionResult;
import com.example.codecombat2026.entity.Submission;
import com.example.codecombat2026.repository.SubmissionRepository;
import com.example.codecombat2026.service.judge.DockerJudgeService;
import com.example.codecombat2026.util.TimeUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Background worker pool — consumes from Valkey submission queue.
 *
 * Durability model:
 *   - Producer (SubmissionService) LPUSHes a job onto {@code submission:queue}.
 *   - Worker uses {@code LMOVE submission:queue submission:processing:&lt;workerId&gt;}
 *     to atomically claim a job. If the worker crashes mid-processing, the
 *     job stays on the processing list and is requeued by
 *     {@link #reclaimStuckJobs()} after a grace period.
 *   - On finalize, the worker LREMs the job from its processing list.
 *
 * 100 concurrent users:
 *   8 workers × ~5s per job = ~62s max wait for last user
 *   Queue is durable in Valkey — no jobs lost on crash
 *
 * Tune via JUDGE_WORKERS env var (default 8).
 */
@Component
public class SubmissionWorkerPool {

    private static final Logger log = LoggerFactory.getLogger(SubmissionWorkerPool.class);

    public static final String QUEUE_KEY = "submission:queue";
    /** Per-worker processing list prefix: submission:processing:&lt;hostPid&gt;:&lt;workerId&gt; */
    public static final String PROCESSING_KEY_PREFIX = "submission:processing:";
    /** Tracks all known processing list keys so the janitor knows where to look. */
    public static final String PROCESSING_REGISTRY = "submission:processing:registry";

    @Value("${JUDGE_WORKERS:8}")
    private int workerCount;

    /** Wall-clock minutes after which an unfinalised processing-list job is considered stuck. */
    @Value("${JUDGE_STUCK_JOB_TIMEOUT_MINUTES:5}")
    private int stuckJobTimeoutMinutes;

    private final AtomicInteger activeJobs    = new AtomicInteger(0);
    private final AtomicInteger totalProcessed = new AtomicInteger(0);

    private ThreadPoolExecutor pool;
    private volatile boolean shuttingDown = false;

    /** Stable identity for this JVM instance — survives restarts only if PID is reused (rare). */
    private String instanceId;

    private static final java.util.concurrent.atomic.AtomicLong WORKER_THREAD_SEQ = new java.util.concurrent.atomic.AtomicLong();

    @Autowired private StringRedisTemplate redis; // API pool — for queue push/status
    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("workerRedisTemplate")
    private StringRedisTemplate workerRedis; // Worker pool — for blocking ops (long timeout)
    @Autowired private ObjectMapper objectMapper;
    @Autowired private DockerJudgeService judgeService;
    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private LeaderboardCacheService leaderboard;
    @Autowired private SseEmitterRegistry sseRegistry;
    @Autowired private CacheService cacheService;

    @PostConstruct
    public void startWorkers() {
        instanceId = ManagementFactory.getRuntimeMXBean().getName(); // pid@host
        pool = new ThreadPoolExecutor(
            workerCount, workerCount,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            r -> {
                Thread t = new Thread(r);
                t.setName("judge-worker-" + WORKER_THREAD_SEQ.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        );

        for (int i = 0; i < workerCount; i++) {
            final int idx = i;
            pool.submit(() -> workerLoop(idx));
        }
        log.info("✅ Started {} judge workers (instance={}, queue={}) — ~{} jobs/min max",
            workerCount, instanceId, QUEUE_KEY, workerCount * 10);
    }

    @jakarta.annotation.PreDestroy
    public void shutdown() {
        shuttingDown = true;
        if (pool == null) return;
        log.info("Submission worker pool shutting down — draining in-flight jobs...");
        pool.shutdown();
        try {
            if (!pool.awaitTermination(15, TimeUnit.SECONDS)) {
                log.warn("Workers did not finish in 15s — force shutdown");
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
        // Register this processing list so the janitor can scan it later
        try { redis.opsForSet().add(PROCESSING_REGISTRY, procKey); }
        catch (Exception ignored) {}

        while (!Thread.currentThread().isInterrupted() && !shuttingDown) {
            try {
                // Atomic claim: pop from main queue's RIGHT, push onto processing
                // list's LEFT (so newest claims are at head — easier to reason about).
                String jobJson = workerRedis.opsForList()
                        .move(QUEUE_KEY, org.springframework.data.redis.connection.RedisListCommands.Direction.RIGHT,
                              procKey,   org.springframework.data.redis.connection.RedisListCommands.Direction.LEFT,
                              java.time.Duration.ofSeconds(3));

                if (jobJson == null) continue;

                SubmissionJob job = objectMapper.readValue(jobJson, SubmissionJob.class);
                activeJobs.incrementAndGet();
                log.debug("[{}] claimed job {} (active={}, queue={})",
                    Thread.currentThread().getName(), job.getSubmissionId(),
                    activeJobs.get(), getQueueDepth());

                try {
                    processJob(job);
                } finally {
                    activeJobs.decrementAndGet();
                    totalProcessed.incrementAndGet();
                    // ACK: remove the in-flight job from this worker's processing list.
                    // count=0 removes all matches; we only ever have one match per JSON.
                    try {
                        redis.opsForList().remove(procKey, 0, jobJson);
                    } catch (Exception e) {
                        log.warn("Failed to LREM processed job from {}: {}", procKey, e.getMessage());
                    }
                }

            } catch (Exception e) {
                if (Thread.currentThread().isInterrupted() || shuttingDown) break;
                log.error("Worker {} error: {}", Thread.currentThread().getName(), e.getMessage());
                try { Thread.sleep(500); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * Janitor — scans all known processing lists for jobs that are still
     * sitting there long after they should have been finalised. These are
     * either crashed workers (JVM died mid-process) or jobs whose finalize
     * silently failed. Re-enqueues them onto the main queue so another worker
     * can retry, and marks the DB row as RE if it's still PENDING/JUDGING
     * after multiple reclaim cycles.
     *
     * Runs every minute. Cheap — most processing lists are empty.
     */
    @Scheduled(fixedDelayString = "${JUDGE_RECLAIM_INTERVAL_MS:60000}")
    public void reclaimStuckJobs() {
        try {
            java.util.Set<String> keys = redis.opsForSet().members(PROCESSING_REGISTRY);
            if (keys == null || keys.isEmpty()) return;

            long maxAgeMs = stuckJobTimeoutMinutes * 60_000L;
            long now = System.currentTimeMillis();
            int reclaimed = 0;

            for (String procKey : keys) {
                List<String> jobs = redis.opsForList().range(procKey, 0, -1);
                if (jobs == null || jobs.isEmpty()) continue;

                // Inspect each job — its submittedAt is a coarse age proxy
                for (String jobJson : new ArrayList<>(jobs)) {
                    try {
                        SubmissionJob job = objectMapper.readValue(jobJson, SubmissionJob.class);
                        Submission sub = submissionRepository.findById(job.getSubmissionId()).orElse(null);
                        if (sub == null) {
                            // Submission was deleted — drop the orphan
                            redis.opsForList().remove(procKey, 0, jobJson);
                            continue;
                        }
                        long submittedMs = sub.getSubmittedAt() != null
                            ? java.sql.Timestamp.valueOf(sub.getSubmittedAt()).getTime() : now;
                        long age = now - submittedMs;

                        // Final verdict already written → safe to drop from processing list
                        Submission.SubmissionStatus s = sub.getStatus();
                        if (s != Submission.SubmissionStatus.PENDING && s != Submission.SubmissionStatus.JUDGING) {
                            redis.opsForList().remove(procKey, 0, jobJson);
                            continue;
                        }

                        if (age > maxAgeMs) {
                            // Stuck — remove from this processing list and push back to the main
                            // queue. Another worker will re-process it. The submission row stays
                            // PENDING/JUDGING; the next worker resets it.
                            redis.opsForList().remove(procKey, 0, jobJson);
                            redis.opsForList().leftPush(QUEUE_KEY, jobJson);
                            reclaimed++;
                            log.warn("Reclaimed stuck job {} (age {}ms) from {}",
                                job.getSubmissionId(), age, procKey);
                        }
                    } catch (Exception ex) {
                        log.warn("Janitor: bad job in {}: {}", procKey, ex.getMessage());
                    }
                }
            }
            if (reclaimed > 0) log.info("Janitor reclaimed {} stuck job(s)", reclaimed);
        } catch (Exception e) {
            log.warn("Janitor failed: {}", e.getMessage());
        }
    }

    private void processJob(SubmissionJob job) {
        Long submissionId = job.getSubmissionId();
        long startMs = System.currentTimeMillis();
        boolean finalized = false;

        try {
            if (!job.isTestRun() && submissionId != null && submissionId > 0) {
                submissionRepository.updateStatus(submissionId, Submission.SubmissionStatus.JUDGING);
            }

            String harness = cacheService.getSnippetHarness(job.getProblemId(), job.getLanguage());
            if (harness == null) {
                finalizeAndNotify(job, submissionId,
                    Submission.SubmissionStatus.CE,
                    "No code harness configured for language: " + job.getLanguage(),
                    0, 0, 0, 0L, "[]");
                finalized = true;
                return;
            }

            String executableCode = injectUserCode(harness, job.getCode(), job.getLanguage());

            Submission.ProgrammingLanguage lang =
                Submission.ProgrammingLanguage.valueOf(job.getLanguage());
            double timeLimit = job.getTimeLimit() != null ? job.getTimeLimit() : 5.0;
            int memoryLimit  = job.getMemoryLimit() != null ? job.getMemoryLimit() : 256;
            ExecutionResult result = judgeService.execute(executableCode, lang, timeLimit, memoryLimit, null);

            ParsedResult parsed = parseOutput(result);

            finalizeAndNotify(job, submissionId, parsed.status, parsed.errorMessage,
                parsed.passed, parsed.total, parsed.score, result.getTimeTaken(), parsed.details);
            finalized = true;

            log.info("{} {} → {} ({}/{}) in {}ms",
                job.isTestRun() ? "TestRun" : "Submission",
                submissionId, parsed.status, parsed.passed, parsed.total,
                System.currentTimeMillis() - startMs);

        } catch (Exception e) {
            log.error("Job {} failed: {}", submissionId, e.getMessage(), e);
            try {
                finalizeAndNotify(job, submissionId,
                    Submission.SubmissionStatus.RE,
                    e.getMessage() != null ? e.getMessage() : "Internal judge error",
                    0, 0, 0, 0L, "[]");
                finalized = true;
            } catch (Exception inner) {
                log.error("Catastrophic: even RE finalize failed for {}: {}", submissionId, inner.getMessage());
            }
        } finally {
            if (!finalized && submissionId != null && submissionId > 0) {
                try {
                    submissionRepository.updateResult(
                        submissionId, Submission.SubmissionStatus.RE,
                        "Judge worker terminated unexpectedly",
                        0, 0, 0.0, 0, "[]", TimeUtil.now()
                    );
                } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Finalize a job: update DB, leaderboard, push SSE.
     * Each section is wrapped in try/catch so one failure doesn't lose the verdict.
     *
     * Idempotency: leaderboard increment is keyed off the submission row, not
     * a counter — replays will set the same final score, not stack. The DB
     * update is overwriting all result fields so re-running is safe.
     */
    void finalizeAndNotify(SubmissionJob job, Long submissionId,
                           Submission.SubmissionStatus status, String errorMessage,
                           int passed, int total, int score, long timeMs, String details) {

        // 1. Update DB — polling needs this. Wrap in try so it never throws.
        if (submissionId != null && submissionId > 0) {
            try {
                submissionRepository.updateResult(
                    submissionId, status, errorMessage, passed, total,
                    (double) timeMs, score, details, TimeUtil.now()
                );
            } catch (Exception e) {
                log.error("DB update failed for submission {}: {}", submissionId, e.getMessage());
            }
        }

        // 2. Leaderboard — only for real successful submissions
        if (!job.isTestRun() && status == Submission.SubmissionStatus.AC && job.getContestId() != null) {
            try {
                leaderboard.updateScore(job.getContestId(), job.getUserId(), score);
            } catch (Exception e) {
                log.warn("Leaderboard update failed: {}", e.getMessage());
            }
        }

        // 3. Push verdict to user's open SSE connection (if any)
        try {
            sseRegistry.sendVerdict(job.getUserId(), new VerdictEvent(
                submissionId, status.name(), passed, total, score, timeMs, errorMessage, details,
                job.isTestRun()
            ));
        } catch (Exception e) {
            log.warn("SSE push failed for user {}: {}", job.getUserId(), e.getMessage());
        }
    }

    // ─── Monitoring ───────────────────────────────────────────────────────────

    public int getActiveJobs()     { return activeJobs.get(); }
    public int getTotalProcessed() { return totalProcessed.get(); }
    public Long getQueueDepth() {
        try { return redis.opsForList().size(QUEUE_KEY); }
        catch (Exception e) { return -1L; }
    }
    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String injectUserCode(String harness, String userCode, String language) {
        boolean isPython = "PYTHON".equalsIgnoreCase(language);
        String startMarker = isPython ? "# USER_CODE_START" : "// USER_CODE_START";
        String endMarker   = isPython ? "# USER_CODE_END"   : "// USER_CODE_END";

        int startIdx = harness.indexOf(startMarker);
        int endIdx   = harness.indexOf(endMarker);

        if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
            return harness.substring(0, startIdx + startMarker.length())
                + "\n" + userCode + "\n"
                + harness.substring(endIdx);
        }
        return harness
            .replace("// USER_CODE_PLACEHOLDER", userCode)
            .replace("# USER_CODE_PLACEHOLDER", userCode)
            .replace("/* USER_CODE_PLACEHOLDER */", userCode);
    }

    private ParsedResult parseOutput(ExecutionResult result) {
        if (result.isCompilationError()) {
            return new ParsedResult(Submission.SubmissionStatus.CE, result.getStderr(), 0, 0, 0, "[]");
        }
        if (result.isTimeLimitExceeded()) {
            return new ParsedResult(Submission.SubmissionStatus.TLE, "Time Limit Exceeded", 0, 0, 0, "[]");
        }
        if (result.isMemoryLimitExceeded()) {
            return new ParsedResult(Submission.SubmissionStatus.MLE, "Memory Limit Exceeded", 0, 0, 0, "[]");
        }

        String stdout = result.getStdout() != null ? result.getStdout() : "";
        List<TcLine> lines = parseTcLines(stdout);

        if (lines.isEmpty() && result.getExitCode() != 0) {
            return new ParsedResult(Submission.SubmissionStatus.RE, result.getStderr(), 0, 0, 0, "[]");
        }
        if (lines.isEmpty()) {
            return new ParsedResult(Submission.SubmissionStatus.RE,
                "Harness produced no output. " + result.getStderr(), 0, 0, 0, "[]");
        }

        int total  = lines.size();
        int passed = (int) lines.stream().filter(l -> l.passed).count();
        int score  = total > 0 ? (int) Math.round((passed * 100.0) / total) : 0;

        StringBuilder details = new StringBuilder("[");
        for (int i = 0; i < lines.size(); i++) {
            TcLine tc = lines.get(i);
            if (i > 0) details.append(",");
            details.append(String.format(
                "{\"testCase\":%d,\"status\":\"%s\",\"hidden\":%b",
                tc.number, tc.passed ? "PASS" : "FAIL", tc.hidden));
            if (!tc.hidden && !tc.passed) {
                if (tc.input != null) details.append(",\"input\":\"").append(escapeJson(tc.input)).append("\"");
                if (tc.expected != null) details.append(",\"expected\":\"").append(escapeJson(tc.expected)).append("\"");
                if (tc.got != null) details.append(",\"got\":\"").append(escapeJson(tc.got)).append("\"");
            }
            details.append("}");
        }
        details.append("]");

        Submission.SubmissionStatus status = passed == total
            ? Submission.SubmissionStatus.AC
            : Submission.SubmissionStatus.WA;

        return new ParsedResult(status, null, passed, total, score, details.toString());
    }

    private List<TcLine> parseTcLines(String stdout) {
        List<TcLine> results = new ArrayList<>();
        for (String line : stdout.split("\\r?\\n")) {
            line = line.trim();
            if (!line.startsWith("TC:")) continue;
            String[] parts = line.split(":", 6);
            if (parts.length < 3) continue;
            try {
                int number  = Integer.parseInt(parts[1]);
                boolean ok  = "PASS".equalsIgnoreCase(parts[2]);
                boolean hid = false;
                String input = null;
                String expected = null;
                String got = null;

                for (int p = 3; p < parts.length; p++) {
                    String part = parts[p];
                    if ("hidden".equalsIgnoreCase(part)) {
                        hid = true;
                    } else if (part.startsWith("input=")) {
                        input = part.substring(6);
                    } else if (part.startsWith("expected=")) {
                        expected = part.substring(9);
                    } else if (part.startsWith("got=")) {
                        got = part.substring(4);
                    }
                }
                results.add(new TcLine(number, ok, hid, input, expected, got));
            } catch (NumberFormatException ignored) {}
        }
        return results;
    }

    private static class TcLine {
        final int number; final boolean passed; final boolean hidden;
        final String input; final String expected; final String got;
        TcLine(int n, boolean p, boolean h, String input, String expected, String got) {
            number=n; passed=p; hidden=h; this.input=input; this.expected=expected; this.got=got;
        }
    }

    private static class ParsedResult {
        final Submission.SubmissionStatus status;
        final String errorMessage;
        final int passed, total, score;
        final String details;
        ParsedResult(Submission.SubmissionStatus s, String e, int p, int t, int sc, String d) {
            status=s; errorMessage=e; passed=p; total=t; score=sc; details=d;
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
