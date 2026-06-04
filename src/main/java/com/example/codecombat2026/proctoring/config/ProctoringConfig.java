package com.example.codecombat2026.proctoring.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * Single ops-control surface for the proctoring layer.
 *
 * <p>Loaded by Spring's {@code @ConfigurationProperties} binding from
 * {@code application.properties} (and any {@code .env.*} file the operator
 * has merged into the JVM environment). Every key listed in Req 20.1 lives
 * here, plus the {@code weights} map (Req 12.2 defaults) and the {@code Bands}
 * inner record carrying the {@code LOW}/{@code MEDIUM}/{@code HIGH} thresholds
 * (Req 12.3).
 *
 * <p>Spring's relaxed binding handles both kebab-case in property files
 * (e.g. {@code proctoring.no-face-threshold-seconds}) and SCREAMING_SNAKE_CASE
 * env vars (e.g. {@code PROCTORING_NO_FACE_THRESHOLD_SECONDS}).
 *
 * <p>Validates: Req 7.1, 9.7, 9.8, 11.4, 11.5, 12.2, 12.3, 17.2, 17.4, 17.6,
 * 20.1, 20.2.
 */
@Configuration
@ConfigurationProperties(prefix = "proctoring")
@Data
public class ProctoringConfig {

    // ── Req 20.1 — 13 tunable keys ─────────────────────────────────────────

    /** No-face debounce window before {@code NO_FACE} fires (Req 7.3). */
    private int noFaceThresholdSeconds = 5;

    /** AI inference tick period in the browser worker (Req 7.1). */
    private int aiInferenceIntervalMs = 1000;

    /** Candidate-side heartbeat send cadence (Req 9.7). */
    private int heartbeatIntervalSeconds = 15;

    /** Server-side heartbeat-timeout deadline (Req 9.8). */
    private int heartbeatTimeoutSeconds = 45;

    /** Offline-buffer modal trigger after this many seconds disconnected (Req 11.5). */
    private int maxOfflineSeconds = 60;

    /** IndexedDB offline buffer cap before overflow (Req 11.4). */
    private int maxOfflineEvents = 1000;

    /** Hard cap on a single inbound event frame in bytes (Req 17.6). */
    private int maxEventBytes = 4096;

    /** Hard cap on a single screenshot upload in bytes (Req 17.4). */
    private int maxScreenshotBytes = 262144;

    /** Browser-side screenshot canvas width (Req 8.4 sizing). */
    private int screenshotMaxWidth = 640;

    /** Browser-side screenshot canvas height (Req 8.4 sizing). */
    private int screenshotMaxHeight = 480;

    /** JPEG quality for browser-side {@code canvas.toBlob} (Req 8.4 sizing). */
    private double screenshotJpegQuality = 0.7;

    /** Per-session inbound event rate cap, averaged over a 10s window (Req 17.2). */
    private int eventRateLimitPerSecond = 30;

    /** Per-session screenshot upload rate cap (Req 17.4). */
    private int screenshotRateLimitPerMinute = 10;

    // ── Req 12.2 — Risk_Weight_Config defaults ─────────────────────────────

    /**
     * {@code event_type → score_delta} mapping consulted by
     * {@code RiskScoringEngine.weightFor}. Operators can override individual
     * entries via {@code proctoring.weights.<EVENT_TYPE>=<delta>} keys.
     * Unknown event types resolve to {@code 0} with a single warning log
     * per (type, JVM lifetime) tuple (Req 22.3).
     */
    private Map<String, Integer> weights = defaultWeights();

    // ── Req 12.3 — Risk_Band thresholds ────────────────────────────────────

    /** {@code LOW=0..50}, {@code MEDIUM=51..100}, {@code HIGH=>100} (Req 12.3). */
    private Bands bands = new Bands(50, 100);

    /**
     * Risk_Band thresholds. Inclusive upper bounds: a score equal to
     * {@code lowMax} is still {@code LOW}; a score equal to {@code mediumMax}
     * is still {@code MEDIUM}; anything strictly above {@code mediumMax} is
     * {@code HIGH}.
     */
    public record Bands(
            @DefaultValue("50") int lowMax,
            @DefaultValue("100") int mediumMax) {
    }

    private static Map<String, Integer> defaultWeights() {
        // LinkedHashMap so the operator-visible iteration order matches the
        // Risk_Weight_Config table in design.md.
        Map<String, Integer> w = new LinkedHashMap<>();
        // Req 12.2 — explicit defaults
        w.put("TAB_SWITCH", 20);
        w.put("WINDOW_BLUR", 10);
        w.put("FULLSCREEN_EXIT", 30);
        w.put("MULTIPLE_FACES", 50);
        w.put("NO_FACE", 40);
        w.put("COPY_ATTEMPT", 5);
        w.put("PASTE_ATTEMPT", 15);
        w.put("CONTEXT_MENU_ATTEMPT", 2);
        w.put("WEBCAM_STREAM_LOST", 40);
        w.put("HEARTBEAT_TIMEOUT", 20);
        // design.md additions (zero-weighted informational events are
        // intentionally omitted — unknown types resolve to 0 anyway)
        w.put("CUT_ATTEMPT", 5);
        w.put("DEVTOOLS_OPEN", 30);
        w.put("BUFFER_OVERFLOW", 10);
        return w;
    }
}
