package com.example.codecombat2026.proctoring.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Metadata row for a screenshot captured on a triggering event.
 *
 * <p>Maps to {@code proctoring_screenshots} (V7). The actual JPEG bytes
 * live on disk under {@code uploads/proctoring/sessions/{session_id}/}
 * and are referenced via {@link #storageRef}; raw bytes are never stored
 * in the database (Req 14.3).
 */
@Entity
@Table(name = "proctoring_screenshots")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProctoringScreenshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "captured_at", nullable = false)
    private LocalDateTime capturedAt;

    @Column(name = "mime_type", nullable = false, length = 32)
    private String mimeType;

    @Column(name = "byte_size", nullable = false)
    private Integer byteSize;

    /** On-disk path relative to the application root, never the bytes themselves. */
    @Column(name = "storage_ref", nullable = false, length = 255)
    private String storageRef;
}
