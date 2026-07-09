package com.example.codecombat2026.config;

import com.example.codecombat2026.interceptor.RateLimitInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for the application.
 * 
 * Configures:
 * - Static resource handlers (profile photos)
 * - Interceptors (rate limiting for private contests)
 * 
 * Requirements: 24.5, 24.6
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Static exposure is intentionally narrow: only public assets that are
        // safe to serve unauthenticated belong here. Proctoring artefacts under
        // uploads/proctoring/** (screenshots) MUST NEVER be mapped as a static
        // resource — they are served by ProctoringScreenshotController behind
        // an admin-role check (Req 14.3, 15.4, 16.1). If a new public upload
        // bucket is introduced, add it as its own explicit per-prefix handler;
        // do not regress to a broad "/uploads/**" mapping.
        registry.addResourceHandler("/uploads/profile-photos/**")
                .addResourceLocations("file:uploads/profile-photos/");
    }

    /**
     * Register interceptors for request processing.
     * 
     * Currently registers:
     * - RateLimitInterceptor: Enforces rate limits on @RateLimited endpoints
     * 
     * The rate limit interceptor applies to all paths but only activates
     * when a controller method is annotated with @RateLimited.
     * 
     * Requirements: 24.5, 24.6
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Register rate limit interceptor for all paths
        // It will only activate on methods with @RateLimited annotation
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**");
    }
}
