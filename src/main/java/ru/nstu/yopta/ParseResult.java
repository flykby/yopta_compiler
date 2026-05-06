package ru.nstu.yopta;

import ru.nstu.yopta.ast.AstProgram;

import java.util.Collections;
import java.util.List;

/**
 * Результат синтаксического разбора: список диагностик; успех при пустом списке ошибок;
 * опционально AST программы (для семантики после успешного разбора).
 */
public final class ParseResult {

    private final List<SyntaxDiagnostic> errors;
    private final AstProgram ast;

    public ParseResult(List<SyntaxDiagnostic> errors, AstProgram ast) {
        this.errors = errors != null ? List.copyOf(errors) : List.of();
        this.ast = ast;
    }

    public ParseResult(List<SyntaxDiagnostic> errors) {
        this(errors, null);
    }

    public List<SyntaxDiagnostic> getErrors() {
        return errors;
    }

    /** AST может быть {@code null}, если дерево не строилось или синтаксис с ошибками. */
    public AstProgram getAst() {
        return ast;
    }

    public boolean isSuccess() {
        return errors.isEmpty();
    }

    public static ParseResult ok() {
        return new ParseResult(Collections.emptyList(), AstProgram.empty());
    }

    public static ParseResult ok(AstProgram program) {
        return new ParseResult(Collections.emptyList(), program);
    }
}
