package com.example.codecombat2026.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Tracks problems solved in practice mode (independent of contests).
 * One row per (user, problem) — first AC wins, no duplicates.
 */
@Entity
@Table(name = "user_problem_solved",
       uniqueConstraints = @UniqueConstraint(name = "uk_user_problem", columnNames = {"user_id", "problem_id"}),
       indexes = {
           @Index(name = "idx_ups_user", columnList = "user_id"),
           @Index(name = "idx_ups_problem", columnList = "problem_id")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProblemSolved {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "problem_id", nullable = false)
    private Long problemId;

    @Column(name = "solved_at", nullable = false)
    private LocalDateTime solvedAt;

    @Column(name = "points_earned", nullable = false)
    private Integer pointsEarned;
}
