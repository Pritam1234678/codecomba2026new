package com.example.codecombat2026.proctoring.event;

import com.example.codecombat2026.proctoring.entity.RiskBand;
import com.example.codecombat2026.proctoring.ws.ProctoringSessionRegistry;
import com.example.codecombat2026.proctoring.ws.frame.RiskUpdateFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Bridges the in-process {@link ProctoringRiskBandChangedEvent} onto the
 * candidate-side WebSocket as a {@code RISK_UPDATE} frame (Req 10.1,
 * 12.5, 12.6).
 *
 * <p>The risk-engine path inside {@code ProctoringEventService.ingest}
 * publishes a {@link ProctoringRiskBandChangedEvent} the moment a
 * sliding-score INCRBY crosses a band boundary. Two consumers fan out
 * from that single ApplicationEvent:
 *
 * <ul>
 *   <li>{@link ProctoringSseListener} — republishes onto the per-contest
 *       admin SSE channel as {@code RISK_BAND_CHANGED}.</li>
 *   <li>This class — pushes a {@link RiskUpdateFrame} to the candidate's
 *       bound WebSocket (if any) so the on-screen risk pill updates
 *       in real time.</li>
 * </ul>
 *
 * <p>Best-effort delivery: the candidate may have no WS bound (just
 * reconnecting, on a flaky link, or the session was force-ended in the
 * same tick). {@link ProctoringSessionRegistry#send(Long, Object)}
 * already short-circuits on a missing or closed session and swallows
 * IO errors at WARN, so the risk-engine commit is never coupled to
 * socket health (Req 18.2 — Valkey/WS hiccups must not break the
 * scoring path).
 *
 * <p>Decoupled from {@link ProctoringSessionRegistry} on purpose:
 * the registry stays generic ("send any frame") and the band→frame
 * mapping lives next to the event it consumes, matching the layout
 * of {@code ProctoringSseListener}.
 */
@Component
public class ProctoringRiskUpdateBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(ProctoringRiskUpdateBroadcaster.class);

    private final ProctoringSessionRegistry registry;

    public ProctoringRiskUpdateBroadcaster(ProctoringSessionRegistry registry) {
        this.registry = registry;
    }

    /**
     * Build a {@link RiskUpdateFrame} and push it to the candidate's
     * bound WS. No-op when the session is not bound or the socket has
     * already closed — both are normal flows (reconnect window,
     * force-end race) that the candidate frontend reconciles on
     * reconnect via the initial RISK_UPDATE the WS handler sends after
     * handshake (task 4.4).
     */
    @EventListener
    public void onRiskBandChanged(ProctoringRiskBandChangedEvent event) {
        try {
            RiskBand band = event.newBand();
            RiskUpdateFrame frame = RiskUpdateFrame.of(
                    event.newScore(),
                    band != null ? band.name() : null);
            registry.send(event.sessionId(), frame);
        } catch (RuntimeException e) {
            // Defensive: registry.send is already swallow-on-IO, but
            // any pre-send mishap (null event field, etc.) must not
            // propagate back into the publishing transaction.
            log.warn("Failed to push RISK_UPDATE to candidate WS for session {} (contest {}): {}",
                    event.sessionId(), event.contestId(), e.getMessage());
        }
    }
}
