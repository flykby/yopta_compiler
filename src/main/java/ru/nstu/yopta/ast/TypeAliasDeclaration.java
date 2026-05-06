package ru.nstu.yopta.ast;

import ru.nstu.yopta.Lexeme;

import java.util.List;

public final class TypeAliasDeclaration implements AstDeclaration {

    private final Lexeme name;
    private final List<FieldDeclaration> fields;

    public TypeAliasDeclaration(Lexeme name, List<FieldDeclaration> fields) {
        this.name = name;
        this.fields = fields != null ? List.copyOf(fields) : List.of();
    }

    public Lexeme getName() {
        return name;
    }

    public List<FieldDeclaration> getFields() {
        return fields;
    }
}
