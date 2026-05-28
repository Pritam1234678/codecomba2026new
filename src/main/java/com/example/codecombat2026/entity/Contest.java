package com.example.codecombat2026.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.example.codecombat2026.util.TimeUtil;

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

    /**
     * Inverse-only navigation side of the M:N relation between contests and
     * problems. The owning side is {@link Problem#contests} which maps the
     * {@code contest_problems} junction table.
     *
     * <p><b>Do not mutate this collection directly.</b> All attach/detach
     * operations MUST go through {@code ContestProblemService} so the
     * dual-write bookkeeping against the legacy {@code problems.contest_id}
     * column runs. Calling {@code contest.getProblems().add(...)} bypasses
     * that invariant and will leave the database in an inconsistent state.
     */
    @ManyToMany(mappedBy = "contests", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Problem> problems = new ArrayList<>();

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
        LocalDateTime now = TimeUtil.now();

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
