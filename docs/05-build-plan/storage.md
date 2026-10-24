# Storage — back up, edit, apply

**Not a tab.** How BindForge backs up, holds, writes and restores the user's game input configuration files, built
from [storage.md](../02-features/bindforge/storage.md) and [the principles](../02-features/bindforge/principles.md).
It replaces [Core](core.md), whose master, draft, three-way merge, adopt and Edit History are gone from the specs
(2026-10-21 to 10-24, Alan). Every tab writes through this; no tab builds its own.

**The end state, in one paragraph.** One working copy — files in Elite-Intel's folder, laid out like the game's — is
the source of truth. Edits stay on screen until the one APPLY above BindForge's tabs, which runs the check, backs up
everything, saves the edits into the working copy and writes it to every installation. A backup is a dated folder holding
every live copy from every installation, taken silently at every Elite-Intel start and before every write, kept 30
days, first setup's kept forever. RESTORE puts a whole backup back. The check finds a live copy changed outside
BindForge, and the user overwrites, merges or reconciles.

## Spec — read end to end before every slice

- [principles.md](../02-features/bindforge/principles.md) and [storage.md](../02-features/bindforge/storage.md) —
  they win over every other document; where two others disagree, stop and ask Alan (CLAUDE.md rule 17).
- [overview.md](../02-features/bindforge/overview.md) from **Managed File Domains** through **Unsaved Work on Exit**.
- [file-manager.md](../02-features/bindforge/file-manager.md) — backups, restore, Game Install Locations.
- The screen the slice moves: [alias-designer.md](../02-features/bindforge/alias-designer.md) (S2, S4, S9),
  [bind-editor.md — Shell and Capture Dialog](../02-features/bindforge/bind-editor.md#shell-common-to-all-modes)
  (S5, S12), [preset-editor.md](../02-features/bindforge/preset-editor.md) (S6).
- [elite-intel-platform-map.md — the shared write path](../01-host-integration/elite-intel-platform-map.md#the-shared-write-path-is-eliteintelio--built-2026-10-03).

Sections marked *"Built against the master/draft model"* in alias-designer.md keep their rules; only their mechanics
change. The slice that moves them rewrites the *Built* note.

## Built today — what the slices replace (checked 2026-10-24)

| Today | Where | Becomes |
|---|---|---|
| **Done in S1:** the one backup system — dated folders of every live copy, taken at every start and by Backup Now, pruned after the retention setting | `bindforge.storage.Backups`, `Backup`, `BackupSources`, `BackupDestination`, `StartupBackup` (started in `App.main`); `ui.screen.bindings.BindingManagementPanel` (Player Backups) | — |
| Restore to Editing Slot / Restore to Live, reading a backup's `Bindings\` or an old folder's top | `bindforge.io.PlayerBackupService` (restore only since S1) | S3 |
| The "before BindForge" backup, a folder of its own, recorded in `bindforge_activation.backup_folder` | `devicefiles.BeforeBindForgeBackup`, `DeviceFilesSnapshot` | S1, S2 |
| Restore: one `.binds` at a time, to the editing copy or the game | `PlayerBackupService.restoreToWorkingCopy` / `restoreToLive` | S3 |
| Activation and first setup fill a **database** master | `devicefiles.Activation`, `FirstSetup`, `FirstSetupPlan`, `LabelMerge`, `db.managers.BindForgeDeviceDraftManager.establish`, migration `12005` | S2 (the reconcile logic is kept) |
| Device master, draft, pending removals, old names, hardware-id moves | migrations `12002`–`12004`, `12008`; `BindForgeDeviceMasterManager`, `BindForgeDeviceDraftManager`, `BindForgeDeviceInstallsManager`, `BindForgeHardwareIdMoveManager` | S4 |
| Device Apply and per-installation push, Edit History, catch-up | `devicefiles.DeviceApply`, `DeviceFilesPush`, `EditHistory`, `MissedWrites`, `CatchUpAnnouncement`, migration `12006` | S2 (write path), S4 (removed) |
| Divergence list, ways out, installation markers, the 2-second poll | `DeviceDivergence`, `DeviceDivergenceScanner`, `DivergenceWaysOut`, `DeviceFilesAdopt`, `LabelReMerge`, `devices.InstallationMarkers`, `DeviceFilesMonitor`, `DeviceFilesCheck` | S4 (removed), S9 (the new check) |
| `.buttonMap` written at every connect | `devices.ButtonMapStartup`, `devicefiles.ButtonMapGeneration` | S4: default labels written at APPLY |
| `.binds`: a copy of the **active** file, a checksum, its own APPLY / REVERT and close prompt; copied at launch before any backup | `bindforge.io.BindingsWorkingCopyRepository`, `BindingsApplyService`, `ui.screen.bindings.BindingProfilePanel` (`promptCloseWithDraft`) | S2 (launch copy goes), S5 |
| Preset file: master and draft files, per-line merge, its own Apply | `bindforge.preseteditor.StartPresetStore`, `StartPresetComparison`, `StartPresetApply` | S6 |
| The Vega tab's KEYMAP readout | `ui.screen.AiTabPanel` (`keymapBadge`) | S5: removed |
| Installations found; the stored list | `bindforge.install.*`, `InstallationRegistry`, migration `12001`; `GameInstallLocationsPanel` (no Add missing, no match states) | S2, S9 |

**Kept as they are:** `elite.intel.io.AtomicFiles` (**never write a game file any other way**), the readers
(`DeviceMappingsParser`, `ButtonMapReader`, `BindingsLoader`, `BindNameCatalog`), `BindingsWriter` and its device
rewrite, `FrontierStockDevices` / `FrontierStockButtonMaps`, `DeviceNames`, `DeviceRename`, `IdentityQuestions` and
its table (`12007`), `BindingsMonitor` (not BindForge's — it reloads the assistant's keys).

## Settled — and where it is recorded

- **Back up, edit, apply** — [storage.md](../02-features/bindforge/storage.md), the whole page.
- **The working copy lives in `AppPaths.getBindingsWorkingDir()`**, `Bindings\` and `ControlSchemes\DeviceButtonMaps\`
  inside. [overview.md](../02-features/bindforge/overview.md#where-the-working-copy-lives--2026-10-23)
- **Run first-time startup again** is a button on Game Install Locations, shown to every user, to be hidden or
  removed before V1.2 ships (Alan, 2026-10-24). [file-manager.md](../02-features/bindforge/file-manager.md#game-install-locations)
- **Testers already set up go through first-time startup again**, from their live files; nothing is carried over
  from the database master, since V1.2 has not shipped (Alan, 2026-10-24, approving this plan).
- **Until S8, APPLY runs without the check.** The backup before every APPLY is the safety net; V1.2 does not ship
  before S8 (Alan, 2026-10-24, approving this plan).
- **Every slice removes the old code it makes unused**, and its tests — there is no clean-up slice at the end.
- **One write per apply, and never publish `BindingsUpdatedEvent`.** [overview.md](../02-features/bindforge/overview.md#bindforge-must-not-publish-bindingsupdatedevent)
- **Values, never dates or bytes.** [Freshness Checks](../02-features/bindforge/overview.md#freshness-checks)

## Before V1.2 ships

- [ ] **Hide or remove Run first-time startup again** (S2). Alan, 2026-10-24: *"we just have to remember later."*
- [ ] **S8 is done**, so APPLY runs the check.

## Known issues

- **The branch carries both models from S2 to S6.** A tab not yet moved still edits and applies the old way, and its
  writes do not reach the working copy. Run first-time startup again after S6 to start clean.
- **Linux, several installations:** every `.binds` read and write uses the one bindings folder Elite-Intel is set
  to ([input environment §7](../00-overview/input-environment.md#7-linux-the-bindings-folder-is-not-shared)). The
  working copy holds one `Bindings\`; APPLY's confirm keeps A11's Linux note.
- **Migration `12009__hunting_ground_scan_per_commander.sql` is in BindForge's band** (`12000–12499`), so the next
  free BindForge number is **12010**. Raise with Krondor; never edit an applied migration.
- **`BindingsMonitor` ignores `StartPreset.4.start`** — a V1.1 defect, fixed on `V1.1-Release`, not here.
- **Old folder backups** — V1.1's `playerbackups\<time>\` and the `_before-bindforge` and `_first-setup` folders stay
  where they are: not listed, never pruned (Alan, 2026-10-24; [file-manager.md](../02-features/bindforge/file-manager.md#old-folder-backups--v11s-and-the-old-bindforge-ones-settled-2026-10-24-alan)).
  `BeforeBindForgeBackup` and `FirstSetup` keep writing such folders until S2 removes them.

## Boundaries

- **New code** in `elite.intel.bindforge.storage` — backups since S1; the working copy, the write path and the check
  go there too (Alan, 2026-10-24).
- **`bindforge.io`, `bindforge.rules`, `elite.intel.io`, `AppPaths`** are changed here like any V1.2 code.
- **`ai.hands`** — never. **Migrations** from `12010`, never editing an applied one.
- **UI** follows [the HUD canon](../02-features/bindforge/ui-component-map.md); strings in all seven bundles.

## Slices

| # | Slice | Rules | Status |
|---|---|---|---|
| S1 | **One backup system.** A backup is a dated folder holding every live copy — every `.binds`, `StartPreset.4.start`, and each installation's `DeviceMappings.xml` and `.buttonMap`s — named with its date stamp, in Elite-Intel's backup folder (the destination setting, falling back to the default). Full or Partial, naming what could not be read. **Taken silently at every Elite-Intel start, before anything else BindForge does**, whether or not BindForge is ever opened; a failure is logged and retried next start; cannot be switched off. **Backup Now** takes one. The list is the folder's listing, sorted by the stamp — no table. **Retention** at start: older than the setting (30 days, at least 1) is deleted, except the backup marked as first setup's. One call every later write uses: *back up everything*. **Removes:** the folder format in `PlayerBackupService.createBackup` and its `findStartPresetFile`; the auto-backup and Edit History retention settings from the settings screen. Restore stays as it is until S3 | 11, 12 | **done 2026-10-24** |
| S2 | **The working copy, and first-time startup.** The working copy as files; a store that reads it whole and writes it in one step. **The write path:** back up (S1), write the working copy to every live copy — device files whole to every installation, a missing `DeviceMappings.xml` simply written, a matching file not rewritten, a `.buttonMap` the working copy lacks removed except Frontier's shipped ones — writing everything it can, reporting *"updated 2 of 3"*, nothing rolled back. **First-time startup** on the BindForge tab's first click while the flag is not set: the installation list with **Confirm** and **Add missing** (checked for `EliteDangerous64.exe`); device files reconciled by **use one installation for all** (warning when it removes an entry bindings use) or **merge** (`FirstSetupPlan`, `LabelMerge` kept; bindings on a losing name moved to the winner in the working copy's binds files); the backup marked kept forever; the working copy created and written out; the flag set. Game Install Locations gets **Add missing**, **Remove** and **Run first-time startup again**. **Removes:** the launch-time copy of the active `.binds` (`loadOrImportFromGame` before any backup), `BeforeBindForgeBackup`, `DeviceFilesSnapshot`, first setup's database fill | 1–4, 8 | **next** |
| S3 | **RESTORE.** Choose a backup; it backs up what is there now, puts the whole backup back — `.binds` and `StartPreset.4.start` to the bindings folder, device files to every installation as one set, reconciled as in first setup when the backup's installations differ — and replaces the working copy. Its confirmation says everything is replaced and edits on screen are lost. **Browse for Backup File**, with *"Only restore your own backups…"*. **If the working copy cannot be read**, say so and offer the newest backup. **Removes:** `restoreToWorkingCopy`, `restoreToLive`, Restore to Editing Slot | 10 | pending |
| S4 | **APPLY / DISCARD above the tabs, with Alias Designer on it.** Edits not applied, held on screen and shared by the tabs; the bar under Elite-Intel's main tabs, lit only when there are edits. **APPLY:** back up, save the edits into the working copy, write it out (S2), confirm with rule 13's sentence and the Linux note. **DISCARD** drops every edit. Alias Designer reads the working copy; Device Editor SAVE, CLEAR, RESET LABELS, rename, shadow, onboarding, retarget and A17's id move become edits; a rename or id move rewrites the working copy's binds files as part of the edit; default labels shown on screen and written at the next APPLY without lighting the bar. **Removes:** the screen's own APPLY / DISCARD, the divergence list and its ways out, the installation markers, catch-up and `missed_write`, Edit History, startup `.buttonMap` generation, the poll, and the master / draft / pending-removal / old-name / id-move tables (a new migration drops them). **Rewrites** [device-provenance.md](../03-data-models/device-provenance.md) to what is left | 6, 7, 8, 13 | pending |
| S5 | **Bind Editor on the bar.** Load opens any binds file from the working copy, not only the active one; capture SAVE and the inline toggles become edits. **Removes:** its APPLY / REVERT, `promptCloseWithDraft`, the checksum in `BindingsWorkingCopyRepository` and the `.bak` beside it, `BindingsApplyService`'s own backup folder, the KEYMAP readout | 6, 7 | pending |
| S6 | **The preset file on the bar.** The working copy's `StartPreset.4.start`; **APPLY refuses first**, naming the sections, when any of the four lines is empty or names no binds file the BindName list offers — before any backup or write. Written as four lines, CRLF, no final newline. **P3 review [finding 2](../../code-integrity-review.md):** the live file is found once and that file is read and written. **Finding 5:** a test that nothing is written when the backup fails. **Removes:** `StartPresetStore`'s master and draft, `StartPresetComparison`, `StartPresetApply`'s merge. Then [P4](preset-editor.md#slices), the screen, resumes | 7 | pending |
| S7 | **Leaving with edits not applied** — another Elite-Intel tab, or closing Elite-Intel: **Go back**, **Discard**, **Apply**; nothing selected, Enter chooses nothing, Esc and the close glyph are Go back | 9 | pending |
| S8 | **The check: binds and preset files.** The first BindForge click in a session and every APPLY; compared by value. Warn, then: overwrite the live copy, **merge** (every differing line item a row, nothing pre-selected, Keep all mine / Keep all in-game, merge grain per slot), or overwrite the working copy. A live copy matching Frontier's factory file is named a reset, putting the working copy back offered first, *"Are you sure?"* before it replaces the working copy. At APPLY with edits on screen the check settles the working copy first. **At Elite-Intel start**, read only: one spoken and logged line when anything differs | 5, 13, 14, 15 | pending |
| S9 | **The check: device files.** An installation that no longer matches is reconciled as in first setup, the working copy one side — keep it, keep the installation's, or merge — then written to every installation after the backup. A missing or factory `DeviceMappings.xml` named as a reset. Game Install Locations shows *matches* / *differs* / *not found* from the same comparison | 14 | pending |
| S10 | **A newer game version.** `.4.2.binds` → `.4.3.binds`, or a newer XML root version: never overwritten. All bindings carried over → approve it becoming the working copy under its new name; otherwise new slots inserted, the rest decided line by line, the working copy renamed. The new slots are listed | 14 | pending |
| S11 | **CREATE** at the left of the bar while the user has no custom binds file; the three editors greyed until it is pressed, File Manager usable. A factory binds file (`KeyboardMouseOnly` first) and a name (`Custom`); the file and all four preset lines are edits not applied | 16 | pending |
| S12 | **Renaming a binds file** — the Bind Editor's Name box beside the file dropdown: file name, `PresetName` and every preset line change together as one edit; APPLY removes the old-named live file after the backup; read-only for a factory binds file | 7 | pending |

**Why this order.** S1 protects every user, BindForge opened or not — principle 2, and the one thing done unasked.
S2 makes the working copy, which everything else reads and writes. S3 is File Manager's primary feature and needs
only S1 and S2. S4–S6 move each tab onto the bar, S4 first because it carries the most old machinery. S7 closes the
edit model. S8–S10 are the check, which APPLY then runs. S11 and S12 are new features on top.

**After these:** Preset Editor P4 (after S6); Bind Editor E2–E6 (after S5); Alias Designer A19, CLEAR moving bindings
to a hardware ID (after S4); A18 and E7 stay blocked on [testing item 27](../00-overview/testing-required.md#bindforge).

## Hand-off notes

*Newest first. Two or three lines each: what was decided and where it is recorded, what was found, what the next
slice needs.*

- **2026-10-24 — S1 done.** **Decided (Alan):** a backup is a plain folder, not a zip, as in V1.1 — recorded in
  principles.md *Backup*, storage.md rule 12 and file-manager.md *Backup Format*; old folder backups untouched and
  unlisted; no Delete button; `ButtonMapStartup` waits for the startup backup and writes nothing if it failed; migration
  `12010` drops `auto_backup_on_launch` (there is no settings screen; `edit_history_retention` stays until S4). Layout,
  `backup-info.txt` and retention-only-after-success are in file-manager.md. **S2 needs:** call
  `Backups.stored().backUpEverything(Kind.FIRST_SETUP)` for first setup's kept-forever backup, and `backUpEverything()`
  before every write; the existing writers (`DeviceApply`, `StartPresetApply`, `BindingsApplyService`) still take their
  own backups until their slices move them.
- **2026-10-24 — brief written** (Alan approved the slice list, with Run first-time startup again added to S2). The
  specs were rewritten to back up, edit, apply in `6eb29435c`; this brief replaces [core.md](core.md). **Found:** the
  Preset Editor screen was never built, so S6 changes a store with no screen; `12009` sits in BindForge's band.
  **S1 needs:** the old backup folders question settled in its sources table, and the package for the new code.
