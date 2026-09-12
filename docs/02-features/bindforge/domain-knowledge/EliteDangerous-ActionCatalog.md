# Elite Dangerous — Action Catalog (Groups, Subgroups, and In-Game Name → XML Element Mapping)

**Scope:** The full category/subgroup structure of the in-game Controls UI, and the corresponding catalog mapping each in-game action name to its `.binds` XML element name, its group, and its subgroup. See `EliteDangerous-BindsFileFormat.md` for what the XML element shapes themselves mean once you have the element name.

**IMPORTANT — accuracy caution:** This catalog is the result of a careful cross-reference between in-game UI transcription and a real `.binds` file's structural extraction, with known discrepancies between the two already investigated and corrected. It is **good, but not proven perfect**. At least one naming correction was already caught *after* an earlier pass had explicitly concluded everything in this table was verified: `ExplorationFSSEnter` was originally recorded as "Enter Pilot Mode" and marked as a confirmed, non-flagged match — a fresh in-game screenshot later showed the real row label is "Enter FSS Mode." That correction was only caught because the row happened to get used for something real (an in-game conflict test). **Treat any single row in this table as probably correct, not certainly correct.** Anything load-bearing built on this table — a UI label, a conflict-detection key, a display name — should be spot-checked against a live game screenshot first, especially before shipping it as user-facing text.

**Top-level groups are exactly four, confirmed directly from in-game screenshots:** General Controls, Ship Controls, SRV Controls, On Foot Controls. There is no separate fifth "Settlement" category — settlement/colonisation-facility actions are a *subgroup inside General Controls*, not their own top-level group.

---

## 1. Subgroup Structure Overview

### General Controls
1. Interface Mode
2. Galaxy Map
3. Camera Suite
4. Free Camera
5. Holo-Me
6. Playlist
7. Store Camera
8. System Colonisation Facility Placement

### Ship Controls
1. Mouse Controls
2. Flight Rotation
3. Flight Thrust
4. Alternate Flight Controls
5. Flight Throttle
6. Flight Landing Overrides
7. Flight Miscellaneous
8. Targeting
9. Weapons
10. Cooling
11. Miscellaneous
12. Mode Switches
13. Headlook Mode
14. Multicrew
15. Fighter Orders
16. Full Spectrum System Scanner
17. Detailed Surface Scanner

### SRV Controls
1. Driving
2. Driving Targeting
3. Driving Turret Controls
4. Drive Throttle
5. Driving Miscellaneous
6. Driving Mode Switches

### On Foot Controls
1. On Foot
2. On Foot Mode Switches
3. On Foot Miscellaneous
4. On Foot Emotes

35 subgroups total across the 4 groups.

### Actions that appear under more than one group/subgroup

The **in-game name** is often reused across ship/SRV/on-foot contexts, but each context almost always has its **own distinct XML element** (frequently the same base name with a mode-specific suffix, e.g. `_Buggy` for SRV, `_Humanoid` for on-foot). Never assume a shared in-game label means a shared XML element — always resolve through the full group/subgroup mapping in §2.

**The suffix only applies where a counterpart exists** (noted 2026-09-12). `<ShipAction>_Buggy` means "the SRV's version of this ship control", so stripping the suffix yields its ship twin. **Controls the SRV has and the ship does not — the drive and turret controls — are spelled `Buggy*` as a prefix instead, and have no twin to strip to.** Any code deriving a ship action by removing `_Buggy` must return nothing for those rather than inventing a control, which is what `BindingConflictRules.shipTwinOf` does.

Reused labels observed:

| In-Game Name | Appears In |
|---|---|
| Primary Fire | Ship Controls → Weapons; Ship Controls → Multicrew |
| Secondary Fire | Ship Controls → Weapons; Ship Controls → Multicrew |
| Divert Power To Engines / Weapons / Systems, Balance Power Distribution | Ship Controls → Miscellaneous; SRV Controls → Driving Miscellaneous |
| Cargo Scoop | Ship Controls → Miscellaneous; SRV Controls → Driving Miscellaneous |
| Jettison All Cargo | Ship Controls → Miscellaneous; SRV Controls → Driving Miscellaneous |
| Comms Panel, Quick Comms, Open Galaxy Map, Open System Map | Ship Controls → Mode Switches; SRV Controls → Driving Mode Switches; On Foot Controls → On Foot Mode Switches |
| Open Discovery | Ship Controls → Mode Switches; SRV Controls → Driving Mode Switches |

---

## 2. Full Action Catalog

**Columns:** In-Game Name (as transcribed from the Controls UI) · XML Element (the `.binds` element name) · Bindable (`Yes` = has a `<Primary>`/`<Secondary>`/`<Binding>` slot and can hold a key; `Settings-only` = a bare `Value=` element with no binding slot at all, cannot conflict with anything).

### General Controls

#### Interface Mode

| In-Game Name | XML Element | Bindable |
|---|---|---|
| UI Panel Up | `UI_Up` | Yes |
| UI Panel Down | `UI_Down` | Yes |
| UI Panel Left | `UI_Left` | Yes |
| UI Panel Right | `UI_Right` | Yes |
| UI Panel Select | `UI_Select` | Yes |
| UI Back | `UI_Back` | Yes |
| UI Nested Toggle | `UI_Toggle` | Yes |
| Next Panel Tab | `CycleNextPanel` | Yes |
| Previous Panel Tab | `CyclePreviousPanel` | Yes |
| Next Page | `CycleNextPage` | Yes |
| Previous Page | `CyclePreviousPage` | Yes |

#### Galaxy Map

| In-Game Name | XML Element | Bindable |
|---|---|---|
| Galaxy Cam Pitch Axis | `CamPitchAxis` | Yes |
| Galaxy Cam Pitch Up | `CamPitchUp` | Yes |
| Galaxy Cam Pitch Down | `CamPitchDown` | Yes |
| Galaxy Cam Yaw Axis | `CamYawAxis` | Yes |
| Galaxy Cam Yaw Left | `CamYawLeft` | Yes |
| Galaxy Cam Yaw Right | `CamYawRight` | Yes |
| Galaxy Cam Translate Y-Axis | `CamTranslateYAxis` | Yes |
| Galaxy Cam Translate Forward | `CamTranslateForward` | Yes |
| Galaxy Cam Translate Backward | `CamTranslateBackward` | Yes |
| Galaxy Cam Translate X-Axis | `CamTranslateXAxis` | Yes |
| Galaxy Cam Translate Left | `CamTranslateLeft` | Yes |
| Galaxy Cam Translate Right | `CamTranslateRight` | Yes |
| Galaxy Cam Translate Z-Axis | `CamTranslateZAxis` | Yes |
| Galaxy Cam Translate Up | `CamTranslateUp` | Yes |
| Galaxy Cam Translate Down | `CamTranslateDown` | Yes |
| Galaxy Cam Zoom Axis | `CamZoomAxis` | Yes |
| Galaxy Cam Zoom In | `CamZoomIn` | Yes |
| Galaxy Cam Zoom Out | `CamZoomOut` | Yes |
| Galaxy Cam Set Y-Axis to Z-Axis | `CamTranslateZHold` | Yes |
| Galaxy Cam Select Current System | `GalaxyMapHome` | Yes |

#### Camera Suite

| In-Game Name | XML Element | Bindable |
|---|---|---|
| Ship - Toggle Camera Suite | `PhotoCameraToggle` | Yes |
| SRV - Toggle Camera Suite | `PhotoCameraToggle_Buggy` | Yes |
| Commander - Toggle Camera Suite | `PhotoCameraToggle_Humanoid` | Yes |
| Previous Camera | `VanityCameraScrollLeft` | Yes |
| Next Camera | `VanityCameraScrollRight` | Yes |
| Enter Free Camera | `ToggleFreeCam` | Yes |
| Camera - Cockpit Front | `VanityCameraOne` | Yes |
| Camera - Cockpit Back | `VanityCameraTwo` | Yes |
| Camera - CMDR 1 | `VanityCameraThree` | Yes |
| Camera - CMDR 2 | `VanityCameraFour` | Yes |
| Camera - Co-Pilot 1 | `VanityCameraFive` | Yes |
| Camera - Co-Pilot 2 | `VanityCameraSix` | Yes |
| Camera - Co-Pilot 3 | `VanityCameraSeven` | Yes |
| Camera - Front | `VanityCameraEight` | Yes |
| Camera - Back | `VanityCameraNine` | Yes |
| Camera - Low | `VanityCameraTen` | Yes |

#### Free Camera

| In-Game Name | XML Element | Bindable |
|---|---|---|
| Toggle HUD | `FreeCamToggleHUD` | Yes |
| Increase Speed | `FreeCamSpeedInc` | Yes |
| Decrease Speed | `FreeCamSpeedDec` | Yes |
| Forward Axis | `MoveFreeCamY` | Yes |
| Throttle Axis Range | `ThrottleRangeFreeCam` | Settings-only |
| Forward Only Throttle Reverse | `ToggleReverseThrottleInputFreeCam` | Yes |
| Move Forward | `MoveFreeCamForward` | Yes |
| Move Backward | `MoveFreeCamBackwards` | Yes |
| Lateral Axis | `MoveFreeCamX` | Yes |
| Move Right | `MoveFreeCamRight` | Yes |
| Move Left | `MoveFreeCamLeft` | Yes |
| Lift Axis | `MoveFreeCamZ` | Yes |
| Move Up [Analogue] | `MoveFreeCamUpAxis` | Yes |
| Move Down [Analogue] | `MoveFreeCamDownAxis` | Yes |
| Move Up | `MoveFreeCamUp` | Yes |
| Move Down | `MoveFreeCamDown` | Yes |
| Pitch Axis | `PitchCamera` | Yes |
| Free Camera Mouse Sensitivity | `FreeCamMouseSensitivity` | Settings-only |
| Pitch Up | `PitchCameraUp` | Yes |
| Pitch Down | `PitchCameraDown` | Yes |
| Yaw Axis | `YawCamera` | Yes |
| Yaw Left | `YawCameraLeft` | Yes |
| Yaw Right | `YawCameraRight` | Yes |
| Roll Axis | `RollCamera` | Yes |
| Roll Left | `RollCameraLeft` | Yes |
| Roll Right | `RollCameraRight` | Yes |
| Stabiliser On/Off Toggle | `ToggleRotationLock` | Yes |
| Camera / Ship Controls Toggle | `FixCameraRelativeToggle` | Yes |
| Attach / Detach Camera | `FixCameraWorldToggle` | Yes |
| Exit Free Camera | `QuitCamera` | Yes |
| Zoom / Blur Toggle | `ToggleAdvanceMode` | Yes |
| Increase Zoom/Focus | `FreeCamZoomIn` | Yes |
| Decrease Zoom/Focus | `FreeCamZoomOut` | Yes |
| Decrease Blur | `FStopDec` | Yes |
| Increase Blur | `FStopInc` | Yes |

#### Holo-Me

| In-Game Name | XML Element | Bindable |
|---|---|---|
| Undo | `CommanderCreator_Undo` | Yes |
| Redo | `CommanderCreator_Redo` | Yes |
| Toggle Mouse Rotation | `CommanderCreator_Rotation_MouseToggle` | Yes |
| Rotate Camera | `CommanderCreator_Rotation` | Yes |

#### Playlist

| In-Game Name | XML Element | Bindable |
|---|---|---|
| Play / Pause | `GalnetAudio_Play_Pause` | Yes |
| Skip Forward | `GalnetAudio_SkipForward` | Yes |
| Skip Backward | `GalnetAudio_SkipBackward` | Yes |
| Clear Queue | `GalnetAudio_ClearQueue` | Yes |

#### Store Camera

| In-Game Name | XML Element | Bindable |
|---|---|---|
| Hold To Rotate | `StoreEnableRotation` | Yes |
| Pitch Axis | `StorePitchCamera` | Yes |
| Yaw Axis | `StoreYawCamera` | Yes |
| Store Camera Zoom In | `StoreCamZoomIn` | Yes |
| Store Camera Zoom Out | `StoreCamZoomOut` | Yes |
| Store Toggle | `StoreToggle` | Yes |

#### System Colonisation Facility Placement

| In-Game Name | XML Element | Bindable |
|---|---|---|
| Place Facility | `PlaceSettlement` | Yes |
| Change Construction Option | `ChangeConstructionOption` | Yes |
| Rotate Facility | `RotateSettlement` | Yes |
| Rotate Facility Left | `RotateSettlementLeft` | Yes |
| Rotate Facility Right | `RotateSettlementRight` | Yes |
| Exit Placement Camera | `ExitSettlementPlacementCamera` | Yes |
| Forward Axis | `MovePlacementCamY` | Yes |
| Move Forward | `MovePlacementCamForward` | Yes |
| Move Backward | `MovePlacementCamBackwards` | Yes |
| Lateral Axis | `MovePlacementCamX` | Yes |
| Move Right | `MovePlacementCamRight` | Yes |
| Move Left | `MovePlacementCamLeft` | Yes |
| Lift Axis | `MovePlacementCamZ` | Yes |
| Move Up | `MovePlacementCamUp` | Yes |
| Move Down | `MovePlacementCamDown` | Yes |
| Move Up [Analogue] | `MovePlacementCamUpAxis` | Yes |
| Move Down [Analogue] | `MovePlacementCamDownAxis` | Yes |
| Pitch Axis | `PitchPlacementCamera` | Yes |
| Pitch Up | `PitchPlacementCameraUp` | Yes |
| Pitch Down | `PitchPlacementCameraDown` | Yes |
| Yaw Axis | `YawPlacementCamera` | Yes |
| Yaw Left | `YawPlacementCameraLeft` | Yes |
| Yaw Right | `YawPlacementCameraRight` | Yes |
| Increase Speed | `PlacementCamSpeedInc` | Yes |
| Decrease Speed | `PlacementCamSpeedDec` | Yes |
| Free Camera Mouse Sensitivity | `PlacementCamMouseSensitivity` | Settings-only |

**Note:** `RotateSettlementLeft`/`RotateSettlementRight` live here, under **General Controls**, not Ship Controls — they must not be classified into the same conflict-zone as `RollLeftButton`/`BuggyRollLeftButton` (which are genuinely Ship/SRV, respectively) just because the names sound similar.

---

### Ship Controls

#### Mouse Controls

| In-Game Name | XML Element | Bindable |
|---|---|---|
| Mouse X-Axis | `MouseXMode` | Settings-only |
| Relative Mouse X-Axis | `MouseXDecay` | Settings-only |
| Mouse Y-Axis | `MouseYMode` | Settings-only |
| Relative Mouse Y-Axis | `MouseYDecay` | Settings-only |
| Reset Mouse | `MouseReset` | Yes *(see §3, item 2 — UI shows this as a toggle-only setting, but the XML has a real binding slot)* |
| Mouse Sensitivity | `MouseSensitivity` | Settings-only |
| Relative Mouse Rate | `MouseDecayRate` | Settings-only |
| Mouse Deadzone | `MouseDeadzone` | Settings-only |
| Mouse Power Curve | `MouseLinearity` | Settings-only |
| Show Mouse Widget | `MouseGUI` | Settings-only |
| Disable Relative Mouse | `BlockMouseDecay` | Yes *(see §3, item 2 — same caveat as Reset Mouse)* |

#### Flight Rotation

| In-Game Name | XML Element | Bindable |
|---|---|---|
| Yaw Axis | `YawAxisRaw` | Yes |
| Yaw Left | `YawLeftButton` | Yes |
| Yaw Right | `YawRightButton` | Yes |
| Yaw Into Roll | `YawToRollMode` | Settings-only |
| Yaw Into Roll Sensitivity | `YawToRollSensitivity` | Settings-only |
| Yaw Into Roll - Flight Assist Off | `YawToRollMode_FAOff` | Settings-only |
| Yaw Roll Button | `YawToRollButton` | Yes |
| Roll Axis | `RollAxisRaw` | Yes |
| Roll Left | `RollLeftButton` | Yes |
| Roll Right | `RollRightButton` | Yes |

#### Flight Thrust

| In-Game Name | XML Element | Bindable |
|---|---|---|
| Pitch Axis | `PitchAxisRaw` | Yes |
| Pitch Up | `PitchUpButton` | Yes |
| Pitch Down | `PitchDownButton` | Yes |
| Lateral Thrust Axis | `LateralThrustRaw` | Yes |
| Thrust Right | `RightThrustButton` | Yes |
| Thrust Left | `LeftThrustButton` | Yes |
| Vertical Thrust Axis | `VerticalThrustRaw` | Yes |
| Thrust Up | `UpThrustButton` | Yes |
| Thrust Down | `DownThrustButton` | Yes |
| Thrust Forward and Backward Axis | `AheadThrust` | Yes |
| Thrust Forward | `ForwardThrustButton` | Yes |
| Thrust Backward | `BackwardThrustButton` | Yes |

#### Alternate Flight Controls

| In-Game Name | XML Element | Bindable |
|---|---|---|
| Alternate Controls Toggle | `UseAlternateFlightValuesToggle` | Yes |
| Yaw Axis | `YawAxisAlternate` | Yes |
| Roll Axis | `RollAxisAlternate` | Yes |
| Pitch Axis | `PitchAxisAlternate` | Yes |
| Lateral Thrust Axis | `LateralThrustAlternate` | Yes |
| Vertical Thrust Axis | `VerticalThrustAlternate` | Yes |

#### Flight Throttle

| In-Game Name | XML Element | Bindable |
|---|---|---|
| Throttle Axis | `ThrottleAxis` | Yes |
| Throttle Axis Range | `ThrottleRange` | Settings-only |
| Forward Only Throttle Reverse | `ToggleReverseThrottleInput` | Yes |
| Increase Throttle | `ForwardKey` | Yes |
| Decrease Throttle | `BackwardKey` | Yes |
| Throttle Increments | `ThrottleIncrement` | Settings-only |
| Set Speed to -100% | `SetSpeedMinus100` | Yes |
| Set Speed to -75% | `SetSpeedMinus75` | Yes |
| Set Speed to -50% | `SetSpeedMinus50` | Yes |
| Set Speed to -25% | `SetSpeedMinus25` | Yes |
| Set Speed to 0% | `SetSpeedZero` | Yes |
| Set Speed to 25% | `SetSpeed25` | Yes |
| Set Speed to 50% | `SetSpeed50` | Yes |
| Set Speed to 75% | `SetSpeed75` | Yes |
| Set Speed to 100% | `SetSpeed100` | Yes |

#### Flight Landing Overrides

| In-Game Name | XML Element | Bindable |
|---|---|---|
| Yaw Axis | `YawAxis_Landing` | Yes |
| Yaw Left | `YawLeftButton_Landing` | Yes |
| Yaw Right | `YawRightButton_Landing` | Yes |
| Yaw Into Roll | `YawToRollMode_Landing` | Settings-only |
| Pitch Axis | `PitchAxis_Landing` | Yes |
| Pitch Up | `PitchUpButton_Landing` | Yes |
| Pitch Down | `PitchDownButton_Landing` | Yes |
| Roll Axis | `RollAxis_Landing` | Yes |
| Roll Left | `RollLeftButton_Landing` | Yes |
| Roll Right | `RollRightButton_Landing` | Yes |
| Lateral Thrust Axis | `LateralThrust_Landing` | Yes |
| Thrust Left | `LeftThrustButton_Landing` | Yes |
| Thrust Right | `RightThrustButton_Landing` | Yes |
| Vertical Thrust Axis | `VerticalThrust_Landing` | Yes |
| Thrust Up | `UpThrustButton_Landing` | Yes |
| Thrust Down | `DownThrustButton_Landing` | Yes |
| Thrust Forward and Backward Axis | `AheadThrust_Landing` | Yes |
| Thrust Forward | `ForwardThrustButton_Landing` | Yes |
| Thrust Backward | `BackwardThrustButton_Landing` | Yes |

#### Flight Miscellaneous

| In-Game Name | XML Element | Bindable |
|---|---|---|
| Toggle Flight Assist | `ToggleFlightAssist` | Yes |
| Engine Boost | `UseBoostJuice` | Yes |
| Toggle Frame Shift Drive | `HyperSuperCombination` | Yes |
| Supercruise | `Supercruise` | Yes |
| Hyperspace Jump | `Hyperspace` | Yes |
| Rotational Correction | `DisableRotationCorrectToggle` | Yes |
| Toggle Orbit Lines | `OrbitLinesToggle` | Yes |

#### Targeting

| In-Game Name | XML Element | Bindable |
|---|---|---|
| Select Target Ahead | `SelectTarget` | Yes |
| Cycle Next Target | `CycleNextTarget` | Yes |
| Cycle Previous Ship | `CyclePreviousTarget` | Yes |
| Select Highest Threat | `SelectHighestThreat` | Yes |
| Cycle Next Hostile Target | `CycleNextHostileTarget` | Yes |
| Cycle Previous Hostile Ship | `CyclePreviousHostileTarget` | Yes |
| Select Teammate 1 | `TargetWingman0` | Yes |
| Select Teammate 2 | `TargetWingman1` | Yes |
| Select Teammate 3 | `TargetWingman2` | Yes |
| Select Teammate's Target | `SelectTargetsTarget` | Yes |
| Teammate NavLock | `WingNavLock` | Yes |
| Cycle Next Subsystem | `CycleNextSubsystem` | Yes |
| Cycle Previous Subsystem | `CyclePreviousSubsystem` | Yes |
| Target Next System in Route | `TargetNextRouteSystem` | Yes |

*("Select Next Target" is deliberately omitted — see §3, item 3.)*

#### Weapons

| In-Game Name | XML Element | Bindable |
|---|---|---|
| Primary Fire | `PrimaryFire` | Yes |
| Secondary Fire | `SecondaryFire` | Yes |
| Cycle Next Fire Group | `CycleFireGroupNext` | Yes |
| Cycle Previous Fire Group | `CycleFireGroupPrevious` | Yes |
| Deploy Hardpoints | `DeployHardpointToggle` | Yes |
| Firing Deploys Hardpoints | `DeployHardpointsOnFire` | Settings-only |

#### Cooling

| In-Game Name | XML Element | Bindable |
|---|---|---|
| Silent Running | `ToggleButtonUpInput` | Yes |
| Deploy Heat Sink | `DeployHeatSink` | Yes |

#### Miscellaneous

| In-Game Name | XML Element | Bindable |
|---|---|---|
| Ship Lights | `ShipSpotLightToggle` | Yes |
| Sensor Zoom Axis | `RadarRangeAxis` | Yes |
| Decrease Sensor Zoom | `RadarDecreaseRange` | Yes |
| Increase Sensor Zoom | `RadarIncreaseRange` | Yes |
| Divert Power To Engines | `IncreaseEnginesPower` | Yes |
| Divert Power To Weapons | `IncreaseWeaponsPower` | Yes |
| Divert Power To Systems | `IncreaseSystemsPower` | Yes |
| Balance Power Distribution | `ResetPowerDistribution` | Yes |
| Reset HMD Orientation | `HMDReset` | Yes |
| Cargo Scoop | `ToggleCargoScoop` | Yes |
| Jettison All Cargo | `EjectAllCargo` | Yes |
| Landing Gear | `LandingGearToggle` | Yes |
| Microphone Mute | `MicrophoneMute` | Yes |
| Use Shield Cell | `UseShieldCell` | Yes |
| Use Chaff Launcher | `FireChaffLauncher` | Yes |
| Use Shutdown Field Neutraliser | `TriggerFieldNeutraliser` | Yes |
| Charge ECM | `ChargeECM` | Yes |
| Weapon Colour | `WeaponColourToggle` | Yes |
| Engine Colour | `EngineColourToggle` | Yes |
| Night Vision | `NightVisionToggle` | Yes |
| System Colonisation Suite | `TriggerColonisationModule` | Yes |

#### Mode Switches

| In-Game Name | XML Element | Bindable |
|---|---|---|
| UI Focus | `UIFocus` | Yes |
| External Panel | `FocusLeftPanel` | Yes |
| Comms Panel | `FocusCommsPanel` | Yes |
| Quick Comms | `QuickCommsPanel` | Yes |
| Role Panel | `FocusRadarPanel` | Yes |
| Internal Panel | `FocusRightPanel` | Yes |
| Open Galaxy Map | `GalaxyMapOpen` | Yes |
| Open System Map | `SystemMapOpen` | Yes |
| Show DSS Score Screen | `ShowPGScoreSummaryInput` | Yes |
| Headlook | `HeadLookToggle` | Yes |
| Game Menu | `Pause` | Yes |
| Friends Menu | `FriendsMenu` | Yes |
| Open Discovery | `OpenCodexGoToDiscovery` | Yes |
| Switch Cockpit Mode | `PlayerHUDModeToggle` | Yes |
| Enter FSS Mode *(corrected 2026-07-17 — see §3, item 4)* | `ExplorationFSSEnter` | Yes |

#### Headlook Mode

| In-Game Name | XML Element | Bindable |
|---|---|---|
| Reset Headlook | `HeadLookReset` | Yes |
| Look Up | `HeadLookPitchUp` | Yes |
| Look Down | `HeadLookPitchDown` | Yes |
| Look Up and Down Axis | `HeadLookPitchAxisRaw` | Yes |
| Look Left | `HeadLookYawLeft` | Yes |
| Look Right | `HeadLookYawRight` | Yes |
| Look Left and Right Axis | `HeadLookYawAxis` | Yes |

#### Multicrew

| In-Game Name | XML Element | Bindable |
|---|---|---|
| Mode Toggle | `MultiCrewToggleMode` | Yes |
| Primary Fire | `MultiCrewPrimaryFire` | Yes |
| Secondary Fire | `MultiCrewSecondaryFire` | Yes |
| Primary Utility Fire | `MultiCrewPrimaryUtilityFire` | Yes |
| Secondary Utility Fire | `MultiCrewSecondaryUtilityFire` | Yes |
| Mouse X-Axis | `MultiCrewThirdPersonMouseXMode` | Settings-only |
| Relative Mouse X-Axis | `MultiCrewThirdPersonMouseXDecay` | Settings-only |
| Mouse Y-Axis | `MultiCrewThirdPersonMouseYMode` | Settings-only |
| Relative Mouse Y-Axis | `MultiCrewThirdPersonMouseYDecay` | Settings-only |
| Third-Person Yaw Axis | `MultiCrewThirdPersonYawAxisRaw` | Yes |
| Third-Person Yaw Left | `MultiCrewThirdPersonYawLeftButton` | Yes |
| Third-Person Yaw Right | `MultiCrewThirdPersonYawRightButton` | Yes |
| Third-Person Pitch Axis | `MultiCrewThirdPersonPitchAxisRaw` | Yes |
| Third-Person Pitch Up | `MultiCrewThirdPersonPitchUpButton` | Yes |
| Third-Person Pitch Down | `MultiCrewThirdPersonPitchDownButton` | Yes |
| Multi-Crew Mouse Sensitivity | `MultiCrewThirdPersonMouseSensitivity` | Settings-only |
| Third-Person Field of View Axis | `MultiCrewThirdPersonFovAxisRaw` | Yes |
| Third-Person Field of View Out | `MultiCrewThirdPersonFovOutButton` | Yes |
| Third-Person Field of View In | `MultiCrewThirdPersonFovInButton` | Yes |
| Cycle Cockpit UI Forwards | `MultiCrewCockpitUICycleForward` | Yes |
| Cycle Cockpit UI Backwards | `MultiCrewCockpitUICycleBackward` | Yes |

#### Fighter Orders

| In-Game Name | XML Element | Bindable |
|---|---|---|
| Recall Fighter | `OrderRequestDock` | Yes |
| Defend | `OrderDefensiveBehaviour` | Yes |
| Engage at Will | `OrderAggressiveBehaviour` | Yes |
| Attack Target | `OrderFocusTarget` | Yes |
| Maintain Formation | `OrderHoldFire` | Yes |
| Hold Position | `OrderHoldPosition` | Yes |
| Follow Me | `OrderFollow` | Yes |
| Open Orders | `OpenOrders` | Yes |

#### Full Spectrum System Scanner

| In-Game Name | XML Element | Bindable |
|---|---|---|
| Camera Pitch | `ExplorationFSSCameraPitch` | Yes |
| Camera Pitch Increase | `ExplorationFSSCameraPitchIncreaseButton` | Yes |
| Camera Pitch Decrease | `ExplorationFSSCameraPitchDecreaseButton` | Yes |
| Camera Yaw | `ExplorationFSSCameraYaw` | Yes |
| Camera Yaw Increase | `ExplorationFSSCameraYawIncreaseButton` | Yes |
| Camera Yaw Decrease | `ExplorationFSSCameraYawDecreaseButton` | Yes |
| Zoom In Target | `ExplorationFSSZoomIn` | Yes |
| Zoom Out | `ExplorationFSSZoomOut` | Yes |
| Stepped Zoom In | `ExplorationFSSMiniZoomIn` | Yes |
| Stepped Zoom Out | `ExplorationFSSMiniZoomOut` | Yes |
| Tuning | `ExplorationFSSRadioTuningX_Raw` | Yes |
| Tuning Right | `ExplorationFSSRadioTuningX_Increase` | Yes |
| Tuning Left | `ExplorationFSSRadioTuningX_Decrease` | Yes |
| Absolute Tuning | `ExplorationFSSRadioTuningAbsoluteX` | Yes |
| FSS Tuning Sensitivity | `FSSTuningSensitivity` | Settings-only |
| Discovery Scan | `ExplorationFSSDiscoveryScan` | Yes |
| Leave FSS | `ExplorationFSSQuit` | Yes |
| Mouse X-Axis | `FSSMouseXMode` | Settings-only |
| Relative Mouse X-Axis | `FSSMouseXDecay` | Settings-only |
| Mouse Y-Axis | `FSSMouseYMode` | Settings-only |
| Relative Mouse Y-Axis | `FSSMouseYDecay` | Settings-only |
| Mouse Sensitivity | `FSSMouseSensitivity` | Settings-only |
| FSS Mouse Deadzone | `FSSMouseDeadzone` | Settings-only |
| FSS Mouse Power Curve | `FSSMouseLinearity` | Settings-only |
| Target Current Signal | `ExplorationFSSTarget` | Yes |
| Show Help | `ExplorationFSSShowHelp` | Yes |

#### Detailed Surface Scanner

| In-Game Name | XML Element | Bindable |
|---|---|---|
| Toggle Front/Back View | `ExplorationSAAChangeScannedAreaViewToggle` | Yes |
| Exit Mode | `ExplorationSAAExitThirdPerson` | Yes |
| Next Filter | `ExplorationSAANextGenus` | Yes |
| Previous Filter | `ExplorationSAAPreviousGenus` | Yes |
| Mouse X-Axis | `SAAThirdPersonMouseXMode` | Settings-only |
| Relative Mouse X-Axis | `SAAThirdPersonMouseXDecay` | Settings-only |
| Mouse Y-Axis | `SAAThirdPersonMouseYMode` | Settings-only |
| Relative Mouse Y-Axis | `SAAThirdPersonMouseYDecay` | Settings-only |
| DSS Mouse Sensitivity | `SAAThirdPersonMouseSensitivity` | Settings-only |
| Third-Person Yaw Axis | `SAAThirdPersonYawAxisRaw` | Yes |
| Third-Person Yaw Left | `SAAThirdPersonYawLeftButton` | Yes |
| Third-Person Yaw Right | `SAAThirdPersonYawRightButton` | Yes |
| Third-Person Pitch Axis | `SAAThirdPersonPitchAxisRaw` | Yes |
| Third-Person Pitch Up | `SAAThirdPersonPitchUpButton` | Yes |
| Third-Person Pitch Down | `SAAThirdPersonPitchDownButton` | Yes |
| Third-Person Field of View Axis | `SAAThirdPersonFovAxisRaw` | Yes |
| Third-Person Field of View Out | `SAAThirdPersonFovOutButton` | Yes |
| Third-Person Field of View In | `SAAThirdPersonFovInButton` | Yes |

---

### SRV Controls

#### Driving

| In-Game Name | XML Element | Bindable |
|---|---|---|
| Drive Assist | `ToggleDriveAssist` | Yes |
| Drive Assist Default | `DriveAssistDefault` | Settings-only |
| SRV Steering Mouse X-Axis | `MouseBuggySteeringXMode` | Settings-only |
| Relative Mouse X-Axis | `MouseBuggySteeringXDecay` | Settings-only |
| SRV Rolling Mouse X-Axis | `MouseBuggyRollingXMode` | Settings-only |
| Relative Mouse X-Roll | `MouseBuggyRollingXDecay` | Settings-only |
| SRV Pitch Mouse Y-Axis | `MouseBuggyYMode` | Settings-only |
| Relative Mouse Y-Pitch | `MouseBuggyYDecay` | Settings-only |
| Steering Axis | `SteeringAxis` | Yes |
| Steering Left Button | `SteerLeftButton` | Yes |
| Steering Right Button | `SteerRightButton` | Yes |
| Roll Axis | `BuggyRollAxisRaw` | Yes |
| Roll Left Button | `BuggyRollLeftButton` | Yes |
| Roll Right Button | `BuggyRollRightButton` | Yes |
| Pitch Axis | `BuggyPitchAxis` | Yes |
| Pitch Up Button | `BuggyPitchUpButton` | Yes |
| Pitch Down Button | `BuggyPitchDownButton` | Yes |
| Vertical Thrusters | `VerticalThrustersButton` | Yes |
| SRV Primary Fire | `BuggyPrimaryFireButton` | Yes |
| SRV Secondary Fire | `BuggySecondaryFireButton` | Yes |
| Handbrake | `AutoBreakBuggyButton` | Yes |
| Headlights | `HeadlightsBuggyButton` | Yes |
| Toggle SRV Turret | `ToggleBuggyTurretButton` | Yes |
| Cycle Next Fire Group | `BuggyCycleFireGroupNext` | Yes |
| Cycle Previous Fire Group | `BuggyCycleFireGroupPrevious` | Yes |

**Note:** `BuggyRollLeftButton`/`BuggyRollRightButton` live here, under **SRV Controls** — a genuinely different, mutually-exclusive zone from Ship Controls' `RollLeftButton`/`RollRightButton`. They may share the same physical chord in a given profile but can never co-fire, since a player cannot be in a ship and an SRV simultaneously.

#### Driving Targeting

| In-Game Name | XML Element | Bindable |
|---|---|---|
| Select Target Ahead | `SelectTarget_Buggy` | Yes |

#### Driving Turret Controls

| In-Game Name | XML Element | Bindable |
|---|---|---|
| Turret Mouse X-Axis | `MouseTurretXMode` | Settings-only |
| Turret Relative Mouse X-Axis | `MouseTurretXDecay` | Settings-only |
| Turret Mouse Y-Axis | `MouseTurretYMode` | Settings-only |
| Turret Relative Mouse Y-Axis | `MouseTurretYDecay` | Settings-only |
| SRV Turret Yaw Axis | `BuggyTurretYawAxisRaw` | Yes |
| SRV Turret Yaw Left | `BuggyTurretYawLeftButton` | Yes |
| SRV Turret Yaw Right | `BuggyTurretYawRightButton` | Yes |
| SRV Turret Pitch Axis | `BuggyTurretPitchAxisRaw` | Yes |
| SRV Turret Pitch Up | `BuggyTurretPitchUpButton` | Yes |
| SRV Turret Pitch Down | `BuggyTurretPitchDownButton` | Yes |
| SRV Turret Mouse Sensitivity | `BuggyTurretMouseSensitivity` | Settings-only |
| SRV Turret Mouse Deadzone | `BuggyTurretMouseDeadzone` | Settings-only |
| SRV Turret Mouse Power Curve | `BuggyTurretMouseLinearity` | Settings-only |

#### Drive Throttle

| In-Game Name | XML Element | Bindable |
|---|---|---|
| Drive Speed Axis | `DriveSpeedAxis` | Yes |
| Forward Only Throttle Reverse | `BuggyToggleReverseThrottleInput` | Yes |
| Accelerate Button | `IncreaseSpeedButtonMax` | Yes |
| Decelerate Button | `DecreaseSpeedButtonMax` | Yes |
| Accelerate Axis | `IncreaseSpeedButtonPartial` | Yes |
| Decelerate Axis | `DecreaseSpeedButtonPartial` | Yes |

Note: `IncreaseSpeedButtonPartial`/`DecreaseSpeedButtonPartial` are named `*ButtonPartial` but are structurally AXIS elements per `EliteDangerous-BindsFileFormat.md` §2 — a concrete real-world instance of the "element names don't indicate type" pitfall.

#### Driving Miscellaneous

| In-Game Name | XML Element | Bindable |
|---|---|---|
| Divert Power To Engines | `IncreaseEnginesPower_Buggy` | Yes |
| Divert Power To Weapons | `IncreaseWeaponsPower_Buggy` | Yes |
| Divert Power To Systems | `IncreaseSystemsPower_Buggy` | Yes |
| Balance Power Distribution | `ResetPowerDistribution_Buggy` | Yes |
| Cargo Scoop | `ToggleCargoScoop_Buggy` | Yes |
| Jettison All Cargo | `EjectAllCargo_Buggy` | Yes |
| Recall/Dismiss Ship | `RecallDismissShip` | Yes |
| Enable Context Menu in SRV | `EnableMenuGroupsSRV` | Settings-only |

#### Driving Mode Switches

| In-Game Name | XML Element | Bindable |
|---|---|---|
| UI Focus | `UIFocus_Buggy` | Yes |
| External Panel | `FocusLeftPanel_Buggy` | Yes |
| Comms Panel | `FocusCommsPanel_Buggy` | Yes |
| Quick Comms | `QuickCommsPanel_Buggy` | Yes |
| Role Panel | `FocusRadarPanel_Buggy` | Yes |
| Internal Panel | `FocusRightPanel_Buggy` | Yes |
| Open Galaxy Map | `GalaxyMapOpen_Buggy` | Yes |
| Open System Map | `SystemMapOpen_Buggy` | Yes |
| Open Discovery | `OpenCodexGoToDiscovery_Buggy` | Yes |
| Switch Cockpit Mode | `PlayerHUDModeToggle_Buggy` | Yes |
| Headlook | `HeadLookToggle_Buggy` | Yes |

---

### On Foot Controls

#### On Foot

| In-Game Name | XML Element | Bindable |
|---|---|---|
| Mouse X-Axis | `MouseHumanoidXMode` | Settings-only |
| Mouse Y-Axis | `MouseHumanoidYMode` | Settings-only |
| Mouse Sensitivity | `MouseHumanoidSensitivity` | Settings-only |
| Forward Axis | `HumanoidForwardAxis` | Yes |
| Move Forward | `HumanoidForwardButton` | Yes |
| Move Backward | `HumanoidBackwardButton` | Yes |
| Strafe Axis | `HumanoidStrafeAxis` | Yes |
| Strafe Left | `HumanoidStrafeLeftButton` | Yes |
| Strafe Right | `HumanoidStrafeRightButton` | Yes |
| Rotate Axis | `HumanoidRotateAxis` | Yes |
| Rotate Sensitivity | `HumanoidRotateSensitivity` | Settings-only |
| Turn Left | `HumanoidRotateLeftButton` | Yes |
| Turn Right | `HumanoidRotateRightButton` | Yes |
| Pitch Axis | `HumanoidPitchAxis` | Yes |
| Pitch Sensitivity | `HumanoidPitchSensitivity` | Settings-only |
| Look Up | `HumanoidPitchUpButton` | Yes |
| Look Down | `HumanoidPitchDownButton` | Yes |
| Sprint | `HumanoidSprintButton` | Yes |
| Walk | `HumanoidWalkButton` | Yes |
| Crouch | `HumanoidCrouchButton` | Yes |
| Jump | `HumanoidJumpButton` | Yes |
| Interact | `HumanoidPrimaryInteractButton` | Yes |
| Secondary Interact | `HumanoidSecondaryInteractButton` | Yes |
| Open Item Menu | `HumanoidItemWheelButton` | Yes |
| Open Emote Wheel | `HumanoidEmoteWheelButton` | Yes |
| Cycle Utility Wheel Mode | `HumanoidUtilityWheelCycleMode` | Yes |
| Wheel Horizontal | `HumanoidItemWheelButton_XAxis` | Yes |
| Wheel Left | `HumanoidItemWheelButton_XLeft` | Yes |
| Wheel Right | `HumanoidItemWheelButton_XRight` | Yes |
| Wheel Vertical | `HumanoidItemWheelButton_YAxis` | Yes |
| Wheel Up | `HumanoidItemWheelButton_YUp` | Yes |
| Wheel Down | `HumanoidItemWheelButton_YDown` | Yes |
| Wheel Accepts Mouse Input | `HumanoidItemWheel_AcceptMouseInput` | Settings-only |
| Fire Weapon/Use Tool | `HumanoidPrimaryFireButton` | Yes |
| Aim Down Sights | `HumanoidZoomButton` | Yes |
| Throw Grenade | `HumanoidThrowGrenadeButton` | Yes |
| Melee Attack | `HumanoidMeleeButton` | Yes |
| Reload | `HumanoidReloadButton` | Yes |
| Switch Weapon | `HumanoidSwitchWeapon` | Yes |
| Select Primary Weapon | `HumanoidSelectPrimaryWeaponButton` | Yes |
| Select Secondary Weapon | `HumanoidSelectSecondaryWeaponButton` | Yes |
| Select Tool | `HumanoidSelectUtilityWeaponButton` | Yes |
| Select Next Weapon | `HumanoidSelectNextWeaponButton` | Yes |
| Select Previous Weapon | `HumanoidSelectPreviousWeaponButton` | Yes |
| Holster Weapon | `HumanoidHideWeaponButton` | Yes |
| Select Next Grenade Type | `HumanoidSelectNextGrenadeTypeButton` | Yes |
| Select Previous Grenade Type | `HumanoidSelectPreviousGrenadeTypeButton` | Yes |
| Toggle Flashlight | `HumanoidToggleFlashlightButton` | Yes |
| Toggle Night Vision | `HumanoidToggleNightVisionButton` | Yes |
| Toggle Shields | `HumanoidToggleShieldsButton` | Yes |
| Clear Authority Level | `HumanoidClearAuthorityLevel` | Yes |
| Use Health Pack | `HumanoidHealthPack` | Yes |
| Use Energy Cell | `HumanoidBattery` | Yes |
| Select Frag Grenade | `HumanoidSelectFragGrenade` | Yes |
| Select EMP Grenade | `HumanoidSelectEMPGrenade` | Yes |
| Select Shield Grenade | `HumanoidSelectShieldGrenade` | Yes |
| Select Energylink | `HumanoidSwitchToRechargeTool` | Yes |
| Select Profile Analyser | `HumanoidSwitchToCompAnalyser` | Yes |
| Select Suit Specific Tool | `HumanoidSwitchToSuitTool` | Yes |
| Toggle Tool Mode | `HumanoidToggleToolModeButton` | Yes |
| Toggle Help | `HumanoidToggleMissionHelpPanelButton` | Yes |
| Ping / Call Out | `HumanoidPing` | Yes *(currently unbound in the reference profile, and completely absent from the in-game UI — see `EliteDangerous-BindsFileFormat.md` §8)* |

#### On Foot Mode Switches

| In-Game Name | XML Element | Bindable |
|---|---|---|
| Open Galaxy Map | `GalaxyMapOpen_Humanoid` | Yes |
| Open System Map | `SystemMapOpen_Humanoid` | Yes |
| Comms Panel | `FocusCommsPanel_Humanoid` | Yes |
| Quick Comms | `QuickCommsPanel_Humanoid` | Yes |
| Open Insight Hub | `HumanoidOpenAccessPanelButton` | Yes |
| Open Conflict Zone Battle Stats | `HumanoidConflictContextualUIButton` | Yes |

#### On Foot Miscellaneous

| In-Game Name | XML Element | Bindable |
|---|---|---|
| Enable Context Menu on Foot | `EnableMenuGroupsOnFoot` | Settings-only |
| Enable Aim Assist | `EnableAimAssistOnFoot` | Settings-only |

#### On Foot Emotes

| In-Game Name | XML Element | Bindable |
|---|---|---|
| Point | `HumanoidEmoteSlot1` | Yes |
| Wave | `HumanoidEmoteSlot2` | Yes |
| Agree | `HumanoidEmoteSlot3` | Yes |
| Disagree | `HumanoidEmoteSlot4` | Yes |
| Go | `HumanoidEmoteSlot5` | Yes |
| Stop | `HumanoidEmoteSlot6` | Yes |
| Applaud | `HumanoidEmoteSlot7` | Yes |
| Salute | `HumanoidEmoteSlot8` | Yes |

---

## 3. Needs-Review Items — Full Detail

Rows in §2 where the match required inference rather than a direct, confirmed transcription, or where a prior correction was applied:

1. **`UI_Select` → "UI Panel Select" — CONFIRMED**, resolved by direct screenshot. Earlier passes had no transcribed name for this row and used an inferred placeholder ("Select / Confirm," guessed from its function rather than transcribed). A real in-game screenshot of General Controls → Interface Mode confirms the actual label is **"UI Panel Select"** — Primary `[SPACE]`, Secondary a joystick button — matching `<UI_Select><Primary Device="Keyboard" Key="Key_Space" /><Secondary Device="..." Key="Joy_4" /></UI_Select>` in the `.binds` file. This also happens to match the name `ActionCatalog.json` (the separately-sourced third-party catalog) already used, independently confirming that source was right on this row.
2. **`MouseReset`/`BlockMouseDecay` marked `Bindable: Yes`.** The in-game UI describes these as having "no standard key binding slots," but the underlying XML confirms both have real `<Primary>`/`<Secondary>` child elements (currently unbound). Marked `Yes` per that correction — the in-game UI apparently hides the binding row and exposes only the toggle.
3. **"Select Next Target" is omitted entirely** from the Targeting subgroup. Confirmed to be a duplicate transcription of "Cycle Next Target" — no separate XML element exists for it.
4. **`ExplorationFSSEnter` → "Enter FSS Mode"** (Ship Controls → Mode Switches), corrected 2026-07-17. Previously recorded as "Enter Pilot Mode" and *not* flagged as needing review — the original pass considered it a direct, confirmed match. It wasn't: a fresh in-game screenshot (2026-07-17) showed the real row label is "Enter FSS Mode," directly above a dedicated "Leave FSS" row (`ExplorationFSSQuit`, unaffected by this correction). **This means "only one true exception" was never a safe assumption** — this error passed an earlier confirmation pass undetected, and was only caught because the row was later used for something real (an in-game conflict test). Treat every row in this catalog with the same suspicion until it's been independently exercised and held up.

Every row not called out above is presented as a direct, confirmed match — in-game name and XML element name both independently verified — but item 4 above is the reason "not flagged" should read as "probably fine," not "guaranteed correct."
