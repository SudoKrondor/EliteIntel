# Bindings infrastructure analysis — controller/joystick button queries

Diagnostic report only. No code changed.

Scope examined: `elite.intel.ai.hands` (`KeyBindingsParser`, `BindingsMonitor`,
`KeyboardKeyAvailabilityService`, `BindingModifier`, `BindingSlotType`, `Bindings`,
`BindingConflictRules`, `BindingGroup`/`BindingGroupClassifier`), plus the one consumer of
the read-only model, `elite.intel.ui.view.BindingsTabPanel` /
`BindingSlotDisplayFormatter`, and the StarVizion SDL device model
(`elite.intel.starvizion.model.SvDevice`, `SdlInputService`) for the device-identity
question in Q5.

---

## 1. Does `KeyBindingsParser` parse/expose controller bindings, or only keyboard?

**Both — at different layers, by design:**

- `parseReadOnlyBindingSlots(File)` (`KeyBindingsParser.java:156-203`) parses **every**
  Primary/Secondary slot for **every** action element in the `.binds` XML, regardless of
  `Device`. This includes `Device="Keyboard"`, `Device="Mouse"`, `Device="{NoDevice}"`, and
  raw 8-hex-char controller device IDs (e.g. `Device="045E028E"` with `Key="Joy_1"`, as seen
  in `BindingsWriterTest`). This is the "read-only diagnostic model" referenced in the
  class's own Javadoc.

- `parseBindingSlots(File)` and `parseBindings(File)` (`KeyBindingsParser.java:114-147`)
  build the **executable** maps used by command dispatch / missing-binding checks. They
  call `parseReadOnlyBindingSlots()` internally, then run each slot through
  `toExecutableBinding()` (`KeyBindingsParser.java:222-225`), which returns `null` unless
  `slot.keyboardUsable()` is true. `isKeyboardUsable()` (`KeyBindingsParser.java:232-235`)
  requires `Device="Keyboard"` with a real key **and** every `<Modifier>` also on
  `Device="Keyboard"`. Any HOTAS/joystick/gamepad/mouse slot collapses to `null` here and
  is dropped from the executable map entirely.

**Net answer:** controller/joystick bindings ARE parsed and exposed, but only through the
read-only diagnostic API (`parseReadOnlyBindingSlots`); the keyboard-execution path
(`parseBindingSlots`/`parseBindings`, and everything built on top of it —
`BindingsMonitor.getBindings()`, conflict detection, missing-binding checks) is
keyboard-only by construction.

---

## 2. Is there a data model with device type + device ID + button ID?

**Partially.** `ReadOnlyBindingSlot` (`KeyBindingsParser.java:78-97`) is the closest thing:

```java
public record ReadOnlyBindingSlot(
        String device,                       // raw Device="..." attribute
        String key,                          // raw Key="..." attribute
        String[] modifiers,                  // flattened modifier key tokens
        List<BindingModifier> bindingModifiers, // full Modifier device+key pairs
        boolean hold,
        BindingSlotType slotType,
        boolean keyboardUsable,
        boolean editable
)
```

and `BindingModifier` (`BindingModifier.java:10`) is `record BindingModifier(String
device, String key)` for `<Modifier>` children.

What this gives you, for a controller slot like `<Primary Device="045E028E" Key="Joy_1" />`:

- `device` = `"045E028E"` — an opaque 8-hex-character string. For Elite's `.binds` format
  this is derived from the controller's hardware GUID (VID/PID-based), but **the codebase
  does not decode or interpret it** — it's treated as an opaque diagnostic string. The only
  place anything is *done* with it is `BindingSlotDisplayFormatter.isRawDeviceId()`
  (`BindingSlotDisplayFormatter.java:51-53`), which just regex-checks "is this 8 hex
  digits" to decide whether to prefix the UI label with `"Device "`.
- `key` = `"Joy_1"` — also an opaque string token. Nothing parses the `Joy_` prefix into a
  numeric button index; `BindingSlotDisplayFormatter.formatBindingToken()`
  (`BindingSlotDisplayFormatter.java:25-33`) strips the prefix only for *display*
  (`"Joystick 1"`), and only for tokens that literally start with `"Joy_"`. Axis tokens
  (`Slider1`, `XAxis`, etc.) and POV/hat tokens (`Joy_POV1Up` etc.) aren't specifically
  handled anywhere — `formatBindingToken` would pass `Joy_POV1Up` through its `Joy_`
  branch and render `"Joystick POV1Up"`, and would pass an axis token like `XAxis` through
  unchanged.

So: **device + key together form a de-facto (device-id, button/axis-token) pair**, but:
- There is **no separate "device type" field/enum** — `device` conflates "type" (Keyboard /
  Mouse / {NoDevice}) and "instance identity" (the 8-hex controller ID) into one string,
  with no enum distinguishing Joystick vs Gamepad vs HOTAS vs wheel, etc. (Elite's `.binds`
  format itself doesn't carry that distinction beyond the opaque hex ID.)
- There is **no structured button/axis/POV model** — `key` is a raw string token, not a
  parsed `{kind: BUTTON|AXIS|POV, index: N, direction: ...}` structure.

---

## 3. What does the "read-only diagnostic model for non-keyboard bindings" contain, and how is it accessed?

It's the `ReadOnlyBindingSlot` / `ReadOnlyBindingSlots` pair described above (the term
comes directly from the Javadoc on both `KeyBindingsParser.KeyBinding`
(`KeyBindingsParser.java:39-44`) and `ReadOnlyBindingSlot`
(`KeyBindingsParser.java:71-97`)).

- **`ReadOnlyBindingSlots(ReadOnlyBindingSlot primary, ReadOnlyBindingSlot secondary)`**
  (`KeyBindingsParser.java:105-106`) — one pair per action.
- **Access point:** `KeyBindingsParser.getInstance().parseReadOnlyBindingSlots(File
  bindsFile)` → `Map<String, ReadOnlyBindingSlots>`, keyed by the XML action tag name
  (e.g. `"GalaxyMapOpen"`, `"PrimaryFire"`). Either `primary` or `secondary` may be `null`
  if that slot isn't present in the file at all (not just empty).
- **Per-slot contents** (repeated from Q2): `device`, `key`, `modifiers[]`,
  `bindingModifiers` (full `BindingModifier` list, including non-keyboard-device
  modifiers), `hold`, `slotType` (`PRIMARY`/`SECONDARY`), `keyboardUsable`, `editable`.

**Current consumer:** only `BindingsTabPanel` (`BindingsTabPanel.java:267-268`, `:563-571`),
via `BindingSlotDisplayFormatter.formatSlot()`. It's used purely to render the read-only
"Bindings" tab — e.g. turning `device="045E028E", key="Joy_1"` into the string `"Device
045E028E | Joystick 1"`. Nothing in the codebase currently iterates this map
programmatically to answer occupancy/conflict questions for non-keyboard devices — it's
display-only today.

---

## 4. Is there an existing mechanism to ask "is controller device X button Y already bound to a game action?"

**No reliable existing mechanism**, though the raw data is reachable. Specifically:

- **`KeyboardKeyAvailabilityService`** (`KeyboardKeyAvailabilityService.java`) is the
  existing "is this binding occupied" service, but it is hard-scoped to keyboard:
  `isOccupiedKeyboardKey()` (`KeyboardKeyAvailabilityService.java:227-236`) literally
  requires `"Keyboard".equals(device)`. `occupiedKeyboardKeys()`,
  `isKeyOccupiedByOtherSlot()`, `availableKeys()` all funnel through this — a controller
  `Device="045E028E" Key="Joy_1"` slot is invisible to every method in this class (per its
  own doc comment: "HOTAS, joystick, gamepad, and mouse bindings ... cannot consume a
  keyboard key for this MVP").

- **`BindingsMonitor.checkForConflictsAndPersist()`** (`BindingsMonitor.java:196-260`)
  builds a `byCombo: Map<comboString, List<actionName>>` inversion — conceptually the
  right shape for "what actions share this binding" — but it's built from
  `getBindings()`, which is `KeyBindingsParser.parseBindings()`'s **executable, keyboard-
  only** map (`BindingsMonitor.java:60, 183-185, 198`). Non-keyboard combos never enter
  `byCombo`.

- **`parseReadOnlyBindingSlots()`** is the only API that surfaces `(device="045E028E",
  key="Joy_1")` pairs at all, but nothing inverts/indexes it by `(device, key)` — you'd
  have to write a fresh loop over every `Map.Entry<String, ReadOnlyBindingSlots>`, check
  both `primary`/`secondary` (and their `bindingModifiers`, for cases where a controller
  button is used as a *modifier* on another slot) for a matching `(device, key)`.

- **Separately**, there is no existing correlation between "controller device X" as a
  *caller* would name it and the `.binds` file's `device` string:
  - The StarVizion SDL layer (`elite.intel.starvizion.model.SvDevice`,
    `SdlInputService.onDeviceAdded`) identifies devices by **SDL joystick instance ID**
    (`int`, session-scoped, reassigned on reconnect) plus `SDL_GetJoystickNameForID()`.
  - The `.binds` file identifies devices by an **8-hex-character ID** derived from the
    device's DirectInput/SDL GUID.
  - A grep across `elite.intel.ai.hands` and the wider `elite.intel` tree found **no code
    that computes, stores, or compares these two identifiers** — `VID`/`PID`/`GUID` do not
    appear anywhere outside the StarVizion SDL bindings and unrelated `ProcessHandle`
    "PID" usages in `Updater`/`WindowsNativeKeyInput`.

So today, "is controller device X button Y bound" can't be answered end-to-end: even if you
write the `(device, key)` scan against `parseReadOnlyBindingSlots()`, you have no way to
turn "device X" (as SDL/StarVizion would identify it) into the `.binds` file's `device`
string to scan for, and no way to turn "button Y" (an SDL button index) into the `Joy_N` /
POV / axis token the `.binds` file uses.

---

## 5. What would need to be added/extended to support that query reliably?

Three roughly independent pieces, in increasing order of how much is "new":

1. **A non-keyboard occupancy index/service**, sibling to `KeyboardKeyAvailabilityService`
   (e.g. operating on `parseReadOnlyBindingSlots()` output rather than re-parsing XML):
   - Invert every action's `primary`/`secondary` slots **and** their `bindingModifiers`
     into `(device, key) -> List<(actionName, slotType, isModifier)>`.
   - This is the same shape of inversion `BindingsMonitor` already does for keyboard combos
     (`byCombo` in `checkForConflictsAndPersist()`), just generalized to raw
     `(device, key)` pairs without the `isKeyboardUsable()` filter, and additionally
     covering `bindingModifiers` (currently parsed into `ReadOnlyBindingSlot` but never
     indexed/queried by anything).
   - Could reuse `BindingsMonitor`'s existing reload-on-file-change cycle
     (`parseAndUpdateBindings()`) to keep the index fresh without per-query re-parsing.

2. **A device-identity bridge** between SDL's joystick identity (`SvDevice` /
   `SdlInputService`, instance-ID + name, queryable via `SDL_GetJoystickGUIDForID`) and the
   `.binds` file's 8-hex `Device` attribute. Without this, "controller device X" from the
   caller's perspective (StarVizion/SDL) can never be turned into the string to look up in
   the index from (1). This is new code in either `elite.intel.ai.hands` or a shared
   identity-utility package — nothing like it exists today.

3. **A button/axis/POV token mapper** between SDL's representation (button index, axis
   index + sign, hat index + direction — all integers/enums) and Elite's `.binds` tokens
   (`Joy_N`, `SliderN`, `XAxis`/`YAxis`/..., `Joy_POVN<Direction>`). Notes:
   - `BindingSlotDisplayFormatter.formatBindingToken()` only does a cosmetic `Joy_N` →
     `"Joystick N"` rename for display; it is not a structured parser and doesn't handle
     axes or POV tokens distinctly.
   - SDL button indices are 0-based; Elite's `Joy_N` tokens appear 1-based in the existing
     test fixtures (`Joy_1`), so an off-by-one normalization is required, plus entirely
     separate handling for axes/sliders/hats which aren't "buttons" at all.

Given the project's existing conventions (`KeyboardKeyAvailabilityService` as a stateless
service taking a `Path bindsFile`, `BindingModifier`/`BindingSlotType` as small immutable
records, `BindingsMonitor` owning reload/caching), the most consistent approach would be a
new stateless `ControllerBindingAvailabilityService`-style class for (1), with (2) and (3)
as separate small utilities/records it depends on — keeping the `.binds`-parsing concerns
in `elite.intel.ai.hands` and the SDL-identity concerns wherever StarVizion's device model
ends up living, rather than merging the two subsystems.
