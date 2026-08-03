package com.example.codecombat2026.sqljudge.normalizer;

import com.example.codecombat2026.sqljudge.dto.SqlResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SqlResultNormalizerTest {

    private final SqlResultNormalizer normalizer = new SqlResultNormalizer();

    @Mock private ResultSet rs;
    @Mock private ResultSetMetaData md;

    @Test
    void nullCellUsesSentinel() {
        assertEquals(SqlResultNormalizer.NULL_SENTINEL, normalizer.normalizeCell(null));
    }

    @Test
    void decimalTrailingZerosAreStripped() {
        assertEquals("10", normalizer.normalizeCell(new BigDecimal("10.00")));
        assertEquals("10", normalizer.normalizeCell(new BigDecimal("10.0")));
        assertEquals("0", normalizer.normalizeCell(new BigDecimal("0.000")));
        assertEquals("1.5", normalizer.normalizeCell(new BigDecimal("1.50")));
    }

    @Test
    void integerStaysStable() {
        assertEquals("42", normalizer.normalizeCell(42L));
        assertEquals("7", normalizer.normalizeCell(7));
    }

    @Test
    void booleanIsCanonical() {
        assertEquals("true", normalizer.normalizeCell(Boolean.TRUE));
        assertEquals("false", normalizer.normalizeCell(Boolean.FALSE));
    }

    @Test
    void datesAndTimestampsUseIso() {
        assertEquals("2024-01-15", normalizer.normalizeCell(LocalDate.of(2024, 1, 15)));
        assertEquals("2024-01-15T10:30:00", normalizer.normalizeCell(LocalDateTime.of(2024, 1, 15, 10, 30)));
    }

    @Test
    void normalizeTruncatesBeyondMaxRows() throws SQLException {
        when(rs.getMetaData()).thenReturn(md);
        when(md.getColumnCount()).thenReturn(1);
        when(md.getColumnLabel(1)).thenReturn("x");
        when(rs.next()).thenReturn(true, true, true, false);
        when(rs.getObject(1)).thenReturn("a", "b", "c");

        SqlResult result = normalizer.normalize(rs, 2);
        assertEquals(2, result.getRows().size());
        assertTrue(result.isTruncated());
    }

    @Test
    void normalizeNotTruncatedWhenWithinLimit() throws SQLException {
        when(rs.getMetaData()).thenReturn(md);
        when(md.getColumnCount()).thenReturn(1);
        when(md.getColumnLabel(1)).thenReturn("x");
        when(rs.next()).thenReturn(true, false);
        when(rs.getObject(1)).thenReturn("a");

        SqlResult result = normalizer.normalize(rs, 100);
        assertEquals(1, result.getRows().size());
        assertFalse(result.isTruncated());
    }
}
