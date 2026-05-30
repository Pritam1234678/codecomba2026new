package com.example.codecombat2026.repository;

import com.example.codecombat2026.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);

    Long countByEnabled(Boolean enabled);

    Optional<User> findByEmail(String email);

    /**
     * Search non-admin users by username or full name (case-insensitive).
     * Excludes users who have ROLE_ADMIN.
     */
    @Query("""
        SELECT DISTINCT u FROM User u
        WHERE u.enabled = true
          AND NOT EXISTS (
              SELECT r FROM u.roles r WHERE r.name = 'ROLE_ADMIN'
          )
          AND (
              LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%'))
          )
        ORDER BY u.username ASC
        """)
    Page<User> searchNonAdminUsers(@Param("q") String q, Pageable pageable);
}
