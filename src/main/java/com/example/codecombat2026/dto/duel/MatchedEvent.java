package com.example.codecombat2026.dto.duel;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payload of the {@code matched} SSE event emitted on the per-user
 * channel ({@code /api/submissions/stream}) immediately after the
 * pair-loop hands the user pair to
 * {@code DuelService.pairAndStart}.
 *
 * <p>The frontend's {@code useDuelMatchmaking} hook listens for this
 * event and navigates to {@code /duel/{matchId}} on receipt, completing
 * the lobby → arena handoff (Requirements 1.6, 4.1, 13.2).
 * {@code opponentUsername} is the other participant's username — looked
 * up at emit time on the server so the browser does not need an extra
 * round-trip.
 *
 * @param matchId          freshly-assigned duel match identifier
 * @param opponentUsername the other participant's username
 * @param problemId        the duel problem; same value is sent to both
 *                         participants
 * @param startedAt        match start timestamp
 *                         ({@code DuelMatch.startedAt})
 */
public record MatchedEvent(
        UUID matchId,
        String opponentUsername,
        Long problemId,
        LocalDateTime startedAt
) {}
