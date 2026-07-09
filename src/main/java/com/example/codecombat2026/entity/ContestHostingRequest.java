package com.example.codecombat2026.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "contest_hosting_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContestHostingRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "intended_use_case", nullable = false, length = 50)
    private IntendedUseCase intendedUseCase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HostingRequestStatus status = HostingRequestStatus.PENDING;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum IntendedUseCase {
        EDUCATION,
        RECRUITMENT,
        COMMUNITY,
        INTERNAL_TRAINING,
        OTHER
    }

    public enum HostingRequestStatus {
        PENDING,
        APPROVED,
        REJECTED,
        REVOKED
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (submittedAt == null) {
            submittedAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
