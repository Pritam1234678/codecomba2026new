package com.example.codecombat2026.entity;

import com.example.codecombat2026.util.TimeUtil;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "problem_complaints",
    indexes = {
        @Index(name = "idx_complaints_user", columnList = "user_id"),
        @Index(name = "idx_complaints_status", columnList = "status"),
        @Index(name = "idx_complaints_problem", columnList = "problem_id")
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProblemComplaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "problem_id", nullable = false)
    private Long problemId;

    @Column(name = "contest_id")
    private Long contestId;

    @Column(name = "complaint_type", nullable = false, length = 50)
    private String complaintType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(length = 20, nullable = false)
    private String status = "PENDING";

    @Column(name = "admin_response", columnDefinition = "TEXT")
    private String adminResponse;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = TimeUtil.now();
        updatedAt = TimeUtil.now();
        if (status == null) status = "PENDING";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = TimeUtil.now();
    }
}
