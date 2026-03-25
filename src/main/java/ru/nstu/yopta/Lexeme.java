package ru.nstu.yopta;

/**
 * Результат лексического анализа: одна лексема с условным кодом, типом, текстом и позицией.
 */
public final class Lexeme {

    private final int code;
    private final String typeName;
    private final String text;
    private final int line;
    private final int startColumn;
    private final int endColumn;
    private final boolean error;

    public Lexeme(int code, String typeName, String text, int line, int startColumn, int endColumn, boolean error) {
        this.code = code;
        this.typeName = typeName;
        this.text = text;
        this.line = line;
        this.startColumn = startColumn;
        this.endColumn = endColumn;
        this.error = error;
    }

    public int getCode() { return code; }
    public String getTypeName() { return typeName; }
    public String getText() { return text; }
    public int getLine() { return line; }
    public int getStartColumn() { return startColumn; }
    public int getEndColumn() { return endColumn; }
    public boolean isError() { return error; }

    /** Строка местоположения вида «строка N, M–K» (M и K — начальная и конечная позиция включительно). */
    public String getLocationString() {
        if (startColumn == endColumn) {
            return String.format("строка %d, %d", line, startColumn);
        }
        return String.format("строка %d, %d-%d", line, startColumn, endColumn);
    }
}
