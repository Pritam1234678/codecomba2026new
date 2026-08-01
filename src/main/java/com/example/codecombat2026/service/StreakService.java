package com.example.codecombat2026.service;

import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.repository.UserRepository;
import com.example.codecombat2026.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class StreakService {

    private static final Logger log = LoggerFactory.getLogger(StreakService.class);

    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbc;

    // ── Reactive update (called on new AC submission) ───────────────────────
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
                return;  // Already active today
            } else if (lastActive.equals(today.minusDays(1))) {
                current++;  // Consecutive day
            } else {
                current = 1;  // Break in streak
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

    // ── Full rebuild from history (called on startup / on demand) ───────────
    @Transactional
    public void rebuildAllStreaks() {
        log.info("Starting full streak rebuild...");

        // 1) Compute max_streak from ALL submissions (contest + practice)
        String maxStreakSql = """
            WITH all_submissions AS (
                SELECT user_id, submitted_at::date AS day
                FROM submissions WHERE status = 'AC' AND is_test_run = false
                UNION
                SELECT user_id, submitted_at::date AS day
                FROM practice_submissions WHERE status = 'AC'
            ),
            user_days AS (
                SELECT user_id, day,
                    day - (row_number() OVER (PARTITION BY user_id ORDER BY day))::int AS grp
                FROM (SELECT DISTINCT user_id, day FROM all_submissions) d
            ),
            streak_lengths AS (
                SELECT user_id, grp, COUNT(*) AS cnt, MAX(day) AS last_day
                FROM user_days GROUP BY user_id, grp
            ),
            max_streaks AS (
                SELECT user_id, MAX(cnt) AS max_streak, MAX(last_day) AS last_active_date
                FROM streak_lengths GROUP BY user_id
            )
            UPDATE users u SET
                max_streak = COALESCE(m.max_streak, 0),
                last_active_date = m.last_active_date
            FROM max_streaks m WHERE u.id = m.user_id
            """;
        jdbc.execute(maxStreakSql);

        // 2) Compute current_streak (consecutive days ending today/yesterday)
        String currentStreakSql = """
            WITH all_submissions AS (
                SELECT user_id, submitted_at::date AS day
                FROM submissions WHERE status = 'AC' AND is_test_run = false
                UNION
                SELECT user_id, submitted_at::date AS day
                FROM practice_submissions WHERE status = 'AC'
            ),
            streak_groups AS (
                SELECT user_id, day,
                    day - (row_number() OVER (PARTITION BY user_id ORDER BY day))::int AS grp
                FROM (SELECT DISTINCT user_id, day FROM all_submissions) d
            ),
            current_counts AS (
                SELECT user_id, COUNT(*) AS cur
                FROM streak_groups sg
                WHERE sg.grp = (
                    SELECT grp FROM streak_groups sg2
                    WHERE sg2.user_id = sg.user_id
                    ORDER BY day DESC LIMIT 1
                )
                GROUP BY user_id
            )
            UPDATE users u SET current_streak = COALESCE(cc.cur, 0)
            FROM current_counts cc WHERE u.id = cc.user_id
            """;
        jdbc.execute(currentStreakSql);

        // Reset nulls to 0
        jdbc.update("UPDATE users SET current_streak = 0 WHERE current_streak IS NULL");
        jdbc.update("UPDATE users SET max_streak = 0 WHERE max_streak IS NULL");

        log.info("Streak rebuild completed for all users");
    }

    // ── Daily scheduled rebuild (runs at 00:30 IST) ─────────────────────────
    @Scheduled(cron = "0 30 0 * * *", zone = "Asia/Kolkata")
    public void dailyStreakRebuild() {
        rebuildAllStreaks();
    }

    // ── Force rebuild for a single user (admin API) ─────────────────────────
    @Transactional
    public void rebuildUserStreak(Long userId) {
        String sql = """
            WITH all_submissions AS (
                SELECT user_id, submitted_at::date AS day
                FROM submissions WHERE status = 'AC' AND is_test_run = false
                UNION
                SELECT user_id, submitted_at::date AS day
                FROM practice_submissions WHERE status = 'AC'
            ),
            streak_groups AS (
                SELECT user_id, day,
                    day - (row_number() OVER (PARTITION BY user_id ORDER BY day))::int AS grp
                FROM (SELECT DISTINCT user_id, day FROM all_submissions) d
            ),
            max_streaks AS (
                SELECT user_id, MAX(cnt) AS max_streak, MAX(last_day) AS last_active_date
                FROM (
                    SELECT user_id, grp, COUNT(*) AS cnt, MAX(day) AS last_day
                    FROM (
                        SELECT user_id, day,
                            day - (row_number() OVER (PARTITION BY user_id ORDER BY day))::int AS grp
                        FROM (SELECT DISTINCT user_id, day FROM all_submissions) d
                    ) sub GROUP BY user_id, grp
                ) g
                GROUP BY user_id
            ),
            current_counts AS (
                SELECT user_id, COUNT(*) AS cur
                FROM streak_groups sg
                WHERE sg.grp = (
                    SELECT grp FROM streak_groups sg2
                    WHERE sg2.user_id = sg.user_id
                    ORDER BY day DESC LIMIT 1
                )
                GROUP BY user_id
            )
            UPDATE users u SET
                max_streak = COALESCE(m.max_streak, 0),
                current_streak = COALESCE(cc.cur, 0),
                last_active_date = m.last_active_date
            FROM max_streaks m
            JOIN current_counts cc ON cc.user_id = m.user_id
            WHERE u.id = m.user_id
            """;
        int updated = jdbc.update(sql, userId);
        log.info("Rebuilt streak for user {}: updated={}", userId, updated);
    }
}