package com.example.codecombat2026.service;

import com.example.codecombat2026.dto.ExecutionResult;
import com.example.codecombat2026.dto.VerdictEvent;
import com.example.codecombat2026.dto.WebContestJob;
import com.example.codecombat2026.entity.Submission;
import com.example.codecombat2026.repository.SubmissionRepository;
import com.example.codecombat2026.service.judge.SandboxRunner;
import com.example.codecombat2026.util.TimeUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Background worker pool for web contest jobs.
 *
 * Drains from "web-contest:queue" — each job copies a template project,
 * overwrites editable files with user code, runs `mvn test` inside a bwrap
 * sandbox, and parses TC:N:PASS/FAIL output (same format as the main judge).
 */
@Component
public class WebContestWorkerPool {

    private static final Logger log = LoggerFactory.getLogger(WebContestWorkerPool.class);

    public static final String QUEUE_KEY = "web-contest:queue";
    private static final String TEMP_BASE = System.getProperty("java.io.tmpdir") + "/web-exec";

    /**
     * RLIMIT_AS padding for Java — the JVM needs ~4 GB of virtual address space
     * just to start (compressed class space, code cache, GC reserves, thread stacks).
     */
    private static final int JAVA_AS_PADDING_MB = 4096;

    @Value("${WEB_CONTEST_WORKERS:4}")
    private int workerCount;

    private final AtomicInteger activeJobs = new AtomicInteger(0);
    private final AtomicInteger totalProcessed = new AtomicInteger(0);

    private ThreadPoolExecutor pool;
    private volatile boolean shuttingDown = false;

    private static final java.util.concurrent.atomic.AtomicLong THREAD_SEQ = new java.util.concurrent.atomic.AtomicLong();

    @Autowired private StringRedisTemplate redis;
    @Autowired @Qualifier("workerRedisTemplate") private StringRedisTemplate workerRedis;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private LeaderboardCacheService leaderboard;
    @Autowired private SseEmitterRegistry sseRegistry;
    @Autowired private SandboxRunner sandbox;

    @PostConstruct
    public void startWorkers() {
        pool = new ThreadPoolExecutor(
            workerCount, workerCount,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            r -> {
                Thread t = new Thread(r);
                t.setName("web-contest-worker-" + THREAD_SEQ.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        );

        for (int i = 0; i < workerCount; i++) {
            pool.submit(this::workerLoop);
        }
        log.info("✅ Started {} web contest workers (queue={})", workerCount, QUEUE_KEY);
    }

    @jakarta.annotation.PreDestroy
    public void shutdown() {
        shuttingDown = true;
        if (pool == null) return;
        log.info("Web contest worker pool shutting down...");
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

    private void workerLoop() {
        while (!Thread.currentThread().isInterrupted() && !shuttingDown) {
            try {
                // Blocking pop from the queue (3s timeout to check shutdown flag)
                String jobJson = workerRedis.opsForList().rightPop(QUEUE_KEY, Duration.ofSeconds(3));
                if (jobJson == null) continue;

                WebContestJob job = objectMapper.readValue(jobJson, WebContestJob.class);
                activeJobs.incrementAndGet();

                try {
                    processJob(job);
                } finally {
                    activeJobs.decrementAndGet();
                    totalProcessed.incrementAndGet();
                }

            } catch (Exception e) {
                if (Thread.currentThread().isInterrupted() || shuttingDown) break;
                log.error("Web contest worker error: {}", e.getMessage());
                try { Thread.sleep(500); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void processJob(WebContestJob job) {
        Long submissionId = job.getSubmissionId();
        long startMs = System.currentTimeMillis();
        Path workDir = null;

        try {
            // Mark as JUDGING
            if (submissionId != null && submissionId > 0) {
                submissionRepository.updateStatus(submissionId, Submission.SubmissionStatus.JUDGING);
            }

            // 1. Copy template to temp dir
            String uuid = UUID.randomUUID().toString().substring(0, 12);
            workDir = Paths.get(TEMP_BASE, uuid);
            Path templateDir = Paths.get(job.getTemplatePath());

            if (!Files.isDirectory(templateDir)) {
                finalizeAndNotify(job, Submission.SubmissionStatus.RE,
                    "Template not found: " + job.getTemplatePath(),
                    0, 0, 0, 0L, "[]");
                return;
            }

            copyDirectory(templateDir, workDir);

            // 2. Overwrite editable files with user's code
            if (job.getEditableFiles() != null) {
                for (Map.Entry<String, String> entry : job.getEditableFiles().entrySet()) {
                    Path targetFile = workDir.resolve(entry.getKey());
                    Files.createDirectories(targetFile.getParent());
                    Files.writeString(targetFile, entry.getValue());
                }
            }

            // 3. Execute: mvn test inside bwrap sandbox
            ExecutionResult result = executeMvnTest(workDir, job);

            // 4. Parse TC:N:PASS/FAIL output
            ParsedResult parsed = parseOutput(result, job.isTestRun());

            // 5. Finalize
            finalizeAndNotify(job, parsed.status, parsed.errorMessage,
                parsed.passed, parsed.total, parsed.score,
                System.currentTimeMillis() - startMs, parsed.details);

            log.info("WebContest {} {} → {} ({}/{}) in {}ms",
                job.isTestRun() ? "run" : "submit", submissionId,
                parsed.status, parsed.passed, parsed.total,
                System.currentTimeMillis() - startMs);

        } catch (Exception e) {
            log.error("Web contest job {} failed: {}", submissionId, e.getMessage(), e);
            try {
                finalizeAndNotify(job, Submission.SubmissionStatus.RE,
                    e.getMessage() != null ? e.getMessage() : "Internal error",
                    0, 0, 0, 0L, "[]");
            } catch (Exception inner) {
                log.error("Catastrophic failure for web contest job {}: {}", submissionId, inner.getMessage());
            }
        } finally {
            // 8. Cleanup temp dir
            if (workDir != null) {
                cleanup(workDir);
            }
        }
    }

    private ExecutionResult executeMvnTest(Path workDir, WebContestJob job) throws Exception {
        double timeLimit = job.getTimeLimit() != null ? job.getTimeLimit() : 60.0;
        int memoryMb = job.getMemoryLimit() != null ? job.getMemoryLimit() : 512;

        // Total RLIMIT_AS: user memory + JVM runtime overhead
        int totalAsMb = memoryMb + JAVA_AS_PADDING_MB;
        int cpuSeconds = (int) Math.ceil(timeLimit) + 5; // extra buffer for Maven startup

        List<String> command = List.of(
            "mvn", "test", "-o", "-q",
            "-Dsurefire.useFile=false",
            "-Dspring.main.banner-mode=off"
        );

        // Use sandbox with network disabled, generous limits for Maven
        SandboxRunner.SandboxLimits limits = SandboxRunner.SandboxLimits.forRun(totalAsMb, cpuSeconds);

        List<String> wrapped = sandbox.wrap(command, workDir, limits);
        ProcessBuilder pb = new ProcessBuilder(wrapped);
        pb.directory(workDir.toFile());
        pb.redirectErrorStream(false);

        long startMs = System.currentTimeMillis();
        Process process = pb.start();

        // Close stdin immediately
        try { process.getOutputStream().close(); } catch (IOException ignored) {}

        // Read stdout/stderr concurrently
        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();

        Thread stdoutReader = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    synchronized (stdout) { stdout.append(line).append("\n"); }
                }
            } catch (IOException ignored) {}
        }, "web-contest-stdout");
        stdoutReader.setDaemon(true);

        Thread stderrReader = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    synchronized (stderr) { stderr.append(line).append("\n"); }
                }
            } catch (IOException ignored) {}
        }, "web-contest-stderr");
        stderrReader.setDaemon(true);

        stdoutReader.start();
        stderrReader.start();

        int timeoutSec = (int) timeLimit + 10; // wall clock buffer
        boolean finished = process.waitFor(timeoutSec, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - startMs;

        if (!finished) {
            process.destroyForcibly();
            process.waitFor(2, TimeUnit.SECONDS);
            stdoutReader.join(500);
            stderrReader.join(500);
            return new ExecutionResult(stdout.toString(), "Time Limit Exceeded",
                elapsed, 0, 1, true, false, false);
        }

        stdoutReader.join(1000);
        stderrReader.join(1000);

        int exitCode = process.exitValue();

        // Check for OOM kill
        if (exitCode == 137) {
            return new ExecutionResult(stdout.toString(), "Memory Limit Exceeded",
                elapsed, 0, exitCode, false, true, false);
        }

        // Compilation error detection: Maven exits with 1 on compile failure
        String stderrStr = stderr.toString();
        if (exitCode != 0 && (stderrStr.contains("COMPILATION ERROR") ||
                              stderrStr.contains("[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin"))) {
            return new ExecutionResult(stdout.toString(), stderrStr,
                elapsed, 0, exitCode, false, false, true);
        }

        return new ExecutionResult(stdout.toString(), stderrStr,
            elapsed, 0, exitCode, false, false, false);
    }

    /**
     * Finalize: update DB, leaderboard delta, push SSE verdict.
     */
    private void finalizeAndNotify(WebContestJob job, Submission.SubmissionStatus status,
                                   String errorMessage, int passed, int total, int score,
                                   long timeMs, String details) {
        Long submissionId = job.getSubmissionId();

        // 1. Update DB
        if (submissionId != null && submissionId > 0) {
            try {
                submissionRepository.updateResult(
                    submissionId, status, errorMessage, passed, total,
                    (double) timeMs, score, details, TimeUtil.now()
                );
            } catch (Exception e) {
                log.error("DB update failed for web contest submission {}: {}", submissionId, e.getMessage());
            }
        }

        // 2. Leaderboard update (only for real submits with a contest)
        if (!job.isTestRun() && job.getContestId() != null) {
            String problemScoreKey = "contest:score:" + job.getContestId()
                + ":" + job.getUserId() + ":" + job.getProblemId();
            try {
                String prevStr = redis.opsForValue().get(problemScoreKey);
                int prevScore = (prevStr != null) ? Integer.parseInt(prevStr) : 0;
                int delta = score - prevScore;

                redis.opsForValue().set(problemScoreKey, String.valueOf(score),
                    Duration.ofHours(26));

                if (delta != 0) {
                    leaderboard.updateScore(job.getContestId(), job.getUserId(), delta);
                    log.debug("Web contest leaderboard delta={} user={} problem={} contest={}",
                        delta, job.getUserId(), job.getProblemId(), job.getContestId());
                }
            } catch (Exception e) {
                log.warn("Web contest leaderboard update failed: {}", e.getMessage());
            }
        }

        // 3. Invalidate caches
        try {
            redis.delete("submissions:user:" + job.getUserId());
            redis.delete("submission:status:" + submissionId);
            redis.delete("submission:user:problem:" + job.getUserId() + ":" + job.getProblemId());
        } catch (Exception ignored) {}

        // 4. Push SSE verdict
        try {
            sseRegistry.sendVerdict(job.getUserId(), new VerdictEvent(
                submissionId, status.name(), passed, total, score, timeMs, errorMessage, details,
                job.isTestRun()
            ));
        } catch (Exception e) {
            log.warn("SSE push failed for web contest user {}: {}", job.getUserId(), e.getMessage());
        }
    }

    // ─── Output parsing (same TC:N:PASS/FAIL format as SubmissionWorkerPool) ──

    private ParsedResult parseOutput(ExecutionResult result, boolean isTestRun) {
        if (result.isCompilationError()) {
            String err = result.getStderr();
            if (err != null && err.length() > 2000) err = err.substring(0, 2000) + "...";
            return new ParsedResult(Submission.SubmissionStatus.CE, err, 0, 0, 0, "[]");
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
            String err = result.getStderr();
            if (err != null && err.length() > 2000) err = err.substring(0, 2000) + "...";
            return new ParsedResult(Submission.SubmissionStatus.RE, err, 0, 0, 0, "[]");
        }
        if (lines.isEmpty()) {
            return new ParsedResult(Submission.SubmissionStatus.RE,
                "Tests produced no output. " + result.getStderr(), 0, 0, 0, "[]");
        }

        int total = lines.size();
        int passed = (int) lines.stream().filter(l -> l.passed).count();
        int score = total > 0 ? (int) Math.round((passed * 100.0) / total) : 0;

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
            String[] parts = line.split(":", 4);
            if (parts.length < 3) continue;
            try {
                int number = Integer.parseInt(parts[1]);
                boolean ok = "PASS".equalsIgnoreCase(parts[2]);
                boolean hidden = parts.length >= 4 && "hidden".equalsIgnoreCase(parts[3]);
                results.add(new TcLine(number, ok, hidden));
            } catch (NumberFormatException ignored) {}
        }
        return results;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source).forEach(src -> {
            try {
                Path dest = target.resolve(source.relativize(src));
                if (Files.isDirectory(src)) {
                    Files.createDirectories(dest);
                } else {
                    Files.createDirectories(dest.getParent());
                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private void cleanup(Path workDir) {
        try {
            if (Files.exists(workDir)) {
                Files.walk(workDir)
                    .sorted(java.util.Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
        } catch (Exception e) {
            log.warn("Cleanup failed for {}: {}", workDir, e.getMessage());
        }
    }

    // ─── Monitoring ───────────────────────────────────────────────────────────

    public int getActiveJobs() { return activeJobs.get(); }
    public int getTotalProcessed() { return totalProcessed.get(); }
    public Long getQueueDepth() {
        try { return redis.opsForList().size(QUEUE_KEY); }
        catch (Exception e) { return -1L; }
    }

    // ─── Inner classes ────────────────────────────────────────────────────────

    private static class TcLine {
        final int number;
        final boolean passed;
        final boolean hidden;
        TcLine(int n, boolean p, boolean h) { number = n; passed = p; hidden = h; }
    }

    private static class ParsedResult {
        final Submission.SubmissionStatus status;
        final String errorMessage;
        final int passed, total, score;
        final String details;
        ParsedResult(Submission.SubmissionStatus s, String e, int p, int t, int sc, String d) {
            status = s; errorMessage = e; passed = p; total = t; score = sc; details = d;
        }
    }
}
