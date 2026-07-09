package com.example.codecombat2026.scheduler;

import com.example.codecombat2026.entity.Contest;
import com.example.codecombat2026.entity.PrivateContest;
import com.example.codecombat2026.entity.PrivateContestParticipant;
import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.repository.ContestRepository;
import com.example.codecombat2026.repository.PrivateContestParticipantRepository;
import com.example.codecombat2026.repository.PrivateContestRepository;
import com.example.codecombat2026.repository.UserRepository;
import com.example.codecombat2026.service.PrivateContestEmailService;
import com.example.codecombat2026.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Scheduler for sending reminder notifications to participants of private contests.
 * 
 * Sends reminders at two intervals:
 * - 24 hours before contest start
 * - 1 hour before contest start
 * 
 * Uses Redis to track which reminders have been sent to avoid duplicates.
 * 
 * Requirements: 31.1, 31.2, 31.3, 31.4, 31.5
 * 
 * @see PrivateContestEmailService for email templates
 * @see ContestStatusScheduler for lifecycle management
 */
@Component
public class PrivateContestReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(PrivateContestReminderScheduler.class);

    @Autowired
    private ContestRepository contestRepository;

    @Autowired
    private PrivateContestRepository privateContestRepository;

    @Autowired
    private PrivateContestParticipantRepository participantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PrivateContestEmailService emailService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${private.contest.reminder.enabled:true}")
    private boolean reminderEnabled;

    // Redis key prefixes for tracking sent reminders
    private static final String REMINDER_24H_KEY_PREFIX = "private:contest:reminder:24h:";
    private static final String REMINDER_1H_KEY_PREFIX = "private:contest:reminder:1h:";

    /**
     * Scheduled task to send contest reminders to participants.
     * 
     * Runs every hour to check for contests that need reminders sent.
     * 
     * Requirement: 31.1, 31.2
     */
    @Scheduled(cron = "${private.contest.reminder.cron:0 0 * * * *}") // Every hour at minute 0
    public void sendContestReminders() {
        if (!reminderEnabled) {
            log.debug("Private contest reminders are disabled");
            return;
        }

        LocalDateTime now = TimeUtil.now();
        log.debug("Running private contest reminder scheduler at {}", now);

        try {
            // Find all UPCOMING private contests
            List<Contest> allContests = contestRepository.findByStatus(Contest.ContestStatus.UPCOMING);
            
            for (Contest contest : allContests) {
                // Check if this is a private contest
                Optional<PrivateContest> privateContestOpt = privateContestRepository.findByContestId(contest.getId());
                
                if (privateContestOpt.isEmpty()) {
                    continue; // Skip public contests
                }
                
                PrivateContest privateContest = privateContestOpt.get();
                
                // Skip cancelled contests
                if (Boolean.TRUE.equals(privateContest.getCancelled())) {
                    continue;
                }
                
                // Check if contest is within reminder window
                LocalDateTime startTime = contest.getStartTime();
                if (startTime == null) {
                    continue;
                }
                
                Duration timeUntilStart = Duration.between(now, startTime);
                long hoursUntilStart = timeUntilStart.toHours();
                
                // Send 24-hour reminder (23-25 hour window to handle hourly checks)
                if (hoursUntilStart >= 23 && hoursUntilStart <= 25) {
                    sendReminderIfNotSent(contest, privateContest, 24);
                }
                
                // Send 1-hour reminder (0.5-1.5 hour window)
                if (hoursUntilStart >= 0 && hoursUntilStart <= 1 && timeUntilStart.toMinutes() >= 30) {
                    sendReminderIfNotSent(contest, privateContest, 1);
                }
            }
        } catch (Exception e) {
            log.error("Error in private contest reminder scheduler: {}", e.getMessage(), e);
        }
    }

    /**
     * Send reminder email to all participants if not already sent.
     * 
     * Uses Redis to track which reminders have been sent to avoid duplicates.
     * 
     * @param contest The contest entity
     * @param privateContest The private contest entity
     * @param hoursBeforeStart Hours before start (24 or 1)
     */
    private void sendReminderIfNotSent(Contest contest, PrivateContest privateContest, int hoursBeforeStart) {
        Long contestId = contest.getId();
        String redisKey = getReminderRedisKey(contestId, hoursBeforeStart);
        
        // Check if reminder already sent
        Boolean alreadySent = redisTemplate.hasKey(redisKey);
        if (Boolean.TRUE.equals(alreadySent)) {
            log.debug("Reminder already sent for contestId={}, hours={}", contestId, hoursBeforeStart);
            return;
        }
        
        try {
            // Get all participants
            List<PrivateContestParticipant> participants = participantRepository.findByContestId(contestId);
            
            if (participants.isEmpty()) {
                log.debug("No participants to notify for contestId={}", contestId);
                // Still mark as sent to avoid repeated checks
                markReminderAsSent(redisKey, contest.getStartTime());
                return;
            }
            
            // Load participant users
            List<User> participantUsers = new ArrayList<>();
            for (PrivateContestParticipant participant : participants) {
                User user = participant.getUser();
                if (user != null) {
                    Long userId = user.getId();
                    if (userId != null) {
                        // If user is a lazy proxy, fetch from repository
                        if (user.getEmail() == null || user.getUsername() == null) {
                            Optional<User> userOpt = userRepository.findById(userId);
                            if (userOpt.isPresent()) {
                                user = userOpt.get();
                            }
                        }
                        // TODO: Check user preferences for opt-out (Requirement 31.5)
                        // For now, send to all participants
                        if (user.getEmail() != null && !user.getEmail().isBlank()) {
                            participantUsers.add(user);
                        }
                    }
                }
            }
            
            if (!participantUsers.isEmpty()) {
                // Send reminder emails (Requirement 31.1, 31.2)
                emailService.sendContestReminderEmail(participantUsers, contestId, hoursBeforeStart);
                
                log.info("Sent {} hour reminder for contestId={} to {} participants", 
                    hoursBeforeStart, contestId, participantUsers.size());
            }
            
            // Mark reminder as sent
            markReminderAsSent(redisKey, contest.getStartTime());
            
        } catch (Exception e) {
            log.error("Failed to send {} hour reminder for contestId={}: {}", 
                hoursBeforeStart, contestId, e.getMessage(), e);
        }
    }

    /**
     * Get Redis key for tracking reminder status.
     * 
     * @param contestId Contest ID
     * @param hoursBeforeStart Hours before start (24 or 1)
     * @return Redis key string
     */
    private String getReminderRedisKey(Long contestId, int hoursBeforeStart) {
        if (hoursBeforeStart == 24) {
            return REMINDER_24H_KEY_PREFIX + contestId;
        } else if (hoursBeforeStart == 1) {
            return REMINDER_1H_KEY_PREFIX + contestId;
        } else {
            throw new IllegalArgumentException("Invalid hoursBeforeStart: " + hoursBeforeStart);
        }
    }

    /**
     * Mark reminder as sent in Redis with expiration.
     * 
     * Key expires after contest start time to prevent clutter.
     * 
     * @param redisKey Redis key
     * @param contestStartTime Contest start time
     */
    private void markReminderAsSent(String redisKey, LocalDateTime contestStartTime) {
        try {
            // Set key with value "sent"
            redisTemplate.opsForValue().set(redisKey, "sent");
            
            // Calculate expiration: contest start time + 1 day (for cleanup)
            LocalDateTime now = TimeUtil.now();
            Duration ttl = Duration.between(now, contestStartTime.plusDays(1));
            
            if (ttl.isNegative() || ttl.isZero()) {
                // Contest already started or ended, expire in 1 hour
                redisTemplate.expire(redisKey, 1, TimeUnit.HOURS);
            } else {
                redisTemplate.expire(redisKey, ttl.toSeconds(), TimeUnit.SECONDS);
            }
            
            log.debug("Marked reminder as sent: {}", redisKey);
        } catch (Exception e) {
            log.error("Failed to mark reminder as sent in Redis: {}", e.getMessage(), e);
        }
    }
}
