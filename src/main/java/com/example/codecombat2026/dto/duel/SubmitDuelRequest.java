package com.example.codecombat2026.dto.duel;

/**
 * Request body for {@code POST /api/duels/{matchId}/submissions}.
 *
 * <p>Mirrors the existing non-duel submit payload: just the source code
 * and a programming-language tag. The duel context (problem, time limit,
 * memory limit, participating users) is derived server-side from the
 * {@code matchId} path parameter so the client cannot tamper with it.
 *
 * @param code     user's source code; passed verbatim to the existing
 *                 {@code bwrap+prlimit} sandbox
 * @param language one of the values accepted by
 *                 {@code Submission.ProgrammingLanguage}
 *                 (case-insensitive at the controller boundary)
 */
public record SubmitDuelRequest(String code, String language) {}
