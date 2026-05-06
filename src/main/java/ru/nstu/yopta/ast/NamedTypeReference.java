package ru.nstu.yopta.ast;

import ru.nstu.yopta.Lexeme;

/** Встроенный или пользовательский тип по лексеме имени. */
public final class NamedTypeReference implements TypeReference {

    private final Lexeme nameLexeme;

    public NamedTypeReference(Lexeme nameLexeme) {
        this.nameLexeme = nameLexeme;
    }

    public Lexeme getNameLexeme() {
        return nameLexeme;
    }
}
