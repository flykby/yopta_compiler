package ru.nstu.yopta;

import java.util.List;

/** Результат разбора арифметического выражения: тетрады и диагностики синтаксиса. */
public record ArithmeticParseOutcome(List<Quad> quads, List<SyntaxDiagnostic> errors) {
}
