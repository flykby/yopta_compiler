package ru.nstu.yopta;

import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Панель дерева файлов проекта. Кнопки добавления папки/файла; по двойному клику открывает файл.
 */
public class ProjectPanel extends VBox {

    private final TreeView<Path> treeView;
    private final Button addFolderBtn;
    private final Button addFileBtn;
    private Path projectRoot;
    private Consumer<Path> onFileOpen;

    public ProjectPanel() {
        treeView = new TreeView<>();
        treeView.setShowRoot(true);
        treeView.setCellFactory(tv -> {
            TreeCell<Path> cell = new TreeCell<>() {
                @Override
                protected void updateItem(Path path, boolean empty) {
                    super.updateItem(path, empty);
                    setText(empty || path == null ? null : path.getFileName().toString());
                }
            };
            return cell;
        });
        treeView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                TreeItem<Path> item = treeView.getSelectionModel().getSelectedItem();
                if (item != null && item.getValue() != null && Files.isRegularFile(item.getValue())) {
                    if (onFileOpen != null) {
                        onFileOpen.accept(item.getValue());
                    }
                }
            }
        });

        addFolderBtn = new Button(Messages.getString("project.addFolder"));
        addFolderBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(addFolderBtn, Priority.ALWAYS);
        addFolderBtn.setOnAction(e -> addNewFolder());
        addFolderBtn.setDisable(true);

        addFileBtn = new Button(Messages.getString("project.addFile"));
        addFileBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(addFileBtn, Priority.ALWAYS);
        addFileBtn.setOnAction(e -> addNewFile());
        addFileBtn.setDisable(true);

        HBox toolbar = new HBox(4);
        toolbar.getChildren().addAll(addFolderBtn, addFileBtn);
        toolbar.getStyleClass().add("project-toolbar");

        ScrollPane scroll = new ScrollPane(treeView);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(toolbar, scroll);
        setMinWidth(0);
        setPrefWidth(220);
        setMaxWidth(400);
    }

    public void setProjectRoot(Path root) {
        this.projectRoot = root;
        setMinWidth(180);
        addFolderBtn.setDisable(false);
        addFileBtn.setDisable(false);
        TreeItem<Path> rootItem = new TreeItem<>(root);
        rootItem.setExpanded(true);
        buildChildren(rootItem, root);
        treeView.setRoot(rootItem);
    }

    public void clear() {
        projectRoot = null;
        setMinWidth(0);
        addFolderBtn.setDisable(true);
        addFileBtn.setDisable(true);
        treeView.setRoot(null);
    }

    public boolean hasProject() {
        return projectRoot != null;
    }

    public Path getProjectRoot() {
        return projectRoot;
    }

    public void setOnFileOpen(Consumer<Path> handler) {
        this.onFileOpen = handler;
    }

    private void buildChildren(TreeItem<Path> parentItem, Path dir) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path p : stream) {
                if (Files.isHidden(p)) continue;
                TreeItem<Path> item = new TreeItem<>(p);
                parentItem.getChildren().add(item);
                if (Files.isDirectory(p)) {
                    buildChildren(item, p);
                    item.setExpanded(true);
                }
            }
            parentItem.getChildren().sort(Comparator.comparing(t -> t.getValue().getFileName().toString().toLowerCase()));
        } catch (IOException ignored) {
            // skip inaccessible dirs
        }
    }

    /** Родительская папка для нового элемента: выбранная папка, родитель файла или корень проекта. */
    private Path getParentPathForNew() {
        TreeItem<Path> sel = treeView.getSelectionModel().getSelectedItem();
        if (sel != null && sel.getValue() != null) {
            Path p = sel.getValue();
            if (Files.isDirectory(p)) return p;
            Path parent = p.getParent();
            if (parent != null && projectRoot != null && parent.startsWith(projectRoot)) return parent;
        }
        return projectRoot;
    }

    private void addNewFolder() {
        if (projectRoot == null) return;
        Path parent = getParentPathForNew();
        TextInputDialog d = new TextInputDialog();
        d.setTitle(Messages.getString("project.newFolderTitle"));
        d.setHeaderText(null);
        d.setContentText(Messages.getString("project.newFolderPrompt"));
        Optional<String> name = d.showAndWait();
        if (name.isEmpty() || name.get().isBlank()) return;
        String n = name.get().trim();
        Path newPath = parent.resolve(n);
        if (Files.exists(newPath)) {
            showError(Messages.getString("project.errorExists", n));
            return;
        }
        try {
            Files.createDirectories(newPath);
            addNodeToParent(parent, newPath);
        } catch (IOException e) {
            showError(Messages.getString("project.errorCreate", e.getMessage()));
        }
    }

    private void addNewFile() {
        if (projectRoot == null) return;
        Path parent = getParentPathForNew();
        TextInputDialog d = new TextInputDialog();
        d.setTitle(Messages.getString("project.newFileTitle"));
        d.setHeaderText(null);
        d.setContentText(Messages.getString("project.newFilePrompt"));
        Optional<String> name = d.showAndWait();
        if (name.isEmpty() || name.get().isBlank()) return;
        String n = name.get().trim();
        Path newPath = parent.resolve(n);
        if (Files.exists(newPath)) {
            showError(Messages.getString("project.errorExists", n));
            return;
        }
        try {
            Files.createFile(newPath);
            addNodeToParent(parent, newPath);
            if (Files.isRegularFile(newPath) && onFileOpen != null) {
                onFileOpen.accept(newPath);
            }
        } catch (IOException e) {
            showError(Messages.getString("project.errorCreate", e.getMessage()));
        }
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }

    private void addNodeToParent(Path parentPath, Path newPath) {
        TreeItem<Path> parentItem = findTreeItem(treeView.getRoot(), parentPath);
        if (parentItem == null) return;
        TreeItem<Path> newItem = new TreeItem<>(newPath);
        parentItem.getChildren().add(newItem);
        parentItem.getChildren().sort(Comparator.comparing(t -> t.getValue().getFileName().toString().toLowerCase()));
        parentItem.setExpanded(true);
    }

    private TreeItem<Path> findTreeItem(TreeItem<Path> node, Path path) {
        if (node == null || node.getValue() == null) return null;
        if (path.equals(node.getValue())) return node;
        for (TreeItem<Path> child : node.getChildren()) {
            TreeItem<Path> found = findTreeItem(child, path);
            if (found != null) return found;
        }
        return null;
    }
}
