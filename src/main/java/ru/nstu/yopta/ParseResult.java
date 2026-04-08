package ru.nstu.yopta;

import java.util.Collections;
import java.util.List;

/**
 * Результат синтаксического разбора: список диагностик; успех при пустом списке ошибок.
 */
public final class ParseResult {

    private final List<SyntaxDiagnostic> errors;

    public ParseResult(List<SyntaxDiagnostic> errors) {
        this.errors = errors != null ? List.copyOf(errors) : List.of();
    }

    public List<SyntaxDiagnostic> getErrors() {
        return errors;
    }

    public boolean isSuccess() {
        return errors.isEmpty();
    }

    public static ParseResult ok() {
        return new ParseResult(Collections.emptyList());
    }
}
