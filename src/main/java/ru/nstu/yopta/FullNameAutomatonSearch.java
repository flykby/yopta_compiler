package ru.nstu.yopta;

import java.util.ArrayList;
import java.util.List;

/**
 * Поиск строк формата «Last, First Middle» с помощью конечного автомата (без java.util.regex).
 *
 * Формат совпадает с РВ: {@code ^[A-Z][a-z]*,\\s+[A-Z][a-z]*\\s+[A-Z][a-z]*$} (построчно).
 */
public final class FullNameAutomatonSearch {

    private FullNameAutomatonSearch() {
    }

    /** Возвращает все совпадения (каждое совпадение — целая строка). */
    public static List<RegexMatch> findAll(String text) {
        List<RegexMatch> out = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return out;
        }

        int line = 1;
        int lineStartOffset = 0;
        int i = 0;
        int n = text.length();
        while (i <= n) {
            int lineEnd = i;
            if (i == n || text.charAt(i) == '\n') {
                // строка: [lineStartOffset, lineEnd)
                int trimmedEnd = lineEnd;
                if (trimmedEnd > lineStartOffset && text.charAt(trimmedEnd - 1) == '\r') {
                    trimmedEnd--;
                }
                if (trimmedEnd > lineStartOffset) {
                    CharSequence lineText = text.subSequence(lineStartOffset, trimmedEnd);
                    if (matchesLine(lineText)) {
                        int len = trimmedEnd - lineStartOffset;
                        String fragment = lineText.toString();
                        out.add(new RegexMatch(fragment, line, 1, len, lineStartOffset));
                    }
                }
                // next line
                line++;
                lineStartOffset = i + 1;
            }
            i++;
        }

        return out;
    }

    private static boolean matchesLine(CharSequence s) {
        int i = 0;
        int n = s.length();

        // Last: [A-Z][a-z]*
        if (i >= n || !isUpper(s.charAt(i))) return false;
        i++;
        while (i < n && isLower(s.charAt(i))) i++;

        // comma
        if (i >= n || s.charAt(i) != ',') return false;
        i++;

        // ws+
        int ws1 = i;
        while (i < n && isWs(s.charAt(i))) i++;
        if (i == ws1) return false;

        // First: [A-Z][a-z]*
        if (i >= n || !isUpper(s.charAt(i))) return false;
        i++;
        while (i < n && isLower(s.charAt(i))) i++;

        // ws+
        int ws2 = i;
        while (i < n && isWs(s.charAt(i))) i++;
        if (i == ws2) return false;

        // Middle: [A-Z][a-z]*
        if (i >= n || !isUpper(s.charAt(i))) return false;
        i++;
        while (i < n && isLower(s.charAt(i))) i++;

        // end of line
        return i == n;
    }

    private static boolean isUpper(char c) {
        return c >= 'A' && c <= 'Z';
    }

    private static boolean isLower(char c) {
        return c >= 'a' && c <= 'z';
    }

    private static boolean isWs(char c) {
        return c == ' ' || c == '\t' || c == '\r';
    }
}

