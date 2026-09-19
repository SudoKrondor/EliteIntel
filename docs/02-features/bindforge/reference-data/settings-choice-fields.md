# Settings Choice Fields — Observed Vocabularies

**Measured, and settled.** Every value below was read out of real files rather than inferred from an
element's name, and **four independent sources agree on it**:

| Source | What it contributed |
|---|---|
| **47,898 commander-shared `.binds` files** (after the [456 that were not binds files](../bind-editor.md#a-binds-name-does-not-make-it-a-binds-file--2026-09-17) were set aside) | every token in use, at a scale where a real option cannot hide and a hand edit stands out as one file in forty-eight thousand |
| **Frontier's own `ControlSchemes\Help.txt`**, shipped with the game | the documented vocabularies for yaw-into-roll, throttle range, UI focus, headlook and gunsights |
| **The 90 preset files Elite ships**, in all three installs | tokens no commander file happened to hold |
| **The game itself**, on 2026-09-17 | labels read off the options screen, and values watched being written while one field was changed and its siblings held as controls |

**Why that is enough to build against.** The corpus establishes what the game writes; Frontier's own
documentation covers what nobody happened to have set; and the in-game passes tie each token to the words a
commander actually sees. Where the three disagreed, the disagreement is recorded rather than smoothed over —
`Bindings_TrailingGunsights` is in Frontier's documentation and in **none** of the 47,898 files, which is
precisely why the cross-check exists and why file counts alone were never treated as the whole answer.

**This table is the record now.** The counts are a snapshot of that corpus on **2026-09-17**; the
vocabularies are the point, and they do not move unless Frontier changes a setting. If an update adds a
value, edit the row.

**What a *choice field* is:** a setting whose value comes from a fixed list rather than a number the
commander types or drags. BindForge gives these a dropdown, and **a dropdown may only offer values the
game has been seen to write** — see
[Bind Editor — settings entries](../bind-editor.md#settings-entries-are-rows-too--settled-2026-09-16).

| Group | Fields | Editor |
|---|---|---|
| Token choices | 34 | dropdown of named options |
| Numeric choices | 3 | dropdown wearing numbers |
| Booleans | 45 | checkbox |
| Free numbers | 21 | slider or typed value — **not** a choice |

`''` means the element is present with an empty value, which is **that field’s default choice**, not an
absent one. Which choice it stands for differs per field, and is only known where it has been read off the
options screen.

## Token choices

| Element | Observed values (files) | In-game labels, where captured | Hand edits seen |
|---|---|---|---|
| `BuggyThrottleRange` | `Bindings_BuggyThrottleForewardOnly` (31,854), `''` (15,290) | BuggyThrottleForewardOnly = FORWARD ONLY | `Bindings_ThrottleForewardOnly` (2) |
| `CommsPanelFocusOptions` | `''` (30,744), `FocusOption_Nothing` (11,721), `FocusOption_Show` (4,679) | Nothing = DOES NOTHING · Show = SHOWS THE PANEL | `1` (3), `Joy_POV1Up` (1) |
| `CqcMuteButtonMode` | `mute_pushToTalk` (45,392), `mute_toggle` (1,636), `mute_pushToMute` (122) | pushToTalk = PUSH TO TALK · toggle = TOGGLE · pushToMute = PUSH TO MUTE | — |
| `FSSMouseXMode` | `''` (28,517), `Bindings_MouseYaw` (16,908) | MouseYaw = YAW (ROTATE on foot) | — |
| `FSSMouseYMode` | `''` (28,428), `Bindings_MousePitchInverted` (15,738), `Bindings_MousePitch` (1,259) | MousePitchInverted = PITCH INVERTED · MousePitch = PITCH | — |
| `GunsightSystem` | `Bindings_TraditionalGunsights` (768) | TraditionalGunsights = TRADITIONAL | — |
| `HeadlookMode` | `Bindings_HeadlookModeAccumulate` (44,047), `Bindings_HeadlookModeDirect` (3,262) | HeadlookModeAccumulate = ACCUMULATE · HeadlookModeDirect = DIRECT | — |
| `LeftPanelFocusOptions` | `''` (33,827), `FocusOption_Nothing` (13,317) | Nothing = DOES NOTHING | `1` (3), `Joy_POV1Left` (1) |
| `MouseBuggyRollingXMode` | `''` (46,482), `Bindings_MouseRoll` (641) | MouseRoll = ROLL | `Bindings_MouseRole` (1) |
| `MouseBuggySteeringXMode` | `''` (46,052), `Bindings_MouseYaw` (1,072) | MouseYaw = YAW (ROTATE on foot) | — |
| `MouseBuggyYMode` | `''` (45,987), `Bindings_MousePitch` (955), `Bindings_MousePitchInverted` (182) | MousePitch = PITCH · MousePitchInverted = PITCH INVERTED | — |
| `MouseHumanoidXMode` | `Bindings_MouseYaw` (33,847), `''` (10,559) | MouseYaw = YAW (ROTATE on foot) | `Bindings_MouseHumanoidYawRotate` (6) |
| `MouseHumanoidYMode` | `Bindings_MousePitch` (33,067), `''` (10,600), `Bindings_MousePitchInverted` (745) | MousePitch = PITCH · MousePitchInverted = PITCH INVERTED | `Bindings_MouseHumanoidPitchRotate` (6) |
| `MouseTurretXMode` | `''` (25,133), `Bindings_MouseYaw` (22,008) | MouseYaw = YAW (ROTATE on foot) | — |
| `MouseTurretYMode` | `''` (24,981), `Bindings_MousePitch` (20,617), `Bindings_MousePitchInverted` (1,543) | MousePitch = PITCH · MousePitchInverted = PITCH INVERTED | — |
| `MouseXMode` | `''` (40,591), `Bindings_MouseRoll` (3,499), `Bindings_MouseYaw` (3,235) | MouseRoll = ROLL · MouseYaw = YAW (ROTATE on foot) | — |
| `MouseYMode` | `''` (40,567), `Bindings_MousePitch` (6,498), `Bindings_MousePitchInverted` (256) | MousePitch = PITCH · MousePitchInverted = PITCH INVERTED | — |
| `MultiCrewThirdPersonMouseXMode` | `''` (34,623), `Bindings_MouseYaw` (12,265) | MouseYaw = YAW (ROTATE on foot) | — |
| `MultiCrewThirdPersonMouseYMode` | `''` (34,625), `Bindings_MousePitch` (12,120), `Bindings_MousePitchInverted` (143) | MousePitch = PITCH · MousePitchInverted = PITCH INVERTED | — |
| `MuteButtonMode` | `mute_toggle` (44,381), `mute_pushToTalk` (2,601), `mute_pushToMute` (191) | toggle = TOGGLE · pushToTalk = PUSH TO TALK · pushToMute = PUSH TO MUTE | — |
| `PitchCameraMouse` | `''` (26,939), `Bindings_MousePitchInverted` (17,584), `Bindings_MousePitch` (1,156) | MousePitchInverted = PITCH INVERTED · MousePitch = PITCH | — |
| `RightPanelFocusOptions` | `''` (33,800), `FocusOption_Nothing` (13,344) | Nothing = DOES NOTHING | `1` (3), `Joy_POV1Right` (1) |
| `RolePanelFocusOptions` | `''` (33,696), `FocusOption_Nothing` (13,448) | Nothing = DOES NOTHING | `1` (3), `Joy_POV1Down` (1) |
| `SAAThirdPersonMouseXMode` | `''` (30,792), `Bindings_MouseYaw` (14,552) | MouseYaw = YAW (ROTATE on foot) | — |
| `SAAThirdPersonMouseYMode` | `''` (30,806), `Bindings_MousePitch` (14,064), `Bindings_MousePitchInverted` (474) | MousePitch = PITCH · MousePitchInverted = PITCH INVERTED | — |
| `ThrottleRange` | `''` (24,151), `Bindings_ThrottleForewardOnly` (23,168), `Bindings_ThrottleFullRange` (13) | ThrottleForewardOnly = FORWARD ONLY · ThrottleFullRange = FULL RANGE | `Forward` (1), `Bindings_ThrustForwardOnly` (1), `Bindings_ThrottleRange_ForwardOnly` (1), `Bindings_ThrottleRangeFullRange` (1) |
| `ThrottleRangeFreeCam` | `''` (43,195), `Bindings_ThrottleForewardOnlyFreeCam` (2,133), `Bindings_ThrottleForewardOnly` (29) | ThrottleForewardOnly = FORWARD ONLY | `Bindings_ThrottleFullRangeFreeCam` (6) |
| `UIFocusMode` | `Bindings_FocusModeHold` (45,448), `Bindings_FocusModeToggle` (1,862) | FocusModeHold = DIRECTION · FocusModeToggle = CYCLE | — |
| `YawCameraMouse` | `''` (26,939), `Bindings_MouseYaw` (18,422), `Bindings_MouseRoll` (311) | MouseYaw = YAW (ROTATE on foot) · MouseRoll = ROLL | — |
| `YawToRollMode` | `Bindings_YawIntoRollNone` (44,500), `Bindings_YawIntoRollTime` (2,540), `Bindings_YawIntoRollLowRoll` (271) | YawIntoRollNone = OFF | — |
| `YawToRollMode_FAOff` | `''` (42,644), `Bindings_YawIntoRollNone` (4,265), `Bindings_YawIntoRollTime` (186), `Bindings_YawIntoRollLowRoll` (56) | YawIntoRollNone = OFF | — |
| `YawToRollMode_Landing` | `''` (46,574), `Bindings_YawIntoRollNone` (696), `Bindings_YawIntoRollLowRoll` (30) | YawIntoRollNone = OFF | `Bindings_YawIntoRollTime` (8) |

## Numeric choices — a dropdown wearing numbers

The game stores these as floats, and the options screen offers a fixed list. **The stored number is the
fraction the label names** — `0.10000000` is 10%. A free numeric field here would let a commander
type a value the game never offers.

| Element | Values (files) | Labels |
|---|---|---|
| `ThrottleIncrement` | `0.00000000` (39,307), `0.10000000` (6,186), `0.25000000` (1,108), `0.12500000` (640), `0.16666667` (67) Written short in some files as `0.0000`. | 0.00000000 = CONTINUOUS · 0.10000000 = 10% · 0.25000000 = 25% (LARGE on headlook) · 0.12500000 = 12.5% · 0.16666667 = 16.7% |
| `BuggyThrottleIncrement` | `0.00000000` (43,789), `0.10000000` (2,773), `0.12500000` (284), `0.25000000` (225), `0.16666667` (62) Written short in some files as `0.10000`. | 0.00000000 = CONTINUOUS · 0.10000000 = 10% · 0.12500000 = 12.5% · 0.25000000 = 25% (LARGE on headlook) · 0.16666667 = 16.7% |
| `HeadlookIncrement` | `0.00000000` (45,749), `0.50000000` (869), `0.33333334` (365), `0.25000000` (177) Written short in some files as `0.00`, `0.0000`. | 0.00000000 = CONTINUOUS · 0.50000000 = LARGE · 0.33333334 = MEDIUM · 0.25000000 = 25% (LARGE on headlook) |

**Not choice fields, despite looking like one in a first pass:**
- `Deadzone` — not a top-level setting at all — it belongs inside an axis binding, and appears here only in files whose structure is wrong
- `HeadlookMotionSensitivity` — free number nobody changes; its second value is `1.0000`, a formatting variant
- `MultiCrewThirdPersonMouseSensitivity` — free number, 58 distinct values
- `PlacementCamMouseSensitivity` — free number, 27 distinct values

## Booleans — checkbox, no vocabulary to capture

Every one holds `0` or `1`. A checkbox writes exactly what the game writes, so none of these needs testing.

`DeployHardpointsOnFire`, `DriveAssistDefault`, `EnableAimAssistOnFoot`, `EnableCameraLockOn`, `EnableMenuGroups`, `EnableMenuGroupsOnFoot`, `EnableMenuGroupsSRV`, `EnableRumbleTrigger`, `FSSMouseXDecay`, `FSSMouseYDecay`, `FocusOnTextEntryField`, `FreeCamMouseXDecay`, `FreeCamMouseYDecay`, `HeadlookDefault`, `HeadlookResetOnToggle`, `HeadlookSmoothing`, `Hold`, `HumanoidItemWheel_AcceptMouseInput`, `Inverted`, `MotionHeadlook`, `MouseBuggyRollingXDecay`, `MouseBuggySteeringXDecay`, `MouseBuggyYDecay`, `MouseDecay`, `MouseGUI`, `MouseHeadingUpInsteadOfRoll`, `MouseHeadlook`, `MouseHeadlookInvert`, `MouseRelativeMode`, `MouseTurretXDecay`, `MouseTurretYDecay`, `MouseWheelThrottle`, `MouseWheelThrottleInverted`, `MouseXDecay`, `MouseXSign`, `MouseYDecay`, `MouseYSign`, `MultiCrewThirdPersonMouseXDecay`, `MultiCrewThirdPersonMouseYDecay`, `PlacementCamMouseXDecay`, `PlacementCamMouseYDecay`, `SAAThirdPersonMouseXDecay`, `SAAThirdPersonMouseYDecay`, `ToggleOn`, `yawRotateHeadlook`.

## Free numbers — slider or typed value

Sensitivities, deadzones and power curves, each with hundreds of distinct values across the corpus, which
is what tells them apart from a list: `BuggyTurretMouseDeadzone` (297 distinct), `BuggyTurretMouseLinearity` (182 distinct), `BuggyTurretMouseSensitivity` (488 distinct), `FSSMouseDeadzone` (174 distinct), `FSSMouseLinearity` (455 distinct), `FSSMouseSensitivity` (1240 distinct), `FSSTuningSensitivity` (1161 distinct), `FreeCamMouseSensitivity` (272 distinct), `HeadlookSensitivity` (557 distinct), `HumanoidPitchSensitivity` (217 distinct), `HumanoidRotateSensitivity` (324 distinct), `MouseDeadzone` (499 distinct), `MouseDecayRate` (472 distinct), `MouseHeadlookSensitivity` (366 distinct), `MouseHumanoidSensitivity` (657 distinct), `MouseLinearity` (186 distinct), `MouseSensitivity` (793 distinct), `SAAThirdPersonMouseSensitivity` (628 distinct), `YawToRollSensitivity` (512 distinct).

## What this corpus does **not** tell us

- **`Bindings_TrailingGunsights`** is listed in Frontier’s `Help.txt` and appears in **no file here** —
  `GunsightSystem` is always `Bindings_TraditionalGunsights`. Evidence from files is a floor, never a
  ceiling: the two sources cover each other’s gaps.
- **`''` is *FOCUSES THE PANEL*** on the panel-focus fields — **settled by experiment, 2026-09-17.**
  Alan set *Looking at Comms Panel* to that option in-game and left its three siblings alone; the game rewrote
  `CommsPanelFocusOptions` from `FocusOption_Nothing` to an **empty value**, and left the siblings untouched.
  Three options on screen, two tokens in the file, and the third is the blank — which is why 47,898 files
  never yielded a token for it. **Writing that choice means writing `Value=""`, not removing the element.**
- **Vocabularies differ between siblings.** `MouseXMode` holds both `Bindings_MouseRoll` and
  `Bindings_MouseYaw`, but `FSSMouseXMode` only ever holds `Bindings_MouseYaw`, and
  `ThrottleRangeFreeCam` has a token of its own, `Bindings_ThrottleForewardOnlyFreeCam`. **A field’s
  list may not be copied from its family.**
- **`Bindings_MouseHumanoidYawRotate` and `Bindings_MouseHumanoidPitchRotate`** appear in 6 files each, and
  are **not what the current build writes** — settled 2026-09-17. With On Foot *Mouse X-Axis* showing
  ROTATE, an in-game apply re-emitted `MouseHumanoidXMode` as `Bindings_MouseYaw`. They are a legacy or
  short-lived token: a dropdown offers the current one, and preserves either if it finds it in a file.

## Hand edits are separable at this scale

A value in one file out of 47,898 is a person editing XML, not an option: `Bindings_MouseRole` (a typo of
*Roll*), `Bindings_ThrottleRange_ForwardOnly`, plain `Forward`, and a joystick token pasted into a
panel-focus field. **BindForge still has to open those files, keep the value, and not “correct” it** — see
[what real files contain](../../../03-data-models/binding-schema.md#binding-element--three-distinct-kinds).

