package com.example.codecombat2026.proctoring.ws.frame;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Outbound {@code EVENT_ACK} frame.
 *
 * <p>Wire shape:
 * <pre>{@code
 * {
 *   "type": "EVENT_ACK",
 *   "client_correlation_id": "c-7f2a",
 *   "event_id": 42
 * }
 * }</pre>
 *
 * <p>Sent in reply to every accepted {@link EventFrame}. The
 * {@code client_correlation_id} round-trips the value the candidate sent so
 * the frontend's {@code sendEvent(eventType, payload)} helper can resolve
 * the inflight promise. {@code event_id} is the persisted
 * {@code proctoring_events.id} which the screenshot upload path uses as a
 * foreign key (Req 14.1).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventAckFrame(
        @JsonProperty("type") String type,
        @JsonProperty("client_correlation_id") String clientCorrelationId,
        @JsonProperty("event_id") Long eventId) {

    public static EventAckFrame of(String clientCorrelationId, Long eventId) {
        return new EventAckFrame("EVENT_ACK", clientCorrelationId, eventId);
    }
}
