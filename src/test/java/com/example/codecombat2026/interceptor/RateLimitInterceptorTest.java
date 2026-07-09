package com.example.codecombat2026.interceptor;

import com.example.codecombat2026.annotation.RateLimited;
import com.example.codecombat2026.exception.TooManyRequestsException;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.example.codecombat2026.service.PrivateContestRateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RateLimitInterceptor.
 * 
 * Tests:
 * - Interceptor activates only on methods with @RateLimited annotation
 * - Correct rate limit service method is called based on annotation type
 * - User ID is extracted from security context
 * - Contest ID is extracted from path variables
 * - Rate limit violations throw TooManyRequestsException
 * - Unauthenticated requests pass through (authentication handled by @PreAuthorize)
 */
@ExtendWith(MockitoExtension.class)
class RateLimitInterceptorTest {

    @Mock
    private PrivateContestRateLimitService rateLimitService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private RateLimitInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new RateLimitInterceptor(rateLimitService);
        SecurityContextHolder.clearContext();
    }

    // ─── Test: Non-annotated methods pass through ─────────────────────────────

    @Test
    void preHandle_withoutAnnotation_returnsTrue() throws Exception {
        // Arrange
        Method method = TestController.class.getMethod("unannotatedMethod");
        HandlerMethod handlerMethod = new HandlerMethod(new TestController(), method);
        setupAuthentication(42L);

        // Act
        boolean result = interceptor.preHandle(request, response, handlerMethod);

        // Assert
        assertTrue(result);
        verifyNoInteractions(rateLimitService);
    }

    // ─── Test: Contest creation rate limit ────────────────────────────────────

    @Test
    void preHandle_contestCreation_callsCorrectRateLimitCheck() throws Exception {
        // Arrange
        Method method = TestController.class.getMethod("createContest");
        HandlerMethod handlerMethod = new HandlerMethod(new TestController(), method);
        setupAuthentication(42L);
        setupRequest("/api/contests/private", "POST");

        // Act
        boolean result = interceptor.preHandle(request, response, handlerMethod);

        // Assert
        assertTrue(result);
        verify(rateLimitService).checkContestCreationLimit(42L);
    }

    @Test
    void preHandle_contestCreation_rateLimitExceeded_throwsException() throws Exception {
        // Arrange
        Method method = TestController.class.getMethod("createContest");
        HandlerMethod handlerMethod = new HandlerMethod(new TestController(), method);
        setupAuthentication(42L);
        setupRequest("/api/contests/private", "POST");

        doThrow(new TooManyRequestsException("Rate limit exceeded", 3600L))
                .when(rateLimitService).checkContestCreationLimit(42L);

        // Act & Assert
        assertThrows(TooManyRequestsException.class, () -> {
            interceptor.preHandle(request, response, handlerMethod);
        });
    }

    // ─── Test: AI problem generation rate limit ───────────────────────────────

    @Test
    void preHandle_aiProblemGeneration_callsCorrectRateLimitCheck() throws Exception {
        // Arrange
        Method method = TestController.class.getMethod("generateProblem");
        HandlerMethod handlerMethod = new HandlerMethod(new TestController(), method);
        setupAuthentication(55L);
        setupRequest("/api/contests/private/1/problems/generate", "POST");

        // Act
        boolean result = interceptor.preHandle(request, response, handlerMethod);

        // Assert
        assertTrue(result);
        verify(rateLimitService).checkAIProblemGenLimit(55L);
    }

    // ─── Test: Invite regeneration rate limit ─────────────────────────────────

    @Test
    void preHandle_inviteRegeneration_callsCorrectRateLimitCheck() throws Exception {
        // Arrange
        Method method = TestController.class.getMethod("regenerateInvite");
        HandlerMethod handlerMethod = new HandlerMethod(new TestController(), method);
        setupAuthentication(42L);
        setupRequest("/api/contests/private/123/invite/regenerate", "POST");
        setupPathVariables(Map.of("contestId", "123"));

        // Act
        boolean result = interceptor.preHandle(request, response, handlerMethod);

        // Assert
        assertTrue(result);
        verify(rateLimitService).checkInviteRegenLimit(123L);
    }

    @Test
    void preHandle_inviteRegeneration_missingContestId_passesThrough() throws Exception {
        // Arrange
        Method method = TestController.class.getMethod("regenerateInvite");
        HandlerMethod handlerMethod = new HandlerMethod(new TestController(), method);
        setupAuthentication(42L);
        setupRequest("/api/contests/private/123/invite/regenerate", "POST");
        // No path variables set

        // Act
        boolean result = interceptor.preHandle(request, response, handlerMethod);

        // Assert
        assertTrue(result);
        // Should pass through without calling rate limit service
        verifyNoInteractions(rateLimitService);
    }

    // ─── Test: Invite acceptance rate limit ───────────────────────────────────

    @Test
    void preHandle_inviteAcceptance_callsCorrectRateLimitCheck() throws Exception {
        // Arrange
        Method method = TestController.class.getMethod("acceptInvite");
        HandlerMethod handlerMethod = new HandlerMethod(new TestController(), method);
        setupAuthentication(55L);
        setupRequest("/api/contests/private/456/join", "POST");
        setupPathVariables(Map.of("contestId", "456"));

        // Act
        boolean result = interceptor.preHandle(request, response, handlerMethod);

        // Assert
        assertTrue(result);
        verify(rateLimitService).checkInviteAcceptLimit(456L);
    }

    // ─── Test: Unauthenticated requests ───────────────────────────────────────

    @Test
    void preHandle_unauthenticatedUser_passesThrough() throws Exception {
        // Arrange
        Method method = TestController.class.getMethod("createContest");
        HandlerMethod handlerMethod = new HandlerMethod(new TestController(), method);
        // No authentication set
        setupRequest("/api/contests/private", "POST");

        // Act
        boolean result = interceptor.preHandle(request, response, handlerMethod);

        // Assert
        assertTrue(result);
        // Should pass through without calling rate limit service
        verifyNoInteractions(rateLimitService);
    }

    // ─── Test: Non-HandlerMethod objects ──────────────────────────────────────

    @Test
    void preHandle_nonHandlerMethod_passesThrough() throws Exception {
        // Arrange
        Object handler = new Object(); // Not a HandlerMethod

        // Act
        boolean result = interceptor.preHandle(request, response, handler);

        // Assert
        assertTrue(result);
        verifyNoInteractions(rateLimitService);
    }

    // ─── Helper Methods ────────────────────────────────────────────────────────

    private void setupAuthentication(Long userId) {
        UserDetailsImpl userDetails = mock(UserDetailsImpl.class);
        when(userDetails.getId()).thenReturn(userId);
        
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void setupRequest(String uri, String method) {
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getMethod()).thenReturn(method);
    }

    private void setupPathVariables(Map<String, String> variables) {
        when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
                .thenReturn(variables);
    }

    // ─── Test Controller ───────────────────────────────────────────────────────

    /**
     * Dummy controller class for testing annotation detection.
     */
    private static class TestController {

        public void unannotatedMethod() {
            // No annotation
        }

        @RateLimited(type = RateLimited.RateLimitType.CONTEST_CREATION)
        public void createContest() {
            // Annotated with CONTEST_CREATION
        }

        @RateLimited(type = RateLimited.RateLimitType.AI_PROBLEM_GENERATION)
        public void generateProblem() {
            // Annotated with AI_PROBLEM_GENERATION
        }

        @RateLimited(type = RateLimited.RateLimitType.INVITE_REGENERATION)
        public void regenerateInvite() {
            // Annotated with INVITE_REGENERATION
        }

        @RateLimited(type = RateLimited.RateLimitType.INVITE_ACCEPTANCE)
        public void acceptInvite() {
            // Annotated with INVITE_ACCEPTANCE
        }
    }
}
