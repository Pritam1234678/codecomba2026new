package com.example.codecombat2026.service;

import com.example.codecombat2026.dto.SubmissionJob;
import com.example.codecombat2026.dto.duel.DuelMatchView;
import com.example.codecombat2026.dto.duel.DuelMetrics;
import com.example.codecombat2026.duel.Seat;
import com.example.codecombat2026.duel.SeatAssigner;
import com.example.codecombat2026.entity.DuelMatch;
import com.example.codecombat2026.entity.DuelSubmission;
import com.example.codecombat2026.entity.Problem;
import com.example.codecombat2026.entity.Submission;
import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.exception.DuelForbiddenException;
import com.example.codecombat2026.exception.DuelNotFoundException;
import com.example.codecombat2026.exception.DuelStateConflictException;
import com.example.codecombat2026.repository.DuelEligibleProblemRepository;
import com.example.codecombat2026.repository.DuelMatchRepository;
import com.example.codecombat2026.repository.DuelSubmissionRepository;
import com.example.codecombat2026.repository.ProblemRepository;
import com.example.codecombat2026.repository.SubmissionRepository;
import com.example.codecombat2026.repository.UserRepository;
import com.example.codecombat2026.util.TimeUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * Live Duel Mode lifecycle service.
 *
 * <p>Implements the full duel state machine: pairing, problem selection,
 * submission entry, verdict callback, win adjudication, draw / reconnect /
 * forfeit / admin-cancel finalization, and JVM-restart recovery. All
 * mutating paths use the conditional UPDATE pattern on
 * {@link DuelMatchRepository} so racing AC verdicts, draw timer, reconnect
 * timer, forfeit, and admin-cancel each see exactly one "I won the race"
 * row count of {@code 1} — every other path observes {@code 0} and
 * defers to the row that was already written.
 *
 * <p>The service owns two {@link ScheduledExecutorService}s:
 * <ul>
 *   <li>{@code drawTimerExec} — schedules the per-match 600-second draw
 *       timer (Requirement 6.6) and the dual-disconnect combined draw
 *       timer (Requirement 7.4).</li>
 *   <li>{@code reconnectTimerExec} — schedules per-(match, user)
 *       30-second reconnect-grace timers (Requirements 7.1 / 7.3).</li>
 * </ul>
 *
 * <p>The {@link MatchmakingService} dependency is injected via
 * {@link Lazy @Lazy} solely so the {@code queueDepth()} metric can be
 * surfaced via {@link #getMetrics()} without creating a startup cycle.
 *
 * <p>Forfeit semantics note: Requirement 7.5 specifies the opponent as the
 * winner of a forfeit, which conflicts with the V3 CHECK constraint that
 * forces {@code winner_user_id IS NULL} when {@code outcome='ABANDONED'}.
 * The conflict is resolved by recording the forfeit as
 * {@code USER_A_WIN} / {@code USER_B_WIN} (whichever seat the opponent
 * holds), preserving "the opponent wins" semantics; the {@code ABANDONED}
 * outcome is reserved for paths that genuinely produce no winner — admin
 * cancel and dual-disconnect-as-DRAW (which uses outcome {@code DRAW}
 * for stricter semantic correctness with Req 7.4).
 */
@Service
public class DuelService {

    private static final Logger log = LoggerFactory.getLogger(DuelService.class);

    // ─── Valkey key constants (mirrored from MatchmakingService for clarity) ──

    /** Redundant-with-DB win-claim flag (Requirement 9.2). */
    static final String WINNER_PREFIX = "duel:winner:";

    /** Per-user post-match cooldown (Requirements 10.3 / 10.4). */
    static final String COOLDOWN_PREFIX = "duel:cooldown:";

    /** Mirror of the matchmaking sorted-pair create-lock (Requirement 8.4). */
    static final String CREATE_LOCK_PREFIX = "duel:create:";

    /** Win-claim key TTL — 2 h, well past the 600 s match budget. */
    private static final long WINNER_KEY_TTL_SEC = 7200L;

    /** Heartbeat-to-opponent rate limit per Property 16 (1 emit per 1500 ms per user). */
    private static final long TYPING_HEARTBEAT_MIN_INTERVAL_MS = 1500L;

    // ─── Run/Submit limits per match (V4 rework) ─────────────────────────────

    /** Maximum compile-and-run-against-examples invocations per (match, user). */
    static final int RUN_LIMIT_PER_MATCH = 5;

    /** Maximum full-judge submissions per (match, user). */
    static final int SUBMIT_LIMIT_PER_MATCH = 2;

    /** Valkey key prefix for run counters. */
    static final String RUN_COUNTER_PREFIX = "duel:runs:";

    /** Valkey key prefix for submit counters. */
    static final String SUBMIT_COUNTER_PREFIX = "duel:submits:";

    /** Difficulty → time-limit (seconds) mapping for V4 matches. */
    private static int timeLimitFor(String difficulty) {
        if (difficulty == null) return 2400;
        return switch (difficulty.toUpperCase()) {
            case "EASY" -> 1200;
            case "HARD" -> 3900;
            default -> 2400; // MEDIUM and any unknown bucket
        };
    }

    // ─── Configurable durations ──────────────────────────────────────────────

    @Value("${DUEL_DRAW_TIMEOUT_SEC:600}")
    long drawTimeoutSec;

    @Value("${DUEL_GRACE_PERIOD_SEC:30}")
    long gracePeriodSec;

    @Value("${DUEL_COOLDOWN_SEC:5}")
    long cooldownSec;

    // ─── Dependencies ────────────────────────────────────────────────────────

    private final DuelMatchRepository duelMatchRepository;
    private final DuelSubmissionRepository duelSubmissionRepository;
    private final DuelEligibleProblemRepository duelEligibleProblemRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final ProblemRepository problemRepository;
    private final StringRedisTemplate redis;
    private final DuelSseEmitterRegistry duelSseEmitterRegistry;
    private final SseEmitterRegistry sseEmitterRegistry;
    private final MatchmakingService matchmakingService;
    private final ObjectMapper objectMapper;
    /** Compile + run service used by the synchronous /run endpoint. */
    private final CompilerService compilerService;

    // ─── In-memory timer state ───────────────────────────────────────────────

    /** Per-match draw timer (also used for the dual-disconnect combined timer). */
    private final ConcurrentHashMap<UUID, ScheduledFuture<?>> drawTimers = new ConcurrentHashMap<>();

    /**
     * Reconnect-grace timers keyed by {@code matchId + ":" + userId}. The
     * dual-disconnect combined timer reuses the same map under the
     * synthetic key {@code matchId + ":both"} so a re-subscription from
     * either user can cancel it.
     */
    private final ConcurrentHashMap<String, ScheduledFuture<?>> reconnectTimers = new ConcurrentHashMap<>();

    /** Last-emit timestamp per user for the typing heartbeat rate-limit. */
    private final ConcurrentHashMap<Long, AtomicLong> typingHeartbeatLastEmitMs = new ConcurrentHashMap<>();

    /** Two thread executors so draw and reconnect work do not contend. */
    private final ScheduledExecutorService drawTimerExec;
    private final ScheduledExecutorService reconnectTimerExec;

    public DuelService(DuelMatchRepository duelMatchRepository,
                       DuelSubmissionRepository duelSubmissionRepository,
                       DuelEligibleProblemRepository duelEligibleProblemRepository,
                       SubmissionRepository submissionRepository,
                       UserRepository userRepository,
                       ProblemRepository problemRepository,
                       StringRedisTemplate redis,
                       DuelSseEmitterRegistry duelSseEmitterRegistry,
                       SseEmitterRegistry sseEmitterRegistry,
                       @Lazy MatchmakingService matchmakingService,
                       ObjectMapper objectMapper,
                       CompilerService compilerService) {
        this.duelMatchRepository = duelMatchRepository;
        this.duelSubmissionRepository = duelSubmissionRepository;
        this.duelEligibleProblemRepository = duelEligibleProblemRepository;
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
        this.problemRepository = problemRepository;
        this.redis = redis;
        this.duelSseEmitterRegistry = duelSseEmitterRegistry;
        this.sseEmitterRegistry = sseEmitterRegistry;
        this.matchmakingService = matchmakingService;
        this.objectMapper = objectMapper;
        this.compilerService = compilerService;

        this.drawTimerExec = Executors.newScheduledThreadPool(2, namedThreadFactory("duel-draw-"));
        this.reconnectTimerExec = Executors.newScheduledThreadPool(4, namedThreadFactory("duel-reconnect-"));
    }

    private static ThreadFactory namedThreadFactory(String prefix) {
        AtomicLongArray counter = new AtomicLongArray(1);
        return r -> {
            long n = counter.incrementAndGet(0);
            Thread t = new Thread(r);
            t.setName(prefix + n);
            t.setDaemon(true);
            return t;
        };
    }

    // ─────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────

    @PostConstruct
    void init() {
        // Wire the SSE close-callback once the bean is fully constructed.
        duelSseEmitterRegistry.setLastSubscriptionClosedCallback(this::onLastSubscriptionClosed);
        try {
            recoverInProgressMatches();
        } catch (RuntimeException ex) {
            log.error("DuelService recovery failed: {}", ex.getMessage(), ex);
        }
    }

    @PreDestroy
    void shutdown() {
        shutdownExec(drawTimerExec, "duel-draw");
        shutdownExec(reconnectTimerExec, "duel-reconnect");
    }

    private static void shutdownExec(ScheduledExecutorService exec, String name) {
        try {
            exec.shutdown();
            if (!exec.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("{} executor did not terminate in 5s, forcing shutdown", name);
                exec.shutdownNow();
            }
        } catch (InterruptedException ie) {
            exec.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * On JVM start, scan {@code duel_matches WHERE status='IN_PROGRESS'}.
     * For each row, either reschedule the draw timer (if the match still
     * has remaining time) or finalize as {@code ABANDONED} immediately
     * (Requirement 9.5).
     */
    void recoverInProgressMatches() {
        List<DuelMatch> active = duelMatchRepository.findAllInProgress();
        if (active.isEmpty()) {
            return;
        }
        log.info("Duel recovery: {} IN_PROGRESS match(es) found", active.size());
        LocalDateTime now = TimeUtil.now();

        for (DuelMatch match : active) {
            UUID matchId = match.getMatchId();
            LocalDateTime startedAt = match.getStartedAt();
            if (startedAt == null) {
                // Defensive — a row without started_at cannot have its remaining
                // window computed; finalize as abandoned.
                int rows = duelMatchRepository.adminCancelIfActive(matchId, now);
                if (rows == 1) {
                    log.warn("Duel recovery: match {} had no started_at, finalized as ABANDONED", matchId);
                    setCooldown(match.getUserAId());
                    setCooldown(match.getUserBId());
                }
                continue;
            }
            long elapsedSec = Duration.between(startedAt, now).getSeconds();
            // V4 — read per-match window from the row instead of the global
            // @Value drawTimeoutSec, so EASY/MEDIUM/HARD windows are honored
            // across JVM restarts. Falls back to drawTimeoutSec for legacy
            // pre-V4 rows that somehow lack timeLimitSec.
            int rowLimit = match.getTimeLimitSec() != null
                    ? match.getTimeLimitSec()
                    : (int) drawTimeoutSec;
            long remainingSec = rowLimit - elapsedSec;
            if (remainingSec > 0) {
                scheduleDrawTimer(matchId, remainingSec);
                log.info("Duel recovery: rescheduled draw timer for match {} ({} s remaining)",
                        matchId, remainingSec);
            } else {
                int rows = duelMatchRepository.adminCancelIfActive(matchId, now);
                if (rows == 1) {
                    log.info("Duel recovery: match {} expired during downtime, finalized as ABANDONED",
                            matchId);
                    setCooldown(match.getUserAId());
                    setCooldown(match.getUserBId());
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // pairAndStart
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Pair two users into a brand-new {@code duel_matches} row, transition
     * to {@code IN_PROGRESS}, schedule the draw timer, and emit
     * {@code matched} on the per-user SSE channel.
     *
     * <p>V4: difficulty-bucketed pairing. The problem is drawn from the
     * subset of {@code duel_eligible_problems} whose {@code problems.level}
     * matches {@code difficulty}; both-solved exclusion has been removed.
     * The per-match time-limit is derived from difficulty
     * (EASY=1200s, MEDIUM=2400s, HARD=3900s) and persisted on the row so
     * the draw timer can rehydrate after a JVM restart without consulting
     * any global config.
     *
     * @param userIdA      first participant
     * @param userIdB      second participant
     * @param difficulty   bucket the pair came from (EASY/MEDIUM/HARD)
     * @return the freshly-assigned {@code matchId}
     */
    public UUID pairAndStart(Long userIdA, Long userIdB, String difficulty) {
        if (userIdA == null || userIdB == null) {
            throw new IllegalArgumentException("Duel participants must not be null");
        }
        if (userIdA.equals(userIdB)) {
            throw new IllegalArgumentException("Duel participants must be distinct: " + userIdA);
        }
        String normalizedDifficulty = difficulty == null ? "MEDIUM"
                : difficulty.trim().toUpperCase();
        int timeLimitSec = timeLimitFor(normalizedDifficulty);

        long[] sorted = SeatAssigner.orderedPair(userIdA, userIdB);
        Long minId = sorted[0];
        Long maxId = sorted[1];

        // V4 — pull eligible problems by difficulty level. No both-solved
        // filtering; user explicitly said dropping that exclusion is fine.
        List<Long> eligible = duelEligibleProblemRepository.findEligibleByLevel(normalizedDifficulty);
        if (eligible.isEmpty()) {
            log.warn("duel.problem_pool.empty_for_level level={} users=({},{})",
                    normalizedDifficulty, minId, maxId);
            Map<String, Object> payload = new HashMap<>();
            payload.put("userA", minId);
            payload.put("userB", maxId);
            payload.put("difficulty", normalizedDifficulty);
            throw new DuelStateConflictException("NO_ELIGIBLE_PROBLEM", payload,
                    "No eligible problem available for level " + normalizedDifficulty);
        }
        Long problemId = eligible.get(
                java.util.concurrent.ThreadLocalRandom.current().nextInt(eligible.size()));

        UUID matchId = UUID.randomUUID();

        try {
            createAndStartMatchRow(matchId, minId, maxId, problemId, normalizedDifficulty, timeLimitSec);
        } catch (DataIntegrityViolationException dive) {
            log.info("pairAndStart concurrent_match users=({},{}) match={} — partial-unique violation",
                    minId, maxId, matchId);
            Map<String, Object> payload = new HashMap<>();
            payload.put("userA", minId);
            payload.put("userB", maxId);
            throw new DuelStateConflictException("CONCURRENT_MATCH", payload,
                    "User already has an active match");
        }

        // Schedule the draw timer with the per-match window.
        scheduleDrawTimer(matchId, timeLimitSec);

        String userAUsername = lookupUsername(minId);
        String userBUsername = lookupUsername(maxId);
        LocalDateTime startedAt = duelMatchRepository.findById(matchId)
                .map(DuelMatch::getStartedAt)
                .orElse(TimeUtil.now());

        emitMatched(minId, matchId, userBUsername, problemId, startedAt, normalizedDifficulty, timeLimitSec);
        emitMatched(maxId, matchId, userAUsername, problemId, startedAt, normalizedDifficulty, timeLimitSec);

        log.info("Duel paired match={} users=({},{}) problem={} difficulty={} window={}s",
                matchId, minId, maxId, problemId, normalizedDifficulty, timeLimitSec);
        invalidateMetricsCache();
        return matchId;
    }

    /**
     * Backward-compatible overload — defaults to MEDIUM. Kept for callers
     * that may not yet pass the difficulty (e.g. older tests / scripts).
     * Production matchmaker always calls the 3-arg form.
     */
    public UUID pairAndStart(Long userIdA, Long userIdB) {
        return pairAndStart(userIdA, userIdB, "MEDIUM");
    }

    /**
     * Insert the {@code WAITING} row and transition it to {@code IN_PROGRESS}
     * inside a single transaction so the partial-unique-index check fires
     * at commit time.
     */
    @Transactional
    void createAndStartMatchRow(UUID matchId, Long minId, Long maxId, Long problemId,
                                String difficulty, int timeLimitSec) {
        DuelMatch match = new DuelMatch();
        match.setMatchId(matchId);
        match.setUserAId(minId);
        match.setUserBId(maxId);
        match.setProblemId(problemId);
        match.setStatus(DuelMatch.Status.WAITING);
        match.setCreatedAt(TimeUtil.now());
        match.setDifficulty(difficulty);
        match.setTimeLimitSec(timeLimitSec);

        duelMatchRepository.save(match);
        duelMatchRepository.flush();

        int rows = duelMatchRepository.startIfWaiting(matchId, TimeUtil.now());
        if (rows != 1) {
            throw new IllegalStateException(
                    "startIfWaiting returned " + rows + " for match " + matchId);
        }
    }

    private void emitMatched(Long userId, UUID matchId, String opponentUsername,
                             Long problemId, LocalDateTime startedAt,
                             String difficulty, int timeLimitSec) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("matchId", matchId.toString());
        payload.put("opponentUsername", opponentUsername);
        payload.put("problemId", problemId);
        payload.put("startedAt", startedAt != null ? startedAt.toString() : null);
        payload.put("difficulty", difficulty);
        payload.put("timeLimitSec", timeLimitSec);
        try {
            sseEmitterRegistry.sendEvent(userId, "matched", payload);
        } catch (Exception e) {
            log.debug("Failed to emit matched event for user {}: {}", userId, e.getMessage());
        }
    }

    private String lookupUsername(Long userId) {
        try {
            return userRepository.findById(userId).map(User::getUsername).orElse(null);
        } catch (Exception e) {
            log.debug("Username lookup failed for user {}: {}", userId, e.getMessage());
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // getMatch / listMatches
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Read-model lookup. Returns 404 via {@link DuelNotFoundException} if
     * no row exists, and 403 via {@link DuelForbiddenException} if the
     * caller is not one of the two participants (Requirement 13.5).
     */
    public DuelMatchView getMatch(UUID matchId, Long requesterId) {
        if (matchId == null) {
            throw new DuelNotFoundException("Match id must not be null");
        }
        DuelMatch match = duelMatchRepository.findById(matchId)
                .orElseThrow(() -> new DuelNotFoundException("Match not found: " + matchId));
        if (requesterId == null
                || (!requesterId.equals(match.getUserAId())
                && !requesterId.equals(match.getUserBId()))) {
            throw new DuelForbiddenException("Not a participant of match " + matchId);
        }
        Seat yourSeat = SeatAssigner.seatFor(match.getUserAId(), match.getUserBId(), requesterId);
        return toView(match, yourSeat, requesterId);
    }

    /** Admin-listing variant that does not enforce participant-only access. */
    public Page<DuelMatchView> listMatches(String status, int limit, int offset) {
        if (limit <= 0) {
            limit = 25;
        }
        if (offset < 0) {
            offset = 0;
        }
        DuelMatch.Status statusEnum;
        try {
            statusEnum = DuelMatch.Status.valueOf(status);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new IllegalArgumentException("Unknown duel status: " + status);
        }
        int pageNumber = offset / limit;
        Pageable pageable = PageRequest.of(pageNumber, limit,
                Sort.by(Sort.Direction.DESC, "startedAt"));
        Page<DuelMatch> page = duelMatchRepository.findByStatus(statusEnum, pageable);
        List<DuelMatchView> views = page.getContent().stream()
                .map(m -> toView(m, null, false, null))
                .toList();
        return new PageImpl<>(views, pageable, page.getTotalElements());
    }

    /**
     * Returns the calling user's most recent FINISHED duel matches as
     * compact history rows. The frontend lobby ("Recent Duels" card)
     * polls this once per page-load.
     *
     * @param userId the requesting user
     * @param limit  max rows to return (clamped to {@code [1, 50]})
     * @return rows ordered by ended_at DESC, opponent and problem title
     *         resolved server-side
     */
    public List<DuelHistoryEntry> getDuelHistory(Long userId, int limit) {
        if (userId == null) return List.of();
        int safeLimit = Math.max(1, Math.min(50, limit));
        Pageable pageable = PageRequest.of(0, safeLimit);
        List<DuelMatch> rows = duelMatchRepository.findFinishedByUser(userId, pageable);
        List<DuelHistoryEntry> out = new java.util.ArrayList<>(rows.size());
        for (DuelMatch m : rows) {
            Long opponentId = userId.equals(m.getUserAId()) ? m.getUserBId() : m.getUserAId();
            String opponentUsername = lookupUsername(opponentId);
            String problemTitle = null;
            if (m.getProblemId() != null) {
                try {
                    problemTitle = problemRepository.findById(m.getProblemId())
                            .map(Problem::getTitle).orElse(null);
                } catch (Exception ignored) { /* nullable */ }
            }
            String outcome = m.getOutcome() != null ? m.getOutcome().name() : null;
            out.add(new DuelHistoryEntry(
                    m.getMatchId(),
                    opponentUsername,
                    m.getProblemId(),
                    problemTitle,
                    outcome,
                    m.getWinnerUserId(),
                    m.getEndedAt()
            ));
        }
        return out;
    }

    /**
     * Per-user, per-match submission list — the frontend rebuilds the
     * "Your submissions" panel from this on mount so a refresh inside
     * the arena keeps the verdict history.
     */
    public List<DuelSubmissionView> getMatchSubmissions(UUID matchId, Long userId) {
        if (matchId == null || userId == null) return List.of();
        // Participant gate — same logic as getMatch but without throwing
        // 404 if the match is gone (we just return empty).
        DuelMatch match = duelMatchRepository.findById(matchId).orElse(null);
        if (match == null) return List.of();
        if (!userId.equals(match.getUserAId()) && !userId.equals(match.getUserBId())) {
            throw new DuelForbiddenException("Not a participant of match " + matchId);
        }

        // Pull duel_submissions for this match, then for each one fetch
        // the underlying Submission row and filter to the calling user.
        List<DuelSubmission> links = duelSubmissionRepository.findByMatchId(matchId);
        List<DuelSubmissionView> out = new java.util.ArrayList<>();
        for (DuelSubmission link : links) {
            Submission sub = submissionRepository.findById(link.getSubmissionId()).orElse(null);
            if (sub == null) continue;
            if (sub.getUser() == null || !userId.equals(sub.getUser().getId())) continue;
            out.add(new DuelSubmissionView(
                    sub.getId(),
                    sub.getStatus() != null ? sub.getStatus().name() : null,
                    sub.getTestCasesPassed() != null ? sub.getTestCasesPassed() : 0,
                    sub.getTotalTestCases() != null ? sub.getTotalTestCases() : 0,
                    sub.getLanguage() != null ? sub.getLanguage().name() : null,
                    sub.getSubmittedAt(),
                    Boolean.TRUE.equals(link.isFirstAc())
            ));
        }
        // Newest first.
        out.sort((a, b) -> {
            if (a.submittedAt() == null) return 1;
            if (b.submittedAt() == null) return -1;
            return b.submittedAt().compareTo(a.submittedAt());
        });
        return out;
    }

    /** Compact projection of a duel-tagged submission for the arena history list. */
    public record DuelSubmissionView(
            Long submissionId,
            String status,
            int testCasesPassed,
            int totalTestCases,
            String language,
            LocalDateTime submittedAt,
            boolean firstAc
    ) {}

    /** Compact projection of a finished duel for the lobby history table. */
    public record DuelHistoryEntry(
            UUID matchId,
            String opponentUsername,
            Long problemId,
            String problemTitle,
            String outcome,
            Long winnerUserId,
            LocalDateTime endedAt
    ) {}

    private DuelMatchView toView(DuelMatch match, Seat yourSeat) {
        return toView(match, yourSeat, true, null);
    }

    private DuelMatchView toView(DuelMatch match, Seat yourSeat, Long requesterUserId) {
        return toView(match, yourSeat, true, requesterUserId);
    }

    /**
     * @param includeProblem when true, fetches and inlines the problem
     *                       statement (used by participant-facing
     *                       {@code getMatch}). Admin listing views pass
     *                       {@code false} to skip the per-row problem
     *                       fetch.
     * @param requesterUserId user whose run/submit counters to inline; null
     *                       for admin views.
     */
    private DuelMatchView toView(DuelMatch match, Seat yourSeat, boolean includeProblem,
                                 Long requesterUserId) {
        String userAUsername = lookupUsername(match.getUserAId());
        String userBUsername = lookupUsername(match.getUserBId());

        // Per-row time-limit (V4). Falls back to the global drawTimeoutSec
        // for any pre-V4 row that somehow has timeLimitSec=null.
        int rowLimitSec = match.getTimeLimitSec() != null
                ? match.getTimeLimitSec()
                : (int) drawTimeoutSec;

        Long elapsedSec = null;
        Long remainingSec = null;
        LocalDateTime startedAt = match.getStartedAt();
        if (match.getStatus() == DuelMatch.Status.IN_PROGRESS && startedAt != null) {
            long elapsed = Math.max(0L,
                    Duration.between(startedAt, TimeUtil.now()).getSeconds());
            elapsedSec = elapsed;
            remainingSec = Math.max(0L, rowLimitSec - elapsed);
        } else if (match.getStatus() == DuelMatch.Status.FINISHED && startedAt != null
                && match.getEndedAt() != null) {
            elapsedSec = Math.max(0L,
                    Duration.between(startedAt, match.getEndedAt()).getSeconds());
        }

        DuelMatchView.DuelProblemView problemView = null;
        if (includeProblem && match.getProblemId() != null) {
            try {
                problemView = problemRepository.findById(match.getProblemId())
                        .map(p -> new DuelMatchView.DuelProblemView(
                                p.getId(),
                                p.getTitle(),
                                p.getDescription(),
                                p.getInputFormat(),
                                p.getOutputFormat(),
                                p.getConstraints(),
                                p.getExample1(),
                                p.getExample2(),
                                p.getExample3(),
                                p.getTimeLimit(),
                                p.getMemoryLimit(),
                                p.getLevel()))
                        .orElse(null);
            } catch (Exception e) {
                log.debug("Problem inline lookup failed for problemId={}: {}",
                        match.getProblemId(), e.getMessage());
            }
        }

        // Run/submit counters — fetched from Valkey only for participant calls.
        Integer runsUsed = null, submitsUsed = null, runsRemaining = null, submitsRemaining = null;
        if (requesterUserId != null) {
            int rUsed = getRunCount(match.getMatchId(), requesterUserId);
            int sUsed = getSubmitCount(match.getMatchId(), requesterUserId);
            runsUsed = rUsed;
            submitsUsed = sUsed;
            runsRemaining = Math.max(0, RUN_LIMIT_PER_MATCH - rUsed);
            submitsRemaining = Math.max(0, SUBMIT_LIMIT_PER_MATCH - sUsed);
        }

        return new DuelMatchView(
                match.getMatchId(),
                match.getUserAId(),
                userAUsername,
                match.getUserBId(),
                userBUsername,
                match.getProblemId(),
                problemView,
                match.getStatus() != null ? match.getStatus().name() : null,
                match.getOutcome() != null ? match.getOutcome().name() : null,
                match.getWinnerUserId(),
                match.getWinnerUserId() != null ? lookupUsername(match.getWinnerUserId()) : null,
                startedAt,
                match.getEndedAt(),
                elapsedSec,
                remainingSec,
                yourSeat != null ? yourSeat.name() : null,
                match.getDifficulty(),
                rowLimitSec,
                runsUsed,
                submitsUsed,
                runsRemaining,
                submitsRemaining
        );
    }

    // ─────────────────────────────────────────────────────────────────────
    // submitForDuel
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Build a {@code submissions} row plus its {@code duel_submissions}
     * link, push a {@code SubmissionJob} (with {@code duelId}) onto
     * {@code submission:queue}, and emit {@code progress {event:'submitted'}}.
     *
     * @return the assigned {@code submissionId}
     */
    @Transactional
    public Long submitForDuel(UUID matchId, Long userId, String code, String language) {
        if (matchId == null) {
            throw new DuelNotFoundException("Match id must not be null");
        }
        if (userId == null) {
            throw new DuelForbiddenException("Authenticated user required");
        }
        if (language == null || language.isBlank()) {
            throw new IllegalArgumentException("language is required");
        }

        DuelMatch match = duelMatchRepository.findById(matchId)
                .orElseThrow(() -> new DuelNotFoundException("Match not found: " + matchId));
        if (!userId.equals(match.getUserAId()) && !userId.equals(match.getUserBId())) {
            throw new DuelForbiddenException("Not a participant of match " + matchId);
        }
        if (match.getStatus() == DuelMatch.Status.FINISHED) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("matchId", matchId.toString());
            payload.put("outcome", match.getOutcome() != null ? match.getOutcome().name() : null);
            throw new DuelStateConflictException("MATCH_FINISHED", payload,
                    "Match already finished: " + matchId);
        }
        if (match.getStatus() != DuelMatch.Status.IN_PROGRESS) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("matchId", matchId.toString());
            payload.put("status", match.getStatus().name());
            throw new DuelStateConflictException("MATCH_NOT_ACTIVE", payload,
                    "Match is not in progress: " + matchId);
        }

        // V4 — per-match submit limit (2/match/user). Throws SUBMIT_LIMIT_EXCEEDED
        // (409) if the user has already used both submits.
        tryIncrementSubmit(matchId, userId, match);

        Submission.ProgrammingLanguage langEnum;
        try {
            langEnum = Submission.ProgrammingLanguage.valueOf(language.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported language: " + language);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DuelForbiddenException("Unknown user: " + userId));
        Problem problem = problemRepository.findById(match.getProblemId())
                .orElseThrow(() -> new IllegalStateException(
                        "Match references missing problem " + match.getProblemId()));

        Submission submission = new Submission();
        submission.setUser(user);
        submission.setProblem(problem);
        submission.setCode(code);
        submission.setLanguage(langEnum);
        submission.setStatus(Submission.SubmissionStatus.PENDING);
        submission.setSubmittedAt(TimeUtil.now());
        submission.setUserName(user.getUsername());
        submission.setProblemName(problem.getTitle());
        Submission saved = submissionRepository.save(submission);
        submissionRepository.flush();

        DuelSubmission link = new DuelSubmission(saved.getId(), matchId, false);
        duelSubmissionRepository.save(link);

        SubmissionJob job = new SubmissionJob(
                saved.getId(),
                userId,
                problem.getId(),
                null,                // contestId — duels are independent of contests
                code,
                language,
                problem.getTimeLimit(),
                problem.getMemoryLimit(),
                false,               // testRun
                matchId
        );

        try {
            String json = objectMapper.writeValueAsString(job);
            redis.opsForList().leftPush(SubmissionWorkerPool.QUEUE_KEY, json);
        } catch (JsonProcessingException ex) {
            log.error("Failed to enqueue duel submission job: {}", ex.getMessage(), ex);
            throw new IllegalStateException("Failed to enqueue duel submission", ex);
        }

        Map<String, Object> progress = new LinkedHashMap<>();
        progress.put("event", "submitted");
        progress.put("userId", userId);
        progress.put("submissionId", saved.getId());
        progress.put("ts", Instant.now().toString());
        try {
            duelSseEmitterRegistry.emit(matchId, "progress", progress);
        } catch (Exception e) {
            log.debug("Failed to emit submitted progress for match {}: {}", matchId, e.getMessage());
        }

        return saved.getId();
    }

    // ─────────────────────────────────────────────────────────────────────
    // forfeit
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Forfeit the match for the calling user. The opponent is recorded as
     * winner (outcome {@code USER_A_WIN} / {@code USER_B_WIN} per their
     * seat), satisfying Requirement 7.5 while staying within the V3 CHECK
     * constraint that forces {@code winner_user_id IS NULL} when
     * {@code outcome='ABANDONED'}.
     */
    public void forfeit(UUID matchId, Long userId) {
        if (matchId == null) {
            throw new DuelNotFoundException("Match id must not be null");
        }
        if (userId == null) {
            throw new DuelForbiddenException("Authenticated user required");
        }
        DuelMatch match = duelMatchRepository.findById(matchId)
                .orElseThrow(() -> new DuelNotFoundException("Match not found: " + matchId));
        if (!userId.equals(match.getUserAId()) && !userId.equals(match.getUserBId())) {
            throw new DuelForbiddenException("Not a participant of match " + matchId);
        }
        if (match.getStatus() == DuelMatch.Status.FINISHED) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("matchId", matchId.toString());
            payload.put("outcome", match.getOutcome() != null ? match.getOutcome().name() : null);
            throw new DuelStateConflictException("MATCH_FINISHED", payload,
                    "Match already finished: " + matchId);
        }

        Long opponentId = userId.equals(match.getUserAId()) ? match.getUserBId() : match.getUserAId();
        DuelMatch.Outcome outcome = opponentId.equals(match.getUserAId())
                ? DuelMatch.Outcome.USER_A_WIN
                : DuelMatch.Outcome.USER_B_WIN;
        LocalDateTime now = TimeUtil.now();

        int rows = duelMatchRepository.finalizeIfActive(matchId, opponentId, outcome, now);
        if (rows == 1) {
            setCooldown(match.getUserAId());
            setCooldown(match.getUserBId());
            DuelMatch reread = duelMatchRepository.findById(matchId).orElse(match);
            emitMatchFinished(reread);
            cancelTimers(matchId);
            log.info("Duel forfeited match={} loser={} winner={}", matchId, userId, opponentId);
            return;
        }

        // Race: another path finalized first. Re-read and report the existing outcome.
        DuelMatch reread = duelMatchRepository.findById(matchId).orElse(null);
        if (reread != null && reread.getStatus() == DuelMatch.Status.IN_PROGRESS) {
            // Try once more — perhaps a previous transaction rolled back.
            rows = duelMatchRepository.finalizeIfActive(matchId, opponentId, outcome, TimeUtil.now());
            if (rows == 1) {
                setCooldown(match.getUserAId());
                setCooldown(match.getUserBId());
                DuelMatch postRead = duelMatchRepository.findById(matchId).orElse(match);
                emitMatchFinished(postRead);
                cancelTimers(matchId);
                log.info("Duel forfeited match={} loser={} winner={} (retry)", matchId, userId, opponentId);
                return;
            }
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("matchId", matchId.toString());
        payload.put("outcome", reread != null && reread.getOutcome() != null
                ? reread.getOutcome().name() : null);
        throw new DuelStateConflictException("MATCH_FINISHED", payload,
                "Match already finished: " + matchId);
    }

    // ─────────────────────────────────────────────────────────────────────
    // heartbeat
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Forward a typing heartbeat to the opponent's subscriptions, rate
     * limited to one emit per {@value #TYPING_HEARTBEAT_MIN_INTERVAL_MS}
     * ms per user (Property 16). Silent no-op for finished matches.
     */
    public void heartbeat(UUID matchId, Long userId) {
        if (matchId == null || userId == null) {
            return;
        }
        DuelMatch match = duelMatchRepository.findById(matchId).orElse(null);
        if (match == null) {
            throw new DuelNotFoundException("Match not found: " + matchId);
        }
        if (!userId.equals(match.getUserAId()) && !userId.equals(match.getUserBId())) {
            throw new DuelForbiddenException("Not a participant of match " + matchId);
        }
        if (match.getStatus() != DuelMatch.Status.IN_PROGRESS) {
            return; // silently drop — no need to fan typing past match end
        }

        AtomicLong last = typingHeartbeatLastEmitMs.computeIfAbsent(userId, k -> new AtomicLong(0L));
        long now = System.currentTimeMillis();
        long prev = last.get();
        if (now - prev < TYPING_HEARTBEAT_MIN_INTERVAL_MS) {
            return;
        }
        if (!last.compareAndSet(prev, now)) {
            return;
        }

        Long opponentId = userId.equals(match.getUserAId()) ? match.getUserBId() : match.getUserAId();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "typing");
        payload.put("userId", userId);
        payload.put("ts", Instant.now().toString());
        try {
            duelSseEmitterRegistry.emitTo(matchId, opponentId, "progress", payload);
        } catch (Exception e) {
            log.debug("Failed to emit typing heartbeat for match {} from user {}: {}",
                    matchId, userId, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // onDuelVerdict
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Verdict callback from {@code SubmissionWorkerPool.finalizeAndNotify}.
     * Always emits {@code progress {event:'verdict'}} to the duel room.
     * When the verdict is {@code AC}, attempts the win-claim using the
     * Valkey {@code SET NX EX} flag plus the conditional UPDATE on
     * {@code duel_matches.winner_user_id IS NULL} (Requirements 6.1 / 9.2 /
     * 9.3 / Property 8).
     */
    public void onDuelVerdict(UUID matchId, Long userId, Long submissionId,
                              Submission.SubmissionStatus status, int passed, int total) {
        if (matchId == null || userId == null || submissionId == null || status == null) {
            log.debug("onDuelVerdict received null parameter: match={} user={} sub={} status={}",
                    matchId, userId, submissionId, status);
            return;
        }

        Map<String, Object> verdictPayload = new LinkedHashMap<>();
        verdictPayload.put("event", "verdict");
        verdictPayload.put("userId", userId);
        verdictPayload.put("submissionId", submissionId);
        verdictPayload.put("status", status.name());
        verdictPayload.put("testCasesPassed", passed);
        verdictPayload.put("totalTestCases", total);
        // V4 — re-read counters from Valkey so the frontend can update its
        // "Runs: x/5  Submits: y/2" display from the verdict payload.
        verdictPayload.put("runsUsed", getRunCount(matchId, userId));
        verdictPayload.put("submitsUsed", getSubmitCount(matchId, userId));
        verdictPayload.put("runsRemaining",
                Math.max(0, RUN_LIMIT_PER_MATCH - getRunCount(matchId, userId)));
        verdictPayload.put("submitsRemaining",
                Math.max(0, SUBMIT_LIMIT_PER_MATCH - getSubmitCount(matchId, userId)));
        verdictPayload.put("ts", Instant.now().toString());
        try {
            duelSseEmitterRegistry.emit(matchId, "progress", verdictPayload);
        } catch (Exception e) {
            log.debug("Failed to emit verdict progress for match {}: {}", matchId, e.getMessage());
        }

        // V4 — only full-AC (all test cases passing) wins immediately.
        // The existing parser sets status=AC only when passed==total, so
        // this check is defense-in-depth.
        boolean fullAc = status == Submission.SubmissionStatus.AC
                && total > 0 && passed == total;
        if (!fullAc) {
            return;
        }

        // Win-claim path.
        String winnerKey = WINNER_PREFIX + matchId;
        Boolean acquired = redis.opsForValue()
                .setIfAbsent(winnerKey, userId.toString(), WINNER_KEY_TTL_SEC, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(acquired)) {
            // Another participant's AC already won — emit match_finished from
            // the re-read row (no DB rewrite, Requirement 9.4).
            duelMatchRepository.findById(matchId).ifPresent(this::emitMatchFinished);
            return;
        }

        DuelMatch match = duelMatchRepository.findById(matchId).orElse(null);
        if (match == null) {
            log.warn("onDuelVerdict for unknown match {}", matchId);
            return;
        }
        if (!userId.equals(match.getUserAId()) && !userId.equals(match.getUserBId())) {
            log.warn("onDuelVerdict winner-claim from non-participant user={} match={}", userId, matchId);
            return;
        }
        Seat seat = SeatAssigner.seatFor(match.getUserAId(), match.getUserBId(), userId);
        DuelMatch.Outcome outcome = seat == Seat.A
                ? DuelMatch.Outcome.USER_A_WIN
                : DuelMatch.Outcome.USER_B_WIN;
        LocalDateTime now = TimeUtil.now();

        int rows = duelMatchRepository.finalizeIfActive(matchId, userId, outcome, now);
        if (rows == 1) {
            try {
                duelSubmissionRepository.markFirstAc(submissionId);
            } catch (Exception e) {
                log.warn("markFirstAc failed for submission {}: {}", submissionId, e.getMessage());
            }
            setCooldown(match.getUserAId());
            setCooldown(match.getUserBId());
            DuelMatch reread = duelMatchRepository.findById(matchId).orElse(match);
            emitMatchFinished(reread);
            cancelTimers(matchId);
            log.info("Duel finished match={} winner={} outcome={}", matchId, userId, outcome);
            return;
        }
        // The Valkey claim succeeded but the conditional UPDATE failed —
        // means another path (forfeit, draw timer, reconnect timer, admin
        // cancel) raced ahead. Emit match_finished from the re-read row.
        DuelMatch reread = duelMatchRepository.findById(matchId).orElse(match);
        emitMatchFinished(reread);
    }

    // ─────────────────────────────────────────────────────────────────────
    // SSE lifecycle hooks (called by DuelSseEmitterRegistry)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Cancel any pending reconnect-grace timer for the (match, user) pair
     * and emit {@code progress {event:'reconnected'}}. Also cancels the
     * dual-disconnect combined timer if it is the only one present.
     */
    public void onSubscriptionOpened(UUID matchId, Long userId) {
        if (matchId == null || userId == null) {
            return;
        }
        boolean cancelled = false;
        ScheduledFuture<?> single = reconnectTimers.remove(reconnectKey(matchId, userId));
        if (single != null) {
            single.cancel(false);
            cancelled = true;
        }
        ScheduledFuture<?> combined = reconnectTimers.remove(combinedKey(matchId));
        if (combined != null) {
            combined.cancel(false);
            cancelled = true;
        }
        if (cancelled) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("event", "reconnected");
            payload.put("userId", userId);
            payload.put("ts", Instant.now().toString());
            try {
                duelSseEmitterRegistry.emit(matchId, "progress", payload);
            } catch (Exception e) {
                log.debug("Failed to emit reconnected progress for match {}: {}",
                        matchId, e.getMessage());
            }
        }
    }

    /**
     * Schedule the reconnect-grace timer (Requirements 7.1 / 7.2 / 7.4).
     * If both participants have no active subscription, schedule a single
     * combined timer that finalizes as {@code DRAW}; otherwise schedule a
     * single-user timer that finalizes the match with the opponent as
     * winner.
     */
    public void onLastSubscriptionClosed(UUID matchId, Long userId) {
        if (matchId == null || userId == null) {
            return;
        }
        DuelMatch match = duelMatchRepository.findById(matchId).orElse(null);
        if (match == null || match.getStatus() != DuelMatch.Status.IN_PROGRESS) {
            return;
        }
        if (!userId.equals(match.getUserAId()) && !userId.equals(match.getUserBId())) {
            return;
        }
        Long opponentId = userId.equals(match.getUserAId()) ? match.getUserBId() : match.getUserAId();
        boolean opponentActive = duelSseEmitterRegistry.hasActiveSubscription(matchId, opponentId);

        if (!opponentActive) {
            // Both disconnected — schedule a single combined timer that
            // finalizes as DRAW per Requirement 7.4.
            scheduleCombinedDisconnectTimer(matchId);
        } else {
            scheduleSingleReconnectTimer(matchId, userId);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "disconnected");
        payload.put("userId", userId);
        payload.put("gracePeriodSec", gracePeriodSec);
        payload.put("ts", Instant.now().toString());
        try {
            duelSseEmitterRegistry.emit(matchId, "progress", payload);
        } catch (Exception e) {
            log.debug("Failed to emit disconnected progress for match {}: {}",
                    matchId, e.getMessage());
        }
    }

    private void scheduleSingleReconnectTimer(UUID matchId, Long userId) {
        String key = reconnectKey(matchId, userId);
        ScheduledFuture<?> existing = reconnectTimers.remove(key);
        if (existing != null) {
            existing.cancel(false);
        }
        ScheduledFuture<?> f = reconnectTimerExec.schedule(
                () -> safeRun(() -> reconnectExpired(matchId, userId)),
                gracePeriodSec, TimeUnit.SECONDS);
        reconnectTimers.put(key, f);
    }

    private void scheduleCombinedDisconnectTimer(UUID matchId) {
        String key = combinedKey(matchId);
        ScheduledFuture<?> existing = reconnectTimers.remove(key);
        if (existing != null) {
            existing.cancel(false);
        }
        ScheduledFuture<?> f = reconnectTimerExec.schedule(
                () -> safeRun(() -> combinedDisconnectFinalize(matchId)),
                gracePeriodSec, TimeUnit.SECONDS);
        reconnectTimers.put(key, f);
    }

    private static String reconnectKey(UUID matchId, Long userId) {
        return matchId + ":" + userId;
    }

    private static String combinedKey(UUID matchId) {
        return matchId + ":both";
    }

    /**
     * Single-user reconnect-grace expiry: finalize as ABANDONED with the
     * opponent as winner (Requirement 7.3, encoded as USER_A_WIN /
     * USER_B_WIN per the seat constraint discussed on {@link #forfeit}).
     */
    void reconnectExpired(UUID matchId, Long userId) {
        DuelMatch match = duelMatchRepository.findById(matchId).orElse(null);
        if (match == null || match.getStatus() != DuelMatch.Status.IN_PROGRESS) {
            return;
        }
        if (duelSseEmitterRegistry.hasActiveSubscription(matchId, userId)) {
            // Reconnected after the timer fired but before this method
            // grabbed the lock — drop the abandonment.
            return;
        }
        Long opponentId = userId.equals(match.getUserAId()) ? match.getUserBId() : match.getUserAId();
        DuelMatch.Outcome outcome = opponentId.equals(match.getUserAId())
                ? DuelMatch.Outcome.USER_A_WIN
                : DuelMatch.Outcome.USER_B_WIN;
        int rows = duelMatchRepository.finalizeIfActive(matchId, opponentId, outcome, TimeUtil.now());
        if (rows == 1) {
            setCooldown(match.getUserAId());
            setCooldown(match.getUserBId());
            DuelMatch reread = duelMatchRepository.findById(matchId).orElse(match);
            emitMatchFinished(reread);
            cancelDrawTimer(matchId);
            log.info("Duel abandoned match={} disconnectedUser={} winner={}",
                    matchId, userId, opponentId);
        }
    }

    /**
     * Both-disconnected expiry: finalize as DRAW per Requirement 7.4.
     */
    void combinedDisconnectFinalize(UUID matchId) {
        DuelMatch match = duelMatchRepository.findById(matchId).orElse(null);
        if (match == null || match.getStatus() != DuelMatch.Status.IN_PROGRESS) {
            return;
        }
        // If either participant reconnected during the grace window, drop the draw.
        if (duelSseEmitterRegistry.hasActiveSubscription(matchId, match.getUserAId())
                || duelSseEmitterRegistry.hasActiveSubscription(matchId, match.getUserBId())) {
            return;
        }
        int rows = duelMatchRepository.finalizeAsDrawIfActive(matchId, TimeUtil.now());
        if (rows == 1) {
            setCooldown(match.getUserAId());
            setCooldown(match.getUserBId());
            DuelMatch reread = duelMatchRepository.findById(matchId).orElse(match);
            emitMatchFinished(reread);
            cancelDrawTimer(matchId);
            log.info("Duel both-disconnect DRAW match={}", matchId);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Timers
    // ─────────────────────────────────────────────────────────────────────

    private void scheduleDrawTimer(UUID matchId, long delaySec) {
        ScheduledFuture<?> existing = drawTimers.remove(matchId);
        if (existing != null) {
            existing.cancel(false);
        }
        ScheduledFuture<?> f = drawTimerExec.schedule(
                () -> safeRun(() -> finalizeDrawTimerExpired(matchId)),
                Math.max(1L, delaySec), TimeUnit.SECONDS);
        drawTimers.put(matchId, f);
    }

    /**
     * Match-window timer expiry — V4 win-by-max-test-cases-passed.
     *
     * <p>Replaces the prior "always-DRAW" finalize. Now we:
     * <ol>
     *   <li>Pull each user's best {@code submissions} row (by max
     *       {@code testCasesPassed}, tie-break by earliest
     *       {@code submittedAt}) for this match.</li>
     *   <li>If max-A &gt; max-B → USER_A_WIN. Vice versa for B.</li>
     *   <li>Tie on count → compare submission timestamps; earlier wins.</li>
     *   <li>No submissions on either side → DRAW.</li>
     * </ol>
     *
     * <p>The existing CHECK constraint on {@code duel_matches} requires
     * {@code winner_user_id IS NOT NULL} for USER_*_WIN, so we route through
     * {@link DuelMatchRepository#finalizeIfActive} for the win path and the
     * existing {@code finalizeAsDrawIfActive} for the DRAW path.
     */
    void finalizeDrawTimerExpired(UUID matchId) {
        DuelMatch match = duelMatchRepository.findById(matchId).orElse(null);
        if (match == null || match.getStatus() != DuelMatch.Status.IN_PROGRESS) {
            return;
        }

        BestSubmission bestA = findBestSubmission(matchId, match.getUserAId());
        BestSubmission bestB = findBestSubmission(matchId, match.getUserBId());

        Long winnerUserId = null;
        DuelMatch.Outcome outcome;

        if (bestA == null && bestB == null) {
            outcome = null;            // pure draw
        } else if (bestA != null && bestB == null) {
            winnerUserId = match.getUserAId();
            outcome = DuelMatch.Outcome.USER_A_WIN;
        } else if (bestA == null) {
            winnerUserId = match.getUserBId();
            outcome = DuelMatch.Outcome.USER_B_WIN;
        } else if (bestA.passed > bestB.passed) {
            winnerUserId = match.getUserAId();
            outcome = DuelMatch.Outcome.USER_A_WIN;
        } else if (bestB.passed > bestA.passed) {
            winnerUserId = match.getUserBId();
            outcome = DuelMatch.Outcome.USER_B_WIN;
        } else {
            // Tie on test-cases-passed → earlier submission wins.
            int cmp = compareSubmittedAt(bestA.submittedAt, bestB.submittedAt);
            if (cmp < 0) {
                winnerUserId = match.getUserAId();
                outcome = DuelMatch.Outcome.USER_A_WIN;
            } else if (cmp > 0) {
                winnerUserId = match.getUserBId();
                outcome = DuelMatch.Outcome.USER_B_WIN;
            } else {
                outcome = null;        // total tie → DRAW
            }
        }

        LocalDateTime now = TimeUtil.now();
        int rows;
        if (winnerUserId == null) {
            rows = duelMatchRepository.finalizeAsDrawIfActive(matchId, now);
        } else {
            rows = duelMatchRepository.finalizeIfActive(matchId, winnerUserId, outcome, now);
        }
        if (rows == 1) {
            setCooldown(match.getUserAId());
            setCooldown(match.getUserBId());
            DuelMatch reread = duelMatchRepository.findById(matchId).orElse(match);
            emitMatchFinished(reread);
            cancelReconnectTimers(matchId);
            log.info("Duel timeout-finalize match={} outcome={} winner={} maxA={}/{} maxB={}/{}",
                    matchId,
                    outcome != null ? outcome : "DRAW",
                    winnerUserId,
                    bestA != null ? bestA.passed : 0,
                    bestA != null ? bestA.total : 0,
                    bestB != null ? bestB.passed : 0,
                    bestB != null ? bestB.total : 0);
        }
    }

    /** Compact projection of a user's best submission within a match. */
    private static final class BestSubmission {
        final int passed;
        final int total;
        final LocalDateTime submittedAt;
        BestSubmission(int passed, int total, LocalDateTime submittedAt) {
            this.passed = passed;
            this.total = total;
            this.submittedAt = submittedAt;
        }
    }

    /**
     * Walk the per-match {@code duel_submissions} link rows, fetch the
     * underlying {@code submissions} for the given user, and pick the one
     * with the highest {@code testCasesPassed} (tie-broken by earliest
     * {@code submittedAt}). Returns {@code null} if the user has no
     * submissions for this match.
     */
    private BestSubmission findBestSubmission(UUID matchId, Long userId) {
        if (matchId == null || userId == null) return null;
        try {
            List<DuelSubmission> links = duelSubmissionRepository.findByMatchId(matchId);
            BestSubmission best = null;
            for (DuelSubmission link : links) {
                Submission sub = submissionRepository.findById(link.getSubmissionId()).orElse(null);
                if (sub == null || sub.getUser() == null) continue;
                if (!userId.equals(sub.getUser().getId())) continue;
                int passed = sub.getTestCasesPassed() != null ? sub.getTestCasesPassed() : 0;
                int total = sub.getTotalTestCases() != null ? sub.getTotalTestCases() : 0;
                LocalDateTime when = sub.getSubmittedAt();
                if (best == null
                        || passed > best.passed
                        || (passed == best.passed && compareSubmittedAt(when, best.submittedAt) < 0)) {
                    best = new BestSubmission(passed, total, when);
                }
            }
            return best;
        } catch (Exception e) {
            log.warn("findBestSubmission failed match={} user={}: {}", matchId, userId, e.getMessage());
            return null;
        }
    }

    private static int compareSubmittedAt(LocalDateTime a, LocalDateTime b) {
        if (a == null && b == null) return 0;
        if (a == null) return 1;     // null is "later"
        if (b == null) return -1;
        return a.compareTo(b);
    }

    private void cancelDrawTimer(UUID matchId) {
        ScheduledFuture<?> f = drawTimers.remove(matchId);
        if (f != null) {
            f.cancel(false);
        }
    }

    private void cancelReconnectTimers(UUID matchId) {
        // Walk the map and cancel anything keyed by this match.
        String matchPrefix = matchId.toString();
        reconnectTimers.entrySet().removeIf(entry -> {
            if (entry.getKey().startsWith(matchPrefix)) {
                entry.getValue().cancel(false);
                return true;
            }
            return false;
        });
    }

    private void cancelTimers(UUID matchId) {
        cancelDrawTimer(matchId);
        cancelReconnectTimers(matchId);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Admin
    // ─────────────────────────────────────────────────────────────────────

    /** Force-finalize a match as ABANDONED with no winner. */
    public void adminCancel(UUID matchId) {
        if (matchId == null) {
            throw new DuelNotFoundException("Match id must not be null");
        }
        DuelMatch match = duelMatchRepository.findById(matchId)
                .orElseThrow(() -> new DuelNotFoundException("Match not found: " + matchId));
        int rows = duelMatchRepository.adminCancelIfActive(matchId, TimeUtil.now());
        if (rows == 1) {
            setCooldown(match.getUserAId());
            setCooldown(match.getUserBId());
            DuelMatch reread = duelMatchRepository.findById(matchId).orElse(match);
            emitMatchFinished(reread);
            cancelTimers(matchId);
            log.info("Duel admin-cancelled match={}", matchId);
            return;
        }
        // Already finalized — surface the existing outcome as a 409.
        DuelMatch reread = duelMatchRepository.findById(matchId).orElse(match);
        Map<String, Object> payload = new HashMap<>();
        payload.put("matchId", matchId.toString());
        payload.put("outcome", reread.getOutcome() != null ? reread.getOutcome().name() : null);
        throw new DuelStateConflictException("MATCH_FINISHED", payload,
                "Match already finished: " + matchId);
    }

    // ─── Cache keys for hot read paths (added to absorb 5s admin polling) ────

    /** Admin metrics cache key — only the DB-derived counts; live members not cached. */
    public static final String METRICS_CACHE_KEY = "duel:metrics:db";

    /** Metrics cache TTL — short so live state stays current. */
    private static final long METRICS_CACHE_TTL_SEC = 4L;

    /** Eligible-problem list cache key. */
    public static final String ELIGIBLE_LIST_CACHE_KEY = "duel:eligible:list";

    /** Eligible list cache TTL — only invalidated by admin add/delete. */
    public static final long ELIGIBLE_LIST_CACHE_TTL_SEC = 60L;

    /** Snapshot of duel runtime metrics for the admin dashboard. */
    public DuelMetrics getMetrics() {
        // Try the Valkey cache first — admin dashboard polls every 5s, so
        // a 4s TTL absorbs the steady-state load without making the panel
        // feel stale.
        long active;
        long finishedToday;
        long abandonedToday;
        boolean cacheHit = false;
        try {
            String cached = redis.opsForValue().get(METRICS_CACHE_KEY);
            if (cached != null) {
                String[] parts = cached.split(",");
                if (parts.length == 3) {
                    active = Long.parseLong(parts[0]);
                    finishedToday = Long.parseLong(parts[1]);
                    abandonedToday = Long.parseLong(parts[2]);
                    cacheHit = true;
                } else {
                    active = 0;
                    finishedToday = 0;
                    abandonedToday = 0;
                }
            } else {
                active = 0;
                finishedToday = 0;
                abandonedToday = 0;
            }
        } catch (Exception e) {
            log.debug("Metrics cache read failed: {}", e.getMessage());
            active = 0;
            finishedToday = 0;
            abandonedToday = 0;
        }

        if (!cacheHit) {
            active = duelMatchRepository.countByStatusIn(
                    List.of(DuelMatch.Status.WAITING, DuelMatch.Status.IN_PROGRESS));
            LocalDateTime startOfToday = TimeUtil.now().toLocalDate().atStartOfDay();
            finishedToday = duelMatchRepository.countFinishedSince(startOfToday);
            abandonedToday = duelMatchRepository.countAbandonedSince(startOfToday);
            try {
                redis.opsForValue().set(
                        METRICS_CACHE_KEY,
                        active + "," + finishedToday + "," + abandonedToday,
                        METRICS_CACHE_TTL_SEC,
                        TimeUnit.SECONDS);
            } catch (Exception e) {
                log.debug("Metrics cache write failed: {}", e.getMessage());
            }
        }

        // queueDepth and sseConnectionCount are always live — they read
        // Valkey list size and an in-memory map respectively, both cheap.
        int qd;
        try {
            qd = matchmakingService.queueDepth();
        } catch (Exception e) {
            log.debug("queueDepth metric lookup failed: {}", e.getMessage());
            qd = 0;
        }
        int sseConns;
        try {
            sseConns = duelSseEmitterRegistry.connectionCount();
        } catch (Exception e) {
            log.debug("SSE connection count lookup failed: {}", e.getMessage());
            sseConns = 0;
        }
        return new DuelMetrics(active, qd, finishedToday, abandonedToday, sseConns);
    }

    /**
     * Drop the metrics cache. Called from every state-mutating path that
     * could move {@code activeMatchCount} / {@code matchesFinishedToday} /
     * {@code matchesAbandonedToday} out of date — i.e. immediately after
     * any successful {@code finalizeIfActive} / {@code adminCancelIfActive}
     * / {@code finalizeAsDrawIfActive} / {@code startIfWaiting}.
     */
    void invalidateMetricsCache() {
        try {
            redis.delete(METRICS_CACHE_KEY);
        } catch (Exception e) {
            log.debug("Metrics cache invalidate failed: {}", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Fan a {@code match_finished} event out to every subscriber on the
     * duel room. Carries outcome / winner / endedAt so the frontend can
     * close out without any extra REST round-trip.
     */
    private void emitMatchFinished(DuelMatch match) {
        if (match == null) {
            return;
        }
        // Invalidate the metrics cache — every match_finished moves the
        // active / finished_today / abandoned_today counters.
        invalidateMetricsCache();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("matchId", match.getMatchId().toString());
        payload.put("outcome", match.getOutcome() != null ? match.getOutcome().name() : null);
        payload.put("winnerUserId", match.getWinnerUserId());
        // Inline winner username so the result modal can render
        // "winner: <username>" without an extra round-trip.
        if (match.getWinnerUserId() != null) {
            payload.put("winnerUsername", lookupUsername(match.getWinnerUserId()));
        } else {
            payload.put("winnerUsername", null);
        }
        payload.put("endedAt", match.getEndedAt() != null ? match.getEndedAt().toString() : null);
        payload.put("ts", Instant.now().toString());
        try {
            duelSseEmitterRegistry.emit(match.getMatchId(), "match_finished", payload);
        } catch (Exception e) {
            log.debug("Failed to emit match_finished for match {}: {}",
                    match.getMatchId(), e.getMessage());
        }
    }

    private void setCooldown(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            redis.opsForValue().set(COOLDOWN_PREFIX + userId, "1", cooldownSec, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.debug("Failed to set cooldown for user {}: {}", userId, e.getMessage());
        }
    }

    private static void safeRun(Runnable r) {
        try {
            r.run();
        } catch (RuntimeException ex) {
            log.error("Duel timer task failed: {}", ex.getMessage(), ex);
        }
    }

    /** Test hook — exposes the eligible-match lookup for unit tests. */
    @SuppressWarnings("unused")
    Optional<DuelMatch> currentMatch(UUID matchId) {
        return duelMatchRepository.findById(matchId);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Run / Submit counters (V4 — per-match limits via Valkey)
    // ─────────────────────────────────────────────────────────────────────

    /** Returns the current run count for (matchId, userId), or 0 on miss/error. */
    public int getRunCount(UUID matchId, Long userId) {
        return readCounter(RUN_COUNTER_PREFIX + matchId + ":" + userId);
    }

    /** Returns the current submit count for (matchId, userId), or 0 on miss/error. */
    public int getSubmitCount(UUID matchId, Long userId) {
        return readCounter(SUBMIT_COUNTER_PREFIX + matchId + ":" + userId);
    }

    private int readCounter(String key) {
        try {
            String v = redis.opsForValue().get(key);
            if (v == null) return 0;
            return Integer.parseInt(v);
        } catch (Exception e) {
            log.debug("Counter read failed for {}: {}", key, e.getMessage());
            return 0;
        }
    }

    /**
     * Atomically increment a run counter. Throws
     * {@link DuelStateConflictException}({@code RUN_LIMIT_EXCEEDED}) if the
     * caller has already used all 5 runs for this match.
     *
     * @param matchId the match id (used for TTL derivation)
     * @param userId  the calling user
     * @param match   pre-loaded match row (used to compute the TTL window)
     */
    public void tryIncrementRun(UUID matchId, Long userId, DuelMatch match) {
        tryIncrementCounter(RUN_COUNTER_PREFIX + matchId + ":" + userId,
                RUN_LIMIT_PER_MATCH, "RUN_LIMIT_EXCEEDED", match);
    }

    /**
     * Atomically increment a submit counter. Throws
     * {@link DuelStateConflictException}({@code SUBMIT_LIMIT_EXCEEDED}) if
     * the caller has already used all 2 submits for this match.
     */
    public void tryIncrementSubmit(UUID matchId, Long userId, DuelMatch match) {
        tryIncrementCounter(SUBMIT_COUNTER_PREFIX + matchId + ":" + userId,
                SUBMIT_LIMIT_PER_MATCH, "SUBMIT_LIMIT_EXCEEDED", match);
    }

    private void tryIncrementCounter(String key, int limit, String code, DuelMatch match) {
        Long incr;
        try {
            incr = redis.opsForValue().increment(key);
        } catch (Exception e) {
            // Valkey hiccup — fail open so a transient network glitch does
            // not lock the user out of the match.
            log.warn("Counter INCR failed for {}: {}", key, e.getMessage());
            return;
        }
        if (incr == null) incr = 1L;

        if (incr > limit) {
            // Roll back the speculative increment so retries see a stable count.
            try { redis.opsForValue().decrement(key); } catch (Exception ignored) { /* fall through */ }
            Map<String, Object> payload = new HashMap<>();
            payload.put("remaining", 0);
            payload.put("limit", limit);
            throw new DuelStateConflictException(code, payload,
                    "Per-match limit exceeded: " + code);
        }

        // First successful increment — set the TTL once. Window = match
        // duration + a 60 s buffer so the key cleans itself up after the
        // match ends without depending on the finalize path.
        if (incr == 1L) {
            try {
                int rowLimit = match != null && match.getTimeLimitSec() != null
                        ? match.getTimeLimitSec()
                        : 3900; // worst-case HARD window
                redis.expire(key, rowLimit + 60L, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.debug("Failed to set TTL on counter {}: {}", key, e.getMessage());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // runForDuel — sync compile + examples-only run
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Compile the user's code and execute against the problem's example
     * test cases only. Synchronous — returns the result inline so the
     * frontend can render output / expected / pass/fail without waiting
     * on an SSE event.
     *
     * <p>Counts toward the 5-runs-per-match-per-user limit. Does NOT
     * count toward the 2-submits limit. Does NOT touch
     * {@code submissions} / {@code duel_submissions} — practice runs are
     * fire-and-forget.
     */
    public RunResult runForDuel(UUID matchId, Long userId, String code, String language, String stdin) {
        if (matchId == null) {
            throw new DuelNotFoundException("Match id must not be null");
        }
        if (userId == null) {
            throw new DuelForbiddenException("Authenticated user required");
        }
        if (language == null || language.isBlank()) {
            throw new IllegalArgumentException("language is required");
        }

        DuelMatch match = duelMatchRepository.findById(matchId)
                .orElseThrow(() -> new DuelNotFoundException("Match not found: " + matchId));
        if (!userId.equals(match.getUserAId()) && !userId.equals(match.getUserBId())) {
            throw new DuelForbiddenException("Not a participant of match " + matchId);
        }
        if (match.getStatus() != DuelMatch.Status.IN_PROGRESS) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("matchId", matchId.toString());
            payload.put("status", match.getStatus() != null ? match.getStatus().name() : null);
            throw new DuelStateConflictException("MATCH_NOT_ACTIVE", payload,
                    "Match is not in progress: " + matchId);
        }

        Submission.ProgrammingLanguage langEnum;
        try {
            langEnum = Submission.ProgrammingLanguage.valueOf(language.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported language: " + language);
        }

        Problem problem = problemRepository.findById(match.getProblemId())
                .orElseThrow(() -> new IllegalStateException(
                        "Match references missing problem " + match.getProblemId()));

        // Increment run counter (throws RUN_LIMIT_EXCEEDED on overflow).
        tryIncrementRun(matchId, userId, match);

        // Build the example test inputs/expected outputs from problem.example1..3.
        // Each example is stored as "input\noutput-divider-output" — we treat
        // the first half (before "Output:" or first blank line) as input and
        // the rest as expected. Since the existing platform doesn't
        // standardize a structured examples representation, we send the user
        // the raw string and let them eyeball; we only rely on stdin for the
        // first example to actually feed the program.
        java.util.List<String[]> examples = parseExamples(problem);

        // Compile + run the user's code through the same compiler service
        // the public /compiler endpoint uses. We feed the example input as
        // stdin and capture stdout/stderr/exit code.
        java.util.List<Map<String, Object>> caseResults = new java.util.ArrayList<>();
        boolean overallPassed = true;
        boolean ranAtLeastOne = false;
        String compileError = null;
        long totalTimeMs = 0;
        for (String[] ex : examples) {
            String input = ex[0];
            String expected = ex[1];
            // First case may carry user-provided stdin override
            String effectiveStdin = (stdin != null && !stdin.isBlank() && !ranAtLeastOne)
                    ? stdin : input;
            CompilerService.CompilerResponse cr = compilerService.compile(
                    code, langEnum.name(), effectiveStdin, 10);
            ranAtLeastOne = true;
            totalTimeMs += cr.executionTimeMs;
            if (cr.compileError) {
                compileError = cr.stderr != null && !cr.stderr.isBlank() ? cr.stderr
                        : (cr.errorMessage != null ? cr.errorMessage : "Compilation error");
                overallPassed = false;
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("input", input);
                r.put("expected", expected);
                r.put("output", "");
                r.put("stderr", cr.stderr);
                r.put("passed", false);
                r.put("compileError", true);
                caseResults.add(r);
                break; // no point running more cases on a CE
            }
            String stdout = cr.stdout != null ? cr.stdout : "";
            boolean passed = expected != null
                    && normalizeOutput(stdout).equals(normalizeOutput(expected));
            if (!passed) overallPassed = false;
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("input", input);
            r.put("expected", expected);
            r.put("output", stdout);
            r.put("stderr", cr.stderr);
            r.put("passed", passed);
            r.put("timeLimitExceeded", cr.timeLimitExceeded);
            r.put("exitCode", cr.exitCode);
            caseResults.add(r);
        }

        int runsUsed = getRunCount(matchId, userId);
        int runsRemaining = Math.max(0, RUN_LIMIT_PER_MATCH - runsUsed);

        return new RunResult(
                ranAtLeastOne && overallPassed && compileError == null,
                compileError,
                caseResults,
                runsUsed,
                runsRemaining,
                totalTimeMs
        );
    }

    /**
     * Parse the three optional {@code example1/2/3} fields on
     * {@link Problem} into a list of {@code [input, expectedOutput]}
     * tuples. The on-disk format is freeform; we make a best-effort split
     * on the first occurrence of {@code "Output:"} (case-insensitive),
     * falling back to {@code "Expected:"}, then to a blank-line split.
     */
    private static java.util.List<String[]> parseExamples(Problem p) {
        java.util.List<String[]> out = new java.util.ArrayList<>();
        String[] sources = new String[]{p.getExample1(), p.getExample2(), p.getExample3()};
        for (String raw : sources) {
            if (raw == null || raw.isBlank()) continue;
            String[] parts = splitInputOutput(raw);
            if (parts != null) out.add(parts);
        }
        return out;
    }

    private static String[] splitInputOutput(String raw) {
        if (raw == null) return null;
        String lower = raw.toLowerCase();
        int idx = lower.indexOf("output:");
        if (idx < 0) idx = lower.indexOf("expected:");
        if (idx >= 0) {
            // Walk forward to the first newline after the marker.
            int afterColon = raw.indexOf(':', idx) + 1;
            // Find the input portion — text BEFORE the marker (after stripping "Input:" prefix).
            String beforeMarker = raw.substring(0, idx);
            String inputBody = stripLeadingLabel(beforeMarker, "input");
            String outputBody = raw.substring(afterColon);
            return new String[]{inputBody.trim(), outputBody.trim()};
        }
        // Fallback: split on first double newline.
        int blank = raw.indexOf("\n\n");
        if (blank > 0) {
            return new String[]{
                    stripLeadingLabel(raw.substring(0, blank), "input").trim(),
                    raw.substring(blank + 2).trim()
            };
        }
        // No reliable split — treat the whole thing as input with no expected.
        return new String[]{stripLeadingLabel(raw, "input").trim(), ""};
    }

    private static String stripLeadingLabel(String s, String label) {
        if (s == null) return "";
        String trimmed = s.trim();
        String lower = trimmed.toLowerCase();
        if (lower.startsWith(label + ":")) {
            return trimmed.substring(label.length() + 1).trim();
        }
        return trimmed;
    }

    /** Whitespace-tolerant comparison: trim each line, drop trailing blank lines. */
    private static String normalizeOutput(String s) {
        if (s == null) return "";
        String[] lines = s.replace("\r\n", "\n").split("\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line.stripTrailing()).append('\n');
        }
        // Drop trailing newlines.
        while (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    /** Result DTO for the {@code POST /api/duels/{matchId}/run} endpoint. */
    public record RunResult(
            boolean passed,
            String compileError,
            java.util.List<Map<String, Object>> cases,
            int runsUsed,
            int runsRemaining,
            long totalTimeMs
    ) {}
}
