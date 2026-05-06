package ru.nstu.yopta;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** ЛР6: лексика, синтаксис, тетрады, ПОЛИЗ и целочисленное значение. */
class ArithmeticIrAnalysisTest {

    @BeforeAll
    static void initMessages() {
        Messages.setLocale(java.util.Locale.ROOT);
    }

    @Test
    void lexerRecognizesOperatorsNumbersIdent() {
        List<Lexeme> lex = ArithmeticScanner.scan("42 + x * (7)");
        assertFalse(lex.stream().anyMatch(Lexeme::isError));
        assertEquals(7, lex.size());
        assertEquals(ArithmeticScanner.CODE_NUMBER, lex.get(0).getCode());
        assertEquals(ArithmeticScanner.CODE_PLUS, lex.get(1).getCode());
        assertEquals(ArithmeticScanner.CODE_IDENT, lex.get(2).getCode());
    }

    @Test
    void lexerReportsIllegalCharacter() {
        List<Lexeme> lex = ArithmeticScanner.scan("1@2");
        assertTrue(lex.stream().anyMatch(Lexeme::isError));
    }

    @Test
    void lexerDoubleStarAndModAndIdentWithDollar() {
        List<Lexeme> lex = ArithmeticScanner.scan("a$b + 2**3 % 5");
        assertFalse(lex.stream().anyMatch(Lexeme::isError));
        assertEquals(ArithmeticScanner.CODE_IDENT, lex.get(0).getCode());
        assertEquals("a$b", lex.get(0).getText());
        assertEquals(ArithmeticScanner.CODE_POW, lex.get(3).getCode());
        assertEquals("**", lex.get(3).getText());
        assertEquals(ArithmeticScanner.CODE_MOD, lex.get(5).getCode());
    }

    @Test
    void quadsForParenthesesAndPrecedence() {
        List<Lexeme> lex = ArithmeticScanner.scan("(1+2)*3");
        ArithmeticParseOutcome po = ArithmeticParser.parse(lex);
        assertTrue(po.errors().isEmpty());
        assertEquals(2, po.quads().size());
        assertEquals("+", po.quads().get(0).getOp());
        assertEquals("1", po.quads().get(0).getArg1());
        assertEquals("2", po.quads().get(0).getArg2());
        assertEquals("t1", po.quads().get(0).getResult());
        assertEquals("*", po.quads().get(1).getOp());
        assertEquals("t1", po.quads().get(1).getArg1());
        assertEquals("3", po.quads().get(1).getArg2());
    }

    @Test
    void syntaxErrorMissingOperandClearsQuads() {
        List<Lexeme> lex = ArithmeticScanner.scan("1+");
        ArithmeticParseOutcome po = ArithmeticParser.parse(lex);
        assertFalse(po.errors().isEmpty());
        assertTrue(po.quads().isEmpty());
    }

    @Test
    void polizAndEvalIntegerOnly() {
        List<Lexeme> lex = ArithmeticScanner.scan("2+3*4");
        assertTrue(ArithmeticPoliz.isIntegerLiteralExpression(lex));
        assertEquals("2 3 4 * +", ArithmeticPoliz.toPolizString(lex));
        assertEquals("14", ArithmeticPoliz.evaluateIntegerPoliz(lex));
    }

    @Test
    void polizPowModAndEval() {
        List<Lexeme> lex = ArithmeticScanner.scan("2**10+10%7");
        assertEquals("2 10 ** 10 7 % +", ArithmeticPoliz.toPolizString(lex));
        assertEquals("1027", ArithmeticPoliz.evaluateIntegerPoliz(lex));
        assertEquals(8L, ArithmeticPoliz.longPowExact(2, 3));
    }

    @Test
    void polizSkippedWhenIdentifierPresent() {
        List<Lexeme> lex = ArithmeticScanner.scan("a+1");
        assertFalse(ArithmeticPoliz.isIntegerLiteralExpression(lex));
        ArithmeticParseOutcome po = ArithmeticParser.parse(lex);
        assertTrue(po.errors().isEmpty());
        assertEquals(1, po.quads().size());
    }

    @Test
    void fullPipelineSkipsQuadsOnLexError() {
        ArithmeticAnalysisResult r = ArithmeticAnalysis.analyze("1$2");
        assertTrue(r.lexerErrors());
        assertTrue(r.quads().isEmpty());
    }

    @Test
    void fullPipelineEvaluatesCleanExpression() {
        ArithmeticAnalysisResult r = ArithmeticAnalysis.analyze("(10-2)/4");
        assertFalse(r.lexerErrors());
        assertTrue(r.syntaxErrors().isEmpty());
        assertEquals(2, r.quads().size());
        assertEquals("10 2 - 4 /", r.polizLine());
        assertEquals("2", r.evaluationValue());
    }

    @Test
    void divisionByZeroInEvaluation() {
        List<Lexeme> lex = ArithmeticScanner.scan("1/0");
        assertEquals(Messages.getString("arith.poliz.divZero"), ArithmeticPoliz.evaluateIntegerPoliz(lex));
        ArithmeticAnalysisResult r = ArithmeticAnalysis.analyze("1/0");
        assertEquals(Messages.getString("arith.poliz.divZero"), r.evaluationValue());
    }

    @Test
    void moduloByZeroInEvaluation() {
        List<Lexeme> lex = ArithmeticScanner.scan("10%0");
        assertEquals(Messages.getString("arith.poliz.modZero"), ArithmeticPoliz.evaluateIntegerPoliz(lex));
    }
}
