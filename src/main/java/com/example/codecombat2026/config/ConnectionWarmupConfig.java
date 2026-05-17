package com.example.codecombat2026.config;

import com.example.codecombat2026.controller.AdminDashboardController;
import com.example.codecombat2026.repository.ContestRepository;
import com.example.codecombat2026.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Warms up DB + Valkey connections and pre-populates caches at startup.
 * First user request gets instant response instead of paying cold-start penalty.
 */
@Component
public class ConnectionWarmupConfig {

    private static final Logger log = LoggerFactory.getLogger(ConnectionWarmupConfig.class);

    @Autowired private DataSource dataSource;
    @Autowired private StringRedisTemplate redis;
    @Autowired private ContestRepository contestRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ObjectMapper objectMapper;

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void warmUpConnections() {
        // 1. Warm up HikariCP — open minimum-idle connections to Railway MySQL
        log.info("🔥 Warming up DB connections...");
        for (int i = 0; i < 3; i++) {
            try (Connection conn = dataSource.getConnection()) {
                conn.createStatement().execute("SELECT 1");
            } catch (Exception e) {
                log.warn("DB warmup attempt {} failed: {}", i + 1, e.getMessage());
            }
        }
        log.info("✅ DB connections warmed up");

        // 2. Warm up Valkey connection
        log.info("🔥 Warming up Valkey connection...");
        try {
            redis.opsForValue().get("warmup:ping");
            redis.opsForValue().set("warmup:ping", "ok");
            log.info("✅ Valkey connection warmed up");
        } catch (Exception e) {
            log.warn("Valkey warmup failed: {}", e.getMessage());
        }

        // 3. Pre-populate contest cache
        log.info("🔥 Pre-loading contest cache...");
        try {
            long count = contestRepository.count();
            log.info("✅ Contest cache pre-loaded ({} contests)", count);
        } catch (Exception e) {
            log.warn("Contest cache pre-load failed: {}", e.getMessage());
        }

        // 4. Pre-populate admin dashboard stats cache
        // So first admin login gets instant dashboard load
        log.info("🔥 Pre-loading admin stats cache...");
        try {
            Map<String, Long> userStats = new HashMap<>();
            userStats.put("total",    userRepository.count());
            userStats.put("enabled",  userRepository.countByEnabled(true));
            userStats.put("disabled", userRepository.countByEnabled(false));

            Map<String, Long> contestStats = new HashMap<>();
            contestStats.put("total",    contestRepository.count());
            contestStats.put("active",   contestRepository.countByActive(true));
            contestStats.put("inactive", contestRepository.countByActive(false));

            Map<String, Object> statsPayload = new HashMap<>();
            statsPayload.put("userStats",    userStats);
            statsPayload.put("contestStats", contestStats);

            redis.opsForValue().set(
                AdminDashboardController.STATS_CACHE_KEY,
                objectMapper.writeValueAsString(statsPayload),
                Duration.ofMinutes(2)
            );
            log.info("✅ Admin stats cache pre-loaded (users={}, contests={})",
                userStats.get("total"), contestStats.get("total"));
        } catch (Exception e) {
            log.warn("Admin stats cache pre-load failed: {}", e.getMessage());
        }
    }
}
