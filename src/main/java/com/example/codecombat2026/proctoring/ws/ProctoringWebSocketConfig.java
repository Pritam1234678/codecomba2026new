package com.example.codecombat2026.proctoring.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Arrays;
import java.util.List;

/**
 * Registers the proctoring WebSocket endpoint at {@code /api/proctoring/ws}.
 *
 * <p>Mirrors {@link com.example.codecombat2026.config.CompilerWebSocketConfig}
 * but with two material differences:
 * <ul>
 *   <li><b>No anonymous upgrades.</b> The proctoring channel always requires
 *       a valid single-use ticket — the {@link ProctoringHandshakeInterceptor}
 *       rejects with HTTP 401 otherwise (Req 9.2, 9.3, 16.2, 16.3).</li>
 *   <li><b>No wildcard origins.</b> Allowed origins are read from the same
 *       {@code APP_ALLOWED_ORIGINS} env var as
 *       {@link com.example.codecombat2026.security.SecurityConfig}; an empty
 *       list logs an error and effectively rejects all browsers (Req 16.2).
 *       The compiler WS uses {@code "*"} because anonymous compiler runs are
 *       allowed; proctoring is candidate-only and must mirror REST CORS.</li>
 * </ul>
 *
 * <p>The handler ({@link ProctoringWebSocketHandler}) is created in task 4.4
 * and is injected here by Spring at wire-up time. Until that bean exists this
 * configuration will not start the application — that is intentional, since
 * registering the endpoint with no handler would leak a partially-functional
 * upgrade path.
 */
@Configuration
@EnableWebSocket
public class ProctoringWebSocketConfig implements WebSocketConfigurer {

    private static final Logger log = LoggerFactory.getLogger(ProctoringWebSocketConfig.class);

    private static final String ENDPOINT_PATH = "/api/proctoring/ws";

    private final ProctoringWebSocketHandler handler;
    private final ProctoringHandshakeInterceptor handshakeInterceptor;

    /**
     * Comma-separated list of allowed origins for the proctoring upgrade.
     * Same env var as {@code SecurityConfig.allowedOrigins} so REST CORS and
     * WS CORS share a single ops surface (Req 16.2). Default permits localhost
     * dev only; production deployments MUST set this.
     */
    @Value("${APP_ALLOWED_ORIGINS:http://localhost:5173,http://localhost:3000}")
    private String allowedOrigins;

    public ProctoringWebSocketConfig(ProctoringWebSocketHandler handler,
                                     ProctoringHandshakeInterceptor handshakeInterceptor) {
        this.handler = handler;
        this.handshakeInterceptor = handshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        String[] origins = parseOrigins(allowedOrigins);
        if (origins.length == 0) {
            // Match the SecurityConfig pattern: log loudly but still register the
            // endpoint so the rest of the JVM boots. With zero allowed origins the
            // upgrade simply never succeeds — which is the safe failure mode.
            log.error("APP_ALLOWED_ORIGINS is empty for proctoring WS — no browser will be allowed to upgrade.");
        } else {
            log.info("Proctoring WS allowed origins: {}", Arrays.toString(origins));
        }

        registry.addHandler((WebSocketHandler) handler, ENDPOINT_PATH)
                .addInterceptors(handshakeInterceptor)
                .setAllowedOriginPatterns(origins);
    }

    /**
     * Split the comma-separated env value into a clean origin array. Strips
     * whitespace, drops empties. Returns an empty array if the input contains
     * no usable entries.
     */
    private static String[] parseOrigins(String raw) {
        if (raw == null || raw.isBlank()) return new String[0];
        List<String> cleaned = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        return cleaned.toArray(new String[0]);
    }
}
