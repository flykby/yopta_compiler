package ru.nstu.yopta;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

/**
 * Диалог настроек: размер шрифта редактора и вывода, язык интерфейса.
 */
public class SettingsDialog {

    private final Stage owner;
    private Spinner<Integer> editorSizeSpinner;
    private Spinner<Integer> outputSizeSpinner;
    private ComboBox<String> languageCombo;

    public SettingsDialog(Stage owner) {
        this.owner = owner;
    }

    public boolean showAndWait() {
        AppSettings s = AppSettings.getInstance();
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle(Messages.getString("settings.title"));
        dialog.initOwner(owner);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.APPLY, ButtonType.CANCEL);

        editorSizeSpinner = new Spinner<>(8, 32, s.getEditorFontSize());
        editorSizeSpinner.setEditable(true);
        outputSizeSpinner = new Spinner<>(8, 32, s.getOutputFontSize());
        outputSizeSpinner.setEditable(true);

        languageCombo = new ComboBox<>();
        languageCombo.getItems().addAll(
                Messages.getString("language.system"),
                Messages.getString("language.ru"),
                Messages.getString("language.en")
        );
        String lang = s.getLanguage();
        if ("ru".equalsIgnoreCase(lang)) languageCombo.getSelectionModel().select(Messages.getString("language.ru"));
        else if ("en".equalsIgnoreCase(lang)) languageCombo.getSelectionModel().select(Messages.getString("language.en"));
        else languageCombo.getSelectionModel().select(Messages.getString("language.system"));

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));
        grid.add(new Label(Messages.getString("settings.editorFontSize")), 0, 0);
        grid.add(editorSizeSpinner, 1, 0);
        grid.add(new Label(Messages.getString("settings.outputFontSize")), 0, 1);
        grid.add(outputSizeSpinner, 1, 1);
        grid.add(new Label(Messages.getString("settings.language")), 0, 2);
        grid.add(languageCombo, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.APPLY) return false;
            int editorVal = editorSizeSpinner.getValue();
            s.setEditorFontSize(editorVal);
            s.setOutputFontSize(outputSizeSpinner.getValue());
            String sel = languageCombo.getSelectionModel().getSelectedItem();
            if (Messages.getString("language.ru").equals(sel)) s.setLanguage("ru");
            else if (Messages.getString("language.en").equals(sel)) s.setLanguage("en");
            else s.setLanguage("");
            s.save();
            return true;
        });

        return Boolean.TRUE.equals(dialog.showAndWait().orElse(false));
    }
}
