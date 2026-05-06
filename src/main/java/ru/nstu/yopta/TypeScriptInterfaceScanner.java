package ru.nstu.yopta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Лексический анализатор для объявлений TypeScript: {@code interface} и {@code type} с объектным типом
 * {@code type Имя = { ... };}.
 * Конечный автомат: начало → идентификатор / разделитель / пробел / ошибка.
 *
 * <pre>
 * stateDiagram-v2
 *     [*] --> Start
 *     Start --> Identifier: letter/_
 *     Start --> Delimiter: { } ; :
 *     Start --> Whitespace: space/tab/newline
 *     Start --> Error: other
 *     Identifier --> Identifier: letter/digit/_
 *     Identifier --> [*]: other
 *     Whitespace --> Whitespace: space/tab/newline
 *     Whitespace --> [*]: other
 * </pre>
 */
public final class TypeScriptInterfaceScanner {

    public static final int CODE_KEYWORD = 1;
    public static final int CODE_IDENTIFIER = 2;
    public static final int CODE_TYPE = 3;
    public static final int CODE_BRACE_OPEN = 4;
    public static final int CODE_BRACE_CLOSE = 5;
    public static final int CODE_SEMICOLON = 6;
    public static final int CODE_COLON = 7;
    public static final int CODE_WHITESPACE = 8;
    public static final int CODE_ERROR = 9;
    public static final int CODE_BRACKET_OPEN = 10;
    public static final int CODE_BRACKET_CLOSE = 11;
    public static final int CODE_EQUALS = 12;

    private static final String KEYWORD_INTERFACE = "interface";
    private static final String KEYWORD_TYPE = "type";
    private static final Set<String> TYPE_KEYWORDS = Set.of("string", "number", "boolean", "any", "unknown", "object", "void", "null", "undefined");

    /**
     * Возвращает true, если идентификатор или лексема типа допустимы как имя типа в теле объявления
     * (встроенный тип, либо пользовательский идентификатор с заглавной буквы).
     */
    public static boolean isAcceptableTypeLexeme(Lexeme lex) {
        if (lex == null) return false;
        int code = lex.getCode();
        if (code == CODE_TYPE) return true;
        if (code != CODE_IDENTIFIER) return false;
        String t = lex.getText();
        if (TYPE_KEYWORDS.contains(t)) return true;
        return !t.isEmpty() && Character.isUpperCase(t.charAt(0));
    }

    /** Встроенное имя типа (как в лексике: {@code string}, {@code number}, …). */
    public static boolean isBuiltinTypeName(String name) {
        return name != null && TYPE_KEYWORDS.contains(name);
    }

    /**
     * Только лексический разбор: символы → лексемы. Структурная проверка — в {@link TypeScriptInterfaceParser}.
     */
    public static List<Lexeme> tokenize(String source) {
        List<Lexeme> result = new ArrayList<>();
        if (source == null) return result;

        int line = 1;
        int col = 1;
        int i = 0;
        final int len = source.length();

        while (i < len) {
            char c = source.charAt(i);
            int lineStart = line;
            int colStart = col;

            if (c == '\n') {
                line++;
                col = 1;
                i++;
                result.add(new Lexeme(CODE_WHITESPACE, "разделитель (перенос строки)", "\n", lineStart, colStart, colStart, false));
                continue;
            }
            if (c == ' ' || c == '\t' || c == '\r') {
                int start = i;
                while (i < len && (source.charAt(i) == ' ' || source.charAt(i) == '\t' || source.charAt(i) == '\r')) {
                    if (source.charAt(i) == '\n') break;
                    col++;
                    i++;
                }
                result.add(new Lexeme(CODE_WHITESPACE, "разделитель (пробел)", source.substring(start, i), lineStart, colStart, col - 1, false));
                continue;
            }

            switch (c) {
                case '{':
                    result.add(new Lexeme(CODE_BRACE_OPEN, "разделитель `{`", "{", line, col, col, false));
                    col++;
                    i++;
                    continue;
                case '}':
                    result.add(new Lexeme(CODE_BRACE_CLOSE, "разделитель `}`", "}", line, col, col, false));
                    col++;
                    i++;
                    continue;
                case ';':
                    result.add(new Lexeme(CODE_SEMICOLON, "конец оператора", ";", line, col, col, false));
                    col++;
                    i++;
                    continue;
                case ':':
                    result.add(new Lexeme(CODE_COLON, "разделитель `:`", ":", line, col, col, false));
                    col++;
                    i++;
                    continue;
                case '[':
                    result.add(new Lexeme(CODE_BRACKET_OPEN, "разделитель `[`", "[", line, col, col, false));
                    col++;
                    i++;
                    continue;
                case ']':
                    result.add(new Lexeme(CODE_BRACKET_CLOSE, "разделитель `]`", "]", line, col, col, false));
                    col++;
                    i++;
                    continue;
                case '=':
                    result.add(new Lexeme(CODE_EQUALS, "оператор присваивания", "=", line, col, col, false));
                    col++;
                    i++;
                    continue;
                default:
                    break;
            }

            if (isLetterOrUnderscore(c)) {
                int start = i;
                while (i < len && isLetterDigitOrUnderscore(source.charAt(i))) {
                    i++;
                    col++;
                }
                String token = source.substring(start, i);
                int code;
                String typeName;
                if (KEYWORD_INTERFACE.equals(token) || KEYWORD_TYPE.equals(token)) {
                    code = CODE_KEYWORD;
                    typeName = "ключевое слово";
                } else if (TYPE_KEYWORDS.contains(token)) {
                    code = CODE_TYPE;
                    typeName = "тип данных";
                } else {
                    code = CODE_IDENTIFIER;
                    typeName = "идентификатор";
                }
                result.add(new Lexeme(code, typeName, token, lineStart, colStart, col - 1, false));
                continue;
            }

            result.add(new Lexeme(CODE_ERROR, "ошибка (недопустимый символ)", String.valueOf(c), line, col, col, true));
            col++;
            i++;
        }

        return result;
    }

    /**
     * Сканирует исходный текст и возвращает список лексем (то же, что {@link #tokenize}).
     * Учитывает многострочность. Недопустимые символы выдаются как лексемы с кодом CODE_ERROR.
     */
    public static List<Lexeme> scan(String source) {
        return tokenize(source);
    }

    private static boolean isLetterOrUnderscore(char c) {
        return c == '_' || Character.isLetter(c);
    }

    private static boolean isLetterDigitOrUnderscore(char c) {
        return c == '_' || Character.isLetterOrDigit(c);
    }
}
