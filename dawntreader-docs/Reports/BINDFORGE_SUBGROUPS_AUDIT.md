# BindForge Sub-Groups Audit
**Source:** `DualVirpilDawnTreader.4.2.binds` (canonical)  
**Against:** `Specifications/BindForge_GameMode_SubGroups.md`  
**Method:** XML element names extracted from .binds; matched against UI display names in spec doc.  
**Note:** DeviceMappings.xml and .buttonMap files excluded as cosmetic-only per task instructions.

---

## Section 1 — Doc entries with NO matching XML element

These are action names written in the sub-groups doc that do not correspond to any element in the .binds file. Possible causes: typo, renamed action, or duplicate transcription of the same UI row.

| # | Doc Section | Doc Entry (as written) | Diagnosis |
|---|---|---|---|
| 1 | §2.8 Targeting | **Select Next Target** | No `SelectNextTarget` XML element exists. The immediately following entry ("Cycle Next Target") maps to `CycleNextTarget`. The game likely shows a single UI row that was transcribed twice, or the row label changed between game builds. The XML binding is under `CycleNextTarget` only. |

---

## Section 2 — XML elements NOT covered by any doc section

These bindable action elements exist in the .binds file but are absent from BindForge_GameMode_SubGroups.md entirely, or are present in the doc but incorrectly described as having no binding slot.

### 2A — Completely absent from doc

| XML Element | Bound? | Notes |
|---|---|---|
| `UI_Select` | **Yes** — Keyboard Space + RVWAP Joy_4 | The "Confirm / Select" action in all panel UIs. Should appear in **§1.1 Interface Mode** alongside UI_Up/Down/Left/Right/Back/Toggle. The doc lists 10 Interface Mode actions but omits this one. |
| `HumanoidPing` | No — both slots empty | On-foot "Ping / Call Out" action. Not present in §4.1, §4.2, §4.3, or §4.4. |

### 2B — Present in doc but binding slot existence not acknowledged

These elements appear in the doc under §2.1 Mouse Controls with the note "there are no standard key binding slots," but the XML shows they **do** have `<Primary>` / `<Secondary>` child elements (even if currently unbound).

| XML Element | Inferred UI Name (§2.1) | XML has Primary/Secondary? |
|---|---|---|
| `MouseReset` | Reset Mouse `[toggle: ON/OFF]` | Yes — both `{NoDevice}` (unbound) |
| `BlockMouseDecay` | Disable Relative Mouse `[toggle: ON/OFF]` | Yes — both `{NoDevice}` (unbound), plus `<ToggleOn Value="0"/>` |

The in-game UI likely hides the binding row for these two and only exposes the toggle switch, which is why the doc transcription missed them. They are functionally bindable at the XML level.

---

## Section 3 — Full coverage confirmation (all other elements)

Every other XML bindable element maps cleanly to a doc entry. The table below records the key mappings verified, grouped by doc section, for traceability. "Bindable" = has `<Primary>`/`<Secondary>` or `<Binding>` child; "Settings-only" = bare `Value=""` attribute — listed in the doc as sliders/dropdowns/toggles with no key slot.

### §1.1 Interface Mode
| XML Element | UI Name in Doc | Status |
|---|---|---|
| `UI_Up` | UI Panel Up | ✓ |
| `UI_Down` | UI Panel Down | ✓ |
| `UI_Left` | UI Panel Left | ✓ |
| `UI_Right` | UI Panel Right | ✓ |
| `UI_Back` | UI Back | ✓ |
| `UI_Toggle` | UI Nested Toggle | ✓ |
| `CycleNextPanel` | Next Panel Tab | ✓ |
| `CyclePreviousPanel` | Previous Panel Tab | ✓ |
| `CycleNextPage` | Next Page | ✓ |
| `CyclePreviousPage` | Previous Page | ✓ |
| `UI_Select` | *(missing from doc — see §2A above)* | **GAP** |

### §1.2 Galaxy Map
| XML Element | UI Name in Doc | Status |
|---|---|---|
| `CamPitchAxis` | Galaxy Cam Pitch Axis | ✓ |
| `CamPitchUp` | Galaxy Cam Pitch Up | ✓ |
| `CamPitchDown` | Galaxy Cam Pitch Down | ✓ |
| `CamYawAxis` | Galaxy Cam Yaw Axis | ✓ |
| `CamYawLeft` | Galaxy Cam Yaw Left | ✓ |
| `CamYawRight` | Galaxy Cam Yaw Right | ✓ |
| `CamTranslateYAxis` | Galaxy Cam Translate Y-Axis | ✓ |
| `CamTranslateForward` | Galaxy Cam Translate Forward | ✓ |
| `CamTranslateBackward` | Galaxy Cam Translate Backward | ✓ |
| `CamTranslateXAxis` | Galaxy Cam Translate X-Axis | ✓ |
| `CamTranslateLeft` | Galaxy Cam Translate Left | ✓ |
| `CamTranslateRight` | Galaxy Cam Translate Right | ✓ |
| `CamTranslateZAxis` | Galaxy Cam Translate Z-Axis | ✓ |
| `CamTranslateUp` | Galaxy Cam Translate Up | ✓ |
| `CamTranslateDown` | Galaxy Cam Translate Down | ✓ |
| `CamZoomAxis` | Galaxy Cam Zoom Axis | ✓ |
| `CamZoomIn` | Galaxy Cam Zoom In | ✓ |
| `CamZoomOut` | Galaxy Cam Zoom Out | ✓ |
| `CamTranslateZHold` | Galaxy Cam Set Y-Axis to Z-Axis | ✓ |
| `GalaxyMapHome` | Galaxy Cam Select Current System | ✓ |

### §1.3 Camera Suite
| XML Element | UI Name in Doc | Status |
|---|---|---|
| `PhotoCameraToggle` | Ship - Toggle Camera Suite | ✓ |
| `PhotoCameraToggle_Buggy` | SRV - Toggle Camera Suite | ✓ |
| `PhotoCameraToggle_Humanoid` | Commander - Toggle Camera Suite | ✓ |
| `VanityCameraScrollLeft` | Previous Camera | ✓ |
| `VanityCameraScrollRight` | Next Camera | ✓ |
| `ToggleFreeCam` | Enter Free Camera | ✓ |
| `VanityCameraOne` | Camera - Cockpit Front | ✓ |
| `VanityCameraTwo` | Camera - Cockpit Back | ✓ |
| `VanityCameraThree` | Camera - CMDR 1 | ✓ |
| `VanityCameraFour` | Camera - CMDR 2 | ✓ |
| `VanityCameraFive` | Camera - Co-Pilot 1 | ✓ |
| `VanityCameraSix` | Camera - Co-Pilot 2 | ✓ |
| `VanityCameraSeven` | Camera - Co-Pilot 3 | ✓ |
| `VanityCameraEight` | Camera - Front | ✓ |
| `VanityCameraNine` | Camera - Back | ✓ |
| `VanityCameraTen` | Camera - Low | ✓ |

### §1.4 Free Camera
| XML Element | UI Name in Doc | Status |
|---|---|---|
| `FreeCamToggleHUD` | Toggle HUD | ✓ |
| `FreeCamSpeedInc` | Increase Speed | ✓ |
| `FreeCamSpeedDec` | Decrease Speed | ✓ |
| `MoveFreeCamY` | Forward Axis | ✓ |
| `ThrottleRangeFreeCam` | Throttle Axis Range | settings-only |
| `ToggleReverseThrottleInputFreeCam` | Forward Only Throttle Reverse | ✓ |
| `MoveFreeCamForward` | Move Forward | ✓ |
| `MoveFreeCamBackwards` | Move Backward | ✓ |
| `MoveFreeCamX` | Lateral Axis | ✓ |
| `MoveFreeCamRight` | Move Right | ✓ |
| `MoveFreeCamLeft` | Move Left | ✓ |
| `MoveFreeCamZ` | Lift Axis | ✓ |
| `MoveFreeCamUpAxis` | Move Up [Analogue] | ✓ |
| `MoveFreeCamDownAxis` | Move Down [Analogue] | ✓ |
| `MoveFreeCamUp` | Move Up | ✓ |
| `MoveFreeCamDown` | Move Down | ✓ |
| `PitchCamera` | Pitch Axis | ✓ |
| `FreeCamMouseSensitivity` | Free Camera Mouse Sensitivity | settings-only |
| `PitchCameraUp` | Pitch Up | ✓ |
| `PitchCameraDown` | Pitch Down | ✓ |
| `YawCamera` | Yaw Axis | ✓ |
| `YawCameraLeft` | Yaw Left | ✓ |
| `YawCameraRight` | Yaw Right | ✓ |
| `RollCamera` | Roll Axis | ✓ |
| `RollCameraLeft` | Roll Left | ✓ |
| `RollCameraRight` | Roll Right | ✓ |
| `ToggleRotationLock` | Stabiliser On/Off Toggle | ✓ |
| `FixCameraRelativeToggle` | Camera / Ship Controls Toggle | ✓ |
| `FixCameraWorldToggle` | Attach / Detach Camera | ✓ |
| `QuitCamera` | Exit Free Camera | ✓ |
| `ToggleAdvanceMode` | Zoom / Blur Toggle | ✓ |
| `FreeCamZoomIn` | Increase Zoom/Focus | ✓ |
| `FreeCamZoomOut` | Decrease Zoom/Focus | ✓ |
| `FStopDec` | Decrease Blur | ✓ |
| `FStopInc` | Increase Blur | ✓ |

### §1.5 Holo-Me
| XML Element | UI Name in Doc | Status |
|---|---|---|
| `CommanderCreator_Undo` | Undo | ✓ |
| `CommanderCreator_Redo` | Redo | ✓ |
| `CommanderCreator_Rotation_MouseToggle` | Toggle Mouse Rotation | ✓ |
| `CommanderCreator_Rotation` | Rotate Camera | ✓ |

### §1.6 Playlist
| XML Element | UI Name in Doc | Status |
|---|---|---|
| `GalnetAudio_Play_Pause` | Play / Pause | ✓ |
| `GalnetAudio_SkipForward` | Skip Forward | ✓ |
| `GalnetAudio_SkipBackward` | Skip Backward | ✓ |
| `GalnetAudio_ClearQueue` | Clear Queue | ✓ |

### §1.7 Store Camera
| XML Element | UI Name in Doc | Status |
|---|---|---|
| `StoreEnableRotation` | Hold To Rotate | ✓ |
| `StorePitchCamera` | Pitch Axis | ✓ |
| `StoreYawCamera` | Yaw Axis | ✓ |
| `StoreCamZoomIn` | Store Camera Zoom In | ✓ |
| `StoreCamZoomOut` | Store Camera Zoom Out | ✓ |
| `StoreToggle` | Store Toggle | ✓ |

### §1.8 System Colonisation Facility Placement
| XML Element | UI Name in Doc | Status |
|---|---|---|
| `PlaceSettlement` | Place Facility | ✓ |
| `ChangeConstructionOption` | Change Construction Option | ✓ |
| `RotateSettlement` | Rotate Facility | ✓ |
| `RotateSettlementLeft` | Rotate Facility Left | ✓ |
| `RotateSettlementRight` | Rotate Facility Right | ✓ |
| `ExitSettlementPlacementCamera` | Exit Placement Camera | ✓ |
| `MovePlacementCamY` | Forward Axis | ✓ |
| `MovePlacementCamForward` | Move Forward | ✓ |
| `MovePlacementCamBackwards` | Move Backward | ✓ |
| `MovePlacementCamX` | Lateral Axis | ✓ |
| `MovePlacementCamRight` | Move Right | ✓ |
| `MovePlacementCamLeft` | Move Left | ✓ |
| `MovePlacementCamZ` | Lift Axis | ✓ |
| `MovePlacementCamUp` | Move Up | ✓ |
| `MovePlacementCamDown` | Move Down | ✓ |
| `MovePlacementCamUpAxis` | Move Up [Analogue] | ✓ |
| `MovePlacementCamDownAxis` | Move Down [Analogue] | ✓ |
| `PitchPlacementCamera` | Pitch Axis | ✓ |
| `PitchPlacementCameraUp` | Pitch Up | ✓ |
| `PitchPlacementCameraDown` | Pitch Down | ✓ |
| `YawPlacementCamera` | Yaw Axis | ✓ |
| `YawPlacementCameraLeft` | Yaw Left | ✓ |
| `YawPlacementCameraRight` | Yaw Right | ✓ |
| `PlacementCamSpeedInc` | Increase Speed | ✓ |
| `PlacementCamSpeedDec` | Decrease Speed | ✓ |

### §2.1 Mouse Controls
| XML Element | UI Name in Doc | Status |
|---|---|---|
| `MouseXMode` | Mouse X-Axis `[selector: YAW / PITCH]` | settings-only |
| `MouseXDecay` | Relative Mouse X-Axis `[toggle: ON/OFF]` | settings-only |
| `MouseYMode` | Mouse Y-Axis `[selector: PITCH]` | settings-only |
| `MouseYDecay` | Relative Mouse Y-Axis `[toggle: ON/OFF]` | settings-only |
| `MouseReset` | Reset Mouse `[toggle: ON/OFF]` | settings-only in UI / bindable in XML (see §2B) |
| `MouseSensitivity` | Mouse Sensitivity `[slider]` | settings-only |
| `MouseDecayRate` | Relative Mouse Rate `[slider]` | settings-only |
| `MouseDeadzone` | Mouse Deadzone `[slider]` | settings-only |
| `MouseLinearity` | Mouse Power Curve `[slider]` | settings-only |
| `MouseGUI` | Show Mouse Widget `[toggle: ON/OFF]` | settings-only |
| `BlockMouseDecay` | Disable Relative Mouse `[toggle: ON/OFF]` | settings-only in UI / bindable in XML (see §2B) |
| *(none identified)* | Button Mode `[dropdown: HOLD]` | settings-only (no XML element identified) |

### §2.2 Flight Rotation
| XML Element | UI Name in Doc | Status |
|---|---|---|
| `YawAxisRaw` | Yaw Axis | ✓ |
| `YawLeftButton` | Yaw Left | ✓ |
| `YawRightButton` | Yaw Right | ✓ |
| `YawToRollButton` | Yaw Roll Button | ✓ |
| `RollAxisRaw` | Roll Axis | ✓ |
| `RollLeftButton` | Roll Left | ✓ |
| `RollRightButton` | Roll Right | ✓ |

### §2.3 Flight Thrust
| XML Element | UI Name in Doc | Status |
|---|---|---|
| `PitchAxisRaw` | Pitch Axis | ✓ |
| `PitchUpButton` | Pitch Up | ✓ |
| `PitchDownButton` | Pitch Down | ✓ |
| `LateralThrustRaw` | Lateral Thrust Axis | ✓ |
| `RightThrustButton` | Thrust Right | ✓ |
| `LeftThrustButton` | Thrust Left | ✓ |
| `VerticalThrustRaw` | Vertical Thrust Axis | ✓ |
| `UpThrustButton` | Thrust Up | ✓ |
| `DownThrustButton` | Thrust Down | ✓ |
| `AheadThrust` | Thrust Forward and Backward Axis | ✓ |
| `ForwardThrustButton` | Thrust Forward | ✓ |
| `BackwardThrustButton` | Thrust Backward | ✓ |

### §2.4 Alternate Flight Controls
| XML Element | UI Name in Doc | Status |
|---|---|---|
| `UseAlternateFlightValuesToggle` | Alternate Controls Toggle | ✓ |
| `YawAxisAlternate` | Yaw Axis | ✓ |
| `RollAxisAlternate` | Roll Axis | ✓ |
| `PitchAxisAlternate` | Pitch Axis | ✓ |
| `LateralThrustAlternate` | Lateral Thrust Axis | ✓ |
| `VerticalThrustAlternate` | Vertical Thrust Axis | ✓ |

### §2.5 Flight Throttle
| XML Element | UI Name in Doc | Status |
|---|---|---|
| `ThrottleAxis` | Throttle Axis | ✓ |
| `ToggleReverseThrottleInput` | Forward Only Throttle Reverse | ✓ |
| `ForwardKey` | Increase Throttle | ✓ |
| `BackwardKey` | Decrease Throttle | ✓ |
| `SetSpeedMinus100` | Set Speed to -100% | ✓ |
| `SetSpeedMinus75` | Set Speed to -75% | ✓ |
| `SetSpeedMinus50` | Set Speed to -50% | ✓ |
| `SetSpeedMinus25` | Set Speed to -25% | ✓ |
| `SetSpeedZero` | Set Speed to 0% | ✓ |
| `SetSpeed25` | Set Speed to 25% | ✓ |
| `SetSpeed50` | Set Speed to 50% | ✓ |
| `SetSpeed75` | Set Speed to 75% | ✓ |
| `SetSpeed100` | Set Speed to 100% | ✓ |

### §2.6 Flight Landing Overrides
| XML Element | UI Name in Doc | Status |
|---|---|---|
| `YawAxis_Landing` | Yaw Axis | ✓ |
| `YawLeftButton_Landing` | Yaw Left | ✓ |
| `YawRightButton_Landing` | Yaw Right | ✓ |
| `YawToRollMode_Landing` | Yaw Into Roll `[dropdown: DEFAULT TO STANDARD CONTROLS]` | settings-only |
| `PitchAxis_Landing` | Pitch Axis | ✓ |
| `PitchUpButton_Landing` | Pitch Up | ✓ |
| `PitchDownButton_Landing` | Pitch Down | ✓ |
| `RollAxis_Landing` | Roll Axis | ✓ |
| `RollLeftButton_Landing` | Roll Left | ✓ |
| `RollRightButton_Landing` | Roll Right | ✓ |
| `LateralThrust_Landing` | Lateral Thrust Axis | ✓ |
| `LeftThrustButton_Landing` | Thrust Left | ✓ |
| `RightThrustButton_Landing` | Thrust Right | ✓ |
| `VerticalThrust_Landing` | Vertical Thrust Axis | ✓ |
| `UpThrustButton_Landing` | Thrust Up | ✓ |
| `DownThrustButton_Landing` | Thrust Down | ✓ |
| `AheadThrust_Landing` | Thrust Forward and Backward Axis | ✓ |
| `ForwardThrustButton_Landing` | Thrust Forward | ✓ |
| `BackwardThrustButton_Landing` | Thrust Backward | ✓ |

### §2.7 Flight Miscellaneous
| XML Element | UI Name in Doc | Status |
|---|---|---|
| `ToggleFlightAssist` | Toggle Flight Assist | ✓ |
| `UseBoostJuice` | Engine Boost | ✓ |
| `HyperSuperCombination` | Toggle Frame Shift Drive | ✓ |
| `Supercruise` | Supercruise | ✓ |
| `Hyperspace` | Hyperspace Jump | ✓ |
| `DisableRotationCorrectToggle` | Rotational Correction | ✓ |
| `OrbitLinesToggle` | Toggle Orbit Lines | ✓ |

### §2.8 Targeting
| XML Element | UI Name in Doc | Status |
|---|---|---|
| `SelectTarget` | Select Target Ahead | ✓ |
| `CycleNextTarget` | Cycle Next Target | ✓ |
| — | **Select Next Target** | **MISMATCH — no XML counterpart (see §1)** |
| `CyclePreviousTarget` | Cycle Previous Ship | ✓ |
| `SelectHighestThreat` | Select Highest Threat | ✓ |
| `CycleNextHostileTarget` | Cycle Next Hostile Target | ✓ |
| `CyclePreviousHostileTarget` | Cycle Previous Hostile Ship | ✓ |
| `TargetWingman0` | Select Teammate 1 | ✓ |
| `TargetWingman1` | Select Teammate 2 | ✓ |
| `TargetWingman2` | Select Teammate 3 | ✓ |
| `SelectTargetsTarget` | Select Teammate's Target | ✓ |
| `WingNavLock` | Teammate NavLock | ✓ |
| `CycleNextSubsystem` | Cycle Next Subsystem | ✓ |
| `CyclePreviousSubsystem` | Cycle Previous Subsystem | ✓ |
| `TargetNextRouteSystem` | Target Next System in Route | ✓ |

### §2.9 Weapons
| XML Element | UI Name in Doc | Status |
|---|---|---|
| `PrimaryFire` | Primary Fire | ✓ |
| `SecondaryFire` | Secondary Fire | ✓ |
| `CycleFireGroupNext` | Cycle Next Fire Group | ✓ |
| `CycleFireGroupPrevious` | Cycle Previous Fire Group | ✓ |
| `DeployHardpointToggle` | Deploy Hardpoints | ✓ |

### §2.10 Cooling
| XML Element | UI Name in Doc | Status |
|---|---|---|
| `ToggleButtonUpInput` | Silent Running | ✓ |

### §2.11 Miscellaneous
| XML Element | UI Name in Doc | Status |
|---|---|---|
| `ShipSpotLightToggle` | Ship Lights | ✓ |
| `RadarRangeAxis` | Sensor Zoom Axis | ✓ |
| `RadarDecreaseRange` | Decrease Sensor Zoom | ✓ |
| `RadarIncreaseRange` | Increase Sensor Zoom | ✓ |
| `IncreaseEnginesPower` | Divert Power To Engines | ✓ |
| `IncreaseWeaponsPower` | Divert Power To Weapons | ✓ |
| `IncreaseSystemsPower` | Divert Power To Systems | ✓ |
| `ResetPowerDistribution` | Balance Power Distribution | ✓ |
| `HMDReset` | Reset HMD Orientation | ✓ |
| `ToggleCargoScoop` | Cargo Scoop | ✓ |
| `EjectAllCargo` | Jettison All Cargo | ✓ |
| `LandingGearToggle` | Landing Gear | ✓ |
| `MicrophoneMute` | Microphone Mute | ✓ |
| `UseShieldCell` | Use Shield Cell | ✓ |
| `FireChaffLauncher` | Use Chaff Launcher | ✓ |
| `TriggerFieldNeutraliser` | Use Shutdown Field Neutraliser | ✓ |
| `ChargeECM` | Charge ECM | ✓ |
| `WeaponColourToggle` | Weapon Colour | ✓ |
| `EngineColourToggle` | Engine Colour | ✓ |
| `NightVisionToggle` | Night Vision | ✓ |
| `TriggerColonisationModule` | System Colonisation Suite | ✓ |
| `DeployHeatSink` | *(not in §2.10 or §2.11)* | **GAP — see note below** |

> **`DeployHeatSink` note:** This element is bound (Keyboard Numpad_Divide) and represents "Deploy Heat Sink." It does not appear in §2.10 Cooling or §2.11 Miscellaneous. It should appear under §2.10 Cooling alongside Silent Running.

### §2.12 Mode Switches
| XML Element | UI Name in Doc | Status |
|---|---|---|
| `UIFocus` | UI Focus | ✓ |
| `FocusLeftPanel` | External Panel | ✓ |
| `FocusCommsPanel` | Comms Panel | ✓ |
| `QuickCommsPanel` | Quick Comms | ✓ |
| `FocusRadarPanel` | Role Panel | ✓ |
| `FocusRightPanel` | Internal Panel | ✓ |
| `GalaxyMapOpen` | Open Galaxy Map | ✓ |
| `SystemMapOpen` | Open System Map | ✓ |
| `ShowPGScoreSummaryInput` | Show DSS Score Screen | ✓ |
| `HeadLookToggle` | Headlook | ✓ |
| `Pause` | Game Menu | ✓ |
| `FriendsMenu` | Friends Menu | ✓ |
| `OpenCodexGoToDiscovery` | Open Discovery | ✓ |
| `PlayerHUDModeToggle` | Switch Cockpit Mode | ✓ |
| `ExplorationFSSEnter` | Enter Pilot Mode | ✓ |

### §2.13 Headlook Mode
| XML Element | UI Name in Doc | Status |
|---|---|---|
| `HeadLookReset` | Reset Headlook | ✓ |
| `HeadLookPitchUp` | Look Up | ✓ |
| `HeadLookPitchDown` | Look Down | ✓ |
| `HeadLookPitchAxisRaw` | Look Up and Down Axis | ✓ |
| `HeadLookYawLeft` | Look Left | ✓ |
| `HeadLookYawRight` | Look Right | ✓ |
| `HeadLookYawAxis` | Look Left and Right Axis | ✓ |

### §2.14 Multicrew
| XML Element | UI Name in Doc | Status |
|---|---|---|
| `MultiCrewToggleMode` | Mode Toggle | ✓ |
| `MultiCrewPrimaryFire` | Primary Fire | ✓ |
| `MultiCrewSecondaryFire` | Secondary Fire | ✓ |
| `MultiCrewPrimaryUtilityFire` | Primary Utility Fire | ✓ |
| `MultiCrewSecondaryUtilityFire` | Secondary Utility Fire | ✓ |
| `MultiCrewThirdPersonMouseXMode` | Mouse X-Axis `[toggle: ON/OFF]` | settings-only |
| `MultiCrewThirdPersonMouseXDecay` | Relative Mouse X-Axis `[toggle: ON/OFF]` | settings-only |
| `MultiCrewThirdPersonMouseYMode` | Mouse Y-Axis `[toggle: ON/OFF]` | settings-only |
| `MultiCrewThirdPersonMouseYDecay` | Relative Mouse Y-Axis `[toggle: ON/OFF]` | settings-only |
| `MultiCrewThirdPersonYawAxisRaw` | Third-Person Yaw Axis | ✓ |
| `MultiCrewThirdPersonYawLeftButton` | Third-Person Yaw Left | ✓ |
| `MultiCrewThirdPersonYawRightButton` | Third-Person Yaw Right | ✓ |
| `MultiCrewThirdPersonPitchAxisRaw` | Third-Person Pitch Axis | ✓ |
| `MultiCrewThirdPersonPitchUpButton` | Third-Person Pitch Up | ✓ |
| `MultiCrewThirdPersonPitchDownButton` | Third-Person Pitch Down | ✓ |
| `MultiCrewThirdPersonMouseSensitivity` | Multi-Crew Mouse Sensitivity `[slider]` | settings-only |
| `MultiCrewThirdPersonFovAxisRaw` | Third-Person Field of View Axis | ✓ |
| `MultiCrewThirdPersonFovOutButton` | Third-Person Field of View Out | ✓ |
| `MultiCrewThirdPersonFovInButton` | Third-Person Field of View In | ✓ |
| `MultiCrewCockpitUICycleForward` | Cycle Cockpit UI Forwards | ✓ |
| `MultiCrewCockpitUICycleBackward` | Cycle Cockpit UI Backwards | ✓ |

### §2.15 Fighter Orders
| XML Element | UI Name in Doc | Status |
|---|---|---|
| `OrderRequestDock` | Recall Fighter | ✓ |
| `OrderDefensiveBehaviour` | Defend | ✓ |
| `OrderAggressiveBehaviour` | Engage at Will | ✓ |
| `OrderFocusTarget` | Attack Target | ✓ |
| `OrderHoldFire` | Maintain Formation | ✓ |
| `OrderHoldPosition` | Hold Position | ✓ |
| `OrderFollow` | Follow Me | ✓ |
| `OpenOrders` | Open Orders | ✓ |

### §2.16 Full Spectrum System Scanner
| XML Element | UI Name in Doc | Status |
|---|---|---|
| `ExplorationFSSCameraPitch` | Camera Pitch | ✓ |
| `ExplorationFSSCameraPitchIncreaseButton` | Camera Pitch Increase | ✓ |
| `ExplorationFSSCameraPitchDecreaseButton` | Camera Pitch Decrease | ✓ |
| `ExplorationFSSCameraYaw` | Camera Yaw | ✓ |
| `ExplorationFSSCameraYawIncreaseButton` | Camera Yaw Increase | ✓ |
| `ExplorationFSSCameraYawDecreaseButton` | Camera Yaw Decrease | ✓ |
| `ExplorationFSSZoomIn` | Zoom In Target | ✓ |
| `ExplorationFSSZoomOut` | Zoom Out | ✓ |
| `ExplorationFSSMiniZoomIn` | Stepped Zoom In | ✓ |
| `ExplorationFSSMiniZoomOut` | Stepped Zoom Out | ✓ |
| `ExplorationFSSRadioTuningX_Raw` | Tuning | ✓ |
| `ExplorationFSSRadioTuningX_Increase` | Tuning Right | ✓ |
| `ExplorationFSSRadioTuningX_Decrease` | Tuning Left | ✓ |
| `ExplorationFSSRadioTuningAbsoluteX` | Absolute Tuning | ✓ |
| `FSSTuningSensitivity` | FSS Tuning Sensitivity `[slider]` | settings-only |
| `ExplorationFSSDiscoveryScan` | Discovery Scan | ✓ |
| `ExplorationFSSQuit` | Leave FSS | ✓ |
| `FSSMouseXMode` | Mouse X-Axis `[toggle: ON/OFF]` | settings-only |
| `FSSMouseXDecay` | Relative Mouse X-Axis `[toggle: ON/OFF]` | settings-only |
| `FSSMouseYMode` | Mouse Y-Axis `[toggle: ON/OFF]` | settings-only |
| `FSSMouseYDecay` | Relative Mouse Y-Axis `[toggle: ON/OFF]` | settings-only |
| `FSSMouseSensitivity` | Mouse Sensitivity `[slider]` | settings-only |
| `FSSMouseDeadzone` | FSS Mouse Deadzone `[slider]` | settings-only |
| `FSSMouseLinearity` | FSS Mouse Power Curve `[slider]` | settings-only |
| `ExplorationFSSTarget` | Target Current Signal | ✓ |
| `ExplorationFSSShowHelp` | Show Help | ✓ |

### §2.17 Detailed Surface Scanner
| XML Element | UI Name in Doc | Status |
|---|---|---|
| `ExplorationSAAChangeScannedAreaViewToggle` | Toggle Front/Back View | ✓ |
| `ExplorationSAAExitThirdPerson` | Exit Mode | ✓ |
| `ExplorationSAANextGenus` | Next Filter | ✓ |
| `ExplorationSAAPreviousGenus` | Previous Filter | ✓ |
| `SAAThirdPersonMouseXMode` | Mouse X-Axis `[toggle: ON/OFF]` | settings-only |
| `SAAThirdPersonMouseXDecay` | Relative Mouse X-Axis `[toggle: ON/OFF]` | settings-only |
| `SAAThirdPersonMouseYMode` | Mouse Y-Axis `[toggle: ON/OFF]` | settings-only |
| `SAAThirdPersonMouseYDecay` | Relative Mouse Y-Axis `[toggle: ON/OFF]` | settings-only |
| `SAAThirdPersonMouseSensitivity` | DSS Mouse Sensitivity `[slider]` | settings-only |
| `SAAThirdPersonYawAxisRaw` | Third-Person Yaw Axis | ✓ |
| `SAAThirdPersonYawLeftButton` | Third-Person Yaw Left | ✓ |
| `SAAThirdPersonYawRightButton` | Third-Person Yaw Right | ✓ |
| `SAAThirdPersonPitchAxisRaw` | Third-Person Pitch Axis | ✓ |
| `SAAThirdPersonPitchUpButton` | Third-Person Pitch Up | ✓ |
| `SAAThirdPersonPitchDownButton` | Third-Person Pitch Down | ✓ |
| `SAAThirdPersonFovAxisRaw` | Third-Person Field of View Axis | ✓ |
| `SAAThirdPersonFovOutButton` | Third-Person Field of View Out | ✓ |
| `SAAThirdPersonFovInButton` | Third-Person Field of View In | ✓ |

### §3.1 Driving
| XML Element | UI Name in Doc | Status |
|---|---|---|
| `ToggleDriveAssist` | Drive Assist | ✓ |
| `DriveAssistDefault` | Drive Assist Default | settings-only |
| `MouseBuggySteeringXMode` | SRV Steering Mouse X-Axis `[toggle: ON/OFF]` | settings-only |
| `MouseBuggySteeringXDecay` | Relative Mouse X-Axis `[toggle: ON/OFF]` | settings-only |
| `MouseBuggyRollingXMode` | SRV Rolling Mouse X-Axis `[toggle: ON/OFF]` | settings-only |
| `MouseBuggyRollingXDecay` | Relative Mouse X-Roll `[toggle: ON/OFF]` | settings-only |
| `MouseBuggyYMode` | SRV Pitch Mouse Y-Axis `[toggle: ON/OFF]` | settings-only |
| `MouseBuggyYDecay` | Relative Mouse Y-Pitch `[toggle: ON/OFF]` | settings-only |
| `SteeringAxis` | Steering Axis | ✓ |
| `SteerLeftButton` | Steering Left Button | ✓ |
| `SteerRightButton` | Steering Right Button | ✓ |
| `BuggyRollAxisRaw` | Roll Axis | ✓ |
| `BuggyRollLeftButton` | Roll Left Button | ✓ |
| `BuggyRollRightButton` | Roll Right Button | ✓ |
| `BuggyPitchAxis` | Pitch Axis | ✓ |
| `BuggyPitchUpButton` | Pitch Up Button | ✓ |
| `BuggyPitchDownButton` | Pitch Down Button | ✓ |
| `VerticalThrustersButton` | Vertical Thrusters | ✓ |
| `BuggyPrimaryFireButton` | SRV Primary Fire | ✓ |
| `BuggySecondaryFireButton` | SRV Secondary Fire | ✓ |
| `AutoBreakBuggyButton` | Handbrake | ✓ |
| `HeadlightsBuggyButton` | Headlights | ✓ |
| `ToggleBuggyTurretButton` | Toggle SRV Turret | ✓ |
| `BuggyCycleFireGroupNext` | Cycle Next Fire Group | ✓ |
| `BuggyCycleFireGroupPrevious` | Cycle Previous Fire Group | ✓ |

### §3.2 Driving Targeting
| XML Element | UI Name in Doc | Status |
|---|---|---|
| `SelectTarget_Buggy` | Select Target Ahead | ✓ |

### §3.3 Driving Turret Controls
| XML Element | UI Name in Doc | Status |
|---|---|---|
| `MouseTurretXMode` | Turret Mouse X-Axis `[selector: YAW]` | settings-only |
| `MouseTurretXDecay` | Turret Relative Mouse X-Axis `[toggle: ON/OFF]` | settings-only |
| `MouseTurretYMode` | Turret Mouse Y-Axis `[selector: PITCH INVERTED]` | settings-only |
| `MouseTurretYDecay` | Turret Relative Mouse Y-Axis `[toggle: ON/OFF]` | settings-only |
| `BuggyTurretYawAxisRaw` | SRV Turret Yaw Axis | ✓ |
| `BuggyTurretYawLeftButton` | SRV Turret Yaw Left | ✓ |
| `BuggyTurretYawRightButton` | SRV Turret Yaw Right | ✓ |
| `BuggyTurretPitchAxisRaw` | SRV Turret Pitch Axis | ✓ |
| `BuggyTurretPitchUpButton` | SRV Turret Pitch Up | ✓ |
| `BuggyTurretPitchDownButton` | SRV Turret Pitch Down | ✓ |
| `BuggyTurretMouseSensitivity` | SRV Turret Mouse Sensitivity `[slider]` | settings-only |
| `BuggyTurretMouseDeadzone` | SRV Turret Mouse Deadzone `[slider]` | settings-only |
| `BuggyTurretMouseLinearity` | SRV Turret Mouse Power Curve `[slider]` | settings-only |

### §3.4 Drive Throttle
| XML Element | UI Name in Doc | Status |
|---|---|---|
| `DriveSpeedAxis` | Drive Speed Axis | ✓ |
| `BuggyToggleReverseThrottleInput` | Forward Only Throttle Reverse | ✓ |
| `IncreaseSpeedButtonMax` | Accelerate Button | ✓ |
| `DecreaseSpeedButtonMax` | Decelerate Button | ✓ |
| `IncreaseSpeedButtonPartial` | Accelerate Axis | ✓ |
| `DecreaseSpeedButtonPartial` | Decelerate Axis | ✓ |

### §3.5 Driving Miscellaneous
| XML Element | UI Name in Doc | Status |
|---|---|---|
| `IncreaseEnginesPower_Buggy` | Divert Power To Engines | ✓ |
| `IncreaseWeaponsPower_Buggy` | Divert Power To Weapons | ✓ |
| `IncreaseSystemsPower_Buggy` | Divert Power To Systems | ✓ |
| `ResetPowerDistribution_Buggy` | Balance Power Distribution | ✓ |
| `ToggleCargoScoop_Buggy` | Cargo Scoop | ✓ |
| `EjectAllCargo_Buggy` | Jettison All Cargo | ✓ |
| `RecallDismissShip` | Recall/Dismiss Ship | ✓ |
| `EnableMenuGroupsSRV` | Enable Context Menu in SRV `[toggle: ON/OFF]` | settings-only |

### §3.6 Driving Mode Switches
| XML Element | UI Name in Doc | Status |
|---|---|---|
| `UIFocus_Buggy` | UI Focus | ✓ |
| `FocusLeftPanel_Buggy` | External Panel | ✓ |
| `FocusCommsPanel_Buggy` | Comms Panel | ✓ |
| `QuickCommsPanel_Buggy` | Quick Comms | ✓ |
| `FocusRadarPanel_Buggy` | Role Panel | ✓ |
| `FocusRightPanel_Buggy` | Internal Panel | ✓ |
| `GalaxyMapOpen_Buggy` | Open Galaxy Map | ✓ |
| `SystemMapOpen_Buggy` | Open System Map | ✓ |
| `OpenCodexGoToDiscovery_Buggy` | Open Discovery | ✓ |
| `PlayerHUDModeToggle_Buggy` | Switch Cockpit Mode | ✓ |
| `HeadLookToggle_Buggy` | Headlook | ✓ |

### §4.1 On Foot
| XML Element | UI Name in Doc | Status |
|---|---|---|
| `MouseHumanoidXMode` | Mouse X-Axis `[selector: ROTATE]` | settings-only |
| `MouseHumanoidYMode` | Mouse Y-Axis `[selector: PITCH]` | settings-only |
| `MouseHumanoidSensitivity` | Mouse Sensitivity `[slider]` | settings-only |
| `HumanoidForwardAxis` | Forward Axis | ✓ |
| `HumanoidForwardButton` | Move Forward | ✓ |
| `HumanoidBackwardButton` | Move Backward | ✓ |
| `HumanoidStrafeAxis` | Strafe Axis | ✓ |
| `HumanoidStrafeLeftButton` | Strafe Left | ✓ |
| `HumanoidStrafeRightButton` | Strafe Right | ✓ |
| `HumanoidRotateAxis` | Rotate Axis | ✓ |
| `HumanoidRotateSensitivity` | Rotate Sensitivity `[slider]` | settings-only |
| `HumanoidRotateLeftButton` | Turn Left | ✓ |
| `HumanoidRotateRightButton` | Turn Right | ✓ |
| `HumanoidPitchAxis` | Pitch Axis | ✓ |
| `HumanoidPitchSensitivity` | Pitch Sensitivity `[slider]` | settings-only |
| `HumanoidPitchUpButton` | Look Up | ✓ |
| `HumanoidPitchDownButton` | Look Down | ✓ |
| `HumanoidSprintButton` | Sprint | ✓ |
| `HumanoidWalkButton` | Walk | ✓ |
| `HumanoidCrouchButton` | Crouch | ✓ |
| `HumanoidJumpButton` | Jump | ✓ |
| `HumanoidPrimaryInteractButton` | Interact | ✓ |
| `HumanoidSecondaryInteractButton` | Secondary Interact | ✓ |
| `HumanoidItemWheelButton` | Open Item Menu | ✓ |
| `HumanoidEmoteWheelButton` | Open Emote Wheel | ✓ |
| `HumanoidUtilityWheelCycleMode` | Cycle Utility Wheel Mode | ✓ |
| `HumanoidItemWheelButton_XAxis` | Wheel Horizontal | ✓ |
| `HumanoidItemWheelButton_XLeft` | Wheel Left | ✓ |
| `HumanoidItemWheelButton_XRight` | Wheel Right | ✓ |
| `HumanoidItemWheelButton_YAxis` | Wheel Vertical | ✓ |
| `HumanoidItemWheelButton_YUp` | Wheel Up | ✓ |
| `HumanoidItemWheelButton_YDown` | Wheel Down | ✓ |
| `HumanoidItemWheel_AcceptMouseInput` | Wheel Accepts Mouse Input `[toggle: ON/OFF]` | settings-only |
| `HumanoidPrimaryFireButton` | Fire Weapon/Use Tool | ✓ |
| `HumanoidZoomButton` | Aim Down Sights | ✓ |
| `HumanoidThrowGrenadeButton` | Throw Grenade | ✓ |
| `HumanoidMeleeButton` | Melee Attack | ✓ |
| `HumanoidReloadButton` | Reload | ✓ |
| `HumanoidSwitchWeapon` | Switch Weapon | ✓ |
| `HumanoidSelectPrimaryWeaponButton` | Select Primary Weapon | ✓ |
| `HumanoidSelectSecondaryWeaponButton` | Select Secondary Weapon | ✓ |
| `HumanoidSelectUtilityWeaponButton` | Select Tool | ✓ |
| `HumanoidSelectNextWeaponButton` | Select Next Weapon | ✓ |
| `HumanoidSelectPreviousWeaponButton` | Select Previous Weapon | ✓ |
| `HumanoidHideWeaponButton` | Holster Weapon | ✓ |
| `HumanoidSelectNextGrenadeTypeButton` | Select Next Grenade Type | ✓ |
| `HumanoidSelectPreviousGrenadeTypeButton` | Select Previous Grenade Type | ✓ |
| `HumanoidToggleFlashlightButton` | Toggle Flashlight | ✓ |
| `HumanoidToggleNightVisionButton` | Toggle Night Vision | ✓ |
| `HumanoidToggleShieldsButton` | Toggle Shields | ✓ |
| `HumanoidClearAuthorityLevel` | Clear Authority Level | ✓ |
| `HumanoidHealthPack` | Use Health Pack | ✓ |
| `HumanoidBattery` | Use Energy Cell | ✓ |
| `HumanoidSelectFragGrenade` | Select Frag Grenade | ✓ |
| `HumanoidSelectEMPGrenade` | Select EMP Grenade | ✓ |
| `HumanoidSelectShieldGrenade` | Select Shield Grenade | ✓ |
| `HumanoidSwitchToRechargeTool` | Select Energylink | ✓ |
| `HumanoidSwitchToCompAnalyser` | Select Profile Analyser | ✓ |
| `HumanoidSwitchToSuitTool` | Select Suit Specific Tool | ✓ |
| `HumanoidToggleToolModeButton` | Toggle Tool Mode | ✓ |
| `HumanoidToggleMissionHelpPanelButton` | Toggle Help | ✓ |
| `HumanoidPing` | *(missing from doc — see §2A above)* | **GAP** |

### §4.2 On Foot Mode Switches
| XML Element | UI Name in Doc | Status |
|---|---|---|
| `GalaxyMapOpen_Humanoid` | Open Galaxy Map | ✓ |
| `SystemMapOpen_Humanoid` | Open System Map | ✓ |
| `FocusCommsPanel_Humanoid` | Comms Panel | ✓ |
| `QuickCommsPanel_Humanoid` | Quick Comms | ✓ |
| `HumanoidOpenAccessPanelButton` | Open Insight Hub | ✓ |
| `HumanoidConflictContextualUIButton` | Open Conflict Zone Battle Stats | ✓ |

### §4.3 On Foot Miscellaneous
| XML Element | UI Name in Doc | Status |
|---|---|---|
| `EnableMenuGroupsOnFoot` | Enable Context Menu on Foot `[toggle: ON/OFF]` | settings-only |
| `EnableAimAssistOnFoot` | Enable Aim Assist `[toggle: ON/OFF]` | settings-only |

### §4.4 On Foot Emotes
| XML Element | UI Name in Doc | Status |
|---|---|---|
| `HumanoidEmoteSlot1` | Point | ✓ |
| `HumanoidEmoteSlot2` | Wave | ✓ |
| `HumanoidEmoteSlot3` | Agree | ✓ |
| `HumanoidEmoteSlot4` | Disagree | ✓ |
| `HumanoidEmoteSlot5` | Go | ✓ |
| `HumanoidEmoteSlot6` | Stop | ✓ |
| `HumanoidEmoteSlot7` | Applaud | ✓ |
| `HumanoidEmoteSlot8` | Salute | ✓ |

---

## Summary

| Category | Count | Items |
|---|---|---|
| Doc entries with no XML match | 1 | "Select Next Target" (§2.8) |
| XML elements completely absent from doc | 2 | `UI_Select`, `HumanoidPing` |
| XML elements present in doc but binding-slot status wrong | 2 | `MouseReset`, `BlockMouseDecay` (§2.1 says no slots; XML has them) |
| XML bindable element in doc wrong section / missing from section | 1 | `DeployHeatSink` missing from §2.10 Cooling |
| Everything else | ~270 elements | Verified ✓ |

### Action items for BindForge_GameMode_SubGroups.md

1. **§1.1 Interface Mode** — Add `UI_Select` ("Select / Confirm")
2. **§2.1 Mouse Controls** — Amend the "no binding slots" note: `MouseReset` and `BlockMouseDecay` (`BlockMouseDecay` = "Disable Relative Mouse") technically have binding slots in the XML, though the in-game UI may not expose them visibly
3. **§2.8 Targeting** — Remove the duplicate "Select Next Target" entry; only "Cycle Next Target" (`CycleNextTarget`) exists in the XML
4. **§2.10 Cooling** — Add `DeployHeatSink` ("Deploy Heat Sink"), which is bound to Numpad_Divide in this profile
5. **§4.1 On Foot** (or §4.3 Miscellaneous) — Add `HumanoidPing` ("Ping / Call Out"), currently unbound but present in the XML
