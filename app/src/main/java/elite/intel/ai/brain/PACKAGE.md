# `elite.intel.ai.brain` - Developer Reference

The brain package is the LLM layer of EliteIntel. It owns the full pipeline from raw STT transcript to dispatched game command or spoken query answer.

---

## Pipeline Overview

```
UserInputEvent (raw STT text)
    │
    ▼
[SttCorrector]              vocabulary-driven typo repair (Levenshtein)
    │
    ▼
[InputNormalizer]           synonym expansion + noise-word stripping (per language)
    │
    ▼
[AiActionsMap / Reducer]    build action map → keyword-reduce to candidate subset
    │
    ▼
[CommandEndPoint]           assemble system + user messages (AiPromptFactory)
    │
    ▼
[Provider endpoint]         HTTP call to LLM (Ollama / Anthropic / OpenAI / etc.)
    │
    ▼
[ResponseRouter]            parse JSON → dispatch to command or query handler
    │
    ├──▶ IntelCommand.execute()   side-effect (keystroke, UI change, …)
    │
    └──▶ IntelQuery.handle()
              │
              ▼
         [BaseQueryAnalyzer]  fetch session data → AiAnalysisInterface (second LLM call)
              │
              ▼
         AiVoxResponseEvent   spoken answer → TTS
```

Sensor events (`SensorDataEvent`) bypass the STT pipeline and go straight to the provider's analysis endpoint via
`CommandEndPoint.buildSensorMessages`.

---

## Stage 1 - Pre-LLM Input Processing

### `SttCorrector`

Runs first. Tokenises the raw transcript and applies Levenshtein distance against a vocabulary extracted from the current action-map keys. A token is only replaced when there is a
**single unambiguous best match** within a length-proportional ceiling (`< 5 chars → 0`, `5–6 → 1`, `7–9 → 2`,
`10+ → 3`). Ties are preserved unchanged to avoid false corrections.

### `InputNormalizer`

Singleton. Applies a language-specific synonym map (from `InputNormalizerLocalizations`)
to canonicalise phrases, then strips noise-word patterns. Runs after `SttCorrector`
so it always sees the corrected text.

### `AiActionsMap` / `Reducer`

`AiActionsMap.actionMap()` delegates to `AiActionMapGenerator`, which assembles the full
`phrase-group → action-id` map from `CommandRegistry` +
`QueryRegistry` (plus custom commands), filtered to the current session context and language.

`Reducer.reduce()` then keyword-filters that map down to the candidate subset whose trigger phrases share meaningful words with the normalised input. This keeps the system prompt small. When no candidates survive, the reducer falls back to
`ignore_nonsensical_input` (or `general_conversation` in conversation mode).

---

## Stage 2 - Prompt Assembly & LLM Call

### `CommandEndPoint` (abstract base, `commons/`)

All provider-specific `UserInputProcessor` classes extend this. It holds references to `AiPromptFactory` and
`AIRouterInterface` (resolved via `ApiFactory`). Key methods:

| Method | Purpose |
|---|---|
| `buildVoiceCommandMessages(userInput)` | Assembles `[system, user]` message array |
| `buildSensorMessages(event)` | Assembles `[system, instructions, user]` for sensor events |
| `tryProcessExactCustomCommandCommand(input)` | Short-circuits LLM for exact custom-command phrase matches |

### `AiPromptFactory` (interface)

Provider-specific implementations produce the system prompt. Key methods:

- `generateUserInputSystemPrompt(rawInput)` - includes the reduced action map and personality clause
- `generateSensorPrompt()` - sensor analysis system prompt
- `normalizeInput(rawInput)` - user-turn content (may re-normalise or pass through)
- `appendBehavior()` - injects the `ShipPersonality` roleplay clause

### `ShipPersonality` (enum)

Five personalities (`PROFESSIONAL`, `CASUAL`, `FRIENDLY`, `UNHINGED`, `ROGUE`). Each carries a
`behaviorClause` string appended verbatim to the system prompt.

### `AIChatInterface`

Single method:
`processAiPrompt(JsonArray messages, float temp) → JsonObject`. All provider command endpoints implement this.

---

## Stage 3 - Provider Inference Layer (`inference/`)

One sub-package per provider. Each follows the same four-class pattern:

| Class suffix | Implements | Responsibility |
|---|---|---|
| `*Client` | `Client`, extends `BaseAiClient` | HTTP transport, prompt parameter object, token-usage event |
| `*CommandEndPoint` | `AIChatInterface`, extends `AiEndPoint` | Command/intent classification call |
| `*UserInputProcessor` | `AiCommandInterface`, extends `CommandEndPoint` | EventBus subscriber; drives the pipeline on `UserInputEvent` |
| `*AnalysisEndpoint` | `AiAnalysisInterface` | Data analysis second-pass call |

Providers: `anthropic`, `deepseek`, `gemini`, `lmstudio`, `mistral`, `ollama`, `openai`, `xai`.

**Ollama dual-model split**: `OllamaClient` accepts an int selector (`MODEL_COMMANDS = 1`,
`MODEL_QUERIES = 2`). The command model uses `top_k 10` + short `num_predict`; the query model uses
`top_k 80` + longer context. Both share one HTTP client instance.

### `BaseAiClient`

Shared HTTP client (`HttpClient.HTTP_1_1`). Handles 400/401/429/500 error codes with
`AiVoxResponseEvent` feedback and exposes `cancelCurrentRequest()` (thread interrupt). Error responses carry
`text_to_speech_response` so `ResponseRouter` can detect and discard them with `isHttpErrorResponse()`.

### `AiEndPoint` (abstract, `commons/`)

Base for all endpoint classes. Provides:

- `sanitizeJsonArray` - strips non-role/content keys before sending
- `extractJsonFromContent` - brace-counting JSON extractor (handles markdown fences)
- `checkResponse` - validates OpenAI-style `choices[0].message.content`
- `isHttpErrorResponse` - sentinel detection

---

## Stage 4 - Response Routing (`commons/ResponseRouter`)

`ResponseRouter` is a singleton that holds the live handler maps built from
`CommandHandlerFactory` and `QueryHandlerFactory`. `processAiResponse(jsonResponse, userInput)`
does:

1. If `text_to_speech_response` present and no `action` → publish `AiVoxResponseEvent` directly (chat mode).
2. If `action` matches a command handler → `handleCommand` (speaks an affirmative, runs handler on a new thread).
3. If `action` matches a query handler → `handleQuery` (runs handler inline, speaks the returned
   `text_to_speech_response`).
4. Unknown action → logs a hallucination warning.

`executeCommandFromGUI(action, params)` bypasses STT and LLM entirely for UI-triggered commands and feeds directly into
`handleCommand`.

---

## Action System (`actions/`)

### `IntelAction` (interface)

The unified contract for both commands and queries:

```java
String id();

boolean isVisibleForLLM(Status status);   // default true

List<ActionParameterSpec> parameters();   // default empty

JsonObject handle(String action, JsonObject params, String text) throws Exception;
```

### `IntelCommand` extends `IntelAction`

Side-effect only. `handle` delegates to `execute(params, responseText)` and returns null. Extra metadata:
`isDangerous()`, `voiceStrategy()`, `bindingName()`, `kind()`.

Annotate with `@RegisterCommand` for automatic discovery. The optional `before`
attribute declares ordering constraints for the action-map topological sort.

### `IntelQuery` extends `IntelAction`

Returns a `JsonObject` with `text_to_speech_response`. Extend `BaseQueryAnalyzer`
to get the standard two-pass flow (handler fetches data → calls `AiAnalysisInterface`).

Annotate with `@RegisterQuery`.

### Registries

`CommandRegistry` and
`QueryRegistry` use Reflections to scan their packages at startup, instantiate each annotated class via no-arg constructor, and store by
`id()`.
`CommandHandlerFactory` / `QueryHandlerFactory` expose the merged maps that
`ResponseRouter` holds.

`CustomCommandRegistry` contributes user-defined commands to both the action map and the handler map.
`CommandHandlerFactory.refreshCustomCommandHandlers()` updates the live handler map in-place when custom commands change at runtime.

---

## Adding a New LLM Provider

1. Create `elite.intel.ai.brain.inference.<provider>/` with the four classes above.
2. Implement `AiCommandInterface` on the `*UserInputProcessor` - it will be registered via `SubscriberRegistration` or
   `EventBusManager.register(this)` in its constructor.
3. Implement `AiAnalysisInterface` on the `*AnalysisEndpoint`.
4. Register the provider in `ApiFactory` and wire it to the configuration toggle in
   `SystemSession`.
5. No changes to `ResponseRouter`, registries, or action handlers are needed.

## Adding a New Command

1. Create a class in `elite.intel.ai.brain.actions.command.builtin/` (or a sub-package).
2. Implement `IntelCommand`, provide a stable `id()`, implement `execute()`.
3. Annotate with `@RegisterCommand`.
4. Add localized trigger phrases to the alias bundles under `i18n/` for each language.
5. `CommandRegistry` discovers it automatically on next startup.

## Adding a New Query

1. Create a class in `elite.intel.ai.brain.actions.handlers.query/`.
2. Extend `BaseQueryAnalyzer`, implement `IntelQuery`, provide `id()` and `handle()`.
3. Annotate with `@RegisterQuery`.
4. Add localized phrases to alias bundles.
5. `QueryRegistry` discovers it automatically.

---

## Key Interfaces - Quick Reference

| Interface | Location | Role |
|---|---|---|
| `Client` | `brain/` | HTTP prompt builder + sender |
| `AiCommandInterface` | `brain/` | EventBus subscriber; drives pipeline per voice input |
| `AIRouterInterface` | `brain/` | Post-LLM response dispatcher |
| `AiPromptFactory` | `brain/` | System prompt generation |
| `AIChatInterface` | `brain/` | Single LLM chat call |
| `AiAnalysisInterface` | `brain/` | Second-pass data analysis call |
| `IntelAction` | `brain/actions/` | Unified command/query contract |
| `IntelCommand` | `brain/actions/command/` | Side-effect action |
| `IntelQuery` | `brain/actions/query/` | Data-returning action |

## Key Constants

| Constant | Value | Meaning |
|---|---|---|
| `AIConstants.TYPE_ACTION` | `"action"` | JSON key for the LLM-chosen action id |
| `AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE` | `"text_to_speech_response"` | JSON key for spoken text |
| `AIConstants.PARAMS` | `"params"` | JSON key for action parameters |
| `AiEndPoint.CONNECTION_CHECK_COMMAND` | `"command_verify_connection"` | Special action that tests LLM connectivity |