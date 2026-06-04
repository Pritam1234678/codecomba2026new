package com.example.codecombat2026.proctoring.exception;

/**
 * Thrown when a proctoring request collides with the current lifecycle state
 * of a contest, session, or screenshot upload — e.g. enabling proctoring on a
 * non-{@code UPCOMING} contest, creating a session while an active one already
 * exists, finishing a session that has already ended.
 *
 * <p>Carries a stable machine-readable {@code code} (used as the {@code error}
 * field of the 409 JSON body) and an optional {@code payload}. When the
 * payload is a {@code Map}, the global exception handler merges its entries
 * into the response body so callers receive the extra structured detail
 * (such as {@code endReason} for {@code LOCKED_OUT} or {@code sessionId} for
 * {@code ALREADY_ACTIVE}) needed to recover client-side.
 *
 * <p>Mapped to HTTP 409 by
 * {@link com.example.codecombat2026.exception.GlobalExceptionHandler}.
 */
public class ProctoringStateConflictException extends RuntimeException {

    private final String code;
    private final Object payload;

    public ProctoringStateConflictException(String code) {
        super(code);
        this.code = code;
        this.payload = null;
    }

    public ProctoringStateConflictException(String code, String message) {
        super(message);
        this.code = code;
        this.payload = null;
    }

    public ProctoringStateConflictException(String code, String message, Object payload) {
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
