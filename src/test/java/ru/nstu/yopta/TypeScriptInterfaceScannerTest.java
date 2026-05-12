package ru.nstu.yopta;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypeScriptInterfaceScannerTest {

    @Test
    void malformedDeclarationWithoutKeywordReportsOnlyFourStructureErrors() {
        String source = """
                interfae Pep%%e
                \ta: number;
                \tb: string;
                }
                """;

        List<String> structureErrors = structureErrors(source);
        assertEquals(4, structureErrors.size(), "Ожидается ровно 4 структурные ошибки");
        assertContains(structureErrors, "отсутствует ключевое слово");
        assertContains(structureErrors, "неверный формат");
        assertContains(structureErrors, "отсутствует '{'");
        assertContains(structureErrors, "ожидается ';' после '}'");
    }

    @Test
    void malformedInterfaceNameWithSymbolsReportsFormatAndMissingSemicolonOnly() {
        String source = """
                interf%%ace Pepe {
                \ta: number;
                \tb: string;
                }
                """;

        List<String> structureErrors = structureErrors(source);
        assertEquals(2, structureErrors.size(), "Ожидается ровно 2 структурные ошибки");
        assertContains(structureErrors, "неверный формат");
        assertContains(structureErrors, "ожидается ';' после '}'");
    }

    @Test
    void unknownTypeProducesSingleUnknownTypeError() {
        String source = """
                interface Man {
                \tname: strin;
                };
                """;

        List<String> structureErrors = structureErrors(source);
        assertEquals(1, structureErrors.size(), "Ожидается ровно 1 структурная ошибка");
        assertContains(structureErrors, "неизвестный тип 'strin'");
    }

    @Test
    void malformedBuiltinTypeNameWithSymbolsProducesSingleUnknownTypeError() {
        String source = """
                interface pepe {
                \ta: num$$ber;
                \tb: string;
                };
                """;

        List<String> structureErrors = structureErrors(source);
        assertEquals(1, structureErrors.size(), "Ожидается ровно 1 структурная ошибка");
        assertContains(structureErrors, "неизвестный тип");
    }

    @Test
    void malformedTypeWithExtraSymbolsAtBeginningAndEndProducesSingleUnknownTypeError() {
        String source = """
                interface Pepe {
                \ta: $num^^ber;
                \tb: string;
                };
                """;

        List<String> structureErrors = structureErrors(source);
        assertEquals(1, structureErrors.size(), "Ожидается ровно 1 структурная ошибка");
        assertContains(structureErrors, "неизвестный тип");
    }

    @Test
    void malformedTypeWithLeadingSymbolProducesSingleErrorInLexemeOutput() {
        String source = """
                interface Pepe {
                \ta: $number;
                \tb: string;
                };
                """;

        List<Lexeme> errors = TypeScriptInterfaceScanner.scan(source).stream()
                .filter(Lexeme::isError)
                .collect(Collectors.toList());

        assertEquals(1, errors.size(), "Ожидается ровно 1 ошибка в выводе лексем");
        assertTrue(errors.get(0).getTypeName().contains("неизвестный тип"),
                () -> "Ожидалась ошибка про неизвестный тип, фактически: " + errors.get(0).getTypeName());
    }

    @Test
    void missingColonBeforeTypeProducesSingleErrorWithoutCascade() {
        String source = """
                interface Pepe {
                \ta: number;
                \tb string;
                };
                """;

        List<String> structureErrors = structureErrors(source);
        assertEquals(1, structureErrors.size(), "Ожидается ровно 1 структурная ошибка");
        assertContains(structureErrors, "ожидается ':'");
    }

    @Test
    void interfaceKeywordWithoutNameReportsExpectedIdentifier() {
        String source = "interface ";
        List<String> structureErrors = structureErrors(source);
        assertEquals(1, structureErrors.size(), "Ожидается ровно 1 структурная ошибка");
        assertContains(structureErrors, "ожидается имя идентификатора");
    }

    @Test
    void interfaceNameThenCloseBraceReportsOnlyMissingOpenBrace() {
        String source = "interface Pepe };";
        List<String> structureErrors = structureErrors(source);
        assertEquals(1, structureErrors.size(), "Ожидается ровно 1 структурная ошибка");
        assertContains(structureErrors, "ожидается '{'");
    }

    @Test
    void interfaceWithoutNameBeforeCloseBraceReportsNameAndOpenBraceErrors() {
        String source = "interface   };";
        List<String> structureErrors = structureErrors(source);
        assertEquals(2, structureErrors.size(), "Ожидается ровно 2 структурные ошибки");
        assertContains(structureErrors, "ожидается имя идентификатора");
        assertContains(structureErrors, "ожидается '{'");
    }

    @Test
    void validInterfaceHasNoErrors() {
        String source = """
                interface Person {
                \tname: string;
                \tage: number;
                };
                """;
        assertEquals(0, structureErrors(source).size(), "Для корректного interface ошибок быть не должно");
    }

    @Test
    void missingSemicolonAfterFieldTypeReportsSingleError() {
        String source = """
                interface Person {
                \tname: string
                \tage: number;
                };
                """;
        List<String> structureErrors = structureErrors(source);
        assertEquals(1, structureErrors.size(), "Ожидается 1 ошибка при пропущенной ';' после типа");
        assertContains(structureErrors, "ожидается ';' или '[' после типа");
    }

    @Test
    void missingSemicolonAfterClosingBraceReportsSingleError() {
        String source = """
                interface Person {
                \tname: string;
                }
                """;
        List<String> structureErrors = structureErrors(source);
        assertEquals(1, structureErrors.size(), "Ожидается 1 ошибка при пропущенной ';' после '}'");
        assertContains(structureErrors, "ожидается ';' после '}'");
    }

    @Test
    void unknownLowercaseTypeReportsSingleUnknownTypeError() {
        String source = """
                interface Person {
                \tchild: customtype;
                };
                """;
        List<String> structureErrors = structureErrors(source);
        assertEquals(1, structureErrors.size(), "Ожидается 1 ошибка для неизвестного типа");
        assertContains(structureErrors, "неизвестный тип");
    }

    @Test
    void uppercaseUserTypeIsAccepted() {
        String source = """
                interface Person {
                \tchild: Child;
                };
                """;
        assertEquals(0, structureErrors(source).size(), "Тип с заглавной буквы должен считаться пользовательским и валидным");
    }

    @Test
    void interfaceWithUserDefinedArrayTypeIsAccepted() {
        String source = """
                interface Box {
                \titems: Item[];
                };
                """;
        assertEquals(0, structureErrors(source).size(),
                "Поле с типом-массивом пользовательского типа должно быть корректным");
    }

    @Test
    void interfaceWithBuiltinArrayTypeIsAccepted() {
        String source = """
                interface Tags {
                \titems: string[];
                };
                """;
        assertEquals(0, structureErrors(source).size(),
                "Поле с типом-массивом встроенного типа должно быть корректным");
    }

    @Test
    void emptyInterfaceBodyIsAccepted() {
        String source = """
                interface Empty {
                };
                """;
        assertEquals(0, structureErrors(source).size(),
                "Пустое тело интерфейса должно быть корректным");
    }

    @Test
    void twoCorrectInterfacesInSameFileAreAccepted() {
        String source = """
                interface A {
                \tx: number;
                };
                interface B {
                \ty: string;
                };
                """;
        assertEquals(0, structureErrors(source).size(),
                "Несколько корректных interface подряд должны разбираться без ошибок");
    }

    @Test
    void interfaceWithoutNameBeforeOpenBraceReportsSingleError() {
        String source = """
                interface {
                \tname: string;
                };
                """;
        List<String> structureErrors = structureErrors(source);
        assertEquals(1, structureErrors.size(),
                "Должна быть одна ошибка, если имя интерфейса пропущено перед '{'");
        assertContains(structureErrors, "ожидается имя интерфейса перед '{'");
    }

    @Test
    void fieldWithoutTypeAfterColonReportsSingleError() {
        String source = """
                interface Person {
                \ta: ;
                };
                """;
        List<String> structureErrors = structureErrors(source);
        assertEquals(1, structureErrors.size(),
                "Должна быть одна ошибка, если после ':' нет типа поля");
        assertContains(structureErrors, "ожидается тип после ':'");
    }

    @Test
    void unclosedInterfaceBodyReportsMissingCloseBrace() {
        String source = "interface Person {\n\tname: string;\n";
        List<String> structureErrors = structureErrors(source);
        assertEquals(1, structureErrors.size(),
                "Должна быть одна ошибка о пропущенной '}' для незакрытого тела");
        assertContains(structureErrors, "ожидается '}'");
    }

    @Test
    void interfaceBodyStartingWithSemicolonReportsOnlyMissingCloseBrace() {
        String source = "interface  Pepe {;";
        List<String> structureErrors = structureErrors(source);
        assertEquals(1, structureErrors.size(),
                "Должна быть одна ошибка про пропущенную '}', без шума на лишний ';'");
        assertContains(structureErrors, "ожидается '}'");
    }

    @Test
    void missingSemicolonBetweenTwoInterfacesReportsSingleError() {
        String source = """
                interface A {
                }
                interface B {
                };
                """;
        List<String> structureErrors = structureErrors(source);
        assertEquals(1, structureErrors.size(),
                "Должна быть одна ошибка, если между объявлениями пропущена ';'");
        assertContains(structureErrors, "ожидается ';' после объявления");
    }

    @Test
    void interaceWithMalformedNumTypeAndMissingSemiAfterStringReportsFiveStructureErrors() {
        String source = """
                interace  Pepe  {
                  a num%%ber;
                  b: string
                }
                """;
        List<String> structureErrors = structureErrors(source);
        assertEquals(5, structureErrors.size(), "Ожидается ровно 5 структурных ошибок без дубля «неизвестный тип»");
        assertContains(structureErrors, "отсутствует ключевое слово");
        assertContains(structureErrors, "ожидается ':'");
        assertContains(structureErrors, "неизвестный тип");
        assertContains(structureErrors, "ожидается ';' после типа");
        assertContains(structureErrors, "ожидается ';' после '}'");
    }

    @Test
    void interaceWithMalformedNumAndStrDotTailReportsSevenStructureErrors() {
        String source = """
                 interace  Pepe  {
                  a num%%ber;
                  b str...ing
                }
                """;
        List<String> structureErrors = structureErrors(source);
        assertEquals(7, structureErrors.size(), "Ожидается ровно 7 структурных ошибок без дублей неизвестного типа");
        assertContains(structureErrors, "отсутствует ключевое слово");
        assertContains(structureErrors, "ожидается ':'");
        assertContains(structureErrors, "неизвестный тип");
        assertContains(structureErrors, "ожидается ';' после типа");
        assertContains(structureErrors, "ожидается ';' после '}'");
    }

    private static List<String> structureErrors(String source) {
        return TypeScriptInterfaceScanner.scan(source).stream()
                .filter(Lexeme::isError)
                .map(Lexeme::getTypeName)
                .filter(t -> t != null && t.startsWith("ошибка:"))
                .collect(Collectors.toList());
    }

    private static void assertContains(List<String> values, String expectedSubstring) {
        assertTrue(values.stream().anyMatch(v -> v.contains(expectedSubstring)),
                () -> "Не найдено сообщение, содержащее: " + expectedSubstring + "; фактически: " + values);
    }
}
