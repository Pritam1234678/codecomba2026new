package com.example.codecombat2026.dto.duel;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payload of the initial {@code room_state} SSE event emitted on the
 * per-match duel channel immediately after a subscriber connects to
 * {@code /api/duels/{matchId}/stream}.
 *
 * <p>{@code room_state} is the snapshot a freshly-arriving (or
 * reconnecting) browser uses to hydrate {@code DuelArena.jsx} without
 * having to issue a separate {@code GET /api/duels/{matchId}}. It carries
 * both participants' identities and live presence flags (Requirement 4.1)
 * plus the match-budget countdown — {@code remainingSeconds} is computed
 * server-side as {@code DUEL_DRAW_TIMEOUT_SEC - (now - startedAt)} so
 * client clock drift does not matter.
 *
 * @param matchId          duel match identifier
 * @param userA            seat-A participant info
 * @param userB            seat-B participant info
 * @param problemId        the duel problem (same for both seats)
 * @param status           {@code WAITING} / {@code IN_PROGRESS} / {@code FINISHED}
 * @param startedAt        match start time, {@code null} until pairing finalizes
 * @param remainingSeconds seconds remaining in the match window, {@code null}
 *                         once the match is {@code FINISHED}
 */
public record RoomStateEvent(
        UUID matchId,
        ParticipantInfo userA,
        ParticipantInfo userB,
        Long problemId,
        String status,
        LocalDateTime startedAt,
        Long remainingSeconds
) {

    /**
     * Per-seat presence snapshot embedded in {@link RoomStateEvent}.
     *
     * <p>{@code connected} reflects the live state of the
     * {@code DuelSseEmitterRegistry} at emit time: {@code true} iff at
     * least one SSE subscription for {@code (matchId, userId)} is open.
     */
    public record ParticipantInfo(Long id, String username, boolean connected) {}
}
