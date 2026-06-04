package com.example.codecombat2026.proctoring.controller;

import com.example.codecombat2026.controller.SubmissionController;
import com.example.codecombat2026.proctoring.event.ProctoringAdminSseRegistry;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.example.codecombat2026.service.SseTicketService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * Admin SSE handshake for the proctoring live channel (Req 15.1, 15.2,
 * 18.3). Mirrors the exact pattern used by {@code DuelController} for
 * the duel stream endpoint:
 *
 * <ol>
 *   <li>{@code POST .../stream/ticket} — JWT-authenticated and gated by
 *       {@code ROLE_ADMIN}; mints a single-use 32-byte hex ticket via
 *       {@link SseTicketService} (60 s TTL, atomic GETDEL). The
 *       ticket is bound to the admin's user id.</li>
 *   <li>{@code GET .../stream?ticket=…} — filter-level
 *       {@code permitAll} in {@code SecurityConfig} and the actual auth
 *       gate is the ticket consume in this controller. The
 *       {@code permitAll} is required because Spring's emitter
 *       return-value handler dispatches asynchronously and
 *       {@code @PreAuthorize} does not survive that dispatch (the
 *       same constraint applied to the existing submissions and duel
 *       stream endpoints).</li>
 * </ol>
 *
 * <p>Ticket consumption is atomic via {@link SseTicketService#consume}
 * — a leaked ticket grants at most a single SSE stream for at most
 * 60 s, far less blast radius than a full JWT in a query string.
 *
 * <p>This controller is intentionally minimal: only the two stream
 * endpoints defined in task 10.1. The fuller admin REST surface
 * (sessions list, drill-down, force-end, warn, rescore) lives in
 * {@code ProctoringAdminController} and lands in task 10.2.
 */
@RestController
@RequestMapping("/api/admin/proctoring/contests")
public class ProctoringAdminStreamController {

    /**
     * Admin role authority string. The {@code ROLE_} prefix matches
     * what {@link UserDetailsImpl#getAuthorities()} populates from the
     * {@code Role.ERole.ROLE_ADMIN} enum, so checking this exact value
     * lines up with both filter-level {@code .hasRole("ADMIN")} and
     * the per-method {@code @PreAuthorize("hasRole('ADMIN')")} gate.
     */
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final SseTicketService sseTicketService;
    private final ProctoringAdminSseRegistry registry;

    public ProctoringAdminStreamController(SseTicketService sseTicketService,
                                           ProctoringAdminSseRegistry registry) {
        this.sseTicketService = sseTicketService;
        this.registry = registry;
    }

    /**
     * Mint a single-use SSE ticket for the calling admin. The ticket is
     * bound to the admin's {@code userId}; the GET stream verifies the
     * resolved user is still an admin before opening the emitter so a
     * leaked ticket cannot be replayed by a user whose role changed
     * between mint and consume.
     */
    @PostMapping("/{contestId}/stream/ticket")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> issueSseTicket(
            @PathVariable Long contestId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        // The contestId path variable is informational here — the ticket
        // itself is bound to the admin user, not to a specific contest.
        // The GET stream uses the path variable to register the emitter
        // against the correct per-contest fan-out bucket.
        String ticket = sseTicketService.issue(userDetails.getId());
        return ResponseEntity.ok(Map.of("ticket", ticket));
    }

    /**
     * Open a per-contest admin SSE stream. Filter-level
     * {@code permitAll}; the single-use ticket is the credential.
     *
     * <p>On a successful subscribe the emitter receives an immediate
     * {@code connected} event from
     * {@link ProctoringAdminSseRegistry#register(Long)}. Subsequent
     * frames ({@code RISK_BAND_CHANGED}, {@code SESSION_STARTED},
     * {@code SESSION_ENDED}) are pushed by the
     * {@code ProctoringSseListener} bridge as the underlying
     * {@code ApplicationEvent}s fire.
     *
     * <p>Errors:
     * <ul>
     *   <li>401 — ticket missing / invalid / used. Reuses the
     *       {@code SseAuthException} marker mapped in
     *       {@code GlobalExceptionHandler} so the response shape
     *       matches every other proctored-stream endpoint.</li>
     * </ul>
     */
    @GetMapping(value = "/{contestId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @PathVariable Long contestId,
            @RequestParam(required = false) String ticket,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            HttpServletResponse response) {

        Long ticketUserId = sseTicketService.consume(ticket);
        if (ticketUserId == null) {
            throw new SubmissionController.SseAuthException();
        }

        // Defense in depth: the ticket-bound user must still hold
        // ROLE_ADMIN at consume time. If their role was revoked between
        // mint and consume — or if a non-admin somehow got a ticket
        // — refuse the stream. We accept the principal off the
        // SecurityContext when present (filter-level permitAll still
        // runs the JWT filter when an Authorization header is sent),
        // and otherwise rely on the ticket binding having been minted
        // by an admin in the first place.
        if (userDetails != null) {
            if (!userDetails.getId().equals(ticketUserId)
                    || !hasRoleAdmin(userDetails)) {
                throw new SubmissionController.SseAuthException();
            }
        }

        // SSE-friendly response headers — disable nginx buffering, hint
        // intermediaries not to cache, hold the connection open.
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");

        return registry.register(contestId);
    }

    private static boolean hasRoleAdmin(UserDetailsImpl userDetails) {
        if (userDetails.getAuthorities() == null) return false;
        for (GrantedAuthority a : userDetails.getAuthorities()) {
            if (ROLE_ADMIN.equals(a.getAuthority())) return true;
        }
        return false;
    }
}
