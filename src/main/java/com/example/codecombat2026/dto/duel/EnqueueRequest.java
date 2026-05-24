package com.example.codecombat2026.dto.duel;

/**
 * Request body for {@code POST /api/duels/queue}.
 *
 * <p>V4 — adds the {@code difficulty} bucket the user picked
 * (EASY / MEDIUM / HARD). The frontend lobby disables the Find Match
 * button until one is chosen.
 */
public record EnqueueRequest(String difficulty) {}
