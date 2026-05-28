package com.example.codecombat2026.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Composite primary key for {@link ContestProblem}.
 *
 * <p>Mirrors the {@code (contest_id, problem_id)} primary key on the
 * {@code contest_problems} junction table. Lombok's {@code @Data} generates
 * the {@code equals} and {@code hashCode} required by JPA for {@code @IdClass}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContestProblemId implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long contestId;
    private Long problemId;
}
