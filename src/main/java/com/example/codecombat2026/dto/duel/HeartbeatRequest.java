package com.example.codecombat2026.dto.duel;

/**
 * Request body for {@code POST /api/duels/{matchId}/heartbeat}.
 *
 * <p>Sent by {@code DuelArena.jsx} debounced to one POST per 1500 ms
 * while the editor has focus and the cursor moved within that window.
 * The server rate-limits emissions further (Property 16) and only fans
 * the resulting {@code progress {event:'typing'}} event out to the
 * opponent — never to the sender.
 *
 * @param kind discriminator for future heartbeat variants. Currently
 *             only {@code "typing"} is meaningful; unknown values are
 *             accepted but produce no SSE emission.
 */
public record HeartbeatRequest(String kind) {}
