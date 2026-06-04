package com.example.codecombat2026.proctoring.ws.frame;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Outbound {@code RATE_LIMIT_EXCEEDED} frame (Req 17.3).
 *
 * <p>Wire shape:
 * <pre>{@code
 * { "type": "RATE_LIMIT_EXCEEDED", "window_seconds": 10 }
 * }</pre>
 *
 * <p>Emitted at most once per 10-second window when the candidate exceeds
 * the per-session inbound event rate. Wiring lives in task 13.2.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RateLimitExceededFrame(
        @JsonProperty("type") String type,
        @JsonProperty("window_seconds") int windowSeconds) {

    public static RateLimitExceededFrame of(int windowSeconds) {
        return new RateLimitExceededFrame("RATE_LIMIT_EXCEEDED", windowSeconds);
    }
}
