# Elite Intel — Devices Package Specification

**Project:** Elite Intel
**Package:** `elite.intel.devices`
**Version:** Draft
**Status:** Not started — required before BindForge controller features and StarVizion refactor can be completed.

---

## 1. Overview

`elite.intel.devices` is shared infrastructure for Elite Intel that provides USB gaming device discovery, identity, and input event publishing to any feature that needs it. It is owned by no single feature — it is a platform-level package that multiple features consume.

### 1.1 Consumers

| Consumer | What it needs |
|---|---|
| `elite.intel.starvizion` | Live axis and button state for overlay visualization |
| `elite.intel.bindforge` | Device enumeration for Device Mapping, button capture for Bind Editor, live highlighting |
| `elite.intel.ui.view.settings.InputSettingsPanel` | Button state for push to talk detection |

### 1.2 Origin

The SDL3 device input code currently living in `elite.intel.starvizion.input` was built as a prototype to prove that SDL3 could work inside Elite Intel on both Windows and Linux. That proof of concept succeeded. The code now needs to be promoted from a StarVizion-specific prototype into proper shared infrastructure.

The refactor moves the SDL3 layer out of StarVizion and into `elite.intel.devices`. StarVizion then becomes a consumer of `elite.intel.devices` rather than the owner of the SDL3 layer. All other consumers follow the same pattern.

---

## 2. Package Structure

```
elite.intel.devices                    ← device registry, lifecycle, service entry point
elite.intel.devices.input              ← SDL3 polling, hardware communication
elite.intel.devices.events             ← device and input events published on EventBusManager
elite.intel.devices.model              ← Device, Axis, Button data models
```

---

## 3. Core Classes

### 3.1 `DeviceService` (`elite.intel.devices`)

The singleton entry point for all device operations. Replaces `SdlInputService` as the owner of the SDL3 lifecycle.

Responsibilities:
- Initialize and shut down SDL3
- Manage the device poll loop on a dedicated thread
- Maintain the registry of currently connected devices
- Publish device and input events on `EventBusManager`
- Expose `getConnectedDevices()` for snapshot queries

Key methods:
```java
public static DeviceService getInstance()
public void start()           // idempotent — safe to call multiple times
public void stop()
public boolean isAvailable()  // true once SDL3 initialized successfully
public List<Device> getConnectedDevices()  // thread-safe snapshot
```

### 3.2 `Device` (`elite.intel.devices.model`)

Replaces `SvDevice`. Represents a connected USB gaming device.

```java
public record Device(
    int id,           // SDL3 instance ID — session-scoped, reassigned on reconnect
    String name,      // SDL3 device name
    int axisCount,    // number of analog axes
    int buttonCount,  // number of buttons
    String usbPath,   // USB port path from SDL_GetJoystickPathForID — for duplicate VID/PID detection
    String guid       // SDL3 GUID — used for VID/PID extraction and .binds file correlation
)
```

### 3.3 `DeviceIdentity` (`elite.intel.devices.model`)

A resolved device identity — VID, PID, and the 8-character hex string used in `.binds` files for axis bindings.

```java
public record DeviceIdentity(
    String vid,           // vendor ID — 4 hex chars
    String pid,           // product ID — 4 hex chars
    String bindsHexId,    // VID+PID concatenated — matches Device= attribute in .binds axis bindings
    String usbPath        // USB port path for duplicate device differentiation
)
```

### 3.4 `ButtonInputMapper` (`elite.intel.devices.model`)

Translates between SDL3 button indices and Elite Dangerous `.binds` token format.

```java
public class ButtonInputMapper {
    public static String toBindsToken(int sdlButtonIndex)  // 0-based → "Joy_N" (1-based)
    public static int fromBindsToken(String token)          // "Joy_N" → 0-based index
    public static String axisToBindsToken(int sdlAxisIndex) // 0-based → "Joy_XAxis" etc.
}
```

Token mapping (subject to verification in Binds File Audit):

| SDL3 | Elite Token |
|---|---|
| Button 0 | `Joy_1` |
| Button 1 | `Joy_2` |
| Button N | `Joy_N+1` |
| Axis 0 | `Joy_XAxis` |
| Axis 1 | `Joy_YAxis` |
| Axis 2 | `Joy_ZAxis` |
| Axis 3 | `Joy_RXAxis` |
| Axis 4 | `Joy_RYAxis` |
| Axis 5 | `Joy_RZAxis` |

Exact mapping verified during Binds File Audit. See `BindForge_BindsFileAudit.md` Section 5.5.

---

## 4. Events

All events are published on `elite.intel.gameapi.EventBusManager` and follow the existing Elite Intel event conventions.

### 4.1 `DeviceConnectedEvent` (`elite.intel.devices.events`)

Published when a new device is detected by the poll loop.

```java
public record DeviceConnectedEvent(Device device) {}
```

Replaces `SvDeviceConnectedEvent`.

### 4.2 `DeviceDisconnectedEvent` (`elite.intel.devices.events`)

Published when a previously connected device is no longer detected.

```java
public record DeviceDisconnectedEvent(int deviceId) {}
```

Replaces `SvDeviceDisconnectedEvent`.

### 4.3 `DeviceButtonEvent` (`elite.intel.devices.events`)

Published on every button state transition — press and release. Not published continuously while held.

```java
public record DeviceButtonEvent(int deviceId, int buttonIndex, boolean pressed) {}
```

Replaces `SvButtonStateEvent`.

### 4.4 `DeviceAxisEvent` (`elite.intel.devices.events`)

Published when an axis value changes beyond the deadzone threshold.

```java
public record DeviceAxisEvent(int deviceId, int axisIndex, float value) {}
```

`value` range: -1.0 to +1.0 following SDL3 convention.

### 4.5 `DeviceServiceStateEvent` (`elite.intel.devices.events`)

Published when SDL3 initializes successfully or fails to initialize.

```java
public record DeviceServiceStateEvent(boolean available, String errorMessage) {}
```

---

## 5. Poll Loop

The poll loop runs on a dedicated platform thread named `"elite-intel-devices"` at approximately 60Hz. It:

1. Calls `SDL_PumpEvents()` at the start of each iteration
2. Enumerates connected joystick IDs via `SDL_GetJoysticks()`
3. Detects newly connected devices — opens them, reads identity, publishes `DeviceConnectedEvent`
4. Detects disconnected devices — closes handles, publishes `DeviceDisconnectedEvent`
5. For each open device polls axis values and button states
6. Publishes `DeviceAxisEvent` for axes that changed beyond threshold
7. Publishes `DeviceButtonEvent` for buttons that changed state
8. Sleeps for the remainder of the 16ms poll interval

The poll loop does not read keyboard state. See `dawntreader-docs/KEYBOARD_DEBUG.md` for the keyboard polling limitation.

---

## 6. SDL3 Initialization

SDL3 is initialized with:

```java
SDL_Init(SDL_INIT_JOYSTICK | SDL_INIT_GAMEPAD)
```

This is sufficient for joystick and gamepad input. It does not initialize the video subsystem. As documented in `KEYBOARD_DEBUG.md`, keyboard state via `SDL_GetKeyboardState()` requires `SDL_INIT_VIDEO` and is explicitly not supported in this package.

Native libraries are loaded from `distribution/native/lwjgl/` following the existing Elite Intel pattern for native library distribution.

---

## 7. Duplicate Device Detection

When two connected devices share the same VID and PID, `DeviceService` detects this condition and publishes a `DeviceDuplicateWarningEvent`:

```java
public record DeviceDuplicateWarningEvent(Device device1, Device device2) {}
```

The Elite Dangerous game itself cannot distinguish between two devices with the same VID/PID. BindForge uses the `usbPath` field from `Device` to differentiate them for display purposes only — in the `.binds` file they remain indistinguishable.

Consumers that receive this event should warn the user and suggest including the USB port in device naming. See `BindForge_Spec.md` Section 5.4.

---

## 8. Refactor Plan

### 8.1 What Moves

The following classes move from `elite.intel.starvizion.input` and `elite.intel.starvizion.model` into `elite.intel.devices`:

| Old class | New class | Notes |
|---|---|---|
| `SdlInputService` | `DeviceService` | Renamed, promoted to shared infrastructure |
| `SvDevice` | `Device` | Renamed, GUID and usbPath fields added |
| `SvDeviceConnectedEvent` | `DeviceConnectedEvent` | Renamed |
| `SvDeviceDisconnectedEvent` | `DeviceDisconnectedEvent` | Renamed |
| `SvButtonStateEvent` | `DeviceButtonEvent` | Renamed |

`SvKeyPressedEvent`, `KeyboardVizlet`, `CounterVizlet`, and `KeyboardSettingsDialog` are not moved — they are prototype-only classes that may be removed or kept as experimental code in StarVizion.

### 8.2 What StarVizion Keeps

After the refactor StarVizion retains its overlay and Vizlet code. It becomes a pure consumer of `elite.intel.devices` events. The `elite.intel.starvizion.input` package is removed or emptied. The `elite.intel.starvizion.event` package retains only StarVizion-specific events that are not relevant to other consumers.

### 8.3 PTT Update

The push to talk feature in `InputSettingsPanel` currently depends on `SdlInputService` and `SvButtonStateEvent` directly. After the refactor it subscribes to `DeviceButtonEvent` from `elite.intel.devices` instead. The PTT implementation record `dawntreader-docs/PTT_IMPLEMENTATION_RECORD.md` documents the current dependency surface — Section 8.3 of that document identifies the minimal dependency set that needs updating.

### 8.4 Refactor Order

1. Create `elite.intel.devices` package structure
2. Move and rename classes from StarVizion
3. Update StarVizion to consume `elite.intel.devices`
4. Update `InputSettingsPanel` to consume `elite.intel.devices`
5. Verify build and test on both Windows and Linux
6. Then build BindForge device features on top of `elite.intel.devices`

---

## 9. AV and Security Considerations

SDL3 joystick and gamepad input via `SDL_INIT_JOYSTICK | SDL_INIT_GAMEPAD` on Windows uses XInput and DirectInput — Microsoft-signed, well-known APIs. The SDL3 native library `SDL3.dll` was verified clean on VirusTotal with a score of 0/63 on June 9 2026. See `dawntreader-docs/` for the verification hash.

SDL3 does not install kernel hooks, does not inject into any process, and does not intercept keyboard or mouse input. It reads joystick and gamepad state only.

This package complies with Elite Intel's contribution requirements:
- JNI to a library available on both Windows and Linux ✅
- No Python dependency ✅
- No unsigned library ✅
- No AV risk from joystick/gamepad polling ✅
