package com.example.codecombat2026.proctoring.controller;

import com.example.codecombat2026.entity.Submission;
import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.proctoring.entity.EndReason;
import com.example.codecombat2026.proctoring.entity.ProctoringEvent;
import com.example.codecombat2026.proctoring.entity.ProctoringScreenshot;
import com.example.codecombat2026.proctoring.entity.ProctoringSession;
import com.example.codecombat2026.proctoring.entity.RiskBand;
import com.example.codecombat2026.proctoring.exception.ProctoringNotFoundException;
import com.example.codecombat2026.proctoring.exception.ProctoringStateConflictException;
import com.example.codecombat2026.proctoring.repository.ProctoringEventRepository;
import com.example.codecombat2026.proctoring.repository.ProctoringScreenshotRepository;
import com.example.codecombat2026.proctoring.repository.ProctoringSessionRepository;
import com.example.codecombat2026.proctoring.service.ProctoringAdminAuditService;
import com.example.codecombat2026.proctoring.service.ProctoringSessionService;
import com.example.codecombat2026.proctoring.service.RiskScoringEngine;
import com.example.codecombat2026.proctoring.ws.ProctoringSessionRegistry;
import com.example.codecombat2026.repository.SubmissionRepository;
import com.example.codecombat2026.repository.UserRepository;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Admin REST surface for the proctoring live dashboard, drill-down,
 * force-end, warning, and rescore actions (Req 10.2, 10.3, 12.8, 15.x,
 * 19.4, 21.5).
 *
 * <p>The class-level {@code @PreAuthorize("hasRole('ADMIN')")} gate
 * applies to every method below, on top of the {@code /api/admin/**}
 * filter-level {@code .hasRole("ADMIN")} matcher in {@code SecurityConfig}.
 * Belt-and-braces: an admin who lost their role between JWT issuance and
 * the call still gets a 403.
 *
 * <p>The companion SSE channel
 * ({@code GET .../contests/{cid}/stream} +
 * {@code POST .../contests/{cid}/stream/ticket}) lives in
 * {@link ProctoringAdminStreamController} so the filter-level
 * {@code permitAll} required for SSE async dispatch is scoped narrowly.
 *
 * <p>Audit writes (Req 15.7, 21.5) are funnelled through
 * {@link ProctoringAdminAuditService}; the audit row is persisted
 * <em>before</em> the WS-side termination/notification because the
 * append-only audit table is the single source of truth for "what did
 * the admin do" and best-effort WS pushes are a separate concern.
 *
 * <p>The live-list endpoint reads from the Valkey hot projection rather
 * than the DB so a 100-session contest is a single {@code SMEMBERS} +
 * a few {@code MGET}s, with the durable {@code proctoring_sessions}
 * row supplying only {@code username} and the at-rest {@code riskScore}
 * fallback (Req 15.1, 18.3). When Valkey is down, we fall back to the
 * DB so the dashboard degrades to read-only-with-stale-counters rather
 * than going dark.
 */
@RestController
@RequestMapping("/api/admin/proctoring")
@PreAuthorize("hasRole('ADMIN')")
public class ProctoringAdminController {

    private static final Logger log = LoggerFactory.getLogger(ProctoringAdminController.class);

    /** Connected-freshness window — Q6 resolution (Req 15.1). */
    private static final long CONNECTED_FRESHNESS_MS = 90_000L;

    /** Application-defined close code for {@code ADMIN_FORCED} terminations (Req 10.2, 13.7). */
    private static final int CLOSE_CODE_ADMIN_FORCED = 4003;

    /** Hot key prefixes mirroring the layout owned by {@code RiskScoringEngine} / {@code ProctoringSessionService}. */
    private static final String CONTEST_ACTIVE_KEY_PREFIX = "proctoring:contest:";
    private static final String CONTEST_ACTIVE_KEY_SUFFIX = ":active";
    private static final String SESSION_KEY_PREFIX = "proctoring:session:";
    private static final String SCORE_KEY_SUFFIX = ":score";
    private static final String BAND_KEY_SUFFIX = ":band";
    private static final String LAST_EVENT_KEY_SUFFIX = ":lastEventAt";
    private static final String CONNECTED_KEY_SUFFIX = ":connected";

    private final ProctoringSessionRepository sessionRepo;
    private final ProctoringEventRepository eventRepo;
    private final ProctoringScreenshotRepository screenshotRepo;
    private final ProctoringSessionService sessionService;
    private final RiskScoringEngine riskEngine;
    private final ProctoringAdminAuditService auditService;
    private final ProctoringSessionRegistry registry;
    private final StringRedisTemplate redis;
    private final UserRepository userRepo;
    private final SubmissionRepository submissionRepo;

    public ProctoringAdminController(ProctoringSessionRepository sessionRepo,
                                     ProctoringEventRepository eventRepo,
                                     ProctoringScreenshotRepository screenshotRepo,
                                     ProctoringSessionService sessionService,
                                     RiskScoringEngine riskEngine,
                                     ProctoringAdminAuditService auditService,
                                     ProctoringSessionRegistry registry,
                                     StringRedisTemplate redis,
                                     UserRepository userRepo,
                                     SubmissionRepository submissionRepo) {
        this.sessionRepo = sessionRepo;
        this.eventRepo = eventRepo;
        this.screenshotRepo = screenshotRepo;
        this.sessionService = sessionService;
        this.riskEngine = riskEngine;
        this.auditService = auditService;
        this.registry = registry;
        this.redis = redis;
        this.userRepo = userRepo;
        this.submissionRepo = submissionRepo;
    }

    // ── Wire shapes ────────────────────────────────────────────────────────

    /**
     * Live-list row returned by {@code GET /contests/{cid}/sessions}.
     * Field ordering mirrors the dashboard column order so a frontend
     * snapshot of the JSON reads top-to-bottom in the same direction the
     * grid renders.
     */
    public record LiveSessionRow(
            Long sessionId,
            Long userId,
            String username,
            int riskScore,
            RiskBand riskBand,
            boolean flagged,
            Long lastEventAtMs,
            boolean connected
    ) {}

    /**
     * Wire shape of a {@link ProctoringSession} returned to the admin UI.
     * We don't expose the JPA entity directly so the response stays
     * stable even if the entity grows columns later.
     */
    public record SessionView(
            Long id,
            Long contestId,
            Long userId,
            String username,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            EndReason endReason,
            Integer riskScore,
            RiskBand riskBand,
            Boolean flagged,
            String clientIp,
            Integer consentVersion
    ) {}

    /** Wire shape for an event row in the drill-down. */
    public record EventView(
            Long id,
            Long sessionId,
            String eventType,
            LocalDateTime clientTimestamp,
            LocalDateTime serverTimestamp,
            String payloadJson,
            Boolean replayed,
            Integer scoreDelta
    ) {
        static EventView of(ProctoringEvent e) {
            return new EventView(
                    e.getId(), e.getSessionId(), e.getEventType(),
                    e.getClientTimestamp(), e.getServerTimestamp(),
                    e.getPayloadJson(), e.getReplayed(), e.getScoreDelta());
        }
    }

    /** Wire shape for a screenshot row in the drill-down. */
    public record ScreenshotView(
            Long id,
            Long sessionId,
            Long eventId,
            LocalDateTime capturedAt,
            String mimeType,
            Integer byteSize
    ) {
        static ScreenshotView of(ProctoringScreenshot s) {
            return new ScreenshotView(
                    s.getId(), s.getSessionId(), s.getEventId(),
                    s.getCapturedAt(), s.getMimeType(), s.getByteSize());
        }
    }

    /** Wire shape for a submission row in the drill-down. */
    public record SubmissionView(
            Long id,
            Long userId,
            Long problemId,
            String problemName,
            String language,
            String status,
            LocalDateTime submittedAt,
            Integer score,
            Integer testCasesPassed,
            Integer totalTestCases
    ) {
        static SubmissionView of(Submission s) {
            return new SubmissionView(
                    s.getId(),
                    s.getUserId(),
                    s.getProblemId(),
                    s.getProblemName(),
                    s.getLanguage() == null ? null : s.getLanguage().name(),
                    s.getStatus() == null ? null : s.getStatus().name(),
                    s.getSubmittedAt(),
                    s.getScore(),
                    s.getTestCasesPassed(),
                    s.getTotalTestCases());
        }
    }

    /** Wire shape returned by {@code GET /sessions/{id}}. */
    public record SessionDetail(
            SessionView session,
            List<EventView> events,
            List<ScreenshotView> screenshots,
            List<SubmissionView> submissions
    ) {}

    /** Body for {@code POST /sessions/{id}/force-end}. */
    public record ForceEndRequest(String reason) {}

    /** Body for {@code POST /sessions/{id}/warn}. */
    public record WarnRequest(String message) {}

    /** Wire shape returned by {@code POST /sessions/{id}/force-end}. */
    public record ForceEndResponse(LocalDateTime endedAt, EndReason endReason) {}

    /** Wire shape returned by {@code POST /sessions/{id}/rescore}. */
    public record RescoreResponse(int riskScore, RiskBand riskBand, boolean flagged) {}

    // ── Endpoints ──────────────────────────────────────────────────────────

    /**
     * List of sessions for a contest, optionally filtered by the
     * {@code flagged} predicate (Req 15.1, 15.5).
     *
     * <p>Default ({@code status} absent or {@code LIVE}): reads the Valkey hot
     * set {@code proctoring:contest:{cid}:active} and follows the existing
     * cache-first path.
     *
     * <p>{@code status=ALL}: returns every session (active + ended) for this
     * contest directly from the DB, sorted by id ascending. The Valkey hot
     * projection is never consulted in this mode because ended sessions are
     * explicitly removed from the active set. Used by the admin dashboard's
     * "All Sessions" toggle so admins can review completed sessions after
     * the contest has finished.
     *
     * @param cid     parent {@code contests.id}
     * @param flagged optional filter — when present, restrict to rows
     *                whose {@code flagged} predicate matches
     * @param status  when {@code ALL}, return every session for the contest
     *                (active + ended); otherwise (including absent) return
     *                only live sessions
     * @return session rows in session-id order (stable for snapshot diffs)
     */
    @GetMapping("/contests/{cid}/sessions")
    public ResponseEntity<List<LiveSessionRow>> liveSessions(@PathVariable Long cid,
                                                             @RequestParam(value = "flagged", required = false) Boolean flagged,
                                                             @RequestParam(value = "status", required = false) String status) {
        // ── ALL mode: every session for this contest, straight from DB. ──
        boolean allMode = "ALL".equalsIgnoreCase(status);
        if (allMode) {
            List<ProctoringSession> dbRows = sessionRepo.findByContestIdOrderByIdAsc(cid);
            if (dbRows.isEmpty()) {
                return ResponseEntity.ok(Collections.emptyList());
            }

            List<Long> userIds = new ArrayList<>(dbRows.size());
            for (ProctoringSession s : dbRows) {
                if (s.getUserId() != null) userIds.add(s.getUserId());
            }
            Map<Long, String> usernameByUserId = new HashMap<>(userIds.size() * 2);
            for (User u : userRepo.findAllById(userIds)) {
                usernameByUserId.put(u.getId(), u.getUsername());
            }

            long nowMs = System.currentTimeMillis();
            List<LiveSessionRow> rows = new ArrayList<>(dbRows.size());
            for (ProctoringSession dbRow : dbRows) {
                boolean isFlagged = Boolean.TRUE.equals(dbRow.getFlagged());
                if (flagged != null && isFlagged != flagged) continue;

                int riskScore = dbRow.getRiskScore() == null ? 0 : dbRow.getRiskScore();
                RiskBand riskBand = dbRow.getRiskBand() == null ? RiskBand.LOW : dbRow.getRiskBand();
                String username = usernameByUserId.getOrDefault(dbRow.getUserId(), null);

                // ended sessions: connected=false, lastEventAtMs from
                // DB's ended_at (fallback to started_at).
                boolean active = dbRow.getEndedAt() == null;
                Long lastEventAtMs = null;
                if (active) {
                    try {
                        String raw = redis.opsForValue().get(SESSION_KEY_PREFIX + dbRow.getId() + LAST_EVENT_KEY_SUFFIX);
                        lastEventAtMs = parseLongOrNull(raw);
                    } catch (Exception ignored) { /* stay null */ }
                }

                rows.add(new LiveSessionRow(
                        dbRow.getId(), dbRow.getUserId(), username,
                        riskScore, riskBand, isFlagged, lastEventAtMs,
                        active && registry.isConnected(dbRow.getId())));
            }
            return ResponseEntity.ok(rows);
        }

        // ── LIVE mode (default) ──────────────────────────────────────────
        List<Long> sessionIds = readActiveSessionIds(cid);
        if (sessionIds.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<ProctoringSession> dbRows = sessionRepo.findAllById(sessionIds);
        Map<Long, ProctoringSession> dbBySid = new HashMap<>(dbRows.size() * 2);
        for (ProctoringSession s : dbRows) {
            dbBySid.put(s.getId(), s);
        }

        List<Long> userIds = new ArrayList<>(dbRows.size());
        for (ProctoringSession s : dbRows) {
            if (s.getUserId() != null) userIds.add(s.getUserId());
        }
        Map<Long, String> usernameByUserId = new HashMap<>(userIds.size() * 2);
        for (User u : userRepo.findAllById(userIds)) {
            usernameByUserId.put(u.getId(), u.getUsername());
        }

        List<String> keys = new ArrayList<>(sessionIds.size() * 4);
        for (Long sid : sessionIds) {
            keys.add(SESSION_KEY_PREFIX + sid + SCORE_KEY_SUFFIX);
            keys.add(SESSION_KEY_PREFIX + sid + BAND_KEY_SUFFIX);
            keys.add(SESSION_KEY_PREFIX + sid + LAST_EVENT_KEY_SUFFIX);
            keys.add(SESSION_KEY_PREFIX + sid + CONNECTED_KEY_SUFFIX);
        }
        List<String> mgetResults;
        try {
            mgetResults = redis.opsForValue().multiGet(keys);
        } catch (Exception ex) {
            log.warn("Proctoring admin: MGET failed for contest {}; using DB fallback for hot fields.", cid, ex);
            mgetResults = null;
        }

        long nowMs = System.currentTimeMillis();
        List<LiveSessionRow> rows = new ArrayList<>(sessionIds.size());

        for (int i = 0; i < sessionIds.size(); i++) {
            Long sid = sessionIds.get(i);
            ProctoringSession dbRow = dbBySid.get(sid);
            if (dbRow == null) continue;

            String scoreRaw = mgetResults != null ? safeAt(mgetResults, i * 4) : null;
            String bandRaw = mgetResults != null ? safeAt(mgetResults, i * 4 + 1) : null;
            String lastEventRaw = mgetResults != null ? safeAt(mgetResults, i * 4 + 2) : null;

            int riskScore = parseIntOrFallback(scoreRaw, dbRow.getRiskScore() == null ? 0 : dbRow.getRiskScore());
            RiskBand riskBand = parseBandOrFallback(bandRaw,
                    dbRow.getRiskBand() == null ? RiskBand.LOW : dbRow.getRiskBand());
            Long lastEventAtMs = parseLongOrNull(lastEventRaw);

            boolean wsBound = registry.isConnected(sid);
            boolean fresh = lastEventAtMs != null && (nowMs - lastEventAtMs) < CONNECTED_FRESHNESS_MS;
            boolean connected = wsBound && fresh;

            boolean isFlagged = Boolean.TRUE.equals(dbRow.getFlagged());

            if (flagged != null && isFlagged != flagged) continue;

            String username = usernameByUserId.getOrDefault(dbRow.getUserId(), null);

            rows.add(new LiveSessionRow(
                    sid, dbRow.getUserId(), username,
                    riskScore, riskBand, isFlagged, lastEventAtMs, connected));
        }

        return ResponseEntity.ok(rows);
    }

    /**
     * Drill-down for a single session — returns the session row plus the
     * full event log, screenshot index, and the submissions placed
     * inside the session window (Req 15.3, 15.4, 19.4).
     *
     * <p>Submissions are correlated by {@code (user_id, contest_id,
     * submitted_at BETWEEN started_at AND COALESCE(ended_at, NOW()))} per
     * Q4 — the {@code submissions} table stays untouched.
     *
     * @param sessionId owning {@code proctoring_sessions.id}
     * @return 200 with the four-list payload, 404 if the session is unknown
     */
    @GetMapping("/sessions/{id}")
    public ResponseEntity<SessionDetail> sessionDetail(@PathVariable("id") Long sessionId) {
        ProctoringSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new ProctoringNotFoundException(
                        "Proctoring session not found: " + sessionId));

        String username = userRepo.findById(session.getUserId())
                .map(User::getUsername).orElse(null);

        List<ProctoringEvent> events = eventRepo.findBySessionIdOrderByServerTimestampAsc(sessionId);
        List<ProctoringScreenshot> screenshots = screenshotRepo.findBySessionIdOrderByCapturedAtAsc(sessionId);

        // Open sessions: the upper bound is "now" so live sessions still
        // surface in-flight submissions in the drill-down.
        LocalDateTime windowStart = session.getStartedAt();
        LocalDateTime windowEnd = session.getEndedAt() != null ? session.getEndedAt() : LocalDateTime.now();
        List<Submission> submissions = submissionRepo.findByUser_IdAndContest_IdAndSubmittedAtBetween(
                session.getUserId(), session.getContestId(), windowStart, windowEnd);

        SessionView sessionView = new SessionView(
                session.getId(), session.getContestId(), session.getUserId(), username,
                session.getStartedAt(), session.getEndedAt(), session.getEndReason(),
                session.getRiskScore(), session.getRiskBand(), session.getFlagged(),
                session.getClientIp(), session.getConsentVersion());

        List<EventView> eventViews = new ArrayList<>(events.size());
        for (ProctoringEvent e : events) eventViews.add(EventView.of(e));
        List<ScreenshotView> shotViews = new ArrayList<>(screenshots.size());
        for (ProctoringScreenshot s : screenshots) shotViews.add(ScreenshotView.of(s));
        List<SubmissionView> submissionViews = new ArrayList<>(submissions.size());
        for (Submission s : submissions) submissionViews.add(SubmissionView.of(s));

        return ResponseEntity.ok(new SessionDetail(sessionView, eventViews, shotViews, submissionViews));
    }

    /**
     * Force-end a session — durable close, audit row, then push
     * {@code SESSION_TERMINATED} and close the WS with code 4003 (Req
     * 10.2, 13.7, 15.6, 15.7).
     *
     * <p>Order is critical:
     * <ol>
     *   <li>{@link ProctoringSessionService#forceEnd} runs the conditional
     *       close. {@code closed == false} means another writer (a racing
     *       admin tab, the heartbeat sweep, etc.) already terminated the
     *       session — surface as 409 {@code ALREADY_ENDED} so the frontend
     *       can refresh and stop showing stale "active" controls.</li>
     *   <li>Audit row is persisted before the WS push (the audit is
     *       authoritative; the WS is best-effort).</li>
     *   <li>{@link ProctoringSessionRegistry#terminate} sends
     *       {@code SESSION_TERMINATED} and closes the WS with 4003.</li>
     * </ol>
     *
     * @param sessionId owning {@code proctoring_sessions.id}
     * @param body      free-text reason recorded in the audit row
     * @param admin     authenticated admin principal
     * @return 200 with {@code endedAt} and {@code endReason}, 404 if unknown,
     *         409 {@code ALREADY_ENDED} if a previous close already finalized it
     */
    @PostMapping("/sessions/{id}/force-end")
    public ResponseEntity<ForceEndResponse> forceEnd(@PathVariable("id") Long sessionId,
                                                     @RequestBody(required = false) ForceEndRequest body,
                                                     @AuthenticationPrincipal UserDetailsImpl admin) {
        String reason = (body != null && body.reason() != null) ? body.reason() : "";
        Long adminId = admin.getId();

        boolean closed = sessionService.forceEnd(sessionId, adminId, reason);
        if (!closed) {
            throw new ProctoringStateConflictException(
                    "ALREADY_ENDED", "Session has already ended");
        }

        // Audit before WS push — the audit is the durable record of the
        // admin's action and must survive any failure on the best-effort
        // WS terminate path.
        try {
            auditService.logForceEnd(adminId, sessionId, reason);
        } catch (Exception e) {
            // Should never happen — the audit table is unconstrained on
            // reason text — but if it does, log and continue. The
            // session is already closed; we'd rather lose an audit row
            // than 500 the admin and leave them wondering whether the
            // close stuck.
            log.error("Failed to write FORCE_END audit row for session {} by admin {}: {}",
                    sessionId, adminId, e.getMessage(), e);
        }

        // Best-effort WS termination — registry.terminate is idempotent
        // and silently no-ops if no WS is bound for this sid.
        try {
            registry.terminate(sessionId, reason, EndReason.ADMIN_FORCED, CLOSE_CODE_ADMIN_FORCED);
        } catch (Exception e) {
            log.warn("Failed to terminate WS for force-ended session {}: {}", sessionId, e.getMessage());
        }

        // Re-load to surface the persisted endedAt — the conditional
        // UPDATE returned 1 above so the row is guaranteed present.
        ProctoringSession refreshed = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new ProctoringNotFoundException(
                        "Proctoring session not found after close: " + sessionId));
        return ResponseEntity.ok(new ForceEndResponse(refreshed.getEndedAt(), refreshed.getEndReason()));
    }

    /**
     * Send a non-terminating warning to a candidate (Req 10.3, 15.6, 15.7).
     *
     * <p>Order:
     * <ol>
     *   <li>Session existence check (404 if unknown).</li>
     *   <li>Audit row is persisted so the admin's warning is durable
     *       even if the WS push doesn't reach the candidate (offline,
     *       reconnecting, etc.).</li>
     *   <li>Push a {@code WARNING} frame via the registry; the session
     *       stays active.</li>
     * </ol>
     *
     * <p>Frame shape (matches the documented contract):
     * <pre>{@code
     * { "type": "WARNING",
     *   "admin_id": <number>,
     *   "message": "<text>",
     *   "acted_at": "<ISO-8601>" }
     * }</pre>
     *
     * @param sessionId owning {@code proctoring_sessions.id}
     * @param body      warning text recorded in the audit row and pushed to the WS
     * @param admin     authenticated admin principal
     * @return 204 no content
     */
    @PostMapping("/sessions/{id}/warn")
    public ResponseEntity<Void> warn(@PathVariable("id") Long sessionId,
                                     @RequestBody(required = false) WarnRequest body,
                                     @AuthenticationPrincipal UserDetailsImpl admin) {
        // 404 if the session is unknown — admins should not be writing
        // audit rows for ghost ids.
        if (!sessionRepo.existsById(sessionId)) {
            throw new ProctoringNotFoundException("Proctoring session not found: " + sessionId);
        }

        String message = (body != null && body.message() != null) ? body.message() : "";
        Long adminId = admin.getId();

        // Audit first — durable record of the admin's action.
        LocalDateTime actedAt = LocalDateTime.now();
        try {
            auditService.logWarning(adminId, sessionId, message);
        } catch (Exception e) {
            log.error("Failed to write WARNING audit row for session {} by admin {}: {}",
                    sessionId, adminId, e.getMessage(), e);
        }

        // Push the WARNING frame — best-effort; the audit row above is
        // the source of truth. Use a LinkedHashMap so the JSON key
        // order matches the documented shape.
        Map<String, Object> frame = new LinkedHashMap<>();
        frame.put("type", "WARNING");
        frame.put("admin_id", adminId);
        frame.put("message", message);
        frame.put("acted_at", actedAt.toString());
        try {
            registry.send(sessionId, frame);
        } catch (Exception e) {
            log.warn("Failed to push WARNING frame to session {}: {}", sessionId, e.getMessage());
        }

        return ResponseEntity.noContent().build();
    }

    /**
     * Recompute risk score from the persisted event log (Req 12.8) and
     * return the recomputed triple. Used after admin edits to the weight
     * table or to recover from a Valkey divergence (the engine resyncs
     * Valkey from the DB as a side-effect).
     *
     * @param sessionId owning {@code proctoring_sessions.id}
     * @return 200 with {@code { riskScore, riskBand, flagged }} from the
     *         freshly-recomputed durable projection
     */
    @PostMapping("/sessions/{id}/rescore")
    public ResponseEntity<RescoreResponse> rescore(@PathVariable("id") Long sessionId) {
        // 404 early so the rescore path doesn't write zero values into a
        // ghost row (it wouldn't, the UPDATE matches zero rows, but the
        // explicit 404 is a clearer wire response than the default 200
        // with a synthetic empty body).
        if (!sessionRepo.existsById(sessionId)) {
            throw new ProctoringNotFoundException("Proctoring session not found: " + sessionId);
        }

        int newScore = riskEngine.rescore(sessionId);

        // Reload the row to pick up the band/flagged that rescore just
        // wrote — the engine returns the score and rewrites the rest in
        // a single UPDATE, so this is one extra read for the response
        // body and avoids duplicating bandFor logic in the controller.
        ProctoringSession refreshed = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new ProctoringNotFoundException(
                        "Proctoring session not found after rescore: " + sessionId));

        return ResponseEntity.ok(new RescoreResponse(
                newScore,
                refreshed.getRiskBand() == null ? RiskBand.LOW : refreshed.getRiskBand(),
                Boolean.TRUE.equals(refreshed.getFlagged())));
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /**
     * Resolve the active session ids for a contest — Valkey first, DB
     * fallback. The Valkey set is the live source of truth; the DB
     * query is the conservative fallback used during a cache outage so
     * the dashboard never blanks out.
     */
    private List<Long> readActiveSessionIds(Long contestId) {
        String activeKey = CONTEST_ACTIVE_KEY_PREFIX + contestId + CONTEST_ACTIVE_KEY_SUFFIX;
        try {
            Set<String> raw = redis.opsForSet().members(activeKey);
            if (raw != null && !raw.isEmpty()) {
                List<Long> ids = new ArrayList<>(raw.size());
                for (String s : raw) {
                    try {
                        ids.add(Long.parseLong(s));
                    } catch (NumberFormatException nfe) {
                        log.warn("Proctoring admin: non-numeric sid '{}' in {}; ignoring.", s, activeKey);
                    }
                }
                Collections.sort(ids);
                return ids;
            }
        } catch (Exception ex) {
            log.warn("Proctoring admin: SMEMBERS failed for {}; falling back to DB.", activeKey, ex);
        }
        // DB fallback. List is small (≤ 100 sessions/JVM per Req 18.4).
        List<ProctoringSession> dbActive = sessionRepo.findByContestIdAndEndedAtIsNull(contestId);
        List<Long> ids = new ArrayList<>(dbActive.size());
        for (ProctoringSession s : dbActive) {
            if (s.getId() != null) ids.add(s.getId());
        }
        Collections.sort(ids);
        return ids;
    }

    private static String safeAt(List<String> list, int idx) {
        if (list == null || idx < 0 || idx >= list.size()) return null;
        return list.get(idx);
    }

    private static int parseIntOrFallback(String raw, int fallback) {
        if (raw == null) return fallback;
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static Long parseLongOrNull(String raw) {
        if (raw == null) return null;
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static RiskBand parseBandOrFallback(String raw, RiskBand fallback) {
        if (raw == null) return fallback;
        try {
            return RiskBand.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
