package com.example.codecombat2026.proctoring.service;

import com.example.codecombat2026.proctoring.entity.ProctoringScreenshot;
import com.example.codecombat2026.proctoring.repository.ProctoringEventRepository;
import com.example.codecombat2026.proctoring.repository.ProctoringScreenshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;

/**
 * Daily 30-day retention sweep for the proctoring artifact tables.
 *
 * <p>Runs once per day at 03:00 Asia/Kolkata (3 AM IST — outside contest
 * hours; the platform is India-only). On each run it deletes both
 * {@code proctoring_screenshots} rows and the JPEG files referenced by
 * {@code storage_ref} in fixed batches of 500, then bulk-deletes
 * {@code proctoring_events} older than the same 30-day cutoff.
 *
 * <p>Design rationale:
 * <ul>
 *   <li>The cutoff is computed once per run as
 *       {@code LocalDateTime.now().minusDays(30)} so screenshots and events
 *       use a single consistent boundary for the sweep (Req 21.2).</li>
 *   <li>Screenshots are paged so memory stays bounded regardless of
 *       backlog. Each iteration always asks for page 0 — once the rows
 *       in that page are deleted, what was page 1 becomes page 0, so a
 *       fresh {@code PageRequest.of(0, 500)} is correct (Req 14.6).</li>
 *   <li>Per-row {@link Files#deleteIfExists(java.nio.file.Path)} is wrapped
 *       in a try/catch so a single bad path (already-purged file,
 *       permission error, etc.) does not abort the whole batch. The
 *       row is still deleted in the subsequent {@code deleteAllInBatch}
 *       call so the DB does not accumulate dangling references; orphan
 *       files are logged and can be reaped by ops, but the 30-day
 *       retention window itself is honoured at the row level.</li>
 *   <li>Events use a JPQL bulk delete (Req 21.3) so we never load
 *       entities into the persistence context just to evict them.</li>
 * </ul>
 *
 * <p>Validates: Req 21.2, 21.3, 21.4.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProctoringRetentionJob {

    private static final int BATCH_SIZE = 500;
    private static final int RETENTION_DAYS = 30;

    private final ProctoringScreenshotRepository shotRepo;
    private final ProctoringEventRepository eventRepo;

    /**
     * Daily retention sweep at 03:00 IST.
     *
     * <p>Cron expression {@code 0 0 3 * * *} fires at second 0, minute 0,
     * hour 3 every day; {@code zone = "Asia/Kolkata"} pins the schedule to
     * IST regardless of JVM default timezone (Req 21.4).
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Kolkata")
    public void purge() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
        int totalShots = 0;

        while (true) {
            Page<ProctoringScreenshot> batch =
                    shotRepo.findByCapturedAtBefore(cutoff, PageRequest.of(0, BATCH_SIZE));
            if (batch.isEmpty()) {
                break;
            }
            for (ProctoringScreenshot s : batch.getContent()) {
                try {
                    Files.deleteIfExists(Paths.get(s.getStorageRef()));
                } catch (IOException e) {
                    // One bad path must not abort the rest of the batch.
                    log.warn("Failed to delete proctoring screenshot file {}: {}",
                            s.getStorageRef(), e.getMessage());
                }
            }
            shotRepo.deleteAllInBatch(batch.getContent());
            totalShots += batch.getNumberOfElements();
        }

        int totalEvents = eventRepo.deleteByServerTimestampBefore(cutoff);

        log.info("Proctoring retention purge: {} screenshots, {} events",
                totalShots, totalEvents);
    }
}
