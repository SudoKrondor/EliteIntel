# Elite Dangerous — `.binds` File Format Reference

**Scope:** Engine-agnostic reference for Frontier's `.binds` key-binding file format and the game's own binding behavior — structure, vocabulary, and confirmed quirks/pitfalls. This document describes the game and the file itself, independent of any particular parser implementation.

**Confidence:** Structural facts (element shapes, attributes, device ID derivation) come from a full structural extraction of a real, complex dual-HOTAS `.binds` file (457 top-level elements). Behavioral facts (the modifier press-order mechanism, exact-chord matching, file re-read timing) come from direct in-game testing conducted 2026-06-23 through 2026-06-24, including one theory (subset-suppression) that was proposed, tested further, and explicitly retracted — see §9. Treat anything not marked "confirmed by live testing" as a structural observation only, not a behavior-verified fact.

---

## 1. Root Element

A `.binds` file is an XML document whose root looks like:

```xml
<Root PresetName="DualVirpilDawnTreader" MajorVersion="4" MinorVersion="2">
  <!-- every binding element, elided -->
</Root>
```

- **PresetName** — human-readable profile name, shown in the game's own Controls UI.
- **MajorVersion / MinorVersion** — bind format version (e.g. `4.2`). The `.binds` filename itself typically embeds this same version (e.g. `DualVirpilDawnTreader.4.2.binds`).
- A parser/writer should preserve these attributes, any unrecognized attributes, and the root element name exactly — see §10.

---

## 2. The Three Element Shapes

Every top-level child of the root is one of three structurally distinct shapes. **The element's tag name never reliably indicates its type** — some elements literally named `*ButtonPartial` (e.g. `IncreaseSpeedButtonPartial`) are structurally AXIS elements, not BUTTON elements. A parser must classify by which child elements/attributes are actually present, never by the tag name.

**Element inventory, refreshed 2026-09-08 against two independent current files.** The 457-element
figure below is an older capture and is superseded for the current format:

| Version | Elements | BUTTON | AXIS | STANDALONE | `KeyboardLayout` | Addressable slots |
|---|---|---|---|---|---|---|
| `3.0` | 401 | 264 | 56 | 80 | 1 | 584 |
| `4.1` | 486 | — | — | — | 1 | — |
| **`4.2`** | **515** | **352** | **70** | **92** | 1 | **774** |

**The element inventory is fixed by format version.** A stock `Custom.4.2.binds` and a heavily edited
personal `4.2` file have identical shape counts — 352/70/92 — differing only in what is assigned. So a
file can be validated against its declared version.

**The version is in the file, not only the filename:**

```xml
<Root PresetName="Custom" MajorVersion="4" MinorVersion="2">
  <!-- every binding element, elided -->
</Root>
```

**And the format is strictly additive.** 3.0's elements are a subset of 4.1's, and 4.1's of 4.2's, with
**zero removed at either step** — 85 added in 4.1, 29 in 4.2 (Odyssey construction-camera controls such as
`MovePlacementCam*`). 4.2's additions over 3.0 are 77 `Humanoid*` On Foot controls among 114. **A parser
written for the newest version reads every older one**; it simply finds fewer elements.

*Superseded, retained for provenance —* proportions observed in a reference 457-element specimen file: 268 BUTTON, 72 AXIS, 117 STANDALONE SETTING.

### 2.1 BUTTON

```xml
<YawLeftButton>
    <Primary Device="Keyboard" Key="Key_A" />
    <Secondary Device="{NoDevice}" Key="" />
</YawLeftButton>
```

- Has a `<Primary>` and/or `<Secondary>` child, each carrying `Device`/`Key` attributes.
- Either slot may be unbound: `Device="{NoDevice}" Key=""`.
- Optional element-wide `<ToggleOn Value="0|1" />` — see §3.3.
- A **self-closing, fully unbound** BUTTON element is structurally valid too: `<YawLeft />`. Rare in practice but must be handled as a binding element with no inputs assigned.

### 2.2 AXIS

```xml
<YawAxisRaw>
    <Binding Device="SaitekX52" Key="Joy_RZAxis" />
    <Inverted Value="0" />
    <Deadzone Value="0.00000000" />
</YawAxisRaw>
```

- Has exactly one `<Binding>` child (not `<Primary>`/`<Secondary>`) — this is the single reliable structural signal for AXIS.
- Optional `<Inverted Value="0|1" />` and `<Deadzone Value="0.0–1.0" />`.
- AXIS elements never have a Secondary slot.

Named devices can appear here too, not just VID/PID hex IDs:

```xml
<PitchAxisRaw>
    <Binding Device="RVWAP" Key="Joy_YAxis" />
    <Inverted Value="1" />
    <Deadzone Value="0.05725000" />
</PitchAxisRaw>
```

`RVWAP` is a logical device name from `DeviceMappings.xml` (see the companion `EliteDangerous-DeviceMappings-ButtonMap.md` doc), not a VID/PID hex string — a `Device=` attribute must be treated as "either a name or an 8-hex-char ID," never assumed to be one or the other by element type or position.

### 2.3 STANDALONE SETTING

```xml
<MouseSensitivity Value="1.00000000" />
<LeftPanelFocusOptions Value="FocusOption_Nothing" />
<EnableCameraLockOn Value="1" />
<MuteButtonMode Value="mute_pushToTalk" />
```

- A bare leaf element: just a `Value="..."` attribute on the element itself, no children at all.
- Not a binding — cannot hold a key, cannot conflict with anything.
- Covers sliders, dropdowns, and toggles with no binding slot: mouse sensitivity, deadzone curves, menu-group toggles, focus-panel options, etc.
- Value types vary by element — boolean-ish (`0`/`1`), float, or string enum — inferred from content, not declared by any schema.
- These appear interleaved with binding elements throughout the file. Example — a binding element immediately followed by unrelated-looking config elements that are actually related to it:

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

Never assume configuration elements are grouped separately from binding elements, or that adjacency implies a formal relationship encoded anywhere in the schema.

### 2.4 One non-conforming element: `KeyboardLayout`

```xml
<KeyboardLayout>en-US</KeyboardLayout>
```

This is global file metadata, not a binding or a `Value=` setting — the value is **text content**, not an attribute. It's the one element in the whole file that doesn't fit any of the three shapes above and needs its own handling path.

---

## 3. Slot-Level Properties

These attach to `<Primary>`, `<Secondary>`, or `<Binding>` — the "slot" elements.

### 3.1 `Modifier`

```xml
<Primary Device="Keyboard" Key="Key_I">
    <Modifier Device="Keyboard" Key="Key_LeftControl"/>
    <Modifier Device="Keyboard" Key="Key_LeftAlt"/>
    <Modifier Device="Keyboard" Key="Key_LeftShift"/>
</Primary>
```

- Nested inside `<Primary>` or `<Secondary>` (or `<Binding>`) — never at the action-element level.
- **Hard cap of 3 modifiers per slot, confirmed by live testing.** Holding 4 inputs at once (e.g. `LeftCtrl+LeftAlt+LeftShift+Y`) does not produce a 4-modifier bind — the game always caps at 3.
- A modifier can be any device type, not just keyboard — confirmed via a joystick button used as a modifier, and via a HOTAS slider's end-of-travel behavior acting as a modifier.
- Left/Right variants are tracked as distinct values and are freely mixable on the same slot (`LeftShift` + `RightShift` together on one slot is valid).
- The same key cannot be both the slot's base `Key=` and one of its own `<Modifier>` entries.
- Multiple `<Modifier>` elements are allowed per slot; their XML order should be preserved by any tool that edits the file, although (see below) order does not affect the game's own conflict matching.

#### The actual mechanism behind the cap — confirmed by live testing, 2026-06-23

This is the single most important pitfall in the whole format. **The `<Primary>` vs. `<Modifier>` XML labels are not semantically validated by the game at all.** Elite does not know or care which physical keys are "really" modifiers (Ctrl/Alt/Shift) vs. base keys. Any key can syntactically end up inside a `<Modifier>` element, and a genuine modifier key can end up as the slot's `Key=` attribute.

Reproduced directly: holding `LeftCtrl + LeftShift + LeftAlt + Y` (4 inputs) during capture produced:

```xml
<YawLeftButton_Landing>
    <Primary Device="Keyboard" Key="Key_LeftControl">
        <Modifier Device="Keyboard" Key="Key_LeftShift" />
        <Modifier Device="Keyboard" Key="Key_LeftAlt" />
        <Modifier Device="Keyboard" Key="Key_Y" />
    </Primary>
    <Secondary Device="Keyboard" Key="Key_Tab">
        <Modifier Device="Keyboard" Key="Key_LeftShift" />
    </Secondary>
</YawLeftButton_Landing>
```

`Key_Y` — a genuine action key — was written as a `<Modifier>`. `Key_LeftControl` — a genuine modifier key — was written as the Primary `Key=`.

**The actual rule: whatever is held down when the *last* key is pressed becomes the `<Modifier>` set (in whatever order); the last-pressed key becomes the slot's `Key=` attribute.** It's purely a function of press order, not key identity. Practical consequence for any capture UI: accumulate held keys in press order and finalize the binding on the first non-modifier key pressed — that reproduces the game's own capture behavior.

**Modifier order in the XML does not affect the game's own conflict recognition** — confirmed via the in-game "Are you sure?" rebind-conflict dialog: two captures of the same key set, captured in different orderings, are correctly recognized by the game as the same combo regardless of order.

**Execution consequence:** any external tool that takes the `<Primary>`/`<Modifier>` XML labels at face value for *execution* (tapping whatever's in Primary, holding whatever's in Modifier) will get hold/tap roles backwards whenever Frontier's own capture happened to mislabel a real action key into a `<Modifier>` slot, as shown above. Two concrete failure modes result:

- A key that's really the intended tap-key, but is sitting in a `<Modifier>` slot, gets held for the full chord duration — long enough to also trigger that key's own separate binding elsewhere, if it has one.
- While the (mislabeled) modifiers are held but the (mislabeled) Primary hasn't fired yet, the currently-held key subset can be an exact match for some *other* binding's full combo, spuriously firing that one too.

**The fix is not "the primary can never be a modifier" — it's "never trust the slot labels for execution."** Any code that needs to actually press keys to fire a binding must classify each key by its own identity, independent of which XML element it's nested in: Ctrl/Alt/Shift (any left/right variant) are always held; anything else is always tapped, regardless of XML position. Two edge cases are worth handling explicitly rather than silently mis-executing: every key in a chord turning out to be a modifier (nothing left to tap), and two or more non-modifier keys in one chord (ambiguous — preferring whichever one the slot originally labeled Primary is a reasonable tie-break).

### 3.2 `Hold`

```xml
<Primary Device="Keyboard" Key="Key_Y">
    <Hold Value="1" />
</Primary>
```

- Boolean-ish (`Value="1"` observed). Per-slot, not element-wide.
- Origin is *how the key was captured* — held roughly 1+ second during detection vs. tapped — not a user-facing setting exposed anywhere obvious in the UI.
- Never appears on AXIS.
- Independent of, and can coexist with, `ToggleOn` on the same element (confirmed via a cargo-scoop toggle binding that carries both).
- **UI display inconsistency, confirmed 2026-06-23:** `Hold` does not appear in the game's right-hand "About this setting" explanation panel, but it does show directly in the bind slot's own display. The two UI surfaces can show inconsistent information for the same binding.

### 3.3 `ToggleOn`

- Element-level, covering both Primary and Secondary as **one shared behavior** — unlike `Modifier`/`Hold`, it is not a per-slot property.

```xml
<ToggleCargoScoop>
    <Primary Device="Keyboard" Key="Key_Home" />
    <Secondary Device="vJoy" Key="Joy_11" />
    <ToggleOn Value="1" />
</ToggleCargoScoop>
```

- Only two observed values (`0`/`1`): `1` = toggle behavior, `0` = hold/momentary.
- Never appears on AXIS — BUTTON-only.

### 3.4 `Deadzone` / `Inverted`

- AXIS-only; never appear on BUTTON.
- `Deadzone`: float, range 0.0–1.0.
- `Inverted`: boolean-ish (`0`/`1`).

### 3.5 Unbound slots

```xml
<MicrophoneMute>
    <Primary Device="{NoDevice}" Key="" />
    <Secondary Device="{NoDevice}" Key="" />
    <ToggleOn Value="0" />
</MicrophoneMute>
```

`Device="{NoDevice}" Key=""` marks an unbound slot. An element can be partially bound (one slot set, one `{NoDevice}`) or fully unbound (both slots `{NoDevice}`, or the self-closing form from §2.1).

### 3.6 Mixed device types on one element

Nothing prevents mixing keyboard and HOTAS assignments across a single element's two slots:

```xml
<DeployHardpointToggle>
    <Primary Device="Keyboard" Key="Key_U" />
    <Secondary Device="334443F4" Key="Joy_8" />
</DeployHardpointToggle>
```

---

## 4. Device Identifier Format

One of the most confusing parts of the format.

### 4.0 The rule, confirmed 2026-09-08

> **`.binds` names a device by its `DeviceMappings.xml` element name, matched on VID/PID. With no
> matching entry it falls back to the VID+PID hex form.**

Sections 4.1 and 4.2 below describe the two halves of that one rule. An earlier reading treated them as
competing possibilities — [testing item 9](../../../00-overview/testing-required.md) — and §4.2's
original wording had it backwards, saying devices *configured through* `DeviceMappings.xml` use hex. The
opposite is true: an entry is what **replaces** the hex with a name.

**Confirmed against five specimens spanning 2024-08 to 2026-09**, including the same commander's file
before and after adding entries for the same two devices.

**`.binds` cannot tell who authored an entry.** `SaitekX56Joystick` is a Frontier-shipped element and
`RVWAP` is a hand-added one; both produce an identical `Device="..."`. That is precisely why the
[shipped stock reference](../reference-data/FrontierStock-README.md) is load-bearing rather than a
convenience.

`Keyboard` and `Mouse` are neither — they appear in no `DeviceMappings.xml` and are reserved literals.

**Consequence: renaming a device is a multi-file operation.** The element name in `DeviceMappings.xml`,
every `Device="..."` in `.binds` that uses it, and the `.buttonMap` filename all have to move together.
One real file carries 99 bindings on a single alias.

### 4.1 Named devices

| Value | Meaning |
|---|---|
| `Keyboard` | Standard keyboard |
| `Mouse` | Mouse |
| `{NoDevice}` | Unbound / no device assigned |
| Other plain strings (`T-Rudder`, `vJoy`, `RVWAP`, `LVWAP`, ...) | Devices with a fixed logical name defined in `DeviceMappings.xml` — there is no fixed enum; any name can appear |

### 4.2 VID/PID-derived hex identifiers

**Corrected 2026-09-08.** This section previously read *"devices configured through `DeviceMappings.xml`
use a derived 8-hex-character identifier instead of a name"*, which is backwards. **The hex form is what
a device gets when no `DeviceMappings.xml` entry matches it** — adding an entry is what replaces hex with
a name. See [§4.0](#40-the-rule-confirmed-2026-09-08).

**The VID+PID order is confirmed, not asserted (2026-09-08).** A 2025-05 specimen carries
`Device="334483F3"` and `Device="334443F4"` for two VIRPIL devices, and `3344` is VIRPIL's vendor ID —
so the vendor half leads. A Razer device in the same file reads `15320244`, VID `1532` first. Every
unnamed device in every specimen follows it.

**Read defensively anyway.** Both halves are fixed-width, so accepting either order costs one extra
comparison and covers a future version or platform that differs. A false match would need a second device
whose VID and PID are the exact reverse pair. **BindForge never writes hex**, so this is a read concern
only.

A device with no matching `DeviceMappings.xml` entry uses a derived 8-hex-character identifier instead of a name:

```xml
<Binding Device="334443F4" Key="Joy_XAxis" />
<Primary Device="334443F4" Key="Joy_8" />
```

**These are not Frontier device GUIDs.** They're derived from the device's VID and PID as configured in `DeviceMappings.xml`:

```
DeviceIdentifier = VID + PID   (8 hex characters, concatenated)
```

Example — given this `DeviceMappings.xml` entry:

```xml
<VPCPanel>
    <PID>0259</PID>
    <VID>3344</VID>
</VPCPanel>
```

the resulting `.binds` device ID is `33440259`.

**Implications:**

- The same physical device can produce a *different* device ID if the user changes its VID/PID through vendor configuration software (this is a real, observed scenario with Virpil hardware, for example) — a `.binds` file's device references can go stale relative to a device's current VID/PID without any other change to the file.
- **Controller/joystick device identity is opaque within the `.binds` file alone.** The 8-hex-character string does not decode to reveal VID vs. PID individually, or device type (joystick vs. gamepad vs. wheel) — that distinction simply is not carried by the format. Recovering a human-readable name requires cross-referencing `DeviceMappings.xml` — see `EliteDangerous-DeviceMappings-ButtonMap.md`.
- A conceptual lookup chain from a live device to a `.binds` reference: enumerate the device's hardware GUID → extract VID+PID → concatenate as an 8-char hex string → match against `Device=` attributes in the `.binds` file → optionally look up a human name via `DeviceMappings.xml` for display.
- **Exact formatting details are not fully verified from documentation alone:** byte order (VID-first vs. PID-first), hex case (upper vs. lower), zero-padding behavior, and whether interface/collection info is ever appended are not independently confirmed — the derivation formula above is drawn from a single matching example (VID `3344` + PID `0259` → `33440259`) rather than an exhaustive verification pass. Treat this as reliable for the common case, but re-verify byte order against a live device pairing before depending on it for device-identity correlation.

---

## 5. Input Key / Token Vocabulary

All of the following are unstructured strings from the game's point of view — the format has no parsed `{kind: BUTTON|AXIS|POV, index, direction}` representation. Everything is a token to be pattern-matched by convention, not validated by any schema.

### 5.1 Full axis codes

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

### 5.2 Directional pseudo-axis codes

Used when only half of an axis is bound — a discrete action fired by pushing an axis in one direction. These appear on **BUTTON** slots (`<Primary>`/`<Secondary>`), not on an AXIS element's `<Binding>`.

| Code | Meaning |
|---|---|
| `Pos_Joy_XAxis` | Positive direction of X axis |
| `Neg_Joy_XAxis` | Negative direction of X axis |
| `Pos_Joy_UAxis` | Positive direction of U axis |
| `Neg_Joy_YAxis` | Negative direction of Y axis |

Half-axis and mouse-wheel key strings are not exclusive to any one element shape — e.g. `Neg_Joy_YAxis` or `Pos_Mouse_ZAxis` can appear as a plain BUTTON slot's `Key=` value, not only inside AXIS elements.

### 5.3 Button codes

Joystick buttons use a **1-based** index:

| Code | Meaning |
|---|---|
| `Joy_1` | Button index 1 |
| `Joy_2` | Button index 2 |
| `Joy_N` | Button index N |

**Off-by-one warning:** most native input APIs (e.g. SDL) report 0-based button indices. Converting between a 0-based device index and the game's 1-based `Joy_N` token requires an explicit `+1`/`-1` conversion at the boundary — do not assume they line up directly.

### 5.4 POV / hat codes

| Code | Meaning |
|---|---|
| `Joy_POV1Up` | POV switch 1, up |
| `Joy_POV1Down` | POV switch 1, down |
| `Joy_POV1Left` | POV switch 1, left |
| `Joy_POV1Right` | POV switch 1, right |
| `Joy_POV2Up` | POV switch 2, up (additional POV switches follow the same numbering pattern) |

**Four tokens per hat, and no diagonals — but the hardware reports eight positions.** Confirmed 2026-09-06
against the HID report descriptors of two DualShock 4 controllers: a hat is a single 4-bit field, logical
`0–7` mapped to 0°–315° (`Physical Maximum (315)`), carrying a null state for centred.

So the two models do not line up one-to-one, and the gap is where the bugs live:

| Hat value | Direction | `.binds` tokens active |
|---|---|---|
| 0 | up | `Joy_POV1Up` |
| 1 | up-right | `Joy_POV1Up` **and** `Joy_POV1Right` |
| 2 | right | `Joy_POV1Right` |
| null | centred | none |

**A diagonal is one hardware value but two bound tokens.** Anything reading a hat has to expand the value
into up to two tokens, and anything displaying one has to accept that two tokens can be lit by a single
physical position. Centred is a distinct null rather than the absence of a value, though it collapses to
"no tokens active" without loss.

### 5.5 Keyboard codes

`Key_`-prefixed tokens, e.g.:

```
Key_W, Key_E, Key_Numpad_5, Key_LeftShift, Key_RightShift,
Key_LeftControl, Key_RightControl, Key_LeftAlt, Key_Home,
Key_Numpad_Multiply
```

Left/Right variants of modifier keys are distinct tokens (`Key_LeftShift` ≠ `Key_RightShift`).

**Fuller token list, recovered from `elite.intel.ai.hands.UiNavigationTextTrap` (2026-09-02).** These
spellings were not in the original source material and are worth recording, because a parser that does not
recognise a token cannot classify the binding that uses it:

| Group | Tokens |
|---|---|
| Letters / digits | `Key_A` … `Key_Z`, `Key_0` … `Key_9` |
| Numpad | `Key_Numpad_0` … `Key_Numpad_9`, `Key_Numpad_Decimal`, `Key_Numpad_Add`, `Key_Numpad_Subtract`, `Key_Numpad_Divide`, `Key_Numpad_Multiply`, `Key_Numpad_Enter` |
| Punctuation | `Key_Minus`, `Key_Equals`, `Key_SemiColon`, `Key_Apostrophe`, `Key_Comma`, `Key_Period`, `Key_Slash`, `Key_BackSlash`, `Key_LeftBracket`, `Key_RightBracket`, `Key_Grave`, `Key_GraveAccent`, `Key_Tilde`, `Key_Hash`, `Key_LessThan` |
| Modifiers | `Key_LeftShift`, `Key_RightShift`, `Key_LeftControl`, `Key_RightControl`, `Key_LeftAlt`, `Key_RightAlt`, `Key_LeftSuper`, `Key_RightSuper`, `Key_Apps`, `Key_Menu` |

### Non-ASCII keyboard layouts — the character is the token

German, French AZERTY and Spanish layouts put letters on their own keys, and **Elite serialises those as the
character itself** rather than as a positional name: `Key_é`, `Key_ä`, `Key_ö`, `Key_ü`, `Key_ß`, `Key_è`, `Key_à`,
`Key_ù`, `Key_ç`, `Key_ñ`, plus `Key_ss` and `Key_Acute`.

Two consequences for BindForge:

- **A `.binds` file is not guaranteed to be ASCII.** Any parser, comparator or filename derived from a key
  token has to be Unicode-safe, and case folding has to be locale-aware (`ß` / `ẞ` are the obvious trap).
- **The same physical key produces a different token on a different layout**, which is what
  [`KeyboardLayout`](#24-one-non-conforming-element-keyboardlayout) is recording. A `.binds` moved between
  machines with different layouts is not portable at the token level.

This list is **not** exhaustive — it is what one consumer needed. Treat an unrecognised `Key_` token as valid
and unclassified rather than invalid.

---

## 6. File-Level Structural Quirks

- **Duplicate element names are possible** — e.g. an element name like `MouseGUI` can legitimately appear twice in the same file. Nothing in the format prevents this; a parser must not assume element names are unique within the document.
- **Multi-context variants of the same logical action are independent elements**, not variations of one element — e.g. `YawAxisRaw`, `YawAxisAlternate`, and `YawAxis_Landing` are three completely separate, independently-bindable elements that happen to be semantically related (base / alternate-controls / landing-override variants of "yaw axis"). Each must be parsed and stored independently.
- Configuration (STANDALONE SETTING) elements routinely appear immediately adjacent to the binding element they logically relate to. Proximity in the file is incidental ordering, not a structural signal of any kind.

---

## 7. File Location & Lifecycle

- **`.binds` and `StartPreset.*.start` files live in exactly one canonical, storefront-agnostic location** on Windows: `%LOCALAPPDATA%\Frontier Developments\Elite Dangerous\Options\Bindings\` (note the space in "Frontier Developments") — shared across every storefront install (Steam, Epic, Frontier Direct, Oculus) under the same Windows user account. There is no per-storefront duplication of these two file types. See `EliteDangerous-InstallPaths.md` for the full cross-platform path picture, including Linux/Proton and how this contrasts with `DeviceMappings.xml`/`.buttonMap` multiplicity.
- **The `.#.0`-style version suffix on a `.binds`/`StartPreset.#.start` filename is assigned by the game itself**, tied to the game's own version (e.g. an Odyssey on-foot-component version bump produced a `.4.0` suffix). No external tool mints or should mint this number — a tool only ever reacts to whatever suffix the game already wrote.
- **A game update can overwrite or erase `.binds`, `StartPreset.*.start`, `DeviceMappings.xml`, and `.buttonMap` files — but this is not universal or guaranteed.** It doesn't happen on every update, or for every user, and no reliable trigger pattern is confirmed. Any tool managing these files should always validate presence/correctness after a game update rather than assuming either outcome.
- **Elite only re-reads `.binds` when its own in-game Controls screen is opened.** Editing the file externally — by hand or by any tool — has no effect on a running game session until that screen is opened. This is the single most important lifecycle gotcha for any external editor: a "no-op"-looking edit may just be a stale read, and conversely a stale read can be misdiagnosed as a real binding-logic bug (see §9.2 for a documented case of exactly this happening).
- **`StartPreset.#.start` (the "Active Preset file") records, independently per top-level section (General/Ship/SRV/On Foot), which preset the game currently loads for that section — always, whether that preset is a factory default from the game install's `ControlSchemes` folder or a custom `.binds` file the player created by editing bindings in-game.** This is confirmed to always hold a value, even for a player who has never customized anything — it's how BindForge can know for certain which specific preset is active per section without guessing, rather than only being able to detect the presence of a custom file. See [BindForge Overview — First-Time Startup](../overview.md#first-time-startup) for the feature this enables, and [Preset Editor](../preset-editor.md) for how this file is otherwise managed.

---

## 8. In-Game UI vs. the Actual File

Cross-referencing the in-game Control Bindings UI's displayed action list against the real `.binds` file found several real discrepancies — the in-game settings screen does not faithfully represent everything the underlying file format supports:

- **`UI_Select`** ("Confirm/Select" in panel UIs) is bound and functional in a real profile (Keyboard `Space` + a joystick button) but **does not appear anywhere** in the in-game UI's Interface Mode section.
- **`HumanoidPing`** (on-foot "Ping/Call Out") exists as a real, bindable element in the file but is **completely absent** from the in-game UI in any on-foot section.
- **`MouseReset` and `BlockMouseDecay`** — ~~shown in the in-game UI as toggle-only settings with "no standard key binding slots"~~ **corrected 2026-09-07: the UI does show binding slots for these.** A screenshot of Ship Controls → Mouse Controls shows *Reset Mouse* and *Disable Relative Mouse* each carrying a Primary/Secondary slot pair, matching the XML. Either the original observation was mistaken or Frontier has since changed the screen. The XML **does** have real `<Primary>`/`<Secondary>` child elements for both (currently unbound in the reference profile). They are functionally bindable at the file level; the in-game UI simply doesn't expose the binding row, only the toggle.
- **`DeployHeatSink`** is bound and functional in a real profile but doesn't appear under either the Cooling or Miscellaneous sections of the in-game UI where it would logically belong.
- One apparent UI row, "Select Next Target," turned out to have **no corresponding XML element at all** — likely a duplicate transcription of "Cycle Next Target," or a label that changed between game builds without the underlying action name changing.

**Practical implication:** the in-game Control Bindings screen cannot be trusted as a complete inventory of what's bindable. Anything built against "what the game shows you" rather than "what the `.binds` schema actually supports" will silently miss real, functional bindings. See `EliteDangerous-ActionCatalog.md` for the fuller action-name catalog and its own accuracy caveats.

---

## 9. Binding Match Behavior

### 9.1 Exact-chord matching, not subset/priority

Elite's binding-match model is **exact-chord, not subset/priority-based**: a bare key and a modified chord on that same key are two distinct chords that both fire independently — neither suppresses the other. Two bindings only conflict when they share the **identical** key set within the same active context.

### 9.2 A retracted theory, and the lesson it left behind

An earlier round of testing (2026-06-23) reproduced a case that looked exactly like subset-key-set suppression: `LeftCtrl+LeftShift+LeftAlt+I` (bound to open the galaxy map) fired correctly, while the structurally identical `LeftCtrl+LeftShift+LeftAlt+Y` (also bound to open the galaxy map, with `Y` alone separately bound to a headlook-reset action) silently failed to fire. The working theory at the time was that Elite matches a binding whenever *all* its keys are held — not requiring "and nothing else" — making `{Y}` a subset of `{Ctrl,Shift,Alt,Y}`, and that the engine statically suppresses the longer/more-specific binding whenever this kind of overlap exists.

**This theory was retracted after further testing.** The original failure was traced to a **stale `.binds` reload** — the game had not yet re-read the file (§7) — so the test was observing an old binding state, not a genuine subset-suppression rule. The real, confirmed model is §9.1 above: exact-chord matching, with no subset suppression at all.

The lasting lesson: **any test methodology for `.binds` behavior must explicitly account for the "Controls screen must be opened to reload" lifecycle rule**, or it will misattribute a stale-read symptom to a real binding-logic bug — exactly as happened here.

---

## 10. Parsing / Writing Safety Rules

General good-practice rules for any tool that reads and rewrites `.binds` files, derived from the structural findings above:

1. **Preserve unknown elements exactly as-is.** Never drop an unrecognized element.
2. **Preserve unknown attributes.** Never drop an unrecognized attribute.
3. **Preserve element ordering.** There's no confirmed evidence the game depends on order, but there's no confirmed evidence it doesn't either — treat reordering as unsafe by default.
4. **Preserve formatting where possible** — avoid unnecessary whitespace churn that makes diffs (and backups) harder to reason about.
5. **Never silently drop a binding that fails to parse.** Log the failure and retain the raw element as an opaque, preserved node rather than discarding it.
6. **Always back up before writing.** Game updates already have a track record of unpredictably touching these files (§7) — an external tool adding more risk without a backup path is not acceptable.
7. **Validate well-formed XML before writing.**
8. **Write atomically** — write to a temp file, validate, then rename over the target, rather than writing in place.
9. **Preserve a UTF-8 BOM if present in the source file.**

### The game's own writer does not follow rule 1 — confirmed by live testing, 2026-09-01

An XML comment was placed in a live `.binds` file, the file was confirmed to be the active preset, and a
binding was then changed in-game to force a rewrite. **The comment was gone afterwards.**

This establishes that Elite Dangerous **reconstructs `.binds` from its own in-memory model rather than
round-tripping the file**. The practical consequences for BindForge:

- **Nothing BindForge writes into a `.binds` survives the next in-game binding change.** Not comments, not
  custom attributes, not extra elements. Any data BindForge needs to remember about a `.binds` must live
  outside it — see [Device Provenance](../../../03-data-models/device-provenance.md).
- Rules 1–5 above still bind **BindForge's** writer. They are how a well-behaved external tool avoids
  destroying data; they are not a description of what the game does.
- A corollary worth remembering: because the game rebuilds the file, anything in a `.binds` that the game's
  model does not represent is transient by nature — including any hand-edit a player makes outside the game.

**Not tested for `DeviceMappings.xml` or `.buttonMap`.** The game only reads those, so a play session cannot
exercise their write path. Their real threat is the patcher replacing install files wholesale during an
update, which no in-file marker would survive either — see `EliteDangerous-DeviceMappings-ButtonMap.md`.

---

## 11. Summary — Core Pitfalls in One List

1. Element *names* never indicate type — only structure does (`*ButtonPartial`-named elements are AXIS, not BUTTON).
2. The modifier cap (3) is enforced by **press order**, not key identity — the game's own capture process can and does mislabel a real action key as a `<Modifier>` and a real modifier key as the `Key=` attribute. Any executor trusting those labels at face value gets hold/tap roles backwards (§3.1).
3. **Elite only re-reads `.binds` when its own in-game Controls screen is opened** — editing the file externally has no effect in a running game session until that screen is opened. Skipping this step in testing produces symptoms that look like real bugs but aren't (§7, §9.2).
4. The binding-match model is **exact-chord, not subset/priority-based** — a bare key and a modified chord on that key are distinct and both fire independently (§9.1).
5. `Hold` can show inconsistently between the game's two display surfaces — the explanation panel vs. the slot display itself (§3.2).
6. The in-game Control Bindings UI is not a complete or fully accurate inventory of the `.binds` schema — real bindable elements exist that the UI never shows, and at least one UI row doesn't correspond to any real XML element at all (§8).
7. Controller/joystick device identity in the file is an opaque, undecodable hex string — no VID/PID/device-type distinction survives into the format alone (§4.2).
8. `DeviceMappings.xml`/`.buttonMap` genuinely duplicate per storefront install; `.binds` does not — these two file families have different multiplicity rules on disk (§7; full detail in `EliteDangerous-InstallPaths.md`).
9. Game updates can silently wipe the cosmetic files, and occasionally the bindings themselves, with no confirmed guaranteed pattern for when this happens (§7).
10. `KeyboardLayout` stores its value as element text content, not an attribute — the one structural outlier in the whole file (§2.4).

---

*Consolidated from a full structural extraction of a real 457-element `.binds` file and direct in-game testing (2026-06-23–2026-06-24). Behavioral claims not marked "confirmed by live testing" should be treated as structural observation only.*
