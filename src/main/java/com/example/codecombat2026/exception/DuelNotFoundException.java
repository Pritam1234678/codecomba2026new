package com.example.codecombat2026.exception;

/**
 * Thrown when a duel match cannot be located by id (or has been pruned).
 * Mapped to HTTP 404 by {@link GlobalExceptionHandler}.
 */
public class DuelNotFoundException extends RuntimeException {

    public DuelNotFoundException(String message) {
        super(message);
    }

    public DuelNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
