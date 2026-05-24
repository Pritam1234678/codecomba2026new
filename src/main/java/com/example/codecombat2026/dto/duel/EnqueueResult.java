package com.example.codecombat2026.dto.duel;

import java.time.Instant;

/**
 * REST-contract response body for {@code POST /api/duels/queue}.
 *
 * <p>Adapted from {@code MatchmakingService.EnqueueResult} (the service
 * carries an extra {@code alreadyQueued} flag for internal control flow).
 *
 * @param queueToken 64-char hex idempotency token returned by the
 *                   matchmaking service.
 * @param queuedAt   server-side instant the user landed on the queue.
 * @param difficulty echo of the difficulty the user was queued into
 *                   (EASY / MEDIUM / HARD). V4.
 */
public record EnqueueResult(String queueToken, Instant queuedAt, String difficulty) {}
