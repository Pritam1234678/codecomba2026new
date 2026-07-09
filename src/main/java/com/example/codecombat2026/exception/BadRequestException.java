package com.example.codecombat2026.exception;

/**
 * Exception thrown when a request is invalid or malformed.
 * Results in HTTP 400 Bad Request response.
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
