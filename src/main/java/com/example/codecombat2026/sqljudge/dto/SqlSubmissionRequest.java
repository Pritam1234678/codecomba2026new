package com.example.codecombat2026.sqljudge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for both RUN and SUBMIT of candidate SQL.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SqlSubmissionRequest {
    private String sql;
}
