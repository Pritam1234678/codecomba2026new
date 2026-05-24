package com.example.codecombat2026.duel;

/**
 * Pure helper that maps a duel's two participant {@code userId}s onto the
 * deterministic seats {@link Seat#A} and {@link Seat#B}.
 *
 * <p>Per Requirement 2.6, when a {@code Duel_Match} is created the two
 * participants are assigned to seats ordered by ascending {@code userId}, so
 * that the seat assignment is reproducible from the participant ids alone.
 * This class is the single source of truth for that ordering and is shared by
 * {@code DuelService} (match creation), the win-adjudication path, and any
 * frontend-facing DTO that needs to render "you are seat A" vs "you are seat B".
 *
 * <p>The class is intentionally free of Spring dependencies so it can be
 * exercised in pure JUnit / property-based tests without a Spring context.
 *
 * @see Seat
 */
public final class SeatAssigner {

    private SeatAssigner() {
        // Utility class — not instantiable.
    }

    /**
     * Resolve the {@link Seat} of {@code me} given the two participant ids.
     *
     * @param userA one of the participants (order does not matter — the seat
     *              is determined by {@code min}/{@code max}, not by parameter
     *              position)
     * @param userB the other participant
     * @param me    the user whose seat is being requested; must equal one of
     *              {@code userA} or {@code userB}
     * @return {@link Seat#A} if {@code me == Math.min(userA, userB)},
     *         {@link Seat#B} if {@code me == Math.max(userA, userB)}
     * @throws IllegalArgumentException if {@code userA == userB} (a duel
     *                                  cannot pair a user with themselves) or
     *                                  if {@code me} is neither {@code userA}
     *                                  nor {@code userB}
     */
    public static Seat seatFor(long userA, long userB, long me) {
        if (userA == userB) {
            throw new IllegalArgumentException(
                    "Duel participants must be distinct (userA == userB == " + userA + ")");
        }
        long min = Math.min(userA, userB);
        long max = Math.max(userA, userB);
        if (me == min) {
            return Seat.A;
        }
        if (me == max) {
            return Seat.B;
        }
        throw new IllegalArgumentException(
                "User " + me + " is not a participant of the duel between "
                        + userA + " and " + userB);
    }

    /**
     * Return the two participant ids in canonical ascending order.
     *
     * <p>The ordered pair is used as the key for the
     * {@code duel:create:{minId}_{maxId}} Valkey idempotency lock so that two
     * concurrent pairing attempts for the same pair produce exactly one match.
     *
     * @param u1 one participant id
     * @param u2 the other participant id
     * @return a fresh {@code long[]} of length 2 whose first element is the
     *         smaller id and whose second element is the larger id
     * @throws IllegalArgumentException if {@code u1 == u2}
     */
    public static long[] orderedPair(long u1, long u2) {
        if (u1 == u2) {
            throw new IllegalArgumentException(
                    "Duel participants must be distinct (u1 == u2 == " + u1 + ")");
        }
        return new long[] { Math.min(u1, u2), Math.max(u1, u2) };
    }
}
