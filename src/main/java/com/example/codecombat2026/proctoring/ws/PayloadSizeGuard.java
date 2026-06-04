package com.example.codecombat2026.proctoring.ws;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import com.example.codecombat2026.proctoring.config.ProctoringConfig;

/**
 * Inbound WebSocket frame size guard.
 *
 * <p>Rejects frames whose UTF-8 byte length exceeds
 * {@code proctoring.maxEventBytes} (default 4096) by closing the
 * connection with application close code 4413 and emitting no
 * per-frame ACK.
 *
 * <p>Stateless and idempotent: callers invoke {@link #check} at the
 * top of {@code handleTextMessage} and bail out early when it returns
 * {@code false}.
 *
 * <p>Validates: Req 17.6.
 */
@Component
public class PayloadSizeGuard {

    /** Application-defined close code for "payload too large" (Req 17.6). */
    public static final int CLOSE_CODE_PAYLOAD_TOO_LARGE = 4413;

    @Autowired
    private ProctoringConfig config;

    /**
     * Returns {@code true} when the payload is within the configured
     * cap; closes the WebSocket session with code 4413 and returns
     * {@code false} when the payload exceeds the cap.
     *
     * <p>{@code null} payloads are treated as zero-length and pass.
     * The {@link IOException} that {@link WebSocketSession#close}
     * may raise is intentionally swallowed: by the time we are
     * closing the session there is nothing useful the caller can do
     * with the failure.
     *
     * @param ws      the WebSocket session
     * @param payload the inbound text frame body
     * @return {@code true} if within cap, {@code false} if rejected
     */
    public boolean check(WebSocketSession ws, String payload) {
        if (payload == null) {
            return true;
        }
        int bytes = payload.getBytes(StandardCharsets.UTF_8).length;
        if (bytes > config.getMaxEventBytes()) {
            try {
                ws.close(new CloseStatus(CLOSE_CODE_PAYLOAD_TOO_LARGE, "payload too large"));
            } catch (IOException ignored) {
                // Session is already on its way out; nothing actionable here.
            }
            return false;
        }
        return true;
    }
}
