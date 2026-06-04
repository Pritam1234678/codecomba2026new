package com.example.codecombat2026.proctoring.ws.frame;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Outbound {@code WARNING} frame for non-terminating admin warnings (Req 10.3).
 *
 * <p>Wire shape:
 * <pre>{@code
 * {
 *   "type": "WARNING",
 *   "admin_id": 12,
 *   "message": "Please remain visible to the camera.",
 *   "acted_at": "2025-01-15T10:23:45.123Z"
 * }
 * }</pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WarningFrame(
        @JsonProperty("type") String type,
        @JsonProperty("admin_id") Long adminId,
        @JsonProperty("message") String message,
        @JsonProperty("acted_at") String actedAt) {

    public static WarningFrame of(Long adminId, String message, String actedAt) {
        return new WarningFrame("WARNING", adminId, message, actedAt);
    }
}
