package com.example.codecombat2026.dto.duel;

import java.time.Instant;

/**
 * Payload of a {@code pairing_failed} SSE event emitted on the per-user
 * channel ({@code /api/submissions/stream}) when the pair-loop attempted
 * to start a match but the underlying call to
 * {@code DuelService.pairAndStart} failed.
 *
 * <p>{@code reason} is one of the canonical strings the frontend
 * recognizes:
 * <ul>
 *   <li>{@code concurrent_match}     — the partial-unique index rejected
 *       the INSERT because the user is already in another active match
 *       (Requirements 8.4, 10.2).</li>
 *   <li>{@code no_eligible_problem}  — neither the both-solved exclusion
 *       nor the full pool produced a candidate (Requirement 3.5).</li>
 *   <li>{@code internal}             — fallback for any other unexpected
 *       failure; the user is invited to retry.</li>
 * </ul>
 *
 * @param reason short tag the frontend maps to a user-friendly message
 * @param ts     server-side instant the failure was emitted
 */
public record PairingFailedEvent(String reason, Instant ts) {}
