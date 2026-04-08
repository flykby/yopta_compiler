package ru.nstu.yopta;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

/**
 * Таблица синтаксических ошибок: фрагмент, местоположение, описание; счётчик; навигация по клику.
 */
public class ParserResultsPanel extends VBox {

    private final TableView<SyntaxDiagnostic> table;
    private final Label countLabel;
    private final ObservableList<SyntaxDiagnostic> items = FXCollections.observableArrayList();
    private Consumer<SyntaxDiagnostic> onRowClick;

    public ParserResultsPanel() {
        table = new TableView<>(items);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<SyntaxDiagnostic, String> fragmentCol = new TableColumn<>(Messages.getString("parser.column.fragment"));
        fragmentCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFragment()));

        TableColumn<SyntaxDiagnostic, String> locationCol = new TableColumn<>(Messages.getString("parser.column.location"));
        locationCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getLocationString()));

        TableColumn<SyntaxDiagnostic, String> descCol = new TableColumn<>(Messages.getString("parser.column.description"));
        descCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDescription()));

        table.getColumns().addAll(fragmentCol, locationCol, descCol);
        VBox.setVgrow(table, Priority.ALWAYS);

        countLabel = new Label();
        countLabel.getStyleClass().add("parser-error-count");
        countLabel.setWrapText(true);

        getChildren().addAll(table, countLabel);
        getStyleClass().add("parser-results-panel");

        table.setRowFactory(tv -> {
            TableRow<SyntaxDiagnostic> row = new TableRow<>();
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

    public void setDiagnostics(List<SyntaxDiagnostic> diagnostics) {
        items.clear();
        if (diagnostics != null) {
            items.addAll(diagnostics);
        }
        int n = items.size();
        countLabel.setText(Messages.getString("parser.errorCount", n));
    }

    public void setSuccessMessage() {
        items.clear();
        countLabel.setText(Messages.getString("parser.noErrors"));
    }

    public void setOnRowClick(Consumer<SyntaxDiagnostic> callback) {
        this.onRowClick = callback;
    }
}
