package ru.nstu.yopta;

import ru.nstu.yopta.ast.ArrayTypeReference;
import ru.nstu.yopta.ast.AstDeclaration;
import ru.nstu.yopta.ast.AstProgram;
import ru.nstu.yopta.ast.FieldDeclaration;
import ru.nstu.yopta.ast.InterfaceDeclaration;
import ru.nstu.yopta.ast.NamedTypeReference;
import ru.nstu.yopta.ast.TypeAliasDeclaration;
import ru.nstu.yopta.ast.TypeReference;

import java.util.ArrayList;
import java.util.List;

import static ru.nstu.yopta.TypeScriptInterfaceScanner.CODE_BRACE_CLOSE;
import static ru.nstu.yopta.TypeScriptInterfaceScanner.CODE_BRACE_OPEN;
import static ru.nstu.yopta.TypeScriptInterfaceScanner.CODE_BRACKET_CLOSE;
import static ru.nstu.yopta.TypeScriptInterfaceScanner.CODE_BRACKET_OPEN;
import static ru.nstu.yopta.TypeScriptInterfaceScanner.CODE_COLON;
import static ru.nstu.yopta.TypeScriptInterfaceScanner.CODE_EQUALS;
import static ru.nstu.yopta.TypeScriptInterfaceScanner.CODE_ERROR;
import static ru.nstu.yopta.TypeScriptInterfaceScanner.CODE_IDENTIFIER;
import static ru.nstu.yopta.TypeScriptInterfaceScanner.CODE_KEYWORD;
import static ru.nstu.yopta.TypeScriptInterfaceScanner.CODE_SEMICOLON;
import static ru.nstu.yopta.TypeScriptInterfaceScanner.CODE_TYPE;
import static ru.nstu.yopta.TypeScriptInterfaceScanner.CODE_WHITESPACE;

/**
 * Синтаксический анализатор (рекурсивный спуск) для подмножества TypeScript:
 * {@code interface Id { ... };} и {@code type Id = { ... };}.
 * Построение AST при разборе. Нейтрализация ошибок — метод Айронса.
 */
public final class TypeScriptInterfaceParser {

    private static final String KW_INTERFACE = "interface";
    private static final String KW_TYPE = "type";

    /** Защита от бесконечного добавления ошибок при неверной синхронизации в теле «{ ... }». */
    private static final int MAX_SYNTAX_DIAGNOSTICS = 500;

    /** Страховка от зацикливания при ошибках в разборе. */
    private static final int MAX_PROGRAM_ITERATIONS = 500_000;
    private static final int MAX_FIELD_LIST_ITERATIONS = 500_000;

    private final List<Lexeme> tokens;
    private final List<SyntaxDiagnostic> errors = new ArrayList<>();
    private int pos;

    private TypeScriptInterfaceParser(List<Lexeme> tokens) {
        this.tokens = tokens;
    }

    /**
     * Разбор исходной строки: лексика {@link TypeScriptInterfaceScanner#tokenize}, затем синтаксис и AST.
     */
    public static ParseResult parse(String source) {
        List<Lexeme> lexemes = TypeScriptInterfaceScanner.tokenize(source);
        return new TypeScriptInterfaceParser(lexemes).parseProgram();
    }

    private ParseResult parseProgram() {
        List<AstDeclaration> decls = new ArrayList<>();
        int programSteps = 0;
        while (true) {
            if (++programSteps > MAX_PROGRAM_ITERATIONS) {
                report(peek(), "превышен внутренний лимит итераций разбора программы");
                break;
            }
            skipErrorsAtStatementLevel();
            Lexeme p = peek();
            if (p == null) {
                break;
            }
            if (isDeclarationStart(p)) {
                AstDeclaration d = parseDeclaration();
                if (d != null) {
                    decls.add(d);
                }
            } else {
                report(p, "ожидалось объявление: ключевое слово «interface» или «type»");
                syncToDeclarationStart();
            }
        }
        return new ParseResult(errors, new AstProgram(decls));
    }

    private void skipErrorsAtStatementLevel() {
        while (true) {
            Lexeme p = peek();
            if (p == null || p.getCode() != CODE_ERROR) {
                return;
            }
            Lexeme e = take();
            report(e, "недопустимый символ в исходном тексте");
        }
    }

    private boolean isDeclarationStart(Lexeme p) {
        if (p.getCode() != CODE_KEYWORD) {
            return false;
        }
        String k = p.getText();
        return KW_INTERFACE.equals(k) || KW_TYPE.equals(k);
    }

    private AstDeclaration parseDeclaration() {
        Lexeme p = peek();
        if (p == null) {
            return null;
        }
        if (p.getCode() == CODE_KEYWORD && KW_INTERFACE.equals(p.getText())) {
            return parseInterfaceDecl();
        }
        if (p.getCode() == CODE_KEYWORD && KW_TYPE.equals(p.getText())) {
            return parseTypeAliasDecl();
        }
        report(p, "ожидалось «interface» или «type»");
        syncToDeclarationStart();
        return null;
    }

    private InterfaceDeclaration parseInterfaceDecl() {
        take(); // interface
        Lexeme name = takeIdentifier("после «interface» ожидалось имя интерфейса");
        if (name == null) {
            syncToDeclarationStart();
            return null;
        }
        if (!expectToken(CODE_BRACE_OPEN, "ожидалось «{» перед телом интерфейса")) {
            syncToDeclarationStart();
            return null;
        }
        List<FieldDeclaration> fields = parseFieldList();
        if (!expectToken(CODE_BRACE_CLOSE, "ожидалось «}» после полей интерфейса")) {
            syncToDeclarationStart();
            return null;
        }
        if (!expectToken(CODE_SEMICOLON, "ожидалось «;» после объявления интерфейса")) {
            syncToDeclarationStart();
            return null;
        }
        return new InterfaceDeclaration(name, fields);
    }

    private TypeAliasDeclaration parseTypeAliasDecl() {
        take(); // type
        Lexeme name = takeIdentifier("после «type» ожидалось имя псевдонима типа");
        if (name == null) {
            syncToDeclarationStart();
            return null;
        }
        if (!expectToken(CODE_EQUALS, "ожидалось «=» перед объектным типом")) {
            syncToDeclarationStart();
            return null;
        }
        if (!expectToken(CODE_BRACE_OPEN, "ожидалось «{» — в объявлении type поддерживается только объектный тип")) {
            syncToDeclarationStart();
            return null;
        }
        List<FieldDeclaration> fields = parseFieldList();
        if (!expectToken(CODE_BRACE_CLOSE, "ожидалось «}» после полей типа")) {
            syncToDeclarationStart();
            return null;
        }
        if (!expectToken(CODE_SEMICOLON, "ожидалось «;» после объявления type")) {
            syncToDeclarationStart();
            return null;
        }
        return new TypeAliasDeclaration(name, fields);
    }

    /**
     * FieldList → ε | Field FieldList; Field → Id ':' Type ';'
     */
    private List<FieldDeclaration> parseFieldList() {
        List<FieldDeclaration> fields = new ArrayList<>();
        int fieldSteps = 0;
        while (true) {
            if (++fieldSteps > MAX_FIELD_LIST_ITERATIONS) {
                report(peek(), "превышен внутренний лимит полей в «{ ... }»");
                return fields;
            }
            Lexeme p = peek();
            if (p == null) {
                report(null, "неожиданный конец файла внутри «{ ... }» (ожидалось поле или «}»)");
                return fields;
            }
            if (p.getCode() == CODE_ERROR) {
                Lexeme e = take();
                report(e, "недопустимый символ в исходном тексте");
                syncInBody();
                continue;
            }
            if (p.getCode() == CODE_BRACE_CLOSE) {
                return fields;
            }
            if (p.getCode() == CODE_IDENTIFIER) {
                FieldDeclaration f = parseField();
                if (f != null) {
                    fields.add(f);
                }
                continue;
            }
            report(p, "ожидалось имя поля или «}»");
            syncInBody();
        }
    }

    private FieldDeclaration parseField() {
        Lexeme name = takeIdentifier("ожидалось имя поля");
        if (name == null) {
            syncInBody();
            return null;
        }
        if (!expectToken(CODE_COLON, "ожидалось «:» после имени поля")) {
            syncInBody();
            return null;
        }
        TypeReference type = parseType();
        if (type == null) {
            return null;
        }
        if (!expectToken(CODE_SEMICOLON, "ожидалось «;» после типа поля")) {
            syncInBody();
            return null;
        }
        return new FieldDeclaration(name, type);
    }

    private TypeReference parseType() {
        TypeReference base = parseTypeBase();
        if (base == null) {
            return null;
        }
        while (true) {
            Lexeme p = peek();
            if (p == null) {
                return base;
            }
            if (p.getCode() != CODE_BRACKET_OPEN) {
                return base;
            }
            take();
            if (!expectToken(CODE_BRACKET_CLOSE, "ожидалось «]» после «[»")) {
                syncInBody();
                return base;
            }
            base = new ArrayTypeReference(base);
        }
    }

    private TypeReference parseTypeBase() {
        Lexeme t = peek();
        if (t == null) {
            report(null, "ожидался тип после «:»");
            return null;
        }
        if (t.getCode() == CODE_ERROR) {
            report(take(), "недопустимый символ в исходном тексте");
            syncInBody();
            return null;
        }
        if (t.getCode() == CODE_TYPE) {
            return new NamedTypeReference(take());
        }
        if (t.getCode() == CODE_IDENTIFIER) {
            if (TypeScriptInterfaceScanner.isAcceptableTypeLexeme(t)) {
                return new NamedTypeReference(take());
            }
            Lexeme bad = take();
            report(bad, "неизвестный тип «" + bad.getText() + "» (ожидался встроенный тип или имя с заглавной буквы)");
            syncAfterBadTypeFragment();
            return null;
        }
        report(take(), "ожидался тип (встроенный идентификатор типа или пользовательское имя с заглавной буквы)");
        syncAfterBadTypeFragment();
        return null;
    }

    private Lexeme takeIdentifier(String message) {
        Lexeme t = peek();
        if (t != null && t.getCode() == CODE_IDENTIFIER) {
            return take();
        }
        report(t, message);
        return null;
    }

    private boolean expectToken(int code, String message) {
        Lexeme t = peek();
        if (t == null) {
            report(null, message);
            return false;
        }
        if (t.getCode() == code) {
            take();
            return true;
        }
        report(t, message);
        return false;
    }

    private void report(Lexeme at, String description) {
        if (errors.size() >= MAX_SYNTAX_DIAGNOSTICS) {
            return;
        }
        if (at == null) {
            Lexeme anchor = lastConsumedOrEnd();
            errors.add(new SyntaxDiagnostic("ε", anchor.getLine(), anchor.getStartColumn(), anchor.getEndColumn(), description));
        } else {
            errors.add(new SyntaxDiagnostic(at.getText(), at.getLine(), at.getStartColumn(), at.getEndColumn(), description));
        }
    }

    private Lexeme lastConsumedOrEnd() {
        int i = pos - 1;
        while (i >= 0 && tokens.get(i).getCode() == CODE_WHITESPACE) {
            i--;
        }
        if (i >= 0) {
            return tokens.get(i);
        }
        if (!tokens.isEmpty()) {
            Lexeme last = tokens.get(tokens.size() - 1);
            int line = last.getLine();
            int col = last.getEndColumn();
            return new Lexeme(CODE_ERROR, "", "", line, col, col, false);
        }
        return new Lexeme(CODE_ERROR, "", "", 1, 1, 1, false);
    }

    private Lexeme peek() {
        int i = nextNonWs(pos);
        if (i >= tokens.size()) {
            return null;
        }
        return tokens.get(i);
    }

    private Lexeme take() {
        pos = nextNonWs(pos);
        if (pos >= tokens.size()) {
            return null;
        }
        return tokens.get(pos++);
    }

    private int nextNonWs(int from) {
        int i = from;
        while (i < tokens.size() && tokens.get(i).getCode() == CODE_WHITESPACE) {
            i++;
        }
        return i;
    }

    private void syncToDeclarationStart() {
        while (true) {
            Lexeme p = peek();
            if (p == null) {
                return;
            }
            if (p.getCode() == CODE_KEYWORD) {
                String k = p.getText();
                if (KW_INTERFACE.equals(k) || KW_TYPE.equals(k)) {
                    return;
                }
            }
            take();
        }
    }

    /**
     * Внутри «{ ... }» синхронизация только по «;» или «}». Не останавливаться на «interface»/«type» без
     * потребления токена — иначе бесконечный цикл в {@link #parseFieldList()}.
     */
    private void syncInBody() {
        while (true) {
            Lexeme p = peek();
            if (p == null) {
                return;
            }
            int c = p.getCode();
            if (c == CODE_SEMICOLON || c == CODE_BRACE_CLOSE) {
                return;
            }
            take();
        }
    }

    private void syncAfterBadTypeFragment() {
        while (true) {
            Lexeme p = peek();
            if (p == null) {
                return;
            }
            int c = p.getCode();
            if (c == CODE_SEMICOLON || c == CODE_BRACKET_OPEN || c == CODE_BRACE_CLOSE) {
                return;
            }
            take();
        }
    }
}
