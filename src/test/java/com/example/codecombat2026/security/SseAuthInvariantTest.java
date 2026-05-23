package com.example.codecombat2026.security;

import com.example.codecombat2026.controller.SubmissionController;
import com.example.codecombat2026.exception.GlobalExceptionHandler;
import com.example.codecombat2026.security.jwt.AuthEntryPointJwt;
import com.example.codecombat2026.security.jwt.AuthTokenFilter;
import com.example.codecombat2026.security.jwt.JwtUtils;
import com.example.codecombat2026.security.services.UserDetailsServiceImpl;
import com.example.codecombat2026.service.RateLimiterService;
import com.example.codecombat2026.service.SseEmitterRegistry;
import com.example.codecombat2026.service.SseTicketService;
import com.example.codecombat2026.service.SubmissionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Locks in the SSE auth invariant after the move to single-use tickets:
 *
 *   - GET /api/submissions/stream with no ticket → 401
 *   - GET /api/submissions/stream with an invalid ticket → 401
 *   - GET /api/submissions/stream with a blank ticket → 401
 *
 * If a future change accidentally re-permits unauthenticated SSE this test
 * will fail. The previous JWT-in-query implementation was prone to this
 * class of regression after async-dispatch refactors.
 */
@WebMvcTest(controllers = SubmissionController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
    "codecombat.jwt.secret=dGhpcy1pcy1hLXRlc3Qtc2VjcmV0LXBhZGRlZC10by0zMi1ieXRlcw==",
    "codecombat.jwt.expiration=86400000",
    "APP_ALLOWED_ORIGINS=http://localhost:5173"
})
class SseAuthInvariantTest {

    @Autowired private MockMvc mvc;

    @MockBean private SubmissionService submissionService;
    @MockBean private SseEmitterRegistry sseRegistry;
    @MockBean private RateLimiterService rateLimiter;
    @MockBean private SseTicketService sseTickets;

    @MockBean private UserDetailsServiceImpl userDetailsService;
    @MockBean private AuthEntryPointJwt entryPoint;
    @MockBean private AuthTokenFilter authTokenFilter;
    @MockBean private JwtUtils jwtUtils;

    @BeforeEach
    void passThroughAuthFilter() throws Exception {
        // Mocked filter has no behaviour by default. Make it transparent so
        // requests reach the controller and our manual ticket auth runs.
        doAnswer((InvocationOnMock inv) -> {
            ServletRequest req = inv.getArgument(0);
            ServletResponse res = inv.getArgument(1);
            FilterChain chain = inv.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(authTokenFilter).doFilter(any(), any(), any());
    }

    @Test
    void streamWithoutTicketReturns401() throws Exception {
        when(sseTickets.consume(null)).thenReturn(null);
        MvcResult r = mvc.perform(get("/api/submissions/stream")
            .accept(org.springframework.http.MediaType.TEXT_EVENT_STREAM)).andReturn();
        assertEquals(401, r.getResponse().getStatus());
    }

    @Test
    void streamWithInvalidTicketReturns401() throws Exception {
        when(sseTickets.consume("not-a-real-ticket")).thenReturn(null);
        MvcResult r = mvc.perform(get("/api/submissions/stream")
            .param("ticket", "not-a-real-ticket")
            .accept(org.springframework.http.MediaType.TEXT_EVENT_STREAM)).andReturn();
        assertEquals(401, r.getResponse().getStatus());
    }

    @Test
    void streamWithBlankTicketReturns401() throws Exception {
        when(sseTickets.consume("")).thenReturn(null);
        MvcResult r = mvc.perform(get("/api/submissions/stream")
            .param("ticket", "")
            .accept(org.springframework.http.MediaType.TEXT_EVENT_STREAM)).andReturn();
        assertEquals(401, r.getResponse().getStatus());
    }
}
