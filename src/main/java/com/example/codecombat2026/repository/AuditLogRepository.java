package com.example.codecombat2026.repository;

import com.example.codecombat2026.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for AuditLog entity.
 * 
 * Provides data access methods for audit log management including:
 * - Querying logs by user, action, resource type, and date range
 * - Admin dashboard filtering and pagination
 * - Compliance reporting and investigation
 * - Automated cleanup of old logs (90+ days retention policy)
 * 
 * Requirements: 29.1, 29.2, 29.3
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    /**
     * Find all audit logs for a specific user.
     * Used for user activity tracking and compliance audits.
     * 
     * @param userId The ID of the user
     * @param pageable Pagination parameters
     * @return Page of AuditLog entities
     */
    Page<AuditLog> findByUserId(Long userId, Pageable pageable);
    
    /**
     * Find all audit logs for a specific action type.
     * Used for analyzing specific types of events (e.g., all contest creations).
     * 
     * @param action The action type (e.g., "CONTEST_CREATED")
     * @param pageable Pagination parameters
     * @return Page of AuditLog entities
     */
    Page<AuditLog> findByAction(String action, Pageable pageable);
    
    /**
     * Find all audit logs for a specific resource type.
     * Used for tracking all actions on a resource category (e.g., all PRIVATE_CONTEST actions).
     * 
     * @param resourceType The resource type (e.g., "PRIVATE_CONTEST")
     * @param pageable Pagination parameters
     * @return Page of AuditLog entities
     */
    Page<AuditLog> findByResourceType(String resourceType, Pageable pageable);
    
    /**
     * Find all audit logs for a specific resource instance.
     * Used for investigating actions on a particular contest, participant, etc.
     * 
     * @param resourceType The resource type
     * @param resourceId The resource ID
     * @param pageable Pagination parameters
     * @return Page of AuditLog entities
     */
    Page<AuditLog> findByResourceTypeAndResourceId(
        String resourceType, 
        Long resourceId, 
        Pageable pageable
    );
    
    /**
     * Find audit logs within a date range.
     * Used for compliance reporting and time-bounded investigations.
     * 
     * @param startDate Start of date range (inclusive)
     * @param endDate End of date range (exclusive)
     * @param pageable Pagination parameters
     * @return Page of AuditLog entities
     */
    Page<AuditLog> findByTimestampBetween(
        LocalDateTime startDate, 
        LocalDateTime endDate, 
        Pageable pageable
    );
    
    /**
     * Complex query with multiple optional filters.
     * Used by admin audit log endpoint for flexible searching.
     * 
     * @param userId Optional user ID filter
     * @param action Optional action filter
     * @param resourceType Optional resource type filter
     * @param startDate Optional start date (inclusive)
     * @param endDate Optional end date (exclusive)
     * @param pageable Pagination parameters
     * @return Page of AuditLog entities
     */
    @Query("SELECT al FROM AuditLog al WHERE " +
           "(:userId IS NULL OR al.user.id = :userId) AND " +
           "(:action IS NULL OR al.action = :action) AND " +
           "(:resourceType IS NULL OR al.resourceType = :resourceType) AND " +
           "(:startDate IS NULL OR al.timestamp >= :startDate) AND " +
           "(:endDate IS NULL OR al.timestamp < :endDate) " +
           "ORDER BY al.timestamp DESC")
    Page<AuditLog> findByFilters(
        @Param("userId") Long userId,
        @Param("action") String action,
        @Param("resourceType") String resourceType,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
    
    /**
     * Delete audit logs older than the specified date.
     * Used by scheduled cleanup job to enforce 90-day retention policy.
     * 
     * @param cutoffDate Logs older than this date will be deleted
     * @return Number of logs deleted
     */
    long deleteByTimestampBefore(LocalDateTime cutoffDate);
    
    /**
     * Count audit logs older than the specified date.
     * Used to report how many logs will be deleted before cleanup.
     * 
     * @param cutoffDate Cutoff date for old logs
     * @return Count of logs older than cutoff date
     */
    long countByTimestampBefore(LocalDateTime cutoffDate);
    
    /**
     * Find the most recent audit logs for a specific resource.
     * Used for quick history view in admin dashboards.
     * 
     * @param resourceType The resource type
     * @param resourceId The resource ID
     * @param pageable Pagination parameters (use PageRequest.of(0, limit) for top N)
     * @return List of recent AuditLog entities
     */
    @Query("SELECT al FROM AuditLog al WHERE " +
           "al.resourceType = :resourceType AND al.resourceId = :resourceId " +
           "ORDER BY al.timestamp DESC")
    List<AuditLog> findRecentByResource(
        @Param("resourceType") String resourceType,
        @Param("resourceId") Long resourceId,
        Pageable pageable
    );
    
    /**
     * Find all audit logs for a specific user and action type.
     * Used for tracking specific user behaviors (e.g., how many times a user regenerated invite links).
     * 
     * @param userId The user ID
     * @param action The action type
     * @return List of AuditLog entities
     */
    List<AuditLog> findByUserIdAndAction(Long userId, String action);
    
    /**
     * Count audit logs for a specific action within a date range.
     * Used for analytics and reporting (e.g., total contests created this month).
     * 
     * @param action The action type
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Count of matching logs
     */
    long countByActionAndTimestampBetween(
        String action, 
        LocalDateTime startDate, 
        LocalDateTime endDate
    );
}
