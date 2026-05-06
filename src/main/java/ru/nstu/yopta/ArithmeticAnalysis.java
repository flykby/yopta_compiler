package ru.nstu.yopta;

import java.util.List;

/**
 * Полный конвейер ЛР6: лексика → синтаксис и тетрады → ПОЛИЗ и значение (только для целых литералов).
 */
public final class ArithmeticAnalysis {

    private ArithmeticAnalysis() {
    }

    public static ArithmeticAnalysisResult analyze(String source) {
        List<Lexeme> lexemes = ArithmeticScanner.scan(source);
        boolean lexerErrors = lexemes.stream().anyMatch(Lexeme::isError);
        if (lexerErrors) {
            return new ArithmeticAnalysisResult(
                    lexemes,
                    true,
                    List.of(),
                    List.of(),
                    "",
                    "",
                    Messages.getString("arith.ir.skippedLexer"));
        }
        ArithmeticParseOutcome po = ArithmeticParser.parse(lexemes);
        if (!po.errors().isEmpty()) {
            return new ArithmeticAnalysisResult(
                    lexemes,
                    false,
                    po.errors(),
                    List.of(),
                    "",
                    "",
                    Messages.getString("arith.ir.skippedSyntax"));
        }
        String poliz = "";
        String eval = "";
        String footer = Messages.getString("arith.poliz.integerOnlyNote");
        if (ArithmeticPoliz.isIntegerLiteralExpression(lexemes)) {
            poliz = ArithmeticPoliz.toPolizString(lexemes);
            eval = ArithmeticPoliz.evaluateIntegerPoliz(lexemes);
            footer = Messages.getString("arith.poliz.evalFooter", eval);
        }
        return new ArithmeticAnalysisResult(
                lexemes,
                false,
                List.of(),
                po.quads(),
                poliz,
                eval,
                footer);
    }
}
