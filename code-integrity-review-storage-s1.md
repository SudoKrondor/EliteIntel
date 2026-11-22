# Code Integrity Review: BindForge Storage S1 (one backup system)

**Date:** 2026-10-24
**Branch:** `V1.2-BindForge`, level with `origin/V1.2-BindForge`, working tree clean.
**Change set:** the four commits ahead of `origin/V1.2` (`git log origin/V1.2..HEAD --no-merges`):

| Commit | Author | What |
|---|---|---|
| `565c4d298` | Alan Tonn | BindForge simplification review (report file only) |
| `6eb29435c` | Alan Tonn | Specs rewritten to back up, edit, apply |
| `df51d34c9` | Alan Tonn | Build plan: the Storage brief, S1 to S12 |
| `c32764174` | Alan Tonn | **Storage S1: one backup system** (all the code) |

No Krondor commits are in the set. The code review covers `c32764174` in full. The two spec commits were checked
where S1 implements them (principles.md, storage.md rules 10 to 12, file-manager.md, the Storage brief), not end to end.

**Why a separate file:** `code-integrity-review.md` holds the Preset Editor P3 review, untracked, and the Storage
brief's S6 row links to its findings 2 and 5. Overwriting it would lose them.

## Overview

S1 replaces the old `PlayerBackupService` folder sweep with a new package, `elite.intel.bindforge.storage`:

- `Backups` takes a backup (every `.binds`, `StartPreset.N.start`, and each installation's `DeviceMappings.xml` and
  `.buttonMap`s) into a `.taking-` folder, writes `backup-info.txt`, and renames the folder into place. It lists
  backups from the folder alone, and prunes by the stamp in the name, keeping first setup's.
- `BackupSources` joins detection to the stored installation list; `BackupDestination` resolves the chosen folder or
  falls back to the default (unreadable, unwritable, or inside a game folder).
- `StartupBackup` runs once from `App.main` on its own thread, prunes only after success, and lets `ButtonMapStartup`
  wait for it.
- Player Backups shows the new list (Created, Status, Kept), loses Delete, and moves to `HudConfirmDialog`.
- Migration `12010` drops `auto_backup_on_launch`; strings updated in all seven bundles.

This is careful work. The take-then-rename keeps half-taken backups out of the list, one bad file never stops the
rest, every broad catch is a documented boundary with a `// WHY:`, the seams (`Sources`, `Root`, `Clock`) make the
tests real-filesystem tests without mocks, the destination resolver is still the one place that decision is made, and
the removed tests' responsibilities all moved to `BackupsTest` rather than being dropped. The bundles are correct,
including the doubled apostrophe in French (`n''ont`), which takes an argument. No dependency added, no
`@SuppressWarnings`, no applied migration edited, `ai.hands` untouched, and the next free migration number was used.

## Findings introduced by this change set

### 1. `Backups.java:368` - on Linux, a failed prune can leave a backup that is never listed or pruned again
`robustness` `logic`

`delete()` walks the folder in reverse path order. On Windows paths compare case-insensitively, so `backup-info.txt`
goes last. On Linux they compare case-sensitively: `Bindings` and `Installations` (capitals) sort before
`backup-info.txt`, so in reverse order **`backup-info.txt` is deleted first**. If any later file cannot be deleted,
the folder is left without its info file. It is then no longer a backup (`read()` returns `null`), so it is never
listed and never pruned. The log line says *"tried again at the next start"*, which is then false.

**Simplest fix:** rename the folder to `.taking-<name>` (atomic, like the take) before deleting it. A failed delete
then leaves a leftover that the existing sweep at `Backups.java:213` already removes on the next start. A test: a
backup whose deletion fails part way is still deleted at the next prune.

### 2. `BackupSources.java:212` - every backup runs installation detection twice, and startup runs it a third time
`performance` `duplication`

`gather()` calls `InstallationRegistry.rescan()`, which runs `provider.findInstallations()`, then calls
`provider.findInstallations()` again for the bindings folders. Detection reads the registry, VDF and manifest files
(`InstallationRegistry.java:128` Javadoc). Because it uses `rescan()` and not `currentWithStartupScan()`, it also
does not set the run-once flag, so the first screen refresh and `CatchUpAnnouncement` detect again. That is three
detections at startup where the registry was built to run one.

**Choices:**
- **A (recommended):** `gather()` calls `findInstallations()` once, and passes that list to a registry method that
  records it (`rescan(List<GameInstallation>)`, with `rescan()` delegating to it). One detection per backup.
- **B:** use `currentWithStartupScan()` for the rows and keep the second `findInstallations()`. Startup drops to two
  detections; Backup Now still does two.
- **C:** leave it. It costs time on a slow or sleeping drive, never correctness.

### 3. `docs/02-features/bindforge/file-manager.md:275` - the "What exists in code today" table now contradicts the code and the spec
`docs` `consistency`

The table is dated 2026-10-21. After S1 its first three rows are wrong: Backup Now now copies every file from every
installation, the startup backup exists, and retention deletes. Row 1's right-hand column also still says *"as a
date-stamped zip (rules 11-12)"*, which contradicts rule 12 as amended in this same commit (a folder, not a zip).

**Simplest fix:** mark those three rows *Built 2026-10-24 (Storage S1)*, and change "zip" to "folder" in row 1.

### 4. `docs/05-build-plan/alias-designer.md:87-91` - two notes name code S1 removed or a slice that did not do the work
`docs`

- *"`DeviceFilesSnapshot` writes beside `PlayerBackupService`'s folders, so `listBackups()` lists it"*:
  `listBackups()` no longer exists, and the new list leaves that folder out (no `backup-info.txt`).
- *"The first-setup backup ignores a chosen backup destination ... Goes in Storage S1."* S1 did not change
  `DeviceFilesSnapshot`; the Storage brief has S2 removing it. The note now points at a finished slice.

**Simplest fix:** rewrite the first as resolved by S1 (not listed), and retarget the second to S2.

### 5. `StartupBackup.java:316` - a user with nothing to back up gets an ERROR in the log at every start
`error-handling` `clarity`

When no file exists at all (game not installed yet, or the bindings folder never created),
`NothingToBackUpException` is thrown and logged at `error` on every start. That is an expected state for some users,
and an error line in every support bundle sends whoever reads it looking for a fault.

**Choices:**
- **A (recommended):** catch `NothingToBackUpException` before the broad catch and log it at `warn`, naming what was
  not found. Real failures stay at `error`.
- **B:** leave it. It is honest, just loud.

### 6. `Backups.java:206` - `prune()` resolves the backup folder twice
`clarity` `robustness`

`prune()` calls `root.get()`, then `list()` calls `root.get()` again. With a chosen destination, each call re-reads the
setting and re-runs `BackupDestination`'s checks (which also create the folder). If the answer changes between the two
(the setting changed, or the folder went unwritable), `list()` deletes in one folder while the leftover sweep runs in
another.

**Simplest fix:** a private `list(Path backupRoot)` that both `list()` and `prune()` call, so one resolution serves
the whole prune.

### 7. `BindingManagementPanel.java:288` - two quick refreshes can show an out-of-date list
`logic` (low)

`refreshBackups()` starts a new thread each time and shows whichever result arrives last. Backup Now finishing while
`initData()`'s refresh is still running can put the older listing on screen last, without the backup just taken.

**Simplest fix:** a sequence number that `showBackups` checks, ignoring a result older than the newest request.

### 8. `ButtonMapStartup.java:63` - "nothing writes before the startup backup" is held only by an untested lambda
`testing` (low)

The rule is the composition in the no-argument constructor
(`StartupBackup.getInstance().awaitTaken() && isActivated()`). The tests inject their own `BooleanSupplier`, so
nothing fails if a later edit drops the `awaitTaken()` half. Principle 3 rests on it.

**Choices:**
- **A (recommended):** a package-private constructor that takes the `StartupBackup`, and one test that a failed
  startup backup stops generation.
- **B:** leave it until S4, which removes startup `.buttonMap` generation altogether.

## Pre-existing related issues (not introduced here)

- **`db-migration/12000__bindforge_settings.sql:14`** names `PlayerBackupService.resolvePlayerBackupsDir()`, which S1
  removed. It is an applied migration, so it stays as it is. `file-manager.md` now names `BackupDestination`.
  Nothing to do.
- **`docs/05-build-plan/foundation.md:116-125`** names `resolvePlayerBackupsDir` and says a player backup copies one
  preset file. Foundation is a finished brief and this is its history. A one-line *"Superseded by Storage S1"* would
  keep a reader from following it.

## Checked and fine

- Atomic rename into place; a `.taking-` folder is never listed; collisions within one second get `-2`, `-3`.
- One lock across take and prune, so the startup backup and Backup Now cannot interleave, and the sweep never removes
  a backup being taken.
- Retention refuses below one day at the point of harm, and runs only after a successful startup backup (recorded in
  file-manager.md).
- `BackupDestination` falls back on an unreadable setting, an unmakeable or unwritable folder, or one inside an
  installation or the bindings folder; each case is logged and tested. `Path.startsWith` is case-insensitive on
  Windows, so drive-letter case cannot slip past.
- Linux: an installation's own bindings folder is copied when it is not the main one (input environment section 7).
- Migration `12010`: `DROP COLUMN` has precedent (`01018`), and the driver is sqlite-jdbc 3.51; the migration test
  checks the other settings survive.
- No EDT work: list, take and restore all run off the EDT; `awaitTaken()` runs on `ButtonMapStartup`'s worker, not
  the device bus.
- `CatchUpAnnouncement` and `DeviceFilesMonitor` only read game files at startup, so `ButtonMapStartup` was the one
  writer that needed to wait.
