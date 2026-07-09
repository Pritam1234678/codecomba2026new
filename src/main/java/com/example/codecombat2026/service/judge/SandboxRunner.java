package com.example.codecombat2026.service.judge;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Wraps user-controlled commands in a security sandbox.
 *
 * Two-layer defence:
 *   1. bwrap (bubblewrap) — creates a fresh mount + PID + IPC + UTS + USER + NET
 *      namespace. /usr, /lib, /etc are bound read-only; /tmp and /run are
 *      tmpfs; /proc and /dev are fresh. Only the per-job workDir is bound
 *      read-write. Network is unshared by default. User namespace drops the
 *      process to nobody (uid 65534) so even kernel exploits land on a
 *      privilege-stripped UID.
 *
 *   2. prlimit (runs inside the sandbox) — applies RLIMIT_AS (virtual
 *      memory), RLIMIT_CPU (CPU seconds), RLIMIT_NPROC (process count),
 *      RLIMIT_FSIZE (max file size), RLIMIT_NOFILE (open files). These
 *      survive the namespace boundary because rlimits are per-process.
 *
 * Toolchain resolution: at startup we resolve the host-absolute path of every
 * compiler/interpreter binary the judge needs (javac, java, python3, g++, gcc,
 * node, …). Their containing directories are bind-mounted read-only into the
 * sandbox so the binaries are reachable, and the user command's argv[0] is
 * rewritten from a bare name (e.g. "javac") to its absolute path. PATH inside
 * the sandbox includes those directories so transitive subprocesses keep working
 * (javac → java, etc.). This means callers can keep writing
 * {@code List.of("javac", src)} without caring where javac actually lives.
 *
 * If {@code SANDBOX_ENABLED=false} the sandbox is bypassed (development only).
 * Production MUST run with the sandbox enabled.
 */
@Component
public class SandboxRunner {

    private static final Logger log = LoggerFactory.getLogger(SandboxRunner.class);

    /**
     * Bare names we always try to resolve at startup. Order matters only for
     * logging clarity. If a tool isn't installed, that's not a startup error —
     * the language simply won't run, and the judge will surface a clear
     * "interpreter not found" verdict on first attempt.
     */
    private static final List<String> TOOLCHAIN = List.of(
        "javac", "java",
        "python3",
        "gcc", "g++",
        "node", "npm"
    );

    @Value("${SANDBOX_ENABLED:true}")
    private boolean enabled;

    @Value("${SANDBOX_BWRAP_PATH:/usr/bin/bwrap}")
    private String bwrapPath;

    @Value("${SANDBOX_PRLIMIT_PATH:/usr/bin/prlimit}")
    private String prlimitPath;

    /** name → host absolute path. e.g. "javac" → "/opt/jdk-21.0.11/bin/javac". */
    private final Map<String, String> binaryPaths = new LinkedHashMap<>();

    /** Distinct directories to bind read-only into the sandbox (parents of resolved binaries). */
    private final Set<String> toolchainDirs = new LinkedHashSet<>();

    /** PATH string to expose inside the sandbox (toolchain dirs first, then /usr/bin:/bin). */
    private String sandboxPath = "/usr/bin:/bin";

    @PostConstruct
    public void check() {
        resolveToolchain();
        if (!enabled) {
            log.warn("⚠️  SANDBOX DISABLED — user code runs with full host privileges. " +
                "Production MUST set SANDBOX_ENABLED=true.");
            return;
        }
        if (!new File(bwrapPath).canExecute()) {
            log.error("❌ bwrap not executable at {}. Install bubblewrap " +
                "(apt-get install bubblewrap) or set SANDBOX_BWRAP_PATH.", bwrapPath);
            throw new IllegalStateException("bwrap not executable: " + bwrapPath);
        }
        if (!new File(prlimitPath).canExecute()) {
            log.error("❌ prlimit not executable at {}. Install util-linux or set SANDBOX_PRLIMIT_PATH.",
                prlimitPath);
            throw new IllegalStateException("prlimit not executable: " + prlimitPath);
        }
        log.info("✅ Sandbox enabled (bwrap={}, prlimit={})", bwrapPath, prlimitPath);
    }

    /**
     * Probe the host PATH (and a handful of common install locations) for each
     * tool we know about, resolving symlinks to a real install directory we can
     * bind into the sandbox. Missing tools are logged but not fatal.
     */
    private void resolveToolchain() {
        // Discover candidate dirs from the host's PATH plus a small allowlist
        // of locations where compilers are typically installed.
        List<String> hostPath = new ArrayList<>();
        String env = System.getenv("PATH");
        if (env != null && !env.isEmpty()) {
            hostPath.addAll(Arrays.asList(env.split(File.pathSeparator)));
        }
        // Common locations not always on PATH for systemd/non-login JVMs
        hostPath.addAll(List.of(
            "/usr/local/bin", "/usr/bin", "/bin",
            "/opt/bin", "/snap/bin"
        ));
        // Java/Node managers often install under here
        addIfExists(hostPath, System.getProperty("user.home") + "/.nvm/versions/node");
        addIfExists(hostPath, "/opt");

        for (String tool : TOOLCHAIN) {
            String resolved = locate(tool, hostPath);
            if (resolved != null) {
                binaryPaths.put(tool, resolved);
                Path parent = Paths.get(resolved).getParent();
                if (parent != null) {
                    // Bind the entire toolchain root, not just bin/, so that
                    // e.g. the JDK can find lib/, conf/, jrt-fs.jar, etc.
                    Path root = toolchainRoot(parent);
                    toolchainDirs.add(root.toString());
                }
            } else {
                log.warn("Toolchain: '{}' not found on host — that language will fail at runtime.", tool);
            }
        }

        // Recompute PATH so toolchain bins resolve inside the sandbox
        StringBuilder sb = new StringBuilder();
        for (String dir : toolchainDirs) {
            // Bin subdir (typical) or the dir itself
            File bin = new File(dir, "bin");
            if (bin.isDirectory()) {
                if (sb.length() > 0) sb.append(':');
                sb.append(bin.getAbsolutePath());
            }
        }
        if (sb.length() > 0) sb.append(':');
        sb.append("/usr/bin:/bin");
        sandboxPath = sb.toString();

        log.info("✅ Toolchain resolved: {}", binaryPaths);
        log.info("✅ Sandbox PATH: {}", sandboxPath);
    }

    /**
     * Walk up from a binary's parent directory to find the toolchain root we
     * should bind. For most installs that's the directory itself (e.g.
     * /usr/local/bin); for JDKs and Node it's the parent of bin/ so that the
     * runtime can find its lib/, include/, etc.
     */
    private Path toolchainRoot(Path binDir) {
        if (binDir.getFileName() != null && "bin".equals(binDir.getFileName().toString())) {
            Path parent = binDir.getParent();
            if (parent != null) return parent;
        }
        return binDir;
    }

    private static void addIfExists(List<String> list, String dir) {
        try {
            File f = new File(dir);
            if (f.isDirectory()) {
                // Add the dir itself and any direct subdirs that look like installs
                list.add(dir);
                File[] children = f.listFiles();
                if (children != null) {
                    for (File child : children) {
                        File bin = new File(child, "bin");
                        if (bin.isDirectory()) list.add(bin.getAbsolutePath());
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    /** Find {@code name} on the candidate PATH list, returning a real path (symlinks resolved). */
    private String locate(String name, List<String> candidates) {
        for (String dir : candidates) {
            if (dir == null || dir.isEmpty()) continue;
            File f = new File(dir, name);
            if (f.canExecute()) {
                try {
                    return f.toPath().toRealPath().toString();
                } catch (IOException ignored) {
                    return f.getAbsolutePath();
                }
            }
        }
        return null;
    }

    /** Returns true if sandboxing is active. Useful for callers to log warnings. */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Wrap a user command for safe execution. The original command's argv is
     * preserved; argv[0] is rewritten to its host-absolute path if we resolved
     * one at startup. Returns argv suitable for {@link ProcessBuilder}.
     */
    public List<String> wrap(List<String> command, Path workDir, SandboxLimits limits) {
        // Rewrite argv[0] from bare name → host absolute path so the sandbox
        // doesn't need to do its own PATH lookup. This works whether the
        // sandbox is enabled or not.
        List<String> rewritten = rewriteFirstArg(command);

        if (!enabled) {
            return rewritten;
        }
        List<String> argv = new ArrayList<>();

        // ── 1. bwrap container ────────────────────────────────────────────────
        argv.add(bwrapPath);

        // System libraries — read-only
        addRoBind(argv, "/usr",   "/usr");
        addRoBind(argv, "/lib",   "/lib");
        addRoBind(argv, "/lib64", "/lib64");
        addRoBind(argv, "/bin",   "/bin");
        addRoBind(argv, "/sbin",  "/sbin");
        addRoBind(argv, "/etc",   "/etc");

        // Toolchain dirs (JDKs, node, etc.) bound at their real paths so
        // absolute argv[0] entries resolve inside the sandbox.
        for (String dir : toolchainDirs) {
            addRoBind(argv, dir, dir);
        }

        // Fresh /proc, /dev (just /dev/null, /dev/zero, /dev/random, /dev/urandom)
        argv.add("--proc");  argv.add("/proc");
        argv.add("--dev");   argv.add("/dev");

        // Volatile dirs — tmpfs so user code can write small temp files but
        // cannot leak data between jobs
        argv.add("--tmpfs"); argv.add("/tmp");
        argv.add("--tmpfs"); argv.add("/run");
        argv.add("--tmpfs"); argv.add("/var/tmp");

        // The job's working directory bound RW at its real path so source-file
        // and binary paths created by the caller remain valid inside the sandbox
        argv.add("--bind");  argv.add(workDir.toString()); argv.add(workDir.toString());
        argv.add("--chdir"); argv.add(workDir.toString());

        // Namespace isolation — no network unless explicitly allowed
        argv.add("--unshare-all");
        if (limits.allowNetwork) {
            argv.add("--share-net");
        }

        // Drop to unprivileged UID inside the user namespace
        argv.add("--uid"); argv.add("65534");
        argv.add("--gid"); argv.add("65534");

        // Defence-in-depth
        argv.add("--new-session");        // detach from parent's controlling tty
        argv.add("--die-with-parent");    // kill sandbox if JVM dies (SIGKILL parent)
        argv.add("--cap-drop"); argv.add("ALL");
        argv.add("--clearenv");
        argv.add("--setenv"); argv.add("PATH"); argv.add(sandboxPath);
        argv.add("--setenv"); argv.add("HOME"); argv.add("/tmp");
        argv.add("--setenv"); argv.add("LANG"); argv.add("C.UTF-8");
        argv.add("--setenv"); argv.add("LC_ALL"); argv.add("C.UTF-8");
        argv.add("--hostname"); argv.add("sandbox");

        // ── 2. prlimit inside the sandbox ─────────────────────────────────────
        argv.add("--");
        argv.add(prlimitPath);
        // RLIMIT_AS — total virtual memory (note: JVM/Node need padding,
        //             handled by callers)
        argv.add("--as=" + (limits.memoryMB * 1024L * 1024L));
        // RLIMIT_CPU — CPU seconds (kills with SIGXCPU after limit)
        argv.add("--cpu=" + Math.max(1, limits.cpuSeconds));
        // RLIMIT_NPROC — max threads/processes for this user namespace
        argv.add("--nproc=" + limits.maxProcesses);
        // RLIMIT_FSIZE — max file size in bytes (prevents disk exhaustion)
        argv.add("--fsize=" + (limits.maxFileSizeMB * 1024L * 1024L));
        // RLIMIT_NOFILE — max open file descriptors
        argv.add("--nofile=" + limits.maxOpenFiles);

        // ── 3. The actual user command ────────────────────────────────────────
        argv.add("--");
        argv.addAll(rewritten);
        return argv;
    }

    /**
     * If argv[0] is a bare tool name we resolved at startup, replace it with the
     * absolute host path. Bare names that look like absolute paths or contain
     * a slash are passed through.
     */
    private List<String> rewriteFirstArg(List<String> command) {
        if (command == null || command.isEmpty()) return command;
        String first = command.get(0);
        if (first == null || first.isEmpty()) return command;
        if (first.startsWith("/") || first.contains("/")) return command;
        String resolved = binaryPaths.get(first);
        if (resolved == null) return command;
        List<String> out = new ArrayList<>(command.size());
        out.add(resolved);
        for (int i = 1; i < command.size(); i++) out.add(command.get(i));
        return out;
    }

    private static void addRoBind(List<String> argv, String src, String dst) {
        if (new File(src).exists()) {
            argv.add("--ro-bind"); argv.add(src); argv.add(dst);
        }
    }

    // ─── Limits builder ───────────────────────────────────────────────────────

    /** Resource limits for a sandboxed run. */
    public static final class SandboxLimits {
        public final int memoryMB;
        public final int cpuSeconds;
        public final int maxProcesses;
        public final int maxFileSizeMB;
        public final int maxOpenFiles;
        public final boolean allowNetwork;

        private SandboxLimits(int memoryMB, int cpuSeconds, int maxProcesses,
                              int maxFileSizeMB, int maxOpenFiles, boolean allowNetwork) {
            this.memoryMB      = memoryMB;
            this.cpuSeconds    = cpuSeconds;
            this.maxProcesses  = maxProcesses;
            this.maxFileSizeMB = maxFileSizeMB;
            this.maxOpenFiles  = maxOpenFiles;
            this.allowNetwork  = allowNetwork;
        }

        /** Compilation: bigger memory budget, longer CPU, more procs (compilers fork). */
        public static SandboxLimits forCompile() {
            return new SandboxLimits(
                // RLIMIT_AS (virtual memory). The JVM (javac) needs >4 GB of
                // virtual address space to start — on a 10 GB host the default
                // max heap alone is ~2.5 GB plus metaspace/code cache pushes it
                // past 4 GB, so the old 4096 MB floor made javac fail with
                // "Could not reserve enough space for object heap" (exit 1,
                // no output) inside bwrap. 8 GB is safe headroom.
                /* mem */     8192,
                /* cpu */     60,
                /* procs */   256,
                /* fsize */   64,
                /* nofile */  256,
                /* net */     false);
        }

        /**
         * Execution: tight budget driven by problem time/memory limits.
         * Caller is responsible for adding language-specific overhead
         * (JVM/Node need padding above the user-visible memoryMB).
         *
         * Note: memoryMB here is RLIMIT_AS — virtual address space, not RSS.
         * Modern JVMs reserve multi-GB of virtual space at startup even with
         * tiny -Xmx. We floor at 4 GB so JDK 21 java/javac can start; native
         * code runs ignore that floor and use the caller-supplied budget.
         */
        public static SandboxLimits forRun(int memoryMB, int cpuSeconds) {
            return new SandboxLimits(
                Math.max(64, memoryMB),
                Math.max(2, cpuSeconds),
                /* procs */  64,
                /* fsize */  16,
                /* nofile */ 64,
                /* net */    false);
        }

        /** Public interactive compiler: larger budget for input/output loops. */
        public static SandboxLimits forInteractive(int memoryMB) {
            return new SandboxLimits(
                Math.max(4096, memoryMB),
                /* cpu */    180,
                /* procs */  64,
                /* fsize */  16,
                /* nofile */ 128,
                /* net */    false);
        }
    }
}
