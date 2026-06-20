# BindForge — Binds File Audit

**Project:** Elite Intel
**Feature:** BindForge
**Version:** 2.0 Draft — incorporates original BindForge file format documentation
**Status:** Partially complete — Section 6 contains known format documentation.
Section 5 audit work (game section sub-group mapping) still required.
**Parent document:** `BindForge_Spec.md` Sections 6.3, 12.7, 13.1, 13.2, 13.3

---

## 1. Purpose

This document is the working record of the Elite Dangerous `.binds` file audit. Its goal
is to produce a complete, verified inventory of:

- Every XML element type that appears in a `.binds` file
- Every attribute and child element each type can have
- Which elements are binding elements and which are configuration elements
- The exact sub-group structure within each of the four game sections that matches the
  in-game controls UI
- The full mapping from in-game sub-group label to XML element names
- Every edge case and structural variation that BindForge's parser must handle correctly

This audit is a prerequisite for:
- Implementing Game Mode sub-groups correctly
- Implementing controller and axis binding editing
- Implementing the Binding Editor panel for all binding types
- Defining the default Purpose Mode group definitions

---

## 2. Reference Files

### 2.1 Primary binds reference
```
DualVirpilDawnTreader.4.2.binds
```
DawnTreader's personal `.binds` file representing a complex dual HOTAS configuration
with Virpil controls, pedals, and keyboard assignments. The most comprehensive test case
available.

### 2.2 Additional reference
The in-game controls configuration UI, accessed by launching Elite Dangerous and
navigating to Options → Controls.

---

## 3. File Root Element

A `.binds` file is an XML document beginning with:

```xml
<Root PresetName="DualVirpilDawnTreader" MajorVersion="4" MinorVersion="2">
```

### Root attributes
- **PresetName** — Human-readable profile name shown in the game's controls UI
- **MajorVersion / MinorVersion** — Bind format version (e.g. 4.2)

BindForge must:
- Preserve these attributes exactly
- Preserve any unknown attributes
- Preserve the Root element name

---

## 4. Element Type Classification

The `.binds` file contains two fundamentally different types of XML elements mixed
together. The parser must distinguish them by structure, not by name.

### 4.1 Binding Elements

A binding element represents a game action mapped to one or two physical inputs.

**Classification rule:** An element is a binding element if it contains one or more of:
- A `<Primary>` child element
- A `<Secondary>` child element
- A `<Binding>` child element (axis bindings use this instead of Primary/Secondary)

There are three structural sub-patterns within binding elements:

#### 4.1.1 Button Binding Pattern

```xml
<RollLeftButton>
    <Primary Device="Keyboard" Key="Key_W" />
    <Secondary Device="{NoDevice}" Key="" />
</RollLeftButton>
```

- `<Primary>` and `<Secondary>` define up to two independent input assignments
- Either may be unbound via `Device="{NoDevice}" Key=""`
- Optional `<ToggleOn Value="0|1" />` child element

#### 4.1.2 Button Binding With Modifier Pattern

```xml
<ForwardKey>
    <Primary Device="Keyboard" Key="Key_E">
        <Modifier Device="Keyboard" Key="Key_LeftShift" />
    </Primary>
    <Secondary Device="{NoDevice}" Key="" />
</ForwardKey>
```

- `<Modifier>` is nested inside `<Primary>` or `<Secondary>` — not at the action level
- Multiple `<Modifier>` elements are allowed
- Modifiers may be keyboard keys or joystick buttons
- Modifier order must be preserved

#### 4.1.3 Axis Binding Pattern

```xml
<YawAxisRaw>
    <Binding Device="334443F4" Key="Joy_XAxis" />
    <Inverted Value="0" />
    <Deadzone Value="0.05725000" />
</YawAxisRaw>
```

- Uses `<Binding>` instead of `<Primary>`/`<Secondary>`
- `<Inverted Value="0|1" />` — boolean, axis direction
- `<Deadzone Value="float" />` — analog dead zone, 0.0 to 1.0
- Note: axis bindings do not have a Secondary

### 4.2 Configuration Elements

A configuration element is a standalone value attribute. It is NOT a binding.

**Classification rule:** An element is a configuration element if it has a `Value="..."`
attribute and NO `<Primary>`, `<Secondary>`, or `<Binding>` children.

```xml
<LeftPanelFocusOptions Value="FocusOption_Nothing" />
<CommsPanelFocusOptions Value="FocusOption_Nothing" />
<RolePanelFocusOptions Value="FocusOption_Nothing" />
<RightPanelFocusOptions Value="FocusOption_Nothing" />
<EnableCameraLockOn Value="1" />
<MuteButtonMode Value="mute_pushToTalk" />
<CqcMuteButtonMode Value="mute_pushToTalk" />
<MouseXMode Value="Bindings_MouseYaw" />
<MouseHeadlookSensitivity Value="0.10001005" />
```

Configuration elements appear mixed in with binding elements throughout the file.
They represent game settings stored in the `.binds` file for convenience.

### 4.3 Global Settings Elements

Some elements at the root level are neither bindings nor configuration values — they are
global file metadata:

```xml
<KeyboardLayout>en-US</KeyboardLayout>
```

These use a `<Name>value</Name>` format rather than a `Value="..."` attribute.
BindForge must preserve these elements unchanged.

### 4.4 Self-Closing Unbound Elements

```xml
<YawLeft />
```

Structurally valid but represents a completely unbound action. Parsed as a binding
element with no inputs assigned. Not commonly seen in practice but must be handled.

---

## 5. Attribute Reference

### 5.1 Binding element attributes

| Attribute | Context | Values | Notes |
|---|---|---|---|
| `Device` | `<Primary>`, `<Secondary>`, `<Binding>`, `<Modifier>` | See Section 6 | The device identifier |
| `Key` | `<Primary>`, `<Secondary>`, `<Binding>`, `<Modifier>` | See Section 7 | The input code |
| `Value` | `<Inverted>` | `0`, `1` | Boolean, axis direction |
| `Value` | `<Deadzone>` | Float e.g. `0.05725000` | Analog dead zone 0.0–1.0 |
| `Value` | `<ToggleOn>` | `0`, `1` | `1` = toggle behavior, `0` = hold/momentary |

### 5.2 Configuration element attributes

| Attribute | Context | Values | Notes |
|---|---|---|---|
| `Value` | Configuration elements | String enum, integer `0`/`1`, float | Varies by element |

---

## 6. Device Identifier Format

This is one of the most important and potentially confusing aspects of the `.binds` file.

### 6.1 Named devices

Some devices use a plain name string:

| Value | Meaning |
|---|---|
| `Keyboard` | Standard keyboard |
| `Mouse` | Mouse |
| `{NoDevice}` | Unbound / no device assigned |
| Named strings | e.g. `T-Rudder`, `vJoy` — devices with fixed names |

### 6.2 VID/PID-derived device identifiers

HOTAS, joystick, and gamepad devices that are configured through DeviceMappings.xml use
a derived hex identifier:

```xml
<Binding Device="334443F4" Key="Joy_XAxis" />
<Primary Device="334443F4" Key="Joy_8" />
```

**Critical:** These identifiers are NOT Frontier device GUIDs. They are derived from the
device's VID and PID as configured in DeviceMappings.xml. The exact derivation formula
is:

```
DeviceIdentifier = VID + PID (8 hex characters, concatenated)
```

Example from DeviceMappings.xml:
```xml
<VPCPanel>
    <PID>0259</PID>
    <VID>3344</VID>
</VPCPanel>
```

Results in bind file device ID: `33440259`

**Important implications:**
- The same physical device can have a different device ID if the user changes its VID/PID
  via vendor software (e.g. Virpil VPC Config)
- BindForge must maintain the mapping: `SDL device → VID/PID → DeviceMappings.xml →
  DeviceName → BindFileDeviceID`
- If a user changes their device VID/PID, their existing binds file will have stale
  device IDs that no longer match — BindForge should detect and offer to repair this

### 6.3 Device identifier lookup chain

```
SDL_GetJoystickGUIDForID()
        ↓
Extract VID + PID from GUID
        ↓
Concatenate as 8-char hex string
        ↓
Match against Device= attributes in .binds file
        ↓
Look up human name via DeviceMappings.xml for display
```

---

## 7. Input Key / Code Reference

### 7.1 Axis codes

Full axis assignments use these codes in the `Key` attribute:

| Code | Axis |
|---|---|
| `Joy_XAxis` | X axis (typically roll or yaw) |
| `Joy_YAxis` | Y axis (typically pitch) |
| `Joy_ZAxis` | Z axis (typically throttle or twist) |
| `Joy_RXAxis` | Rotation X axis |
| `Joy_RYAxis` | Rotation Y axis |
| `Joy_RZAxis` | Rotation Z axis |
| `Joy_UAxis` | U slider axis |
| `Joy_VAxis` | V slider axis |

### 7.2 Directional pseudo-axis codes

Some bindings use only half of an axis — positive or negative direction:

| Code | Meaning |
|---|---|
| `Pos_Joy_XAxis` | Positive direction of X axis |
| `Neg_Joy_XAxis` | Negative direction of X axis |
| `Pos_Joy_UAxis` | Positive direction of U axis |
| `Neg_Joy_YAxis` | Negative direction of Y axis |

These appear in button binding elements (`<Primary>` / `<Secondary>`) not in axis binding
elements. They allow an axis to trigger a discrete action when pushed in one direction.

### 7.3 Button codes

Joystick buttons use a 1-based index:

| Code | SDL3 equivalent |
|---|---|
| `Joy_1` | SDL button index 0 |
| `Joy_2` | SDL button index 1 |
| `Joy_N` | SDL button index N-1 |

**Critical off-by-one:** SDL3 uses 0-based button indices. Elite Dangerous uses 1-based
`Joy_N` tokens. `ButtonInputMapper` in `elite.intel.devices.model` must handle this
conversion.

### 7.4 POV / Hat codes

Hat switch directions use this format:

| Code | Direction |
|---|---|
| `Joy_POV1Up` | POV 1 up |
| `Joy_POV1Down` | POV 1 down |
| `Joy_POV1Left` | POV 1 left |
| `Joy_POV1Right` | POV 1 right |
| `Joy_POV2Up` | POV 2 up |

### 7.5 Keyboard codes

Keyboard keys use an `Key_` prefix:

```
Key_W, Key_E, Key_Numpad_5, Key_LeftShift, Key_RightShift,
Key_LeftControl, Key_RightControl, Key_LeftAlt, Key_Home,
Key_Numpad_Multiply, etc.
```

---

## 8. Java Data Models

The following Java records and classes are recommended for
`elite.intel.bindforge.bindseditor.model`. These are translated from the original
BindForge C# data model, adapted for Java and Elite Intel conventions.

### 8.1 BindingEntry

Represents a single game action with its input assignments.

```java
public record BindingEntry(
    String name,                    // XML element name e.g. "YawAxisRaw", "PrimaryFire"
    String category,                // Game section e.g. "Ship Controls"
    String subcategory,             // Sub-group e.g. "Flight Rotation"
    BindingType type,               // AXIS, BUTTON, TOGGLE
    InputSlot primary,              // Primary input assignment
    InputSlot secondary,            // Secondary input assignment (null for axis bindings)
    AxisProperties axisProperties,  // Inverted + Deadzone (null for button bindings)
    boolean toggleOn                // ToggleOn value, false if not present
) {
    public boolean isAxis() {
        return axisProperties != null;
    }

    public boolean hasModifier() {
        return (primary != null && !primary.modifiers().isEmpty()) ||
               (secondary != null && !secondary.modifiers().isEmpty());
    }
}
```

### 8.2 InputSlot

Represents one input assignment (Primary or Secondary).

```java
public record InputSlot(
    String device,              // Raw Device= attribute value
    String key,                 // Raw Key= attribute value
    List<Modifier> modifiers,   // Nested <Modifier> elements
    boolean isEmpty             // true if Device="{NoDevice}" Key=""
) {}
```

### 8.3 Modifier

Represents a modifier key that must be held simultaneously.

```java
public record Modifier(
    String device,  // Modifier Device= attribute
    String key      // Modifier Key= attribute
) {}
```

### 8.4 AxisProperties

Additional properties for axis binding elements.

```java
public record AxisProperties(
    boolean inverted,   // <Inverted Value="1" />
    float deadzone      // <Deadzone Value="0.05725000" />
) {}
```

### 8.5 ConfigParameter

Represents a configuration element — a non-binding value in the `.binds` file.

```java
public record ConfigParameter(
    String name,        // XML element name
    String category,    // Assigned via lookup table
    String subcategory, // Assigned via lookup table
    String value        // Raw Value= attribute string
) {
    public boolean isBoolean() {
        return "0".equals(value) || "1".equals(value);
    }

    public boolean isFloat() {
        try { Float.parseFloat(value); return true; }
        catch (NumberFormatException e) { return false; }
    }
}
```

### 8.6 BindingType enum

```java
public enum BindingType {
    AXIS,    // Has <Binding> child with axis key, plus Inverted/Deadzone
    BUTTON,  // Has <Primary>/<Secondary> with discrete button/key
    TOGGLE   // Has <Primary>/<Secondary> plus <ToggleOn Value="1"/>
}
```

### 8.7 ConfigParameterType enum (optional inference)

Used when categorizing configuration elements without explicit category lookup:

```java
public enum ConfigParameterType {
    TUNING,        // Name ends with Sensitivity, Decay, Linearity
    FLAG,          // Name starts with Enable, Use, Allow
    MODE_SELECTOR, // Name ends with Mode, Range, Option
    ANALOG_CONTROL,// Name ends with Deadzone, Increment
    UNKNOWN
}
```

---

## 9. ButtonMap File Format

ButtonMap files (`*.buttonMap`) define human-readable labels for raw input identifiers.
They are device-specific, optional, and purely cosmetic — they do not affect gameplay.

### 9.1 File structure

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<Root>
    <Joy_1>B1</Joy_1>
    <Joy_2>B2</Joy_2>
    <Joy_13>T1 - Down</Joy_13>
    <Joy_14>T1 - Up</Joy_14>
    <Joy_34>E1 - Counter Clockwise</Joy_34>
    <Joy_35>E1 - Clockwise</Joy_35>
    <Joy_UAxis>A1</Joy_UAxis>
    <Joy_VAxis>A2</Joy_VAxis>
</Root>
```

### 9.2 Filename convention

```
<DeviceName>.buttonMap
```

Where `<DeviceName>` matches the element tag name in `DeviceMappings.xml`. Examples:
- `VPCPanel.buttonMap` → corresponds to `<VPCPanel>` in DeviceMappings.xml
- `T-Rudder.buttonMap` → corresponds to `<T-Rudder>`

### 9.3 Input codes supported

ButtonMap files may contain labels for:
- Button codes: `Joy_1`, `Joy_2`, ... `Joy_N`
- Full axis codes: `Joy_XAxis`, `Joy_YAxis`, `Joy_ZAxis`, `Joy_RXAxis`, `Joy_RYAxis`,
  `Joy_RZAxis`, `Joy_UAxis`, `Joy_VAxis`
- Directional pseudo-axes: `Pos_Joy_UAxis`, `Neg_Joy_YAxis` etc.
- POV/Hat: `Joy_POV1Up`, `Joy_POV1Down` etc.

### 9.4 Label values

Label text may include:
- Short codes: `B1`, `E1`
- Descriptive names: `T1 - Down`
- Directional indicators: `Clockwise`, `Up`, `Left`
- Axis names: `A1`, `A2`

### 9.5 Fallback behavior

If a device has no buttonMap file, or a specific input code has no label entry:
- BindForge generates generic labels: `Button 1`, `Button 2`, `Axis X`, `Axis Y`
- Raw identifier is shown as fallback: `Joy_4`

### 9.6 Relationship chain

```
DeviceMappings.xml (DeviceName → VID/PID)
        ↓
DeviceName → ButtonMap filename
        ↓
ButtonMap file (Joy_N → "Human Label")
        ↓
Displayed in BindForge UI instead of raw Joy_N
```

### 9.7 SDL3 to label mapping chain

```
SDL button index 12
        ↓
ButtonInputMapper.toBindsToken(12) → "Joy_13"
        ↓
Look up "Joy_13" in VPCPanel.buttonMap
        ↓
Display "T1 - Down" in UI
```

---

## 10. DeviceMappings.xml File Format

### 10.1 File structure

```xml
<DeviceMappings>
    <VPCPanel>
        <PID>0259</PID>
        <VID>3344</VID>
    </VPCPanel>
    <T-Rudder>
        <PID>0BF3</PID>
        <VID>044F</VID>
    </T-Rudder>
</DeviceMappings>
```

### 10.2 Element structure

- Root element: `<DeviceMappings>`
- One child element per device, named with the device's logical name
- `<PID>` — Product ID, 4 hex characters
- `<VID>` — Vendor ID, 4 hex characters
- Some devices may have alternative VID/PID pairs (additional child elements)
- Some devices may have an `SupportsIcons` or similar metadata element

### 10.3 Relationship to bind files

The device identifier in a `.binds` file is derived from VID and PID:
```
BindFileDeviceID = VID + PID (concatenated, 8 hex chars)
```

Example: VID=3344, PID=0259 → bind file Device="33440259"

BindForge must maintain this mapping for device identity correlation.

---

## 11. Parsing Safety Rules

These rules are non-negotiable and must be enforced by the BindForge parser and writer:

1. **Preserve unknown elements** — If an XML element is not recognized, preserve it
   exactly as-is. Never drop unknown elements.
2. **Preserve unknown attributes** — If an attribute is not recognized, preserve it.
3. **Preserve element ordering** — The game reads elements in order. Reordering elements
   is not safe.
4. **Preserve formatting where possible** — Avoid unnecessary whitespace changes.
5. **Never drop bindings silently** — If a binding cannot be parsed, log the error and
   include the raw element in the model as an opaque preserved element.
6. **Always create backups before writing** — No exceptions. See `BindForge_Spec.md`
   Section 3.
7. **Validate XML before writing** — The writer must validate that the output is
   well-formed XML before any file is touched.
8. **Atomic writes only** — Write to temp file, validate, then rename over target.
9. **Preserve BOM if present** — Some `.binds` files have a UTF-8 BOM. Preserve it.

---

## 12. Known Edge Cases

These structural variations have been identified and the parser must handle all of them:

### 12.1 Axis bindings use `<Binding>` not `<Primary>`

```xml
<PitchAxisRaw>
    <Binding Device="RVWAP" Key="Joy_YAxis" />
    <Inverted Value="1" />
    <Deadzone Value="0.05725000" />
</PitchAxisRaw>
```

### 12.2 Unbound slots use `{NoDevice}`

```xml
<MicrophoneMute>
    <Primary Device="{NoDevice}" Key="" />
    <Secondary Device="{NoDevice}" Key="" />
    <ToggleOn Value="0" />
</MicrophoneMute>
```

### 12.3 Modifier nested inside Primary or Secondary

```xml
<PrimaryFire>
    <Primary Device="Keyboard" Key="Key_Minus">
        <Modifier Device="Keyboard" Key="Key_RightShift" />
    </Primary>
    <Secondary Device="334443F4" Key="Joy_4" />
</PrimaryFire>
```

### 12.4 Toggle behavior flag

```xml
<ToggleCargoScoop>
    <Primary Device="Keyboard" Key="Key_Home" />
    <Secondary Device="vJoy" Key="Joy_11" />
    <ToggleOn Value="1" />
</ToggleCargoScoop>
```

### 12.5 Mixed keyboard and HOTAS in same binding

```xml
<DeployHardpointToggle>
    <Primary Device="Keyboard" Key="Key_U" />
    <Secondary Device="334443F4" Key="Joy_8" />
</DeployHardpointToggle>
```

### 12.6 Configuration elements adjacent to binding elements

```xml
<FocusRightPanel>
    <Primary Device="Keyboard" Key="Key_4" />
    <Secondary Device="{NoDevice}" Key="" />
</FocusRightPanel>
<LeftPanelFocusOptions Value="FocusOption_Nothing" />
<CommsPanelFocusOptions Value="FocusOption_Nothing" />
<RolePanelFocusOptions Value="FocusOption_Nothing" />
<RightPanelFocusOptions Value="FocusOption_Nothing" />
```

Configuration elements appear immediately after related binding elements. The parser
must not confuse them with binding elements.

### 12.7 Multi-context variants of the same action

```xml
<YawAxisRaw> ... </YawAxisRaw>
<YawAxisAlternate> ... </YawAxisAlternate>
<YawAxis_Landing> ... </YawAxis_Landing>
```

Each context is an independent binding element. They are related semantically but
completely independent structurally.

### 12.8 Named device in axis binding

Some axis bindings reference a named device rather than a VID/PID hex ID:

```xml
<PitchAxisRaw>
    <Binding Device="RVWAP" Key="Joy_YAxis" />
    ...
</PitchAxisRaw>
```

Here `RVWAP` is a logical device name from DeviceMappings.xml, not a VID/PID hex string.
The parser must handle both formats in the Device attribute.

---

## 13. Audit Work Still Required

The following work cannot be completed from documentation alone and requires hands-on
testing with the game:

### 13.1 Game section sub-group mapping

Map every binding element name to its in-game sub-group label within each of the four
sections (General Controls, Ship Controls, SRV Controls, On Foot Controls).

**Method:**
1. Open the in-game controls UI
2. Record every sub-group name within each section
3. Make a test binding change for a representative action in each sub-group
4. Have CCTIJ diff the `.binds` file before and after
5. Record the mapping from in-game label to XML element name

This mapping drives Game Mode's sub-group display in BindForge.

### 13.2 Complete element inventory

Have CCTIJ read the full `DualVirpilDawnTreader.4.2.binds` file and produce:
- A complete list of every unique XML element name
- Classification of each as binding element or configuration element
- Every attribute variation found

### 13.3 Configuration element inventory

For each configuration element found:
- What values it accepts
- What in-game setting it corresponds to
- What edit control is appropriate in BindForge UI

### 13.4 VIDPID format verification

Verify exactly how Elite Dangerous formats the VID/PID device identifier:
- Byte order (VID first or PID first)
- Case (upper or lower hex)
- Zero padding
- Whether interface or collection info is ever appended

This must match exactly for BindForge to correctly correlate SDL3 devices to `.binds`
file device references.

---

## 14. How to Complete the Audit

Recommended CCTIJ-assisted approach:

1. Place `DualVirpilDawnTreader.4.2.binds` in `dawntreader-docs/game-files/`
2. Give CCTIJ a prompt to read the file and produce the complete element inventory
3. Manually work through the in-game controls UI section by section, recording sub-group
   names
4. Make test binding changes in-game and have CCTIJ diff the file before and after
5. Record all findings under Section 15 Audit Results below

---

## 15. Audit Results

*This section is empty. It will be populated as audit work is completed.*
