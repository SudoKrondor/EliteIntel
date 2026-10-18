# Preset Editor — which `.binds` the game loads for each section

**One file, four values.** The screen manages `StartPreset.#.start`: four lines — General, Ship, SRV, On Foot —
each naming the preset that section loads. It never moves, hides or deletes a `.binds` file. Same draft-and-Apply
model as every other section: Save writes the draft, Discard drops unsaved dropdown changes, Apply is what reaches
the live file.

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
- [SPECIMENS-README.md — `StartPreset` specimens](../02-features/bindforge/reference-data/SPECIMENS-README.md#startpreset-specimens--two-generations-and-a-split-preset-in-the-wild).
- [input-environment.md §7](../00-overview/input-environment.md#7-linux-the-bindings-folder-is-not-shared) — one
  bindings folder for now, on Linux too (2026-10-14).
- [The HUD canon](../02-features/bindforge/ui-component-map.md) — for the screen slice.

## Built

| What | Where |
|---|---|
| The live `StartPreset` | `bindforge.io.BindingsLoader.findLiveStartPresetFile(Path)` — highest `StartPreset.N.start`, else `StartPreset.start` (F1) |
| The four sections | `BindingsLoader.readStartPresetSections(Path)` → `bindforge.io.StartPresetSections` (`general`, `ship`, `srv`, `onFoot`, `singleLine`; `parse(String)`, `forSection(BindingSection)`). Empty `Optional` with no file, `IOException` on a failed read. Rules in [preset-editor.md](../02-features/bindforge/preset-editor.md#how-the-four-lines-are-read--settled-2026-10-12-alan-foundation-f1). Tests `StartPresetSectionsTest`, `BindingsLoaderStartPresetTest`; specimens in `app/src/test/resources/bindforge/startpreset/` |
| Every `.binds` in the folder, by extension | `BindingsLoader.listAllBindsFiles(Path)` — **by name only**; the spec wants contents |
| The bindings folder | `PlayerSession.getBindingsDir()` — the one folder, on Linux too ([§7](../00-overview/input-environment.md#what-bindforge-does-with-several-linux-installations---2026-10-14-alan)) |
| Safe replace, timestamped copies | `elite.intel.io.AtomicFiles`, `TimestampedBackups` — **never write a game file any other way** |
| `.binds` Edit History folder | `AppPaths.getBindingsBackupDir()` (C4 split); device-file history is `getBindingsWorkingDir()/history/<installId>/` |
| Retention | `BindForgeSettingsManager.getEditHistoryRetention()` (1–30, default 10) |
| The tab shell | `ui.screen.BindingsTabPanel` — Binding Profile, Binding Management, Alias Designer sub-tabs |
| Backups | `PlayerBackupService` and the never-pruned "before BindForge" backup (`devicefiles.BeforeBindForgeBackup`) already include `StartPreset` |

**Nothing writes `StartPreset` today**, so there is no existing writer to grow: the master, the draft and the
write are new V1.2 classes ([the split](../02-features/bindforge/overview.md#where-the-binds-and-startpreset-master-lives--settled-2026-10-12-alan-foundation-f3)).

## Settled — and where it is recorded

- **Draft and Apply, like everything else; no write-as-you-go.** [preset-editor.md](../02-features/bindforge/preset-editor.md#draft-model--same-as-every-other-section)
- **The four lines are four independent entities** — a game change to one line and a draft change to another
  merge with nothing to ask. [Same section](../02-features/bindforge/preset-editor.md#the-four-lines-are-four-independent-entities)
- **A line holds the preset name**: the filename less its version suffix and extension, the suffix stripped only
  when there is one (`Custom.4.2 (2).binds` → `Custom.4.2 (2)`). [What a line holds](../02-features/bindforge/preset-editor.md#what-a-line-actually-holds--the-preset-name-not-the-file-name)
- **A name identifies exactly one file** in a live folder. [A name does not identify a file](../02-features/bindforge/preset-editor.md#a-name-does-not-identify-a-file)
- **A file is offered on its contents, not its extension.** [Layout](../02-features/bindforge/preset-editor.md#layout), and file-manager.md above.
- **The state is coloured text, never a pill.** [Sync Badge](../02-features/bindforge/preset-editor.md#sync-badge)
- **The master and the write are new V1.2 classes**; the master lives under `elite-intel/bindings/master/`.
  [overview.md](../02-features/bindforge/overview.md#where-the-binds-and-startpreset-master-lives--settled-2026-10-12-alan-foundation-f3)
- **The screen brings the unsaved-work prompt with it** — its dropdowns hold edits outside a modal dialog.
  [C6 ruling](../02-features/bindforge/overview.md#built-as-a-ruling-not-code--2026-10-12-alan-core-c6)
- **Preset Rename and Duplicate are deferred.** [Deferred](../02-features/bindforge/preset-editor.md#deferred)

## Known doc conflicts — settle with Alan when a slice reaches them

1. **Factory presets are not in the bindings folder.** The dropdowns list *"the `.binds` files in the bindings
   folder"*, but a section can sit on a shipped preset — the specimen's General line is `KeyboardMouseOnly`,
   which lives in an installation's `ControlSchemes` — and the spec says *"switching a section back to a factory
   preset is an ordinary edit"*. As written, the dropdown could neither show that value nor offer it. **P1.**
2. **What "wiped" means for `StartPreset`.** preset-editor.md's example is *"all four lines reset to a single
   default preset name"*; overview.md's 2026-10-05 rewrite defines wiped as *matches Frontier's stock file, or is
   gone*, and no stock `StartPreset` reference exists. **P5.**
3. **The badge compares against "the live file".** The master model compares the game's file against the
   **master**, and the draft against the master. IN SYNC / DRAFT need restating in those terms. **P4.**
4. **Writing the single-line form** is left to this screen by F1, and so is the line ending (the specimen is
   CRLF with no final newline). **P3.**

## Known issues

- **`BindingsMonitor` ignores `StartPreset`.** It reacts only to names ending `.binds`
  (`BindingsMonitor.java`, the `endsWith(".binds")` test), so a change to `StartPreset` — in the game, or by
  this screen's Apply — does not make the assistant reload the newly active General preset until some `.binds`
  changes. V1.1 code: a fix goes on the foundation branch as an addition, or P5 watches the file itself. Settle in
  P3's sources table, since Apply is the first write that trips it.
- **Two `StartPreset` generations in one folder**: `findStartPresetFile` (the assistant, player backups) and
  `findLiveStartPresetFile` (this screen) can name different files. Krondor's, as maintenance — see
  [foundation.md known issues](foundation.md#known-issues). This screen must use the live finder only.
- **A section repointed at a name no file answers to fails silently** — the game loads something else and the
  user finds out in the cockpit ([the spec](../02-features/bindforge/preset-editor.md#what-a-line-actually-holds--the-preset-name-not-the-file-name)).
  Whatever the dropdown offers, Apply must never write a name that resolves to no file.

## Boundaries

- **New code** goes in a new package, `elite.intel.bindforge.presets` (P1 confirms it in the sources table), and
  the screen in `ui.screen.bindings`.
- **`bindforge.io`, `bindforge.rules`, `elite.intel.io`, `AppPaths`** are shared with V1.1 — a change to an
  existing file goes on [the foundation branch](README.md#the-foundation-branch--how-a-v12-feature-changes-a-file-v11-ships-krondor-2026-10-12);
  a new class only V1.2 calls does not.
- **Not this section:** the `.binds` whole-file master and its startup check (Core C4); First-Time Startup
  (Core C7, which repoints through this section's write); Edit History browsing (File Manager); Rename and
  Duplicate (deferred).
- **`ai.hands`** — never.

## Slices

| # | Slice | Needs | Status |
|---|---|---|---|
| P1 | **The preset list** — the names the four dropdowns offer: every file in the bindings folder that *is* a `.binds` by its contents, each reduced to its preset name (suffix stripped only when present), plus a sentence for each file that is not one. Settles doc conflict 1 (factory presets). Read-only, no UI | F1 (done) | **next** |
| P2 | **`StartPreset` master and draft** — where each lives (master under `elite-intel/bindings/master/`), what Save writes, Discard, "has the draft changed", and the per-line three-way compare the screen and P5 both need | P1 | pending |
| P3 | **Apply** — the draft to the live file through `AtomicFiles`, the replaced file kept in Edit History and pruned, the draft promoted to master; refuses a name no file answers to; the single-line form and line ending (doc conflict 4); the `BindingsMonitor` question above | P2 | pending |
| P4 | **The screen** — a fourth sub-tab: four dropdowns, Set All to Same, the consistency warning (live, never blocking), the sync state as coloured text (doc conflict 3), SAVE / DISCARD / APPLY; strings in all seven bundles | P3 | pending |
| P5 | **Change detection** — the game's `StartPreset` against the master: unchanged, edited (adopt / revert, merged per line with the draft), wiped (doc conflict 2); refreshed when the file changes | P4 | pending |
| P6 | **Unsaved work on exit** — Save / Discard, never Apply, on the tab switch and `AppView`'s close, for this screen's unsaved dropdowns only | P4 | pending |

**Core C7 (First-Time Startup) waits on P3**: it repoints a section through this section's write.

### P1 — what the session needs to know

- **Content, not extension.** A `<Root>` element carrying binding children is a binds file; anything else gets a
  sentence saying what it looks like (*"a Start Preset list, not a bindings file"*), and is never offered. Check
  for an existing content test before writing one — `BindingsApplyService.validateXml` and `KeyBindingsParser`
  both read `.binds`, and the latter must **not** be widened ([why](../02-features/bindforge/overview.md#two-narrow-boundaries--one-stays-one-widens)).
- **Test against the specimens** in `reference-data/`: `Custom.4.2.binds`, `DualVirpilDawnTreader.4.1.binds`,
  `Custom.4.2 (2).binds`, the `.binds.backup` (not a `.binds` name at all), and `StartPreset.start` copied under a
  `.binds` name. No shipped preset is a specimen, so `KeyboardMouseOnly.binds` (no suffix) is a small file the test
  builds. `Custom.3.0.binds` beside `Custom.4.2.binds` is the two-files-one-name case below.
- **Factory presets (doc conflict 1)** — the options: list the shipped presets from each installation's
  `ControlSchemes` beside the user's files; show a section's current factory value without offering factory
  presets; or leave factory presets to First-Time Startup. Bring them to the sources table; do not pick one in code.
- **Two files reducing to one name** cannot happen in a live folder (testing item 15, withdrawn) — but say what
  the list does if it meets one, rather than letting a map silently keep the last.

## Hand-off notes

*Newest first. Two or three lines each: what was decided and where it is recorded, what was found, what the
next slice needs.*

- **2026-10-17 — brief written.** Nothing of the screen exists; F1's four-line read is merged up. **Found:**
  `BindingsMonitor` reacts only to `.binds` names, so a `StartPreset` change reloads nothing for the assistant
  (known issues); and the spec's dropdown source omits factory presets, which a section can be on (doc
  conflict 1). **P1 needs:** the spec read end to end, and Alan's answer on factory presets in its sources table.
