# BindForge — Type Mode Specification

**Project:** Elite Intel
**Feature:** BindForge — Type Mode
**Version:** Draft
**Status:** Deferred — design not yet finalized. To be revisited after Purpose Mode is complete.
**Parent document:** `BindForge_Spec.md` Section 6.3

---

## 1. Overview

Type Mode is a planned future viewing mode for the BindForge Bind Editor. The intent is to group bindings by input type rather than by game category or player intent.

Where Game Mode asks "what section of the game does this binding belong to?" and Purpose Mode asks "what is the player trying to accomplish?", Type Mode asks "what kind of physical input is this?"

The primary motivation is to help players verify that their axis assignments make logical sense across their full controller setup. For example a player with a dual HOTAS setup wants to see all their axis bindings together to confirm that pitch, roll, yaw, and thrust are assigned to axes that feel physically natural on their specific controllers — and that no axis directions are accidentally inverted relative to each other.

---

## 2. Likely Input Type Groups

The following input types are expected to form the basis of Type Mode grouping. This list is not finalized and is subject to revision during design:

| Input Type | Description |
|---|---|
| Keyboard | Bindings assigned to keyboard keys with or without modifiers |
| Joystick Axis | Bindings assigned to analog axes — pitch, roll, yaw, thrust, sliders |
| Joystick Button | Bindings assigned to discrete controller buttons |
| POV / Hat | Bindings assigned to POV hat switches and directional pads |
| Mouse | Bindings assigned to mouse buttons or axes |
| Unbound | Bindings with no Primary or Secondary assignment |

---

## 3. Relationship to Other Modes

Type Mode may function as a reverse of either Game Mode or Purpose Mode:

- **Reverse of Game Mode** — input-first instead of action-first. Start with "I pressed Joy_1 on my left stick" and see every game action bound to that input.
- **Reverse of Purpose Mode** — start with an input type and see all purpose groups that use it.

The exact design relationship is not yet determined. Further design work is required before Type Mode can be fully specced.

---

## 4. Prerequisites

Type Mode cannot be designed or built until the following are complete:

- **Binds File Audit** — the complete inventory of all binding actions and input types is required. See `BindForge_BindsFileAudit.md`.
- **Game Mode stable** — Type Mode builds on the same binding data model as Game Mode.
- **Purpose Mode complete or explicitly deferred indefinitely** — Type Mode's design may depend on or overlap with Purpose Mode concepts.

---

## 5. Known Open Questions

- Is Type Mode a standalone mode or a filter that works within Game Mode or Purpose Mode?
- Should Type Mode show a visual representation of the controller — a diagram of the device with bound actions labeled next to each control?
- How does Type Mode handle bindings that use both a keyboard modifier and a joystick button simultaneously?
- Is there user value in Type Mode beyond axis verification? If axis verification is the primary use case, a simpler dedicated "Axis Review" panel might serve better than a full mode.
