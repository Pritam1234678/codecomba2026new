package com.example.codecombat2026.proctoring.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Single-use ticket for the proctoring WebSocket.
 *
 * Mirrors {@link com.example.codecombat2026.service.WsTicketService} but uses
 * the {@code proctoring:ws:ticket:} key prefix so a ticket minted for the
 * proctoring channel cannot be replayed against the compiler WS (and vice
 * versa). 32-byte hex, 60 s TTL, atomic {@code GETDEL} on consume, fail-closed
 * on Valkey errors.
 */
@Service
public class ProctoringWsTicketService {

    private static final Logger log = LoggerFactory.getLogger(ProctoringWsTicketService.class);

    private static final String KEY_PREFIX = "proctoring:ws:ticket:";
    private static final Duration TTL = Duration.ofSeconds(60);
    private static final SecureRandom RNG = new SecureRandom();

    @Autowired
    private StringRedisTemplate redis;

    public String mint(Long userId) {
        byte[] raw = new byte[32];
        RNG.nextBytes(raw);
        String ticket = HexFormat.of().formatHex(raw);
        try {
            redis.opsForValue().set(KEY_PREFIX + ticket, userId.toString(), TTL);
        } catch (Exception e) {
            log.error("Failed to store proctoring WS ticket for user {}: {}", userId, e.getMessage());
            throw new IllegalStateException("Could not issue proctoring WS ticket");
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
            log.warn("Failed to consume proctoring WS ticket: {}", e.getMessage());
            return null;
        }
    }
}
