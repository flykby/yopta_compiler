package ru.nstu.yopta;

import java.util.List;

import ru.nstu.yopta.ast.ArrayTypeReference;
import ru.nstu.yopta.ast.AstDeclaration;
import ru.nstu.yopta.ast.AstProgram;
import ru.nstu.yopta.ast.FieldDeclaration;
import ru.nstu.yopta.ast.InterfaceDeclaration;
import ru.nstu.yopta.ast.NamedTypeReference;
import ru.nstu.yopta.ast.TypeAliasDeclaration;
import ru.nstu.yopta.ast.TypeReference;

/**
 * Текстовое дерево AST (отступы, символы {@code ├──} / {@code └──}).
 */
public final class AstPrinter {

    private AstPrinter() {
    }

    public static String print(AstProgram program) {
        if (program == null || program.getDeclarations().isEmpty()) {
            return "(пустая программа)\n";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Program\n");
        List<AstDeclaration> decls = program.getDeclarations();
        for (int i = 0; i < decls.size(); i++) {
            boolean last = i == decls.size() - 1;
            printDeclaration(sb, "", decls.get(i), last);
        }
        return sb.toString();
    }

    private static void printDeclaration(StringBuilder sb, String prefix, AstDeclaration decl, boolean last) {
        String branch = last ? "└── " : "├── ";
        String next = prefix + (last ? "    " : "│   ");

        if (decl instanceof InterfaceDeclaration id) {
            sb.append(prefix).append(branch).append("InterfaceDecl ").append(quote(id.getName().getText())).append("\n");
            printFields(sb, next, id.getFields());
        } else if (decl instanceof TypeAliasDeclaration td) {
            sb.append(prefix).append(branch).append("TypeAliasDecl ").append(quote(td.getName().getText())).append("\n");
            printFields(sb, next, td.getFields());
        }
    }

    private static void printFields(StringBuilder sb, String prefix, java.util.List<FieldDeclaration> fields) {
        for (int i = 0; i < fields.size(); i++) {
            boolean last = i == fields.size() - 1;
            FieldDeclaration f = fields.get(i);
            String branch = last ? "└── " : "├── ";
            String next = prefix + (last ? "    " : "│   ");
            sb.append(prefix).append(branch).append("Field ").append(quote(f.getName().getText())).append("\n");
            sb.append(next).append(last ? "    " : "│   ").append("└── type: ").append(formatType(f.getType())).append("\n");
        }
    }

    private static String formatType(TypeReference ref) {
        return switch (ref) {
            case NamedTypeReference nt -> nt.getNameLexeme().getText();
            case ArrayTypeReference arr -> formatType(arr.getElementType()) + "[]";
            default -> "?";
        };
    }

    private static String quote(String s) {
        return "\"" + s + "\"";
    }
}
