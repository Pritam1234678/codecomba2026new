package com.example.codecombat2026.service;

import com.example.codecombat2026.dto.ExecutionResult;
import com.example.codecombat2026.entity.Problem;
import com.example.codecombat2026.entity.Submission;
import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.entity.UserProblemSolved;
import com.example.codecombat2026.exception.ResourceNotFoundException;
import com.example.codecombat2026.repository.ProblemRepository;
import com.example.codecombat2026.repository.UserProblemSolvedRepository;
import com.example.codecombat2026.repository.UserRepository;
import com.example.codecombat2026.service.judge.DockerJudgeService;
import com.example.codecombat2026.util.TimeUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.*;

/**
 * Practice mode — fully separate from contests.
 *
 * Uses its own bounded thread pool so it doesn't compete with the contest
 * judge engine. Test cases are run via the same harness mechanism but
 * results are NEVER written to the submissions table.
 *
 * If user gets all test cases AC, awards points (5/7/10 by difficulty)
 * and records in user_problem_solved (one-time only).
 */
@Service
public class PracticeService {

    private static final Logger log = LoggerFactory.getLogger(PracticeService.class);

    @Value("${PRACTICE_WORKERS:4}")
    private int workerCount;

    @Value("${PRACTICE_QUEUE_SIZE:50}")
    private int queueSize;

    private static final java.util.concurrent.atomic.AtomicLong PRACTICE_THREAD_SEQ = new java.util.concurrent.atomic.AtomicLong();

    @Autowired private DockerJudgeService judgeService;
    @Autowired private CacheService cacheService;
    @Autowired private ProblemRepository problemRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private UserProblemSolvedRepository solvedRepository;

    private ThreadPoolExecutor pool;

    @PostConstruct
    public void init() {
        pool = new ThreadPoolExecutor(
            workerCount, workerCount,
            0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(queueSize),
            r -> {
                Thread t = new Thread(r);
                t.setName("practice-" + PRACTICE_THREAD_SEQ.incrementAndGet());
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.AbortPolicy()
        );
        log.info("✅ PracticeService ready — {} workers, {} queue capacity", workerCount, queueSize);
    }

    @PreDestroy
    public void shutdown() {
        if (pool != null) {
            pool.shutdown();
            try { if (!pool.awaitTermination(5, TimeUnit.SECONDS)) pool.shutdownNow(); }
            catch (InterruptedException e) { pool.shutdownNow(); Thread.currentThread().interrupt(); }
        }
    }

    /**
     * Run user's code through the problem's harness.
     * Returns AC/WA verdict + per-test-case details.
     * If AC and not already solved, awards points (idempotent).
     */
    public PracticeVerdict runPractice(Long userId, Long problemId, String code, String language) {
        Problem problem = problemRepository.findById(problemId)
            .orElseThrow(() -> new ResourceNotFoundException("Problem not found"));

        if (!Boolean.TRUE.equals(problem.getActive())) {
            return PracticeVerdict.error("This problem has been disabled by the administrator.");
        }

        String harness = cacheService.getSnippetHarness(problemId, language);
        if (harness == null) {
            return PracticeVerdict.error("No code harness configured for language: " + language);
        }

        Future<PracticeVerdict> future;
        try {
            future = pool.submit(() -> doJudge(userId, problem, code, language, harness));
        } catch (RejectedExecutionException e) {
            return PracticeVerdict.error("Server busy — too many practice runs in queue. Try again in a few seconds.");
        }

        try {
            return future.get(15, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            return PracticeVerdict.error("Judging took too long. Try again.");
        } catch (Exception e) {
            return PracticeVerdict.error("Internal error: " + e.getMessage());
        }
    }

    private PracticeVerdict doJudge(Long userId, Problem problem, String code, String language, String harness) {
        long startMs = System.currentTimeMillis();

        Submission.ProgrammingLanguage lang;
        try {
            lang = Submission.ProgrammingLanguage.valueOf(language.toUpperCase());
        } catch (Exception e) {
            return PracticeVerdict.error("Unsupported language: " + language);
        }

        // Inject user code into harness (same logic as worker pool)
        String executable = injectUserCode(harness, code, language);
        double timeLimit = problem.getTimeLimit() != null ? problem.getTimeLimit() : 5.0;
        int memoryLimit  = problem.getMemoryLimit() != null ? problem.getMemoryLimit() : 256;
        ExecutionResult result = judgeService.execute(executable, lang, timeLimit, memoryLimit, null);

        long elapsed = System.currentTimeMillis() - startMs;

        if (result.isCompilationError()) {
            return PracticeVerdict.compileError(result.getStderr(), elapsed);
        }
        if (result.isTimeLimitExceeded()) {
            return PracticeVerdict.tle(elapsed);
        }

        // Parse TC lines
        List<TcResult> tcs = parseTcLines(result.getStdout() != null ? result.getStdout() : "");
        if (tcs.isEmpty()) {
            String stderr = result.getStderr() != null ? result.getStderr() : "";
            return PracticeVerdict.runtimeError(stderr.isEmpty() ? "Harness produced no output" : stderr, elapsed);
        }

        int total  = tcs.size();
        int passed = (int) tcs.stream().filter(tc -> tc.passed).count();
        boolean allPassed = passed == total;

        PracticeVerdict v = new PracticeVerdict();
        v.status        = allPassed ? "AC" : "WA";
        v.passed        = passed;
        v.total         = total;
        v.executionTime = elapsed;
        v.testCases     = tcs;

        if (allPassed) {
            int awarded = awardPointsIfFirstSolve(userId, problem);
            v.pointsAwarded     = awarded;
            v.alreadySolved     = (awarded == 0);
            v.totalPointsByLevel = pointsForLevel(problem.getLevel());
        }
        return v;
    }

    /**
     * Award points only on first AC. Returns awarded amount (0 if already solved).
     * Atomic — uses unique constraint on (user_id, problem_id).
     */
    @Transactional
    private int awardPointsIfFirstSolve(Long userId, Problem problem) {
        if (solvedRepository.existsByUserIdAndProblemId(userId, problem.getId())) {
            return 0;
        }
        int points = pointsForLevel(problem.getLevel());
        try {
            UserProblemSolved entry = new UserProblemSolved();
            entry.setUserId(userId);
            entry.setProblemId(problem.getId());
            entry.setSolvedAt(TimeUtil.now());
            entry.setPointsEarned(points);
            solvedRepository.save(entry);

            // Increment user's total points
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            user.setTotalPoints((user.getTotalPoints() != null ? user.getTotalPoints() : 0) + points);
            userRepository.save(user);

            log.info("Practice: User {} solved problem {} (+{} pts)", userId, problem.getId(), points);
            return points;
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            
            return 0;
        }
    }

    private int pointsForLevel(String level) {
        if (level == null) return 5;
        switch (level.toUpperCase()) {
            case "EASY":   return 5;
            case "MEDIUM": return 7;
            case "HARD":   return 10;
            default:       return 5;
        }
    }

    // ─── Harness helpers (copied verbatim from SubmissionWorkerPool) ──────────

    private String injectUserCode(String harness, String userCode, String language) {
        boolean isPython = "PYTHON".equalsIgnoreCase(language);
        String startMarker = isPython ? "# USER_CODE_START" : "// USER_CODE_START";
        String endMarker   = isPython ? "# USER_CODE_END"   : "// USER_CODE_END";
        int s = harness.indexOf(startMarker), e = harness.indexOf(endMarker);
        if (s != -1 && e != -1 && e > s) {
            return harness.substring(0, s + startMarker.length()) + "\n" + userCode + "\n" + harness.substring(e);
        }
        return harness
            .replace("// USER_CODE_PLACEHOLDER", userCode)
            .replace("# USER_CODE_PLACEHOLDER", userCode)
            .replace("/* USER_CODE_PLACEHOLDER */", userCode);
    }

    private List<TcResult> parseTcLines(String stdout) {
        List<TcResult> list = new ArrayList<>();
        for (String line : stdout.split("\\r?\\n")) {
            line = line.trim();
            if (!line.startsWith("TC:")) continue;
            String[] parts = line.split(":", 6);
            if (parts.length < 3) continue;
            try {
                TcResult tc = new TcResult();
                tc.testCase = Integer.parseInt(parts[1]);
                tc.passed   = "PASS".equalsIgnoreCase(parts[2]);
                for (int p = 3; p < parts.length; p++) {
                    String part = parts[p];
                    if ("hidden".equalsIgnoreCase(part)) tc.hidden = true;
                    else if (part.startsWith("input="))   tc.input = part.substring(6);
                    else if (part.startsWith("expected=")) tc.expected = part.substring(9);
                    else if (part.startsWith("got="))      tc.got = part.substring(4);
                }
                list.add(tc);
            } catch (NumberFormatException ignored) {}
        }
        return list;
    }

    // ─── DTOs ─────────────────────────────────────────────────────────────────

    public static class PracticeVerdict {
        public String status;          // AC, WA, CE, RE, TLE, ERROR
        public int passed;
        public int total;
        public long executionTime;
        public List<TcResult> testCases;
        public String errorMessage;
        public int pointsAwarded;
        public boolean alreadySolved;
        public int totalPointsByLevel;

        public static PracticeVerdict error(String msg) {
            PracticeVerdict v = new PracticeVerdict();
            v.status = "ERROR"; v.errorMessage = msg; v.testCases = Collections.emptyList();
            return v;
        }
        public static PracticeVerdict compileError(String msg, long elapsed) {
            PracticeVerdict v = new PracticeVerdict();
            v.status = "CE"; v.errorMessage = msg; v.executionTime = elapsed; v.testCases = Collections.emptyList();
            return v;
        }
        public static PracticeVerdict runtimeError(String msg, long elapsed) {
            PracticeVerdict v = new PracticeVerdict();
            v.status = "RE"; v.errorMessage = msg; v.executionTime = elapsed; v.testCases = Collections.emptyList();
            return v;
        }
        public static PracticeVerdict tle(long elapsed) {
            PracticeVerdict v = new PracticeVerdict();
            v.status = "TLE"; v.executionTime = elapsed; v.testCases = Collections.emptyList();
            return v;
        }
    }

    public static class TcResult {
        public int testCase;
        public boolean passed;
        public boolean hidden;
        public String input;
        public String expected;
        public String got;
    }
}
