# `elite.intel.ai.hands` - Developer Reference

The hands package owns **keystroke
execution**: sending hardware-level key events to Elite Dangerous in a way that DirectInput actually receives, resolved from the live bindings map at the moment each step runs.

Reading, watching, editing and applying the game's `.binds` file, and the rules that judge what is in it, live in
`elite.intel.bindforge` - see [its reference](../../bindforge/PACKAGE.md). This package reads the bindings through
`bindforge.io.BindingsMonitor` and never touches the file itself.

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

---

## Keystroke Execution

### `GameInputSequenceEvent` / `GameInputStep`

The public API. Every command handler that needs to press keys builds a
`GameInputSequenceEvent` and publishes it on `GameControllerBus` (not the main
`EventBusManager`).

```java
GameControllerBus.publish(GameInputSequenceEvent.of(
        GameInputStep.bindingTap(BINDING_GALAXY_MAP.getGameBinding()),
        GameInputStep.delay(3000),
        GameInputStep.rawKey(KeyProcessor.KEY_ENTER, 0, 0)  // no modifier, tap not hold
));
```

| Step type | Factory method | Notes |
|---|---|---|
| `BINDING_TAP` | `bindingTap(bindingId)` | Presses the binding as the `.binds` file configures it - a long-press action holds |
| `BINDING_FORCED_TAP` | `bindingForcedTap(bindingId)` | Always taps, ignoring the binding's `hold` flag - for callers whose own contract is a tap |
| `BINDING_HOLD` | `bindingHold(bindingId, holdMs)` | Holds the main key for the given duration |
| `BINDING_DOWN` / `BINDING_UP` | `bindingDown(bindingId)` / `bindingUp(bindingId)` | Press and release as separate steps, when an external signal decides the release moment |
| `RAW_KEY` | `rawKey(keyCode, modCode, holdMs)` | Bypasses binding lookup; uses `KeyProcessor` codes directly |
| `TEXT` | `text(string)` | Types characters via `KeyProcessor.enterText`; handles non-QWERTY layouts |
| `DELAY` | `delay(ms)` | Explicit pause; does not trigger the default post-input delay |
| `WAIT_UNTIL` | `waitUntil(description, condition, timeoutMs)` | Blocks until the game reports the expected state (polled every 50 ms), or until the timeout, which is logged and not fatal. Use instead of `delay` whenever the wait is for the game to catch up, so slow hardware is not left behind by a guessed duration |

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

### Supporting Utilities (`gameapi.inputs`)

**`PreFtlChecks.preJumpCheck(status, message)`
** - runs before any FTL command (supercruise, hyperspace). Conditionally retracts hardpoints, landing gear, cargo scoop; turns off lights and night vision; docks fighter; sets speed to 100% - each step governed by a
`GlobalSettingsManager` toggle.

**`RoutePlotter.plotRoute(destination)`** - publishes a full galaxy-map navigation sequence:
wait for any open map to close → open galaxy map → wait for `GuiFocus` to report it open → settle → zoom in → navigate to search → type destination → Enter → `UI_Right` → `UI_Select` → yaw to grab focus. The two waits are
`WAIT_UNTIL` steps rather than fixed delays: the map takes seconds to appear on slower hardware, and every step after it is worthless if it fires while the map is still opening.

**`UiNavCommon`** - shared UI helpers: `close()` (handles open system/galaxy map, then
`UI_Back`); `prepToKnownUiPositionWhileInTheShipAtStation()` (three UI_Down steps).
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
| `GameInputStep` | One semantic input step (BINDING_TAP, BINDING_FORCED_TAP, BINDING_HOLD, BINDING_DOWN, BINDING_UP, RAW_KEY, TEXT, DELAY, WAIT_UNTIL) |
| `InputSequenceExecutor` | Serializes `GameInputSequenceEvent`s through one worker thread |
| `KeyProcessor` | Low-level key dispatcher; routes to native or Robot |
| `NativeKeyInput` | Platform key injection interface |
| `WindowsNativeKeyInput` | `SendInput` (scan codes) via JNA |
| `LinuxX11NativeKeyInput` | `XTestFakeKeyEvent` (keysyms) via JNA |
| `KeyBindingExecutor` | Translates Frontier key names → `KeyProcessor` codes; drives execution |
| `FrontierBindingKeyResolver` | Handles named-char and literal-Unicode Frontier key names |
| `Bindings.GameCommand` | Enum of all game action names EliteIntel may invoke |

## Key Constants

| Constant | Value | Meaning |
|---|---|---|
| `KeyProcessor.NATIVE_BASE` | `0x10000` | Codes ≥ this go to `NativeKeyInput` not Robot |
| `KeyProcessor.NATIVE_CHARACTER_BASE` | `0x20000` | Layout-independent Frontier character codes |
| `InputSequenceExecutor.DEFAULT_POST_INPUT_DELAY_MIN_MS` | `99` | Random post-input delay lower bound |
| `InputSequenceExecutor.DEFAULT_POST_INPUT_DELAY_MAX_MS` | `201` | Random post-input delay upper bound |