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
                       ObjectMapper objectMapper) {
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
            long remainingSec = drawTimeoutSec - elapsedSec;
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
     * <p>Picks a problem from {@code duel_eligible_problems} that is not
     * jointly solved by both participants; falls back to the full pool if
     * the exclusion empties the candidate set, with a logged warning
     * (Requirements 3.2 / 3.3 / 3.5). Throws
     * {@link DuelStateConflictException} with code
     * {@code NO_ELIGIBLE_PROBLEM} if even the full pool is empty.
     *
     * @return the freshly-assigned {@code matchId}
     * @throws DuelStateConflictException with code {@code CONCURRENT_MATCH}
     *                                    if either user already has an
     *                                    active match (partial-unique-index
     *                                    violation), or
     *                                    {@code NO_ELIGIBLE_PROBLEM} if
     *                                    no problem can be selected.
     */
    public UUID pairAndStart(Long userIdA, Long userIdB) {
        if (userIdA == null || userIdB == null) {
            throw new IllegalArgumentException("Duel participants must not be null");
        }
        if (userIdA.equals(userIdB)) {
            throw new IllegalArgumentException("Duel participants must be distinct: " + userIdA);
        }

        long[] sorted = SeatAssigner.orderedPair(userIdA, userIdB);
        Long minId = sorted[0];
        Long maxId = sorted[1];

        // Problem selection — exclude problems jointly solved by both.
        List<Long> eligible = duelEligibleProblemRepository.findEligibleNotBothSolved(minId, maxId);
        if (eligible.isEmpty()) {
            eligible = duelEligibleProblemRepository.findAllProblemIds();
            log.warn("duel.problem_pool.exhausted users=({},{}) — falling back to full pool",
                    minId, maxId);
        }
        if (eligible.isEmpty()) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("userA", minId);
            payload.put("userB", maxId);
            throw new DuelStateConflictException("NO_ELIGIBLE_PROBLEM", payload,
                    "No eligible problem available for users " + minId + "/" + maxId);
        }
        Long problemId = eligible.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(eligible.size()));

        UUID matchId = UUID.randomUUID();

        // INSERT + transition to IN_PROGRESS happens in a separate
        // transactional helper so the partial-unique-index violation is
        // visible as a DataIntegrityViolationException at commit time.
        try {
            createAndStartMatchRow(matchId, minId, maxId, problemId);
        } catch (DataIntegrityViolationException dive) {
            log.info("pairAndStart concurrent_match users=({},{}) match={} — partial-unique violation",
                    minId, maxId, matchId);
            Map<String, Object> payload = new HashMap<>();
            payload.put("userA", minId);
            payload.put("userB", maxId);
            throw new DuelStateConflictException("CONCURRENT_MATCH", payload,
                    "User already has an active match");
        }

        // Schedule the draw timer.
        scheduleDrawTimer(matchId, drawTimeoutSec);

        // Look up usernames for the matched event payload.
        String userAUsername = lookupUsername(minId);
        String userBUsername = lookupUsername(maxId);
        LocalDateTime startedAt = duelMatchRepository.findById(matchId)
                .map(DuelMatch::getStartedAt)
                .orElse(TimeUtil.now());

        // Emit matched event to both users on the per-user channel
        // (Requirement 4.1 / Requirement 13.2).
        emitMatched(minId, matchId, userBUsername, problemId, startedAt);
        emitMatched(maxId, matchId, userAUsername, problemId, startedAt);

        log.info("Duel paired match={} users=({},{}) problem={}", matchId, minId, maxId, problemId);
        invalidateMetricsCache();
        return matchId;
    }

    /**
     * Insert the {@code WAITING} row and transition it to {@code IN_PROGRESS}
     * inside a single transaction so the partial-unique-index check fires
     * at commit time.
     */
    @Transactional
    void createAndStartMatchRow(UUID matchId, Long minId, Long maxId, Long problemId) {
        DuelMatch match = new DuelMatch();
        match.setMatchId(matchId);
        match.setUserAId(minId);
        match.setUserBId(maxId);
        match.setProblemId(problemId);
        match.setStatus(DuelMatch.Status.WAITING);
        match.setCreatedAt(TimeUtil.now());

        duelMatchRepository.save(match);
        duelMatchRepository.flush(); // surface unique-index violations before the transition

        int rows = duelMatchRepository.startIfWaiting(matchId, TimeUtil.now());
        if (rows != 1) {
            // Should be unreachable — we just inserted the WAITING row.
            throw new IllegalStateException(
                    "startIfWaiting returned " + rows + " for match " + matchId);
        }
    }

    private void emitMatched(Long userId, UUID matchId, String opponentUsername,
                             Long problemId, LocalDateTime startedAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("matchId", matchId.toString());
        payload.put("opponentUsername", opponentUsername);
        payload.put("problemId", problemId);
        payload.put("startedAt", startedAt != null ? startedAt.toString() : null);
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
        return toView(match, yourSeat);
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
                .map(m -> toView(m, null, false))
                .toList();
        return new PageImpl<>(views, pageable, page.getTotalElements());
    }

    private DuelMatchView toView(DuelMatch match, Seat yourSeat) {
        return toView(match, yourSeat, true);
    }

    /**
     * @param includeProblem when true, fetches and inlines the problem
     *                       statement (used by participant-facing
     *                       {@code getMatch}). Admin listing views pass
     *                       {@code false} to skip the per-row problem
     *                       fetch.
     */
    private DuelMatchView toView(DuelMatch match, Seat yourSeat, boolean includeProblem) {
        String userAUsername = lookupUsername(match.getUserAId());
        String userBUsername = lookupUsername(match.getUserBId());

        Long elapsedSec = null;
        Long remainingSec = null;
        LocalDateTime startedAt = match.getStartedAt();
        if (match.getStatus() == DuelMatch.Status.IN_PROGRESS && startedAt != null) {
            long elapsed = Math.max(0L,
                    Duration.between(startedAt, TimeUtil.now()).getSeconds());
            elapsedSec = elapsed;
            remainingSec = Math.max(0L, drawTimeoutSec - elapsed);
        } else if (match.getStatus() == DuelMatch.Status.FINISHED && startedAt != null
                && match.getEndedAt() != null) {
            elapsedSec = Math.max(0L,
                    Duration.between(startedAt, match.getEndedAt()).getSeconds());
            // remainingSec stays null for FINISHED rows.
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
                startedAt,
                match.getEndedAt(),
                elapsedSec,
                remainingSec,
                yourSeat != null ? yourSeat.name() : null
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
        verdictPayload.put("ts", Instant.now().toString());
        try {
            duelSseEmitterRegistry.emit(matchId, "progress", verdictPayload);
        } catch (Exception e) {
            log.debug("Failed to emit verdict progress for match {}: {}", matchId, e.getMessage());
        }

        if (status != Submission.SubmissionStatus.AC) {
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

    /** 600-second draw timer expiry (Requirement 6.6). */
    void finalizeDrawTimerExpired(UUID matchId) {
        DuelMatch match = duelMatchRepository.findById(matchId).orElse(null);
        if (match == null || match.getStatus() != DuelMatch.Status.IN_PROGRESS) {
            return;
        }
        int rows = duelMatchRepository.finalizeAsDrawIfActive(matchId, TimeUtil.now());
        if (rows == 1) {
            setCooldown(match.getUserAId());
            setCooldown(match.getUserBId());
            DuelMatch reread = duelMatchRepository.findById(matchId).orElse(match);
            emitMatchFinished(reread);
            cancelReconnectTimers(matchId);
            log.info("Duel draw-timeout match={}", matchId);
        }
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
}
