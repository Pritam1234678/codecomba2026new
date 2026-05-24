package com.example.codecombat2026.dto.duel;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payload of the terminal {@code match_finished} SSE event emitted on the
 * per-match duel channel.
 *
 * <p>Fired exactly once per match (Property 8) by every adjudication path
 * — first-AC win, draw timer, reconnect-grace expiry, forfeit, admin
 * cancel — after the conditional UPDATE on {@code duel_matches} reports a
 * row count of 1. Racing paths that observe row count 0 re-read the row
 * and emit using the persisted state; they do not write anything.
 *
 * <p>{@code winnerUserId} and {@code winnerUsername} are {@code null} for
 * outcomes {@code DRAW} and {@code ABANDONED} (when produced by the
 * dual-disconnect / admin-cancel paths). For forfeits and AC wins both
 * fields are populated.
 *
 * @param matchId        duel match identifier
 * @param outcome        one of {@code USER_A_WIN}, {@code USER_B_WIN},
 *                       {@code DRAW}, {@code ABANDONED}
 * @param winnerUserId   the winner's user id, or {@code null} for draws /
 *                       abandonments without a designated winner
 * @param winnerUsername the winner's username, looked up at emit time
 * @param endedAt        timestamp the row was finalized; matches the
 *                       persisted {@code ended_at} column
 */
public record MatchFinishedEvent(
        UUID matchId,
        String outcome,
        Long winnerUserId,
        String winnerUsername,
        LocalDateTime endedAt
) {}
