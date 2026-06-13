# Keyboard & Counter Vizlet — Implementation Report

This documents the addition of two new StarVizion prototype Vizlets — **Keyboard** and
**Counter** — built on top of the existing Axes/Button Vizlet patterns and the existing SDL3
instance in `SdlInputService`.

## Goal

Determine whether SDL3 keyboard scanning via `SDL_GetKeyboardState()` works without
application focus on Windows and Linux, as a viability check for push-to-talk. The Counter
Vizlet provides the visual proof: if it increments while another application (e.g. Elite
Dangerous) has focus, global SDL3 keyboard polling is viable.

## New files created

- **`app/src/main/java/elite/intel/starvizion/event/SvKeyPressedEvent.java`**
  New event record: `SvKeyPressedEvent(int scancode, String keyName)`. Published on a
  key-down rising edge only. `keyName` is a ready-to-display string including held modifiers
  (e.g. `"Ctrl+F"`, `"Shift+A"`, `"Alt+Tab"`, or just `"Ctrl"` if a modifier key alone is
  pressed).

- **`app/src/main/java/elite/intel/starvizion/overlay/KeyboardVizlet.java`**
  Square (160x160 default), borderless, transparent, always-on-top `JWindow` matching the
  `VizletWindow` pattern.
  - Shows `"Waiting for input"` (i18n) by default.
  - On `SvKeyPressedEvent`, displays the key name and starts/restarts a 1-second
    `javax.swing.Timer` (`setRepeats(false)`) that reverts the display back to "Waiting for
    input" if no new key press arrives first.
  - Custom background/text colors (defaults: existing `BG_FILL`, white text), repainted with
    its own background+border fill so per-vizlet color overrides are visible.
  - Font auto-fits to the window via a shrink-to-fit loop (48pt down to 10pt, bold
    sans-serif).
  - Right-click menu: Settings, Lock, Close (default `VizletWindow` behavior, unchanged).
  - `closeVizlet()` overridden to stop the hold timer on teardown.

- **`app/src/main/java/elite/intel/starvizion/overlay/KeyboardSettingsDialog.java`**
  Modal-less settings dialog (mirrors `AxesSettingsDialog`/`ButtonSettingsDialog` styling via
  `AppTheme`): width/height spinners, background color chooser, text color chooser, Save/
  Cancel buttons that call `KeyboardVizlet.configure(...)` and `setSize(...)`.

- **`app/src/main/java/elite/intel/starvizion/overlay/CounterVizlet.java`**
  Square (160x160 default), borderless, transparent, always-on-top `JWindow`.
  - Displays `"Keys detected:"` label and a large bold running count, starting at `0`.
  - Increments on every `SvKeyPressedEvent` regardless of scancode (any key, any modifier).
  - No settings: overrides `hasSettings()` to return `false`, so the right-click menu shows
    only Lock and Close. `openSettings()` is a required-but-unused no-op.
  - Counter reset-to-zero on Deactivate is achieved naturally — the vizlet instance is
    discarded and a fresh `CounterVizlet` (count = 0) is created on the next Activate.

## Existing files modified

- **`app/src/main/java/elite/intel/starvizion/input/SdlInputService.java`**
  - New imports: `SvKeyPressedEvent`, `SDLKeyboard`, `SDLKeycode`, `SDLScancode`,
    `java.nio.ByteBuffer`.
  - New field `prevKeyState` (`boolean[]`, sized to `SDL_GetKeyboardState()`'s `numkeys`,
    cleared in `closeAllHandles()`).
  - New `pollKeyboard()` call added to the existing `sdlLoop()` poll iteration, right after
    joystick axis/button polling — **no new SDL context, no new thread**, same 60Hz loop.
  - `pollKeyboard()`: calls `SDL_GetKeyboardState()` + `SDL_GetModState()`, diffs against
    `prevKeyState`, and publishes `SvKeyPressedEvent` for each scancode transitioning from
    released to pressed.
  - New helper methods `buildKeyDisplayName()` / `scancodeDisplayName()`: build the
    "Ctrl+F"-style name, mapping the 8 modifier scancodes (L/R Ctrl/Shift/Alt/GUI) to short
    names ("Ctrl"/"Shift"/"Alt"/"Win") and suppressing a modifier's own name from being
    duplicated when that modifier key itself is the one pressed.
  - Class-level Javadoc updated to mention keyboard polling.

- **`app/src/main/java/elite/intel/starvizion/overlay/VizletWindow.java`**
  - Added overridable `protected boolean hasSettings() { return true; }`.
  - `installContextMenu()` now only adds the "Settings" menu item when `hasSettings()` is
    `true` (used by `CounterVizlet` to hide it).

- **`app/src/main/java/elite/intel/starvizion/StarVizionTabPanel.java`**
  - New imports for `KeyboardVizlet`, `CounterVizlet`.
  - New layout constants `KEYBOARD_DEFAULT_W` / `COUNTER_DEFAULT_W` (160 each).
  - New fields `keyboardVizlet`, `counterVizlet`.
  - `activate()`: spawns both new vizlets on a second row below Axes/Button
    (`row2Y = VIZLET_TOP_Y + AXES_DEFAULT_H + VIZLET_GAP`), right-aligned under the
    Axes+Button row (160+160+10 = 200+120+10, so the rows line up).
  - `deactivate()`: closes and nulls out both new vizlets alongside the existing ones.

- **`app/src/main/resources/i18n/gui.properties`**
  - Added: `starvizion.keyboard.waiting`, `starvizion.keyboard.settings.title`,
    `starvizion.keyboard.backgroundColor`, `starvizion.keyboard.textColor`,
    `starvizion.keyboard.width`, `starvizion.keyboard.height`,
    `starvizion.keyboard.chooseBackgroundColor`, `starvizion.keyboard.chooseTextColor`,
    `starvizion.counter.label`.
  - No other locale files touched — `MultiLingualTextProvider` falls back to the English
    (root) bundle for missing keys, consistent with how the Axes/Button settings strings are
    handled.

## Issues encountered

None. No new SDL3 init flags, libraries, or threads were required —
`SDL_INIT_JOYSTICK | SDL_INIT_GAMEPAD` already initializes the SDL events subsystem, which
SDL3 uses for keyboard state tracking, so `SDL_GetKeyboardState()` is callable from the
existing `sdlThread` poll loop unchanged. `pollKeyboard()` defensively no-ops if
`SDL_GetKeyboardState()` ever returns `null`.

## Build result

```
./gradlew shadowJar
BUILD SUCCESSFUL in 19s
7 actionable tasks: 3 executed, 4 up-to-date
```

`distribution/elite_intel.jar` rebuilt successfully (96,253,572 bytes).

## Open question for manual testing

The core unresolved question — whether `SDL_GetKeyboardState()` reports key presses while a
**different** window (e.g. Elite Dangerous) has OS input focus, on both Windows and Linux —
is a runtime/platform behavior that can't be verified by a build or unit test. Activate
StarVizion, focus another application, and watch the Counter Vizlet:

- **Counter increments while unfocused** → global SDL3 keyboard polling is viable for
  push-to-talk.
- **Counter stays frozen while unfocused** → SDL3 keyboard state is focus-gated on that
  platform, and an alternative (e.g. a global hook) would be needed.
