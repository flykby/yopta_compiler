# Yopta Compiler / IDE

Десктоп-приложение (редактор/IDE) на Java: JavaFX + RichTextFX, лексер JFlex.

## Требования

- **JDK 25** (сборка и запуск). Используется Gradle 9.3, поддерживающий Java 25.

## Сборка и запуск

```bash
./gradlew build
./gradlew run
```

## Зависимости

- **JavaFX 22** — UI
- **RichTextFX 0.11.7** — редактор кода с номерами строк
- **JFlex 1.9.0** — генерация лексера (исходники в `src/main/jflex/*.flex`)

- Подробнее про оформление: **[STYLING.md](STYLING.md)** — как менять темы и стили (шрифты, цвета, редактор).

## Задачи Gradle

- `./gradlew run` — запуск приложения
- `./gradlew build` — сборка (включает генерацию JFlex при наличии `.flex` файлов)
- `./gradlew generateJflex` — только генерация лексера из `src/main/jflex`
