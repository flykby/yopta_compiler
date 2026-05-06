package ru.nstu.yopta;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.TransferMode;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import org.reactfx.Subscription;

import javax.imageio.ImageIO;
import java.awt.Taskbar;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import java.util.regex.Pattern;

import ru.nstu.yopta.ast.AstProgram;

public class Main extends Application {

    /** Лексический/синтаксический/семантический разбор в памяти — ограничение на размер текста редактора. */
    private static final int MAX_ANALYSIS_SOURCE_CHARS = 2_000_000;

    public int autosave_timeout_ms = 3_000;

    private Stage stage;
    private TabPane tabPane;
    private ProjectPanel projectPanel;
    private OutputPanel outputPanel;
    private SplitPane centerAndProjectSplit;
    private BorderPane root;
    private StatusBar statusBar;
    private boolean autoSaveEnabled;
    private RunConfig runConfig;
    /** Текущий процесс запуска (для остановки). */
    private volatile Process currentRunProcess;
    private List<String> baseStylesheets;
    /** Последний добавленный data URL стилей шрифта редактора (чтобы удалять при обновлении). */
    private String lastEditorFontStylesheetUrl;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        Messages.setLocale(AppSettings.getInstance().getLocale());

        projectPanel = new ProjectPanel();
        projectPanel.setVisible(false);
        projectPanel.setOnFileOpen(this::openFileInTab);

        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        tabPane.getSelectionModel().selectedItemProperty().addListener((o, old, t) -> updateWindowTitle());
        addNewTab(null, "");

        MenuBar menuBar = buildMenuBar();

        root = new BorderPane();
        root.setTop(menuBar);

        centerAndProjectSplit = new SplitPane();
        centerAndProjectSplit.getItems().addAll(projectPanel, tabPane);
        centerAndProjectSplit.setDividerPositions(0);

        outputPanel = new OutputPanel();
        outputPanel.getLexerResultsPanel().setOnErrorClick(this::navigateToEditorPosition);
        outputPanel.getParserResultsPanel().setOnRowClick(this::navigateToDiagnostic);
        outputPanel.getRegexSearchPanel().setOnRunRequested(this::runRegexSearch);
        outputPanel.getRegexSearchPanel().setOnRowClick(this::navigateToRegexMatch);
        outputPanel.getSemanticResultsPanel().setOnRowClick(this::navigateToSemanticDiagnostic);
        SplitPane mainSplit = new SplitPane();
        mainSplit.setOrientation(javafx.geometry.Orientation.VERTICAL);
        mainSplit.getItems().addAll(centerAndProjectSplit, outputPanel);
        mainSplit.setDividerPositions(0.78);
        root.setCenter(mainSplit);

        statusBar = new StatusBar();
        root.setBottom(statusBar);

        Scene scene = new Scene(root, 800, 600);
        String appCss = getClass().getResource("/styles/app.css").toExternalForm();
        String editorCss = getClass().getResource("/styles/editor.css").toExternalForm();
        baseStylesheets = List.of(appCss, editorCss);
        scene.getStylesheets().addAll(baseStylesheets);
        lastEditorFontStylesheetUrl = null;
        applyEditorFontStylesheet(scene, AppSettings.getInstance().getEditorFontSize());
        setupDragAndDrop(scene);
        tabPane.getSelectionModel().selectedItemProperty().addListener((o, old, t) -> {
            updateStatusBar();
            attachCaretListener();
        });
        attachCaretListener();
        updateStatusBar();

        stage.setTitle(Messages.getString("app.title"));
        var iconUrl = getClass().getResource("/logo.png");
        if (iconUrl != null) {
            stage.getIcons().add(new Image(iconUrl.toExternalForm()));
        }
        try (var iconStream = getClass().getResourceAsStream("/logo.png")) {
            if (iconStream != null && Taskbar.isTaskbarSupported()) {
                var taskbar = Taskbar.getTaskbar();
                if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                    var awtImage = ImageIO.read(iconStream);
                    if (awtImage != null) {
                        try {
                            taskbar.setIconImage(awtImage);
                        } catch (UnsupportedOperationException ignored) {
                            // на некоторых платформах (например Windows в jpackage) может выброситься
                        }
                    }
                }
            }
        } catch (IOException ignored) {
            // иконка в панели ОС опциональна
        }
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> {
            if (confirmExit()) {
                outputPanel.destroy();
            } else {
                e.consume();
            }
        });
        stage.show();
    }

    private void setupDragAndDrop(Scene scene) {
        scene.setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) e.acceptTransferModes(TransferMode.COPY);
        });
        scene.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            if (!db.hasFiles()) return;
            for (java.io.File file : db.getFiles()) {
                if (file.isFile()) openFileInTab(file.toPath());
            }
            e.setDropCompleted(true);
        });
    }

    private void attachCaretListener() {
        CodeArea area = getCurrentCodeArea();
        if (area != null) {
            area.caretPositionProperty().addListener((o, oldVal, newVal) -> updateStatusBar());
        }
        updateStatusBar();
    }

    private void updateStatusBar() {
        EditorTab et = getCurrentEditorTab();
        statusBar.setFileName(et != null ? et.getDisplayName() : null);
        CodeArea area = getCurrentCodeArea();
        if (area != null && area.getLength() > 0) {
            try {
                int offset = Math.min(area.getCaretPosition(), area.getLength());
                var pos = area.offsetToPosition(offset, org.fxmisc.richtext.model.TwoDimensional.Bias.Forward);
                int line = pos.getMajor() + 1;
                int col = pos.getMinor() + 1;
                statusBar.setPosition(line, col);
            } catch (Exception e) {
                statusBar.clearPosition();
            }
        } else {
            statusBar.clearPosition();
        }
        statusBar.setMessage(Messages.getString("status.ready"));
    }

    /** Строит data URL таблицы стилей с размером шрифта редактора. */
    private String buildEditorFontStylesheetUrl(int editorSize) {
        String size = editorSize + "px";
        String css = ".code-area, .styled-text-area { -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: " + size + "; }"
            + " .code-area .paragraph-text, .styled-text-area .paragraph-text { -fx-font-size: " + size + "; }";
        return "data:text/css;charset=utf-8," + URLEncoder.encode(css, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /** Подключает динамическую таблицу стилей сцены и добавляет/обновляет стиль шрифта в каждом CodeArea. */
    private void applyEditorFontStylesheet(Scene scene, int editorSize) {
        if (scene != null && baseStylesheets != null) {
            String dataUrl = buildEditorFontStylesheetUrl(editorSize);
            scene.getStylesheets().setAll(baseStylesheets);
            scene.getStylesheets().add(dataUrl);
        }
        String newUrl = buildEditorFontStylesheetUrl(editorSize);
        for (Tab t : tabPane.getTabs()) {
            Object ud = t.getUserData();
            if (ud instanceof EditorTab) {
                CodeArea area = ((EditorTab) ud).getCodeArea();
                if (lastEditorFontStylesheetUrl != null) while (area.getStylesheets().remove(lastEditorFontStylesheetUrl)) { }
                while (area.getStylesheets().remove(newUrl)) { }
                area.getStylesheets().add(newUrl);
            }
        }
        lastEditorFontStylesheetUrl = newUrl;
    }

    public void refreshUI() {
        Messages.setLocale(AppSettings.getInstance().getLocale());
        root.setTop(buildMenuBar());
        updateStatusBar();
        statusBar.setMessage(Messages.getString("status.ready"));
        int editorSize = AppSettings.getInstance().getEditorFontSize();
        applyEditorFontStylesheet(stage.getScene(), editorSize);
        outputPanel.setOutputFontSize(AppSettings.getInstance().getOutputFontSize());
        outputPanel.updateTabTitles();
        stage.setTitle(Messages.getString("app.title"));
    }

    private MenuBar buildMenuBar() {
        MenuBar menuBar = new MenuBar();

        Menu fileMenu = new Menu(Messages.getString("menu.file"));
        MenuItem newItem = new MenuItem(Messages.getString("menu.new"));
        newItem.setAccelerator(KeyCombination.keyCombination("Shortcut+N"));
        newItem.setOnAction(e -> newFile());

        MenuItem openItem = new MenuItem(Messages.getString("menu.open"));
        openItem.setAccelerator(KeyCombination.keyCombination("Shortcut+O"));
        openItem.setOnAction(e -> openFile());

        MenuItem openProjectItem = new MenuItem(Messages.getString("menu.openProject"));
        openProjectItem.setAccelerator(KeyCombination.keyCombination("Shortcut+Shift+O"));
        openProjectItem.setOnAction(e -> openProject());

        MenuItem saveItem = new MenuItem(Messages.getString("menu.save"));
        saveItem.setAccelerator(KeyCombination.keyCombination("Shortcut+S"));
        saveItem.setOnAction(e -> saveFile());

        MenuItem saveAsItem = new MenuItem(Messages.getString("menu.saveAs"));
        saveAsItem.setAccelerator(KeyCombination.keyCombination("Shortcut+Shift+S"));
        saveAsItem.setOnAction(e -> saveFileAs());

        CheckMenuItem autoSaveItem = new CheckMenuItem(Messages.getString("menu.autoSave"));
        autoSaveItem.setAccelerator(KeyCombination.keyCombination("Shortcut+Shift+A"));
        autoSaveItem.setSelected(false);
        autoSaveItem.setOnAction(e -> {
            autoSaveEnabled = autoSaveItem.isSelected();
            if (autoSaveEnabled) startAutoSaveTimer();
        });

        fileMenu.getItems().addAll(newItem, openItem, openProjectItem, new SeparatorMenuItem(),
                saveItem, saveAsItem, new SeparatorMenuItem(), autoSaveItem);

        Menu editMenu = new Menu(Messages.getString("menu.edit"));
        MenuItem undoItem = new MenuItem(Messages.getString("menu.undo"));
        undoItem.setAccelerator(KeyCombination.keyCombination("Shortcut+Z"));
        undoItem.setOnAction(e -> undo());

        MenuItem redoItem = new MenuItem(Messages.getString("menu.redo"));
        redoItem.setAccelerator(KeyCombination.keyCombination("Shortcut+Shift+Z"));
        redoItem.setOnAction(e -> redo());

        MenuItem cutItem = new MenuItem(Messages.getString("menu.cut"));
        cutItem.setAccelerator(KeyCombination.keyCombination("Shortcut+X"));
        cutItem.setOnAction(e -> cut());

        MenuItem copyItem = new MenuItem(Messages.getString("menu.copy"));
        copyItem.setAccelerator(KeyCombination.keyCombination("Shortcut+C"));
        copyItem.setOnAction(e -> copy());

        MenuItem pasteItem = new MenuItem(Messages.getString("menu.paste"));
        pasteItem.setAccelerator(KeyCombination.keyCombination("Shortcut+V"));
        pasteItem.setOnAction(e -> paste());

        MenuItem deleteItem = new MenuItem(Messages.getString("menu.delete"));
        deleteItem.setAccelerator(KeyCombination.keyCombination("Delete"));
        deleteItem.setOnAction(e -> delete());

        MenuItem selectAllItem = new MenuItem(Messages.getString("menu.selectAll"));
        selectAllItem.setAccelerator(KeyCombination.keyCombination("Shortcut+A"));
        selectAllItem.setOnAction(e -> selectAll());

        editMenu.getItems().addAll(undoItem, redoItem, new SeparatorMenuItem(),
                cutItem, copyItem, pasteItem, deleteItem, new SeparatorMenuItem(), selectAllItem);

        Menu runMenu = new Menu(Messages.getString("menu.run"));
        MenuItem runDebugItem = new MenuItem(Messages.getString("menu.runDebug"));
        runDebugItem.setAccelerator(KeyCombination.keyCombination("Shortcut+F5"));
        runDebugItem.setOnAction(e -> runWithDebug());

        MenuItem runItem = new MenuItem(Messages.getString("menu.runNoDebug"));
        runItem.setAccelerator(KeyCombination.keyCombination("Shortcut+F6"));
        runItem.setOnAction(e -> runWithoutDebug());

        MenuItem stopItem = new MenuItem(Messages.getString("menu.stop"));
        stopItem.setAccelerator(KeyCombination.keyCombination("Shortcut+Shift+F5"));
        stopItem.setOnAction(e -> stopRun());

        MenuItem configRunItem = new MenuItem(Messages.getString("menu.configureRun"));
        configRunItem.setAccelerator(KeyCombination.keyCombination("Shortcut+Alt+R"));
        configRunItem.setOnAction(e -> configureRun());

        MenuItem lexerItem = new MenuItem(Messages.getString("menu.lexer"));
        lexerItem.setAccelerator(KeyCombination.keyCombination("Shortcut+Shift+L"));
        lexerItem.setOnAction(e -> runLexer());

        MenuItem parserItem = new MenuItem(Messages.getString("menu.parser"));
        parserItem.setAccelerator(KeyCombination.keyCombination("Shortcut+Shift+P"));
        parserItem.setOnAction(e -> runParser());

        MenuItem regexSearchItem = new MenuItem(Messages.getString("menu.regexSearch"));
        regexSearchItem.setAccelerator(KeyCombination.keyCombination("Shortcut+Shift+X"));
        regexSearchItem.setOnAction(e -> runRegexSearch());

        MenuItem semanticItem = new MenuItem(Messages.getString("menu.semantic"));
        semanticItem.setAccelerator(KeyCombination.keyCombination("Shortcut+Shift+M"));
        semanticItem.setOnAction(e -> runSemanticAnalysis());

        runMenu.getItems().addAll(runDebugItem, runItem, stopItem, new SeparatorMenuItem(),
                lexerItem, parserItem, regexSearchItem, semanticItem, configRunItem);

        Menu viewMenu = new Menu(Messages.getString("menu.view"));
        MenuItem settingsItem = new MenuItem(Messages.getString("menu.settings"));
        settingsItem.setOnAction(e -> {
            SettingsDialog dlg = new SettingsDialog(stage);
            if (dlg.showAndWait()) refreshUI();
        });
        viewMenu.getItems().add(settingsItem);

        Menu helpMenu = new Menu(Messages.getString("menu.help"));
        MenuItem helpItem = new MenuItem(Messages.getString("menu.helpCall"));
        helpItem.setAccelerator(KeyCombination.keyCombination("F1"));
        helpItem.setOnAction(e -> showHelp());

        MenuItem aboutItem = new MenuItem(Messages.getString("menu.about"));
        aboutItem.setOnAction(e -> showAbout());

        helpMenu.getItems().addAll(helpItem, aboutItem);

        menuBar.getMenus().addAll(fileMenu, editMenu, runMenu, viewMenu, helpMenu);
        return menuBar;
    }

    private EditorTab getCurrentEditorTab() {
        Tab tab = tabPane.getSelectionModel().getSelectedItem();
        if (tab == null) return null;
        Object ud = tab.getUserData();
        return ud instanceof EditorTab ? (EditorTab) ud : null;
    }

    private CodeArea getCurrentCodeArea() {
        EditorTab et = getCurrentEditorTab();
        return et != null ? et.getCodeArea() : null;
    }

    private void applyHighlighting(CodeArea codeArea, SyntaxHighlighter.Language lang) {
        int n = codeArea.getParagraphs().size();
        for (int i = 0; i < n; i++) {
            String line = codeArea.getText(i);
            codeArea.setStyleSpans(i, 0, SyntaxHighlighter.computeSpans(line, lang));
        }
    }

    private EditorTab addNewTab(Path path, String content) {
        CodeArea codeArea = new CodeArea();
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.replaceText(content);
        int fontSize = AppSettings.getInstance().getEditorFontSize();
        codeArea.getStylesheets().add(buildEditorFontStylesheetUrl(fontSize));

        Tab tab = new Tab();
        EditorTab editorTab = new EditorTab(tab, path, codeArea, false);
        tab.setUserData(editorTab);
        editorTab.updateTabTitle();

        codeArea.textProperty().addListener((o, oldVal, newVal) -> {
            if (path == null) {
                editorTab.setDirty(!newVal.isBlank());
            } else {
                try {
                    editorTab.setDirty(!Files.readString(path).equals(newVal));
                } catch (IOException e) {
                    editorTab.setDirty(true);
                }
            }
            editorTab.updateTabTitle();
        });

        SyntaxHighlighter.Language lang = SyntaxHighlighter.fromPath(path);
        applyHighlighting(codeArea, lang);
        Subscription highlightSub = codeArea.multiPlainChanges()
                .reduceSuccessions((a, b) -> b, Duration.ofMillis(300))
                .subscribe(ignore -> Platform.runLater(() -> applyHighlighting(codeArea, lang)));
        editorTab.setHighlightCleanup(highlightSub::unsubscribe);

        StackPane pane = new StackPane(codeArea);
        codeArea.prefWidthProperty().bind(pane.widthProperty());
        codeArea.prefHeightProperty().bind(pane.heightProperty());
        tab.setContent(pane);

        tab.setOnCloseRequest(e -> {
            if (editorTab.getHighlightCleanup() != null) {
                editorTab.getHighlightCleanup().run();
            }
            if (editorTab.isDirty()) {
                Optional<ButtonType> r = new Alert(Alert.AlertType.CONFIRMATION,
                        Messages.getString("confirmSave", editorTab.getDisplayName()),
                        ButtonType.YES, ButtonType.NO, ButtonType.CANCEL).showAndWait();
                if (r.orElse(ButtonType.CANCEL) == ButtonType.CANCEL) {
                    e.consume();
                    return;
                }
                if (r.get() == ButtonType.YES) saveFile();
            }
        });

        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
        return editorTab;
    }

    private EditorTab findTabByPath(Path path) {
        if (path == null) return null;
        for (Tab t : tabPane.getTabs()) {
            Object ud = t.getUserData();
            if (ud instanceof EditorTab) {
                EditorTab et = (EditorTab) ud;
                if (path.equals(et.getPath())) return et;
            }
        }
        return null;
    }

    private void newFile() {
        EditorTab current = getCurrentEditorTab();
        if (current != null && current.isDirty()) {
            Optional<ButtonType> result = new Alert(Alert.AlertType.CONFIRMATION,
                    Messages.getString("confirmSaveBeforeExit"),
                    ButtonType.YES, ButtonType.NO, ButtonType.CANCEL).showAndWait();
            if (result.orElse(ButtonType.CANCEL) == ButtonType.CANCEL) return;
            if (result.get() == ButtonType.YES) saveFile();
        }
        addNewTab(null, "");
        updateWindowTitle();
    }

    private void openFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle(Messages.getString("menu.open"));
        var file = fc.showOpenDialog(stage);
        if (file == null) return;
        openFileInTab(file.toPath());
    }

    private void openFileInTab(Path path) {
        if (path == null || !Files.isRegularFile(path)) return;
        EditorTab existing = findTabByPath(path);
        if (existing != null) {
            tabPane.getSelectionModel().select(existing.getTab());
            updateWindowTitle();
            return;
        }
        try {
            String text = Files.readString(path);
            EditorTab et = addNewTab(path, text);
            et.setDirty(false);
            et.updateTabTitle();
            updateWindowTitle();
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, "Ошибка открытия: " + ex.getMessage()).showAndWait();
        }
    }

    private void openProject() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Открыть проект (папку)");
        var dir = dc.showDialog(stage);
        if (dir == null) return;
        Path projectPath = dir.toPath();
        projectPanel.setProjectRoot(projectPath);
        projectPanel.setVisible(true);
        centerAndProjectSplit.setDividerPositions(0.2);
        runConfig = RunConfig.loadFromProject(projectPath);
        updateWindowTitle();
    }

    private void saveFile() {
        EditorTab et = getCurrentEditorTab();
        if (et == null) return;
        if (et.getPath() == null) {
            saveFileAs();
            return;
        }
        try {
            Files.writeString(et.getPath(), et.getCodeArea().getText());
            et.setDirty(false);
            et.updateTabTitle();
            updateWindowTitle();
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, "Ошибка сохранения: " + ex.getMessage()).showAndWait();
        }
    }

    private void saveFileAs() {
        EditorTab et = getCurrentEditorTab();
        if (et == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle(Messages.getString("menu.saveAs"));
        var file = fc.showSaveDialog(stage);
        if (file == null) return;
        try {
            Files.writeString(file.toPath(), et.getCodeArea().getText());
            et.setPath(file.toPath());
            et.setDirty(false);
            et.updateTabTitle();
            updateWindowTitle();
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, "Ошибка сохранения: " + ex.getMessage()).showAndWait();
        }
    }

    private boolean hasUnsavedChanges() {
        EditorTab et = getCurrentEditorTab();
        return et != null && et.isDirty();
    }

    /** Возвращает список вкладок с несохранёнными изменениями. */
    private List<EditorTab> getDirtyTabs() {
        List<EditorTab> dirty = new ArrayList<>();
        for (Tab t : tabPane.getTabs()) {
            Object ud = t.getUserData();
            if (ud instanceof EditorTab) {
                EditorTab et = (EditorTab) ud;
                if (et.isDirty()) dirty.add(et);
            }
        }
        return dirty;
    }

    /** Сохраняет вкладку по пути (если путь задан). Возвращает false при ошибке. */
    private boolean saveEditorTab(EditorTab et) {
        if (et.getPath() == null) return true;
        try {
            Files.writeString(et.getPath(), et.getCodeArea().getText());
            et.setDirty(false);
            et.updateTabTitle();
            return true;
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, "Ошибка сохранения \"" + et.getDisplayName() + "\": " + ex.getMessage()).showAndWait();
            return false;
        }
    }

    /**
     * При выходе: если есть несохранённые изменения — показывает список файлов и предлагает сохранить.
     * @return true — можно закрывать окно, false — отменить выход
     */
    private boolean confirmExit() {
        List<EditorTab> dirty = getDirtyTabs();
        if (dirty.isEmpty()) return true;

        StringBuilder list = new StringBuilder();
        for (EditorTab et : dirty) {
            list.append("  • ").append(et.getDisplayName()).append("\n");
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(Messages.getString("exit.title"));
        alert.setHeaderText(Messages.getString("exit.unsaved"));
        alert.setContentText(list.toString());

        ButtonType saveAll = new ButtonType(Messages.getString("exit.saveAll"));
        ButtonType dontSave = new ButtonType(Messages.getString("exit.dontSave"));
        ButtonType cancel = new ButtonType(Messages.getString("exit.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(saveAll, dontSave, cancel);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() == cancel) return false;
        if (result.get() == dontSave) return true;

        if (result.get() == saveAll) {
            for (EditorTab et : dirty) {
                if (et.getPath() != null) {
                    if (!saveEditorTab(et)) return false;
                } else {
                    tabPane.getSelectionModel().select(et.getTab());
                    saveFileAs();
                    if (et.isDirty()) return false; // пользователь отменил «Сохранить как»
                }
            }
            updateWindowTitle();
            return true;
        }
        return false;
    }

    private void updateWindowTitle() {
        EditorTab et = getCurrentEditorTab();
        String name = et != null ? et.getDisplayName() : Messages.getString("app.title");
        if (et != null && et.isDirty()) name = "• " + name;
        stage.setTitle(Messages.getString("app.title") + " — " + name);
    }

    private void startAutoSaveTimer() {
        Thread thread = new Thread(() -> {
            while (autoSaveEnabled) {
                try {
                    Thread.sleep(autosave_timeout_ms);
                    if (!autoSaveEnabled) break;
                    Platform.runLater(() -> {
                        for (Tab t : tabPane.getTabs()) {
                            Object ud = t.getUserData();
                            if (ud instanceof EditorTab) {
                                EditorTab et = (EditorTab) ud;
                                if (et.getPath() != null && et.isDirty()) {
                                    tabPane.getSelectionModel().select(t);
                                    saveFile();
                                }
                            }
                        }
                    });
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void undo() {
        CodeArea area = getCurrentCodeArea();
        if (area != null) area.getUndoManager().undo();
    }

    private void redo() {
        CodeArea area = getCurrentCodeArea();
        if (area != null) area.getUndoManager().redo();
    }

    private void cut() {
        CodeArea area = getCurrentCodeArea();
        if (area == null) return;
        String selection = area.getSelectedText();
        if (selection.isEmpty()) return;
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(selection);
        clipboard.setContent(content);
        area.replaceSelection("");
    }

    private void copy() {
        CodeArea area = getCurrentCodeArea();
        if (area == null) return;
        String selection = area.getSelectedText();
        if (selection.isEmpty()) return;
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(selection);
        clipboard.setContent(content);
    }

    private void paste() {
        CodeArea area = getCurrentCodeArea();
        if (area == null) return;
        Clipboard clipboard = Clipboard.getSystemClipboard();
        if (clipboard.hasString()) area.replaceSelection(clipboard.getString());
    }

    private void delete() {
        CodeArea area = getCurrentCodeArea();
        if (area != null) area.replaceSelection("");
    }

    private void selectAll() {
        CodeArea area = getCurrentCodeArea();
        if (area != null) area.selectAll();
    }

    private void runWithDebug() {
        if (runConfig == null || runConfig.getDebugCommand().isBlank()) {
            new Alert(Alert.AlertType.WARNING, "Сначала настройте запуск: Пуск → Настроить запуск, укажите команду отладки.").showAndWait();
            return;
        }
        runCommand(runConfig.getDebugCommand(), runConfig.getProjectRootPath());
    }

    private void runWithoutDebug() {
        if (runConfig == null || runConfig.getRunCommand().isBlank()) {
            new Alert(Alert.AlertType.WARNING, "Сначала настройте запуск: Пуск → Настроить запуск, укажите команду запуска.").showAndWait();
            return;
        }
        runCommand(runConfig.getRunCommand(), runConfig.getProjectRootPath());
    }

    private void runCommand(String command, Path workDir) {
        if (command == null || command.isBlank()) return;
        Platform.runLater(() -> statusBar.setRunState(StatusBar.RunState.BUILDING));
        try {
            ProcessBuilder pb = new ProcessBuilder();
            boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
            if (isWindows) {
                pb.command("cmd", "/c", command);
            } else {
                pb.command("/bin/sh", "-c", command);
            }
            if (workDir != null && Files.isDirectory(workDir)) {
                pb.directory(workDir.toFile());
            }
            Process p = pb.start();
            currentRunProcess = p;
            Platform.runLater(() -> statusBar.setRunState(StatusBar.RunState.RUNNING));
            outputPanel.selectOutputTab();
            Platform.runLater(() -> outputPanel.appendOutput("> " + command + "\n"));
            Thread stdoutReader = new Thread(() -> readStreamToOutput(p.getInputStream(), false), "run-stdout");
            Thread stderrReader = new Thread(() -> readStreamToOutput(p.getErrorStream(), true), "run-stderr");
            stdoutReader.setDaemon(true);
            stderrReader.setDaemon(true);
            stdoutReader.start();
            stderrReader.start();
            Thread waitThread = new Thread(() -> {
                try {
                    p.waitFor();
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } finally {
                    currentRunProcess = null;
                    Platform.runLater(() -> statusBar.setRunState(StatusBar.RunState.STOPPED));
                }
            }, "run-wait");
            waitThread.setDaemon(true);
            waitThread.start();
        } catch (IOException ex) {
            currentRunProcess = null;
            Platform.runLater(() -> statusBar.setRunState(StatusBar.RunState.STOPPED));
            new Alert(Alert.AlertType.ERROR, "Ошибка запуска: " + ex.getMessage()).showAndWait();
        }
    }

    /** Останавливает текущий запущенный процесс (Пуск → Остановить). */
    private void stopRun() {
        Process p = currentRunProcess;
        if (p != null && p.isAlive()) {
            p.destroyForcibly();
            currentRunProcess = null;
            Platform.runLater(() -> statusBar.setRunState(StatusBar.RunState.STOPPED));
        }
    }

    /** Запуск лексического анализа: текст из текущей вкладки → сканер → таблица лексем. */
    private void runLexer() {
        CodeArea area = getCurrentCodeArea();
        if (area == null) return;
        String text = area.getText();
        if (!checkAnalysisInputSize(text)) {
            return;
        }
        statusBar.setMessage(Messages.getString("status.analyzing"));
        CompletableFuture.supplyAsync(() -> TypeScriptInterfaceScanner.scan(text))
                .thenAccept(lexemes -> Platform.runLater(() -> {
                    statusBar.setMessage(Messages.getString("status.ready"));
                    outputPanel.showLexerResults(lexemes);
                }));
    }

    /** Синтаксический анализ: лексика + парсер, вкладка «Синтаксис». */
    private void runParser() {
        CodeArea area = getCurrentCodeArea();
        if (area == null) return;
        String text = area.getText();
        if (!checkAnalysisInputSize(text)) {
            return;
        }
        statusBar.setMessage(Messages.getString("status.analyzing"));
        CompletableFuture.supplyAsync(() -> TypeScriptInterfaceParser.parse(text))
                .thenAccept(result -> Platform.runLater(() -> {
                    statusBar.setMessage(Messages.getString("status.ready"));
                    outputPanel.showParserResults(result);
                }));
    }

    /** Семантический анализ (ЛР5): AST и проверки после успешного синтаксиса (разбор в фоне — не блокирует интерфейс). */
    private void runSemanticAnalysis() {
        CodeArea area = getCurrentCodeArea();
        if (area == null) return;
        String text = area.getText();
        if (!checkAnalysisInputSize(text)) {
            return;
        }
        statusBar.setMessage(Messages.getString("status.analyzing"));
        CompletableFuture.supplyAsync(() -> SemanticAnalysisBundle.compute(text))
                .thenAccept(bundle -> Platform.runLater(() -> finishSemanticAnalysis(bundle)));
    }

    private void finishSemanticAnalysis(SemanticAnalysisBundle bundle) {
        statusBar.setMessage(Messages.getString("status.ready"));
        ParseResult parseResult = bundle.parseResult();
        if (!parseResult.isSuccess()) {
            outputPanel.showSemanticSkippedDueToSyntax();
            new Alert(Alert.AlertType.WARNING, Messages.getString("semantic.needCleanSyntax")).showAndWait();
            outputPanel.showParserResults(parseResult);
            return;
        }
        outputPanel.showSemanticResults(bundle.astTree(), bundle.semErrors());
    }

    private boolean checkAnalysisInputSize(String text) {
        if (text.length() <= MAX_ANALYSIS_SOURCE_CHARS) {
            return true;
        }
        new Alert(Alert.AlertType.WARNING,
                Messages.getString("analysis.inputTooLarge", MAX_ANALYSIS_SOURCE_CHARS)).showAndWait();
        return false;
    }

    /** Результат фонового семантического конвейера (парсинг + семантика + печать AST). */
    private record SemanticAnalysisBundle(ParseResult parseResult, List<SemanticDiagnostic> semErrors, String astTree) {
        static SemanticAnalysisBundle compute(String source) {
            ParseResult pr = TypeScriptInterfaceParser.parse(source);
            if (!pr.isSuccess()) {
                return new SemanticAnalysisBundle(pr, List.of(), "");
            }
            AstProgram ast = pr.getAst() != null ? pr.getAst() : AstProgram.empty();
            return new SemanticAnalysisBundle(pr, SemanticAnalyzer.analyze(ast), AstPrinter.print(ast));
        }
    }

    /** Поиск подстрок по выбранному регулярному выражению (ЛР4). */
    private void runRegexSearch() {
        CodeArea area = getCurrentCodeArea();
        if (area == null) return;
        String text = area.getText();
        if (text.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, Messages.getString("regex.noData")).showAndWait();
            outputPanel.selectRegexTab();
            return;
        }
        RegexSearchKind kind = outputPanel.getRegexSearchPanel().getSelectedKind();
        if (kind == null) {
            new Alert(Alert.AlertType.WARNING, Messages.getString("regex.noKind")).showAndWait();
            return;
        }
        Pattern pattern = kind.getPattern();
        List<RegexMatch> matches = RegexSearch.findAll(text, pattern);
        outputPanel.showRegexResults(matches);
    }

    /** Выделяет в редакторе фрагмент, соответствующий строке таблицы поиска по РВ. */
    private void navigateToRegexMatch(RegexMatch m) {
        CodeArea area = getCurrentCodeArea();
        if (area == null || m == null) return;
        int start = m.startOffset();
        int end = Math.min(start + m.length(), area.getLength());
        end = Math.max(end, start);
        area.selectRange(start, end);
        area.requestFocus();
    }

    private int lineColToOffset(CodeArea area, int line1Based, int col1Based) {
        int paragraphs = area.getParagraphs().size();
        int offset = 0;
        int line = Math.max(1, line1Based);
        int col = Math.max(1, col1Based);
        for (int p = 0; p < line - 1 && p < paragraphs; p++) {
            offset += area.getText(p).length() + 1;
        }
        if (line > paragraphs) {
            return area.getLength();
        }
        String lineText = area.getText(line - 1);
        int col0 = Math.min(col - 1, lineText.length());
        offset += col0;
        return Math.min(offset, area.getLength());
    }

    /** Переводит курсор в редакторе на позицию (строка, столбец). Вызывается при клике по ошибке в таблице лексем. */
    private void navigateToEditorPosition(int line, int column) {
        CodeArea area = getCurrentCodeArea();
        if (area == null) return;
        int offset = lineColToOffset(area, line, column);
        area.selectRange(offset, offset);
        area.requestFocus();
        tabPane.getSelectionModel().select(getCurrentEditorTab() != null ? getCurrentEditorTab().getTab() : null);
    }

    /** Клик по строке таблицы семантических ошибок. */
    private void navigateToSemanticDiagnostic(SemanticDiagnostic d) {
        CodeArea area = getCurrentCodeArea();
        if (area == null || d == null) return;
        int start = lineColToOffset(area, d.getLine(), d.getStartColumn());
        int endExclusive = lineColToOffset(area, d.getLine(), d.getEndColumn() + 1);
        endExclusive = Math.min(Math.max(endExclusive, start), area.getLength());
        area.selectRange(start, endExclusive);
        area.requestFocus();
        tabPane.getSelectionModel().select(getCurrentEditorTab() != null ? getCurrentEditorTab().getTab() : null);
    }

    /** Выделяет диапазон столбцов на одной строке (клик по строке таблицы синтаксиса). */
    private void navigateToDiagnostic(SyntaxDiagnostic d) {
        CodeArea area = getCurrentCodeArea();
        if (area == null) return;
        int start = lineColToOffset(area, d.getLine(), d.getStartColumn());
        int endExclusive = lineColToOffset(area, d.getLine(), d.getEndColumn() + 1);
        endExclusive = Math.min(Math.max(endExclusive, start), area.getLength());
        area.selectRange(start, endExclusive);
        area.requestFocus();
        tabPane.getSelectionModel().select(getCurrentEditorTab() != null ? getCurrentEditorTab().getTab() : null);
    }

    private void readStreamToOutput(java.io.InputStream stream, boolean isStderr) {
        byte[] buf = new byte[1024];
        java.nio.charset.Charset cs = java.nio.charset.Charset.defaultCharset();
        try (var in = stream) {
            int n;
            while ((n = in.read(buf)) != -1) {
                String line = new String(buf, 0, n, cs);
                String prefix = isStderr ? "[stderr] " : "";
                String finalText = prefix + line;
                Platform.runLater(() -> outputPanel.appendOutput(finalText));
            }
        } catch (IOException ignored) {
        }
    }

    private void configureRun() {
        RunConfig initial = runConfig;
        if (initial == null && projectPanel.hasProject()) {
            Path root = projectPanel.getProjectRoot();
            initial = RunConfig.loadFromProject(root);
        }
        if (initial == null) initial = new RunConfig();
        RunConfigDialog dialog = new RunConfigDialog(stage, initial);
        Optional<RunConfig> result = dialog.showAndWait();
        result.ifPresent(config -> {
            runConfig = config;
            try {
                config.saveToProject();
            } catch (IOException ex) {
                new Alert(Alert.AlertType.WARNING, "Не удалось сохранить конфиг в проект: " + ex.getMessage()).showAndWait();
            }
            new Alert(Alert.AlertType.INFORMATION, "Настройки запуска сохранены.").showAndWait();
        });
    }

    private void showHelp() {
        new Alert(Alert.AlertType.INFORMATION,
                "Справка по Yopta IDE.\n\n" +
                        "Файл: создать, открыть файл/проект, сохранить, автосохранение.\n" +
                        "Вкладки: несколько файлов одновременно; закрытие вкладки — по крестику.\n" +
                        "Правка: отмена, буфер обмена, выделить всё.\n" +
                        "Пуск: лексический и синтаксический анализ, семантический анализ (AST и контекстные ошибки), поиск по регулярным выражениям (горячие клавиши в меню), запуск с отладкой и без.\n" +
                        "Вкладка «Лексемы» — токены; «Синтаксис» — ошибки разбора; «Семантика» — дерево AST и семантические ошибки; щелчок по строке таблицы выделяет фрагмент в редакторе.").showAndWait();
    }

    private void showAbout() {
        new Alert(Alert.AlertType.INFORMATION,
                "Yopta\nВерсия 0.1.0\n\nДесктоп-редактор на JavaFX и RichTextFX.").showAndWait();
    }

    public static void main(String[] args) {
        // Имя в доке (macOS) и в панели задач вместо "Java"
        System.setProperty("apple.awt.application.name", "Yopta Code");
        launch(args);
    }
}
