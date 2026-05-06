package ru.nstu.yopta;

/**
 * Одно совпадение регулярного выражения во входном тексте.
 *
 * @param fragment    найденная подстрока
 * @param line        номер строки (с 1)
 * @param column      номер символа в строке (с 1)
 * @param length      длина фрагмента в символах
 * @param startOffset смещение начала совпадения от начала текста (0-based), для выделения в редакторе
 */
public record RegexMatch(String fragment, int line, int column, int length, int startOffset) {

    public String getLocationString() {
        return Messages.getString("regex.match.location", line, column);
    }
}
