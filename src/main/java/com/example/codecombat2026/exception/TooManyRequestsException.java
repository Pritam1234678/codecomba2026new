package com.example.codecombat2026.exception;

/**
 * Exception thrown when rate limits or quotas are exceeded.
 * Results in HTTP 429 Too Many Requests response.
 * Optionally includes retry-after information for Retry-After header.
 */
public class TooManyRequestsException extends RuntimeException {
    
    private Long retryAfterSeconds;
    
    public TooManyRequestsException(String message) {
        super(message);
    }
    
    public TooManyRequestsException(String message, Long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }
    
    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
