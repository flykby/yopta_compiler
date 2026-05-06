package ru.nstu.yopta.ast;

import java.util.Collections;
import java.util.List;

/** Корень AST: последовательность объявлений верхнего уровня. */
public final class AstProgram {

    private final List<AstDeclaration> declarations;

    public AstProgram(List<AstDeclaration> declarations) {
        this.declarations = declarations != null ? List.copyOf(declarations) : List.of();
    }

    public List<AstDeclaration> getDeclarations() {
        return declarations;
    }

    public static AstProgram empty() {
        return new AstProgram(Collections.emptyList());
    }
}
