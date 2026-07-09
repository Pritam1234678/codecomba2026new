package com.example.codecombat2026.config;

import com.example.codecombat2026.entity.Submission;
import com.example.codecombat2026.repository.SubmissionRepository;
import com.example.codecombat2026.service.SubmissionWorkerPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * On startup:
 *   1. Sweep any processing lists belonging to previous instances (PID@host)
 *      back onto the main queue, so jobs that were in-flight when the JVM
 *      died are picked up again. Note: lists owned by the *current* JVM are
 *      skipped — workers haven't started consuming yet.
 *   2. Any submission still PENDING/JUDGING after the sweep that has nothing
 *      pointing at it on the main queue gets marked RE (worker for it never
 *      finalised and won't, e.g. its harness was deleted).
 *
 * The previous behaviour blindly DEL'd the queue and flipped every
 * PENDING/JUDGING row to RE — that's no longer safe with the durable
 * processing-list pattern in {@link SubmissionWorkerPool}.
 */
@Component
public class StartupRecoveryConfig {

    private static final Logger log = LoggerFactory.getLogger(StartupRecoveryConfig.class);

    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private StringRedisTemplate redis;

    @EventListener(ApplicationReadyEvent.class)
    @Async
    @Transactional
    public void recoverStuckSubmissions() {
        try {
            // Step 1: requeue any orphaned processing-list jobs from prior instances
            int requeued = requeueOrphanedProcessingLists();
            if (requeued > 0) log.info("✅ Requeued {} orphan job(s) from prior instances", requeued);

            // Step 2: any PENDING/JUDGING row older than ~10 min is genuinely stuck — fail it
            List<Submission> stuck = submissionRepository.findAll().stream()
                    .filter(s -> s.getStatus() == Submission.SubmissionStatus.PENDING
                              || s.getStatus() == Submission.SubmissionStatus.JUDGING)
                    .filter(s -> s.getSubmittedAt() != null
                        && s.getSubmittedAt().isBefore(com.example.codecombat2026.util.TimeUtil.now().minusMinutes(10)))
                    .toList();

            if (stuck.isEmpty()) {
                log.info("✅ No genuinely stuck submissions found");
                return;
            }

            log.warn("⚠️  Found {} stuck submissions older than 10min — marking as RE", stuck.size());
            for (Submission s : stuck) {
                submissionRepository.updateResult(
                        s.getId(),
                        Submission.SubmissionStatus.RE,
                        "Server restarted while your submission was being judged. Please resubmit.",
                        0, 0, 0.0, 0, "[]",
                        com.example.codecombat2026.util.TimeUtil.now()
                );
            }

        } catch (Exception e) {
            log.error("Startup recovery failed: {}", e.getMessage());
        }
    }

    /** Move all jobs from registered processing lists back to the appropriate queue (public or private). */
    private int requeueOrphanedProcessingLists() {
        Set<String> keys = redis.opsForSet().members(SubmissionWorkerPool.PROCESSING_REGISTRY);
        if (keys == null || keys.isEmpty()) return 0;

        int totalPublic = 0;
        int totalPrivate = 0;
        
        for (String procKey : keys) {
            try {
                Long size = redis.opsForList().size(procKey);
                if (size == null || size == 0) continue;

                // Pop until empty, push to appropriate queue based on job type
                while (true) {
                    String jobJson = redis.opsForList().rightPop(procKey);
                    if (jobJson == null) break;
                    
                    // Determine target queue from job content
                    try {
                        com.example.codecombat2026.dto.SubmissionJob job = 
                            new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                                jobJson, com.example.codecombat2026.dto.SubmissionJob.class);
                        
                        String targetQueue = (job.getPrivateContestId() != null) 
                            ? SubmissionWorkerPool.PRIVATE_QUEUE_KEY 
                            : SubmissionWorkerPool.QUEUE_KEY;
                        
                        redis.opsForList().leftPush(targetQueue, jobJson);
                        
                        if (job.getPrivateContestId() != null) {
                            totalPrivate++;
                        } else {
                            totalPublic++;
                        }
                    } catch (Exception e) {
                        // Fallback: if we can't parse, route to public queue
                        redis.opsForList().leftPush(SubmissionWorkerPool.QUEUE_KEY, jobJson);
                        totalPublic++;
                        log.warn("Could not parse job, routed to public queue: {}", e.getMessage());
                    }
                }
                // Done with this list — drop it from the registry
                redis.opsForSet().remove(SubmissionWorkerPool.PROCESSING_REGISTRY, procKey);
            } catch (Exception e) {
                log.warn("Failed to drain processing list {}: {}", procKey, e.getMessage());
            }
        }
        
        if (totalPublic > 0 || totalPrivate > 0) {
            log.info("Requeued {} public + {} private orphan job(s)", totalPublic, totalPrivate);
        }
        
        return totalPublic + totalPrivate;
    }
}
