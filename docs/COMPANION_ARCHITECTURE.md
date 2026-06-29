# COMPANION_ARCHITECTURE.md

Архитектура режима **«младший член экипажа» (компаньон)** для проекта **EliteIntel**.

**Компонентная карта** режима: концепция, решения, компоненты, потоки, границы ответственности и lifecycle-правила между ними.

Версия **v0.17**.

> **Статус.** Рабочая версия в разработке. Приоритет — за текущей проработкой; этот файл её догоняет, не наоборот.
> «Решение» = текущая согласованная картина, не застывший стандарт.

> **v0.17 (2026-06-26).** Добавлен **рефлекс** — детерминированный fast-path для прямых команд, минующий ЛЛМ.
> - **`ReflexResolver`** (пакет `companion.prompt`) — гейт перед рождением мысли: даёт `id` команды, только если ввод **дословно** совпал с тренировочной фразой И нашлась **ровно одна** команда, **без параметров**, **видимая** сейчас и **не опасная**. Всё прочее (неоднозначное, параметризованное, опасное, не-дословное, не команда) → пусто и идёт обычным путём. Переиспользует `GameToolCandidates` (видимые команды + фразы + параметры), `AiActionLocalizations.splitPhraseGroup`, `DangerousActionPolicy` — без второй классификации.
> - **`ReflexThought`** (source `COMMANDER`, командирский lane) — короткий ход без ЛЛМ/промпта: записать реплику `[COMMANDER]` → выполнить команду → `recordOutcome` (та же per-type озвучка/память, что у `CommanderThought`). Без прерываний (стартовавшая команда не отменяется, §1.9.41).
> - **`ThoughtDispatcher.submitCommanderInput`** прогоняет `ReflexResolver`: совпало → `ReflexThought`, иначе → `CommanderThought`. Резолвер — коллаборатор диспетчера (как `UrgencyPolicy`), не поле `ThoughtContext`.
> - **`recordOutcome`/`voice`/`description`/`rememberAction` подняты из `CommanderThought` в базу `Thought`** — общий владелец озвучки/памяти исхода для обоих исполнителей командирского lane. *(§1.2, §2.3, §2.4, §2.5, §5.1)*

> **v0.16 (2026-06-26).** Решена главная проблема: долгая синхронная команда/запрос больше не блокирует командирский поток.
> - **Командирский lane — bounded-пул** (`ThoughtLane(name, concurrency)`): до `MAX_LIVE_COMMANDER_THOUGHTS` = 5 командирских мыслей живут одновременно, остальное в очереди; `EVENT`/`NARRATION` — по одному воркеру. Долгая команда занимает воркер, новые командирские мысли идут на свободные. Работает потому, что медленная часть — **хендлер** (на пуле `ExecutionGateway`), а не ЛЛМ-раунд: пока мысль ждёт хендлер, ЛЛМ свободен. Мысль остаётся **целой** (сама владеет итогом через `recordOutcome`), цепочка ЛЛМ сохранена — без отцепа/outcome-мыслей.
> - **Interrupt по множеству живых:** barge-in/urgent прерывают **всех** живых в lane; watchdog — поштучно тех, кто висит дольше таймаута.
> - **Потокобезопасность:** `MemoryGateway` `synchronized`, `CompanionState` `volatile` (конкурентный `classify_turn` = last-write-wins, принято). *(§1.2, §1.7, §2.3)*

> **v0.15 (2026-06-26).** Доработка модели речи/памяти командирского хода (отменяет п.4 v0.14 про async):
> 1. **Откат fire-and-forget.** Command/query снова исполняются **синхронно** (мысль ждёт хендлер; результат идёт во flow, чтобы ЛЛМ мог цепочить). Долгая команда держит lane — это принятый baseline; «итог долгой команды → narration-канал вместо удержания мысли» отложено как cause-level правка. *(§1.9, §5.1)*
> 2. **Озвучка и память — по типу действия** (`CommanderThought.recordOutcome`; единый владелец классификации — `IntelActionTypeResolver` → `COMMAND/QUERY/MACRO/SYSTEM/UNKNOWN`, пакет `companion.tools`): COMMAND — текст хендлера (crit→urgent) либо ack `affirmative()` для side-effect; QUERY — ответ; MACRO — молча (озвучивает свои шаги сам); SYSTEM/UNKNOWN — речь не трогаем. `speak` подавляется на `COMMAND|QUERY|MACRO`. *(§2.14, §5.1)*
> 3. **Компактная память.** В timeline: «command/macro `id` executed» + текст/описание; ответ запроса — `[COMPANION]`; сырой `{data:…}` в память **не идёт** (остаётся только во flow); системные функции timeline не пишут. Тот же `recordOutcome` и на подтверждённом dangerous-наборе. *(§1.10, §5.1)*
> 4. **Удалён `silentInCompanion()`** (рудимент): тишину side-effect-команды определяет пустой `text_to_speech_response`, классификацию — `IntelActionTypeResolver`.

> **v0.14 (2026-06-26).** Реализован переход к одному классу мысли на источник и завершён curated-narration
> proposal; проза §0–§5 приведена в соответствие. Сводка изменений:
> 1. **Четыре вида мысли, у каждого свой `run()`.** `Thought` стал тонкой общей базой (промпт/LLM-раунд/
>    исполнение/память/interrupt) и **не владеет циклом мышления**. `CommanderThought` — полный
>    tool-calling-цикл с подтверждением опасного; `EventThought` — memory-only (ЛЛМ не зовётся);
>    `NarrationThought` — один короткий ЛЛМ-раунд; `VerbatimNarrationThought` — дословная озвучка без ЛЛМ.
>    *(затрагивает §1.2, §2.3, §2.4, §5)*
> 2. **EVENT — чистый «knowing»-канал, без речи.** `importance()` стал фильтром релевантности для памяти:
>    `HIGH` → запись в память, `NORMAL` → отбрасывается, `LOW` → отсекает `GameEventFilter`. HIGH-события,
>    у которых есть курируемая наррация, понижены до `NORMAL` (нет дубля сырого `[EVENT]` рядом с
>    `[COMPANION]`); HIGH остаётся только у событий без наррации (напр. `MissionFailed`). *(§2.2.1)*
> 3. **`NARRATION` — самостоятельный `ThoughtSource`.** Свой профиль кэша `NARRATION`, свой лаконичный
>    промпт (без topic enum / memory / safety), нулевой набор game-tools. EVENT промпт не строит. *(§1.4, §2.10)*
> 4. **Память слов компаньона — источник `COMPANION`.** Произнесённая фраза пишется как `[COMPANION]` (сам
>    текст, не `{status:spoken}`-ack). *(§1.10, §3.2)*
> 5. **Три lane по источнику.** `ThoughtDispatcher` держит lane на каждый `ThoughtSource` (commander/event/
>    narration) в карте; максимум один live на источник; медленная narration не блокирует запись событий.
>    *(§1.2, §2.3)*
> 6. **Curated narration заведена в компаньон.** `SensorDataEvent` → `NarrationThought` (ЛЛМ фразирует);
>    announcement-события (mining/discovery/route/radar/navigation) → `VerbatimNarrationThought` (дословно)
>    через `CompanionAnnouncementBridge`; в companion-режиме legacy `VocalisationRouter` для них молчит,
>    тумблеры остаются авторитетными; radio — только legacy, без памяти. *(§2.2, §4.2)*
> Удалены `EventInputKind`, `EventSpeechPolicy` (их роль несёт тип класса / отсутствие речи у EVENT).

> **v0.13.** Версия после сценарного прогона и grill-review. Главная правка — честно разделены hard architectural boundaries, trusted developer contracts и accepted operator/gameplay risks.

> **⚠ Частично устарело (2026-06-25).** По итогам тестирования исходной реализации вокализация и
> execution-модель пересмотрены; перечисленные ниже разделы ещё не догнали код:
> 1. **LLM не решает, что говорить.** Слой `gameapi.journal.subscribers` (8 месяцев тюнинга) и
     >    command/query-хендлеры владеют тем, *что* и *когда* озвучивается. LLM — только фразировка/диспетчер,
     >    не источник игровых фактов. *(затрагивает §0, §1.4, §2.14, §4.2)*
> 2. **Убрана LLM-вокализация на старте обработки команды.** *(§5.1)*
> 3. **Детерминированная вокализация command/query.** Точная фраза приходит из хендлера, а не из выбора
     >    LLM «говорить/не говорить»; LLM-`speak` подавляется, если за ход отработал command/query.
     >    *(§1.5, §2.14, §5.1)*
> 4. **Command/query выполняются асинхронно (fire-and-forget).** ~~Запрос может идти до ~3 минут; пайплайн
     >    не блокируется...~~ **Откатано в v0.15 — снова синхронно** (см. баннер v0.15 п.1).
     >    *(§1.9, §2.12, §5.1)*
> 5. **Отмена выполняющегося query отложена намеренно.** Поздний результат может ещё озвучиться после
     >    interrupt — это принятый риск, не дефект. *(§1.7, §2.7, §7.3)*

> **Уровень.** Компоненты, потоки, lifecycle-правила и важные инварианты между компонентами.
> Не классовая UML. Имена классов/методов, точные сигнатуры, значения таймаутов, лимитов и деталей реализации уточняются по исходникам и отдельными задачами.

> **Диаграммы.** Рядом, формат **Graphviz `.dot`**. PNG/SVG не храним — рендерит пользователь (`dot -Tsvg …`).
> §5 — `companion_module_graph.dot` (модули). Классовая диаграмма — позже.

> **Связь с другими треками.** Рефактор модели команд и sequence-first input foundation считаются готовым фундаментом. Компаньон строится поверх существующей модели команд, `GameInputSequenceEvent`, `GameInputStep`, `InputSequenceExecutor`, EventBus, STT/TTS/PTT и текущей HUD/GUI-инфраструктуры. HUD-дизайн — отдельный трек, сюда не смешивать.

---

## §0. Концепция

Компаньон — не «ассистент команд» в смысле «речь → одно нажатие». Это **младший член экипажа с памятью**:

* ведёт непрерывный диалог с командиром;
* слышит голос командира;
* получает отфильтрованные игровые события и **помнит** их (knowing-канал);
* вызывает функции для действий и чтения данных;
* помнит ход сессии;
* озвучивает **курируемые наррации** — что и когда сказать о событиях решает subscriber-слой, не сознание;
* не играет сам за командира по событиям.

Основные идеи:

* **Режим-замена.** Компаньон заменяет старый command mode, а не работает параллельно с ним. Активен один режим за раз.
* **Два входных потока, три источника мысли.** Реплики командира и игровые события рождают мысли; событийная сторона расщеплена на `EVENT` (сырое событие → только память, «знание») и `NARRATION` (курируемая наррация → речь).
* **Один класс мысли на источник, у каждого свой ход.** `CommanderThought` (полное рассуждение), `EventThought` (memory-only), `NarrationThought` (ЛЛМ фразирует курируемые данные), `VerbatimNarrationThought` (дословная озвучка готового текста). Поведение несёт тип класса, а не ветки `if (origin)`.
* **Knowing ≠ speaking.** Сырые события только запоминаются (сознание по ним не говорит); спонтанную речь даёт исключительно курируемый narration-слой. Это устраняет «болтовню ЛЛМ по своему усмотрению».
* **Память сессии.** В пределах процесса. Персистентная память — будущий отдельный трек.
* **Сознание — единственный умный узел.** Остальные компоненты — механика, шлюзы, исполнители, фильтры, очереди и хранилища. ЛЛМ работает только в `COMMANDER` и `NARRATION`; `EVENT` и verbatim — детерминированные, без ЛЛМ.
* **Tool-calling only (в ЛЛМ-мыслях).** В `COMMANDER`/`NARRATION` ответ LLM должен быть function/tool call; свободный текст невалиден. `EVENT`/verbatim ЛЛМ не зовут.
* **Опасное подтверждается кодом.** Dangerous actions никогда не исполняются только потому, что LLM так решила.

### §0.1. Типы гарантий

В документе различаются три уровня правил.

**Hard architectural boundary** — граница, которую должен обеспечивать runtime/lifecycle:

* `EventThought` вообще не строит промпт и не зовёт ЛЛМ (memory-only); `NarrationThought` получает нулевой набор game-tools, `VerbatimNarrationThought` — ни ЛЛМ, ни tools. Action/macro-tools физически доступны только `CommanderThought`;
* retry не пересобирает prompt/tools и использует исходный immutable tools snapshot;
* `LlmGateway`, `SpeechGateway` и `ExecutionModule` не получают объект `Thought` и не callback'ают в него;
* `MemoryConsolidator` не является `Thought` и не использует consciousness prompt/tools;
* `MemoryGateway` — единственная дверь к памяти.

**Trusted developer contract** — контракт реализации, который не является sandbox против разработчика с доступом к коду:

* `QUERY` tools должны быть технически read-only;
* `SYSTEM_FUNCTION` tools не должны оборачивать gameplay actions/macros;
* реестры tools/categories должны быть корректно размечены;
* нарушение этих правил считается implementation bug и ловится review/tests, а не lifecycle-магией.

**Accepted operator/gameplay risk** — осознанный прагматичный риск:

* spoken confirmation имеет слабую identity-привязку: подтверждает текущую thought в `awaiting_confirmation` внутри короткого окна;
* текст confirmation request генерируется LLM в контексте линейного диалога и не проходит semantic validation кодом;
* уже стартовавшая input sequence не прерывается даже срочным событием, чтобы не оставить game UI/input state в неизвестном состоянии;
* сессионная память best-effort и не является audit log.

---

## §1. Реестр решений locked

### §1.1. Режим и входные потоки

1. **`companionModeOn` — главный gate режима.**
   Флаг в `SystemSession` решает, кто обслуживает вход: старый `CommandEndPoint` или компаньон.

2. **Компаньон получает два входа.**

    * STT/PTT → `UserInputEvent` → gate → `ThoughtDispatcher`;
    * journal/status/game events → EventBus → gate → `EventFilter` → `ThoughtDispatcher`.

3. **`EventFilter` механический.**
   Он только отсеивает шум. Он не думает, не пишет в память, не определяет срочность, не меняет тему.

4. **Срочность определяет `ThoughtDispatcher`.**
   Срочность ставится при рождении мысли механически:

    * голос — по шаблонам/матчерам срочных фраз;
    * событие — по типу события из списка срочных event types.

---

### §1.2. Типы мыслей

5. **Есть три источника мысли (`ThoughtSource`) и четыре конкретных вида.**

    * `COMMANDER` → `CommanderThought` (полный ЛЛМ-цикл) **или** `ReflexThought` (детерминированный fast-path без ЛЛМ для дословно распознанной прямой команды, v0.17). Какой из двух — решает `ReflexResolver` при рождении мысли (§2.3).
    * `EVENT` → `EventThought` — мысль от сырого игрового события (memory-only, без ЛЛМ).
    * `NARRATION` → `NarrationThought` (фразирует курируемые сенсорные данные через ЛЛМ) **или** `VerbatimNarrationThought` (дословно озвучивает готовый announcement-текст, без ЛЛМ).

   `Thought` — абстрактная база (общие хелперы, исполнение и озвучка/память исхода `recordOutcome`, interrupt), цикла мышления она не содержит: его несёт каждый вид в своём `run()`.

6. **Сколько мыслей живёт одновременно — по lane источника (v0.16):**

    * `COMMANDER` — до `MAX_LIVE_COMMANDER_THOUGHTS` (=5) одновременно (bounded-пул), остальное в очереди;
    * `EVENT` — одна (memory-only, мгновенная);
    * `NARRATION` — одна (короткий раунд).

   `ThoughtDispatcher` держит lane на источник в `EnumMap` (§2.3). Командирский пул нужен, чтобы долгая синхронная команда (медленна не из-за ЛЛМ, а из-за хендлера) не держала новые командирские мысли; медленный narration-ЛЛМ не блокирует мгновенную запись событий.

7. **Каждая новая мысль при рождении получает:**

    * `source`;
    * `urgency`;
    * `currentInput`.

   Отдельного per-thought `topic` нет (см. §2.4/§2.5): тег памяти разрешается по источнику — глобальная тема для `COMMANDER`, тема из статической мапы события для `EVENT`/`NARRATION`.

8. **`currentInput` не пишется сразу в память.**
   Это текущий вход мысли, а не прошлое. Он передаётся в `PromptComposer` отдельно и пишется в память только после разрешения темы или fallback.

---

### §1.3. Права COMMANDER thought

`COMMANDER thought` может:

* выполнять built-in commands;
* выполнять user macros;
* выполнять read-only queries;
* использовать полный commander-набор системных функций;
* вызывать `classify_turn` (тема + важность хода);
* менять global `TopicModel`;
* вызывать `search_in_memory`;
* менять болтливость;
* уточнять;
* искать действие.

---

### §1.4. Права EVENT и NARRATION мыслей

**`EventThought` — чистый «knowing»-канал.** Он не строит промпт, не зовёт ЛЛМ, не говорит и не вызывает никаких tools. Его единственное действие: при `importance() == HIGH` записать событие в память под статической темой (`NORMAL`/`LOW` — не пишет; см. §2.2.1). Спонтанную речь по событиям он не производит вовсе.

**`NarrationThought` (ЛЛМ-фразировка).** Источник — курируемые сенсорные данные (`SensorDataEvent`). Получает **нулевой** набор game-tools (ни команд, ни query) и системные функции только `speak` + `nothing_to_do` (без verbosity-гейта — решение «озвучить» уже принято subscriber-слоем). За один короткий раунд ЛЛМ фразирует данные в характере → `speak`. Не вызывает `search_in_memory`/`clarify`/`change_verbosity`/`classify_turn` и не двигает глобальную тему.

**`VerbatimNarrationThought` (дословно).** Источник — готовый announcement-текст. Не зовёт ЛЛМ и не получает tools вообще: пишет фразу как `[COMPANION]` и озвучивает её дословно.

Тема narration/event-мысли для записи в память берётся не от LLM, а из источника (статическая мапа `event-type → topic` для `EVENT`, переданная subscriber'ом тема для `NARRATION`); событийная сторона никогда не двигает глобальную тему разговора.

Запрет action/macro/query-tools для событийной стороны — **code-level enforcement**, а не prompt-инструкция: `IntelActionAccessPolicy` для `EVENT` отдаёт `QUERY` (но `EventThought` промпт не строит), для `NARRATION` — пусто; `CommanderThought` — единственный, кто получает `ACTION`/`MACRO`.

---

### §1.5. Tool-calling

9. **В контуре сознания любой ответ LLM должен быть tool-call.**
   Свободный текст, пустой ответ, unknown tool, malformed arguments, invalid schema → невалидный ответ.

10. **Используется native OpenAI/Mistral-compatible tool calling.**
    Не JSON-в-тексте.

11. **Мультивызов разрешён.**
    LLM может вернуть несколько tool-calls в одном ответе.

12. **Tool-calls выполняются в порядке ответа LLM.**
    Но перед исполнением всего набора выполняется проверка dangerous actions.

13. **`tool_call_id` обязателен внутри текущего LLM flow.**
    Он связывает assistant tool-call и tool result.
    `tool_call_id` не является частью долгосрочной identity памяти.

14. **Каждый `LlmRequest` имеет immutable tools snapshot.**
    LLM может вызвать только tool из этого конкретного snapshot.
    Unknown/stale/forbidden tool считается invalid response.

15. **Один невалидный tool-call делает невалидным весь response.**
    Частичного исполнения нет.
    Даже валидный `classify_turn` из response, где есть другой invalid tool-call, не применяется.

16. **Repair/retry не пересобирает tools.**
    Retry использует исходный request payload / tools snapshot и тот же cancellation/owner token.
    `LlmGateway` не вызывает `PromptComposer`, `Reducer`, `ToolAccessPolicy` или `SystemToolProvider`.

17. **`classify_turn` из первого валидного response обрабатывается до прочих tool-calls.**
    Это pre-execution step внутри thought lifecycle.
    Но это правило действует только после полной валидации всего tool-call set.

---

### §1.6. Опасные действия

18. **Dangerous action подтверждается кодом.**
    Если в наборе валидных tool-calls есть dangerous action, код замораживает весь набор.

19. **Dangerous classification не живёт в prompt.**
    Для этого нужен `DangerousActionPolicy` / `ActionSafetyClassifier` или эквивалентный контракт.
    Он оценивает как минимум:

    * `operationType`;
    * `toolName`;
    * `arguments`.

    Dangerous может зависеть от аргументов, а не только от имени tool.
    Malformed/unknown dangerous-relevant arguments не должны исполняться.

20. **Обычный `speak` тоже замораживается.**
    Исключение — только `speak` с пометкой `confirmation_request`.

21. **Confirmation request звучит сразу.**
    Он не размораживает действие, а только задаёт вопрос командиру.

22. **Текст confirmation request генерирует LLM.**
    Код не делает semantic validation текста.
    Это accepted operator risk: используется короткий линейный контекст диалога “командир дал опасный приказ → помощник уточнил подтверждение”.
    Prompt должен просить LLM явно называть действие, но это steering, не hard safety.

23. **Разблокировка — только через `ConfirmEvent`.**
    LLM не участвует в подтверждении.

24. **Confirm имеет силу только для текущей thought в состоянии `awaiting_confirmation`.**
    Сильной identity-привязки spoken confirmation к конкретному frozen set нет.
    Это accepted low-probability risk: окно подтверждения короткое, параллельные dangerous-confirmation диалоги не предполагаются, а спам опасными приказами считается operator responsibility.

25. **Желательно не иметь overlapping confirmations.**
    Новая dangerous confirmation не должна открываться, пока предыдущая не завершена, не отменена, не interrupted или не timed out.

26. **Ожидание confirmation принадлежит самой thought.**
    `ThoughtDispatcher` не должен знать внутреннее состояние `awaiting_confirmation`.

27. **Отмена confirmation — отдельный путь.**
    Командирские фразы “нет / отмена / стоп / не надо” должны приводить к cancel confirmation path, если текущая thought ждёт confirmation.
    Это может быть отдельный `CancelConfirmationEvent` или эквивалентная ветка в confirmation input routing.

### §1.7. Очереди и interrupt

28. **Обычная мысль становится в хвост своей очереди.**

29. **Срочная мысль становится в голову своей очереди и прерывает все живые мысли (во всех lane).**
    Независимо от источника срочной мысли. (Narration рождается срочной, см. §2.3.)

30. **Interrupt не должен создавать дыру в памяти.**
    `CommanderThought` перед смертью делает `safe-flush` (записывает ещё не сохранённый вход как `INTERRUPTED`).
    `EventThought`/`NarrationThought`/verbatim коротки и почти мгновенны: им нечего флашить (event пишет сразу либо ничего; narration пишет `[COMPANION]` по завершении).

31. **Командирская мысль при interrupt умирает сразу** после safe-flush.

32. **Событийная сторона при interrupt просто завершается** (сырое событие либо уже записано, либо отбрасывается).

33. **После interrupt мысль не ждёт долгий LLM-ответ.**
    In-flight requests отменяются/помечаются cancelled на уровне handle.

---

### §1.8. Шлюзы

34. **`LlmGateway` queues `LlmRequest`, not `Thought`.**
    Нет callback из `LlmGateway` в `Thought`.

35. **`SpeechGateway` queues `SpeechRequest`, not `Thought`.**
    Нет callback из `SpeechGateway` в `Thought`.

36. **`ExecutionModule` executes requests, not `Thought`.**
    Нет callback из `ExecutionModule` в `Thought`.

37. **Cancelled queued requests удаляются/пропускаются.**
    Cancelled in-flight results discard + diagnostics.

---

### §1.9. Execution

38. **Command/query выполняются синхронно (fire-and-forget откатан, v0.15).**
    `CommanderThought` **ждёт** хендлер; результат идёт во flow (ЛЛМ может цепочить) и пишется в память **компактно** (см. §1.10): «command `id` executed» + текст/описание, ответ запроса — `[COMPANION]`, без сырого `{data:…}`. Озвучка — детерминированная, по типу действия (§2.14). Долгая команда держит lane — принятый baseline; развязка «итог → narration-канал» отложена. Строгую последовательность нажатий обеспечивает `InputSequenceExecutor`.

39. **Read-only queries могут выполняться параллельно.**
    И друг с другом, и параллельно commands/macros.

40. **`QUERY` — технически read-only.**
    Query tool не должен:

    * публиковать `GameInputSequenceEvent`;
    * вызывать input execution layers;
    * нажимать клавиши;
    * открывать/закрывать игровые панели;
    * двигать game UI;
    * менять состояние игры или session state;
    * оборачивать action/macro behavior.

    Tool, который “только читает”, но для чтения нажимает кнопки, не является `QUERY`.
    Если такой tool размечен как `QUERY`, это implementation bug.

41. **Команды/макросы можно отменить только до старта.**
    Если action/macro уже начал выполняться, он выполняется до конца.
    Это intentional consistency trade-off: прерывание input sequence посередине может оставить игру в неизвестном UI/input state.

42. **Срочное событие не отменяет уже стартовавший game input.**
    Оно может прервать thought lifecycle и speech, но не уже начатую sequence.
    Длинные/high-risk sequences должны быть короткими, dangerous-gated или позже получить cooperative cancellation только в safe boundary points.

43. **`ExecutionModule` не пишет в память.**
    Если owning thought умерла, результат завершившегося action/macro идёт только в diagnostics.
    Память получит реальное подтверждение позже через game event/status path, если такое подтверждение существует.

44. **Action tool result — это dispatch/execution status, не факт игрового состояния.**
    Например: `accepted`, `queued`, `started`, `completed_by_executor`, `failed_to_queue`, `binding_missing`.
    Реальное изменение игры подтверждается только game event/status path, если игра вообще даёт такой сигнал.

### §1.10. Память

45. **Память — только сессионная.**
    На диск ничего не пишется. Persistent memory — будущее.
    Companion memory is best-effort session memory, not an audit log.

46. **Память за `MemoryGateway`.**
    Снаружи никто не обращается к внутренним уровням памяти напрямую.

47. **Запись памяти содержит source.**
    LLM должна понимать, откуда пришла информация:

    * `COMMANDER` — реплика командира;
    * `EVENT` — сырое игровое событие;
    * `TOOL_RESULT` — результат command/query;
    * `SYSTEM` — служебная запись (напр. dangerous-confirmation);
    * `COMPANION` — собственные слова компаньона (произнесённая фраза, сам текст, не `{status:spoken}`).

48. **Память — единая timeline опыта.**
    Source не создаёт отдельные памяти, а только маркирует происхождение информации.

49. **Есть 4 области памяти.**

    * short-term memory;
    * mid-term topic memory;
    * long_term_summary;
    * llm_memory.

50. **short-term memory — горячий контекст.**
    Новые записи попадают сначала только туда.

51. **mid-term topic memory получает записи только при вытеснении из short-term.**
    Не дублируем активные записи сразу в short-term и mid-term.

52. **long_term_summary — одна общая на всю сессию.**
    Всегда добавляется в prompt.

53. **llm_memory — отдельная маленькая циклическая память LLM.**
    Не делится на темы, не консолидируется в long_term_summary.

---

## §2. Компоненты и потоки

### §2.1. Режим на шине событий

Весь ввод/вывод остаётся через существующую event-driven инфраструктуру.

#### Голосовой вход

```text
STT / PTT
→ UserInputEvent
→ companionModeOn gate
→ ThoughtDispatcher
→ COMMANDER thought
```

Если `companionModeOn = false`, голосовой ввод обслуживает старый command mode.

#### Событийный вход — три пути

«Сырое» событие (только знание):

```text
Journal / Status / Game events
→ EventBus → companionModeOn gate → GameEventFilter → ThoughtDispatcher
→ EventThought   (memory-only)
```

Курируемая наррация — ЛЛМ фразирует:

```text
subscriber → SensorDataEvent → CompanionSensorDataBridge → ThoughtDispatcher.submitSensorData
→ NarrationThought   (один ЛЛМ-раунд, speak)
```

Курируемое announcement-объявление — дословно:

```text
subscriber → MiningAnnouncementEvent / Discovery / Route / RadarContact / Navigation
→ CompanionAnnouncementBridge (тумблеры PlayerSession) → ThoughtDispatcher.submitVerbatimNarration
→ VerbatimNarrationThought   (без ЛЛМ: запись [COMPANION] + дословная озвучка)
```

В companion-режиме legacy `VocalisationRouter` для этих announcement-событий молчит (чтобы не озвучить дважды); radio-трансмиссия остаётся на legacy-пути и в память не попадает. Если `companionModeOn = false`, companion event flow не активен.

---

### §2.2. EventFilter

`EventFilter` — механический компонент.

Он:

* получает игровые события;
* отсекает шум: события вне gameplay-таксономии (`EventTopicMap`) и события важности `LOW` (см. §2.2.1);
* пропускает события, которые достойны внимания;
* не пишет в память;
* не определяет срочность;
* не вызывает LLM;
* не вызывает query/action tools;
* не меняет topic.

Единственный выход:

```text
EventFilter
→ ThoughtDispatcher
```

Срочность события определяет `ThoughtDispatcher` по типу события.

---

### §2.2.1. Важность события (importance)

У каждого игрового события есть `importance()` (см. `BaseEvent.Importance`): `LOW`, `NORMAL`, `HIGH`.
**Это фильтр релевантности для памяти**, а не триггер речи (раньше `HIGH` открывал ЛЛМ микрофон; теперь
`EventThought` memory-only и не озвучивает вовсе — спонтанную речь по событиям даёт только курируемый
narration-слой). Важность читается по экземпляру, поэтому может зависеть от payload.

* **`LOW` — компаньон игнорирует полностью.** `GameEventFilter` отбрасывает событие: ни память, ни мысль не
  создаются. Это высокочастотная телеметрия (`FSSSignalDiscovered`, `MaterialCollected`, `Cargo`,
  `FSDTarget` и т.п.).
* **`NORMAL` — доходит до мысли, но не сохраняется.** `EventThought` создаётся, но событие **не пишется в
  память** (чтобы не засорять ленту) и ИИ не зовётся. (Мысль всё равно создаётся — оставлено на будущее.)
* **`HIGH` — пишется в память.** `EventThought` записывает событие под статическим topic события и
  завершается. **Без ЛЛМ и без речи** — EVENT-канал только «знает».

**Понижение HIGH под curated narration (§4.2).** Если у HIGH-события есть курируемая наррация (озвучка +
запись `[COMPANION]` через subscriber-слой), то писать ещё и сырое `[EVENT]` — дубль. Такие события понижены
до `NORMAL` (`ScanOrganic`, `ProspectedAsteroid`, `CarrierBuy`, `CodexEntry`, `MissionAccepted/Completed/
Redirected`, `Promotion`, `Resurrect`, `ShipyardNew`). `HIGH` остаётся только у событий **без** наррации,
которые стоит помнить (напр. `MissionFailed`): они сохраняются сырыми, но не озвучиваются.

Примеры payload-зависимой важности: `ShipTargeted` — только для отсканированной wanted-цели; `ReceiveText` —
только для пиратского оклика при наличии груза. (`ProspectedAsteroid` свой target-чек делегировал
`ProspectorSubscriber`, который и владеет mining-наррацией.)

Детерминированную и критическую озвучку событий (топливо, кислород, скан груза, пиратский оклик, kill
confirmed и т.п.) владеет `EventNarrator`, который звучит во всех режимах.

---

### §2.3. ThoughtDispatcher

`ThoughtDispatcher` — учётно-распорядительный узел сознания.

Он не думает и не интерпретирует смысл.

Он знает:

* по одному `ThoughtLane` на каждый `ThoughtSource`, хранятся в `EnumMap` и публикуются одной volatile-ссылкой (commander / event / narration); командирский lane — **bounded-пул** на `MAX_LIVE_COMMANDER_THOUGHTS` воркеров, event/narration — по одному;
* набор живых мыслей на каждом lane (до N на commander, по одной на остальных);
* urgency каждой мысли;
* source каждой мысли.

Он умеет:

* создать мысль (`submitCommanderInput` / `submitEvent` / `submitSensorData` / `submitVerbatimNarration`); `submitCommanderInput` сперва прогоняет `ReflexResolver` (коллаборатор диспетчера, как `UrgencyPolicy`): дословно распознанная прямая команда → `ReflexThought` (без ЛЛМ), иначе → `CommanderThought`. Резолвер — детерминированная подстановка по фразам/реестрам, не интерпретация смысла;
* поставить мысль в lane её источника;
* срочную мысль поставить первой;
* при срочной мысли отправить interrupt всем живым мыслям (во всех lane);
* запустить следующую мысль соответствующего источника, если lane свободен;
* аварийно остановить мысль по общему watchdog timeout.

Cross-cutting операции (start/stop, interrupt, watchdog, idle) итерируют `lanes.values()` — добавление источника = одна ячейка карты. **Сам диспетчер ничего не пишет в память и не озвучивает** — это делает мысль.

Он не знает:

* находится ли мысль внутри `awaiting_confirmation`;
* какие tool-calls внутри мысли;
* какие LLM requests у мысли;
* какие speech/execution handles у мысли;
* что именно мысль хранит в local messageFlow.

---

### §2.4. Thought — база и виды

`Thought` — **абстрактная база**, общая для всех видов: держит `source`/`urgency`/`currentInput`/`ctx`, interrupt-механику (`interrupted` + `inFlight` + `interrupt()`), и строительные блоки — `composeInitialPrompt`, `submitRound` (один interruptible ЛЛМ-раунд), `execute`, `recordCurrentInput`, `recordCompanionSpeech`, а также озвучку/память исхода по типу действия `recordOutcome` (+ `voice`/`description`/`rememberAction`) — общую для исполнителей командирского lane (`CommanderThought` и `ReflexThought`). **Цикла мышления база не содержит**: его несёт `run()` каждого вида.

```text
Thought (abstract)
├─ CommanderThought       полный tool-calling-цикл (до 8 раундов) + dangerous-confirmation; синхронное
│                         исполнение, озвучка/память итога по типу действия (recordOutcome), подавление LLM-speak
├─ ReflexThought          fast-path без ЛЛМ: run() = recordCurrentInput → execute(команда) → recordOutcome;
│                         рождается, когда ReflexResolver дословно распознал безопасную беспараметрную команду (§2.3)
├─ EventThought           run() = (HIGH) recordCurrentInput, иначе ничего; ЛЛМ/речи/tools нет
├─ NarrationThought       run() = один раунд → взять speak → озвучить + записать [COMPANION]
└─ VerbatimNarrationThought  run() = записать [COMPANION] + озвучить дословно (без ЛЛМ)
```

Поля общей мысли:

```text
source = COMMANDER | EVENT | NARRATION
urgency = normal | urgent
currentInput
```

> **Тема — не поле мысли (см. §2.5).** Для `COMMANDER` тег памяти — глобальная тема; для `EVENT`/`NARRATION` — из источника. `CommanderThought` применяет `classify_turn` (тема + важность) как pre-execution шаг до записи реплики; до первого валидного ответа действует fallback `unresolved_*`.

`currentInput`:

* для `COMMANDER` — реплика командира;
* для `EVENT` — текст/summary игрового события;
* для `NARRATION` — данные/инструкции сенсора (ЛЛМ-фразировка) или готовый announcement-текст (verbatim).

Для `CommanderThought` `currentInput` не является memory entry до topic resolution. `EventThought` пишет `currentInput` напрямую (под темой события); `NarrationThought`/verbatim **не пишут `currentInput` вовсе** — в память идёт только произнесённая фраза `[COMPANION]`.

---

### §2.5. Topic resolution

Тема — это **одна глобальная тема разговора**, и нужна она только для тегирования записей в памяти. Отдельного per-thought `topic` нет: запись мысли тегируется темой, определённой ниже по источнику.

#### COMMANDER thought

Каждый ход командира LLM вызывает `classify_turn` ровно один раз: он несёт `topic` (тема хода) и `importance` (важность хода для памяти). Тема — глобальная и липкая, меняется **только** через `classify_turn`; важность — пер-ход (штампует записи этого хода, не сохраняется как state). Реплики командира пишутся в память под текущей глобальной темой и с выбранной важностью.

Если `classify_turn(validTopic, importance)`:

```text
global TopicModel = validTopic   # применяется до записи реплики командира в память
turn importance   = importance   # штампует записи этого хода (NORMAL при unknown)
```

Если `classify_turn(unknownTopic, ...)`:

```text
игнорировать (tool result = error), глобальная тема не меняется
```

Если LLM не вызвала `classify_turn`:

```text
глобальная тема остаётся прежней; реплика тегируется текущей темой с важностью NORMAL
```

Порядок: при валидном response `classify_turn` применяется как pre-execution step (до записи `currentInput`), даже если LLM вернула его не первым; при invalid response он не применяется.

#### EVENT thought

EVENT-мысль **не** трогает глобальную тему и не вызывает `classify_turn` (его нет в её tools). Тема для записи события берётся механически из статической мапы `event-type → topic` (каталог событий). Это даёт честный тег памяти, не перебивая тему разговора командира.

```text
global TopicModel — без изменений
memory tag = EventTopicMap.topicFor(eventType)   # fallback: unresolved_game_event
```

---

### §2.6. Запись currentInput в память

После topic resolution, но до исполнения tool-calls:

```text
currentInput
→ MemoryGateway
```

Порядок внутри мысли:

```text
1. Thought created
2. initial LLM turn returns a valid tool-call set
3. topic resolved: COMMANDER applies classify_turn (if called) to the global topic and turn importance;
   EVENT uses its static event-type topic
4. currentInput записывается в память под разрешённой темой
5. tool-calls выполняются по порядку
6. tool results пишутся в память отдельно
```

Это даёт честный порядок памяти (`CommanderThought`):

```text
[COMMANDER] requested action
[TOOL_RESULT] action result      # либо [COMPANION] произнесённая фраза, если ход был разговорный
```

или у narration:

```text
[COMPANION] произнесённая в характере фраза
```

(`EventThought` пишет одну запись `[EVENT]` для HIGH-события и ничего больше.)

---

### §2.7. Safe-flush при interrupt (только CommanderThought)

Safe-flush — забота `CommanderThought` (у него длинный ЛЛМ-цикл). `EventThought` мгновенный; `NarrationThought`/verbatim коротки — флашить нечего. Команды/запросы исполняются синхронно (v0.15): долгая команда держит lane до конца; начатую input-sequence interrupt не прерывает (§1.9.41), чтобы не оставить игру в неизвестном состоянии.

При interrupt `CommanderThought` не начинает новых действий. Она делает только safe-flush.

Safe-flush:

1. Если `currentInput` ещё не записан (interrupt до первого валидного ответа):

    * тема `unresolved_commander_input`, source `COMMANDER`;
    * `processing_state = INTERRUPTED`.

   Если вход уже записан (тема разрешена на первом валидном ответе), перезаписывать его не нужно.

2. Если есть уже полученные query/tool results, но они ещё не записаны:

    * записать их в MemoryGateway.

3. Если мысль ждала dangerous confirmation:

    * записать итог `interrupted/cancelled`.

4. Если interrupt reason известен, записать его в diagnostics и/или memory entry, где это полезно:

    * `interrupted_by_urgent_event`;
    * `interrupted_by_barge_in`;
    * `interrupted_before_response`.

5. Отменить свои LLM/Speech/Execution handles, где это применимо.

6. Не начинать:

    * новый LLM request;
    * новый query;
    * новое action/tool execution;
    * новую озвучку.

`CommanderThought` после этого умирает.

---

### §2.8. Local messageFlow внутри Thought

Внутри одной мысли есть локальный `messageFlow`.

Старт:

```text
PromptComposer
→ initial messages
→ Thought.localMessageFlow
```

Дальше:

```text
LLM assistant tool_call
→ ExecutionModule
→ result
→ MemoryGateway write
→ tool-result appended to localMessageFlow with tool_call_id
→ next LLM round
```

`tool_call_id`:

* берётся из LLM tool-call;
* используется для tool-result в рамках текущего messageFlow;
* не является частью long-term memory identity;
* не восстанавливается для будущих мыслей.

Будущие мысли не replay старые `tool` messages.
Они читают прошлое через MemoryGateway.

---

### §2.9. LlmGateway

`LlmGateway` — единственная дверь к моделям.

Он не знает объект `Thought`.

Он получает:

```text
LlmRequest
```

`LlmRequest` содержит:

```text
requestId
messages
tools                # immutable tools snapshot для этого запроса
profile              # PromptCacheProfile: COMMANDER | EVENT | COMPRESSION; задаёт Mistral prompt_cache_key
```

`LlmGateway` возвращает:

```text
CompletableFuture<LlmResult>
```

При interrupt мысль отменяет future (`future.cancel(...)`); отдельного cancellation/owner token нет.

`LlmGateway` не знает, как пересобрать consciousness prompt.
Он не имеет доступа к `Thought`, `PromptComposer`, `Reducer`, `ToolAccessPolicy` или `SystemToolProvider`.
Repair/retry может использовать только исходный request payload / immutable tools snapshot.

Поведение gateway:

* queued cancelled request → удалить/пропустить;
* in-flight cancelled request → дождаться технически, discard result, diagnostics;
* result cancelled request не попадает:

    * в Thought;
    * в ExecutionModule;
    * в SpeechGateway;
    * в MemoryGateway;
    * в TopicModel.

#### Invalid response

Если модель вернула:

* plain text вместо tool-call;
* empty response;
* malformed tool-call;
* unknown tool;
* invalid arguments/schema;

то `LlmGateway` делает одну repair/retry попытку, но только если потраченный token cost ниже настроечного порога.
Retry использует тот же tools snapshot и тот же cancellation/owner token.

Если хотя бы один tool-call invalid, invalid считается весь response.
Никакие tool-calls из такого response не применяются, включая валидно выглядящий `classify_turn`.

Если retry не выполняется или не помогает:

```text
INVALID_RESPONSE
```

#### Реакция Thought на INVALID_RESPONSE

`CommanderThought`:

```text
currentInput → MemoryGateway
topic = unresolved_commander_input
processing_state = UNRESOLVED
SpeechGateway → служебная фраза “не могу выполнить”
Thought ends
```

`NarrationThought` (best-effort): просто завершается молча, ничего не пишет. `EventThought`/verbatim ЛЛМ не зовут — INVALID_RESPONSE у них не бывает.

Unresolved-записи идут обычным путём памяти.

---

### §2.10. PromptComposer

`PromptComposer` — тупой укладчик `messages + tools`; ветвится по источнику: `COMMANDER` → полный промпт (persona + tool-calling + commander-rules + safety + language + topic enum + memory + timeline + current input, профиль `COMMANDER`); `NARRATION` → лаконичный промпт (narration-persona + задача + language + timeline + данные, **без** topic enum/memory/safety, профиль `NARRATION`); `EVENT` промпт не строит (memory-only).

Он не решает:

* какие tools разрешены;
* какие команды релевантны;
* как описывать каждую команду.

Он получает уже готовые данные:

```text
short-term memory timeline
currentInput
origin
urgency
global TopicModel
Topic enum
long_term_summary
memory indexes
selected command/query tools
system tools
```

Он собирает:

```text
messages + tools
```

#### Структура messages

Стабильный prefix:

```text
system:
  persona / behavior rules
  tool-calling-only rules
  full Topic enum with descriptions
  COMMANDER/EVENT rules
  safety/confirmation rules
  llm_memory index
  topic memory index
  long_term_summary
```

Краткосрочная память:

```text
context block:
  Session memory timeline:
  [COMMANDER][topic][processing_state] ...
  [EVENT][topic][processing_state] ...
  [TOOL_RESULT][topic][processing_state] ...
  [SYSTEM][topic][processing_state] ...
```

Текущий вход:

```text
role = user
content:
  Current input:
  source: COMMANDER | EVENT
  urgency: normal | urgent
  content: ...
```

Игровые события не представляются как `tool` messages.

`tool` messages используются только внутри текущего function-calling flow для результатов реальных tool-calls.

---

### §2.11. ToolAccessPolicy, Reducer, SystemToolProvider

#### ToolAccessPolicy

Отвечает только за категории игровых/query tools.

Он получает source мысли и возвращает `allowedToolCategories`:

```text
COMMANDER → QUERY, ACTION, MACRO
EVENT     → QUERY            (но EventThought промпт не строит — memory-only)
NARRATION → ∅               (нулевой набор: ни команд, ни query)
```

Это единственная точка категорий. Корректная классификация query/action/macro — implementation contract.

#### Reducer

`Reducer` не знает про источник мысли.

Он получает:

```text
allowedToolCategories
currentInput
topic
other selection context
```

и возвращает конкретные tools из разрешённых категорий.

Если разрешена только категория `QUERY`, action/macro tools не могут попасть в результат.

#### SystemToolProvider

Возвращает системные функции по origin мысли.

COMMANDER system tools:

```text
speak
nothing_to_do
clarify
search_in_memory
classify_turn
change_verbosity
```

NARRATION system tools (`EVENT` промпт не строит и системных функций не получает):

```text
speak
nothing_to_do
```

`NARRATION speak` **не гейтится** verbosity (прежний `EventSpeechPolicy` удалён): курируемый subscriber-слой уже решил, что фразу нужно озвучить. `availableFor(NARRATION)` у `SpeakFunction`/`NothingToDoFunction` возвращает true; остальные системные функции — COMMANDER-only.

Системные функции присутствуют в prompt только если разрешены для источника.
`SYSTEM_FUNCTION` — trusted internal category: она не должна публиковать `GameInputSequenceEvent`, выполнять macro/action behavior или менять game state.
Если system function делает gameplay input, это должна быть `ACTION`/`MACRO`, а не `SYSTEM_FUNCTION`.

#### PromptComposer

Берёт:

```text
Reducer selected tools
+ SystemToolProvider tools
```

и собирает OpenAI/Mistral-compatible tool descriptions.

Каждая команда/запрос/системная функция сама знает, как описать себя в tool schema.
PromptComposer не генерирует schemas сам.

---

### §2.12. ExecutionModule

`ExecutionModule` — единый вход выполнения tool-calls.

Он не знает объект `Thought`.

Он получает:

```text
ExecutionRequest
```

Содержит:

```text
requestId
toolName
arguments
```

`operationType` (action/macro/query/system-function lane) выводится при резолве `toolName` по реестрам и в запросе не передаётся.
Он возвращает `CompletableFuture<JsonObject>`.

#### Очереди

Commands/macros:

```text
serialized action lane
```

Read-only queries:

```text
parallel query lane
```

Read-only query lane не означает “безопасно, потому что цель — узнать”.
`QUERY` tool технически не должен выполнять game input или изменять session/game state.

Системные функции исполняются по своей логике, но не превращают ExecutionModule в память/сознание.

#### Cancellation

Action/macro:

* если ещё не стартовал → можно отменить;
* если уже стартовал → выполнить до конца;
* если owning thought умерла → result only diagnostics;
* MemoryGateway не трогать.

Query:

* если ещё не стартовал → можно отменить;
* если завершился после смерти мысли → result ignored;
* MemoryGateway не трогать.

ExecutionModule сам в память не пишет.

Фактические изменения игры должны прийти позже через journal/status/event path.

---

### §2.13. Dangerous confirmation

Перед исполнением набора tool-calls Thought проверяет dangerous actions через `DangerousActionPolicy` / эквивалентную классификацию.
Проверка выполняется только после того, как весь tool-call set валиден.
Malformed dangerous-looking call — это invalid response, не confirmation candidate.

Если dangerous action есть:

```text
freeze all tool-calls
```

Исключение:

```text
speak with confirmation_request marker
```

Она выполняется сразу через SpeechGateway.

Thought пишет в память:

```text
source = SYSTEM
topic = thought.topic
processing_state = awaiting_confirmation
content = dangerous action requires confirmation
```

Текст confirmation request генерирует LLM в текущем conversational context.
Код не валидирует семантически, насколько полно этот текст описывает frozen set.
Prompt должен просить LLM явно назвать подтверждаемое действие.

Потом Thought ждёт `ConfirmEvent` с собственным timeout.

#### ConfirmEvent

Источники:

* STT code word из настроек;
* input module key/button.

Confirm идёт по выделенной confirmation bus.

Confirm имеет силу только если есть current Thought в `awaiting_confirmation`.
Сильной привязки spoken confirmation к конкретному frozen action set нет; это accepted operator risk.

Если confirm пришёл:

```text
unfreeze tool-call set
execute all in original LLM order
write outcome to memory
```

Если timeout:

```text
discard frozen set
write timed_out / not confirmed to memory
Thought ends
```

Если commander явно отменяет:

```text
discard frozen set
write cancelled_by_commander to memory/diagnostics
Thought ends
```

Если interrupt:

```text
discard frozen set
write interrupted/cancelled to memory
Thought ends
```

Новый commander input во время ожидания confirmation — policy decision.
Базовое правило v0.13: explicit cancel/no/stop phrases должны отменять pending confirmation.
Остальной commander input может идти в обычную очередь, но нельзя иметь несколько overlapping dangerous confirmations.

---

### §2.14. SpeechGateway

> **Кто решает, что озвучивать.** Не LLM. Источники речи: (1) command/query/macro-итог — детерминированно по
> типу действия (`CommanderThought.recordOutcome`, §5.1): COMMAND — текст хендлера (crit→urgent) либо ack
> `affirmative()` для side-effect; QUERY — ответ; MACRO — молча (свои шаги). LLM-`speak` за ход с
> `COMMAND|QUERY|MACRO` подавляется. (2) курируемый subscriber-слой (`gameapi.journal.subscribers`) →
> `NarrationThought` (ЛЛМ фразирует) / `VerbatimNarrationThought` (дословно); (3) свободный `speak` LLM —
> только на разговорном ходу без игрового действия. Слова компаньона (свободный `speak`, ответ запроса,
> narration) пишутся в память как `[COMPANION]`.

`SpeechGateway` — единственная дверь на озвучку.

Он не знает объект `Thought`.

Он получает:

```text
SpeechRequest
```

Содержит:

```text
requestId
text
urgency
```

Возвращает `CompletableFuture<Void>` (отмена — `future.cancel(...)`).

При interrupt Thought отменяет свои speech handles.

SpeechGateway:

* queued cancelled speech → удалить/пропустить;
* currently speaking cancelled/stale speech → остановить;
* urgent speech может прервать текущую речь;
* barge-in может прервать текущую речь и очистить очередь.

#### Системные нотификации

Системные компоненты могут говорить через SpeechGateway напрямую как system notification:

```text
MemoryConsolidator failure
LLM unavailable
TTS/STT service issue
```

Это не идёт через Сознание и не требует LLM.
System notification text is fixed/code-generated, not companion LLM-generated.

System notifications должны иметь severity/priority policy.
Не каждая техническая ошибка должна перебивать игровой момент; часть может идти в diagnostics или очередь.

---

### §2.15. Barge-in

`BargeInController` — отдельный узел вне SpeechGateway.

Вход:

```text
PTT / commander speech while TTS speaking
```

Выходы:

```text
SpeechGateway interrupt
ThoughtDispatcher interrupt
```

Speech interruption и Thought interruption разделены.

`BargeInController` не принимает центрального решения “убить всё”.
Он рассылает split signal двум адресатам, а каждый адресат применяет свою lifecycle-логику.

SpeechGateway не должен сам решать судьбу Thought.
ThoughtDispatcher не должен управлять аудио-очередью напрямую.

Если commander speech содержит control prefix вроде “стоп / тихо / отмена”, barge-in path может съесть этот prefix.
Оставшийся текст должен идти обычным путём `UserInputEvent → COMMANDER thought`.
Если после control prefix ничего нет, новая thought не создаётся.

Barge-in не является gameplay command path и не обходит `ToolAccessPolicy`, `Reducer`, dangerous confirmation или `ExecutionModule`.

---

## §3. Память подробно

### §3.1. MemoryGateway

`MemoryGateway` — единственная дверь к памяти.

Он умеет:

* записать обычную memory entry;
* прочитать short-term timeline;
* прочитать topic memory по topic;
* прочитать llm_memory;
* записать llm_memory;
* отдать memory indexes для PromptComposer;
* отдать long_term_summary;
* принять обновлённую long_term_summary от MemoryConsolidator.

Он не умеет:

* интерпретировать смысл;
* решать, что важно;
* сам вызывать LLM;
* формировать semantic summary;
* менять topic.

---

### §3.2. MemoryEntry

Обычная запись памяти содержит минимум:

```text
timestamp
topic
source
content
processing_state
```

`source`:

```text
COMMANDER
EVENT
TOOL_RESULT
SYSTEM
```

`processing_state` примеры:

```text
processed
unresolved_due_to_llm_error
interrupted_before_topic_resolution
awaiting_confirmation
confirmed
cancelled
timed_out
interrupted
```

> Реализация (`MemoryProcessingState`) сейчас содержит только `PROCESSED` и `UNRESOLVED`; остальные состояния добавляются под их код-пути в последующих фазах.

Память сортируется по фактическому времени записи в `MemoryGateway`.

Память не знает о lifecycle мысли и не сортирует записи по времени рождения мысли.

---

### §3.3. Short-term memory

Short-term memory — горячая хронологическая лента.

Свойства:

* единая timeline;
* не разделена на темы как структура;
* каждая запись всё равно имеет topic;
* содержит последние записи живого контекста;
* прямо вставляется в prompt как context block.

Ограничения:

* max entry count;
* token budget.

Если после новой записи short-term превышает лимит:

```text
oldest entries evicted
→ moved to mid-term topic memory by topic
```

Запись, пока она в short-term, не дублируется в mid-term.

---

### §3.4. Mid-term topic memory

Mid-term topic memory — тематический архив записей, вытесненных из short-term.

Структура:

```text
topic -> list of MemoryEntry
```

Ограничение:

```text
max entries per topic
```

Отдельный token-budget хранения не нужен, потому что topic memory никогда не возвращается целиком.

Если topic pool переполнен:

```text
oldest entries evicted
→ consolidation buffer
```

---

### §3.5. Search in memory

`search_in_memory(query)` — единый поиск по памяти, заменивший прежний двухскоупный `recall(scope=...)`:

* доступен только COMMANDER thought;
* единственный параметр — `query` (без `scope`, без `topic`);
* ищет сразу по **всей** mid-term topic memory (по всем темам), по short-term timeline **и** по `llm_memory` (осознанные факты);
* фильтр — простое текстовое вхождение `query` (case-insensitive); пустой query → просто самые свежие записи;
* результаты всех источников объединяются и сортируются по времени записи (свежие первыми);
* максимум N записей (N задаётся настройкой);
* short-term тоже ищется (хотя и вставлен в prompt целиком) — это страховка: если модель всё же решит искать в памяти, она получает цельную картину; запись живёт строго в одном уровне (short-term **или** mid-term), поэтому дублей между уровнями нет;
* `long_term_summary` не ищется — он всегда вставлен в prompt целиком;
* без LLM; без embeddings на первом этапе.

Зачем единый scope: малая локальная модель плохо выбирает scope/topic (в evals `recall(topic_memory)` не вызывался вовсе); один `search_in_memory(query)` убирает это решение — модель просто задаёт, что ищет.

---

### §3.6. long_term_summary

`long_term_summary`:

* одна общая на всю сессию;
* не делится на темы;
* всегда вставляется в prompt;
* должна быть компактной;
* содержит сжатую выжимку старой вытесненной mid-term памяти.

`llm_memory` не попадает в `long_term_summary`.

---

### §3.7. MemoryConsolidator

`MemoryConsolidator` обслуживает переход:

```text
mid-term topic memory evicted entries
→ consolidation buffer
→ long_term_summary
```

Он:

1. получает записи, вытесненные из mid-term topic memory;
2. копит их в consolidation buffer;
3. ждёт достижения бюджетного порога;
4. вызывает LlmGateway в режиме compression;
5. передаёт LLM:

    * current long_term_summary;
    * consolidation buffer;
6. получает новую компактную long_term_summary;
7. атомарно заменяет старую long_term_summary;
8. после успешной консолидации очищает buffer.

Если consolidation failed:

```text
LLM timeout
INVALID_RESPONSE
provider error
malformed / too long compression output
```

то:

```text
только current consolidation buffer очищается
raw entries из этого buffer считаются потерянными
существующий long_term_summary остаётся без изменений
short-term memory не трогается
оставшаяся mid-term topic memory не трогается
llm_memory не трогается
diagnostics пишет ошибку
system notification создаётся через SpeechGateway / SystemNotificationPolicy
ошибка не пишется в память компаньона
```

Причина: если не очистить buffer, он выйдет за пределы бюджета. Потерянная информация уже была старой/вытесненной.

`MemoryConsolidator` не является `Thought`.
Он не использует `PromptComposer`, `ToolAccessPolicy`, `Reducer` или `SystemToolProvider`.
Он отправляет compression-only `LlmRequest` без gameplay/system tools.
Tool-calling-only относится к consciousness mode; compression mode имеет свой output contract: компактный summary text/schema, прошедший size/format validation.

---

### §3.8. llm_memory

`llm_memory` — отдельная маленькая память LLM/компаньона.

Свойства:

* отдельный слой;
* не делится на темы;
* не участвует в short/mid/long transitions;
* не консолидируется;
* хранит максимум 15 записей;
* каждая запись максимум 50 символов;
* цикличная;
* новая 16-я запись вытесняет самую старую;
* точные дубли не добавляются.

`remember(content)`:

* доступен только COMMANDER thought;
* не принимает topic;
* пишет только в llm_memory;
* если content длиннее 50 символов, код обрезает;
* tool description должен явно сказать: max 50 characters;
* после записи возвращает tool result, чтобы LLM знала, что запись сделана.

Дедупликация:

```text
trim
collapse spaces
case-insensitive compare
```

Чтение `llm_memory`: осознанные факты доступны через единый `search_in_memory(query)` (§3.5) наравне с mid-term памятью — отдельного scope больше нет. llm_memory мал (максимум 15 × 50 символов), его записи хранятся с отметкой времени, чтобы попадать в общую сортировку поиска по времени.

---

### §3.9. Memory indexes в prompt

PromptComposer всегда вставляет:

1. полный `Topic enum` с описаниями;
2. индекс `llm_memory`;
3. индекс mid-term topic memory;
4. `long_term_summary`.

#### Topic enum

Полный список тем всегда присутствует в prompt, иначе LLM не знает допустимые значения для:

```text
classify_turn (параметр topic)
```

Topic enum должен быть компактным: примерно 10–15 тем.

Каждая тема имеет:

```text
id
short description
```

#### llm_memory index

Пример:

```text
Remembered facts:
7 / 15 items.
```

Полное содержимое llm_memory не вставляется автоматически; для поиска по нему используется `search_in_memory(query)`.

#### topic memory index

Показывает только темы, где реально есть mid-term memory.

Hints не генерируются LLM.
Они берутся из статических описаний Topic enum + дешёвой metadata из MemoryGateway.

Пример:

```text
Topics with stored memory:
- navigation
- trade
```

---

## §4. Исполнительные функции

### §4.1. System tools для COMMANDER thought

COMMANDER thought получает:

```text
speak
nothing_to_do
clarify
search_in_memory
classify_turn
change_verbosity
```

#### `speak`

Озвучить текст через SpeechGateway.

Может иметь marker:

```text
confirmation_request
```

Только такой speak проходит сразу при frozen dangerous set.

#### `nothing_to_do`

Завершить ход: делать (больше) нечего. Явный терминатор tool-calling-only цикла, отличающий намеренно пустой ход от пустого/битого ответа LLM. Это не «тишина»: не озвучивать — это просто отсутствие вызова `speak`, ход может действовать и молча.

#### `clarify`

Задать уточнение командиру.

#### `search_in_memory`

Единый поиск по памяти: `search_in_memory(query)`. Один параметр `query`; ищет одновременно по mid-term памяти всех тем и по `llm_memory`, возвращает свежие совпадения, отсортированные по времени (см. §3.5). Без `scope`/`topic`.

#### `find_action` (retired)

Поиск действия по каталогу. **Выведен из обращения**: больше не регистрируется и не предлагается модели (`@RegisterSystemFunction` снята). Причина — редьюсер достаточно надёжно поднимает нужные инструменты, а малая локальная модель за `find_action` не тянется (в evals 0 вызовов). Класс `FindActionFunction` сохранён как наследие; recovery промахов редьюсера, если понадобится, делать **системным fallback'ом** (второй проход с расширенным набором), а не модельным инструментом.

#### `classify_turn`

COMMANDER-only. Классифицирует ход для памяти — один вызов с двумя параметрами `topic` + `importance` (объединил прежние `change_global_topic` и `set_importance`). Вызывается ровно раз за ход; только организует память, сам ход не разрешает.

```text
global TopicModel = validTopic   # тема (липкая): тег для записи реплик командира в память
turn importance   = importance   # важность хода: штампует записи этого хода (NORMAL при unknown)
```

#### `change_verbosity`

Меняет режим болтливости.

---

### §4.2. System tools для событийной стороны

`EventThought` системных функций **не получает вовсе** — он memory-only, промпт не строит, ЛЛМ не зовёт.

`NarrationThought` (`source = NARRATION`) получает ровно две:

```text
speak            # без verbosity-гейта: решение «озвучить» принял subscriber-слой
nothing_to_do
```

Verbosity narration не глушит (прежний `EventSpeechPolicy` удалён): курируемый слой уже решил, что фраза достойна озвучки. ЛЛМ за один раунд формулирует фразу → `speak`; `nothing_to_do` завершает ход. `search_in_memory`/`clarify`/`change_verbosity`/`classify_turn` ему недоступны.

`VerbatimNarrationThought` ЛЛМ и tools не получает вовсе — он детерминированно пишет `[COMPANION]` и озвучивает готовый текст.

Тему для записи берёт не LLM, а источник (§2.5): статическая мапа `event-type → topic` для `EVENT`, переданная subscriber'ом тема для `NARRATION`.

---

## §5. Tool execution flow

### §5.1. Normal COMMANDER flow

Command/query/macro исполняются **синхронно**; результат всегда идёт во flow (ЛЛМ может цепочить), а озвучка и timeline-память — **по типу действия** (`recordOutcome`). `speak` за ход с `COMMAND|QUERY|MACRO` подавляется; свободный `speak` выживает только на разговорном ходу (без игрового действия) и пишется как `[COMPANION]`.

```text
UserInputEvent
→ ThoughtDispatcher → CommanderThought
→ PromptComposer initial messages → LlmGateway → tool-calls
→ turn classified (classify_turn applied if called: global topic + turn importance)
→ currentInput written to memory   ([COMMANDER])
→ execute tool-calls in order (sync), result → flow; then recordOutcome by IntelActionType:
     COMMAND  → voice text (crit→urgent) | ack affirmative() if side-effect;  mem [TOOL_RESULT] "command id executed"+text/desc
     QUERY    → voice answer;                                                  mem [COMPANION] = answer
     MACRO    → no voice (own steps);                                          mem [TOOL_RESULT] "macro id executed"+desc
     SYSTEM   → no speech, no timeline (result only in flow)
     speak    → suppressed if game action this turn; else voice + [COMPANION]
→ next LLM round (если не nothing_to_do)
→ nothing_to_do / end
```

Раунд завершает ход не только по `nothing_to_do`: если за ход уже прошло игровое действие (`COMMAND|QUERY|MACRO`) и очередной раунд **не сделал прогресса** — не запустил инструмент и лишь выдал подавленный `speak` — ход завершается немедленно (как `nothing_to_do`). Подавленный `speak` ничего не озвучивает, поэтому раннее завершение ничего не теряет; это страховка от петли подавленного `speak`, которая иначе крутила бы полноразмерные раунды до `MAX_TOOL_ROUNDS`. Раунд, исполнивший инструмент (включая `search_in_memory`/`remember`) или озвучивший `speak` на разговорном ходу, считается прогрессом и не завершает цикл.
(Тот же `recordOutcome` исполняет и подтверждённый dangerous-набор, §5.3.)

---

### §5.2. EVENT и NARRATION flows

Сырое событие (memory-only, без ЛЛМ):

```text
BaseEvent → GameEventFilter → ThoughtDispatcher → EventThought
→ HIGH ? write [EVENT] under event-type topic : drop
→ end   (ЛЛМ и речь не задействованы)
```

Курируемая наррация (ЛЛМ фразирует):

```text
SensorDataEvent → CompanionSensorDataBridge → NarrationThought
→ PromptComposer (lean narration prompt, NARRATION cache profile) → LlmGateway → один раунд
→ взять speak → озвучить + write [COMPANION] under provided topic
→ end   (currentInput/сырые данные в память не пишутся)
```

Курируемое объявление (дословно, без ЛЛМ):

```text
AnnouncementEvent → CompanionAnnouncementBridge (toggle) → VerbatimNarrationThought
→ write [COMPANION] under topic → озвучить дословно → end
```

Событийная сторона не может вызывать game input и не двигает глобальную тему.

---

### §5.3. Dangerous COMMANDER flow

```text
COMMANDER thought
→ LLM returns tool-call set with dangerous action
→ Thought freezes whole set
→ writes awaiting_confirmation to memory
→ speaks confirmation_request
→ waits ConfirmEvent
```

Confirm:

```text
→ unfreeze
→ execute full set in original order
→ write outcome
```

Timeout/interrupted:

```text
→ discard frozen set
→ write timed_out/interrupted/cancelled
→ end
```

---

### §5.4. Invalid LLM flow

```text
LlmGateway receives invalid model response
→ retry/repair once if token cost below threshold
→ if still invalid: INVALID_RESPONSE
```

COMMANDER:

```text
→ currentInput saved as unresolved_commander_input
→ speech: cannot execute
→ diagnostics
→ end
```

Invalid-flow касается только ЛЛМ-мыслей. `NarrationThought` best-effort: на невалидном/прерванном раунде просто молчит (ничего не пишет). `EventThought`/verbatim ЛЛМ не зовут — invalid-пути у них нет.

---

### §5.5. Interrupt flow

Urgent thought arrives (narration рождается urgent):

```text
→ placed first in its lane
→ all live thoughts interrupted (во всех lane)
```

Interrupted thought:

```text
→ CommanderThought: safe-flush; короткие event/narration просто завершаются
→ cancel handles (in-flight LLM future)
→ no new LLM/query/action/speech
→ end
```

Queued cancelled requests:

```text
→ skipped
```

In-flight cancelled requests:

```text
→ result discarded
→ diagnostics
```

---

## §6. Переиспользуемое vs новое

### §6.1. Переиспользуем

* EventBus / existing event infrastructure.
* `SystemSession` settings.
* STT/PTT: existing STT path and `UserInputEvent`.
* TTS: existing TTS/Mouth/Vocalisation infrastructure behind SpeechGateway.
* Journal/status events: `BaseEvent`, journal parser, status/session managers.
* Command model:

    * built-in commands;
    * queries;
    * command catalog;
    * self-describing command/tool schemas.
* Sequence-first input:

    * `GameInputSequenceEvent`;
    * `GameInputStep`;
    * `InputSequenceExecutor`.
* Existing command execution internals where possible.
* Existing provider clients where possible (`MistralClient`, local OpenAI-compatible transports).

---

### §6.2. Новое

* `companionModeOn` gate.
* `EventFilter` (`GameEventFilter`).
* `ThoughtDispatcher` (lane на источник, `EnumMap`) + `ThoughtLane`.
* `Thought` (abstract) + `CommanderThought` / `ReflexThought` / `EventThought` / `NarrationThought` / `VerbatimNarrationThought`.
* рефлекс-гейт: `ReflexResolver` (`submitCommanderInput`: дословная безопасная беспараметрная команда → `ReflexThought` без ЛЛМ).
* curated-narration мосты: `CompanionSensorDataBridge` (ЛЛМ) / `CompanionAnnouncementBridge` (verbatim).
* `ToolAccessPolicy` (`IntelActionAccessPolicy`, источник → категории; `NARRATION` → пусто).
* `SystemToolProvider`.
* `DangerousActionPolicy` / `ActionSafetyClassifier`.
* `ToolCallValidator` / exact tools snapshot validation boundary.
* Updated `Reducer` usage with allowed tool categories.
* `PromptComposer` for companion consciousness mode.
* `LlmGateway` with consciousness/compression modes, request handles, cancellation, invalid-response retry.
* `ExecutionModule` request facade with action/query lanes.
* `SpeechGateway` request facade with handles/cancellation.
* Confirmation bus / `ConfirmEvent`.
* `SystemNotificationPolicy` for system speech severity/defer rules.
* Memory model:

    * short-term memory;
    * mid-term topic memory;
    * long_term_summary;
    * llm_memory.
* `MemoryGateway`.
* `MemoryConsolidator`.
* Topic enum + descriptions.
* Verbosity slot.
* Component-level diagnostics for orphaned/cancelled requests.

---

## §7. Открытые вопросы

### §7.1. Под read-only разведку кода

* Где лучше поставить `companionModeOn` gate на текущей EventBus-схеме.
* Какие `BaseEvent` типы пропускать через `EventFilter`.
* Какие event types считать urgent.
* Какие voice phrases считать urgent commander phrases.
* Какой существующий input module подходит для `ConfirmEvent` button/key.
* Как расширить текущий command/query catalog так, чтобы Reducer получал категории `QUERY`, `ACTION`, `MACRO`.
* Где лучше разместить `ToolAccessPolicy`, `SystemToolProvider`, `PromptComposer`.
* Как текущий Mistral/local provider код лучше обернуть в `LlmGateway`.
* Как текущую TTS цепочку лучше спрятать за `SpeechGateway`.

---

### §7.2. Настройки, значения уточнить позже

* confirmation timeout.
* confirmation cancel phrases.
* system notification severity/defer policy.
* event speech / commentary verbosity policy.
* global thought watchdog timeout.
* invalid-response retry token threshold.
* short-term memory max entries.
* short-term memory token budget.
* mid-term max entries per topic.
* memory search return limit N (`search_in_memory`).
* consolidation buffer threshold.
* long_term_summary compact size limit.
* llm_memory fixed limits currently agreed:

    * max 15 entries;
    * max 50 characters per entry.

---

### §7.3. Отложено намеренно

* Persistent memory.
* Embeddings / semantic search for topic memory.
* Full RAG.
* GUI language/persona controls for companion mode.
* Class UML with exact signatures.
* Macro editor details.
* Detailed `.dot` graph update.
* Exact prompt wording.
* Exact provider fallback/health-check strategy.
* Exact policy for summary too long / malformed compression output.

---

## §8. UML — модули

Диаграмма: **`companion_module_graph.dot`**.

Цветовая идея остаётся прежней:

* переиспользуемые компоненты — existing layer;
* новый companion layer — new components;
* приватные внутренности шлюзов/памяти — hidden internals;
* пунктир — event/async/background/system-notification flow.

Читается так:

```text
STT/PTT
→ UserInputEvent
→ companionModeOn gate
→ ThoughtDispatcher
→ COMMANDER thought

Journal/Status events
→ companionModeOn gate
→ EventFilter
→ ThoughtDispatcher
→ EVENT thought

Thought
→ PromptComposer
→ LlmGateway
→ tool-calls

Thought
→ ExecutionModule
→ game actions / queries / system functions

Thought
→ MemoryGateway
→ short-term / mid-term / long_term_summary / llm_memory

Thought
→ SpeechGateway
→ TTS

MemoryConsolidator
→ LlmGateway(compression)
→ MemoryGateway(long_term_summary update)

System notifications
→ SpeechGateway

BargeInController
→ SpeechGateway interrupt
→ ThoughtDispatcher interrupt
```

Особые границы для диаграммы:

* `LlmGateway` принимает `LlmRequest`, не `Thought`.
* `SpeechGateway` принимает `SpeechRequest`, не `Thought`.
* `ExecutionModule` принимает `ExecutionRequest`, не `Thought`.
* `MemoryGateway` — единственная дверь к памяти.
* `PromptComposer` собирает prompt, но не решает tool access.
* `Reducer` не знает origin мысли.
* `ToolAccessPolicy` знает origin и выдаёт категории.
* `SystemToolProvider` выдаёт системные функции по origin.
* `EventFilter` не пишет память и не определяет urgency.
* `EVENT thought` не может получить action/macro tools.

---

## §9. Scenario review / grill-checkpoint для v0.13

v0.13 основана на прогоне правдоподобных сценариев Elite Dangerous:

1. простая безопасная голосовая команда;
2. обычное игровое событие без action;
3. событие + read-only query;
4. commander query + поиск в памяти (`search_in_memory`);
5. dangerous action + confirmation;
6. urgent event interrupt;
7. barge-in во время TTS;
8. invalid / malformed LLM response;
9. memory eviction + consolidation failure;
10. EVENT thought пытается получить лишние права.

Итог review:

* базовый lifecycle `COMMANDER thought` / `EVENT thought` выдерживает сценарии;
* главные hard boundaries должны быть реализованы через exact tools snapshot, validation и ownership handles;
* `QUERY` и `SYSTEM_FUNCTION` являются trusted developer contracts, а не sandbox;
* dangerous confirmation intentionally pragmatic: код блокирует execution до `ConfirmEvent`, но spoken confirmation и текст вопроса опираются на короткий линейный human context;
* memory является best-effort session memory, а не журналом/audit log;
* уже начатая input sequence не прерывается, чтобы не оставить игру в неизвестном состоянии.

### §9.1. Hard boundaries после review

* `EVENT thought` не получает `ACTION`/`MACRO` tools.
* Retry использует original immutable tools snapshot.
* Invalid response не исполняется частично.
* `classify_turn` из invalid response не применяется.
* `LlmGateway` не callback'ает в `Thought` и не route'ит results в `ExecutionModule`.
* Только owning `Thought` может consume LLM future/handle и превратить result в tool-calls.
* `MemoryConsolidator` не использует consciousness pipeline и не получает tools.
* `ExecutionModule` не пишет в память.

### §9.2. Trusted developer contracts

* `QUERY` tools не делают game input и не меняют game/session state.
* `SYSTEM_FUNCTION` tools не оборачивают gameplay actions/macros.
* Tool/category registries должны быть корректно размечены.
* Нарушение этих контрактов считается bug/review failure.

### §9.3. Accepted risks

* Confirmation identity слабая: `ConfirmEvent` подтверждает текущую thought в `awaiting_confirmation` внутри короткого окна.
* Confirmation request text генерируется LLM и не проходит semantic validation кодом.
* Командир не должен спамить опасными приказами и подтверждениями.
* Уже стартовавшие actions/macros не прерываются urgent events.
* При failed consolidation теряется current consolidation buffer.

### §9.4. Implementation traps

* Не делать `Reducer.select(origin, ...)`: origin должен знать `ToolAccessPolicy`, не `Reducer`.
* Не делать retry через новый `PromptComposer` call.
* Не добавлять callback continuation из `LlmGateway`, который может пережить owning thought.
* Не классифицировать UI-reading tools как `QUERY`, если они нажимают кнопки.
* Не добавлять gameplay action в system-function registry.
* Не писать обычный `nothing_to_do()` в memory timeline как отдельный successful event.
* Не давать compression requests равный приоритет с urgent consciousness requests.

---

## §10. Реализация: имена и пакеты (Фаза 1)

Документ концептуальный (см. шапку: «имена уточняются по исходникам»). Ниже — соответствие концептов фактическим именам классов и раскладке пакетов в коде (Фаза 1, скелет). При расхождении прозы выше и кода истина по именам — здесь и в исходниках.

### §10.1. Карта имён (концепт → класс)

| Концепт в документе | Класс в коде | Пакет |
|---|---|---|
| companionModeOn gate | `CompanionSubsystemGate` | `companion.input` |
| `EventFilter` | `GameEventFilter` | `companion.input` |
| curated-narration мосты | `CompanionSensorDataBridge` (ЛЛМ) / `CompanionAnnouncementBridge` (verbatim) | `companion.input` |
| origin мысли | `ThoughtSource` (COMMANDER/EVENT/NARRATION) | `companion.model` |
| вид мысли (один на источник) | `CommanderThought` / `ReflexThought` / `EventThought` / `NarrationThought` / `VerbatimNarrationThought` (abstract `Thought`) | `companion.mind` |
| `ThoughtDispatcher` / lane | `ThoughtDispatcher` / `ThoughtLane` | `companion.mind` |
| рефлекс-гейт (fast-path команды) | `ReflexResolver` | `companion.prompt` |
| `Topic` enum | `ConversationTopic` | `companion.model` |
| `ToolAccessPolicy` | `IntelActionAccessPolicy` | `companion.prompt` |
| tool category (QUERY/ACTION/MACRO) | `IntelActionCategory` | `companion.model` |
| `SystemToolProvider` | `SystemFunctionProvider` | `companion.tools` |
| системная функция | `SystemFunction` + `@RegisterSystemFunction` + `SystemFunctionRegistry` | `companion.tools` |
| `PromptComposer` / `ComposedPrompt` | те же | `companion.prompt` |
| `ExecutionModule` | `ExecutionGateway` | `companion.execution` |
| `LlmGateway` / `SpeechGateway` | те же | `companion.llm` / `companion.speech` |
| `MemoryGateway` (+ impl) | `MemoryGateway` / `SessionMemoryGateway` | `companion.memory` |
| `MemoryConsolidator` | `MidTermToLongTermConsolidator` | `companion.memory` |
| `ToolSpec` | `LlmToolDefinition` | `companion.model.llm` |
| `ToolCall` | `LlmToolInvocation` | `companion.model.llm` |
| message / `ChatMessage` | `LlmMessage` (+ `LlmMessageRole`) | `companion.model.llm` |
| `LlmRequest` / `LlmResult` | те же | `companion.model.llm` |
| `processing_state` | `MemoryProcessingState` | `companion.model.memory` |
| `MemoryEntry` / source | `MemoryEntry` / `MemorySource` | `companion.model.memory` |
| `ConfirmEvent` | `DangerousActionConfirmedEvent` | `companion.confirm` |

### §10.2. Раскладка пакетов

```text
elite.intel.companion
├─ CompanionRuntime     static access point to the running subsystem (gateways + reducer + state)
├─ model                ThoughtSource, Urgency, ConversationTopic, IntelActionCategory, Verbosity
│  ├─ llm               LlmMessage, LlmMessageRole, LlmToolDefinition, LlmToolInvocation,
│  │                    LlmRequest, LlmResult, PromptCacheProfile
│  ├─ speech            SpeechRequest
│  ├─ execution         ExecutionRequest
│  └─ memory            MemoryEntry, MemorySource, MemoryProcessingState
├─ input                CompanionSubsystemGate, GameEventFilter, EventTopicMap, BargeInController,
│                       CompanionSensorDataBridge, CompanionAnnouncementBridge
├─ mind                 Thought (abstract) + CommanderThought/ReflexThought/EventThought/NarrationThought/VerbatimNarrationThought,
│                       ThoughtDispatcher, ThoughtLane, UrgencyPolicy, ThoughtContext, CompanionState
├─ prompt               PromptComposer, ComposedPrompt, IntelActionAccessPolicy,
│                       CompanionActionReducer, WordOverlapActionReducer, GameToolCandidates, ReflexResolver
├─ tools                SystemFunction, RegisterSystemFunction, SystemFunctionRegistry, SystemFunctionProvider,
│                       IntelActionTypeResolver (id → COMMAND/QUERY/MACRO/SYSTEM/UNKNOWN),
│                       + the 6 system functions (speak, nothing_to_do, classify_turn, clarify,
│                         search_in_memory, change_verbosity), each an IntelAction (FindActionFunction retired, unregistered)
├─ llm                  LlmGateway, CompanionLlmGateway, ...
├─ speech               SpeechGateway, CompanionSpeechGateway
├─ execution            ExecutionGateway, CompanionExecutionGateway
├─ memory               MemoryGateway, MemoryAvailabilitySnapshot, SessionMemoryGateway,
│                       ShortTermMemory, MidTermTopicMemory, LongTermMemory, LlmMemory, MidTermToLongTermConsolidator
└─ confirm              DangerousActionConfirmedEvent
```

> **`CompanionRuntime` / `CompanionState`.** `CompanionRuntime` is the static install/clear access point so system-function `handle`s reach the gateways, the `CompanionActionReducer`, and the shared `CompanionState` (global `TopicModel` + `Verbosity`) — installed at subsystem start. `CompanionState` is a plain mutable holder that the `ThoughtDispatcher` will own as a field once it exists. There is a single global topic (no per-thought topic): `classify_turn` is COMMANDER-only and is an ordinary executed function whose `handle` writes `CompanionState.setGlobalTopic` (its `topic` param) and echoes the turn's `importance`; an EVENT thought never gets it (its memory topic comes from a static event-type map). `change_verbosity` likewise executes and writes `CompanionState` (`find_action` is retired and no longer registered). The only lifecycle-only signal left is `nothing_to_do` (turn terminator, intercepted by the `Thought`). `LlmMemory` and `MidTermTopicMemory` search are implemented, so `search_in_memory` is functional.

### §10.3. Уточнения механизмов (отличия от ранних разделов)

* **Шлюзы возвращают `CompletableFuture`, не handle/owner-token.** `LlmGateway` → `CompletableFuture<LlmResult>`, `SpeechGateway` → `CompletableFuture<Void>`, `ExecutionGateway` → `CompletableFuture<JsonObject>`. Отмена — `future.cancel(...)` (skip из очереди / discard результата); отдельного `CancellationToken` нет. Инвариант «только owning thought потребляет result» сохраняется: future держит сама мысль.
* **Один класс мысли на источник.** `Thought` — тонкая общая база (`composeInitialPrompt`/`submitRound`/`execute`/`recordCurrentInput`/`recordCompanionSpeech`/interrupt), **без цикла мышления**. `CommanderThought` владеет полным tool-calling-циклом и dangerous-confirmation; `NarrationThought` — один короткий ЛЛМ-раунд (фразирует `SensorDataEvent`); `VerbatimNarrationThought` — дословная озвучка announcement-текста без ЛЛМ; `EventThought` — memory-only (`HIGH` пишет в память, `NORMAL`/`LOW` — нет), ЛЛМ не зовёт и промпт не строит. Слова компаньона пишутся источником памяти `COMPANION` (сам текст, не `{status:spoken}`). `ThoughtDispatcher` держит lane на каждый `ThoughtSource` в `EnumMap`; командирский lane — bounded-пул на `MAX_LIVE_COMMANDER_THOUGHTS` (v0.16), event/narration — одиночные.
* **`mode` → `PromptCacheProfile`** {COMMANDER, NARRATION, COMPRESSION}. У каждого стабильный `cacheKey()` → Mistral `prompt_cache_key` (свой кэш-префикс на профиль). `EVENT` промпт не строит (memory-only), поэтому своего профиля не имеет; `NARRATION` несёт собственный лаконичный промпт (без topic enum / memory / safety). Признак «ждём tool-calls vs текст» выводится (consciousness vs COMPRESSION / `tools.isEmpty()`), отдельного флага нет.
* **`LlmRequest` = `(requestId, messages, tools, profile)`.** Список `tools` и есть immutable snapshot; `urgency` на запросе не нужен — приоритет/преемпция реализуются через interrupt на уровне `ThoughtDispatcher`.
* **`ExecutionRequest` = `(requestId, toolName, arguments)`.** Lane (action/query) выводится при резолве `toolName` по реестрам; `operationType` в запросе не передаётся.
* **`SpeechRequest` = `(requestId, text, urgency)`.** Различие conscious / system-notification — забота вызывающей стороны, поля `source` нет.
* **Tool-схема:** игровые tools строит companion-адаптер из существующих `IntelAction.id()/parameters()` (классы команд не зависят от companion); системные — из `SystemFunction`. Нейтральный носитель — `LlmToolDefinition` (имя, описание, локализованные тренировочные фразы из `AiActionLocalizations`, `ActionParameterSpec`); рендер в нативный JSON провайдера — в `LlmGateway`-bridge.
  * **Категории и видимость:** `IntelCommand` → `ACTION`, `IntelQuery` → `QUERY`, user macro → `MACRO`. В набор tools попадает любой action с `isVisibleForLLM(status) == true` — это автоматически отсекает неуместный в текущем контексте набор (например, on-foot команды, когда командир в корабле). Наличие локализованной фразы **не** является условием включения: при native tool-calling LLM выбирает tool по `name`/`description`/`parameters`, поэтому action без фразы остаётся доступен — он лишь хуже сопоставляется с иноязычной репликой. Companion-нерелевантные fallback-id старого пути (general-conversation, ignore-nonsensical, connection-check) не включаются.
  * **Описание игрового tool — авторская английская суть (`llmDescription`) + английские тренировочные фразы.** Описание для провайдера = `IntelAction.llmDescription()` (короткая английская фраза назначения) **плюс английские тренировочные фразы команды** (из английской alias-карты, `{key:…}`-аннотации срезаются) — конкретные образцы для сопоставления (`GameToolCandidates.appendEnglishPhrases`). Английские нарочно: схема английская, не-английский ввод модель сперва переводит на английский (см. language rule), и английское описание одинаково для всех языков → единый кэш-префикс. **Локализованные** фразы в описание не идут — они через `phraseKey` кандидата кормят только **редьюсер**. Системные функции описываются так же через `llmDescription()`; тренировочных фраз у них нет. Параметры: `examples`/`extractionHint` из `ActionParameterSpec` сворачиваются в `description` параметра JSON-схемы (`OpenAiCompatibleLlmAdapter`) — иначе модель их не видит (был баг: `target drive` уходил в `clarify`). Синтетический префикс «Game action `<id>`» убран.
* **System-prompt steering (`CompanionSystemPromptPart`).** Помимо контракта tool-calling, статический промпт несёт поведенческие правила (steering, не hard-enforcement): (1) **граундинг** — говорить только из результатов функций и памяти, не выдумывать факты (числа/имена/дистанции/статус); для того, что командир сообщил или что ты запомнил, — `search_in_memory`, для текущего состояния корабля/галактики — `query`-функция, при неоднозначности можно дёрнуть оба; (2) **no-fit** — если ни один offered-tool не подходит, не форсировать неуместный и **не делать вид, что выполнил** несуществующее действие, а `clarify` или честно сказать «не могу» и завершить `nothing_to_do`; (3) **вежливое закрытие** — если после проверки ответа/действия всё-таки нет, сказать об этом до конца хода, не «обещать проверить и замолчать»; (4) **язык** (`languageRule`) — если командир говорит не по-английски, понять запрос по-английски перед выбором функции (схема инструментов и фразы английские), извлекая аргументы по правилу каждого параметра (verbatim, где указано). Замер на отдельном пробнике подтвердил выигрыш на терсовых фразах (`цель двигатели`). Покрыто `CompanionSystemPromptPartTest`.
* **`CompanionActionReducer` → `WordOverlapActionReducer` (собственный отбор, не легаси `Reducer`).** Берёт actions из реестров через `GameToolCandidates`, получает `allowedToolCategories` (из `IntelActionAccessPolicy` по origin: `COMMANDER` → `QUERY/ACTION/MACRO`, `EVENT` → `QUERY` (memory-only, промпт не строит), `NARRATION` → пусто) и `currentInput`, отбирает по совпадению значимых слов реплики с локализованными тренировочными фразами кандидата (`phraseKey`) и возвращает `List<LlmToolDefinition>`. Алгоритм отбора:
  * **Сопоставление слов** — `CompanionWordMatch`: для флективных языков терпит окончания (одно слово — начало другого / общая основа / 1–2 правки Левенштейна, бюджет растёт с длиной), для аналитических (английский) — точное равенство; выбор по языку сессии (`ANALYTIC = {EN}`, всё прочее — fuzzy). Это чинит русские склонения (`навігація`/`навигации`, `ведомого`/`ведомый`), которые точное совпадение прячет.
  * **Стоп-слова** — служебные слова языка (`InputNormalizerLocalizations.stopWords()`, теперь заполнены для всех языков) и слова короче 3 букв отбрасываются.
  * **Вес по редкости (IDF)** — слово, встречающееся у многих команд, весит меньше; команда оценивается суммой весов совпавших слов, список ранжируется по баллу (общее «авианосцем» само по себе тащит слабо, «управление»+«авианосцем» — сильно).
  * **Отсечка и потолок** — отбрасываются команды с баллом ниже `KEEP_FRACTION` от лучшего (срезает «хвост семейства» от общего слова) и список ограничен `MAX_TOOLS`.
  * **Панель/инфо-буст** — на одиночном слове команды `show_*`/`display_*`/`query_*` получают небольшой бонус (по English-id, одинаково для всех языков): голое существительное чаще значит «покажи панель/инфо».
  Fallback-id (general-conversation, ignore-nonsensical, connection-check) не включаются. **Рефлекс не затронут** — `ReflexResolver` остаётся на точном совпадении всей фразы (fuzzy только в отборе кандидатов, не в рефлексе).
* **LLM provider seam:** провайдер-специфичный рендер/разбор — `LlmProviderAdapter`. Общий OpenAI-совместимый рендер/парсинг живёт в базовом `OpenAiCompatibleLlmAdapter`; тонкие per-provider impl'ы задают только модель, `tool_choice` и `prompt_cache_key`: `MistralLlmAdapter` (cloud — `any`, с cache key) и `LmStudioLlmAdapter` (local LM Studio — `required`, без cache key). Это бывш. `CompanionLlmDialect`/`MistralToolCallDialect`, переименованы. У `LlmGateway` две операции: `submit` (tool-calling сознания) и `compressMidTermMemory(LlmRequest) → CompletableFuture<String>` (текстовый ответ для сжатия памяти; адаптер даёт `parseText`, тело — тот же `buildRequestBody` с пустыми `tools`).
* **Long-term память реализована:** `LongTermMemory` (холдер), `MidTermTopicMemory.evictOverflow` (per-topic cap), `MidTermEvictionListener` (гейтвей отдаёт overflow, сам LLM не зовёт) и `MidTermToLongTermConsolidator` (буфер→порог→`compressMidTermMemory`→валидация `SUMMARY_MAX_CHARS`→atomic `replaceLongTermSummary`; провал → буфер потерян, summary цела, `SpeechGateway` system-notification). Все лимиты памяти — в `CompanionMemoryLimits`. Подключение listener'а к гейтвею — при bootstrap (`CompanionSubsystemGate`).
* **Итог tool-call по типу действия (`Thought.recordOutcome`, v0.15; скорректировано после отката `CommandOutcome`).** Тип резолвит `IntelActionTypeResolver` (`companion.tools`, инжектируемый тест-сим) → `COMMAND/QUERY/MACRO/SYSTEM/UNKNOWN`. Озвучка/память: `CommanderThought` даёт LLM-команде немедленный ack `affirmative()` перед `execute(inv)`, чтобы подтверждение не ждало handler; сам COMMAND остаётся самохозяином речи (handler снова self-narrating через старый voice path), а `recordOutcome` только пишет `[TOOL_RESULT]` «command id executed»+текст/описание и не добавляет поздний fallback-ack для пустого результата. QUERY — `text_to_speech_response` озвучивается и пишется в память как `[COMPANION]`; MACRO — молча, память «macro id executed»+описание; SYSTEM/UNKNOWN — речь и timeline не трогаем. Описание берётся из снапшота tools (`LlmToolDefinition.description()`), не из реестра. Команды/запросы синхронны (fire-and-forget откатан). `silentInCompanion()` удалён.
* **`MemoryProcessingState`** = `PROCESSED`, `UNRESOLVED`, `AWAITING_CONFIRMATION`, `CONFIRMED`, `CANCELLED`, `TIMED_OUT`, `INTERRUPTED`.

