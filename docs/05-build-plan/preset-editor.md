# Preset Editor — which binds file the game loads for each section

**One file, four values.** The screen manages the preset file, `StartPreset.4.start`: four lines — General, Ship, SRV, On Foot —
each holding the BindName of the binds file that section loads. It never moves, hides or deletes a `.binds` file. Like every
other section, its changes are edits not applied, held on screen, and the one APPLY above BindForge's tabs writes them.

> **Rewritten 2026-10-24 (Alan): back up, edit, apply** ([storage.md](../02-features/bindforge/storage.md)). P2's master
> and draft and P3's per-line merge are replaced by the working copy in **[Storage S6](storage.md#slices)**, which also
> moves APPLY's checks — every line names a listed binds file, four lines, CRLF — onto the one APPLY. **S6 is done
> (2026-10-28), so P4, the screen, is next.** P5 and P6 are dropped: the check (S8) and the leave prompt (S7) replace
> them.

**The spec's own build order** ([what exists in code](../02-features/bindforge/preset-editor.md#what-exists-in-code-today--checked-2026-09-17)):
read all four lines, then the draft model, then the write path. The first is done (Foundation F1); the slices
below follow the rest of that order.

## Spec

- [preset-editor.md](../02-features/bindforge/preset-editor.md), **end to end** — it is 150 lines.
- [overview.md](../02-features/bindforge/overview.md): **Managed File Domains** (the four-line paragraph at its end),
  **The master and the draft**, **What the master covers**, **What the startup check actually decides**,
  **Freshness Checks**, **Destructive Change Detection**, **Where the `.binds` and `StartPreset` master lives**
  (2026-10-12), **External change detection**, **Unsaved Work on Exit** including the C6 ruling.
- [file-manager.md](../02-features/bindforge/file-manager.md): **What a `.binds` File Might Not Be**, **Edit History**.
- [bind-editor.md — a `.binds` name does not make it a binds file](../02-features/bindforge/bind-editor.md#a-binds-name-does-not-make-it-a-binds-file--2026-09-17).
- [SPECIMENS-README.md — preset file specimens](../02-features/bindforge/reference-data/SPECIMENS-README.md#preset-file-specimens--two-generations-and-a-split-preset-file-in-the-wild).
- [input-environment.md §7](../00-overview/input-environment.md#7-linux-the-bindings-folder-is-not-shared) — one
  bindings folder for now, on Linux too (2026-10-14).
- [The HUD canon](../02-features/bindforge/ui-component-map.md) — for the screen slice.

## Built

| What | Where |
|---|---|
| **The preset file as an edit** (Storage S6) | `bindforge.storage.PresetEdits` — `presetFile(files)` (the set's highest `StartPreset.N.start`), `sections(files)`, `change(current, applied, sections)` (an `Edits.EditFromApplied`: the game's four-line form, or the working copy's bytes when the lines are put back), `gameForm`, `check(files, bindNames)` → `PresetRefusedException(sections)`, `bindsFiles(files)`, `SECTIONS`. `Apply` runs the check first, on every APPLY; `ApplyBar` names the sections (`bindings.applyBar.presetRefused`). Tests `PresetEditsTest`, `ApplyTest` |
| The four sections | `bindforge.io.StartPresetSections` (`general`, `ship`, `srv`, `onFoot`, `singleLine`; `parse(String)`, `forSection(BindingSection)`). Rules in [preset-editor.md](../02-features/bindforge/preset-editor.md#how-the-four-lines-are-read--settled-2026-10-12-alan-foundation-f1). Test `StartPresetSectionsTest`; specimens in `app/src/test/resources/bindforge/startpreset/` |
| **The BindName list** (P1) | `bindforge.preseteditor.BindNameCatalog.list(userFiles, controlSchemesFolders)` — the player's binds files as bytes from the edits (`PresetEdits.bindsFiles`), each listed as a `Path` of its name alone, since Storage S6 — → `BindNameList` (`entries`, `notOffered`, `find(bindName)`). Entries `BindNameEntry(bindName, origin, files)`, origin a `BindsFileOrigin` USER/FACTORY; rejects `NotOfferedFile(file, origin, NotOfferedReason, detail)`. `BindNameCatalog.controlSchemesOf(rows)` turns `InstallationRegistry.current()` into folders. Content check `BindsContent` (package-private), BindName check `BindName.fromFileName`. Tests `BindNameTest`, `BindsContentTest`, `BindNameCatalogTest` read the specimens in `reference-data/` where they are |
| The bindings folder | `PlayerSession.getBindingsDir()` — the one folder, on Linux too ([§7](../00-overview/input-environment.md#what-bindforge-does-with-several-linux-installations---2026-10-14-alan)) |
| Writing | the one APPLY, through `bindforge.storage.WritePath` and `elite.intel.io.AtomicFiles` — **never write a game file any other way** |
| The tab shell | `ui.screen.BindingsTabPanel` — Binding Profile, Binding Management, Alias Designer sub-tabs |
| Backups | `bindforge.storage.Backups` - every backup includes `StartPreset.4.start` |

*Removed in Storage S6 (2026-10-28):* P2's `StartPresetStore` and `StartPresetComparison`, P3's `StartPresetApply`,
`StartPresetLines`, `BindingsLoader.findLiveStartPresetFile` and `readStartPresetSections`, `TimestampedBackups`,
`AppPaths.getBindingsBackupDir` and the Edit History retention setting.

## Settled — and where it is recorded

- **Draft and Apply, like everything else; no write-as-you-go.** [preset-editor.md](../02-features/bindforge/preset-editor.md#edits-and-apply--same-as-every-other-section)
- **The four lines are four independent entities** — a game change to one line and a draft change to another
  merge with nothing to ask. [Same section](../02-features/bindforge/preset-editor.md#the-four-lines-are-four-independent-entities)
- **A line holds the file's `PresetName`** — its BindName — which must match the filename less its version suffix and extension,
  the suffix stripped only when there is one; a file where they disagree (`Custom.4.2 (2).binds`) is not offered
  (Alan, 2026-10-17). [What a line holds](../02-features/bindforge/preset-editor.md#what-a-line-actually-holds--the-bindname-not-the-file-name)
- **The list includes the factory binds files**, one entry per BindName across installations; a player's binds file
  shadows a factory one of the same BindName. [The BindName list](../02-features/bindforge/preset-editor.md#the-bindname-list--settled-2026-10-17-alan-p1)
- **A BindName identifies exactly one file** in a live folder. [A name does not identify a file](../02-features/bindforge/preset-editor.md#a-name-does-not-identify-a-file)
- **A file is offered on its contents, not its extension.** [Layout](../02-features/bindforge/preset-editor.md#layout), and file-manager.md above.
- **The state is coloured text, never a pill.** [Sync Badge](../02-features/bindforge/preset-editor.md#is-there-anything-to-apply)
- **The master and the write are new V1.2 classes**; the master lives under `elite-intel/bindings/master/`.
  [overview.md](../02-features/bindforge/overview.md#where-the-working-copy-lives--2026-10-23)
- **The screen brings the unsaved-work prompt with it** — its dropdowns hold edits outside a modal dialog.
  [C6 ruling](../02-features/bindforge/overview.md#unsaved-work-on-exit)
- **Master made from the game's file on first read; a draft exists only when it holds a change; values compared,
  never bytes; the screen's Discard is the dropdowns', the store only drops the draft** (Alan, 2026-10-18, P2).
  [The master and the draft](../02-features/bindforge/preset-editor.md#what-apply-checks-and-writes-for-the-preset-file--2026-10-24)
- **Binds File Rename and Duplicate are deferred.** [Deferred](../02-features/bindforge/preset-editor.md#deferred)

## Known doc conflicts — settle with Alan when a slice reaches them

1. ~~**Factory presets are not in the bindings folder.**~~ **Settled 2026-10-17 (Alan, P1):** the list reads
   each installation's `ControlSchemes` too. [The BindName list](../02-features/bindforge/preset-editor.md#the-bindname-list--settled-2026-10-17-alan-p1)
2. **What "wiped" means for `StartPreset.4.start`.** storage.md rule 14 names a reset when a live copy matches
   Frontier's factory file, and no stock `StartPreset.4.start` reference exists. **Storage S8** settles it.
3. ~~**The badge compares against "the live file".**~~ **Settled 2026-10-20 (Alan):** DRAFT means a saved draft
   differs from the master; a game change is a separate signal (P5). [Sync Badge](../02-features/bindforge/preset-editor.md#is-there-anything-to-apply)
4. ~~**Writing the single-line form.**~~ **Settled 2026-10-20 (Alan, P3):** always four lines, CRLF between them, no
   final newline. [Apply](../02-features/bindforge/preset-editor.md#what-apply-checks-and-writes-for-the-preset-file--2026-10-24)

## Known issues

- **`BindingsMonitor` ignores `StartPreset.4.start`.** It reacts only to names ending `.binds`
  (`BindingsMonitor.java`, the `endsWith(".binds")` test), so a change to the preset file — in the game, or by
  this screen's Apply — does not make the assistant reload the newly active General binds file until some `.binds`
  changes. **Settled in P3 (Alan, 2026-10-20):** a V1.1 defect, fixed on `V1.1-Release`, not by Apply
  ([Apply](../02-features/bindforge/preset-editor.md#what-apply-checks-and-writes-for-the-preset-file--2026-10-24)). Still unfixed.
- **Two preset file generations in one folder** (`StartPreset.4.start` and `StartPreset.start`): `findStartPresetFile` (the assistant)
  can name the old one. See [foundation.md known issues](foundation.md#known-issues). This screen reads the working
  copy through `PresetEdits.presetFile`, which takes the highest `StartPreset.N.start` only.
- **`BindingsWorkingCopyRepository.isSyncedWithGame` has no caller** (IntelliJ, 2026-10-20). A `.binds` working-copy
  check, so not this section's; found while P3's review fixes moved that class onto `AtomicFiles`. Use it or remove
  it when the `.binds` sync state is next worked on (Core C4).
- **A section repointed at a BindName no file answers to fails silently** — the game loads something else and the
  user finds out in the cockpit ([the spec](../02-features/bindforge/preset-editor.md#what-a-line-actually-holds--the-bindname-not-the-file-name)).
  Whatever the dropdown offers, Apply must never write a BindName that resolves to no file.

## Boundaries

- **New code** goes in the package `elite.intel.bindforge.preseteditor` (named `presets` until 2026-10-19), and
  the screen in `ui.screen.bindings`.
- **`bindforge.io`, `bindforge.rules`, `elite.intel.io`, `AppPaths`** are shared with V1.1 but changed on
  `V1.2-BindForge` like any V1.2 code — the foundation branch was repealed 2026-10-20 (Krondor).
- **Not this section:** the `.binds` whole-file master and its startup check (Core C4); First-Time Startup
  (Core C7, which repoints through this section's write); Edit History browsing (File Manager); Rename and
  Duplicate (deferred).
- **`ai.hands`** — never.

## Slices

| # | Slice | Needs | Status |
|---|---|---|---|
| P1 | **The BindName list** — the BindNames the four dropdowns offer: every file in the bindings folder that *is* a binds file by its contents, each reduced to its BindName (suffix stripped only when present), plus a sentence for each file that is not one. Settles doc conflict 1 (factory binds files). Read-only, no UI | F1 (done) | **done 2026-10-17** |
| P2 | ~~**`StartPreset.4.start` master and draft**~~ *replaced by Storage S6* — where each lives (master under `elite-intel/bindings/master/`), what Save writes, Discard, "has the draft changed", and the per-line three-way compare the screen and P5 both need | P1 | **done 2026-10-18** |
| P3 | ~~**Apply**~~ *replaced by Storage S6, which keeps its checks* — the draft to the live file through `AtomicFiles`, the replaced file kept in Edit History and pruned, the draft promoted to master; refuses a BindName no file answers to; the single-line form and line ending (doc conflict 4); the `BindingsMonitor` question above | P2 | **done 2026-10-20** |
| P4 | **The screen** — a sub-tab: four dropdowns from the BindName list, Set All to Same, the consistency warning (live, never blocking); every change an edit not applied on the one APPLY / DISCARD bar, with no buttons of its own and no badge; each file the list does not offer turned into its sentence; strings in all seven bundles | Storage S6 | **next** |
| P5 | ~~**Change detection**~~ — dropped 2026-10-24: the check, [Storage S8](storage.md#slices) | — | dropped |
| P6 | ~~**Unsaved work on exit**~~ — dropped 2026-10-24: the leave prompt, [Storage S7](storage.md#slices) | — | dropped |

*Core C7 (First-Time Startup) is gone with Core; CREATE ([Storage S11](storage.md#slices)) points the four lines at a new binds file as edits not applied.*

### P1 — what the session needed to know (done 2026-10-17)

- **Content, not extension.** A `<Root>` element carrying binding children is a binds file; anything else gets a
  sentence saying what it looks like (*"a preset file, not a binds file"*), and is never offered. Check
  for an existing content test before writing one — `BindingsApplyService.validateXml` and `KeyBindingsParser`
  both read `.binds`, and the latter must **not** be widened ([why](../02-features/bindforge/overview.md#two-narrow-boundaries--one-stays-one-widens)).
- **Test against the specimens** in `reference-data/`: `Custom.4.2.binds`, `DualVirpilDawnTreader.4.1.binds`,
  `Custom.4.2 (2).binds`, the `.binds.backup` (not a `.binds` name at all), and `StartPreset.start` copied under a
  `.binds` name. No factory binds file is a specimen, so `KeyboardMouseOnly.binds` (no suffix) is a small file the test
  builds. `Custom.3.0.binds` beside `Custom.4.2.binds` is the two-files-one-name case below.
- **Factory binds files (doc conflict 1)** and **two files reducing to one BindName** — settled; see the hand-off note.

## Hand-off notes

*Newest first. Two or three lines each: what was decided and where it is recorded, what was found, what the
next slice needs.*

- **2026-10-28 — Storage S6 done: the preset file on the bar.** The preset file is an edit, and APPLY refuses first a
  line that is empty or names no binds file the BindName list offers - on every APPLY (preset-editor.md *What APPLY
  checks and writes*). P2's and P3's classes are removed. **P4 needs:** the dropdowns from `PresetEdits.sections`
  over `Edits.snapshot().current()`, each change `Edits.change(EditFromApplied)` with `PresetEdits.change`, and the
  list from `BindNameCatalog.list(PresetEdits.bindsFiles(current), BindNameCatalog.controlSchemesOf(...))`, read off
  the EDT. Until P4 nothing in BindForge can fix a refused preset file (storage brief *Known issues*).

- **2026-10-24 — build plan rewritten to back up, edit, apply** (Alan). P2–P3's store and merge are replaced in
  [Storage S6](storage.md#slices), which keeps P3's checks and folds in P3 review findings 2 and 5 (finding 7 went
  with the spec rewrite). P4 follows S6; P5 and P6 dropped. The **Built** table below is the state before S6.

- **2026-10-20 — P3 done** (`StartPresetApply`), **with Krondor's second P2 review and his repeal of the
  foundation branch.** **Decided** (in [Apply](../02-features/bindforge/preset-editor.md#what-apply-checks-and-writes-for-the-preset-file--2026-10-24)
  and [Sync Badge](../02-features/bindforge/preset-editor.md#is-there-anything-to-apply)): merged per line, a conflict writes
  nothing; every line must name a listed binds file, an empty one refused; the game's form is four lines, CRLF, no
  final newline, written to the file `findLiveStartPresetFile` names; values already in the game's file are not
  written; DRAFT means differs from the master. **Review fixes:** `StartPresetSections.equals` compares the four
  values only, so `StartPresetLines.same` is gone; `StartPresetSections.withoutByteOrderMark` is the one BOM strip;
  `Line.gameMoved()` backs `gameChanged()`; `BindingsWorkingCopyRepository` copies through `AtomicFiles`; a test for
  the master moving under a draft; "a preset file" in the 363-files wording; the glossary says the number follows
  the game's version. **P4 needs:** the badge from `hasDraftChanges()`, the dropdowns from `BindNameCatalog`, each
  `Outcome` turned into a sentence and its strings.

- **2026-10-20 — code-integrity review fixes** (P2 and the sweep). `StartPresetComparison.Line.change()` is now
  derived from the three values and the comparison refuses anything but the four sections in order; Save checks
  its text by reading it back; `hasDraftChanges()` keeps comparing against the master, and **a draft with no
  master counts as changed** (Alan; in [the master and the draft](../02-features/bindforge/preset-editor.md#what-apply-checks-and-writes-for-the-preset-file--2026-10-24)).
  `preseteditor` added to `bindforge/PACKAGE.md`. ~~**Still open:** the `StartPresetSections` Javadoc says
  "preset name".~~ Fixed 2026-10-20.

- **2026-10-19 — terminology sweep** (Alan settled *binds file*, *preset file* and *BindName* in
  [input-environment.md](../00-overview/input-environment.md#binds-file-preset-file-bindname--settled-2026-10-19-alan)).
  The notes below keep the names they were written with; the map: package `bindforge.presets` →
  `bindforge.preseteditor`; `PresetCatalog` → `BindNameCatalog`; `PresetList` (`presets`) → `BindNameList`
  (`entries`); `PresetEntry` (`name`) → `BindNameEntry` (`bindName`); `PresetName` → `BindName`; `PresetOrigin` →
  `BindsFileOrigin` (`USER` / `FACTORY` unchanged); `NotOfferedReason.START_PRESET_LIST` → `PRESET_FILE`. The
  `StartPreset*` classes keep their names. The master and draft are now named `StartPreset.4.start`, not
  `StartPreset.start`, which is the pre-Odyssey file. Shared V1.1 code is unchanged.

- **2026-10-18 — P2 done** (`StartPresetStore`, `StartPresetComparison`). **Decided** (in [the master and the draft](../02-features/bindforge/preset-editor.md#what-apply-checks-and-writes-for-the-preset-file--2026-10-24)):
  the master is made from the game's file on first read; Save of the master's values leaves no draft, so a draft
  that exists holds a change; lines compare by value, single-line equal to four matching lines. **Found:** Core C4
  also names the `StartPreset` master — P2 built it, so C4 is now `.binds` only. **P3 needs:** apply the draft
  (refuse a line `PresetList.find` cannot answer; settle whether an empty line counts as one), keep the replaced file in Edit History and
  prune, then copy the written file over the master and `dropDraft()`; decide the written form and line ending.

- **2026-10-17 — P1 done** (`bindforge.presets`). **Decided** (all in [the preset list](../02-features/bindforge/preset-editor.md#the-bindname-list--settled-2026-10-17-alan-p1)):
  the shipped presets are listed beside the player's own (option A); a line holds the file's `PresetName`, which
  must match the filename rule, and a file where they disagree is not offered; two of the player's files for one
  name cannot happen, but both are kept if it does. **Found:** all 30 shipped presets on Alan's Steam install
  match the rule. **P2 needs:** P3 refuses a name `PresetList.find` cannot answer; the screen (P4) turns each
  `NotOfferedReason` into a sentence and its strings, and reads installations through `PresetCatalog.controlSchemesOf`.

- **2026-10-17 — brief written.** Nothing of the screen exists; F1's four-line read is merged up. **Found:**
  `BindingsMonitor` reacts only to `.binds` names, so a `StartPreset` change reloads nothing for the assistant
  (known issues); and the spec's dropdown source omits factory presets, which a section can be on (doc
  conflict 1). **P1 needs:** the spec read end to end, and Alan's answer on factory presets in its sources table.
