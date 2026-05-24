package com.example.codecombat2026.exception;

/**
 * Thrown when a duel-related request collides with the match's current state
 * (e.g. trying to enqueue while already in an active match, submitting to a
 * FINISHED match, forfeiting a match that has already ended, concurrent match
 * creation losing the partial-unique-index race).
 *
 * Carries a stable machine-readable {@code code} (used as the {@code error}
 * field in the 409 JSON body) and an optional {@code payload} that the global
 * exception handler merges into the response body so callers receive the
 * extra context (such as the existing {@code matchId} or {@code outcome})
 * needed to recover client-side.
 *
 * Mapped to HTTP 409 by {@link GlobalExceptionHandler}.
 */
public class DuelStateConflictException extends RuntimeException {

    private final String code;
    private final Object payload;

    public DuelStateConflictException(String code, Object payload) {
        super(code);
        this.code = code;
        this.payload = payload;
    }

    public DuelStateConflictException(String code, Object payload, String message) {
        super(message);
        this.code = code;
        this.payload = payload;
    }

    public String getCode() {
        return code;
    }

    public Object getPayload() {
        return payload;
    }
}
