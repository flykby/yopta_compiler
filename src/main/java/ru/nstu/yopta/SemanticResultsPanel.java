package ru.nstu.yopta;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

/**
 * Вкладка семантики: текст AST, таблица ошибок, счётчик.
 */
public class SemanticResultsPanel extends VBox {

    private final TextArea astArea;
    private final TableView<SemanticDiagnostic> table;
    private final Label countLabel;
    private final ObservableList<SemanticDiagnostic> items = FXCollections.observableArrayList();
    private Consumer<SemanticDiagnostic> onRowClick;

    public SemanticResultsPanel() {
        astArea = new TextArea();
        astArea.setEditable(false);
        astArea.setWrapText(false);
        astArea.setPromptText("(AST)");
        astArea.setMinHeight(160);
        astArea.setPrefRowCount(14);
        String fontStyle = "-fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: "
                + AppSettings.getInstance().getOutputFontSize() + "px;";
        astArea.setStyle(fontStyle);
        VBox.setVgrow(astArea, Priority.ALWAYS);

        table = new TableView<>(items);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(140);

        TableColumn<SemanticDiagnostic, String> fragmentCol = new TableColumn<>(Messages.getString("semantic.column.fragment"));
        fragmentCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFragment()));

        TableColumn<SemanticDiagnostic, String> locationCol = new TableColumn<>(Messages.getString("semantic.column.location"));
        locationCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getLocationString()));

        TableColumn<SemanticDiagnostic, String> descCol = new TableColumn<>(Messages.getString("semantic.column.description"));
        descCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDescription()));

        table.getColumns().addAll(fragmentCol, locationCol, descCol);

        countLabel = new Label();
        countLabel.setWrapText(true);

        getChildren().addAll(astArea, table, countLabel);
        getStyleClass().add("semantic-results-panel");

        table.setRowFactory(tv -> {
            TableRow<SemanticDiagnostic> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 1 && onRowClick != null) {
                    int idx = table.getSelectionModel().getSelectedIndex();
                    if (idx >= 0 && idx < items.size()) {
                        onRowClick.accept(items.get(idx));
                    }
                }
            });
            return row;
        });
    }

    public void setAstText(String text) {
        astArea.setText(text != null ? text : "");
    }

    public void setDiagnostics(List<SemanticDiagnostic> diagnostics) {
        items.clear();
        if (diagnostics != null) {
            items.addAll(diagnostics);
        }
        int n = items.size();
        countLabel.setText(Messages.getString("semantic.errorCount", n));
    }

    public void setOnRowClick(Consumer<SemanticDiagnostic> callback) {
        this.onRowClick = callback;
    }

    public void setFontSize(int size) {
        String fontStyle = "-fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: " + size + "px;";
        astArea.setStyle(fontStyle);
    }

    public void refreshLocale() {
        if (!table.getColumns().isEmpty()) {
            table.getColumns().get(0).setText(Messages.getString("semantic.column.fragment"));
            table.getColumns().get(1).setText(Messages.getString("semantic.column.location"));
            table.getColumns().get(2).setText(Messages.getString("semantic.column.description"));
        }
        int n = items.size();
        countLabel.setText(Messages.getString("semantic.errorCount", n));
    }
}
