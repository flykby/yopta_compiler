package ru.nstu.yopta;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Вкладка ЛР6: лексемы арифметического выражения, синтаксические ошибки, тетрады, ПОЛИЗ и вычисление.
 */
public class IrResultsPanel extends VBox {

    private final Label lexerCaption;
    private final TableView<Lexeme> lexTable;
    private final Label syntaxCaption;
    private final TableView<SyntaxDiagnostic> synTable;
    private final Label quadCaption;
    private final TableView<Quad> quadTable;
    private final Label polizCaption;
    private final TextArea polizArea;
    private final Label footerLabel;
    private final ObservableList<Lexeme> lexItems = FXCollections.observableArrayList();
    private final ObservableList<SyntaxDiagnostic> synItems = FXCollections.observableArrayList();
    private final ObservableList<Quad> quadItems = FXCollections.observableArrayList();
    private BiConsumer<Integer, Integer> onLexerErrorClick;
    private Consumer<SyntaxDiagnostic> onSyntaxClick;

    public IrResultsPanel() {
        lexerCaption = new Label(Messages.getString("ir.section.lexer"));
        syntaxCaption = new Label(Messages.getString("ir.section.syntax"));
        quadCaption = new Label(Messages.getString("ir.section.quads"));
        polizCaption = new Label(Messages.getString("ir.section.poliz"));

        int fs = AppSettings.getInstance().getOutputFontSize();
        String fontStyle = "-fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: " + fs + "px;";

        lexTable = buildLexTable();
        lexTable.setPrefHeight(130);
        synTable = buildSynTable();
        synTable.setPrefHeight(110);
        quadTable = buildQuadTable();
        quadTable.setPrefHeight(130);

        polizArea = new TextArea();
        polizArea.setEditable(false);
        polizArea.setWrapText(true);
        polizArea.setPromptText(Messages.getString("ir.poliz.prompt"));
        polizArea.setStyle(fontStyle);
        polizArea.setPrefRowCount(3);
        polizArea.setMinHeight(60);

        footerLabel = new Label();
        footerLabel.setWrapText(true);
        footerLabel.setStyle(fontStyle);

        VBox inner = new VBox(6,
                lexerCaption, lexTable,
                syntaxCaption, synTable,
                quadCaption, quadTable,
                polizCaption, polizArea,
                footerLabel);
        inner.setFillWidth(true);
        ScrollPane scroll = new ScrollPane(inner);
        scroll.setFitToWidth(true);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().add(scroll);
        getStyleClass().add("ir-results-panel");
    }

    private TableView<Lexeme> buildLexTable() {
        TableView<Lexeme> table = new TableView<>(lexItems);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<Lexeme, Number> codeCol = new TableColumn<>(Messages.getString("lexer.column.code"));
        codeCol.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getCode()));
        TableColumn<Lexeme, String> typeCol = new TableColumn<>(Messages.getString("lexer.column.type"));
        typeCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTypeName()));
        TableColumn<Lexeme, String> lexCol = new TableColumn<>(Messages.getString("lexer.column.lexeme"));
        lexCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getText()));
        TableColumn<Lexeme, String> locCol = new TableColumn<>(Messages.getString("lexer.column.location"));
        locCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getLocationString()));
        table.getColumns().addAll(codeCol, typeCol, lexCol, locCol);
        table.setRowFactory(tv -> {
            TableRow<Lexeme> row = new TableRow<>();
            row.itemProperty().addListener((o, oldVal, lex) -> {
                if (lex != null && lex.isError()) {
                    if (!row.getStyleClass().contains("lexer-error-row")) {
                        row.getStyleClass().add("lexer-error-row");
                    }
                } else {
                    row.getStyleClass().removeAll("lexer-error-row");
                }
            });
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 1 && onLexerErrorClick != null) {
                    int idx = table.getSelectionModel().getSelectedIndex();
                    if (idx >= 0 && idx < lexItems.size()) {
                        Lexeme lex = lexItems.get(idx);
                        if (lex.isError()) {
                            onLexerErrorClick.accept(lex.getLine(), lex.getStartColumn());
                        }
                    }
                }
            });
            return row;
        });
        return table;
    }

    private TableView<SyntaxDiagnostic> buildSynTable() {
        TableView<SyntaxDiagnostic> table = new TableView<>(synItems);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<SyntaxDiagnostic, String> fragmentCol = new TableColumn<>(Messages.getString("parser.column.fragment"));
        fragmentCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFragment()));
        TableColumn<SyntaxDiagnostic, String> locationCol = new TableColumn<>(Messages.getString("parser.column.location"));
        locationCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getLocationString()));
        TableColumn<SyntaxDiagnostic, String> descCol = new TableColumn<>(Messages.getString("parser.column.description"));
        descCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDescription()));
        table.getColumns().addAll(fragmentCol, locationCol, descCol);
        table.setRowFactory(tv -> {
            TableRow<SyntaxDiagnostic> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 1 && onSyntaxClick != null) {
                    int idx = table.getSelectionModel().getSelectedIndex();
                    if (idx >= 0 && idx < synItems.size()) {
                        onSyntaxClick.accept(synItems.get(idx));
                    }
                }
            });
            return row;
        });
        return table;
    }

    private TableView<Quad> buildQuadTable() {
        TableView<Quad> table = new TableView<>(quadItems);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<Quad, String> opCol = new TableColumn<>(Messages.getString("ir.column.op"));
        opCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getOp()));
        TableColumn<Quad, String> a1 = new TableColumn<>(Messages.getString("ir.column.arg1"));
        a1.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getArg1()));
        TableColumn<Quad, String> a2 = new TableColumn<>(Messages.getString("ir.column.arg2"));
        a2.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getArg2()));
        TableColumn<Quad, String> resCol = new TableColumn<>(Messages.getString("ir.column.result"));
        resCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getResult()));
        table.getColumns().addAll(opCol, a1, a2, resCol);
        return table;
    }

    public void setResult(ArithmeticAnalysisResult r) {
        lexItems.clear();
        synItems.clear();
        quadItems.clear();
        if (r == null) {
            polizArea.clear();
            footerLabel.setText("");
            return;
        }
        if (r.lexemes() != null) {
            lexItems.addAll(r.lexemes());
        }
        if (r.syntaxErrors() != null) {
            synItems.addAll(r.syntaxErrors());
        }
        if (r.showQuads() && r.quads() != null) {
            quadItems.addAll(r.quads());
        }
        polizArea.setText(r.polizLine() != null ? r.polizLine() : "");
        footerLabel.setText(r.footerNote() != null ? r.footerNote() : "");
    }

    public void setOnLexerErrorClick(BiConsumer<Integer, Integer> callback) {
        this.onLexerErrorClick = callback;
    }

    public void setOnSyntaxClick(Consumer<SyntaxDiagnostic> callback) {
        this.onSyntaxClick = callback;
    }

    public void setFontSize(int size) {
        String fontStyle = "-fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: " + size + "px;";
        polizArea.setStyle(fontStyle);
        footerLabel.setStyle(fontStyle);
    }

    public void refreshLocale() {
        lexerCaption.setText(Messages.getString("ir.section.lexer"));
        syntaxCaption.setText(Messages.getString("ir.section.syntax"));
        quadCaption.setText(Messages.getString("ir.section.quads"));
        polizCaption.setText(Messages.getString("ir.section.poliz"));
        polizArea.setPromptText(Messages.getString("ir.poliz.prompt"));
        refreshTableColumns(lexTable, 0,
                Messages.getString("lexer.column.code"),
                Messages.getString("lexer.column.type"),
                Messages.getString("lexer.column.lexeme"),
                Messages.getString("lexer.column.location"));
        refreshTableColumns(synTable, 0,
                Messages.getString("parser.column.fragment"),
                Messages.getString("parser.column.location"),
                Messages.getString("parser.column.description"));
        refreshTableColumns(quadTable, 0,
                Messages.getString("ir.column.op"),
                Messages.getString("ir.column.arg1"),
                Messages.getString("ir.column.arg2"),
                Messages.getString("ir.column.result"));
    }

    @SuppressWarnings("unchecked")
    private void refreshTableColumns(TableView<?> table, int startCol, String... titles) {
        var cols = table.getColumns();
        for (int i = 0; i < titles.length && i + startCol < cols.size(); i++) {
            ((TableColumn<?, ?>) cols.get(i + startCol)).setText(titles[i]);
        }
    }
}
