# `elite.intel.bindforge` - Developer Reference

BindForge owns the game's `.binds` file and the rules about what is in it. The keystroke side - pressing what the file says - stays in `elite.intel.ai.hands` (see [its reference](../ai/hands/PACKAGE.md)), which reads the live bindings through `BindingsMonitor`.

| Package | Holds |
|---|---|
| `bindforge.io` | Finding, parsing, watching and editing `.binds` bytes (`BindingsLoader`, `KeyBindingsParser`, `BindingsMonitor`, `BindingsWriter`) and their request/result types. Nothing here writes a game file: the edits are held and applied by `bindforge.storage` |
| `bindforge.model` | The shared vocabulary: modifiers, slot types, the game's control sections and row labels (`BindingDisplayNames`), the assignable key list (`EliteKeyboardKeys`) |
| `bindforge.rules` | Judging a file: conflict detection and its context rules, reserved chords, key availability, the missing-binding auto-assigner and its safe key pool, and the startup check that speaks the results (`KeyBindCheck`) |
| `bindforge.devicefiles` | V1.2: reading and making `DeviceMappings.xml` and `.buttonMap` files as bytes - reading (`DeviceMappingsParser`, `ButtonMapReader`, `ButtonMapAudit`), Frontier's shipped list (`FrontierStockDevices`, `FrontierStockButtonMaps`), what the binds files name (`BindsDeviceReferences`), and changing them (`DeviceMappingsMerge`, `ButtonMapWriter`, `ButtonMapLabels`). Also first setup's decision (`FirstSetupPlan`, `FirstSetupReader`, `LabelMerge` - the flow itself is `bindforge.storage.FirstTimeStartup`). Nothing here writes a game file: every write is `bindforge.storage`'s |
| `bindforge.devices` | V1.2: the user's controllers as BindForge lists them - live hardware from `elite.intel.devices` matched to the entries in those files (`MyDeviceList`, `MyDevice`, `DeviceIdentities`, `BuiltInDeviceList`); naming one (`DeviceNames`, `Onboarding`); the Device Editor's rules (`DeviceEditorInputs`, `DeviceEditorMiddle`, `DeviceEditorLights`); and the missing-controller warning (`MissingControllers`, `MissingControllerWarning`) |
| `bindforge.storage` | V1.2: back up, edit, apply ([storage.md](../../../../../../../docs/02-features/bindforge/storage.md)) - the one backup system (`Backups`, `Backup`, `BackupSources`, `BackupDestination`, `StartupBackup`), the working copy and the one write path (`WorkingCopy`, `LiveCopies`, `WritePath`, `ConfigurationFiles`, `WriteReport`, `Written`, and `ReadOnlyFlag`, which keeps a protected live file protected, and `FileNames`, the name rules - "a file name, not a path", and which names are binds and preset files), first-time startup (`FirstTimeStartup`, `FirstSetupReading`, `InstallationFiles`, `FirstSetupFiles`), RESTORE (`Restore`, `BackupContents`), the edits not applied and what each Alias Designer action does to them (`Edits`, `DeviceEdits`, `BindsFiles`), what the Bind Editor and the Preset Editor do to them (`BindsEdits`, `PresetEdits`), the controllers' names and labels as the Bind Editor reads them (`SavedDeviceLabels`), and APPLY (`Apply`, refusing first a preset file that names no binds file it can find - `PresetRefusedException` - and leaving alone an installation that differs - `DeviceFilesComparison`) |
| `bindforge.install` | V1.2: finding game installations (`InstallationRegistry`, `GameInstallationProvider` and its `forThisPlatform`: `WindowsGameInstallationProvider`, `LinuxGameInstallationProvider`, both reading Steam's index through `SteamLibraries`) |
| `bindforge.preseteditor` | V1.2: the preset file, `StartPreset.4.start` - the BindNames a line of it can hold, read from the binds files in the set APPLY writes and each installation's `ControlSchemes` (`BindNameCatalog`, `BindNameList`, `BindNameEntry`, `BindName`, `BindsFileOrigin`), and the check of what is in a file with a `.binds` name (`BindsContent`, `NotOfferedFile`, `NotOfferedReason`). Editing and writing the preset file is `bindforge.storage`'s (`PresetEdits`, since Storage S6) |

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

UI edit flow (Bind Editor, since Storage S5):
    [bindforge.storage.Edits]  → holds the edits not applied (the working copy, changed)
    [BindsEdits]               → one SAVE, all or none
    [BindingsWriter]           → surgical XML text edit of the bytes, in memory
    [bindforge.storage.Apply]  → the one APPLY: check, back up, write the working copy to the game
```

---

## Bindings File Management

### File Discovery (`BindingsLoader`)

Reads `StartPreset.*.start` in the game's bindings directory to find the active preset name, then locates
`<presetName>.<version>.binds`. Falls back to the most recently modified
`.binds` file if the preset file is absent or the name cannot be resolved.

That one name is the first non-empty line, which is the right answer for the assistant. In a folder holding both
generations, `findStartPresetFile` (and so the assistant's single name) can pick either, because it takes whichever
the listing yields first; it is left that way for its V1.1 callers. BindForge reads all four lines (General, Ship, SRV
and On Foot) as a `StartPresetSections`, from the working copy's highest `StartPreset.N.start`
(`bindforge.storage.PresetEdits`, Storage S6).

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

The editor never writes a file (Storage S5). Its changes are edits not applied, held by `bindforge.storage.Edits`
with every other tab's, and the one APPLY above BindForge's tabs writes them through the write path
([storage.md](../../../../../../../docs/02-features/bindforge/storage.md) rules 6-7). `bindforge.storage.BindsEdits`
makes one SAVE all or none; `BindingsWriter` changes the bytes.

**`BindingsWriter`**

Surgical regex-based XML text editor - does not use a DOM Transformer so unrelated XML bytes are preserved byte-for-byte.
Each method takes a binds file's bytes and returns an `EditedBinds`: the result, and the changed bytes. A UTF-8
byte-order mark is kept.

Sequence for a single slot assignment:

1. Locate the action element by tag name in raw XML text.
2. Locate the slot (`Primary` or `Secondary`) inside the action body.
3. Inspect the slot: reject if it has unexpected attributes, unexpected child elements, or any non-keyboard / unsupported modifier (
   `UNSUPPORTED_XML`). Any number of supported keyboard modifiers is accepted.
4. Key availability check: reject if the chosen key + full modifier set is already used by another slot in the same file (
   `KEY_OCCUPIED`). Modifier order is not significant - the set is compared.

`assignKeyboardKey()` - plain key, no modifier.
`assignKeyboardKeyWithModifier()` - key + one supported keyboard modifier (thin wrapper).
`assignKeyboardKeyWithModifiers()` - key + a chord of one or more supported keyboard modifiers (e.g. Left Ctrl + Left Shift), written as repeated `<Modifier>` nodes. Clearing a slot writes
`Device="{NoDevice}" Key=""`.

`BindingSaveResult` values: `SAVED`, `NO_CHANGE`, `UNKNOWN_KEY`, `KEY_OCCUPIED`, `BINDING_NOT_FOUND`,
`UNSUPPORTED_XML`. Every one but `SAVED` leaves the bytes as they were; a failure of the edits themselves is an
exception, not a result.

**The user-edit path** - `assignSlot(byte[], SlotAssignment)` and `assignAxis(byte[], AxisAssignment)` - is for an edit the user
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

**The device rewrite** - `rewriteDeviceReferences(String, DeviceReferenceRewrite)` - moves every `Device="..."`
naming one device to a new name in one binds file's text, and returns a `RewrittenXml`. It is pure; `bindforge.storage.BindsFiles`
runs it over every binds file in the set. `DeviceReferenceRewrite` builds the three cases:
`hardwareIdToName` (VID then PID, the order the game writes; a caller that found the reverse uses the constructor), `rename`, and `nameToHardwareId` (VID then PID, upper case). Matching
ignores case.

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
| `BindingsWriter` | Surgical text-based XML slot editor, on bytes in memory |
| `BindingConflictRules` | Safe-overlap logic and curated dangerous-pair descriptions |
| `BindingChordSpeech` | Renders a chord as speakable words ("Left Control plus W") for the spoken warnings |
| `BindingGroupClassifier` | Classifies binding IDs into display groups |
| `EliteKeyboardKeys` | Static allow-list of assignable key tokens for the UI |
