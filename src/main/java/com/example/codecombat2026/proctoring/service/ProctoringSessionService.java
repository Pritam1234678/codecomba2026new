package com.example.codecombat2026.proctoring.service;

import com.example.codecombat2026.entity.Contest;
import com.example.codecombat2026.proctoring.entity.EndReason;
import com.example.codecombat2026.proctoring.entity.ProctoringSession;
import com.example.codecombat2026.proctoring.entity.RiskBand;
import com.example.codecombat2026.proctoring.event.ProctoringSessionEndedEvent;
import com.example.codecombat2026.proctoring.event.ProctoringSessionStartedEvent;
import com.example.codecombat2026.proctoring.exception.ProctoringForbiddenException;
import com.example.codecombat2026.proctoring.exception.ProctoringNotFoundException;
import com.example.codecombat2026.proctoring.exception.ProctoringStateConflictException;
import com.example.codecombat2026.proctoring.repository.ProctoringSessionRepository;
import com.example.codecombat2026.repository.ContestRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Single source of truth for {@link ProctoringSession} lifecycle (Req 13).
 *
 * <p>Every terminating method is implemented as a single conditional
 * UPDATE via
 * {@link ProctoringSessionRepository#closeIfActive(Long, LocalDateTime, EndReason)}:
 * {@code SET ended_at=:ts, end_reason=:reason WHERE id=:id AND ended_at IS NULL}.
 * The {@code endedAt IS NULL} predicate makes every close idempotent —
 * a duplicate force-end from two admin tabs, or a heartbeat-timeout
 * racing the candidate's "Quit" click, returns {@code 0} rows and is
 * silently no-op'd. The first writer wins and the original
 * {@link EndReason} is preserved. This mirrors the same pattern used
 * by the duel module's {@code finalizeIfActive} (see
 * {@code DuelMatchRepository}).
 *
 * <p>Lockout: {@link #isLocked(Long, Long)} returns true when a previous
 * session for {@code (user_id, contest_id)} ended with
 * {@link EndReason#SELF_QUIT}, {@link EndReason#ADMIN_FORCED}, or
 * {@link EndReason#HEARTBEAT_TIMEOUT} (Req 13.9, 24.6). The check is
 * consulted at session creation here and, separately, by the
 * submission gate.
 *
 * <p>Recovery: {@link #recoverOrphanedSessions()} runs on
 * {@code @PostConstruct} and finalizes every active session left over
 * from a previous JVM with {@link EndReason#HEARTBEAT_TIMEOUT}. This is
 * the conservative choice from {@code design.md}: a candidate who
 * reconnects after a JVM restart sees the lockout and is told the
 * session ended.
 *
 * <p>Sweeps: {@link #contestEndedSweep()} runs every 30 s and closes
 * every session whose parent contest has elapsed (Req 13.5).
 *
 * <p>Valkey projection: the active-session set
 * {@code proctoring:contest:{cid}:active} and the per-session score
 * counters are best-effort writes — Valkey hiccups must not block the
 * lifecycle path, so failures are logged at WARN and swallowed.
 */
@Service
public class ProctoringSessionService {

    private static final Logger log = LoggerFactory.getLogger(ProctoringSessionService.class);

    /** Lockout end reasons — Req 13.9, 24.6. */
    private static final Set<EndReason> LOCKOUT_REASONS =
            Set.of(EndReason.SELF_QUIT, EndReason.ADMIN_FORCED, EndReason.HEARTBEAT_TIMEOUT);

    /**
     * Maximum number of times a candidate may resume (rejoin) an active session
     * after an accidental refresh / tab close. After this many resumes the entry
     * API refuses further re-entry and directs the candidate to support.
     */
    public static final int MAX_RESUMES = 2;

    private static final String CONTEST_ACTIVE_KEY_PREFIX = "proctoring:contest:";
    private static final String CONTEST_ACTIVE_KEY_SUFFIX = ":active";
    private static final String SESSION_KEY_PREFIX = "proctoring:session:";

    private final ProctoringSessionRepository sessionRepo;
    private final ContestRepository contestRepository;
    private final StringRedisTemplate redis;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public ProctoringSessionService(ProctoringSessionRepository sessionRepo,
                                    ContestRepository contestRepository,
                                    StringRedisTemplate redis,
                                    ApplicationEventPublisher eventPublisher) {
        this.sessionRepo = sessionRepo;
        this.contestRepository = contestRepository;
        this.redis = redis;
        this.eventPublisher = eventPublisher;
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────

    /**
     * Create a new {@link ProctoringSession} for {@code (contestId, userId)}.
     *
     * <p>Order of checks:
     * <ol>
     *   <li>{@link #isLocked(Long, Long)} — terminal-state lockout from a
     *       previous attempt, throws {@link ProctoringStateConflictException}
     *       with code {@code "LOCKED_OUT"} (Req 13.9, 24.6).</li>
     *   <li>{@code findByContestIdAndUserIdAndEndedAtIsNull} — an active
     *       session already exists, throws {@link ProctoringStateConflictException}
     *       with code {@code "ALREADY_ACTIVE"} (Req 13.4).</li>
     *   <li>Insert + Valkey projection.</li>
     * </ol>
     *
     * @param contestId      parent {@code contests.id}
     * @param userId         candidate {@code users.id}
     * @param clientIp       resolved client IP (typically {@code X-Forwarded-For[0]});
     *                       may be {@code null}
     * @param consentVersion consent version the candidate accepted
     * @return the persisted {@link ProctoringSession}
     */
    @Transactional
    public ProctoringSession createSession(Long contestId,
                                           Long userId,
                                           String clientIp,
                                           int consentVersion) {
        // Req 13.9 / 24.6 — terminal-state lockout for prior SELF_QUIT,
        // ADMIN_FORCED, or HEARTBEAT_TIMEOUT closes.
        if (isLocked(contestId, userId)) {
            // Surface the previous endReason in the payload so the
            // candidate UI can show a precise lockout message.
            EndReason lockedReason = sessionRepo.findByContestIdAndUserId(contestId, userId)
                    .map(ProctoringSession::getEndReason)
                    .orElse(null);
            throw new ProctoringStateConflictException(
                    "LOCKED_OUT",
                    "Candidate is locked out of this proctored contest",
                    Map.of("endReason", lockedReason == null ? "UNKNOWN" : lockedReason.name()));
        }

        // Req 13.4 — at most one active session per (contest, user).
        // Instead of a hard block, surface enough state for the UI to offer
        // a Resume button (or the support message once the cap is hit).
        sessionRepo.findByContestIdAndUserIdAndEndedAtIsNull(contestId, userId)
                .ifPresent(active -> {
                    throw new ProctoringStateConflictException(
                            "ALREADY_ACTIVE",
                            "An active proctoring session already exists",
                            Map.of(
                                "sessionId", active.getId(),
                                "resumeCount", active.getResumeCount() == null ? 0 : active.getResumeCount(),
                                "maxResumes", MAX_RESUMES,
                                "resumeLimitReached",
                                    (active.getResumeCount() == null ? 0 : active.getResumeCount()) >= MAX_RESUMES));
                });

        // Distributed lock to close the TOCTOU window between the SELECT
        // above and the INSERT below. Two concurrent createSession calls
        // could both pass the endedAtIsNull check otherwise.
        String lockKey = "proctoring:session:create:" + contestId + ":" + userId;
        Boolean locked = redis.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(10));
        if (!Boolean.TRUE.equals(locked)) {
            // Re-check — the concurrent create may have finished by now
            sessionRepo.findByContestIdAndUserIdAndEndedAtIsNull(contestId, userId)
                    .ifPresent(active -> {
                        throw new ProctoringStateConflictException(
                                "ALREADY_ACTIVE",
                                "An active proctoring session already exists",
                                Map.of("sessionId", active.getId()));
                    });
            throw new ProctoringStateConflictException(
                    "CONCURRENT_CREATE",
                    "Another session creation is in progress — please retry.");
        }
        try {

        ProctoringSession session = new ProctoringSession();
        session.setContestId(contestId);
        session.setUserId(userId);
        session.setStartedAt(LocalDateTime.now());
        session.setRiskScore(0);
        session.setRiskBand(RiskBand.LOW);
        session.setFlagged(false);
        session.setClientIp(clientIp);
        session.setConsentVersion(consentVersion);

        ProctoringSession saved = sessionRepo.save(session);

        // Best-effort Valkey projection — keep lifecycle on rails even
        // if Valkey is unavailable.
        addToContestActiveSet(contestId, saved.getId());
        seedSessionCounters(saved.getId());

        // Announce on the admin SSE bus (Req 15.1, 15.2). The publisher
        // delivers to in-process @EventListener beans synchronously; a
        // failing listener must not roll back the create transaction or
        // mask the candidate response, so we wrap defensively.
        try {
            eventPublisher.publishEvent(new ProctoringSessionStartedEvent(
                    saved.getId(), contestId, userId));
        } catch (RuntimeException e) {
            log.warn("SESSION_STARTED publish failed for session {}: {}",
                    saved.getId(), e.getMessage());
        }

        return saved;
        } finally {
            redis.delete(lockKey);
        }
    }

    /**
     * Look up an active session for the requesting candidate. Throws
     * {@link ProctoringNotFoundException} (404) if the session id does
     * not exist, {@link ProctoringForbiddenException} (403) if it
     * belongs to a different user.
     */
    @Transactional(readOnly = true)
    public ProctoringSession getActiveSession(Long sessionId, Long requestingUserId) {
        ProctoringSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new ProctoringNotFoundException(
                        "Proctoring session not found: " + sessionId));
        if (!session.getUserId().equals(requestingUserId)) {
            throw new ProctoringForbiddenException("FORBIDDEN", "Session does not belong to caller");
        }
        return session;
    }

    /**
     * Resume (rejoin) the candidate's still-active session for
     * {@code (contestId, userId)} after an accidental refresh / tab close.
     *
     * <p>Order of checks:
     * <ol>
     *   <li>{@link #isLocked(Long, Long)} — a previous terminal close locks the
     *       candidate out entirely (409 {@code LOCKED_OUT}).</li>
     *   <li>An active session must exist; if none, there is nothing to resume
     *       (409 {@code NO_ACTIVE_SESSION}) and the caller should create a fresh
     *       one through the normal flow.</li>
     *   <li>{@link ProctoringSessionRepository#incrementResumeIfAllowed} — a
     *       single conditional UPDATE that increments {@code resume_count} only
     *       while it stays under {@link #MAX_RESUMES}. If it returns 0 the cap
     *       has been hit, so we throw 409 {@code RESUME_LIMIT_REACHED} and the
     *       candidate is directed to support.</li>
     * </ol>
     *
     * @param contestId parent {@code contests.id}
     * @param userId    candidate {@code users.id}
     * @return the resumed {@link ProctoringSession} (with the incremented count)
     */
    @Transactional
    public ProctoringSession resumeSession(Long contestId, Long userId) {
        if (isLocked(contestId, userId)) {
            EndReason lockedReason = sessionRepo.findByContestIdAndUserId(contestId, userId)
                    .map(ProctoringSession::getEndReason)
                    .orElse(null);
            throw new ProctoringStateConflictException(
                    "LOCKED_OUT",
                    "Candidate is locked out of this proctored contest",
                    Map.of("endReason", lockedReason == null ? "UNKNOWN" : lockedReason.name()));
        }

        ProctoringSession active = sessionRepo
                .findByContestIdAndUserIdAndEndedAtIsNull(contestId, userId)
                .orElseThrow(() -> new ProctoringStateConflictException(
                        "NO_ACTIVE_SESSION",
                        "There is no active session to resume"));

        int updated = sessionRepo.incrementResumeIfAllowed(active.getId(), MAX_RESUMES);
        if (updated == 0) {
            // Cap already reached (resume_count == MAX_RESUMES). Refuse and
            // surface the limit so the UI can show the support message.
            throw new ProctoringStateConflictException(
                    "RESUME_LIMIT_REACHED",
                    "You have reached the maximum number of resumes for this contest",
                    Map.of("maxResumes", MAX_RESUMES));
        }

        // Re-read so the caller (and the WS ticket mint) see the incremented row.
        return sessionRepo.findById(active.getId()).orElse(active);
    }

    /**
     * Candidate clicked "Finish" with at least one accepted submission.
     * Closes with {@link EndReason#SELF_FINISHED} (Req 13.6).
     *
     * @return {@code true} iff this caller actually closed the session
     *         ({@code false} for duplicate / already-closed)
     */
    @Transactional
    public boolean finish(Long sessionId, Long requestingUserId) {
        ProctoringSession session = getOwnedSession(sessionId, requestingUserId);
        return closeAndProject(session.getContestId(), sessionId, EndReason.SELF_FINISHED);
    }

    /**
     * Candidate clicked "Quit" — closes with {@link EndReason#SELF_QUIT}
     * which triggers the permanent lockout for re-entry (Req 13.6, 13.9).
     *
     * @return {@code true} iff this caller actually closed the session
     */
    @Transactional
    public boolean quit(Long sessionId, Long requestingUserId) {
        ProctoringSession session = getOwnedSession(sessionId, requestingUserId);
        return closeAndProject(session.getContestId(), sessionId, EndReason.SELF_QUIT);
    }

    /**
     * Admin-initiated termination — closes with
     * {@link EndReason#ADMIN_FORCED}. The {@code reason} string is
     * captured by the audit row written separately by the admin
     * controller; we accept it here for symmetry with the design's
     * service surface and log it for post-mortem.
     *
     * @return {@code true} iff this caller actually closed the session
     */
    @Transactional
    public boolean forceEnd(Long sessionId, Long adminId, String reason) {
        ProctoringSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new ProctoringNotFoundException(
                        "Proctoring session not found: " + sessionId));
        boolean closed = closeAndProject(session.getContestId(), sessionId, EndReason.ADMIN_FORCED);
        if (closed) {
            log.info("Admin {} force-ended proctoring session {} (reason={})",
                    adminId, sessionId, reason);
        }
        return closed;
    }

    /**
     * Heartbeat-timeout path — closes with
     * {@link EndReason#HEARTBEAT_TIMEOUT} (Req 13.7).
     *
     * @return {@code true} iff this caller actually closed the session
     */
    @Transactional
    public boolean heartbeatTimeout(Long sessionId) {
        ProctoringSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new ProctoringNotFoundException(
                        "Proctoring session not found: " + sessionId));
        return closeAndProject(session.getContestId(), sessionId, EndReason.HEARTBEAT_TIMEOUT);
    }

    /**
     * Close every still-active session for {@code contestId} with
     * {@link EndReason#CONTEST_ENDED} (Req 13.5). Used by the
     * end-of-contest sweep below.
     *
     * @return number of sessions actually closed
     */
    @Transactional
    public int closeAllForContest(Long contestId) {
        List<ProctoringSession> active = sessionRepo.findByContestIdAndEndedAtIsNull(contestId);
        int closedCount = 0;
        for (ProctoringSession s : active) {
            // Reuse the same conditional-UPDATE-plus-projection helper so
            // the SESSION_ENDED event fans out exactly once per session
            // even on concurrent contest-end vs candidate-finish races.
            if (closeAndProject(contestId, s.getId(), EndReason.CONTEST_ENDED)) {
                closedCount++;
            }
        }
        if (closedCount > 0) {
            log.info("Closed {} active proctoring sessions for contest {} (CONTEST_ENDED)",
                    closedCount, contestId);
        }
        return closedCount;
    }

    /**
     * Lockout primitive (Req 13.9, 24.6) — true iff the candidate has
     * a previous session for this contest that ended with
     * {@link EndReason#SELF_QUIT}, {@link EndReason#ADMIN_FORCED}, or
     * {@link EndReason#HEARTBEAT_TIMEOUT}.
     */
    @Transactional(readOnly = true)
    public boolean isLocked(Long contestId, Long userId) {
        return sessionRepo.existsByUserIdAndContestIdAndEndReasonIn(userId, contestId, LOCKOUT_REASONS);
    }

    /**
     * Companion to {@link #isLocked(Long, Long)} (Req 13.9, 24.6) —
     * returns the {@link EndReason} that caused the candidate to be
     * locked out of this contest's proctoring flow, or {@code null}
     * if no lockout exists. The eligibility endpoint surfaces this
     * value as {@code lockReason} so the entry page can render the
     * matching terminal screen ({@code SELF_QUIT} / {@code ADMIN_FORCED}
     * / {@code HEARTBEAT_TIMEOUT}) directly without a second round-trip.
     *
     * <p>The unique-per-({@code contest_id}, {@code user_id})
     * constraint on V7 guarantees there is at most one row to inspect,
     * so a single {@link ProctoringSessionRepository#findByContestIdAndUserId}
     * lookup is sufficient. Only end reasons in {@link #LOCKOUT_REASONS}
     * are surfaced — a session that ended with {@code SELF_FINISHED}
     * or {@code CONTEST_ENDED} is not a lockout and yields {@code null}.
     *
     * @param contestId parent {@code contests.id}
     * @param userId    candidate {@code users.id}
     * @return the lockout {@link EndReason}, or {@code null} if not locked
     */
    @Transactional(readOnly = true)
    public EndReason getLockReason(Long contestId, Long userId) {
        return sessionRepo.findByContestIdAndUserId(contestId, userId)
                .map(ProctoringSession::getEndReason)
                .filter(LOCKOUT_REASONS::contains)
                .orElse(null);
    }

    // ── Recovery / sweeps ──────────────────────────────────────────────────

    /**
     * JVM-restart recovery (Req 13.7 read with design's "conservative
     * close on restart"). Finalizes every session left active when the
     * previous JVM died with {@link EndReason#HEARTBEAT_TIMEOUT} so a
     * reconnecting candidate sees the lockout instead of being silently
     * promoted into a stale-but-active row.
     */
    @PostConstruct
    public void recoverOrphanedSessions() {
        try {
            int closed = sessionRepo.closeAllOrphans(LocalDateTime.now(), EndReason.HEARTBEAT_TIMEOUT);
            if (closed > 0) {
                log.warn("Proctoring recovery: closed {} orphaned active sessions on startup (HEARTBEAT_TIMEOUT)",
                        closed);
            }
        } catch (Exception e) {
            // Do not block boot — we'd rather come up with stale rows
            // than refuse to serve. The 30 s sweep below will catch
            // any contest-ended cases.
            log.error("Proctoring startup recovery failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Per-30 s sweep (Req 13.5) — for every contest whose
     * {@code end_time < now()}, close every still-active proctoring
     * session with {@link EndReason#CONTEST_ENDED}. Uses a filter query
     * that only loads ended contests so the sweep scales beyond the MVP
     * target without a full-table scan (fixes Bug 14).
     */
    @Scheduled(fixedDelay = 30_000L, initialDelay = 30_000L)
    public void contestEndedSweep() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Contest> endedContests = contestRepository.findByEndTimeBeforeAndStatusNot(
                    now, Contest.ContestStatus.ENDED);
            for (Contest contest : endedContests) {
                Long contestId = contest.getId();
                if (contestId == null) continue;
                // Directly call closeAllForContest — it internally runs
                // the conditional UPDATE so a double-query is unnecessary
                // (fixes Bug 5).
                closeAllForContest(contestId);
            }
        } catch (Exception e) {
            log.error("Proctoring contest-ended sweep failed: {}", e.getMessage(), e);
        }
    }

    // ── Internals ──────────────────────────────────────────────────────────

    /**
     * Public ownership check — used by the WebSocket handler for defense-in-depth
     * on every EVENT frame. Returns true if the session exists and belongs to
     * the given userId, false otherwise (session not found, wrong owner, ended).
     */
    public boolean isOwnedBy(Long sessionId, Long userId) {
        return sessionRepo.findById(sessionId)
                .map(s -> s.getUserId() != null && s.getUserId().equals(userId)
                        && s.getEndedAt() == null)
                .orElse(false);
    }

    /**
     * Resolve a session by id, validating ownership in one place so the
     * candidate-facing {@link #finish} / {@link #quit} paths share the
     * same 404 / 403 surface.
     */
    private ProctoringSession getOwnedSession(Long sessionId, Long requestingUserId) {
        ProctoringSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new ProctoringNotFoundException(
                        "Proctoring session not found: " + sessionId));
        if (!session.getUserId().equals(requestingUserId)) {
            throw new ProctoringForbiddenException("FORBIDDEN", "Session does not belong to caller");
        }
        return session;
    }

    /**
     * Issue the conditional UPDATE and, on success, scrub the Valkey
     * active-set projection. Returns {@code true} iff this caller
     * actually closed the session (1 row updated). All callers must
     * use this helper so the projection bookkeeping stays in lock-step
     * with the durable close.
     */
    private boolean closeAndProject(Long contestId, Long sessionId, EndReason reason) {
        int updated = sessionRepo.closeIfActive(sessionId, LocalDateTime.now(), reason);
        if (updated == 0) {
            return false;
        }
        removeFromContestActiveSet(contestId, sessionId);
        // Single-writer: the conditional UPDATE returned 1, so this is the
        // unique close. Announce on the admin SSE bus exactly once
        // (Req 15.1, 15.2). Best-effort — listener failures must not
        // mask the close response to the caller.
        try {
            eventPublisher.publishEvent(new ProctoringSessionEndedEvent(
                    sessionId, contestId, reason));
        } catch (RuntimeException e) {
            log.warn("SESSION_ENDED publish failed for session {}: {}",
                    sessionId, e.getMessage());
        }
        return true;
    }

    private void addToContestActiveSet(Long contestId, Long sessionId) {
        try {
            redis.opsForSet().add(contestActiveKey(contestId), String.valueOf(sessionId));
        } catch (Exception e) {
            log.warn("Failed to add proctoring session {} to contest {} active set: {}",
                    sessionId, contestId, e.getMessage());
        }
    }

    private void removeFromContestActiveSet(Long contestId, Long sessionId) {
        try {
            redis.opsForSet().remove(contestActiveKey(contestId), String.valueOf(sessionId));
        } catch (Exception e) {
            log.warn("Failed to remove proctoring session {} from contest {} active set: {}",
                    sessionId, contestId, e.getMessage());
        }
    }

    /**
     * Seed the per-session hot counters consumed by
     * {@code RiskScoringEngine}. Best-effort: a Valkey hiccup must not
     * fail session creation.
     */
    private void seedSessionCounters(Long sessionId) {
        try {
            redis.opsForValue().set(SESSION_KEY_PREFIX + sessionId + ":score", "0");
            redis.opsForValue().set(SESSION_KEY_PREFIX + sessionId + ":band", RiskBand.LOW.name());
        } catch (Exception e) {
            log.warn("Failed to seed Valkey counters for proctoring session {}: {}",
                    sessionId, e.getMessage());
        }
    }

    private static String contestActiveKey(Long contestId) {
        return CONTEST_ACTIVE_KEY_PREFIX + contestId + CONTEST_ACTIVE_KEY_SUFFIX;
    }
}
