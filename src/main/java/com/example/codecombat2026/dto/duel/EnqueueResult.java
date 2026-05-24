package com.example.codecombat2026.dto.duel;

import java.time.Instant;

/**
 * REST-contract response body for {@code POST /api/duels/queue}.
 *
 * <p>This top-level record is intentionally distinct from the
 * {@code MatchmakingService.EnqueueResult} static-nested class. The service
 * carries an extra {@code alreadyQueued} flag for internal control flow
 * (idempotent retry detection); the controller adapts the service value
 * into this framework-clean DTO so the public contract stays minimal —
 * just the queue token and the timestamp at which the user joined the
 * queue (Requirements 1.1, 1.2, 8.1).
 *
 * @param queueToken 64-char hex idempotency token returned by the
 *                   matchmaking service. Identical across retries within
 *                   the 5 s idempotency window.
 * @param queuedAt   server-side instant the user landed on
 *                   {@code duel:queue}. Frontend uses this only for
 *                   display; pairing latency is bounded by the 250 ms
 *                   pair-loop tick.
 */
public record EnqueueResult(String queueToken, Instant queuedAt) {}
