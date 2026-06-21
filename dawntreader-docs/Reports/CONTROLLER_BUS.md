# Controller Bus

## What it is

`GameControllerBus` (`elite.intel.eventbus.GameControllerBus`) is a small, dedicated
Guava `EventBus`, kept separate from the main `EventBusManager` so that keystroke
dispatch into the game is never blocked by — and never blocks — STT/TTS/LLM/journal
traffic. It is a synchronous message-passing pipe with `publish` / `register` /
`unregister`.

The word "controller" here means **"the thing that controls the game"** — i.e. it
dispatches synthetic keyboard input into Elite Dangerous on the player's behalf. It
has nothing to do with a physical USB game controller, joystick, or gamepad.

## What it currently does

It provides an ordered, single-threaded pipeline that turns a routed voice command
into actual keypresses inside Elite Dangerous:

1. A command handler decides "press binding X" / "hold key Y" / "type this text" and
   publishes a `GameInputSequenceEvent` (a list of `GameInputStep`s — `BINDING_TAP`,
   `BINDING_HOLD`, `RAW_KEY`, `TEXT`, `DELAY`) onto the bus.
2. `InputSequenceExecutor` (registered on the bus) serializes every published
   sequence through one worker thread, so command handlers can never interleave
   keystrokes, and adds small randomized post-input delays between input-producing
   steps.
3. For binding-based steps, it resolves the Elite Dangerous binding name (e.g.
   `Key_LeftControl`) to a physical key via `KeyBindingExecutor`, which maps Elite's
   key-naming conventions (including locale quirks — UK `#`, German umlauts, numpad
   enter, etc.) onto `KeyProcessor` key codes.
4. `KeyProcessor` (with `NativeKeyInput` / `WindowsNativeKeyInput` /
   `LinuxX11NativeKeyInput`) performs the actual synthetic key press/hold/release,
   built on Java AWT `Robot` with native Win32/X11 fallbacks for keys Robot can't
   express (left/right modifier distinction, numpad enter, etc.).
5. To know *which* physical key corresponds to *which* in-game action,
   `BindingsMonitor` watches the player's `*.binds` directory (the same XML config
   files Elite Dangerous itself writes when a player customizes controls) and
   `KeyBindingsParser` parses them into a `Map<String, KeyBinding>` that the executor
   consults at run time. This means the app automatically respects whatever the
   player has remapped in-game.
6. `HandsService` (a `ManagedService`) wires this all together — it owns the
   `BindingsMonitor` and `InputSequenceExecutor` and starts/stops them with the app.
7. `FireGroups` is a small helper that uses the bus to cycle the ship through fire
   groups: it repeatedly taps the "cycle next fire group" binding and polls `Status`
   to confirm the change landed.
8. Dozens of command handlers under
   `elite.intel.ai.brain.actions.handlers.commands.*` publish
   `GameInputSequenceEvent`s onto this bus as the final step of routing a voice
   command (e.g. "deploy landing gear", "open galaxy map", "cycle fire group alpha")
   into an actual in-game action.

## Classes involved

- `elite.intel.eventbus.GameControllerBus` — the dedicated synchronous event bus
- `elite.intel.ai.hands.events.GameInputSequenceEvent` — typed, immutable list of
  input steps
- `elite.intel.ai.hands.events.GameInputStep` — one semantic input step (binding tap,
  binding hold, raw key, text entry, or delay)
- `elite.intel.ai.hands.InputSequenceExecutor` — subscribes to the bus, serializes
  and executes sequences on a single worker thread
- `elite.intel.ai.hands.KeyBindingExecutor` — translates Elite Dangerous binding
  names into `KeyProcessor` key codes and presses/holds/releases them
- `elite.intel.ai.hands.KeyProcessor` / `NativeKeyInput` / `WindowsNativeKeyInput` /
  `LinuxX11NativeKeyInput` — the low-level synthetic-input layer (AWT `Robot` plus
  native Win32/X11 fallbacks)
- `elite.intel.ai.hands.BindingsMonitor` — watches the `*.binds` config directory for
  changes and keeps the live bindings map up to date
- `elite.intel.ai.hands.KeyBindingsParser` — parses Elite Dangerous `.binds` XML
  files into executable (and read-only diagnostic) binding models
- `elite.intel.ai.hands.HandsService` — `ManagedService` that owns and starts/stops
  the `BindingsMonitor` and `InputSequenceExecutor`
- `elite.intel.gameapi.FireGroups` — helper that cycles fire groups via the bus and
  confirms the change against session `Status`
- Command handlers in `elite.intel.ai.brain.actions.handlers.commands.*` — the
  callers that publish `GameInputSequenceEvent`s as the action side-effect of routed
  voice commands

## What it does not do

- **It does not read live USB/HID device input.** There is no joystick, HOTAS,
  gamepad, or game-controller-hardware library anywhere in this code path, and
  nothing polls physical controller state.
- **It only reads static configuration files.** `BindingsMonitor` /
  `KeyBindingsParser` read the `*.binds` XML files Elite Dangerous writes to disk
  when a player customizes their controls (under
  `Frontier Developments\...\Options\Bindings`). That's the *only* source of "what
  key does this action map to" — there is no live device polling involved.
- **Non-keyboard bindings are deliberately excluded from execution.** When a
  `.binds` file assigns an action to a HOTAS, joystick, mouse, or gamepad (visible in
  the file as a raw device id like `044F0422`), `KeyBindingsParser` surfaces that only
  in a read-only diagnostic model for the UI's Bindings tab — it is explicitly never
  treated as something the app can press, since the app can only synthesize keyboard
  input.
- **It never touches the game process or memory.** All synthetic input goes through
  OS-level injection (AWT `Robot` / native Win32 or X11 calls), consistent with the
  project's hard constraint of no memory reading, no overlay injection, and no
  game-client modification.
