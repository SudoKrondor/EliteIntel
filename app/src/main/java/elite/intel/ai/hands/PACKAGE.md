# `elite.intel.ai.hands` - Developer Reference

The hands package owns two distinct responsibilities:

1. **Keystroke execution
   ** - sending hardware-level key events to Elite Dangerous in a way that DirectInput actually receives.
2. **Bindings lifecycle management** - reading, monitoring, editing, and atomically applying the game's
   `.binds` XML file.

---

## Package Architecture

```
Command handler
    │ GameControllerBus.publish(GameInputSequenceEvent)
    ▼
[InputSequenceExecutor]        single worker thread, serializes all steps
    │
    ├─ BINDING_TAP / BINDING_HOLD → [KeyBindingExecutor] → resolve key name → [KeyProcessor]
    ├─ RAW_KEY                    →                         [KeyProcessor]
    ├─ TEXT                       →                         [KeyProcessor.enterText]
    └─ DELAY                      →                         Thread.sleep
                                        │
                             ┌──────────┴──────────┐
                             ▼                     ▼
                   NativeKeyInput             java.awt.Robot
            (scan codes / keysyms)      (VK codes, non-native keys)
```

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

## Part 1 - Keystroke Execution

### `GameInputSequenceEvent` / `GameInputStep`

The public API. Every command handler that needs to press keys builds a
`GameInputSequenceEvent` and publishes it on `GameControllerBus` (not the main
`EventBusManager`).

```java
GameControllerBus.publish(GameInputSequenceEvent.of(
        GameInputStep.bindingTap(BINDING_GALAXY_MAP.getGameBinding()),
        GameInputStep.

delay(3000),
    GameInputStep.

rawKey(KeyProcessor.KEY_ENTER)
));
```

| Step type | Factory method | Notes |
|---|---|---|
| `BINDING_TAP` | `bindingTap(bindingId)` | Forced tap regardless of binding's `hold` flag - safe for UI navigation |
| `BINDING_HOLD` | `bindingHold(bindingId, holdMs)` | Holds the main key for the given duration |
| `RAW_KEY` | `rawKey(keyCode)` / `rawKey(keyCode, modCode, holdMs)` | Bypasses binding lookup; uses `KeyProcessor` codes directly |
| `TEXT` | `text(string)` | Types characters via `KeyProcessor.enterText`; handles non-QWERTY layouts |
| `DELAY` | `delay(ms)` | Explicit pause; does not trigger the default post-input delay |

After every input-producing step (
`isInputProducing() == true`) the executor automatically inserts a random 99–201 ms post-input delay so the game's DirectInput poller has time to see the key event before the next step fires.

### `InputSequenceExecutor`

Subscribed to `GameControllerBus` via `@Subscribe`. Serializes all sequences through a single-thread
`ExecutorService` so command handlers cannot interleave key events.

`Future.get()` is called from the caller's thread, which means the caller blocks until the full sequence completes. This is intentional - command handlers should not race ahead of the game.

Nested sequences (a step's handler publishing another `GameInputSequenceEvent`) are detected via
`workerThread` comparison and executed inline to avoid self-deadlock.

### `KeyProcessor` (singleton)

The low-level key dispatcher. Routes each key code to `NativeKeyInput` or `Robot`:

- `keyCode >= NATIVE_BASE (0x10000)` → always native (left/right modifiers, EU special chars, etc.)
- `nativeKeyInput.handles(keyCode)` → native if the platform has an explicit mapping
- Otherwise → `java.awt.Robot`

**Key code space:**

| Range | Meaning |
|---|---|
| `0x0000–0xFFFF` | AWT VK codes (`KeyEvent.VK_*`) |
| `0x10000` (NATIVE_BASE) | Synthetic base; modifiers start here |
| `NATIVE_BASE + 1–9` | LeftControl, RightControl, LeftShift, RightShift, LeftAlt, RightAlt, LeftSuper, RightSuper, Menu |
| `NATIVE_BASE + 10–27` | ISO 102nd key (`<>`), NumpadEnter, ä, ö, ü, ß, ´, é, è, à, ù, ç, ñ, numpad operators |
| `0x20000` (NATIVE_CHARACTER_BASE) | Layout-independent Frontier character codes (see below) |

**Why scan codes?** DirectInput identifies keys by PS/2 hardware scan code, not VK code.
`java.awt.Robot` sends VK-based events that DirectInput may silently ignore. This is why modifier keys and EU special chars need native treatment.

**Key methods:**

| Method | Behaviour |
|---|---|
| `pressKey(code)` | keyDown + jitter delay + keyUp |
| `holdKey(code)` | keyDown only; modifier keys add a settling jitter so DirectInput sees them as held before the main key |
| `releaseKey(code)` | keyUp only |
| `pressAndHoldKey(code, ms)` | keyDown, sleep ms, keyUp |
| `pressKeyCombo(codes...)` | hold all in order, sleep 100ms, release in reverse |
| `enterText(text)` | per-char: `nativeKeyInput.typeChar(c)` first, falls back to Robot |

### `NativeKeyInputFactory`

Creates the appropriate `NativeKeyInput` based on OS. If native init fails, falls back to
`RobotFallback` (which merges L/R modifier variants to generic AWT VKs).

| Platform | Implementation | Mechanism |
|---|---|---|
| Windows | `WindowsNativeKeyInput` | `SendInput` via `user32.dll` (JNA) |
| Linux with X11 | `LinuxX11NativeKeyInput` | `XTestFakeKeyEvent` via `libXtst` (JNA) |
| Linux Wayland / Other | `RobotFallback` | `java.awt.Robot`, no L/R distinction |

### `WindowsNativeKeyInput`

Uses `KEYEVENTF_SCANCODE` with PS/2 Set-1 scan codes.

**Scan code resolution:**

1. `NATIVE_CHARACTER_BASE` codes → `VkKeyScanExW` +
   `MapVirtualKeyEx` with the foreground window's keyboard layout (handles AZERTY/QWERTZ letter remapping).
2. `NATIVE_BASE` synthetic codes → `SCAN_MAP` (static table).
3. Latin letters → `MapVirtualKeyEx` with foreground layout, fallback to `SCAN_MAP`.
4. Navigation cluster / others → `SCAN_MAP` directly.

Extended keys (right-side modifiers, navigation cluster, numpad `/`, numpad Enter, PrintScreen)
require `KEYEVENTF_EXTENDEDKEY` alongside `KEYEVENTF_SCANCODE` to produce the E0-prefix.

`typeChar(c)` uses `KEYEVENTF_UNICODE` - the only path that doesn't need a scan code.

**UIPI warning:** If Elite Dangerous is launched as Administrator (UAC shield on launcher)
and EliteIntel runs as a standard user, `SendInput` calls return 0 with `GetLastError=5`
(ERROR_ACCESS_DENIED) and all keystrokes are silently dropped. Startup diagnostics log this. Fix: remove "Run as administrator" from the ED launcher's compatibility settings.

Startup diagnostics (`logStartupDiagnostics`) run on every
`WindowsNativeKeyInput` construction and log JNA connectivity, keyboard layout, process elevation state, and a SendInput smoke test.

### `LinuxX11NativeKeyInput`

Uses `XTestFakeKeyEvent` (libXtst) to inject key events directly into the X input stream.

- `KEYSYM_MAP`: synthetic codes → X11 keysym values (from `keysymdef.h`).
- Keycodes are resolved at startup via `XKeysymToKeycode` and cached in `keycodeCache`.
- `typeChar(c)`: queries
  `XGetKeyboardMapping` to determine if Shift is needed for the character's level (level 0 = plain, level 1 = Shift), then injects accordingly. Falls back to Robot for AltGr or other multi-level chars.
- Gracefully degrades on Wayland (where `XOpenDisplay` returns null).

### `FrontierBindingKeyResolver`

Handles keys read from `.binds` files where Frontier serializes characters as named strings
(e.g. `Key_Semicolon`, `Key_LeftParenthesis`) or as literal Unicode characters (e.g. `Key_é`).

Named characters → `NATIVE_CHARACTER_BASE + char` code. Single non-ASCII Unicode suffix (e.g. `Key_ä`) →
`NATIVE_CHARACTER_BASE + char` code. Everything else → delegates to the standard `ELITE_TO_KEYPROCESSOR_MAP` in
`KeyBindingExecutor`.

---

## Part 2 - Bindings File Management

### File Discovery (`BindingsLoader`)

Reads `StartPreset.*.start` in the game's bindings directory to find the active preset name, then locates
`<presetName>.<version>.binds`. Falls back to the most recently modified
`.binds` file if the preset file is absent or the name cannot be resolved.

### `BindingsMonitor` (singleton)

Monitors the bindings directory via `WatchService` on a dedicated background thread.

On startup and on any `.binds` file create/modify event:

1. Re-resolves the active file via `BindingsLoader`.
2. Parses it with `KeyBindingsParser.parseBindings()`.
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

### `KeyBindingExecutor` (singleton)

Translates Frontier's key name strings (e.g. `"Key_LeftControl"`, `"Key_Ü"`) to
`KeyProcessor` int codes, then drives `KeyProcessor` to press them.

`ELITE_TO_KEYPROCESSOR_MAP` is built at class initialization:

- Reflection over all `KEY_*` fields of `KeyProcessor` (covers letters, digits, function keys, punctuation).
- Explicit overrides for keys the reflection cannot handle: `KEY_APPS`, `KEY_GRAVE`,
  `KEY_HASH` (UK physical position), German QWERTZ (`KEY_Ä`, `KEY_Ö`, `KEY_Ü`, `KEY_SS`, `KEY_ACUTE`), French AZERTY (
  `KEY_É`, `KEY_È`, `KEY_À`, `KEY_Ù`, `KEY_Ç`), Spanish (`KEY_Ñ`), numpad variants.
- All lookups are case-insensitive.
- `FrontierBindingKeyResolver` handles named-character and literal-Unicode edge cases.

Key execution:

- `executeTap(binding)` - always `pressKey` (ignores
  `binding.hold`); used by UI navigation steps where hold causes key-repeat overshoot.
- `executeBindingWithHold(binding, holdMs)` - respects `holdMs`, then `binding.hold`, then plain press.
- Modifiers are held first (in order), then the main key fires, then modifiers release in reverse. On exception, all keys are force-released.

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
7. Atomic write: `tmp` file → `Files.move(ATOMIC_MOVE)` with non-atomic fallback.

`assignKeyboardKey()` - plain key, no modifier.
`assignKeyboardKeyWithModifier()` - key + one supported keyboard modifier (thin wrapper).
`assignKeyboardKeyWithModifiers()` - key + a chord of one or more supported keyboard modifiers
(e.g. Left Ctrl + Left Shift), written as repeated `<Modifier>` nodes. Clearing a slot writes
`Device="{NoDevice}" Key=""`.

`BindingSaveResult` values: `SAVED`, `NO_CHANGE`, `STALE_FILE`, `UNKNOWN_KEY`,
`BINDING_NOT_FOUND`, `UNSUPPORTED_XML`, `KEY_OCCUPIED`, `WRITE_FAILED`.

**3. `BindingsApplyService`**

Pushes the approved working copy to the game directory:

1. Read working copy bytes.
2. Round-trip XML validation (strips BOM before parse).
3. Conflict check: reject if game file changed since the draft was created.
4. Backup current game file to `elite-intel/bindings/backups/` via `BindingsBackupService`.
5. Atomic write to game directory (temp + move).
6. `workingCopyRepo.markApplied()` updates the baseline hash to the newly written content.

### Conflict & Missing Binding Detection

**`BindingConflictRules`**

Inverted binding map → `keyCombo → [actionNames]`. For each
`Bindings.GameCommand`, check if another action shares the same key+modifier combo.

Safe overlaps are not flagged:

- Different vehicle states (ship / buggy / humanoid) - mutually exclusive in-game.
- Sub-state overlays (FreeCam, FSS, SAA, GalnetAudio) - only active inside a specific UI mode.

Dangerous pairs have curated descriptions; unknown conflicts get a generic humanized description. Results are diffed against the DB state so only newly-appearing conflicts are announced.

**`BindingGroupClassifier`**

Classifies a binding ID into a
`BindingGroup` enum value (SHIP_FLIGHT, COMBAT, UI_PANELS, MAPS, EXPLORATION, CAMERA, SRV, ON_FOOT, MISCELLANEOUS) by suffix/keyword matching. Used by the Bindings UI tab to group rows.

### Supporting Utilities

**`PreFtlChecks.preJumpCheck(status, message)`
** - runs before any FTL command (supercruise, hyperspace). Conditionally retracts hardpoints, landing gear, cargo scoop; turns off lights and night vision; docks fighter; sets speed to 100% - each step governed by a
`GlobalSettingsManager` toggle.

**`RoutePlotter.plotRoute(destination)`** - publishes a full galaxy-map navigation sequence:
open galaxy map → zoom in → navigate to search → type destination → arrow-down → Enter → Enter.

**`UiNavCommon`** - shared UI helpers: `close()` (handles open system/galaxy map, then
`UI_Back`); `prepToKnownUiPositionWhileInTheShipAtStation()` (three UI_Down steps).

**`KeyBindCheck.check()`
** - triggers missing/conflict detection and publishes voice announcements summarizing the counts.

**`EliteKeyboardKeys`
** - static allow-list of keyboard token strings the MVP editor can assign. Kept static so the UI dropdown can show keys that are not yet in the active file.

---

## `Bindings.GameCommand`

The authoritative registry of Elite Dangerous action names that EliteIntel commands and queries may invoke. Each entry holds the Frontier binding ID string (e.g.
`"GalaxyMapOpen"`,
`"DeployHardpointToggle"`). Command handlers reference these via
`Bindings.GameCommand.BINDING_GALAXY_MAP.getGameBinding()`.

> Note: some entries share the same binding ID (e.g. `BINDING_FOCUS_STATUS_PANEL` and
> `BINDING_FOCUS_INTERNAL_PANEL` both map to `"FocusRightPanel"`). This is intentional -
> the game uses one binding for multiple logical commands depending on context.

---

## Key Interfaces & Classes - Quick Reference

| Class | Role |
|---|---|
| `HandsService` | `ManagedService` entry point; starts/stops `BindingsMonitor` and `InputSequenceExecutor` |
| `GameInputSequenceEvent` | Public game input API; list of `GameInputStep`s |
| `GameInputStep` | One semantic input step (BINDING_TAP, BINDING_HOLD, RAW_KEY, TEXT, DELAY) |
| `InputSequenceExecutor` | Serializes `GameInputSequenceEvent`s through one worker thread |
| `KeyProcessor` | Low-level key dispatcher; routes to native or Robot |
| `NativeKeyInput` | Platform key injection interface |
| `WindowsNativeKeyInput` | `SendInput` (scan codes) via JNA |
| `LinuxX11NativeKeyInput` | `XTestFakeKeyEvent` (keysyms) via JNA |
| `KeyBindingExecutor` | Translates Frontier key names → `KeyProcessor` codes; drives execution |
| `FrontierBindingKeyResolver` | Handles named-char and literal-Unicode Frontier key names |
| `KeyBindingsParser` | Parses `.binds` XML → executable and read-only binding models |
| `BindingsLoader` | Finds the active `.binds` file via `StartPreset.*.start` |
| `BindingsMonitor` | Watches for `.binds` changes; holds the live bindings map |
| `BindingsWorkingCopyRepository` | Per-preset draft management with SHA-256 baseline tracking |
| `BindingsWriter` | Surgical text-based XML slot editor; atomic write |
| `BindingsApplyService` | Validate → backup → atomic apply to game directory |
| `BindingConflictRules` | Safe-overlap logic and curated dangerous-pair descriptions |
| `BindingGroupClassifier` | Classifies binding IDs into display groups |
| `EliteKeyboardKeys` | Static allow-list of assignable key tokens for the UI |
| `Bindings.GameCommand` | Enum of all game action names EliteIntel may invoke |

## Key Constants

| Constant | Value | Meaning |
|---|---|---|
| `KeyProcessor.NATIVE_BASE` | `0x10000` | Codes ≥ this go to `NativeKeyInput` not Robot |
| `KeyProcessor.NATIVE_CHARACTER_BASE` | `0x20000` | Layout-independent Frontier character codes |
| `InputSequenceExecutor.DEFAULT_POST_INPUT_DELAY_MIN_MS` | `99` | Random post-input delay lower bound |
| `InputSequenceExecutor.DEFAULT_POST_INPUT_DELAY_MAX_MS` | `201` | Random post-input delay upper bound |