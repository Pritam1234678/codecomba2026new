package com.example.codecombat2026.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "submissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Submission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_id")
    private Contest contest;

    @Column(columnDefinition = "TEXT")
    private String code;

    @Enumerated(EnumType.STRING)
    private ProgrammingLanguage language;

    @Enumerated(EnumType.STRING)
    private SubmissionStatus status;

    private LocalDateTime submittedAt;

    // Execution stats
    private Double timeConsumed; // ms

    // For partially correct answers if needed, or simple pass/fail info logic
    private Integer testCasesPassed;
    private Integer totalTestCases;

    // Score out of 100 based on test cases passed
    private Integer score;

    // Store compilation or runtime error messages
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    // Store detailed test case results (JSON format)
    // Format: [{"testCase": 1, "status": "PASS", "hidden": false}, ...]
    @Column(columnDefinition = "TEXT")
    private String testCaseDetails;

    // Store user and problem details for easy access
    @Column(name = "user_name")
    private String userName;

    @Column(name = "user_roll")
    private String userRoll;

    @Column(name = "problem_name")
    private String problemName;

    // Helper methods to expose IDs for JSON serialization without loading lazy
    // entities
    public Long getUserId() {
        return user != null ? user.getId() : null;
    }

    public Long getProblemId() {
        return problem != null ? problem.getId() : null;
    }

    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
    }

    public enum ProgrammingLanguage {
        JAVA,
        PYTHON,
        CPP,
        C,
        JAVASCRIPT
    }

    public enum SubmissionStatus {
        PENDING,
        JUDGING,
        AC, // Accepted
        WA, // Wrong Answer
        TLE, // Time Limit Exceeded
        RE, // Runtime Error
        CE, // Compilation Error
        MLE // Memory Limit Exceeded
    }
}
