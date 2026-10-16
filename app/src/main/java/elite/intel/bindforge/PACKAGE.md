# `elite.intel.bindforge` - Developer Reference

BindForge owns the game's `.binds` file and the rules about what is in it. The keystroke side - pressing what the file says - stays in `elite.intel.ai.hands` (see [its reference](../ai/hands/PACKAGE.md)), which reads the live bindings through `BindingsMonitor`.

| Package | Holds |
|---|---|
| `bindforge.io` | Finding, parsing, watching, editing, applying and backing up `.binds` files (`BindingsLoader`, `KeyBindingsParser`, `BindingsMonitor`, `BindingsWriter`, `BindingsWorkingCopyRepository`, `BindingsApplyService`, `PlayerBackupService`) and their request/result types. The generic file mechanics they share (atomic replace, timestamped backups) live in `elite.intel.io`, outside BindForge |
| `bindforge.model` | The shared vocabulary: modifiers, slot types, the game's control sections and row labels (`BindingDisplayNames`), the assignable key list (`EliteKeyboardKeys`) |
| `bindforge.rules` | Judging a file: conflict detection and its context rules, reserved chords, key availability, the missing-binding auto-assigner and its safe key pool, and the startup check that speaks the results (`KeyBindCheck`) |
| `bindforge.devicefiles` | V1.2: each installation's `DeviceMappings.xml` and `.buttonMap` files - reading (`DeviceMappingsParser`, `InstallationDeviceScanner`, `ButtonMapAudit`), Frontier's shipped list and who owns an entry (`FrontierStockDevices`, `Provenance`, `ProvenanceRule`), comparing installations against each other and against `.binds` (`DeviceMappingsComparison`, `DeviceDivergenceScanner`, `BindsDeviceReferences`), and writing them - Apply for device files (`DeviceApply`, which promotes the draft and pushes with `DeviceFilesPush`, merging with `DeviceMappingsMerge` and writing labels with `ButtonMapWriter`, each replaced file kept in `EditHistory`). Also the one-off flows: activation (`Activation`, `BeforeBindForgeBackup`), first setup (`FirstSetup`, `FirstSetupPlan`, `FirstSetupReader`), the startup check and its ways out (`DeviceFilesCheck`, `DeviceFilesAdopt`, `LabelReMerge`, `DivergenceWaysOut`), onboarding's writes (`DeviceOnboarding`), catch-up for an installation that missed a write (`MissedWrites`, `DeviceApply.catchUp`, `InstallationHoldings`, `CatchUpAnnouncement`), startup `.buttonMap` generation (`ButtonMapGeneration`) and the watcher that notices the files change (`DeviceFilesMonitor`) |
| `bindforge.devices` | V1.2: the user's controllers as BindForge lists them - live hardware from `elite.intel.devices` matched to the entries in those files (`MyDeviceList`, `MyDevice`, `DeviceIdentities`, `BuiltInDeviceList`, `InstallationMarkers`); naming one (`DeviceNames`, `Onboarding`); the Device Editor's rules (`DeviceEditorInputs`, `DeviceEditorMiddle`, `DeviceEditorLights`); the missing-controller warning (`MissingControllers`, `MissingControllerWarning`); and the service that generates `.buttonMap` files once BindForge is activated (`ButtonMapStartup`) |
| `bindforge.install` | V1.2: finding game installations (`InstallationRegistry`, `GameInstallationProvider` and its `forThisPlatform`: `WindowsGameInstallationProvider`, `LinuxGameInstallationProvider`, both reading Steam's index through `SteamLibraries`) |

**Three packages with "device" in the name, three different things:**

| Package | Is about |
|---|---|
| `elite.intel.devices` | the hardware, live - polling, push-to-talk, the Device Service |
| `bindforge.devices` | the user's controllers, as the My Devices list shows them |
| `bindforge.devicefiles` | the device files inside each game installation |

---

## Architecture

```
BindingsMonitor (background thread, WatchService)
    │ .binds file changed
    ▼
[BindingsLoader] → find active preset → [KeyBindingsParser] → bindings map
    │
    ├─ [KeyBindingExecutor] resolves at call time from live map
    ├─ checkForMissingBindingsAndPersist() → DB (KeyBindingManager)
    └─ checkForConflictsAndPersist()       → DB (BindingConflictManager)

UI edit flow (Bindings tab):
    [BindingsWorkingCopyRepository] → import / track draft
    [BindingsWriter]                → surgical XML text edit (in-place, atomic)
    [BindingsApplyService]          → validate XML → backup → atomic write to game dir
```

---

## Bindings File Management

### File Discovery (`BindingsLoader`)

Reads `StartPreset.*.start` in the game's bindings directory to find the active preset name, then locates
`<presetName>.<version>.binds`. Falls back to the most recently modified
`.binds` file if the preset file is absent or the name cannot be resolved.

That one name is the first non-empty line, which is the right answer for the assistant. `readStartPresetSections`
reads all four lines (General, Ship, SRV and On Foot) into a `StartPresetSections`, for a caller that needs
each section's preset, such as a split preset with General on a different file. It reads the file the live game
uses (`findLiveStartPresetFile`: the highest `StartPreset.N.start`, else `StartPreset.start`), and the
pre-Odyssey single-line form counts as one preset for all four sections. In a folder holding both generations,
`findStartPresetFile` (and so the assistant's single name) can still pick the other file, because it takes
whichever the listing yields first; it is left that way for its V1.1 callers.

### `BindingsMonitor` (singleton)

Monitors the bindings directory via `WatchService` on a dedicated background thread.

On startup and on any `.binds` file create/modify event:

1. Re-resolves the active file via `BindingsLoader`.
2. Parses it with `KeyBindingsParser.parseBindingSlots()`, then derives the execution view with
   `toExecutableBindings()`. Both are published as one immutable snapshot, so `getBindings()`
   (the one key per control that EliteIntel presses) and `getBindingSlots()` (both slots, for conflict scanning) can never be read from different generations of the file.
3. Publishes `BindingsUpdatedEvent` on the EventBus.

After each file event:

- `checkForMissingBindingsAndPersist()` - diffs
  `Bindings.GameCommand` against loaded map; persists new missing bindings to DB.
- `checkForConflictsAndPersist()` - detects shared key combos among
  `GameCommand` bindings; persists new conflicts to DB.

The live bindings map is exposed via `getBindings()`.
`InputSequenceExecutor` reads from this map at execution time (not at sequence creation time), so it always uses the latest parsed state.

### `KeyBindingsParser`

Parses the `.binds` XML file. Two distinct models:

**`KeyBinding`** (executable) - keyboard-only. Used by `KeyBindingExecutor` to press keys.

**`ReadOnlyBindingSlot`** - diagnostic. Preserves the raw `Device` attribute (e.g. `044F0422`
for a HOTAS axis) so the UI can display HOTAS/mouse/gamepad assignments without making them executable.
`keyboardUsable` = true only when `Device="Keyboard"` and all modifiers are also keyboard.
`editable` = true for the subset the chord editor can safely rewrite (a keyboard main key whose modifiers, if any, are all
*supported keyboard* modifiers - L/R Ctrl/Shift/Alt - in any number).

`parseBindings()` → keyboard-executable map only (primary slot wins over secondary).
`parseReadOnlyBindingSlots()` → full diagnostic map for the Bindings tab UI.

### Bindings Edit Pipeline

The editor never writes directly to the game's live
`.binds` file until the user explicitly applies. All edits go through three layers:

**1. `BindingsWorkingCopyRepository`**

Maintains a per-preset working copy in `elite-intel/bindings/`. Tracks a SHA-256 baseline hash (stored in
`<filename>.elite-intel.base.sha256`) to distinguish three states:

| State | Working hash vs baseline | Game hash vs baseline | Meaning |
|---|---|---|---|
| Clean | Equal | Equal | No pending draft |
| Auto-refresh | Equal | Different | Game changed; WC silently updated to match |
| Pending draft | Different | Equal | EI has changes not yet applied |
| Conflict | Different | Different | Both EI and game changed; apply blocked |

`hasUnappliedDraft()` returns true only when the working copy diverges from the baseline.
`gameFileMatchesBaseline()` must return true before `BindingsApplyService` will proceed.

**2. `BindingsWriter`**

Surgical regex-based XML text editor - does not use a DOM Transformer so unrelated XML bytes are preserved byte-for-byte.

Write sequence for a single slot assignment:

1. Stale-file guard (mtime + size must match `KeyboardBindingEdit.expectedLastModified/expectedFileSize`).
2. Locate the action element by tag name in raw XML text.
3. Locate the slot (`Primary` or `Secondary`) inside the action body.
4. Inspect the slot: reject if it has unexpected attributes, unexpected child elements, or any non-keyboard / unsupported modifier (
   `UNSUPPORTED_XML`). Any number of supported keyboard modifiers is accepted.
5. Key availability check: reject if the chosen key + full modifier set is already used by another slot in the same file (
   `KEY_OCCUPIED`). Modifier order is not significant - the set is compared.
6. Second stale-file guard (between read and write).
7. Atomic write through `elite.intel.io.AtomicFiles` (unique temp file, flushed, renamed over the target, non-atomic fallback).

`assignKeyboardKey()` - plain key, no modifier.
`assignKeyboardKeyWithModifier()` - key + one supported keyboard modifier (thin wrapper).
`assignKeyboardKeyWithModifiers()` - key + a chord of one or more supported keyboard modifiers (e.g. Left Ctrl + Left Shift), written as repeated `<Modifier>` nodes. Clearing a slot writes
`Device="{NoDevice}" Key=""`.

`BindingSaveResult` values: `SAVED`, `NO_CHANGE`, `STALE_FILE`, `UNKNOWN_KEY`,
`BINDING_NOT_FOUND`, `UNSUPPORTED_XML`, `KEY_OCCUPIED`, `WRITE_FAILED`.

**The user-edit path** - `assignSlot(SlotAssignment)` and `assignAxis(AxisAssignment)` - is for an edit the user
made on purpose in the bindings editor, and nothing automatic calls it. It may write any device: a button slot
takes a key, mouse button or wheel direction, or a controller's button, hat or axis direction, with up to three
modifiers on any device and the slot's Hold flag; an axis takes a controller's whole axis (`Joy_XAxis`-`Joy_VAxis`)
with Inverted and Deadzone, the deadzone written as the game writes a float. It does not check `KEY_OCCUPIED` - the
editor warns and the user decides - and refuses a token that does not suit the slot (`UNKNOWN_KEY`) or a slot with
structure it does not know (`UNSUPPORTED_XML`). A keyboard token is any `Key_` name, not only the keys
`EliteKeyboardKeys` lists, because a non-English layout writes the character itself (`Key_ß`).
The game's own literals (`Keyboard`, `Mouse`, `{NoDevice}`) are taken in the game's spelling only, and never as a
controller's name in another case. Hold is written as the `<Hold Value="1" />` child the game writes, unless the
slot already carries it as an attribute. `ToggleOn` is never touched, and `KeyBindingsParser` is not widened:
writing a controller binding never makes one executable.

**The device rewrite** - `rewriteDeviceReferences(List<Path>, DeviceReferenceRewrite, Path)` - moves every
`Device="..."` naming one device to a new name across several `.binds` files (the game's, drafts, saved copies),
all or nothing: every file read first (a file named twice, under any spelling, counts once; one whose text cannot be
rewritten whole stops it as `UNSUPPORTED_XML`), each changed file backed up, re-read before its write, and put back
if a later one fails or was saved meanwhile - unless the game saved it again after the rewrite, which is left as the
game wrote it (`DeviceRewriteResult.notRestored`). `DeviceReferenceRewrite` builds the three cases:
`hardwareIdToName` (VID then PID, the order the game writes; a caller that found the reverse uses the constructor), `rename`, and `nameToHardwareId` (VID then PID, upper case). Matching
ignores case. The text-only form, `rewriteDeviceReferences(String, DeviceReferenceRewrite)`, is pure and returns a
`RewrittenXml`. Files are written in the order given, and the game's file goes last: writing it makes the monitor's reload refresh a working copy that holds no draft, which a later working copy would read as changed. Rewriting a working copy with its game file moves both off the baseline, so the caller then calls
`BindingsWorkingCopyRepository.recordGameFileBaseline` for that preset; without it the next apply refuses the draft.

**3. `BindingsApplyService`**

Pushes the approved working copy to the game directory:

1. Read working copy bytes.
2. Round-trip XML validation (strips BOM before parse).
3. Conflict check: reject if game file changed since the draft was created.
4. Backup current game file to `elite-intel/bindings/backups/` via `elite.intel.io.TimestampedBackups`.
5. Atomic write to game directory (`AtomicFiles`).
6. `workingCopyRepo.markApplied()` updates the baseline hash to the newly written content.

`applyFrom(source, gameFile)` pushes a file that is not the working copy - BindForge's saved copy, on a revert -
through steps 1, 2, 4 and 5, leaving the working copy and its baseline alone. A game file already identical to the
source is not rewritten. A draft kept across it needs its baseline moved the same way, with
`recordGameFileBaseline`.

### Conflict & Missing Binding Detection

**`BindingConflictRules`**

Inverted binding map → `keyCombo → [actionNames]`. For each
`Bindings.GameCommand`, check if another action shares the same key+modifier combo.

Safe overlaps are not flagged:

- Different vehicle states (ship / buggy / humanoid) - mutually exclusive in-game, so one control bound the same way in every vehicle (trigger, fire groups, cargo scoop, panels, maps, lamps, night vision) is a layout, not a clash. The vehicle comes from `BindingDisplayNames` (the game's own controls screen), not from the tag: the SRV drive and fire controls are spelled `Buggy*` and
  `SteerLeftButton`, with no `_Buggy` suffix.
- Sub-state overlays (FreeCam, FSS, SAA, GalnetAudio, the map camera incl. `GalaxyMapHome`) - only active inside a specific UI mode.

Dangerous pairs have curated descriptions; unknown conflicts get a generic humanized description. Results are diffed against the DB state so only newly-appearing conflicts are announced.

**`BindingGroupClassifier`**

Classifies a binding ID into a
`BindingGroup` enum value (SHIP_FLIGHT, COMBAT, UI_PANELS, MAPS, EXPLORATION, CAMERA, SRV, ON_FOOT, MISCELLANEOUS) by suffix/keyword matching. Used by the Bindings UI tab to group rows.

### Supporting Utilities

**`KeyBindCheck.check()`
** - triggers missing/conflict detection and publishes the voice announcements. Missing bindings are announced as a count pointing at the Bindings tab (the names go to the log at INFO, for the support bundle); ordinary conflicts are announced as counts; a
**blocking** conflict instead names the chords in collision (via
`BindingChordSpeech`), because the commander has to know which keys to move, and Frontier's W/A/S/D default is not necessarily theirs.

**`EliteKeyboardKeys`
** - static allow-list of keyboard token strings the MVP editor can assign. Kept static so the UI dropdown can show keys that are not yet in the active file.

---

## Key Interfaces & Classes - Quick Reference

| Class | Role |
|---|---|
| `KeyBindingsParser` | Parses `.binds` XML → executable and read-only binding models |
| `BindingsLoader` | Finds the active `.binds` file via `StartPreset.*.start` |
| `BindingsMonitor` | Watches for `.binds` changes; holds the live bindings map |
| `BindingsWorkingCopyRepository` | Per-preset draft management with SHA-256 baseline tracking |
| `BindingsWriter` | Surgical text-based XML slot editor; atomic write |
| `BindingsApplyService` | Validate → backup → atomic apply to game directory |
| `BindingConflictRules` | Safe-overlap logic and curated dangerous-pair descriptions |
| `BindingChordSpeech` | Renders a chord as speakable words ("Left Control plus W") for the spoken warnings |
| `BindingGroupClassifier` | Classifies binding IDs into display groups |
| `EliteKeyboardKeys` | Static allow-list of assignable key tokens for the UI |
