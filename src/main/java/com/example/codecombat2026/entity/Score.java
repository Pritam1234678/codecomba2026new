package com.example.codecombat2026.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "scores", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_id", "contest_id" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Score {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_id", nullable = false)
    private Contest contest;

    private Double totalScore;

    // Time penalty or sum of execution times? usually penalty is submission time -
    // contest start time
    private Long totalTimePenalty;

    private Integer problemsSolved;
}
