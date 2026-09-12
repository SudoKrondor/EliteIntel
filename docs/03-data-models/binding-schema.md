# Data Model — Binding Schema

Shared conceptual model for a parsed Elite Dangerous binding profile, used by BindForge. A binding may be
held by any input device — keyboard, mouse, joystick, HOTAS, gamepad or pedals — and the model does not
privilege one over another. Described here as data structures/concepts — not as source code for any particular language or framework. For the raw on-disk file format this is derived from, see [BindForge's binds-file domain knowledge](../02-features/bindforge/domain-knowledge/EliteDangerous-BindsFileFormat.md).

## What a Loaded Profile Holds

- Which `.binds` filename is assigned to each of the four binding sections (General, Ship, SRV, On Foot) — sourced from the active-preset file.
- The file-level keyboard layout setting — must be preserved and written back on every save, to support layout-aware key resolution.
- The full list of parsed binding elements from the active `.binds` file.
- The list of known devices, from `DeviceMappings.xml`.
- Button/axis friendly-label data, from `.buttonMap` files.

## Binding Element — Three Distinct Kinds

A binding element is one of exactly three kinds, distinguished by structure, never by its name (some real elements are named in a way that suggests one kind but are structurally another):

1. **Button-type** — has a Primary and/or Secondary input slot; either slot may be unbound; may carry an element-wide toggle flag covering both slots as one shared behavior (not per-slot).
2. **Axis-type** — has exactly one input slot (not a Primary/Secondary pair); may carry Inverted and Deadzone properties; never has a toggle flag, never has a Secondary slot, never takes a modifier.
3. **Standalone-setting-type** — a bare value with no slot at all; cannot conflict with anything; covers sliders/dropdowns/toggles that have no key-binding concept (mouse sensitivity, deadzone curves, menu-group toggles, etc.).

**Design principle:** the three kinds are modeled with distinct shapes, not one flat structure with a lot of "only applies sometimes" fields. This prevents assigning an axis-only property to a button element (or vice versa) as a category of error at the modeling level itself.

## Input Slot Shape

An input slot holds: a device identifier, a key/button token, and zero to three modifier device+key pairs. A button-type slot additionally carries a Hold flag — whether the input was held for roughly a second or more versus tapped during capture. This is a capture-method artifact, not something a user chooses through an obvious setting.

**Critical behavioral caveat:** the raw parsed values for a slot's main key versus its modifiers must be preserved exactly as read, for accurate round-tripping — but must never be trusted at face value by anything that actually presses keys to fire a binding. The game's own capture process does not treat "main key" versus "modifier" as meaningful category labels; it only tracks press order, so whichever key was released last ends up recorded as the slot's main value, even if that key is conceptually the modifier. Anything executing a binding must classify each key by its own identity (is this literally one of the recognized modifier keys, regardless of which slot position it landed in) rather than trusting slot position. See [Binding Conflict Rules](../02-features/bindforge/domain-knowledge/EliteDangerous-ConflictRules.md) for the confirmed behavior this derives from.

## Device Identity

One device entry per named controller: a friendly name, a primary VID/PID pair, and zero or more alternate VID/PID pairs (for a controller that can present a different PID depending on firmware configuration). VID/PID hex strings are inconsistently cased across real files and must be compared case-insensitively, though stored as read.

For an axis-type slot or any VID/PID-identified device, the on-disk device identifier is an 8-hex-character string formed by concatenating the 4-hex VID and 4-hex PID exactly as stored in the device-identity file (for example, VID `3344` + PID `43F4` → `334443F4`). This string is not a general-purpose device GUID and carries no separate "device type" — joystick versus gamepad versus wheel is not distinguishable from the binding file alone. Button-type bindings instead reference the device's element name directly.

## Live-Input-to-File-Format Numbering Offset (Needs Independent Testing)

Two different systems number a controller's buttons, and they don't start counting from the same number. The underlying system BindForge uses to read live controller input (powering things like live highlighting when a button is pressed, and click-to-bind) numbers buttons starting from **0** — the first button is index 0, the second is index 1, the third is index 2, and so on. Elite Dangerous's own `.binds` file format numbers the same buttons using `Joy_N` tokens starting from **1** — `Joy_1`, `Joy_2`, `Joy_3`.

So the third physical button on a controller is index `2` to the live-input system, but `Joy_3` in the `.binds` file — a consistent one-off gap, not a rare edge case. Any code path that converts between the two (translating "the user just pressed this live-input index" into "the `Joy_N` token to write or match against") must account for this offset in the right direction, or every button will be captured/highlighted one off from the one the user actually pressed.

This exact conversion is stated as settled fact with worked examples in some source documents, and flagged as still needing independent verification in others, with nothing bridging the two the way the game's conflict-resolution behavior was verified elsewhere in this project (see [Bind Editor — Shared Conflict Detection](../02-features/bindforge/bind-editor.md#how-the-game-actually-resolves-conflicts-confirmed-by-direct-in-game-testing) for what that level of confirmation looks like). Treat this offset as correct in direction but **not yet independently confirmed by testing** — alongside the VID/PID-duplication question and the Linux path question, this is a standing testing-required item, not a design decision. See [conflicts-and-open-questions.md](../00-overview/conflicts-and-open-questions.md).

## Button/Axis Label Data

A per-device map from a raw button/axis token to either free-form text or a bracketed icon-reference token. No dedicated wrapper is needed beyond that map, since a wrapper's only field beyond the label text would duplicate the key already used to store it.

## Parsing Tolerances (confirmed against real files)

- Duplicate top-level element names are possible and must be tolerated on read — model as a list, not a map, so duplicates do not collide.
- The device-identity file may or may not have an XML declaration and may contain comments inside its root — both must be tolerated.
- Button-label files show no duplicate keys in samples but do not follow button-number document order — nothing should assume ordering.

## Open Question — Modeling Philosophy

An earlier design pass proposed a single flat binding-entry shape covering both button and axis concerns simultaneously (all fields always present, with derived flags indicating which fields are actually meaningful for a given row). The distinct-shapes model above was chosen specifically to reject that pattern. Both describe the same underlying real-world data; they disagree only on shape. See [conflicts-and-open-questions.md](../00-overview/conflicts-and-open-questions.md).
