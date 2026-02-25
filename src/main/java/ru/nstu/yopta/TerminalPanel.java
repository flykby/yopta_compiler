package ru.nstu.yopta;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Панель встроенного терминала: вывод и строка ввода с приглашением, как в настоящем терминале.
 */
public class TerminalPanel extends VBox {

    private static final String PROMPT = System.getProperty("os.name").toLowerCase().startsWith("windows") ? "> " : "$ ";

    private final TextArea outputArea;
    private final TextField inputField;
    private final Label promptLabel;
    private Process process;
    private int outputFontSize;
    private final ExecutorService reader = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "terminal-reader");
        t.setDaemon(true);
        return t;
    });

    public TerminalPanel() {
        this(AppSettings.getInstance().getOutputFontSize());
    }

    public TerminalPanel(int outputFontSize) {
        this.outputFontSize = outputFontSize;
        setMinHeight(80);
        setPrefHeight(180);
        setMaxHeight(400);

        Label header = new Label(Messages.getString("output.tab.terminal"));
        header.getStyleClass().add("terminal-header");

        String fontStyle = "-fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: " + outputFontSize + "px;";
        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setWrapText(true);
        outputArea.setStyle(fontStyle);
        outputArea.setPromptText("");
        VBox.setVgrow(outputArea, Priority.ALWAYS);

        promptLabel = new Label(TerminalPanel.PROMPT);
        promptLabel.setStyle(fontStyle + " -fx-text-fill: #eceff1;");
        promptLabel.getStyleClass().add("terminal-prompt");

        inputField = new TextField();
        inputField.setPromptText("");
        inputField.setStyle(fontStyle + " -fx-background-color: transparent; -fx-text-fill: #eceff1;");
        inputField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                sendLine();
            }
        });
        HBox.setHgrow(inputField, Priority.ALWAYS);

        HBox inputLine = new HBox(0);
        inputLine.getChildren().addAll(promptLabel, inputField);
        inputLine.getStyleClass().add("terminal-input-line");

        getChildren().addAll(header, outputArea, inputLine);
        getStyleClass().add("terminal-panel");

        startShell();
    }

    private void startShell() {
        try {
            ProcessBuilder pb = new ProcessBuilder();
            boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
            if (isWindows) {
                pb.command("cmd.exe");
            } else {
                pb.command("/bin/bash", "--norc");
            }
            pb.redirectErrorStream(false);
            process = pb.start();

            reader.execute(this::readStream);
            reader.execute(this::readErrorStream);
            Platform.runLater(() -> appendOutput(PROMPT));
        } catch (IOException e) {
            appendOutput("Ошибка запуска терминала: " + e.getMessage() + "\n" + PROMPT);
        }
    }

    private void readStream() {
        byte[] buf = new byte[1024];
        Charset cs = Charset.defaultCharset();
        try (var in = process.getInputStream()) {
            int n;
            while ((n = in.read(buf)) != -1) {
                String s = new String(buf, 0, n, cs);
                String fin = s;
                Platform.runLater(() -> appendOutput(fin));
            }
        } catch (IOException ignored) {
        }
    }

    private void readErrorStream() {
        byte[] buf = new byte[1024];
        Charset cs = Charset.defaultCharset();
        try (var in = process.getErrorStream()) {
            int n;
            while ((n = in.read(buf)) != -1) {
                String s = new String(buf, 0, n, cs);
                String fin = s;
                Platform.runLater(() -> appendOutput(fin));
            }
        } catch (IOException ignored) {
        }
    }

    private void appendOutput(String text) {
        outputArea.appendText(text);
        Platform.runLater(() -> {
            outputArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    private void sendLine() {
        String line = inputField.getText().trim();
        if (process == null || !process.isAlive()) {
            appendOutput("Терминал не запущен.\n" + PROMPT);
            return;
        }
        appendOutput(PROMPT + line + "\n");
        inputField.clear();
        OutputStream out = process.getOutputStream();
        try {
            out.write((line + "\n").getBytes(Charset.defaultCharset()));
            out.flush();
        } catch (IOException e) {
            appendOutput("[Ошибка: " + e.getMessage() + "]\n" + PROMPT);
        }
    }

    public void setOutputFontSize(int size) {
        this.outputFontSize = size;
        String fontStyle = "-fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: " + size + "px;";
        outputArea.setStyle(fontStyle);
        promptLabel.setStyle(fontStyle + " -fx-text-fill: #eceff1;");
        inputField.setStyle(fontStyle + " -fx-background-color: transparent; -fx-text-fill: #eceff1;");
    }

    /** Останавливает процесс оболочки при закрытии приложения. */
    public void destroy() {
        reader.shutdownNow();
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
        }
    }
}
