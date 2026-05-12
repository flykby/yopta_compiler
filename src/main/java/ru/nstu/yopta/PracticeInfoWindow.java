package ru.nstu.yopta;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Отдельное немодальное окно с текстом раздела практической работы (меню «Текст»).
 */
public final class PracticeInfoWindow {

    private PracticeInfoWindow() {}

    public static void show(Window owner, String title, String text) {
        Stage w = new Stage();
        if (owner != null) {
            w.initOwner(owner);
        }
        w.initModality(Modality.NONE);
        w.setTitle(title);

        TextArea ta = new TextArea(text);
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setStyle("-fx-font-family: system-ui, 'Segoe UI', sans-serif; -fx-font-size: 13px;");
        VBox.setVgrow(ta, Priority.ALWAYS);

        Button close = new Button(Messages.getString("textPractice.close"));
        close.setOnAction(e -> w.close());
        VBox root = new VBox(8, ta, close);
        VBox.setMargin(close, new Insets(0, 8, 8, 8));
        root.setPadding(new Insets(8));

        Scene sc = new Scene(root, 680, 520);
        w.setScene(sc);
        w.show();
    }
}
