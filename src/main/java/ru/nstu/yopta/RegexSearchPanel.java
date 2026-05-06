package ru.nstu.yopta;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

/**
 * Панель поиска по регулярным выражениям: выбор варианта, кнопка «Пуск», таблица совпадений, счётчик.
 */
public class RegexSearchPanel extends VBox {

    private final ComboBox<RegexSearchKind> kindCombo;
    private final Button runButton;
    private final TableView<RegexMatch> table;
    private final Label countLabel;
    private final ObservableList<RegexMatch> items = FXCollections.observableArrayList();
    private Runnable onRunRequested;
    private Consumer<RegexMatch> onRowClick;

    public RegexSearchPanel() {
        kindCombo = new ComboBox<>(FXCollections.observableArrayList(RegexSearchKind.values()));
        kindCombo.getSelectionModel().selectFirst();
        kindCombo.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(kindCombo, Priority.NEVER);
        kindCombo.setCellFactory(lv -> newKindListCell());
        kindCombo.setButtonCell(newKindListCell());

        runButton = new Button(Messages.getString("regex.run"));
        runButton.setOnAction(e -> {
            if (onRunRequested != null) {
                onRunRequested.run();
            }
        });

        ToolBar bar = new ToolBar(kindCombo, runButton);

        table = new TableView<>(items);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<RegexMatch, String> fragmentCol = new TableColumn<>(Messages.getString("regex.column.fragment"));
        fragmentCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().fragment()));

        TableColumn<RegexMatch, String> locationCol = new TableColumn<>(Messages.getString("regex.column.location"));
        locationCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getLocationString()));

        TableColumn<RegexMatch, Number> lengthCol = new TableColumn<>(Messages.getString("regex.column.length"));
        lengthCol.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().length()));

        table.getColumns().addAll(fragmentCol, locationCol, lengthCol);
        VBox.setVgrow(table, Priority.ALWAYS);

        countLabel = new Label();
        countLabel.setWrapText(true);
        countLabel.getStyleClass().add("regex-match-count");

        getChildren().addAll(bar, table, countLabel);
        getStyleClass().add("regex-search-panel");

        table.setRowFactory(tv -> {
            TableRow<RegexMatch> row = new TableRow<>();
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

    public RegexSearchKind getSelectedKind() {
        return kindCombo.getSelectionModel().getSelectedItem();
    }

    public void setMatches(List<RegexMatch> matches) {
        items.clear();
        if (matches != null) {
            items.addAll(matches);
        }
        int n = items.size();
        countLabel.setText(Messages.getString("regex.matchCount", n));
    }

    public void setOnRunRequested(Runnable callback) {
        this.onRunRequested = callback;
    }

    public void setOnRowClick(Consumer<RegexMatch> callback) {
        this.onRowClick = callback;
    }

    /** Обновить подписи после смены языка интерфейса. */
    public void refreshLocale() {
        runButton.setText(Messages.getString("regex.run"));
        if (!table.getColumns().isEmpty()) {
            table.getColumns().get(0).setText(Messages.getString("regex.column.fragment"));
            table.getColumns().get(1).setText(Messages.getString("regex.column.location"));
            table.getColumns().get(2).setText(Messages.getString("regex.column.length"));
        }
        RegexSearchKind sel = kindCombo.getSelectionModel().getSelectedItem();
        kindCombo.setItems(FXCollections.observableArrayList(RegexSearchKind.values()));
        if (sel != null) {
            kindCombo.getSelectionModel().select(sel);
        }
        int n = items.size();
        countLabel.setText(Messages.getString("regex.matchCount", n));
    }

    private static ListCell<RegexSearchKind> newKindListCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(RegexSearchKind item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getDisplayName());
            }
        };
    }
}
