# BindForge — Binds File Audit

**Project:** Elite Intel
**Feature:** BindForge
**Version:** Draft
**Status:** Not started — required before controller binding editing and Game Mode sub-groups can be implemented.
**Parent document:** `BindForge_Spec.md` Sections 6.3, 12.7, 13.1, 13.2, 13.3

---

## 1. Purpose

This document is the working record of the Elite Dangerous `.binds` file audit. Its goal is to produce a complete, verified inventory of:

- Every XML element type that appears in a `.binds` file
- Every attribute and child element each type can have
- Which elements are binding elements and which are configuration elements
- The exact sub-group structure within each of the four game sections that matches the in-game controls UI
- The full mapping from in-game sub-group label to XML element names
- Every edge case and structural variation that BindForge's parser must handle correctly

This audit is a prerequisite for:
- Implementing Game Mode sub-groups correctly
- Implementing controller and axis binding editing
- Implementing the Binding Editor panel for all binding types
- Defining the default Purpose Mode group definitions

---

## 2. Reference File

The primary reference file for this audit is:

```
DualVirpilDawnTreader.4.2.binds
```

This is DawnTreader's personal `.binds` file representing a complex dual HOTAS configuration with Virpil controls, pedals, and keyboard assignments. It is the most comprehensive test case available and covers a wide range of binding types.

Additional reference: the in-game controls configuration UI, accessed by launching Elite Dangerous and navigating to Options → Controls.

---

## 3. Known Element Types

The following element types have been identified so far. This list is incomplete and will be expanded during the audit.

### 3.1 Binding Elements

Binding elements have Primary and Secondary child elements with Device and Key attributes. They represent a game action mapped to one or two inputs.

**Simple keyboard binding:**
```xml
<FocusRightPanel>
    <Primary Device="Keyboard" Key="Key_4" />
    <Secondary Device="{NoDevice}" Key="" />
</FocusRightPanel>
```

**Keyboard binding with modifier:**
```xml
<ToggleFreeCamera>
    <Primary Device="Keyboard" Key="Key_C" />
    <Secondary Device="RVWAP" Key="Joy_7" />
</ToggleFreeCamera>
```

**Axis binding with Inverted and Deadzone:**
```xml
<PitchAxisRaw>
    <Binding Device="RVWAP" Key="Joy_YAxis" />
    <Inverted Value="1" />
    <Deadzone Value="0.05725000" />
</PitchAxisRaw>
```

**Binding with ToggleOn:**
```xml
<ToggleCargoScoop>
    <Primary Device="Keyboard" Key="Key_Home" />
    <Secondary Device="vJoy" Key="Joy_11" />
    <ToggleOn Value="1" />
</ToggleCargoScoop>
```

**Unbound slot:**
```xml
<MicrophoneMute>
    <Primary Device="{NoDevice}" Key="" />
    <Secondary Device="{NoDevice}" Key="" />
    <ToggleOn Value="0" />
</MicrophoneMute>
```

### 3.2 Configuration Elements

Configuration elements are standalone value attributes that live in the `.binds` file alongside binding elements but are not bindings themselves. They represent settings that the game stores in the `.binds` file for convenience.

**Known examples:**
```xml
<LeftPanelFocusOptions Value="FocusOption_Nothing" />
<CommsPanelFocusOptions Value="FocusOption_Nothing" />
<RolePanelFocusOptions Value="FocusOption_Nothing" />
<RightPanelFocusOptions Value="FocusOption_Nothing" />
<EnableCameraLockOn Value="1" />
<MuteButtonMode Value="mute_pushToTalk" />
<CqcMuteButtonMode Value="mute_pushToTalk" />
```

---

## 4. Known Attribute Types

| Attribute | Values seen | Notes |
|---|---|---|
| `Device` | `Keyboard`, `{NoDevice}`, device element name (e.g. `RVWAP`, `vJoy`) | Raw VIDPID hex string for axis bindings |
| `Key` | Key name (e.g. `Key_4`, `Joy_7`, `Joy_YAxis`) | Empty string when `{NoDevice}` |
| `Inverted` | `0`, `1` | Only on axis bindings |
| `Deadzone` | Float value e.g. `0.05725000` | Only on axis bindings |
| `ToggleOn` | `0`, `1` | `1` = toggle, `0` = hold |
| `Value` | Various — string enum, integer, float | Configuration elements only |

---

## 5. Audit Work To Be Done

The following work is required to complete this document:

### 5.1 Complete Element Inventory

Read the full `DualVirpilDawnTreader.4.2.binds` file and produce a complete list of every unique XML element name, categorized as binding element or configuration element. Note every attribute and child element variation found.

### 5.2 Game Section Mapping

For each of the four game sections — General Controls, Ship Controls, SRV Controls, On Foot Controls — map every binding element to its in-game sub-group label. This requires:

1. Opening the in-game controls UI
2. Recording every sub-group name within each section
3. Making a test binding change for representative actions in each sub-group
4. Observing which XML element changed in the `.binds` file
5. Recording the mapping from in-game label to XML element name

### 5.3 Configuration Element Inventory

Identify every configuration element in the file and determine:
- What values it accepts
- What in-game setting it corresponds to
- What edit control is appropriate in the BindForge UI — dropdown, checkbox, numeric input, text field

### 5.4 Edge Case Identification

Identify any structurally unusual XML that BindForge's parser must handle or gracefully reject:
- Actions with unexpected attributes
- Multiple Modifier children
- Non-standard child elements
- Duplicate action tags
- Actions that appear in unexpected sections

### 5.5 Axis Token Mapping

Produce a complete mapping from SDL3 axis index to Elite Dangerous axis token:

| SDL3 | Elite Token | Notes |
|---|---|---|
| Axis 0 | `Joy_XAxis` | To be verified |
| Axis 1 | `Joy_YAxis` | To be verified |
| Axis 2 | `Joy_ZAxis` | To be verified |
| Axis 3 | `Joy_RXAxis` | To be verified |
| Axis 4 | `Joy_RYAxis` | To be verified |
| Axis 5 | `Joy_RZAxis` | To be verified |
| Slider 0 | `Joy_UAxis` | To be verified |
| Slider 1 | `Joy_VAxis` | To be verified |

Verify off-by-one between SDL3 button indices (0-based) and Elite's `Joy_N` tokens (1-based).

### 5.6 VIDPID Format

Document exactly how Elite Dangerous formats VIDPID strings in axis binding Device attributes — byte order, case, zero padding — so BindForge can match SDL3 hardware identity to `.binds` file device references correctly.

---

## 6. Audit Results

*This section is empty. It will be populated as audit work is completed.*

---

## 7. How to Conduct the Audit

The recommended approach is a CCTIJ-assisted session:

1. Have CCTIJ read the full `DualVirpilDawnTreader.4.2.binds` file
2. Ask CCTIJ to produce a complete element inventory categorized by type
3. Manually work through the in-game controls UI section by section, recording sub-group names
4. Make test binding changes in-game and have CCTIJ diff the `.binds` file before and after to identify which element changed
5. Record all findings in this document under Section 6

This work is best done in a dedicated session separate from implementation work.
