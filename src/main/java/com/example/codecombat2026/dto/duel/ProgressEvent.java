package com.example.codecombat2026.dto.duel;

import java.time.Instant;

/**
 * Payload of a {@code progress} SSE event emitted on the per-match duel
 * channel ({@code /api/duels/{matchId}/stream}).
 *
 * <p>The {@code progress} event is a tagged union over several sub-events
 * — {@code typing}, {@code submitted}, {@code verdict}, {@code disconnected},
 * {@code reconnected} — distinguished by the {@code event} discriminator
 * field. Different sub-events fill in different subsets of the optional
 * fields below; every field except {@code event} and {@code ts} may be
 * {@code null}.
 *
 * <p>Sub-event field map (per design SSE schema):
 * <ul>
 *   <li>{@code typing}        — {@code event}, {@code userId}, {@code ts}</li>
 *   <li>{@code submitted}     — {@code event}, {@code userId},
 *                               {@code submissionId}, {@code ts}</li>
 *   <li>{@code verdict}       — {@code event}, {@code userId},
 *                               {@code submissionId}, {@code status},
 *                               {@code testCasesPassed},
 *                               {@code totalTestCases}, {@code ts}</li>
 *   <li>{@code disconnected}  — {@code event}, {@code userId},
 *                               {@code gracePeriodSec}, {@code ts}</li>
 *   <li>{@code reconnected}   — {@code event}, {@code userId}, {@code ts}</li>
 * </ul>
 *
 * <p>Validates Requirements 4.2, 4.3, 4.4, 4.5, 5.6.
 */
public record ProgressEvent(
        String event,
        Long userId,
        Long submissionId,
        String status,
        Integer testCasesPassed,
        Integer totalTestCases,
        Integer gracePeriodSec,
        Instant ts
) {}
