package ru.nstu.yopta;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Панель с таблицей результатов лексического анализа.
 * При щелчке по строке с ошибкой вызывается callback для навигации в редакторе.
 */
public class LexerResultsPanel extends VBox {

    private final TableView<Lexeme> table;
    private final ObservableList<Lexeme> items = FXCollections.observableArrayList();
    private BiConsumer<Integer, Integer> onErrorClick;

    public LexerResultsPanel() {
        table = new TableView<>(items);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Lexeme, Number> codeCol = new TableColumn<>(Messages.getString("lexer.column.code"));
        codeCol.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getCode()));

        TableColumn<Lexeme, String> typeCol = new TableColumn<>(Messages.getString("lexer.column.type"));
        typeCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTypeName()));

        TableColumn<Lexeme, String> lexemeCol = new TableColumn<>(Messages.getString("lexer.column.lexeme"));
        lexemeCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getText()));

        TableColumn<Lexeme, String> locationCol = new TableColumn<>(Messages.getString("lexer.column.location"));
        locationCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getLocationString()));

        table.getColumns().addAll(codeCol, typeCol, lexemeCol, locationCol);
        VBox.setVgrow(table, Priority.ALWAYS);
        getChildren().add(table);
        getStyleClass().add("lexer-results-panel");

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
                if (event.getClickCount() == 1 && onErrorClick != null) {
                    int idx = table.getSelectionModel().getSelectedIndex();
                    if (idx >= 0 && idx < items.size()) {
                        Lexeme lex = items.get(idx);
                        if (lex.isError()) {
                            onErrorClick.accept(lex.getLine(), lex.getStartColumn());
                        }
                    }
                }
            });
            return row;
        });
    }

    /** Заполняет таблицу результатами сканирования. */
    public void setLexemes(List<Lexeme> lexemes) {
        items.clear();
        if (lexemes != null) {
            items.addAll(lexemes);
        }
    }

    /** Callback: при клике по строке-ошибке вызывается с (номер строки, столбец). */
    public void setOnErrorClick(BiConsumer<Integer, Integer> callback) {
        this.onErrorClick = callback;
    }
}
