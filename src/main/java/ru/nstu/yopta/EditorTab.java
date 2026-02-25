package ru.nstu.yopta;

import javafx.scene.control.Tab;
import org.fxmisc.richtext.CodeArea;

import java.nio.file.Path;

/**
 * Состояние одной вкладки редактора: файл (или null для безымянного), редактор и флаг изменений.
 */
public class EditorTab {
    private final Tab tab;
    private Path path;
    private final CodeArea codeArea;
    private boolean dirty;
    private Runnable highlightCleanup;

    public EditorTab(Tab tab, Path path, CodeArea codeArea, boolean dirty) {
        this.tab = tab;
        this.path = path;
        this.codeArea = codeArea;
        this.dirty = dirty;
    }

    public Tab getTab() {
        return tab;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public CodeArea getCodeArea() {
        return codeArea;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public String getDisplayName() {
        if (path == null) return Messages.getString("status.untitled");
        return path.getFileName().toString();
    }

    public void updateTabTitle() {
        tab.setText((dirty ? "• " : "") + getDisplayName());
    }

    public void setHighlightCleanup(Runnable highlightCleanup) {
        this.highlightCleanup = highlightCleanup;
    }

    public Runnable getHighlightCleanup() {
        return highlightCleanup;
    }
}
