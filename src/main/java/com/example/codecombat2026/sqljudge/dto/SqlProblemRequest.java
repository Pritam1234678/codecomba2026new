package com.example.codecombat2026.sqljudge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin input for creating (or re-provisioning) a SQL question. All fields are
 * data-driven — problem id to schema mapping is derived, never hardcoded.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SqlProblemRequest {
    private String title;
    private String description;
    private String setupSql;
    private String officialSolutionSql;
    /** ORDERED | UNORDERED */
    private String comparisonMode = "UNORDERED";
    private Integer timeLimitMs;
    private Integer maxResultRows;
}
