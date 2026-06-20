# `elite.intel.devices` - Developer Reference

The devices package owns joystick, HOTAS, gamepad, and pedal input. It polls SDL3 on a single dedicated platform thread and publishes connect/disconnect, axis, and button events on the DeviceBus. It is shared infrastructure - StarVizion, BindForge, and push-to-talk all consume these events rather than managing their own SDL3 context.

Read-only device access only. This package never writes to the game, the
`.binds` file, or any other file.

---

## Architecture

```
DeviceService.start()
    │
    │  Thread.ofPlatform("elite-intel-devices")
    ▼
pollLoop()
    │
    ├─ configureLwjglNativePath()   ← must run before any LWJGL class is touched
    ├─ SDL_Init(JOYSTICK | GAMEPAD)
    │
    │  loop every 16ms (~60 Hz):
    │
    ├─ SDL_PumpEvents()             ← flush SDL event queue (required for state updates)
    ├─ SDL_GetJoysticks() → IntBuffer of current JoystickIDs
    │
    ├─ diff against knownIds set:
    │     new id  → onDeviceAdded()   → DeviceConnectedEvent
    │     gone id → onDeviceRemoved() → DeviceDisconnectedEvent
    │
    └─ for each open handle:
          pollJoystick()
            ├─ SDL_GetJoystickAxis()   → DeviceAxisEvent (on change only)
            └─ SDL_GetJoystickButton() → DeviceButtonEvent (on state transition only)
```

---

## 1. Startup and SDL3 Initialization

### LWJGL3 Native Library Path

`configureLwjglNativePath()` must run before any LWJGL3 class is instantiated. It sets two "Dynamic" LWJGL3 properties (read on every
`.get()` call) to point at `native/lwjgl/` relative to the application JAR:

| Property | Role |
|---|---|
| `org.lwjgl.system.SharedLibraryExtractPath` | Where LWJGL extracts native DLLs |
| `org.lwjgl.librarypath` | Where LWJGL searches for already-extracted DLLs |

Redirecting away from `%TEMP%` avoids three Windows failure modes:

- Antivirus holds an exclusive lock on newly-written temp files longer than LWJGL3's `lock()` wait.
- Usernames containing non-ASCII characters trigger JDK-8195129 (`LoadLibraryA` cannot handle them).
- Group Policy or `noexec` mount flags prevent executing binaries from temp directories.

If the `native/lwjgl/` directory does not exist, the method falls back silently to LWJGL's default temp extraction.

### SDL3 Init

`SDL_Init(SDL_INIT_JOYSTICK | SDL_INIT_GAMEPAD)` is called after the native path is configured. Failure publishes `DeviceServiceStateEvent(available=false,
errorMessage)` and exits the poll thread. `UnsatisfiedLinkError`,
`ExceptionInInitializerError`, and
`NoClassDefFoundError` are all caught - the last of these is thrown on second access when the static initializer of an LWJGL class already failed.

Success publishes `DeviceServiceStateEvent(available=true, null)`.

---

## 2. Poll Loop

The loop runs on the `elite-intel-devices` platform thread (not a virtual thread - SDL3 is not coroutine-safe).
`running` is an `AtomicBoolean`; calling
`stop()` sets it to false and joins the thread with a 3-second timeout.

**Device enumeration** runs every tick via `SDL_GetJoysticks()` which returns an
`IntBuffer` of SDL3 JoystickIDs. The diff against `knownIds` catches hot-plug and hot-unplug events.

**State polling** is delta-only:

- Axes: previous raw `short` stored in
  `prevAxes[deviceId][axisIndex]`. Published only when the raw value changes; no deadzone threshold - all movement is reported and consumers apply their own deadzone if needed.
- Buttons: previous `boolean` stored in
  `prevButtons[deviceId][buttonIndex]`. Published on both press and release transitions. Never published while held
  (unlike keyboard repeat events).

`POLL_INTERVAL_MS = 16` gives approximately 60 Hz polling rate. The `openHandles`,
`prevAxes`, `prevButtons`, and
`vidPidByDevice` maps are accessed exclusively from the poll thread; no synchronization is needed.

`connectedDevices` is a `CopyOnWriteArrayList` so
`getConnectedDevices()` can be called safely from any thread (e.g., the EDT for UI population).

---

## 3. Events

All events are published on `DeviceBus`.

| Event | Published when | Key fields |
|---|---|---|
| `DeviceServiceStateEvent` | SDL3 init succeeds or fails | `available`, `errorMessage` |
| `DeviceConnectedEvent` | New device detected by poll diff | `device` (Device record) |
| `DeviceDisconnectedEvent` | Known device no longer enumerated | `deviceId` (SDL3 instance ID) |
| `DeviceAxisEvent` | Axis raw value changes | `deviceId`, `axisIndex`, `value` [-1.0, 1.0] |
| `DeviceButtonEvent` | Button state transitions | `deviceId`, `buttonIndex`, `pressed` |
| `DeviceDuplicateWarningEvent` | Two connected devices share VID/PID | `device1`, `device2` |

`DeviceServiceStateEvent` is published from the poll thread before any other events. Subscribers that touch Swing components must switch to the EDT.

The
`deviceId` in all events is the SDL3 JoystickID - a session-scoped integer reassigned on reconnect. Do not persist it across sessions; use the GUID or VID/PID for durable identity.

---

## 4. Data Model

### `Device` (record)

| Field | Type | Meaning |
|---|---|---|
| `id` | `int` | SDL3 instance ID (session-scoped; reassigned on reconnect) |
| `name` | `String` | SDL3 device name; fallback: `"Joystick <id>"` |
| `axisCount` | `int` | Number of analog axes reported by SDL3 |
| `buttonCount` | `int` | Number of buttons reported by SDL3 |
| `usbPath` | `String` | USB port path from `SDL_GetJoystickPathForID`; empty string if unavailable |
| `guid` | `String` | 32-char hex SDL3 GUID (see below) |

### `DeviceIdentity` (record)

A resolved identity suitable for `.binds` file correlation. Not produced by
`DeviceService` directly - derived by consumers from a `Device`.

| Field | Type | Meaning |
|---|---|---|
| `vid` | `String` | Vendor ID, 4 hex chars |
| `pid` | `String` | Product ID, 4 hex chars |
| `bindsHexId` | `String` | `vid + pid` - matches the `Device=` attribute in `.binds` axis binding XML |
| `usbPath` | `String` | USB port path; used to differentiate duplicate VID/PID devices |

### SDL3 GUID

`readGuid(id)` calls `SDL_GetJoystickGUIDForID` + `SDL_GUIDToString` via a stack-allocated
`MemoryStack` to produce the 32-character hex string. The GUID encodes VID, PID, and bus type; extracting bytes 8-11 (little-endian) yields the VID and PID used for
`.binds` correlation. Returns `""` on failure (logged at DEBUG level).

---

## 5. Axis Normalization

SDL3 reports axis values as `short` in the range [−32768, 32767].

```
normalized = Math.max(-1f, raw * AXIS_SCALE)   where AXIS_SCALE = 1.0f / 32767.0f
```

- Maximum positive: `32767 / 32767 = 1.0` exactly.
- Maximum negative: `−32768 / 32767 ≈ −1.00003` → clamped to `−1.0`.

No upper clamp is needed. `DeviceAxisEvent.value` is always in [−1.0, 1.0].

---

## 6. Duplicate Device Detection

After opening each device,
`checkForDuplicate()` compares its VID/PID against all already-opened devices. A match publishes
`DeviceDuplicateWarningEvent`. This matters because Elite Dangerous uses VID/PID to identify devices in
`.binds` files and cannot distinguish two identical physical devices (e.g., two of the same HOTAS). The
`usbPath` field (
`SDL_GetJoystickPathForID`) allows UI consumers to display which USB port each physical unit is plugged into, but the game itself has no mechanism to separate them.

---

## 7. `.binds` Token Translation (`ButtonInputMapper`)

Static utility class. Translates between SDL3 0-based indices and Elite Dangerous `.binds` token strings.

**Buttons:**

| Direction | Conversion |
|---|---|
| SDL3 → `.binds` | `toBindsToken(idx)` → `"Joy_" + (idx + 1)` |
| `.binds` → SDL3 | `fromBindsToken("Joy_N")` → `N - 1` |

Elite Dangerous uses 1-based `Joy_N` tokens; SDL3 uses 0-based indices.

**Axes:**

| SDL3 index | `.binds` token |
|---|---|
| 0 | `Joy_XAxis` |
| 1 | `Joy_YAxis` |
| 2 | `Joy_ZAxis` |
| 3 | `Joy_RXAxis` |
| 4 | `Joy_RYAxis` |
| 5 | `Joy_RZAxis` |

`axisToBindsToken(idx)` throws `IllegalArgumentException` for index ≥ 6 (no standard `.binds` name beyond RZ).

---

## Key Classes - Quick Reference

| Class | Role |
|---|---|
| `DeviceService` | Singleton; SDL3 poll loop; publishes all device events |
| `model/Device` | Immutable record for a connected device (id, name, axes, buttons, usbPath, guid) |
| `model/DeviceIdentity` | Resolved VID/PID + bindsHexId for `.binds` correlation |
| `model/ButtonInputMapper` | SDL3 index ↔ Elite `.binds` token translation |
| `events/DeviceServiceStateEvent` | SDL3 init result (available + optional error) |
| `events/DeviceConnectedEvent` | New device detected |
| `events/DeviceDisconnectedEvent` | Device removed |
| `events/DeviceAxisEvent` | Axis change (delta only) |
| `events/DeviceButtonEvent` | Button press or release |
| `events/DeviceDuplicateWarningEvent` | Two devices share VID/PID |

## Key Constants

| Constant | Value | Location |
|---|---|---|
| `POLL_INTERVAL_MS` | `16` ms (~60 Hz) | `DeviceService` |
| `AXIS_SCALE` | `1.0f / 32767.0f` | `DeviceService` |
| `stop()` join timeout | `3000` ms | `DeviceService` |
| GUID string length | 32 hex chars | `DeviceService.readGuid` |