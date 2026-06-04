package com.example.codecombat2026.proctoring.ws.frame;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Inbound {@code EVENT} frame from the candidate WebSocket.
 *
 * <p>Wire shape (per {@code design.md}):
 * <pre>{@code
 * {
 *   "type": "EVENT",
 *   "event_type": "TAB_SWITCH",
 *   "client_timestamp": "2025-01-15T10:23:45.123Z",
 *   "payload": { "duration_ms": 4200 },
 *   "replayed": false,
 *   "client_correlation_id": "c-7f2a"
 * }
 * }</pre>
 *
 * <p>{@code clientTimestamp} is kept as a raw string here so the handler can
 * tolerate both {@code ISO_LOCAL_DATE_TIME} ({@code ...123}) and
 * {@code ISO_OFFSET_DATE_TIME} ({@code ...123Z}) shapes the frontend may
 * emit, parsing it into a {@link java.time.LocalDateTime} only when
 * forwarding to the service layer.
 *
 * <p>{@code clientCorrelationId} round-trips through the matching
 * {@link EventAckFrame} so the frontend's {@code sendEvent} helper can
 * resolve the inflight promise.
 *
 * <p>{@code payload} defaults to an empty map when absent (Req 9.5 — every
 * accepted frame produces a forensically faithful row).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventFrame(
        @JsonProperty("type") String type,
        @JsonProperty("event_type") String eventType,
        @JsonProperty("client_timestamp") String clientTimestamp,
        @JsonProperty("payload") Map<String, Object> payload,
        @JsonProperty("replayed") Boolean replayed,
        @JsonProperty("client_correlation_id") String clientCorrelationId) {
}
