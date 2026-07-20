package com.example.codecombat2026.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "practice_submissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PracticeSubmission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "problem_id", nullable = false)
    private Long problemId;

    @Column(columnDefinition = "TEXT")
    private String code;

    @Enumerated(EnumType.STRING)
    private Submission.ProgrammingLanguage language;

    @Enumerated(EnumType.STRING)
    private Submission.SubmissionStatus status;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "time_consumed")
    private Double timeConsumed;

    @Column(name = "test_cases_passed")
    private Integer testCasesPassed;

    @Column(name = "total_test_cases")
    private Integer totalTestCases;

    private Integer score;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "test_case_details", columnDefinition = "TEXT")
    private String testCaseDetails;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "problem_name")
    private String problemName;
}
