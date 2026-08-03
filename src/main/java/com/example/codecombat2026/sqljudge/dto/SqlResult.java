package com.example.codecombat2026.sqljudge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Normalized, deterministic representation of a query result:
 *
 * <pre>
 * { "columns": ["department", "count"],
 *   "rows":    [["HR", "5"], ["IT", "10"]] }
 * </pre>
 *
 * Every cell is a canonical string produced by SqlResultNormalizer, so
 * comparison is safe across JDBC representation differences (numeric 10.00,
 * timestamps, NULL, booleans).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SqlResult {
    private List<String> columns = new ArrayList<>();
    private List<List<String>> rows = new ArrayList<>();
    /** True when the result was truncated at maxResultRows. */
    private boolean truncated = false;

    public SqlResult(List<String> columns, List<List<String>> rows) {
        this.columns = columns;
        this.rows = rows;
    }
}
