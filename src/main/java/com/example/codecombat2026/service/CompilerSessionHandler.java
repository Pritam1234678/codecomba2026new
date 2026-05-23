package com.example.codecombat2026.service;

import com.example.codecombat2026.service.judge.SandboxRunner;
import com.example.codecombat2026.service.judge.SandboxRunner.SandboxLimits;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Interactive terminal-like compiler.
 *
 * All user processes run inside a {@link SandboxRunner} sandbox (bwrap +
 * prlimit) so a hostile snippet cannot read the JVM's environment, escape
 * the work directory, fork-bomb the host, or open network sockets.
 *
 * Per session lifecycle:
 *   client → {type: 'start', language, code}
 *     → server compiles inside sandbox, spawns process, streams stdout/stderr
 *   client → {type: 'input', data: 'user typed text\n'}
 *     → server pipes to process stdin
 *   server → {type: 'output', data: 'process stdout chunk'}
 *   server → {type: 'exit', code: N}
 */
@Component
public class CompilerSessionHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(CompilerSessionHandler.class);
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + "/compiler-ws";

    @Value("${COMPILER_WS_MAX_SESSIONS:30}")
    private int maxSessions;

    /** Hard wall-clock cap for any single interactive run, in seconds. */
    @Value("${COMPILER_WS_MAX_RUNTIME_SEC:180}")
    private int maxRuntimeSec;

    /** Rate limit window — applies to anonymous users only. */
    @Value("${COMPILER_WS_RATE_WINDOW_SEC:60}")
    private int rateWindowSec;

    /** Max anon sessions per IP per window. Authenticated users get 3x this. */
    @Value("${COMPILER_WS_RATE_LIMIT_ANON:5}")
    private int rateLimitAnon;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private SandboxRunner sandbox;

    private final ObjectMapper mapper = new ObjectMapper();

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final AtomicInteger active = new AtomicInteger(0);

    private final ExecutorService ioPool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "compiler-ws-io");
        t.setDaemon(true);
        return t;
    });

    @Override
    public void afterConnectionEstablished(WebSocketSession ws) throws Exception {
        String ip = clientIp(ws);
        Long userId = userIdOf(ws);
        if (!allowSession(ip, userId != null)) {
            send(ws, "error", "Rate limit exceeded. Try again later.");
            ws.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        if (active.get() >= maxSessions) {
            send(ws, "error", "Server is at capacity. Try again in a few seconds.");
            ws.close(CloseStatus.SERVICE_OVERLOAD);
            return;
        }
        log.debug("WS opened: {} from {} (userId={})", ws.getId(), ip,
            userId != null ? userId : "anon");
    }

    @Override
    protected void handleTextMessage(WebSocketSession ws, TextMessage msg) throws Exception {
        JsonNode root = mapper.readTree(msg.getPayload());
        String type = root.path("type").asText();

        if ("start".equals(type)) {
            String language = root.path("language").asText("PYTHON").toUpperCase();
            String code     = root.path("code").asText("");
            startSession(ws, language, code);
        } else if ("input".equals(type)) {
            Session s = sessions.get(ws.getId());
            if (s != null && s.process != null && s.process.isAlive()) {
                String data = root.path("data").asText("");
                try {
                    s.stdin.write(data);
                    s.stdin.flush();
                } catch (IOException e) {
                    // process likely exited
                }
            }
        } else if ("kill".equals(type)) {
            killSession(ws.getId());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession ws, CloseStatus status) throws Exception {
        killSession(ws.getId());
        log.debug("WS closed: {}", ws.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession ws, Throwable e) throws Exception {
        killSession(ws.getId());
    }

    // ─── Session lifecycle ────────────────────────────────────────────────────

    private void startSession(WebSocketSession ws, String language, String code) throws IOException {
        if (code.length() > 50_000) {
            send(ws, "error", "Code too long (max 50000 chars)");
            ws.close();
            return;
        }

        String sessionId = ws.getId();
        Path workDir = Paths.get(TEMP_DIR, UUID.randomUUID().toString().substring(0, 8));
        Files.createDirectories(workDir);

        Session s = new Session();
        s.workDir = workDir;
        sessions.put(sessionId, s);
        active.incrementAndGet();

        ioPool.submit(() -> runSession(ws, s, language, code));
    }

    private void runSession(WebSocketSession ws, Session s, String language, String code) {
        try {
            // Compile happens inside the sandbox too (forCompile limits)
            List<String> runArgv = compileAndPrepare(ws, s, language, code);
            if (runArgv == null) return;

            send(ws, "ready", "Process started");

            // Wrap the run command in a sandbox with interactive limits
            List<String> sandboxed = sandbox.wrap(runArgv, s.workDir,
                SandboxLimits.forInteractive(/* memMB */ 512));
            ProcessBuilder runPb = new ProcessBuilder(sandboxed);
            runPb.directory(s.workDir.toFile());
            runPb.redirectErrorStream(false);
            Process p = runPb.start();
            s.process = p;
            s.stdin = new BufferedWriter(new OutputStreamWriter(p.getOutputStream(), StandardCharsets.UTF_8));

            ioPool.submit(() -> streamToClient(ws, p.getInputStream(), "output"));
            ioPool.submit(() -> streamToClient(ws, p.getErrorStream(), "stderr"));

            boolean finished = p.waitFor(maxRuntimeSec, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                p.descendants().forEach(d -> { try { d.destroyForcibly(); } catch (Exception ignored) {} });
                send(ws, "error", "Time Limit Exceeded (" + (maxRuntimeSec / 60) + " minutes)");
                send(ws, "exit", 124);
            } else {
                send(ws, "exit", p.exitValue());
            }
        } catch (Exception e) {
            log.warn("Session {} failed: {}", ws.getId(), e.getMessage());
            try { send(ws, "error", "Internal error"); } catch (Exception ignored) {}
            try { send(ws, "exit", 1); } catch (Exception ignored) {}
        } finally {
            cleanup(s);
            active.decrementAndGet();
            try { ws.close(); } catch (Exception ignored) {}
            sessions.remove(ws.getId());
        }
    }

    /**
     * Compiles inside a sandbox (forCompile limits). Returns the run argv
     * (raw, not yet sandboxed — caller wraps with forInteractive limits).
     * Returns null on compile error.
     */
    private List<String> compileAndPrepare(WebSocketSession ws, Session s, String language, String code) throws Exception {
        Path workDir = s.workDir;
        switch (language) {
            case "JAVA": {
                String className = extractJavaClassName(code);
                Path src = workDir.resolve(className + ".java");
                Files.writeString(src, code);
                if (!compile(ws, List.of("javac", src.toString()), workDir)) return null;
                return List.of("java", "-Xmx384m", "-cp", workDir.toString(), className);
            }
            case "CPP": {
                Path src = workDir.resolve("main.cpp");
                Path bin = workDir.resolve("main");
                Files.writeString(src, code);
                if (!compile(ws, List.of("g++", "-O2", "-o", bin.toString(), src.toString()), workDir)) return null;
                return List.of(bin.toString());
            }
            case "C": {
                Path src = workDir.resolve("main.c");
                Path bin = workDir.resolve("main");
                Files.writeString(src, code);
                if (!compile(ws, List.of("gcc", "-O2", "-o", bin.toString(), src.toString()), workDir)) return null;
                return List.of(bin.toString());
            }
            case "PYTHON": {
                Path src = workDir.resolve("main.py");
                Files.writeString(src, code);
                // -u for unbuffered output (interactive prompts work)
                return List.of("python3", "-u", src.toString());
            }
            case "JAVASCRIPT": {
                Path src = workDir.resolve("main.js");
                Files.writeString(src, code);
                return List.of("node", "--max-old-space-size=384", src.toString());
            }
            default:
                send(ws, "error", "Unsupported language: " + language);
                return null;
        }
    }

    /**
     * Run a compile step inside the sandbox (forCompile limits).
     * Returns true on success, false on compile failure (also sends stderr to client).
     */
    private boolean compile(WebSocketSession ws, List<String> command, Path workDir) throws Exception {
        List<String> sandboxed = sandbox.wrap(command, workDir, SandboxLimits.forCompile());
        Process proc = new ProcessBuilder(sandboxed)
            .directory(workDir.toFile())
            .redirectErrorStream(true)
            .start();
        String out = readAll(proc.getInputStream());
        if (!proc.waitFor(60, TimeUnit.SECONDS) || proc.exitValue() != 0) {
            try { proc.destroyForcibly(); } catch (Exception ignored) {}
            send(ws, "stderr", "── Compilation Error ──\n" + out);
            send(ws, "exit", 1);
            return false;
        }
        return true;
    }

    /**
     * Streams a process output stream to the WebSocket, flushing on each chunk.
     * Crucial for interactive terminals — output appears immediately when the
     * process flushes its stdout.
     */
    private void streamToClient(WebSocketSession ws, InputStream is, String type) {
        byte[] buf = new byte[1024];
        try {
            int n;
            while ((n = is.read(buf)) != -1) {
                String chunk = new String(buf, 0, n, StandardCharsets.UTF_8);
                synchronized (ws) {
                    if (ws.isOpen()) {
                        ws.sendMessage(new TextMessage(mapper.writeValueAsString(
                            Map.of("type", type, "data", chunk)
                        )));
                    } else {
                        break;
                    }
                }
            }
        } catch (IOException ignored) {
            // process exited
        }
    }

    private void killSession(String sessionId) {
        Session s = sessions.remove(sessionId);
        if (s != null) cleanup(s);
    }

    private void cleanup(Session s) {
        if (s.process != null && s.process.isAlive()) {
            s.process.destroyForcibly();
            s.process.descendants().forEach(d -> { try { d.destroyForcibly(); } catch (Exception ignored) {} });
        }
        try { if (s.stdin != null) s.stdin.close(); } catch (Exception ignored) {}
        try {
            if (s.workDir != null && Files.exists(s.workDir)) {
                Files.walk(s.workDir)
                    .sorted(java.util.Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
        } catch (Exception ignored) {}
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void send(WebSocketSession ws, String type, Object data) throws IOException {
        if (!ws.isOpen()) return;
        synchronized (ws) {
            ws.sendMessage(new TextMessage(mapper.writeValueAsString(
                Map.of("type", type, "data", data)
            )));
        }
    }

    private String readAll(InputStream is) {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append("\n");
            return sb.toString();
        } catch (IOException e) {
            return "";
        }
    }

    private String extractJavaClassName(String code) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("public\\s+class\\s+(\\w+)").matcher(code);
        return m.find() ? m.group(1) : "Main";
    }

    private boolean allowSession(String ip, boolean authenticated) {
        // Authenticated users get 3x the anonymous quota; identical key prefix
        // so a hostile authed user still hits a cap.
        int limit = authenticated ? rateLimitAnon * 3 : rateLimitAnon;
        String key = "compiler:ws:rl:" + ip;
        try {
            Long c = redis.opsForValue().increment(key);
            if (c != null && c == 1) redis.expire(key, Duration.ofSeconds(rateWindowSec));
            return c != null && c <= limit;
        } catch (Exception e) {
            return true; // fail open — Valkey downtime shouldn't deny dev users
        }
    }

    private String clientIp(WebSocketSession ws) {
        Object ip = ws.getAttributes().get("ip");
        if (ip != null) return ip.toString();
        return ws.getRemoteAddress() != null ? ws.getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }

    private Long userIdOf(WebSocketSession ws) {
        Object u = ws.getAttributes().get("userId");
        return u instanceof Long ? (Long) u : null;
    }

    // ─── Session state ────────────────────────────────────────────────────────

    private static class Session {
        Path workDir;
        Process process;
        BufferedWriter stdin;
    }
}
