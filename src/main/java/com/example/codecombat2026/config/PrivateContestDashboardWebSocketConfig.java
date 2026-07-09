package com.example.codecombat2026.config;

import com.example.codecombat2026.ws.PrivateContestDashboardWebSocketHandler;
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
 * WebSocket configuration for private contest real-time dashboard.
 * 
 * Registers the WebSocket endpoint at {@code /ws/contests/private/{contestId}/dashboard}
 * for real-time analytics updates to contest hosts.
 * 
 * Security:
 * - Endpoint requires JWT authentication via query parameter
 * - Access validation ensures only the contest host can connect
 * - CORS configured to match REST API allowed origins
 * 
 * Requirements: 32.1, 32.3, 32.4
 */
@Configuration
@EnableWebSocket
public class PrivateContestDashboardWebSocketConfig implements WebSocketConfigurer {

    private static final Logger log = LoggerFactory.getLogger(PrivateContestDashboardWebSocketConfig.class);

    private static final String ENDPOINT_PATH = "/ws/contests/private/*/dashboard";

    private final PrivateContestDashboardWebSocketHandler handler;

    /**
     * Comma-separated list of allowed origins for WebSocket connections.
     * Matches the REST API CORS configuration.
     */
    @Value("${APP_ALLOWED_ORIGINS:http://localhost:5173,http://localhost:3000}")
    private String allowedOrigins;

    public PrivateContestDashboardWebSocketConfig(PrivateContestDashboardWebSocketHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        String[] origins = parseOrigins(allowedOrigins);
        if (origins.length == 0) {
            log.error("APP_ALLOWED_ORIGINS is empty for private contest dashboard WS — no browser will be allowed to connect.");
        } else {
            log.info("Private contest dashboard WS allowed origins: {}", Arrays.toString(origins));
        }

        registry.addHandler((WebSocketHandler) handler, ENDPOINT_PATH)
                .setAllowedOriginPatterns(origins);
    }

    /**
     * Parse comma-separated origins into array, stripping whitespace.
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
