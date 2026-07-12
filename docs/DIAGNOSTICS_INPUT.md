# Diagnostics `input.txt` — справочник

Файловый режим диагностики позволяет «говорить» приложению фразы и подавать игровые события
из текстового файла, без микрофона и без запущенной игры. Это дев-инструмент для проверки
маршрутизации команд/запросов и реакции companion на события.

Источники истины (правьте примеры, если меняется код):
- `app/src/main/java/elite/intel/diagnostics/DiagnosticsInputTailer.java` — парсер строк.
- `app/src/main/java/elite/intel/diagnostics/DiagnosticsContext.java` — контексты `@status`/`@visible`.
- `app/src/main/java/elite/intel/diagnostics/DiagnosticsMode.java` — гейт режима и `language.txt`.
- `.claude/skills/elite-intel-diagnostic-run/SKILL.md` — автоматический прогон маршрутизации.

## Расположение файлов

Всё лежит в `%LOCALAPPDATA%\elite-intel\diagnostics\`
(у типичной машины: `C:\Users\<user>\AppData\Local\elite-intel\diagnostics\`):

| Файл | Роль |
|---|---|
| `input.txt` | **Гейт режима** + канал ввода. Само наличие файла при старте включает диагностику. Приложение только **читает** его и никогда не создаёт — жизненным циклом владеете вы. |
| `language.txt` | Язык загрузки (код: `RU`, `EN`, …). Читается при старте, до сборки companion/reducer. Это **данные**, не гейт. |
| `session.log` | Зеркало SYSTEM LOG + `DIAG`-маркеры хода. |

## Как включить режим (кратко)

1. Создать каталог и **пустой** `input.txt` (его наличие = гейт) — **до** запуска.
2. Записать код языка в `language.txt` (напр. `RU`).
3. Очистить `session.log`.
4. Запустить: `.\gradlew :app:run` (окно может появиться до ~1 мин).
5. Ждать в логе `DIAG ready` — единственный сигнал готовности.
6. Дописывать строки в `input.txt` по одной.
7. По завершении **удалить `input.txt`**, иначе следующий обычный запуск снова уйдёт в диагностику.

Пишите файлы как **UTF-8 без BOM**. `Set-Content -Encoding utf8` в PowerShell 5.1 добавляет BOM
и портит первую строку; для начальных записей используйте
`[System.IO.File]::WriteAllText($path, $text, (New-Object System.Text.UTF8Encoding($false)))`.
Дописывать строки можно `Add-Content -Encoding utf8` (BOM ведущей строки приложение снимает само).

## Виды строк

Приложение читает только **новые дописанные** строки, по одной за раз.

| Строка | Что делает |
|---|---|
| `обычный текст` | Произнесённая фраза → маршрутизируется как ввод с микрофона. Открывает ход companion. |
| `@visible <actionId>` | Ставит игру в первый контекст, где **команда/запрос** `<actionId>` видима роутеру (`isVisibleForLLM`). Только для команд/запросов. Мгновенно, без хода. |
| `@status <context>` | Ручной контекст: `main_ship`, `supercruise`, `docked`, `landed`, `srv`, `on_foot`. Выставляет флаги `Status`. Мгновенно, без хода. |
| `@fighter on` / `@fighter off` | Флаг «истребитель выпущен» (`on`/`true` = выпущен). |
| `@lang <CODE>` | Смена языка команд в рантайме. **Не** для задания языка прогона (reducer фиксирует язык при создании — используйте `language.txt`). |
| `{... "event": ... }` | JSON-строка с полем `"event"` → инжект журнального игрового события. |
| `# ...` | Комментарий, игнорируется. |
| пустая строка | Игнорируется. |

### `@`-директивы

Применяются мгновенно и **не создают ход** companion. Каждая пишет свою строку в лог:

- `@visible <id>` → `DIAG visible=<id> state=<ctx>`.
  `state=unknown-action` = неверный id; `main_ship(fallback)` = action нигде не виден.
- `@status <ctx>` → `DIAG status=<ctx>` или `DIAG status unknown=<ctx>`.
- `@fighter on|off` → `DIAG fighter=true|false`.
- `@lang <CODE>` → `DIAG lang=<CODE>` или `DIAG lang unknown=<CODE>`.

Контексты `@status` (флаги мирроринг `StatusFlags`):
`main_ship`, `supercruise`, `docked`, `landed`, `srv`, `on_foot`.

### События (JSON) — важные правила

- **Одна строка.** Парсер принимает событие, только если строка начинается с `{`, заканчивается `}`
  и содержит `"event"`. Переносов строк быть не должно.
- **Не указывайте `timestamp`.** `isReplay()` = «timestamp раньше старта приложения» → событие
  отбрасывается (`DIAG event skipped=<type>`). Если поля нет, tailer штампует свежий `Instant.now()`,
  и событие проходит.
- Событие публикуется в шину точно как из `JournalParser`, **без хода companion**: в лог идёт
  `DIAG event=<type>`, но `DIAG turn-done` для события не будет. Реакция (озвучка) идёт **асинхронно**,
  на виртуальном потоке — появится через мгновение. Не заливайте следующую строку сразу.
- **У субскрайберов бывают собственные гейты по состоянию.** Событие «видно всегда» и доходит до
  субскрайбера безусловно, но сам субскрайбер может решить не реагировать вне нужного состояния.
  Пример: `SAASignalsFoundSubscriber.announce()` озвучивает сигналы только при
  `isInMainShip() && !isLanded() && !isDocked()`. Поэтому перед таким событием ставьте
  `@status supercruise` (или `main_ship`) — иначе реакции не будет, хотя событие принято.

### Фразы

Обычная строка = произнесённая фраза. Открывает полный ход: в лог идёт `DIAG input="<фраза>"`,
затем `DIAG dispatch tool=<id>` (что распозналось), затем `DIAG turn-done` по завершении озвучки.
Язык уже задан через `language.txt`, `@lang` для этого не нужен.

## Маркеры в `session.log`

`<UTC-timestamp> <marker>`:

- `DIAG ready` — все сервисы подняты и LLM-эндпоинт доступен. **Единственный** сигнал готовности.
- `DIAG log opened` — свежий инстанс переоткрыл (очищенный) лог.
- `DIAG tailer watching input` — tailer запущен и читает `input.txt`.
- `DIAG input="<фраза>"` — открыт ход фразы.
- `DIAG dispatch tool=<id>` — action, который companion распознал в этом ходу.
- `DIAG turn-done` — ход фразы завершён (после `dispatch` и `speaking=false`).
- `DIAG speaking=true|false` — границы озвучки TTS.
- `DIAG event=<type>` / `DIAG event skipped=<type>` — событие принято / отброшено (replay/expired).
- `DIAG boot-language=<CODE>` — язык применён из `language.txt` при старте.
- `DIAG visible=<id> state=<ctx>` / `DIAG status=<ctx>` / `DIAG fighter=<bool>` / `DIAG lang=<CODE>` — применена директива.
- `LOG` / `DBG` / `AI` / `USER` — зеркалированный SYSTEM LOG.

## Примеры

Маршрутизация команды (контекст через `@visible`, затем фраза):
```
@visible enter_super_cruise
Уходим в суперкруиз
```

Ручной контекст:
```
@status supercruise
Сбрось скорость наполовину
```

Инжект события (одна строка, без `timestamp`) с нужным состоянием корабля:
```
@status supercruise
{"event":"SAASignalsFound","BodyName":"Phylurn IB-O b22-0 1 A Ring","SystemAddress":724374595777,"BodyID":12,"Signals":[{"Type":"Alexandrite","Count":1},{"Type":"Grandidierite","Count":5},{"Type":"LowTemperatureDiamond","Type_Localised":"Low Temp. Diamonds","Count":4},{"Type":"Opal","Type_Localised":"Void Opal","Count":2},{"Type":"Tritium","Count":4},{"Type":"Bromellite","Count":2}],"Genuses":[]}
```

Комментарий и пустые строки допустимы:
```
# сценарий: сканирование кольца
@status supercruise
{"event":"SAASignalsFound","BodyName":"Test 1 A Ring","SystemAddress":1,"BodyID":2,"Signals":[{"Type":"Alexandrite","Count":3}],"Genuses":[]}
```

Дописать строку из PowerShell (JSON содержит только двойные кавычки — оборачиваем в одинарные):
```powershell
Add-Content -Path "$env:LOCALAPPDATA\elite-intel\diagnostics\input.txt" -Encoding utf8 `
  -Value '{"event":"SAASignalsFound","BodyName":"Test 1 A Ring","SystemAddress":1,"BodyID":2,"Signals":[{"Type":"Alexandrite","Count":3}],"Genuses":[]}'
```

## Частые причины «нет реакции»

- **Событие в одну строку, но с `timestamp` в прошлом** → `DIAG event skipped`. Убрать `timestamp`.
- **Событие с переносами строк** → не распознаётся как JSON. Свернуть в одну строку.
- **Субскрайбер гейтит по `Status`** (напр. `SAASignalsFound` требует полёта на корабле) → поставить
  `@status supercruise`/`main_ship` перед событием.
- **Читаете старый лог** → очистить `session.log` перед запуском и ждать свежий `DIAG ready`.
- **Залили следующую строку сразу после события** → асинхронная озвучка наложилась; дать паузу.
