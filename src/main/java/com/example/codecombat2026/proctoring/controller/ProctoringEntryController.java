package com.example.codecombat2026.proctoring.controller;

import com.example.codecombat2026.entity.Contest;
import com.example.codecombat2026.proctoring.entity.EndReason;
import com.example.codecombat2026.proctoring.entity.ProctoredContest;
import com.example.codecombat2026.proctoring.entity.ProctoringConsentAck;
import com.example.codecombat2026.proctoring.entity.ProctoringSession;
import com.example.codecombat2026.proctoring.exception.ProctoringForbiddenException;
import com.example.codecombat2026.proctoring.exception.ProctoringNotFoundException;
import com.example.codecombat2026.proctoring.exception.ProctoringStateConflictException;
import com.example.codecombat2026.proctoring.repository.ProctoredContestRepository;
import com.example.codecombat2026.proctoring.service.ProctoringConsentService;
import com.example.codecombat2026.proctoring.service.ProctoringSessionService;
import com.example.codecombat2026.proctoring.ws.ProctoringWsTicketService;
import com.example.codecombat2026.repository.ContestRepository;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.example.codecombat2026.service.ContestRegistrationService;
import com.example.codecombat2026.util.TimeUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Candidate-facing entry surface for proctored contests.
 *
 * <p>Three preflight steps and one reconnect helper:
 *
 * <ol>
 *   <li>{@code GET /contests/{cid}/eligibility} — single round-trip the
 *       entry shell uses to decide whether to render the registration
 *       gate, the consent dialog, the lockout terminal, or the live
 *       arena (Req 2.3, 2.5, 2.6, 13.9).</li>
 *   <li>{@code POST /contests/{cid}/consent} — records the candidate's
 *       acceptance against the current {@code consent_version} of the
 *       proctored contest (Req 2.3).</li>
 *   <li>{@code POST /contests/{cid}/sessions} — creates the
 *       {@link ProctoringSession} after verifying consent. Lockout and
 *       already-active conflicts surface as 409 via the global
 *       exception handler (Req 13.3, 13.4, 13.9).</li>
 *   <li>{@code POST /sessions/{id}/ws-ticket} — mints a fresh single-use
 *       WS ticket for reconnect after a network blip; verifies the
 *       session belongs to the caller and is still active (Req 16.3).</li>
 * </ol>
 *
 * <p>Every method is JWT-authenticated via
 * {@code @PreAuthorize("isAuthenticated()")} (Req 16.1). Authorization
 * specific to each endpoint (registration, consent, ownership) is
 * enforced inline because the rules differ per route.
 *
 * <p><b>WS ticket minting is deferred to task 4.1.</b> Both
 * {@link #createSession(Long, ConsentRequest, UserDetailsImpl, HttpServletRequest)}
 * and {@link #wsTicket(Long, UserDetailsImpl)} return a {@code null} /
 * empty {@code wsTicket} for now and carry an explicit {@code TODO}
 * marker so the WebSocket task can wire {@code ProctoringWsTicketService.mint}
 * in without touching the rest of the entry flow. The frontend reads
 * the absence of the field as "not yet available".
 */
@RestController
@RequestMapping("/api/proctoring")
public class ProctoringEntryController {

    private final ContestRegistrationService registrationService;
    private final ProctoredContestRepository proctoredContestRepository;
    private final ProctoringConsentService consentService;
    private final ProctoringSessionService sessionService;
    private final ProctoringWsTicketService wsTicketService;
    private final ContestRepository contestRepository;

    public ProctoringEntryController(ContestRegistrationService registrationService,
                                     ProctoredContestRepository proctoredContestRepository,
                                     ProctoringConsentService consentService,
                                     ProctoringSessionService sessionService,
                                     ProctoringWsTicketService wsTicketService,
                                     ContestRepository contestRepository) {
        this.registrationService = registrationService;
        this.proctoredContestRepository = proctoredContestRepository;
        this.consentService = consentService;
        this.sessionService = sessionService;
        this.wsTicketService = wsTicketService;
        this.contestRepository = contestRepository;
    }

    // ── Request / response shapes ──────────────────────────────────────────

    /** Body for {@code POST /contests/{cid}/consent}. */
    public record ConsentRequest(Integer consentVersion) {}

    /**
     * Wire shape returned by {@code GET /contests/{cid}/eligibility}.
     *
     * <p>{@code consentVersion} is non-null only when the contest is
     * proctored — for non-proctored contests there is no version to
     * acknowledge against. {@code lockReason} is the {@link EndReason}
     * from the candidate's previous terminated session and is {@code null}
     * when the candidate is not locked out.
     */
    public record EligibilityResponse(
            boolean registered,
            boolean proctored,
            boolean consentAccepted,
            boolean locked,
            EndReason lockReason,
            Integer consentVersion,
            boolean contestEnded
    ) {}

    // ── Endpoints ──────────────────────────────────────────────────────────

    /**
     * Single-round-trip eligibility snapshot consumed by the entry shell.
     *
     * <p>Reads {@code contest_registrations} (cache-first via
     * {@link ContestRegistrationService}), {@code proctored_contests},
     * {@code proctoring_consent_acks}, and {@code proctoring_sessions}
     * and folds them into the small DTO above. The shell uses the four
     * booleans to decide which screen to render without making four
     * separate calls.
     *
     * @param cid         parent {@code contests.id}
     * @param userDetails authenticated principal
     * @return 200 with the eligibility snapshot
     */
    @GetMapping("/contests/{cid}/eligibility")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EligibilityResponse> eligibility(@PathVariable Long cid,
                                                           @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long userId = userDetails.getId();

        // Verify the contest exists — return 404 for invalid contest ids
        // instead of a silent all-false eligibility (fixes Bug 12).
        Contest contest = contestRepository.findById(cid)
                .orElseThrow(() -> new ProctoringNotFoundException(
                        "Contest not found: " + cid));

        boolean registered = registrationService.isRegistered(cid, userId);

        // Existence of a proctored_contests row is the only authoritative
        // signal that this contest runs in proctored mode (Req 1.6).
        var proctoredOpt = proctoredContestRepository.findByContestId(cid);
        boolean proctored = proctoredOpt.isPresent();

        // consentAccepted and consentVersion are only meaningful for
        // proctored contests — leave them at false / null otherwise so
        // the shell's "needs consent" branch never triggers on a regular
        // contest.
        boolean consentAccepted = false;
        Integer consentVersion = null;
        if (proctored) {
            ProctoredContest pc = proctoredOpt.get();
            consentVersion = pc.getConsentVersion();
            consentAccepted = consentService.hasAck(userId, cid, consentVersion);
        }

        boolean locked = sessionService.isLocked(cid, userId);
        EndReason lockReason = sessionService.getLockReason(cid, userId);

        // The contest is ended when its end_time has passed or its DB status
        // is ENDED. This lets the frontend show a "contest has ended" screen
        // rather than a confused state where everything looks normal but
        // sessions can't be created (fixes Bug 3).
        boolean contestEnded = contest.getEndTime() != null
                && contest.getEndTime().isBefore(com.example.codecombat2026.util.TimeUtil.now());

        return ResponseEntity.ok(new EligibilityResponse(
                registered, proctored, consentAccepted, locked, lockReason,
                consentVersion, contestEnded));
    }

    /**
     * Record (or replay) a consent acknowledgment for the calling
     * candidate.
     *
     * <p>Order of checks:
     * <ol>
     *   <li>403 {@code NOT_REGISTERED} — candidate must have a
     *       {@code contest_registrations} row before we accept a
     *       consent ack; consent for a contest the candidate isn't
     *       even registered for is dead data.</li>
     *   <li>403 {@code CONSENT_MISSING} when the contest is not
     *       proctored (no {@code proctored_contests} row to pin the
     *       version against). The service would otherwise return a 404
     *       which leaks the proctored/non-proctored distinction; the
     *       403 here keeps the response shape consistent with the
     *       create-session path.</li>
     *   <li>Delegate to {@link ProctoringConsentService#recordAck} which
     *       enforces version match (400 on mismatch) and is idempotent
     *       on replay thanks to the unique constraint
     *       {@code (user_id, contest_id, consent_version)} from V7.</li>
     * </ol>
     *
     * <p>{@code client_ip} is taken from the first hop of the
     * {@code X-Forwarded-For} header (we sit behind nginx in
     * production); when the header is missing we fall back to
     * {@code X-Real-IP} and finally {@code remoteAddr}.
     *
     * @param cid         parent {@code contests.id}
     * @param body        {@link ConsentRequest} carrying the
     *                    {@code consentVersion} the candidate clicked
     *                    "Accept" against
     * @param userDetails authenticated principal
     * @param request     servlet request used for IP / UA extraction
     * @return 200 with {@code { acceptedAt }}
     */
    @PostMapping("/contests/{cid}/consent")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> consent(@PathVariable Long cid,
                                                       @RequestBody ConsentRequest body,
                                                       @AuthenticationPrincipal UserDetailsImpl userDetails,
                                                       HttpServletRequest request) {
        Long userId = userDetails.getId();

        if (!registrationService.isRegistered(cid, userId)) {
            throw new ProctoringForbiddenException(
                    "NOT_REGISTERED",
                    "Candidate is not registered for this contest");
        }

        // Per design: hide the proctored/non-proctored distinction behind
        // a single 403 CONSENT_MISSING on this path so the wire shape
        // matches the create-session path's CONSENT_MISSING. Version
        // mismatch is handled inside the service (400).
        if (!proctoredContestRepository.existsByContestId(cid)) {
            throw new ProctoringForbiddenException(
                    "CONSENT_MISSING",
                    "Contest is not proctored");
        }

        String clientIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        ProctoringConsentAck ack = consentService.recordAck(
                userId, cid, body.consentVersion(), clientIp, userAgent);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("acceptedAt", ack.getAcceptedAt());
        return ResponseEntity.ok(response);
    }

    /**
     * Create a {@link ProctoringSession} for the calling candidate.
     *
     * <p>Order of checks:
     * <ol>
     *   <li>403 {@code CONSENT_MISSING} when the contest is not proctored
     *       (no row to pin the version against) or when the candidate
     *       has not yet accepted the current {@code consent_version}.
     *       The eligibility endpoint surfaces both signals so a correct
     *       client never reaches this state — the check here is the
     *       server-side enforcement.</li>
     *   <li>Delegate to {@link ProctoringSessionService#createSession}
     *       which enforces lockout (409 {@code LOCKED_OUT}) and the
     *       at-most-one-active-session invariant (409
     *       {@code ALREADY_ACTIVE}) via
     *       {@link ProctoringStateConflictException}; the global
     *       exception handler renders both as 409 with the appropriate
     *       payload.</li>
     * </ol>
     *
     * <p>The minted {@code wsTicket} is intentionally left {@code null}
     * for now — wiring it to {@code ProctoringWsTicketService.mint(userId)}
     * lands in task 4.1 once the WebSocket skeleton ships. The frontend
     * reads {@code wsTicket == null} as "WS not yet available" and falls
     * back to polling until the field appears.
     *
     * @param cid         parent {@code contests.id}
     * @param userDetails authenticated principal
     * @param request     servlet request used for IP extraction
     * @return 201 with {@code { sessionId, wsTicket, startedAt }}
     */
    @PostMapping("/contests/{cid}/sessions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> createSession(@PathVariable Long cid,
                                                             @AuthenticationPrincipal UserDetailsImpl userDetails,
                                                             HttpServletRequest request) {
        Long userId = userDetails.getId();

        // Verify proctored mode and resolve the current consent version
        // in one lookup so we avoid a second hit inside the service.
        ProctoredContest proctored = proctoredContestRepository.findByContestId(cid)
                .orElseThrow(() -> new ProctoringForbiddenException(
                        "CONSENT_MISSING", "Contest is not proctored"));
        Integer consentVersion = proctored.getConsentVersion();

        if (!consentService.hasAck(userId, cid, consentVersion)) {
            throw new ProctoringForbiddenException(
                    "CONSENT_MISSING",
                    "Consent missing for current version");
        }

        String clientIp = getClientIp(request);
        ProctoringSession session = sessionService.createSession(
                cid, userId, clientIp, consentVersion);

        // TODO(task 4.1): mint wsTicket via ProctoringWsTicketService.mint(userId)
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sessionId", session.getId());
        response.put("wsTicket", wsTicketService.mint(userId));
        response.put("startedAt", session.getStartedAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Resume (rejoin) the calling candidate's still-active session after an
     * accidental refresh / tab close.
     *
     * <p>Delegates to {@link ProctoringSessionService#resumeSession} which:
     * <ul>
     *   <li>enforces lockout (409 {@code LOCKED_OUT}),</li>
     *   <li>requires an active session to exist (409 {@code NO_ACTIVE_SESSION}),</li>
     *   <li>caps the number of resumes at {@code MAX_RESUMES} and throws
     *       409 {@code RESUME_LIMIT_REACHED} once the cap is reached.</li>
     * </ul>
     *
     * <p>On success returns the same {@code { sessionId, wsTicket, startedAt }}
     * shape as create-session so the frontend can hand it straight to the arena.
     *
     * @param cid         parent {@code contests.id}
     * @param userDetails authenticated principal
     * @return 200 with {@code { sessionId, wsTicket, startedAt, resumeCount }}
     */
    @PostMapping("/contests/{cid}/sessions/resume")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> resumeSession(@PathVariable Long cid,
                                                            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long userId = userDetails.getId();

        ProctoringSession session = sessionService.resumeSession(cid, userId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sessionId", session.getId());
        response.put("wsTicket", wsTicketService.mint(userId));
        response.put("startedAt", session.getStartedAt());
        response.put("resumeCount", session.getResumeCount());
        return ResponseEntity.ok(response);
    }

    /**
     * Mint a fresh WebSocket ticket for an existing active session — used
     * by the candidate UI on reconnect after a transient network blip
     * (Req 16.3).
     *
     * <p>Verifies that the session belongs to the caller (via
     * {@link ProctoringSessionService#getActiveSession} which throws
     * 404 / 403 as appropriate) and that the session is still active —
     * a {@code SESSION_ENDED} 409 prevents minting a ticket against a
     * terminated session, which would otherwise let a closed candidate
     * silently reconnect.
     *
     * <p>Ticket minting itself is deferred to task 4.1; for now this
     * endpoint returns 200 with an empty body so the frontend can
     * exercise the request shape.
     *
     * @param sessionId   owning {@code proctoring_sessions.id}
     * @param userDetails authenticated principal
     * @return 200 with empty body (ticket field added in task 4.1)
     */
    @PostMapping("/sessions/{id}/ws-ticket")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> wsTicket(@PathVariable("id") Long sessionId,
                                                        @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long userId = userDetails.getId();

        ProctoringSession session = sessionService.getActiveSession(sessionId, userId);

        LocalDateTime endedAt = session.getEndedAt();
        if (endedAt != null) {
            throw new ProctoringStateConflictException(
                    "SESSION_ENDED",
                    "Session is no longer active",
                    Map.of("endReason", session.getEndReason() == null
                            ? "UNKNOWN" : session.getEndReason().name()));
        }

        // Check consent-version bump: if the admin has bumped the
        // consent_version on the proctored_contests row since this
        // session was created, the candidate must re-consent before
        // reconnecting (fixes Bug 7).
        Integer sessionConsentVersion = session.getConsentVersion();
        if (sessionConsentVersion != null) {
            Integer currentVersion = proctoredContestRepository.findByContestId(session.getContestId())
                    .map(ProctoredContest::getConsentVersion)
                    .orElse(null);
            if (currentVersion != null && !currentVersion.equals(sessionConsentVersion)
                    && !consentService.hasAck(userId, session.getContestId(), currentVersion)) {
                throw new ProctoringStateConflictException(
                        "CONSENT_VERSION_CHANGED",
                        "Consent version has been updated — please re-acknowledge before reconnecting",
                        Map.of("currentVersion", currentVersion,
                               "yourVersion", sessionConsentVersion));
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("wsTicket", wsTicketService.mint(userId));
        return ResponseEntity.ok(response);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /**
     * Resolve the real client IP behind the production nginx hop. Mirrors
     * the helper in {@code AuthController}, {@code SupportController}, and
     * {@code CompilerController} so the proctoring layer logs and stores
     * the same IP value as the rest of the platform.
     *
     * <p>{@code X-Forwarded-For} wins (first hop, not the proxy chain),
     * then {@code X-Real-IP}, then {@code remoteAddr}. May return
     * {@code null} only when the servlet container itself returns
     * {@code null} for {@code remoteAddr}, which doesn't happen in
     * practice but the consent / session columns are nullable to match.
     */
    private static String getClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String real = req.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) {
            return real;
        }
        return req.getRemoteAddr();
    }
}
