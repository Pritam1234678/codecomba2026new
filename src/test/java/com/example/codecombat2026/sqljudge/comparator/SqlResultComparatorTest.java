package com.example.codecombat2026.sqljudge.comparator;

import com.example.codecombat2026.sqljudge.dto.SqlResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlResultComparatorTest {

    private final SqlResultComparator comparator = new SqlResultComparator();

    private static SqlResult result(List<String> cols, List<List<String>> rows) {
        return new SqlResult(cols, rows);
    }

    @Test
    void unorderedIgnoresRowOrder() {
        SqlResult expected = result(List.of("a", "b"), List.of(
            List.of("1", "x"), List.of("2", "y")));
        SqlResult actual = result(List.of("a", "b"), List.of(
            List.of("2", "y"), List.of("1", "x")));
        assertTrue(comparator.matches(expected, actual, "UNORDERED"));
    }

    @Test
    void orderedRequiresSamePositions() {
        SqlResult expected = result(List.of("a", "b"), List.of(
            List.of("1", "x"), List.of("2", "y")));
        SqlResult actual = result(List.of("a", "b"), List.of(
            List.of("2", "y"), List.of("1", "x")));
        assertFalse(comparator.matches(expected, actual, "ORDERED"));
    }

    @Test
    void orderedMatchesWhenPositionalEqual() {
        SqlResult expected = result(List.of("a", "b"), List.of(
            List.of("1", "x"), List.of("2", "y")));
        SqlResult actual = result(List.of("a", "b"), List.of(
            List.of("1", "x"), List.of("2", "y")));
        assertTrue(comparator.matches(expected, actual, "ORDERED"));
    }

    @Test
    void columnCountMismatchFails() {
        SqlResult expected = result(List.of("a", "b"), List.of(List.of("1", "x")));
        SqlResult actual = result(List.of("a"), List.of(List.of("1")));
        assertFalse(comparator.matches(expected, actual, "UNORDERED"));
    }

    @Test
    void columnNamesAreIgnored() {
        SqlResult expected = result(List.of("alias_one", "alias_two"), List.of(List.of("1", "x")));
        SqlResult actual = result(List.of("totally", "different"), List.of(List.of("1", "x")));
        assertTrue(comparator.matches(expected, actual, "UNORDERED"));
    }

    @Test
    void differentRowCountsFail() {
        SqlResult expected = result(List.of("a"), List.of(List.of("1"), List.of("2")));
        SqlResult actual = result(List.of("a"), List.of(List.of("1")));
        assertFalse(comparator.matches(expected, actual, "UNORDERED"));
    }

    @Test
    void nullColumnCountMismatchFails() {
        SqlResult expected = result(List.of("a", "b"), List.of(List.of("1", "x")));
        SqlResult actual = result(List.of("a", "b"), List.of(List.of("1", "x")));
        SqlResult wrong = result(List.of("a"), List.of(List.of("1")));
        assertFalse(comparator.matches(expected, wrong, "UNORDERED"));
    }
}
