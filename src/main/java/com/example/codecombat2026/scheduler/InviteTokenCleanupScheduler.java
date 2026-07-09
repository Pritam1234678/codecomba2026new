package com.example.codecombat2026.scheduler;

import com.example.codecombat2026.repository.PrivateContestInvitationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Scheduler for cleaning up expired invite tokens from the private_contest_invitations table.
 * 
 * This scheduler periodically deletes invitation tokens that have expired, keeping the database
 * clean and preventing accumulation of stale data. The cleanup runs daily at 02:00 UTC by default,
 * configurable via the 'invite.token.cleanup.cron' property.
 * 
 * Business Rules:
 * - Deletes tokens where expiresAt < now()
 * - Runs as a transactional operation for consistency
 * - Logs the count of deleted tokens for monitoring
 * 
 * Requirements: 26.1, 26.2
 * 
 * Related:
 * - Task 6.1: PrivateInviteService manages token generation and validation
 * - Task 10.2: This scheduler automates cleanup of expired tokens
 */
@Component
public class InviteTokenCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(InviteTokenCleanupScheduler.class);

    @Autowired
    private PrivateContestInvitationRepository invitationRepository;

    /**
     * Clean up expired invite tokens.
     * 
     * Deletes all invitation records where the expiryDate has passed.
     * Runs daily at 02:00 UTC by default (configurable via application.properties).
     * 
     * The cron expression can be overridden using the application property invite.token.cleanup.cron
     * 
     * Default is 0 0 2 asterisk asterisk asterisk (02:00 UTC daily)
     * Format: second minute hour day month weekday
     * 
     * Requirements: 26.1, 26.2
     */
    @Scheduled(cron = "${invite.token.cleanup.cron:0 0 2 * * *}")
    @Transactional
    public void cleanupExpiredInvitations() {
        try {
            LocalDateTime now = LocalDateTime.now();
            log.debug("Starting expired invite token cleanup at {}", now);

            // Delete all invitations where expiresAt < now
            long deletedCount = invitationRepository.deleteByExpiresAtBefore(now);

            if (deletedCount > 0) {
                log.info("Cleaned up {} expired invite token(s)", deletedCount);
            } else {
                log.debug("No expired invite tokens found during cleanup");
            }
        } catch (Exception e) {
            // Log the error but don't propagate it - we don't want to break the scheduler
            // The next scheduled run will retry the cleanup
            log.error("Error during invite token cleanup: {}", e.getMessage(), e);
        }
    }
}
