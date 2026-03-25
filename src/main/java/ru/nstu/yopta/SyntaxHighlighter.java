package ru.nstu.yopta;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Подсветка синтаксиса для TypeScript (объявления {@code interface} и {@code type}) и обычный текст.
 */
public final class SyntaxHighlighter {

    public enum Language {
        TYPESCRIPT,
        PLAIN
    }

    private static final Pattern TYPESCRIPT_INTERFACE_PATTERN = buildTypeScriptInterfacePattern();

    public static Language fromPath(java.nio.file.Path path) {
        if (path == null) return Language.PLAIN;
        String name = path.getFileName().toString();
        if (name.endsWith(".ts")) return Language.TYPESCRIPT;
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
        if (lang == Language.TYPESCRIPT) return computeTypeScript(text);
        StyleSpansBuilder<Collection<String>> b = new StyleSpansBuilder<>();
        b.add(Collections.emptyList(), text.length());
        return b.create();
    }

    /** interface/type (фиолетовый), имя после ключевого слова (коричневый), Child[] (коричневый), поля — чёрный, типы string/number (зелёный). */
    private static Pattern buildTypeScriptInterfacePattern() {
        String typeKw = "string|number|boolean|any|unknown|object|void|null|undefined";
        return Pattern.compile(
            "(?<kw>interface|type)\\s+(?<typename>[a-zA-Z_][a-zA-Z0-9_]*)\\b|(?<typeref>[a-zA-Z_][a-zA-Z0-9_]*)\\s*\\[\\s*\\]|\\b(?<typekw>" + typeKw + ")\\b|\\b(?<id>[a-zA-Z_][a-zA-Z0-9_]*)\\b"
        );
    }

    private static StyleSpans<Collection<String>> computeTypeScript(String text) {
        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
        int lastEnd = 0;
        Matcher m = TYPESCRIPT_INTERFACE_PATTERN.matcher(text);
        while (m.find()) {
            String style = null;
            if (m.group("kw") != null) {
                builder.add(Collections.emptyList(), m.start() - lastEnd);
                builder.add(Collections.singleton("interface-keyword"), m.end(1) - m.start(1));
                builder.add(Collections.emptyList(), m.start(2) - m.end(1));
                builder.add(Collections.singleton("type-name"), m.end(2) - m.start(2));
                lastEnd = m.end();
                continue;
            }
            if (m.group("typeref") != null) style = "type-name";
            else if (m.group("typekw") != null) style = "type-keyword";
            else if (m.group("id") != null) style = "identifier";
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
