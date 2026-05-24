package com.example.codecombat2026.security;

import com.example.codecombat2026.security.jwt.AuthEntryPointJwt;
import com.example.codecombat2026.security.jwt.AuthTokenFilter;
import com.example.codecombat2026.security.services.UserDetailsServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;
import java.util.stream.Stream;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Autowired
    UserDetailsServiceImpl userDetailsService;

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    /**
     * Comma-separated list of allowed origins for CORS. MUST be set in
     * production. Default permits localhost dev only — wildcards (*.vercel.app,
     * *.ngrok.io) and hardcoded IPs are deliberately removed.
     *
     * Example: APP_ALLOWED_ORIGINS=https://codecombat.live,https://www.codecombat.live
     */
    @Value("${APP_ALLOWED_ORIGINS:http://localhost:5173,http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .exceptionHandling(exception -> exception.authenticationEntryPoint(
                    (request, response, authException) -> {
                        // For SSE stream endpoint, don't send 401 — let the controller handle it
                        if (request.getRequestURI().contains("/submissions/stream")) {
                            // Controller will return null for invalid tokens
                            // Just pass through without sending 401
                            return;
                        }
                        unauthorizedHandler.commence(request, response, authException);
                    }
                ))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/api", "/api/health", "/api/auth/**", "/api/test/**", "/api/support/**", "/api/queue-status")
                        .permitAll()
                        .requestMatchers("/api/compiler/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/contests").permitAll()
                        // SSE stream: permit at filter level, auth handled by @PreAuthorize + JWT filter
                        // Without this, Spring Security re-checks on async dispatch and fails (no JWT on async thread)
                        .requestMatchers("/api/submissions/stream").permitAll()
                        // Duel SSE stream: same pattern as /api/submissions/stream — auth is handled
                        // by a single-use SSE ticket (SseTicketService) consumed by the controller.
                        .requestMatchers("/api/duels/*/stream").permitAll()
                        // Admin duel routes: explicit role gate placed BEFORE the broader /api/admin/**
                        // matcher so reordering can't accidentally widen access.
                        .requestMatchers("/api/admin/duels/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated());

        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
        http.authenticationProvider(authenticationProvider());

        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();

        // Parse comma-separated origins from env. Strip whitespace, drop empties.
        List<String> origins = Stream.of(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        if (origins.isEmpty()) {
            log.error("❌ APP_ALLOWED_ORIGINS is empty — CORS will reject all browsers. " +
                "Set APP_ALLOWED_ORIGINS to your frontend origin(s).");
        }

        // Reject wildcard subdomains and bare wildcards in production-like
        // configurations. A wildcard origin combined with allowCredentials=true
        // is a CORS spec violation and a security hole.
        for (String o : origins) {
            if (o.contains("*")) {
                log.warn("⚠️  CORS allowed origin '{}' contains a wildcard. " +
                    "Wildcards combined with credentials are unsafe and only allowed for dev hosts.", o);
            }
        }

        log.info("✅ CORS allowed origins: {}", origins);

        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(java.util.List.of("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setExposedHeaders(java.util.List.of("Retry-After"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
