package com.example.codecombat2026.sqljudge.comparator;

import com.example.codecombat2026.sqljudge.dto.SqlResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Compares a candidate result against the expected result.
 *
 * <p>Both sides are already normalized to canonical strings, so comparison is
 * pure value equality. Behavior:
 *
 * <ul>
 *   <li><b>ORDERED</b> — row order matters: rows are compared positionally.</li>
 *   <li><b>UNORDERED</b> — row order does not matter: each row is flattened to
 *       a stable key and both row-sets are sorted before comparing.</li>
 * </ul>
 *
 * <p>Column count must match in both modes (the expected result defines the
 * required column shape). Column names themselves are NOT compared — matching
 * on the aliases/names would make equivalent queries with different aliases
 * fail; the authoritative check is the data shape (column count + values).
 */
@Component
public class SqlResultComparator {

    /** Column separator used to flatten rows — a char that cannot appear in canonical cells. */
    private static final char CELL_SEP = '\u0001';

    public boolean matches(SqlResult expected, SqlResult actual, String comparisonMode) {
        if (expected == null || actual == null) return false;
        if (expected.getColumns().size() != actual.getColumns().size()) return false;

        boolean unordered = "UNORDERED".equalsIgnoreCase(comparisonMode);
        if (unordered) {
            return sortedKeys(expected).equals(sortedKeys(actual));
        }
        return positional(expected, actual);
    }

    private boolean positional(SqlResult expected, SqlResult actual) {
        if (expected.getRows().size() != actual.getRows().size()) return false;
        for (int i = 0; i < expected.getRows().size(); i++) {
            if (!flatten(expected.getRows().get(i)).equals(flatten(actual.getRows().get(i)))) {
                return false;
            }
        }
        return true;
    }

    private List<String> sortedKeys(SqlResult result) {
        List<String> keys = new ArrayList<>(result.getRows().size());
        for (List<String> row : result.getRows()) {
            keys.add(flatten(row));
        }
        keys.sort(Comparator.naturalOrder());
        return keys;
    }

    private String flatten(List<String> row) {
        StringBuilder sb = new StringBuilder();
        for (String cell : row) {
            if (sb.length() > 0) sb.append(CELL_SEP);
            sb.append(cell == null ? "" : cell);
        }
        return sb.toString();
    }
}
