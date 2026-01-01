package com.example.codecombat2026.scheduler;

import com.example.codecombat2026.entity.Contest;
import com.example.codecombat2026.repository.ContestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import com.example.codecombat2026.util.TimeUtil;
import java.util.List;

/**
 * Scheduled task to automatically update contest status
 * Runs every 5 minutes to check and update contest statuses
 */
@Component
public class ContestStatusScheduler {

    @Autowired
    private ContestRepository contestRepository;

    /**
     * Update contest statuses every 5 minutes
     * Checks all contests and updates their status based on current time
     */
    @Scheduled(fixedRate = 300000) // 5 minutes = 300,000 milliseconds
    public void updateContestStatuses() {
        LocalDateTime now = TimeUtil.now();
        List<Contest> contests = contestRepository.findAll();

        for (Contest contest : contests) {
            if (contest.getStartTime() != null && contest.getEndTime() != null) {
                Contest.ContestStatus oldStatus = contest.getStatus();
                Contest.ContestStatus newStatus;

                if (now.isBefore(contest.getStartTime())) {
                    newStatus = Contest.ContestStatus.UPCOMING;
                } else if (now.isAfter(contest.getEndTime())) {
                    newStatus = Contest.ContestStatus.ENDED;

                    // Auto-deactivate contest when it ends
                    if (contest.getActive()) {
                        contest.setActive(false);
                        System.out.println("Auto-deactivated ended contest: " + contest.getName());
                    }
                } else {
                    newStatus = Contest.ContestStatus.LIVE;
                }

                // Only update if status changed
                if (oldStatus != newStatus) {
                    contest.setStatus(newStatus);
                    contestRepository.save(contest);
                    System.out.println("Updated contest '" + contest.getName() + "' status from " + oldStatus + " to "
                            + newStatus);
                }
            }
        }
    }
}
