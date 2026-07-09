package com.example.codecombat2026.service.judge;

import com.example.codecombat2026.dto.ExecutionResult;
import com.example.codecombat2026.entity.Submission;
import com.example.codecombat2026.service.judge.SandboxRunner.SandboxLimits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Local judge service — executes user code under a security sandbox
 * (see {@link SandboxRunner}). All compile + run steps are wrapped in
 * bwrap + prlimit, so a hostile submission cannot read the JVM's
 * environment, escape its work directory, or open network sockets.
 *
 * Supported languages: JAVA, CPP, C, PYTHON, JAVASCRIPT
 *
 * Hardening (against hung user code):
 *   - stdin closed BEFORE waitFor → unblocks code stuck on Scanner.nextInt() etc.
 *   - destroyForcibly() + waitFor(2s) → confirms subprocess actually died
 *   - Reader threads are daemon → don't prevent JVM exit
 *   - Memory + CPU + nproc + fsize limits enforced by prlimit
 *   - Filesystem and network isolated by bwrap namespaces
 */
@Service
public class DockerJudgeService {

    private static final Logger log = LoggerFactory.getLogger(DockerJudgeService.class);
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + "/judge";

    /** Default memory budget when problem doesn't specify (MB). */
    private static final int DEFAULT_MEMORY_MB = 256;

    /**
     * Virtual address space (RLIMIT_AS) overhead required to host the
     * language runtime, on top of the user-visible problem memory budget.
     *
     * Important: RLIMIT_AS limits virtual memory, not resident memory. Modern
     * JVMs reserve 3+ GB of virtual address space at startup (compressed
     * class space, code cache, GC reserves, thread stacks) regardless of how
     * small -Xmx is. If RLIMIT_AS < what the JVM wants to reserve, the VM
     * fails to initialise and exits before the user's code runs. The numbers
     * below are minimum virtual reservations observed in practice on JDK 21
     * and Node 20+; they're orders of magnitude above actual RSS so do not
     * loosen real-memory enforcement (handled by -Xmx / --max-old-space-size).
     */
    private static int runtimePaddingMB(Submission.ProgrammingLanguage lang) {
        return switch (lang) {
            case JAVA       -> 4096;
            case JAVASCRIPT -> 1024;
            case PYTHON     -> 256;
            case CPP, C     -> 32;
        };
    }

    @Autowired
    private SandboxRunner sandbox;

    public ExecutionResult execute(String code, Submission.ProgrammingLanguage language, double timeLimitSeconds) {
        return execute(code, language, timeLimitSeconds, DEFAULT_MEMORY_MB, null);
    }

    public ExecutionResult execute(String code, Submission.ProgrammingLanguage language,
                                   double timeLimitSeconds, String stdin) {
        return execute(code, language, timeLimitSeconds, DEFAULT_MEMORY_MB, stdin);
    }

    /**
     * Execute code under sandbox with explicit memory budget.
     *
     * @param memoryMB user-visible memory limit from the problem; runtime padding
     *                 is added internally for JVM/Node/Python.
     */
    public ExecutionResult execute(String code, Submission.ProgrammingLanguage language,
                                   double timeLimitSeconds, int memoryMB, String stdin) {
        String jobId = UUID.randomUUID().toString().substring(0, 8);
        Path workDir = Paths.get(TEMP_DIR, jobId);

        // Defensive normalization — timeLimit is ALWAYS seconds here. Legacy rows
        // stored milliseconds (e.g. 5000, 100000); without this a "5000" would make
        // the judge wait ~83 minutes before declaring TLE. Any value > 100 is treated
        // as milliseconds and converted, then clamped to a sane [1, 15]s window so a
        // bad value can never hang a worker.
        if (timeLimitSeconds > 100) timeLimitSeconds = timeLimitSeconds / 1000.0;
        timeLimitSeconds = Math.max(1.0, Math.min(15.0, timeLimitSeconds));

        try {
            Files.createDirectories(workDir);
            // Owner-only permissions — defence in depth even with sandbox
            try { workDir.toFile().setReadable(true, true); } catch (Exception ignored) {}

            int totalMemMB = memoryMB + runtimePaddingMB(language);
            int cpuSec     = (int) Math.ceil(timeLimitSeconds) + 1; // +1 wallclock-vs-CPU buffer
            log.debug("[{}] Executing {} (timeLimit={}s, userMem={}MB, asLimit={}MB)",
                jobId, language, timeLimitSeconds, memoryMB, totalMemMB);

            // Two memory numbers flow into the language runners:
            //   userMemMB  : enforced as -Xmx / --max-old-space-size — the
            //                limit the user sees (MLE triggers here)
            //   totalMemMB : enforced as RLIMIT_AS — virtual address space,
            //                must be high enough for the runtime to start
            return switch (language) {
                case JAVA       -> executeJava(code, workDir, jobId, timeLimitSeconds, memoryMB, totalMemMB, cpuSec, stdin);
                case CPP        -> executeCpp(code, workDir, jobId, timeLimitSeconds, totalMemMB, cpuSec, stdin);
                case C          -> executeC(code, workDir, jobId, timeLimitSeconds, totalMemMB, cpuSec, stdin);
                case PYTHON     -> executePython(code, workDir, jobId, timeLimitSeconds, totalMemMB, cpuSec, stdin);
                case JAVASCRIPT -> executeJavaScript(code, workDir, jobId, timeLimitSeconds, memoryMB, totalMemMB, cpuSec, stdin);
            };

        } catch (Exception e) {
            log.error("[{}] Unexpected error: {}", jobId, e.getMessage());
            return error("Internal judge error: " + e.getMessage());
        } finally {
            cleanup(workDir);
        }
    }

    // ─── Language Runners ─────────────────────────────────────────────────────

    private ExecutionResult executeJava(String code, Path workDir, String jobId,
                                        double timeLimitSeconds, int userMemMB, int asMemMB,
                                        int cpuSec, String stdin) throws Exception {
        String className = extractJavaClassName(code);
        Path sourceFile = workDir.resolve(className + ".java");
        Files.writeString(sourceFile, code);

        ExecutionResult compileResult = runProcess(
            List.of("javac", sourceFile.toString()),
            workDir, 30, false, null, SandboxLimits.forCompile()
        );
        if (compileResult.getExitCode() != 0) {
            String err = compileResult.getStderr();
            if (err == null || err.trim().isEmpty()) {
                err = "Compilation failed (exit=" + compileResult.getExitCode() + ", no compiler output)";
                log.warn("javac CE empty stderr: exit={}, jobId={}", compileResult.getExitCode(), jobId);
            }
            return new ExecutionResult("", err, 0, 0, 1, false, false, true);
        }

        return runProcess(
            List.of("java", "-Xmx" + userMemMB + "m", "-cp", workDir.toString(), className),
            workDir, (int) Math.ceil(timeLimitSeconds), true, stdin,
            SandboxLimits.forRun(asMemMB, cpuSec)
        );
    }

    private ExecutionResult executeCpp(String code, Path workDir, String jobId,
                                       double timeLimitSeconds, int memMB, int cpuSec, String stdin) throws Exception {
        Path sourceFile = workDir.resolve("solution.cpp");
        Path binary     = workDir.resolve("solution");
        Files.writeString(sourceFile, code);

        ExecutionResult compileResult = runProcess(
            List.of("g++", "-O2", "-o", binary.toString(), sourceFile.toString()),
            workDir, 30, false, null, SandboxLimits.forCompile()
        );
        if (compileResult.getExitCode() != 0) {
            String err = compileResult.getStderr();
            if (err == null || err.trim().isEmpty()) {
                err = "Compilation failed (exit=" + compileResult.getExitCode() + ", no compiler output)";
                log.warn("g++ CE empty stderr: exit={}, jobId={}", compileResult.getExitCode(), jobId);
            }
            return new ExecutionResult("", err, 0, 0, 1, false, false, true);
        }

        return runProcess(List.of(binary.toString()),
            workDir, (int) Math.ceil(timeLimitSeconds), true, stdin,
            SandboxLimits.forRun(memMB, cpuSec));
    }

    private ExecutionResult executeC(String code, Path workDir, String jobId,
                                     double timeLimitSeconds, int memMB, int cpuSec, String stdin) throws Exception {
        Path sourceFile = workDir.resolve("solution.c");
        Path binary     = workDir.resolve("solution");
        Files.writeString(sourceFile, code);

        ExecutionResult compileResult = runProcess(
            List.of("gcc", "-O2", "-o", binary.toString(), sourceFile.toString()),
            workDir, 30, false, null, SandboxLimits.forCompile()
        );
        if (compileResult.getExitCode() != 0) {
            String err = compileResult.getStderr();
            if (err == null || err.trim().isEmpty()) {
                err = "Compilation failed (exit=" + compileResult.getExitCode() + ", no compiler output)";
                log.warn("gcc CE empty stderr: exit={}, jobId={}", compileResult.getExitCode(), jobId);
            }
            return new ExecutionResult("", err, 0, 0, 1, false, false, true);
        }

        return runProcess(List.of(binary.toString()),
            workDir, (int) Math.ceil(timeLimitSeconds), true, stdin,
            SandboxLimits.forRun(memMB, cpuSec));
    }

    private ExecutionResult executePython(String code, Path workDir, String jobId,
                                          double timeLimitSeconds, int memMB, int cpuSec, String stdin) throws Exception {
        Path sourceFile = workDir.resolve("solution.py");
        Files.writeString(sourceFile, code);

        // python3 is assumed to exist in /usr/bin (sandbox prevents shelling
        // out via `which`, so we don't probe like the unsandboxed path did).
        return runProcess(
            List.of("python3", sourceFile.toString()),
            workDir, (int) Math.ceil(timeLimitSeconds), true, stdin,
            SandboxLimits.forRun(memMB, cpuSec)
        );
    }

    private ExecutionResult executeJavaScript(String code, Path workDir, String jobId,
                                              double timeLimitSeconds, int userMemMB, int asMemMB,
                                              int cpuSec, String stdin) throws Exception {
        Path sourceFile = workDir.resolve("solution.js");
        Files.writeString(sourceFile, code);

        // --max-old-space-size enforces V8's heap limit (real RSS budget).
        // RLIMIT_AS only constrains virtual address space; Node 20+ reserves
        // ~700 MB virtual at startup so the AS limit must be higher.
        int v8Heap = Math.max(64, userMemMB);
        return runProcess(
            List.of("node", "--max-old-space-size=" + v8Heap, sourceFile.toString()),
            workDir, (int) Math.ceil(timeLimitSeconds), true, stdin,
            SandboxLimits.forRun(asMemMB, cpuSec)
        );
    }

    // ─── Core Process Runner ──────────────────────────────────────────────────

    private ExecutionResult runProcess(List<String> command, Path workDir,
                                       int timeLimitSeconds, boolean enforceTimeLimit,
                                       String stdin, SandboxLimits limits) throws Exception {
        List<String> wrapped = sandbox.wrap(command, workDir, limits);
        ProcessBuilder pb = new ProcessBuilder(wrapped);
        pb.directory(workDir.toFile());
        pb.redirectErrorStream(false);

        long startMs = System.currentTimeMillis();
        Process process = pb.start();

        // CRITICAL: write stdin (if any) and close. If user code calls Scanner.nextInt()
        // or cin >> or sys.stdin.read(), they get the input or EOF instead of hanging forever.
        try {
            if (stdin != null && !stdin.isEmpty()) {
                process.getOutputStream().write(stdin.getBytes());
                process.getOutputStream().flush();
            }
            process.getOutputStream().close();
        } catch (IOException ignored) {}

        // Read stdout and stderr concurrently to avoid blocking on full pipe buffer.
        // Use daemon threads so they never prevent JVM shutdown.
        // Uses read() not readLine() — compiler/runtime may write partial lines
        // without trailing newlines, and readLine() would block indefinitely.
        // After the process exits, read() returns -1 (EOF) unless orphaned child
        // processes still hold the pipe open — the join timeout below handles that.
        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();

        Thread stdoutReader = new Thread(() -> {
            try (InputStream is = process.getInputStream()) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = is.read(buf)) != -1) {
                    synchronized (stdout) { stdout.append(new String(buf, 0, n, StandardCharsets.UTF_8)); }
                }
            } catch (IOException ignored) {}
        }, "judge-stdout-reader");
        stdoutReader.setDaemon(true);

        Thread stderrReader = new Thread(() -> {
            try (InputStream is = process.getErrorStream()) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = is.read(buf)) != -1) {
                    synchronized (stderr) { stderr.append(new String(buf, 0, n, StandardCharsets.UTF_8)); }
                }
            } catch (IOException ignored) {}
        }, "judge-stderr-reader");
        stderrReader.setDaemon(true);

        stdoutReader.start();
        stderrReader.start();

        boolean finished = process.waitFor(timeLimitSeconds + 5L, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - startMs;

        if (!finished) {
            // Process exceeded limit. Kill it and CONFIRM death.
            killProcessTree(process);
            log.warn("Process killed after {}ms (limit={}s)", elapsed, timeLimitSeconds);

            // Give readers brief time to drain any final bytes after kill
            awaitReader(stdoutReader, 500, "stdout after kill");
            awaitReader(stderrReader, 500, "stderr after kill");

            return new ExecutionResult(stdout.toString(), "Time Limit Exceeded",
                elapsed, 0, 1, true, false, false);
        }

        // Process exited normally — wait for readers to finish draining the pipe.
        // 5s timeout handles cases where orphaned child processes keep the pipe
        // open, preventing EOF from being delivered to the reader thread.
        awaitReader(stdoutReader, 5000, "stdout");
        awaitReader(stderrReader, 5000, "stderr");

        int exitCode = process.exitValue();
        boolean isTle = enforceTimeLimit && elapsed > (timeLimitSeconds * 1000L);

        if (isTle) {
            return new ExecutionResult(stdout.toString(), "Time Limit Exceeded",
                elapsed, 0, exitCode, true, false, false);
        }

        // 137 = 128 + SIGKILL, 152 = 128 + SIGXCPU — both indicate kernel kill
        // due to resource limit (memory or CPU)
        if (exitCode == 137 || exitCode == 152 || exitCode == 153) {
            String stderrStr = stderr.toString();
            boolean isMle = stderrStr.toLowerCase().contains("killed")
                         || stderrStr.toLowerCase().contains("memoryerror")
                         || stderrStr.toLowerCase().contains("outofmemory")
                         || exitCode == 137;
            if (isMle) {
                return new ExecutionResult(stdout.toString(),
                    "Memory Limit Exceeded", elapsed, 0, exitCode, false, true, false);
            }
            return new ExecutionResult(stdout.toString(), "Time Limit Exceeded",
                elapsed, 0, exitCode, true, false, false);
        }

        return new ExecutionResult(stdout.toString(), stderr.toString(),
            elapsed, 0, exitCode, false, false, false);
    }

    /**
     * Await reader thread completion with a timeout. Logs a warning if the
     * thread is still alive after the timeout — this indicates an orphaned
     * child process holding the pipe open (most likely a compiler subprocess
     * that was not cleaned up by its parent).
     */
    private void awaitReader(Thread reader, long timeoutMs, String label) {
        try {
            reader.join(timeoutMs);
            if (reader.isAlive()) {
                log.warn("Reader '{}' still alive after {}ms — orphaned process keeping pipe open?", label, timeoutMs);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Aggressive kill: destroyForcibly() + waitFor(2s) + verify dead.
     * On Linux, destroyForcibly() sends SIGKILL but is async — the OS
     * may keep the process around briefly. We wait until it's actually gone.
     * With bwrap, killing the bwrap parent kills the entire namespace
     * (--die-with-parent + new PID namespace), so descendants die automatically.
     */
    private void killProcessTree(Process process) {
        try {
            process.destroyForcibly();
            if (!process.waitFor(2, TimeUnit.SECONDS)) {
                log.error("Process {} survived destroyForcibly after 2s — leaking", process.pid());
            }
            // Belt-and-suspenders for non-sandboxed dev mode
            process.descendants().forEach(d -> {
                try { d.destroyForcibly(); } catch (Exception ignored) {}
            });
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("Error killing process: {}", e.getMessage());
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String extractJavaClassName(String code) {
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("public\\s+class\\s+(\\w+)")
            .matcher(code);
        return m.find() ? m.group(1) : "Main";
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

    private ExecutionResult error(String msg) {
        return new ExecutionResult("", msg, 0, 0, 1, false, false, false);
    }
}
