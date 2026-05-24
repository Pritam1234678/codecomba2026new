package com.example.codecombat2026.duel;

/**
 * Deterministic seat assignment for the two participants of a Live Duel match.
 *
 * <p>Per Requirement 2.6, the two participants are mapped to fixed seats
 * {@link #A} and {@link #B} ordered by ascending {@code userId}, so that the
 * seat assignment is reproducible from the participant ids alone.
 *
 * @see SeatAssigner
 */
public enum Seat {
    /** The participant with the smaller {@code userId}. */
    A,
    /** The participant with the larger {@code userId}. */
    B
}
