package ru.nstu.yopta;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Диалог «Настройки запуска»: корень проекта, команда запуска, команда отладки.
 */
public class RunConfigDialog {

    private final Stage owner;
    private final RunConfig initial;

    public RunConfigDialog(Stage owner, RunConfig initial) {
        this.owner = owner;
        this.initial = initial != null ? initial : new RunConfig();
    }

    public Optional<RunConfig> showAndWait() {
        Dialog<RunConfig> dialog = new Dialog<>();
        dialog.setTitle("Настройки запуска");
        dialog.setHeaderText("Укажите корень проекта и команды для запуска и отладки.");
        dialog.initOwner(owner);

        ButtonType ok = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);

        TextField projectRootField = new TextField(initial.getProjectRoot());
        projectRootField.setPromptText("Путь к корню проекта (рабочая директория)");
        projectRootField.setPrefColumnCount(40);

        Button browseBtn = new Button("Обзор…");
        browseBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Выбрать корень проекта");
            if (!projectRootField.getText().isBlank()) {
                try {
                    dc.setInitialDirectory(Path.of(projectRootField.getText()).toFile());
                } catch (Exception ignored) {
                }
            }
            var dir = dc.showDialog(owner);
            if (dir != null) projectRootField.setText(dir.getAbsolutePath());
        });

        TextField runCommandField = new TextField(initial.getRunCommand());
        runCommandField.setPromptText("Например: java -jar app.jar или ./gradlew run");
        runCommandField.setPrefColumnCount(40);

        TextField debugCommandField = new TextField(initial.getDebugCommand());
        debugCommandField.setPromptText("Например: java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y -jar app.jar");
        debugCommandField.setPrefColumnCount(40);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));
        grid.add(new Label("Корень проекта:"), 0, 0);
        grid.add(projectRootField, 1, 0);
        grid.add(browseBtn, 2, 0);
        grid.add(new Label("Команда запуска:"), 0, 1);
        grid.add(runCommandField, 1, 1);
        grid.add(new Label("Команда отладки:"), 0, 2);
        grid.add(debugCommandField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn != ok) return null;
            return new RunConfig(
                    projectRootField.getText().trim(),
                    runCommandField.getText().trim(),
                    debugCommandField.getText().trim()
            );
        });

        return dialog.showAndWait();
    }
}
