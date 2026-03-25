package ru.nstu.yopta;

import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Нижняя панель с вкладками: Терминал, Вывод, Сборка, Лексемы.
 */
public class OutputPanel extends VBox {

    private final TabPane tabPane;
    private final TerminalPanel terminalPanel;
    private final TextArea outputArea;
    private final TextArea buildArea;
    private final LexerResultsPanel lexerResultsPanel;
    private static final int LEXER_TAB_INDEX = 3;

    public OutputPanel() {
        setMinHeight(80);
        setPrefHeight(180);
        setMaxHeight(400);

        int fontSize = AppSettings.getInstance().getOutputFontSize();
        String fontStyle = "-fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: " + fontSize + "px;";

        terminalPanel = new TerminalPanel(fontSize);
        terminalPanel.getStyleClass().add("output-tab-content");

        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setWrapText(true);
        outputArea.setStyle(fontStyle);
        outputArea.setPromptText("Вывод запуска и другие сообщения...");
        VBox.setVgrow(outputArea, Priority.ALWAYS);
        ScrollPane outputScroll = new ScrollPane(outputArea);
        outputScroll.setFitToWidth(true);
        outputScroll.setFitToHeight(true);

        buildArea = new TextArea();
        buildArea.setEditable(false);
        buildArea.setWrapText(true);
        buildArea.setStyle(fontStyle);
        buildArea.setPromptText("Вывод сборки...");
        VBox.setVgrow(buildArea, Priority.ALWAYS);
        ScrollPane buildScroll = new ScrollPane(buildArea);
        buildScroll.setFitToWidth(true);
        buildScroll.setFitToHeight(true);

        tabPane = new TabPane();
        Tab termTab = new Tab(Messages.getString("output.tab.terminal"), terminalPanel);
        termTab.setClosable(false);
        Tab outTab = new Tab(Messages.getString("output.tab.output"), outputScroll);
        outTab.setClosable(false);
        Tab buildTab = new Tab(Messages.getString("output.tab.build"), buildScroll);
        buildTab.setClosable(false);
        lexerResultsPanel = new LexerResultsPanel();
        Tab lexerTab = new Tab(Messages.getString("output.tab.lexer"), lexerResultsPanel);
        lexerTab.setClosable(false);
        tabPane.getTabs().addAll(termTab, outTab, buildTab, lexerTab);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        getChildren().add(tabPane);
        getStyleClass().add("output-panel");
    }

    public TerminalPanel getTerminalPanel() {
        return terminalPanel;
    }

    /** Вывод в вкладку «Вывод». */
    public void appendOutput(String text) {
        outputArea.appendText(text);
    }

    /** Переключиться на вкладку «Вывод». */
    public void selectOutputTab() {
        tabPane.getSelectionModel().select(1);
    }

    /** Переключиться на вкладку «Сборка». */
    public void selectBuildTab() {
        tabPane.getSelectionModel().select(2);
    }

    /** Переключиться на вкладку «Лексемы». */
    public void selectLexerTab() {
        tabPane.getSelectionModel().select(LEXER_TAB_INDEX);
    }

    /** Показать результаты лексического анализа в таблице. */
    public void showLexerResults(List<Lexeme> lexemes) {
        lexerResultsPanel.setLexemes(lexemes);
        selectLexerTab();
    }

    /** Панель результатов сканера (для установки callback навигации по ошибкам). */
    public LexerResultsPanel getLexerResultsPanel() {
        return lexerResultsPanel;
    }

    /** Вывод в вкладку «Сборка». */
    public void appendBuild(String text) {
        buildArea.appendText(text);
    }

    /** Применить новый размер шрифта вывода ко всем вкладкам. */
    public void setOutputFontSize(int size) {
        String fontStyle = "-fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: " + size + "px;";
        outputArea.setStyle(fontStyle);
        buildArea.setStyle(fontStyle);
        terminalPanel.setOutputFontSize(size);
    }

    /** Обновить заголовки вкладок при смене языка. */
    public void updateTabTitles() {
        tabPane.getTabs().get(0).setText(Messages.getString("output.tab.terminal"));
        tabPane.getTabs().get(1).setText(Messages.getString("output.tab.output"));
        tabPane.getTabs().get(2).setText(Messages.getString("output.tab.build"));
        tabPane.getTabs().get(LEXER_TAB_INDEX).setText(Messages.getString("output.tab.lexer"));
    }

    public void destroy() {
        terminalPanel.destroy();
    }
}
