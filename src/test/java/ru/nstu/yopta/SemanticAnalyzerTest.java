package ru.nstu.yopta;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.nstu.yopta.ast.ArrayTypeReference;
import ru.nstu.yopta.ast.AstProgram;
import ru.nstu.yopta.ast.FieldDeclaration;
import ru.nstu.yopta.ast.InterfaceDeclaration;
import ru.nstu.yopta.ast.NamedTypeReference;
import ru.nstu.yopta.ast.TypeAliasDeclaration;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.nstu.yopta.TypeScriptInterfaceScanner.CODE_IDENTIFIER;
import static ru.nstu.yopta.TypeScriptInterfaceScanner.CODE_TYPE;

class SemanticAnalyzerTest {

    @BeforeAll
    static void initMessages() {
        Messages.setLocale(Locale.ROOT);
    }

    private static Lexeme ident(String text, int line, int col) {
        int end = col + text.length() - 1;
        return new Lexeme(CODE_IDENTIFIER, "идентификатор", text, line, col, end, false);
    }

    private static Lexeme typeKw(String text, int line, int col) {
        int end = col + text.length() - 1;
        return new Lexeme(CODE_TYPE, "тип данных", text, line, col, end, false);
    }

    @Test
    void duplicateTopLevelName() {
        Lexeme n = ident("Person", 1, 10);
        var a = new InterfaceDeclaration(n, List.of());
        var b = new InterfaceDeclaration(ident("Person", 2, 10), List.of());
        var prog = new AstProgram(List.of(a, b));
        List<SemanticDiagnostic> e = SemanticAnalyzer.analyze(prog);
        assertEquals(1, e.size());
        assertTrue(e.get(0).getDescription().contains("Person"));
    }

    @Test
    void duplicateFieldName() {
        Lexeme iface = ident("A", 1, 10);
        FieldDeclaration f1 = new FieldDeclaration(ident("x", 2, 2), new NamedTypeReference(typeKw("number", 2, 5)));
        FieldDeclaration f2 = new FieldDeclaration(ident("x", 3, 2), new NamedTypeReference(typeKw("string", 3, 5)));
        var decl = new InterfaceDeclaration(iface, List.of(f1, f2));
        List<SemanticDiagnostic> e = SemanticAnalyzer.analyze(new AstProgram(List.of(decl)));
        assertEquals(1, e.size());
        assertTrue(e.get(0).getDescription().contains("x"));
    }

    @Test
    void unknownUserType() {
        Lexeme iface = ident("Box", 1, 10);
        FieldDeclaration f = new FieldDeclaration(ident("ref", 2, 2), new NamedTypeReference(ident("Unknown", 2, 7)));
        var decl = new InterfaceDeclaration(iface, List.of(f));
        List<SemanticDiagnostic> e = SemanticAnalyzer.analyze(new AstProgram(List.of(decl)));
        assertEquals(1, e.size());
        assertTrue(e.get(0).getDescription().contains("Unknown"));
    }

    @Test
    void forwardReferenceOk() {
        Lexeme person = ident("Person", 1, 10);
        Lexeme box = ident("Box", 2, 10);
        FieldDeclaration fp = new FieldDeclaration(ident("name", 2, 2), new NamedTypeReference(typeKw("string", 2, 8)));
        var personDecl = new InterfaceDeclaration(person, List.of(fp));
        FieldDeclaration fb = new FieldDeclaration(ident("owner", 3, 2), new NamedTypeReference(ident("Person", 3, 9)));
        var boxDecl = new InterfaceDeclaration(box, List.of(fb));
        List<SemanticDiagnostic> e = SemanticAnalyzer.analyze(new AstProgram(List.of(personDecl, boxDecl)));
        assertTrue(e.isEmpty());
    }

    @Test
    void builtinAndArrayNoExtraErrors() {
        Lexeme i = ident("I", 1, 10);
        FieldDeclaration f = new FieldDeclaration(ident("items", 2, 2),
                new ArrayTypeReference(new NamedTypeReference(typeKw("number", 2, 10))));
        List<SemanticDiagnostic> e = SemanticAnalyzer.analyze(new AstProgram(List.of(new InterfaceDeclaration(i, List.of(f)))));
        assertTrue(e.isEmpty());
    }

    @Test
    void typeAliasCountsAsDeclaredType() {
        Lexeme point = ident("Point", 1, 10);
        var pointDecl = new TypeAliasDeclaration(point, List.of(
                new FieldDeclaration(ident("x", 2, 2), new NamedTypeReference(typeKw("number", 2, 5)))));
        Lexeme r = ident("Ref", 4, 10);
        FieldDeclaration f = new FieldDeclaration(ident("p", 5, 2), new NamedTypeReference(ident("Point", 5, 5)));
        var refDecl = new InterfaceDeclaration(r, List.of(f));
        List<SemanticDiagnostic> e = SemanticAnalyzer.analyze(new AstProgram(List.of(pointDecl, refDecl)));
        assertTrue(e.isEmpty());
    }

    @Test
    void endToEndParseAndSemantics() {
        String src = """
                interface Person {
                  name: string;
                  age: number;
                };
                interface Box {
                  owner: Person;
                };
                """;
        ParseResult pr = TypeScriptInterfaceParser.parse(src);
        assertTrue(pr.isSuccess());
        List<SemanticDiagnostic> e = SemanticAnalyzer.analyze(pr.getAst());
        assertTrue(e.isEmpty());
    }
}
