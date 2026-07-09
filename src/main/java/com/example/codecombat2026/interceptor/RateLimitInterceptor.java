package com.example.codecombat2026.interceptor;

import com.example.codecombat2026.annotation.RateLimited;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.example.codecombat2026.service.PrivateContestRateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

/**
 * Interceptor that enforces rate limiting on controller endpoints marked with @RateLimited.
 * 
 * This interceptor:
 * 1. Checks if the handler method has @RateLimited annotation
 * 2. Extracts the user ID from the security context
 * 3. Extracts contest ID from path variables (for contest-scoped limits)
 * 4. Calls the appropriate rate limit check method
 * 5. Logs rate limit violations for monitoring
 * 
 * The rate limit service will throw TooManyRequestsException if the limit is exceeded,
 * which is caught by the GlobalExceptionHandler and converted to HTTP 429 response
 * with Retry-After header.
 * 
 * Configuration:
 * This interceptor must be registered in WebMvcConfigurer to be active.
 * 
 * Rate Limits (per Requirement 24):
 * - Contest creation: 5 per hour per user
 * - AI problem generation: 5 per day per user
 * - Invite regeneration: 10 per hour per contest
 * - Invite acceptance: 100 per hour per contest
 * 
 * Requirements: 24.5, 24.6
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private final PrivateContestRateLimitService rateLimitService;

    public RateLimitInterceptor(PrivateContestRateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    /**
     * Pre-handle method called before the controller method executes.
     * 
     * Checks for @RateLimited annotation and enforces the appropriate rate limit.
     * 
     * @param request Current HTTP request
     * @param response Current HTTP response
     * @param handler The handler (controller method) being invoked
     * @return true to proceed with execution, false to abort
     * @throws Exception if rate limit is exceeded or other error occurs
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) 
            throws Exception {
        
        // Only process if handler is a controller method
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        
        // Check if method has @RateLimited annotation
        RateLimited rateLimited = handlerMethod.getMethodAnnotation(RateLimited.class);
        if (rateLimited == null) {
            // No rate limiting required for this endpoint
            return true;
        }

        // Get the rate limit type
        RateLimited.RateLimitType limitType = rateLimited.type();
        
        log.debug("Rate limit check for endpoint: {} {}, type: {}", 
                request.getMethod(), request.getRequestURI(), limitType);

        // Extract user ID from security context
        Long userId = extractUserId();
        if (userId == null) {
            log.warn("Rate limit check skipped - no authenticated user found");
            // Allow unauthenticated requests to pass through
            // The controller's @PreAuthorize will handle authentication
            return true;
        }

        // Apply the appropriate rate limit check based on type
        switch (limitType) {
            case CONTEST_CREATION:
                log.debug("Checking contest creation rate limit for user {}", userId);
                rateLimitService.checkContestCreationLimit(userId);
                break;
                
            case AI_PROBLEM_GENERATION:
                log.debug("Checking AI problem generation rate limit for user {}", userId);
                rateLimitService.checkAIProblemGenLimit(userId);
                break;
                
            case INVITE_REGENERATION:
                Long contestIdRegen = extractContestId(request, rateLimited.contestIdParam());
                if (contestIdRegen == null) {
                    log.error("Contest ID not found in path variables for invite regeneration rate limit");
                    return true; // Allow request to proceed, will fail at controller level
                }
                log.debug("Checking invite regeneration rate limit for contest {}", contestIdRegen);
                rateLimitService.checkInviteRegenLimit(contestIdRegen);
                break;
                
            case INVITE_ACCEPTANCE:
                Long contestIdAccept = extractContestId(request, rateLimited.contestIdParam());
                if (contestIdAccept == null) {
                    log.error("Contest ID not found in path variables for invite acceptance rate limit");
                    return true; // Allow request to proceed, will fail at controller level
                }
                log.debug("Checking invite acceptance rate limit for contest {}", contestIdAccept);
                rateLimitService.checkInviteAcceptLimit(contestIdAccept);
                break;
                
            default:
                log.warn("Unknown rate limit type: {}", limitType);
        }

        // Rate limit check passed, allow request to proceed
        return true;
    }

    /**
     * Extract the authenticated user ID from the security context.
     * 
     * @return User ID if authenticated, null otherwise
     */
    private Long extractUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetailsImpl) {
            return ((UserDetailsImpl) principal).getId();
        }
        
        return null;
    }

    /**
     * Extract contest ID from path variables.
     * 
     * @param request The HTTP request
     * @param paramName The name of the path variable (default: "contestId")
     * @return Contest ID if found, null otherwise
     */
    @SuppressWarnings("unchecked")
    private Long extractContestId(HttpServletRequest request, String paramName) {
        try {
            // Get path variables from Spring MVC
            Map<String, String> pathVariables = 
                    (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            
            if (pathVariables != null && pathVariables.containsKey(paramName)) {
                String contestIdStr = pathVariables.get(paramName);
                return Long.parseLong(contestIdStr);
            }
        } catch (NumberFormatException e) {
            log.error("Invalid contest ID format in path variable: {}", paramName, e);
        } catch (Exception e) {
            log.error("Error extracting contest ID from path variables", e);
        }
        
        return null;
    }
}
