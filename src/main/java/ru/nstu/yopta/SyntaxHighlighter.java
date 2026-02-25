package ru.nstu.yopta;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Базовая подсветка синтаксиса для Go и Python: комментарии, строки, числа, ключевые слова, вызовы функций.
 */
public final class SyntaxHighlighter {

    public enum Language {
        GO,
        PYTHON,
        PLAIN
    }

    private static final String[] GO_KEYWORDS = {
        "break", "case", "chan", "const", "continue", "default", "defer", "else",
        "fallthrough", "for", "func", "go", "goto", "if", "import", "interface",
        "map", "package", "range", "return", "select", "struct", "switch", "type", "var"
    };
    private static final String[] PYTHON_KEYWORDS = {
        "False", "None", "True", "and", "as", "async", "await", "break", "class",
        "continue", "def", "del", "elif", "else", "except", "finally", "for",
        "from", "global", "if", "import", "in", "is", "lambda", "nonlocal",
        "not", "or", "pass", "raise", "return", "try", "while", "with", "yield"
    };

    private static final Pattern GO_PATTERN = buildGoPattern();
    private static final Pattern PYTHON_PATTERN = buildPythonPattern();

    public static Language fromPath(java.nio.file.Path path) {
        if (path == null) return Language.PLAIN;
        String name = path.getFileName().toString();
        if (name.endsWith(".go")) return Language.GO;
        if (name.endsWith(".py")) return Language.PYTHON;
        return Language.PLAIN;
    }

    /** Подсветка одной строки (параграфа). */
    public static StyleSpans<Collection<String>> computeSpans(String text, Language lang) {
        if (text == null) text = "";
        if (lang == Language.PLAIN) {
            StyleSpansBuilder<Collection<String>> b = new StyleSpansBuilder<>();
            b.add(Collections.emptyList(), text.length());
            return b.create();
        }
        if (lang == Language.GO) return computeGo(text);
        if (lang == Language.PYTHON) return computePython(text);
        StyleSpansBuilder<Collection<String>> b = new StyleSpansBuilder<>();
        b.add(Collections.emptyList(), text.length());
        return b.create();
    }

    private static Pattern buildGoPattern() {
        String kw = "\\b(" + String.join("|", GO_KEYWORDS) + ")\\b";
        String str = "\"([^\"\\\\]|\\\\.)*\"|`[^`]*`";
        // Line-based: // to EOL, /* */ on same line, or /* to EOL
        String comment = "//[^\n]*|/\\*[^\n]*\\*/|/\\*[^\n]*";
        String num = "\\b(0x[\\da-fA-F]+|\\d+\\.?\\d*(?:[eE][+-]?\\d+)?)\\b";
        String fn = "\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(";
        String regex = "(?<str>" + str + ")|(?<comment>" + comment + ")|(?<num>" + num + ")|(?<kw>" + kw + ")|(?<fn>" + fn + ")";
        return Pattern.compile(regex);
    }

    private static Pattern buildPythonPattern() {
        String kw = "\\b(" + String.join("|", PYTHON_KEYWORDS) + ")\\b";
        String str = "\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'";
        String comment = "#[^\n]*";
        String num = "\\b\\d+\\.?\\d*(?:[eE][+-]?\\d+)?\\b|\\b\\d+j\\b";
        String fn = "\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(";
        String regex = "(?<str>" + str + ")|(?<comment>" + comment + ")|(?<num>" + num + ")|(?<kw>" + kw + ")|(?<fn>" + fn + ")";
        return Pattern.compile(regex);
    }

    private static StyleSpans<Collection<String>> computeGo(String text) {
        return computeWithPattern(text, GO_PATTERN);
    }

    private static StyleSpans<Collection<String>> computePython(String text) {
        return computeWithPattern(text, PYTHON_PATTERN);
    }

    private static StyleSpans<Collection<String>> computeWithPattern(String text, Pattern pattern) {
        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
        int lastEnd = 0;
        Matcher m = pattern.matcher(text);
        while (m.find()) {
            String style = null;
            if (m.group("str") != null) style = "string";
            else if (m.group("comment") != null) style = "comment";
            else if (m.group("num") != null) style = "number";
            else if (m.group("kw") != null) style = "keyword";
            else if (m.group("fn") != null) style = "function";
            if (style != null) {
                builder.add(Collections.emptyList(), m.start() - lastEnd);
                builder.add(Collections.singleton(style), m.end() - m.start());
                lastEnd = m.end();
            }
        }
        builder.add(Collections.emptyList(), text.length() - lastEnd);
        return builder.create();
    }
}
