# Keyboard / Counter Vizlet — Why the counter never increments

## Summary

The wiring described in the task — `SDL_PumpEvents()` before `pollKeyboard()`,
`SvKeyPressedEvent` publication, and the two Vizlet subscriptions — is **all correct** in
the current code. The reason the Counter Vizlet never increments is upstream of all of
that: **`SDL_Init()` is called without `SDL_INIT_VIDEO`**, so SDL3 never creates a video
device / window and never pumps OS keyboard messages into its internal keyboard-state
buffer. `SDL_GetKeyboardState()` therefore reports every scancode as "not pressed", on
every poll, forever — `pollKeyboard()` runs correctly but has nothing to detect.

---

## 1. `SDL_PumpEvents()` ordering — confirmed correct

`SdlInputService.sdlLoop()` (`app/src/main/java/elite/intel/starvizion/input/SdlInputService.java:111-141`):

```java
while (running.get()) {
    SDLEvents.SDL_PumpEvents();          // line 112 — called every iteration

    Set<Integer> currentIds = enumerateJoystickIds();
    ...
    for (Map.Entry<Integer, Long> entry : openHandles.entrySet()) {
        pollJoystick(entry.getKey(), entry.getValue());
    }

    pollKeyboard();                      // line 137 — called after SDL_PumpEvents()

    Thread.sleep(POLL_INTERVAL_MS);
}
```

`SDL_PumpEvents()` (line 112) runs unconditionally at the top of every loop iteration,
before joystick polling and before `pollKeyboard()` (line 137). This part of the report's
premise is correct — the call ordering is not the bug.

---

## 2. `SvKeyPressedEvent` publication — confirmed correct (but never reached with `pressed == true`)

`pollKeyboard()` (`SdlInputService.java:294-311`):

```java
private void pollKeyboard() {
    ByteBuffer state = SDLKeyboard.SDL_GetKeyboardState();
    if (state == null) return;

    int numKeys = state.remaining();
    if (prevKeyState == null || prevKeyState.length != numKeys) {
        prevKeyState = new boolean[numKeys];
    }

    short modState = SDLKeyboard.SDL_GetModState();
    for (int i = 0; i < numKeys; i++) {
        boolean pressed = state.get(i) != 0;
        if (pressed && !prevKeyState[i]) {
            EventBusManager.publish(new SvKeyPressedEvent(i, buildKeyDisplayName(i, modState)));
        }
        prevKeyState[i] = pressed;
    }
}
```

`EventBusManager.publish(...)` (`elite.intel.gameapi.EventBusManager`) is a thin
synchronous wrapper around a Guava `EventBus.post(...)` — correct, and used the same way
everywhere else in the codebase.

The publish call itself is correctly placed and correctly constructed. The problem is that
`state.get(i)` is **always `0`** for every `i`, every iteration (see Root Cause below), so
`pressed` is always `false`, `pressed && !prevKeyState[i]` is never `true`, and
`SvKeyPressedEvent` is **never published** — not because of a wiring bug, but because SDL
never reports a key as pressed in the first place.

(A secondary, currently-unreachable defensive branch: if `SDL_GetKeyboardState()` ever
returns `null` instead of an all-zero buffer, `pollKeyboard()` returns immediately at line
296 with the same net effect — no event ever published.)

---

## 3. Vizlet subscriptions — confirmed correct

Both Vizlets declare an `@Subscribe` handler for `SvKeyPressedEvent`:

- `KeyboardVizlet.onKeyPressed(SvKeyPressedEvent)` — `KeyboardVizlet.java:44-51`
- `CounterVizlet.onKeyPressed(SvKeyPressedEvent)` — `CounterVizlet.java:32-36`

Both classes extend `VizletWindow`, whose `showVizlet()` registers the instance on the bus:

```java
// VizletWindow.java:46-50
public void showVizlet() {
    EventBusManager.register(this);
    setVisible(true);
}
```

`StarVizionTabPanel.activate()` constructs both Vizlets and calls `showVizlet()` on each
(`StarVizionTabPanel.java:137-143`), so by the time the SDL thread starts polling, both
instances are registered on `EventBusManager`'s singleton `EventBus` and would receive
`SvKeyPressedEvent` if it were ever posted. **This part of the pipeline is correct.**

---

## Root cause: `SDL_Init()` is missing `SDL_INIT_VIDEO`

`initSdl()` (`SdlInputService.java:190-211`):

```java
boolean ok = SDLInit.SDL_Init(SDL_INIT_JOYSTICK | SDL_INIT_GAMEPAD);
```

Only `SDL_INIT_JOYSTICK | SDL_INIT_GAMEPAD` are requested. `SDL_INIT_EVENTS` is implicitly
added by SDL3 for *any* subsystem init, but **`SDL_INIT_VIDEO` is not, and is not
requested here**.

`SDL_GetKeyboardState()` returns a pointer to SDL's internal keyboard-state array. That
array is populated exclusively via `SDL_SendKeyboardKey()`, which is called from the
**video backend's** event-pump implementation (`SDL_VideoDevice->PumpEvents`) as it
translates OS window messages (e.g. Win32 `WM_KEYDOWN`/`WM_KEYUP`, X11 `KeyPress`/
`KeyRelease`) into SDL events. `SDL_PumpEvents()` only invokes that video-backend pump if
a video device exists — i.e. only if `SDL_INIT_VIDEO` (or a subsystem that implies it) was
initialized successfully.

Without `SDL_INIT_VIDEO`:
- No SDL video device is created.
- `SDL_PumpEvents()` still runs (it's a real, successful call — that part of the original
  premise holds), but the branch that pumps keyboard/window events is skipped entirely
  because there is no video device to pump.
- `SDL_GetKeyboardState()`'s backing buffer is therefore never updated from its initial
  all-zero state — `state.get(i)` is `0` for every scancode, on every poll, indefinitely.

This is consistent with the observed symptom: the Counter Vizlet is correctly subscribed
and would correctly increment on `SvKeyPressedEvent`, but that event is structurally never
produced because `pollKeyboard()` never observes a `0 -> 1` transition for any scancode.

It also explains why this regression is *specific to the new keyboard/counter Vizlets*:
joystick/gamepad polling (`pollJoystick()`, used by the existing `AxesVizlet` /
`ButtonVizlet`, which are reported working) goes through `SDL_GetJoystickAxis()` /
`SDL_GetJoystickButton()` against handles opened via `SDL_OpenJoystick()` — on
Windows/Linux this path does not require a video device, so `SDL_INIT_JOYSTICK |
SDL_INIT_GAMEPAD` alone is sufficient for it. Keyboard state tracking has a hard
dependency on the video subsystem that joystick/gamepad polling does not.

`KEYBOARD_VIZLET_REPORT.md` (the implementation report for these two Vizlets) explicitly
states the assumption that drove this:

> No new SDL3 init flags, libraries, or threads were required — `SDL_INIT_JOYSTICK |
> SDL_INIT_GAMEPAD` already initializes the SDL events subsystem, which SDL3 uses for
> keyboard state tracking, so `SDL_GetKeyboardState()` is callable from the existing
> `sdlThread` poll loop unchanged.

`SDL_GetKeyboardState()` is indeed *callable* without `SDL_INIT_VIDEO` (it doesn't crash
or return `null` for that reason) — but "callable" and "populated with real key-state
data" are different things, and that's the gap.

---

## Secondary open question (flagged but not yet reachable)

`KEYBOARD_VIZLET_REPORT.md` already flags an "Open question for manual testing": whether
`SDL_GetKeyboardState()` reflects key presses while a *different* window (e.g. Elite
Dangerous) has OS input focus.

That question is currently moot — the counter doesn't even increment with the app
focused — but it will become the next blocker once `SDL_INIT_VIDEO` is added. SDL's
default keyboard-state tracking is normally scoped to **SDL's own window(s)** receiving
focused input; an unfocused SDL window typically will *not* see `WM_KEYDOWN`/`KeyPress`
for keys typed into Elite Dangerous's window. Achieving true global (out-of-focus) key
detection on top of `SDL_INIT_VIDEO` would likely additionally require either:

- A hidden/utility SDL window plus OS-level raw input registration (e.g. Windows
  `RegisterRawInputDevices` with `RIDEV_INPUTSINK`, which SDL3 can opt into via a hint such
  as `SDL_HINT_WINDOWS_RAW_KEYBOARD` on Windows), or
- An equivalent platform-specific global hook on Linux (e.g. XInput2 raw events on X11).

This is a separate, larger investigation from the immediate "counter doesn't increment"
bug and is **out of scope for this report**, but worth flagging before assuming that
adding `SDL_INIT_VIDEO` alone makes the Counter Vizlet usable for its stated push-to-talk
viability goal.

---

## What would need to change (not implemented here, per task scope)

This report is diagnostic only, per the request. The minimal change to make the Counter
Vizlet increment *while the app/SDL has focus* would be adding `SDL_INIT_VIDEO` to the
`SDL_Init()` call in `initSdl()` (`SdlInputService.java:194`). Whether that alone is
sufficient for the Vizlet's actual goal (detecting keys while Elite Dangerous has focus)
depends on the secondary question above and would need separate manual verification on
both Windows and Linux.
