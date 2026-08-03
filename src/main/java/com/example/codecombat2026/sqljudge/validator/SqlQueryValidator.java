package com.example.codecombat2026.sqljudge.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.codecombat2026.sqljudge.config.SqlJudgeProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Application-side SQL validation — a SECONDARY layer. The primary security
 * boundary is the Neon database itself: candidate queries run as a read-only
 * role that only has USAGE+SELECT on the current problem's schema, inside a
 * read-only transaction with a server-side statement timeout.
 *
 * This validator is the defence-in-depth that (a) rejects multi-statement
 * input (which the pooled Neon endpoint cannot safely run anyway), (b) rejects
 * statements that are not read-only SELECTs/WITH-CTEs, and (c) blocks
 * references to other question schemas and catalog/setup objects. It uses a
 * small hand-rolled scanner that is aware of string literals, dollar-quoted
 * strings, line/block comments, and quoted identifiers — NOT naive regex over
 * the raw text, so a column literally named {@code "insert"} doesn't
 * false-positive and a comment containing {@code DROP TABLE} doesn't trigger.
 */
@Component
public class SqlQueryValidator {

    private final SqlJudgeProperties properties;

    @Autowired
    public SqlQueryValidator(SqlJudgeProperties properties) {
        this.properties = properties;
    }

    /**
     * Words banned ANYWHERE in the query (outside literals/comments) as a
     * defensive second check. Mirrors the DB read-only boundary: even if a
     * statement sneaks past the leading-token gate, these can't be used.
     */
    private static final Set<String> FORBIDDEN_ANYWHERE = Set.of(
        "INSERT", "UPDATE", "DELETE", "DROP", "ALTER", "CREATE", "TRUNCATE",
        "GRANT", "REVOKE", "COPY", "CALL", "DO", "VACUUM", "ANALYZE", "REINDEX",
        "REFRESH", "SET", "RESET", "LOCK", "CLUSTER", "COMMENT", "PREPARE",
        "EXECUTE", "DEALLOCATE", "LISTEN", "NOTIFY", "UNLISTEN", "DISCARD",
        "IMPORT", "REASSIGN", "SECURITY", "ROLE", "USER"
    );

    /** Qualified-schema prefixes we never allow (catalog / other questions). */
    private static final Set<String> FORBIDDEN_SCHEMAS = Set.of(
        "pg_catalog", "information_schema", "pg_toast", "pg_temp"
    );

    private static final String FORBIDDEN_SCHEMA_REGEX =
        "(" + String.join("|", FORBIDDEN_SCHEMAS) + ")";

    /** These function usages imply data access / unsafe reads — blocked. */
    private static final Set<String> FORBIDDEN_FUNCTIONS = Set.of(
        "pg_read_file", "pg_read_binary_file", "lo_", "pg_ls_dir",
        "pg_stat_file", "pg_relation_filepath", "gen_random_uuid"
    );

    private static final String SELECT = "SELECT";

    public static final class ValidationResult {
        public final boolean ok;
        public final String message;
        ValidationResult(boolean ok, String message) {
            this.ok = ok;
            this.message = message;
        }
        public static ValidationResult pass() { return new ValidationResult(true, null); }
        public static ValidationResult fail(String msg) { return new ValidationResult(false, msg); }
    }

    /**
     * Validate candidate SQL. Returns ok=false with a human-readable reason on
     * any violation.
     */
    public ValidationResult validate(String sql) {
        if (sql == null || sql.isBlank()) {
            return ValidationResult.fail("SQL query is empty");
        }
        if (sql.length() > properties.getMaxSqlLength()) {
            return ValidationResult.fail("SQL query too long (max " + properties.getMaxSqlLength() + " chars)");
        }

        List<String> statements = splitStatements(sql);
        if (statements.size() != 1) {
            return ValidationResult.fail(
                statements.isEmpty() ? "No SQL statement found"
                                     : "Multiple SQL statements are not allowed (found " + statements.size() + ")");
        }

        String stmt = statements.get(0).trim();
        if (stmt.isEmpty()) {
            return ValidationResult.fail("No SQL statement found");
        }

        // First non-comment token must be SELECT (or WITH for CTEs).
        String firstToken = firstToken(stmt);
        if (!SELECT.equalsIgnoreCase(firstToken) && !"WITH".equalsIgnoreCase(firstToken)) {
            return ValidationResult.fail("Only read-only SELECT queries are allowed (got '" + firstToken + "')");
        }

        // Banned words anywhere.
        for (String token : extractTokens(stmt)) {
            if (FORBIDDEN_ANYWHERE.contains(token)) {
                return ValidationResult.fail("Statement contains forbidden keyword: " + token);
            }
            if (FORBIDDEN_FUNCTIONS.contains(token.toLowerCase(Locale.ROOT))) {
                return ValidationResult.fail("Statement uses a forbidden function: " + token);
            }
            String prefix = token.contains(".") ? token.split("\\.")[0].toLowerCase(Locale.ROOT) : "";
            if (!prefix.isEmpty() && prefix.matches(FORBIDDEN_SCHEMA_REGEX)) {
                return ValidationResult.fail("References to schema '" + prefix + "' are not allowed");
            }
        }

        // Qualified references to other question schemas (q_<id>) — secondary
        // layer; the DB grants are the real enforcement.
        for (String token : extractTokens(stmt)) {
            String lower = token.toLowerCase(Locale.ROOT);
            if (lower.matches("q_\\d+\\..+")) {
                return ValidationResult.fail("Access to other question schemas is not allowed");
            }
        }

        return ValidationResult.pass();
    }

    /**
     * Split SQL into top-level statements, respecting single/double-quoted
     * strings, dollar-quoted strings ($$...$$, $tag$...$tag$), and comments.
     * A statement ends at a ';' that is not inside any of those constructs.
     */
    List<String> splitStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int i = 0;
        int n = sql.length();
        while (i < n) {
            char c = sql.charAt(i);

            // Line comment
            if (c == '-' && i + 1 < n && sql.charAt(i + 1) == '-') {
                appendToEndOfLine(sql, i, current);
                i = nextLineBreak(sql, i);
                continue;
            }
            // Block comment
            if (c == '/' && i + 1 < n && sql.charAt(i + 1) == '*') {
                int end = sql.indexOf("*/", i + 2);
                current.append(sql, i, end < 0 ? n : end + 2);
                i = end < 0 ? n : end + 2;
                continue;
            }
            // Single-quoted string
            if (c == '\'') {
                current.append(c); // keep the opening quote so the literal is
                                   // preserved verbatim in the statement text
                i = copyStringLiteral(sql, i, current);
                continue;
            }
            // Double-quoted identifier
            if (c == '"') {
                current.append(c);
                i = copyQuotedIdentifier(sql, i, current);
                continue;
            }
            // Dollar-quoted string
            if (c == '$') {
                int dollarEnd = dollarQuotedEnd(sql, i);
                if (dollarEnd > i) {
                    current.append(sql, i, dollarEnd);
                    i = dollarEnd;
                    continue;
                }
            }
            // Statement terminator
            if (c == ';') {
                if (current.toString().trim().isEmpty()) {
                    current.setLength(0);
                } else {
                    statements.add(current.toString());
                    current.setLength(0);
                }
                i++;
                continue;
            }
            current.append(c);
            i++;
        }
        if (!current.toString().trim().isEmpty()) {
            statements.add(current.toString());
        }
        return statements;
    }

    private void appendToEndOfLine(String sql, int i, StringBuilder sb) {
        int end = sql.indexOf('\n', i);
        sb.append(sql, i, end < 0 ? sql.length() : end + 1);
    }

    private int nextLineBreak(String sql, int i) {
        int end = sql.indexOf('\n', i);
        return end < 0 ? sql.length() : end + 1;
    }

    private int copyStringLiteral(String sql, int i, StringBuilder sb) {
        // Handles '' escaped quotes inside the literal.
        int j = i + 1;
        while (j < sql.length()) {
            char c = sql.charAt(j);
            sb.append(c);
            j++;
            if (c == '\'') {
                // doubled '' is an escaped quote, keep going
                if (j < sql.length() && sql.charAt(j) == '\'') {
                    sb.append('\'');
                    j++;
                    continue;
                }
                return j;
            }
        }
        return j;
    }

    private int copyQuotedIdentifier(String sql, int i, StringBuilder sb) {
        int j = i + 1;
        while (j < sql.length()) {
            char c = sql.charAt(j);
            sb.append(c);
            j++;
            if (c == '"') {
                if (j < sql.length() && sql.charAt(j) == '"') {
                    sb.append('"');
                    j++;
                    continue;
                }
                return j;
            }
        }
        return j;
    }

    /** Returns end index (exclusive) of a dollar-quoted string starting at i, or i+1 if none. */
    private int dollarQuotedEnd(String sql, int i) {
        int endTag = sql.indexOf('$', i + 1);
        if (endTag < 0) return i + 1;
        String tag = sql.substring(i + 1, endTag);
        if (!tag.matches("[A-Za-z_][A-Za-z0-9_]*|")) return i + 1;
        String closing = "$" + tag + "$";
        int closeIdx = sql.indexOf(closing, endTag + 1);
        return closeIdx < 0 ? sql.length() : closeIdx + closing.length();
    }

    /** First non-comment, non-whitespace token of a statement. */
    String firstToken(String stmt) {
        StringBuilder token = new StringBuilder();
        int i = 0;
        int n = stmt.length();
        while (i < n) {
            char c = stmt.charAt(i);
            if (Character.isWhitespace(c)) { i++; continue; }
            if (c == '-' && i + 1 < n && stmt.charAt(i + 1) == '-') {
                int end = stmt.indexOf('\n', i);
                i = end < 0 ? n : end + 1;
                continue;
            }
            if (c == '/' && i + 1 < n && stmt.charAt(i + 1) == '*') {
                int end = stmt.indexOf("*/", i + 2);
                i = end < 0 ? n : end + 2;
                continue;
            }
            break;
        }
        while (i < n && !Character.isWhitespace(stmt.charAt(i)) && stmt.charAt(i) != '(') {
            token.append(stmt.charAt(i));
            i++;
        }
        return token.toString();
    }

    /**
     * Extract bare-word tokens outside strings/comments/quoted identifiers,
     * uppercased, for keyword scanning. Dotted names (schema.table) come
     * through as a single token.
     */
    List<String> extractTokens(String sql) {
        List<String> tokens = new ArrayList<>();
        StringBuilder word = new StringBuilder();
        int i = 0;
        int n = sql.length();
        boolean inQuote = false;
        while (i < n) {
            char c = sql.charAt(i);

            if (c == '\'') {
                flushWord(word, tokens);
                i = copyStringLiteralNoAppend(sql, i);
                continue;
            }
            if (c == '"') {
                flushWord(word, tokens);
                i = copyQuotedIdentifierNoAppend(sql, i);
                continue;
            }
            if (c == '$') {
                int dollarEnd = dollarQuotedEnd(sql, i);
                if (dollarEnd > i) {
                    flushWord(word, tokens);
                    i = dollarEnd;
                    continue;
                }
            }
            if (c == '-' && i + 1 < n && sql.charAt(i + 1) == '-') {
                flushWord(word, tokens);
                i = nextLineBreak(sql, i);
                continue;
            }
            if (c == '/' && i + 1 < n && sql.charAt(i + 1) == '*') {
                flushWord(word, tokens);
                int end = sql.indexOf("*/", i + 2);
                i = end < 0 ? n : end + 2;
                continue;
            }

            if (Character.isLetterOrDigit(c) || c == '_' || c == '.') {
                word.append(c);
            } else {
                flushWord(word, tokens);
            }
            i++;
        }
        flushWord(word, tokens);
        return tokens;
    }

    private void flushWord(StringBuilder word, List<String> tokens) {
        if (word.length() > 0) {
            tokens.add(word.toString().toUpperCase(Locale.ROOT));
            word.setLength(0);
        }
    }

    private int copyStringLiteralNoAppend(String sql, int i) {
        int j = i + 1;
        while (j < sql.length()) {
            char c = sql.charAt(j);
            j++;
            if (c == '\'') {
                if (j < sql.length() && sql.charAt(j) == '\'') { j++; continue; }
                return j;
            }
        }
        return j;
    }

    private int copyQuotedIdentifierNoAppend(String sql, int i) {
        int j = i + 1;
        while (j < sql.length()) {
            char c = sql.charAt(j);
            j++;
            if (c == '"') {
                if (j < sql.length() && sql.charAt(j) == '"') { j++; continue; }
                return j;
            }
        }
        return j;
    }
}
