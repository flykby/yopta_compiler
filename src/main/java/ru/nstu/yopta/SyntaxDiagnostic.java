package ru.nstu.yopta;

/**
 * Одна синтаксическая ошибка для таблицы вывода и навигации в редакторе.
 */
public final class SyntaxDiagnostic {

    private final String fragment;
    private final int line;
    private final int startColumn;
    private final int endColumn;
    private final String description;

    public SyntaxDiagnostic(String fragment, int line, int startColumn, int endColumn, String description) {
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

    /** Местоположение в формате «строка N, M» или «строка N, M–K». */
    public String getLocationString() {
        if (startColumn == endColumn) {
            return String.format("строка %d, позиция %d", line, startColumn);
        }
        return String.format("строка %d, позиция %d–%d", line, startColumn, endColumn);
    }
}
