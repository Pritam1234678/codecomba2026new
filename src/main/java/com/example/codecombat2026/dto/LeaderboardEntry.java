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
    private Double totalScore;
    private Integer problemsSolved;
    private Integer rank;
    private String photoUrl;
}
