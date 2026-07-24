package com.example.codecombat2026.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "practice_sheets")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PracticeSheet {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String company;
    @Column(columnDefinition = "TEXT")
    private String description;
    private String tags;
    private Boolean active = true;
    private LocalDateTime createdAt;
}
