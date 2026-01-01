package com.example.codecombat2026.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import com.example.codecombat2026.util.TimeUtil;

@Entity
@Table(name = "code_snippets", uniqueConstraints = @UniqueConstraint(columnNames = { "problem_id", "language" }))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodeSnippet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    @JsonIgnore
    private Problem problem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProgrammingLanguage language;

    @Column(name = "starter_code", columnDefinition = "TEXT", nullable = false)
    private String starterCode;

    @Column(name = "solution_template", columnDefinition = "TEXT", nullable = false)
    private String solutionTemplate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ProgrammingLanguage {
        JAVA,
        CPP,
        PYTHON,
        JAVASCRIPT,
        C
    }

    @PrePersist
    protected void onCreate() {
        createdAt = TimeUtil.now();
        updatedAt = TimeUtil.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = TimeUtil.now();
    }
}
