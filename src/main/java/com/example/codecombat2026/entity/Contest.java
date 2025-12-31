package com.example.codecombat2026.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "contests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Contest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    private ContestStatus status;

    @Column(nullable = false)
    private Boolean active = false;

    public enum ContestStatus {
        UPCOMING,
        LIVE,
        ENDED
    }

    /**
     * Automatically calculate and set contest status based on current time
     * Called before insert and update operations
     */
    @PrePersist
    @PreUpdate
    public void calculateStatus() {
        LocalDateTime now = LocalDateTime.now();

        if (startTime != null && endTime != null) {
            if (now.isBefore(startTime)) {
                // Current time is before start time -> UPCOMING
                this.status = ContestStatus.UPCOMING;
            } else if (now.isAfter(endTime)) {
                // Current time is after end time -> ENDED
                this.status = ContestStatus.ENDED;
            } else {
                // Current time is between start and end -> LIVE
                this.status = ContestStatus.LIVE;
            }
        }
    }
}
