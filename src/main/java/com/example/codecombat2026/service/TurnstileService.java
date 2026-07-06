package com.example.codecombat2026.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Server-side validator for Cloudflare Turnstile tokens. The frontend renders
 * the Turnstile widget which produces a single-use token; we POST it back to
 * Cloudflare's siteverify endpoint along with our secret key. Cloudflare
 * returns {success, error-codes, action, cdata, ...}.
 *
 * Tokens are valid for 5 minutes and are single-use — Cloudflare itself
 * rejects re-submission, so we don't need our own replay store.
 *
 * Failure modes:
 *  - Cloudflare unreachable (network blip) → fail OPEN with a warning. The
 *    rest of the pipeline (rate limit, honeypot, account lockout) still
 *    blocks abuse, and refusing legit users due to a CF outage is worse
 *    than a brief abuse window.
 *  - Token empty / malformed → fail CLOSED, return false.
 */
@Service
public class TurnstileService {

    private static final Logger log = LoggerFactory.getLogger(TurnstileService.class);
    private static final String SITEVERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    @Value("${TURNSTILE_SECRET:}")
    private String secret;

    @Value("${TURNSTILE_ENABLED:true}")
    private boolean enabled;

    private final RestClient http = RestClient.builder().build();

    /**
     * Validate a Turnstile token. Returns true on success or when verification
     * is disabled (dev). On Cloudflare API outage we log a warning and return
     * true (fail-open) — see class javadoc.
     *
     * @param token the cf-turnstile-response value submitted by the client
     * @param remoteIp the originating client IP (X-Forwarded-For aware) — optional
     */
    @SuppressWarnings("unchecked")
    public boolean verify(String token, String remoteIp) {
        if (!enabled) {
            log.debug("Turnstile disabled — token accepted without verification");
            return true;
        }
        if (token == null || token.isBlank()) {
            return false;
        }
        if (secret == null || secret.isBlank()) {
            log.error("TURNSTILE_SECRET is not configured — refusing all Turnstile checks");
            return false;
        }

        Map<String, String> body = new LinkedHashMap<>();
        body.put("secret", secret);
        body.put("response", token);
        if (remoteIp != null && !remoteIp.isBlank()) {
            body.put("remoteip", remoteIp);
        }

        try {
            Map<String, Object> resp = http.post()
                    .uri(SITEVERIFY_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (resp == null) {
                log.warn("Turnstile siteverify returned null body — failing open");
                return true;
            }

            Object success = resp.get("success");
            boolean ok = Boolean.TRUE.equals(success);
            if (!ok) {
                log.warn("Turnstile token rejected by Cloudflare: {}", resp.get("error-codes"));
            }
            return ok;
        } catch (Exception e) {
            log.warn("Turnstile siteverify call failed ({}): {} — failing open", e.getClass().getSimpleName(), e.getMessage());
            return true;  // fail open on network errors
        }
    }
}
