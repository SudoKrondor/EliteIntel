# Upstream Sync Report

## Summary

Fetched `stone-alex/EliteIntel` (`upstream`), merged 48 new `upstream/V1.1` commits into local
`V1.1`, resolved 2 conflicts, verified StarVizion and Push-to-Talk (PTT) code still builds and
tests pass, and pushed the result to `origin/V1.1`.

## Before merge

- `git fetch upstream` pulled `upstream/V1.1` from `5faf245e` → `b6ff071d` (48 new commits),
  plus several new branches/tags on `upstream` unrelated to this sync.
- Local `V1.1` was **6 commits ahead** of `upstream/V1.1`, **48 commits behind**
  (`git rev-list --left-right --count V1.1...upstream/V1.1` → `6 48`).
- Merge base: `5faf245e`.
- Local-only commits (since merge base):
  - `520fa489` — StarVizion prototype (SDL3 overlay + controller input)
  - `ae77e5aa` — Merge upstream V1.1 into local V1.1
  - `cb9a13c7` — debug: StarVizion SDL3 device detection logging
  - `376cbc1b` — Merge upstream V1.1 into local V1.1
  - `c927513a` — feat: push to talk via SDL3 controller button with Input settings tab
  - `13026367` — docs: PTT implementation record

## Upstream changes pulled in

48 commits, `b6ff071d` (`Fixing Kokoro crash`) as new tip, 167 files changed
(+12,260 / -2,632). Highlights:

- Full HUD-style UI redesign: new `Hud*` Swing components (`HudPanel`, `HudSection`,
  `HudTabbedPane`, `HudButton`, `HudTable`, `HudSearchField`, `TopStatusBar`,
  `HudUpdateButton`, etc.) and a heavily reworked `AppTheme.java` (+/-693 lines) and
  `AppView.java`.
- "Rework AI Tab" parts 1-6, "Rework Player Tab", "Rework topbar", "Rework
  Tables & Shortcuts", new `CustomSettingsTabPanel` + custom-commands tab rework.
- New Neutron Star route plotter (Spansh client + DB-backed route manager + commands/queries).
- New "Plot route to nearest Interstellar Factors" + "Clear active mission(s)" commands.
- i18n overhaul: per-language packages (`ai/brain/i18n/{de,en,es,fr,ru,uk}/...`) for action
  aliases, input-normalizer rules, and prompt rules; `InputNormalizer.java` shrunk from ~700
  lines to a thin dispatcher over these.
- TTS: Kokoro fix, Google voice provider reorganized under `ai.mouth.google`.
- Test suite reorganized under `elite.intel.junit.prompt` (was `elite.intel.test`), new
  FR/RU natural-speech integration tests.
- New DB migration `01002__schema.sql`.

## Conflicts and resolution

Only 2 of the 167 changed files conflicted — both because our local commits touched the same
lines:

1. **`.gitignore`** — both sides *added* different blocks at the same location (ours:
   `/PACKAGES.md`; upstream: a "SECURITY — never commit these" block for `.cursor`/`.claude`/
   `.gemini`/`.github`/`.vscode` files). Resolved by keeping both additions.

2. **`app/src/main/java/elite/intel/ui/view/SettingsTabPanel.java`** — only the `dispose()`
   method conflicted:
   - Ours added `inputPanel.dispose() + EventBusManager.unregister(this)` (for the new Input
     tab from the PTT feature).
   - Upstream removed `EventBusManager.unregister(this)` entirely (the panel no longer
     self-registers on the bus) and added `if (updateAppButton != null)
     updateAppButton.dispose()` (new `HudUpdateButton`).
   - Resolved to: `inputPanel.dispose(); if (updateAppButton != null)
     updateAppButton.dispose();` — keeps `InputSettingsPanel`'s required unregister (it still
     self-registers in its own constructor) and adopts upstream's new button disposal.

Everything else — including `app/src/main/resources/i18n/gui.properties` (both sides changed
it: ours added the 7 `settings.input.*` / `settings.tab.input` keys, upstream did a large i18n
restructure) and `app/src/main/java/elite/intel/ui/view/AppView.java` — auto-merged cleanly.

## Working-tree handling

The working tree had uncommitted changes (from earlier, unrelated StarVizion Keyboard/Counter
Vizlet work) to `app/src/main/resources/i18n/gui.properties`, which would have collided with
the incoming merge changes to that same file. These were stashed
(`git stash push -- app/src/main/resources/i18n/gui.properties`) before merging, then restored
via `git stash pop` after the merge commit — it auto-merged cleanly with no conflicts. The
other 3 uncommitted StarVizion files (`StarVizionTabPanel.java`, `SdlInputService.java`,
`VizletWindow.java`) were untouched by the merge (upstream's 48 commits don't touch the
`starvizion` package at all) and remain uncommitted exactly as before.

## Post-merge verification

- **StarVizion package intact**: all 15 files under `app/src/main/java/elite/intel/starvizion/`
  present in the merge commit (events, model, input, overlay, tab panel).
- **PTT/Input tab intact**: `InputSettingsPanel.java` present;
  `SettingsTabPanel.java` still wires the `inputPanel` field, `tabs.addTab("settings.tab.input",
  ..., inputPanel)`, `inputPanel.initData()`, and `inputPanel.dispose()`.
- **`AppTheme` API compatibility**: all helpers `InputSettingsPanel` depends on
  (`baseGbc`, `nextRow`, `addLabel`, `addField`, `bindLock`, `BUTTON_BG`, `BUTTON_FG`,
  `FG_MUTED`, `ACCENT`, `DISABLED_FG`) still exist in the reworked `AppTheme.java`.
- `./gradlew compileJava` → **BUILD SUCCESSFUL**.
- `./gradlew test` → **BUILD SUCCESSFUL** (default suite, excludes `local-integration`).
- No leftover conflict markers anywhere in the tree (`git grep` for `<<<<<<<`/`=======`/`>>>>>>>`
  returned nothing).

## Push

- Merge commit: `a7194e6c` ("Merge remote-tracking branch 'upstream/V1.1' into V1.1").
- Pushed to `origin/V1.1`: `13026367..a7194e6c`.
- `upstream` was only fetched, not pushed to.

## Outstanding / follow-ups

- The merge brings in a major HUD/AppTheme redesign. `InputSettingsPanel` still compiles and
  uses the *old* `GridBagLayout`-based section styling (`baseGbc`/`addLabel`/`addField` +
  `LineBorder`), which upstream's other settings panels (`SettingsTabPanel`,
  `CustomSettingsTabPanel`, etc.) have now moved to `HudSection`/`AppTheme.makeStandardTabs()`.
  The Input tab is functionally intact but visually may now be inconsistent with the
  redesigned Settings shell — a follow-up visual pass to restyle `InputSettingsPanel` using
  the new `Hud*` components would bring it in line.
- The pre-existing uncommitted StarVizion Keyboard/Counter Vizlet changes (3 `.java` files +
  `gui.properties` additions + 4 new untracked files) remain uncommitted, as before — out of
  scope for this sync.
