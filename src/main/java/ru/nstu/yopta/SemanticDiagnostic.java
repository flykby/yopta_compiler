package ru.nstu.yopta;

/**
 * Семантическая ошибка: фрагмент, позиция в исходном тексте, описание.
 */
public final class SemanticDiagnostic {

    private final String fragment;
    private final int line;
    private final int startColumn;
    private final int endColumn;
    private final String description;

    public SemanticDiagnostic(String fragment, int line, int startColumn, int endColumn, String description) {
        this.fragment = fragment != null ? fragment : "";
        this.line = line;
        this.startColumn = startColumn;
        this.endColumn = endColumn;
        this.description = description != null ? description : "";
    }

    public String getFragment() {
        return fragment;
    }

    public int getLine() {
        return line;
    }

    public int getStartColumn() {
        return startColumn;
    }

    public int getEndColumn() {
        return endColumn;
    }

    public String getDescription() {
        return description;
    }

    public String getLocationString() {
        if (startColumn == endColumn) {
            return String.format("строка %d, позиция %d", line, startColumn);
        }
        return String.format("строка %d, позиция %d–%d", line, startColumn, endColumn);
    }
}
