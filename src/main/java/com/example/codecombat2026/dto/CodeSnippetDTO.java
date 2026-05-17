package com.example.codecombat2026.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for CodeSnippet.
 *
 * When returned to users (public endpoint):
 *   - solutionTemplate is null (never exposed)
 *   - starterCode contains only the code between USER_CODE_START / USER_CODE_END markers
 *
 * When returned to admins (admin endpoint):
 *   - solutionTemplate contains the full harness
 *   - starterCode is null (not stored separately — extracted from harness on the fly)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodeSnippetDTO {
    private Long id;
    private Long problemId;
    private String language;

    /**
     * User-facing: the code between USER_CODE_START and USER_CODE_END markers.
     * Null when returned to admins (they work with the full harness).
     */
    private String starterCode;

    /**
     * Admin-facing: the full harness file.
     * Null when returned to users (security — never expose test cases).
     */
    private String solutionTemplate;
}
