# VEGA Self-Diagnostic

A spoken check the commander can ask for when VEGA is talking but things aren't working as expected. It answers "is it me, my setup, or a bug?" without anyone opening the log.

Out of scope: a broken microphone, speech recognition or voice. If VEGA can't hear or can't speak, there is no conversation to diagnose, and the settings screen covers that case.

## Why

Support bundle of 2026-09-27 (fresh database, local gemma-4-e4b in LM Studio):

- The commander asked "do we have llm connection". No game action matched (best 0.836, under the 0.85 floor), so the model answered as conversation: "I do not have an LLM connection." The model itself said it, so the connection was fine. The model just doesn't know it is the LLM.
- One turn earlier, "looks like we have a lm connection right" scored 0.866, just over the floor. It was offered eight unrelated tools and ran `query_ship_loadout`.
- The case that matters most in practice: LM Studio isn't running, or a cloud provider is down, while reflexes keep working. The commander hears commands being carried out and decides the failing ones are bugs.

## What exists today

`ConnectionCheckQuery` (`command_verify_connection`) is a connection probe left over from the legacy path. It can't do this job:

- **VEGA never offers
  it.** It sits in `GameToolCandidates.EXCLUDED_IDS` and `WordOverlapActionReducer.FALLBACK_IDS`, and it has no aliases, so neither the reflex nor the model can reach it.
- **It probes the wrong
  model.** It calls `getAnalysisEndpoint().verifyConnection()`, which sends a chat request to each provider's
  *analysis* model (`MODEL_ANALYSIS_MODEL`, `MODEL_GPT`, `MODEL_QUERIES`...), not the command model VEGA actually runs on. A cloud setup can pass the probe while VEGA's model fails.
- **It passes with no model
  configured.** In the bundle, the startup probe went out with an empty model name (`LM Studio request -> model: `), and LM Studio answered with whatever it had loaded. "Answered" is not the same as "configured".
- **It is slow and
  unbounded.** That startup probe generated 569 completion tokens (about 3 seconds) because nothing caps the reply.
- **It gives no reason.** "Connection check failed. Probable cause: settings are incorrect or LLM service is off-line."

`AppController.connectionCheck()` uses the same `verifyConnection()` for the GUI status light and its 30-second retry timer.

Things we can build on:

- A query result carrying `AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE` is spoken verbatim (`Thought.spokenTextOf`), with no LLM narration.
- `ReflexResolver` fires a parameterless safe action with no model involved.
- `SetupCheck` already checks for a configured LLM, a chosen cloud provider, journal files and a `.binds` file, and it already has spoken templates (`speech.setup.*`).
- `CommanderThought.onUnusableResponse` and `BaseAiClient.transportFailurePhrase` already speak fixed phrases when a turn's LLM call fails.

## Design rules

1. **It works when the LLM is
   broken.** The report is built in Java from localized templates and returned as `PROPERTY_TEXT_TO_SPEECH_RESPONSE`. The model never writes it. The trigger phrases are reflex-fireable, so they never need the model to route them.
2. **Report what we saw, never what the provider
   claims.** Cloud HTTP codes lie: Mistral answered `429 rate_limited` while a whole cluster of models was off for everyone for about a week. The only split we trust is whether the request reached the provider at all (DNS, connection refused, timeout) versus any HTTP answer. After that, we report what we saw: when the last request worked, how long it has been failing, and how many attempts in a row. A local check (LM Studio's own model list) is trustworthy in a way a cloud code is not.
3. **Problems first,
   briefly.** At most three problems are spoken, most serious first. All clear is one sentence. The full detail goes to the app log (`AppLogEvent`), never into speech.
4. **No identifiers in
   speech.** Say the provider's display name from `ProviderEnum`, never a raw model id like `google/gemma-4-e4b`. Name the model only as "the recommended model" or "not the recommended model". No file paths, no keys, no key fragments.
5. **Never the word "
   system".** Not in phrases, templates or action names, in any language. The game already uses the word for ship systems and star systems.

## The checks

They run in dependency order. When a check fails, the ones that depend on it are skipped, not reported as failed too.

### 1. AI model (built in phase 1)

Code: `elite.intel.ai.brain.health` (`AiServiceCheck`, `AiServiceHealth`, `AiServiceVerdict`, `AiServiceReport`). Templates: `query.selfDiagnostic.*` in `responses*.properties`, in all nine languages.

| Situation | How we know | Spoken (English) |
|---|---|---|
| Nothing configured | `SetupCheck.isLlmUnconfigured()`, or local with no model named | "No AI model is set up yet. Choose one on the AI Services settings tab." |
| Cloud chosen, no provider | `LlmProviderResolver.cloudProvider()` is empty | "You are set up for a cloud AI, but no provider is chosen. Pick one on the AI Services settings tab." |
| LM Studio not answering | No HTTP answer to its model list, or to the test request | "LM Studio is not answering. Check that it is running and that its local server is started." |
| LM Studio up, model not available | The configured model isn't in LM Studio's model list | "LM Studio is running, but it does not have the model you chose. Check the model name on the AI Services settings tab." |
| Cloud unreachable | The test request got no HTTP answer | "I cannot reach Mistral at all. Check your internet connection." |
| Answering but refusing | The test request got a non-2xx answer | "Mistral is answering, but it refused my test request." When the refusals started at least a minute before the check: "...but it has refused every request for 2 hours." A 401 or 403 adds "If you changed your API key recently, check it on the AI Services settings tab." |
| No answer in time | Nothing came back within 10 seconds | "OpenAI did not answer within 10 seconds." |
| All good | The test request got a 2xx | "LM Studio is answering. Replies take about 1 second." For a local model other than gemma-4-e4b, it adds "It is running a model other than the recommended one, so some commands may go wrong." |

The local model name is never spoken. Providers are spoken by `ProviderEnum` display name with the brackets removed ("Anthropic Claude"). The reply time is the median of the last five answers of any kind, so one long narration can't skew it. There is no "running on the processor" hint: we can't see that, so we don't say it.

**The probe.** It goes through a short-lived gateway from `VegaLlmGatewayFactory`, so it tests the
**command** model VEGA runs on, not the analysis model:

- **LM
  Studio:** first its model list at `.../v1/models`, derived from the stored chat address (skipped when the address has another shape). Then one plain-text request.
- **Cloud:** one plain-text request.
-
**Limits:** its own prompt profile (`CONNECTION_PROBE`), a one-line prompt, and a 10-second wait. That's long enough for LM Studio to load a model on first use. The gateway's own resend ladder still runs inside those 10 seconds.

**Health
record (`AiServiceHealth`).** This is the one new piece of state: one in-memory record for whichever provider is active. It holds:

- the last exchange and how it ended (answered, unreachable or refused)
- when the current failure run began
- the status of the last refusal
- the last five reply times

It is fed from `BaseAiClient.sendTransportRequest`, the single place every provider exchange passes through. The check reads the verdict from this record after its test request, because the gateway folds every failure into the same empty reply. It is reset whenever the services start or the LLM service restarts, which is how a provider change takes effect.

### 2. Game link

| Situation | How we know | Spoken |
|---|---|---|
| No journal files | `SetupCheck.hasJournalFiles` | Reuse `speech.setup.noJournals` |
| Journal found, game quiet | No journal or Status.json update in N minutes | "I haven't seen the game in forty minutes. Is it running?" This is information, not a fault |
| No commander yet | Open commander file is `pending` | "I haven't seen a commander load yet." |
| All good | Recent events | "I'm reading your game." |

### 3. Controls

| Situation | How we know | Spoken |
|---|---|---|
| No `.binds` file | `SetupCheck.hasBindingsFiles` / `isEliteBindingsFolder` | Reuse `speech.setup.noBindings` / `noSavedBindings` |
| Blocking conflicts | The existing blocking-conflict detection (game menu key, UI_Select vs QuickComms, interface switch) | "Two of your key bindings clash in a way that stops my commands. Check the bindings tab." |

Phase 2 could add a count of key commands with no binding. Leave it out until we know which ones count as "key".

### 4. Hearing

Only when it's marginal. If it were broken, the commander couldn't have asked.

| Situation | How we know | Spoken |
|---|---|---|
| Never calibrated | No calibration stored | "I haven't calibrated your microphone yet." |
| Voice too close to background noise | Trigger level vs noise level gap below a threshold (the bundle had 23 dB) | "Your voice is only just above the background noise, so I may miss words." |

The threshold needs real data before it ships. Counting recently rejected captures would help too, but needs a counter in the STT path, so it is phase 2.

## The action

- **Id:** `query_vega_self_diagnostic` (`SelfDiagnosticQuery`), following the long, descriptive naming rule.
-
**Replaced `command_verify_connection`.** `ConnectionCheckQuery`, `AiEndPoint.CONNECTION_CHECK_COMMAND`, `AiAnalysisInterface.verifyConnection()` and its seven implementations, and the `probeConnection`/`probeChatStyle` helpers are gone, along with the `speech.connectionFailed` line.
- **The startup check uses it
  too.** `AppController.connectionCheck()` runs `AiServiceCheck` for the AI tab's status light and its 30-second retries. A failure at startup now speaks the verdict ("LM Studio is not answering...") instead of the generic "Connection check failed" line. A setup gap stays silent there, because `SetupCheck` has just said it. Success still says "Connection successful."
- **Offered to the LLM as well as the
  reflex.** A paraphrase still routes while the model works, and a trained phrase fires even when it doesn't.
- **Parameterless, not dangerous, visible in every game state.**
- **The answer is recorded to memory like any query
  answer.** If the commander later asks "so what was wrong?", the model has it.

## Phrases (English)

- run a diagnostic / run diagnostics / diagnostics
- self diagnostic / run a self diagnostic / self test / run a self test
- check yourself / health check
- are you online / are you working properly
- is the AI connected / check your connection / check the AI connection / do we have an AI connection / do we have LLM connection / check the LLM connection

All nine locales have their own phrases in `ai_action_aliases*.properties`, and none collides with another action's. `ReflexAliasFormsTest` pins four of the English ones to this action.

Avoid:

- **"system" / "systems"** in any language: *sistema*, *System*, *système*, *система*...
- **bare "status"**, which clashes with ship status.
- **"comms check" and "radio check"**, which clash with the comms panel and the carrier radio voice.
- **"can you hear me"**, which is usually said to Discord or a wingmate.

Every phrase must belong to this action alone. A phrase shared with another action can never reflex-fire. The other eight locales get their own phrases written from scratch, not translated from this list. "Self diagnostic" and "self test" have clean equivalents everywhere.

## Related changes

These are small and independent of the action, and each could ship on its own.

- **Failure phrases name the
  provider.** `handler.common.aiServiceUnreachable` becomes "I can't get an answer from {0}." It drops "try again in a moment", which was wrong for a week during the Mistral outage. `aiServiceRejected` keeps the key hint but as a possibility, not a verdict. Watch the MessageFormat apostrophe trap when adding `{0}`.
- **The model knows what it
  is.** A few words in `VegaIdentity.identityClause()`, such as "you are the language model the app is talking to", so a casual question that misses the reflex doesn't get "I do not have an LLM connection". Pay for them by cutting the same amount elsewhere in the prompt.

## Tests

Phase 1 has:

- `AiServiceHealthTest`: failure runs, reset on an answer, median reply time.
- `AiServiceCheckTest`: every verdict, from a fake gateway that records outcomes the way the transport does.
- `AiServiceReportTest`: the English sentences and Russian plurals.
- `LMStudioModelListTest`: the models address, parsing, and model-name matching.
- `ReflexAliasFormsTest`: pins the phrases.

No test makes a live cloud call. When later phases add checks, the report needs tests for the ordering, the three-problem cap, and skipping dependent checks.

## Phases

1.
**Done.** AI model check, health record, the action and retiring `ConnectionCheckQuery`, with phrases and templates in all nine languages (the i18n parity test allows no new gaps). This alone covers the "LM Studio is down but reflexes work" case.
2. Game link, controls and hearing checks.
3. No longer a phase of its own: `BundleKeyParityTest` allows no untranslated key, so each phase ships its phrases and templates in all nine languages.
4. The failure-phrase and identity-clause changes, which can come earlier if wanted.

## Open questions

- **Target
  branch.** Phase 1 went onto `V1.1-Release` as maintenance, since it fixes a misleading answer on fresh installs. Phases 2 and 4 are still open.
- **Should the failure phrase point to the
  diagnostic?** For example, "...say 'run a diagnostic' for details". Or is naming the provider enough?
- **Quiet-game
  threshold.** How long without journal or Status.json updates before "haven't seen the game" is worth saying?
- **Hearing threshold.** Which trigger-to-noise gap counts as marginal?
