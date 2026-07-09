package com.example.codecombat2026.controller;

import com.example.codecombat2026.entity.Submission;
import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.exception.ForbiddenException;
import com.example.codecombat2026.proctoring.entity.ProctoringEvent;
import com.example.codecombat2026.proctoring.entity.ProctoringScreenshot;
import com.example.codecombat2026.proctoring.entity.ProctoringSession;
import com.example.codecombat2026.proctoring.entity.RiskBand;
import com.example.codecombat2026.proctoring.exception.ProctoringNotFoundException;
import com.example.codecombat2026.proctoring.repository.ProctoringEventRepository;
import com.example.codecombat2026.proctoring.repository.ProctoringScreenshotRepository;
import com.example.codecombat2026.proctoring.repository.ProctoringSessionRepository;
import com.example.codecombat2026.repository.SubmissionRepository;
import com.example.codecombat2026.repository.UserRepository;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.example.codecombat2026.service.PrivateContestAccessValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * REST controller for contest hosts to access proctoring data for their private contests.
 * 
 * This controller provides endpoints for hosts to:
 * - List proctoring sessions for their contest
 * - View detailed proctoring data including events and screenshots
 * - Monitor participant risk scores and flags
 * 
 * Authorization:
 * - Only the contest host or an admin can access proctoring data
 * - Access is validated per-request using PrivateContestAccessValidator
 * 
 * Data source:
 * - Reads from both Valkey (hot/live data) and PostgreSQL (durable storage)
 * - Falls back to database if Valkey is unavailable
 * - Uses same data structures as admin proctoring dashboard
 * 
 * Requirements: 15.1, 15.2, 15.3, 15.4, 15.5
 * Related: Task 12.2
 */
@RestController
@RequestMapping("/api/contests/private")
@PreAuthorize("hasRole('USER')")
public class PrivateContestProctoringController {

    private static final Logger log = LoggerFactory.getLogger(PrivateContestProctoringController.class);

    /** Connected-freshness window — same as admin controller. */
    private static final long CONNECTED_FRESHNESS_MS = 90_000L;

    /** Hot key prefixes mirroring the layout owned by RiskScoringEngine / ProctoringSessionService. */
    private static final String SESSION_KEY_PREFIX = "proctoring:session:";
    private static final String SCORE_KEY_SUFFIX = ":score";
    private static final String BAND_KEY_SUFFIX = ":band";
    private static final String LAST_EVENT_KEY_SUFFIX = ":lastEventAt";

    private final ProctoringSessionRepository sessionRepo;
    private final ProctoringEventRepository eventRepo;
    private final ProctoringScreenshotRepository screenshotRepo;
    private final UserRepository userRepo;
    private final SubmissionRepository submissionRepo;
    private final StringRedisTemplate redis;
    private final PrivateContestAccessValidator accessValidator;

    public PrivateContestProctoringController(ProctoringSessionRepository sessionRepo,
                                             ProctoringEventRepository eventRepo,
                                             ProctoringScreenshotRepository screenshotRepo,
                                             UserRepository userRepo,
                                             SubmissionRepository submissionRepo,
                                             StringRedisTemplate redis,
                                             PrivateContestAccessValidator accessValidator) {
        this.sessionRepo = sessionRepo;
        this.eventRepo = eventRepo;
        this.screenshotRepo = screenshotRepo;
        this.userRepo = userRepo;
        this.submissionRepo = submissionRepo;
        this.redis = redis;
        this.accessValidator = accessValidator;
    }

    // ── Wire shapes ────────────────────────────────────────────────────────

    /**
     * Live session row returned by GET /contests/private/{id}/proctoring/sessions.
     * Mirrors the admin controller's LiveSessionRow.
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
     * Wire shape of a ProctoringSession returned to the host UI.
     */
    public record SessionView(
            Long id,
            Long contestId,
            Long userId,
            String username,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            String endReason,
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

    /** Wire shape returned by GET /sessions/{id}. */
    public record SessionDetail(
            SessionView session,
            List<EventView> events,
            List<ScreenshotView> screenshots,
            List<SubmissionView> submissions
    ) {}

    // ── Endpoints ──────────────────────────────────────────────────────────

    /**
     * List proctoring sessions for a private contest.
     * Only accessible by the contest host or an admin.
     * 
     * Returns all sessions (active + ended) for the contest, optionally filtered by flagged status.
     * Uses database as primary source since this is typically accessed post-contest for review.
     * 
     * @param contestId parent contests.id
     * @param flagged optional filter - when present, restrict to rows whose flagged predicate matches
     * @param currentUser authenticated user
     * @return session rows in session-id order
     */
    @GetMapping("/{contestId}/proctoring/sessions")
    public ResponseEntity<List<LiveSessionRow>> listSessions(
            @PathVariable Long contestId,
            @RequestParam(value = "flagged", required = false) Boolean flagged,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        
        Long userId = currentUser.getId();
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        
        // Validate access: must be host or admin
        if (!isAdmin && !accessValidator.isHost(contestId, userId)) {
            throw new ForbiddenException("You do not have access to proctoring data for this contest");
        }
        
        log.info("User {} (admin={}) accessing proctoring sessions for contest {}", userId, isAdmin, contestId);
        
        // Fetch all sessions for this contest from database
        List<ProctoringSession> dbRows = sessionRepo.findByContestIdOrderByIdAsc(contestId);
        if (dbRows.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        
        // Fetch usernames
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
            
            // For active sessions, try to get last event time from Valkey
            boolean active = dbRow.getEndedAt() == null;
            Long lastEventAtMs = null;
            boolean connected = false;
            
            if (active) {
                try {
                    String raw = redis.opsForValue().get(SESSION_KEY_PREFIX + dbRow.getId() + LAST_EVENT_KEY_SUFFIX);
                    lastEventAtMs = parseLongOrNull(raw);
                    if (lastEventAtMs != null) {
                        connected = (nowMs - lastEventAtMs) < CONNECTED_FRESHNESS_MS;
                    }
                } catch (Exception ignored) { 
                    // Valkey unavailable, stay with database values
                }
            }
            
            rows.add(new LiveSessionRow(
                    dbRow.getId(), dbRow.getUserId(), username,
                    riskScore, riskBand, isFlagged, lastEventAtMs, connected));
        }
        
        return ResponseEntity.ok(rows);
    }

    /**
     * Get detailed proctoring data for a single session.
     * Only accessible by the contest host or an admin.
     * 
     * Returns the session details, event log, screenshots, and submissions
     * placed during the session window.
     * 
     * @param contestId parent contests.id
     * @param sessionId owning proctoring_sessions.id
     * @param currentUser authenticated user
     * @return detailed session data including events, screenshots, and submissions
     */
    @GetMapping("/{contestId}/proctoring/sessions/{sessionId}")
    public ResponseEntity<SessionDetail> sessionDetail(
            @PathVariable Long contestId,
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        
        Long userId = currentUser.getId();
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        
        // Validate access: must be host or admin
        if (!isAdmin && !accessValidator.isHost(contestId, userId)) {
            throw new ForbiddenException("You do not have access to proctoring data for this contest");
        }
        
        // Fetch session and validate it belongs to the specified contest
        ProctoringSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new ProctoringNotFoundException(
                        "Proctoring session not found: " + sessionId));
        
        if (!session.getContestId().equals(contestId)) {
            throw new ForbiddenException("Session does not belong to the specified contest");
        }
        
        log.info("User {} (admin={}) accessing proctoring session {} for contest {}", 
                userId, isAdmin, sessionId, contestId);
        
        // Fetch username
        String username = userRepo.findById(session.getUserId())
                .map(User::getUsername).orElse(null);
        
        // Fetch events, screenshots, and submissions
        List<ProctoringEvent> events = eventRepo.findBySessionIdOrderByServerTimestampAsc(sessionId);
        List<ProctoringScreenshot> screenshots = screenshotRepo.findBySessionIdOrderByCapturedAtAsc(sessionId);
        
        // Submissions within session window
        LocalDateTime windowStart = session.getStartedAt();
        LocalDateTime windowEnd = session.getEndedAt() != null ? session.getEndedAt() : LocalDateTime.now();
        List<Submission> submissions = submissionRepo.findByUser_IdAndContest_IdAndSubmittedAtBetween(
                session.getUserId(), session.getContestId(), windowStart, windowEnd);
        
        // Build response
        SessionView sessionView = new SessionView(
                session.getId(), session.getContestId(), session.getUserId(), username,
                session.getStartedAt(), session.getEndedAt(), 
                session.getEndReason() == null ? null : session.getEndReason().name(),
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

    // ── Helpers ────────────────────────────────────────────────────────────

    private static Long parseLongOrNull(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
