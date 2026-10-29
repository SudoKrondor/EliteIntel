# Storage — back up, edit, apply

**Not a tab.** How BindForge backs up, holds, writes and restores the user's game input configuration files, built
from [storage.md](../02-features/bindforge/storage.md) and [the principles](../02-features/bindforge/principles.md).
It replaces [Core](core.md), whose master, draft, three-way merge, adopt and Edit History are gone from the specs
(2026-10-21 to 10-24, Alan). Every tab writes through this; no tab builds its own.

**The end state, in one paragraph.** One working copy — files in Elite-Intel's folder, laid out like the game's — is
the source of truth. Edits stay on screen until the one APPLY above BindForge's tabs, which runs the check, backs up
everything, saves the edits into the working copy and writes it to every installation. A backup is a zip holding every
live copy from every installation, of four kinds - daily (the first start each day, kept by age), before changes (every
write, kept by count), user (Backup Now, kept until deleted) and first setup (kept forever). RESTORE puts a whole backup
back. The check finds a live copy changed outside
BindForge, and the user overwrites, merges or reconciles.

## Spec — read end to end before every slice

- [principles.md](../02-features/bindforge/principles.md) and [storage.md](../02-features/bindforge/storage.md) —
  they win over every other document; where two others disagree, stop and ask Alan (CLAUDE.md rule 17).
- [overview.md](../02-features/bindforge/overview.md) from **Managed File Domains** through **Unsaved Work on Exit**.
- [file-manager.md](../02-features/bindforge/file-manager.md) — backups, restore, Game Install Locations.
- The screen the slice moves: [alias-designer.md](../02-features/bindforge/alias-designer.md) (S2b, S4, S9),
  [bind-editor.md — Shell and Capture Dialog](../02-features/bindforge/bind-editor.md#shell-common-to-all-modes)
  (S5, S12), [preset-editor.md](../02-features/bindforge/preset-editor.md) (S6).
- [elite-intel-platform-map.md — the shared write path](../01-host-integration/elite-intel-platform-map.md#the-shared-write-path-is-eliteintelio--built-2026-10-03).

Sections marked *"Built against the master/draft model"* in alias-designer.md keep their rules; only their mechanics
change. The slice that moves them rewrites the *Built* note.

## Built today — what the slices replace (checked 2026-10-26)

| Today | Where | Becomes |
|---|---|---|
| **Done in S1 and S1b:** the one backup system — a zip of every live copy in `Daily\`, `BeforeChanges\`, `User\` or `FirstSetup\`; the daily one at the first start each day, user ones by Backup Now (Delete for these only); daily kept by age, before-changes by count; retention settings on Player Backups (migration `12011`) | `bindforge.storage.Backups`, `Backup`, `BackupSources`, `BackupDestination`, `StartupBackup` (started in `App.main`); `ui.screen.bindings.BindingManagementPanel` (Player Backups) | — |
| **Done in S3:** RESTORE - the whole backup back, through `WritePath`, after a before-RESTORE backup; a backup whose installations differ reconciled in `FirstSetupDialog`; Browse for Backup File; the working copy read at every tab click, the newest backup offered when it cannot be; the read-only flag kept by every live write | `bindforge.storage.Restore`, `BackupContents`, `ReadOnlyFlag`, `Written` (shared with first-time startup), `LiveCopies`; `ui.screen.bindings.RestoreFlow`, `BindingManagementPanel`, `FirstTimeStartupFlow` | - |
| **Done in S2b:** first-time startup - the installation list (Confirm / Add missing), the reconcile (use one installation for all, or merge), the working copy written through `WritePath` with the first setup backup, the flag; Game Install Locations' Add missing, Remove (stays removed), Run first-time startup again (migration `12012`) | `bindforge.storage.FirstTimeStartup`, `FirstSetupReading`, `InstallationFiles`, `FirstSetupFiles`; `ui.screen.bindings.FirstTimeStartupFlow`, `InstallationListDialog`, `FirstSetupDialog`, `GameInstallLocationsPanel` | - |
| **Done in S4a:** the edits not applied, in memory and shared by the tabs; the APPLY / DISCARD bar above BindForge's tabs; APPLY through `WritePath` with a before-APPLY backup, leaving alone an installation whose device files differ by value; the Alias Designer and Device Editor on the edits - SAVE, rename, shadow, going back, CLEAR, RESET LABELS, the undos, onboarding's FINISH with A17's old-id move; default labels written at APPLY | `bindforge.storage.Edits`, `DeviceEdits`, `BindsFiles`, `Apply`, `DeviceFilesComparison` (and `WritePath`'s hand-off and leave-alone, `WriteReport.Place.differs`); `ui.screen.bindings.ApplyBar`, `AliasDesignerReader`, `AliasDesignerController`, `AliasDesignerPanel`, `DeviceEditorDialog`; `ui.event.BindForgeEditsChangedEvent` | - |
| **Done in S4b:** the identity question reads the edits and its retarget is an edit; the Bind Editor's controller names (the working copy's) and labels (the edits'); the device master, draft, pending removals, per-installation rows, hardware-id moves and `missed_write` dropped (migration `12013`) | `bindforge.storage.SavedDeviceLabels`, `DeviceEdits.retarget`, `DeviceMappingsMerge.retarget`; `bindforge.devices.IdentityQuestions`, `CaptureControllers`; `ui.screen.bindings.AliasDesignerReader`, `AliasDesignerController` | - |
| **Done in S5:** the Bind Editor on the edits - any binds file loads (LOAD), the General line's file first; capture SAVE (all or none), the Clear column and Auto-Assign are edits; the writer edits bytes in memory; the capture tab names a controller as the edits do; the AI tab's counts from the live bindings | `bindforge.storage.BindsEdits`, `SavedDeviceLabels`; `bindforge.io.BindingsWriter`, `EditedBinds`; `ui.screen.bindings.BindingProfilePanel`, `ui.dialog.ClearKeyboardBindingDialog` | - |
| ~~Device Apply and per-installation push, Edit History, catch-up~~ **removed in S4a**; `missed_write` dropped in S4b | - | - |
| **Done in S6:** the preset file on the edits; APPLY refuses first, naming the sections, a preset line that is empty or names no binds file the BindName list offers (the list read from the edits and each installation's factory files); the preset file's master, draft, merge and history, `TimestampedBackups`, `getBindingsBackupDir` and `edit_history_retention` removed (migration `12014`) | `bindforge.storage.PresetEdits`, `PresetRefusedException`, `Apply`; `bindforge.preseteditor.BindNameCatalog`; `ui.screen.bindings.ApplyBar` | - |
| ~~Divergence list, ways out, installation markers, the 2-second poll~~ **removed in S4a**, with `io.FilePoller` | - | S9 (the new check) |
| ~~`.buttonMap` written at every connect~~ removed in S2b; **default labels written at APPLY since S4a**, worked out as it runs since the S4a review | `AliasDesignerReader.defaultLabels`, `Apply.DefaultLabels` | - |
| ~~`.binds`: a copy of the **active** file, a checksum, its own APPLY / REVERT and close prompt~~ **removed in S5** | - | - |
| ~~Preset file: master and draft files, per-line merge, its own Apply, `edit_history_retention`~~ **removed in S6** | - | - |
| ~~The Vega tab's KEYMAP readout~~ **removed in S5** | - | - |
| Installations found; the stored list | `bindforge.install.*`, `InstallationRegistry`, migrations `12001`, `12012`; `GameInstallLocationsPanel` (no match states yet) | S9 |

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
- **No interim compatibility** (Alan, 2026-10-24): `V1.2-BindForge` has no users, so nothing is kept working, or
  bridged, between slices. A slice that replaces old code removes it, even when another screen stops working until its
  own slice. Never chosen for, or argued from, keeping users happy.
- **S2 is two slices, S2a and S2b** (Alan, 2026-10-24), in the order the table gives; S3 stays before S4.
- **S4 is two slices, S4a and S4b** (Alan, 2026-10-26): the bar, the Device Editor and onboarding first, then the identity
  question and the tables. The identity retarget is broken between the two.
- **How the working copy and the live copies are written** — [storage.md, How the files are written](../02-features/bindforge/storage.md#how-the-files-are-written--2026-10-24).
- **Game Install Locations loses Rescan and Relocate, and a removed installation stays removed** — [file-manager.md](../02-features/bindforge/file-manager.md#game-install-locations).

## Before V1.2 ships

- [ ] **Hide or remove Run first-time startup again** (S2b). Alan, 2026-10-24: *"we just have to remember later."*
- [ ] **S8 is done**, so APPLY runs the check.

## Known issues

- **Edits not applied are lost when Elite-Intel closes**, without a word, until S7's leave prompt (Alan, 2026-10-26,
  accepted under no interim compatibility).

- **Until P4 nothing in BindForge can fix a refused preset file** (Alan, 2026-10-28, accepted under no interim
  compatibility). The Preset Editor screen is not built, so a working-copy preset file whose line names a binds file
  BindForge cannot find blocks every APPLY until P4, RESTORE or Run first-time startup again. Unlikely, since the file
  came from the game.
- **Testers set up before S6** should run first-time startup again, so the working copy holds the live preset file
  rather than one the old Preset Editor left in its own store.
- **The old preset store's files** (`elite-intel/bindings/master\`, `draft\`) and the old copies in
  `elite-intel/bindings/backups\` stay on disk; nothing reads them (Alan, 2026-10-28).
- **The old Bind Editor's loose files** (`<name>.binds`, `.bak`, `.sha256`) stay at the top of the working copy folder;
  nothing reads them (Alan, 2026-10-28; overview.md *Where the working copy lives*).
- **Until S9:** an installation that missed a write differs from the working copy, so APPLY leaves it alone and nothing
  puts the working copy there - rule 8 promises the next APPLY only the bindings folder since 2026-10-27, and the
  report says the installation is left as it is until the check (storage.md, *How the files are written*). Accepted
  by Alan 2026-10-27 - nothing is built for it before S9, which must close it. A `.buttonMap` kept because it is read-only makes its installation differ the same way, so the report tells the user to delete it themselves rather than to clear the flag for BindForge to remove it (review, 2026-10-27).
- **Linux, several installations:** every `.binds` read and write uses the one bindings folder Elite-Intel is set
  to ([input environment §7](../00-overview/input-environment.md#7-linux-the-bindings-folder-is-not-shared)). The
  working copy holds one `Bindings\`; APPLY's confirm keeps A11's Linux note.
- **Migration `12009__hunting_ground_scan_per_commander.sql` is in BindForge's band** (`12000–12499`). Raise with
  Krondor; never edit an applied migration.
- **`BindingsMonitor` ignores `StartPreset.4.start`** — a V1.1 defect, fixed on `V1.1-Release`, not here.
- **Old folder backups** — V1.1's `playerbackups\<time>\` and the `_before-bindforge` and `_first-setup` folders stay
  where they are: not listed, never pruned (Alan, 2026-10-24; [file-manager.md](../02-features/bindforge/file-manager.md#old-folder-backups--v11s-and-the-old-bindforge-ones-settled-2026-10-24-alan)).
  Since S2b nothing writes such folders.

## Boundaries

- **New code** in `elite.intel.bindforge.storage` — backups since S1; the working copy, the write path and the check
  go there too (Alan, 2026-10-24).
- **`bindforge.io`, `bindforge.rules`, `elite.intel.io`, `AppPaths`** are changed here like any V1.2 code.
- **`ai.hands`** — never. **Migrations:** look in `db-migration\` when you write one and take the next free number in
  `12000–12499` then — the brief names no number, because a named number goes stale. If Krondor lands the same number
  first, whoever merges second renumbers, which is safe while the migration has not shipped (Alan, 2026-10-24). Never
  edit an applied one.
- **UI** follows [the HUD canon](../02-features/bindforge/ui-component-map.md); strings in all seven bundles.

## Slices

| # | Slice | Rules | Status |
|---|---|---|---|
| S1 | **One backup system.** A backup is a dated folder holding every live copy — every `.binds`, `StartPreset.4.start`, and each installation's `DeviceMappings.xml` and `.buttonMap`s — named with its date stamp, in Elite-Intel's backup folder (the destination setting, falling back to the default). Full or Partial, naming what could not be read. **Taken silently at every Elite-Intel start, before anything else BindForge does**, whether or not BindForge is ever opened; a failure is logged and retried next start; cannot be switched off. **Backup Now** takes one. The list is the folder's listing, sorted by the stamp — no table. **Retention** at start: older than the setting (30 days, at least 1) is deleted, except the backup marked as first setup's. One call every later write uses: *back up everything*. **Removes:** the folder format in `PlayerBackupService.createBackup` and its `findStartPresetFile`; the auto-backup and Edit History retention settings from the settings screen. Restore stays as it is until S3 | 11, 12 | **done 2026-10-24** |
| S1b | **Four kinds of backup, every one a zip** (Alan, 2026-10-24; [file-manager.md — Four kinds of backup](../02-features/bindforge/file-manager.md#four-kinds-of-backup--settled-2026-10-24-alan)). `Backups` writes zips — `backup-info.txt` inside, written to a temporary file and renamed into place — into `Daily\`, `BeforeChanges\`, `User\` and `FirstSetup\`. **Daily:** the first Elite-Intel start each day, retried at the next start if it failed; kept 30, 60, 90 or 180 days (default 30). **Before changes:** the kind every write uses — APPLY, RESTORE of what is there now, the check before the working copy is replaced — recording which in `backup-info.txt`; the newest 100 kept (10 to 1000). **User:** Backup Now; kept until the user deletes them, with **Delete** for user backups only (`HudConfirmDialog`). **First setup:** kept forever. One list, newest first, with a **Taken** column. **A retention settings section on Player Backups:** Backups go to, Keep daily backups for, Keep before-changes backups — a migration replaces `backup_retention_days` with the two new settings. S1's dated folders are left untouched and unlisted, like V1.1's. **Restore to Editing Slot / Restore to Live** read the `.binds` from the zip until S3 | 11, 12 | **done 2026-10-24** |
| S2a | **The working copy store and the write path.** `WorkingCopy` reads the working copy whole and writes it in one step (a folder swap, finished or thrown away after a crash); `LiveCopies` writes it to every live copy — device files whole to every installation, a missing `DeviceMappings.xml` simply written, a file with the same bytes not rewritten, a `.buttonMap` the working copy lacks removed except Frontier's shipped ones — everything it can, reported per place (*"updated 2 of 3"*), nothing rolled back; `WritePath` backs up first, then the working copy, then the live copies. No UI, no migration, no caller yet | 3, 7, 8 | **done 2026-10-24** |
| S2b | **First-time startup.** On the BindForge tab's first click while the flag is not set: the installation list with **Confirm** and **Add missing** (checked for `EliteDangerous64.exe`); device files reconciled by **use one installation for all** (that installation's files whole; warning when it removes an entry bindings use) or **merge** (`FirstSetupPlan`, `LabelMerge` kept; bindings on a losing name moved to the winner in the working copy's binds files); the working copy created and written out through `WritePath` with the first setup backup; the flag set. Game Install Locations: **Add missing**, **Remove** (stays removed), **Run first-time startup again**; Rescan and Relocate removed. A migration adds the removed flag and drops `bindforge_activation.backup_folder`. **Removes:** `BeforeBindForgeBackup`, `DeviceFilesSnapshot`, `Activation`'s silent path, first setup's database fill (the master stays empty until S4 removes it) | 1–4, 8 | **done 2026-10-25** |
| S3 | **RESTORE.** Choose a backup; it backs up what is there now, puts the whole backup back — `.binds` and `StartPreset.4.start` to the bindings folder, device files to every installation as one set, reconciled as in first setup when the backup's installations differ; an installation the backup does not hold is replaced too, no reconcile, and the confirmation names every installation (Alan, 2026-10-25) — and replaces the working copy. Its confirmation says everything is replaced and edits on screen are lost. **Browse for Backup File**, with *"Only restore your own backups…"*. **If the working copy cannot be read**, say so and offer the newest backup. **Removes:** `restoreToWorkingCopy`, `restoreToLive`, Restore to Editing Slot | 10 | **done 2026-10-25** |
| S4a | **The APPLY / DISCARD bar, and the Device Editor on it** (S4 split in two, Alan, 2026-10-26). Edits not applied, held on screen and shared by the tabs (`bindforge.storage.Edits`); the bar under Elite-Intel's main tabs, lit only when there are edits. **APPLY:** back up, save the edits into the working copy, write it out (S2), confirm with rule 13's sentence and the Linux note; **an installation whose device files differ from the working copy, by value, is not written and is reported** until it is reconciled (S9) (Alan, 2026-10-26). **DISCARD** drops every edit; RESTORE and first-time startup drop them too. Alias Designer reads the working copy; Device Editor SAVE, CLEAR, RESET LABELS, rename and shadow, and **onboarding's FINISH with A17's old-id move** (Alan, 2026-10-26: its writes went through the push this slice removes), become edits; a rename or a binding move rewrites the working copy's binds files as part of the edit; default labels shown on screen and written at the next APPLY without lighting the bar. **Removes:** the screen's own APPLY / DISCARD, the divergence list and its ways out, the installation markers and the Device Editor's install strip, catch-up and its startup line, Edit History, the poll, `DeviceApply`, `DeviceFilesPush` and `DeviceOnboarding` | 6, 7, 8, 13 | **done 2026-10-26** |
| S4R | **Krondor's storage review** (Alan, 2026-10-27): the findings in [Krondor's review](#krondors-review--2026-10-27). He reviewed the storage code up to S3 and the `V1.2-i18n-IT` merge, so **check each finding against today's code first** - S3's follow-ups and S4a may have fixed some - and fix only those still true; say which were already fixed. Findings 2, 3 and 14 are in Andrea's Supertonic and gamepad work, merged into this branch: they are fixed here like the rest (Alan, 2026-10-27). **Then narrowed by Krondor, the same day: only BindForge changes, by package** - 14 (`devices.model.DeviceDescription`) and 15 (`CLAUDE.md`) are dropped; 2 and 3 stay (`bindforge.devices`) | - | **done 2026-10-27** |
| S4b | **The identity question on the bar, and the old tables gone** (Alan, 2026-10-26). The identity retarget becomes an edit, and the questions read the working copy. **Removes:** the master / draft / pending-removal / old-name / id-move tables, `missed_write` and `edit_history_retention` (a new migration drops them), their managers and DAOs. The Bind Editor's label readers (`SavedDeviceLabels`, `CaptureControllers`, `InputSettingsPanel`) read the working copy and the edits. **Rewrites** [device-provenance.md](../03-data-models/device-provenance.md) to what is left | 6, 7 | **done 2026-10-28** |
| S5 | **Bind Editor on the bar.** Load opens any binds file from the working copy, not only the active one; capture SAVE and the inline toggles become edits. **Removes:** the launch-time copy of the active `.binds` (`loadOrImportFromGame`), its APPLY / REVERT, `promptCloseWithDraft`, the checksum in `BindingsWorkingCopyRepository` and the `.bak` beside it, `BindingsApplyService`'s own backup folder, the KEYMAP readout. **Then the capture tab names a controller as the edits do** - `SavedDeviceLabels.names()` and `nameOf` read the applied set only because the Bind Editor writes the live `.binds` until this slice | 6, 7 | **done 2026-10-28** |
| S6 | **The preset file on the bar.** The working copy's `StartPreset.4.start`; **APPLY refuses first**, naming the sections, when any of the four lines is empty or names no binds file the BindName list offers — before any backup or write. Written as four lines, CRLF, no final newline. **P3 review finding 2:** the live file is found once and that file is read and written. **Finding 5:** a test that nothing is written when the backup fails. **Removes:** `StartPresetStore`'s master and draft, `StartPresetComparison`, `StartPresetApply`'s merge and its own history, and `bindforge_settings.edit_history_retention` with `BindForgeSettingsManager`'s getter and setter (a migration drops the column - Alan, 2026-10-28, from S4b). Then [P4](preset-editor.md#slices), the screen, resumes | 7 | **done 2026-10-28** |
| S6R | **The S5/S6 code-integrity review follow-up** (Alan, 2026-10-29, all eight decisions as recommended; the review is `code-integrity-review.md` at the repo root, findings numbered there). **D1, finding 3:** `BindNameCatalog.controlSchemesOf` lists only installations that pass `BackupSources.Installation.reachable`; when a preset line is refused because an installation could not be reached, APPLY says the installation is not reachable instead of blaming the preset file - still refused; a test for that message. **D2, finding 4:** delete `KeyboardKeyAvailabilityService`'s `Path` overloads of `isKeyOccupiedByOtherSlot` and the unused `availableKeys`, `occupiedKeyboardKeys`, `currentKeyboardSlotKey`; move their tests onto the bytes version. **D3, finding 5:** remove `BindingSaveResult.WRITE_FAILED`; one message for a failed Bind Editor change (*"The change could not be made; nothing was changed."*) in all seven bundles, replacing `bindings.assign.writeFailed`. **D4:** findings 1 (`bindforge/PACKAGE.md`: cut S5's removed classes, reword row 7), 2 (`CLAUDE.md`'s `bindforge` row - gitignored, local only), 7 (`button.close`), 8 (drop the two `set(0)` and their comment), 11 (`KeyBindingsParser`'s bytes parse uses the hardened builder), 12 (log the exception, not its message). **D5, finding 6:** `ClearOutcome` carries an enum `CLEARED / REFUSED / ALREADY_REPORTED`. **D6, finding 9:** `AtomicReference` for every result carried out of an `Edits.change` lambda; `Edits` unchanged. **D7, finding 10:** the binds-file and preset-file name patterns, `version(name)` and `presetGeneration(name)` in `FileNames`; `BindNameCatalog`'s second `.binds` filter dropped. **D8:** finding 13 left; the 18 `JOptionPane` uses in the Bind Editor (`BindingProfilePanel`, `ClearKeyboardBindingDialog`, `BindingSaveResultPresenter`) added to the [Bind Editor brief](bind-editor-editing.md)'s notes as their own item | - | **done 2026-10-29** |
| S7 | **Leaving with edits not applied** — another Elite-Intel tab, or closing Elite-Intel: **Go back**, **Discard**, **Apply**; nothing selected, Enter chooses nothing, Esc and the close glyph are Go back | 9 | **next** |
| S8 | **The check: binds and preset files.** The first BindForge click in a session and every APPLY; compared by value. Warn, then: overwrite the live copy, **merge** (every differing line item a row, nothing pre-selected, Keep all mine / Keep all in-game, merge grain per slot), or overwrite the working copy. A live copy matching Frontier's factory file is named a reset, putting the working copy back offered first, *"Are you sure?"* before it replaces the working copy. At APPLY with edits on screen the check settles the working copy first. **At Elite-Intel start**, read only: one spoken and logged line when anything differs | 5, 13, 14, 15 | pending |
| S9 | **The check: device files.** An installation that no longer matches is reconciled as in first setup, the working copy one side — keep it, keep the installation's, or merge — then written to every installation after the backup. A missing or factory `DeviceMappings.xml` named as a reset. Game Install Locations shows *matches* / *differs* / *not found* from the same comparison | 14 | pending |
| S10 | **A newer game version.** `.4.2.binds` → `.4.3.binds`, or a newer XML root version: never overwritten. All bindings carried over → approve it becoming the working copy under its new name; otherwise new slots inserted, the rest decided line by line, the working copy renamed. The new slots are listed | 14 | pending |
| S11 | **CREATE** at the left of the bar while the user has no custom binds file; the three editors greyed until it is pressed, File Manager usable. A factory binds file (`KeyboardMouseOnly` first) and a name (`Custom`); the file and all four preset lines are edits not applied | 16 | pending |
| S12 | **Renaming a binds file** — the Bind Editor's Name box beside the file dropdown: file name, `PresetName` and every preset line change together as one edit; APPLY removes the old-named live file after the backup; read-only for a factory binds file | 7 | pending |

**Why this order.** S1 protects every user, BindForge opened or not — principle 2, and the one thing done unasked.
S2 makes the working copy, which everything else reads and writes. S3 is File Manager's primary feature and needs
only S1 and S2. S4–S6 move each tab onto the bar, S4 first because it carries the most old machinery. S7 closes the
edit model. S8–S10 are the check, which APPLY then runs. S11 and S12 are new features on top.

**After these:** Preset Editor P4 (S6 done 2026-10-28); Bind Editor E2–E6 (after S5); Alias Designer A19, CLEAR moving bindings
to a hardware ID (after S4b); A18 and E7 stay blocked on [testing item 27](../00-overview/testing-required.md#bindforge).

## Krondor's review — 2026-10-27

*Krondor's review of the storage code (S1 to S3) and the `V1.2-i18n-IT` merge, as he sent it to Alan. Built in slice
S4R. His own numbers are kept. He found no new dependencies, no `@SuppressWarnings`, no event-shape changes and no
effect on security or the journal-only rule, and left out uncalled API and the Alias Designer gap (S4a has since
closed it). He judged the storage code careful overall: the write order explicit, interrupted writes handled,
failures reported per place, tests in the matching package, migrations `12010`–`12012` clean.*

**Most important**

1. **Wrong "nothing was written" message.** If first-time startup cannot record that it is complete, it has already
   written the backup, the working copy and the game files, yet the flow tells the user *"Nothing was written to your
   game files"*. RESTORE handles the same failure correctly with `setUpAskedAgain`. No test covers it.
2. **Axis capture fires on its own.** It measures movement from centre, not from where the axis rests, so a throttle,
   slider or toe brake resting at -1 is assigned the moment it jitters. `DeviceEditorLights` already learns each
   axis's rest position; capture should too. *(Andrea's work.)*
3. **Gamepad sticks get `Joy_` tokens** such as `Neg_Joy_XAxis`. The repo's own format reference says a gamepad never
   uses `Joy_` tokens; its sticks are `GamePad_LStickX` and the like. Krondor marked it *plausible*, not confirmed:
   which token family the game writes is not measured yet. *(Andrea's work.)*
4. **`FirstSetupFiles.identical()` can throw on the Swing UI thread.** With two identical installations that both
   lack `DeviceMappings.xml` it throws, leaving first-time startup unavailable for the rest of the run, or Player
   Backups busy for good.
5. **First-time startup can overwrite an installation it never reconciled.** The write looks for installations again
   instead of using the ones it read, so a drive plugged in while the dialogs are open gets the working copy written
   over it - which storage.md rule 1 forbids.

**Robustness**

6. Backup Now catches only `IOException`; a database failure leaves Player Backups disabled until restart.
7. `LiveCopies.isReadOnly` swallows errors and returns false, so a protected `.buttonMap` gets deleted.

**Consistency, duplication and docs**

8. First-time startup and the backup and write disagree on what counts as a reachable installation.
9. Constants, helpers and the "file name, not a path" check are duplicated, and the duplicates differ.
10. Three functional interfaces have the same signature.
11. `RestoreFlow` and `FirstTimeStartupFlow` repeat the same background-step and message code.
12. `ReadOnlyFlag` checks the OS itself instead of using `OsDetector`.
13. `BackupSources.gather()` writes to the database despite its name.
14. A comment on the gamepad axis order contradicts the list it describes. *(Andrea's work.)*
15. The CLAUDE.md package table does not mention `bindforge.storage`.
16. to 18. Smaller items: cleanup in `LiveCopiesTest`, a lost cause on a corrupt zip, and the merge running on the
    Swing UI thread.

## Hand-off notes

*Newest first. Two or three lines each: what was decided and where it is recorded, what was found, what the next
slice needs.*

- **2026-10-29 — S6R done: the S5/S6 review follow-up.** All eight decisions built as approved. **Recorded:** the
  reachable-only factory list and the not-reachable refusal in preset-editor.md *What APPLY checks and writes*; the
  one change-failed message in bind-editor.md *Shell*; the 18 `JOptionPane` uses in the Bind Editor brief's notes.
  **Built:** `Apply.offered(files, rows)` lists each reachable installation's ControlSchemes (`BackupSources.reachable`)
  and returns the rest as `Offered.unreachable`; a refusal carries them (`PresetRefusedException.withUnreachable`) and
  `ApplyBar.presetRefusedText` names them (`bindings.applyBar.presetUnreachable`). `BindNameCatalog.controlSchemesOf`
  is gone - the folders are chosen in `storage`, which already depends on `preseteditor`. Name rules in `FileNames`
  (`isBindsFile`, `isPresetFile`, `bindName`, `version`, `presetGeneration`), off `Backups`, `BindsEdits`, `PresetEdits`.
  `PACKAGE.md`'s S5 leftovers rewritten. **Found / left:** finding 13 left as decided; `Apply.offered` is
  package-private, so P4 will need a public way to it. **S7 needs:** nothing new from this slice; `Edits` is unchanged.
- **2026-10-29 — S5/S6 code-integrity review: S6R added.** Alan approved all eight decisions as recommended; they are in the S6R row. The review is `code-integrity-review.md` at the repo root. S7 follows S6R.
- **2026-10-28 — S6 done: the preset file on the bar.** **Decided (Alan, all seven):** the check runs on every APPLY,
  a set with no preset file refused for all four; the BindName list from the edits plus each installation's factory
  files; the four-line form written only when a line changes; lines put back restore the working copy's bytes; the
  live-folder preset reads, `TimestampedBackups` and `getBindingsBackupDir` removed with their last caller; a refused
  preset file blocks APPLY until P4 (*Known issues*). Recorded in preset-editor.md *What APPLY checks and writes* and
  *How the four lines are read*, and file-manager.md *Settings*. **Built:** `PresetEdits` (`presetFile`, `sections`,
  `change`, `gameForm`, `check`, `bindsFiles`), `PresetRefusedException`, `Apply.BindNames`; `BindsEdits.generalFile`
  picks the preset file through `PresetEdits`; `BindNameCatalog.list` takes the set's binds files (a player's file is a
  `Path` of its name alone); migration `12014`; `bindings.applyBar.presetRefused` in seven bundles. **Found / left:**
  `CLAUDE.md`'s `elite.intel.io` row updated, but the file is gitignored; `PACKAGE.md` stale from S5 (*Known issues*).
  **P4 needs:** `PresetEdits.sections` for the dropdowns, `Edits.change(EditFromApplied)` with `PresetEdits.change`,
  and `BindNameCatalog.list(PresetEdits.bindsFiles(current), controlSchemesOf(...))` for what they offer (`Apply.offered` since S6R). **S7 needs:**
  nothing new from this slice.

- **2026-10-28 — S5 done: the Bind Editor on the bar.** **Decided (Alan):** LOAD offers every binds file in the edits,
  the General line's newest first; Bindings Directory stays on the screen for now; the AI tab counts the live bindings;
  the writer's uncalled multi-file rewrite removed; the old loose files left (bind-editor.md *Shell*, overview.md *There
  is one writer* and *Where the working copy lives*). **Decided while building** (bind-editor.md *Shell*): one SAVE is all
  or none; the Clear column and Auto-Assign are edits too; the capture tab names a controller as the edits do (*Which
  name a controller's tab writes*). **Built:** `BindsEdits`, `EditedBinds`; the writer, parser and keyboard
  availability read bytes; `SavedDeviceLabels.of(edited)`; reads and changes off the EDT; 35 strings removed, 4 added.
  **Found / left:** `bindings.assign.writeFailed` ("Could not write the binding file") now means the edits could not be
  changed - reword when the screen is next touched; the Bind Editor shows no warning before first-time startup, just an
  empty grid. **S6 needs:** the preset file through `Edits` the same way - `ConfigurationFiles.withBindingsFile` takes the
  preset file too; then `TimestampedBackups` and `getBindingsBackupDir` may have no caller left.

- **2026-10-28 — S4R and S4b review follow-up** (`code-integrity-review.md`, Alan: all recommendations). **Decided:**
  the assign dialog's labels are read once by `BindingProfilePanel` and passed in, still on the EDT until S5 moves it
  (D1); `IdentityQuestions.Known` has no `names` (D2); `FirstSetupDialog` keeps its own thread (D3); an axis pushed
  when capture opens takes that as its rest, accepted (D4, bind-editor.md *Axis directions*). **Fixed:**
  `DeviceEdits.retargetAll` holds the identity answers' loop, with a test; `Restore.Prepared` refuses a reading and
  files that disagree; `// WHY:` at two broad catches; `FlowSteps` documents every parameter (`whenNoNextStep`) and
  closes with `button.close`; stale javadoc and blank lines. **Not done:** finding 5 was wrong - `readIdentity`'s
  `throws IOException` is live (the element reads parse), so it is documented instead. **Left:** finding 11 (where
  `CONTROL_SCHEMES` lives) and 13 (renaming `SavedDeviceLabels` - S5 touches its callers). **S5 needs:** load
  `SavedDeviceLabels` off the EDT when the Bind Editor moves onto the edits.

- **2026-10-28 — S4b done: the identity question on the bar, and the old tables gone.** **Decided (Alan):**
  `edit_history_retention` is dropped in S6, with `StartPresetApply`, which still reads it (file-manager.md *Settings*);
  the identity question counts the edits' binds files (alias-designer.md *On the working copy — 2026-10-28*). **Found,
  and kept to the spec:** bind-editor.md says the capture tab writes the working copy's name, so `SavedDeviceLabels`
  takes names from the working copy and labels from the edits (recorded there). **Built:** migration `12013`;
  `SavedDeviceLabels` moved to `bindforge.storage`; `Provenance` and `ProvenanceRule` removed with the installs table;
  device-provenance.md rewritten. **Found:** a migration ending in comments breaks the database for every test - the
  shared helper `MigrationScripts.run` now runs pieces as `DatabaseMigrator` does, and `MigrationLayoutTest` refuses it.
  **Left:** two comments in `MissingControllers` still say "the master". **S5 needs:** the capture tab's name from
  `snapshot.current()` once the Bind Editor writes through the edits.

- **2026-10-27 — S4R done: Krondor's storage review.** All 18 findings were still true; 16 fixed, 14 and 15 dropped
  (Krondor: BindForge only, by package). **Decided (Alan):** a GamePad's sticks are not captured until testing item 27
  (alias-designer.md *A gamepad is not a joystick*); axis capture measures from rest (bind-editor.md *Axis directions*);
  first-time startup writes only the installations it read, one meaning of "reachable", a flag that cannot be set is
  reported with the write (storage.md *Settled from Krondor's review*); `BackupSources.gather` renamed `rescan`.
  **Built:** `WritePath.write(files, places, backUp)` for first-time startup and RESTORE (APPLY still finds its own);
  `Written.setUpAskedAgain` replaces `Restore.Restored`; `Restore.Prepared` carries its places and the identical set,
  worked out off the EDT; `FileNames`, `FlowSteps`, `Backups.TAKEN_FORMAT` and `isBindsFile`; the merge off the EDT.
  **Left:** `BindsDeviceReferences` (devicefiles) keeps its own `.binds` suffix; no test for a `.buttonMap` whose flag
  cannot be read; `bindings.firstTimeStartup.result.written` still opens *"First-time startup is complete"* above the
  ask-again line when the flag fails. **S4b needs:** nothing new from this slice.
  **Rebased onto Krondor's `e08d5cd46`** (S4a second review follow-up), which fixed findings 1 and 7 his own way: kept
  his one write at a time (`storedWriter` runs `WritePath.write(files, places, backUp)` inside `Edits.whileWriting`),
  the write-running and stopped-part-way messages and his `firstTimeStartup.result.setUpAgain` string; his
  `FirstTimeStartup.Completed` folded into `Written.setUpAskedAgain`; S4R's `NOT_REMOVED_FLAG_UNREADABLE` kept.

- **2026-10-27 — S4a review follow-up** (`code-integrity-review.md`, all 15 findings). **Decided (Alan):** the report
  promises the next APPLY only for the bindings folder - a missed or read-only-kept installation is left as it is
  until the check; the differs line gives no instruction; by value ignores an element's other children - all in
  storage.md *How the files are written* and rule 8; CLEAR and RESET LABELS name no installations (alias-designer.md).
  **Fixed:** default labels worked out as APPLY runs (`Apply.DefaultLabels`); `Edits.snapshot()`, `EditFromApplied`
  for the undo, DISCARD refused during a write; a working copy that cannot be parsed stops APPLY before the backup;
  `DeviceFiles` replaces the `-1` id; a neutral "stopped part way" message for a `RuntimeException`; unused
  `LiveCopies` / `WritePath` overloads removed; tests for the report text, `copyIn` and a missing installation.
  **S4b needs:** read the edits through `Edits.snapshot()` - `current()` and `applied()` are gone.

- **2026-10-26 — S4a done: the APPLY / DISCARD bar, and the Device Editor and onboarding on it.** **Decided (Alan):** S4
  split into S4a and S4b; onboarding's FINISH moved into S4a, since its writes went through the push S4a removed; APPLY
  leaves alone an installation whose device files differ by value, reported, until S9 (storage.md *How the files are
  written*); edits lost on close until S7 (*Known issues*). **Decided while building, confirmed by Alan 2026-10-27** (in
  alias-designer.md *Built on the working copy*): a shadowed Frontier element covering several controllers keeps its
  `.buttonMap`, and going back to its name is refused while labels of the user's are under it. **Found:** an
  installation that missed a write can no longer be caught up by APPLY - accepted by Alan 2026-10-27, S9 must close it (*Known issues*). **Built:** see the
  *Built* table; `FirstTimeStartupFlow.labelsFor` moved from the reader, `FirstSetupPlan.Severity` from the removed
  divergence list. **S4b needs:** move `IdentityQuestions`' inputs and the retarget onto `Edits` (a VID/PID change is
  `DeviceMappingsMerge.merge`); point `SavedDeviceLabels`, `CaptureControllers` and `InputSettingsPanel` at
  `Edits.stored().current()`; then drop the tables, `missed_write` and `edit_history_retention` in one migration, with
  `BindForgeInstallationsManager.setMissedWrite` and `DraftChanges`.

- **2026-10-25 — S3 second review follow-up** (`code-integrity-review.md`, Alan: all four recommendations).
  **Decided:** a read-only `.buttonMap` the set lacks is kept on every system, asked before deleting, and worded from a
  bundle key (`WriteReport.Place.keptReadOnly`, `bindings.firstTimeStartup.result.keptReadOnly`); a protected Linux file
  another user owns is accepted as not written, and reported - both in storage.md rule 10. **Fixed:** a zip entry
  ending in a space or dot is skipped; the read-only tests check through `Files.isWritable`, not `ReadOnlyFlag`;
  `FirstTimeStartupFlow` logs a `RuntimeException` as an error and says it stopped unexpectedly. **S4 needs:** APPLY's
  report shows `keptReadOnly` through `FirstTimeStartupFlow.placeFailures` like the others. **Left (Alan):**
  IntelliJ's "double negation" warning on `WriteReport.Place`'s storefront check (from S2a) - correct as it is; tidy it
  into two named booleans when S4 is in that file.
- **2026-10-25 — S3 review follow-up** (`code-integrity-review.md`, Alan: all four recommendations). **Decided:** with
  the working copy unreadable and no backup, first-time startup is offered from the live files (Set Up Again); on Linux
  the owner's write permission is kept like Windows' read-only flag, Windows decided by `os.name` - recorded in storage.md
  rule 10 and file-manager.md; S3's dates moved to 2026-10-25, Alan's day. **Fixed:** the confirmation's "not found" is
  decided in `Restore.prepare` (`Target`), off the EDT; a merge's moved bindings are reported; odd names in a hand-made
  zip are skipped (`plain` refuses Windows' reserved characters, folder numbers bounded, an unparseable recorded folder
  falls back); a bug in the read step is logged as an error and not blamed on the backup; a flag that cannot be set is
  told (`Restore.Restored`); one `Written` record for first-time startup and RESTORE; `Restore.write` takes the
  `BackupContents`; a read-only `.buttonMap` the set lacks is reported with what to do; tests for the size guard, odd
  names, the flag and Linux. **S4 needs:** APPLY returns `Written` too.
- **2026-10-25 — S3 done: RESTORE.** **Decided (Alan, all six recommendations):** every live write keeps a read-only
  file read-only (clear, write, set again; backups do not record it); a backup missing a half is refused; RESTORE before
  first-time startup marks it complete; one confirmation (the reconcile's summary when one is needed); the working copy
  is read at every BindForge tab click once set up, the newest backup offered when it cannot be; Browse for Backup File
  takes only a zip with `backup-info.txt` and files in a backup's layout. Recorded in storage.md rule 10 and
  file-manager.md (*Restore*, *Backup Format*, *Browse for Backup File*). Also there: no bindings folder refuses before
  the backup, and RESTORE publishes no `BindingsUpdatedEvent`. **Built:** `Restore`, `BackupContents`, `ReadOnlyFlag`,
  `RestoreFlow`; `FirstSetupDialog.Wording`; `FirstTimeStartup.storedFlag()`; `FirstTimeStartupFlow.placeFailures`.
  **Found / left:** a read-only `.buttonMap` the set lacks is not removed, and is reported; a live `.binds` the backup
  lacks stays in the bindings folder but not in the working copy - **S8's check must not keep asking about it**; the
  Bind Editor keeps its own copy and shows the old file until S5; the confirmation's installation list and the write's
  are found separately, a moment apart. **S4 needs:** RESTORE must throw away the edits on screen once the shared edit
  model exists (its confirmation already says so); APPLY's write follows `Restore`'s pattern - `WritePath` with
  `Before.APPLY`.

- **2026-10-25 — S2b second review** (`code-integrity-review.md`). **Decided (Alan):** RESTORE is the one exception to
  "never overwritten automatically" - it replaces an installation the backup does not hold, without a reconcile
  (storage.md rules 1 and 10, the S3 row). **Fixed:** the unreachable list uses `place.installation`. **Left:** the
  migration-test `run` helper is in four tests - share it at the next migration test.
- **2026-10-25 — S2b review follow-up** (`code-integrity-review.md`, Alan: all seven). **Fixed:** every known
  installation unreachable stops first-time startup, naming them (`NoInstallationReachableException`); the backup folder
  check refuses a removed installation's folder too (`findAllIncludingRemoved`); a test for migration `12012`; stale
  lines in file-manager.md, alias-designer.md and the `Backups` javadoc. **Decided (Alan):** an installation added
  later, or one unreachable at first setup that comes back, is left alone if it matches the working copy and reconciled
  as in first setup if it differs - never overwritten automatically (storage.md rule 1, file-manager.md, alias-designer.md
  *An install that appears later*). **S4 and S9 need:** APPLY must not write an installation that differs until it is
  reconciled. **Left (pre-existing):** Add Missing and Remove do not catch a database failure; a removed installation is
  matched by exact path string; one date pattern defined twice.
- **2026-10-25 — S2b done: first-time startup.** **Decided (Alan):** the flag is `bindforge_activation.activated_at`,
  cleared once by migration `12012` so testers set up again; offered at every tab click; a new player with no bindings
  folder is told and not taken through it; startup `.buttonMap` generation removed now (storage.md *First-time
  startup*). A removed installation is neither backed up nor written, and Remove says so; Add missing checks for
  `EliteDangerous64.exe` (file-manager.md *Game Install Locations*). The merged files - one `DeviceMappings.xml` from
  one installation's file with each kept element copied whole, `.buttonMap` bytes kept when unchanged, case-only
  conflicts rewritten to the winner's spelling, no "use one" for an installation without `DeviceMappings.xml` - are in
  alias-designer.md *How the reconciled files are made*. **Changed shared code:** `DeviceReferenceRewrite` allows a
  case-only change. **Found / left:** a merge drops orphan `.buttonMap`s, so the write removes them from the
  installations (they are in the first setup backup); a losing name's labels are not carried to the winner; an
  installation detected only at write time gets the working copy unreconciled; `DeviceApply.moveRenamedReferences`
  keeps a `handedOver` argument that is now always empty (S4 removes `DeviceApply`). **S3 needs:** RESTORE writes through
  `WritePath` like `FirstTimeStartup.write`; a backup whose installations differ reuses `FirstSetupReading.of` and
  `FirstSetupDialog` with the backup's files as `InstallationFiles`.

- **2026-10-24 — S2a review follow-up** (`code-integrity-review.md`, Alan: all six). **Fixed:** `WritePath` finds the
  installations once and hands that list to the backup (`BackUpFirst.take(BackupSources)`, `Backups.backUpBeforeChange(Before,
  BackupSources)` and `backUpEverything(Kind, BackupSources)`), and runs one write at a time; `WriteReport.Place` has a `Kind` and
  a storefront instead of an English label, and its failure lines are documented as log and support text;
  `BackupDestination` says "using the default" only where it falls back. **Recorded** in storage.md *How the files are
  written*: one list and one write at a time; a working copy with no `DeviceMappings.xml` leaves each installation's alone.
  **Dates:** S2a's decisions moved to 2026-10-24, Alan's day. **S2b needs:** the backup lambda is
  `sources -> Backups.stored().backUpEverything(Kind.FIRST_SETUP, sources)`; name a place from `Place.kind()` through a bundle key.

- **2026-10-24 — S2a done: the working copy store and the write path.** **Decided (Alan):** S2 split into S2a and S2b; no
  interim compatibility — first setup stops filling the database master in S2b, and the Alias Designer is broken until S4
  (brief, *Settled* and *Known issues*); the launch-time `.binds` copy goes in S5; Game Install Locations loses Rescan and
  Relocate and a removed installation stays removed (file-manager.md). How the files are written — one-step folder swap,
  byte match, the bindings folder never created, case spellings — is in storage.md *How the files are written*. **Built:**
  `WorkingCopy`, `LiveCopies`, `WritePath`, `ConfigurationFiles`, `WriteReport`, `FrontierStockButtonMaps.shipsMapFor`.
  **S2b needs:** build the first setup set as a `ConfigurationFiles` and call `WritePath.stored().write(files, …)` with the first
  setup backup (see the review follow-up above); report from `WriteReport`. `WorkingCopy.exists()` is not the
  flag — the flag is stored separately and Run first-time startup again clears only it. A `NothingToBackUpException`
  from the backup is S2b's to word.

- **2026-10-24 — S1b review follow-up** (`code-integrity-review.md`). **Fixed:** settings saves run one at a time on one
  thread and `BindForgeSettingsManager.update` is synchronized; Browse… checks a folder with `BackupDestination.check` and
  refuses one that would not be used, saying why (recorded in file-manager.md *Settings*); stale lines in file-manager.md and
  the `Backups` javadoc; tests for the Taken labels, the path guard, a damaged zip, `HudStepper` and `check`. **Dates:** S1b's
  decisions moved to 2026-10-24, Alan's day; migration `12011`'s comments keep 2026-10-25 (never edit an applied migration).
- **2026-10-24 — S1b done: four kinds of backup, every one a zip.** **Decided (Alan):** the zip built in memory and written
  with `AtomicFiles`; list columns Created, Taken, Status (Kept and the mockup's Files dropped); migration `12011` does not
  carry the old retention over; Backups go to has Browse… and Use Default and shows the folder actually used. Retention
  still runs when today's daily backup is already there. All recorded in file-manager.md (*Daily backup*, *How the backup
  folder is laid out*, *Settings*). **Found:** `MigrationLayoutTest` refuses a semicolon in a migration comment; `HudStepper`
  had no change event and now has one. **S2 needs:** `Backups.stored().backUpEverything(Kind.FIRST_SETUP)` for first setup and
  `backUpBeforeChange(Before.APPLY)` before every write — nothing calls before-changes yet.
- **2026-10-24 — S1b written: four kinds of backup** (Alan, after Krondor's review of S1). **Decided:** every backup a
  zip; daily (first start each day, kept by age), before changes (every write, kept by count), user (Backup Now, kept
  until deleted, Delete for these only), first setup (forever); retention settings on Player Backups; the brief names no
  migration number. Recorded in principles.md *Backup*, storage.md rules 11–12 and file-manager.md (*Four kinds of
  backup*, *Retention*, *How the backup folder is laid out*, *Backup Format*, *Settings*). **Fixed here:** the startup
  log line says Alan's new-player words; `backup-info.txt` uses the platform's separator; `SetupCheck`'s leftover
  constructor removed; the S6 row no longer links the gitignored P3 review. **Left:** `awaitTaken()` has no timeout (S4
  removes its caller); a read-only `.binds` loses its flag inside a zip — **S3 decides** whether RESTORE puts it back.
  **S1b needs:** the existing `Backups`, `StartupBackup` and Player Backups code reshaped, not replaced; S2 calls the
  before-changes kind.
- **2026-10-24 — a new player is told to play the training first.** The one exception to the silent backup (Alan):
  `SetupCheck` says `speech.setup.noBindingsYet`, in Alan's words, when the game's default bindings folder is missing
  or holds no `StartPreset` — it used to tell them to set the folder. Recorded in storage.md rule 12 and
  file-manager.md. Backup Now's "Nothing to Back Up Yet" dialog shows the same string since 2026-10-24.
- **2026-10-24 — S1 review follow-up** ([code-integrity-review-storage-s1.md](../../code-integrity-review-storage-s1.md)).
  Fixed: a prune renames a backup to `.taking-` before deleting it, so a delete that stops part way is finished by the
  next prune; one prune resolves the folder once; the backup runs detection once and hands it to
  `InstallationRegistry.rescan(List)`, which counts as the run's startup detection; "nothing to back up" is a warning
  with the advice to play past the tutorials (recorded in file-manager.md); Player Backups shows only the newest list
  refresh; stale notes in file-manager.md, alias-designer.md and foundation.md. **Left (Alan):** finding 8, a test
  that `ButtonMapStartup` waits for the startup backup — S4 removes startup `.buttonMap` generation.
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
