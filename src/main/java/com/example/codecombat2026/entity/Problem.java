package com.example.codecombat2026.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Contest contest;

    @Column(name = "contest_id", insertable = false, updatable = false)
    private Long contestId;

    @Column(nullable = false)
    private Boolean active = true;

    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TestCase> testCases;

    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CodeSnippet> codeSnippets;
}
