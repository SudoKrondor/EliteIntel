# VEGA architecture

How the AI works: what runs a turn, what the model is allowed to return, what it remembers, and what it is told. It replaces three `COMPANION_*.md` documents deleted on 2026-09-10: an architecture description whose memory model no longer existed in the code, and two design proposals that had already been adopted or abandoned.

Verified against the code on `V1.1-Release`, 2026-09-10.

> **A note on the name.** The AI is called **VEGA**, in the product and in the code: the package is
> `elite.intel.ai.brain.vega` and the classes are `VegaRuntime`, `VegaLlmGateway`, `VegaSubsystemGate`
> and so on. The word "companion" was swept out of the tree on 2026-09-10.
>
> Two uses of the word deliberately survive and must not be "fixed": a **binary companion star** in
> `ScanBodyClassifier` and `SupercruiseExitedSubscriber` (Elite's astronomy, nothing to do with the AI),
> and the **companion updater jar** in `util/Updater.java`, where the word means *accompanying* and
> refers to the `updater/` module.

---

## 1. The rules that do not bend

1. There are two sources of thought: **COMMANDER** (something the commander said) and **EVENT**
   (something that happened in the game).
2. Every model turn ends in tool calls, never free text. The caller declares the ceiling in
   `LlmRequest.maxToolCalls`, filled from `Thought.maxToolCallsPerRound()`; every turn allows exactly one call except a COMMANDER turn, which allows up to `VegaConfig.maxCommanderToolCalls()` - currently
   **2**.
3. The model never classifies topic, importance, danger, or what to remember. Code decides all four.
4. Memory accepts only **completed** records. A partial reply, an abandoned query or a lone tool result never enters it.
5. Commands and macros are execution, not conversation, so they write no conversational memory.
6. All data for one turn lives in its immutable `ThoughtContext`. A late result never reads the state of a newer turn.
7. Stopping, interruption or a runtime-generation change must never publish late speech or partial memory.

## 2. What gets assembled

`VegaRuntimeGraph` wires one coherent runtime, published as a whole by `VegaRuntime`. On restart the previous generation is closed, and its in-flight results are no longer allowed to touch the new one.

| Component | Responsibility |
|---|---|
| `ThoughtDispatcher` | accepts commander input and game-subscriber reactions |
| `ThoughtDependencies` | hands each thought its gateways, policies and coordinators |
| `VegaLlmGateway` | owns one logical model request and validates the protocol |
| `ExecutionGateway` | runs game and system functions |
| `SpeechGateway` | passes finished speech to the active voice engine |
| `SessionMemoryGateway` | holds the session's replay window |
| `OversizedMemoryCompressor` | shortens an over-long completed record in the background |
| `VegaDiagnostics`, `VegaMemoryDump` | expose turn progress and memory state |

## 3. Getting in

### 3.1 The commander said something

`ThoughtDispatcher.submitCommanderInput`:

1. keeps the raw text for diagnostics and execution;
2. strips VEGA's name as a form of address and applies acoustic STT corrections only;
3. takes one `GameStateSnapshot`;
4. checks `ReflexResolver` for an exact match;
5. builds a `ReflexThought` on a full match, otherwise a `CommanderThought`;
6. places it on the sequential COMMANDER lane.

Only a full match against a single safe, parameterless function bypasses the model entirely. Everything else goes to the LLM, with `SemanticActionReducer` deciding
*which* functions are offered, never whether to call one.

### 3.2 Something happened in the game

Game subscribers talk to `VegaNarrator` and nothing else. It has three doors:

- `filler` - a one-off service phrase straight to speech; writes no memory;
- `narrate` - an `EventThought` that has the model phrase supplied data;
- `announce` - an `EventThought` that speaks a finished phrase with no model call.

**The subscriber decides whether an event is worth saying at
all**, and hands over data it has already selected. This is the single most important boundary in the design: what to say and when is owned by hand-written subscribers, and the model only decides
*how to word it*. The AI cannot open its own mic.

EVENT turns get no game functions, and dialogue history is not passed into an EVENT prompt.

## 4. Lanes, concurrency, interruption

COMMANDER and EVENT have separate sequential `ThoughtLane`s, so event chatter never delays command recognition and commander turns keep their admission order.

Once a game function is chosen, a long-running handler detaches from the cognitive lane:

- commands and macros run sequentially;
- data queries run in a pool of up to `VegaConfig.maxParallelQueryExecutions()` threads (**4**);
- short system functions run inside the thought.

The thought stays registered as live until its detached handler finishes, so idle checks, the watchdog and interruption all account for the full lifecycle.

An urgent thought interrupts live thoughts and jumps its lane. The watchdog interrupts a turn running longer than `thoughtWatchdogTimeout()` (**60
s**); the model request's own deadline is
`llmLogicalDeadline()` (**50 s**), so it expires first.

Cancelling does not promise to undo an external action already started. It only forbids the late result from producing speech or memory.

## 5. The three kinds of thought

### `CommanderThought`

The full path: the reducer picks a small set of game functions, memory supplies the replay window,
`PromptComposer` assembles messages and tools, `VegaLlmGateway` returns validated calls, and the thought settles them one at a time.

One utterance may carry two requests ("check the loadout, what is our cargo capacity"), so a commander turn settles up to 2 calls. A batch is several answers to one utterance, not simultaneous actions:
calls run strictly in model order, each starting after the previous finishes, and one failing does not cancel the rest.

Two call types never join a batch - `request_input` suspends the turn until the commander answers, and a dangerous action waits for confirmation - so either reduces the response to itself alone. A `speak`
alongside a game call is dropped, because the call's own outcome is the answer.
`CommanderThought.settleableCalls` is the sole owner of that reduction.

The acknowledgement is spoken once per turn however many commands it carries; each command still speaks its own outcome.

### `ReflexThought`

Runs a pre-selected safe, parameterless function with no model call at all. A command writes no memory; only a successful query with a non-empty answer publishes a `QUERY` record. Failure, an empty result, cancellation and interruption write nothing.

### `EventThought`

Either has the model phrase one `speak` call, or speaks a finished phrase verbatim. **Neither mode stores
anything.** A gameplay narration was never replayed into the commander prompt, so when the
`EVENT` record kind went, event turns stopped writing memory altogether - they only speak.

## 6. What the model may return

### 6.1 The offered set

`CommanderPrompt` requires tool calls and no free text. The admissible options are:

- the game function the reducer offered for this turn;
- `request_input`, for one missing required parameter;
- `speak`, for conversation, for an answer drawn from a trusted fact, or to report an unsupported request.

Those are the only two system functions registered (`SpeakFunction`, `RequestInputFunction`).
`FindActionFunction` still exists in the tree but is **retired** - its `@RegisterSystemFunction`
annotation was removed because a small local model did not reliably reach for it and the reducer surfaces the right tools without it. It is kept as reference, not wired.

There is no `classify_turn`, no composite response, no topic or importance field. **There is no
`memory_search` and no `remember`** - see section 8.

The prompt states one short if-else order:

1. a continuing `pending_clarification`;
2. any offered game function that fits, choosing the single most probable one (several plausible candidates is not a reason to ask);
3. a complete answer from a trusted fact;
4. `speak` as the final branch.

Dialogue history is context only, never evidence of current game state. Trusted game data means the live `<facts>` block. A relevance-limited set of facts cannot prove a complete list, the absence of other data, or an exact total.

### 6.2 Validation

`VegaLlmGateway` accepts a response only when it parsed, carries at least one call within the declared limit, every function was offered this turn, and any parameterized function's arguments match its exact schema.

Repeated identical calls (same name, same arguments) are dropped before validation - that is one intent stated twice. The same function with different arguments stays two calls. If an offered function declares no parameters, stray argument fields are discarded and the handler receives `{}`; for functions with parameters, unknown fields still invalidate the call.

A repairable violation gets one retry:

- an unparseable response repeats the original request without inventing history;
- an unknown function gets a truthful `rejected` result and the model chooses again;
- wrong arguments get `rejected` plus the exact schema, with the repair narrowed to the function already chosen, so the model fixes parameters instead of falling back to conversation;
- too many calls get `rejected` on every call, stating the limit, with the set narrowed to the functions the model itself named - only the count is in dispute. If it still overshoots, the calls it named first are executed up to the allowance so the turn is not wasted.

LM Studio is sent `parallel_tool_calls` matching whether the turn settles several calls, so the limit is identical at request and provider level. Network failures, `429` and `5xx` may get one physical resend after 250-750 ms. Permanent failures and cancellation start no repair.

### 6.3 Providers

`VegaLlmGatewayFactory` maps a provider to an `LlmProviderAdapter`. Six cloud adapters ship - Anthropic, OpenAI, Gemini, Grok, DeepSeek, Mistral - plus LM Studio, which is the only local host and so has a single `LOCAL_GATEWAY` field rather than a map. Ollama was removed in V1.1 maintenance.

## 7. Clarification and dangerous actions

`request_input(action_id, parameter_name, question)` opens a `PendingClarification` only if the function was in this turn's set, it is a game function, the named parameter really is required, and the question is non-empty. The next utterance takes that state atomically; it is passed to the model separately from the commander's words and never written to memory.

**Danger is decided by code after the function is
chosen.** The model is never asked whether an action is dangerous. VEGA speaks a localized confirmation question and waits for the code word through
`ConfirmationCoordinator`. Confirmation, refusal and waiting write no memory.

## 8. Memory: the replay window, and nothing below it

This section is where the previous document had gone furthest out of date, so it is worth being blunt about what changed.

**There is one memory area: a bounded window of completed exchanges that the next prompt replays.**
`SessionMemoryGateway` owns it; `RecentMemory` holds it.

| Limit | Value | Source |
|---|---|---|
| records in the window | 15 | `VegaMemoryPolicy.recentRecordLimit()` |
| soft token budget | 1200 | `recentTokenBudget()` |
| per-entry bound | 200 chars | `entryMaxChars()` |

Overflow evicts the **oldest whole
record**, and at least one record is always kept even over the token budget. Records are inserted by completion time, so a gist that arrives late from the compressor does not reorder history.

Two record kinds survive, and both are replayed as conversation turns:

| `MemoryKind` | Shape | Written when |
|---|---|---|
| `DIALOGUE` | `COMMANDER` → `VEGA` | a completed non-empty `speak` in a commander turn |
| `QUERY` | `COMMANDER` → `VEGA` | a query handler returned a non-empty answer |

`QUERY` deliberately stores no function name, tool-call id or JSON arguments - memory needs the completed semantic pair, not the execution protocol. A commander turn voices each answer as the handler returns it but publishes
**one** pair for the whole turn, in the order voiced, so a single utterance does not appear in history as several identical questions.

Everything else leaves memory unchanged: a failed or empty handler answer, all commands and macros, a dangerous-action confirmation, `request_input`, a service error reply, and any cancelled or interrupted turn.

### What was removed, and why it is not coming back

An earlier design had four areas - recent, retained history, a consolidation waiting area and a long-term session area - with `EVENT` and `SAVED_TEXT` record kinds, an LLM-written summariser (`MidTermToLongTermConsolidator`), a `remember` command, and a `memory_search` query the model could call. All of it is gone.

- `memory_search` was the only reader of the lower tiers. Once it went, everything they collected was written and never read again, at the cost of a model call per consolidation batch.
- The `EVENT` kind was never replayed in the prompt, so each event record spent a window slot and part of the token budget evicting an exchange that
  *would* have been replayed.
- What VEGA knows about the game now comes from the live `<facts>` block and the game queries, both of which read the database - not from stored conversation.

`MidTermToLongTermConsolidator` and `RememberCommand` no longer exist in the tree.

### Oversized records

If any ordinary entry exceeds 200 characters, the whole completed record is handed to
`OversizedMemoryCompressor` before the first mutation. It works off the thought lane, shortens only the long entries through a separate LLM request whose only tool is `speak`, and republishes the record with its original kind, timestamp and entry order. So a long `QUERY` can never end up in memory as just the question or just the answer, and its full original answer is still spoken without waiting for the gist.

An empty answer, an unusable one or a provider failure does not destroy the record: a deterministic fallback truncates at the nearest word boundary. If the worker is closed or refuses the task, that fallback runs synchronously in the gateway.

## 9. What the prompt contains

`PromptComposer` builds exactly **one** SYSTEM message: static rules first, the dynamic `<facts>`
block last. The current utterance is the final USER message, passed unwrapped unless a
`pending_clarification` is active, in which case the clarification state is appended in a separate
`<context>` - it is the continuation of an unfinished request, not a game fact.

Recent records are replayed in their original roles: `DIALOGUE` as `user` then `assistant`, `QUERY` as
`user` then `assistant` with the finished answer. There are no artificial turn boundaries, intermediate states or classification messages, so every fragment is complete and valid chat protocol.

### The `<facts>` block

Live data only, from sources annotated `@RegisterMemoryFactSource` under `memory/facts/sources/`.
`MemoryFactSourceRegistry` scans that package reflectively. Each source judges its own relevance (`isRelevant`), and `isAmbient` marks the ones that speak on every turn rather than in answer to a subject - ambient sources are gathered last, so a fact answering the commander's actual question keeps its slot when the block fills.

`MergedFactCandidates` caps the block at **6 facts total and 2 per
source**. That cap is a budget, not a default: pay for a new source by cutting an old one. The sources currently registered are current system, body, station, situation, open screen, mission, objective and system signals.

## 10. Speech

Finished text goes through `SpeechGateway` to the active voice engine, which receives a concrete
`VocalisationHandle` and must complete it on success, failure, cancellation or stop.

STT stays live while VEGA speaks. A newly recognized utterance raises a barge-in (`BargeInController`), after which the controller separately interrupts speech and live thoughts. Acoustic echo suppression is not part of this design.

## 11. Diagnostics

Every thought carries a `SOURCE#n` tag. The stages are `intake` (accepted text and chosen path),
`reduce` (function candidates), `compose` (counts of system functions, facts and history records),
`llm-http` (physical request time), `llm` (the validated call or repair reason), `settle` (settling path), `exec-time` (handler time), `memory`, and `done` (total time).

`VegaMemoryDump` shows the window's records by kind, preserving record boundaries.

The file-driven diagnostics harness that drives all of this for routing tests is documented separately in `DIAGNOSTICS_INPUT.md`.

## 12. Checking a change

1. `:app:compileJava` and `:app:compileTestJava`;
2. `:app:test`, then `:app:subscriberTest` (excluded from CI for timing, not correctness);
3. a targeted `:app:localIntegrationTest` for the chosen language when a local model is running;
4. a memory-dump check: no partial records, `QUERY` in paired form, nothing below the window;
5. a diagnostic transcript check: one settling call, no classification stage.

A change to the memory rules is finished only when the record model, the store, prompt replay, diagnostics, this document and the tests have all moved together.
