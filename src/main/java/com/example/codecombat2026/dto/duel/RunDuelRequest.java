package com.example.codecombat2026.dto.duel;

/**
 * Request body for {@code POST /api/duels/{matchId}/run}.
 *
 * <p>V4 — runs the user's code synchronously against the problem's
 * example test cases only. Counts toward the 5-runs-per-match limit
 * but does not produce a {@code submissions} row.
 */
public record RunDuelRequest(String code, String language, String stdin) {}
