# Sync — 2026-06-18

## Summary

Fetch-and-check only — **no pull, no merge performed.** Checked `origin/V1.1` and
`upstream/V1.1` against the last known tip `432a3172` (from the prior sync). Both remotes have
advanced to the same new tip, `368c6b34`, 28 commits ahead of `432a3172`. Local `V1.1` was not
fast-forwarded and the working branch was not touched.

## Fetch results

```
git fetch origin V1.1     -> 432a3172..368c6b34  V1.1 -> origin/V1.1
git fetch upstream V1.1   -> 432a3172..368c6b34  V1.1 -> upstream/V1.1
```

`origin/V1.1` and `upstream/V1.1` are identical (`368c6b34fccf890f454761d6b9dd3b9269a02250`).

## Current state (as of this check)

- Local `V1.1`: still at `432a3172` — **28 commits behind** `upstream/V1.1`
  (`git rev-list --count 432a3172..upstream/V1.1` -> `28`).
- Working branch `V1.1-KAN-6-push-to-talk-dawntreader` (HEAD `b90be73d`, "Merge branch 'V1.1'
  into V1.1-KAN-6-push-to-talk-dawntreader"): **5 commits ahead**, **27 commits behind** local
  `V1.1` (`git rev-list --left-right --count V1.1-KAN-6-push-to-talk-dawntreader...V1.1` ->
  `5 27`) — i.e. the working branch has not yet absorbed the previously-fetched portion of
  local `V1.1`, on top of which the 28 new upstream commits would also need to be pulled in.

## New commits on upstream/V1.1 (28, oldest -> newest)

| Commit | Summary |
|---|---|
| `72bd879c` | FIX - comma-separated aliases are not exact-matched individually |
| `5df3e673` | Merge `refs/heads/V1.1` into V1.1-french-i18n |
| `a65fed6d` | Merge V1.1 into V1.1-french-i18n |
| `77d5d69e` | Adding framework for Portuguese language |
| `ec1d8795` | Merge V1.1 into V1.1-french-i18n |
| `262e3572` | Merge V1.1 into V1.1-i18n-pt |
| `e43a3563` | audio settings i18n for PT |
| `74a8a703` | FR - Revamp classification and add commentary |
| `a301e7c1` | Merge V1.1 into V1.1-french-i18n |
| `16b7d1f5` | FR - Fix commodity french |
| `d32923ce` | fast test. EN pass 339 test tulu |
| `13843306` | fast test. EN pass 339 test tulu and Mistral |
| `a076c0d2` | FR - Fix multiple choices TTS gui.properties |
| `d5662fce` | FR - Improved French disambiguation |
| `d9e0b361` | FR - Improved French disambiguation |
| `9a07e7c1` | Merge V1.1 into V1.1-french-i18n |
| `8d04640d` | Rusish |
| `17e5f3b5` | FR - Fix NaturalSpeechIntegrationTestFR for LMS matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF test PASS |
| `80d3d8e3` | adjustments for Gemma 4 |
| `f14b60c2` | commands rework part 1 |
| `f5fb06c1` | Merge V1.1 into V1.1-i18n-pt |
| `1d8613c0` | commands rework part 2 |
| `54dfe95c` | Merge `refs/heads/V1.1-i18n-pt` into V1.1-i18n-IT |
| `0e568057` | adding Italian localization scaffold |
| `e0f2270d` | adding Italian localization scaffold |
| `24e5fa72` | adding Italian localization scaffold |
| `7c41f72c` | Merge V1.1-french-i18n into V1.1-i18n-IT |
| `368c6b34` | fixing FrenchPromptRules post refactoring |

Mostly French/Portuguese/Italian i18n work and LLM-routing test passes. Two commits flagged for
possible overlap with active work: `f14b60c2` ("commands rework part 1") and `1d8613c0`
("commands rework part 2") — not yet inspected in detail.

## Commit detail review — `f14b60c2` / `1d8613c0` ("commands rework part 1/2")

Inspected in full (`git show --stat` + targeted content `grep` + manual diff review) before
deciding whether to pull. **Still not pulled or merged — investigation only.**

### `f14b60c2` — "commands rework part 1"

252 files changed, +5629/-2533. This is a **wholesale replacement of the `Commands` enum-based
command pipeline** described in `CLAUDE.md`'s "Adding a new voice command" section, not an
incremental change.

**What actually changed:**
- `elite.intel.ai.brain.actions.Commands` (the 226-entry enum) is **deleted entirely**.
- New package `elite.intel.ai.brain.actions.command` replaces it:
  - `CommandIds` — plain `String` constants (e.g. `INTERRUPT = "interrupt"`,
    `SWITCH_TO_COMBAT_MODE = "switch_to_combat_mode"`) — **verified the string values are
    byte-for-byte identical to the old enum's action strings**, so LLM-prompt/i18n-alias
    matching is preserved.
  - `IntelCommand` — new interface each command implements directly (replaces the old
    `CommandHandler` class-per-enum-entry indirection).
  - `CommandRegistry` + `@RegisterCommand` — annotation-driven self-registration
    (`CommandRegistry.getInstance().load()`, now called from `App.java`), replacing
    `CommandHandlerFactory`'s old reflective enum-driven instantiation.
  - `CommandKind`, `CommandParamRules`, `VoiceStrategy`, `SimpleTapCommand`,
    `RegisterCommand` — supporting plumbing for the new model.
  - ~145 new files under `actions/command/builtin/` (one class per command, e.g.
    `SleepCommand`, `WakeupCommand`, `DeployHardpointsCommand`, `FindMiningSiteCommand`, ...),
    each replacing an old `actions/handlers/commands/*Handler.java` (~50 old handler files
    deleted).
  - `CommandHandlerFactory.registerCommandHandlers()` rewritten to pull from
    `CommandRegistry.getInstance().byId()` instead of iterating the `Commands` enum and
    reflectively instantiating `getHandlerClass()`.
  - `ResponseRouter`, `AiActionsMap`, `Reducer`, `InterdictionHandler`, `ParakeetSTTImpl` —
    all updated from `Commands.X.getAction()` references to `CommandIds.X` constants
    (mechanical reference swap, confirmed same string values, e.g. `INTERRUPT_TTS`/
    `ACTIVATE_COMBAT_MODE`/`SELECT_HIGHEST_THREAT` → `INTERRUPT`/`SWITCH_TO_COMBAT_MODE`/
    `TARGET_HOSTILE_HIGHEST_THREAT`).
  - i18n: `AiActionAliasProvider.java` (282 lines touched) and most `*PromptRules.java`
    files updated for the new constant references; `llm*.properties` minor additions.
  - Test suites (`CommandCatalogTest`, `NaturalSpeechIntegrationTestEN/FR/PT/RU`) reworked to
    match the new model.

**Binding/PTT/brain-pipeline/SDL3 overlap check (full diff body, not just filenames):**
- **Binding system** (`KeyBindingsParser`, `BindingsApplyService`,
  `BindingsWorkingCopyRepository`, `BindForgeTabPanel`, `BindingsMonitor`, `KeyBindCheck`):
  **zero references** anywhere in this commit's diff.
- **PTT system**: two old handler files touched `SystemSession.isPushToTalkEnabled()` /
  `isPushToTalkToggleMode()` — `IgnoreMeHandler` and `StartListeningHandler`. Both are
  **deleted and replaced 1:1** by `SleepCommand` and `WakeupCommand` respectively, with the
  **exact same body** (same `SystemSession` calls, same `PttModeChangedEvent`/
  `VoiceInputModeToggleEvent` publishes) — just re-homed into the new `IntelCommand`
  interface (`ownsExecution() == true`). No `InputSettingsPanel` or `PushToTalkHandler`
  references anywhere in the diff — this is a pure relocation, not a PTT logic change.
- **Brain/command pipeline** (handlers, EventBus subscribers): this **is** the pipeline —
  see above. `CommandHandlerFactory`, `ResponseRouter`, `AiActionsMap`, `Reducer` are all
  touched, but only to swap the lookup/registration mechanism; the action-string contract
  (the thing `Reducer`/LLM prompts/i18n aliases depend on) is preserved.
- **SDL3 / device-input classes**: zero references anywhere in this commit.

### `1d8613c0` — "commands rework part 2"

71 files changed, +928/-402. Same rework, mirrored for the **`Queries` enum** instead of
`Commands`:
- `elite.intel.ai.brain.actions.Queries` (75 lines) **deleted**.
- New package `elite.intel.ai.brain.actions.query`: `IntelQuery`, `QueryIds`, `QueryRegistry`,
  `RegisterQuery`, `QueryI18nKeys` — same annotation-self-registration pattern as part 1.
- ~48 `*QueryHandler.java` files under `actions/handlers/query/` renamed in place to
  `*QueryCommand.java` and retrofitted with `@RegisterQuery` + `IntelQuery.id()` (e.g.
  `AnalyzeFsdTargetHandler` → `AnalyzeFsdTargetQueryCommand`) — **logic bodies unchanged**,
  confirmed via diff (only added imports + the `id()` override + interface swap).
- `QueryHandlerFactory` rewritten to pull from `QueryRegistry.getInstance()` instead of
  iterating the `Queries` enum.
- New `QueryRegistryTest`; `NaturalSpeechIntegrationTestEN/FR/PT/RU` and
  `HeadlessBootstrap` updated.

**Binding/PTT/brain-pipeline/SDL3 overlap check:**
- **Binding system**: one filename hit — `AnalyzeMisingKeyBindingHandler.java` renamed to
  `AnalyzeMisingKeyBindingQueryCommand.java`. Diffed in full: this is the *query* that reports
  missing key bindings to the user via voice, not the binding system itself. It still calls
  `KeyBindingManager.getInstance()` exactly as before — **no changes to
  `KeyBindingsParser`/`BindingsApplyService`/`BindingsWorkingCopyRepository`/
  `BindForgeTabPanel`/`BindingsMonitor`/`KeyBindCheck`** anywhere in this commit.
- **PTT system**: zero references.
- **Brain/command pipeline**: this is the `Queries`-side mirror of part 1's rework — same
  registration-mechanism swap, same action/query-string-contract preservation pattern
  (spot-checked `KEY_BINDINGS_ANALYSIS` and others).
- **SDL3 / device-input classes**: zero references anywhere in this commit.

### Assessment

Neither commit touches the binding system, PTT system, or SDL3/device-input layer in any
way that changes behavior — the only PTT-adjacent code is a verbatim relocation. However,
**both commits are a significant, repo-wide architectural change to the command/query
dispatch pipeline** that `CLAUDE.md` currently documents in enum terms ("Add an enum constant
to `elite.intel.ai.brain.actions.Commands` (or `Queries`)..."). If these are pulled, that
section of `CLAUDE.md` will describe a pipeline that no longer exists, and any in-progress or
future work that adds commands/queries the "old way" would need to follow the new
`@RegisterCommand`/`@RegisterQuery` pattern instead. Worth a wider regression pass (full
`./gradlew test` + a manual voice-command smoke test) before merging into the working branch,
given the size and blast radius of the change, even though the action-string contract itself
appears preserved.

## Sync executed

Proceeded with the sync after the commit-detail review above found no behavioral overlap with
the binding system, PTT system, or SDL3/device-input layer.

**Step 1 — fast-forward local `V1.1`:**
```
git checkout V1.1
git merge --ff-only origin/V1.1
```
Fast-forwarded cleanly, `432a3172` → `368c6b34`. No local-only commits existed on `V1.1`
(confirmed `0` unique commits in the earlier fetch check), so this carried zero risk of losing
work.

**Step 2 — merge `V1.1` into the working branch:**
```
git checkout V1.1-KAN-6-push-to-talk-dawntreader
git merge V1.1 --no-edit
```
Working tree was clean apart from the same pre-existing untracked `dawntreader-docs/` files
noted in earlier syncs (none overlap with `app/` source touched by this merge). **Merge
completed with zero conflicts** — merge commit `73f84f95` ("Merge branch 'V1.1' into
V1.1-KAN-6-push-to-talk-dawntreader"). Confirmed no `UU`/`AA`/`DD` conflict markers in
`git status` and no leftover `<<<<<<<`/`=======`/`>>>>>>>` markers.

Notable structural changes pulled in beyond the commands/queries rework already reviewed:
`BindingsTabPanel.java` → `BindForgeTabPanel.java`, `PlayerTabPanel.java` →
`CommanderTabPanel.java`, new `SpectralNoiseReducer.java`, new Italian/Portuguese i18n
packages, new migrations `01005__schema.sql` and `01007__schema.sql`, new
`BindingsApplyServiceTest`/`BindingsWorkingCopyRepositoryTest`.

## Post-merge test run

```
./gradlew test
```

**Result: `BUILD SUCCESSFUL` in 25s.**

| Metric | Count |
|---|---|
| Total tests | 219 |
| Failures | 0 |
| Errors | 0 |
| Skipped | 0 |

All 17 test-result XML files report zero failures/errors. **No tests failed** — nothing to
attribute to the commands rework (`f14b60c2`/`1d8613c0`) or otherwise. The new
`QueryRegistryTest`, `BindingsApplyServiceTest`, `BindingsWorkingCopyRepositoryTest`,
`MultiLingualTextProviderTest`, and `NaturalSpeechIntegrationTestIT`/`...PT` (new from this
batch) all passed alongside the existing suite.

## Push

**Not pushed**, per instruction. `V1.1` and the working branch both remain local-only ahead of
their `origin` counterparts.

## Outstanding / follow-ups

- `CLAUDE.md`'s "Adding a new voice command or query" section is now **out of date** — it
  documents the deleted `Commands`/`Queries` enum pattern. Update is a separate, explicitly
  deferred step per instruction — not done in this pass.
- Consider a manual voice-command smoke test in addition to the automated suite, given the
  size of the commands/queries rework, before pushing.
