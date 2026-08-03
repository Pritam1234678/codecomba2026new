package com.example.codecombat2026.sqljudge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for the candidate-facing problem list and detail. Never exposes
 * the schema name, setup SQL, expected result, or any Neon metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SqlProblemView {
    private Long id;
    private String title;
    private String description;

    public static SqlProblemView from(com.example.codecombat2026.sqljudge.entity.SqlProblem p) {
        return new SqlProblemView(p.getId(), p.getTitle(), p.getDescription());
    }

    public static List<SqlProblemView> fromAll(List<com.example.codecombat2026.sqljudge.entity.SqlProblem> problems) {
        return problems.stream().map(SqlProblemView::from).toList();
    }
}
