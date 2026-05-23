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
 * Single-use ticket for the interactive compiler WebSocket.
 *
 * Browsers can't set Authorization headers on a raw WebSocket upgrade. To
 * keep the JWT out of URLs/logs, the client POSTs /api/compiler/ws-ticket,
 * receives a 32-byte hex ticket bound to its userId, and includes it as
 * {@code ?ticket=...} on the upgrade. The handshake handler GETDELs the key —
 * the ticket dies on first use within 60 seconds.
 *
 * Anonymous users are still allowed via the WS rate limit (10/min/IP). For
 * those, no ticket is required; the handler treats absence of a ticket as
 * "anonymous" and applies the public limits.
 */
@Service
public class WsTicketService {

    private static final Logger log = LoggerFactory.getLogger(WsTicketService.class);

    private static final String KEY_PREFIX = "ws:ticket:";
    private static final Duration TTL = Duration.ofSeconds(60);
    private static final SecureRandom RNG = new SecureRandom();

    @Autowired
    private StringRedisTemplate redis;

    public String issue(Long userId) {
        byte[] raw = new byte[32];
        RNG.nextBytes(raw);
        String ticket = HexFormat.of().formatHex(raw);
        try {
            redis.opsForValue().set(KEY_PREFIX + ticket, userId.toString(), TTL);
        } catch (Exception e) {
            log.error("Failed to store WS ticket for user {}: {}", userId, e.getMessage());
            throw new IllegalStateException("Could not issue WS ticket");
        }
        return ticket;
    }

    /** Returns userId on success, null if ticket is invalid / expired / used. */
    public Long consume(String ticket) {
        if (ticket == null || ticket.isBlank()) return null;
        try {
            String userId = redis.opsForValue().getAndDelete(KEY_PREFIX + ticket);
            if (userId == null) return null;
            return Long.parseLong(userId);
        } catch (Exception e) {
            log.warn("Failed to consume WS ticket: {}", e.getMessage());
            return null;
        }
    }
}
