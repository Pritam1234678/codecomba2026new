package com.example.codecombat2026.proctoring.ws.frame;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Outbound {@code SESSION_TERMINATED} frame (Req 10.2, 10.4, 13.6, 13.7, 13.8).
 *
 * <p>Wire shape:
 * <pre>{@code
 * {
 *   "type": "SESSION_TERMINATED",
 *   "reason": "Force-ended by admin",
 *   "end_reason": "ADMIN_FORCED"
 * }
 * }</pre>
 *
 * <p>Pushed immediately before the server closes the WebSocket. The
 * {@code end_reason} is the {@code EndReason.name()} value so the frontend
 * can render the matching terminal screen ({@code SELF_FINISHED} →
 * leaderboard, {@code SELF_QUIT}/{@code ADMIN_FORCED}/
 * {@code HEARTBEAT_TIMEOUT} → lockout).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SessionTerminatedFrame(
        @JsonProperty("type") String type,
        @JsonProperty("reason") String reason,
        @JsonProperty("end_reason") String endReason) {

    public static SessionTerminatedFrame of(String reason, String endReason) {
        return new SessionTerminatedFrame("SESSION_TERMINATED", reason, endReason);
    }
}
