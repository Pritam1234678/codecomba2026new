package com.example.codecombat2026.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Pushed to the user's browser via SSE when judging completes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerdictEvent {
    private Long submissionId;
    private String status;        // AC, WA, CE, RE, TLE
    private int testCasesPassed;
    private int totalTestCases;
    private int score;            // 0-100
    private long timeConsumedMs;
    private String errorMessage;  // null unless CE/RE
    private String testCaseDetails; // JSON array of TC results
    private boolean testRun;      // true = test run, not saved to DB
    private boolean practice;     // true = practice mode submission
    private int pointsAwarded;    // points earned (practice first AC)
    private boolean alreadySolved; // already solved before (practice)
}
