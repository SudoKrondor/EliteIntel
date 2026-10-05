# Core — the machinery every tab writes through

**Not a tab.** The master and the draft, Apply, Edit History filling, the startup check, change detection,
unsaved work on exit and first-time startup. Every tab edits a draft and every tab applies through this — built
once here so no tab builds its own.

**Built only as far as the next tab needs.** C1 exists because Alias Designer's first setup cannot run without it.

## Spec

- [overview.md](../02-features/bindforge/overview.md), from **Managed File Domains** through **Unsaved Work on
  Exit**. Read whole sections, not the line a search found.
- [file-manager.md — Edit History](../02-features/bindforge/file-manager.md#edit-history), including the three
  rulings of 2026-10-03: files only, where the folder lives, what gets kept.
- [elite-intel-platform-map.md — the shared write path](../01-host-integration/elite-intel-platform-map.md#the-shared-write-path-is-eliteintelio--built-2026-10-03).

## Built

| What | Where |
|---|---|
| Safe replace, timestamped copies, pruning | `elite.intel.io.AtomicFiles`, `elite.intel.io.TimestampedBackups` (`create`, `prune`) — Krondor's, tested. **Never write a game file any other way.** |
| Device master store | migration `12002`; `db.managers.BindForgeDeviceMasterManager` (devices and their labels), `BindForgeDeviceInstallsManager` (`FoundDevice`, transactional `replaceForInstall`) |
| What each installation holds | `bindforge.devicefiles.InstallationDeviceScanner`, run on every Alias Designer refresh |
| Who owns an entry | `bindforge.devicefiles.Provenance`, `ProvenanceRule` — a read only ever yields `FRONTIER` or `UNKNOWN` |
| Installations | migration `12001`; `bindforge.install.InstallationRegistry` |
| Settings | migration `12000`; `BindForgeSettingsManager` — `getEditHistoryRetention()` (1–30, default 10), backup destination, backup retention days |
| Reading device files | `bindforge.devicefiles.DeviceMappingsParser` (path or stream, keeps `<Alternative>`), `FrontierStockDevices` (`ships`, `covering`), `ButtonMapAudit` |
| Device-file write (C1) | `bindforge.devicefiles.DeviceFilesPush` — Apply for `DeviceMappings.xml` and `.buttonMap`, per installation, with Edit History in `elite-intel/bindings/history/<installId>/`; `DeviceMappingsMerge`, `ButtonMapWriter`, `DeviceFileXml` (the layout). **No caller yet** |
| Device draft and Apply (C2) | migrations `12003`, `12004`; `db.managers.BindForgeDeviceDraftManager` — `start()` copies the whole master in (a started draft may hold no edits, so `isStarted()` is not "changed"); `promote()` makes it the master and stores what it removed as pending removals (`BindForgeDeviceMasterManager.pendingRemovals()`, `clearPendingRemoval`); `revert(masterId)`, `discard()`. Master ids change on every promote. `bindforge.devicefiles.DeviceApply` — promote, then `DeviceFilesPush.push()`. **No caller yet** |
| `.binds` draft and apply | `bindforge.io.BindingsWorkingCopyRepository` (drafts in `elite-intel/bindings/`), `BindingsApplyService`, `BindingsMonitor` |

**The `.binds` master is only a hash today.** `BindingsWorkingCopyRepository` records a baseline fingerprint
(`recordGameFileBaseline`, `gameFileMatchesBaseline`, `markApplied`). The spec wants a **whole-file master**.
Growing it touches `bindforge.io` — see C4.

## Settled — and where it is recorded

- **Edit History is files, no table; it lives under Elite-Intel's data folder; it keeps what is unique.**
  [file-manager.md](../02-features/bindforge/file-manager.md#edit-history)
- **The game parses `DeviceMappings.xml` as XML and leaves the layout alone.** Formatting is ours to choose;
  content is not — preserve every child element. [Domain doc §1.2c and §1.3](../02-features/bindforge/domain-knowledge/EliteDangerous-DeviceMappings-ButtonMap.md#12c-the-game-parses-it-as-xml-and-does-not-reformat-it--measured-2026-10-03)
- **Shared code changes on `V1.1-Release` and merges up.** [CLAUDE.md](../../CLAUDE.md), and the platform map above.
- **A read never claims an entry is the user's.** `ProvenanceRule`'s javadoc;
  [device-provenance.md](../03-data-models/device-provenance.md)
- **Never push a master over a game file of a higher version.** [overview.md](../02-features/bindforge/overview.md#new-slots-are-the-only-thing-an-update-brings--settled-2026-09-24)
- **One write per apply, and never publish `BindingsUpdatedEvent`** — the watcher does.
  [overview.md](../02-features/bindforge/overview.md#bindforge-must-not-publish-bindingsupdatedevent)

## Known doc conflicts — settle with Alan when a slice reaches them

1. **[Freshness Checks](../02-features/bindforge/overview.md#freshness-checks) and [Destructive Change
   Detection](../02-features/bindforge/overview.md#destructive-change-detection) still describe the old model**,
   where the live file is truth and the working copy refreshes from it. The sections above them say that was
   inverted on 2026-09-24 — the master is truth, and the startup check decides adopt or revert. **Before C3.**
2. **[External change detection stays in one place](../02-features/bindforge/overview.md#external-change-detection-stays-in-one-place)
   says to extend `BindingsMonitor`'s `WatchService` to the install folders.** Krondor, 2026-10-03: files another
   process writes need polling, or a watch plus a periodic re-check, in `elite.intel.io` — see "Still to come:
   watching" in the platform map. `DeviceMappings.xml` is written by the game. **Before C5.**

## Boundaries

- **New BindForge code** goes under `elite.intel.bindforge` — `devicefiles` (C1 writes device files), `devices`, `install`, or a new package if Apply
  wants one. Decide in the sources table.
- **`bindforge.io`, `bindforge.rules`, `elite.intel.io`, `AppPaths`** are shared with V1.1. Slices that need them
  are blocked until the question in [the README](README.md#one-question-to-settle-with-krondor-before-sections-3-5-and-6) is answered.
- **`ai.hands`** — never.

## Slices

| # | Slice | Status |
|---|---|---|
| C1 | **Device-file write** — merge the master's elements into each installation's `DeviceMappings.xml` and `.buttonMap`; keep unique prior content; atomic replace; prune; per installation, reported | **done 2026-10-04** |
| C2 | **Device draft** — where unapplied device edits live; SAVE writes the draft, APPLY promotes it | **done 2026-10-04** |
| C3 | **Startup check for device files** — each installation against the master: unchanged, wiped (matches the stock reference), or edited; adopt or revert | **next** — settle doc conflict 1 with Alan first |
| C4 | **`.binds` and `StartPreset`** — whole-file master, Edit History on apply, the version-bump merge | blocked — `bindforge.io` |
| C5 | **Change detection for the install folders** | blocked — `elite.intel.io`; doc conflict 2 |
| C6 | **Unsaved work on exit** — Save or Discard, never Apply | ready |
| C7 | **First-time startup** | after Preset Editor can read all four `StartPreset` lines |

### C1 in detail — designed 2026-10-03, built 2026-10-04

**Decided with Alan, 2026-10-04** — recorded in [alias-designer.md, Apply](../02-features/bindforge/alias-designer.md#actions-and-what-each-one-reaches) and [file-manager.md, Edit History](../02-features/bindforge/file-manager.md#edit-history):

1. **Partial success, reported per installation.** A missing folder or a failed write skips that
   installation and names the reason (*"updated 2 of 3 — Epic: folder not found"*). **Within one
   installation** its `DeviceMappings.xml` and `.buttonMap` files change together or not at all. *The rename
   transaction needs all-or-none across installations, and builds that itself.*
2. **No `DeviceMappings.xml` → seed it from the stock resource** in the jar, then merge, and say so in the report.
3. **A `.buttonMap` is written whole from the master's labels**; the old file goes to Edit History. A device
   with no labels is skipped. Hand edits made after first setup are C3's to catch, not C1's.
4. **No removals.** C1 adds and updates only. CLEAR, rename (`previous_name`) and RESET LABELS each need an
   explicit removal list from a later slice.
5. **User elements go at the top of `<Root>`**, because [the first VID/PID match wins](../02-features/bindforge/domain-knowledge/EliteDangerous-DeviceMappings-ButtonMap.md#12d-two-entries-for-one-vidpid-the-first-in-the-file-wins--measured-2026-10-03).
   **A master element already in the file moves to the top** (Alan, 2026-10-04) — left below Frontier's entry
   for the same VID/PID, it would never win.
6. **Built-ins are no longer special to C1.** A Frontier entry the user renamed or shadowed is a master row like
   any other ([reversed 2026-10-04](../02-features/bindforge/alias-designer.md#built-in-controllers-in-my-devices--settled-2026-09-13)).
   A Frontier element **not** in the master is never touched.

- **Merge, never regenerate.** The master holds only the user's elements. So each
  installation's existing file is read, and everything that is not the user's is kept untouched — Frontier's
  entries, comments, `<SupportsIcons>`, any child nobody has documented. The user's elements are replaced or
  added from the master. Reconcile [per element, never per file](../02-features/bindforge/alias-designer.md#reconciliation-is-per-device-element-never-per-file).
  *The first attempt at the formatting test emitted only `PID`, `VID` and `Alternative`, and silently dropped all
  five `<SupportsIcons>`. The file looked right and the content was gone.*
- **History by installation.** Two installations' `DeviceMappings.xml` share a name, so kept copies are keyed by
  installation id, which survives a relocate. `AppPaths.getAppDataBase()` is private; `elite-intel/bindings/`
  (`AppPaths.getBindingsWorkingDir()`) could host `history/<installId>/` without touching `AppPaths`, or Krondor
  adds a method. Decide in the sources table.
- **Keep what is unique.** Before replacing a file, keep its content — unless identical content was already kept
  in this operation.
- ~~**Every installation or none.**~~ Replaced by decision 1 above: per installation, with the reason reported.
  A rename still puts the shared `.binds` last and needs all-or-none across installations
  ([why](../02-features/bindforge/alias-designer.md#a-rename-spans-every-installation--added-2026-09-26)), which the
  rename slice builds on top of C1's per-installation unit.
- **Skip missing installations.** Writing to an absent folder is not a reachable state worth reporting as success.
- **Prune** each file's history to `getEditHistoryRetention()`.
- **Test against real shapes:** the stock file in `src/main/resources/bindforge/`, plus a small specimen with a
  comment inside `<GamePad>` and a `<SupportsIcons>` — the developer's own Steam file has both. Build the
  specimen; do not copy his file into the tests.

## Hand-off notes

*Newest first. Two or three lines each: what was decided and where it is recorded, what was found, what the
next slice needs.*

- **2026-10-04 — C2 review fixes** (`code-integrity-review.md`). Removed devices are now **stored** as pending
  removals (`12004`, Alan's choice over taking removal out of the draft), not only returned; renaming back to
  the original name leaves no rename pending; `exists()` became `isStarted()`; promote empties the draft before
  the master; revert refuses a name clash by name; the draft DAO uses the master DAO on the same handle instead
  of copying its queries. Recorded in alias-designer.md under Actions. **Left for later, from the review:** the
  master's own `rename` sets `previous_name` without `COALESCE`, so two renames lose the name on disk (A11);
  multi-statement master writes in A1/A6 should run in a transaction like the draft manager's.

- **2026-10-04 — C2 built.** The device draft is two tables beside the master (`12003`), not a working folder
  of files: the master is already rows, so Apply is a plain copy (Alan). **The draft goes into the master before
  the push**, so a failed write loses no edits. Recorded in [alias-designer.md, Actions](../02-features/bindforge/alias-designer.md#actions-and-what-each-one-reaches).
  **Found:** a `;` inside a migration comment breaks the runner, which splits on it — `MigrationLayoutTest`
  catches it. **Later slices owe C2:** *A10* picks which DISCARD the button means (unsaved edits, or the draft
  back to the master — the spec says both) and removes the files for each pending removal, clearing it after. *A6 onboarding and A1
  first setup* write the master outside Apply — if a draft exists then, the same write must land in it too, or
  the next Apply drops it. *A11* must clear `previous_name` in the draft as well as the master if a draft exists
  when a rename completes. **C3 needs:** doc conflict 1 settled first; the install strip and startup check
  compare against the master, never the draft.

- **2026-10-04 — C1 built.** `DeviceFilesPush.push()` pushes the stored master to every stored installation and
  returns a per-installation `Report`; nothing calls it yet. Decisions are in alias-designer.md under Apply, the
  history path in file-manager.md. **Found:** the first VID/PID match in the file wins, so user elements go at
  the top ([domain doc §1.2d](../02-features/bindforge/domain-knowledge/EliteDangerous-DeviceMappings-ButtonMap.md#12d-two-entries-for-one-vidpid-the-first-in-the-file-wins--measured-2026-10-03));
  the built-in name lock was reversed. **Also done this session:** the device-file classes moved to
  `bindforge.devicefiles` (`bindforge.devices` keeps the user's controllers), and `docs/build/` became
  `docs/05-build-plan/`, because `.gitignore`'s `build/` had kept it out of git.
- **What later slices owe C1.** *Removals:* CLEAR, rename (`previous_name`) and RESET LABELS each need an
  explicit removal list, since the master cannot tell a cleared device from one BindForge never owned.
  *Rename:* needs all-or-none across installations on top of C1's per-installation unit. *Hand edits after
  first setup:* overwritten by Apply until C3 exists, recoverable from Edit History only. *A8 (startup
  `.buttonMap`):* must never overwrite, so it needs a create-only path rather than `push()`.
- **C2 needs:** where the device draft lives. The master tables are what `push()` reads, so SAVE writing straight
  into them would make SAVE an unapplied Apply - the design question the slice table names.

- **2026-10-03 — brief written.** Sessions before this one built the master store, the stock reference, the GUID
  derivation, the device list and the installation scan; this brief records their state. C1's design came out of
  the session that wrote this.
