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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Background worker pool — consumes from Valkey submission queue.
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

    @Value("${JUDGE_WORKERS:8}")
    private int workerCount;

    private final AtomicInteger activeJobs    = new AtomicInteger(0);
    private final AtomicInteger totalProcessed = new AtomicInteger(0);

    @Autowired private StringRedisTemplate redis; // API pool — for queue push/status
    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("workerRedisTemplate")
    private StringRedisTemplate workerRedis; // Worker pool — for BRPOP (long timeout)
    @Autowired private ObjectMapper objectMapper;
    @Autowired private DockerJudgeService judgeService;
    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private LeaderboardCacheService leaderboard;
    @Autowired private SseEmitterRegistry sseRegistry;
    @Autowired private CacheService cacheService;

    @PostConstruct
    public void startWorkers() {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
            workerCount, workerCount,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            r -> {
                Thread t = new Thread(r);
                t.setName("judge-worker-" + t.getId());
                t.setDaemon(true);
                return t;
            }
        );

        for (int i = 0; i < workerCount; i++) {
            pool.submit(this::workerLoop);
        }
        log.info("✅ Started {} judge workers on queue '{}' — ~{} jobs/min max",
            workerCount, QUEUE_KEY, workerCount * 10);
    }

    private void workerLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // BRPOP blocks for 3s — uses workerRedis (10s timeout pool)
                // so API requests on main redis pool are NOT blocked
                String jobJson = workerRedis.opsForList()
                        .rightPop(QUEUE_KEY, java.time.Duration.ofSeconds(3));

                if (jobJson == null) continue; // timeout, loop again

                SubmissionJob job = objectMapper.readValue(jobJson, SubmissionJob.class);
                activeJobs.incrementAndGet();
                log.debug("[{}] job {} (active={}, queue={})",
                    Thread.currentThread().getName(), job.getSubmissionId(),
                    activeJobs.get(), getQueueDepth());

                try {
                    processJob(job);
                } finally {
                    activeJobs.decrementAndGet();
                    totalProcessed.incrementAndGet();
                }

            } catch (Exception e) {
                if (Thread.currentThread().isInterrupted()) break;
                log.error("Worker {} error: {}", Thread.currentThread().getName(), e.getMessage());
                try { Thread.sleep(500); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void processJob(SubmissionJob job) {
        Long submissionId = job.getSubmissionId();
        long startMs = System.currentTimeMillis();

        try {
            submissionRepository.updateStatus(submissionId, Submission.SubmissionStatus.JUDGING);

            String harness = cacheService.getSnippetHarness(job.getProblemId(), job.getLanguage());
            if (harness == null) {
                finalizeAndNotify(job, submissionId,
                    Submission.SubmissionStatus.CE,
                    "No code harness configured for language: " + job.getLanguage(),
                    0, 0, 0, 0L, "[]");
                return;
            }

            String executableCode = injectUserCode(harness, job.getCode(), job.getLanguage());

            Submission.ProgrammingLanguage lang =
                Submission.ProgrammingLanguage.valueOf(job.getLanguage());
            double timeLimit = job.getTimeLimit() != null ? job.getTimeLimit() : 5.0;
            ExecutionResult result = judgeService.execute(executableCode, lang, timeLimit);

            ParsedResult parsed = parseOutput(result);

            finalizeAndNotify(job, submissionId, parsed.status, parsed.errorMessage,
                parsed.passed, parsed.total, parsed.score, result.getTimeTaken(), parsed.details);

            log.info("Submission {} → {} ({}/{}) in {}ms",
                submissionId, parsed.status, parsed.passed, parsed.total,
                System.currentTimeMillis() - startMs);

        } catch (Exception e) {
            log.error("Job {} failed: {}", submissionId, e.getMessage());
            finalizeAndNotify(job, submissionId,
                Submission.SubmissionStatus.RE, e.getMessage(),
                0, 0, 0, 0L, "[]");
        }
    }

    @Transactional
    private void finalizeAndNotify(SubmissionJob job, Long submissionId,
                                   Submission.SubmissionStatus status, String errorMessage,
                                   int passed, int total, int score, long timeMs, String details) {
        submissionRepository.updateResult(
            submissionId, status, errorMessage, passed, total,
            (double) timeMs, score, details, TimeUtil.now()
        );

        if (status == Submission.SubmissionStatus.AC && job.getContestId() != null) {
            leaderboard.updateScore(job.getContestId(), job.getUserId(), score);
        }

        sseRegistry.sendVerdict(job.getUserId(), new VerdictEvent(
            submissionId, status.name(), passed, total, score, timeMs, errorMessage, details
        ));
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
                "{\"testCase\":%d,\"status\":\"%s\",\"hidden\":%b}",
                tc.number, tc.passed ? "PASS" : "FAIL", tc.hidden));
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
            String[] parts = line.split(":");
            if (parts.length < 3) continue;
            try {
                int number  = Integer.parseInt(parts[1]);
                boolean ok  = "PASS".equalsIgnoreCase(parts[2]);
                boolean hid = parts.length >= 4 && "hidden".equalsIgnoreCase(parts[3]);
                results.add(new TcLine(number, ok, hid));
            } catch (NumberFormatException ignored) {}
        }
        return results;
    }

    private static class TcLine {
        final int number; final boolean passed; final boolean hidden;
        TcLine(int n, boolean p, boolean h) { number=n; passed=p; hidden=h; }
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
}
