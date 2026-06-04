package com.example.codecombat2026.proctoring.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when the request payload itself is malformed for the proctoring
 * pipeline — wrong MIME type, magic-byte mismatch, oversized body, or a
 * cross-row FK that does not line up — but the underlying contest/session
 * state is fine. Distinct from {@link ProctoringStateConflictException}
 * (409, lifecycle collisions) and {@link ProctoringForbiddenException}
 * (403, policy refusals): this one is the catch-all for "the bytes you
 * uploaded are not valid for this endpoint".
 *
 * <p>Carries a stable machine-readable {@code code} (e.g.
 * {@code UNSUPPORTED_MEDIA_TYPE}, {@code PAYLOAD_TOO_LARGE},
 * {@code INVALID_EVENT}) plus the {@link HttpStatus} the caller wants
 * the global handler to write — typically {@code 415}, {@code 413}, or
 * {@code 400}. Keeping the status on the exception itself lets a single
 * handler in {@link com.example.codecombat2026.exception.GlobalExceptionHandler}
 * cover every validation failure with one branch and a stable JSON
 * shape ({@code error} + {@code message}).
 */
public class ProctoringValidationException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    public ProctoringValidationException(String code, HttpStatus httpStatus, String message) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
