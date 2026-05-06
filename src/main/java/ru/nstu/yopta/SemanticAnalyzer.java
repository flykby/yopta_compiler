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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static ru.nstu.yopta.TypeScriptInterfaceScanner.CODE_TYPE;

/**
 * Семантический анализ: уникальность имён, разрешение ссылок на типы.
 */
public final class SemanticAnalyzer {

    private SemanticAnalyzer() {
    }

    public static List<SemanticDiagnostic> analyze(AstProgram program) {
        List<SemanticDiagnostic> errors = new ArrayList<>();
        if (program == null || program.getDeclarations().isEmpty()) {
            return errors;
        }

        Set<String> seenDeclNames = new HashSet<>();
        for (AstDeclaration decl : program.getDeclarations()) {
            var nameLex = declarationNameLexeme(decl);
            String n = nameLex.getText();
            if (!seenDeclNames.add(n)) {
                errors.add(new SemanticDiagnostic(
                        nameLex.getText(),
                        nameLex.getLine(),
                        nameLex.getStartColumn(),
                        nameLex.getEndColumn(),
                        Messages.getString("semantic.err.duplicateDecl", n)));
            }
        }

        Set<String> declaredTypes = new HashSet<>(seenDeclNames);
        for (AstDeclaration decl : program.getDeclarations()) {
            List<FieldDeclaration> fields = declarationFields(decl);
            Set<String> fieldNames = new HashSet<>();
            for (FieldDeclaration field : fields) {
                Lexeme fn = field.getName();
                String fname = fn.getText();
                if (!fieldNames.add(fname)) {
                    errors.add(new SemanticDiagnostic(
                            fn.getText(),
                            fn.getLine(),
                            fn.getStartColumn(),
                            fn.getEndColumn(),
                            Messages.getString("semantic.err.duplicateField", fname)));
                }
                checkType(field.getType(), declaredTypes, errors);
            }
        }

        return errors;
    }

    private static Lexeme declarationNameLexeme(AstDeclaration decl) {
        if (decl instanceof InterfaceDeclaration id) {
            return id.getName();
        }
        if (decl instanceof TypeAliasDeclaration td) {
            return td.getName();
        }
        throw new IllegalArgumentException(decl.toString());
    }

    private static List<FieldDeclaration> declarationFields(AstDeclaration decl) {
        if (decl instanceof InterfaceDeclaration id) {
            return id.getFields();
        }
        if (decl instanceof TypeAliasDeclaration td) {
            return td.getFields();
        }
        throw new IllegalArgumentException(decl.toString());
    }

    private static void checkType(TypeReference ref, Set<String> declaredTypes, List<SemanticDiagnostic> errors) {
        switch (ref) {
            case ArrayTypeReference arr -> checkType(arr.getElementType(), declaredTypes, errors);
            case NamedTypeReference nt -> checkNamedType(nt, declaredTypes, errors);
            default -> {
            }
        }
    }

    private static void checkNamedType(NamedTypeReference nt, Set<String> declaredTypes, List<SemanticDiagnostic> errors) {
        Lexeme lx = nt.getNameLexeme();
        String name = lx.getText();
        if (lx.getCode() == CODE_TYPE) {
            return;
        }
        if (TypeScriptInterfaceScanner.isBuiltinTypeName(name)) {
            return;
        }
        if (declaredTypes.contains(name)) {
            return;
        }
        errors.add(new SemanticDiagnostic(
                lx.getText(),
                lx.getLine(),
                lx.getStartColumn(),
                lx.getEndColumn(),
                Messages.getString("semantic.err.unknownType", name)));
    }
}
