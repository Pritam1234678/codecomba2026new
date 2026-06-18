package com.example.codecombat2026.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "web_contest_templates",
       indexes = {
           @Index(name = "idx_web_contest_templates_problem_lang", columnList = "problem_id, language")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebContestTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "problem_id", nullable = false)
    private Long problemId;

    @Column(name = "language", nullable = false, length = 20)
    private String language;

    @Column(name = "template_path", nullable = false, length = 500)
    private String templatePath;

    @Column(name = "manifest_json", columnDefinition = "TEXT", nullable = false)
    private String manifestJson;

    @Column(name = "test_count", nullable = false)
    private Integer testCount = 6;

    @Column(name = "timeout_seconds", nullable = false)
    private Integer timeoutSeconds = 60;

    @Column(name = "memory_mb", nullable = false)
    private Integer memoryMb = 512;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
