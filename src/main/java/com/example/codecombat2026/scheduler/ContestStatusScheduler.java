package com.example.codecombat2026.scheduler;

import com.example.codecombat2026.entity.Contest;
import com.example.codecombat2026.repository.ContestRepository;
import com.example.codecombat2026.service.ContestService;
import com.example.codecombat2026.service.SseEmitterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import com.example.codecombat2026.util.TimeUtil;
import java.util.List;

@Component
public class ContestStatusScheduler {

    private static final Logger log = LoggerFactory.getLogger(ContestStatusScheduler.class);

    @Autowired private ContestRepository contestRepository;
    @Autowired private ContestService contestService; // for cache eviction
    @Autowired private SseEmitterRegistry sseRegistry;

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
                anyChanged = true;
                log.info("Contest '{}' status: {} → {}", contest.getName(), oldStatus, newStatus);
            }
        }

        // If any contest changed, evict the active list cache too
        if (anyChanged) {
            contestService.evictContestCache();
        }
    }

    /** Keep SSE connections alive — send heartbeat every 25 seconds */
    @Scheduled(fixedRate = 25000)
    public void sseHeartbeat() {
        sseRegistry.sendHeartbeat();
    }
}
