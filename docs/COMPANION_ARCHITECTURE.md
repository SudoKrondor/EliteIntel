# COMPANION_ARCHITECTURE.md

Архитектура режима **«младший член экипажа» (компаньон)** для проекта **EliteIntel**.

**Компонентная карта** режима: концепция, решения, компоненты, потоки, границы ответственности и lifecycle-правила между ними.

Версия **v0.13**.

> **Статус.** Рабочая версия в разработке. Приоритет — за текущей проработкой; этот файл её догоняет, не наоборот.
> «Решение» = текущая согласованная картина, не застывший стандарт.

> **v0.13.** Версия после сценарного прогона и grill-review. Главная правка — честно разделены hard architectural boundaries, trusted developer contracts и accepted operator/gameplay risks.

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
* получает отфильтрованные игровые события;
* вызывает функции для действий и чтения данных;
* помнит ход сессии;
* комментирует игровые события, если это разрешено режимом болтливости/срочности;
* не играет сам за командира по событиям.

Основные идеи:

* **Режим-замена.** Компаньон заменяет старый command mode, а не работает параллельно с ним. Активен один режим за раз.
* **Два входных потока.** Реплики командира и игровые события оба могут рождать мысли.
* **Разные права у разных мыслей.** Мысль от командира и мысль от игрового события имеют общий lifecycle, но разные права на tools.
* **Память сессии.** В пределах процесса. Персистентная память — будущий отдельный трек.
* **Сознание — единственный умный узел.** Остальные компоненты — механика, шлюзы, исполнители, фильтры, очереди и хранилища.
* **Tool-calling only.** В контуре сознания ответ LLM должен быть function/tool call. Свободный текст считается невалидным результатом.
* **Опасное подтверждается кодом.** Dangerous actions никогда не исполняются только потому, что LLM так решила.

### §0.1. Типы гарантий

В документе различаются три уровня правил.

**Hard architectural boundary** — граница, которую должен обеспечивать runtime/lifecycle:

* `EVENT thought` не получает `ACTION`/`MACRO` tools;
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

5. **Есть два origin мысли.**

    * `COMMANDER` — мысль от реплики командира.
    * `EVENT` — мысль от игрового события.

6. **Одновременно живыми могут быть максимум две мысли.**

    * максимум одна `COMMANDER thought`;
    * максимум одна `EVENT thought`.

7. **Каждая новая мысль при рождении получает:**

    * `origin`;
    * `urgency`;
    * `topic = PENDING`;
    * `currentInput`.

8. **`currentInput` не пишется сразу в память.**
   Это текущий вход мысли, а не прошлое. Он передаётся в `PromptComposer` отдельно и пишется в память только после разрешения темы или fallback.

---

### §1.3. Права COMMANDER thought

`COMMANDER thought` может:

* выполнять built-in commands;
* выполнять user macros;
* выполнять read-only queries;
* использовать полный commander-набор системных функций;
* вызывать `set_topic`;
* менять global `TopicModel`;
* вызывать `remember`;
* вызывать `recall`;
* менять болтливость;
* уточнять;
* искать действие.

---

### §1.4. Права EVENT thought

`EVENT thought` может:

* выполнять read-only queries;
* вызывать `set_topic`, но только для темы самой мысли;
* вызывать `speak`, если это разрешено срочностью/болтливостью;
* вызывать `silence`.

`EVENT thought` не может:

* выполнять игровые действия;
* выполнять user macros;
* получать action/macro tools в prompt;
* вызывать `remember`;
* вызывать `recall`;
* вызывать `clarify`;
* вызывать `find_action`;
* вызывать `change_verbosity`;
* менять global `TopicModel`.

`EVENT thought` может получить только те `QUERY` tools, которые по implementation contract являются технически read-only.
Если tool выполняет input, публикует `GameInputSequenceEvent`, вызывает input execution layers, двигает game UI или меняет состояние игры/сессии, это не `QUERY`, а `ACTION`/`MACRO` либо implementation bug.

Запрет action/macro tools для `EVENT thought` — **code-level enforcement**, а не prompt-инструкция.
LLM в событийной мысли физически не получает такие tools.

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
    Даже валидный `set_topic` из response, где есть другой invalid tool-call, не применяется.

16. **Repair/retry не пересобирает tools.**
    Retry использует исходный request payload / tools snapshot и тот же cancellation/owner token.
    `LlmGateway` не вызывает `PromptComposer`, `Reducer`, `ToolAccessPolicy` или `SystemToolProvider`.

17. **`set_topic` из первого валидного response обрабатывается до non-topic tool-calls.**
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

29. **Срочная мысль становится в голову своей очереди и прерывает обе живые мысли.**
    Независимо от origin срочной мысли.

30. **Interrupt не должен создавать дыру в памяти.**
    Перед смертью мысль делает `safe-flush`.

31. **Командирская мысль при interrupt умирает сразу.**

32. **Событийная мысль при interrupt гарантирует, что событие/currentInput попало в память.**

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

38. **Команды и макросы выполняются строго последовательно.**

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

    * `COMMANDER`;
    * `EVENT`;
    * `TOOL_RESULT`;
    * `SYSTEM`.

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

#### Событийный вход

```text
Journal / Status / Game events
→ EventBus
→ companionModeOn gate
→ EventFilter
→ ThoughtDispatcher
→ EVENT thought
```

Если `companionModeOn = false`, companion event flow не активен.

---

### §2.2. EventFilter

`EventFilter` — механический компонент.

Он:

* получает игровые события;
* отсекает шум;
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

### §2.3. ThoughtDispatcher

`ThoughtDispatcher` — учётно-распорядительный узел сознания.

Он не думает и не интерпретирует смысл.

Он знает:

* commander queue;
* event queue;
* максимум одну live commander thought;
* максимум одну live event thought;
* urgency каждой мысли;
* origin каждой мысли.

Он умеет:

* создать мысль;
* поставить мысль в очередь;
* срочную мысль поставить первой;
* при срочной мысли отправить interrupt обеим живым мыслям;
* запустить следующую мысль соответствующего origin, если live-slot свободен;
* аварийно остановить мысль по общему watchdog timeout.

Он не знает:

* находится ли мысль внутри `awaiting_confirmation`;
* какие tool-calls внутри мысли;
* какие LLM requests у мысли;
* какие speech/execution handles у мысли;
* что именно мысль хранит в local messageFlow.

---

### §2.4. Thought

`Thought` — единица работы сознания.

Каждая мысль имеет:

```text
origin = COMMANDER | EVENT
urgency = normal | urgent
topic = PENDING | Topic
currentInput
localMessageFlow
request handles
```

`currentInput`:

* для `COMMANDER` — реплика командира;
* для `EVENT` — текст/summary игрового события.

`currentInput` не является memory entry до topic resolution.

---

### §2.5. Topic resolution

При старте мысли:

```text
topic = PENDING
```

Первый LLM turn должен либо вызвать `set_topic`, либо тема будет выставлена fallback-правилом.

Перед исполнением любых non-topic tool-calls весь tool-call set сначала валидируется целиком.
Если response валиден, `set_topic` из первого turn обрабатывается как pre-execution step, даже если LLM вернула его не первым в списке.
Если response invalid, `set_topic` из него не применяется.

#### COMMANDER thought

Если `set_topic(validTopic)`:

```text
thought.topic = validTopic
global TopicModel = validTopic
```

Если `set_topic(unknownTopic)`:

```text
игнорировать
diagnostics по желанию
```

Если LLM не вызвала `set_topic`:

```text
thought.topic = current global TopicModel
```

#### EVENT thought

Если `set_topic(validTopic)`:

```text
thought.topic = validTopic
global TopicModel не меняется
```

Если `set_topic(unknownTopic)`:

```text
игнорировать
diagnostics по желанию
```

Если LLM не вызвала `set_topic`:

```text
thought.topic = unresolved_game_event
```

`EVENT thought` никогда не меняет global `TopicModel`.

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
2. topic = PENDING
3. initial LLM turn resolves topic
4. currentInput записывается в память
5. tool-calls выполняются
6. tool results пишутся в память отдельно
```

Это даёт честный порядок памяти:

```text
[COMMANDER] requested action
[TOOL_RESULT] action result
```

или:

```text
[EVENT] event happened
[TOOL_RESULT] query result
```

---

### §2.7. Safe-flush при interrupt

При interrupt мысль не начинает новых действий. Она делает только safe-flush.

Safe-flush:

1. Если `currentInput` ещё не записан:

    * если topic resolved → писать с resolved topic;
    * если topic = PENDING:

        * `COMMANDER` → `unresolved_commander_input`;
        * `EVENT` → `unresolved_game_event`;
    * `processing_state = interrupted_before_topic_resolution`.

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

COMMANDER thought после этого умирает.
EVENT thought после этого умирает.

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
Никакие tool-calls из такого response не применяются, включая валидно выглядящий `set_topic`.

Если retry не выполняется или не помогает:

```text
INVALID_RESPONSE
```

#### Реакция Thought на INVALID_RESPONSE

COMMANDER thought:

```text
currentInput → MemoryGateway
topic = unresolved_commander_input
processing_state = unresolved_due_to_llm_error
SpeechGateway → служебная фраза “не могу выполнить”
diagnostics
Thought ends
```

EVENT thought:

```text
currentInput → MemoryGateway
topic = unresolved_game_event
processing_state = unresolved_due_to_llm_error
diagnostics
Thought ends silently
```

Unresolved-записи идут обычным путём памяти.

---

### §2.10. PromptComposer

`PromptComposer` — тупой укладчик `messages + tools`.

Он не решает:

* какие tools разрешены;
* какие команды релевантны;
* можно ли event thought выполнять action;
* как описывать каждую команду.

Он получает уже готовые данные:

```text
short-term memory timeline
currentInput
origin
urgency
global TopicModel
thought topic / PENDING
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

Он получает origin мысли и возвращает:

```text
allowedToolCategories
```

Для `COMMANDER`:

```text
QUERY
ACTION
MACRO
```

Для `EVENT`:

```text
QUERY
```

Это permission только на trusted read-only `QUERY` tools.
Корректная классификация query/action/macro — implementation contract.

#### Reducer

`Reducer` не знает про `COMMANDER/EVENT`.

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
silence
clarify
remember
recall
find_action
set_topic
change_verbosity
```

EVENT system tools:

```text
speak
silence
set_topic
```

`EVENT speak` должен быть gated политикой болтливости/срочности.
Предпочтительный вариант: если `EventSpeechPolicy` / `CommentaryPolicy` не разрешает речь, `speak` вообще не включается в EVENT tools; thought получает `set_topic` и `silence`.

Системные функции присутствуют в prompt только если разрешены для origin и текущей policy.
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

### §3.5. Recall topic memory

`recall(scope=topic_memory, topic=..., query=...)`:

* доступен только COMMANDER thought;
* topic обязателен;
* unknown topic → ignored / empty result / diagnostics;
* не читает всю обычную память;
* читает только mid-term topic memory одной темы;
* не читает short-term memory, потому что short-term уже вставлен в prompt;
* не читает `long_term_summary`, потому что summary уже вставлен в prompt;
* не читает `llm_memory`, для этого есть отдельный scope;
* если query не задан → последние/самые свежие N записей темы;
* если query задан → простой текстовый фильтр внутри этой темы;
* максимум N записей;
* N задаётся настройкой;
* без LLM;
* без embeddings на первом этапе.

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

`recall(scope=llm_memory)`:

* доступен только COMMANDER thought;
* не принимает topic;
* не принимает query;
* возвращает весь список llm_memory.

Почему можно вернуть всё: максимум 15 × 50 символов.

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
set_topic
recall(topic=...)
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
llm_memory:
7 / 15 remembered items available.
Use recall(scope=llm_memory) to load all.
```

Полное содержимое llm_memory не вставляется автоматически.

#### topic memory index

Показывает только темы, где реально есть mid-term memory.

Hints не генерируются LLM.
Они берутся из статических описаний Topic enum + дешёвой metadata из MemoryGateway.

Пример:

```text
topic memory available:
- navigation: jumps, routes, systems, docking, location changes
- trade: market, commodities, prices, cargo, profit
```

---

## §4. Исполнительные функции

### §4.1. System tools для COMMANDER thought

COMMANDER thought получает:

```text
speak
silence
clarify
remember
recall
find_action
set_topic
change_verbosity
```

#### `speak`

Озвучить текст через SpeechGateway.

Может иметь marker:

```text
confirmation_request
```

Только такой speak проходит сразу при frozen dangerous set.

#### `silence`

Закрыть ход без озвучки.

#### `clarify`

Задать уточнение командиру.

#### `remember`

Записать короткий факт в llm_memory.

#### `recall`

Читать:

```text
scope=llm_memory
scope=topic_memory
```

`llm_memory` — без topic/query.
`topic_memory` — topic обязателен, query опционален.

#### `find_action`

Поиск действия по каталогу.

#### `set_topic`

Для COMMANDER thought:

```text
thought.topic = validTopic
global TopicModel = validTopic
```

#### `change_verbosity`

Меняет режим болтливости.

---

### §4.2. System tools для EVENT thought

EVENT thought получает:

```text
speak
silence
set_topic
```

#### `speak`

Можно использовать только если:

* событие срочное;
* или режим болтливости допускает комментарий;
* или policy разрешает commentary для такого события.

Предпочтительно не выдавать `speak` в EVENT tools, если `EventSpeechPolicy` запрещает речь.
Если `speak` всё же присутствует всегда, executor обязан reject'ить EVENT speak, когда policy запрещает озвучку.

#### `silence`

Закрыть событийную мысль без озвучки.

#### `set_topic`

Для EVENT thought:

```text
thought.topic = validTopic
global TopicModel unchanged
```

EVENT thought не получает:

```text
remember
recall
clarify
find_action
change_verbosity
```

---

## §5. Tool execution flow

### §5.1. Normal COMMANDER flow

```text
UserInputEvent
→ ThoughtDispatcher
→ COMMANDER thought(topic=PENDING)
→ PromptComposer initial messages
→ LlmGateway
→ tool-calls
→ topic resolved
→ currentInput written to memory
→ execute tool-calls in order
→ tool results written to memory
→ tool results appended to local messageFlow
→ next LLM round
→ speak/silence/end
```

---

### §5.2. Normal EVENT flow

```text
BaseEvent
→ EventFilter
→ ThoughtDispatcher
→ EVENT thought(topic=PENDING)
→ PromptComposer initial messages
→ LlmGateway
→ tool-calls
→ topic resolved
→ currentInput written to memory
→ allowed query/system tools only
→ optional speak/silence
→ end
```

EVENT thought cannot cause game input.

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

EVENT:

```text
→ currentInput saved as unresolved_game_event
→ diagnostics
→ silent end
```

---

### §5.5. Interrupt flow

Urgent thought arrives:

```text
→ placed first in its queue
→ both live thoughts interrupted
```

Interrupted thought:

```text
→ safe-flush
→ cancel handles
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
* `EventFilter`.
* `ThoughtDispatcher`.
* `Thought`.
* `ToolAccessPolicy`.
* `SystemToolProvider`.
* `EventSpeechPolicy` / `CommentaryPolicy`.
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
* topic memory recall limit N.
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
4. commander query + recall памяти;
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
* `set_topic` из invalid response не применяется.
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
* Не писать обычный `silence()` в memory timeline как отдельный successful event.
* Не давать compression requests равный приоритет с urgent consciousness requests.

---

## §10. Реализация: имена и пакеты (Фаза 1)

Документ концептуальный (см. шапку: «имена уточняются по исходникам»). Ниже — соответствие концептов фактическим именам классов и раскладке пакетов в коде (Фаза 1, скелет). При расхождении прозы выше и кода истина по именам — здесь и в исходниках.

### §10.1. Карта имён (концепт → класс)

| Концепт в документе | Класс в коде | Пакет |
|---|---|---|
| companionModeOn gate | `CompanionSubsystemGate` | `companion.input` |
| `EventFilter` | `GameEventFilter` | `companion.input` |
| origin мысли | `ThoughtSource` (COMMANDER/EVENT) | `companion.model` |
| `Thought` / `ThoughtDispatcher` | те же | `companion.mind` |
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
├─ model                ThoughtSource, Urgency, ConversationTopic, IntelActionCategory, Verbosity
│  ├─ llm               LlmMessage, LlmMessageRole, LlmToolDefinition, LlmToolInvocation,
│  │                    LlmRequest, LlmResult, PromptCacheProfile
│  ├─ speech            SpeechRequest
│  ├─ execution         ExecutionRequest
│  └─ memory            MemoryEntry, MemorySource, MemoryProcessingState
├─ input                CompanionSubsystemGate, GameEventFilter
├─ mind                 Thought, ThoughtDispatcher, ThoughtContext
├─ prompt               PromptComposer, ComposedPrompt, IntelActionAccessPolicy
├─ tools                SystemFunction, RegisterSystemFunction, SystemFunctionRegistry, SystemFunctionProvider
├─ llm                  LlmGateway
├─ speech               SpeechGateway
├─ execution            ExecutionGateway
├─ memory               MemoryGateway, MemoryAvailabilitySnapshot, SessionMemoryGateway,
│                       ShortTermMemory, MidTermTopicMemory, LongTermMemory, MidTermToLongTermConsolidator
└─ confirm              DangerousActionConfirmedEvent
```

### §10.3. Уточнения механизмов (отличия от ранних разделов)

* **Шлюзы возвращают `CompletableFuture`, не handle/owner-token.** `LlmGateway` → `CompletableFuture<LlmResult>`, `SpeechGateway` → `CompletableFuture<Void>`, `ExecutionGateway` → `CompletableFuture<JsonObject>`. Отмена — `future.cancel(...)` (skip из очереди / discard результата); отдельного `CancellationToken` нет. Инвариант «только owning thought потребляет result» сохраняется: future держит сама мысль.
* **`mode` → `PromptCacheProfile`** {COMMANDER, EVENT, COMPRESSION}. У каждого стабильный `cacheKey()` → Mistral `prompt_cache_key` (свой кэш-префикс на профиль). Признак «ждём tool-calls vs текст» выводится (consciousness vs COMPRESSION / `tools.isEmpty()`), отдельного флага нет.
* **`LlmRequest` = `(requestId, messages, tools, profile)`.** Список `tools` и есть immutable snapshot; `urgency` на запросе не нужен — приоритет/преемпция реализуются через interrupt на уровне `ThoughtDispatcher`.
* **`ExecutionRequest` = `(requestId, toolName, arguments)`.** Lane (action/query) выводится при резолве `toolName` по реестрам; `operationType` в запросе не передаётся.
* **`SpeechRequest` = `(requestId, text, urgency)`.** Различие conscious / system-notification — забота вызывающей стороны, поля `source` нет.
* **Tool-схема:** игровые tools строит companion-адаптер из существующих `IntelAction.id()/parameters()` (классы команд не зависят от companion); системные — из `SystemFunction`. Нейтральный носитель — `LlmToolDefinition` (имя, описание, локализованные тренировочные фразы из `AiActionLocalizations`, `ActionParameterSpec`); рендер в нативный JSON провайдера — в `LlmGateway`-bridge.
* **`MemoryProcessingState`** сейчас = `PROCESSED`, `UNRESOLVED`; остальные состояния добавляются под их код-пути в следующих фазах.

