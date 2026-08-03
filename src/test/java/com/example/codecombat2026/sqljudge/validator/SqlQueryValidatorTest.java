package com.example.codecombat2026.sqljudge.validator;

import com.example.codecombat2026.sqljudge.config.SqlJudgeProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlQueryValidatorTest {

    private SqlQueryValidator validator;

    @BeforeEach
    void setUp() {
        SqlJudgeProperties props = new SqlJudgeProperties();
        props.setMaxSqlLength(20000);
        validator = new SqlQueryValidator(props);
    }

    @Test
    void plainSelectIsAllowed() {
        assertTrue(validator.validate("SELECT * FROM employees").ok);
    }

    @Test
    void selectWithWhereGroupByOrderIsAllowed() {
        assertTrue(validator.validate(
            "SELECT department, COUNT(*) AS c FROM employees WHERE salary > 50000 GROUP BY department ORDER BY c DESC").ok);
    }

    @Test
    void cteWithIsAllowed() {
        assertTrue(validator.validate(
            "WITH dept_sal AS (SELECT department, AVG(salary) s FROM employees GROUP BY department) SELECT * FROM dept_sal").ok);
    }

    @Test
    void windowFunctionsAreAllowed() {
        assertTrue(validator.validate(
            "SELECT name, salary, ROW_NUMBER() OVER (ORDER BY salary DESC) AS rank FROM employees").ok);
    }

    @Test
    void insertIsRejected() {
        SqlQueryValidator.ValidationResult r = validator.validate("INSERT INTO employees VALUES (1, 'a')");
        assertFalse(r.ok);
    }

    @Test
    void updateIsRejected() {
        assertFalse(validator.validate("UPDATE employees SET salary = 0").ok);
    }

    @Test
    void deleteIsRejected() {
        assertFalse(validator.validate("DELETE FROM employees").ok);
    }

    @Test
    void dropIsRejected() {
        assertFalse(validator.validate("DROP TABLE employees").ok);
    }

    @Test
    void multiStatementIsRejected() {
        assertFalse(validator.validate("SELECT * FROM employees; SELECT * FROM departments").ok);
    }

    @Test
    void forbiddenKeywordInStringLiteralIsIgnored() {
        assertTrue(validator.validate("SELECT * FROM employees WHERE name = 'INSERT is fine as text'").ok);
    }

    @Test
    void forbiddenKeywordInCommentIsIgnored() {
        assertTrue(validator.validate("SELECT * FROM employees -- DROP TABLE employees").ok);
        assertTrue(validator.validate("SELECT * FROM employees /* DELETE FROM x */").ok);
    }

    @Test
    void columnNamedLikeKeywordInQuotedIdentIsAllowed() {
        assertTrue(validator.validate("SELECT \"delete\", \"update\" FROM employees").ok);
    }

    @Test
    void qualifiedReferenceToOtherQuestionSchemaIsRejected() {
        assertFalse(validator.validate("SELECT * FROM q_12.secret_table").ok);
    }

    @Test
    void qualifiedReferenceToPgCatalogIsRejected() {
        assertFalse(validator.validate("SELECT * FROM pg_catalog.pg_class").ok);
        assertFalse(validator.validate("SELECT * FROM information_schema.tables").ok);
    }

    @Test
    void forbiddenFunctionIsRejected() {
        assertFalse(validator.validate("SELECT pg_read_file('/etc/passwd')").ok);
    }

    @Test
    void semicolonInStringLiteralIsFine() {
        assertTrue(validator.validate("SELECT * FROM employees WHERE name = 'a;b'").ok);
    }

    @Test
    void trailingSemicolonIsAllowed() {
        assertTrue(validator.validate("SELECT * FROM employees;").ok);
    }

    @Test
    void leadingCommentThenSelectIsAllowed() {
        assertTrue(validator.validate("-- a comment\nSELECT * FROM employees").ok);
    }

    @Test
    void emptySqlIsRejected() {
        assertFalse(validator.validate("   ").ok);
        assertFalse(validator.validate(null).ok);
    }

    @Test
    void overLengthSqlIsRejected() {
        StringBuilder sb = new StringBuilder("SELECT 1");
        while (sb.length() < 30000) sb.append(" + 1");
        assertFalse(validator.validate(sb.toString()).ok);
    }
}
