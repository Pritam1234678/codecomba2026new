package com.example.codecombat2026.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Issues short-lived, single-use tickets that authenticate SSE connections.
 *
 * The browser's EventSource API can't set Authorization headers, so the SSE
 * endpoint previously took a JWT in the {@code ?token=} query string. JWTs
 * are long-lived and end up in nginx logs, browser history, and proxy logs —
 * unacceptable for a credential.
 *
 * Flow:
 *   1. Authenticated user POSTs /api/submissions/sse-ticket → gets a
 *      cryptographically random 32-byte hex ticket bound to their userId.
 *      Ticket is stored in Valkey with a 60-second TTL.
 *   2. Browser opens GET /api/submissions/stream?ticket=&lt;ticket&gt;.
 *      Server atomically GETDELs the key. If present, the value is the
 *      userId and the SSE stream is registered. If absent (already used or
 *      expired), 401.
 *
 * Tickets are single-use and short-lived, so leaking one through logs grants
 * at most a 60-second window to open a single SSE stream tied to one user —
 * far smaller blast radius than a JWT.
 */
@Service
public class SseTicketService {

    private static final Logger log = LoggerFactory.getLogger(SseTicketService.class);

    private static final String KEY_PREFIX = "sse:ticket:";
    private static final Duration TTL = Duration.ofSeconds(60);
    private static final SecureRandom RNG = new SecureRandom();

    @Autowired
    private StringRedisTemplate redis;

    /** Issue a fresh ticket for the given userId. */
    public String issue(Long userId) {
        byte[] raw = new byte[32];
        RNG.nextBytes(raw);
        String ticket = HexFormat.of().formatHex(raw);
        try {
            redis.opsForValue().set(KEY_PREFIX + ticket, userId.toString(), TTL);
        } catch (Exception e) {
            log.error("Failed to store SSE ticket for user {}: {}", userId, e.getMessage());
            throw new IllegalStateException("Could not issue SSE ticket");
        }
        return ticket;
    }

    /**
     * Atomically consume a ticket. Returns the bound userId, or null if the
     * ticket is invalid / already used / expired.
     */
    public Long consume(String ticket) {
        if (ticket == null || ticket.isBlank()) return null;
        try {
            // GETDEL is atomic — at most one consumer can succeed
            String userId = redis.opsForValue().getAndDelete(KEY_PREFIX + ticket);
            if (userId == null) return null;
            return Long.parseLong(userId);
        } catch (NumberFormatException nfe) {
            log.warn("Malformed SSE ticket value: {}", nfe.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Failed to consume SSE ticket: {}", e.getMessage());
            return null;
        }
    }
}
