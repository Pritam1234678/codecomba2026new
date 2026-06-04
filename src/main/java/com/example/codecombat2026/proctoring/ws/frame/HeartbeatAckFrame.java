package com.example.codecombat2026.proctoring.ws.frame;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Outbound {@code HEARTBEAT_ACK} frame.
 *
 * <p>Wire shape:
 * <pre>{@code
 * { "type": "HEARTBEAT_ACK", "server_time": "2025-01-15T10:23:45.123Z" }
 * }</pre>
 *
 * <p>Sent in reply to every {@link HeartbeatFrame} the server accepts.
 * {@code server_time} is the JVM wall clock at the moment the ack was
 * built, formatted as {@code Instant.toString()} (UTC ISO-8601). The
 * frontend uses it for clock-skew diagnostics only — it is informational,
 * never load-bearing.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record HeartbeatAckFrame(
        @JsonProperty("type") String type,
        @JsonProperty("server_time") String serverTime) {

    public static HeartbeatAckFrame of(String serverTime) {
        return new HeartbeatAckFrame("HEARTBEAT_ACK", serverTime);
    }
}
