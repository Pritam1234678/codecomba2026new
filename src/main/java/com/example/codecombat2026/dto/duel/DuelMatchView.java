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
 * relative to {@code TimeUtil.now()} and the per-match {@code timeLimitSec}
 * — they are never persisted.
 *
 * <p>V4 fields:
 * <ul>
 *   <li>{@code difficulty} — EASY/MEDIUM/HARD bucket the match was created in.</li>
 *   <li>{@code timeLimitSec} — total match window in seconds (1200/2400/3900).</li>
 *   <li>{@code runsUsed} / {@code submitsUsed} — counters from Valkey for the
 *       calling user (null on admin views).</li>
 *   <li>{@code runsRemaining} / {@code submitsRemaining} — derived from the
 *       per-match limits (5 runs, 2 submits).</li>
 * </ul>
 */
public record DuelMatchView(
        UUID matchId,
        Long userAId,
        String userAUsername,
        Long userBId,
        String userBUsername,
        Long problemId,
        DuelProblemView problem,
        String status,
        String outcome,
        Long winnerUserId,
        String winnerUsername,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Long elapsedSeconds,
        Long remainingSeconds,
        String yourSeat,
        String difficulty,
        Integer timeLimitSec,
        Integer runsUsed,
        Integer submitsUsed,
        Integer runsRemaining,
        Integer submitsRemaining
) {

    /**
     * Inline problem snapshot embedded in {@link DuelMatchView}.
     */
    public record DuelProblemView(
            Long id,
            String title,
            String description,
            String inputFormat,
            String outputFormat,
            String constraints,
            String example1,
            String example2,
            String example3,
            Double timeLimit,
            Integer memoryLimit,
            String level
    ) {}
}
