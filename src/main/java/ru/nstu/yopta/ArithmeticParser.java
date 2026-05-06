package ru.nstu.yopta;

import java.util.ArrayList;
import java.util.List;

/**
 * Синтаксический разбор арифметического выражения методом рекурсивного спуска и построение цепочки тетрад.
 *
 * <pre>
 * E → T A, A → ε | '+' T A | '-' T A
 * T → F B, B → ε | '*' F B | '/' F B | '%' F B | '**' F B
 * F → num | id | '(' E ')'
 * </pre>
 *
 * Схема рекурсивного спуска: {@link #parseExpr()} соответствует {@code E}/{@code A},
 * {@link #parseTerm()} — {@code T}/{@code B}, {@link #parseFactor()} — {@code F}.
 */
public final class ArithmeticParser {

    private final List<Lexeme> tokens;
    private int pos;
    private int tempId;
    private final List<Quad> quads = new ArrayList<>();
    private final List<SyntaxDiagnostic> errors = new ArrayList<>();

    private ArithmeticParser(List<Lexeme> tokens) {
        this.tokens = tokens;
    }

    /**
     * Полный разбор: при отсутствии лексических ошибок строит тетрады; иначе возвращает пустой список тетрад.
     */
    public static ArithmeticParseOutcome parse(List<Lexeme> lexemes) {
        if (lexemes == null || lexemes.stream().anyMatch(Lexeme::isError)) {
            return new ArithmeticParseOutcome(List.of(), List.of());
        }
        ArithmeticParser p = new ArithmeticParser(lexemes);
        p.parseProgram();
        return new ArithmeticParseOutcome(List.copyOf(p.quads), List.copyOf(p.errors));
    }

    private void parseProgram() {
        if (tokens.isEmpty()) {
            addError(new SyntaxDiagnostic("", 1, 1, 1, Messages.getString("arith.err.empty")));
            return;
        }
        String value = parseExpr();
        if (!errors.isEmpty()) {
            return;
        }
        if (pos < tokens.size()) {
            Lexeme extra = tokens.get(pos);
            addError(new SyntaxDiagnostic(
                    extra.getText(),
                    extra.getLine(),
                    extra.getStartColumn(),
                    extra.getEndColumn(),
                    Messages.getString("arith.err.trailing")));
            return;
        }
        if (value == null) {
            quads.clear();
        }
    }

    private String parseExpr() {
        String left = parseTerm();
        if (left == null) {
            return null;
        }
        while (pos < tokens.size()) {
            Lexeme t = tokens.get(pos);
            int code = t.getCode();
            if (code != ArithmeticScanner.CODE_PLUS && code != ArithmeticScanner.CODE_MINUS) {
                break;
            }
            pos++;
            String right = parseTerm();
            if (right == null) {
                return null;
            }
            String op = code == ArithmeticScanner.CODE_PLUS ? "+" : "-";
            String res = nextTemp();
            quads.add(new Quad(op, left, right, res));
            left = res;
        }
        return left;
    }

    private String parseTerm() {
        String left = parseFactor();
        if (left == null) {
            return null;
        }
        while (pos < tokens.size()) {
            Lexeme t = tokens.get(pos);
            int code = t.getCode();
            if (code != ArithmeticScanner.CODE_MULT
                    && code != ArithmeticScanner.CODE_DIV
                    && code != ArithmeticScanner.CODE_MOD
                    && code != ArithmeticScanner.CODE_POW) {
                break;
            }
            pos++;
            String right = parseFactor();
            if (right == null) {
                return null;
            }
            String op = switch (code) {
                case ArithmeticScanner.CODE_MULT -> "*";
                case ArithmeticScanner.CODE_DIV -> "/";
                case ArithmeticScanner.CODE_MOD -> "%";
                case ArithmeticScanner.CODE_POW -> "**";
                default -> "?";
            };
            String res = nextTemp();
            quads.add(new Quad(op, left, right, res));
            left = res;
        }
        return left;
    }

    private String parseFactor() {
        if (pos >= tokens.size()) {
            Lexeme last = tokens.get(tokens.size() - 1);
            addError(new SyntaxDiagnostic(
                    "",
                    last.getLine(),
                    last.getEndColumn() + 1,
                    last.getEndColumn() + 1,
                    Messages.getString("arith.err.expectedOperand")));
            return null;
        }
        Lexeme t = tokens.get(pos);
        int code = t.getCode();
        if (code == ArithmeticScanner.CODE_NUMBER || code == ArithmeticScanner.CODE_IDENT) {
            pos++;
            return t.getText();
        }
        if (code == ArithmeticScanner.CODE_LPAREN) {
            pos++;
            String inner = parseExpr();
            if (inner == null) {
                return null;
            }
            if (pos >= tokens.size() || tokens.get(pos).getCode() != ArithmeticScanner.CODE_RPAREN) {
                Lexeme at = pos < tokens.size() ? tokens.get(pos) : t;
                addError(new SyntaxDiagnostic(
                        at.getText(),
                        at.getLine(),
                        at.getStartColumn(),
                        at.getEndColumn(),
                        Messages.getString("arith.err.expectedClose")));
                return null;
            }
            pos++;
            return inner;
        }
        addError(new SyntaxDiagnostic(
                t.getText(),
                t.getLine(),
                t.getStartColumn(),
                t.getEndColumn(),
                Messages.getString("arith.err.expectedOperand")));
        return null;
    }

    private void addError(SyntaxDiagnostic d) {
        errors.add(d);
        quads.clear();
    }

    private String nextTemp() {
        tempId++;
        return "t" + tempId;
    }
}
