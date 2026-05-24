package com.example.codecombat2026.dto.duel;

/**
 * Snapshot of Live Duel Mode runtime metrics for the admin dashboard.
 *
 * <p>Returned by {@code DuelService.getMetrics()} and serialized verbatim by
 * {@code AdminDuelController}. All counts are point-in-time — the dashboard
 * polls the endpoint every 5 s.
 *
 * <ul>
 *   <li>{@code activeMatchCount} — rows in {@code duel_matches} with
 *       {@code status} in {@code ('WAITING','IN_PROGRESS')}.</li>
 *   <li>{@code queueDepth} — entries on the {@code duel:queue} Valkey list.</li>
 *   <li>{@code matchesFinishedToday} — rows in {@code duel_matches} with
 *       {@code status='FINISHED'} and {@code ended_at &gt;=} start-of-today
 *       (Asia/Kolkata).</li>
 *   <li>{@code matchesAbandonedToday} — same, restricted to
 *       {@code outcome='ABANDONED'}.</li>
 *   <li>{@code sseConnectionCount} — live SSE subscriptions on the
 *       {@code DuelSseEmitterRegistry}.</li>
 * </ul>
 */
public record DuelMetrics(
        long activeMatchCount,
        int queueDepth,
        long matchesFinishedToday,
        long matchesAbandonedToday,
        int sseConnectionCount
) {}
