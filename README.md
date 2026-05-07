## Лабораторная работа 7. Анализ и преобразование кода с использованием Clang и LLVM

### 1) Автор

- Выполнил: **Зохно Даниил Русланович**
- Группа: *(укажите вашу группу)*

## Постановка задачи

### Общее задание
Познакомиться с инструментарием Clang и LLVM, освоить получение абстрактного синтаксического дерева (AST) и промежуточного представления (LLVM IR) для кода на C/C++, научиться применять базовые оптимизации, строить графы потока управления (CFG), а также анализировать влияние оптимизаций на различные синтаксические конструкции языка.

### Описание индивидуального задания

Вариант — **передача структуры по значению**. Дан фрагмент кода с объявлением `struct Point` и функцией `sum`, принимающей структуру по значению:

1. Получить AST и LLVM IR.
2. Применить `-O2` и описать, изменилась ли передача структуры (по значению / по ссылке) на уровне IR и на уровне ABI.
3. Построить CFG для функций `sum` и `main`.
4. Исследовать, что происходит, если добавить `__attribute__((always_inline))` к `sum`.
5. **Вывод:** как LLVM оптимизирует передачу структур.

## Общее задание

### Установка среды

На macOS (Apple Silicon) установка LLVM-инструментов и Graphviz:

```bash
brew install llvm graphviz
echo 'export PATH="$(brew --prefix llvm)/bin:$PATH"' >> ~/.zshrc   # один раз
```

В Ubuntu:

```bash
sudo apt update && sudo apt install -y clang llvm graphviz
```

Проверка:

```bash
clang --version
opt   --version
dot -V
```

### Исходный код

Файл [`src/point_struct.c`](src/point_struct.c):

```c
#include <stdio.h>

struct Point {
    int x;
    int y;
};

int sum(struct Point p) {
    return p.x + p.y;
}

int main(void) {
    struct Point p = {2, 3};
    int result = sum(p);
    printf("%d\n", result);
    return 0;
}
```

Скрипты-обёртки лежат в [`scripts/`](scripts) и кладут результаты в [`out/`](out):

```bash
chmod +x scripts/*.sh
./scripts/gen_artifacts.sh                                  # AST + IR (-O0/-O2)
./scripts/gen_artifacts.sh src/point_struct_always_inline.c # вариант для п. 4
./scripts/gen_cfg.sh out/point_struct_O0.ll                 # CFG в out/cfg_point_struct_O0/
./scripts/gen_cfg.sh out/point_struct_O2.ll                 # CFG в out/cfg_point_struct_O2/
```

### Работа с AST

Для получения дерева используется флаг `-ast-dump`:

```bash
clang -Xclang -ast-dump -fsyntax-only -fno-color-diagnostics src/point_struct.c
```

Релевантный фрагмент (определение `Point`, `sum`, `main` в `out/point_struct_ast.txt`):

```text
RecordDecl ... struct Point definition
|-FieldDecl ... x 'int'
`-FieldDecl ... y 'int'

FunctionDecl ... sum 'int (struct Point)'
|-ParmVarDecl ... p 'struct Point'
`-CompoundStmt
  `-ReturnStmt
    `-BinaryOperator '+'
      |-MemberExpr .x
      | `-DeclRefExpr 'p'
      `-MemberExpr .y
        `-DeclRefExpr 'p'

FunctionDecl ... main 'int ()'
`-CompoundStmt
  |-DeclStmt
  | `-VarDecl p 'struct Point' cinit
  |   `-InitListExpr ... { 2, 3 }
  |-DeclStmt
  | `-VarDecl result 'int' cinit
  |   `-CallExpr ... sum
  |     `-ImplicitCastExpr LValueToRValue
  |       `-DeclRefExpr 'p'
  |-CallExpr ... printf
  | |-...
  | `-ImplicitCastExpr LValueToRValue
  |   `-DeclRefExpr 'result'
  `-ReturnStmt
    `-IntegerLiteral 0
```

Видно, что C-структура отображается как `RecordDecl` с двумя `FieldDecl`, передача `p` в `sum` — обычное чтение `LValueToRValue` (фактически — копия по значению).

> Скриншот вывода `-ast-dump` целиком: `docs/lab7/ast.png` (см. ниже).

### Генерация LLVM IR

```bash
clang -S -emit-llvm src/point_struct.c -o out/point_struct.ll
```

В `out/point_struct.ll` (по умолчанию это `-O0` без `-O0`-обёрток инлайнинга) — те же три блока, что и в `point_struct_O0.ll`, см. ниже.

### Оптимизация IR

```bash
clang -S -emit-llvm -O0 src/point_struct.c -o out/point_struct_O0.ll
clang -S -emit-llvm -O2 src/point_struct.c -o out/point_struct_O2.ll
```

**В режиме `-O0`** IR сохраняет прямое соответствие исходному коду: используются `alloca`/`load`/`store`, в `main` — `memcpy` начальных значений в локальную копию (`out/point_struct_O0.ll`):

```llvm
%struct.Point = type { i32, i32 }
@__const.main.p = private unnamed_addr constant %struct.Point { i32 2, i32 3 }, align 4

define i32 @sum(i64 %0) #0 {
  %2 = alloca %struct.Point, align 4
  store i64 %0, ptr %2, align 4
  %3 = getelementptr inbounds %struct.Point, ptr %2, i32 0, i32 0
  %4 = load i32, ptr %3, align 4
  %5 = getelementptr inbounds %struct.Point, ptr %2, i32 0, i32 1
  %6 = load i32, ptr %5, align 4
  %7 = add nsw i32 %4, %6
  ret i32 %7
}

define i32 @main() #0 {
  %1 = alloca i32, align 4
  %2 = alloca %struct.Point, align 4
  %3 = alloca i32, align 4
  store i32 0, ptr %1, align 4
  call void @llvm.memcpy.p0.p0.i64(ptr align 4 %2, ptr align 4 @__const.main.p, i64 8, i1 false)
  %4 = load i64, ptr %2, align 4
  %5 = call i32 @sum(i64 %4)
  store i32 %5, ptr %3, align 4
  %6 = load i32, ptr %3, align 4
  %7 = call i32 (ptr, ...) @printf(ptr noundef @.str, i32 noundef %6)
  ret i32 0
}
```

**В режиме `-O2`** IR преобразован в SSA-форму, лишние обращения к памяти удалены, выражение `2 + 3` свёрнуто в константу `5`, вызов `sum` устранён вместе с инструкциями работы со структурой (`out/point_struct_O2.ll`):

```llvm
define i32 @sum(i64 %0) local_unnamed_addr #0 {
  %2 = trunc i64 %0 to i32
  %3 = lshr i64 %0, 32
  %4 = trunc nuw i64 %3 to i32
  %5 = add nsw i32 %4, %2
  ret i32 %5
}

define noundef i32 @main() local_unnamed_addr #1 {
  %1 = tail call i32 (ptr, ...) @printf(ptr noundef nonnull dereferenceable(1) @.str, i32 noundef 5)
  ret i32 0
}
```

### Построение CFG

```bash
opt -passes="dot-cfg" out/point_struct_O0.ll -disable-output    # создаст .main.dot, .sum.dot
mv .main.dot cfg_main_O0.dot
mv .sum.dot  cfg_sum_O0.dot

opt -passes="dot-cfg" out/point_struct_O2.ll -disable-output
mv .main.dot cfg_main_O2.dot
mv .sum.dot  cfg_sum_O2.dot

dot -Tpng cfg_main_O0.dot -o cfg_main_O0.png
dot -Tpng cfg_sum_O0.dot  -o cfg_sum_O0.png
dot -Tpng cfg_main_O2.dot -o cfg_main_O2.png
dot -Tpng cfg_sum_O2.dot  -o cfg_sum_O2.png
```

Тот же эффект даёт скрипт `scripts/gen_cfg.sh` (раскладывает `.dot` и `.png` в `out/cfg_*`).

В обеих функциях один базовый блок `entry` без ветвлений; **топология CFG** при `-O2` не меняется (нет циклов/`if`), но **наполнение** блоков сильно сокращается: в `main` остаётся только `tail call printf` и `ret`.

> Скриншоты CFG: `docs/lab7/cfg_sum_O0.png`, `docs/lab7/cfg_main_O0.png`, `docs/lab7/cfg_main_O2.png` (приложите PNG из `out/cfg_*` сюда).

## Индивидуальное задание

### 1. AST и LLVM IR

См. предыдущий раздел: `out/point_struct_ast.txt`, `out/point_struct_O0.ll`, `out/point_struct_O2.ll`.

### 2. Применение `-O2`: изменилась ли передача структуры?

**На уровне исходного C** — формально по-прежнему «по значению»: `int sum(struct Point p)` копирует структуру в параметр.

**На уровне LLVM IR / ABI** — на цели `arm64-apple-macosx` (datalayout `e-m:o-i64:64-i128:128-…`) маленькая структура (8 байт = `{ i32, i32 }`) **не передаётся через `byval`-указатель**, а упаковывается в один скалярный аргумент `i64`. Это видно уже в `-O0`:

```llvm
define i32 @sum(i64 %0) #0 { ... store i64 %0, ptr %2 ... }
```

То есть фактически структура едет в **регистре**, и ABI ARM64 этот случай покрывает scalar-параметрами AAPCS64 (HVA здесь не применима — там целые i32/i64).

В `-O2`:

- В `sum` распаковка структуры из `i64` сделана арифметикой над регистром (`trunc` для младшей половины, `lshr 32 + trunc` для старшей), без `alloca`/`load`/`store`.
- В `main` копия `p`, `memcpy` константного инициализатора, чтение полей и сам **вызов** `sum` исчезли: компилятор выполнил `2 + 3 = 5` на этапе компиляции и заменил всю цепочку на `tail call printf("%d\n", 5)`.

**Итог п. 2:** при `-O2` передача структуры на уровне IR упрощается до операций над скалярным регистром (`i64`), а на уровне `main` вообще исчезает — цепочка свёрнута до константы.

### 3. CFG для `sum` и `main`

Команды и сами картинки описаны в разделе «Построение CFG». На обеих оптимизациях у нас по одному базовому блоку `entry` в каждой функции — ветвлений нет, но содержимое блоков отличается: при `-O0` в `entry` десятки инструкций со стеком, при `-O2` — буквально 5 инструкций в `sum` и 2 инструкции в `main`.

### 4. Что меняет `__attribute__((always_inline))` у `sum`?

Файл [`src/point_struct_always_inline.c`](src/point_struct_always_inline.c):

```c
__attribute__((always_inline)) static inline int sum(struct Point p) {
    return p.x + p.y;
}
```

`-O0`-IR (`out/point_struct_always_inline_O0.ll`) уже **не содержит отдельной функции `@sum`**: тело `sum` встроено внутрь `main`, остался только один `define i32 @main()` со всеми `alloca`/`load`/`add`/`printf`:

```llvm
define i32 @main() #0 {
  %1 = alloca %struct.Point, align 4
  ...
  call void @llvm.memcpy.p0.p0.i64(ptr align 4 %3, ptr align 4 @__const.main.p, i64 8, i1 false)
  %5 = load i64, ptr %3, align 4
  store i64 %5, ptr %1, align 4
  %6 = load i32, ptr %1, align 4
  %7 = getelementptr inbounds %struct.Point, ptr %1, i32 0, i32 1
  %8 = load i32, ptr %7, align 4
  %9 = add nsw i32 %6, %8
  store i32 %9, ptr %4, align 4
  %10 = load i32, ptr %4, align 4
  %11 = call i32 (ptr, ...) @printf(ptr noundef @.str, i32 noundef %10)
  ret i32 0
}
```

В `-O2` (`out/point_struct_always_inline_O2.ll`) результат идентичен обычному `-O2` без атрибута — всё свёрнуто до константы `5`:

```llvm
define noundef i32 @main() local_unnamed_addr #0 {
  %1 = tail call i32 (ptr, ...) @printf(ptr ... @.str, i32 noundef 5)
  ret i32 0
}
```

**Вывод по п. 4:** `always_inline` форсирует **встраивание уже на `-O0`** (когда обычный inliner отключён), что и подтверждается отсутствием `@sum` в `O0`-IR. На `-O2` стандартный инлайнер всё равно встраивает короткую функцию, поэтому атрибут не меняет финальный результат.

### 5. Вывод: как LLVM оптимизирует передачу структур?

1. **На уровне ABI** маленькие структуры (≤ 16 байт на ARM64 / SysV-x86_64) уже на `-O0` упаковываются в один-два скалярных регистровых параметра (в нашем IR — `i64`), без `byval`-указателя. Это означает, что «передача по значению» в IR ≠ копированию через указатель.
2. **`mem2reg` / SROA** при `-O1+` устраняют локальные `alloca` под структуру: поля становятся независимыми SSA-значениями, исчезают пары `store`/`load`.
3. **GVN, InstCombine, ConstProp/SCCP** свёртывают арифметику над константными инициализаторами полей: `Point{2,3}.x + .y → 5`.
4. **Inliner / `always_inline`** убирает сам вызов `sum`, открывая дорогу для сворачивания всей цепочки в одну инструкцию `tail call printf("%d\n", 5)`.
5. **Топология CFG** в простых функциях не меняется (один базовый блок до и после), меняется только содержимое; вся «оптимизация передачи структур» — это локальные преобразования внутри блоков плюс interprocedural-инлайн.

## Ответы на контрольные вопросы

**1. Что такое Clang, и какова его роль в процессе компиляции программ?**

Clang — фронтенд LLVM для C/C++/Objective-C. Он выполняет лексический и синтаксический анализ, строит AST, проверяет типы и порождает LLVM IR. Дальнейшие проходы оптимизации и кодогенерации выполняет уже LLVM.

**2. Что представляет собой LLVM и как он используется в современных компиляторах?**

LLVM — инфраструктура компиляторов: набор библиотек, инструментов (`opt`, `llc`, `lld` и т. д.) и единое промежуточное представление LLVM IR. На LLVM построены Clang, Swift, Rust, Julia и многие другие; разные фронтенды генерируют один и тот же IR, оптимизации и кодогенерация — общие для всех.

**3. Чем отличается AST от LLVM IR?**

AST близко к синтаксису C/C++: вершины — это объявления, выражения, операторы, в нём сохранены типы и сем. информация языка. LLVM IR — это **низкоуровневый трёхадресный код в SSA**, с явной памятью (`alloca`/`load`/`store`), типами LLVM (`i32`, `ptr`, `%struct.Point`) и арифметическими инструкциями (`add nsw`, `mul`, …). AST — для семантики языка, IR — для оптимизаций и генерации машинного кода.

**4. Для чего необходимо промежуточное представление (IR)?**

IR делает оптимизации **независимыми от языка и от целевой архитектуры**. Один и тот же набор проходов работает для C, C++, Rust и Swift; одни и те же IR-модули кладутся в разные кодогенераторы (x86-64, ARM64, RISC-V).

**5. Что делает инструкция `alloca` в LLVM IR?**

`alloca` резервирует место в стеке текущей функции и возвращает указатель на эту память. В IR на `-O0` каждая локальная C-переменная и каждая по-значению-структура получают свой `alloca`. На `-O1+` проход `mem2reg` (вместе с SROA) превращает эти `alloca` в чистые SSA-значения — за исключением случаев, когда от переменной берут адрес.

**6. Зачем нужна оптимизация кода в компиляторе?**

Чтобы скомпилированный код быстрее работал и/или меньше весил: устраняется «мусор» вроде лишних `load`/`store`, инлайнятся короткие функции, свёртываются константные выражения, разворачиваются простые циклы, удаляются недостижимые ветви. В нашем примере `-O2` буквально превратил вызов `sum(p)` с инициализацией структуры в один `printf("%d\n", 5)`.

**7. Что такое SSA-форма и почему она важна для оптимизаций?**

SSA (Static Single Assignment) — каждая «переменная» IR определяется ровно один раз, для слияния значений из разных путей CFG используются `phi`-узлы. Это делает анализ потока данных тривиальным: у каждого значения один точный источник, что упрощает constant propagation, GVN, dead code elimination и т. д. В `-O2`-IR `sum` это видно: `%2`, `%3`, `%4`, `%5` — каждая SSA-переменная определяется один раз.

**8. Что такое граф потока управления (CFG)?**

CFG — ориентированный граф, в котором вершины — базовые блоки (последовательности инструкций без внутренних ветвлений), а рёбра — возможные переходы между ними (`br`, `switch`, `ret`). По CFG считаются достижимость, доминирование, циклы, живость значений; на нём же работают inline-эвристики и алгоритмы регистрового распределения.

**9. Как устроены арифметические операции в LLVM IR?**

Это бинарные SSA-инструкции с явным типом: `add`, `sub`, `mul`, `sdiv`/`udiv`, `srem`/`urem` для целых; `fadd`, `fsub`, `fmul`, `fdiv` для плавающей точки. Флаги вроде `nsw`/`nuw`/`fast` уточняют семантику переполнения и FP-вычислений. В нашем IR `%7 = add nsw i32 %4, %6` — это вычисление `p.x + p.y` со знаковой арифметикой.

**10. Почему функции — отдельные единицы анализа и оптимизации?**

У функции явные границы: список параметров с типами и атрибутами, локальная память (`alloca`), точки выхода (`ret`). Большинство оптимизаций (mem2reg, SROA, GVN, LICM, Inliner-эвристики) формулируются как преобразования над одной функцией; межпроцедурные проходы (Inliner, IPSCCP, GlobalOpt) надстраиваются сверху, а не вместо них.

**11. Что происходит с короткой функцией, вызываемой один раз?**

Inliner с большой вероятностью встроит её в место вызова и удалит само определение. Когда таких функций ставится `__attribute__((always_inline))`, инлайн происходит даже на `-O0`. После инлайна открываются дополнительные оптимизации: `mem2reg`, constant folding, dead store elimination — что и видно в нашем `always_inline`-IR на `-O0` и в обычном `-O2`-IR (вызов `sum` исчез).

**12. Какие преимущества IR/CFG для автоматических оптимизаций?**

IR имеет однородную структуру, явные типы и SSA, что делает анализ потока данных дешёвым. CFG даёт явную модель управления — без неё любые рассуждения о достижимости и живости приходилось бы реконструировать каждый раз из синтаксиса. Анализ исходного C затруднён препроцессором, неявными преобразованиями типов, сложными выражениями и зависимостью от окружения; в IR/CFG всё это уже разложено на простые SSA-инструкции и базовые блоки.

---

### Артефакты в репозитории

| Файл | Что внутри |
|------|------------|
| `out/point_struct_ast.txt` | AST из `clang -ast-dump` |
| `out/point_struct_O0.ll` | LLVM IR без оптимизаций |
| `out/point_struct_O2.ll` | LLVM IR с `-O2` |
| `out/point_struct_always_inline_O0.ll` | IR `-O0` для варианта с `always_inline` |
| `out/point_struct_always_inline_O2.ll` | IR `-O2` для того же варианта |
| `out/cfg_point_struct_O0/*.dot/.png` | CFG для `-O0` (после `gen_cfg.sh`) |
| `out/cfg_point_struct_O2/*.dot/.png` | CFG для `-O2` |

Каталог `out/` целиком в `.gitignore` (кроме `.gitkeep`); скриншоты для README кладите в `docs/lab7/` (создайте при необходимости).
