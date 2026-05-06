package ru.nstu.yopta;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты регулярных выражений ЛР4: положительные и отрицательные примеры.
 */
class RegexSearchTest {

    @Test
    void integer_matches_and_positions() {
        Pattern p = RegexSearchKind.INTEGER.getPattern();
        List<RegexMatch> m = RegexSearch.findAll("x -42 y 0 z 15\n", p);
        assertEquals(3, m.size());
        assertEquals("-42", m.get(0).fragment());
        assertEquals(1, m.get(0).line());
        assertEquals(3, m.get(0).column());
        assertEquals("15", m.get(2).fragment());
    }

    @Test
    void integer_negative_examples() {
        Pattern p = RegexSearchKind.INTEGER.getPattern();
        assertTrue(RegexSearch.findAll("abc", p).isEmpty());
        assertTrue(RegexSearch.findAll("", p).isEmpty());
    }

    @Test
    void fullName_line_based() {
        Pattern p = RegexSearchKind.FULL_NAME.getPattern();
        String ok = "Smith, John Michael\n";
        List<RegexMatch> m = RegexSearch.findAll(ok, p);
        assertEquals(1, m.size());
        assertEquals("Smith, John Michael", m.get(0).fragment());

        assertTrue(RegexSearch.findAll("Smith, john Michael\n", p).isEmpty());
        assertTrue(RegexSearch.findAll("Smith John Michael\n", p).isEmpty());
    }

    @Test
    void password_per_line() {
        Pattern p = RegexSearchKind.PASSWORD.getPattern();
        String ok = "Aa1!aaaaaaaaaa\n";
        assertFalse(RegexSearch.findAll(ok, p).isEmpty());

        assertTrue(RegexSearch.findAll("Aa1!short\n", p).isEmpty());
        assertTrue(RegexSearch.findAll("Aa1!aaaaaaaaaa \n", p).isEmpty());
    }

    @Test
    void offsetToLineColumn_first_line() {
        int[] lc = RegexSearch.offsetToLineColumn("hello", 0);
        assertEquals(1, lc[0]);
        assertEquals(1, lc[1]);
    }

    @Test
    void offsetToLineColumn_second_line() {
        int[] lc = RegexSearch.offsetToLineColumn("a\nbc", 3);
        assertEquals(2, lc[0]);
        assertEquals(2, lc[1]);
    }
}
