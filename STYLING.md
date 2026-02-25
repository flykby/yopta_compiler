# Как поменять стили в Yopta

Стили задаются через **JavaFX CSS** в папке `src/main/resources/styles/`.

## Файлы стилей

| Файл | Назначение |
|------|------------|
| **app.css** | Всё приложение: меню, вкладки, кнопки, фон окна, разделители |
| **editor.css** | Редактор кода: шрифт, фон, цвет текста, номера строк (светлая тема) |
| **editor-dark.css** | Тёмная тема редактора (подключите вместо editor.css при необходимости) |

Подключение в коде: `Main.java`, метод `start()` — к сцене добавляются `app.css` и `editor.css`. Чтобы использовать тёмную тему редактора, замените `editor.css` на `editor-dark.css`.

## Как менять

1. **Шрифт и размер в редакторе**  
   В `editor.css` (или `editor-dark.css`) найдите `.code-area` и измените:
   ```css
   -fx-font-family: "JetBrains Mono", "Consolas", monospace;
   -fx-font-size: 14px;
   ```

2. **Цвета редактора**  
   В том же блоке:
   ```css
   -fx-background-color: #ffffff;   /* фон */
   -fx-text-fill: #24292e;         /* цвет текста */
   ```
   В `.code-area .paragraph-box` — фон и граница панели с номерами строк.

3. **Меню и вкладки**  
   В `app.css` правьте селекторы `.menu-bar`, `.menu-item`, `.tab-pane`, `.tab` и т.д.

4. **Своя тема**  
   Создайте новый файл, например `editor-monokai.css`, и подключите его в `Main.java` вместо `editor.css`:
   ```java
   String editorCss = getClass().getResource("/styles/editor-monokai.css").toExternalForm();
   ```

## Справка по JavaFX CSS

- [JavaFX CSS Reference](https://openjfx.io/javadoc/21/javafx.graphics/javafx/scene/doc-files/cssref.html)  
- Стили задаются свойствами с префиксом `-fx-`: `-fx-background-color`, `-fx-font-size`, `-fx-text-fill` и т.д.  
- Класс редактора RichTextFX: **`.code-area`**; номера строк — **`.paragraph-box`**, **`.paragraph-text`**.
