package com.example.codecombat2026.proctoring.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 1:1 extension marker on a {@code contests} row.
 *
 * <p>Maps to {@code proctored_contests} (V7). Existence of a row for a given
 * {@code contest_id} is the only authoritative signal that a contest runs in
 * proctored mode (Req 1.6) — there is no {@code proctored} flag on the
 * {@code contests} table itself, which keeps the existing {@code Contest}
 * entity untouched per the design's "by-id only, no JPA association" rule.
 *
 * <p>{@code consent_version} lets ops bump the consent text and force a
 * fresh acknowledgment from every candidate (Req 2.6).
 */
@Entity
@Table(
    name = "proctored_contests",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_proctored_contests_contest_id",
        columnNames = "contest_id"
    )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProctoredContest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "contest_id", nullable = false)
    private Long contestId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "consent_version", nullable = false)
    private Integer consentVersion;
}
