# Alias Designer

**Naming controllers, and keeping every installation's device files identical.** The tab exists and shows real
data; most of what the spec describes is still ahead.

## Spec

- [alias-designer.md](../02-features/bindforge/alias-designer.md) — **read end to end at the start of every
  session.** It is the screen's definition, and building from the slice's name instead of the page is how this
  tab went wrong the first time — see `CLAUDE.md`.
- [device-provenance.md](../03-data-models/device-provenance.md) — the three tables, and why there are three.
- [EliteDangerous-DeviceMappings-ButtonMap.md](../02-features/bindforge/domain-knowledge/EliteDangerous-DeviceMappings-ButtonMap.md)
  — the file formats, and what has been measured on a live game.
- [ui-component-map.md — Alias Designer](../02-features/bindforge/ui-component-map.md#alias-designer).

## Built

| What | Where |
|---|---|
| The tab | `ui.screen.bindings.AliasDesignerPanel` — My Devices above the divergence list, in `BindingsTabPanel` |
| My Devices — four of its five columns | `bindforge.devices.MyDeviceList` (the union, a pure function), `MyDevice`. Built-ins show `BUILT-IN` in the Alias column |
| Device names | `bindforge.devices.DeviceNames` — the default-name rule, alias validation (`Verdict`, `Problem`), `sameName`, and `needingName` (who onboarding asks). Pure; the caller passes the names already taken |
| Hardware VID/PID | `bindforge.devices.DeviceIdentities` — from SDL's GUID; confirmed on four controllers, real GUIDs in its test |
| Frontier's shipped list | `bindforge.devicefiles.FrontierStockDevices`, resource `bindforge/FrontierStock-DeviceMappings.xml` |
| First setup (A1) | `bindforge.devicefiles.FirstSetup` (read, then backup → master → push), `FirstSetupPlan` (the four cases and the answers, pure), `FirstSetupReader`, `DeviceFilesSnapshot` (the labelled backup), `FrontierStockButtonMaps` (resources `FrontierStock-VPC*.buttonMap.xml`); `BindForgeDeviceDraftManager.establish`; `ui.screen.bindings.FirstSetupDialog`, opened by **SET UP** on `AliasDesignerPanel` while the master is empty |
| `.buttonMap` label merge (A2) | `bindforge.devicefiles.LabelMerge` (pure: the disagreeing inputs, answers per input, the whole-file route, the merged labels; sides keyed by name); `ui.screen.bindings.LabelMergeDialog`, opened by **Compare N inputs** on a *Button labels disagree* row in `FirstSetupDialog` |
| Startup `.buttonMap` (A8) | `bindforge.devicefiles.ButtonMapGeneration` (per installation, create-only), `ButtonMapLabels` (the generated labels); `bindforge.devices.ButtonMapStartup` runs it on every connect, started as `BUTTON_MAP_STARTUP` in `AppController` |
| Divergence list (A3) | `DeviceDivergence.againstMaster` (pure; each installation against the master, from `DeviceFilesCheck`), `DeviceDivergenceScanner.scanAgainstMaster` (adds orphans and `.binds`), `BindsDeviceReferences`, `ButtonMapAudit`. Before first setup, `DeviceDivergence.rank` still compares installations with each other |
| Ways out per row (A3b) | `bindforge.devicefiles.DivergenceWaysOut` (which ways a row gets, pure), `DeviceFilesAdopt.adopt(installId, deviceName)` (one device) and `adoptLabels` (the re-merge), `LabelReMerge` (master plus installations as `LabelMerge` sides); buttons beneath the list in `AliasDesignerPanel`, each confirmed. Revert is `DeviceFilesPush.push(installId)` |
| What each installation holds | `InstallationDeviceScanner`, run on every refresh; rows in `bindforge_device_installs` |
| Game Install Locations screen | `ui.screen.bindings.GameInstallLocationsPanel` — in Binding Management, where the spec puts it in File Manager |
| Master store | see [core.md](core.md) |

**The divergence list compares each installation with the master** once first setup has filled it, and the
installations with each other before then. Once the master exists, each row offers its ways out.

## Settled — and where it is recorded

- **The list is the union of live hardware and file entries.** `DeviceMappings.xml` supplies names, never
  existence. [Spec, My Devices](../02-features/bindforge/alias-designer.md#my-devices)
- **The Installations markers wait for first setup.** `M`, *not added* and the divergence colour are all defined
  against a master record. With the master empty, an installation holding `LVWAP` is in none of those states,
  and the spec does not define one. Building markers first would mean inventing it.
- **"Device service is not running" is a row in the table**, not just a log line — an empty list because nothing
  is plugged in and one because nothing is looking are different states.
- **Duplicate-VID/PID handling is out of scope.** [Spec, Deferred](../02-features/bindforge/alias-designer.md#deferred)
- **DirectInput instance GUIDs are out of scope** — the game keys on VID/PID, and no file could carry more.
- **The game ignores `DeviceMappings.xml` formatting; it does not ignore missing children.**
  [Domain doc §1.2c](../02-features/bindforge/domain-knowledge/EliteDangerous-DeviceMappings-ButtonMap.md#12c-the-game-parses-it-as-xml-and-does-not-reformat-it--measured-2026-10-03)

## Known issues — fix when a slice passes them


- **The first-setup backup shows in the Player Backups list.** `DeviceFilesSnapshot` writes beside
  `PlayerBackupService`'s folders, so `listBackups()` lists it, with installation subfolders as its "files". File
  Manager's restore work should recognise it (or Krondor moves it) — `bindforge.io`.
- **The first-setup backup ignores a chosen backup destination** — the resolver is private to `PlayerBackupService`.
- **`ButtonInputMapper.axisToBindsToken` throws for axis index 6 and above.** A8 went around it (`ButtonMapLabels`
  labels all eight tokens); the class is shared with V1.1, so the fix — add U and V — is Krondor's, and the
  Bind Editor's capture needs it.
- **SDL numbers axes without gaps**, so anything mapping an SDL axis index to a game token is wrong for a stick
  reporting X, Y and RZ. A8 sidesteps it by labelling all eight. Recorded under [the startup `.buttonMap`](../02-features/bindforge/alias-designer.md#at-elite-intel-startup-a-buttonmap-for-every-connected-named-controller);
  it will bite the Device Editor's live highlighting and the capture dialog too.
- **Still open from the first code review:**
  - **#7 — paths are stored as typed**, so `E:\SteamLibrary\…` and `e:\steamlibrary\…\` become two rows for one
    installation. Discussed 2026-09-30 and leaning towards deleting **relocate** altogether, since Remove then
    Add covers every case and nothing yet depends on an installation keeping its id. **Undecided — ask Alan.**
  - **#12 — `WindowsGameInstallationProvider` reads `InstallPath` under `HKCU\Software\Valve\Steam`**, where the
    value is named `SteamPath`. The two `HKLM` lookups work, so detection works; the `HKCU` fallback never finds
    anything.

## Boundaries

- **Owns** `bindforge.devices`, `bindforge.devicefiles`, `bindforge.install` and `ui.screen.bindings.AliasDesignerPanel`.
- **Writes through Core, never around it.** No slice here writes a game file itself.
- **The Game Install Locations screen belongs to File Manager** — fix bugs in it here if a slice needs to, but its
  move into File Manager is that section's work.

## Slices

| # | Slice | Needs | Status |
|---|---|---|---|
| A1 | **First setup** — turn what the installations hold into the master: per element, the four cases, a name conflict routed into the rename, a labelled backup and Edit History before writing | Core C1, C2 (both done) | **done 2026-10-07** |
| A2 | **`.buttonMap` label merge** — one row per input that disagrees, nothing else | A1 (done) | **done 2026-10-08** |
| A3 | **Divergence against the master** — replace installation-against-installation | A1 (done) | **done 2026-10-08** |
| A3b | **The ways out, per row** — adopt (`DeviceFilesAdopt`), revert (`DeviceFilesPush.push(installId)`), and the label re-merge with the master as one side of `LabelMergeDialog`. Confirms first; backups before any game-file write | A3 (done) | **done 2026-10-08** |
| A4 | **Installations markers** — `M`, *not added*, severity colour | A1 (done) | **next** |
| A5 | **Built-in Devices tab** — Frontier's list, read-only | — | ready |
| A6 | **Onboarding: the rule** — the default-name rule and alias validation | Core C1 (done) | **done 2026-10-06** |
| A6b | **Onboarding: the dialog and its writes** — name the controllers `needingName` returns, then write the entry and a generated `.buttonMap` straight away. A controller `.binds` names by hex waits for Apply, with a `.binds` rewrite | A6, A8 | waiting — the hex case is `bindforge.io`, so it is blocked until the question in the README is answered |
| A7 | **Automatic registration and hot-plug** — `DeviceBus` connect and disconnect | A6b | waiting |
| A8 | **`.buttonMap` generation at Elite-Intel startup** — connected, named, none on disk | Core C1 (done) | **done 2026-10-06** |
| A9 | **Device Editor** — inline expansion, install strip, labels, live highlighting | C2 (done) | ready |
| A10 | **Actions** — SAVE, DISCARD, CLEAR, RESET LABELS, APPLY, reported per installation. Unlocks [Core C6](core.md#slices), the exit prompt, which needs an editor holding unsaved edits. **RESET LABELS must delete the `.buttonMap` in every installation**, or A8 never regenerates it | A9 | waiting |
| A11 | **Rename** — every installation first, `.binds` last, all or nothing | A10 | waiting |
| A12 | **When hardware changes** — a missing device, the identity question, retarget, one missing device at a time | A11 | waiting |
| A13 | **The missing-controller warning**, once per run | — | ready |

**A5 and A13 need nothing from Core** — either is a good slice for a session that wants something self-contained
while Core is in progress.

## Hand-off notes

*Newest first. Two or three lines each: what was decided and where it is recorded, what was found, what the
next slice needs.*

- **2026-10-08 — A3b built** (Alan chose all five recommendations). Recorded in [the ways out](../02-features/bindforge/alias-designer.md#the-ways-out--2026-10-08-alan):
  adopt from a row takes **one device** (`adopt(installId, deviceName)`), never a missing entry; revert stays the
  whole installation and the confirm counts the rows it reaches; Edit History is the backup before revert; adopt and
  the re-merge write the master (and draft) only, so the other installations become rows to revert. An unreadable
  `DeviceMappings.xml` and an orphan `.buttonMap` have no way out. **Owed:** the orphan's removal is A10's (CLEAR);
  an unreadable file has no repair until File Manager's restore. **A4** reads the same `DeviceFilesCheck.Report` the
  panel now keeps as `currentReport`.

- **2026-10-08 — A3 review fixes** (`code-integrity-review.md`, Alan). Recorded in [against the master](../02-features/bindforge/alias-designer.md#against-the-master--2026-10-08-alan):
  reset to Frontier's file is red only when `.binds` names one of the user's own devices, and no file at all is
  always red (`InstallationCheck.fileGone`); one cause is one row — no label rows beside a reset, no orphan row for
  a missing entry's map. `DeviceFilesCheck.Report` now carries `masterNames` and `userNames`, so the screen reads
  the master once; `againstMaster` refuses a report with no master; `installKey(long)` is shared by the panel;
  `ENTRY_MISSING_FROM_SOME` became `ENTRY_MISSING`; an unchanged installation logs at DEBUG. **A4** can read the
  same `Report`.

- **2026-10-08 — A3 built** (Alan chose all three recommendations). Recorded in [against the master](../02-features/bindforge/alias-designer.md#against-the-master--2026-10-08-alan):
  before first setup the list still compares installations with each other; a reset or unreadable
  `DeviceMappings.xml` is one row; an entry the master lacks, and one out of place, are red when `.binds` names
  them; labels are one yellow row per device. **Split:** the ways out became **A3b** — they are writes, and the
  re-merge needs a new write of one device's labels into the master *and* the draft (three-way, like adopt).
  **A3b needs:** findings carry installation ids (`DeviceDivergence.installKey(long)`), which is what adopt and revert take;
  `LabelMerge` takes the master as a side by name. A4's `M` markers can read the same `DeviceFilesCheck.Report`.

- **2026-10-08 — A2 built** (Alan chose option A). Recorded in [the label merge](../02-features/bindforge/alias-designer.md#the-buttonmap-label-merge--2026-10-08-alan):
  the device keeps one row in first setup and opens the merge view; an answer is the label, keyed
  `labels:<device>:<input>`; every disagreeing input is its own question; *use X as the master* answers only inputs X
  labels. **Found:** `List.copyOf(...).contains(null)` throws — `LabelMerge.unanswered` checks null first. **A3 needs:**
  the re-merge is not built; `LabelMerge` takes sides by name, so pass the master as one side and reuse
  `LabelMergeDialog` (its `sideNames` map names the master).

- **2026-10-08 — A1 review fixes** (`code-integrity-review.md`, Alan). Recorded under *After review* in
  [First setup](../02-features/bindforge/alias-designer.md#settled-while-building-it--2026-10-07-alan): two names
  on one VID/PID inside one installation are not a question — the first in the file is the device, the rest are
  `SHADOWED` rows and pending removals (Alan chose this over asking); a name winning any name conflict is kept, and
  answers putting two devices on one VID/PID count as unanswered; `Outcome.FAILED` names the step, and the dialog
  closes whenever the master is filled; a failed snapshot removes its folder. `Target.isReachable()` replaced the
  same check in five classes. **A10 owes:** the pending-removal cleanup must not remove an entry `.binds` still
  names — a shadowed or losing name can be one.

- **2026-10-07 — A1 built** (Alan). Rulings in [First setup, settled while building it](../02-features/bindforge/alias-designer.md#settled-while-building-it--2026-10-07-alan):
  a name conflict is answerable only when the losing name is not in `.binds` (the rename is A11, its `.binds`
  rewrite blocked) — the loser stays on disk under the winner as a pending removal; colliding labels are answered
  per device by an installation, the rest unioned; "use as master" never drops another installation's extras; the
  backup is device files only, in the default folder; any draft is discarded as the master is filled. **Found:**
  `app/build.gradle` packages resources by extension, so the Frontier maps ship as `.buttonMap.xml`. **A2 needs:**
  replace `FirstSetupPlan`'s per-device `LABELS_DIFFER` answer with one row per colliding input (the spec's merge
  table), keeping the whole-file route; the same view should serve a later re-merge. **A3/A4** now have a master
  to compare against — call `DeviceFilesCheck.check()` (C3 note). **Still owed (C3 note):** generated A8 maps are
  mastered by first setup as labels; whether those count as user content is undecided.

- **2026-10-06 — A8 second review fixes** (`code-integrity-review.md`, Alan). `stop()` now interrupts and drops
  queued work, so nothing lands in a game folder after it returns and no temp file is left at exit; the name and
  the icon flag come from one pass over one element; the create-only write moved to package-private
  `CreateOnlyFiles` (temp names as `AtomicFiles` names them), for A6b to share; the case-insensitive master check
  carries its `// WHY:`. **Found by the new test:** on Windows an element named `C:x` resolved to a path on drive
  C, outside the game folder — `GameInstallation.buttonMapIn` now refuses any name that does not land directly in
  `DeviceButtonMaps`, which also covers Apply and the startup check. **Owed by whoever wires the startup check (C3) at startup:** an element an installation
  added gets a generated `.buttonMap`, which the check lists as 30–40 `ADDED` labels and adoption would take into
  the master. Decide whether a file byte-identical to a generated one is user content, and which runs first.

- **2026-10-06 — A8 review fixes** (`code-integrity-review.md`, Alan). All eight axis labels whenever a
  controller has any axis, instead of by position; devices the game draws itself are skipped — `<SupportsIcons>`
  and `<GamePad>` — after Frontier's readme confirmed icon tokens (`[x52b1]`); the create-only write publishes by
  hard link, so it refuses atomically on Linux too; temp cleanup no longer masks the outcome; device-file paths
  live once in `GameInstallation` (`deviceMappingsIn`, `buttonMapIn`), used by Push, Check, the scanners and both
  panels; `ButtonMapStartupTest` added. All recorded in the spec. Frontier's readme is in `reference-data`; its
  "put maps in the bindings folder" line does **not** change where `.buttonMap` lives — the installation's
  `ControlSchemes\DeviceButtonMaps`, as documented (Alan).

- **2026-10-06 — A8 built** (Alan). `ButtonMapGeneration` creates a `.buttonMap` for a connected controller an
  installation already names, never over an existing file; decisions recorded in
  [the startup `.buttonMap`](../02-features/bindforge/alias-designer.md#at-elite-intel-startup-a-buttonmap-for-every-connected-named-controller):
  every connect, not only startup; the name is per installation (first match); a device the master labels is
  skipped as C3's drift; axes by position X–V; no hats (they report as buttons). **Found:** SDL compacts axis
  indices — see Known issues. Started as `BUTTON_MAP_STARTUP` in `AppController.buildServices` after `DEVICE`
  (Alan: no need to wait on Krondor for one line; diagnostics stubs it). **A6b reuses** `ButtonMapLabels` and
  `CreateOnlyFiles.createNew`. **A1 next** — first setup will
  meet generated maps for built-ins in every installation; they are identical, so they merge without a question.

- **2026-10-06 — A6 built: the rule, not the dialog** (Alan). `bindforge.devices.DeviceNames` is pure, and the
  spec's six examples are its tests. Decided and recorded in [the default-name rule](../02-features/bindforge/alias-designer.md#the-default-name-rule):
  ASCII letters only with accents folded; numbering starts at 2 and shortens by the number's width; a device's
  own name is not a clash with itself. **Split:** the onboarding dialog and its writes became **A6b**, because
  the hex case rewrites `.binds` (blocked on `bindforge.io`) and generated labels need A8's generator. **A8
  needs:** a create-only `.buttonMap` write in `bindforge.devicefiles` (never `push()`), and a fix for
  `ButtonInputMapper.axisToBindsToken` throwing at axis 6 and above. A6b will reuse A8's label generator.
  **Callers owe the rule:** the names-taken set is the master's names, every installation's entries and
  Frontier's `names()`, minus the device's own name when it is renamed.

- **2026-10-03 — brief written.** Built so far: install detection, device-file parsing, the orphan check, severity,
  the divergence list, the master store, Frontier's stock reference, the hardware VID/PID derivation, My Devices
  and the installation scan.
