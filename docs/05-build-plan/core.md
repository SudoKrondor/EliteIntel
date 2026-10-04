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
| C1 | **Device-file write** — merge the master's elements into each installation's `DeviceMappings.xml` and `.buttonMap`; keep unique prior content; atomic replace; prune; every installation or none | **next** |
| C2 | **Device draft** — where unapplied device edits live; SAVE writes the draft, APPLY promotes it | ready — design question first |
| C3 | **Startup check for device files** — each installation against the master: unchanged, wiped (matches the stock reference), or edited; adopt or revert | ready after doc conflict 1 |
| C4 | **`.binds` and `StartPreset`** — whole-file master, Edit History on apply, the version-bump merge | blocked — `bindforge.io` |
| C5 | **Change detection for the install folders** | blocked — `elite.intel.io`; doc conflict 2 |
| C6 | **Unsaved work on exit** — Save or Discard, never Apply | ready |
| C7 | **First-time startup** | after Preset Editor can read all four `StartPreset` lines |

### C1 in detail — designed 2026-10-03, decisions 2026-10-04, sources table not yet approved

**Decided with Alan, 2026-10-04** — to be recorded in the specs when C1 is built:

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

- **2026-10-03 — brief written.** Sessions before this one built the master store, the stock reference, the GUID
  derivation, the device list and the installation scan; this brief records their state. C1's design came out of
  the session that wrote this.
