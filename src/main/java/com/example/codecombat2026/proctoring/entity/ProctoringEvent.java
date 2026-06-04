package com.example.codecombat2026.proctoring.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Per-event log row written for every proctoring frame.
 *
 * <p>Maps to {@code proctoring_events} (V7). {@code payload_json} is a
 * Postgres {@code jsonb} column, mapped here as a {@link String} field —
 * {@link JdbcTypeCode} {@link SqlTypes#JSON} tells Hibernate 6 to bind the
 * value with the {@code jsonb} JDBC type rather than {@code varchar} so
 * INSERTs round-trip cleanly. Services parse the JSON with Jackson on read;
 * we never store domain DTOs as embedded Hibernate types because event
 * shapes vary per {@code event_type} and we want the schema to stay loose.
 *
 * <p>The {@code replayed} flag distinguishes real-time frames from frames
 * that were buffered offline in IndexedDB and replayed after reconnect
 * (Req 11.3). The {@code score_delta} column is the materialised weight
 * applied when the event was scored — kept on the row so deterministic
 * rescoring (Req 12.7) does not have to reapply the live weight table.
 */
@Entity
@Table(name = "proctoring_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProctoringEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "event_type", nullable = false, length = 32)
    private String eventType;

    @Column(name = "client_timestamp", nullable = false)
    private LocalDateTime clientTimestamp;

    @Column(name = "server_timestamp", nullable = false)
    private LocalDateTime serverTimestamp;

    /**
     * Raw JSON payload. Stored as Postgres {@code jsonb}; held as a
     * {@link String} on the Java side and parsed by Jackson on read.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", columnDefinition = "jsonb")
    private String payloadJson;

    @Column(name = "replayed", nullable = false)
    private Boolean replayed;

    @Column(name = "score_delta", nullable = false)
    private Integer scoreDelta;

    /**
     * Browser-generated UUID stamped at original capture time and round-tripped
     * through the offline buffer (Req 11.2, 11.3). Used by the server-side
     * replay dedup path: when a {@code replayed=true} frame arrives carrying a
     * non-null {@code client_correlation_id}, the ingest path looks up an
     * existing row by {@code (session_id, client_correlation_id)} and, on hit,
     * returns that row's id without re-inserting and without re-applying the
     * score delta. Nullable for backward compatibility with synthetic
     * server-side events (e.g. {@code HEARTBEAT_TIMEOUT}) that are not part of
     * the replay flow. The matching DB column has a partial unique index
     * (V8 migration) keyed on the same pair so any client misbehaviour
     * surfaces as a constraint violation rather than silent duplicates.
     */
    @Column(name = "client_correlation_id", length = 64)
    private String clientCorrelationId;
}
