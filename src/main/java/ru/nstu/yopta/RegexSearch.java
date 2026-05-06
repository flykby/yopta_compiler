package ru.nstu.yopta;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Поиск всех непересекающихся совпадений шаблона во всём тексте (аналог «глобального» поиска).
 */
public final class RegexSearch {

    private RegexSearch() {
    }

    /**
     * Находит все совпадения {@code pattern} в {@code text}, вычисляет строку и столбец начала (с 1).
     */
    public static List<RegexMatch> findAll(String text, Pattern pattern) {
        List<RegexMatch> out = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return out;
        }
        Matcher m = pattern.matcher(text);
        while (m.find()) {
            int start = m.start();
            int end = m.end();
            String fragment = text.substring(start, end);
            int[] lineCol = offsetToLineColumn(text, start);
            out.add(new RegexMatch(fragment, lineCol[0], lineCol[1], end - start, start));
        }
        return out;
    }

    /** Строка и столбец (оба с 1) для смещения {@code offset} (0-based). */
    static int[] offsetToLineColumn(String text, int offset) {
        int line = 1;
        int lineStart = 0;
        for (int i = 0; i < offset && i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                line++;
                lineStart = i + 1;
            }
        }
        int column = offset - lineStart + 1;
        return new int[]{line, column};
    }
}
