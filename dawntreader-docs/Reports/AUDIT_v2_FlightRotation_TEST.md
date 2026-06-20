# AUDIT_v2 — §2.2 Flight Rotation (Yaw / Roll): Structural Extraction Test Pass

**Source:** `dawntreader-docs/Actual Game Files/PlayerBinds/DualVirpilDawnTreader.4.2.binds`
**Method:** Direct XML structural read — every child node and attribute extracted verbatim from the XML.
**Scope:** All Yaw- and Roll-related flight elements (main flight, alternate flight values, landing mode overrides).
**Cross-reference against BindForge_GameMode_SubGroups.md:** NOT performed in this pass — raw structural inventory only.

---

## Element Count

20 elements extracted across three groups:

| Group | Elements |
|-------|----------|
| Main flight (Yaw/Roll) | 10 |
| Alternate flight values (Yaw/Roll + toggle) | 3 |
| Landing mode overrides (Yaw/Roll) | 7 |

---

## Full Structural Table

Column key:
- **Binding Device / Key** — `<Binding>` child (axis elements only)
- **Primary Device / Key** — `<Primary>` child (button elements only)
- **Primary Modifier(s)** — `<Modifier>` children inside `<Primary>` (listed as `Device/Key`; multiple separated by ` + `)
- **Secondary Device / Key** — `<Secondary>` child (button elements only)
- **Secondary Modifier(s)** — `<Modifier>` children inside `<Secondary>`
- **Inverted** — `<Inverted Value="">` (axis elements only)
- **Deadzone** — `<Deadzone Value="">` (axis elements only)
- **ToggleOn** — `<ToggleOn Value="">` (some button elements)
- **Setting Value** — bare `Value=""` attribute on root element (standalone setting elements only)
- `{NoDevice}` — slot structurally present but unbound
- `(empty)` — Key attribute present but set to `""`
- `—` — property not applicable to this element type, or child node absent from XML

### Group 1 — Main Flight (Yaw / Roll)

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

### Group 2 — Alternate Flight Values (Yaw / Roll)

These elements are only active when `UseAlternateFlightValuesToggle` is engaged (ToggleOn: 1, currently unbound).

| XML Element | Type | Binding Device | Binding Key | Primary Device | Primary Key | Primary Modifier(s) | Secondary Device | Secondary Key | Secondary Modifier(s) | Inverted | Deadzone | ToggleOn | Setting Value |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `UseAlternateFlightValuesToggle` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | 1 | — |
| `YawAxisAlternate` | AXIS | {NoDevice} | (empty) | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `RollAxisAlternate` | AXIS | {NoDevice} | (empty) | — | — | — | — | — | — | 0 | 0.00000000 | — | — |

### Group 3 — Landing Mode Overrides (Yaw / Roll)

These elements override the main flight bindings when landing mode is active. All are currently unbound in this profile.

| XML Element | Type | Binding Device | Binding Key | Primary Device | Primary Key | Primary Modifier(s) | Secondary Device | Secondary Key | Secondary Modifier(s) | Inverted | Deadzone | ToggleOn | Setting Value |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `YawAxis_Landing` | AXIS | {NoDevice} | (empty) | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `YawLeftButton_Landing` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `YawRightButton_Landing` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `YawToRollMode_Landing` | STANDALONE SETTING | — | — | — | — | — | — | — | — | — | — | — | (empty) |
| `RollAxis_Landing` | AXIS | {NoDevice} | (empty) | — | — | — | — | — | — | 0 | 0.00000000 | — | — |
| `RollLeftButton_Landing` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |
| `RollRightButton_Landing` | BUTTON | — | — | {NoDevice} | (empty) | — | {NoDevice} | (empty) | — | — | — | — | — |

---

## Modifier Findings

**No `<Modifier>` children were found in any element in this section.** The Primary Modifier(s) and Secondary Modifier(s) columns are included per spec but are uniformly absent (`—`) across all 20 elements. This is expected: modifier combos in this profile appear on combat, camera, and fighter-order bindings, not flight axis controls.

---

## Structural Notes

- **`YawAxisRaw`** is the only yaw axis actively bound: RVWAP Joy_XAxis, Deadzone 0.05725. Non-zero deadzone is intentional (stick center deadband).
- **`RollAxisRaw`** is actively bound: RVWAP Joy_ZAxis, Deadzone 0.00000 (no deadband on roll).
- **`RollLeftButton` / `RollRightButton`** are the only button elements in this section that are actually bound (Keyboard Key_W and Key_R respectively). All other button slots are `{NoDevice}`.
- **`YawLeftButton` / `YawRightButton`** are both unbound — yaw is handled exclusively via `YawAxisRaw` axis.
- **`YawToRollMode`** is a dropdown-style setting (`Bindings_YawIntoRollNone`) — no key binding slot, no Inverted/Deadzone.
- **`YawToRollSensitivity`** is a slider setting (Value: 0.40000001) — no key binding slot.
- **`YawToRollMode_FAOff`** has an empty Value string — the FA-Off yaw-to-roll mode override is unset (uses the same mode as FA-On).
- **`YawToRollButton`** has a `<ToggleOn Value="0">` child — this is a BUTTON element (has Primary/Secondary slots) with an additional toggle state, currently with ToggleOn=0 (off). Both slots unbound.
- **`UseAlternateFlightValuesToggle`** has `ToggleOn Value="1"` but both slots are `{NoDevice}` — the toggle is configured as ON but has no key bound to flip it.
- **Alternate axes** (`YawAxisAlternate`, `RollAxisAlternate`) are both unbound — alternate flight value axis overrides are not used in this profile.
- **All 7 landing-mode override elements** are unbound or empty — the profile inherits main-flight bindings in landing mode rather than using dedicated landing overrides.
- **`DisableRotationCorrectToggle`** (XML lines 298–302, Primary: Keyboard/Key_Numpad_Multiply, Secondary: RVWAP/Joy_24, ToggleOn: 1) appears near this section in the file but is a flight-assist toggle, not a yaw/roll axis/button element. It is excluded from this table and should be assigned to its own sub-group (likely §2.6 Flight Assists or similar) during the full audit.

---

## XML Source Lines (for verification)

| XML Element | Source Lines |
|---|---|
| `YawAxisRaw` | 22–26 |
| `YawLeftButton` | 27–30 |
| `YawRightButton` | 31–34 |
| `YawToRollMode` | 35 |
| `YawToRollSensitivity` | 36 |
| `YawToRollMode_FAOff` | 37 |
| `YawToRollButton` | 38–42 |
| `RollAxisRaw` | 43–47 |
| `RollLeftButton` | 48–51 |
| `RollRightButton` | 52–55 |
| `UseAlternateFlightValuesToggle` | 108–112 |
| `YawAxisAlternate` | 113–117 |
| `RollAxisAlternate` | 118–122 |
| `YawAxis_Landing` | 198–202 |
| `YawLeftButton_Landing` | 203–206 |
| `YawRightButton_Landing` | 207–210 |
| `YawToRollMode_Landing` | 211 |
| `RollAxis_Landing` | 225–229 |
| `RollLeftButton_Landing` | 230–233 |
| `RollRightButton_Landing` | 234–237 |
