package com.example.codecombat2026.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "problems")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Problem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String inputFormat;

    @Column(columnDefinition = "TEXT")
    private String outputFormat;

    @Column(columnDefinition = "TEXT")
    private String constraints;

    // Time limit in seconds
    private Double timeLimit;

    // Memory limit in MB
    private Integer memoryLimit;

    // Optional examples (nullable)
    @Column(columnDefinition = "TEXT")
    private String example1;

    @Column(columnDefinition = "TEXT")
    private String example2;

    @Column(columnDefinition = "TEXT")
    private String example3;

    // Optional images - stores comma-separated URLs or JSON array (nullable)
    @Column(columnDefinition = "TEXT")
    private String images;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_id")
    @JsonIgnore
    private Contest contest;

    @Column(name = "contest_id", insertable = false, updatable = false)
    private Long contestId;

    /**
     * Canonical M:N relation between problems and contests, backed by the
     * {@code contest_problems} junction table. All application reads of a
     * problem's contest membership MUST go through this collection.
     *
     * <p>The legacy {@link #contest} / {@link #contestId} fields above are
     * deprecated transition-only mappings: they are dual-written
     * (best-effort) by {@code ContestProblemService} during the migration
     * window and MUST NOT be read by application code. A future migration
     * will drop the {@code problems.contest_id} column entirely.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "contest_problems",
            joinColumns = @JoinColumn(name = "problem_id"),
            inverseJoinColumns = @JoinColumn(name = "contest_id")
    )
    @JsonIgnore
    private List<Contest> contests = new ArrayList<>();

    @Column(nullable = false)
    private Boolean active = true;

    /**
     * Difficulty level: EASY, MEDIUM, HARD
     */
    @Column(nullable = false)
    private String level = "MEDIUM";

    /**
     * Code snippets for each language.
     * Each snippet contains:
     *   - starterCode: shown to the user in the editor
     *   - solutionTemplate: the full harness with embedded test cases (never sent to users)
     */
    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<CodeSnippet> codeSnippets;
}
