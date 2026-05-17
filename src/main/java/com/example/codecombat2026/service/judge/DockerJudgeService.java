package com.example.codecombat2026.service.judge;

import com.example.codecombat2026.dto.ExecutionResult;
import com.example.codecombat2026.entity.Submission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Executes user code in isolated OS processes (not Docker containers despite the name).
 *
 * Each execution:
 *   1. Writes source to a temp directory
 *   2. Compiles (Java/C/C++) or skips (Python/JS)
 *   3. Runs with a time limit
 *   4. Reads stdout/stderr via a shared thread pool (not per-job threads)
 *   5. Cleans up temp directory
 *
 * Thread safety: stateless — safe to call from multiple workers concurrently.
 */
@Service
public class DockerJudgeService {

    private static final Logger log = LoggerFactory.getLogger(DockerJudgeService.class);

    @Value("${codecombat.docker.host:unix:///var/run/docker.sock}")
    private String dockerHost;

    // Shared thread pool for stdout/stderr readers — avoids spawning 2 threads per job
    // Size = 2 × max workers (8) = 16 reader threads max
    private static final ExecutorService IO_POOL = Executors.newFixedThreadPool(16, r -> {
        Thread t = new Thread(r, "io-reader-" + System.nanoTime());
        t.setDaemon(true);
        return t;
    });

    private final boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

    @PostConstruct
    public void init() {
        log.info("DockerJudgeService initialized (host: {}, OS: {})",
            dockerHost, isWindows ? "Windows" : "Unix");
    }

    public ExecutionResult execute(String code, Submission.ProgrammingLanguage language, double timeLimit) {
        String fileName;
        String[] compileArgs;
        String[] runArgs;

        switch (language) {
            case JAVA:
                fileName = "Main.java";
                compileArgs = new String[]{"javac", fileName};
                runArgs = new String[]{"java", "-Xmx256m", "Main"};
                break;
            case CPP:
                fileName = "main.cpp";
                compileArgs = new String[]{"g++", "-O2", "-o", "main", fileName};
                runArgs = new String[]{isWindows ? "main.exe" : "./main"};
                break;
            case PYTHON:
                fileName = "main.py";
                compileArgs = null;
                runArgs = new String[]{isWindows ? "python" : "python3", "-u", fileName};
                break;
            case C:
                fileName = "main.c";
                compileArgs = new String[]{"gcc", "-O2", "-o", "main", fileName};
                runArgs = new String[]{isWindows ? "main.exe" : "./main"};
                break;
            case JAVASCRIPT:
                fileName = "main.js";
                compileArgs = null;
                runArgs = new String[]{"node", "--max-old-space-size=256", fileName};
                break;
            default:
                return error("Unsupported language: " + language);
        }

        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("judge_");
            Path sourceFile = tempDir.resolve(fileName);
            Files.write(sourceFile, code.getBytes(StandardCharsets.UTF_8));

            // ── Compile ──────────────────────────────────────────────────────
            if (compileArgs != null) {
                ProcessBuilder pb = new ProcessBuilder(compileArgs);
                pb.directory(tempDir.toFile());
                pb.redirectErrorStream(true);

                Process proc = pb.start();
                String compileOut = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                boolean ok = proc.waitFor(30, TimeUnit.SECONDS);

                if (!ok) {
                    proc.destroyForcibly();
                    return compilationError("Compilation timeout");
                }
                if (proc.exitValue() != 0) {
                    return compilationError(compileOut);
                }

                // Resolve absolute path for C/C++ executable
                if (language == Submission.ProgrammingLanguage.CPP
                        || language == Submission.ProgrammingLanguage.C) {
                    Path exePath = tempDir.resolve(isWindows ? "main.exe" : "main");
                    if (!Files.exists(exePath)) {
                        return compilationError("Executable not found after compilation");
                    }
                    runArgs = new String[]{exePath.toAbsolutePath().toString()};
                }
            }

            // ── Execute ──────────────────────────────────────────────────────
            long execStart = System.currentTimeMillis();
            ProcessBuilder execPb = new ProcessBuilder(runArgs);
            execPb.directory(tempDir.toFile());

            Process proc = execPb.start();
            proc.getOutputStream().close(); // no stdin needed

            // Use shared IO pool — avoids spawning 2 new threads per job
            final StringBuilder stdoutBuf = new StringBuilder();
            final StringBuilder stderrBuf = new StringBuilder();

            Future<?> stdoutFuture = IO_POOL.submit(() -> {
                try {
                    stdoutBuf.append(new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
                } catch (Exception ignored) {}
            });
            Future<?> stderrFuture = IO_POOL.submit(() -> {
                try {
                    stderrBuf.append(new String(proc.getErrorStream().readAllBytes(), StandardCharsets.UTF_8));
                } catch (Exception ignored) {}
            });

            long timeLimitMs = (long) (timeLimit * 1000);
            boolean finished = proc.waitFor(timeLimitMs, TimeUnit.MILLISECONDS);
            long execTime = System.currentTimeMillis() - execStart;

            if (!finished) {
                proc.destroyForcibly();
                stdoutFuture.cancel(true);
                stderrFuture.cancel(true);
                return new ExecutionResult("", "Time Limit Exceeded", execTime, 0, 124, true, false, false);
            }

            // Wait for IO readers to drain (max 2s)
            try { stdoutFuture.get(2, TimeUnit.SECONDS); } catch (Exception ignored) {}
            try { stderrFuture.get(2, TimeUnit.SECONDS); } catch (Exception ignored) {}

            return new ExecutionResult(
                stdoutBuf.toString(),
                stderrBuf.toString(),
                execTime,
                0, // memory tracking not reliable per-process
                proc.exitValue(),
                false, false, false
            );

        } catch (Exception e) {
            log.error("Execution error for language {}: {}", language, e.getMessage());
            return error("Execution error: " + e.getMessage());
        } finally {
            // Always clean up temp directory
            if (tempDir != null) cleanup(tempDir);
        }
    }

    private void cleanup(Path dir) {
        try {
            Files.walk(dir)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> { try { Files.delete(p); } catch (Exception ignored) {} });
        } catch (Exception ignored) {}
    }

    private ExecutionResult error(String msg) {
        return new ExecutionResult("", msg, 0, 0, 1, false, false, false);
    }

    private ExecutionResult compilationError(String msg) {
        return new ExecutionResult("", msg, 0, 0, 1, false, false, true);
    }
}
