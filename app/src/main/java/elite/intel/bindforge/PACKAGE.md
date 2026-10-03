# `elite.intel.bindforge` - Developer Reference

BindForge owns the game's `.binds` file and the rules about what is in it. The keystroke side - pressing what the file says - stays in `elite.intel.ai.hands` (see [its reference](../ai/hands/PACKAGE.md)), which reads the live bindings through `BindingsMonitor`.

| Package | Holds |
|---|---|
| `bindforge.io` | Finding, parsing, watching, editing, applying and backing up `.binds` files (`BindingsLoader`, `KeyBindingsParser`, `BindingsMonitor`, `BindingsWriter`, `BindingsWorkingCopyRepository`, `BindingsApplyService`, `PlayerBackupService`) and their request/result types. The generic file mechanics they share (atomic replace, timestamped backups) live in `elite.intel.io`, outside BindForge |
| `bindforge.model` | The shared vocabulary: modifiers, slot types, the game's control sections and row labels (`BindingDisplayNames`), the assignable key list (`EliteKeyboardKeys`) |
| `bindforge.rules` | Judging a file: conflict detection and its context rules, reserved chords, key availability, the missing-binding auto-assigner and its safe key pool, and the startup check that speaks the results (`KeyBindCheck`) |
| `bindforge.devices`, `bindforge.install` | V1.2: device mappings and game installations |

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

**3. `BindingsApplyService`**

Pushes the approved working copy to the game directory:

1. Read working copy bytes.
2. Round-trip XML validation (strips BOM before parse).
3. Conflict check: reject if game file changed since the draft was created.
4. Backup current game file to `elite-intel/bindings/backups/` via `elite.intel.io.TimestampedBackups`.
5. Atomic write to game directory (`AtomicFiles`).
6. `workingCopyRepo.markApplied()` updates the baseline hash to the newly written content.

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
