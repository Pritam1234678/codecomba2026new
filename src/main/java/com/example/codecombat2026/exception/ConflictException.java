package com.example.codecombat2026.exception;

/**
 * Exception thrown when a request conflicts with the current state.
 * Results in HTTP 409 Conflict response.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
