package com.example.codecombat2026.proctoring.exception;

/**
 * Thrown when an authenticated candidate cannot proceed with a proctoring
 * action because of policy rather than missing data — e.g. the candidate is
 * locked out of the contest after self-finishing/quitting (Req 13.9), is
 * missing a current consent ack, or is acting on a session that does not
 * belong to them.
 *
 * <p>The single-argument constructor takes a stable machine-readable
 * {@code code} (e.g. {@code "LOCKED_OUT"}, {@code "CONSENT_MISSING"}). When
 * only a code is supplied, the handler echoes it as both the {@code error}
 * field and the {@code message} so the wire shape is stable. The two-argument
 * constructor lets callers attach a human-readable message while keeping the
 * code stable for client-side branching.
 *
 * <p>Mapped to HTTP 403 by
 * {@link com.example.codecombat2026.exception.GlobalExceptionHandler}.
 */
public class ProctoringForbiddenException extends RuntimeException {

    private final String code;

    public ProctoringForbiddenException(String code) {
        super(code);
        this.code = code;
    }

    public ProctoringForbiddenException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
