package ru.nstu.yopta.ast;

import ru.nstu.yopta.Lexeme;

/** Поле объекта: {@code имя: тип;}. */
public final class FieldDeclaration {

    private final Lexeme name;
    private final TypeReference type;

    public FieldDeclaration(Lexeme name, TypeReference type) {
        this.name = name;
        this.type = type;
    }

    public Lexeme getName() {
        return name;
    }

    public TypeReference getType() {
        return type;
    }
}
