# AUDIT_v2_FULL — DualVirpilDawnTreader.4.2.binds: Complete Structural Extraction

**Source:** `dawntreader-docs/Actual Game Files/PlayerBinds/DualVirpilDawnTreader.4.2.binds`
**Preset:** DualVirpilDawnTreader — MajorVersion 4, MinorVersion 2
**Root attribute:** `KeyboardLayout` = `en-US`
**Total XML lines:** 1939
**Method:** Direct XML structural read — every child node and attribute extracted verbatim.
**Purpose:** Schema specimen — mapping every possible element type and property pattern so a data model and editor can handle any .binds file.

---

## Column Key (applies to all tables)

| Column | Meaning |
|---|---|
| XML Element | Element tag name as it appears in the XML |
| Type | AXIS / BUTTON / STANDALONE SETTING / Other |
| Binding Device | `<Binding Device="">` attribute (AXIS only) |
| Binding Key | `<Binding Key="">` attribute (AXIS only) |
| Primary Device | `<Primary Device="">` attribute (BUTTON only) |
| Primary Key | `<Primary Key="">` attribute (BUTTON only) |
| Primary Modifier(s) | All `<Modifier>` children inside `<Primary>`, as `Device/Key`; multiple joined by ` + ` |
| Secondary Device | `<Secondary Device="">` attribute (BUTTON only) |
| Secondary Key | `<Secondary Key="">` attribute (BUTTON only) |
| Secondary Modifier(s) | All `<Modifier>` children inside `<Secondary>`, as `Device/Key` |
| Inverted | `<Inverted Value="">` (AXIS only) |
| Deadzone | `<Deadzone Value="">` (AXIS only) |
| ToggleOn | `<ToggleOn Value="">` where present |
| Setting Value | Bare `Value=""` attribute on root element (STANDALONE SETTING only) |

- `{NoDevice}` — Device attribute is literally `{NoDevice}` in XML
- `(empty)` — Key attribute is literally `""` in XML
- `—` — property not applicable to this element type, or child node absent

---

# Game Mode 1: General Controls

## §1.1 Interface Mode

| XML Element | Type | Binding Device | Binding Key | Primary Device | Primary Key | Primary Modifier(s) | Secondary Device | Secondary Key | Secondary Modifier(s) | Inverted | Deadzone | ToggleOn | Setting Value |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `UI_Up` | BUTTON | — | — | Keyboard | Key_E | — | RVWAP | Joy_9 | — | — | — | — | — |
| `UI_Down` | BUTTON | — | — | Keyboard | Key_D | — | RVWAP | Joy_11 | — | — | — | — | — |
| `UI_Left` | BUTTON | — | — | Keyboard | Key_S | — | RVWAP | Joy_10 | — | — | — | — | — |
| `UI_Right` | BUTTON | — | — | Keyboard | Key_F | — | RVWAP | Joy_12 | — | — | — | — | — |
| `UI_Select` | BUTTON | — | — | Keyboard | Key_Space | — | RVWAP | Joy_4 | — | — | — | — | — |
| `UI_Back` | BUTTON | — | — | Keyboard | Key_Backspace | — | RVWAP | Joy_13 | — | — | — | — | — |
| `UI_Toggle` | BUTTON | — | — | Keyboard | Key_T | Keyboard/Key_LeftControl | RVWAP | Joy_7 | — | — | — | — | — |
| `CycleNextPanel` | BUTTON | — | — | Keyboard | Key_R | — | RVWAP | Joy_18 | — | — | — | — | — |
| `CyclePreviousPanel` | BUTTON | — | — | Keyboard | Key_W | — | RVWAP | Joy_16 | — | — | — | — | — |
| `CycleNextPage` | BUTTON | — | — | Keyboard | Key_N | — | RVWAP | Joy_17 | — | — | — | — | — |
| `CyclePreviousPage` | BUTTON | — | — | Keyboard | Key_P | Keyboard/Key_LeftControl | RVWAP | Joy_15 | — | — | — | — | — |

## §1.2 Galaxy Map

| XML Element | Type | Binding Device | Binding Key | Primary Device | Primary Key | Primary Modifier(s) | Secondary Device | Secondary Key | Secondary Modifier(s) | Inverted | Deadzone | ToggleOn | Setting Value |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `CamPitchAxis` | AXIS | RVWAP | Joy_YAxis | — | — | — | — | — | — | 1 | 0.00000000 | — | — |
| `CamPitchUp` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `CamPitchDown` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `CamYawAxis` | AXIS | RVWAP | Joy_XAxis | — | — | — | — | — | — | 1 | 0.00000000 | — | — |
| `CamYawLeft` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `CamYawRight` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `CamTranslateYAxis` | AXIS | LVWAP | Joy_YAxis | — | — | — | — | — | — | 1 | 0.00000000 | — | — |
| `CamTranslateForward` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `CamTranslateBackward` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `CamTranslateXAxis` | AXIS | LVWAP | Joy_XAxis | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `CamTranslateLeft` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `CamTranslateRight` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `CamTranslateZAxis` | AXIS | LVWAP | Joy_ZAxis | — | — | — | — | — | — | 1 | 0.00000000 | — | — |
| `CamTranslateUp` | BUTTON | — | — | {NoDevice} | (empty) | — | T-Rudder | Neg_Joy_YAxis | — | — | — | — | — |
| `CamTranslateDown` | BUTTON | — | — | {NoDevice} | (empty) | — | T-Rudder | Neg_Joy_XAxis | — | — | — | — | — |
| `CamZoomAxis` | AXIS | RVWAP | Joy_ZAxis | — | — | — | — | — | — | 1 | 0.00000000 | — | — |
| `CamZoomIn` | BUTTON | — | — | Keyboard | Key_Comma | — | {NoDevice} | (empty) | — | — | — | — | — |
| `CamZoomOut` | BUTTON | — | — | Keyboard | Key_Period | — | {NoDevice} | (empty) | — | — | — | — | — |
| `CamTranslateZHold` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | 0 | — |
| `GalaxyMapHome` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |

## §1.3 Camera Suite

| XML Element | Type | Binding Device | Binding Key | Primary Device | Primary Key | Primary Modifier(s) | Secondary Device | Secondary Key | Secondary Modifier(s) | Inverted | Deadzone | ToggleOn | Setting Value |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `PhotoCameraToggle` | BUTTON | — | — | Keyboard | Key_SemiColon | — | RVWAP | Joy_6 | — | — | — | — | — |
| `PhotoCameraToggle_Buggy` | BUTTON | — | — | Keyboard | Key_SemiColon | — | RVWAP | Joy_6 | — | — | — | — | — |
| `PhotoCameraToggle_Humanoid` | BUTTON | — | — | Keyboard | Key_SemiColon | — | RVWAP | Joy_6 | — | — | — | — | — |
| `VanityCameraScrollLeft` | BUTTON | — | — | Keyboard | Key_P | Keyboard/Key_LeftControl | RVWAP | Joy_29 | — | — | — | — | — |
| `VanityCameraScrollRight` | BUTTON | — | — | Keyboard | Key_N | Keyboard/Key_LeftControl | RVWAP | Joy_30 | — | — | — | — | — |
| `VanityCameraOne` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `VanityCameraTwo` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `VanityCameraThree` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `VanityCameraFour` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `VanityCameraFive` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `VanityCameraSix` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `VanityCameraSeven` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `VanityCameraEight` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `VanityCameraNine` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `VanityCameraTen` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |

## §1.4 Free Camera

| XML Element | Type | Binding Device | Binding Key | Primary Device | Primary Key | Primary Modifier(s) | Secondary Device | Secondary Key | Secondary Modifier(s) | Inverted | Deadzone | ToggleOn | Setting Value |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `ToggleFreeCam` | BUTTON | — | — | Keyboard | Key_C | Keyboard/Key_LeftControl | RVWAP | Joy_7 | — | — | — | — | — |
| `FreeCamToggleHUD` | BUTTON | — | — | {NoDevice} | (empty) | — | RVWAP | Joy_13 | — | — | — | — | — |
| `FreeCamSpeedInc` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `FreeCamSpeedDec` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `MoveFreeCamY` | AXIS | LVWAP | Joy_YAxis | — | — | — | — | — | — | 1 | 0.00000000 | — | — |
| `ThrottleRangeFreeCam` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | (empty) |
| `ToggleReverseThrottleInputFreeCam` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | 1 | — |
| `MoveFreeCamForward` | BUTTON | — | — | Keyboard | Key_E | — | {NoDevice} | (empty) | — | — | — | — | — |
| `MoveFreeCamBackwards` | BUTTON | — | — | Keyboard | Key_D | — | {NoDevice} | (empty) | — | — | — | — | — |
| `MoveFreeCamX` | AXIS | LVWAP | Joy_XAxis | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `MoveFreeCamRight` | BUTTON | — | — | Keyboard | Key_F | — | {NoDevice} | (empty) | — | — | — | — | — |
| `MoveFreeCamLeft` | BUTTON | — | — | Keyboard | Key_S | — | {NoDevice} | (empty) | — | — | — | — | — |
| `MoveFreeCamZ` | AXIS | LVWAP | Joy_ZAxis | — | — | — | — | — | — | 1 | 0.00000000 | — | — |
| `MoveFreeCamUpAxis` | AXIS | {NoDevice} | (empty) | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `MoveFreeCamDownAxis` | AXIS | {NoDevice} | (empty) | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `MoveFreeCamUp` | BUTTON | — | — | Keyboard | Key_A | — | {NoDevice} | (empty) | — | — | — | — | — |
| `MoveFreeCamDown` | BUTTON | — | — | Keyboard | Key_Z | — | {NoDevice} | (empty) | — | — | — | — | — |
| `PitchCameraMouse` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | (empty) |
| `YawCameraMouse` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | (empty) |
| `PitchCamera` | AXIS | RVWAP | Joy_YAxis | — | — | — | — | — | — | 1 | 0.00000000 | — | — |
| `FreeCamMouseSensitivity` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 5.00000000 |
| `FreeCamMouseYDecay` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 1 |
| `PitchCameraUp` | BUTTON | — | — | Keyboard | Key_3 | — | {NoDevice} | (empty) | — | — | — | — | — |
| `PitchCameraDown` | BUTTON | — | — | Keyboard | Key_C | — | {NoDevice} | (empty) | — | — | — | — | — |
| `YawCamera` | AXIS | RVWAP | Joy_XAxis | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `FreeCamMouseXDecay` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 1 |
| `YawCameraLeft` | BUTTON | — | — | Keyboard | Key_X | — | {NoDevice} | (empty) | — | — | — | — | — |
| `YawCameraRight` | BUTTON | — | — | Keyboard | Key_V | — | {NoDevice} | (empty) | — | — | — | — | — |
| `RollCamera` | AXIS | RVWAP | Joy_ZAxis | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `RollCameraLeft` | BUTTON | — | — | Keyboard | Key_W | — | {NoDevice} | (empty) | — | — | — | — | — |
| `RollCameraRight` | BUTTON | — | — | Keyboard | Key_R | — | {NoDevice} | (empty) | — | — | — | — | — |
| `ToggleRotationLock` | BUTTON | — | — | {NoDevice} | (empty) | — | RVWAP | Joy_29 | — | — | — | — | — |
| `FixCameraRelativeToggle` | BUTTON | — | — | Keyboard | Key_T | Keyboard/Key_LeftShift + Keyboard/Key_LeftControl | RVWAP | Joy_30 | — | — | — | — | — |
| `FixCameraWorldToggle` | BUTTON | — | — | {NoDevice} | (empty) | — | RVWAP | Joy_28 | — | — | — | — | — |
| `QuitCamera` | BUTTON | — | — | {NoDevice} | (empty) | — | RVWAP | Joy_6 | — | — | — | — | — |
| `ToggleAdvanceMode` | BUTTON | — | — | {NoDevice} | (empty) | — | LVWAP | Joy_13 | — | — | — | — | — |
| `FreeCamZoomIn` | BUTTON | — | — | {NoDevice} | (empty) | — | LVWAP | Joy_15 | — | — | — | — | — |
| `FreeCamZoomOut` | BUTTON | — | — | {NoDevice} | (empty) | — | LVWAP | Joy_17 | — | — | — | — | — |
| `FStopDec` | BUTTON | — | — | {NoDevice} | (empty) | — | RVWAP | Joy_22 | — | — | — | — | — |
| `FStopInc` | BUTTON | — | — | {NoDevice} | (empty) | — | RVWAP | Joy_21 | — | — | — | — | — |

## §1.5 Holo-Me

| XML Element | Type | Binding Device | Binding Key | Primary Device | Primary Key | Primary Modifier(s) | Secondary Device | Secondary Key | Secondary Modifier(s) | Inverted | Deadzone | ToggleOn | Setting Value |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `CommanderCreator_Undo` | BUTTON | — | — | Keyboard | Key_Z | Keyboard/Key_LeftControl | {NoDevice} | (empty) | — | — | — | — | — |
| `CommanderCreator_Redo` | BUTTON | — | — | Keyboard | Key_Z | Keyboard/Key_LeftControl + Keyboard/Key_LeftShift | {NoDevice} | (empty) | — | — | — | — | — |
| `CommanderCreator_Rotation_MouseToggle` | BUTTON | — | — | Keyboard | Key_M | — | {NoDevice} | (empty) | — | — | — | — | — |
| `CommanderCreator_Rotation` | AXIS | {NoDevice} | (empty) | — | — | — | — | — | — | 1 | 0.00000000 | — | — |

## §1.6 Playlist

| XML Element | Type | Binding Device | Binding Key | Primary Device | Primary Key | Primary Modifier(s) | Secondary Device | Secondary Key | Secondary Modifier(s) | Inverted | Deadzone | ToggleOn | Setting Value |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `GalnetAudio_Play_Pause` | BUTTON | — | — | Keyboard | Key_PlayPause | — | {NoDevice} | (empty) | — | — | — | — | — |
| `GalnetAudio_SkipForward` | BUTTON | — | — | Keyboard | Key_NextTrack | — | {NoDevice} | (empty) | — | — | — | — | — |
| `GalnetAudio_SkipBackward` | BUTTON | — | — | Keyboard | Key_PrevTrack | — | {NoDevice} | (empty) | — | — | — | — | — |
| `GalnetAudio_ClearQueue` | BUTTON | — | — | Keyboard | Key_Q | — | {NoDevice} | (empty) | — | — | — | — | — |

## §1.7 Store Camera

| XML Element | Type | Binding Device | Binding Key | Primary Device | Primary Key | Primary Modifier(s) | Secondary Device | Secondary Key | Secondary Modifier(s) | Inverted | Deadzone | ToggleOn | Setting Value |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `StoreEnableRotation` | BUTTON | — | — | LVWAP | Joy_4 | — | {NoDevice} | (empty) | — | — | — | — | — |
| `StorePitchCamera` | AXIS | RVWAP | Joy_YAxis | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `StoreYawCamera` | AXIS | RVWAP | Joy_XAxis | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `StoreCamZoomIn` | BUTTON | — | — | LVWAP | Joy_21 | — | {NoDevice} | (empty) | — | — | — | — | — |
| `StoreCamZoomOut` | BUTTON | — | — | LVWAP | Joy_22 | — | {NoDevice} | (empty) | — | — | — | — | — |
| `StoreToggle` | BUTTON | — | — | LVWAP | Joy_13 | — | {NoDevice} | (empty) | — | — | — | — | — |

## §1.8 System Colonisation Facility Placement

| XML Element | Type | Binding Device | Binding Key | Primary Device | Primary Key | Primary Modifier(s) | Secondary Device | Secondary Key | Secondary Modifier(s) | Inverted | Deadzone | ToggleOn | Setting Value |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `PlaceSettlement` | BUTTON | — | — | Keyboard | Key_T | — | RVWAP | Joy_13 | — | — | — | — | — |
| `ChangeConstructionOption` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `RotateSettlement` | AXIS | RVWAP | Joy_ZAxis | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `RotateSettlementLeft` | BUTTON | — | — | Keyboard | Key_W | — | {NoDevice} | (empty) | — | — | — | — | — |
| `RotateSettlementRight` | BUTTON | — | — | Keyboard | Key_R | — | {NoDevice} | (empty) | — | — | — | — | — |
| `ExitSettlementPlacementCamera` | BUTTON | — | — | LVWAP | Joy_6 | — | RVWAP | Joy_6 | — | — | — | — | — |
| `MovePlacementCamY` | AXIS | LVWAP | Joy_YAxis | — | — | — | — | — | — | 1 | 0.00000000 | — | — |
| `MovePlacementCamForward` | BUTTON | — | — | Keyboard | Key_E | — | {NoDevice} | (empty) | — | — | — | — | — |
| `MovePlacementCamBackwards` | BUTTON | — | — | Keyboard | Key_D | — | {NoDevice} | (empty) | — | — | — | — | — |
| `MovePlacementCamX` | AXIS | LVWAP | Joy_XAxis | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `MovePlacementCamRight` | BUTTON | — | — | Keyboard | Key_F | — | {NoDevice} | (empty) | — | — | — | — | — |
| `MovePlacementCamLeft` | BUTTON | — | — | Keyboard | Key_S | — | {NoDevice} | (empty) | — | — | — | — | — |
| `MovePlacementCamZ` | AXIS | LVWAP | Joy_ZAxis | — | — | — | — | — | — | 1 | 0.00000000 | — | — |
| `MovePlacementCamUp` | BUTTON | — | — | Keyboard | Key_A | — | {NoDevice} | (empty) | — | — | — | — | — |
| `MovePlacementCamDown` | BUTTON | — | — | Keyboard | Key_Z | — | {NoDevice} | (empty) | — | — | — | — | — |
| `MovePlacementCamUpAxis` | AXIS | {NoDevice} | (empty) | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `MovePlacementCamDownAxis` | AXIS | {NoDevice} | (empty) | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `PitchPlacementCamera` | AXIS | RVWAP | Joy_YAxis | — | — | — | — | — | — | 1 | 0.00000000 | — | — |
| `PitchPlacementCameraUp` | BUTTON | — | — | Keyboard | Key_3 | — | {NoDevice} | (empty) | — | — | — | — | — |
| `PitchPlacementCameraDown` | BUTTON | — | — | Keyboard | Key_C | — | {NoDevice} | (empty) | — | — | — | — | — |
| `YawPlacementCamera` | AXIS | RVWAP | Joy_XAxis | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `YawPlacementCameraRight` | BUTTON | — | — | Keyboard | Key_V | — | {NoDevice} | (empty) | — | — | — | — | — |
| `YawPlacementCameraLeft` | BUTTON | — | — | Keyboard | Key_X | — | {NoDevice} | (empty) | — | — | — | — | — |
| `PlacementCamSpeedInc` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `PlacementCamSpeedDec` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `PlacementCamMouseSensitivity` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 4.00000000 |
| `PlacementCamMouseXDecay` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 1 |
| `PlacementCamMouseYDecay` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 1 |

---

# Game Mode 2: Ship Controls

## §2.1 Mouse Controls

| XML Element | Type | Binding Device | Binding Key | Primary Device | Primary Key | Primary Modifier(s) | Secondary Device | Secondary Key | Secondary Modifier(s) | Inverted | Deadzone | ToggleOn | Setting Value |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `MouseXMode` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | Bindings_MouseYaw |
| `MouseXDecay` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 0 |
| `MouseYMode` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | Bindings_MousePitch |
| `MouseYDecay` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 0 |
| `MouseReset` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `MouseSensitivity` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 1.52687681 |
| `MouseDecayRate` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 6.12779999 |
| `MouseDeadzone` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 0.02313905 |
| `MouseLinearity` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 2.03253651 |
| `MouseGUI` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 1 |
| `BlockMouseDecay` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | 0 | — |

*Note: `MouseGUI` appears twice in the XML (lines 15 and 471) with Value="1" both times. This is a duplicate element — both instances are recorded here as a single entry since the values are identical. See Schema Findings.*

## §2.2 Flight Rotation

| XML Element | Type | Binding Device | Binding Key | Primary Device | Primary Key | Primary Modifier(s) | Secondary Device | Secondary Key | Secondary Modifier(s) | Inverted | Deadzone | ToggleOn | Setting Value |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `YawAxisRaw` | AXIS | RVWAP | Joy_XAxis | — | — | — | — | — | — | 0 | 0.05725000 | — | — |
| `YawLeftButton` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `YawRightButton` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `YawToRollMode` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | Bindings_YawIntoRollNone |
| `YawToRollSensitivity` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 0.40000001 |
| `YawToRollMode_FAOff` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | (empty) |
| `YawToRollButton` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | 0 | — |
| `RollAxisRaw` | AXIS | RVWAP | Joy_ZAxis | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `RollLeftButton` | BUTTON | — | — | Keyboard | Key_W | — | {NoDevice} | (empty) | — | — | — | — | — |
| `RollRightButton` | BUTTON | — | — | Keyboard | Key_R | — | {NoDevice} | (empty) | — | — | — | — | — |

## §2.3 Flight Thrust

| XML Element | Type | Binding Device | Binding Key | Primary Device | Primary Key | Primary Modifier(s) | Secondary Device | Secondary Key | Secondary Modifier(s) | Inverted | Deadzone | ToggleOn | Setting Value |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `PitchAxisRaw` | AXIS | RVWAP | Joy_YAxis | — | — | — | — | — | — | 1 | 0.05725000 | — | — |
| `PitchUpButton` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `PitchDownButton` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `LateralThrustRaw` | AXIS | LVWAP | Joy_XAxis | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `LeftThrustButton` | BUTTON | — | — | Keyboard | Key_S | — | {NoDevice} | (empty) | — | — | — | — | — |
| `RightThrustButton` | BUTTON | — | — | Keyboard | Key_F | — | {NoDevice} | (empty) | — | — | — | — | — |
| `VerticalThrustRaw` | AXIS | LVWAP | Joy_ZAxis | — | — | — | — | — | — | 1 | 0.00000000 | — | — |
| `UpThrustButton` | BUTTON | — | — | Keyboard | Key_A | — | T-Rudder | Neg_Joy_YAxis | — | — | — | — | — |
| `DownThrustButton` | BUTTON | — | — | Keyboard | Key_Z | — | T-Rudder | Neg_Joy_XAxis | — | — | — | — | — |
| `AheadThrust` | AXIS | {NoDevice} | (empty) | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `ForwardThrustButton` | BUTTON | — | — | Keyboard | Key_E | — | {NoDevice} | (empty) | — | — | — | — | — |
| `BackwardThrustButton` | BUTTON | — | — | Keyboard | Key_D | — | {NoDevice} | (empty) | — | — | — | — | — |

## §2.4 Alternate Flight Controls

| XML Element | Type | Binding Device | Binding Key | Primary Device | Primary Key | Primary Modifier(s) | Secondary Device | Secondary Key | Secondary Modifier(s) | Inverted | Deadzone | ToggleOn | Setting Value |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `UseAlternateFlightValuesToggle` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | 1 | — |
| `YawAxisAlternate` | AXIS | {NoDevice} | (empty) | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `RollAxisAlternate` | AXIS | {NoDevice} | (empty) | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `PitchAxisAlternate` | AXIS | {NoDevice} | (empty) | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `LateralThrustAlternate` | AXIS | {NoDevice} | (empty) | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `VerticalThrustAlternate` | AXIS | {NoDevice} | (empty) | — | — | — | — | — | — | 0 | 0.00000000 | — | — |

## §2.5 Flight Throttle

| XML Element | Type | Binding Device | Binding Key | Primary Device | Primary Key | Primary Modifier(s) | Secondary Device | Secondary Key | Secondary Modifier(s) | Inverted | Deadzone | ToggleOn | Setting Value |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `ThrottleAxis` | AXIS | LVWAP | Joy_YAxis | — | — | — | — | — | — | 1 | 0.00000000 | — | — |
| `ThrottleRange` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | (empty) |
| `ToggleReverseThrottleInput` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | 1 | — |
| `ForwardKey` | BUTTON | — | — | Keyboard | Key_E | Keyboard/Key_LeftShift | {NoDevice} | (empty) | — | — | — | — | — |
| `BackwardKey` | BUTTON | — | — | Keyboard | Key_D | Keyboard/Key_LeftShift | {NoDevice} | (empty) | — | — | — | — | — |
| `ThrottleIncrement` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 0.00000000 |
| `SetSpeedMinus100` | BUTTON | — | — | Keyboard | Key_Numpad_9 | — | {NoDevice} | (empty) | — | — | — | — | — |
| `SetSpeedMinus75` | BUTTON | — | — | Keyboard | Key_Numpad_6 | — | {NoDevice} | (empty) | — | — | — | — | — |
| `SetSpeedMinus50` | BUTTON | — | — | Keyboard | Key_Numpad_3 | — | {NoDevice} | (empty) | — | — | — | — | — |
| `SetSpeedMinus25` | BUTTON | — | — | Keyboard | Key_Numpad_Decimal | — | {NoDevice} | (empty) | — | — | — | — | — |
| `SetSpeedZero` | BUTTON | — | — | Keyboard | Key_Numpad_5 | — | {NoDevice} | (empty) | — | — | — | — | — |
| `SetSpeed25` | BUTTON | — | — | Keyboard | Key_Numpad_0 | — | {NoDevice} | (empty) | — | — | — | — | — |
| `SetSpeed50` | BUTTON | — | — | Keyboard | Key_Numpad_1 | — | {NoDevice} | (empty) | — | — | — | — | — |
| `SetSpeed75` | BUTTON | — | — | Keyboard | Key_Numpad_4 | — | {NoDevice} | (empty) | — | — | — | — | — |
| `SetSpeed100` | BUTTON | — | — | Keyboard | Key_Numpad_7 | — | {NoDevice} | (empty) | — | — | — | — | — |

## §2.6 Flight Landing Overrides

| XML Element | Type | Binding Device | Binding Key | Primary Device | Primary Key | Primary Modifier(s) | Secondary Device | Secondary Key | Secondary Modifier(s) | Inverted | Deadzone | ToggleOn | Setting Value |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `YawAxis_Landing` | AXIS | {NoDevice} | (empty) | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `YawLeftButton_Landing` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `YawRightButton_Landing` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `YawToRollMode_Landing` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | (empty) |
| `PitchAxis_Landing` | AXIS | {NoDevice} | (empty) | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `PitchUpButton_Landing` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `PitchDownButton_Landing` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `RollAxis_Landing` | AXIS | {NoDevice} | (empty) | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `RollLeftButton_Landing` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `RollRightButton_Landing` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `LateralThrust_Landing` | AXIS | {NoDevice} | (empty) | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `LeftThrustButton_Landing` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `RightThrustButton_Landing` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `VerticalThrust_Landing` | AXIS | {NoDevice} | (empty) | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `UpThrustButton_Landing` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `DownThrustButton_Landing` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `AheadThrust_Landing` | AXIS | {NoDevice} | (empty) | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `ForwardThrustButton_Landing` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `BackwardThrustButton_Landing` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |

## §2.7 Flight Miscellaneous

| XML Element | Type | Binding Device | Binding Key | Primary Device | Primary Key | Primary Modifier(s) | Secondary Device | Secondary Key | Secondary Modifier(s) | Inverted | Deadzone | ToggleOn | Setting Value |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `ToggleFlightAssist` | BUTTON | — | — | Keyboard | Key_Numpad_2 | — | RVWAP | Joy_23 | — | — | — | 1 | — |
| `UseBoostJuice` | BUTTON | — | — | Keyboard | Key_B | — | LVWAP | Joy_24 | — | — | — | — | — |
| `HyperSuperCombination` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `Supercruise` | BUTTON | — | — | Keyboard | Key_L | — | LVWAP | Joy_27 | — | — | — | — | — |
| `Hyperspace` | BUTTON | — | — | Keyboard | Key_O | — | LVWAP | Joy_25 | — | — | — | — | — |
| `DisableRotationCorrectToggle` | BUTTON | — | — | Keyboard | Key_Numpad_Multiply | — | RVWAP | Joy_24 | — | — | — | 1 | — |
| `OrbitLinesToggle` | BUTTON | — | — | Keyboard | Key_Equals | — | RVWAP | Joy_27 | — | — | — | — | — |

## §2.8 Targeting

| XML Element | Type | Binding Device | Binding Key | Primary Device | Primary Key | Primary Modifier(s) | Secondary Device | Secondary Key | Secondary Modifier(s) | Inverted | Deadzone | ToggleOn | Setting Value |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `SelectTarget` | BUTTON | — | — | Keyboard | Key_T | — | RVWAP | Joy_13 | — | — | — | — | — |
| `CycleNextTarget` | BUTTON | — | — | Keyboard | Key_G | — | LVWAP | Joy_12 | — | — | — | — | — |
| `CyclePreviousTarget` | BUTTON | — | — | {NoDevice} | (empty) | — | LVWAP | Joy_10 | — | — | — | — | — |
| `SelectHighestThreat` | BUTTON | — | — | Keyboard | Key_V | — | LVWAP | Joy_8 | — | — | — | — | — |
| `CycleNextHostileTarget` | BUTTON | — | — | Keyboard | Key_I | — | LVWAP | Joy_9 | — | — | — | — | — |
| `CyclePreviousHostileTarget` | BUTTON | — | — | Keyboard | Key_P | — | LVWAP | Joy_11 | — | — | — | — | — |
| `TargetWingman0` | BUTTON | — | — | Keyboard | Key_7 | — | RVWAP | Joy_16 | — | — | — | — | — |
| `TargetWingman1` | BUTTON | — | — | Keyboard | Key_8 | — | RVWAP | Joy_15 | — | — | — | — | — |
| `TargetWingman2` | BUTTON | — | — | Keyboard | Key_9 | — | RVWAP | Joy_18 | — | — | — | — | — |
| `SelectTargetsTarget` | BUTTON | — | — | Keyboard | Key_F2 | — | RVWAP | Joy_17 | — | — | — | — | — |
| `WingNavLock` | BUTTON | — | — | Keyboard | Key_Minus | — | RVWAP | Joy_25 | — | — | — | — | — |
| `CycleNextSubsystem` | BUTTON | — | — | Keyboard | Key_Y | — | LVWAP | Joy_15 | — | — | — | — | — |
| `CyclePreviousSubsystem` | BUTTON | — | — | Keyboard | Key_J | — | LVWAP | Joy_17 | — | — | — | — | — |
| `TargetNextRouteSystem` | BUTTON | — | — | Keyboard | Key_K | — | RVWAP | Joy_26 | — | — | — | — | — |

## §2.9 Weapons

| XML Element | Type | Binding Device | Binding Key | Primary Device | Primary Key | Primary Modifier(s) | Secondary Device | Secondary Key | Secondary Modifier(s) | Inverted | Deadzone | ToggleOn | Setting Value |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `PrimaryFire` | BUTTON | — | — | Keyboard | Key_Minus | Keyboard/Key_RightShift | RVWAP | Joy_4 | — | — | — | — | — |
| `SecondaryFire` | BUTTON | — | — | Keyboard | Key_Minus | Keyboard/Key_LeftShift | LVWAP | Joy_4 | — | — | — | — | — |
| `CycleFireGroupNext` | BUTTON | — | — | Keyboard | Key_N | — | RVWAP | Joy_9 | — | — | — | — | — |
| `CycleFireGroupPrevious` | BUTTON | — | — | Keyboard | Key_H | — | RVWAP | Joy_11 | — | — | — | — | — |
| `DeployHardpointToggle` | BUTTON | — | — | Keyboard | Key_U | — | RVWAP | Joy_8 | — | — | — | — | — |
| `DeployHardpointsOnFire` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 1 |

## §2.10 Cooling

| XML Element | Type | Binding Device | Binding Key | Primary Device | Primary Key | Primary Modifier(s) | Secondary Device | Secondary Key | Secondary Modifier(s) | Inverted | Deadzone | ToggleOn | Setting Value |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `ToggleButtonUpInput` | BUTTON | — | — | Keyboard | Key_Delete | — | {NoDevice} | (empty) | — | — | — | 1 | — |
| `DeployHeatSink` | BUTTON | — | — | Keyboard | Key_Numpad_Divide | — | {NoDevice} | (empty) | — | — | — | — | — |

## §2.11 Miscellaneous

| XML Element | Type | Binding Device | Binding Key | Primary Device | Primary Key | Primary Modifier(s) | Secondary Device | Secondary Key | Secondary Modifier(s) | Inverted | Deadzone | ToggleOn | Setting Value |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `ShipSpotLightToggle` | BUTTON | — | — | Keyboard | Key_Insert | — | {NoDevice} | (empty) | — | — | — | — | — |
| `RadarRangeAxis` | AXIS | vJoy | Joy_YAxis | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `RadarIncreaseRange` | BUTTON | — | — | Keyboard | Key_PageUp | — | RVWAP | Joy_21 | — | — | — | — | — |
| `RadarDecreaseRange` | BUTTON | — | — | Keyboard | Key_PageDown | — | RVWAP | Joy_22 | — | — | — | — | — |
| `IncreaseEnginesPower` | BUTTON | — | — | Keyboard | Key_UpArrow | — | {NoDevice} | (empty) | — | — | — | — | — |
| `IncreaseWeaponsPower` | BUTTON | — | — | Keyboard | Key_RightArrow | — | {NoDevice} | (empty) | — | — | — | — | — |
| `IncreaseSystemsPower` | BUTTON | — | — | Keyboard | Key_LeftArrow | — | {NoDevice} | (empty) | — | — | — | — | — |
| `ResetPowerDistribution` | BUTTON | — | — | Keyboard | Key_DownArrow | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HMDReset` | BUTTON | — | — | Keyboard | Key_F8 | — | LVWAP | Joy_32 | — | — | — | — | — |
| `ToggleCargoScoop` | BUTTON | — | — | Keyboard | Key_Home | — | vJoy | Joy_11 | — | — | — | 1 | — |
| `EjectAllCargo` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `LandingGearToggle` | BUTTON | — | — | Keyboard | Key_X | — | vJoy | Joy_10 | — | — | — | — | — |
| `MicrophoneMute` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | 0 | — |
| `MuteButtonMode` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | mute_pushToTalk |
| `CqcMuteButtonMode` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | mute_pushToTalk |
| `UseShieldCell` | BUTTON | — | — | Keyboard | Key_BackSlash | — | vJoy | Joy_6 | — | — | — | — | — |
| `FireChaffLauncher` | BUTTON | — | — | Keyboard | Key_6 | — | vJoy | Joy_5 | — | — | — | — | — |
| `TriggerFieldNeutraliser` | BUTTON | — | — | Keyboard | Key_RightBracket | — | vJoy | Joy_4 | — | — | — | — | — |
| `ChargeECM` | BUTTON | — | — | Keyboard | Key_5 | — | vJoy | Joy_7 | — | — | — | — | — |
| `EnableRumbleTrigger` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 1 |
| `EnableMenuGroups` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 0 |
| `WeaponColourToggle` | BUTTON | — | — | {NoDevice} | (empty) | — | vJoy | Joy_8 | — | — | — | — | — |
| `EngineColourToggle` | BUTTON | — | — | {NoDevice} | (empty) | — | vJoy | Joy_9 | — | — | — | — | — |
| `NightVisionToggle` | BUTTON | — | — | Keyboard | Key_Grave | — | vJoy | Joy_3 | — | — | — | — | — |
| `TriggerColonisationModule` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |

## §2.12 Mode Switches

| XML Element | Type | Binding Device | Binding Key | Primary Device | Primary Key | Primary Modifier(s) | Secondary Device | Secondary Key | Secondary Modifier(s) | Inverted | Deadzone | ToggleOn | Setting Value |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `UIFocus` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `UIFocusMode` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | Bindings_FocusModeHold |
| `FocusLeftPanel` | BUTTON | — | — | Keyboard | Key_1 | — | {NoDevice} | (empty) | — | — | — | — | — |
| `FocusCommsPanel` | BUTTON | — | — | Keyboard | Key_2 | — | {NoDevice} | (empty) | — | — | — | — | — |
| `FocusOnTextEntryField` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 0 |
| `QuickCommsPanel` | BUTTON | — | — | Keyboard | Key_Enter | — | {NoDevice} | (empty) | — | — | — | — | — |
| `FocusRadarPanel` | BUTTON | — | — | Keyboard | Key_3 | — | {NoDevice} | (empty) | — | — | — | — | — |
| `FocusRightPanel` | BUTTON | — | — | Keyboard | Key_4 | — | {NoDevice} | (empty) | — | — | — | — | — |
| `LeftPanelFocusOptions` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | FocusOption_Nothing |
| `CommsPanelFocusOptions` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | FocusOption_Nothing |
| `RolePanelFocusOptions` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | FocusOption_Nothing |
| `RightPanelFocusOptions` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | FocusOption_Nothing |
| `EnableCameraLockOn` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 1 |
| `GalaxyMapOpen` | BUTTON | — | — | Keyboard | Key_Q | — | {NoDevice} | (empty) | — | — | — | — | — |
| `SystemMapOpen` | BUTTON | — | — | Keyboard | Key_C | — | {NoDevice} | (empty) | — | — | — | — | — |
| `ShowPGScoreSummaryInput` | BUTTON | — | — | Keyboard | Key_F1 | — | {NoDevice} | (empty) | — | — | — | 0 | — |
| `HeadLookToggle` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | 1 | — |
| `Pause` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `FriendsMenu` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `OpenCodexGoToDiscovery` | BUTTON | — | — | Keyboard | Key_Period | — | {NoDevice} | (empty) | — | — | — | — | — |
| `PlayerHUDModeToggle` | BUTTON | — | — | Keyboard | Key_Comma | — | LVWAP | Joy_13 | — | — | — | — | — |
| `ExplorationFSSEnter` | BUTTON | — | — | Keyboard | Key_Slash | — | LVWAP | Joy_29 | — | — | — | — | — |

## §2.13 Headlook Mode

| XML Element | Type | Binding Device | Binding Key | Primary Device | Primary Key | Primary Modifier(s) | Secondary Device | Secondary Key | Secondary Modifier(s) | Inverted | Deadzone | ToggleOn | Setting Value |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `MouseHeadlook` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 0 |
| `MouseHeadlookInvert` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 0 |
| `MouseHeadlookSensitivity` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 0.10001005 |
| `HeadlookDefault` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 0 |
| `HeadlookIncrement` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 0.00000000 |
| `HeadlookMode` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | Bindings_HeadlookModeAccumulate |
| `HeadlookResetOnToggle` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 0 |
| `HeadlookSensitivity` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 0.78936458 |
| `HeadlookSmoothing` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 1 |
| `HeadLookReset` | BUTTON | — | — | {NoDevice} | (empty) | — | LVWAP | Joy_6 | — | — | — | — | — |
| `HeadLookPitchUp` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HeadLookPitchDown` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HeadLookPitchAxisRaw` | AXIS | LVWAP | Joy_RYAxis | — | — | — | — | — | — | 1 | 0.00000000 | — | — |
| `HeadLookYawLeft` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HeadLookYawRight` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HeadLookYawAxis` | AXIS | LVWAP | Joy_RXAxis | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `MotionHeadlook` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 0 |
| `HeadlookMotionSensitivity` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 1.00000000 |
| `yawRotateHeadlook` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 0 |

## §2.14 Multicrew

| XML Element | Type | Binding Device | Binding Key | Primary Device | Primary Key | Primary Modifier(s) | Secondary Device | Secondary Key | Secondary Modifier(s) | Inverted | Deadzone | ToggleOn | Setting Value |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `MultiCrewToggleMode` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `MultiCrewPrimaryFire` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `MultiCrewSecondaryFire` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `MultiCrewPrimaryUtilityFire` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `MultiCrewSecondaryUtilityFire` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `MultiCrewThirdPersonMouseXMode` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | (empty) |
| `MultiCrewThirdPersonMouseXDecay` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 0 |
| `MultiCrewThirdPersonMouseYMode` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | (empty) |
| `MultiCrewThirdPersonMouseYDecay` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 0 |
| `MultiCrewThirdPersonYawAxisRaw` | AXIS | {NoDevice} | (empty) | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `MultiCrewThirdPersonYawLeftButton` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `MultiCrewThirdPersonYawRightButton` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `MultiCrewThirdPersonPitchAxisRaw` | AXIS | {NoDevice} | (empty) | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `MultiCrewThirdPersonPitchUpButton` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `MultiCrewThirdPersonPitchDownButton` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `MultiCrewThirdPersonMouseSensitivity` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 30.00000000 |
| `MultiCrewThirdPersonFovAxisRaw` | AXIS | {NoDevice} | (empty) | — | — | — | — | — | — | 1 | 0.00000000 | — | — |
| `MultiCrewThirdPersonFovOutButton` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `MultiCrewThirdPersonFovInButton` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `MultiCrewCockpitUICycleForward` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `MultiCrewCockpitUICycleBackward` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |

## §2.15 Fighter Orders

| XML Element | Type | Binding Device | Binding Key | Primary Device | Primary Key | Primary Modifier(s) | Secondary Device | Secondary Key | Secondary Modifier(s) | Inverted | Deadzone | ToggleOn | Setting Value |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `OrderRequestDock` | BUTTON | — | — | Keyboard | Key_R | Keyboard/Key_LeftShift | {NoDevice} | (empty) | — | — | — | — | — |
| `OrderDefensiveBehaviour` | BUTTON | — | — | Keyboard | Key_D | Keyboard/Key_LeftShift | {NoDevice} | (empty) | — | — | — | — | — |
| `OrderAggressiveBehaviour` | BUTTON | — | — | Keyboard | Key_W | Keyboard/Key_LeftShift | {NoDevice} | (empty) | — | — | — | — | — |
| `OrderFocusTarget` | BUTTON | — | — | Keyboard | Key_A | Keyboard/Key_LeftShift | {NoDevice} | (empty) | — | — | — | — | — |
| `OrderHoldFire` | BUTTON | — | — | Keyboard | Key_M | Keyboard/Key_LeftShift | {NoDevice} | (empty) | — | — | — | — | — |
| `OrderHoldPosition` | BUTTON | — | — | Keyboard | Key_H | Keyboard/Key_LeftShift | {NoDevice} | (empty) | — | — | — | — | — |
| `OrderFollow` | BUTTON | — | — | Keyboard | Key_F | Keyboard/Key_LeftShift | {NoDevice} | (empty) | — | — | — | — | — |
| `OpenOrders` | BUTTON | — | — | Keyboard | Key_O | Keyboard/Key_LeftShift | {NoDevice} | (empty) | — | — | — | — | — |

## §2.16 Full Spectrum System Scanner

| XML Element | Type | Binding Device | Binding Key | Primary Device | Primary Key | Primary Modifier(s) | Secondary Device | Secondary Key | Secondary Modifier(s) | Inverted | Deadzone | ToggleOn | Setting Value |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `ExplorationFSSCameraPitch` | AXIS | RVWAP | Joy_YAxis | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `ExplorationFSSCameraPitchIncreaseButton` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `ExplorationFSSCameraPitchDecreaseButton` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `ExplorationFSSCameraYaw` | AXIS | RVWAP | Joy_XAxis | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `ExplorationFSSCameraYawIncreaseButton` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `ExplorationFSSCameraYawDecreaseButton` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `ExplorationFSSZoomIn` | BUTTON | — | — | RVWAP | Joy_15 | — | {NoDevice} | (empty) | — | — | — | — | — |
| `ExplorationFSSZoomOut` | BUTTON | — | — | RVWAP | Joy_17 | — | {NoDevice} | (empty) | — | — | — | — | — |
| `ExplorationFSSMiniZoomIn` | BUTTON | — | — | LVWAP | Joy_15 | — | RVWAP | Joy_16 | — | — | — | — | — |
| `ExplorationFSSMiniZoomOut` | BUTTON | — | — | LVWAP | Joy_17 | — | RVWAP | Joy_18 | — | — | — | — | — |
| `ExplorationFSSRadioTuningX_Raw` | AXIS | LVWAP | Joy_RYAxis | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `ExplorationFSSRadioTuningX_Increase` | BUTTON | — | — | LVWAP | Joy_16 | — | {NoDevice} | (empty) | — | — | — | — | — |
| `ExplorationFSSRadioTuningX_Decrease` | BUTTON | — | — | LVWAP | Joy_18 | — | {NoDevice} | (empty) | — | — | — | — | — |
| `ExplorationFSSRadioTuningAbsoluteX` | AXIS | LVWAP | Joy_RXAxis | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `FSSTuningSensitivity` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 1.00000000 |
| `ExplorationFSSDiscoveryScan` | BUTTON | — | — | {NoDevice} | (empty) | — | RVWAP | Joy_4 | — | — | — | — | — |
| `ExplorationFSSQuit` | BUTTON | — | — | Keyboard | Key_Slash | — | RVWAP | Joy_29 | — | — | — | — | — |
| `FSSMouseXMode` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | (empty) |
| `FSSMouseXDecay` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 1 |
| `FSSMouseYMode` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | (empty) |
| `FSSMouseYDecay` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 1 |
| `FSSMouseSensitivity` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 5.00000000 |
| `FSSMouseDeadzone` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 0.00000000 |
| `FSSMouseLinearity` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 1.00000000 |
| `ExplorationFSSTarget` | BUTTON | — | — | RVWAP | Joy_13 | — | LVWAP | Joy_4 | — | — | — | — | — |
| `ExplorationFSSShowHelp` | BUTTON | — | — | Keyboard | Key_H | — | {NoDevice} | (empty) | — | — | — | — | — |

## §2.17 Detailed Surface Scanner

| XML Element | Type | Binding Device | Binding Key | Primary Device | Primary Key | Primary Modifier(s) | Secondary Device | Secondary Key | Secondary Modifier(s) | Inverted | Deadzone | ToggleOn | Setting Value |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `ExplorationSAAChangeScannedAreaViewToggle` | BUTTON | — | — | vJoy | Joy_7 | — | RVWAP | Joy_19 | — | — | — | 1 | — |
| `ExplorationSAAExitThirdPerson` | BUTTON | — | — | Keyboard | Key_Backspace | — | RVWAP | Joy_28 | — | — | — | — | — |
| `ExplorationSAANextGenus` | BUTTON | — | — | Keyboard | Key_R | — | RVWAP | Joy_29 | — | — | — | — | — |
| `ExplorationSAAPreviousGenus` | BUTTON | — | — | Keyboard | Key_W | — | RVWAP | Joy_30 | — | — | — | — | — |
| `SAAThirdPersonMouseXMode` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | (empty) |
| `SAAThirdPersonMouseXDecay` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 0 |
| `SAAThirdPersonMouseYMode` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | (empty) |
| `SAAThirdPersonMouseYDecay` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 0 |
| `SAAThirdPersonMouseSensitivity` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 30.00000000 |
| `SAAThirdPersonYawAxisRaw` | AXIS | RVWAP | Joy_XAxis | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `SAAThirdPersonYawLeftButton` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `SAAThirdPersonYawRightButton` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `SAAThirdPersonPitchAxisRaw` | AXIS | RVWAP | Joy_YAxis | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `SAAThirdPersonPitchUpButton` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `SAAThirdPersonPitchDownButton` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `SAAThirdPersonFovAxisRaw` | AXIS | RVWAP | Joy_ZAxis | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `SAAThirdPersonFovOutButton` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `SAAThirdPersonFovInButton` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |

---

# Game Mode 3: SRV (Driving) Controls

## §3.1 Driving

| XML Element | Type | Binding Device | Binding Key | Primary Device | Primary Key | Primary Modifier(s) | Secondary Device | Secondary Key | Secondary Modifier(s) | Inverted | Deadzone | ToggleOn | Setting Value |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `ToggleDriveAssist` | BUTTON | — | — | Keyboard | Key_Z | — | {NoDevice} | (empty) | — | — | — | 1 | — |
| `DriveAssistDefault` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 1 |
| `MouseBuggySteeringXMode` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | (empty) |
| `MouseBuggySteeringXDecay` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 0 |
| `MouseBuggyRollingXMode` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | (empty) |
| `MouseBuggyRollingXDecay` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 0 |
| `MouseBuggyYMode` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | (empty) |
| `MouseBuggyYDecay` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 0 |
| `SteeringAxis` | AXIS | RVWAP | Joy_XAxis | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `SteerLeftButton` | BUTTON | — | — | Keyboard | Key_S | — | {NoDevice} | (empty) | — | — | — | — | — |
| `SteerRightButton` | BUTTON | — | — | Keyboard | Key_F | — | {NoDevice} | (empty) | — | — | — | — | — |
| `BuggyRollAxisRaw` | AXIS | RVWAP | Joy_ZAxis | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `BuggyRollLeftButton` | BUTTON | — | — | Keyboard | Key_W | — | {NoDevice} | (empty) | — | — | — | — | — |
| `BuggyRollRightButton` | BUTTON | — | — | Keyboard | Key_R | — | {NoDevice} | (empty) | — | — | — | — | — |
| `BuggyPitchAxis` | AXIS | RVWAP | Joy_YAxis | — | — | — | — | — | — | 1 | 0.00000000 | — | — |
| `BuggyPitchUpButton` | BUTTON | — | — | Keyboard | Key_A | — | {NoDevice} | (empty) | — | — | — | — | — |
| `BuggyPitchDownButton` | BUTTON | — | — | Keyboard | Key_Z | — | {NoDevice} | (empty) | — | — | — | — | — |
| `VerticalThrustersButton` | BUTTON | — | — | Keyboard | Key_Space | — | T-Rudder | Neg_Joy_YAxis | — | — | — | 0 | — |
| `BuggyPrimaryFireButton` | BUTTON | — | — | Keyboard | Key_Minus | Keyboard/Key_RightShift | RVWAP | Joy_4 | — | — | — | — | — |
| `BuggySecondaryFireButton` | BUTTON | — | — | Keyboard | Key_Minus | Keyboard/Key_LeftShift | LVWAP | Joy_4 | — | — | — | — | — |
| `AutoBreakBuggyButton` | BUTTON | — | — | Keyboard | Key_V | — | RVWAP | Pos_Joy_UAxis | Modifier: RVWAP/Joy_32 | — | — | — | 0 | — |
| `HeadlightsBuggyButton` | BUTTON | — | — | Keyboard | Key_Insert | — | RVWAP | Joy_7 | — | — | — | — | — |
| `ToggleBuggyTurretButton` | BUTTON | — | — | Keyboard | Key_T | — | LVWAP | Joy_7 | — | — | — | — | — |
| `BuggyCycleFireGroupNext` | BUTTON | — | — | Keyboard | Key_N | — | RVWAP | Joy_11 | — | — | — | — | — |
| `BuggyCycleFireGroupPrevious` | BUTTON | — | — | Keyboard | Key_H | — | RVWAP | Joy_9 | — | — | — | — | — |

## §3.2 Driving Targeting

| XML Element | Type | Binding Device | Binding Key | Primary Device | Primary Key | Primary Modifier(s) | Secondary Device | Secondary Key | Secondary Modifier(s) | Inverted | Deadzone | ToggleOn | Setting Value |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `SelectTarget_Buggy` | BUTTON | — | — | Mouse | Mouse_2 | — | RVWAP | Joy_13 | — | — | — | — | — |

## §3.3 Driving Turret Controls

| XML Element | Type | Binding Device | Binding Key | Primary Device | Primary Key | Primary Modifier(s) | Secondary Device | Secondary Key | Secondary Modifier(s) | Inverted | Deadzone | ToggleOn | Setting Value |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `MouseTurretXMode` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | Bindings_MouseYaw |
| `MouseTurretXDecay` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 1 |
| `MouseTurretYMode` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | Bindings_MousePitchInverted |
| `MouseTurretYDecay` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 1 |
| `BuggyTurretYawAxisRaw` | AXIS | RVWAP | Joy_XAxis | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `BuggyTurretYawLeftButton` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `BuggyTurretYawRightButton` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `BuggyTurretPitchAxisRaw` | AXIS | RVWAP | Joy_YAxis | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `BuggyTurretPitchUpButton` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `BuggyTurretPitchDownButton` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `BuggyTurretMouseSensitivity` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 2.64227581 |
| `BuggyTurretMouseDeadzone` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 0.05000000 |
| `BuggyTurretMouseLinearity` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 2.00349998 |

## §3.4 Drive Throttle

| XML Element | Type | Binding Device | Binding Key | Primary Device | Primary Key | Primary Modifier(s) | Secondary Device | Secondary Key | Secondary Modifier(s) | Inverted | Deadzone | ToggleOn | Setting Value |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `DriveSpeedAxis` | AXIS | T-Rudder | Joy_XAxis | — | — | — | — | — | — | 1 | 0.13712500 | — | — |
| `BuggyThrottleRange` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | Bindings_BuggyThrottleForewardOnly |
| `BuggyToggleReverseThrottleInput` | BUTTON | — | — | {NoDevice} | (empty) | — | RVWAP | Joy_27 | — | — | — | 0 | — |
| `BuggyThrottleIncrement` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 0.00000000 |
| `IncreaseSpeedButtonMax` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `DecreaseSpeedButtonMax` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `IncreaseSpeedButtonPartial` | AXIS | {NoDevice} | (empty) | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `DecreaseSpeedButtonPartial` | AXIS | {NoDevice} | (empty) | — | — | — | — | — | — | 0 | 0.00000000 | — | — |

## §3.5 Driving Miscellaneous

| XML Element | Type | Binding Device | Binding Key | Primary Device | Primary Key | Primary Modifier(s) | Secondary Device | Secondary Key | Secondary Modifier(s) | Inverted | Deadzone | ToggleOn | Setting Value |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `IncreaseEnginesPower_Buggy` | BUTTON | — | — | Keyboard | Key_UpArrow | — | {NoDevice} | (empty) | — | — | — | — | — |
| `IncreaseWeaponsPower_Buggy` | BUTTON | — | — | Keyboard | Key_RightArrow | — | {NoDevice} | (empty) | — | — | — | — | — |
| `IncreaseSystemsPower_Buggy` | BUTTON | — | — | Keyboard | Key_LeftArrow | — | {NoDevice} | (empty) | — | — | — | — | — |
| `ResetPowerDistribution_Buggy` | BUTTON | — | — | Keyboard | Key_DownArrow | — | {NoDevice} | (empty) | — | — | — | — | — |
| `ToggleCargoScoop_Buggy` | BUTTON | — | — | Keyboard | Key_Home | — | {NoDevice} | (empty) | — | — | — | 1 | — |
| `EjectAllCargo_Buggy` | BUTTON | — | — | Keyboard | Key_F4 | — | {NoDevice} | (empty) | — | — | — | — | — |
| `RecallDismissShip` | BUTTON | — | — | Keyboard | Key_F7 | — | {NoDevice} | (empty) | — | — | — | — | — |
| `EnableMenuGroupsSRV` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 0 |

## §3.6 Driving Mode Switches

| XML Element | Type | Binding Device | Binding Key | Primary Device | Primary Key | Primary Modifier(s) | Secondary Device | Secondary Key | Secondary Modifier(s) | Inverted | Deadzone | ToggleOn | Setting Value |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `UIFocus_Buggy` | BUTTON | — | — | Keyboard | Key_LeftShift | — | {NoDevice} | (empty) | — | — | — | — | — |
| `FocusLeftPanel_Buggy` | BUTTON | — | — | Keyboard | Key_1 | — | {NoDevice} | (empty) | — | — | — | — | — |
| `FocusCommsPanel_Buggy` | BUTTON | — | — | Keyboard | Key_2 | — | {NoDevice} | (empty) | — | — | — | — | — |
| `QuickCommsPanel_Buggy` | BUTTON | — | — | Keyboard | Key_Enter | — | {NoDevice} | (empty) | — | — | — | — | — |
| `FocusRadarPanel_Buggy` | BUTTON | — | — | Keyboard | Key_3 | — | {NoDevice} | (empty) | — | — | — | — | — |
| `FocusRightPanel_Buggy` | BUTTON | — | — | Keyboard | Key_4 | — | {NoDevice} | (empty) | — | — | — | — | — |
| `GalaxyMapOpen_Buggy` | BUTTON | — | — | Keyboard | Key_Q | — | {NoDevice} | (empty) | — | — | — | — | — |
| `SystemMapOpen_Buggy` | BUTTON | — | — | Keyboard | Key_C | — | {NoDevice} | (empty) | — | — | — | — | — |
| `OpenCodexGoToDiscovery_Buggy` | BUTTON | — | — | Keyboard | Key_Period | — | {NoDevice} | (empty) | — | — | — | — | — |
| `PlayerHUDModeToggle_Buggy` | BUTTON | — | — | Keyboard | Key_Comma | — | LVWAP | Joy_13 | — | — | — | — | — |
| `HeadLookToggle_Buggy` | BUTTON | — | — | {NoDevice} | (empty) | — | Keyboard | Key_5 | — | — | — | 1 | — |

---

# Game Mode 4: On Foot Controls

## §4.1 On Foot

| XML Element | Type | Binding Device | Binding Key | Primary Device | Primary Key | Primary Modifier(s) | Secondary Device | Secondary Key | Secondary Modifier(s) | Inverted | Deadzone | ToggleOn | Setting Value |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `MouseHumanoidXMode` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | Bindings_MouseYaw |
| `MouseHumanoidYMode` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | Bindings_MousePitch |
| `MouseHumanoidSensitivity` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 8.68082523 |
| `HumanoidForwardAxis` | AXIS | {NoDevice} | (empty) | — | — | — | — | — | — | 1 | 0.00000000 | — | — |
| `HumanoidForwardButton` | BUTTON | — | — | Keyboard | Key_E | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidBackwardButton` | BUTTON | — | — | Keyboard | Key_D | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidStrafeAxis` | AXIS | {NoDevice} | (empty) | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `HumanoidStrafeLeftButton` | BUTTON | — | — | Keyboard | Key_S | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidStrafeRightButton` | BUTTON | — | — | Keyboard | Key_F | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidRotateAxis` | AXIS | {NoDevice} | (empty) | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `HumanoidRotateSensitivity` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 1.19407392 |
| `HumanoidRotateLeftButton` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidRotateRightButton` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidPitchAxis` | AXIS | {NoDevice} | (empty) | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `HumanoidPitchSensitivity` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 3.34429455 |
| `HumanoidPitchUpButton` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidPitchDownButton` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidSprintButton` | BUTTON | — | — | Keyboard | Key_A | — | {NoDevice} | (empty) | — | — | — | 1 | — |
| `HumanoidWalkButton` | BUTTON | — | — | Keyboard | Key_A | Keyboard/Key_LeftControl | {NoDevice} | (empty) | — | — | — | 1 | — |
| `HumanoidCrouchButton` | BUTTON | — | — | Mouse | Mouse_4 | — | {NoDevice} | (empty) | — | — | — | 1 | — |
| `HumanoidJumpButton` | BUTTON | — | — | Keyboard | Key_Space | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidPrimaryInteractButton` | BUTTON | — | — | Keyboard | Key_G | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidSecondaryInteractButton` | BUTTON | — | — | Keyboard | Key_B | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidItemWheelButton` | BUTTON | — | — | Keyboard | Key_I | — | {NoDevice} | (empty) | — | — | — | 1 | — |
| `HumanoidEmoteWheelButton` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | 1 | — |
| `HumanoidUtilityWheelCycleMode` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidItemWheelButton_XAxis` | AXIS | {NoDevice} | (empty) | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `HumanoidItemWheelButton_XLeft` | BUTTON | — | — | Keyboard | Key_S | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidItemWheelButton_XRight` | BUTTON | — | — | Keyboard | Key_F | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidItemWheelButton_YAxis` | AXIS | {NoDevice} | (empty) | — | — | — | — | — | — | 1 | 0.00000000 | — | — |
| `HumanoidItemWheelButton_YUp` | BUTTON | — | — | Keyboard | Key_E | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidItemWheelButton_YDown` | BUTTON | — | — | Keyboard | Key_D | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidItemWheel_AcceptMouseInput` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 0 |
| `HumanoidPrimaryFireButton` | BUTTON | — | — | Mouse | Mouse_1 | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidZoomButton` | BUTTON | — | — | Keyboard | Key_W | — | {NoDevice} | (empty) | — | — | — | 1 | — |
| `HumanoidThrowGrenadeButton` | BUTTON | — | — | Keyboard | Key_T | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidMeleeButton` | BUTTON | — | — | Mouse | Mouse_2 | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidReloadButton` | BUTTON | — | — | Keyboard | Key_R | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidSwitchWeapon` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidSelectPrimaryWeaponButton` | BUTTON | — | — | Keyboard | Key_1 | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidSelectSecondaryWeaponButton` | BUTTON | — | — | Keyboard | Key_2 | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidSelectUtilityWeaponButton` | BUTTON | — | — | Keyboard | Key_3 | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidSelectNextWeaponButton` | BUTTON | — | — | Keyboard | Key_PageUp | — | Mouse | Pos_Mouse_ZAxis | — | — | — | — | — |
| `HumanoidSelectPreviousWeaponButton` | BUTTON | — | — | Keyboard | Key_PageDown | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidHideWeaponButton` | BUTTON | — | — | Keyboard | Key_End | — | Mouse | Mouse_3 | — | — | — | — | — |
| `HumanoidSelectNextGrenadeTypeButton` | BUTTON | — | — | Keyboard | Key_PageUp | Keyboard/Key_RightShift | Mouse | Neg_Mouse_ZAxis | — | — | — | — | — |
| `HumanoidSelectPreviousGrenadeTypeButton` | BUTTON | — | — | Keyboard | Key_PageDown | Keyboard/Key_RightShift | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidToggleFlashlightButton` | BUTTON | — | — | Keyboard | Key_Insert | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidToggleNightVisionButton` | BUTTON | — | — | Keyboard | Key_Apostrophe | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidToggleShieldsButton` | BUTTON | — | — | Keyboard | Key_Z | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidClearAuthorityLevel` | BUTTON | — | — | Keyboard | Key_Delete | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidHealthPack` | BUTTON | — | — | Keyboard | Key_H | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidBattery` | BUTTON | — | — | Keyboard | Key_U | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidSelectFragGrenade` | BUTTON | — | — | Keyboard | Key_L | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidSelectEMPGrenade` | BUTTON | — | — | Keyboard | Key_SemiColon | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidSelectShieldGrenade` | BUTTON | — | — | Keyboard | Key_Comma | Keyboard/Key_LeftShift | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidSwitchToRechargeTool` | BUTTON | — | — | Keyboard | Key_4 | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidSwitchToCompAnalyser` | BUTTON | — | — | Keyboard | Key_5 | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidSwitchToSuitTool` | BUTTON | — | — | Keyboard | Key_6 | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidToggleToolModeButton` | BUTTON | — | — | Keyboard | Key_Tab | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidToggleMissionHelpPanelButton` | BUTTON | — | — | Keyboard | Key_Slash | Keyboard/Key_RightShift | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidPing` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |

## §4.2 On Foot Mode Switches

| XML Element | Type | Binding Device | Binding Key | Primary Device | Primary Key | Primary Modifier(s) | Secondary Device | Secondary Key | Secondary Modifier(s) | Inverted | Deadzone | ToggleOn | Setting Value |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `GalaxyMapOpen_Humanoid` | BUTTON | — | — | Keyboard | Key_Q | — | {NoDevice} | (empty) | — | — | — | — | — |
| `SystemMapOpen_Humanoid` | BUTTON | — | — | Keyboard | Key_C | — | {NoDevice} | (empty) | — | — | — | — | — |
| `FocusCommsPanel_Humanoid` | BUTTON | — | — | Keyboard | Key_LeftBracket | — | {NoDevice} | (empty) | — | — | — | — | — |
| `QuickCommsPanel_Humanoid` | BUTTON | — | — | Keyboard | Key_N | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidOpenAccessPanelButton` | BUTTON | — | — | Keyboard | Key_Y | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidConflictContextualUIButton` | BUTTON | — | — | Keyboard | Key_Home | — | {NoDevice} | (empty) | — | — | — | — | — |

## §4.3 On Foot Miscellaneous

| XML Element | Type | Binding Device | Binding Key | Primary Device | Primary Key | Primary Modifier(s) | Secondary Device | Secondary Key | Secondary Modifier(s) | Inverted | Deadzone | ToggleOn | Setting Value |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `EnableMenuGroupsOnFoot` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 0 |
| `EnableAimAssistOnFoot` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | 1 |

## §4.4 On Foot Emotes

| XML Element | Type | Binding Device | Binding Key | Primary Device | Primary Key | Primary Modifier(s) | Secondary Device | Secondary Key | Secondary Modifier(s) | Inverted | Deadzone | ToggleOn | Setting Value |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `HumanoidEmoteSlot1` | BUTTON | — | — | Keyboard | Key_P | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidEmoteSlot2` | BUTTON | — | — | Keyboard | Key_M | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidEmoteSlot3` | BUTTON | — | — | Keyboard | Key_Slash | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidEmoteSlot4` | BUTTON | — | — | Keyboard | Key_BackSlash | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidEmoteSlot5` | BUTTON | — | — | Keyboard | Key_V | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidEmoteSlot6` | BUTTON | — | — | Keyboard | Key_Period | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidEmoteSlot7` | BUTTON | — | — | Keyboard | Key_Comma | — | {NoDevice} | (empty) | — | — | — | — | — |
| `HumanoidEmoteSlot8` | BUTTON | — | — | Keyboard | Key_O | — | {NoDevice} | (empty) | — | — | — | — | — |

---

## Summary

### Total element count

**Total XML child elements of `<Root>`: 457**

(This is the total count of distinct element names under the root, counting each occurrence. `MouseGUI` appears twice — counted twice.)

### By type

| Type | Count |
|---|---|
| AXIS | 72 |
| BUTTON | 268 |
| STANDALONE SETTING | 117 |
| **Total** | **457** |

### Modifier findings in this file

Every element where one or more `<Modifier>` children were found, with full detail:

| XML Element | Slot | Modifier(s) |
|---|---|---|
| `ForwardKey` | Primary | Keyboard/Key_LeftShift |
| `BackwardKey` | Primary | Keyboard/Key_LeftShift |
| `PrimaryFire` | Primary | Keyboard/Key_RightShift |
| `SecondaryFire` | Primary | Keyboard/Key_LeftShift |
| `UI_Toggle` | Primary | Keyboard/Key_LeftControl |
| `CyclePreviousPage` | Primary | Keyboard/Key_LeftControl |
| `VanityCameraScrollLeft` | Primary | Keyboard/Key_LeftControl |
| `VanityCameraScrollRight` | Primary | Keyboard/Key_LeftControl |
| `ToggleFreeCam` | Primary | Keyboard/Key_LeftControl |
| `FixCameraRelativeToggle` | Primary | Keyboard/Key_LeftShift **+** Keyboard/Key_LeftControl *(two simultaneous Modifiers)* |
| `CommanderCreator_Undo` | Primary | Keyboard/Key_LeftControl |
| `CommanderCreator_Redo` | Primary | Keyboard/Key_LeftControl **+** Keyboard/Key_LeftShift *(two simultaneous Modifiers)* |
| `OrderRequestDock` | Primary | Keyboard/Key_LeftShift |
| `OrderDefensiveBehaviour` | Primary | Keyboard/Key_LeftShift |
| `OrderAggressiveBehaviour` | Primary | Keyboard/Key_LeftShift |
| `OrderFocusTarget` | Primary | Keyboard/Key_LeftShift |
| `OrderHoldFire` | Primary | Keyboard/Key_LeftShift |
| `OrderHoldPosition` | Primary | Keyboard/Key_LeftShift |
| `OrderFollow` | Primary | Keyboard/Key_LeftShift |
| `OpenOrders` | Primary | Keyboard/Key_LeftShift |
| `BuggyPrimaryFireButton` | Primary | Keyboard/Key_RightShift |
| `BuggySecondaryFireButton` | Primary | Keyboard/Key_LeftShift |
| `AutoBreakBuggyButton` | Secondary | RVWAP/Joy_32 *(modifier on a joystick Secondary slot — non-keyboard Modifier)* |
| `HumanoidWalkButton` | Primary | Keyboard/Key_LeftControl |
| `HumanoidSelectNextGrenadeTypeButton` | Primary | Keyboard/Key_RightShift |
| `HumanoidSelectPreviousGrenadeTypeButton` | Primary | Keyboard/Key_RightShift |
| `HumanoidSelectShieldGrenade` | Primary | Keyboard/Key_LeftShift |
| `HumanoidToggleMissionHelpPanelButton` | Primary | Keyboard/Key_RightShift |

**Total elements with Modifiers: 28**
**Elements with two simultaneous Modifiers on one slot: 2** (`FixCameraRelativeToggle`, `CommanderCreator_Redo`)

### Unexpected / notable property patterns

1. **Duplicate `MouseGUI` element** — `MouseGUI Value="1"` appears at XML line 15 and again at line 471. The XML parser would produce two child nodes with the same tag name under `<Root>`. A data model must handle this gracefully (last-wins, or array).

2. **Non-keyboard Modifier on a joystick Secondary slot** — `AutoBreakBuggyButton` has `<Secondary Device="RVWAP" Key="Pos_Joy_UAxis">` with a child `<Modifier Device="RVWAP" Key="Joy_32" />`. This is the only instance in the file where a Modifier appears on a Secondary slot (all others are on Primary), and the only instance where both the base key and the Modifier are on the same joystick device (RVWAP) rather than Keyboard. This directly contradicts the "Modifiers have only been confirmed on BUTTON-type Primary/Secondary slots" framing in the spec — they can appear on Secondary as well — and also contradicts "Modifiers are Keyboard-only."

3. **Axis Key strings beyond `Joy_XAxis/YAxis/ZAxis`** — The file exercises several axis key formats that a data model must enumerate: `Joy_RYAxis`, `Joy_RXAxis` (LVWAP headlook axes), `Neg_Joy_XAxis`, `Neg_Joy_YAxis` (T-Rudder, negative half-axis), `Pos_Joy_UAxis` (RVWAP, positive half-axis). These appear on both AXIS `<Binding>` nodes and BUTTON `<Primary>`/`<Secondary>` nodes.

4. **Half-axis keys on BUTTON slots** — `UpThrustButton` Secondary has `T-Rudder/Neg_Joy_YAxis`; `DownThrustButton` Secondary has `T-Rudder/Neg_Joy_XAxis`; `VerticalThrustersButton` Secondary has `T-Rudder/Neg_Joy_YAxis`. These are full directional-axis inputs treated as digital button bindings (half-axis = negative direction only). The same pattern appears on `AutoBreakBuggyButton` with `Pos_Joy_UAxis`. A data model must not assume BUTTON Key values are always discrete button identifiers.

5. **Mouse axis keys on BUTTON slots** — `HumanoidSelectNextWeaponButton` Secondary uses `Mouse/Pos_Mouse_ZAxis`; `HumanoidSelectNextGrenadeTypeButton` Secondary uses `Mouse/Neg_Mouse_ZAxis`. Mouse scroll wheel directions are used as digital button bindings.

6. **Mouse button keys on BUTTON slots** — `SelectTarget_Buggy` Primary uses `Mouse/Mouse_2`; `HumanoidPrimaryFireButton` Primary uses `Mouse/Mouse_1`; `HumanoidCrouchButton` Primary uses `Mouse/Mouse_4`; `HumanoidMeleeButton` Primary uses `Mouse/Mouse_2`; `HumanoidHideWeaponButton` Secondary uses `Mouse/Mouse_3`. Mouse buttons appear as first-class Primary/Secondary binding devices.

7. **`HeadLookToggle_Buggy` — Secondary bound to Keyboard, Primary unbound** — Uniquely, this element has `<Primary Device="{NoDevice}">` but `<Secondary Device="Keyboard" Key="Key_5">`. Secondary is a Keyboard binding while Primary is unbound. This is the only case in the file where a Keyboard binding is on Secondary with Primary vacant — a data model must not assume Primary is always filled when Secondary is.

8. **`IncreaseSpeedButtonPartial` and `DecreaseSpeedButtonPartial`** — These are classified as AXIS (have `<Binding>`, `<Inverted>`, `<Deadzone>` children) despite their names ending in "Button". Both are unbound. Their presence under SRV Drive Throttle represents an axis-as-throttle-partial-input pattern that is separate from the regular button throttle controls.

9. **`KeyboardLayout` root child** — The file contains `<KeyboardLayout>en-US</KeyboardLayout>` as a child element with text content (not an attribute). This does not fit any of the three classification types. It is a root-level metadata element with a text node value rather than a `Value=""` attribute. See classification note below.

10. **`yawRotateHeadlook` — lowercase first character** — All other element names under `<Root>` begin with an uppercase letter; `yawRotateHeadlook` begins with lowercase `y`. A data model that auto-derives display names by splitting on camelCase must handle the leading-lowercase case.

### Other/Flagged elements

| Element | Why flagged |
|---|---|
| `KeyboardLayout` | Text-content element (`en-US`), not a `Value=""` attribute. Not AXIS, not BUTTON, not STANDALONE SETTING. Unique pattern in the file — a pure metadata node. |

### Schema capability confirmations

Based on direct evidence in this file:

1. **Confirmed: Multiple simultaneous Modifiers on a single Primary slot.** `FixCameraRelativeToggle` has two Modifier children (`Key_LeftShift` + `Key_LeftControl`) on Primary. `CommanderCreator_Redo` has two (`Key_LeftControl` + `Key_LeftShift`) on Primary.

2. **Confirmed: Modifiers can appear on Secondary slots (not only Primary).** `AutoBreakBuggyButton` has a Modifier on its Secondary slot.

3. **Overturned: Modifiers are NOT limited to Keyboard device.** `AutoBreakBuggyButton` has `<Modifier Device="RVWAP" Key="Joy_32" />` on its Secondary slot — a joystick modifier on a joystick base key. The assumption that "Modifiers are Keyboard-only" is not supported by this file.

4. **Confirmed: Modifiers appear only on BUTTON-type slots (Primary/Secondary), not on AXIS Binding slots.** No `<Modifier>` child was found inside any `<Binding>` element in this file.

5. **Confirmed: Device can be `{NoDevice}` on AXIS Binding elements.** Many unbound axes have `<Binding Device="{NoDevice}" Key="" />`.

6. **Confirmed: A Secondary slot can be the only bound slot (Primary unbound).** `HeadLookToggle_Buggy`, `CyclePreviousTarget`, `HeadLookReset`, and many others have Primary `{NoDevice}` while Secondary is bound.

7. **New finding: Axis key strings appear on BUTTON Primary/Secondary slots.** Half-axis strings (`Neg_Joy_YAxis`, `Pos_Joy_UAxis`) and mouse-wheel strings (`Pos_Mouse_ZAxis`, `Neg_Mouse_ZAxis`) are used as Key attribute values on BUTTON slots, not only on AXIS Binding slots.

8. **New finding: Mouse buttons (Mouse_1, Mouse_2, Mouse_3, Mouse_4) are valid Device="Mouse" binding keys on BUTTON slots.** The Mouse device is used on both Primary and Secondary.

9. **New finding: T-Rudder is a valid named device** alongside RVWAP, LVWAP, vJoy, Keyboard, Mouse.

10. **New finding: The XML format supports duplicate element tag names under `<Root>`.** `MouseGUI` appears twice. Parsers must handle this without silently dropping one instance.
