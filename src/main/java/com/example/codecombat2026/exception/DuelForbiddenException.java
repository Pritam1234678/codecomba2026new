package com.example.codecombat2026.exception;

/**
 * Thrown when an authenticated user tries to act on a duel match they are not a
 * participant of (e.g. subscribing to the SSE stream, submitting code, forfeiting,
 * or sending a heartbeat for a match that is not theirs).
 * Mapped to HTTP 403 by {@link GlobalExceptionHandler}.
 */
public class DuelForbiddenException extends RuntimeException {

    public DuelForbiddenException(String message) {
        super(message);
    }

    public DuelForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}
