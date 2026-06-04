package com.example.codecombat2026.proctoring.ws;

import com.example.codecombat2026.proctoring.entity.ProctoringSession;
import com.example.codecombat2026.proctoring.repository.ProctoringSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Authenticates and binds the upgrade for {@code /api/proctoring/ws}.
 *
 * <p>The interceptor is the only auth gate on the proctoring channel. JWT
 * never travels in WS query parameters or sub-protocols (Req 16.2); instead
 * the candidate POSTs a JWT-authenticated REST call to mint a single-use,
 * 60 s, 32-byte hex Valkey ticket via {@link ProctoringWsTicketService}, then
 * upgrades with {@code ?ticket=…&sessionId=…}. This class atomically consumes
 * the ticket (GETDEL semantics on Valkey), looks up the session row, verifies
 * that the ticket-bound user owns the session and that the session is still
 * active, then stores the resolved {@code (userId, sessionId, contestId, ip)}
 * tuple in the WS attributes for later reads by
 * {@code ProctoringWebSocketHandler}.
 *
 * <p>Every rejection returns HTTP 401 at the upgrade (Req 9.3, 16.2, 16.3) —
 * we deliberately do not distinguish "bad ticket" from "session not yours" so
 * an attacker cannot probe for session ids. The ticket is consumed before the
 * session lookup, which is the right order: a single-use ticket should be
 * burned even when the rest of the handshake fails, so an attacker cannot
 * retry the same ticket against a different sessionId.
 *
 * <p>Validates Req 9.2, 9.3, 16.2, 16.3.
 */
@Component
public class ProctoringHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ProctoringHandshakeInterceptor.class);

    private final ProctoringWsTicketService ticketService;
    private final ProctoringSessionRepository sessionRepository;

    public ProctoringHandshakeInterceptor(ProctoringWsTicketService ticketService,
                                          ProctoringSessionRepository sessionRepository) {
        this.ticketService = ticketService;
        this.sessionRepository = sessionRepository;
    }

    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request,
                                   @NonNull ServerHttpResponse response,
                                   @NonNull WebSocketHandler wsHandler,
                                   @NonNull Map<String, Object> attrs) {
        // Resolve real client IP early so it can be logged on rejection paths
        // if needed. The attribute is only consumed on success — Spring discards
        // attrs when beforeHandshake returns false.
        String ip = resolveClientIp(request);

        // 1. Parse + atomically consume the ticket. consume() returns the bound
        //    userId or null if the ticket is missing / expired / already used.
        String ticket = firstQueryParam(request, "ticket");
        Long ticketUserId = (ticket == null) ? null : ticketService.consume(ticket);
        if (ticketUserId == null) {
            log.debug("Proctoring WS rejected: missing or invalid ticket from ip={}", ip);
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        // 2. Parse sessionId. The ticket has already been burned at this point,
        //    so an attacker cannot retry with a different sessionId.
        Long sessionId = parseLong(firstQueryParam(request, "sessionId"));
        if (sessionId == null) {
            log.debug("Proctoring WS rejected: missing or non-numeric sessionId (user={}, ip={})",
                ticketUserId, ip);
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        // 3. Load the session and verify ownership + active state.
        Optional<ProctoringSession> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            log.debug("Proctoring WS rejected: session {} not found (user={}, ip={})",
                sessionId, ticketUserId, ip);
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        ProctoringSession session = sessionOpt.get();
        if (!ticketUserId.equals(session.getUserId())) {
            log.warn("Proctoring WS rejected: ticket user {} does not own session {} (owner={}, ip={})",
                ticketUserId, sessionId, session.getUserId(), ip);
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        if (session.getEndedAt() != null) {
            log.debug("Proctoring WS rejected: session {} already ended at {} (user={}, ip={})",
                sessionId, session.getEndedAt(), ticketUserId, ip);
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        // 4. Bind the resolved tuple into WS attributes for the handler to read.
        attrs.put("userId", ticketUserId);
        attrs.put("sessionId", sessionId);
        attrs.put("contestId", session.getContestId());
        attrs.put("ip", ip);
        return true;
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request,
                               @NonNull ServerHttpResponse response,
                               @NonNull WebSocketHandler wsHandler,
                               @Nullable Exception exception) {
        // No-op. Lifecycle (register, heartbeat, terminate) is owned by the handler.
    }

    /**
     * Real client IP, preferring the first hop of {@code X-Forwarded-For}
     * (the convention nginx uses when fronting the JVM) and falling back to
     * the servlet remote address. Mirrors {@code CompilerWebSocketConfig}'s
     * {@code TicketAndIpInterceptor} so both WS endpoints see the candidate
     * IP, not the load balancer's.
     */
    private static String resolveClientIp(ServerHttpRequest request) {
        List<String> xff = request.getHeaders().get("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            String first = xff.get(0);
            if (first != null) {
                String hop = first.split(",")[0].trim();
                if (!hop.isEmpty()) {
                    return hop;
                }
            }
        }
        if (request instanceof ServletServerHttpRequest sreq) {
            String remote = sreq.getServletRequest().getRemoteAddr();
            if (remote != null && !remote.isEmpty()) {
                return remote;
            }
        }
        return "unknown";
    }

    /**
     * Returns the first occurrence of {@code name} in the raw query string,
     * URL-decoded with UTF-8, or {@code null} if missing. We parse the raw
     * query rather than going through Spring's binding so the interceptor is
     * decoupled from any controller annotations and so we can read the same
     * way for both servlet and (future) reactive transports.
     */
    @Nullable
    private static String firstQueryParam(ServerHttpRequest request, String name) {
        String q = request.getURI().getQuery();
        if (q == null || q.isEmpty()) return null;
        for (String pair : q.split("&")) {
            int eq = pair.indexOf('=');
            if (eq <= 0) continue;
            if (name.equals(pair.substring(0, eq))) {
                return URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    @Nullable
    private static Long parseLong(@Nullable String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
