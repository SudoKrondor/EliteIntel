# Companion architecture

This document describes the current architecture of the companion mode in EliteIntel. It is the source of truth for component boundaries, the function-calling protocol, the memory-write rules and the execution lifecycle.

Version: **v0.34**, 2026-08-06.

## 1. Core rules

1. There are two thought sources: `COMMANDER` and `EVENT`.
2. Every model turn ends in function calls. The caller itself declares the limit (`LlmRequest.maxToolCalls`): every turn accepts exactly one call except a `COMMANDER` turn, where a single utterance may hold several requests.
3. The model does not classify topic, importance or memory kind.
4. Memory accepts only completed `MemoryRecord`s. A partial reply, an unfinished query or a lone function result never enters it.
5. Commands and macros are execution rather than conversation, so they create no conversational memory. The single explicit exception is a direct `RememberCommand` call from a `COMMANDER` turn, which owns the `SAVED_TEXT` record.
6. The record kind is determined by code from the path that actually completed: conversation, data query, game event or explicit memorisation.
7. All data of one turn belongs to its immutable `ThoughtContext`; a late result never reads the state of a newer turn.
8. Stopping, interruption and a runtime-generation change must never publish late speech or partial memory.

## 2. System composition

`CompanionRuntimeGraph` assembles one coherent runtime:

- `ThoughtDispatcher` accepts commander input and game-subscriber reactions;
- `ThoughtDependencies` hands thoughts their gateways, policies and coordinators;
- `CompanionLlmGateway` owns one logical model request and the protocol validation;
- `ExecutionGateway` runs game and system functions;
- `SpeechGateway` passes finished speech to the active voice engine;
- `SessionMemoryGateway` holds the current session's memory;
- `OversizedMemoryCompressor` turns an over-long completed record into a short, whole gist in the background;
- `MidTermToLongTermConsolidator` transactionally folds records awaiting consolidation into separate summaries;
- `CompanionDiagnostics` and `CompanionMemoryDump` expose execution progress and memory state.

`CompanionRuntime` publishes the graph as a whole. On restart the old generation is closed, after which its asynchronous results are no longer allowed to change the new runtime.

## 3. Intake and routing

### 3.1 Commander input

`ThoughtDispatcher.submitCommanderInput` performs the following:

1. keeps the raw text for diagnostics and execution;
2. strips the companion's name as a form of address and applies acoustic STT corrections only;
3. takes a `GameStateSnapshot` once;
4. checks the exact `ReflexResolver`;
5. creates a `ReflexThought` on a full match, otherwise a `CommanderThought`;
6. places the thought on the sequential `COMMANDER` lane.

Only a full match against a single safe, parameterless function bypasses the model. Every other utterance goes to the LLM, and `SemanticActionReducer` serves only to select the function set offered to it.

### 3.2 Game event

Game subscribers address `CompanionNarrator` only:

- `filler` passes a one-off service phrase straight to speech and writes no memory;
- `narrate` creates an `EventThought` that phrases the supplied data through the model;
- `announce` creates an `EventThought` that speaks a finished phrase without the model.

The subscriber decides in advance whether an event is worth reporting and passes data it has already selected. Data and instructions are bounded by separate limits and live only in the current `ThoughtContext` and prompt; dialogue history is not passed into the EVENT prompt. Only the final successful phrase enters memory: the model's `speak` reply, or the ready verbatim announcement. `EVENT` receives no game functions.

## 4. Lanes, concurrency and interruption

`COMMANDER` and `EVENT` have separate sequential `ThoughtLane`s. Event reactions therefore never delay command recognition, and commander turns keep their admission order.

Once a game function has been selected, a long-running handler is detached from the cognitive lane:

- commands and macros run sequentially;
- data queries run in a pool of up to four threads;
- short system functions run inside the thought itself.

The thought stays registered as live until the detached handler finishes. This lets `isIdle`, the watchdog, stop and interrupt account for its full lifecycle.

An urgent thought interrupts live thoughts and goes to the head of its lane. The watchdog checks live thoughts every five seconds and interrupts a turn running longer than 60 seconds. The logical model-request deadline is 50 seconds, so it expires before the thought deadline.

If a handler has already started an external action, cancellation does not promise to undo that action physically. It only forbids the late result from producing speech or memory.

## 5. Kinds of thought

### 5.1 `CommanderThought`

The full commander path:

1. the reducer selects a small set of available game functions;
2. memory supplies recent history and admissible facts;
3. `PromptComposer` assembles the messages and functions;
4. `CompanionLlmGateway` returns validated calls, no more than the limit the turn declared;
5. the thought settles them one after another and applies to each the write rule matching its result.

A single utterance may hold several requests ("check the loadout, what is our cargo capacity"), so a commander turn settles up to `CompanionConfig.maxCommanderToolCalls()` calls. A batch is several answers to one utterance, not simultaneous actions: the calls run strictly in sequence in model order, each starting only once the previous one has finished, and a failure in one does not cancel the rest.

Two kinds of call never enter a batch: `request_input` suspends the turn until the commander answers, and a dangerous action waits for confirmation, so either of them reduces the model's response to itself alone. `speak` beside a game call is dropped, because the answer is the call's own outcome; a response with no game calls keeps the first call. `CommanderThought.settleableCalls` is the single owner of that reduction.

The acknowledgement is spoken once per turn however many commands it carries, but each command still speaks its own outcome.

If a localized training phrase ends in a required string parameter, an exact match on its prefix is guaranteed to add the function to the candidates, but does not displace semantic competitors. The fast reflex never runs parameterized functions.

For `RememberCommand` the model supplies a string argument, but the command does not trust its wording. It extracts the localized suffix itself from the very same canonical utterance the model saw, and stores exactly that. A dropped word or a felicitous rephrasing by the model therefore cannot become trusted text.

`RememberCommand` is available in `COMPANION_COMMANDER` only: it is absent from the legacy action map, and a user macro's `RUN_COMMAND` has no right to invoke it.

### 5.2 `ReflexThought`

Receives a pre-selected safe, parameterless function:

- a command runs with no memory write;
- only a successful query with a non-empty answer publishes one completed `QUERY`; failure, an empty result, cancellation and interruption write nothing.

### 5.3 `EventThought`

Has two modes:

- narration: one `speak` call is phrased by the model;
- verbatim: a finished phrase is spoken without the model.

Only the final successful phrase is published, as a single-entry `EVENT`. The source event data is never recorded. Failure, an empty phrase, cancellation or interruption write nothing.

## 6. Model protocol

### 6.1 One call per request

`CommanderPrompt` requires function calls and no free text. One request takes one call; a second call is licensed only by a second, distinct request in the same utterance, never by indecision between candidates for the same one. The admissible options for a call are:

- the selected game function, including the built-in `remember(text)` command;
- `request_input` for one missing required parameter;
- the ordinary `memory_search` query for an explicit memory lookup;
- `speak` for conversation, for an answer drawn from a trusted fact, or to report an unsupported request.

`memory_search` goes through the reducer and the query registry along with every other game query. It is not part of the system-function set and appears to the model only when the reducer selected it for the current utterance.

`classify_turn` does not exist. Neither does the mandatory composite response, the topic, the importance, the `canonical_fact` or a separate classification stage.

`CommanderPrompt` states one short `if-else` order:

1. a continuing `pending_clarification`;
2. any offered game function other than `memory_search` that fits the input, choosing the single most probable one, since several plausible candidates are not a reason to ask;
3. an explicit request to recall, search, list or count through `memory_search`;
4. a complete answer from a trusted fact;
5. `speak` as the final branch.

Ordinary dialogue history is context only, never evidence of current game state. Trusted game data means the live pluggable fact sources. Memory records, including `EVENT` and `SAVED_TEXT`, are never mixed into the prompt automatically and are reachable only through `memory_search`. A relevance-limited set of live facts cannot prove a complete list, the absence of other data, or an exact total.

### 6.2 Response validation

`CompanionLlmGateway` accepts a response only when all of the following hold:

1. the response parsed as a valid model result;
2. at least one call is present, and their number does not exceed the limit the request declared (`LlmRequest.maxToolCalls`);
3. every function was offered in this request;
4. a parameterized function's arguments match its exact schema.

Repeated identical calls (same name and same arguments) are dropped before validation: that is one intent stated twice, not two actions. The same function with different arguments remains two calls.

If an offered function declares no parameters, any argument fields the model generated are discarded and the handler receives an empty `{}` object. For functions with parameters, unknown fields still make the call invalid.

A repairable violation is allowed one retry:

- an unparseable response repeats the original request without adding invented history: there is nothing in it to reject;
- a call to an unknown function receives a truthful `rejected` result, after which the model chooses again from the original set;
- a call to an offered function with wrong arguments receives `rejected` together with the exact schema, and the repair is narrowed to the function already chosen. The model then fixes the parameters instead of replacing the request with conversation;
- a response with more calls than the limit receives `rejected` on every call, stating the limit, and the function set is narrowed to the ones the model itself named: only the count is in dispute, not the choice. If the model still exceeds the limit after the repair, the calls it named first are executed up to the allowance, so the turn is not wasted.

LM Studio receives `parallel_tool_calls` set to whether the turn settles several calls, so the limit is the same at the request level and at the provider level. The retry belongs to validating one logical response and does not turn the turn into a protocol of several mandatory functions.

Network failures, `429` and `5xx` may receive one physical resend after a 250-750 ms delay. Permanent failures and cancellation do not start a protocol repair.

## 7. Parameter clarification and dangerous actions

`request_input(action_id, parameter_name, question)` opens one `PendingClarification` only if:

- the function was present in the current turn's set;
- it is a game function;
- the named parameter really is required;
- the question is not empty.

The next utterance takes that state atomically. It is passed to the model separately from the commander's words and is never written to memory.

Danger is determined by code after the function has been selected. The model is never asked to classify an action as dangerous. The companion speaks a localized confirmation question and waits for the code word through `ConfirmationCoordinator`. Confirmation, refusal and waiting create no conversational record.

## 8. Memory model

### 8.1 The unit of record

The only writable unit is `MemoryRecord(timestamp, kind, entries)`. The constructor validates the shape, and the store appends, evicts and returns a record only as a whole.

| `MemoryKind` | Record shape | Created by |
| --- | --- | --- |
| `DIALOGUE` | `COMMANDER` → `COMPANION` | a completed `speak` in a commander turn |
| `QUERY` | `COMMANDER` → `COMPANION` | a successful query with a non-empty answer |
| `EVENT` | `EVENT` | the final LLM narration or a verbatim announcement |
| `SAVED_TEXT` | `COMMANDER` | a direct `RememberCommand` |

`QUERY` deliberately stores no function name, `toolCallId` or JSON arguments: memory needs the completed semantic pair, not the technical execution protocol. `EVENT` stores one final phrase without the source event payload.

### 8.2 Publication rules

- Ordinary conversation is recorded only after a non-empty `speak` reply, as the whole pair at once.
- `QUERY` is published only after a successful non-empty handler answer, as a `COMMANDER` → `COMPANION` pair. A failure is voiced but not recorded; an empty answer, cancellation and interruption likewise leave memory unchanged. A reflex publishes the record and the speech as one indivisible action. A commander turn voices each answer as soon as the handler returns it, but publishes one pair for the whole turn with the answers in the order they were voiced: otherwise one utterance would appear in history as several identical questions. Publication happens at the end of the turn, so it does not depend on which path executed the call.
- `EVENT` is published only after a successful final phrase from the model or a ready verbatim announcement. The source event data stays transient and never enters memory.
- `RememberCommand` stores `SAVED_TEXT` only on a direct commander turn. It extracts the non-empty localized suffix itself from the canonical utterance shown to the model; the model's argument is never used as memory content. The command's acknowledgement forms no `DIALOGUE`.
- All other commands, a macro, a dangerous-action confirmation, `request_input`, a service error reply and an unfinished turn leave memory unchanged.

Deciding the `MemoryKind` belongs entirely to these code paths. The model neither chooses the record kind nor assigns metadata to it.

### 8.3 Storage areas and eviction

`SessionMemoryGateway` holds four areas.

#### Recent area

- up to 15 completed records;
- a soft limit of 1200 estimated tokens;
- on overflow the oldest record is removed whole;
- at least one record is kept even when the token limit is exceeded;
- an ordinary field is bounded to 200 characters before it enters history.

If even one ordinary field is longer than the limit, `SessionMemoryGateway` passes the whole completed `MemoryRecord` to `OversizedMemoryCompressor` before the first mutation. The compressor works outside the thought lane, shortens only the long fields through a separate LLM request whose sole tool is `speak`, and then republishes the whole record with its original `kind`, `timestamp` and source order. A long `QUERY` therefore cannot leave only the question or only the answer in memory, and its full original answer keeps being spoken without waiting for the background gist. Until compression finishes, the record is not in memory yet.

Shortening uses a dedicated prompt for a short single-phrase digest, offered the `speak` system function only. The compressor takes the validated `speak.text` argument straight from the LLM result and never passes the call to the executor, so this technical function speaks nothing. Free text from the model, including any reasoning, is ignored by the shared tool-calling gateway and never enters memory.

An empty or unextractable compressor answer and a provider failure do not destroy the record: a deterministic fallback applies an ellipsis at the nearest word boundary of the original text. An extracted gist over the limit is bounded the same way, after which the whole record is stored atomically. If the worker is already closed or did not accept the task, the fallback runs synchronously in the gateway. A record that completed late is inserted at its original `timestamp`, so dialogue and events are not reordered relative to newer records.

On leaving recent history:

- `DIALOGUE` moves to retained conversation history;
- `EVENT` moves to retained event history;
- `QUERY` is deleted;
- `SAVED_TEXT` is not stored here.

Recent history stays verbatim. Before being added to retained history, whole records of the same kind are deduplicated: `DIALOGUE` must match in meaning across both halves of the pair at a threshold of `0.95`, while `EVENT` matches on exact text only. A newer retained record replaces the older one; a `pending` record that was already published is neither changed nor given a second copy.

#### Retained history

Two independent sets with different eviction times:

- up to 60 `DIALOGUE` records;
- up to 120 `EVENT` records.

Overflow of one set never evicts records from the other. Old records move whole to the next area.

#### Consolidation waiting area (`pending`)

- holds evicted `DIALOGUE` and `EVENT` records until a summary commits atomically;
- keeps offering both groups to search;
- keeps offering `EVENT` to an explicit `memory_search`;
- deletes only the records covered by a successfully committed summary.

#### Long-term session area

- a separate `DIALOGUE` summary;
- a separate `EVENT` summary;
- a bounded store of verbatim `SAVED_TEXT`.

The consolidator takes ten waiting records of one kind and merges them with the previous summary. A successful atomic commit under one lock replaces the summary and deletes only the batch it covers. The maximum summary length is 1500 characters.

A compression failure changes neither the previous summary nor removes the batch from the waiting area. The batch returns to the buffer and is retried automatically with a short increasing delay; no new record is required for that. After three failed model responses, a bounded local summary is committed from the already completed records. A persistent model failure therefore cannot block this memory kind for the rest of the session. The user receives one localized message on the first failure.

`SAVED_TEXT` bypasses the recent, retained and waiting areas, the shared automatic 200-character limit and both compression mechanisms. It has its own limits: up to 1000 characters per record and up to 500 records per session. An admissible phrase is stored verbatim; an exact duplicate is not added again, and exceeding the limit is rejected before memory changes.

### 8.4 Search and live facts

`memory_search` performs the single explicit search at whole-`MemoryRecord` level across the recent, retained and waiting areas, the summaries and `SAVED_TEXT`. It returns a list of the most relevant `items`, bounded in count and size. `exactRecordCount` is available only while every match is still represented by individual records; on a match against a summary it is `null`, because an exact historical count is impossible after compression. `matchingUnits` reflects only the number of search units and is never used as a factual count. Every item keeps its provenance; if `truncated=true`, the list must not be presented as complete. Records and summaries are ranked by lexical and semantic match, then by the time of the source data.

The automatic `<facts>` block contains only live data from sources registered through `@RegisterMemoryFactSource`. Each source decides for itself whether it is relevant to the current utterance; the shared collector bounds the result to two facts per source and six facts per turn. Session-memory records, `EVENT`, `SAVED_TEXT` and summaries never enter this block. The pluggable `situation` source reports a stable English description of the current game situation from `PlayerSituation.i18nKey()`; the dialogue language does not affect this internal fact.

## 9. History in the prompt

`PromptComposer` replays recent completed records in their original roles:

- `DIALOGUE`: `user`, then `assistant`;
- `QUERY`: `user`, then `assistant` with the finished answer;
- `EVENT`: not replayed as a chat turn and reachable only by an explicit `memory_search`;
- `SAVED_TEXT`: not replayed as a chat turn and reachable only by an explicit `memory_search`.

`PromptComposer` forms exactly one `SYSTEM` message: the static rules first, then the dynamic `<facts>` at the very end. The current utterance stays the last `USER` message and is passed unwrapped when no `pending_clarification` is active. Clarification state is still appended to the current utterance in a separate `<context>`, because it is the continuation of an unfinished request rather than a game fact.

History contains no artificial turn boundaries, intermediate processing states or classification messages. Every visible fragment is therefore complete and valid for the chat protocol.

## 10. Speech

The companion passes finished text through `SpeechGateway`. The active voice engine accepts a concrete `VocalisationHandle` and must complete it on success, failure, cancellation or stop.

STT stays active during speech. A newly recognized utterance raises an interruption event, after which the controller separately interrupts speech and live thoughts. Software acoustic echo suppression is not part of this architecture.

## 11. Diagnostics

Every thought receives a `SOURCE#n` tag. The main diagnostic stages are:

- `intake` - the accepted text and the chosen path;
- `reduce` - the game-function candidates;
- `compose` - the number of system functions, facts and history records;
- `llm-http` - the time of the physical request;
- `llm` - the validated call or the reason for a repair;
- `settle` - the chosen settling path;
- `exec-time` - the handler's time;
- `memory` and `memory-search` - the number of results found;
- `done` - the thought's total time.

`CompanionMemoryDump` shows records by area and kind, preserving `MemoryRecord` boundaries and showing records awaiting consolidation separately.

## 12. Verifying changes

The minimum set of checks for architecture changes:

1. `:app:compileJava` and `:app:compileTestJava`;
2. `:app:test` for unit and integration checks that need no external model;
3. a targeted `:app:localIntegrationTest` for the chosen language set when a local model is available;
4. a memory-dump check: no partial records and no raw EVENT data, `QUERY` in paired form, records awaiting consolidation visible before the atomic commit;
5. a check of the diagnostic transcript of the model turn: one settling call and no classification stage.

A change to the memory rules counts as finished only when the record model, the store, history replay, diagnostics, documentation and tests have all been updated together.
