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
| Device draft and Apply (C2) | migrations `12003`, `12004`; `db.managers.BindForgeDeviceDraftManager` — `start()` copies the whole master in (a started draft may hold no edits, so `isStarted()` is not "changed"); `promote()` makes it the master and stores what it removed as pending removals (`BindForgeDeviceMasterManager.pendingRemovals()`, `clearPendingRemoval`); `revert(masterId)`, `discard()`. Master ids change on every promote. `bindforge.devicefiles.DeviceApply` — promote, then `DeviceFilesPush.push(Set)` with the pending removals `.binds` does not name; called by APPLY on the Alias Designer screen (A10). `DraftChanges` tells a draft with edits from one only started |
| Startup check, adopt, revert (C3) | `bindforge.devicefiles.DeviceFilesCheck` — each installation against the saved master: `UNCHANGED`, `WIPED`, `EDITED` (with element and label changes), `MISSING`, `UNREADABLE`; an empty master reports *not set up*. `DeviceFilesAdopt` — the installation into the master (and the draft, three-way) through `BindForgeDeviceDraftManager.adopt()`, one transaction. Revert is `DeviceFilesPush.push(installId)`. Helpers: `ButtonMapReader`, `DeviceMappingsParser.primaries()`. **No caller yet** |
| Device-file monitor (C5) | `elite.intel.io.FilePoller` (which files changed since the last look - stamp only, no thread); `bindforge.devicefiles.DeviceFilesMonitor` (polls every installation's `DeviceMappings.xml` and `.buttonMap`s every 2 s, started as `DEVICE_FILES_MONITOR` in `AppController`) publishes `ui.event.DeviceFilesChangedEvent`; `AliasDesignerPanel` refreshes on it; a request made mid-refresh is never dropped (`ui.screen.bindings.RefreshCoalescer`) |
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

1. ~~**Freshness Checks and Destructive Change Detection still describe the old model.**~~ **Settled 2026-10-05**
   (Alan): both rewritten for the master model, the old text struck through, in
   [overview.md](../02-features/bindforge/overview.md#freshness-checks).
2. ~~**External change detection said to extend `BindingsMonitor`'s `WatchService` to the install folders.**~~
   **Settled 2026-10-11** (Alan, C5): two watchers — `BindingsMonitor` keeps the bindings folder, device files are
   polled, per Krondor's 2026-10-03 ruling. [overview.md](../02-features/bindforge/overview.md#external-change-detection--two-watchers-settled-2026-10-11).

## Known issues

- **The `.binds` close prompt breaks [Unsaved Work on Exit](../02-features/bindforge/overview.md#unsaved-work-on-exit).**
  `BindingProfilePanel.promptCloseWithDraft()`, called from `AppView.windowClosing`, is the three-option dialog
  replaced on 2026-09-06: **Apply to Game** first, then Keep Draft and Discard, in a `JOptionPane`. It fires on an
  *unapplied* draft, which the spec does not treat as unsaved work. Shipped in V1.1, in files Krondor maintains on
  `V1.1-Release` — not fixed on this branch. Not settled: whether it goes, or becomes Keep Draft / Discard. Raise
  with Alan and Krondor; it does not wait on C6.

- **Generated `.buttonMap`s will show in the startup check** once it runs at startup. An element an installation
  added gets one from [A8](alias-designer.md#slices), and `DeviceFilesCheck.labelChanges` lists its 30–40 generic
  labels as `ADDED`, which adopting would take into the master. Settle with the slice that wires the check:
  whether a file byte-identical to a generated one counts as user content, and whether generation or the check
  runs first.

## Boundaries

- **New BindForge code** goes under `elite.intel.bindforge` — `devicefiles` (C1 writes device files), `devices`, `install`, or a new package if Apply
  wants one. Decide in the sources table.
- **`bindforge.io`, `bindforge.rules`, `elite.intel.io`, `AppPaths`** are shared with V1.1. Changes to them
  go on [the foundation branch](README.md#the-foundation-branch--how-a-v12-feature-changes-a-file-v11-ships-krondor-2026-10-12)
  (Krondor, 2026-10-12); a slice that needs one waits until that change has merged up.
- **`ai.hands`** — never.

## Slices

| # | Slice | Status |
|---|---|---|
| C1 | **Device-file write** — merge the master's elements into each installation's `DeviceMappings.xml` and `.buttonMap`; keep unique prior content; atomic replace; prune; per installation, reported | **done 2026-10-04** |
| C2 | **Device draft** — where unapplied device edits live; SAVE writes the draft, APPLY promotes it | **done 2026-10-04** |
| C3 | **Startup check for device files** — each installation against the master: unchanged, wiped (matches the stock reference), or edited; adopt or revert | **done 2026-10-05** |
| C4 | **`.binds` and `StartPreset.4.start`** — whole-file master, Edit History on apply, the version-bump merge | ready — scoped 2026-10-12 ([the split](../02-features/bindforge/overview.md#where-the-binds-and-startpreset-master-lives--settled-2026-10-12-alan-foundation-f3)); its one V1.1 change, `BindingsApplyService.applyFrom`, merged up 2026-10-13. The rest is new V1.2 classes. **The `StartPreset.4.start` master was built by Preset Editor P2** (2026-10-18, `bindforge.preseteditor.StartPresetStore`), so C4 is the `.binds` half only |
| C5 | **Change detection for the install folders** | **done 2026-10-11** |
| C6 | **Unsaved work on exit** — Save or Discard, never Apply | **done 2026-10-12** — a ruling, no code |
| C7 | **First-time startup** | after Preset Editor can read all four `StartPreset.4.start` lines |

**C4 is unblocked** (2026-10-13), but Alias Designer does not need it — a master is rewritten only once one
exists — so it is built when Preset Editor or File Manager needs it. C7 waits on Preset Editor.

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

- **2026-10-12 — C6 done as a ruling, no code (Alan).** Recorded in
  [overview.md](../02-features/bindforge/overview.md#built-as-a-ruling-not-code--2026-10-12-alan-core-c6) and
  alias-designer.md's Exit Prompt. **Saved but not applied is not unsaved**, so `DraftChanges.stored()` is *not* the
  prompt's test (the earlier note here was wrong). The only unsaved state is the modal Device Editor, whose own
  Save / Discard / Keep editing is the prompt; nothing else can close the app while it is open. **Later screens
  owe:** Preset Editor and the Bind Editor bring the prompt with them if they hold edits outside a modal dialog,
  hooked on the tab switch and `AppView`'s close. The `.binds` close prompt stays a known issue.

- **2026-10-11 — C5 review fixes** (`code-integrity-review.md`, all but #7). **Every dropped refresh fixed, not
  only file changes (Alan):** `ui.screen.bindings.RefreshCoalescer` replaces `filesChanged` — any request made
  mid-refresh gets one more, so a ship-profile change now costs one extra refresh; recorded in
  [overview.md](../02-features/bindforge/overview.md#external-change-detection--two-watchers-settled-2026-10-11).
  `DeviceFilesWatch` renamed **`DeviceFilesMonitor`** (`DEVICE_FILES_MONITOR`) to match `BindingsMonitor`.
  `FilePoller` reads time and size in one call. **Left (#7):** `FilePoller.stampOf` duplicates
  `AuxiliaryFilesMonitor.stampOf`, which is V1.1 code — that class could use `FilePoller` once on `V1.1-Release`.

- **2026-10-11 — C5 built.** **Branch ruling (Alan):** a new class in `elite.intel.io` that only V1.2 calls is built
  on `V1.2-BindForge` — it edits nothing V1.1 ships; recorded in the README, the platform map and `CLAUDE.md`
  (local only, it is git-ignored). Doc conflict 2 settled as two watchers, in
  [overview.md](../02-features/bindforge/overview.md#external-change-detection--two-watchers-settled-2026-10-11):
  the monitor only reports, the screen's refresh re-runs `DeviceFilesCheck` by content, BindForge's own writes cost
  one harmless refresh. **Found:** `AliasDesignerPanel.initData()` dropped a request arriving mid-refresh — fixed
  for file changes with a `filesChanged` flag, as A13 did. **Left out (Alan):** the Error-level wipe alert and
  the startup report line still have no caller. **C6 needs:** read the 2026-10-06 note; "has edits" is
  `DraftChanges.stored()`.

- **2026-10-06 — C6 deferred, no code (Alan).** C6 guards edits held on screen and not yet saved, and nothing in
  BindForge holds any: the `.binds` grid writes every edit straight into its draft, and the screen designed with
  a SAVE button — the Alias Designer editor — is not built. Built now, the guard would rest on guesses. **It
  comes after A10** (SAVE and DISCARD), built against the real editor. **Found:** the existing close prompt
  breaks the spec — now under Known issues. **No Core slice is ready:** C4 and C5 are blocked, C7 waits on
  Preset Editor. The next work is in [alias-designer.md](alias-designer.md).

- **2026-10-05 — C3 built.** Doc conflict 1 settled: Freshness Checks and Destructive Change Detection
  rewritten for the master model. Six rulings for the device-file check are in
  [overview.md, For the device files](../02-features/bindforge/overview.md#for-the-device-files--settled-2026-10-05-alan):
  unchanged is "Apply writes nothing *and* no hand-added entry"; wiped is byte-identical to stock or no file
  (no version in `DeviceMappings.xml`, so a newer stock file reads as edited — fails safe); empty master =
  not set up; adopt writes the master only, refuses a wipe, stores lost entries as pending removals; the draft
  merges three-way per device and per label. **Found:** `DeviceEntry` holds pairs as a set, so the primary pair
  was unrecoverable — `DeviceMappingsParser.primaries()` added; Apply's no-removal rule hid hand-added entries
  from a plan-only "unchanged". **Later slices owe C3:** *A3/A4* call `check()` for divergence and the `M`
  markers; the startup report line and the Error-level wipe condition have no caller yet; *C5* re-runs the
  check when a file changes. **C6 needs:** the draft's "has edits" test — `isStarted()` is not it (C2 note).

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
