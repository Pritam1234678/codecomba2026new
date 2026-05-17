package com.example.codecombat2026.config;

import com.example.codecombat2026.entity.Submission;
import com.example.codecombat2026.repository.SubmissionRepository;
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

/**
 * On startup, recover any submissions that were stuck in PENDING or JUDGING
 * state from a previous server crash or restart.
 *
 * Also clears the Valkey submission queue so stale jobs don't get re-processed.
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
            // Find all submissions stuck in PENDING or JUDGING
            List<Submission> stuck = submissionRepository.findAll().stream()
                    .filter(s -> s.getStatus() == Submission.SubmissionStatus.PENDING
                              || s.getStatus() == Submission.SubmissionStatus.JUDGING)
                    .toList();

            if (stuck.isEmpty()) {
                log.info("✅ No stuck submissions found");
                return;
            }

            log.warn("⚠️  Found {} stuck submissions — marking as RE (server was restarted)", stuck.size());

            for (Submission s : stuck) {
                submissionRepository.updateResult(
                        s.getId(),
                        Submission.SubmissionStatus.RE,
                        "Server was restarted while your submission was being judged. Please resubmit.",
                        0, 0, 0.0, 0, "[]",
                        com.example.codecombat2026.util.TimeUtil.now()
                );
            }

            log.info("✅ Recovered {} stuck submissions", stuck.size());

            // Clear the Valkey queue — stale jobs from before restart
            Long cleared = redis.opsForList().size("submission:queue");
            if (cleared != null && cleared > 0) {
                redis.delete("submission:queue");
                log.info("✅ Cleared {} stale jobs from submission queue", cleared);
            }

        } catch (Exception e) {
            log.error("Startup recovery failed: {}", e.getMessage());
        }
    }
}
