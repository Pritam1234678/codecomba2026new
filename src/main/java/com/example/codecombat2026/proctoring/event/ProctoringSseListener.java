package com.example.codecombat2026.proctoring.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bridges the in-process proctoring lifecycle and risk-band events
 * onto the per-contest admin SSE channel served by
 * {@link com.example.codecombat2026.proctoring.controller.ProctoringAdminStreamController}.
 *
 * <p>Three event types are republished onto the SSE bus; the
 * {@code event} name on the SSE frame matches the corresponding
 * Requirement-12/15 vocabulary so the admin dashboard can route
 * incoming messages directly without re-discriminating by payload
 * shape:
 *
 * <ul>
 *   <li>{@link ProctoringRiskBandChangedEvent} — Req 12.5,
 *       emitted by {@code ProctoringEventService.ingest} when the
 *       sliding score crosses a band threshold. Frame name
 *       {@code RISK_BAND_CHANGED}.</li>
 *   <li>{@link ProctoringSessionStartedEvent} — Req 15.1, fired by
 *       {@code ProctoringSessionService.createSession}. Frame name
 *       {@code SESSION_STARTED}.</li>
 *   <li>{@link ProctoringSessionEndedEvent} — Req 15.2, fired by
 *       {@code ProctoringSessionService.closeAndProject} when the
 *       conditional UPDATE actually closed the row. Frame name
 *       {@code SESSION_ENDED}.</li>
 * </ul>
 *
 * <p>Listener exceptions are caught and logged at WARN — a broken
 * admin stream must never destabilise the candidate-side lifecycle.
 */
@Component
public class ProctoringSseListener {

    private static final Logger log = LoggerFactory.getLogger(ProctoringSseListener.class);

    private final ProctoringAdminSseRegistry registry;

    public ProctoringSseListener(ProctoringAdminSseRegistry registry) {
        this.registry = registry;
    }

    /**
     * Republish a band-change onto the admin SSE bus (Req 12.5, 15.2).
     * The payload is intentionally compact — the admin UI uses it as a
     * trigger to refetch / re-render the affected row, not as the
     * authoritative read model.
     */
    @EventListener
    public void onRiskBandChanged(ProctoringRiskBandChangedEvent event) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "RISK_BAND_CHANGED");
            payload.put("sessionId", event.sessionId());
            payload.put("contestId", event.contestId());
            payload.put("riskScore", event.newScore());
            payload.put("oldBand", event.oldBand() != null ? event.oldBand().name() : null);
            payload.put("riskBand", event.newBand() != null ? event.newBand().name() : null);
            payload.put("ts", Instant.now().toString());
            registry.emit(event.contestId(), "RISK_BAND_CHANGED", payload);
        } catch (RuntimeException e) {
            log.warn("Failed to fan RISK_BAND_CHANGED for session {} (contest {}): {}",
                    event.sessionId(), event.contestId(), e.getMessage());
        }
    }

    /**
     * Republish a session start onto the admin SSE bus (Req 15.1).
     * Newly arrived candidates appear in the live grid without the
     * dashboard polling.
     */
    @EventListener
    public void onSessionStarted(ProctoringSessionStartedEvent event) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "SESSION_STARTED");
            payload.put("sessionId", event.sessionId());
            payload.put("contestId", event.contestId());
            payload.put("userId", event.userId());
            payload.put("ts", Instant.now().toString());
            registry.emit(event.contestId(), "SESSION_STARTED", payload);
        } catch (RuntimeException e) {
            log.warn("Failed to fan SESSION_STARTED for session {} (contest {}): {}",
                    event.sessionId(), event.contestId(), e.getMessage());
        }
    }

    /**
     * Republish a session close onto the admin SSE bus (Req 15.2).
     * The conditional-UPDATE close in {@code ProctoringSessionService}
     * is single-writer, so this fires exactly once per session even
     * on concurrent force-end / heartbeat / contest-end races.
     */
    @EventListener
    public void onSessionEnded(ProctoringSessionEndedEvent event) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "SESSION_ENDED");
            payload.put("sessionId", event.sessionId());
            payload.put("contestId", event.contestId());
            payload.put("endReason", event.endReason() != null ? event.endReason().name() : null);
            payload.put("ts", Instant.now().toString());
            registry.emit(event.contestId(), "SESSION_ENDED", payload);
        } catch (RuntimeException e) {
            log.warn("Failed to fan SESSION_ENDED for session {} (contest {}): {}",
                    event.sessionId(), event.contestId(), e.getMessage());
        }
    }
}
