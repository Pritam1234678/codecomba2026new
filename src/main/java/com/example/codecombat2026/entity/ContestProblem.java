package com.example.codecombat2026.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA entity for the {@code contest_problems} M:N junction table.
 *
 * <p>Modeled as an explicit {@code @Entity} (rather than just a
 * {@code @JoinTable}) because the junction carries its own payload columns
 * ({@code displayOrder}, {@code addedAt}) and is queried directly by
 * {@link com.example.codecombat2026.repository.ContestProblemRepository}.
 *
 * <p>The composite primary key {@code (contestId, problemId)} is supplied via
 * {@link ContestProblemId} and the {@link IdClass} mapping.
 *
 * <p>Both {@code @ManyToOne} relations use {@code insertable=false,
 * updatable=false} because the underlying FK columns are already mapped as
 * {@code @Id} fields; navigation is read-only and {@code @JsonIgnore} keeps
 * the entities out of REST responses to avoid serialization cycles.
 */
@Entity
@Table(name = "contest_problems")
@IdClass(ContestProblemId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContestProblem {

    @Id
    @Column(name = "contest_id")
    private Long contestId;

    @Id
    @Column(name = "problem_id")
    private Long problemId;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_id", insertable = false, updatable = false)
    @JsonIgnore
    private Contest contest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", insertable = false, updatable = false)
    @JsonIgnore
    private Problem problem;

    /**
     * Default {@code addedAt} to "now" if a caller did not set it explicitly,
     * so service code can simply construct {@code new ContestProblem(cid, pid)}
     * without worrying about the audit timestamp.
     */
    @PrePersist
    void onPrePersist() {
        if (addedAt == null) {
            addedAt = LocalDateTime.now();
        }
    }
}
