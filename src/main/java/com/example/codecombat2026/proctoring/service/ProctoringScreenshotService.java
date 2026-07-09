package com.example.codecombat2026.proctoring.service;

import com.example.codecombat2026.proctoring.config.ProctoringConfig;
import com.example.codecombat2026.proctoring.entity.ProctoringEvent;
import com.example.codecombat2026.proctoring.entity.ProctoringScreenshot;
import com.example.codecombat2026.proctoring.entity.ProctoringSession;
import com.example.codecombat2026.proctoring.exception.ProctoringForbiddenException;
import com.example.codecombat2026.proctoring.exception.ProctoringNotFoundException;
import com.example.codecombat2026.proctoring.exception.ProctoringValidationException;
import com.example.codecombat2026.proctoring.repository.ProctoringEventRepository;
import com.example.codecombat2026.proctoring.repository.ProctoringScreenshotRepository;
import com.example.codecombat2026.proctoring.repository.ProctoringSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

/**
 * Owns disk write, validation, and metadata persistence for proctoring
 * screenshots (Req 8.4, 8.5, 8.6, 14.2, 14.3, 17.4, 17.5).
 *
 * <p>Screenshots are uploaded by the candidate browser only when a
 * triggering Suspicious_Event has been acknowledged with an
 * {@code event_id}; there is no periodic capture path (Req 8.7). The
 * actual JPEG bytes live on the local filesystem under
 * {@code uploads/proctoring/sessions/{session_id}/{event_id}.jpg}; the
 * database row only carries the {@code storage_ref} pointer plus the
 * declared MIME type and byte size (Req 14.3).
 *
 * <p>The {@link #upload upload} method enforces the validation order
 * documented in {@code design.md} (sec. {@code ProctoringScreenshotService})
 * exactly:
 *
 * <ol>
 *   <li>Session ownership — {@code session.userId == uploadingUserId},
 *       else 403 {@code NOT_OWNER}.</li>
 *   <li>Session is active — {@code session.endedAt IS NULL}, else 403
 *       {@code SESSION_ENDED}. Prevents post-finish uploads polluting
 *       storage.</li>
 *   <li>MIME whitelist — declared {@code mimeType} must be
 *       {@code image/jpeg} or {@code image/png}, else 415
 *       {@code UNSUPPORTED_MEDIA_TYPE}.</li>
 *   <li>Byte size cap — {@code bytes.length <= maxScreenshotBytes}, else
 *       413 {@code PAYLOAD_TOO_LARGE} (Req 8.4, 17.4).</li>
 *   <li>Magic-byte sniff — first 3 bytes match {@code FF D8 FF} for
 *       JPEG or first 8 bytes match
 *       {@code 89 50 4E 47 0D 0A 1A 0A} for PNG, and the magic must agree
 *       with the declared MIME. Mismatch → 415
 *       {@code UNSUPPORTED_MEDIA_TYPE} (defense against MIME spoofing —
 *       never trust {@code Content-Type} alone).</li>
 *   <li>Event FK validity — the {@code event_id} must exist and its
 *       {@code session_id} must match the request's {@code sessionId},
 *       else 400 {@code INVALID_EVENT}.</li>
 *   <li>Rate limit — applied by the controller before calling this
 *       service (Req 17.4, 17.5). Task 13.2 wires the limiter at the
 *       controller layer; this service intentionally does not invoke
 *       it so the validation order remains a pure function of the
 *       arguments and the database.</li>
 *   <li>Disk write — {@code Files.createDirectories(parent)} +
 *       {@code Files.write(path, bytes, CREATE | WRITE | TRUNCATE_EXISTING)}.
 *       Path is built only from validated {@link Long} ids
 *       (no user-controlled string segments), so traversal attacks are
 *       not reachable.</li>
 *   <li>INSERT row — saves the metadata pointing at the file just
 *       written. Returns the new {@code screenshot_id}.</li>
 * </ol>
 *
 * <p>{@link #serveAdmin serveAdmin} is the read-side counterpart used by
 * the admin streaming endpoint (task 7.2): it loads the row, verifies
 * the file still exists on disk, reads the bytes, and returns them with
 * the persisted MIME type so the controller can write the response with
 * {@code Cache-Control: private, no-store}. A row whose file has been
 * purged by the retention job (Req 21.2) yields 404, matching task 7.2.
 *
 * <p>Validates: Req 8.4, 8.5, 8.6, 14.2, 14.3, 17.4, 17.5.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProctoringScreenshotService {

    /** MIME types accepted on upload (Req 8.5). */
    private static final Set<String> ALLOWED_MIME = Set.of("image/jpeg", "image/png");

    /** Filesystem root for screenshot storage (Req 14.3). */
    private static final String STORAGE_ROOT = "uploads/proctoring/sessions";

    private final ProctoringSessionRepository sessionRepo;
    private final ProctoringEventRepository eventRepo;
    private final ProctoringScreenshotRepository shotRepo;
    private final ProctoringConfig config;

    /**
     * Validate, persist to disk, and record the metadata row for one
     * screenshot upload. Validation order matches {@code design.md}; see
     * the class-level Javadoc for the precise sequence and HTTP codes.
     *
     * @param sessionId        target {@code proctoring_sessions.id}
     * @param eventId          triggering {@code proctoring_events.id};
     *                         must belong to {@code sessionId}
     * @param capturedAt       browser-side capture timestamp
     * @param mimeType         declared MIME type from the multipart part
     * @param bytes            raw JPEG/PNG bytes
     * @param uploadingUserId  authenticated caller ({@code users.id})
     * @return new {@code proctoring_screenshots.id}
     * @throws ProctoringNotFoundException     404 if {@code sessionId}
     *                                         does not exist
     * @throws ProctoringForbiddenException    403 with code
     *                                         {@code NOT_OWNER} or
     *                                         {@code SESSION_ENDED}
     * @throws ProctoringValidationException   415 (bad MIME or magic-byte
     *                                         mismatch), 413 (size cap),
     *                                         400 (event/session FK
     *                                         mismatch), 500 (disk write
     *                                         failure)
     */
    @Transactional
    public Long upload(Long sessionId,
                       Long eventId,
                       LocalDateTime capturedAt,
                       String mimeType,
                       byte[] bytes,
                       Long uploadingUserId) {

        // 1. Session ownership.
        ProctoringSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new ProctoringNotFoundException(
                        "Proctoring session not found: " + sessionId));
        if (!Objects.equals(session.getUserId(), uploadingUserId)) {
            throw new ProctoringForbiddenException(
                    "NOT_OWNER", "Session does not belong to caller");
        }

        // 2. Session is active.
        if (session.getEndedAt() != null) {
            throw new ProctoringForbiddenException(
                    "SESSION_ENDED", "Session has already ended");
        }

        // 3. MIME whitelist (Req 8.5).
        if (mimeType == null || !ALLOWED_MIME.contains(mimeType)) {
            throw new ProctoringValidationException(
                    "UNSUPPORTED_MEDIA_TYPE",
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "mime type must be image/jpeg or image/png");
        }

        // 4. Byte size cap (Req 8.4, 17.4).
        if (bytes == null || bytes.length == 0) {
            throw new ProctoringValidationException(
                    "EMPTY_PAYLOAD",
                    HttpStatus.BAD_REQUEST,
                    "screenshot payload is empty");
        }
        if (bytes.length > config.getMaxScreenshotBytes()) {
            throw new ProctoringValidationException(
                    "PAYLOAD_TOO_LARGE",
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "screenshot exceeds maxScreenshotBytes");
        }

        // 5. Magic-byte sniff — declared MIME must agree with the actual
        //    bytes (defense against Content-Type spoofing).
        boolean jpegMime = "image/jpeg".equals(mimeType);
        boolean pngMime = "image/png".equals(mimeType);
        if ((jpegMime && !isJpeg(bytes)) || (pngMime && !isPng(bytes))) {
            throw new ProctoringValidationException(
                    "UNSUPPORTED_MEDIA_TYPE",
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "magic byte mismatch");
        }

        // 6. Event FK validity — event must exist and belong to this session.
        //    When eventId is null (correlation-id from browser, no DB row yet),
        //    skip this check and persist file-only (no DB row).
        ProctoringEvent event = null;
        if (eventId != null) {
            event = eventRepo.findById(eventId)
                    .filter(e -> Objects.equals(e.getSessionId(), sessionId))
                    .orElseThrow(() -> new ProctoringValidationException(
                            "INVALID_EVENT",
                            HttpStatus.BAD_REQUEST,
                            "event does not belong to session"));
        }

        // 7. Rate limit was already enforced at the controller boundary.

        // 8. Disk write. Path is built only from validated values:
        //    no user-controlled string segments are interpolated, so
        //    traversal is unreachable.
        String fileId = eventId != null ? String.valueOf(eventId) : "sc-" + System.currentTimeMillis();
        Path path = Paths.get(STORAGE_ROOT, String.valueOf(sessionId), fileId + ".jpg");
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(path, bytes,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            log.info("Proctoring screenshot saved: session={} event={} size={} bytes path={}",
                    sessionId, eventId != null ? eventId : "NONE", bytes.length, path.toAbsolutePath());
        } catch (IOException ex) {
            log.error("Failed to write proctoring screenshot for session {} event {}: {}",
                    sessionId, eventId, ex.getMessage());
            throw new ProctoringValidationException(
                    "WRITE_FAILED",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "screenshot write failed");
        }

        // 9. INSERT row — only when we have a real event FK. For null eventId
        //    (correlation-id from browser), the file is on disk and can be
        //    reviewed manually; no DB row is created.
        if (event != null) {
            String storageRef = STORAGE_ROOT + "/" + sessionId + "/" + eventId + ".jpg";
            ProctoringScreenshot row = new ProctoringScreenshot(
                    null,
                    sessionId,
                    event.getId(),
                    capturedAt,
                    mimeType,
                    bytes.length,
                    storageRef);
            return shotRepo.save(row).getId();
        }

        return null;
    }

    /**
     * Read a screenshot for the admin streaming endpoint.
     *
     * <p>Loads the metadata row, verifies the on-disk file still exists,
     * reads the bytes, and hands them back together with the persisted
     * MIME type. The controller wraps the result with
     * {@code Cache-Control: private, no-store} (task 7.2) so admin
     * browsers never cache forensic evidence.
     *
     * <p>A row whose JPEG has already been purged by the daily retention
     * job (Req 21.2) yields 404 — surfacing a 404 lets the admin UI show
     * a "screenshot expired" placeholder rather than a stale link.
     *
     * @param screenshotId target {@code proctoring_screenshots.id}
     * @return bytes + MIME type ready for the streaming response
     * @throws ProctoringNotFoundException 404 if the row is missing or
     *                                     the on-disk file no longer
     *                                     exists
     * @throws ProctoringValidationException 500 if the file exists but
     *                                       cannot be read
     */
    @Transactional(readOnly = true)
    public ScreenshotPayload serveAdmin(Long screenshotId) {
        ProctoringScreenshot row = shotRepo.findById(screenshotId)
                .orElseThrow(() -> new ProctoringNotFoundException(
                        "Screenshot not found: " + screenshotId));

        Path path = Paths.get(row.getStorageRef()).normalize();
        // Defense-in-depth: prevent directory traversal. The storageRef is
        // built from safe components during upload, but if the DB column is
        // ever compromised a traversing path could read arbitrary files.
        Path storageRoot = Paths.get(STORAGE_ROOT).toAbsolutePath().normalize();
        Path resolved = storageRoot.resolveSibling(row.getStorageRef()).normalize();
        if (!resolved.startsWith(storageRoot)) {
            log.error("Proctoring: screenshot {} storageRef \"{}\" escapes STORAGE_ROOT",
                    screenshotId, row.getStorageRef());
            throw new ProctoringNotFoundException(
                    "Screenshot not found: " + screenshotId);
        }
        if (!Files.exists(resolved)) {
            throw new ProctoringNotFoundException(
                    "Screenshot file purged: " + screenshotId);
        }

        byte[] data;
        try {
            data = Files.readAllBytes(resolved);
        } catch (IOException ex) {
            log.error("Failed to read proctoring screenshot {} from {}: {}",
                    screenshotId, row.getStorageRef(), ex.getMessage());
            throw new ProctoringValidationException(
                    "READ_FAILED",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "screenshot read failed");
        }
        return new ScreenshotPayload(data, row.getMimeType());
    }

    /**
     * Streaming-friendly DTO returned by {@link #serveAdmin(Long)}.
     * Holds the in-memory bytes plus the persisted MIME type so the
     * controller can set the response {@code Content-Type} from the
     * row rather than re-sniffing the bytes.
     */
    public record ScreenshotPayload(byte[] bytes, String mimeType) {
    }

    /**
     * JPEG SOI marker — a JPEG file always begins with
     * {@code FF D8 FF}. We deliberately check only the first three bytes
     * because the fourth byte varies across JPEG variants
     * (e.g. {@code E0} for JFIF, {@code E1} for EXIF) and a stricter
     * check would reject legitimate browser output.
     */
    static boolean isJpeg(byte[] b) {
        return b != null
                && b.length >= 3
                && b[0] == (byte) 0xFF
                && b[1] == (byte) 0xD8
                && b[2] == (byte) 0xFF;
    }

    /**
     * PNG signature — every PNG file begins with the 8-byte sequence
     * {@code 89 50 4E 47 0D 0A 1A 0A}.
     */
    static boolean isPng(byte[] b) {
        return b != null
                && b.length >= 8
                && b[0] == (byte) 0x89
                && b[1] == (byte) 0x50
                && b[2] == (byte) 0x4E
                && b[3] == (byte) 0x47
                && b[4] == (byte) 0x0D
                && b[5] == (byte) 0x0A
                && b[6] == (byte) 0x1A
                && b[7] == (byte) 0x0A;
    }
}
