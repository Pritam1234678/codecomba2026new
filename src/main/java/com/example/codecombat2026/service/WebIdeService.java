package com.example.codecombat2026.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages LOCAL code-server processes on the execution VM ("Into the Web" feature).
 *
 * Each session = one OS process (code-server) bound to a port in [portMin, portMax],
 * serving a per-session workspace directory under {@code workspaceBase}.
 *
 * Responsibilities:
 *   - startSession: allocate a free port, scaffold the workspace from a template,
 *     spawn code-server as a daemon-style process, register it in memory.
 *   - stopSession: kill the process and remove its workspace.
 *   - runTest: execute the language-specific test command in the workspace,
 *     parse {@code TC:N:PASS/FAIL} lines (same convention as WebContestWorkerPool),
 *     and return a verdict object.
 *   - touchSession / reapIdleSessions: idle session lifecycle.
 *
 * This service holds Process references so it can kill them later. Processes keep
 * running after startSession returns (their output is redirected to a log file).
 */
@Service
public class WebIdeService {

    private static final Logger log = LoggerFactory.getLogger(WebIdeService.class);

    private static final long TEST_TIMEOUT_SECONDS = 90L;
    private static final int RAW_OUTPUT_LIMIT = 8000;

    @Value("${WEB_IDE_WORKSPACE_BASE:/home/ubuntu/ide-workspaces}")
    private String workspaceBase;

    @Value("${WEB_IDE_CODE_SERVER:code-server}")
    private String codeServerBin;

    @Value("${WEB_IDE_PORT_MIN:9001}")
    private int portMin;

    @Value("${WEB_IDE_PORT_MAX:9050}")
    private int portMax;

    @Value("${WEB_IDE_IDLE_MINUTES:15}")
    private int idleMinutes;

    /** In-memory session registry: sessionId -> IdeSession. */
    private final Map<String, IdeSession> sessions = new ConcurrentHashMap<>();

    // ─── Session model ──────────────────────────────────────────────────────

    public static class IdeSession {
        public final String sessionId;
        public final String userId;
        public final String problemId;
        public final int port;
        public final String workspaceDir;
        public final String language;
        public volatile long lastActivityMs;
        public transient Process process;

        public IdeSession(String sessionId, String userId, String problemId,
                          int port, String workspaceDir, String language,
                          Process process) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.problemId = problemId;
            this.port = port;
            this.workspaceDir = workspaceDir;
            this.language = language;
            this.process = process;
            this.lastActivityMs = System.currentTimeMillis();
        }
    }

    // ─── Result model ───────────────────────────────────────────────────────

    public static class TestResult {
        public String status;          // AC / WA / CE / RE / TLE
        public int passed;
        public int total;
        public int score;
        public List<Map<String, Object>> details = new ArrayList<>();
        public String rawOutput;
    }

    // ─── Lifecycle ──────────────────────────────────────────────────────────

    /**
     * Start (or return existing) code-server session for {@code sessionId}.
     *
     * @param sessionId    unique session id (also the workspace dir name)
     * @param templatePath directory whose contents are copied into the workspace
     * @param language     JAVA / PYTHON / NODEJS (used later for test commands)
     * @return IdeSession with the allocated port
     */
    public synchronized IdeSession startSession(String sessionId, String templatePath, String language) {
        // Idempotent: return existing live session
        IdeSession existing = sessions.get(sessionId);
        if (existing != null && existing.process != null && existing.process.isAlive()) {
            existing.lastActivityMs = System.currentTimeMillis();
            log.info("Web IDE session {} already running on port {}", sessionId, existing.port);
            return existing;
        }

        int port = allocatePort();
        if (port < 0) {
            throw new IllegalStateException("No free code-server port available in ["
                + portMin + "," + portMax + "]");
        }

        Path workspaceDir = Paths.get(workspaceBase, sessionId);
        try {
            Files.createDirectories(workspaceDir);

            if (templatePath != null && !templatePath.isBlank()) {
                Path template = Paths.get(templatePath);
                if (Files.isDirectory(template)) {
                    copyDirectory(template, workspaceDir);
                } else {
                    log.warn("Web IDE template path is not a directory: {}", templatePath);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to scaffold workspace for session " + sessionId, e);
        }

        Process process = spawnCodeServer(sessionId, port, workspaceDir);

        IdeSession session = new IdeSession(
            sessionId, null, null, port, workspaceDir.toString(),
            language != null ? language.toUpperCase() : "JAVA", process);
        sessions.put(sessionId, session);

        log.info("Started Web IDE session {} on port {} (workspace={}, language={})",
            sessionId, port, workspaceDir, session.language);
        return session;
    }

    private Process spawnCodeServer(String sessionId, int port, Path workspaceDir) {
        List<String> command = List.of(
            codeServerBin,
            "--bind-addr", "0.0.0.0:" + port,
            "--auth", "none",
            "--disable-telemetry",
            "--disable-update-check",
            workspaceDir.toString()
        );

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workspaceDir.toFile());
        pb.redirectErrorStream(true);

        // Redirect output to {workspaceBase}/{sessionId}.log, fall back to DISCARD
        try {
            Path logFile = Paths.get(workspaceBase, sessionId + ".log");
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()));
        } catch (Exception e) {
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        }

        try {
            Process process = pb.start();
            // Close stdin so the process is fully detached (daemon-style).
            try { process.getOutputStream().close(); } catch (IOException ignored) {}
            return process;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to spawn code-server for session " + sessionId, e);
        }
    }

    /**
     * Stop a session: kill its process and remove its workspace directory.
     */
    public synchronized void stopSession(String sessionId) {
        IdeSession session = sessions.remove(sessionId);
        if (session == null) {
            log.debug("stopSession: no session found for {}", sessionId);
            return;
        }

        if (session.process != null) {
            try {
                session.process.destroyForcibly();
                session.process.waitFor(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.warn("Failed to kill code-server process for session {}: {}", sessionId, e.getMessage());
            }
        }

        deleteWorkspace(session.workspaceDir);
        // Best-effort cleanup of the log file.
        try { Files.deleteIfExists(Paths.get(workspaceBase, sessionId + ".log")); }
        catch (Exception ignored) {}

        log.info("Stopped Web IDE session {} (freed port {})", sessionId, session.port);
    }

    /**
     * Run the test suite inside a session's workspace and return a verdict.
     *
     * @param sessionId session id
     * @param isSubmit  whether this is a real submit (vs a test run) — reserved for future use
     */
    public TestResult runTest(String sessionId, boolean isSubmit) {
        IdeSession session = sessions.get(sessionId);
        if (session == null) {
            TestResult r = new TestResult();
            r.status = "RE";
            r.rawOutput = "No such session: " + sessionId;
            return r;
        }
        session.lastActivityMs = System.currentTimeMillis();

        Path workDir = Paths.get(session.workspaceDir);
        List<String> command = testCommandFor(session.language);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workDir.toFile());
        pb.redirectErrorStream(true);

        StringBuilder output = new StringBuilder();
        Process process;
        boolean finished;
        try {
            process = pb.start();
            try { process.getOutputStream().close(); } catch (IOException ignored) {}

            Thread reader = new Thread(() -> {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        synchronized (output) { output.append(line).append("\n"); }
                    }
                } catch (IOException ignored) {}
            }, "web-ide-test-" + sessionId);
            reader.setDaemon(true);
            reader.start();

            finished = process.waitFor(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                process.waitFor(2, TimeUnit.SECONDS);
                reader.join(500);
                TestResult r = new TestResult();
                r.status = "TLE";
                r.rawOutput = truncate(output.toString());
                return r;
            }
            reader.join(1000);
        } catch (IOException e) {
            TestResult r = new TestResult();
            r.status = "RE";
            r.rawOutput = "Failed to start test process: " + e.getMessage();
            return r;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            TestResult r = new TestResult();
            r.status = "RE";
            r.rawOutput = "Test execution interrupted";
            return r;
        }

        int exitCode = process.exitValue();
        String stdout = output.toString();

        return parseResult(stdout, exitCode);
    }

    private List<String> testCommandFor(String language) {
        String lang = language != null ? language.toUpperCase() : "JAVA";
        return switch (lang) {
            case "PYTHON" -> List.of("python3", "-m", "pytest", "-q");
            case "NODEJS", "NODE", "JAVASCRIPT" -> List.of("npm", "test");
            default -> List.of("mvn", "test", "-o", "-q",
                "-Dsurefire.useFile=false", "-Dspring.main.banner-mode=off");
        };
    }

    /**
     * Parse test stdout into a verdict, matching WebContestWorkerPool's TC line convention:
     *   TC:N:PASS, TC:N:FAIL, TC:N:PASS:hidden
     */
    private TestResult parseResult(String stdout, int exitCode) {
        TestResult result = new TestResult();
        result.rawOutput = truncate(stdout);

        // Compile error detection (Java mvn build failure)
        if (exitCode != 0 && (stdout.contains("COMPILATION ERROR")
                || stdout.contains("maven-compiler-plugin"))) {
            result.status = "CE";
            return result;
        }

        List<int[]> tcLines = new ArrayList<>(); // [number, passed(0/1), hidden(0/1)]
        for (String raw : stdout.split("\\r?\\n")) {
            String line = raw.trim();
            if (!line.startsWith("TC:")) continue;
            String[] parts = line.split(":", 4);
            if (parts.length < 3) continue;
            try {
                int number = Integer.parseInt(parts[1]);
                int passed = "PASS".equalsIgnoreCase(parts[2]) ? 1 : 0;
                int hidden = (parts.length >= 4 && "hidden".equalsIgnoreCase(parts[3])) ? 1 : 0;
                tcLines.add(new int[]{number, passed, hidden});
            } catch (NumberFormatException ignored) {}
        }

        if (tcLines.isEmpty()) {
            // No parsable test output → runtime error
            result.status = "RE";
            return result;
        }

        int total = tcLines.size();
        int passed = (int) tcLines.stream().filter(tc -> tc[1] == 1).count();
        int score = total > 0 ? (int) Math.round((passed * 100.0) / total) : 0;

        for (int[] tc : tcLines) {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("testCase", tc[0]);
            detail.put("status", tc[1] == 1 ? "PASS" : "FAIL");
            detail.put("hidden", tc[2] == 1);
            result.details.add(detail);
        }

        result.passed = passed;
        result.total = total;
        result.score = score;
        result.status = (passed == total) ? "AC" : "WA";
        return result;
    }

    // ─── Heartbeat + lookup ─────────────────────────────────────────────────

    /** Update lastActivityMs on heartbeat. */
    public void touchSession(String sessionId) {
        IdeSession session = sessions.get(sessionId);
        if (session != null) {
            session.lastActivityMs = System.currentTimeMillis();
        }
    }

    /** Return the session or null. */
    public IdeSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    /** For admin/debugging — a lightweight view of every active session. */
    public List<Map<String, Object>> listSessions() {
        return sessions.values().stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("sessionId", s.sessionId);
            m.put("port", s.port);
            m.put("language", s.language);
            m.put("workspaceDir", s.workspaceDir);
            m.put("lastActivityMs", s.lastActivityMs);
            m.put("alive", s.process != null && s.process.isAlive());
            return m;
        }).collect(Collectors.toList());
    }

    // ─── Idle reaper ────────────────────────────────────────────────────────

    /**
     * Kill + clean up any session idle longer than {@code idleMinutes}.
     * Also reaps sessions whose process has died.
     */
    @Scheduled(fixedDelay = 60_000)
    public void reapIdleSessions() {
        long now = System.currentTimeMillis();
        long maxIdleMs = idleMinutes * 60_000L;

        List<String> toReap = new ArrayList<>();
        for (IdeSession s : sessions.values()) {
            boolean dead = s.process == null || !s.process.isAlive();
            boolean idle = (now - s.lastActivityMs) > maxIdleMs;
            if (dead || idle) {
                toReap.add(s.sessionId);
            }
        }

        for (String sessionId : toReap) {
            log.info("Reaping idle/dead Web IDE session {}", sessionId);
            try {
                stopSession(sessionId);
            } catch (Exception e) {
                log.warn("Failed to reap session {}: {}", sessionId, e.getMessage());
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Web IDE service shutting down, killing {} session(s)", sessions.size());
        for (String sessionId : new ArrayList<>(sessions.keySet())) {
            try { stopSession(sessionId); }
            catch (Exception e) { log.warn("Shutdown kill failed for {}: {}", sessionId, e.getMessage()); }
        }
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    /** Allocate the lowest free port in [portMin, portMax] not used by an existing session. */
    private int allocatePort() {
        java.util.Set<Integer> inUse = sessions.values().stream()
            .map(s -> s.port)
            .collect(Collectors.toSet());
        for (int p = portMin; p <= portMax; p++) {
            if (!inUse.contains(p)) {
                return p;
            }
        }
        return -1;
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        try (Stream<Path> stream = Files.walk(source)) {
            stream.forEach(src -> {
                try {
                    Path dest = target.resolve(source.relativize(src).toString());
                    if (Files.isDirectory(src)) {
                        Files.createDirectories(dest);
                    } else {
                        if (dest.getParent() != null) Files.createDirectories(dest.getParent());
                        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }

    private void deleteWorkspace(String workspaceDir) {
        if (workspaceDir == null) return;
        Path dir = Paths.get(workspaceDir);
        try {
            if (Files.exists(dir)) {
                try (Stream<Path> stream = Files.walk(dir)) {
                    stream.sorted(Comparator.reverseOrder())
                          .map(Path::toFile)
                          .forEach(File::delete);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to delete workspace {}: {}", workspaceDir, e.getMessage());
        }
    }

    private String truncate(String s) {
        if (s == null) return "";
        if (s.length() <= RAW_OUTPUT_LIMIT) return s;
        return s.substring(0, RAW_OUTPUT_LIMIT) + "\n...[truncated]";
    }
}
