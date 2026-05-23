package com.example.codecombat2026.service;

import com.example.codecombat2026.dto.ExecutionResult;
import com.example.codecombat2026.entity.Submission;
import com.example.codecombat2026.service.judge.DockerJudgeService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Public compiler service — separate thread pool from judge.
 * Bounded queue + rejection policy ensures the server never crashes
 * under load (even 200+ concurrent users). Judge engine for contests
 * is unaffected.
 *
 * Tune via COMPILER_WORKERS / COMPILER_QUEUE_SIZE env vars.
 */
@Service
public class CompilerService {

    private static final Logger log = LoggerFactory.getLogger(CompilerService.class);

    @Value("${COMPILER_WORKERS:6}")
    private int workerCount;

    @Value("${COMPILER_QUEUE_SIZE:100}")
    private int queueSize;

    private static final java.util.concurrent.atomic.AtomicLong COMPILER_THREAD_SEQ = new java.util.concurrent.atomic.AtomicLong();

    @Autowired
    private DockerJudgeService judgeService;

    private ThreadPoolExecutor pool;
    private final AtomicInteger activeJobs = new AtomicInteger(0);

    @PostConstruct
    public void init() {
        // Bounded queue — rejects when full instead of OOM
        pool = new ThreadPoolExecutor(
            workerCount, workerCount,
            0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(queueSize),
            r -> {
                Thread t = new Thread(r);
                t.setName("compiler-" + COMPILER_THREAD_SEQ.incrementAndGet());
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.AbortPolicy() // throws RejectedExecutionException — caller catches it
        );
        log.info("✅ CompilerService ready — {} workers, {} queue capacity", workerCount, queueSize);
    }

    @PreDestroy
    public void shutdown() {
        if (pool != null) {
            pool.shutdown();
            try {
                if (!pool.awaitTermination(5, TimeUnit.SECONDS)) pool.shutdownNow();
            } catch (InterruptedException e) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Submit code for execution. Throws RejectedExecutionException if queue is full —
     * caller should return 503 Service Unavailable in that case.
     *
     * @param timeoutSeconds max wait for the result (queue + execute time)
     */
    public CompilerResponse compile(String code, String language, String stdin, int timeoutSeconds) {
        Submission.ProgrammingLanguage lang;
        try {
            lang = Submission.ProgrammingLanguage.valueOf(language.toUpperCase());
        } catch (IllegalArgumentException e) {
            return CompilerResponse.error("Unsupported language: " + language);
        }

        Future<CompilerResponse> future;
        try {
            future = pool.submit(() -> {
                activeJobs.incrementAndGet();
                long startMs = System.currentTimeMillis();
                try {
                    // 5 second hard timeout, 256 MB budget for the public compiler
                    ExecutionResult result = judgeService.execute(code, lang, 5.0, 256, stdin);
                    long elapsed = System.currentTimeMillis() - startMs;
                    return CompilerResponse.fromExecution(result, elapsed);
                } finally {
                    activeJobs.decrementAndGet();
                }
            });
        } catch (RejectedExecutionException e) {
            return CompilerResponse.error("Server busy — too many compile requests in queue. Try again in a few seconds.");
        }

        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            return CompilerResponse.error("Compile/execution timed out (>" + timeoutSeconds + "s).");
        } catch (Exception e) {
            return CompilerResponse.error("Internal error: " + e.getMessage());
        }
    }

    public int getActiveJobs() { return activeJobs.get(); }
    public int getQueueDepth() { return pool != null ? pool.getQueue().size() : 0; }

    // ─── Response DTO ─────────────────────────────────────────────────────────

    public static class CompilerResponse {
        public String stdout;
        public String stderr;
        public boolean success;
        public boolean compileError;
        public boolean timeLimitExceeded;
        public long executionTimeMs;
        public int exitCode;
        public String errorMessage;

        public static CompilerResponse fromExecution(ExecutionResult r, long elapsed) {
            CompilerResponse resp = new CompilerResponse();
            resp.stdout = r.getStdout() != null ? r.getStdout() : "";
            resp.stderr = r.getStderr() != null ? r.getStderr() : "";
            resp.compileError = r.isCompilationError();
            resp.timeLimitExceeded = r.isTimeLimitExceeded();
            resp.exitCode = r.getExitCode();
            resp.executionTimeMs = elapsed;
            resp.success = !resp.compileError && !resp.timeLimitExceeded && r.getExitCode() == 0;
            return resp;
        }

        public static CompilerResponse error(String msg) {
            CompilerResponse resp = new CompilerResponse();
            resp.success = false;
            resp.errorMessage = msg;
            resp.stdout = "";
            resp.stderr = "";
            return resp;
        }
    }
}
