package com.example.codecombat2026.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import com.example.codecombat2026.util.TimeUtil;

/**
 * Stores the full harness code for a problem in a specific language.
 *
 * The harness is a single complete runnable file that contains:
 *   1. Data structures, helper classes, test runner
 *   2. The user-editable zone, delimited by:
 *        // USER_CODE_START
 *        ... (default/empty function signature)
 *        // USER_CODE_END
 *   3. Test cases embedded as code (no stdin/stdout comparison)
 *      — each test prints TC:N:PASS or TC:N:FAIL to stdout
 *
 * The backend extracts the code between USER_CODE_START and USER_CODE_END
 * to show the user in the editor. On submission, the user's code replaces
 * that section and the full harness is executed.
 */
@Entity
@Table(name = "code_snippets",
       uniqueConstraints = @UniqueConstraint(columnNames = {"problem_id", "language"}),
       indexes = {
           @Index(name = "idx_snippet_problem_lang", columnList = "problem_id, language")
       })
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

    /**
     * The full harness file. Contains USER_CODE_START / USER_CODE_END markers.
     * Never sent to users directly — only the section between the markers is exposed.
     */
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
