package ru.nstu.yopta;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * Строка состояния внизу окна: файл, строка/столбец, состояние запуска (Сборка/Запущено/Выключено), сообщение.
 */
public class StatusBar extends HBox {

    /** Состояние работы приложения: сборка, запущено, выключено. */
    public enum RunState {
        BUILDING,
        RUNNING,
        STOPPED
    }

    private final Label fileLabel;
    private final Label positionLabel;
    private final Label stateLabel;
    private final Label messageLabel;

    public StatusBar() {
        setPadding(new Insets(4, 8, 4, 8));
        setSpacing(16);
        getStyleClass().add("status-bar");

        fileLabel = new Label(Messages.getString("status.untitled"));
        fileLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(fileLabel, Priority.NEVER);

        positionLabel = new Label("");
        stateLabel = new Label(Messages.getString("status.state.stopped"));
        stateLabel.getStyleClass().addAll("status-state", "status-state-stopped");
        stateLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(stateLabel, Priority.NEVER);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        messageLabel = new Label(Messages.getString("status.ready"));
        messageLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(messageLabel, Priority.ALWAYS);

        getChildren().addAll(fileLabel, positionLabel, stateLabel, spacer, messageLabel);
    }

    /** Устанавливает отображаемое состояние запуска (Сборка/Запущено/Выключено). */
    public void setRunState(RunState state) {
        String key = switch (state) {
            case BUILDING -> "status.state.building";
            case RUNNING -> "status.state.running";
            case STOPPED -> "status.state.stopped";
        };
        stateLabel.setText(Messages.getString(key));
        stateLabel.getStyleClass().removeAll("status-state-building", "status-state-running", "status-state-stopped");
        stateLabel.getStyleClass().add("status-state-" + state.name().toLowerCase());
    }

    public void setFileName(String name) {
        fileLabel.setText(name == null || name.isEmpty() ? Messages.getString("status.untitled") : name);
    }

    public void setPosition(int line, int column) {
        positionLabel.setText(Messages.getString("status.lineCol", line, column));
    }

    public void clearPosition() {
        positionLabel.setText("");
    }

    public void setMessage(String message) {
        messageLabel.setText(message == null ? Messages.getString("status.ready") : message);
    }
}
