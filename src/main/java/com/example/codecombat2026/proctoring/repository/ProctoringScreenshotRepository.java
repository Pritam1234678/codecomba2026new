package com.example.codecombat2026.proctoring.repository;

import com.example.codecombat2026.proctoring.entity.ProctoringScreenshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for {@link ProctoringScreenshot}.
 *
 * <p>Two access patterns:
 * <ul>
 *   <li>{@link #findBySessionIdOrderByCapturedAtAsc(Long)} — admin
 *       review of a candidate's screenshot timeline, in capture order.</li>
 *   <li>{@link #findByCapturedAtBefore(LocalDateTime, Pageable)} — paged
 *       retrieval used by the retention job so disk cleanup can iterate
 *       in batches (Req 14.6, 21.2) without ever loading the full
 *       expired set into memory.</li>
 * </ul>
 *
 * <p>The cutoff parameter is {@link LocalDateTime} (not {@link java.time.Instant})
 * because {@code captured_at} is mapped as {@code LocalDateTime};
 * conversion from {@code Instant} happens at the service boundary.
 */
@Repository
public interface ProctoringScreenshotRepository extends JpaRepository<ProctoringScreenshot, Long> {

    /**
     * All screenshots for a session, oldest first. Drives the admin
     * timeline view alongside the event log.
     *
     * @param sessionId owning {@code proctoring_sessions.id}
     * @return screenshots ordered ascending by {@code captured_at}
     */
    List<ProctoringScreenshot> findBySessionIdOrderByCapturedAtAsc(Long sessionId);

    /**
     * Paged sweep of screenshots older than {@code cutoff}. Used by the
     * retention job (Req 21.2) so the on-disk JPEG files can be deleted
     * in fixed-size batches and the corresponding rows removed without
     * blowing up heap.
     *
     * @param cutoff   exclusive upper bound on {@code captured_at}
     * @param pageable batch size + ordering supplied by the caller
     * @return one page of matching screenshots
     */
    Page<ProctoringScreenshot> findByCapturedAtBefore(LocalDateTime cutoff, Pageable pageable);
}
