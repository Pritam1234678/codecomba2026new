package com.example.codecombat2026.scheduler;

import com.example.codecombat2026.entity.Contest;
import com.example.codecombat2026.entity.PrivateContest;
import com.example.codecombat2026.entity.PrivateContestParticipant;
import com.example.codecombat2026.entity.Submission;
import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.repository.ContestRepository;
import com.example.codecombat2026.repository.PrivateContestParticipantRepository;
import com.example.codecombat2026.repository.SubmissionRepository;
import com.example.codecombat2026.repository.UserRepository;
import com.example.codecombat2026.service.ContestService;
import com.example.codecombat2026.service.PrivateContestCacheService;
import com.example.codecombat2026.service.PrivateContestEmailService;
import com.example.codecombat2026.service.PrivateContestLeaderboardService;
import com.example.codecombat2026.service.SseEmitterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.example.codecombat2026.util.TimeUtil;
import java.util.List;
import java.util.Optional;

@Component
public class ContestStatusScheduler {

    private static final Logger log = LoggerFactory.getLogger(ContestStatusScheduler.class);

    @Autowired private ContestRepository contestRepository;
    @Autowired private ContestService contestService; // for cache eviction
    @Autowired private SseEmitterRegistry sseRegistry;
    @Autowired private com.example.codecombat2026.repository.PrivateContestRepository privateContestRepository;
    @Autowired private PrivateContestCacheService privateContestCacheService;
    @Autowired private PrivateContestLeaderboardService privateContestLeaderboardService;
    @Autowired private PrivateContestEmailService privateContestEmailService;
    @Autowired private PrivateContestParticipantRepository privateContestParticipantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private com.example.codecombat2026.config.PrivateContestMetricsConfig metricsConfig;
    
    @Value("${APP_URL:https://codecoder.in}")
    private String appUrl;

    @Scheduled(fixedRate = 300000) // every 5 minutes
    public void updateContestStatuses() {
        LocalDateTime now = TimeUtil.now();
        List<Contest> contests = contestRepository.findAll();
        boolean anyChanged = false;

        for (Contest contest : contests) {
            if (contest.getStartTime() == null || contest.getEndTime() == null) continue;

            Contest.ContestStatus oldStatus = contest.getStatus();
            Contest.ContestStatus newStatus;

            if (now.isBefore(contest.getStartTime())) {
                newStatus = Contest.ContestStatus.UPCOMING;
            } else if (now.isAfter(contest.getEndTime())) {
                newStatus = Contest.ContestStatus.ENDED;
                if (Boolean.TRUE.equals(contest.getActive())) {
                    contest.setActive(false);
                    log.info("Auto-deactivated ended contest: {}", contest.getName());
                }
            } else {
                newStatus = Contest.ContestStatus.LIVE;
            }

            if (oldStatus != newStatus) {
                contest.setStatus(newStatus);
                contestRepository.save(contest);
                // Evict cache so users see updated status immediately
                contestService.evictContest(contest.getId());
                
                // Check if this is a private contest
                Optional<PrivateContest> privateContestOpt = privateContestRepository.findByContestId(contest.getId());
                if (privateContestOpt.isPresent()) {
                    PrivateContest privateContest = privateContestOpt.get();
                    
                    // Invalidate private contest cache
                    privateContestCacheService.invalidateContestCache(contest.getId());
                    log.debug("Invalidated private contest cache for contest {}", contest.getId());
                    
                    // Handle private contest lifecycle transitions
                    handlePrivateContestTransition(contest, privateContest, oldStatus, newStatus);
                }
                
                anyChanged = true;
                log.info("Contest '{}' status: {} → {}", contest.getName(), oldStatus, newStatus);
            }
        }

        // If any contest changed, evict the active list cache too
        if (anyChanged) {
            contestService.evictContestCache();
        }
        
        // Update Prometheus metrics for active contests and participants
        updateMetrics();
    }
    
    /**
     * Update Prometheus gauge metrics for active contests and total participants.
     * Called after every status update cycle.
     * 
     * Requirements: 39.1, 39.2
     */
    private void updateMetrics() {
        try {
            // Count active (LIVE) private contests
            long activeCount = contestRepository.countByStatus(Contest.ContestStatus.LIVE);
            metricsConfig.setActiveContestsCount(activeCount);
            
            // Count total participants across all private contests
            long totalParticipants = privateContestParticipantRepository.count();
            metricsConfig.setTotalParticipantsCount(totalParticipants);
            
            log.debug("Updated metrics: activeContests={}, totalParticipants={}", 
                activeCount, totalParticipants);
        } catch (Exception e) {
            log.error("Failed to update Prometheus metrics: {}", e.getMessage(), e);
            // Don't throw - metric update failures should not block scheduler
        }
    }
    
    /**
     * Handle private contest lifecycle transitions and send notifications.
     * 
     * Requirements: 12.4, 12.5, 17.3, 17.4
     * 
     * @param contest The Contest entity that transitioned
     * @param privateContest The PrivateContest entity
     * @param oldStatus Previous contest status
     * @param newStatus New contest status
     */
    private void handlePrivateContestTransition(Contest contest, PrivateContest privateContest, 
                                               Contest.ContestStatus oldStatus, Contest.ContestStatus newStatus) {
        try {
            // UPCOMING → LIVE: Initialize leaderboard and send start notification
            if (oldStatus == Contest.ContestStatus.UPCOMING && newStatus == Contest.ContestStatus.LIVE) {
                handlePrivateContestStart(contest, privateContest);
            }
            
            // LIVE → ENDED: Persist leaderboard and send end notification
            if (oldStatus == Contest.ContestStatus.LIVE && newStatus == Contest.ContestStatus.ENDED) {
                handlePrivateContestEnd(contest, privateContest);
            }
        } catch (Exception e) {
            log.error("Failed to handle private contest transition for contestId={}: {}", 
                contest.getId(), e.getMessage(), e);
            // Don't throw - lifecycle transition failures should not block scheduler
        }
    }
    
    /**
     * Handle private contest start (UPCOMING → LIVE transition).
     * 
     * - Initialize private:leaderboard:{contestId} cache key
     * - Send email notification to host with dashboard link
     * 
     * Requirements: 12.4, 17.3
     * 
     * @param contest The Contest entity
     * @param privateContest The PrivateContest entity
     */
    private void handlePrivateContestStart(Contest contest, PrivateContest privateContest) {
        Long contestId = contest.getId();
        
        // Initialize leaderboard cache (Requirement 12.4)
        try {
            privateContestLeaderboardService.initializeLeaderboard(contestId);
            log.info("Initialized leaderboard for private contest: contestId={}", contestId);
        } catch (Exception e) {
            log.error("Failed to initialize leaderboard for private contest {}: {}", 
                contestId, e.getMessage(), e);
        }
        
        // Send email notification to host (Requirement 17.3)
        try {
            User host = privateContest.getHostUser();
            Long hostUserId = null;
            
            // Get host user ID even if proxy
            if (host != null) {
                hostUserId = host.getId();
            }
            
            // If host is a lazy proxy without email loaded, fetch from repository
            if (hostUserId != null && (host.getEmail() == null || host.getUsername() == null)) {
                host = userRepository.findById(hostUserId).orElse(null);
            }
            
            // Only send email if we have a fully loaded host with email
            if (host != null && host.getEmail() != null && !host.getEmail().isBlank()) {
                long participantCountLong = privateContestParticipantRepository.countByContestId(contestId);
                int participantCount = (participantCountLong > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) participantCountLong;
                String dashboardUrl = appUrl + "/contests/private/" + contestId + "/manage";
                
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String startTime = contest.getStartTime().format(formatter);
                String endTime = contest.getEndTime().format(formatter);
                
                privateContestEmailService.sendContestStartedEmail(
                    host,
                    contestId,
                    contest.getName(),
                    startTime,
                    endTime,
                    participantCount,
                    dashboardUrl
                );
                
                log.info("Sent contest start notification to host for private contest: contestId={}, hostId={}", 
                    contestId, host.getId());
            } else {
                log.warn("Host not found for private contest: contestId={}", contestId);
            }
        } catch (Exception e) {
            log.error("Failed to send start notification for private contest {}: {}", 
                contestId, e.getMessage(), e);
        }
    }
    
    /**
     * Handle private contest end (LIVE → ENDED transition).
     * 
     * - Persist leaderboard from cache to database
     * - Send email notification to host with analytics link and summary
     * 
     * Requirements: 12.5, 17.4
     * 
     * @param contest The Contest entity
     * @param privateContest The PrivateContest entity
     */
    private void handlePrivateContestEnd(Contest contest, PrivateContest privateContest) {
        Long contestId = contest.getId();
        
        // Persist leaderboard to database (Requirement 12.5)
        try {
            privateContestLeaderboardService.persistLeaderboard(contestId);
            log.info("Persisted final leaderboard for private contest: contestId={}", contestId);
        } catch (Exception e) {
            log.error("Failed to persist leaderboard for private contest {}: {}", 
                contestId, e.getMessage(), e);
        }
        
        // Send email notification to host (Requirement 17.4)
        try {
            User host = privateContest.getHostUser();
            Long hostUserId = null;
            
            // Get host user ID even if proxy
            if (host != null) {
                hostUserId = host.getId();
            }
            
            // If host is a lazy proxy without email loaded, fetch from repository
            if (hostUserId != null && (host.getEmail() == null || host.getUsername() == null)) {
                host = userRepository.findById(hostUserId).orElse(null);
            }
            
            // Only send email if we have a fully loaded host with email
            if (host != null && host.getEmail() != null && !host.getEmail().isBlank()) {
                long participantCountLong = privateContestParticipantRepository.countByContestId(contestId);
                int participantCount = (participantCountLong > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) participantCountLong;
                long submissionCountLong = submissionRepository.countByContestId(contestId);
                int submissionCount = (submissionCountLong > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) submissionCountLong;
                String analyticsUrl = appUrl + "/contests/private/" + contestId + "/analytics";
                
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String endTime = contest.getEndTime().format(formatter);
                
                privateContestEmailService.sendContestEndedEmail(
                    host,
                    contestId,
                    contest.getName(),
                    endTime,
                    participantCount,
                    submissionCount,
                    analyticsUrl
                );
                
                log.info("Sent contest end notification to host for private contest: contestId={}, hostId={}", 
                    contestId, host.getId());
            } else {
                log.warn("Host not found for private contest: contestId={}", contestId);
            }
        } catch (Exception e) {
            log.error("Failed to send end notification for private contest {}: {}", 
                contestId, e.getMessage(), e);
        }
    }

    /** Keep SSE connections alive — send heartbeat every 25 seconds */
    @Scheduled(fixedRate = 25000)
    public void sseHeartbeat() {
        sseRegistry.sendHeartbeat();
    }
}
