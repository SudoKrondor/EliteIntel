# Bind Editor — editing

**Editing every control on every device from the Binding Profile
grid.** The grid, search, conflict colouring and the keyboard capture dialog shipped in V1.1; this section grows them into the Bind Editor the spec describes. Its sibling section, [Anomalies](README.md#sections-in-build-order), is separate.

## Spec

- [bind-editor.md](../02-features/bindforge/bind-editor.md) — **read the sections a slice names end to end**:
  [Capture Dialog](../02-features/bindforge/bind-editor.md#capture-dialog--settled-2026-09-13) (all of it, down to *Open
  items*), and [Modifiers, Hold, Inverted, Deadzone](../02-features/bindforge/bind-editor.md#modifiers-hold-inverted-deadzone).
- [overview.md — Two narrow boundaries](../02-features/bindforge/overview.md#two-narrow-boundaries--one-stays-one-widens)
  — why a deliberate edit may write a controller slot and nothing automatic may.
- [EliteDangerous-BindsFileFormat.md](../02-features/bindforge/domain-knowledge/EliteDangerous-BindsFileFormat.md)
  §5 — the tokens: `Joy_N`, `Joy_POVnUp`, `Pos_`/`Neg_` axis directions, `Joy_XAxis`…`Joy_VAxis`.
- [foundation.md](foundation.md) — F4, the writer's user-edit path this section writes through.

## Built

| What | Where |
|---|---|
| The grid | `ui.screen.bindings.BindingProfilePanel`, tables from `ui.support.BindingsGroupTableFactory`, cells drawn by `ui.render.BindingSlotCellRenderer` |
| Button names in the grid, the dialogs and the search | `bindforge.devicefiles.SavedDeviceLabels` (draft over master, old name while a rename waits); `ui.support.BindingSlotDisplayFormatter` (`withSavedLabels`, `rawTokens`, `controllerSearchText`); `SlotCell` with the raw token as tooltip |
| AI ACCESS column | `bindforge.rules.AiAccess`, `ui.support.AiAccessCell`; recorded in [bind-editor.md](../02-features/bindforge/bind-editor.md#the-mechanism-an-assistant-column-in-the-grid--settled-2026-09-12) |
| The capture dialog (E1) | `ui.dialog.AssignKeyboardBindingDialog` (name kept) — a **row editor**: title = in-game name, CONTROL / XML BINDING, then a card per slot (`SlotCard`: now, → new, CLEAR, what blocks it), the orange card being filled; one picker below that follows the orange card (`showSourceOf`) — KEYBOARD (V1.1's capture field and map, checks unchanged) and a tab per connected controller; one SAVE for every changed slot. Per-slot state in `SlotEdit`. `ui.dialog.ControllerInputsPanel` (BUTTONS / HATS pages, names, free/used/own colouring, click to pick); `KeyboardAvailabilityView.setOtherSlotKey` |
| Which controllers, under which name (E1) | `bindforge.devices.CaptureControllers.all` — connected ones, plus every one the master or the bindings name (click-only when unplugged); the master's name, else Frontier's; unnamed connected ones get a greyed tab |
| Press-to-capture (E1) | `bindforge.devices.ControllerCapture` — held inputs on any controller become modifiers (up to three); hats as `Joy_POV` tokens. Fed from `DeviceBus` while the dialog is open |
| Input already held elsewhere (E1) | `bindforge.rules.SlotInputConflicts` — exact chord, same Hold, `isSafeOverlap` contexts. Blocks SAVE |
| Saving (E1) | `ui.support.AssignSelection` — `AssignKeyboardBindingSelection` (keyboard path, unchanged) or `AssignSlotSelection` → `BindingsWriter.assignSlot`; `BindingProfilePanel.saveSelections` writes them in turn, re-stamping the file after each, Hold kept from each slot |
| Required-by-Vega filter | `BindingProfilePanel.filterVegaOnly`, a footer checkbox beside *Show conflicts only*. It keeps the rows whose AI ACCESS state is not `NOT_USED`, and combines with the conflicts filter and search |
| Clear column | `ui.dialog.ClearKeyboardBindingDialog` — keyboard slots only, as V1.1 shipped it |

## Settled — and where it is recorded

- **A click on a slot opens the
  dialog**; the Capture-button rule is reversed. [Capture is the only way in](../02-features/bindforge/bind-editor.md#capture-is-the-only-way-in)
- **An input another control holds blocks SAVE on every
  tab**; the user frees it first. The writer stays permissive. [What the writer takes from the dialog](../02-features/bindforge/bind-editor.md#what-the-writer-takes-from-the-dialog--settled-2026-10-12-alan-foundation-f4)
- **The dialog edits the row: both slots as cards, all sources in view, one
  SAVE.** [The dialog edits the row](../02-features/bindforge/bind-editor.md#the-dialog-edits-the-row-both-slots--settled-2026-10-15-alan-e1)
- **A controller's tab writes the master's
  name.** [Which name a controller's tab writes](../02-features/bindforge/bind-editor.md#which-name-a-controllers-tab-writes--settled-2026-10-15-alan-e1)
- **The KEYBOARD tab is V1.1's dialog unchanged**, including its refusals (Alan, 2026-10-15).
- **AI ACCESS wording, and only Vega's controls
  judged.** [The mechanism](../02-features/bindforge/bind-editor.md#the-mechanism-an-assistant-column-in-the-grid--settled-2026-09-12)

## Known issues

- **A keyboard key with a stick button held as its modifier cannot be
  captured.** The keyboard tab sees keys only, and a controller tab sees controller inputs only.
- **The Clear column still clears keyboard slots only.** A HOTAS slot is cleared from the dialog's CLEAR BINDING.
- **Losing Vega's access is a warning, not a
  block** (amber banner above the tabs). The user can save past it. The grid's AI ACCESS column shows the result afterwards.
  *Raise with Alan:* it is the kind of message he says nobody reads.
- **The controller conflict check matches exact chords
  only.** The keyboard check also flags a modifier that another control binds on its own (`shadowedModifier`). Whether that holds for controller buttons is unmeasured.

## Boundaries

- **No change to `bindforge.io` or existing `bindforge.rules`
  classes.** The writer is used exactly as F4 merged it. A new class only V1.2 calls may go in `bindforge.rules` (README).
- **`KeyBindingsParser` never widens.** Writing a controller binding never makes it executable.
- **The keyboard path and its refusals stay as they are**, for the auto-fixer and for the keyboard tab.

## Slices

| # | Slice | Status |
|---|---|---|
| E1 | **Controller buttons and hats in the capture dialog.** The gate is removed, there is a tab per connected controller, capture by press or click, held buttons become modifiers, an input in use blocks SAVE, the AI-access warning shows, and saving goes through `assignSlot` | **done 2026-10-15** |
| E2 | **AXIS DIRECTIONS on a button slot.** `Pos_`/`Neg_Joy_*Axis` as a third page of a controller tab, captured by pushing an axis past a threshold from where it rests (reuse `DeviceEditorLights`' rest rule) and mapped through `DeviceDescription.axisAt` | **next** |
| E3 | **Axis rows.** One `<Binding>` plus Inverted and Deadzone, both inline and in the dialog. The AXES page is captured by moving an axis, and saving goes through `assignAxis`. The grid needs the axis-row column shape (spec § *Modifiers, Hold, Inverted, Deadzone*) | ready after E2 |
| E4 | **MOUSE tab.** Buttons and the wheel half-axes are chosen, never captured (spec § *MOUSE is chosen, not captured*) | ready |
| E5 | **Cross-device chords.** A keyboard key with controller buttons held, and a controller input with keyboard modifiers held | ready |
| E6 | **Game Mode and Settings rows** — the spec's grid modes. Read the spec first: scope the slice before building | not scoped |

## Hand-off notes — newest first

**2026-10-15, E1 fix: the input page is larger.** A controller page is now eight rows of four, so a 32-button
stick no longer scrolls, and a page with more than 32 inputs splits into numbered group tabs (`1–32`, `33–64`…),
as the Device Editor does. Pressing an input brings its group forward (`ControllerInputsPanel.showGroup`).
Recorded in [bind-editor.md](../02-features/bindforge/bind-editor.md#tabs-are-input-sources-sub-tabs-are-input-kinds).
**For E2:** an AXIS DIRECTIONS page built through `page()` gets the same grouping for free.

**2026-10-15, E1 reworked after Alan's first run.**

-
**Why:** the per-cell dialog was hard to follow, even for Alan. It is now a row editor; the decision is in the spec, [The dialog edits the row](../02-features/bindforge/bind-editor.md#the-dialog-edits-the-row-both-slots--settled-2026-10-15-alan-e1).
- **Also fixed from that
  run:** the theme's scroll bar, names without the `JOY_N` prefix, and the slot's own input shown orange rather than as free.
- **Watch for in
  E2+:** two writes per SAVE re-stamp the file in between (`saveSelections`). The keyboard map is built for the slot the dialog opened on, which matters only for a Hold slot's colouring.
- **Strings:** `bindings.assign.slot`, `currentValue` and `newInput` were removed from all seven bundles.
-
**Height:** the first rework ran off a laptop screen and hid SAVE. Now the body scrolls under a fixed header and footer, and the dialog is capped to the screen's usable area (`fitToScreen`). CONTROL and the XML tag share one line, the keyboard legend is the map's tooltip, and the controller list shows six rows.
- **Colours and going
  back:** inputs the control holds now sit on blue (`HUD_COLOR_ROLE_ASSIGNED_INPUT_BACKGROUND`). A chord either slot held at opening stays green and is never refused (`isOwnChord`,
  `KeyboardAvailabilityView.setOwnChords`, `ControllerInputsPanel` `own`), so a cleared key no longer reads as taken.
- **Refused picks and
  UNDO:** a pick that cannot be saved no longer sits in its card. `choose` restores the slot and records why in `SlotEdit.rejected`, and each changed card has UNDO (`onUndo`).
- **Unplugged
  controllers:** Alan's stick slept and its tab vanished. Every controller the master or the bindings name now has a tab (`CaptureControllers.all`). The keyboard field no longer says
  *Not defined* under a stick slot; it says *Click here, then press a key*.
- **One picker, not
  two:** with keyboard left and controllers right, a stick Primary and a keyboard Secondary read as cross-wired. The single picker follows the orange card, and the dialog is now at least 760 px wide instead of 1180.

**2026-10-15, E1 done.**

-
**Decisions:** Alan reversed the Capture-button rule, extended the keyboard tab's block-on-conflict to controllers, and settled the master's name for renamed devices. All three are recorded in bind-editor.md with the date.
- **Spec gaps
  closed:** the "what the dialog cannot do yet" table now shows the HOTAS gate, the keyboard-only wording, hats, and the raw-tag title as closed.
- **Strings:** the gate's `bindings.assign.unsupportedReadOnlyMessage` was removed from all seven bundles.
  `bindings.assign.dialogTitle` and `section.assignment` now read *Assign control*.
- **For
  E2:** `ControllerInputsPanel` takes a list of tokens per page, so a third page is a list and a capture method on `ControllerCapture`. A whole axis must never land on a button slot, and the writer refuses one anyway.
- **Not
  done:** the dialog has not been run against a real HOTAS. The unit tests cover the rules (`ControllerCaptureTest`, `SlotInputConflictsTest`); the Swing layout and live capture still need Alan's eyes.
