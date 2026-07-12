# COMPANION_ARCHITECTURE.md

Архитектура режима **«младший член экипажа» (компаньон)** для проекта **EliteIntel**.

**Компонентная карта** режима: концепция, решения, компоненты, потоки, границы ответственности и lifecycle-правила между ними.

Версия **v0.27**.

> **Статус.** Рабочая версия в разработке. Приоритет — за текущей проработкой; этот файл её догоняет, не наоборот.
> «Решение» = текущая согласованная картина, не застывший стандарт.

> **v0.27 (2026-07-12).** Уточнение обязательного аргумента стало типизированным межходовым состоянием.
> - **Не через `speak`:** COMMANDER-only `request_input(action_id, parameter_name, question)` — единственный способ открыть ожидание. Он добавляется в tool snapshot только когда хотя бы один offered game-tool имеет обязательный параметр. `CommanderThought` принимает его только для game-tool из точного offered snapshot и реально обязательного параметра, озвучивает/помнит вопрос и завершает thought без исполнения действия.
> - **Runtime-owned continuation:** `ClarificationCoordinator` хранит один `PendingClarification` максимум 60 секунд. Следующая непустая реплика атомарно claim'ит его; два хода не могут продолжить одну команду, stop очищает slot, а `speak`, новая команда или невалидный ход не переоткрывают уже claim'нутое состояние.
> - **Свежая видимость:** continuation несёт только стабильный action id, имя параметра, исходную команду и заданный вопрос. На новом ходу target заново резолвится через reducer catalog по свежему `GameStateSnapshot` и добавляется к обычным кандидатам новой фразы; исчезнувшее действие не возвращается. Prompt различает заполнение параметра, новую команду, отмену/смену темы и повторный `request_input`.

> **v0.26 (2026-07-12).** HTTP-результат LLM отделён от model/protocol parsing и от пользовательской речи.
> - **Типизированный transport outcome:** `AiTransportResult` различает success, transient, permanent, malformed 2xx response и cancellation. `400`/`401`/`403` не становятся обычным JSON-ответом модели и не входят в protocol repair.
> - **Один ограниченный resend:** только network/IO, `429` и `5xx` получают одну повторную физическую отправку с jitter `250–750 ms`; она использует тот же body/tools snapshot и остаётся внутри общего 50-секундного logical deadline. Ошибка malformed `2xx` остаётся model/protocol defect и получает обычный один repair.
> - **Без технической речи:** companion использует typed provider methods, а не legacy `sendJsonRequest`; поэтому низкоуровневый HTTP текст не публикуется как `AiVoxResponseEvent`. В речи остаётся обычный локализованный service-failure outcome, детали — только в diagnostics.

> **v0.25 (2026-07-11).** Жизненный цикл речи принадлежит активному Mouth и коррелируется с конкретной заявкой.
> - **Один owner speaking-state:** каждый `VocalisationRequestEvent` несёт `VocalisationHandle`; ровно один подходящий запущенный Mouth синхронно claim'ит его перед постановкой в очередь. Только handle публикует `IsSpeakingEvent`, причём лишь на переходах общего счётчика `0→1` и `1→0`, поэтому завершение реплики A не сообщает тишину, пока заявка B ещё активна.
> - **Каждая заявка завершается:** Kokoro и Google проводят один handle через synthesis/playback очереди и закрывают его при успехе, адресной/общей отмене, blank после sanitization, ошибке synthesis/audio/playback и stop. Guava обрабатывает вложенные публикации в очереди того же потока, поэтому отсутствие claim проверяется только после завершения внешнего EventBus-cycle через `GameEventBus.afterCurrentDispatch`; лишь тогда future заявки без активного Mouth завершается ошибкой. `SpeechRequest` запрещает blank text/id и null urgency.
> - **STT всегда активно:** `IsSpeakingEvent` обозначает TTS lifecycle и служит для обнаружения barge-in/диагностики, но не выключает распознавание и не отбрасывает обычные transcripts. Любая распознанная команда во время речи публикует один `BargeInEvent`, затем идёт обычным `UserInputEvent`; `BargeInController` единолично расщепляет сигнал на один TTS interrupt и thought interrupt. Защита от акустического echo — эксплуатационная (наушники), не программное подавление команд.

> **v0.24 (2026-07-11).** Нормальный COMMANDER tool flow сведён к одному физическому LLM-ответу.
> - **Один assistant message:** `CommanderPrompt` требует вернуть `classify_turn` и ровно один settling call вместе, в этом порядке; ждать результат metadata-only классификации запрещено.
> - **Симметричный совместимый fallback:** `CompanionLlmGateway` принимает односторонний provider deviation — либо голый `classify_turn`, либо ровно один offered settling call — и достраивает отсутствующую половину отдельным pending-continuation. До полной пары ничего не исполняется, а наружу всегда возвращается порядок `classify_turn` → settling call.
> - **Диагностика:** repair и оба continuation показывают полученные tool-calls, поэтому различимы provider deviation, неверный выбор функции и malformed response.
> - **Измерение на LM Studio:** до изменения 44–46 из 57 LLM-ходов требовали continuation. После one-message prompt и симметричного fallback три uncached-прогона `NaturalSpeechIntegrationTestEN` дали 339/339 каждый: classify-only = 5/8/7, settling-only = 1/1/1, repair = 0/0/0, invalid = 0/0/0. Это 63/66/65 физических LLM-вызовов вместо прежних 101–103.

> **v0.23 (2026-07-11).** Runtime компаньона стал одной атомарно публикуемой и полностью закрываемой generation.
> - **Один runtime graph:** `CompanionRuntimeGraph` содержит gateways, память, reducer/state, narrator, dispatcher и фоновые memory workers. `CompanionRuntime` публикует одну ссылку через `AtomicReference`, а identity-safe uninstall старой generation не может снять новую.
> - **Транзакционный lifecycle:** `CompanionSubsystemGate` сначала полностью собирает и запускает graph, затем атомарно публикует его и последними регистрирует входные subscriber'ы. Любая ошибка выполняет rollback уже созданных ресурсов. Stop сначала отрезает intake/uninstall, затем в обратном порядке закрывает dispatcher, memory workers, execution lanes и LLM executor.
> - **Generation fencing:** `CompanionRuntimeGeneration` запрещает старой мысли, narrator, compression/consolidation completion и gateway-result публиковать речь/память после restart. `ExecutionRequest.runtimeGenerationId` привязывает синхронный handler к owner-generation, поэтому старый handler не может обратиться через static runtime к новому graph. Уже начатая физическая игровая команда не прерывается, но её future/result больше не принадлежит остановленной generation.

> **v0.22 (2026-07-11).** LLM-cancellation теперь доходит до физического HTTP exchange и ограничена общим deadline.
> - **Адресная отмена:** возвращаемый `CompletableFuture` связан с конкретной `FutureTask`; `cancel(...)` прерывает именно её, а синхронная provider-обёртка отменяет удерживаемый `HttpClient.sendAsync` future.
> - **Один logical deadline:** 50 секунд покрывают ожидание в очереди, initial send, repair и classify continuation вместе. Это оставляет headroom до 60-секундного thought watchdog; timeout прерывает физический вызов и запрещает дальнейший retry/parse.
> - **Публичный контракт не расширен:** отдельного owner/cancellation token нет; owning thought по-прежнему владеет единственным future, а late result после гонки отмены отбрасывается.

> **v0.21 (2026-07-11).** Видимость игровых команд заморожена на один командирский turn.
> - **Один intake-snapshot:** `ThoughtDispatcher` один раз снимает `GameStateSnapshot(flags, flags2, fighterOut)` и передаёт тот же объект в точный `ReflexResolver`, `SemanticReflexResolver` и `ThoughtContext`.
> - **Один контекст до reducer:** `SemanticActionReducer` и его `WordOverlapActionReducer` fallback строят кандидатов из snapshot мысли, поэтому изменение `player_status` посреди маршрутизации не может дать разный набор команд разным стадиям одного turn. Диагностический `CommanderMatchInputChangedEvent` несёт тот же snapshot.
> - **Только routing, без второго gate:** snapshot определяет набор предложенных/распознанных tools. Перед фактическим исполнением команда намеренно не проверяется повторно по live status; новое состояние влияет на следующий turn.

> **v0.20 (2026-07-11).** Командирский поток получил строгий порядок без блокировки на долгих хендлерах.
> - **Один cognitive worker COMMANDER:** prompt → LLM → `classify_turn` → смена global topic → фиксация input идут строго в порядке intake. Медленный command/query/macro после этого detaches; `ThoughtLane` освобождает worker, но продолжает учитывать мысль как live/pending для `isIdle`, watchdog, barge-in и stop.
> - **Тема замораживается на turn:** поздний результат использует тему своей мысли и не читает уже изменившийся global topic. `lastCommanderMatchInput` остаётся только observer-snapshot для UI; reducer получает input из immutable `ThoughtContext`.
> - **Execution lanes:** commands/macros — один serialized action executor; read-only queries — bounded pool из 4 workers; короткие system functions исполняются на cognitive thread.
> - **Поздний outcome:** pending query/macro закрывает исходный turn маркером `<processing/>`; query CALL/RESULT записываются вместе после готовности результата. Interrupt/stop отменяет queued future, а результат уже стартовавшего хендлера отбрасывается без речи и памяти.

> **v0.19.** Событийная сторона переустроена: источников мысли теперь **два** — `COMMANDER` и `EVENT` (`NARRATION` удалён), а единственная дверь для игровых subscriber'ов — `CompanionNarrator`.
> - **`EVENT` поглотил наррацию и verbatim.** Классы `NarrationThought` / `VerbatimNarrationThought` / `VerbatimNarrationSink` удалены; их работу несёт один `EventThought` в двух режимах. **`EVENT` больше не memory-only** — его задача озвучить реакцию subscriber'а на игровое событие: в **narration**-режиме один короткий ЛЛМ-раунд (лаконичный narration-промпт) фразирует переданные (уже переваренные, не сырые) данные, озвучивает и пишет пару `user`→`[COMPANION]`; в **verbatim**-режиме готовая фраза озвучивается как есть без ЛЛМ, а `user`-ходом пишется короткий `sourceId` (не сырые данные). Если модель разбила ответ на два `speak`, озвучивается только **первый**. EVENT никогда не двигает глобальную тему — тег памяти = тема, переданная subscriber'ом.
> - **Удалён весь input/bridge/filter-слой:** `GameEventFilter`, `EventTopicMap`, `SensorInputFormatter`, `CompanionSensorDataBridge`, `CompanionAnnouncementBridge` и intake-событие `SensorDataEvent`. `CompanionSubsystemGate` больше **не** подписан на `BaseEvent` — сырые игровые события не доходят до компаньона напрямую. Гейта по важности события для компаньона больше нет. События участвуют в компаньоне только через своих gameplay-subscriber'ов.
> - **`CompanionNarrator`** (интерфейс в `elite.intel.companion`, статически через `CompanionRuntime.narrator()`; при остановленной подсистеме — `NO_OP`, поэтому subscriber зовёт безусловно) — единственная дверь. Три метода: `filler(text, urgent)` — одноразовая стартовая реплика прямо в `SpeechGateway`, без памяти и ЛЛМ; `narrate(data, instructions, topic)` — результат как данные+инструкции, один ЛЛМ-раунд фразирует, озвучивает и помнит парой `user`→`assistant` (→ `submitEventReaction` → EventThought narration); `announce(sourceId, phrase, topic, urgent)` — готовая фраза, озвучивается дословно без ЛЛМ и помнится, `sourceId` как `user`-ход (→ `submitEventVerbatim` → EventThought verbatim). Продакшн-реализация — `DispatcherCompanionNarrator` (обёртка над `ThoughtDispatcher` + `SpeechGateway`), входит в атомарно публикуемый `CompanionRuntimeGraph`. Тумблеры объявлений (`isMiningAnnouncementOn`, `isDiscoveryAnnouncementOn`, `isRouteAnnouncementOn`, `isRadarContactAnnouncementOn`) проверяются **в самом subscriber'е** до вызова наррратора.
> - **Инструменты по источнику:** `COMMANDER` → QUERY/ACTION/MACRO; `EVENT` → пусто (у реактивного события нет игровых инструментов — subscriber уже посчитал и отфильтровал данные). `IntelActionAccessPolicy` для `EVENT` отдаёт пусто. `speak` доступен для `COMMANDER` и `EVENT`.
> - **`EVENT` теперь строит промпт** (лаконичный narration-блок, профиль кэша `NARRATION`, ролевая история, только системный `speak`, данные события как current input). В реплеe истории стимул EVENT выдаётся тегированным `<event_data>` `user`-ходом (не ambient system-note), чтобы озвученный ответ читался как реакция. `PromptComposer` для `EVENT` → `composeNarration`.
> - **Команды.** `IntelCommand.execute(...)` возвращает `String` (озвучиваемый исход или null) вместо `void`; `handle` заворачивает непустой исход в `text_to_speech_response`. Командные ходы **теперь пишутся в память** (императив как `user`-ход, парой к озвученному ответу/ack). `Thought.recordOutcome` озвучивает исход команды/запроса напрямую — без `AiVoxResponseEvent` (это событие теперь только системное).
> - **`ExecutionRequest` на этом этапе = `(requestId, toolName, arguments, commanderInput)`** — компонент `toolCallId` и механизм thread-scoped `ActiveToolCall` **удалены**. В v0.23 добавлен отдельный `runtimeGenerationId`, не участвующий в CALL/RESULT pairing. Спаривание CALL/RESULT в памяти держит `ToolLink` (свой `toolCallId` внутри мысли через `recordCall`/`recordToolResult`) — это отдельный механизм.
> - **`VocalisationRouter`** озвучивает только системную речь (`AiVoxResponseEvent`, `MissionCriticalAnnouncementEvent`, radio, voice demo) во всех режимах; mining/discovery/route/radar/navigation-объявлений он больше не маршрутизирует (эти события удалены) и гейта `companionVoicesNarration()` у него нет.

> **v0.18 (2026-07-03).** Контекстное окно переведено на **нативные роли** вместо плоского `system`-блока «Visible context».
> - **История диалога — сообщения `user`/`assistant`/`tool`** (`PromptComposer.buildHistoryMessages`): реплики командира → `user`, ответы компаньона → `assistant`, а вызов инструмента модели → `assistant(tool_calls)` + парный `tool`-результат (реплей по `tool_call_id`, пары матчатся по id даже при несоседстве). Подряд идущие одинаковые роли истории коалесятся; текущий ход — отдельное лёгкое `user`-сообщение (обёртка `## Current input` и инъекция `current topic` убраны). `CompanionSystemPromptPart` описывает роли, а не «Visible context».
> - **Пары вызов/результат в памяти.** `MemoryEntry` получил `ToolLink` (CALL/RESULT); мысль пишет CALL (`recordCall`) и RESULT (`recordToolResult`), неся `toolCallId` внутри себя. (В v0.19 `toolCallId` вынесен из `ExecutionRequest`, а thread-scoped `ActiveToolCall` удалён — спаривание держит только `ToolLink`.) Маркер «command X executed» и хелперы `description`/`rememberAction` удалены (исход = связанный RESULT).
> - **Каждый ход командира записывается** (чтобы история чередовалась `user`/`assistant`); вопрос (`is_question`) принудительно `LOW`, поэтому не всплывает как факт-кандидат. Тему всегда возвращает `classify_turn` (по фразе + видимой истории); явная подсказка темы больше не подаётся.

> **v0.17 (2026-06-26).** Добавлен **рефлекс** — детерминированный fast-path для прямых команд, минующий ЛЛМ.
> - **`ReflexResolver`** (пакет `companion.prompt`) — гейт перед рождением мысли: даёт `id` команды, только если ввод **дословно** совпал с тренировочной фразой И нашлась **ровно одна** команда, **без параметров**, **видимая в intake-snapshot turn** и **не опасная**. Всё прочее (неоднозначное, параметризованное, опасное, не-дословное, не команда) → пусто и идёт обычным путём. Переиспользует `GameToolCandidates` (видимые команды + фразы + параметры), `AiActionLocalizations.splitPhraseGroup`, `DangerousActionPolicy` — без второй классификации.
> - **`ReflexThought`** (source `COMMANDER`, командирский lane) — короткий ход без ЛЛМ/промпта: записать реплику `[COMMANDER]` → выполнить команду → `recordOutcome` (та же per-type озвучка/память, что у `CommanderThought`). Без прерываний (стартовавшая команда не отменяется, §1.9.41).
> - **`ThoughtDispatcher.submitCommanderInput`** прогоняет `ReflexResolver`: совпало → `ReflexThought`, иначе → `CommanderThought`. Резолвер — коллаборатор диспетчера (как `UrgencyPolicy`), не поле `ThoughtDependencies`.
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
* реагирует на игровые события через их gameplay-subscriber'ов и **помнит** реакции;
* вызывает функции для действий и чтения данных;
* помнит ход сессии;
* озвучивает **реакции subscriber'ов на события** — что и когда сказать решает subscriber-слой (вызовом `CompanionNarrator`), не сознание;
* не играет сам за командира по событиям.

Основные идеи:

* **Режим-замена.** Компаньон заменяет старый command mode, а не работает параллельно с ним. Активен один режим за раз.
* **Два входных потока, два источника мысли.** Реплики командира → `COMMANDER`; реакции gameplay-subscriber'ов на игровые события → `EVENT`. Сырые события к сознанию напрямую не идут: их обслуживают subscriber'ы, которые зовут `CompanionNarrator`.
* **Один класс мысли на источник.** `CommanderThought` (полное рассуждение) **или** `ReflexThought` (детерминированный fast-path без ЛЛМ) для `COMMANDER`; `EventThought` для `EVENT`. `EventThought` несёт два режима — narration (один ЛЛМ-раунд фразирует переданные данные) и verbatim (дословная озвучка готовой фразы без ЛЛМ). Поведение несёт тип класса, а не ветки `if (origin)`.
* **Единая дверь для событий — `CompanionNarrator`.** Gameplay-subscriber зовёт `filler` (одноразовая стартовая реплика, без памяти), `narrate` (данные → ЛЛМ фразирует → память) или `announce` (готовая фраза → дословно → память). Решение «что и когда сказать» принимает subscriber, сознание только фразирует/озвучивает. Это устраняет «болтовню ЛЛМ по своему усмотрению».
* **Память сессии.** В пределах процесса. Персистентная память — будущий отдельный трек.
* **Сознание — единственный умный узел.** Остальные компоненты — механика, шлюзы, исполнители, очереди и хранилища. ЛЛМ работает в `COMMANDER` и в narration-режиме `EVENT`; verbatim-режим `EVENT` — детерминированный, без ЛЛМ.
* **Tool-calling only (в ЛЛМ-мыслях).** В `COMMANDER` и в narration-`EVENT` ответ LLM должен быть function/tool call; свободный текст невалиден. Verbatim-`EVENT` ЛЛМ не зовёт.
* **Опасное подтверждается кодом.** Dangerous actions никогда не исполняются только потому, что LLM так решила.

### §0.1. Типы гарантий

В документе различаются три уровня правил.

**Hard architectural boundary** — граница, которую должен обеспечивать runtime/lifecycle:

* `EventThought` получает **нулевой** набор game-tools в обоих режимах (subscriber уже посчитал и отфильтровал данные); в verbatim-режиме он к тому же не строит промпт и не зовёт ЛЛМ. Action/macro-tools физически доступны только `CommanderThought`;
* protocol repair и transient transport resend не пересобирают prompt/tools и используют исходный immutable tools snapshot;
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

1. **Компаньон — единственный command mode.**
   `CompanionSubsystemGate` всегда принимает командирский ввод; переключателя на старый `CommandEndPoint` нет.

2. **Компаньон получает два входа.**

    * STT/PTT → `UserInputEvent` → gate → `ThoughtDispatcher`;
    * реакция gameplay-subscriber'а на игровое событие → `CompanionNarrator` (статически через `CompanionRuntime.narrator()`) → `ThoughtDispatcher`.

3. **Сырые события к компаньону напрямую не идут.**
   `CompanionSubsystemGate` не подписан на `BaseEvent`; отдельного event-filter/importance-гейта для компаньона нет. Что (если вообще) делать с событием, решает его subscriber: он либо зовёт `CompanionNarrator` для озвучиваемой реакции, либо пишет знание своим путём.

4. **Срочность определяет `ThoughtDispatcher`.**
   Срочность ставится при рождении мысли механически:

    * голос — по шаблонам/матчерам срочных фраз;
    * реакция subscriber'а — по флагу `urgent` в вызове `CompanionNarrator` (напр. свежий radar-контакт).

---

### §1.2. Типы мыслей

5. **Есть два источника мысли (`ThoughtSource`).**

    * `COMMANDER` → `CommanderThought` (полный ЛЛМ-цикл) **или** `ReflexThought` (детерминированный fast-path без ЛЛМ для дословно распознанной прямой команды, v0.17). Какой из двух — решает `ReflexResolver` при рождении мысли (§2.3).
    * `EVENT` → `EventThought` — реакция gameplay-subscriber'а на игровое событие, в двух режимах: **narration** (один короткий ЛЛМ-раунд фразирует переданные данные → `speak`) и **verbatim** (дословная озвучка готовой фразы без ЛЛМ). Режим выбирает вызов `CompanionNarrator` (`narrate` / `announce`).

   `Thought` — абстрактная база (общие хелперы, исполнение и озвучка/память исхода `recordOutcome`, interrupt), цикла мышления она не содержит: его несёт каждый вид в своём `run()`.

6. **Сколько мыслей живёт одновременно — по cognitive/execution stage (v0.20):**

    * `COMMANDER` — один ordered cognitive worker; после dispatch несколько мыслей могут оставаться live на detached execution (actions последовательно, queries до 4 параллельно);
    * `EVENT` — одна (сериализованный lane, ёмкость 1).

   `ThoughtDispatcher` держит lane на источник в `EnumMap` (§2.3). Ordered cognitive worker сохраняет порядок тем/памяти, а detached execution не даёт долгому хендлеру блокировать следующий input. EVENT и COMMANDER остаются независимыми.

7. **Каждая новая мысль при рождении получает:**

    * `source`;
    * `urgency`;
    * `currentInput`.

   `CommanderThought`/`ReflexThought` замораживают per-turn topic до detached execution; `EVENT` получает фиксированную тему от subscriber'а.

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
* уточнять.

---

### §1.4. Права EVENT мысли

**`EventThought` озвучивает реакцию subscriber'а на игровое событие**, в двух режимах, и в обоих получает **нулевой** набор game-tools (ни команд, ни query): subscriber уже посчитал и отфильтровал данные.

**narration-режим (ЛЛМ-фразировка).** Subscriber отдал переваренные (не сырые) данные + инструкции по фразировке. `EventThought` строит лаконичный narration-промпт и получает единственную системную функцию `speak` (решение «озвучить» уже принято subscriber'ом). За один короткий раунд ЛЛМ фразирует данные в характере → `speak`; затем стимул пишется как `user`-ход, а произнесённая фраза как `[COMPANION]` (чистая пара `user`→`assistant`). Если модель разбила ответ на два `speak`, озвучивается только **первый**. Не вызывает `search_in_memory`/`classify_turn` и не двигает глобальную тему.

**verbatim-режим (дословно).** Subscriber отдал готовую фразу. `EventThought` не зовёт ЛЛМ и не строит промпт: пишет короткий `sourceId` как `user`-ход (не сырые данные), озвучивает фразу дословно и пишет её как `[COMPANION]`.

Тема event-мысли для записи в память берётся не от LLM, а из источника (переданная subscriber'ом тема); событийная сторона никогда не двигает глобальную тему разговора.

Запрет action/macro/query-tools для событийной стороны — **code-level enforcement**, а не prompt-инструкция: `IntelActionAccessPolicy` для `EVENT` отдаёт **пусто**; `CommanderThought` — единственный, кто получает `ACTION`/`MACRO`.

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
    Нормальный response по контракту prompt уже содержит `classify_turn` и settling call в одном
    `assistant(tool_calls)`. Односторонний ответ с ровно одним offered call — `classify_turn` без settling call
    или settling call без `classify_turn` — совместимый provider fallback, не repair и не execution. Gateway
    локально replay'ит полученный call как `assistant(tool_calls)`, возвращает модели protocol-only result с
    `execution=pending` и делает ровно один запрос только отсутствующей половины с тем же immutable snapshot.
    Если continuation вернул ровно один ожидаемый offered call, gateway отдаёт thought двухэлементную пару в
    каноническом порядке `classify_turn` → settling tool; ни одна функция до этого не выполнена. Несколько вызовов
    одного вида, unexpected call и любой другой ответ continuation остаются invalid response.

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
    Независимо от источника срочной мысли. (Срочная реакция события — по флагу `urgent`, см. §2.3.)

30. **Interrupt не должен создавать дыру в памяти.**
    `CommanderThought` перед смертью делает `safe-flush` (записывает ещё не сохранённый вход как `INTERRUPTED`).
    `EventThought` коротка и почти мгновенна: флашить нечего (verbatim ничего не ждёт; narration пишет пару `user`→`[COMPANION]` только по завершении раунда, а на прерванном раунде остаётся молчаливой).

31. **Командирская мысль при interrupt умирает сразу** после safe-flush.

32. **Событийная сторона при interrupt просто завершается** (verbatim либо уже озвучен, либо нет; прерванный narration-раунд молчит).

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

38. **Command/query detach после ordered cognitive stage (v0.20).**
    `CommanderThought` последовательно завершает prompt/LLM/classify/topic/input, затем отправляет game handler в `ExecutionGateway` и освобождает cognitive worker. `ThoughtLane` продолжает владеть detached completion: мысль остаётся live для watchdog/barge-in/stop и idle-accounting. Поздний outcome использует frozen topic своей мысли; после interrupt его речь и память отбрасываются.

39. **Read-only queries могут выполняться параллельно.**
    И друг с другом (bounded pool), и параллельно serialized commands/macros.

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

52a. **MAX-архив — отдельная область дословных «запиши» фактов (`LongTermMemory.pinnedFacts`).**
    При вытеснении из mid-term запись важности `MAX` пинится **дословно** (не сжимается в summary, не дропается). Архив **безлимитен в хранении** (каждое явное «запиши» хранится дословно и никогда не удаляется), но ограничены две вещи: его **доля в одной выдаче поиска** (квота) и его **след в prompt** — в prompt он **не вставляется always-on** (иначе рос бы безгранично и забивал контекст), а находится через `search_in_memory` наравне с mid-term/short-term. Чтобы накопленный архив не монополизировал выдачу поиска, действует две защиты (§3.5): ранжирование результатов **по релевантности в первую очередь** (важность — вторичный ключ) и **квота** на число архивных записей в одной выдаче (`ARCHIVE_RECALL_LIMIT`). Контракт MAX смягчён с «гарантированно в каждом prompt» до «гарантированно находится поиском, importance-ranked». Дубли при пине отбрасываются. «Дословно» — **в пределах лимита длины записи** (§1.10.52b): слишком длинная `MAX`-строка сжимается до сути ещё при записи, поэтому в архив дословно попадает уже сжатая версия.

52b. **Лимит длины одной записи памяти (защита промпта от раздувания).**
    Любая запись в память длиннее `CompanionConfig.memoryEntryMaxChars` (200 символов) **не кладётся сырой**: gateway отдаёт её обработчику (`OversizedMemoryListener` → `OversizedMemoryCompressor`), который на **отдельном выделенном executor** (как `MidTermToLongTermConsolidator`, не на thought-линии — чтобы не блокировать ни запись, ни озвучку) делает один plain-text ЛЛМ-раунд сжатия строки до сути, и короткий gist пишется назад с тем же источником/темой/важностью/временем. Молча (без озвучки). Применяется ко **всем** источникам и важностям, включая `MAX` (это защитный механизм, а не редактирование смысла). Хранение длинной записи поэтому **отложенное**: gist появляется по завершении сжатия, а при ошибке/всё ещё длинном результате/остановке подсистемы запись теряется (это был раздувающий мусор). Gateway сам ЛЛМ не зовёт — он делегирует обработчику.

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
→ CompanionSubsystemGate
→ ThoughtDispatcher
→ COMMANDER thought
```

#### Событийный вход — единственная дверь `CompanionNarrator`

Сырые события к компаньону напрямую не идут. Gameplay-subscriber реагирует на событие и зовёт `CompanionNarrator` (статически через `CompanionRuntime.narrator()`; при выключенной подсистеме — `NO_OP`, поэтому вызов безусловный). Три ветки:

`filler` — одноразовая стартовая реплика, прямо в TTS, без памяти и ЛЛМ:

```text
subscriber → CompanionRuntime.narrator().filler(text, urgent) → SpeechGateway
```

`narrate` — данные+инструкции, ЛЛМ фразирует (один раунд, speak), помнит пару `user`→`[COMPANION]`:

```text
subscriber → CompanionRuntime.narrator().narrate(data, instructions, topic)
→ ThoughtDispatcher.submitEventReaction → EventThought (narration)
```

`announce` — готовая фраза, дословно, без ЛЛМ, помнит `sourceId`→`[COMPANION]`:

```text
subscriber (проверил тумблер, напр. isMiningAnnouncementOn)
→ CompanionRuntime.narrator().announce(sourceId, phrase, topic, urgent)
→ ThoughtDispatcher.submitEventVerbatim → EventThought (verbatim)
```

Тумблеры объявлений (`isMiningAnnouncementOn`, `isDiscoveryAnnouncementOn`, `isRouteAnnouncementOn`, `isRadarContactAnnouncementOn`) проверяются в самом subscriber'е до вызова. Legacy `VocalisationRouter` этих объявлений больше не маршрутизирует (их события удалены); radio-трансмиссия остаётся на legacy-пути и в память не попадает.

---

### §2.2. Дверь событий: CompanionNarrator

Отдельного event-filter для компаньона больше нет. Единственная дверь от gameplay-subscriber'ов к сознанию — `CompanionNarrator` (интерфейс в `elite.intel.companion`, статически через `CompanionRuntime.narrator()`; при остановленной подсистеме — `NO_OP`, поэтому subscriber зовёт безусловно, без гейта на режим). Три метода — по одному на стадию реакции subscriber'а:

* **`filler(text, urgent)`** — одноразовая стартовая реплика (ack, пока идёт работа): прямо в `SpeechGateway`, **никогда не помнится, без ЛЛМ**;
* **`narrate(data, instructions, topic)`** — результат как переваренные (не сырые) данные + инструкции по фразировке: один ЛЛМ-раунд формулирует произнесённую строку, озвучивает и помнит обмен парой `user`→`assistant` (→ `submitEventReaction` → EventThought narration);
* **`announce(sourceId, phrase, topic, urgent)`** — результат как готовая фраза: озвучивается дословно (без ЛЛМ) и помнится, `user`-ходом идёт короткий `sourceId`, а не сырые данные (→ `submitEventVerbatim` → EventThought verbatim).

Продакшн-реализация — `DispatcherCompanionNarrator` (обёртка над `ThoughtDispatcher` + `SpeechGateway`), публикуется вместе с остальными компонентами в одном `CompanionRuntimeGraph`. Срочность реакции задаёт флаг `urgent` в вызове.

---

### §2.2.1. Кто и что решает по событию

Гейта по важности события для компаньона **нет**: сырые события к сознанию не доходят, поэтому нет ни `LOW → drop / NORMAL / HIGH`-фильтра, ни intake-формата события. Что делать с игровым событием, решает его gameplay-subscriber:

* нужна озвучиваемая реакция — subscriber зовёт `CompanionNarrator` (`filler` / `narrate` / `announce`), сам проверив свои тумблеры (напр. `isMiningAnnouncementOn`, `isRadarContactAnnouncementOn`) и payload-условия (напр. `ShipTargeted` — только для отсканированной wanted-цели);
* реакция помнится единообразно парой `user`→`[COMPANION]` под темой, которую передал subscriber (глобальную тему разговора она не двигает).

Детерминированную и критическую озвучку событий (топливо, кислород, скан груза, пиратский оклик, kill
confirmed и т.п.) по-прежнему владеет `EventNarrator`, который звучит во всех режимах.

---

### §2.3. ThoughtDispatcher

`ThoughtDispatcher` — учётно-распорядительный узел сознания.

Он не думает и не интерпретирует смысл.

Он знает:

* по одному `ThoughtLane` на каждый `ThoughtSource`, хранятся в `EnumMap` и публикуются одной volatile-ссылкой (commander / event); оба имеют по одному cognitive worker;
* набор живых мыслей на каждом lane: cognitive thought плюс detached completions, которые worker уже не удерживают;
* urgency каждой мысли;
* source каждой мысли.

Он умеет:

* создать мысль (`submitCommanderInput` / `submitEventReaction` / `submitEventVerbatim`); `submitCommanderInput` сперва прогоняет `ReflexResolver` (коллаборатор диспетчера, как `UrgencyPolicy`): дословно распознанная прямая команда → `ReflexThought` (без ЛЛМ), иначе → `CommanderThought`. Резолвер — детерминированная подстановка по фразам/реестрам, не интерпретация смысла. `submitEventReaction`/`submitEventVerbatim` кладут `EventThought` (narration/verbatim) на EVENT-lane; их зовёт `DispatcherCompanionNarrator` из `CompanionNarrator.narrate`/`announce`;
* поставить мысль в lane её источника;
* срочную мысль поставить первой;
* при срочной мысли отправить interrupt всем живым мыслям (во всех lane);
* запустить следующую cognitive stage, когда worker свободен, не дожидаясь detached handler предыдущей мысли;
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

`Thought` — **абстрактная база**, общая для всех видов: держит immutable `ThoughtContext` (`source`/`urgency`/сырой `currentInput`/канонический `matchInput`, командирский `GameStateSnapshot`, опциональный `SemanticQuery` и monotonic `acceptedAtNanos` только для latency diagnostics) и сервисный `ThoughtDependencies`; последний содержит gateways, состояние, политику безопасности и immutable owner-ссылку на `CompanionRuntimeGeneration`. `ThoughtContext` рождается в intake, живёт ровно один ход и не кэширует tools/facts или результаты ЛЛМ; snapshot хранит только входные сигналы видимости (`flags`/`flags2`/`fighterOut`). База несёт interrupt-механику (`interrupted` + `inFlight` + `interrupt()`), generation fence, `startLifecycle` для tracked detached completion и строительные блоки — `composeInitialPrompt`, `submitRound`, `submitExecution`, `recordCurrentInput`, `recordCompanionSpeech`, а также `recordOutcome` (+ `voice`/`recordCall`/`recordToolResult`). **Цикла мышления база не содержит**: его несёт каждый вид.

```text
Thought (abstract)
├─ CommanderThought       один LLM-раунд + dangerous-confirmation; ordered classify/topic/input → detached handler;
│                         frozen topic, recordOutcome, подавление LLM-speak
├─ ReflexThought          fast-path без ЛЛМ: freeze topic → detached parameterless handler → recordOutcome;
│                         рождается, когда ReflexResolver дословно распознал безопасную беспараметрную команду (§2.3)
└─ EventThought           реакция subscriber'а на событие, два режима:
   ├─ narration           run() = один раунд → взять первый speak → recordCurrentInput (user) + озвучить + записать [COMPANION]
   └─ verbatim            run() = recordCurrentInput (sourceId) + озвучить дословно + записать [COMPANION] (без ЛЛМ)
```

Поля общей мысли:

```text
source = COMMANDER | EVENT
urgency = normal | urgent
currentInput
gameStateSnapshot = flags + flags2 + fighterOut (COMMANDER)
```

> **Тема — не поле мысли (см. §2.5).** Для `COMMANDER` тег памяти — глобальная тема; для `EVENT` — переданная subscriber'ом тема. `CommanderThought` применяет `classify_turn` (тема + важность) как pre-execution шаг до записи реплики; до первого валидного ответа действует fallback `unresolved_*`.

`currentInput`:

* для `COMMANDER` — реплика командира;
* для `EVENT` narration — переваренные данные события (плюс инструкции по фразировке в LLM-видимом вводе);
* для `EVENT` verbatim — короткий `sourceId` (не сырые данные).

Для `CommanderThought` `currentInput` не является memory entry до topic resolution. `EventThought` в **обоих** режимах пишет `currentInput` как `user`-ход (стимул/`sourceId`) в паре с произнесённой фразой `[COMPANION]`; в narration-режиме — только по завершении раунда (прерванный раунд ничего не пишет).

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

EVENT-мысль **не** трогает глобальную тему и не вызывает `classify_turn` (его нет в её tools). Тема для записи берётся из значения, которое передал subscriber в `CompanionNarrator` (`narrate`/`announce`); неизвестное/пустое значение падает в `ConversationTopic.SYSTEM`. Это даёт честный тег памяти, не перебивая тему разговора командира.

```text
global TopicModel — без изменений
memory tag = переданная subscriber'ом тема   # fallback: SYSTEM
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
3. topic resolved: COMMANDER applies classify_turn to global topic, then freezes that topic + importance on the turn;
   EVENT uses the subscriber-supplied topic
4. currentInput записывается в память под разрешённой темой
5. system tool выполняется сразу; game handler dispatch'ится в свою execution lane, cognitive worker освобождается
6. поздний outcome пишется под frozen topic; pending query пишет CALL+RESULT вместе после готовности
```

Это даёт честный порядок памяти (`CommanderThought`):

```text
[COMMANDER] requested action
[COMPANION] <processing/>        # только пока detached query/macro ещё выполняется
[COMPANION/CALL] query call      # CALL и RESULT добавляются вместе после готовности
[TOOL_RESULT] query result
```

или у EVENT-реакции — чистая пара `user`→`assistant`:

```text
[EVENT] стимул (переваренные данные / sourceId)
[COMPANION] произнесённая в характере фраза
```

---

### §2.7. Safe-flush при interrupt (только CommanderThought)

Safe-flush — забота `CommanderThought` до dispatch. После dispatch owning thought остаётся live через detached future: queued execution отменяется, уже начатый handler может завершиться operationally, но его поздняя речь/память отбрасываются. `EventThought` короткий (verbatim мгновенный, narration — один раунд).

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
profile              # PromptCacheProfile: COMMANDER | NARRATION | COMPRESSION; задаёт Mistral prompt_cache_key
```

`LlmGateway` возвращает:

```text
CompletableFuture<LlmResult>
```

При interrupt мысль отменяет future (`future.cancel(...)`); gateway связывает его с конкретной физической задачей,
прерывает её и отменяет удерживаемый HTTP exchange. Отдельного публичного cancellation/owner token нет.

`LlmGateway` не знает, как пересобрать consciousness prompt.
Он не имеет доступа к `Thought`, `PromptComposer`, `Reducer`, `ToolAccessPolicy` или `SystemToolProvider`.
Repair/retry может использовать только исходный request payload / immutable tools snapshot.

Поведение gateway:

* queued cancelled request → удалить/пропустить;
* in-flight cancelled request → прервать физическую задачу и HTTP exchange;
* transport завершился в гонке с отменой → discard result, diagnostics;
* transient network/IO, `429` или `5xx` → одна повторная отправка с jitter; `400`/`401`/`403` → terminal `INVALID_RESPONSE` без protocol repair;
* malformed `2xx` body → обычный model/protocol repair, а не transport resend;
* один 50-секундный logical deadline покрывает очередь, все repair/continuation attempts и transient resend вместе;
* result cancelled request не попадает:

    * в Thought;
    * в ExecutionModule;
    * в SpeechGateway;
    * в MemoryGateway;
    * в TopicModel.

#### Transport failure

`LlmGateway` получает от provider transport не error-JSON, а `AiTransportResult`.
Только transient network/IO, `429` и `5xx` получают одну jittered повторную отправку; auth, authorization и request-shape
ошибки не повторяются и не превращаются в model repair. Низкоуровневый текст transport-ошибки не озвучивается.

#### Invalid response

Если модель вернула:

* plain text вместо tool-call;
* empty response;
* malformed tool-call;
* unknown tool;
* invalid arguments/schema;

то `LlmGateway` делает одну repair/retry попытку, но только если потраченный token cost ниже настроечного порога.
Retry использует тот же tools snapshot и тот же cancellation/owner token.

Ровно один offered `classify_turn` или settling call при ожидаемой двухэлементной COMMANDER-паре не считается
исполняемым частичным результатом: gateway держит call pending, запрашивает только вторую половину и лишь после неё
возвращает thought пару в порядке `classify_turn` → settling call.

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

`EventThought` narration (best-effort): на невалидном/прерванном раунде просто завершается молча, ничего не пишет. Verbatim-режим ЛЛМ не зовёт — INVALID_RESPONSE у него не бывает.

Unresolved-записи идут обычным путём памяти.

---

### §2.10. PromptComposer

`PromptComposer` — тупой укладчик `messages + tools`; ветвится по источнику: `COMMANDER` → `composeCommander` — полный промпт (persona + tool-calling + commander-rules + safety + language + topic enum + memory + timeline + current input, профиль `COMMANDER`); `EVENT` → `composeNarration` — лаконичный промпт (narration-блок + задача + language + ролевая история + данные события как current input, **без** topic enum/memory/safety, профиль кэша `NARRATION`, только системный `speak`). Verbatim-режим `EventThought` промпт не строит.

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
EVENT     → ∅               (нулевой набор: subscriber уже посчитал и отфильтровал данные)
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
search_in_memory
classify_turn
```

EVENT system tools (narration-режим):

```text
speak
```

`EVENT speak` без гейта: решение «озвучить» уже принял subscriber. `SpeakFunction.sources()` = `{COMMANDER, EVENT}`; остальные системные функции — COMMANDER-only. Verbatim-режим промпта не строит и системных функций не получает.

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
commanderInput   # сырая реплика командира (originalUserInput для хендлера); "" когда её нет
```

`operationType` (action/macro/query/system-function lane) выводится при резолве `toolName` по реестрам и в запросе не передаётся. `toolCallId` в `ExecutionRequest` **нет** (удалён в v0.19) — спаривание CALL/RESULT в памяти держит `ToolLink` внутри мысли.
Он возвращает `CompletableFuture<JsonObject>`.

#### Очереди

Commands/macros:

```text
serialized action lane
```

Read-only queries:

```text
bounded parallel query lane (4 workers)
```

Read-only query lane не означает “безопасно, потому что цель — узнать”.
`QUERY` tool технически не должен выполнять game input или изменять session/game state.

Короткие системные функции (`classify_turn`, `speak`) исполняются на caller/cognitive thread: они не должны ждать
remote I/O или занимать query pool. Они не превращают ExecutionModule в память/сознание.

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
> типу действия (`Thought.recordOutcome`, §5.1): COMMAND — озвучиваемый исход из `execute` (crit→urgent); QUERY —
> ответ; MACRO — молча (свои шаги). LLM-`speak` за ход с `COMMAND|QUERY|MACRO` подавляется. (2) реакция
> gameplay-subscriber'а через `CompanionNarrator` → `EventThought`: `narrate` (ЛЛМ фразирует) / `announce`
> (дословно), плюс `filler` (одноразовая реплика прямо в `SpeechGateway`, без памяти); (3) свободный `speak` LLM —
> только на разговорном ходу без игрового действия. Слова компаньона (свободный `speak`, ответ запроса,
> исход команды, реакция события) пишутся в память как `[COMPANION]`.

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

`CompanionSpeechGateway` публикует `VocalisationRequestEvent` с тем же `requestId` и future. Главный или radio-Mouth, которому действительно принадлежит заявка, обязан синхронно claim'ить её `VocalisationHandle` до возврата из EventBus subscriber. Если никто не claim'ил событие, gateway завершает future ошибкой `no active Mouth` — бесконечно pending заявок нет.

`IsSpeakingEvent` принадлежит только `VocalisationHandle`: первый принятый handle публикует `true`, последний завершившийся — `false`. Producer, `VocalisationRouter` и custom-command executor эти события не публикуют. Состояние означает наличие принятой TTS-работы, а не выключатель микрофона: STT продолжает распознавание и передаёт командирскую речь как barge-in.

SpeechGateway:

* queued cancelled speech → адресно удалить/пропустить по `requestId`;
* currently speaking cancelled/stale speech → адресно остановить;
* urgent speech может прервать текущую речь;
* barge-in может прервать текущую речь и очистить очередь.
* success, interruption, blank/error, no-Mouth и stop обязаны завершить каждый принятый future ровно один раз.

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

STT не выключается во время TTS. Если `IsSpeakingEvent=true`, PTT и обычный hot-mic transcript публикуют один `BargeInEvent`, после чего непустая командирская фраза продолжает обычный путь `UserInputEvent`. Сам `ParakeetSTTImpl` не публикует параллельный `TTSInterruptEvent`: единственный interrupt речи создаёт `BargeInController`.

SpeechGateway не должен сам решать судьбу Thought.
ThoughtDispatcher не должен управлять аудио-очередью напрямую.

Если commander speech содержит control prefix вроде “стоп / тихо / отмена”, barge-in path может съесть этот prefix.
Оставшийся текст должен идти обычным путём `UserInputEvent → COMMANDER thought`.
Если после control prefix ничего нет, новая thought не создаётся.

Barge-in не является gameplay command path и не обходит `ToolAccessPolicy`, `Reducer`, dangerous confirmation или `ExecutionModule`.

---

### §2.16. Runtime graph и restart lifecycle

`CompanionSubsystemGate` владеет ровно одним `CompanionRuntimeGraph`. Graph — цельная generation: LLM/speech/execution gateways, session memory, reducer/state, narrator, dispatcher, confirmation coordinator и оба фоновых memory worker'а.

Start транзакционен:

```text
assemble local graph → start dispatcher → atomic CompanionRuntime.installGraph
→ publish graph to gate → register input + barge-in subscribers
```

До `installGraph` статические consumers не видят ни одного компонента новой generation. Ошибка на любой стадии снимает только этот exact graph, отписывает уже зарегистрированный intake и закрывает всё уже созданное. Старый graph нельзя снять через delayed stop: `uninstallGraph(expectedGraph)` использует identity-CAS.

Stop идёт в обратном порядке владения:

```text
gate intake = off → unregister subscribers → atomic uninstall → generation inactive
→ detach memory listeners / cancel confirmation and speech futures
→ interrupt + stop dispatcher → close compression workers
→ close execution lanes → close LLM executor
```

`CompanionRuntimeGeneration` — process-local monotonic owner id. Мысли и фоновые workers проверяют его перед side effect; generation-bound speech/execution wrappers отменяют owned futures. `ExecutionRequest.runtimeGenerationId` thread-scoped связывает синхронный `IntelAction.handle` с исходным graph: после restart статический `CompanionRuntime.narrator()/state()/memory()` не перенаправляет старый handler в новую generation.

Уже вошедшая в игровой handler команда не force-interrupt'ится: это может оставить внешнюю последовательность в неизвестном состоянии. Её executor завершится после естественного возврата handler'а, но future/result, речь и память старой generation отбрасываются. Queued handler, который ещё не начался, отменяется.

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
* ищет сразу по **всей** mid-term topic memory (по всем темам), по short-term timeline **и по MAX-архиву** (дословные pinned-факты, §1.10.52a); `llm_memory` (§3.8) единым поиском пока не охватывается (`recallMatching` его не читает);
* фильтр — **гибридный**: word-overlap, толерантный к словоформам (`CompanionWordMatch`, не contiguous-вхождение), **И** близость по смыслу (cosine вектора запроса к вектору записи; вектор считается один раз при записи и хранится в `MemoryEntry.embedding`). Запись годится по смыслу, если близость ≥ `CompanionConfig.semanticSearchInMemoryFloor()` (0.80). Пустой query → просто самые свежие записи;
* два списка (по словам и по смыслу) объединяются **reciprocal-rank fusion** (RRF, k=60), затем важность, затем свежесть — чтобы ни одна из двух несравнимых шкал (число совпавших слов против cosine) не доминировала, а накопленный архив не монополизировал выдачу;
* near-дубли (перефразировки, эхо-вопросы, повторные «не нашёл») схлопываются в одну запись по смыслу (порог `CompanionConfig.semanticDedupFloor()` = 0.95) — и **при записи** в память, и **в выдаче** поиска; из группы остаётся важнейшая, при равной важности самая свежая, и ей проставляется самое свежее время;
* доля MAX-архива в одной выдаче ограничена квотой (`ARCHIVE_RECALL_LIMIT`), остальные слоты — за short/mid-term;
* максимум N записей (N задаётся настройкой);
* short-term тоже ищется (хотя и вставлен в prompt целиком) — это страховка: если модель всё же решит искать в памяти, она получает цельную картину; запись живёт строго в одном уровне (short-term **или** mid-term), поэтому дублей между уровнями нет;
* `long_term_summary` не ищется — он всегда вставлен в prompt целиком;
* без LLM. Если модель эмбеддингов недоступна или query пустой — поиск деградирует до word-overlap (как было до векторов). Ранжирование живёт в `MemorySearch`; эмбеддинг при записи и дедуп — в `SessionMemoryGateway`.

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
request_input
search_in_memory
classify_turn
```

`request_input` добавляется условно: только если хотя бы один reducer-selected game-tool этого хода содержит параметр с `required=true`. Для набора только из беспараметрических или optional-only tools модель его не видит.

#### `speak`

Озвучить текст через SpeechGateway — сообщить/ответить, выразить мнение, разобрать неоднозначность между действиями или честно отказаться от неподдерживаемой просьбы. Обычный `speak` никогда не открывает ожидание следующей реплики: вопрос в его тексте не является машинным control-сигналом.

Может иметь marker:

```text
confirmation_request
```

Только такой speak проходит сразу при frozen dangerous set.

#### `request_input`

COMMANDER-only settling function для ровно одного отсутствующего обязательного аргумента уже подобранного game-tool:

```text
request_input(action_id, parameter_name, question)
```

`action_id` обязан совпасть с game-tool из точного offered snapshot этого хода, `parameter_name` — с его обязательным параметром, `question` — непустая реплика на активном языке. Даже после условной выдачи это остаётся обязательной host-side проверкой: model output не считается доверенным. Валидация и переход состояния принадлежат `CommanderThought`; сам `RequestInputFunction.handle` metadata-only и не имеет side effect. При успехе thought озвучивает и записывает вопрос, открывает один `PendingClarification(actionId, parameterName, originalInput, question, expiresAt)` и нормально завершается — cognitive worker не блокируется.

Следующая непустая реплика атомарно забирает pending в свой `ThoughtContext`. Обычный reducer по-прежнему подбирает новые команды из новой фразы; прежний target отдельно заново находится по id в текущем видимом каталоге и, если ещё доступен, добавляется к tool snapshot. В prompt pending передаётся как trusted `<pending_clarification>`, отдельно от `<commander_input>`:

- ответ содержит параметр → вызвать прежний target с полной схемой;
- новая команда явно совпала с другим offered tool → вызвать её, прежний pending считается вытесненным;
- отмена/смена темы → `speak`, pending закрыт;
- данных всё ещё недостаточно → новый `request_input`, заменяющий slot;
- target больше не видим → `speak` о недоступности, без исполнения.

Pending живёт не более 60 секунд, принадлежит одной runtime generation и не хранится как conversation memory. История хранит только реальные слова командира и вопрос компаньона.

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

---

### §4.2. System tools для событийной стороны

`EventThought` game-tools не получает ни в одном режиме (subscriber уже посчитал и отфильтровал данные).

В **narration**-режиме он получает ровно одну системную функцию:

```text
speak            # без гейта: решение «озвучить» принял subscriber
```

Subscriber уже решил, что фраза достойна озвучки. ЛЛМ за один раунд формулирует фразу → `speak` (озвучивается только **первый**, если модель дала несколько), и ход завершается. `search_in_memory`/`classify_turn` ему недоступны.

В **verbatim**-режиме ЛЛМ и tools нет вовсе — он детерминированно пишет `sourceId` как `user`-ход, озвучивает готовый текст дословно и пишет его как `[COMPANION]`.

Тему для записи берёт не LLM, а источник (§2.5): переданная subscriber'ом тема (fallback `SYSTEM`).

---

## §5. Tool execution flow

### §5.1. Normal COMMANDER flow

Command/query/macro после ordered cognitive stage исполняются **detached** и завершают owning thought своим future, не удерживая commander worker. `speak` за ход с `COMMAND|QUERY|MACRO` подавляется; свободный `speak` выживает только на разговорном ходу и пишется как `[COMPANION]`.

```text
UserInputEvent
→ ThoughtDispatcher → CommanderThought
→ PromptComposer initial messages → LlmGateway → tool-calls
     (a one-sided physical response is completed inside the gateway before the pair returns)
→ turn classified (classify_turn applied if called: global topic + turn importance)
→ freeze turn topic; currentInput written to memory ([COMMANDER])
→ dispatch game handler and release commander cognitive worker
→ detached completion, still owned/tracked by the same Thought:
     COMMAND  → immediate accepted-ack; serialized action lane; late outcome uses frozen topic
     QUERY    → bounded parallel query lane; pending <processing/>; CALL+RESULT appended together on completion
     MACRO    → serialized action lane; pending <processing/>; own SPEAK steps carry completion futures
     SYSTEM   → no speech, no timeline (result only feeds the flow)
     speak    → suppressed if game action this turn; else voice + [COMPANION]
→ interrupt/stop? queued future cancelled; already-started handler may finish operationally, but late result is discarded
```

Ход однораундовый для LLM и в норме требует одного физического ответа: один `assistant(tool_calls)` содержит `classify_turn` + ровно один settling call. Голый `classify_turn` — совместимый provider fallback; он продолжает только локальный protocol flow внутри `LlmGateway` и не приходит в thought без settling call. Game-handler completion асинхронен относительно следующих cognitive turns, но остаётся частью lifecycle той же мысли.
(Тот же `recordOutcome` исполняет и подтверждённый dangerous-набор, §5.3.)

---

### §5.2. EVENT flows

Стартовая реплика (filler) — прямо в TTS, без мысли и памяти:

```text
subscriber → CompanionRuntime.narrator().filler(text, urgent) → SpeechGateway
```

Реакция-наррация (ЛЛМ фразирует):

```text
subscriber → narrator().narrate(data, instructions, topic)
→ ThoughtDispatcher.submitEventReaction → EventThought (narration)
→ PromptComposer.composeNarration (lean prompt, NARRATION cache profile, only speak) → LlmGateway → один раунд
→ взять первый speak → write [EVENT] stimulus (user) + озвучить + write [COMPANION] under provided topic
→ end
```

Реакция-объявление (дословно, без ЛЛМ):

```text
subscriber (проверил тумблер) → narrator().announce(sourceId, phrase, topic, urgent)
→ ThoughtDispatcher.submitEventVerbatim → EventThought (verbatim)
→ write [EVENT] sourceId (user) → озвучить дословно → write [COMPANION] under topic → end
```

Событийная сторона не получает game-tools и не двигает глобальную тему.

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
LlmGateway receives transient HTTP/network failure
→ resend once with jitter
→ if still failed: INVALID_RESPONSE

LlmGateway receives permanent HTTP failure
→ INVALID_RESPONSE without protocol repair

LlmGateway receives invalid model response or malformed 2xx body
→ protocol repair once if token cost below threshold
→ if still invalid: INVALID_RESPONSE
```

COMMANDER:

```text
→ currentInput saved as unresolved_commander_input
→ speech: cannot execute
→ diagnostics
→ end
```

Invalid-flow касается только ЛЛМ-мыслей. `EventThought` narration best-effort: на невалидном/прерванном раунде просто молчит (ничего не пишет). Verbatim-режим ЛЛМ не зовёт — invalid-пути у него нет.

---

### §5.5. Interrupt flow

Urgent thought arrives (срочность реакции — по флагу `urgent` в вызове `CompanionNarrator`):

```text
→ placed first in its lane
→ all live thoughts interrupted (во всех lane)
```

Interrupted thought:

```text
→ CommanderThought: safe-flush; короткая event-мысль просто завершается
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
→ exact gateway task interrupted
→ physical HTTP future cancelled
→ any racing late result discarded
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

* `ThoughtDispatcher` (lane на источник, `EnumMap`) + `ThoughtLane`.
* `Thought` (abstract) + `CommanderThought` / `ReflexThought` / `EventThought` (narration/verbatim режимы).
* рефлекс-гейт: `ReflexResolver` (`submitCommanderInput`: дословная безопасная беспараметрная команда → `ReflexThought` без ЛЛМ).
* единая дверь событий: `CompanionNarrator` (`filler`/`narrate`/`announce`) + `DispatcherCompanionNarrator`, атомарно публикуется внутри `CompanionRuntimeGraph`.
* `ToolAccessPolicy` (`IntelActionAccessPolicy`, источник → категории; `EVENT` → пусто).
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
* Component-level diagnostics for orphaned/cancelled requests.

---

## §7. Открытые вопросы

### §7.1. Под read-only разведку кода

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
→ CompanionSubsystemGate
→ ThoughtDispatcher
→ COMMANDER thought

Journal/Status events
→ gameplay subscriber
→ CompanionNarrator
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
* Не давать compression requests равный приоритет с urgent consciousness requests.

---

## §10. Реализация: имена и пакеты (Фаза 1)

Документ концептуальный (см. шапку: «имена уточняются по исходникам»). Ниже — соответствие концептов фактическим именам классов и раскладке пакетов в коде (Фаза 1, скелет). При расхождении прозы выше и кода истина по именам — здесь и в исходниках.

### §10.1. Карта имён (концепт → класс)

| Концепт в документе | Класс в коде | Пакет |
|---|---|---|
| commander input gate / lifecycle owner | `CompanionSubsystemGate` | `companion.input` |
| дверь событий (filler/narrate/announce) | `CompanionNarrator` (интерфейс) / `DispatcherCompanionNarrator` (impl) | `companion` / `companion.mind` |
| origin мысли | `ThoughtSource` (COMMANDER/EVENT) | `companion.model` |
| вид мысли (один на источник) | `CommanderThought` / `ReflexThought` / `EventThought` (narration/verbatim режимы) (abstract `Thought`) | `companion.mind` |
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
├─ CompanionRuntime     atomic static access point to one installed runtime graph
├─ CompanionRuntimeGraph, CompanionRuntimeGeneration, CompanionRuntimeGraphFactory
├─ model                ThoughtSource, Urgency, ConversationTopic, IntelActionCategory, GameStateSnapshot
│  ├─ llm               LlmMessage, LlmMessageRole, LlmToolDefinition, LlmToolInvocation,
│  │                    LlmRequest, LlmResult, PromptCacheProfile
│  ├─ speech            SpeechRequest
│  ├─ execution         ExecutionRequest
│  └─ memory            MemoryEntry, MemorySource, MemoryProcessingState
├─ CompanionNarrator    single door for gameplay subscribers (filler/narrate/announce; NO_OP off-mode)
├─ input                CompanionSubsystemGate, BargeInController
├─ mind                 Thought (abstract) + CommanderThought/ReflexThought/EventThought,
│                       DispatcherCompanionNarrator,
│                       ThoughtDispatcher, ThoughtLane, UrgencyPolicy, ThoughtContext, ThoughtDependencies, CompanionState
├─ prompt               PromptComposer, ComposedPrompt, IntelActionAccessPolicy,
│                       CompanionActionReducer, SemanticActionReducer, WordOverlapActionReducer,
│                       GameToolCandidates, ReflexResolver, SemanticReflexResolver
├─ tools                SystemFunction, RegisterSystemFunction, SystemFunctionRegistry, SystemFunctionProvider,
│                       IntelActionTypeResolver (id → COMMAND/QUERY/MACRO/SYSTEM/UNKNOWN),
│                       + the 2 system functions (speak, classify_turn); memory_search is an IntelQuery
│                         (FindActionFunction retired, unregistered)
├─ llm                  LlmGateway, CompanionLlmGateway, ...
├─ speech               SpeechGateway, CompanionSpeechGateway, GenerationBoundSpeechGateway
├─ execution            ExecutionGateway, CompanionExecutionGateway, GenerationBoundExecutionGateway
├─ memory               MemoryGateway, SessionMemoryGateway,
│                       ShortTermMemory, MidTermTopicMemory, LongTermMemory, LlmMemory, MidTermToLongTermConsolidator
└─ confirm              DangerousActionConfirmedEvent
```

> **`CompanionRuntime` / `CompanionState`.** `CompanionRuntime` atomарно держит одну ссылку на полностью собранный `CompanionRuntimeGraph`, поэтому system-function `handle` видит gateways, `CompanionActionReducer`, shared `CompanionState` и `CompanionNarrator` одной generation, без отдельного окна установки narrator. Exact-graph uninstall не может очистить более новую restart-generation. При остановленной подсистеме `narrator()` возвращает `NO_OP`, поэтому gameplay subscribers зовут его безусловно; остальные getters считают off-mode programming error. `CompanionState` owns the sticky global topic for the **next ordered cognitive turn**. `classify_turn` moves it, after which `CommanderThought` freezes the selected topic locally before detaching execution; late outcomes never re-read global topic. `lastCommanderMatchInput` is observer/UI state only, not reducer input. EVENT uses its subscriber-supplied topic. `find_action` is retired and no longer registered. `memory_search` is an `IntelQuery`, not a system function; it uses the same unified recall ranking as pre-turn memory facts.

### §10.3. Уточнения механизмов (отличия от ранних разделов)

* **Шлюзы возвращают `CompletableFuture`; cancellation handle остаётся самим future.** `LlmGateway` → `CompletableFuture<LlmResult>`, `SpeechGateway` → `CompletableFuture<Void>`, `ExecutionGateway` → `CompletableFuture<JsonObject>`. Для LLM gateway связывает future с конкретной `FutureTask`: cancel/50-секундный logical deadline прерывает её и отменяет физический `HttpClient.sendAsync` exchange; очередь, initial call, repair и continuation делят один deadline. Отдельного публичного per-request `CancellationToken` нет. Runtime-level owner — `CompanionRuntimeGeneration`: generation-bound wrappers отменяют owned speech/execution futures на close, а late completion проверяет active generation перед side effect.
* **Один класс мысли на источник.** `Thought` — тонкая общая база (`composeInitialPrompt`/`submitRound`/`submitExecution`/`recordCurrentInput`/`recordCompanionSpeech`/`recordOutcome`/interrupt), **без цикла мышления**. `CommanderThought` владеет полным tool-calling-циклом и dangerous-confirmation; `EventThought` озвучивает реакцию subscriber'а в двух режимах: narration — один короткий ЛЛМ-раунд фразирует переданные данные (лаконичный narration-промпт, только `speak`), verbatim — дословная озвучка готовой фразы без ЛЛМ и промпта; в обоих режимах пишется пара `user`→`[COMPANION]`. Слова компаньона пишутся источником памяти `COMPANION` (сам текст, не `{status:spoken}`). `ThoughtDispatcher` держит lane на каждый `ThoughtSource` в `EnumMap`; COMMANDER имеет один ordered cognitive worker, event — один worker. Detached handler futures остаются live/pending в `ThoughtLane`, не удерживая worker.
* **`mode` → `PromptCacheProfile`** {COMMANDER, NARRATION, COMPRESSION, KEY_GENERATION}. У каждого стабильный `cacheKey()` → Mistral `prompt_cache_key` (свой кэш-префикс на профиль). `EVENT` narration-режим использует профиль `NARRATION` (собственный лаконичный промпт без topic enum / memory / safety); verbatim-режим промпт не строит и профиля не имеет. Признак «ждём tool-calls vs текст» выводится (consciousness vs COMPRESSION / `tools.isEmpty()`), отдельного флага нет.
* **`LlmRequest` = `(requestId, messages, tools, profile)`.** Список `tools` и есть immutable snapshot: `LlmRequest` копирует коллекцию tools, а каждый `LlmToolDefinition` копирует свой список `ActionParameterSpec`, поэтому render и post-parse validation видят один контракт даже при параллельном UI refresh. `urgency` на запросе не нужен — приоритет/преемпция реализуются через interrupt на уровне `ThoughtDispatcher`.
* **`ExecutionRequest` = `(requestId, toolName, arguments, commanderInput, runtimeGenerationId)`.** Lane (action/query) выводится при резолве `toolName` по реестрам; `operationType` в запросе не передаётся. `commanderInput` — сырая реплика командира (`originalUserInput` для хендлера; `""` когда её нет). `runtimeGenerationId` не является tool-call id: он только связывает синхронный handler/static-runtime access с graph-владельцем и равен `0` вне runtime-тестов. Компонент `toolCallId` и прежний thread-scoped `ActiveToolCall` **удалены** (v0.19): спаривание CALL/RESULT в памяти держит только `ToolLink` (свой `toolCallId` внутри мысли через `recordCall`/`recordToolResult`).
* **`SpeechRequest` = `(requestId, text, urgency)`.** Различие conscious / system-notification — забота вызывающей стороны, поля `source` нет.
* **Tool-схема:** игровые tools строит companion-адаптер из существующих `IntelAction.id()/parameters()` (классы команд не зависят от companion); системные — из `SystemFunction`. Нейтральный носитель — `LlmToolDefinition` (имя, описание, локализованные тренировочные фразы из `AiActionLocalizations`, `ActionParameterSpec`); рендер в нативный JSON провайдера — в `LlmGateway`-bridge. Стандартный JSON Schema закрыт через `additionalProperties:false`. После parse `ToolCallValidator` сверяет **весь** response с тем же request-local snapshot: offered name, required-поля, точные JSON-типы без coercion, enum и отсутствие лишних полей. Required `null` невалиден; optional `null` — единственная compatibility-нормализация: после успешной проверки **всего** response поле атомарно удаляется, и handler видит его как omitted. При одном invalid call никакой частичной нормализации нет: весь response отклоняется до side effects; `CompanionLlmGateway` возвращает всем его ещё не исполненным calls truthful rejected tool-results и делает одну repair-попытку с неизменным tools snapshot/deadline. Неправильная provider-shape аргументов (не JSON object) становится `INVALID_RESPONSE` ещё в adapter parse.
  * **Категории и видимость:** `IntelCommand` → `ACTION`, `IntelQuery` → `QUERY`, user macro → `MACRO`. На intake командирского turn один раз снимается `GameStateSnapshot(flags, flags2, fighterOut)`; точный/семантический reflex и reducer проверяют `isVisibleForLLM(status) == true` на detached `Status` из этого snapshot. Поэтому все стадии одного turn видят один контекст, а новое live-состояние применяется со следующего turn. Перед исполнением второго visibility-gate нет. Наличие локализованной фразы **не** является условием включения: при native tool-calling LLM выбирает tool по `name`/`description`/`parameters`, поэтому action без фразы остаётся доступен — он лишь хуже сопоставляется с иноязычной репликой. Companion-нерелевантные fallback-id старого пути (general-conversation, ignore-nonsensical, connection-check) не включаются.
  * **Описание игрового tool — авторская английская суть (`llmDescription`) + английские тренировочные фразы.** Описание для провайдера = `IntelAction.llmDescription()` (короткая английская фраза назначения) **плюс английские тренировочные фразы команды** (из английской alias-карты, `{key:…}`-аннотации срезаются) — конкретные образцы для сопоставления (`GameToolCandidates.appendEnglishPhrases`). Английское описание стабилизирует схему и кэш, но `CommanderPrompt` требует выбирать функцию по исходной формулировке командира и не переводить её на английский. **Локализованные** фразы в описание не идут — они через `phraseKey` кандидата кормят только **редьюсер**. Системные функции описываются так же через `llmDescription()`; тренировочных фраз у них нет. Параметры: `examples`/`extractionHint` из `ActionParameterSpec` сворачиваются в `description` параметра JSON-схемы (`OpenAiCompatibleLlmAdapter`) — иначе модель их не видит (был баг: `target drive` уходил в уточняющий вопрос вместо команды). Синтетический префикс «Game action `<id>`» убран.
* **System-prompt steering (`CommanderPrompt`).** COMMANDER-промпт требует только tool calls: initial `assistant(tool_calls)` должен содержать сначала `classify_turn`, затем ровно один settling call; metadata-only результат классификации не нужен для выбора settling call, поэтому ждать его запрещено. Отдельный tool-result может явно запросить ровно один отсутствующий protocol call; это request-local fallback `CompanionLlmGateway` для classify-only или settling-only provider deviation, а не обычный initial flow. Лестница выбора берёт первый подходящий пункт: (1) offered action/query/macro, который явно соответствует намерению; (2) уже инлайненный `<fact>`, если никакой offered tool не может получить ответ; (3) `memory_search`, если командир явно просит вспомнить, перечислить или посчитать память и query offered; (4) `speak` для разговора, пояснения, неоднозначности или неподдерживаемого запроса. `CommanderThought` однораундовый: `memory_search`, как любой QUERY, озвучивает свой data-grounded outcome напрямую через `recordOutcome`, без второго LLM-раунда.
* **Semantic routing and reduction.** `ThoughtDispatcher` сначала пробует точный `ReflexResolver`, затем `SemanticReflexResolver`. Обе стадии получают один `GameStateSnapshot`; уверенный безопасный беспараметрный матч создаёт `ReflexThought`, а во всех остальных случаях snapshot и входной `SemanticQuery` остаются в `ThoughtContext`. `SemanticActionReducer` использует тот же snapshot для видимости и тот же вектор для shortlist игровых tools; pre-turn memory recall использует вектор для fact candidates. Если embedding недоступен или inference падает, reducer передаёт snapshot своему `WordOverlapActionReducer` fallback, а recall деградирует к word-only пути. EVENT не получает игровых tools.
* **LLM provider seam:** провайдер-специфичный рендер/разбор — `LlmProviderAdapter`. Общий OpenAI-совместимый рендер/парсинг живёт в базовом `OpenAiCompatibleLlmAdapter`; тонкие per-provider impl'ы задают только модель, `tool_choice` и `prompt_cache_key`: `MistralLlmAdapter` (cloud — `any`, с cache key) и `LmStudioLlmAdapter` (local LM Studio — `required`, без cache key). Это бывш. `CompanionLlmDialect`/`MistralToolCallDialect`, переименованы. У `LlmGateway` две операции: `submit` (tool-calling сознания) и `compressMidTermMemory(LlmRequest) → CompletableFuture<String>` (текстовый ответ для сжатия памяти; адаптер даёт `parseText`, тело — тот же `buildRequestBody` с пустыми `tools`).
* **Long-term память реализована:** `LongTermMemory` (холдер), `MidTermTopicMemory.evictOverflow` (per-topic cap), `MidTermEvictionListener` (гейтвей отдаёт overflow, сам LLM не зовёт) и `MidTermToLongTermConsolidator` (буфер→порог→`compressMidTermMemory`→валидация `SUMMARY_MAX_CHARS`→atomic `replaceLongTermSummary`; провал → буфер потерян, summary цела, `SpeechGateway` system-notification). Все лимиты памяти — в `CompanionMemoryLimits`. Подключение listener'а к гейтвею — при bootstrap (`CompanionSubsystemGate`).
* **Итог tool-call по типу действия (`Thought.recordOutcome`).** Тип резолвит `IntelActionTypeResolver` (`companion.tools`, инжектируемый тест-сим) → `COMMAND/QUERY/MACRO/SYSTEM/UNKNOWN`. `recordOutcome` озвучивает исход **напрямую** (без `AiVoxResponseEvent` — это событие теперь только системное). **COMMAND** получает immediate accepted-ack, исполняется на serialized action lane, а непустой поздний outcome озвучивается/пишется `[COMPANION]` под frozen topic. **QUERY** исполняется в bounded parallel pool; пока pending, исходный turn закрыт `<processing/>`, затем CALL и `[TOOL_RESULT]` добавляются одной completion-секцией. **MACRO** — serialized action lane и свои SPEAK-шаги. **SYSTEM/UNKNOWN** — речь и timeline не трогаем. После interrupt late result отбрасывается. `silentInCompanion()` удалён.
* **`MemoryProcessingState`** = `PROCESSED`, `UNRESOLVED`, `AWAITING_CONFIRMATION`, `CONFIRMED`, `CANCELLED`, `TIMED_OUT`, `INTERRUPTED`.

