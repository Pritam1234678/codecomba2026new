package com.example.codecombat2026.proctoring.exception;

/**
 * Thrown when a proctoring resource cannot be located (proctored-contest row,
 * session, screenshot, etc.).
 *
 * <p>The exception message is surfaced as the {@code message} field of the JSON
 * error body produced by {@link com.example.codecombat2026.exception.GlobalExceptionHandler};
 * a stable {@code error} code of {@code "NOT_FOUND"} is added by the handler so
 * frontend callers always see a predictable shape.
 *
 * <p>Mapped to HTTP 404 by
 * {@link com.example.codecombat2026.exception.GlobalExceptionHandler}.
 */
public class ProctoringNotFoundException extends RuntimeException {

    public ProctoringNotFoundException(String message) {
        super(message);
    }

    public ProctoringNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
