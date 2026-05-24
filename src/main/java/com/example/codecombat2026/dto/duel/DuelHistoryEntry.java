package com.example.codecombat2026.dto.duel;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One row of the per-user duel history list returned by
 * {@code GET /api/user/duel-history}.
 *
 * <p>Built from {@code duel_matches} joined with {@code users} (for the
 * opponent's username) and {@code problems} (for the problem title), with
 * the seat-aware perspective collapsed: {@code opponentUsername} is
 * always the user other than the caller, regardless of which seat they
 * occupy.
 *
 * <p>{@code outcome} carries the row's literal {@code outcome} column
 * value ({@code USER_A_WIN} / {@code USER_B_WIN} / {@code DRAW} /
 * {@code ABANDONED}); the frontend interprets it together with
 * {@code winnerUserId} to render "You won" / "You lost" / "Draw" /
 * "Abandoned".
 *
 * @param matchId          duel match identifier
 * @param opponentUsername username of the other participant
 * @param problemId        the problem id solved in this duel
 * @param problemTitle     the problem's title at the time the row is queried
 * @param outcome          one of the four literal outcome strings
 * @param winnerUserId     the winner's user id, or {@code null} for draws
 *                         / abandonments
 * @param endedAt          timestamp the duel finalized
 */
public record DuelHistoryEntry(
        UUID matchId,
        String opponentUsername,
        Long problemId,
        String problemTitle,
        String outcome,
        Long winnerUserId,
        LocalDateTime endedAt
) {}
