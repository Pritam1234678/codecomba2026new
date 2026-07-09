package com.example.codecombat2026.proctoring.controller;

import com.example.codecombat2026.proctoring.entity.EndReason;
import com.example.codecombat2026.proctoring.entity.ProctoringSession;
import com.example.codecombat2026.proctoring.exception.ProctoringNotFoundException;
import com.example.codecombat2026.proctoring.exception.ProctoringStateConflictException;
import com.example.codecombat2026.proctoring.repository.ProctoringSessionRepository;
import com.example.codecombat2026.proctoring.service.ProctoringSessionService;
import com.example.codecombat2026.proctoring.ws.ProctoringSessionRegistry;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * Candidate-facing REST mirror of the WebSocket {@code FINISH} / {@code QUIT}
 * frames. These endpoints exist so a candidate can deliberately end a
 * Proctoring_Session even when the WebSocket is currently disconnected
 * (e.g. inside the offline-buffer modal triggered by Req 11.4 / 11.5).
 *
 * <p>Both endpoints delegate to {@link ProctoringSessionService} which
 * performs the same conditional UPDATE used by every other terminating
 * code path:
 * {@code SET ended_at=:ts, end_reason=:reason WHERE id=:id AND ended_at IS NULL}.
 * The {@code ended_at IS NULL} predicate makes the call idempotent — a
 * duplicate request from a flaky network or a click-after-WS-close races
 * cleanly: the first writer wins, and any subsequent caller observes
 * {@code closed == false} and is mapped to {@code 409 ALREADY_ENDED}.
 *
 * <p>Ownership is enforced inside the service:
 * {@link ProctoringSessionService#finish} and
 * {@link ProctoringSessionService#quit} both throw
 * {@code ProctoringNotFoundException} (mapped to 404) for unknown ids and
 * {@code ProctoringForbiddenException} (mapped to 403) when the session
 * does not belong to the caller, so the controller stays a thin shell.
 *
 * <p><b>WebSocket termination</b> — when the candidate has an active WS
 * connection, the design wants the server to push {@code SESSION_TERMINATED}
 * and close cleanly via {@code ProctoringSessionRegistry.terminate}. The
 * registry is delivered by task 4.3 (not yet present). For now the durable
 * close still happens here; the WS, if any, will detect the session as
 * already-ended on its next heartbeat / event and exit the same path. See
 * the {@code TODO(task 4.3)} block below.
 *
 * <p>Wire-shape contract (Req 13.6, 13.8, 24.2, 24.5):
 * <ul>
 *   <li>{@code POST /api/proctoring/sessions/{id}/finish} → 200
 *       {@code { endedAt, endReason: "SELF_FINISHED" }}</li>
 *   <li>{@code POST /api/proctoring/sessions/{id}/quit}   → 200
 *       {@code { endedAt, endReason: "SELF_QUIT" }} and applies the
 *       lockout from Req 13.9 / 24.6</li>
 *   <li>Either, on a session that is already ended → 409
 *       {@code { error: "ALREADY_ENDED", message: "Session has already ended" }}</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/proctoring/sessions")
public class ProctoringFinishQuitController {

    private static final Logger log = LoggerFactory.getLogger(ProctoringFinishQuitController.class);

    private final ProctoringSessionService sessionService;
    private final ProctoringSessionRepository sessionRepo;
    private final ProctoringSessionRegistry registry;

    public ProctoringFinishQuitController(ProctoringSessionService sessionService,
                                          ProctoringSessionRepository sessionRepo,
                                          ProctoringSessionRegistry registry) {
        this.sessionService = sessionService;
        this.sessionRepo = sessionRepo;
        this.registry = registry;
    }

    /** Wire shape for a successful close — used by both endpoints. */
    public record EndedView(LocalDateTime endedAt, EndReason endReason) {}

    /**
     * Candidate clicked "Finish Contest" (Req 13.6, 24.2). Closes the
     * session with {@link EndReason#SELF_FINISHED}. {@code SELF_FINISHED}
     * does NOT lock the candidate out — it's the happy-path completion.
     *
     * @return 200 with the persisted {@code endedAt} and {@code endReason}
     *         when this caller actually closed the session, 409
     *         {@code ALREADY_ENDED} when a prior close already finalized it.
     */
    @PostMapping("/{id}/finish")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EndedView> finish(@PathVariable Long id,
                                            @AuthenticationPrincipal UserDetailsImpl user) {
        boolean closed = sessionService.finish(id, user.getId());
        if (!closed) {
            throw new ProctoringStateConflictException(
                    "ALREADY_ENDED", "Session has already ended");
        }

        // Push SESSION_TERMINATED and close the WS if one is still connected.
        // The durable close above already won the race; a concurrent WS FINISH
        // frame will observe `closed == false` but is harmless. Close code 1000
        // matches the WS-path dispatchFinish behaviour so the frontend treats
        // it as a clean normal close and does not attempt to reconnect.
        try {
            registry.terminate(id, "completed", EndReason.SELF_FINISHED, 1000);
        } catch (Exception e) {
            log.warn("WS terminate failed for FINISH on session {}: {}", id, e.getMessage());
        }

        return ResponseEntity.ok(loadEndedView(id));
    }

    /**
     * Candidate clicked "Quit Contest" (Req 13.8, 24.5). Closes the
     * session with {@link EndReason#SELF_QUIT} which triggers the
     * permanent lockout from Req 13.9 / 24.6: the candidate cannot
     * create another session for this {@code (contest_id, user_id)}
     * pair for the remainder of the contest's lifetime.
     *
     * @return 200 with the persisted {@code endedAt} and {@code endReason}
     *         when this caller actually closed the session, 409
     *         {@code ALREADY_ENDED} when a prior close already finalized it.
     */
    @PostMapping("/{id}/quit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EndedView> quit(@PathVariable Long id,
                                          @AuthenticationPrincipal UserDetailsImpl user) {
        boolean closed = sessionService.quit(id, user.getId());
        if (!closed) {
            throw new ProctoringStateConflictException(
                    "ALREADY_ENDED", "Session has already ended");
        }

        try {
            registry.terminate(id, "quit", EndReason.SELF_QUIT, 1000);
        } catch (Exception e) {
            log.warn("WS terminate failed for QUIT on session {}: {}", id, e.getMessage());
        }

        return ResponseEntity.ok(loadEndedView(id));
    }

    /**
     * Re-load the just-closed session to surface {@code endedAt} and
     * {@code endReason} in the response body. Splitting this off keeps
     * both handlers symmetrical and makes the read explicit. Throws
     * {@link ProctoringNotFoundException} only in the unreachable case
     * where the row vanishes between the conditional UPDATE and the
     * read-back — that would indicate an external delete and is
     * surfaced as a clean 404 by the global handler.
     */
    private EndedView loadEndedView(Long sessionId) {
        ProctoringSession s = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new ProctoringNotFoundException(
                        "Proctoring session not found: " + sessionId));
        return new EndedView(s.getEndedAt(), s.getEndReason());
    }
}
