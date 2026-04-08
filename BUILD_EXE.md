# Сборка exe под Windows

Инструкция выполняется **на компьютере с Windows** (или в виртуальной машине). Нужны: JDK 25, скопированный проект с Mac.

---

## 1. Открыть терминал в папке проекта

Перейди в каталог проекта (где лежат `build.gradle` и `gradlew.bat`), затем открой **PowerShell** или **cmd** в этой папке.

---

## 2. Собрать проект и дистрибутив

В PowerShell:

```powershell
.\gradlew.bat build
.\gradlew.bat installDist
```

В cmd:

```cmd
gradlew.bat build
gradlew.bat installDist
```

После этого в папке **`build\install\yopta-compiler\lib\`** (имя с **дефисом**: yopta-compiler) появятся все JAR-файлы.

---

## 3. Узнать имя главного JAR (если нужно)

Выполни:

```powershell
dir build\install\yopta-compiler\lib\*.jar
```

Главный JAR называется **`yopta-compiler-0.1.0-SNAPSHOT.jar`**. Дальше в команде jpackage используй именно это имя.

---

## 4. Иконка exe (логотип) — опционально

Для Windows jpackage принимает только формат **.ico**. Нужно получить `logo.ico` из `logo.png`:

- **Онлайн:** загрузи `logo.png` на [convertio.co/png-ico](https://convertio.co/png-ico/) или аналог, скачай `logo.ico`.
- **ImageMagick** (если установлен):  
  `magick logo.png -define icon:auto-resize=256,128,64,48,32,16 logo.ico`

Положи **`logo.ico`** в корень проекта (рядом с `build.gradle`). Тогда в шаге 5 добавь к команде jpackage параметр: **`--icon logo.ico`**.

---

## 5. Создать папку с exe (app-image, без WiX)

Тип **`exe`** в jpackage требует установленный [WiX Toolset](https://wixtoolset.org/). Без WiX используй **`app-image`** — получится папка с готовым **Yopta Code.exe**, без установщика.

Параметр **`--win-console`** при сборке открывает окно консоли при запуске exe — так можно увидеть текст ошибки, если приложение не стартует. Для финальной сборки его можно убрать.

**Обязательно:** добавь **`--module-path`** и **`--add-modules`**, иначе будет ошибка «JavaFX runtime components are missing». В командах ниже они уже указаны.

**PowerShell (с иконкой и консолью для отладки):**

```powershell
& "$env:JAVA_HOME\bin\jpackage.exe" --input build\install\yopta-compiler\lib --main-jar yopta-compiler-0.1.0-SNAPSHOT.jar --main-class ru.nstu.yopta.Main --name "Yopta Code" --type app-image --dest build\dist --win-console --module-path build\install\yopta-compiler\lib --add-modules javafx.controls,javafx.fxml,javafx.graphics --icon logo.ico
```

**PowerShell (без иконки, с консолью):**

```powershell
& "$env:JAVA_HOME\bin\jpackage.exe" --input build\install\yopta-compiler\lib --main-jar yopta-compiler-0.1.0-SNAPSHOT.jar --main-class ru.nstu.yopta.Main --name "Yopta Code" --type app-image --dest build\dist --win-console --module-path build\install\yopta-compiler\lib --add-modules javafx.controls,javafx.fxml,javafx.graphics
```

**cmd (с иконкой и консолью):**

```cmd
"%JAVA_HOME%\bin\jpackage.exe" --input build\install\yopta-compiler\lib --main-jar yopta-compiler-0.1.0-SNAPSHOT.jar --main-class ru.nstu.yopta.Main --name "Yopta Code" --type app-image --dest build\dist --win-console --module-path build\install\yopta-compiler\lib --add-modules javafx.controls,javafx.fxml,javafx.graphics --icon logo.ico
```

---

## 6. Где лежит exe

- Папка: **`build\dist\Yopta Code\`**
- Внутри — файл **`Yopta Code.exe`**. Запускай его: установка не нужна, можно скопировать всю папку на другой компьютер с Windows.

---

## Краткая последовательность (копируй по порядку)

Сначала сделай **logo.ico** из **logo.png** (см. шаг 4) и положи в корень проекта — тогда exe будет с твоим логотипом.

```powershell
.\gradlew.bat build
.\gradlew.bat installDist
& "$env:JAVA_HOME\bin\jpackage.exe" --input build\install\yopta-compiler\lib --main-jar yopta-compiler-0.1.0-SNAPSHOT.jar --main-class ru.nstu.yopta.Main --name "Yopta Code" --type app-image --dest build\dist --win-console --module-path build\install\yopta-compiler\lib --add-modules javafx.controls,javafx.fxml,javafx.graphics --icon logo.ico
```

Если иконки нет — убери `--icon logo.ico`. Когда приложение будет запускаться нормально, можно убрать `--win-console`, чтобы не показывать консоль. Параметры **`--module-path`** и **`--add-modules`** нужны для JavaFX, их не убирай.

Готовый exe: **`build\dist\Yopta Code\Yopta Code.exe`**

---

## Установщик exe (опционально)

Если нужен именно **установщик** (один .exe-файл для установки), установи [WiX Toolset](https://wixtoolset.org/) и добавь его в PATH. Затем в третьей команде замени `--type app-image` на `--type exe` — в `build\dist` появится установщик.

---

## Exe не открывается

1. **Запусти exe из командной строки** — открой cmd, перейди в папку с exe и запусти:
   ```cmd
   cd "build\dist\Yopta Code"
   "Yopta Code.exe"
   ```
   Иногда в консоли появляется текст ошибки.

2. **Пересобери с консолью** — добавь в команду jpackage параметр **`--win-console`** (он уже есть в командах выше). После сборки при запуске exe откроется чёрное окно консоли — в нём будет виден вывод и ошибка (например «Failed to launch JVM», «NoClassDefFoundError», исключение JavaFX).

3. **Проверь, что запускаешь из полной папки** — нужна вся папка **`Yopta Code`** (в ней exe, runtime, lib и т.д.). Один только `Yopta Code.exe` без остальных файлов рядом не заработает.

4. **Антивирус** — временно отключи или добавь папку в исключения: иногда он блокирует или удаляет exe от jpackage.

5. **Сначала проверь, что приложение вообще запускается** — в папке проекта выполни `.\gradlew.bat run`. Если и так не открывается окно или падает с ошибкой, проблема в коде или окружении, а не в jpackage.

---

## Если что-то не так

- **«JAVA_HOME не найден»** — задай переменную окружения `JAVA_HOME` (путь к папке JDK 25, например `C:\Java\jdk-25`).
- **«jpackage не найден»** — используй полный путь: `C:\Java\jdk-25\bin\jpackage.exe` (подставь свой путь к JDK).
- **«Cannot find path ... yopta_compiler»** — в проекте Gradle использует имя **yopta-compiler** (с дефисом), путь должен быть `build\install\yopta-compiler\lib`.
- **Ошибка при сборке** — убедись, что в папке проекта есть `gradlew.bat`, `build.gradle` и папка `src`.
- **Ошибка про `logo.ico`** — не указывай `--icon logo.ico`, пока не создашь файл `logo.ico` в корне проекта (см. шаг 4).
- **«JavaFX runtime components are missing»** — в команду jpackage обязательно добавь: `--module-path build\install\yopta-compiler\lib --add-modules javafx.controls,javafx.fxml,javafx.graphics` (все команды в инструкции уже содержат эти параметры).
