# elite.intel.devices — Package Creation & StarVizion Refactor

Implements the `elite.intel.devices` package described in
`dawntreader-docs/Specifications/EliteIntel_Devices_Spec.md`, and refactors
`elite.intel.starvizion` to consume it instead of owning the SDL3 layer.

## New package: `elite.intel.devices`

### `elite.intel.devices`
- **`DeviceService`** — singleton replacing `SdlInputService`. Owns the SDL3 lifecycle (init,
  `~60Hz` poll loop on a dedicated `elite-intel-devices` platform thread, shutdown) and is shared
  infrastructure for StarVizion, BindForge, and push-to-talk. Preserves the LWJGL3
  native-library-path redirection (`org.lwjgl.system.SharedLibraryExtractPath` /
  `org.lwjgl.librarypath` pointed at `<dist>/native/lwjgl`) from the original implementation.
  Adds VID/PID-based duplicate-device detection (`SDL_GetJoystickVendorForID` /
  `SDL_GetJoystickProductForID`), publishing `DeviceDuplicateWarningEvent` when two connected
  devices share a VID/PID.

### `elite.intel.devices.model`
- **`Device`** (record) — replaces `SvDevice`; adds `guid` (SDL3 GUID, 32-hex-char string via
  `SDL_GetJoystickGUIDForID` + `SDL_GUIDToString`) and `usbPath` (via
  `SDL_GetJoystickPathForID`) fields alongside the existing `id`/`name`/`axisCount`/`buttonCount`.
- **`DeviceIdentity`** (record) — `vid`/`pid`/`bindsHexId`/`usbPath`. Created per spec as a bare
  record; no extraction/factory method was in scope for this task — left for future BindForge
  work.
- **`ButtonInputMapper`** — static helpers translating between SDL3 button/axis indices and
  Elite Dangerous `.binds` token format (`Joy_N` for buttons, `Joy_XAxis`..`Joy_RZAxis` for axes).

### `elite.intel.devices.events`
- **`DeviceConnectedEvent`** (replaces `SvDeviceConnectedEvent`)
- **`DeviceDisconnectedEvent`** (replaces `SvDeviceDisconnectedEvent`)
- **`DeviceButtonEvent`** (replaces `SvButtonStateEvent`)
- **`DeviceAxisEvent`** (replaces `SvAxisStateEvent`)
- **`DeviceServiceStateEvent`** (replaces `SvServiceStateEvent`)
- **`DeviceDuplicateWarningEvent`** — new; published by `DeviceService` when two connected
  devices share a VID/PID, carrying both `Device` records so consumers can warn the user and use
  `usbPath` to tell them apart.

### `elite.intel.devices.input`
Not created as an empty directory — no classes were specified for it in this task, and git does
not track empty directories. `DeviceService` itself lives directly in `elite.intel.devices` per
the class list. Noted as a minor spec inconsistency, not blocking.

## Refactored: `elite.intel.starvizion`

All Vizlet/dialog classes now subscribe to `elite.intel.devices.events.*` and use
`elite.intel.devices.model.Device` / `elite.intel.devices.DeviceService` instead of the old
`elite.intel.starvizion.event.Sv*` / `elite.intel.starvizion.model.SvDevice` /
`elite.intel.starvizion.input.SdlInputService`:

- **`StarVizionTabPanel`** — owns a `DeviceService` instead of `SdlInputService`; subscribes to
  `DeviceServiceStateEvent` instead of `SvServiceStateEvent`.
- **`AxesVizlet`** — subscribes to `DeviceAxisEvent` / `DeviceDisconnectedEvent`; `configure()`
  takes a `Device`.
- **`ButtonVizlet`** — subscribes to `DeviceButtonEvent` / `DeviceDisconnectedEvent`;
  `configure()` takes a `Device`.
- **`AxesSettingsDialog`** / **`ButtonSettingsDialog`** — device combo populated from
  `DeviceService.getInstance().getConnectedDevices()`, typed as `JComboBox<Device>`.
- **`InputSettingsPanel`** (`elite.intel.ui.view.settings`) — push-to-talk controller/button
  selection now driven by `DeviceService`, `DeviceConnectedEvent`, `DeviceDisconnectedEvent`, and
  `DeviceButtonEvent`.

### Left unchanged (per spec section 8.1 — not part of the device-layer move)
- `SvAxis`, `SvButton` — pure UI dropdown item types for axis/button selection in the settings
  dialogs.
- `SvKeyPressedEvent`, `KeyboardVizlet`, `CounterVizlet`, `KeyboardSettingsDialog` — keyboard
  scancode capture was not part of `SdlInputService`'s replacement scope. These remain
  structurally intact and compile fine, but `KeyboardVizlet`/`CounterVizlet` will receive no more
  `SvKeyPressedEvent`s once `SdlInputService`'s keyboard polling is gone — consistent with the
  spec's "may be removed or kept as experimental" framing for this code path.

## Removed

`elite.intel.starvizion.input.SdlInputService` and the now-empty `starvizion.input` package, plus
the superseded `elite.intel.starvizion` model/event classes:
`SvDevice`, `SvDeviceConnectedEvent`, `SvDeviceDisconnectedEvent`, `SvButtonStateEvent`,
`SvAxisStateEvent`, `SvServiceStateEvent`. Confirmed via repo-wide grep (main + test sources) that
nothing else referenced these types before deletion.

## Build

```
./gradlew shadowJar
```

**BUILD SUCCESSFUL** (7 actionable tasks: 3 executed, 4 up-to-date).
