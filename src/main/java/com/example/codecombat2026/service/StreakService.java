package com.example.codecombat2026.service;

import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.repository.UserRepository;
import com.example.codecombat2026.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class StreakService {

    private static final Logger log = LoggerFactory.getLogger(StreakService.class);

    @Autowired private UserRepository userRepository;

    public void updateStreak(Long userId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) return;

            LocalDate today = TimeUtil.now().toLocalDate();
            LocalDate lastActive = user.getLastActiveDate();
            int current = user.getCurrentStreak() != null ? user.getCurrentStreak() : 0;
            int max = user.getMaxStreak() != null ? user.getMaxStreak() : 0;

            if (lastActive == null) {
                current = 1;
            } else if (lastActive.equals(today)) {
                // Already active today — no change
                return;
            } else if (lastActive.equals(today.minusDays(1))) {
                // Consecutive day
                current++;
            } else {
                // Break in streak — reset
                current = 1;
            }

            max = Math.max(max, current);
            user.setCurrentStreak(current);
            user.setMaxStreak(max);
            user.setLastActiveDate(today);
            userRepository.save(user);

            log.debug("Streak updated for user {}: current={}, max={}", userId, current, max);
        } catch (Exception e) {
            log.warn("Streak update failed for user {}: {}", userId, e.getMessage());
        }
    }
}
