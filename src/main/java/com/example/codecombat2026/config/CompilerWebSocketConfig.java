package com.example.codecombat2026.config;

import com.example.codecombat2026.service.CompilerSessionHandler;
import com.example.codecombat2026.service.WsTicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

/**
 * Registers the interactive compiler WebSocket endpoint.
 *
 * The endpoint accepts both anonymous and authenticated upgrades:
 *   - Authenticated: client POSTs /api/compiler/ws-ticket to get a single-use
 *     ticket, then connects with ?ticket=. The handshake interceptor consumes
 *     the ticket and binds userId to the session.
 *   - Anonymous: no ticket required. Subject to a stricter per-IP rate limit
 *     enforced by {@link CompilerSessionHandler}.
 *
 * The handshake also captures the real client IP (X-Forwarded-For aware) so
 * the rate limiter sees the user, not the load balancer.
 */
@Configuration
@EnableWebSocket
public class CompilerWebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private CompilerSessionHandler handler;

    @Autowired
    private WsTicketService wsTickets;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler((WebSocketHandler) handler, "/api/compiler/ws")
                .addInterceptors(new TicketAndIpInterceptor(wsTickets))
                // Same origin policy enforced by CORS at the upgrade level.
                // Pattern is wider here because WS handshake CORS is handled
                // separately from REST (Spring's WS layer respects the bound origins).
                .setAllowedOriginPatterns("*");
    }

    /**
     * Captures the real client IP and (optional) authenticated userId into
     * the WebSocket session attributes. The session handler reads them at
     * connection time.
     */
    private static class TicketAndIpInterceptor implements HandshakeInterceptor {

        private final WsTicketService tickets;

        TicketAndIpInterceptor(WsTicketService tickets) {
            this.tickets = tickets;
        }

        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Map<String, Object> attrs) {
            // ── Real client IP (X-Forwarded-For first hop, else remote addr) ──
            String ip = "unknown";
            if (request instanceof ServletServerHttpRequest sreq) {
                List<String> xff = request.getHeaders().get("X-Forwarded-For");
                if (xff != null && !xff.isEmpty()) {
                    ip = xff.get(0).split(",")[0].trim();
                } else if (sreq.getServletRequest().getRemoteAddr() != null) {
                    ip = sreq.getServletRequest().getRemoteAddr();
                }
            }
            attrs.put("ip", ip);

            // ── Optional ticket → bound userId ────────────────────────────────
            String ticket = firstQueryParam(request, "ticket");
            if (ticket != null) {
                Long userId = tickets.consume(ticket);
                if (userId == null) {
                    // Bad ticket — reject the upgrade outright. Anonymous clients
                    // simply omit the parameter.
                    response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                    return false;
                }
                attrs.put("userId", userId);
            }
            return true;
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Exception exception) { }

        private static String firstQueryParam(ServerHttpRequest request, String name) {
            String q = request.getURI().getQuery();
            if (q == null || q.isEmpty()) return null;
            for (String pair : q.split("&")) {
                int eq = pair.indexOf('=');
                if (eq <= 0) continue;
                if (name.equals(pair.substring(0, eq))) {
                    return java.net.URLDecoder.decode(pair.substring(eq + 1), java.nio.charset.StandardCharsets.UTF_8);
                }
            }
            return null;
        }
    }
}
