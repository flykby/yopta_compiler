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

    /** Состояния конечного автомата для валидации последовательности токенов объявлений. */
    private enum State {
        EXPECT_START,       // ожидается "interface" или "type"
        EXPECT_TYPE_ALIAS_NAME, // после "type" — имя псевдонима
        EXPECT_EQUALS,      // после имени в type — "="
        EXPECT_NAME,        // после "interface" — имя интерфейса
        EXPECT_OPEN_BRACE,  // ожидается "{"
        EXPECT_FIELD_OR_CLOSE, // ожидается имя поля или "}"
        EXPECT_COLON,       // ожидается ":"
        EXPECT_TYPE,        // ожидается тип (ключевое слово или идентификатор)
        EXPECT_ARRAY_OR_SEMI, // после типа: "[" или ";"
        EXPECT_BRACKET_CLOSE,  // ожидается "]"
        EXPECT_FIELD_SEMI,   // после "]" ожидается ";"
        EXPECT_FINAL_SEMI    // после "}" ожидается ";"
    }

    /**
     * Сканирует исходный текст и возвращает список лексем с координатами (строка, столбец).
     * Учитывает многострочность. Недопустимые символы выдаются как лексемы с кодом CODE_ERROR.
     */
    public static List<Lexeme> scan(String source) {
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

        return validateInterfaceStructure(result);
    }

    /**
     * Проверяет структуру объявлений конечным автоматом:
     * {@code interface NAME { ... };} и {@code type NAME = { ... };}.
     */
    private static List<Lexeme> validateInterfaceStructure(List<Lexeme> lexemes) {
        List<Lexeme> result = new ArrayList<>();
        int braceDepth = 0;
        State state = State.EXPECT_START;
        final int size = lexemes.size();
        Lexeme lastLexeme = null;

        for (int i = 0; i < size; i++) {
            Lexeme cur = lexemes.get(i);
            int code = cur.getCode();
            int prevDepth = braceDepth;

            if (code == CODE_BRACE_OPEN) braceDepth++;
            if (code == CODE_BRACE_CLOSE) braceDepth--;

            if (code == CODE_BRACE_CLOSE && braceDepth < 0) {
                result.add(errorLexeme(cur, "ошибка: лишняя '}'"));
                braceDepth = 0;
            }

            result.add(cur);
            lastLexeme = cur;

            if (code == CODE_WHITESPACE) continue;

            switch (state) {
                case EXPECT_START:
                    if (code == CODE_KEYWORD) {
                        String kw = cur.getText();
                        if (KEYWORD_INTERFACE.equals(kw)) {
                            state = State.EXPECT_NAME;
                        } else if (KEYWORD_TYPE.equals(kw)) {
                            state = State.EXPECT_TYPE_ALIAS_NAME;
                        } else {
                            result.add(errorLexeme(cur, "ошибка: ожидается ключевое слово 'interface' или 'type'"));
                        }
                    } else if (code == CODE_IDENTIFIER) {
                        result.add(errorLexeme(cur, "ошибка: ожидается ключевое слово 'interface' или 'type'"));
                        state = State.EXPECT_NAME;
                    } else if (code == CODE_BRACE_CLOSE || code == CODE_SEMICOLON || code == CODE_COLON
                            || code == CODE_TYPE || code == CODE_BRACKET_OPEN || code == CODE_BRACKET_CLOSE
                            || code == CODE_EQUALS) {
                        result.add(errorLexeme(cur, "ошибка: ожидается ключевое слово 'interface' или 'type'"));
                    }
                    break;

                case EXPECT_TYPE_ALIAS_NAME:
                    if (code == CODE_IDENTIFIER) {
                        state = State.EXPECT_EQUALS;
                    } else if (code == CODE_BRACE_OPEN) {
                        result.add(errorLexeme(cur, "ошибка: ожидается имя псевдонима типа перед '{'"));
                        state = State.EXPECT_FIELD_OR_CLOSE;
                    } else if (code == CODE_KEYWORD || code == CODE_TYPE) {
                        result.add(errorLexeme(cur, "ошибка: ожидается имя псевдонима типа"));
                    } else if (code == CODE_BRACE_CLOSE) {
                        result.add(errorLexeme(cur, "ошибка: ожидается '{'"));
                        state = State.EXPECT_FINAL_SEMI;
                    }
                    break;

                case EXPECT_EQUALS:
                    if (code == CODE_EQUALS) {
                        state = State.EXPECT_OPEN_BRACE;
                    } else if (code == CODE_BRACE_OPEN) {
                        result.add(errorLexeme(cur, "ошибка: ожидается '=' перед '{'"));
                        state = State.EXPECT_FIELD_OR_CLOSE;
                    } else if (code == CODE_IDENTIFIER || code == CODE_TYPE) {
                        result.add(errorLexeme(cur, "ошибка: ожидается '{'"));
                    } else if (code == CODE_BRACE_CLOSE) {
                        result.add(errorLexeme(cur, "ошибка: ожидается '='"));
                        state = State.EXPECT_FINAL_SEMI;
                    }
                    break;

                case EXPECT_NAME:
                    if (code == CODE_IDENTIFIER) {
                        state = State.EXPECT_OPEN_BRACE;
                    } else if (code == CODE_KEYWORD || code == CODE_TYPE) {
                        result.add(errorLexeme(cur, "ошибка: ожидается имя интерфейса"));
                    } else if (code == CODE_BRACE_OPEN) {
                        result.add(errorLexeme(cur, "ошибка: ожидается имя интерфейса перед '{'"));
                        state = State.EXPECT_FIELD_OR_CLOSE;
                    } else if (code == CODE_BRACE_CLOSE) {
                        result.add(errorLexeme(cur, "ошибка: ожидается '{'"));
                        state = State.EXPECT_FINAL_SEMI;
                    }
                    break;

                case EXPECT_OPEN_BRACE:
                    if (code == CODE_BRACE_OPEN) {
                        state = State.EXPECT_FIELD_OR_CLOSE;
                    } else if (code == CODE_BRACE_CLOSE) {
                        result.add(errorLexeme(cur, "ошибка: ожидается '{'"));
                        state = State.EXPECT_FINAL_SEMI;
                    } else if (code == CODE_IDENTIFIER || code == CODE_TYPE) {
                        result.add(errorLexeme(cur, "ошибка: ожидается '{'"));
                    } else if (code == CODE_EQUALS) {
                        result.add(errorLexeme(cur, "ошибка: ожидается '{'"));
                    }
                    break;

                case EXPECT_FIELD_OR_CLOSE:
                    if (code == CODE_IDENTIFIER) {
                        state = State.EXPECT_COLON;
                    } else if (code == CODE_BRACE_CLOSE) {
                        state = State.EXPECT_FINAL_SEMI;
                    } else if (code == CODE_KEYWORD) {
                        result.add(errorLexeme(cur, "ошибка: ожидается имя поля или '}'"));
                    } else if (code == CODE_COLON || code == CODE_SEMICOLON || code == CODE_BRACKET_OPEN || code == CODE_BRACKET_CLOSE) {
                        result.add(errorLexeme(cur, "ошибка: ожидается имя поля или '}'"));
                    }
                    break;

                case EXPECT_COLON:
                    if (code == CODE_COLON) {
                        state = State.EXPECT_TYPE;
                    } else if (code == CODE_IDENTIFIER || code == CODE_TYPE) {
                        result.add(errorLexeme(cur, "ошибка: ожидается ':'"));
                    } else if (code == CODE_BRACE_CLOSE) {
                        result.add(errorLexeme(cur, "ошибка: ожидается ':' после имени поля"));
                        state = State.EXPECT_FINAL_SEMI;
                    }
                    break;

                case EXPECT_TYPE:
                    if (code == CODE_TYPE) {
                        state = State.EXPECT_ARRAY_OR_SEMI;
                    } else if (code == CODE_IDENTIFIER) {
                        String text = cur.getText();
                        if (TYPE_KEYWORDS.contains(text)) {
                            state = State.EXPECT_ARRAY_OR_SEMI;
                        } else if (!text.isEmpty() && Character.isUpperCase(text.charAt(0))) {
                            state = State.EXPECT_ARRAY_OR_SEMI;
                        } else {
                            result.add(errorLexeme(cur, "ошибка: неизвестный тип '" + text + "'"));
                            state = State.EXPECT_ARRAY_OR_SEMI;
                        }
                    } else if (code == CODE_BRACE_CLOSE || code == CODE_SEMICOLON) {
                        result.add(errorLexeme(cur, "ошибка: ожидается тип после ':'"));
                        if (code == CODE_BRACE_CLOSE) state = State.EXPECT_FINAL_SEMI;
                        else state = State.EXPECT_FIELD_OR_CLOSE;
                    } else if (code == CODE_COLON || code == CODE_KEYWORD) {
                        result.add(errorLexeme(cur, "ошибка: ожидается тип"));
                    }
                    break;

                case EXPECT_ARRAY_OR_SEMI:
                    if (code == CODE_SEMICOLON) {
                        state = State.EXPECT_FIELD_OR_CLOSE;
                    } else if (code == CODE_BRACKET_OPEN) {
                        state = State.EXPECT_BRACKET_CLOSE;
                    } else if (code == CODE_BRACE_CLOSE) {
                        result.add(errorLexeme(cur, "ошибка: ожидается ';' после типа"));
                        state = State.EXPECT_FINAL_SEMI;
                    } else if (code == CODE_IDENTIFIER || code == CODE_TYPE) {
                        result.add(errorLexeme(cur, "ошибка: ожидается ';' или '[' после типа"));
                        state = State.EXPECT_COLON;
                    } else if (code == CODE_COLON) {
                        result.add(errorLexeme(cur, "ошибка: ожидается ';' после типа"));
                    }
                    break;

                case EXPECT_BRACKET_CLOSE:
                    if (code == CODE_BRACKET_CLOSE) {
                        state = State.EXPECT_FIELD_SEMI;
                    } else if (code == CODE_SEMICOLON || code == CODE_BRACE_CLOSE) {
                        result.add(errorLexeme(cur, "ошибка: ожидается ']'"));
                        if (code == CODE_BRACE_CLOSE) state = State.EXPECT_FINAL_SEMI;
                        else state = State.EXPECT_FIELD_OR_CLOSE;
                    } else if (code == CODE_IDENTIFIER || code == CODE_TYPE || code == CODE_COLON) {
                        result.add(errorLexeme(cur, "ошибка: ожидается ']'"));
                    }
                    break;

                case EXPECT_FIELD_SEMI:
                    if (code == CODE_SEMICOLON) {
                        state = State.EXPECT_FIELD_OR_CLOSE;
                    } else if (code == CODE_BRACE_CLOSE) {
                        result.add(errorLexeme(cur, "ошибка: ожидается ';' после типа массива"));
                        state = State.EXPECT_FINAL_SEMI;
                    } else if (code == CODE_IDENTIFIER || code == CODE_TYPE) {
                        result.add(errorLexeme(cur, "ошибка: ожидается ';' после ']'"));
                        state = State.EXPECT_COLON;
                    } else if (code == CODE_COLON) {
                        result.add(errorLexeme(cur, "ошибка: ожидается ';'"));
                    }
                    break;

                case EXPECT_FINAL_SEMI:
                    if (code == CODE_SEMICOLON) {
                        state = State.EXPECT_START;
                    } else if (code == CODE_KEYWORD) {
                        result.add(errorLexeme(cur, "ошибка: ожидается ';' после объявления"));
                        String kw = cur.getText();
                        if (KEYWORD_INTERFACE.equals(kw)) state = State.EXPECT_NAME;
                        else if (KEYWORD_TYPE.equals(kw)) state = State.EXPECT_TYPE_ALIAS_NAME;
                    } else if (code == CODE_IDENTIFIER || code == CODE_TYPE || code == CODE_BRACE_OPEN) {
                        result.add(errorLexeme(cur, "ошибка: ожидается ';' после '}'"));
                    }
                    break;
            }
        }

        if (braceDepth > 0 && lastLexeme != null) {
            int errLine = lastLexeme.getLine();
            int errCol = lastLexeme.getEndColumn() + 1;
            result.add(new Lexeme(CODE_ERROR, "ошибка: ожидается '}'", "", errLine, errCol, errCol, true));
        } else if (state == State.EXPECT_FINAL_SEMI && lastLexeme != null) {
            int errLine = lastLexeme.getLine();
            int errCol = lastLexeme.getEndColumn() + 1;
            result.add(new Lexeme(CODE_ERROR, "ошибка: ожидается ';' после '}'", "", errLine, errCol, errCol, true));
        }

        return result;
    }

    private static Lexeme errorLexeme(Lexeme at, String message) {
        return new Lexeme(CODE_ERROR, message, at.getText(), at.getLine(), at.getStartColumn(), at.getEndColumn(), true);
    }

    private static boolean isLetterOrUnderscore(char c) {
        return c == '_' || Character.isLetter(c);
    }

    private static boolean isLetterDigitOrUnderscore(char c) {
        return c == '_' || Character.isLetterOrDigit(c);
    }
}
