package com.example.codecombat2026.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionResult {
    private String stdout;
    private String stderr;
    private long timeTaken; // in ms
    private long memoryUsed; // in KB (approx)
    private int exitCode;

    // Status inferred from exit code and time
    private boolean isTimeLimitExceeded;
    private boolean isMemoryLimitExceeded;
    private boolean isCompilationError;
}
