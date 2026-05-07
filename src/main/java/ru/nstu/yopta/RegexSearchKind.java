package ru.nstu.yopta;

import java.util.regex.Pattern;

/**
 * Три варианта поиска из лабораторной работы: целое число, ФИО на английском, надёжный пароль.
 */
public enum RegexSearchKind {

    INTEGER(
            "regex.kind.integer",
            Pattern.compile("-?[0-9]+")
    ),

    /** Формат «Last, First Middle», одна строка целиком; поиск реализован автоматом (доп. задание). */
    FULL_NAME(
            "regex.kind.fullName",
            Pattern.compile("^[A-Z][a-z]*,\\s+[A-Z][a-z]*\\s+[A-Z][a-z]*$", Pattern.MULTILINE)
    ),

    /** Длина ≥ 12, без пробелов; классы символов и спецсимволы по заданию; построчно (MULTILINE). */
    PASSWORD(
            "regex.kind.password",
            Pattern.compile(
                    "^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[/#?!@_$%^&*|\\\\-])\\S{12,}$",
                    Pattern.MULTILINE)
    );

    private final String messageKey;
    private final Pattern pattern;

    RegexSearchKind(String messageKey, Pattern pattern) {
        this.messageKey = messageKey;
        this.pattern = pattern;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public String getDisplayName() {
        return Messages.getString(messageKey);
    }

    public Pattern getPattern() {
        return pattern;
    }

    /** Единая точка поиска: для ФИО — автомат, иначе — Pattern. */
    public java.util.List<RegexMatch> findAll(String text) {
        if (this == FULL_NAME) {
            return FullNameAutomatonSearch.findAll(text);
        }
        return RegexSearch.findAll(text, pattern);
    }
}
