package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.duel.DuelMatchView;
import com.example.codecombat2026.dto.duel.EnqueueRequest;
import com.example.codecombat2026.dto.duel.EnqueueResult;
import com.example.codecombat2026.dto.duel.HeartbeatRequest;
import com.example.codecombat2026.dto.duel.RoomStateEvent;
import com.example.codecombat2026.dto.duel.RunDuelRequest;
import com.example.codecombat2026.dto.duel.SubmitDuelRequest;
import com.example.codecombat2026.exception.DuelStateConflictException;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.example.codecombat2026.service.DuelService;
import com.example.codecombat2026.service.DuelSseEmitterRegistry;
import com.example.codecombat2026.service.MatchmakingService;
import com.example.codecombat2026.service.SseTicketService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST surface for Live Duel Mode.
 *
 * <p>Exposes the eight client-facing endpoints under {@code /api/duels}:
 * matchmaking ({@code POST}/{@code DELETE} {@code /queue}), match details
 * ({@code GET /{matchId}}), gameplay ({@code POST /{matchId}/submissions},
 * {@code POST /{matchId}/forfeit}, {@code POST /{matchId}/heartbeat}),
 * and the SSE handshake ({@code POST /{matchId}/sse-ticket} +
 * {@code GET /{matchId}/stream}).
 *
 * <p>All endpoints require a valid JWT via {@code @PreAuthorize} except
 * {@code /{matchId}/stream} — that endpoint is filter-level
 * {@code permitAll} (configured in {@code SecurityConfig}) and is
 * authenticated by a single-use SSE ticket consumed via
 * {@link SseTicketService#consume(String)}, mirroring the existing
 * {@code /api/submissions/stream} pattern.
 *
 * <p>Most error mapping is delegated to {@code GlobalExceptionHandler}:
 * {@code DuelNotFoundException → 404}, {@code DuelForbiddenException → 403},
 * {@code DuelStateConflictException → 409}. The one special-case here is
 * the {@code COOLDOWN_ACTIVE} state on {@code POST /queue}, which needs an
 * HTTP 429 with a {@code Retry-After} header (Requirement 10.4) and so is
 * caught explicitly before the global handler can map it to 409.
 */
@RestController
@RequestMapping("/api/duels")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DuelController {

    private static final Logger log = LoggerFactory.getLogger(DuelController.class);

    @Autowired private MatchmakingService matchmakingService;
    @Autowired private DuelService duelService;
    @Autowired private DuelSseEmitterRegistry duelSseEmitterRegistry;
    @Autowired private SseTicketService sseTicketService;

    // ─────────────────────────────────────────────────────────────────────
    // Matchmaking
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Enqueue the authenticated user for matchmaking.
     *
     * <p>Idempotent within the 5 s service-side window: a second call from
     * the same user returns the same {@code queueToken} and {@code queuedAt}.
     * The request body is intentionally permissive (empty / missing / any
     * JSON object) so the frontend can post {@code {}} or nothing at all.
     *
     * <p>Cooldown is mapped to HTTP 429 with a {@code Retry-After} header
     * carrying the remaining TTL in seconds. All other state conflicts
     * (e.g. {@code ALREADY_IN_MATCH}) bubble up to the global handler and
     * become 409.
     */
    @PostMapping("/queue")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> enqueue(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody(required = false) EnqueueRequest body) {
        try {
            String difficulty = body != null ? body.difficulty() : null;
            MatchmakingService.EnqueueResult serviceResult =
                    matchmakingService.enqueue(userDetails.getId(), difficulty);
            EnqueueResult dto = new EnqueueResult(
                    serviceResult.getQueueToken(),
                    serviceResult.getQueuedAt(),
                    serviceResult.getDifficulty());
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException ex) {
            // Bad / missing difficulty value — bubble up as 400.
            Map<String, Object> respBody = new LinkedHashMap<>();
            respBody.put("error", "BAD_REQUEST");
            respBody.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(respBody);
        } catch (DuelStateConflictException ex) {
            if ("COOLDOWN_ACTIVE".equals(ex.getCode())) {
                long retryAfterSec = extractRetryAfterSec(ex.getPayload());
                Map<String, Object> respBody = new LinkedHashMap<>();
                respBody.put("error", "COOLDOWN_ACTIVE");
                respBody.put("retryAfterSec", retryAfterSec);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .header("Retry-After", Long.toString(retryAfterSec))
                        .body(respBody);
            }
            throw ex;
        }
    }

    /**
     * Cancel the authenticated user's queue entry. Idempotent — returns
     * 204 whether or not the user was actually in the queue.
     */
    @DeleteMapping("/queue")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancel(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        matchmakingService.cancel(userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Match read-model and gameplay
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Check if the authenticated user has an active (WAITING or IN_PROGRESS)
     * duel match. Returns 204 if no active match exists, or 200 with minimal
     * match info for the "Resume Match" button in the lobby.
     *
     * <p>IMPORTANT: This endpoint MUST be declared before {@code /{matchId}}
     * so Spring does not attempt to parse "active" as a UUID path variable.
     */
    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getActiveMatch(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long userId = userDetails.getId();
        List<com.example.codecombat2026.entity.DuelMatch> active =
                duelService.getActiveMatchesForUser(userId);
        if (active.isEmpty()) {
            return ResponseEntity.noContent().build(); // 204 = no active match
        }
        com.example.codecombat2026.entity.DuelMatch m = active.get(0);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("matchId", m.getMatchId().toString());
        body.put("status", m.getStatus().name());
        body.put("startedAt", m.getStartedAt() != null ? m.getStartedAt().toString() : null);
        body.put("difficulty", m.getDifficulty());
        body.put("timeLimitSec", m.getTimeLimitSec());
        return ResponseEntity.ok(body);
    }

    /**
     * Fetch a duel match's read-model. Returns 403 via
     * {@code DuelForbiddenException} if the caller is not a participant
     * (Requirement 4.6 / 13.5).
     */
    @GetMapping("/{matchId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DuelMatchView> getMatch(
            @PathVariable UUID matchId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        DuelMatchView view = duelService.getMatch(matchId, userDetails.getId());
        return ResponseEntity.ok(view);
    }

    /**
     * Submit a solution inside a duel. Returns 202 with
     * {@code { "submissionId": N }}; the verdict arrives later over the
     * per-match SSE channel (Requirements 5.1 / 4.3).
     */
    @PostMapping("/{matchId}/submissions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> submit(
            @PathVariable UUID matchId,
            @RequestBody SubmitDuelRequest body,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long submissionId = duelService.submitForDuel(
                matchId, userDetails.getId(), body.code(), body.language());
        Map<String, Object> respBody = Map.of("submissionId", submissionId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(respBody);
    }

    /**
     * Run code synchronously against the problem's example test cases only.
     * Counts toward the 5-runs-per-match limit. Returns the result inline —
     * no SSE event, no submissions row.
     */
    @PostMapping("/{matchId}/run")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DuelService.RunResult> run(
            @PathVariable UUID matchId,
            @RequestBody RunDuelRequest body,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        DuelService.RunResult result = duelService.runForDuel(
                matchId,
                userDetails.getId(),
                body.code(),
                body.language(),
                body.stdin());
        return ResponseEntity.ok(result);
    }

    /**
     * Recent finished-duel history for the calling user. Used by the
     * lobby "Recent Duels" card. Query param {@code limit} clamped to
     * {@code [1, 50]} server-side, default 10.
     */
    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public java.util.List<com.example.codecombat2026.service.DuelService.DuelHistoryEntry> history(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") int limit) {
        return duelService.getDuelHistory(userDetails.getId(), limit);
    }

    /**
     * Per-match submission list for the calling user. Used by the arena
     * to rebuild the "Your submissions" panel on mount / refresh.
     */
    @GetMapping("/{matchId}/submissions")
    @PreAuthorize("isAuthenticated()")
    public java.util.List<com.example.codecombat2026.service.DuelService.DuelSubmissionView> mySubmissions(
            @PathVariable UUID matchId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return duelService.getMatchSubmissions(matchId, userDetails.getId());
    }

    /**
     * Forfeit the calling user's seat in the match. Returns the freshly
     * re-read {@link DuelMatchView} so the frontend can render the result
     * modal directly (Requirements 7.5 / 7.6).
     */
    @PostMapping("/{matchId}/forfeit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DuelMatchView> forfeit(
            @PathVariable UUID matchId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long userId = userDetails.getId();
        duelService.forfeit(matchId, userId);
        return ResponseEntity.ok(duelService.getMatch(matchId, userId));
    }

    /**
     * Typing heartbeat. The server rate-limits the resulting opponent
     * SSE emission to one per 1500 ms per user (Property 16). Returns 204
     * unconditionally — heartbeat fan-out is best-effort.
     */
    @PostMapping("/{matchId}/heartbeat")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> heartbeat(
            @PathVariable UUID matchId,
            @RequestBody(required = false) HeartbeatRequest body,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        // Body is currently informational only — DuelService treats every
        // heartbeat as kind="typing". Future kinds can branch here.
        duelService.heartbeat(matchId, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────────────────────────────
    // SSE handshake
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Issue a single-use SSE ticket bound to the calling user.
     *
     * <p>Verifies the caller is a participant of the requested match
     * before minting the ticket — {@code DuelService.getMatch} throws
     * {@code DuelForbiddenException} (→ 403) if not, which is the same
     * gate the stream endpoint enforces post-consume. This keeps the
     * 403 response on a credentialed endpoint rather than on the SSE
     * stream where Spring's emitter return-value handler would rewrite
     * the status to 200.
     */
    @PostMapping("/{matchId}/sse-ticket")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> issueSseTicket(
            @PathVariable UUID matchId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        // Participant check — throws 403 if the caller is not user_a/user_b.
        duelService.getMatch(matchId, userDetails.getId());
        String ticket = sseTicketService.issue(userDetails.getId());
        return ResponseEntity.ok(Map.of("ticket", ticket));
    }

    /**
     * SSE stream for per-match events. Filter-level {@code permitAll}; the
     * single-use ticket is the credential.
     *
     * <p>On a successful subscribe, the registry sends an immediate
     * {@code connected} event, the controller then pushes a
     * {@code room_state} event with the current match snapshot, and the
     * service's {@link DuelService#onSubscriptionOpened} hook cancels any
     * pending reconnect-grace timer for this user and emits
     * {@code progress {event:'reconnected'}} when applicable.
     *
     * <p>Errors:
     * <ul>
     *   <li>401 — ticket missing / invalid / used. Mapped via
     *       {@link SubmissionController.SseAuthException} which
     *       {@code GlobalExceptionHandler} already translates.</li>
     *   <li>403 — ticket valid but the bound user is not a participant
     *       of the requested match. Thrown by {@code DuelService.getMatch}.</li>
     * </ul>
     */
    @GetMapping(value = "/{matchId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @PathVariable UUID matchId,
            @RequestParam(required = false) String ticket,
            HttpServletResponse response) {

        Long ticketUserId = sseTicketService.consume(ticket);
        if (ticketUserId == null) {
            throw new SubmissionController.SseAuthException();
        }

        // Participant check — throws 403 if the ticket-bound user is not
        // in this match. We fetch the view once and reuse it below so the
        // initial room_state event is consistent with the participant
        // check (no TOCTOU window for the username/status fields).
        DuelMatchView view = duelService.getMatch(matchId, ticketUserId);

        // SSE-friendly response headers — disable nginx buffering, hint
        // intermediaries not to cache, hold the connection open.
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");

        SseEmitter emitter = duelSseEmitterRegistry.register(matchId, ticketUserId);

        // Notify the service so any pending reconnect timer is cancelled
        // and a `progress {event:'reconnected'}` event is emitted to the
        // room when applicable. Best-effort: a callback failure must not
        // tear down the freshly-registered subscription.
        try {
            duelService.onSubscriptionOpened(matchId, ticketUserId);
        } catch (RuntimeException ex) {
            log.warn("onSubscriptionOpened failed for match={} user={}: {}",
                    matchId, ticketUserId, ex.getMessage());
        }

        // Initial room_state — gives the browser everything it needs to
        // hydrate DuelArena.jsx without an extra GET (Requirement 4.1).
        try {
            RoomStateEvent roomState = buildRoomState(view);
            duelSseEmitterRegistry.emitTo(matchId, ticketUserId, "room_state", roomState);
        } catch (RuntimeException ex) {
            log.warn("Failed to emit initial room_state for match={} user={}: {}",
                    matchId, ticketUserId, ex.getMessage());
        }

        // Late-joiner shortcut: if the match already finished before this
        // user opened the SSE stream, immediately push match_finished so
        // the arena renders the result modal without waiting for a
        // server-initiated event that will never come (the finalize paths
        // already fired and the registry's late-event guard would normally
        // drop a fresh emit, but we exempt match_finished from that guard).
        if ("FINISHED".equals(view.status())) {
            try {
                java.util.Map<String, Object> mfPayload = new java.util.LinkedHashMap<>();
                mfPayload.put("matchId", matchId.toString());
                mfPayload.put("outcome", view.outcome());
                mfPayload.put("winnerUserId", view.winnerUserId());
                mfPayload.put("winnerUsername", view.winnerUsername());
                mfPayload.put("endedAt", view.endedAt() != null ? view.endedAt().toString() : null);
                mfPayload.put("ts", java.time.Instant.now().toString());
                duelSseEmitterRegistry.emitTo(matchId, ticketUserId, "match_finished", mfPayload);
            } catch (RuntimeException ex) {
                log.warn("Failed to emit late-joiner match_finished for match={} user={}: {}",
                        matchId, ticketUserId, ex.getMessage());
            }
        }

        return emitter;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Internals
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Pull {@code retryAfterSec} out of the {@code COOLDOWN_ACTIVE}
     * payload set by {@code MatchmakingService.enqueue}. Defensive
     * against odd shapes — defaults to {@code 1} second if the value is
     * missing or unparseable so the client always gets a sane
     * {@code Retry-After} header.
     */
    private static long extractRetryAfterSec(Object payload) {
        if (payload instanceof Map<?, ?> map) {
            Object raw = map.get("retryAfterSec");
            if (raw instanceof Number n) {
                long v = n.longValue();
                return v > 0 ? v : 1L;
            }
            if (raw != null) {
                try {
                    long v = Long.parseLong(raw.toString());
                    return v > 0 ? v : 1L;
                } catch (NumberFormatException ignored) {
                    // fall through
                }
            }
        }
        return 1L;
    }

    /**
     * Build a {@link RoomStateEvent} from the {@link DuelMatchView} read
     * model. Live presence flags are filled from
     * {@link DuelSseEmitterRegistry#hasActiveSubscription} so the
     * snapshot reflects the registry state at emit time (Requirement 4.1).
     */
    private RoomStateEvent buildRoomState(DuelMatchView view) {
        boolean aConnected = duelSseEmitterRegistry
                .hasActiveSubscription(view.matchId(), view.userAId());
        boolean bConnected = duelSseEmitterRegistry
                .hasActiveSubscription(view.matchId(), view.userBId());

        RoomStateEvent.ParticipantInfo userA = new RoomStateEvent.ParticipantInfo(
                view.userAId(), view.userAUsername(), aConnected);
        RoomStateEvent.ParticipantInfo userB = new RoomStateEvent.ParticipantInfo(
                view.userBId(), view.userBUsername(), bConnected);

        return new RoomStateEvent(
                view.matchId(),
                userA,
                userB,
                view.problemId(),
                view.status(),
                view.startedAt(),
                view.remainingSeconds()
        );
    }
}
