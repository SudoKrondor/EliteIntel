# Upstream Sync Report

## Summary

Fetched `stone-alex/EliteIntel` (`upstream`), found local `V1.1` 36 commits behind
`upstream/V1.1`, merged the 19 of those commits not already present on
`V1.1-KAN-6-push-to-talk-dawntreader` into this branch, verified `git lfs pull` is clean,
and confirmed `./gradlew shadowJar` still builds. **Not pushed.**

## Before merge

- `git fetch upstream` pulled `upstream/V1.1` from `87241249` -> `dc828423`, plus new
  branches (`V1.1-KAN-57-elite-intel-devices-dawntreader`, `V1.1-starvizion-prototype-dawntreader`)
  and updates to `V1.1-french-i18n`, `V1.1-KAN-6-push-to-talk-dawntreader` on `upstream`.
- Local `V1.1` was **36 commits behind** `upstream/V1.1`
  (`git rev-list --count V1.1..upstream/V1.1` -> `36`).
- Of those, **19 commits were not yet on** `V1.1-KAN-6-push-to-talk-dawntreader`
  (`git rev-list --count HEAD..upstream/V1.1` -> `19`); the remaining 17 were merge/feature
  commits already present on this branch via earlier syncs.
- Before this merge, the branch tip (`55144d70`) was **2 commits ahead** of `upstream/V1.1`
  and **19 commits behind** (`git rev-list --left-right --count HEAD^1...upstream/V1.1` ->
  `2 19`).
- Merge base: `e03b9347` ("fixing the button behavior").
- Local-only commits (since merge base):
  - `55144d70` — Merge `origin/V1.1-KAN-6-push-to-talk-dawntreader` (earlier same-session sync)
  - `58822bbf` — fix: reduce honk fire-group selector from A-Z to A-H

## Upstream changes pulled in

19 commits, `dc828423` as new tip, 38 files changed (+3,001 / -971). Highlights:

- **SETTINGS tab restructure**: new `settings/CommonSettingsPanel.java` (renamed from
  `CustomSettingsTabPanel.java`), `LocalLlmSettingsPanel.java`, reworked
  `SettingsTabPanel.java`, COMMON panel + section tabs + canon sizing.
- **AUDIO tab rework**: segmented mic meter (`HudMicMeter.java`, new), centralized banners,
  `AudioDeviceCombo.java` (new), `AudioWaveformPanel.java` removed,
  `AudioSettingsPanel.java`/`AudioInterfaceDialog.java` heavily reworked.
- New HUD controls: `HudSlider.java`, `HudStepper.java`, `HudFooter.java`; `HudButton.java`,
  `HudModalScaffold.java`, `HudTabbedPane.java`, `HudTextField.java` extended;
  `AppTheme.java` +301/-? lines.
- Custom Command Editor recomposed around `HudStepper`
  (`CustomCommandEditorDialog.java`, `CustomCommandStepEditorDialog.java`).
- `BindingsTabPanel.java` reworked to use the new HUD footer/field-picker components
  (-218 lines net).
- i18n: RU adjustments (carrier market balance prompt, refuel possible/impossible),
  `RussianAiActionAliases.java`, plus updates across `gui*.properties` for all 6 languages
  and `ed_events*.properties`.
- New localized user manuals: `distribution/user-manual-{de,es,fr,ru,uk}.md`.
- Small fixes: `AnalyzeFleetCarrierDataHandler.java`, `FSDTargetSubscriber.java`,
  `InputSettingsPanel.java` (+10/-? from upstream's "fixing the button behavior").

## Conflicts and resolution

None. `git merge upstream/V1.1` completed via the `ort` strategy with **zero conflicts** —
no files were touched by both the local-only commits (`ShipSettingsPopup.java`,
`IgnoreMeHandler.java`-adjacent work) and the incoming upstream changes.

## Working-tree handling

Working tree was clean apart from the pre-existing untracked
`dawntreader-docs/Reports/LOCALIZATION_AUDIT.md`, which the merge did not touch and which
remains untracked.

## Post-merge verification

- `git lfs pull` — clean, no new objects.
- `./gradlew shadowJar` — **BUILD SUCCESSFUL** in 16s (`app:compileJava`, `app:classes`,
  `app:shadowJar` all ran/succeeded; only a non-blocking Gradle 9 deprecation warning).
- No leftover conflict markers anywhere in the tree (`git grep` for
  `<<<<<<<`/`=======`/`>>>>>>>` returned nothing).

## Push

- Merge commit: `c2731133` ("Merge upstream/V1.1 into V1.1-KAN-6-push-to-talk-dawntreader").
- **Not pushed** — `V1.1-KAN-6-push-to-talk-dawntreader` is now 22 commits ahead of
  `origin/V1.1-KAN-6-push-to-talk-dawntreader`.
- `upstream` was only fetched, not pushed to.

## Outstanding / follow-ups

- None identified — merge was clean and the build passes. Push to `origin` when ready.

---

# Sync — 2026-06-16

## Summary

Two sequential syncs performed. First sync: 2 new commits (`d975a89e`→`26d8ea2d`),
fast-forwarded V1.1 and merged into working branch. Second sync (same session): 25 new commits
(`26d8ea2d`→`432a3172`), fast-forwarded V1.1 and merged into working branch. Both merges clean,
no conflicts. **Not pushed.**

## Sync A — minor upstream catch-up

**New on upstream/V1.1 (2 commits):**
- `26d8ea2d` — Removing docked announcement
- `d975a89e` — RU adjustments

Fast-forwarded local V1.1: `857e8cb1` → `26d8ea2d`. Merged into working branch (merge commit
`e9c...`, zero conflicts, 5 files changed). Working branch then 2 commits ahead of updated V1.1.

## Sync B — active development batch (25 commits)

**V1.1 advanced:** `26d8ea2d` → `432a3172` (78 files changed, +2853/-3596).

**New branches observed on origin/upstream:**
`V1.1-KAN-31-allow-to-set-generic-reminder`, `V1.1-KAN-60-remove-cadence`,
`V1.1-audio-filter-attempt`, `V1.1-i18n-pt`, `V1.1-move-al-aliases-to-propertiess`.

**All 25 commits pulled:**

| Commit | Author | Summary |
|---|---|---|
| `432a3172` | Stone | version to -beta-test |
| `fd6785ff` | Stone | Introducing SpectralNoiseReducer and fixing PTT behavior |
| `9282d907` | Stone | Custom Reminder with no reminder details (KAN-31) |
| `dc0e593e` | Stone | Removing Cadence — no longer needed for non-English languages |
| `9e7566eb` | Stone | Small fix to TTS/Kokoro so it does not say "dot" |
| `8f1eb41c` | Gnevko | Fix bindings apply safety and i18n choice parsing |
| `6b10588d` | Gnevko | Fixing mismatched localization strings |
| `00b29f97` | Gnevko | GoogleTTSImpl logging |
| `da4c3792` | — | Merge remote-tracking branch origin/V1.1 into V1.1 |
| `1df9aec2` | — | reducer fix |
| `b4225482` | Gnevko | Fix bindings draft apply overwriting game changes |
| `bb036717` | — | minor changes |
| `cb16c47a` | — | Add HUD_COLOR_ROLE_READOUT_LABEL role for telemetry block labels |
| `a8b4b73e` | Gnevko | Extract Bind Forge tab; revert Actions tab rename |
| `2c8813cf` | — | Rename Actions tab to Action Center (class, field, i18n key) |
| `efe81dcf` | — | Rename Player tab to Commander (class, field, i18n key) |
| `a97b49fb` | — | Add focus highlight to HudTextField |
| `ebddc9ee` | — | Merge remote-tracking branch origin/V1.1 into V1.1 |
| `b3b3025d` | — | restart necessary services on language change |
| `9ebfdb6f` | — | Rework HUD search filter bar |
| `315c4695` | — | refactoring brain package — move action keys to .properties files |
| `bd8fee75` | — | refactoring brain package — move action keys to .properties files |
| `32bb87d5` | — | refactoring brain package — move action keys to .properties files |
| `fd1e4da8` | — | Russian prompt adjustments |
| `aa9bf010` | — | pipe for random pattern for localization choices |

Fast-forwarded local V1.1: `26d8ea2d` → `432a3172`. Merged into working branch (zero
conflicts, 78 files changed).

## Commit detail review (KAN-6 overlap analysis)

Four commits inspected in full before merging, due to overlap with active work:

### `fd6785ff` — SpectralNoiseReducer + PTT behavior fix

**New code:**
- `SpectralNoiseReducer.java` — new singleton, FFT-based spectral subtraction noise reducer
  (16 kHz PCM-16 mono, Hanning window, overlap-add). During VAD-inactive periods accumulates
  noise profile; during transcription applies denoising before Parakeet inference. Strength
  levels: Low (α=1.5, β=0.10) / Medium (α=2.0, β=0.05) / High (α=3.0, β=0.02).
- `ParakeetSTTImpl.java` — reset reducer on `captureLoop()` start; feed silence to
  `accumulateNoise()` during VAD-inactive; call `denoise()` before transcription, both gated
  on `systemSession.isNoiseReductionEnabled()`.
- `AudioSettingsPanel.java` — layout overhauled to 4-column row (Mic combo | Speaker combo)
  + noise reduction row (enable checkbox + `HudSegmentedControl` Low/Medium/High). Persists
  immediately via `SystemSession`.

**PTT fix in `InputSettingsPanel` — direct KAN-6 overlap:**

The enable and disable paths were both wrong:
- Enable path (both at initial load and on toggle): now unconditionally calls
  `SystemSession.stopStartListening(true)` + publishes `SleepWakeStateChangedEvent(true)`.
  Previously this was only done conditionally or not at all depending on toggle/hold mode.
- Disable path: now unconditionally calls `stopStartListening(false)` + publishes
  `SleepWakeStateChangedEvent(false)`. Previously only executed in hold mode — toggle-mode
  PTT disable left the app in the wrong listening state.

`suppressPersistence` flag: **not touched** by this commit.

**New DB columns (via `01007__schema.sql`):**
```sql
alter table game_session add column noiseReductionEnabled boolean default false;
alter table game_session add column noiseReductionStrength integer default 1;
```
`GameSessionDao.GameSession` and `SystemSession` extended with getters/setters for both.
These are noise-reduction columns only — no new PTT columns added.

### `a8b4b73e` — Extract Bind Forge tab

- `BindingsTabPanel.java` **renamed to `BindForgeTabPanel.java`** — class/constructor name
  change only, 99% code similarity.
- `ActionsTabPanel.java` (previously `ActionCenterTabPanel`) — BindForge sub-tab removed;
  now contains only Command Catalog and Custom Commands.
- `AppView.java` — `BindForgeTabPanel` promoted to a **top-level tab** alongside Actions,
  with dedicated anvil icon (`images/anvil.png`) and i18n key `tab.bindForge`. Tab bar order:
  AI | Commander | Actions | **Bind Forge** | Settings | Stats | Manual | Credits.

No new binding capabilities added — purely a structural promotion. The class is functionally
identical to the old `BindingsTabPanel` (keyboard-only editor, Used/Missing tables, apply/revert).

**Impact on this branch:** Any in-branch code referencing `BindingsTabPanel` by name would
need updating to `BindForgeTabPanel` post-merge. No such references existed on this branch.

### `8f1eb41c` — "Fix bindings apply safety and i18n choice parsing"

Despite the name, **no bindings apply classes were touched in this commit.** The only change
is in `MultiLingualTextProvider.pickVariant()`: naive `raw.split("\\|")` replaced with
`splitTopLevelVariants()` which tracks brace depth to avoid splitting on `|` inside
`MessageFormat` choice patterns (e.g. `{0, choice, 1#one|2#many}`). Test class
`MultiLingualTextProviderTest` added. Zero overlap with bindings parser or apply pipeline.

### `b4225482` — Fix bindings draft apply overwriting game changes

The substantial bindings safety fix. Introduced SHA-256 baseline hashing throughout the
working-copy system:

**`BindingsWorkingCopyRepository`:**
- Sidecar file `<preset>.binds.elite-intel.base.sha256` stores the hash of the game file
  at import time.
- `hasUnappliedDraft(preset, gameFile)` — returns true only when EI's working copy differs
  from baseline. If working copy is clean and game file changed, silently refreshes the
  working copy (game-only change, no EI draft to protect).
- `gameFileMatchesBaseline(preset, gameFile)` — detects concurrent game-client edits.
- `markApplied(preset)` — updates baseline hash after a successful apply.
- `migrateLegacyWorkingCopy()` — upgrades existing working copies that predate this system.
- All file writes use atomic temp-file-then-rename (`StandardCopyOption.ATOMIC_MOVE`).

**`BindingsApplyService`:**
- Before applying: checks `hasUnappliedDraft()` — returns `null` (no-op) if no EI draft
  exists (e.g. only the game changed the file).
- Checks `gameFileMatchesBaseline()` — throws localized `BindingsApplyException` with key
  `bindings.apply.conflict` if the game file changed since the baseline was recorded.
- Calls `workingCopyRepo.markApplied()` after successful write.

**`BindingsApplyException`:** new `localizationKey` field + `localized()` factory for
user-facing i18n error dialogs.

**`BindForgeTabPanel`:** switched sync status check from `isSyncedWithGame()` to
`hasUnappliedDraft()`; fixed inverted status badge logic.

**New tests:** `BindingsApplyServiceTest` and `BindingsWorkingCopyRepositoryTest` covering:
clean draft applied, game-only change (no-op), dirty EI draft + concurrent game change
(conflict error).

**Relationship to `EXISTING_BINDFORGE_AUDIT.md`:** The audit identified the working-copy +
apply system as solid. This commit hardens it substantially. Parser gaps (AXIS/STANDALONE
SETTING/ToggleOn) are not addressed — remain as documented.

## Conflicts and resolution

None. Both merges completed via the `ort` strategy with zero conflicts.

## New DB migration

`01007__schema.sql` — adds `noiseReductionEnabled BOOLEAN DEFAULT FALSE` and
`noiseReductionStrength INTEGER DEFAULT 1` to `game_session`. No BindForge or PTT schema
changes.

## Push

**Not pushed.** Working branch is ahead of `origin/V1.1-KAN-6-push-to-talk-dawntreader`
by 58 commits.
