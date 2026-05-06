package ru.nstu.yopta;

import java.util.List;

/**
 * Результат анализа внутренней формы (лексика, синтаксис, тетрады, ПОЛИЗ).
 */
public record ArithmeticAnalysisResult(
        List<Lexeme> lexemes,
        boolean lexerErrors,
        List<SyntaxDiagnostic> syntaxErrors,
        List<Quad> quads,
        String polizLine,
        String evaluationValue,
        String footerNote
) {
    public boolean showQuads() {
        return !lexerErrors && (syntaxErrors == null || syntaxErrors.isEmpty());
    }
}
