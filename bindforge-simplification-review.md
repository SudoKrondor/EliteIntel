# BindForge simplification review — back up, edit, apply

**Started 2026-10-21 (Alan).** BindForge's specs and code grew a version-control model (master, draft, three-way
merge, adopt/revert, rebasing) that Alan never asked for. What he wants is **back up, edit, apply**. This file
records, chunk by chunk, what was designed, what is built, and what Alan decided. **No code or spec is changed
until the walk-through is finished and the solution is agreed.**

## Chunk 1 — the user installs Elite-Intel and opens the Bind Forge tab — agreed 2026-10-21

**Designed:** on the first opening of the tab, (1) a one-time "before BindForge" backup of every `.binds`,
`StartPreset.4.start` and each installation's device files, never pruned, the user told once; (2) device names
recorded silently when every installation agrees, else First Setup to pick one set; (3) a prompt to create a
custom binds file for any section still on a factory one.

**Built:** (1) and (2) match (`BindingsTabPanel` → `Activation`, `BeforeBindForgeBackup`, `FirstSetup`). (3) is not
built (Core C7).

**Deviations:** at application launch, before the tab is ever opened and before the backup, `AppView.initData()`
→ `BindingProfilePanel.initData()` → `BindingsWorkingCopyRepository.loadOrImportFromGame` copies the **active**
`.binds` into Elite-Intel's folder with a checksum file — V1.1's working-copy model, active file only. No preset
file copy exists yet.

**Decided (Alan):**
- **Keep the before-BindForge backup as it is.** It is the "back up" step.
- **First Setup (reconciling installations) is settled in the editing chunk**, not here.
- **The launch-time copy is to be fixed:** nothing touches the user's files before the backup that is meant to come
  first.

## Chunk 2 — the user edits a binding and presses Apply — agreed 2026-10-21

**Designed:** SAVE writes a draft, Apply is the only write to the game, the replaced file kept and pruned to 10.
On top: a master copy of every binds file, a startup check against it, a three-way slot-by-slot merge, a conflict
dialog, and a version-bump merge (Core C4 — ready, **not built**).

**Built (V1.1's editor):** a copy of the **active** `.binds` in Elite-Intel's folder with a checksum of the game's
file when copied; capture-dialog SAVE writes into the copy (`BindingsWriter`); no edits + game changed → the copy
refreshes silently (`BindingsWorkingCopyRepository.refreshFromGameIfClean`); edits + game changed → Apply refuses,
"reload from game or discard the draft"; Apply validates, copies the game's file to the backup folder, writes
atomically, and records the new baseline (`BindingsApplyService.apply`); REVERT reloads from the game.

**Deviations:** built is simpler than the spec. Only the active binds file is editable (spec: every one). The backup
folder is never pruned. A game update that renames the file to a new version is not handled (chunk 5).

**Decided (Alan):**
- **The built binds-file flow is the design.**
- **The unbuilt master, merge and conflict dialog for binds files are dropped from the spec** — later, when the
  walk-through is finished.
- Open, to settle within that model: every binds file editable or only the active one; how many backups to keep.

## Chunk 3 — the user renames a controller and presses Apply — agreed 2026-10-21

**Designed:** the user's own controller names (never Frontier's) kept in Elite-Intel's database; the device
dialog's SAVE writes a draft; APPLY copies the draft into that set and writes it to **every** installation, the
user's entries at the top, Frontier's left below, the replaced file kept first; Apply never removes, so a deleted
device waits as a pending removal for CLEAR; First Setup picks per device which version wins when installations
disagree on first open.

**Built:** all of it (Alias Designer A1–A17, Core C1–C3; 42 classes in `bindforge.devicefiles`, draft and
pending-removal tables). The code matches the spec. What the user sees — edit, SAVE, APPLY to all installations —
already matches back up, edit, apply. The separate draft is internal; the git-like part (divergence list, adopt,
revert, label re-merge, adopt three-way into the draft) is on the game-changed-a-file side.

**Decided (Alan):**
- **Keep controller-name editing and Apply as built.**
- **Keep First Setup** — the one-time choice several installations force, not version tracking. (Settles the
  question left open in chunk 1.)
- **The git-like parts for device files are dealt with in chunk 5.**

## Chunk 4 — the user changes which binds file a section uses, and presses Apply — agreed 2026-10-21

**Designed:** four dropdowns, Set All to Same, a warning when the four differ, SAVE / DISCARD / APPLY. Underneath: a
master and a draft file and a three-way merge per line; Apply refuses a line naming no real binds file, keeps the
replaced file, writes four lines, and makes that the new master.

**Built:** P1 (the BindName list), P2 (master, draft, per-line three-way compare) and P3 (Apply with the per-line
merge, commit `0b1e3eb66`, review open). P4 (screen), P5 (change detection, adopt, revert), P6 (unsaved work) not
built. The code matches the spec; the spec is the most git-like part of BindForge, on its simplest file.

**Decided (Alan):**
- **The preset file gets the binds-file rule (chunk 2):** SAVE writes a copy; beside it, what the game's file held
  when editing started (the current master file can be that record). **Apply refuses if the game's file changed
  since** — "reload or discard" — otherwise keeps the old file, writes the new one, and records it.
- **Kept from P1 and P3:** the BindName list, every line must name a real binds file, the four-line CRLF form, the
  replaced file kept first, the atomic write.
- **Removed:** the per-line merge, the conflict result, and P5's adopt.
- **The P3 review** (`code-integrity-review.md`): finding 1 disappears with the merge; findings 2, 5 and 7 still
  apply and are folded into the rework.

## Chunk 5 — the game, or something else, changes a file — agreed 2026-10-21

**Designed:** at startup and on every change, each file compared with BindForge's copy — unchanged, wiped (matches
Frontier's stock), version bump (merge), or edited — with adopt or revert offered, never automatic, and adopt
merging three-way into any unapplied edits. For device files: a green/yellow/red list, one row per device and kind
of problem, each with its own ways out (revert the installation, adopt one device, compare labels); a stored
missed-write flag with a spoken startup line and an on-screen question.

**Built:** binds files — none of the check; with no edits the copy refreshes silently, with edits Apply refuses; a
patch reset and a version bump are not detected. Preset file — nothing (P5 not built). Device files — all of it:
`DeviceFilesCheck`, `DeviceFilesMonitor` (polls every 2 s), `DeviceDivergence` / `DivergenceWaysOut`,
`DeviceFilesAdopt` (per device, and three-way into the draft), `LabelReMerge`, `MissedWrites` / catch-up /
`CatchUpAnnouncement`. The wipe's Error-level condition has no caller.

**Decided (Alan) — one plain rule for every file type:**
1. **Game changed a file, no unapplied edits:** a binds or preset file refreshes quietly (as binds files already
   do); a device file says the installation no longer matches and offers **Put mine back** (revert) or **Keep the
   game's** (whole-installation adopt) — both already exist.
2. **Game changed a file, with unapplied edits:** Apply refuses — "reload or discard", as chunk 2. "Keep the game's"
   is not offered until the edits are applied or discarded.
3. **A patch reset a file:** the same two buttons, worded as a reset, **Put mine back** first. **Binds files gain this
   check** — today they have none.
4. **An installation missed an Apply:** say it is out of date; the next Apply updates it (catch-up already mostly
   does this).

**Removed:** per-device adopt, the label re-merge, the three-way adopt into the draft, and the per-device coloured
list — it becomes one line per installation that does not match.

**Waits on a test:** whether a game update that moves `Custom.4.2.binds` to `Custom.4.3.binds` carries the
bindings across itself. If it does, rule 1 covers it and no version-bump merge is needed.

## Chunk 6 — backups, and getting the files back — agreed 2026-10-21

**Designed:** Player Backups — all four file types for every installation, automatic at launch and on Backup Now,
kept 30 days; a separate Edit History of the replaced file per Apply, per file, last 10, with its own browse and
restore screen; restore into the editing copy, then Apply; plus the one-time before-BindForge and First Setup
backups.

**Built:** Backup Now copies **binds and preset files only** — no device files (`PlayerBackupService.createBackup`,
which also uses the old `findStartPresetFile`). No backup at launch (the setting exists, nothing reads it). No
retention. Restore is binds files only, one at a time, to the editing copy or straight to the game. The
before-BindForge backup holds everything, but its restore covers binds files only. Edit History is written three
ways — device files in `bindings/history/<installId>/` pruned to 10, binds copies in `getBindingsBackupDir()` never
pruned, the preset file in that same folder pruned — with no screen. Saving the binds copy also leaves a `.bak`
beside it.

**Decided (Alan) — option A, one backup system:**
- **Every backup is a full snapshot** of all four file types across all installations, in **one list**.
- **Taken at launch, on Backup Now, and just before every Apply** (labelled "before Apply").
- **Pruned by age.**
- **Restore is one button:** it confirms, backs up what is there now, then writes the backup to the game through
  Apply's write.
- **Edit History stops being separate** — the before-Apply snapshots do its job.
- The gaps get fixed with it: device files backed up and restorable, the launch trigger wired, pruning.

## What comes next — agreed 2026-10-21 (Alan)

The spec rewrite (step 2) starts in a fresh session.

1. **Settle the three open points:** every binds file editable or only the active one (chunk 2); how many days
   backups are kept (chunk 6); the version-bump test in the game (chunk 5) — Alan's to run.
2. **Rewrite the specs to this model, docs only** — the spec wins over code (CLAUDE.md), so it changes first. The
   master/draft/merge/adopt/Edit History sections are replaced, not struck through and kept.
3. **Rewrite the build plan as slices**, one per session, most valuable first:
   - **S1 — the one backup system** (chunk 6).
   - **S2 — nothing before the backup** (chunk 1's launch-time copy).
   - **S3 — the preset file on the binds-file rule** (chunk 4), with P3 review findings 2, 5 and 7.
   - **S4 — the plain rule for device files** (chunk 5): remove per-device adopt, the label re-merge, the three-way
     adopt and the per-device list.
   - **S5 — the patch-reset check for binds files** (chunk 5, rule 3).
   - Then the Preset Editor screen (P4) resumes, on the simplified store.
4. **`code-integrity-review.md` (P3)** is superseded by this, except findings 2, 5 and 7, which go into S3.
