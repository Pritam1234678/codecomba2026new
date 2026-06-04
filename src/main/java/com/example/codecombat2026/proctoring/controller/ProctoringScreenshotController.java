package com.example.codecombat2026.proctoring.controller;

import com.example.codecombat2026.proctoring.entity.ProctoringScreenshot;
import com.example.codecombat2026.proctoring.exception.ProctoringNotFoundException;
import com.example.codecombat2026.proctoring.repository.ProctoringScreenshotRepository;
import com.example.codecombat2026.proctoring.service.ProctoringRateLimiter;
import com.example.codecombat2026.proctoring.service.ProctoringScreenshotService;
import com.example.codecombat2026.proctoring.service.ProctoringScreenshotService.ScreenshotPayload;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * Public-facing HTTP surface for proctoring screenshots.
 *
 * <p>Two endpoints, two distinct authentication gates:
 *
 * <ul>
 *   <li><b>{@code POST /api/proctoring/screenshots}</b> — JWT-authenticated
 *       multipart upload from the candidate browser. The browser captures a
 *       JPEG only after a triggering Suspicious_Event (e.g. {@code TAB_SWITCH},
 *       {@code FULLSCREEN_EXIT}, {@code MULTIPLE_FACES}, {@code NO_FACE},
 *       {@code WEBCAM_STREAM_LOST}) has been acknowledged on the WebSocket;
 *       the resulting {@code event_id} is bound to the upload so each
 *       screenshot is anchored to the trigger that produced it (Req 8.3, 8.7).
 *       The request body is validated end-to-end inside
 *       {@link ProctoringScreenshotService#upload upload} — magic-byte sniff,
 *       MIME whitelist, byte-size cap, ownership, session liveness, event FK
 *       — and the typed exceptions thrown there carry the correct
 *       {@code 403 / 413 / 415 / 500} status, so this controller simply
 *       lets them propagate to the global handler. The per-session
 *       screenshot rate limit (Req 17.4) is gated <em>at the controller</em>
 *       so the {@code 429 Too Many Requests} response can carry both the
 *       canonical {@code {error,message}} body shape and the
 *       {@code Retry-After: 60} header (Req 17.5) — mirroring the auth-flow
 *       pattern in {@code AuthController} (Req 8.4, 8.5, 14.2, 17.4, 17.5).</li>
 *   <li><b>{@code GET /api/admin/proctoring/sessions/{sid}/screenshots/{shotId}}</b>
 *       — admin-only streaming read. The {@code {sid}} segment is part of
 *       the URL purely for path consistency: it lets us reject mismatched
 *       URLs as {@code 404} without leaking information about which
 *       screenshot rows exist for which session. Bytes are pulled from the
 *       application — never from {@code /uploads/**} statically — so every
 *       fetch is gated by {@code ROLE_ADMIN} (Req 14.3, 15.4, 16.1).</li>
 * </ul>
 *
 * <p>The admin GET writes {@code Cache-Control: private, no-store} so
 * forensic evidence is never persisted in admin browsers' on-disk cache;
 * combined with the per-prefix deny in {@link
 * com.example.codecombat2026.security.SecurityConfig SecurityConfig}, the
 * raw JPEG path on the filesystem is unreachable except through this
 * controller.
 *
 * <p>Validates: Req 8.3, 8.4, 8.5, 14.2, 14.4, 15.4.
 */
@RestController
@RequiredArgsConstructor
public class ProctoringScreenshotController {

    private static final Logger log = LoggerFactory.getLogger(ProctoringScreenshotController.class);

    private final ProctoringScreenshotService screenshotService;
    private final ProctoringScreenshotRepository screenshotRepository;
    private final ProctoringRateLimiter rateLimiter;

    /**
     * Candidate-side screenshot upload. The browser sends the four parts as
     * {@code multipart/form-data}: the two {@code Long} ids, the ISO-8601
     * client capture timestamp, and the JPEG/PNG bytes. All payload
     * validation lives in
     * {@link ProctoringScreenshotService#upload(Long, Long, LocalDateTime, String, byte[], Long)}
     * which throws a {@link
     * com.example.codecombat2026.proctoring.exception.ProctoringValidationException
     * ProctoringValidationException} carrying the exact {@code HttpStatus}
     * for each failure mode (415, 413, 400, 500). The global handler
     * translates that into the JSON envelope; nothing here re-derives status
     * codes, which keeps the upload endpoint thin and the validation rules
     * single-sourced. The one exception is the per-session screenshot
     * rate limit (Req 17.4), which is gated here so the {@code 429}
     * response can carry the canonical {@code {error,message}} body and
     * the {@code Retry-After: 60} header (Req 17.5).
     *
     * <p>{@code mimeType} is taken from the multipart part's
     * {@code Content-Type} header, which the service then re-checks against
     * the actual byte signature — so a spoofed {@code Content-Type:
     * image/png} on JPEG bytes still fails the magic-byte sniff (Req 8.5).
     *
     * @param sessionId      target {@code proctoring_sessions.id}
     * @param eventId        triggering {@code proctoring_events.id}
     * @param capturedAtIso  browser-side capture timestamp (ISO-8601 local
     *                       date-time, e.g. {@code 2025-11-04T12:34:56.789})
     * @param file           the JPEG/PNG part
     * @param userDetails    authenticated caller resolved from the JWT
     * @return {@code 201 { "screenshotId": <id>, "byteSize": <n> }}
     * @throws IOException                           if reading the multipart
     *                                               payload fails before it
     *                                               reaches the service
     * @throws java.time.format.DateTimeParseException if {@code captured_at}
     *                                               is not parseable
     */
    @PostMapping(
            value = "/api/proctoring/screenshots",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> upload(
            @RequestParam("session_id") Long sessionId,
            @RequestParam("event_id") Long eventId,
            @RequestParam("captured_at") String capturedAtIso,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) throws IOException {

        LocalDateTime capturedAt = LocalDateTime.parse(capturedAtIso.trim());
        String mimeType = file.getContentType();
        byte[] bytes = file.getBytes();

        // Per-user screenshot rate limit (Req 17.4, 17.5). Keyed on
        // the authenticated user_id (not session_id from the request)
        // so a forged sessionId cannot consume someone else's rate limit
        // bucket (fixes Bug 13). The ownership check inside upload()
        // is the definitive gate on which sessions this user can write.
        if (!rateLimiter.allowScreenshotUpload(sessionId)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", "60")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "error", "RATE_LIMITED",
                            "message", "screenshot upload rate limit exceeded"));
        }

        // All payload validation, including magic-byte sniff, lives in
        // the service. Typed exceptions there carry the right HttpStatus
        // for the global handler — let them propagate.
        Long screenshotId = screenshotService.upload(
                sessionId,
                eventId,
                capturedAt,
                mimeType,
                bytes,
                userDetails.getId()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "screenshotId", screenshotId,
                        "byteSize", bytes.length
                ));
    }

    /**
     * Admin streaming read for a single screenshot.
     *
     * <p>The {@code {sid}} path segment isn't strictly needed to look up
     * the row, but verifying {@code row.sessionId == sid} stops a
     * malformed URL from quietly returning the wrong session's screenshot
     * — a 404 is preferable to silent cross-session leakage even with
     * {@code ROLE_ADMIN}. A 404 is also returned when the on-disk JPEG has
     * already been purged by the daily retention job (Req 21.2) but the
     * row hasn't yet been deleted; the admin UI surfaces this as a
     * "screenshot expired" placeholder.
     *
     * <p>The response carries {@code Content-Type} from the persisted
     * {@code mime_type} (never re-sniffed on the read path) and
     * {@code Cache-Control: private, no-store} to prevent admin browsers
     * from caching forensic evidence to disk.
     *
     * @param sid    expected owning {@code proctoring_sessions.id}
     * @param shotId target {@code proctoring_screenshots.id}
     * @return {@code 200} streaming JPEG/PNG bytes via
     *         {@link InputStreamResource}, or {@code 404} if the row,
     *         session match, or on-disk file is missing
     */
    @GetMapping("/api/admin/proctoring/sessions/{sid}/screenshots/{shotId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InputStreamResource> serveAdmin(
            @PathVariable("sid") Long sid,
            @PathVariable("shotId") Long shotId
    ) {
        // Path-consistency check first — load the row so we can compare
        // session_id without trusting just the shotId. Surfacing a 404 (not
        // 400) keeps the failure mode indistinguishable from a missing row,
        // which is the right confidentiality posture even though admins
        // already have broad read access.
        ProctoringScreenshot row = screenshotRepository.findById(shotId)
                .orElseThrow(() -> new ProctoringNotFoundException(
                        "Screenshot not found: " + shotId));
        if (!Objects.equals(row.getSessionId(), sid)) {
            throw new ProctoringNotFoundException(
                    "Screenshot " + shotId + " does not belong to session " + sid);
        }

        // Service handles the on-disk read and emits a 404 itself if the
        // file was already purged. We only translate its byte payload into
        // a streaming response here.
        ScreenshotPayload payload = screenshotService.serveAdmin(shotId);

        InputStreamResource resource = new InputStreamResource(
                new ByteArrayInputStream(payload.bytes()));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(payload.mimeType()))
                .contentLength(payload.bytes().length)
                .header("Cache-Control", "private, no-store")
                .body(resource);
    }
}
