package ru.nstu.yopta;

import java.util.Locale;

/**
 * Тексты разделов практической работы «Десятичные константы Pascal» для меню «Текст».
 */
public final class PascalDecimalPracticeContent {

    private PascalDecimalPracticeContent() {}

    private static boolean isEnglish(Locale l) {
        return l != null && l.getLanguage().equalsIgnoreCase(Locale.ENGLISH.getLanguage());
    }

    public static String problemStatement(Locale locale) {
        if (isEnglish(locale)) {
            return """
                    https://docs.google.com/document/d/1xSY1ZmZjrjx1APFY4D5dwDP_l6colafMlBKUZtzP4WQ/edit#heading=h.d82cbnwfgmqp
                    """;
        }
        return """
                https://docs.google.com/document/d/1tKikPurbEzdiUDspBZg-a32C5kTNzfOlcgmNEO77fBY/edit?usp=sharing
                """;
    }

    public static String grammar(Locale locale) {
        if (isEnglish(locale)) {
            return """
                    https://docs.google.com/document/d/1tKikPurbEzdiUDspBZg-a32C5kTNzfOlcgmNEO77fBY/edit?usp=sharing
                    """;
        }
        return """
                https://docs.google.com/document/d/1tKikPurbEzdiUDspBZg-a32C5kTNzfOlcgmNEO77fBY/edit?usp=sharing
                """;
    }

    public static String grammarClassification(Locale locale) {
        if (isEnglish(locale)) {
            return """
                    https://docs.google.com/document/d/1tKikPurbEzdiUDspBZg-a32C5kTNzfOlcgmNEO77fBY/edit?usp=sharing
                    """;
        }
        return """
                https://docs.google.com/document/d/1tKikPurbEzdiUDspBZg-a32C5kTNzfOlcgmNEO77fBY/edit?usp=sharing
                """;
    }

    public static String analysisMethod(Locale locale) {
        if (isEnglish(locale)) {
            return """
                    https://docs.google.com/document/d/1tKikPurbEzdiUDspBZg-a32C5kTNzfOlcgmNEO77fBY/edit?usp=sharing
                    """;
        }
        return """
                    https://docs.google.com/document/d/1xSY1ZmZjrjx1APFY4D5dwDP_l6colafMlBKUZtzP4WQ/edit#heading=h.yh8lqiyde0jn
                    """;
    }

    public static String diagnostics(Locale locale) {
        if (isEnglish(locale)) {
            return """
                    https://docs.google.com/document/d/1xSY1ZmZjrjx1APFY4D5dwDP_l6colafMlBKUZtzP4WQ/edit#heading=h.9sxrlt3s2oio
                    """;
        }
        return """
                    https://docs.google.com/document/d/1xSY1ZmZjrjx1APFY4D5dwDP_l6colafMlBKUZtzP4WQ/edit#heading=h.9sxrlt3s2oio
                    """;
    }

    public static String testExample(Locale locale) {
        if (isEnglish(locale)) {
            return """
                    https://docs.google.com/document/d/1xSY1ZmZjrjx1APFY4D5dwDP_l6colafMlBKUZtzP4WQ/edit#heading=h.9sxrlt3s2oio
                    """;
        }
        return """
                    https://docs.google.com/document/d/1xSY1ZmZjrjx1APFY4D5dwDP_l6colafMlBKUZtzP4WQ/edit#heading=h.9sxrlt3s2oio
                    """;
    }

    public static String bibliography(Locale locale) {
        if (isEnglish(locale)) {
            return """
                    1. Aho A. V., Lam M. S., Sethi R., Ullman J. D. Compilers: Principles, Techniques, and Tools (Dragon Book). — Lexical analysis, finite automata, regular expressions.

                    2. Grune D. et al. Modern Compiler Design. — Scanner construction, error handling.

                    3. Course lecture notes / methodical instructions of your chair on TFLaC (Theory of Formal Languages and Compilers).

                    4. ISO 7185:1990 (Pascal standard) or your faculty’s adopted Pascal dialect specification — numeric literal syntax.
                    """;
        }
        return """
                    1. Ахо А. В., Лам М. С., Сети Р., Ульман Дж. Д. Компиляторы: принципы, технологии и инструменты. — Лексический анализ, конечные автоматы, регулярные выражения.

                    2. Груне Д. и др. Современная разработка компиляторов. — Построение сканера, обработка ошибок.

                    3. Конспект лекций / методические указания кафедры по дисциплине ТФЯиК (теория формальных языков и компиляторов).

                    4. Стандарт Pascal (например, ISO 7185) или описание диалекта Turbo Pascal / Free Pascal, принятое на курсе, — раздел о константам и числовых литералах.
                    """;
    }

    public static String sourceCode(Locale locale) {
        if (isEnglish(locale)) {
            return """
                    https://docs.google.com/document/d/1xSY1ZmZjrjx1APFY4D5dwDP_l6colafMlBKUZtzP4WQ/edit#heading=h.jdir21qugngs
                    """;
        }
        return """
                    https://docs.google.com/document/d/1xSY1ZmZjrjx1APFY4D5dwDP_l6colafMlBKUZtzP4WQ/edit#heading=h.jdir21qugngs
                    """;
    }
}
