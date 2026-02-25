package ru.nstu.yopta;

import java.util.Locale;
import java.util.prefs.Preferences;

/**
 * Настройки приложения: размер шрифта редактора и вывода, язык интерфейса.
 */
public class AppSettings {

    private static final String PREFS_NODE = "ru.nstu.yopta";
    private static final String KEY_EDITOR_FONT_SIZE = "editorFontSize";
    private static final String KEY_OUTPUT_FONT_SIZE = "outputFontSize";
    private static final String KEY_LANGUAGE = "language";

    private static final int DEFAULT_EDITOR_SIZE = 14;
    private static final int DEFAULT_OUTPUT_SIZE = 13;
    private static final int MIN_FONT_SIZE = 8;
    private static final int MAX_FONT_SIZE = 32;

    private int editorFontSize = DEFAULT_EDITOR_SIZE;
    private int outputFontSize = DEFAULT_OUTPUT_SIZE;
    private String language = ""; // пусто = системная локаль

    private static AppSettings instance;

    public static synchronized AppSettings getInstance() {
        if (instance == null) {
            instance = new AppSettings();
            instance.load();
        }
        return instance;
    }

    public void load() {
        Preferences p = Preferences.userRoot().node(PREFS_NODE);
        editorFontSize = clamp(p.getInt(KEY_EDITOR_FONT_SIZE, DEFAULT_EDITOR_SIZE));
        outputFontSize = clamp(p.getInt(KEY_OUTPUT_FONT_SIZE, DEFAULT_OUTPUT_SIZE));
        language = p.get(KEY_LANGUAGE, "");
    }

    public void save() {
        Preferences p = Preferences.userRoot().node(PREFS_NODE);
        p.putInt(KEY_EDITOR_FONT_SIZE, editorFontSize);
        p.putInt(KEY_OUTPUT_FONT_SIZE, outputFontSize);
        p.put(KEY_LANGUAGE, language);
    }

    private static int clamp(int size) {
        return Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, size));
    }

    public int getEditorFontSize() {
        return editorFontSize;
    }

    public void setEditorFontSize(int editorFontSize) {
        this.editorFontSize = clamp(editorFontSize);
    }

    public int getOutputFontSize() {
        return outputFontSize;
    }

    public void setOutputFontSize(int outputFontSize) {
        this.outputFontSize = clamp(outputFontSize);
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language == null ? "" : language;
    }

    public Locale getLocale() {
        if (language == null || language.isBlank()) {
            return Locale.getDefault();
        }
        if ("en".equalsIgnoreCase(language)) return Locale.ENGLISH;
        if ("ru".equalsIgnoreCase(language)) return new Locale("ru");
        return Locale.getDefault();
    }
}
