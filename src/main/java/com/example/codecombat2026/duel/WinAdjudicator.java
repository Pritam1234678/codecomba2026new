package com.example.codecombat2026.duel;

import java.time.LocalDateTime;

/**
 * Pure helper that decides which of the two duel participants wins when both
 * have produced an AC verdict. Centralizing the rule here keeps it free of
 * Spring / DB / Valkey concerns so it can be reasoned about in isolation and
 * exercised with property-based tests.
 *
 * <p>Tiebreak rule (Requirement 6.3):
 * <ol>
 *   <li>The participant whose AC submission has the earlier {@code submitted_at}
 *       wins.</li>
 *   <li>If the two timestamps are exactly equal, the participant with the
 *       smaller {@code submissionId} wins. Submission ids are PostgreSQL
 *       primary keys so this comparison is deterministic and total.</li>
 * </ol>
 *
 * <p>Property 8 (First-AC-wins is uniquely decided): for any pair of AC
 * submissions in any temporal interleaving, this function picks exactly one
 * winner, and that winner has the smaller {@code (submitted_at, submissionId)}
 * pair under lexicographic ordering. The function is total, deterministic and
 * has no side effects.
 *
 * <p>This class is intentionally final with a private constructor — it is a
 * utility holder of static methods, not an instantiable type.
 */
public final class WinAdjudicator {

    private WinAdjudicator() {
        // Utility class — no instances.
    }

    /**
     * Decides which user wins given both participants' AC verdict metadata.
     *
     * <p>Implements the Requirement 6.3 tiebreak: the earlier
     * {@code submitted_at} wins, and on an exact timestamp tie the lower
     * {@code submissionId} wins.
     *
     * @param tA              user A's AC {@code submitted_at} (must not be null)
     * @param submissionIdA   user A's submission id (PK from {@code submissions})
     * @param tB              user B's AC {@code submitted_at} (must not be null)
     * @param submissionIdB   user B's submission id (PK from {@code submissions})
     * @param userA           user A's user id
     * @param userB           user B's user id (must differ from {@code userA})
     * @return                the user id of the winning participant
     * @throws IllegalArgumentException if either timestamp is {@code null},
     *                                  if {@code submissionIdA == submissionIdB}
     *                                  (a primary-key collision that should
     *                                  never occur), or if
     *                                  {@code userA == userB}
     */
    public static long decide(LocalDateTime tA,
                              long submissionIdA,
                              LocalDateTime tB,
                              long submissionIdB,
                              long userA,
                              long userB) {
        if (tA == null) {
            throw new IllegalArgumentException("tA must not be null");
        }
        if (tB == null) {
            throw new IllegalArgumentException("tB must not be null");
        }
        if (submissionIdA == submissionIdB) {
            throw new IllegalArgumentException(
                "submissionIdA and submissionIdB must differ; got " + submissionIdA);
        }
        if (userA == userB) {
            throw new IllegalArgumentException(
                "userA and userB must differ; got " + userA);
        }

        int cmp = tA.compareTo(tB);
        if (cmp < 0) {
            return userA;
        }
        if (cmp > 0) {
            return userB;
        }
        // Exact timestamp tie — fall back to the deterministic submissionId tiebreaker.
        return submissionIdA < submissionIdB ? userA : userB;
    }
}
