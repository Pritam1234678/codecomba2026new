package com.example.codecombat2026.proctoring.ws.frame;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Inbound {@code HEARTBEAT} frame from the candidate WebSocket.
 *
 * <p>Wire shape:
 * <pre>{@code
 * { "type": "HEARTBEAT" }
 * }</pre>
 *
 * <p>The frame carries no payload — the act of receiving it resets the
 * server-side heartbeat-timeout task (Req 9.7, 9.8). The handler replies
 * with a matching {@link HeartbeatAckFrame} carrying the server clock.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record HeartbeatFrame(@JsonProperty("type") String type) {
}
