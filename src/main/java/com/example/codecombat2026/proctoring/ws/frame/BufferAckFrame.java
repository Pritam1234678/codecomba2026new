package com.example.codecombat2026.proctoring.ws.frame;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Outbound {@code BUFFER_ACK} frame (Req 11.2, 11.3).
 *
 * <p>Wire shape:
 * <pre>{@code
 * { "type": "BUFFER_ACK", "replayed_count": 23 }
 * }</pre>
 *
 * <p>Sent after a replayed batch is fully ingested so the candidate can purge
 * the corresponding IndexedDB rows and resume live mode.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BufferAckFrame(
        @JsonProperty("type") String type,
        @JsonProperty("replayed_count") int replayedCount) {

    public static BufferAckFrame of(int replayedCount) {
        return new BufferAckFrame("BUFFER_ACK", replayedCount);
    }
}
