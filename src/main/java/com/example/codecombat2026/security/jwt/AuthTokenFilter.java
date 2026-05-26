package com.example.codecombat2026.security.jwt;

import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.example.codecombat2026.security.services.UserDetailsServiceImpl;
import com.example.codecombat2026.service.JwtBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class AuthTokenFilter extends OncePerRequestFilter {
    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private JwtBlacklistService jwtBlacklistService;

    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

    /**
     * Skip JWT filter on async dispatches (e.g., SSE SseEmitter internal re-dispatch).
     * Without this, Spring Security re-runs the filter on the async thread with no JWT
     * and throws AuthorizationDeniedException, killing the SSE connection.
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return true;
    }

    /**
     * Skip JWT filter for SSE stream endpoint entirely.
     * This endpoint uses permitAll() + manual single-use ticket validation
     * in {@code SubmissionController}. The filter would otherwise send 401
     * before the controller can handle auth, and would also leak JWTs into
     * proxy logs via the URL.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().contains("/submissions/stream");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {

                // Check blacklist (logout / explicit revocation)
                String jti = jwtUtils.getJtiFromJwtToken(jwt);
                if (jti != null && jwtBlacklistService.isBlacklisted(jti)) {
                    logger.debug("Rejecting blacklisted JWT jti={}", jti);
                    filterChain.doFilter(request, response);
                    return;
                }

                String username = jwtUtils.getUserNameFromJwtToken(jwt);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Check per-user "invalidate-before" cutoff (set on password change)
                if (userDetails instanceof UserDetailsImpl impl) {
                    long cutoff = jwtBlacklistService.invalidateBeforeMillis(impl.getId());
                    long iat = jwtUtils.getIssuedAtMillis(jwt);
                    if (cutoff > 0 && iat > 0 && iat < cutoff) {
                        logger.debug("Rejecting JWT for userId={} issued before invalidate cutoff", impl.getId());
                        filterChain.doFilter(request, response);
                        return;
                    }
                }

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e);
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        // Note: We deliberately do NOT accept ?token= as a query param.
        // The previous implementation did so for SSE compatibility, but JWTs
        // in URLs end up in proxy logs and browser history. SSE auth now uses
        // single-use tickets via /submissions/sse-ticket instead.
        return null;
    }
}
