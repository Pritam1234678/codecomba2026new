package com.example.codecombat2026.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark controller methods that require rate limiting.
 * 
 * The rate limit type determines which limit is applied:
 * - CONTEST_CREATION: 5 per hour per user
 * - AI_PROBLEM_GENERATION: 5 per day per user
 * - INVITE_REGENERATION: 10 per hour per contest
 * - INVITE_ACCEPTANCE: 100 per hour per contest
 * 
 * Usage:
 * <pre>
 * {@code @RateLimited(type = RateLimitType.CONTEST_CREATION)}
 * {@code @PostMapping("/api/contests/private")}
 * public ResponseEntity createContest(...) { ... }
 * </pre>
 * 
 * The interceptor will automatically extract the user ID or contest ID
 * from the request context and apply the appropriate rate limit check.
 * 
 * Requirements: 24.5, 24.6
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {
    
    /**
     * The type of rate limit to apply.
     * Determines which service method to call and what limits to enforce.
     */
    RateLimitType type();
    
    /**
     * For contest-scoped rate limits (INVITE_REGENERATION, INVITE_ACCEPTANCE),
     * this specifies the path variable name that contains the contest ID.
     * Default is "contestId".
     */
    String contestIdParam() default "contestId";
    
    /**
     * Enum defining the types of rate limits available.
     */
    enum RateLimitType {
        /** Contest creation - 5 per hour per user */
        CONTEST_CREATION,
        
        /** AI problem generation - 5 per day per user */
        AI_PROBLEM_GENERATION,
        
        /** Invite link regeneration - 10 per hour per contest */
        INVITE_REGENERATION,
        
        /** Invite acceptance - 100 per hour per contest */
        INVITE_ACCEPTANCE
    }
}
