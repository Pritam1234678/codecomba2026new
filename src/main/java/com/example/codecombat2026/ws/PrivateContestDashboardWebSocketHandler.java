package com.example.codecombat2026.ws;

import com.example.codecombat2026.entity.Contest;
import com.example.codecombat2026.entity.PrivateContest;
import com.example.codecombat2026.entity.Submission;
import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.repository.PrivateContestParticipantRepository;
import com.example.codecombat2026.repository.PrivateContestRepository;
import com.example.codecombat2026.repository.SubmissionRepository;
import com.example.codecombat2026.repository.UserRepository;
import com.example.codecombat2026.security.jwt.JwtUtils;
import com.example.codecombat2026.service.PrivateContestAccessValidator;
import com.example.codecombat2026.ws.DashboardUpdateFrame.LeaderboardEntryDTO;
import com.example.codecombat2026.ws.DashboardUpdateFrame.RecentSubmissionDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * WebSocket handler for private contest real-time dashboard updates.
 * 
 * Lifecycle:
 * - afterConnectionEstablished: Authenticate user via JWT token from query parameter,
 *   validate user is the contest host, start periodic dashboard updates every 5 seconds
 * - handleTextMessage: Not used - this is a server-push-only connection
 * - afterConnectionClosed: Cancel periodic updates and cleanup
 * 
 * Dashboard updates include:
 * - Current participant count
 * - Total submission count
 * - Top 10 leaderboard entries
 * - Recent 10 submissions
 * 
 * Updates are pushed every 5 seconds while the connection is active.
 * 
 * Requirements: 32.1, 32.2, 32.3, 32.4
 */
@Component
public class PrivateContestDashboardWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(PrivateContestDashboardWebSocketHandler.class);

    private static final int UPDATE_INTERVAL_SECONDS = 5;
    private static final int TOP_LEADERBOARD_SIZE = 10;
    private static final int RECENT_SUBMISSIONS_SIZE = 10;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PrivateContestAccessValidator accessValidator;

    @Autowired
    private PrivateContestRepository privateContestRepository;

    @Autowired
    private PrivateContestParticipantRepository participantRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Scheduler for periodic dashboard updates (5-second intervals).
     */
    private ScheduledExecutorService updateScheduler;

    /**
     * Tracks active dashboard sessions: contestId -> (sessionId -> ScheduledFuture).
     * Allows multiple hosts/admins to watch the same contest simultaneously.
     */
    private final ConcurrentHashMap<Long, Map<String, ScheduledFuture<?>>> activeSessions = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        AtomicLong counter = new AtomicLong();
        ThreadFactory tf = r -> {
            Thread t = new Thread(r, "dashboard-update-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
        this.updateScheduler = Executors.newScheduledThreadPool(2, tf);
        log.info("Private contest dashboard WebSocket handler initialized");
    }

    @PreDestroy
    void shutdown() {
        if (updateScheduler == null) return;
        
        // Cancel all scheduled updates
        for (Map<String, ScheduledFuture<?>> sessions : activeSessions.values()) {
            for (ScheduledFuture<?> future : sessions.values()) {
                future.cancel(false);
            }
        }
        activeSessions.clear();
        
        try {
            updateScheduler.shutdown();
            if (!updateScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                updateScheduler.shutdownNow();
            }
        } catch (InterruptedException ie) {
            updateScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Private contest dashboard WebSocket handler shut down");
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        try {
            // Extract contest ID from URI path: /ws/contests/private/{contestId}/dashboard
            Long contestId = extractContestId(session);
            if (contestId == null) {
                closeQuietly(session, CloseStatus.BAD_DATA, "Invalid contest ID in path");
                return;
            }

            // Extract and validate JWT token from query parameter
            String token = extractToken(session);
            if (token == null || !jwtUtils.validateJwtToken(token)) {
                closeQuietly(session, new CloseStatus(4401), "Unauthorized: Invalid or missing token");
                return;
            }

            // Get user from token
            String username = jwtUtils.getUserNameFromJwtToken(token);
            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null) {
                closeQuietly(session, new CloseStatus(4401), "Unauthorized: User not found");
                return;
            }

            Long userId = user.getId();

            // Validate user is the host or admin
            boolean isAdmin = user.getRoles().stream()
                    .anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));
            if (!accessValidator.isHost(contestId, userId) && !isAdmin) {
                closeQuietly(session, new CloseStatus(4403), "Forbidden: Not the contest host");
                return;
            }

            // Verify contest exists
            PrivateContest privateContest = privateContestRepository.findByContestId(contestId).orElse(null);
            if (privateContest == null) {
                closeQuietly(session, CloseStatus.BAD_DATA, "Contest not found");
                return;
            }

            log.info("Dashboard WebSocket connected: contestId={}, user={}, sessionId={}", 
                    contestId, username, session.getId());

            // Store session metadata
            session.getAttributes().put("contestId", contestId);
            session.getAttributes().put("userId", userId);
            session.getAttributes().put("username", username);

            // Send immediate update
            sendDashboardUpdate(session, contestId);

            // Schedule periodic updates every 5 seconds
            ScheduledFuture<?> updateTask = updateScheduler.scheduleAtFixedRate(
                    () -> sendDashboardUpdate(session, contestId),
                    UPDATE_INTERVAL_SECONDS,
                    UPDATE_INTERVAL_SECONDS,
                    TimeUnit.SECONDS
            );

            // Track the scheduled task for this session
            activeSessions.computeIfAbsent(contestId, k -> new ConcurrentHashMap<>())
                    .put(session.getId(), updateTask);

        } catch (Exception e) {
            log.error("Error establishing dashboard WebSocket connection: {}", e.getMessage(), e);
            closeQuietly(session, CloseStatus.SERVER_ERROR, "Internal server error");
        }
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) {
        // This is a server-push-only connection. Clients should not send messages.
        // Ignore any incoming messages silently.
        log.debug("Received unexpected message on dashboard WebSocket: sessionId={}", session.getId());
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        Long contestId = (Long) session.getAttributes().get("contestId");
        String username = (String) session.getAttributes().get("username");
        
        if (contestId != null) {
            // Cancel scheduled updates for this session
            Map<String, ScheduledFuture<?>> sessions = activeSessions.get(contestId);
            if (sessions != null) {
                ScheduledFuture<?> task = sessions.remove(session.getId());
                if (task != null) {
                    task.cancel(false);
                }
                // Clean up empty map
                if (sessions.isEmpty()) {
                    activeSessions.remove(contestId);
                }
            }
        }
        
        log.info("Dashboard WebSocket disconnected: contestId={}, user={}, sessionId={}, status={}", 
                contestId, username, session.getId(), status);
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) {
        log.debug("Dashboard WebSocket transport error: sessionId={}, error={}", 
                session.getId(), exception.getMessage());
    }

    /**
     * Send a dashboard update to the connected client.
     */
    private void sendDashboardUpdate(WebSocketSession session, Long contestId) {
        if (!session.isOpen()) {
            return;
        }

        try {
            // Get participant count
            int participantCount = (int) participantRepository.countByContestId(contestId);

            // Get submission count
            int submissionCount = (int) submissionRepository.countByContest_Id(contestId);

            // Get top 10 leaderboard
            List<LeaderboardEntryDTO> topLeaderboard = getTopLeaderboard(contestId);

            // Get recent 10 submissions
            List<RecentSubmissionDTO> recentSubmissions = getRecentSubmissions(contestId);

            // Create and send update frame
            DashboardUpdateFrame frame = DashboardUpdateFrame.of(
                    contestId,
                    participantCount,
                    submissionCount,
                    topLeaderboard,
                    recentSubmissions
            );

            String json = objectMapper.writeValueAsString(frame);
            session.sendMessage(new TextMessage(json));

        } catch (Exception e) {
            log.error("Error sending dashboard update for contestId={}: {}", contestId, e.getMessage(), e);
            // Don't close the connection on error - client will retry on next interval
        }
    }

    /**
     * Get top 10 leaderboard entries from Redis cache.
     */
    private List<LeaderboardEntryDTO> getTopLeaderboard(Long contestId) {
        try {
            String leaderboardKey = "private:leaderboard:" + contestId;
            Set<ZSetOperations.TypedTuple<String>> entries = redis.opsForZSet()
                    .reverseRangeWithScores(leaderboardKey, 0, TOP_LEADERBOARD_SIZE - 1);

            if (entries == null || entries.isEmpty()) {
                return List.of();
            }

            List<LeaderboardEntryDTO> leaderboard = new ArrayList<>();
            int rank = 1;
            for (ZSetOperations.TypedTuple<String> entry : entries) {
                String userId = entry.getValue();
                Double score = entry.getScore();
                
                if (userId == null || score == null) continue;

                // Extract score and penalty from cached format
                // Format: score is stored as (actualScore * 1000 - penalty)
                int actualScore = (int) (score / 1000);
                int penalty = (int) (actualScore * 1000 - score);

                // Get username from score metadata key
                String username = getUsernameFromCache(contestId, userId);

                // Count solved problems from score
                int problemsSolved = actualScore / 100; // Assuming 100 points per problem

                leaderboard.add(new LeaderboardEntryDTO(
                        rank++,
                        username != null ? username : "User#" + userId,
                        actualScore,
                        penalty,
                        problemsSolved
                ));
            }

            return leaderboard;

        } catch (Exception e) {
            log.error("Error fetching leaderboard for contestId={}: {}", contestId, e.getMessage());
            return List.of();
        }
    }

    /**
     * Get username from Redis cache or return default.
     */
    private String getUsernameFromCache(Long contestId, String userId) {
        try {
            String key = "private:leaderboard:" + contestId + ":user:" + userId;
            String username = redis.opsForValue().get(key);
            return username != null ? username : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get recent 10 submissions from database.
     */
    private List<RecentSubmissionDTO> getRecentSubmissions(Long contestId) {
        try {
            PageRequest pageRequest = PageRequest.of(0, RECENT_SUBMISSIONS_SIZE);
            List<Submission> submissions = submissionRepository.findTop10ByContest_IdOrderBySubmittedAtDesc(contestId);
            
            // Limit to 10
            submissions = submissions.size() > RECENT_SUBMISSIONS_SIZE 
                ? submissions.subList(0, RECENT_SUBMISSIONS_SIZE) 
                : submissions;
            
            List<RecentSubmissionDTO> recent = new ArrayList<>();
            for (Submission sub : submissions) {
                User user = sub.getUser();
                String username = user != null ? user.getUsername() : "Unknown";
                
                String problemTitle = sub.getProblem() != null ? sub.getProblem().getTitle() : "Unknown";
                
                String status = sub.getStatus() != null ? sub.getStatus().name() : "PENDING";
                
                recent.add(new RecentSubmissionDTO(
                        sub.getId(),
                        username,
                        problemTitle,
                        status,
                        sub.getSubmittedAt()
                ));
            }

            return recent;

        } catch (Exception e) {
            log.error("Error fetching recent submissions for contestId={}: {}", contestId, e.getMessage());
            return List.of();
        }
    }

    /**
     * Extract contest ID from WebSocket URI path.
     */
    private Long extractContestId(WebSocketSession session) {
        try {
            String path = session.getUri().getPath();
            // Path format: /ws/contests/private/{contestId}/dashboard
            String[] parts = path.split("/");
            if (parts.length >= 5) {
                return Long.parseLong(parts[4]);
            }
        } catch (Exception e) {
            log.debug("Failed to extract contest ID from path: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Extract JWT token from query parameter.
     */
    private String extractToken(WebSocketSession session) {
        try {
            String query = session.getUri().getQuery();
            if (query != null) {
                Map<String, String> params = UriComponentsBuilder.newInstance()
                        .query(query)
                        .build()
                        .getQueryParams()
                        .toSingleValueMap();
                return params.get("token");
            }
        } catch (Exception e) {
            log.debug("Failed to extract token from query: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Close WebSocket session quietly without throwing exceptions.
     */
    private void closeQuietly(WebSocketSession session, CloseStatus status, String reason) {
        try {
            if (session.isOpen()) {
                session.close(new CloseStatus(status.getCode(), reason));
            }
        } catch (IOException e) {
            log.debug("Error closing WebSocket session: {}", e.getMessage());
        }
    }
}
