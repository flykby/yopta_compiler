package ru.nstu.yopta;

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
 * Нейтрализация ошибок — паник-режим с синхронизацией по множествам допустимых токенов (метод Айронса).
 */
public final class TypeScriptInterfaceParser {

    private static final String KW_INTERFACE = "interface";
    private static final String KW_TYPE = "type";

    private final List<Lexeme> tokens;
    private final List<SyntaxDiagnostic> errors = new ArrayList<>();
    private int pos;

    private TypeScriptInterfaceParser(List<Lexeme> tokens) {
        this.tokens = tokens;
    }

    /**
     * Разбор исходной строки: лексика {@link TypeScriptInterfaceScanner#tokenize}, затем синтаксис.
     */
    public static ParseResult parse(String source) {
        List<Lexeme> lexemes = TypeScriptInterfaceScanner.tokenize(source);
        return new TypeScriptInterfaceParser(lexemes).parseProgram();
    }

    private ParseResult parseProgram() {
        while (true) {
            skipErrorsAtStatementLevel();
            Lexeme p = peek();
            if (p == null) {
                break;
            }
            if (isDeclarationStart(p)) {
                parseDeclaration();
            } else {
                report(p, "ожидалось объявление: ключевое слово «interface» или «type»");
                syncToDeclarationStart();
            }
        }
        return new ParseResult(errors);
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

    private void parseDeclaration() {
        Lexeme p = peek();
        if (p == null) {
            return;
        }
        if (p.getCode() == CODE_KEYWORD && KW_INTERFACE.equals(p.getText())) {
            parseInterfaceDecl();
        } else if (p.getCode() == CODE_KEYWORD && KW_TYPE.equals(p.getText())) {
            parseTypeAliasDecl();
        } else {
            report(p, "ожидалось «interface» или «type»");
            syncToDeclarationStart();
        }
    }

    private void parseInterfaceDecl() {
        take(); // interface
        if (!expectIdentifier("после «interface» ожидалось имя интерфейса")) {
            syncToDeclarationStart();
            return;
        }
        if (!expectToken(CODE_BRACE_OPEN, "ожидалось «{» перед телом интерфейса")) {
            syncToDeclarationStart();
            return;
        }
        parseFieldList();
        if (!expectToken(CODE_BRACE_CLOSE, "ожидалось «}» после полей интерфейса")) {
            syncToDeclarationStart();
            return;
        }
        if (!expectToken(CODE_SEMICOLON, "ожидалось «;» после объявления интерфейса")) {
            syncToDeclarationStart();
        }
    }

    private void parseTypeAliasDecl() {
        take(); // type
        if (!expectIdentifier("после «type» ожидалось имя псевдонима типа")) {
            syncToDeclarationStart();
            return;
        }
        if (!expectToken(CODE_EQUALS, "ожидалось «=» перед объектным типом")) {
            syncToDeclarationStart();
            return;
        }
        if (!expectToken(CODE_BRACE_OPEN, "ожидалось «{» — в объявлении type поддерживается только объектный тип")) {
            syncToDeclarationStart();
            return;
        }
        parseFieldList();
        if (!expectToken(CODE_BRACE_CLOSE, "ожидалось «}» после полей типа")) {
            syncToDeclarationStart();
            return;
        }
        if (!expectToken(CODE_SEMICOLON, "ожидалось «;» после объявления type")) {
            syncToDeclarationStart();
        }
    }

    /**
     * FieldList → ε | Field FieldList; Field → Id ':' Type ';'
     */
    private void parseFieldList() {
        while (true) {
            Lexeme p = peek();
            if (p == null) {
                report(null, "неожиданный конец файла внутри «{ ... }» (ожидалось поле или «}»)");
                return;
            }
            if (p.getCode() == CODE_ERROR) {
                Lexeme e = take();
                report(e, "недопустимый символ в исходном тексте");
                syncInBody();
                continue;
            }
            if (p.getCode() == CODE_BRACE_CLOSE) {
                return;
            }
            if (p.getCode() == CODE_IDENTIFIER) {
                parseField();
                continue;
            }
            report(p, "ожидалось имя поля или «}»");
            syncInBody();
        }
    }

    private void parseField() {
        take(); // identifier
        if (!expectToken(CODE_COLON, "ожидалось «:» после имени поля")) {
            syncInBody();
            return;
        }
        parseType();
        if (!expectToken(CODE_SEMICOLON, "ожидалось «;» после типа поля")) {
            syncInBody();
        }
    }

    private void parseType() {
        Lexeme t = peek();
        if (t == null) {
            report(null, "ожидался тип после «:»");
            return;
        }
        if (t.getCode() == CODE_ERROR) {
            report(take(), "недопустимый символ в исходном тексте");
            syncInBody();
            return;
        }
        if (t.getCode() == CODE_TYPE) {
            take();
        } else if (t.getCode() == CODE_IDENTIFIER) {
            if (TypeScriptInterfaceScanner.isAcceptableTypeLexeme(t)) {
                take();
            } else {
                Lexeme bad = take();
                report(bad, "неизвестный тип «" + bad.getText() + "» (ожидался встроенный тип или имя с заглавной буквы)");
                syncAfterBadTypeFragment();
            }
        } else {
            report(take(), "ожидался тип (встроенный идентификатор типа или пользовательское имя с заглавной буквы)");
            syncAfterBadTypeFragment();
        }
        parseArraySuffix();
    }

    private void parseArraySuffix() {
        Lexeme p = peek();
        if (p == null) {
            return;
        }
        if (p.getCode() != CODE_BRACKET_OPEN) {
            return;
        }
        take();
        Lexeme close = peek();
        if (close != null && close.getCode() == CODE_BRACKET_CLOSE) {
            take();
        } else {
            if (close != null) {
                report(close, "ожидалось «]» после «[»");
            } else {
                report(null, "ожидалось «]» после «[»");
            }
            syncInBody();
        }
    }

    /** Ожидание идентификатора (имя), не ключевое слово в роли имени. */
    private boolean expectIdentifier(String message) {
        Lexeme t = peek();
        if (t == null) {
            report(null, message);
            return false;
        }
        if (t.getCode() == CODE_IDENTIFIER) {
            take();
            return true;
        }
        report(t, message);
        return false;
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

    /**
     * Синхронизация на уровне программы: до следующего объявления или конца.
     */
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
     * Синхронизация внутри тела {@code { ... }}: до «;», «}» или начала следующего объявления.
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
            if (c == CODE_KEYWORD) {
                String k = p.getText();
                if (KW_INTERFACE.equals(k) || KW_TYPE.equals(k)) {
                    return;
                }
            }
            take();
        }
    }

    /** После ошибки в имени типа — до «;», «[», «}» или начала объявления. */
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
            if (c == CODE_KEYWORD) {
                String k = p.getText();
                if (KW_INTERFACE.equals(k) || KW_TYPE.equals(k)) {
                    return;
                }
            }
            take();
        }
    }
}
