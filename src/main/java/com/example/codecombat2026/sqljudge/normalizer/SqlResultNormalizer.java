package com.example.codecombat2026.sqljudge.normalizer;

import com.example.codecombat2026.sqljudge.dto.SqlResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts a JDBC {@link ResultSet} into a deterministic {@link SqlResult}
 * where every cell is a canonical string.
 *
 * <p>Why strings? JDBC exposes the same PostgreSQL value as different Java
 * types depending on how the query is written ({@code COUNT(*)} → Long,
 * {@code numeric(10,2)} → BigDecimal, {@code 10.00} vs {@code 10}). If we
 * compared raw JDBC objects, "10.00" (BigDecimal) vs "10" (Long) would
 * falsely differ. Canonical strings make comparison stable and let us store
 * expected results as plain JSON.
 *
 * <p>Canonicalization rules (deterministic, judge-defined):
 * <ul>
 *   <li>{@code NULL} → the sentinel {@code "\u0000NULL"} (never a user value).</li>
 *   <li>Numbers → {@link BigDecimal#toPlainString()} with trailing zeros stripped
 *       (so {@code 10.00} == {@code 10} == {@code 10.0}).</li>
 *   <li>Booleans → {@code true}/{@code false}.</li>
 *   <li>Dates/timestamps → ISO-8601 ({@code 2024-01-15} / {@code 2024-01-15T10:30:00}).</li>
 *   <li>Everything else → {@link String#valueOf}.</li>
 * </ul>
 */
@Component
public class SqlResultNormalizer {

    /** Sentinel for SQL NULL that cannot collide with real user data. */
    public static final String NULL_SENTINEL = "\u0000NULL";

    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * Read up to {@code maxRows} rows from {@code rs} into a SqlResult.
     * Column names come from the result-set metadata. Rows beyond maxRows are
     * dropped and {@code truncated} is set.
     */
    public SqlResult normalize(ResultSet rs, int maxRows) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int columnCount = md.getColumnCount();

        List<String> columns = new ArrayList<>(columnCount);
        for (int i = 1; i <= columnCount; i++) {
            columns.add(md.getColumnLabel(i));
        }

        List<List<String>> rows = new ArrayList<>();
        boolean truncated = false;
        while (rs.next()) {
            if (rows.size() >= maxRows) {
                truncated = true;
                break;
            }
            List<String> row = new ArrayList<>(columnCount);
            for (int i = 1; i <= columnCount; i++) {
                row.add(normalizeCell(rs.getObject(i)));
            }
            rows.add(row);
        }
        return new SqlResult(columns, rows, truncated);
    }

    /** Canonicalize a single JDBC object to a string. */
    public String normalizeCell(Object value) {
        if (value == null) {
            return NULL_SENTINEL;
        }
        if (value instanceof BigDecimal bd) {
            return canonicalNumber(bd);
        }
        if (value instanceof Number num) {
            // Integers, longs, doubles, floats all become a stable plain string.
            if (num instanceof Double || num instanceof Float) {
                return canonicalDecimal(BigDecimal.valueOf(num.doubleValue()));
            }
            return String.valueOf(num);
        }
        if (value instanceof Boolean b) {
            return b.toString();
        }
        if (value instanceof LocalDate ld) {
            return ld.format(DATE_FMT);
        }
        if (value instanceof LocalDateTime ldt) {
            return ldt.format(TIMESTAMP_FMT);
        }
        if (value instanceof OffsetDateTime odt) {
            return odt.toLocalDateTime().format(TIMESTAMP_FMT);
        }
        if (value instanceof java.sql.Date d) {
            return d.toLocalDate().format(DATE_FMT);
        }
        if (value instanceof Timestamp ts) {
            return ts.toLocalDateTime().format(TIMESTAMP_FMT);
        }
        if (value instanceof java.sql.Time t) {
            return t.toLocalTime().toString();
        }
        if (value instanceof byte[] bytes) {
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        }
        return String.valueOf(value);
    }

    /**
     * Canonical number form: plain decimal string, no trailing zeros,
     * {@code 0} not {@code 0.00} and not {@code 0.0}.
     */
    private String canonicalNumber(BigDecimal bd) {
        if (bd.compareTo(BigDecimal.ZERO) == 0) return "0";
        BigDecimal stripped = bd.stripTrailingZeros();
        return stripped.toPlainString();
    }

    private String canonicalDecimal(BigDecimal bd) {
        if (bd.compareTo(BigDecimal.ZERO) == 0) return "0";
        return bd.stripTrailingZeros().toPlainString();
    }
}
