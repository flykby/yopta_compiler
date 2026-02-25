package ru.nstu.yopta;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Конфигурация запуска: корень проекта, команда для запуска и команда для отладки.
 */
public class RunConfig {

    public static final String CONFIG_DIR = ".yopta";
    public static final String CONFIG_FILE = "run-config.properties";
    public static final String KEY_PROJECT_ROOT = "project.root";
    public static final String KEY_RUN_COMMAND = "run.command";
    public static final String KEY_DEBUG_COMMAND = "debug.command";

    private String projectRoot = "";
    private String runCommand = "";
    private String debugCommand = "";

    public RunConfig() {
    }

    public RunConfig(String projectRoot, String runCommand, String debugCommand) {
        this.projectRoot = projectRoot != null ? projectRoot : "";
        this.runCommand = runCommand != null ? runCommand : "";
        this.debugCommand = debugCommand != null ? debugCommand : "";
    }

    public String getProjectRoot() {
        return projectRoot;
    }

    public void setProjectRoot(String projectRoot) {
        this.projectRoot = projectRoot != null ? projectRoot : "";
    }

    public Path getProjectRootPath() {
        if (projectRoot == null || projectRoot.isBlank()) return null;
        return Path.of(projectRoot);
    }

    public String getRunCommand() {
        return runCommand;
    }

    public void setRunCommand(String runCommand) {
        this.runCommand = runCommand != null ? runCommand : "";
    }

    public String getDebugCommand() {
        return debugCommand;
    }

    public void setDebugCommand(String debugCommand) {
        this.debugCommand = debugCommand != null ? debugCommand : "";
    }

    /**
     * Загружает конфиг из папки проекта (projectRoot/.yopta/run-config.properties).
     * Если файла нет или projectRoot пустой — возвращает пустой конфиг.
     */
    public static RunConfig loadFromProject(Path projectRoot) {
        if (projectRoot == null || !Files.isDirectory(projectRoot)) return new RunConfig();
        Path configPath = projectRoot.resolve(CONFIG_DIR).resolve(CONFIG_FILE);
        if (!Files.isRegularFile(configPath)) return new RunConfig();
        Properties p = new Properties();
        try (var in = Files.newInputStream(configPath)) {
            p.load(in);
        } catch (IOException e) {
            return new RunConfig();
        }
        RunConfig c = new RunConfig();
        c.setProjectRoot(projectRoot.toAbsolutePath().toString());
        c.setRunCommand(p.getProperty(KEY_RUN_COMMAND, ""));
        c.setDebugCommand(p.getProperty(KEY_DEBUG_COMMAND, ""));
        return c;
    }

    /**
     * Сохраняет конфиг в projectRoot/.yopta/run-config.properties.
     */
    public void saveToProject() throws IOException {
        if (projectRoot == null || projectRoot.isBlank()) return;
        Path dir = Path.of(projectRoot).resolve(CONFIG_DIR);
        Files.createDirectories(dir);
        Path configPath = dir.resolve(CONFIG_FILE);
        Properties p = new Properties();
        p.setProperty(KEY_PROJECT_ROOT, projectRoot);
        p.setProperty(KEY_RUN_COMMAND, runCommand);
        p.setProperty(KEY_DEBUG_COMMAND, debugCommand);
        try (var out = Files.newOutputStream(configPath)) {
            p.store(out, "Yopta Run Configuration");
        }
    }
}
