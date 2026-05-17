package com.example.codecombat2026.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntry {
    private Long userId;
    private String userName;
    private String userRoll;
    private Double totalScore; // Changed to Double for average with decimals
    private Integer problemsSolved;
    private Integer rank;
}
