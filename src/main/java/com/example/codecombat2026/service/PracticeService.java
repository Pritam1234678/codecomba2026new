package com.example.codecombat2026.service;

import com.example.codecombat2026.dto.ExecutionResult;
import com.example.codecombat2026.dto.SubmissionJob;
import com.example.codecombat2026.dto.VerdictEvent;
import com.example.codecombat2026.entity.Problem;
import com.example.codecombat2026.entity.Submission;
import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.entity.UserProblemSolved;
import com.example.codecombat2026.exception.ResourceNotFoundException;
import com.example.codecombat2026.repository.ProblemRepository;
import com.example.codecombat2026.repository.SubmissionRepository;
import com.example.codecombat2026.repository.UserProblemSolvedRepository;
import com.example.codecombat2026.repository.UserRepository;
import com.example.codecombat2026.service.judge.DockerJudgeService;
import com.example.codecombat2026.service.judge.SandboxRunner;
import com.example.codecombat2026.util.TimeUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Practice mode — fully async, judged by the shared Valkey-backed worker pool.
 *
 * The old sync thread-pool approach blocked HTTP threads and could not scale
 * across VMs.  Now practice submissions are pushed onto {@code submission:queue}
 * (the same queue as contest/duel), the {@link SubmissionWorkerPool} judges
 * them, and the verdict is delivered via SSE — identical to contest mode.
 *
 * Points (5/7/10 by difficulty) are awarded on first AC, idempotently guarded
 * by the unique constraint on (user_id, problem_id).
 */
@Service
public class PracticeService {

    private static final Logger log = LoggerFactory.getLogger(PracticeService.class);

    private static final int  PRACTICE_RUN_LIMIT      = 20;
    private static final int  PRACTICE_RUN_WINDOW_SEC = 60;

    @Autowired private ProblemRepository problemRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private UserProblemSolvedRepository solvedRepository;
    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private CacheService cacheService;
    @Autowired private StringRedisTemplate redis;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private SandboxRunner sandbox;

    @PostConstruct
    public void init() {
        if (!sandbox.isEnabled()) {
            log.warn("⚠️  Sandbox is DISABLED on this node — practice code runs with full host privileges. "
                + "Set SANDBOX_ENABLED=true to enable sandbox.");
        }
        log.info("✅ PracticeService ready — async Valkey queue, rate limit {} runs/{}s per problem",
            PRACTICE_RUN_LIMIT, PRACTICE_RUN_WINDOW_SEC);
    }

    /**
     * Enqueue a practice run for async judging.
     *
     * @return the submission id so the caller can correlate the SSE verdict.
     */
    public Long enqueuePractice(Long userId, Long problemId, String code, String language) {
        Problem problem = problemRepository.findById(problemId)
            .orElseThrow(() -> new ResourceNotFoundException("Problem not found"));

        if (!Boolean.TRUE.equals(problem.getActive())) {
            throw new IllegalArgumentException("This problem has been disabled by the administrator.");
        }

        String harness = cacheService.getSnippetHarness(problemId, language);
        if (harness == null) {
            throw new IllegalArgumentException("No code harness configured for language: " + language);
        }

        // Rate limit: N runs per problem per sliding window
        String rateKey = "practice:runs:" + userId + ":" + problemId;
        Long runCount = redis.opsForValue().increment(rateKey);
        if (runCount != null && runCount == 1) {
            redis.expire(rateKey, java.time.Duration.ofSeconds(PRACTICE_RUN_WINDOW_SEC));
        }
        if (runCount != null && runCount > PRACTICE_RUN_LIMIT) {
            throw new IllegalArgumentException("Rate limit: max " + PRACTICE_RUN_LIMIT
                + " practice runs per " + PRACTICE_RUN_WINDOW_SEC + "s per problem.");
        }

        Submission.ProgrammingLanguage lang;
        try {
            lang = Submission.ProgrammingLanguage.valueOf(language.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Unsupported language: " + language);
        }

        // Create a PENDING submission row in DB so the user can poll / track history
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Submission sub = new Submission();
        sub.setUser(user);
        sub.setProblem(problem);
        sub.setContest(null);
        sub.setCode(code);
        sub.setLanguage(lang);
        sub.setSubmittedAt(TimeUtil.now());
        sub.setStatus(Submission.SubmissionStatus.PENDING);
        sub = submissionRepository.save(sub);

        double timeLimit = problem.getTimeLimit() != null ? problem.getTimeLimit() : 5.0;
        int memoryLimit  = problem.getMemoryLimit() != null ? problem.getMemoryLimit() : 256;

        SubmissionJob job = new SubmissionJob();
        job.setSubmissionId(sub.getId());
        job.setUserId(userId);
        job.setProblemId(problemId);
        job.setContestId(problem.getContestId());
        job.setCode(code);
        job.setLanguage(language);
        job.setTimeLimit(timeLimit);
        job.setMemoryLimit(memoryLimit);
        job.setTestRun(false);
        job.setPractice(true);

        try {
            String json = objectMapper.writeValueAsString(job);
            redis.opsForList().leftPush(SubmissionWorkerPool.QUEUE_KEY, json);
            log.info("Practice run {} enqueued (user={}, problem={})", sub.getId(), userId, problemId);
        } catch (Exception e) {
            submissionRepository.updateStatus(sub.getId(), Submission.SubmissionStatus.RE);
            throw new RuntimeException("Failed to enqueue practice run", e);
        }

        return sub.getId();
    }

    /**
     * Convenience overload — looks up the Problem and delegates.
     * Called by {@link SubmissionWorkerPool} which only has problemId.
     */
    public int awardPointsIfFirstSolve(Long userId, Long problemId) {
        Problem p = problemRepository.findById(problemId)
            .orElse(null);
        if (p == null) return 0;
        return awardPointsIfFirstSolve(userId, p);
    }

    /**
     * Award points only on first AC. Returns awarded amount (0 if already solved).
     * Called by {@link SubmissionWorkerPool} when a practice submission reaches AC.
     * Atomic — uses unique constraint on (user_id, problem_id).
     */
    @Transactional
    public int awardPointsIfFirstSolve(Long userId, Problem problem) {
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

    /** Points awarded for solving a problem of the given difficulty. */
    public int pointsForLevel(String level) {
        if (level == null) return 5;
        switch (level.toUpperCase()) {
            case "EASY":   return 5;
            case "MEDIUM": return 7;
            case "HARD":   return 10;
            default:       return 5;
        }
    }

    // ─── DTOs (kept for polling fallback) ────────────────────────────────

    public static class PracticeVerdict {
        public String status;
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
