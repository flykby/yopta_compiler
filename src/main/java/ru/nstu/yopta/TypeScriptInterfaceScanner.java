package ru.nstu.yopta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Лексический анализатор для объявлений TypeScript: {@code interface} и {@code type} с объектным типом
 * {@code type Имя = { ... };}.
 * <p>
 * <b>Условные коды лексем (полная таблица):</b>
 * <ul>
 *   <li>1 — ключевое слово {@code type}</li>
 *   <li>2 — ключевое слово {@code interface}</li>
 *   <li>3 — встроенный тип {@code string}</li>
 *   <li>4 — встроенный тип {@code number}</li>
 *   <li>5 — пустая входная строка (единственная лексема при {@code ""})</li>
 *   <li>6 — {@code boolean}</li>
 *   <li>7 — {@code any}</li>
 *   <li>8 — {@code unknown}</li>
 *   <li>9 — {@code object}</li>
 *   <li>10 — {@code void}</li>
 *   <li>11 — {@code null}</li>
 *   <li>12 — {@code undefined}</li>
 *   <li>13 — идентификатор</li>
 *   <li>14 — «{»</li>
 *   <li>15 — «}»</li>
 *   <li>16 — «;»</li>
 *   <li>17 — «:»</li>
 *   <li>18 — пробел / таб / перевод строки</li>
 *   <li>19 — ошибка (недопустимый символ или нарушение структуры)</li>
 *   <li>20 — «[»</li>
 *   <li>21 — «]»</li>
 *   <li>22 — «=»</li>
 * </ul>
 */
public final class TypeScriptInterfaceScanner {

    public static final int CODE_KW_TYPE = 1;
    public static final int CODE_KW_INTERFACE = 2;
    public static final int CODE_TYPE_STRING = 3;
    public static final int CODE_TYPE_NUMBER = 4;
    /** Единственная лексема при пустом входе {@code ""}. */
    public static final int CODE_EMPTY = 5;
    public static final int CODE_TYPE_BOOLEAN = 6;
    public static final int CODE_TYPE_ANY = 7;
    public static final int CODE_TYPE_UNKNOWN = 8;
    public static final int CODE_TYPE_OBJECT = 9;
    public static final int CODE_TYPE_VOID = 10;
    public static final int CODE_TYPE_NULL = 11;
    public static final int CODE_TYPE_UNDEFINED = 12;
    public static final int CODE_IDENTIFIER = 13;
    public static final int CODE_BRACE_OPEN = 14;
    public static final int CODE_BRACE_CLOSE = 15;
    public static final int CODE_SEMICOLON = 16;
    public static final int CODE_COLON = 17;
    public static final int CODE_WHITESPACE = 18;
    public static final int CODE_ERROR = 19;
    public static final int CODE_BRACKET_OPEN = 20;
    public static final int CODE_BRACKET_CLOSE = 21;
    public static final int CODE_EQUALS = 22;

    private static final String KEYWORD_INTERFACE = "interface";
    private static final String KEYWORD_TYPE = "type";
    private static final Set<String> TYPE_KEYWORDS = Set.of("string", "number", "boolean", "any", "unknown", "object", "void", "null", "undefined");

    /** true, если код соответствует одному из встроенных имён типов (3, 4, 6–12). */
    public static boolean isBuiltinTypeCode(int code) {
        switch (code) {
            case CODE_TYPE_STRING:
            case CODE_TYPE_NUMBER:
            case CODE_TYPE_BOOLEAN:
            case CODE_TYPE_ANY:
            case CODE_TYPE_UNKNOWN:
            case CODE_TYPE_OBJECT:
            case CODE_TYPE_VOID:
            case CODE_TYPE_NULL:
            case CODE_TYPE_UNDEFINED:
                return true;
            default:
                return false;
        }
    }

    public static boolean isDeclarationKeywordCode(int code) {
        return code == CODE_KW_TYPE || code == CODE_KW_INTERFACE;
    }

    /** Состояния конечного автомата для валидации последовательности токенов объявлений. */
    private enum State {
        EXPECT_START,
        EXPECT_TYPE_ALIAS_NAME,
        EXPECT_EQUALS,
        EXPECT_NAME,
        EXPECT_OPEN_BRACE,
        EXPECT_FIELD_OR_CLOSE,
        EXPECT_COLON,
        EXPECT_TYPE,
        EXPECT_ARRAY_OR_SEMI,
        EXPECT_BRACKET_CLOSE,
        EXPECT_FIELD_SEMI,
        EXPECT_FINAL_SEMI
    }

    /**
     * Сканирует исходный текст и возвращает список лексем с координатами (строка, столбец).
     * Учитывает многострочность. Недопустимые символы выдаются как лексемы с кодом CODE_ERROR.
     */
    public static List<Lexeme> scan(String source) {
        List<Lexeme> result = new ArrayList<>();
        if (source == null) {
            return result;
        }
        if (source.isEmpty()) {
            result.add(new Lexeme(CODE_EMPTY, "пустая строка (вход)", "", 1, 1, 1, false));
            return validateInterfaceStructure(result);
        }

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
                int code = classifyWord(token);
                String typeName = typeNameForWordCode(code);
                result.add(new Lexeme(code, typeName, token, lineStart, colStart, col - 1, false));
                continue;
            }

            result.add(new Lexeme(CODE_ERROR, "ошибка (недопустимый символ)", String.valueOf(c), line, col, col, true));
            col++;
            i++;
        }

        return validateInterfaceStructure(result);
    }

    private static int classifyWord(String token) {
        if (KEYWORD_TYPE.equals(token)) {
            return CODE_KW_TYPE;
        }
        if (KEYWORD_INTERFACE.equals(token)) {
            return CODE_KW_INTERFACE;
        }
        switch (token) {
            case "string":
                return CODE_TYPE_STRING;
            case "number":
                return CODE_TYPE_NUMBER;
            case "boolean":
                return CODE_TYPE_BOOLEAN;
            case "any":
                return CODE_TYPE_ANY;
            case "unknown":
                return CODE_TYPE_UNKNOWN;
            case "object":
                return CODE_TYPE_OBJECT;
            case "void":
                return CODE_TYPE_VOID;
            case "null":
                return CODE_TYPE_NULL;
            case "undefined":
                return CODE_TYPE_UNDEFINED;
            default:
                return CODE_IDENTIFIER;
        }
    }

    private static String typeNameForWordCode(int code) {
        switch (code) {
            case CODE_KW_TYPE:
                return "ключевое слово type";
            case CODE_KW_INTERFACE:
                return "ключевое слово interface";
            case CODE_TYPE_STRING:
                return "тип данных string";
            case CODE_TYPE_NUMBER:
                return "тип данных number";
            case CODE_TYPE_BOOLEAN:
                return "тип данных boolean";
            case CODE_TYPE_ANY:
                return "тип данных any";
            case CODE_TYPE_UNKNOWN:
                return "тип данных unknown";
            case CODE_TYPE_OBJECT:
                return "тип данных object";
            case CODE_TYPE_VOID:
                return "тип данных void";
            case CODE_TYPE_NULL:
                return "тип данных null";
            case CODE_TYPE_UNDEFINED:
                return "тип данных undefined";
            case CODE_IDENTIFIER:
                return "идентификатор";
            default:
                return "лексема";
        }
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

            if (code == CODE_BRACE_OPEN) braceDepth++;
            if (code == CODE_BRACE_CLOSE) braceDepth--;

            if (code == CODE_BRACE_CLOSE && braceDepth < 0) {
                result.add(errorLexeme(cur, "ошибка: лишняя '}'"));
                braceDepth = 0;
            }

            result.add(cur);
            lastLexeme = cur;

            if (code == CODE_WHITESPACE || code == CODE_EMPTY) continue;

            switch (state) {
                case EXPECT_START:
                    if (code == CODE_KW_INTERFACE) {
                        state = State.EXPECT_NAME;
                    } else if (code == CODE_KW_TYPE) {
                        state = State.EXPECT_TYPE_ALIAS_NAME;
                    } else if (code == CODE_IDENTIFIER) {
                        result.add(errorLexeme(cur, "ошибка: ожидается ключевое слово 'interface' или 'type'"));
                        state = State.EXPECT_NAME;
                    } else if (code == CODE_BRACE_CLOSE || code == CODE_SEMICOLON || code == CODE_COLON
                            || isBuiltinTypeCode(code) || code == CODE_BRACKET_OPEN || code == CODE_BRACKET_CLOSE
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
                    } else if (isDeclarationKeywordCode(code) || isBuiltinTypeCode(code)) {
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
                    } else if (code == CODE_IDENTIFIER || isBuiltinTypeCode(code)) {
                        result.add(errorLexeme(cur, "ошибка: ожидается '{'"));
                    } else if (code == CODE_BRACE_CLOSE) {
                        result.add(errorLexeme(cur, "ошибка: ожидается '='"));
                        state = State.EXPECT_FINAL_SEMI;
                    }
                    break;

                case EXPECT_NAME:
                    if (code == CODE_IDENTIFIER) {
                        state = State.EXPECT_OPEN_BRACE;
                    } else if (isDeclarationKeywordCode(code) || isBuiltinTypeCode(code)) {
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
                    } else if (code == CODE_IDENTIFIER || isBuiltinTypeCode(code)) {
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
                    } else if (isDeclarationKeywordCode(code)) {
                        result.add(errorLexeme(cur, "ошибка: ожидается имя поля или '}'"));
                    } else if (code == CODE_COLON || code == CODE_SEMICOLON || code == CODE_BRACKET_OPEN || code == CODE_BRACKET_CLOSE) {
                        result.add(errorLexeme(cur, "ошибка: ожидается имя поля или '}'"));
                    }
                    break;

                case EXPECT_COLON:
                    if (code == CODE_COLON) {
                        state = State.EXPECT_TYPE;
                    } else if (code == CODE_IDENTIFIER || isBuiltinTypeCode(code)) {
                        result.add(errorLexeme(cur, "ошибка: ожидается ':'"));
                    } else if (code == CODE_BRACE_CLOSE) {
                        result.add(errorLexeme(cur, "ошибка: ожидается ':' после имени поля"));
                        state = State.EXPECT_FINAL_SEMI;
                    }
                    break;

                case EXPECT_TYPE:
                    if (isBuiltinTypeCode(code)) {
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
                    } else if (code == CODE_COLON || isDeclarationKeywordCode(code)) {
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
                    } else if (code == CODE_IDENTIFIER || isBuiltinTypeCode(code)) {
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
                    } else if (code == CODE_IDENTIFIER || isBuiltinTypeCode(code) || code == CODE_COLON) {
                        result.add(errorLexeme(cur, "ошибка: ожидается ']'"));
                    }
                    break;

                case EXPECT_FIELD_SEMI:
                    if (code == CODE_SEMICOLON) {
                        state = State.EXPECT_FIELD_OR_CLOSE;
                    } else if (code == CODE_BRACE_CLOSE) {
                        result.add(errorLexeme(cur, "ошибка: ожидается ';' после типа массива"));
                        state = State.EXPECT_FINAL_SEMI;
                    } else if (code == CODE_IDENTIFIER || isBuiltinTypeCode(code)) {
                        result.add(errorLexeme(cur, "ошибка: ожидается ';' после ']'"));
                        state = State.EXPECT_COLON;
                    } else if (code == CODE_COLON) {
                        result.add(errorLexeme(cur, "ошибка: ожидается ';'"));
                    }
                    break;

                case EXPECT_FINAL_SEMI:
                    if (code == CODE_SEMICOLON) {
                        state = State.EXPECT_START;
                    } else if (code == CODE_KW_INTERFACE || code == CODE_KW_TYPE) {
                        result.add(errorLexeme(cur, "ошибка: ожидается ';' после объявления"));
                        if (code == CODE_KW_INTERFACE) state = State.EXPECT_NAME;
                        else state = State.EXPECT_TYPE_ALIAS_NAME;
                    } else if (code == CODE_IDENTIFIER || isBuiltinTypeCode(code) || code == CODE_BRACE_OPEN) {
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
