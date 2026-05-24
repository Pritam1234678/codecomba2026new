package com.example.codecombat2026.dto.duel;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Read-model projection of a {@link com.example.codecombat2026.entity.DuelMatch}
 * row, denormalised for the frontend.
 *
 * <p>Returned by {@code DuelService.getMatch(matchId, requesterId)} and the
 * admin {@code listMatches} endpoint. Usernames are looked up at query time
 * because they are stable and small (≤ 20 chars). The transient fields
 * {@code elapsedSeconds} / {@code remainingSeconds} are computed on demand
 * relative to {@code TimeUtil.now()} and the configured draw-timeout — they
 * are never persisted.
 *
 * <p>{@code yourSeat} carries {@code "A"} / {@code "B"} for participant
 * callers and {@code null} for admin views (where the caller is not in the
 * match).
 */
public record DuelMatchView(
        UUID matchId,
        Long userAId,
        String userAUsername,
        Long userBId,
        String userBUsername,
        Long problemId,
        String status,
        String outcome,
        Long winnerUserId,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Long elapsedSeconds,
        Long remainingSeconds,
        String yourSeat
) {}
