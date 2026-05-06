package ru.nstu.yopta;

import java.util.ArrayList;
import java.util.List;

/**
 * Лексический разбор по грамматике задания: {@code num}, {@code id} (буква, далее буква/цифра/_/$),
 * операции {@code + - * / % **}, скобки. Последовательность {@code **} — одна лексема возведения в степень.
 * Пробелы, табуляция и переводы строк пропускаются.
 *
 * <pre>
 * stateDiagram-v2
 *     [*] --> Start
 *     Start --> Number: digit
 *     Start --> Ident: letter / _
 *     Start --> Star: *
 *     Start --> Single: + - / % ( )
 *     Start --> Skip: пробел / таб / CR / LF
 *     Start --> Err: прочий символ
 *     Star --> Pow: второй *
 *     Star --> Mult: иначе *
 *     Number --> Number: digit
 *     Ident --> Ident: letter / digit / _ / $
 * </pre>
 */
public final class ArithmeticScanner {

    public static final int CODE_NUMBER = 1;
    public static final int CODE_IDENT = 2;
    public static final int CODE_PLUS = 3;
    public static final int CODE_MINUS = 4;
    public static final int CODE_MULT = 5;
    public static final int CODE_DIV = 6;
    public static final int CODE_LPAREN = 7;
    public static final int CODE_RPAREN = 8;
    public static final int CODE_ERROR = 9;
    public static final int CODE_MOD = 10;
    public static final int CODE_POW = 11;

    private static final String TYPE_NUMBER = "number";
    private static final String TYPE_IDENT = "ident";
    private static final String TYPE_PLUS = "+";
    private static final String TYPE_MINUS = "-";
    private static final String TYPE_MULT = "*";
    private static final String TYPE_DIV = "/";
    private static final String TYPE_LPAREN = "(";
    private static final String TYPE_RPAREN = ")";
    private static final String TYPE_MOD = "%";
    private static final String TYPE_POW = "**";
    private static final String TYPE_ERROR = "error";

    private ArithmeticScanner() {
    }

    public static List<Lexeme> scan(String source) {
        List<Lexeme> out = new ArrayList<>();
        if (source == null) {
            return out;
        }
        int line = 1;
        int col = 1;
        int i = 0;
        int len = source.length();
        while (i < len) {
            char c = source.charAt(i);
            if (c == ' ' || c == '\t' || c == '\r') {
                i++;
                col++;
                continue;
            }
            if (c == '\n') {
                i++;
                line++;
                col = 1;
                continue;
            }
            int lineStart = line;
            int colStart = col;
            if (Character.isDigit(c)) {
                int j = i;
                while (j < len && Character.isDigit(source.charAt(j))) {
                    j++;
                }
                String text = source.substring(i, j);
                out.add(new Lexeme(CODE_NUMBER, TYPE_NUMBER, text, lineStart, colStart, colStart + text.length() - 1, false));
                col += j - i;
                i = j;
                continue;
            }
            if (isIdentStart(c)) {
                int j = i + 1;
                while (j < len && isIdentPart(source.charAt(j))) {
                    j++;
                }
                String text = source.substring(i, j);
                out.add(new Lexeme(CODE_IDENT, TYPE_IDENT, text, lineStart, colStart, colStart + text.length() - 1, false));
                col += j - i;
                i = j;
                continue;
            }
            int code;
            String type;
            String text;
            switch (c) {
                case '+':
                    code = CODE_PLUS;
                    type = TYPE_PLUS;
                    text = String.valueOf(c);
                    break;
                case '-':
                    code = CODE_MINUS;
                    type = TYPE_MINUS;
                    text = String.valueOf(c);
                    break;
                case '*':
                    if (i + 1 < len && source.charAt(i + 1) == '*') {
                        code = CODE_POW;
                        type = TYPE_POW;
                        text = TYPE_POW;
                        int endCol = colStart + 1;
                        out.add(new Lexeme(code, type, text, lineStart, colStart, endCol, false));
                        i += 2;
                        col += 2;
                        continue;
                    }
                    code = CODE_MULT;
                    type = TYPE_MULT;
                    text = String.valueOf(c);
                    break;
                case '/':
                    code = CODE_DIV;
                    type = TYPE_DIV;
                    text = String.valueOf(c);
                    break;
                case '%':
                    code = CODE_MOD;
                    type = TYPE_MOD;
                    text = String.valueOf(c);
                    break;
                case '(':
                    code = CODE_LPAREN;
                    type = TYPE_LPAREN;
                    text = String.valueOf(c);
                    break;
                case ')':
                    code = CODE_RPAREN;
                    type = TYPE_RPAREN;
                    text = String.valueOf(c);
                    break;
                default:
                    code = CODE_ERROR;
                    type = TYPE_ERROR;
                    text = String.valueOf(c);
                    out.add(new Lexeme(code, type, text, lineStart, colStart, colStart, true));
                    i++;
                    col++;
                    continue;
            }
            out.add(new Lexeme(code, type, text, lineStart, colStart, colStart + text.length() - 1, false));
            i += text.length();
            col += text.length();
        }
        return out;
    }

    private static boolean isIdentStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private static boolean isIdentPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }
}
