package com.example.codecombat2026.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "sheet_problems", uniqueConstraints = @UniqueConstraint(columnNames = {"sheet_id", "problem_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SheetProblem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "sheet_id")
    private Long sheetId;
    @Column(name = "problem_id")
    private Long problemId;
    @Column(name = "sort_order")
    private Integer sortOrder = 0;
    @Column(name = "added_at")
    private LocalDateTime addedAt;
}
