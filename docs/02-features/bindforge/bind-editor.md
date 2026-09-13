# BindForge — Bind Editor

The Bind Editor is where the player views and edits their actual key/button/axis bindings. It organizes the same underlying binding data through multiple **modes**, each offering a different lens on it. Conflict detection is shared infrastructure used by every mode, not owned by any single one — see [Shared Conflict Detection](#shared-conflict-detection) below.

## Shell (Common to All Modes)

- A Bindings File dropdown plus Load button, above everything. Load performs the standard [Live File Synchronization](overview.md#live-file-synchronization) freshness check for the Bind Domain before opening the file for editing.
- A Search box, above all mode tabs — filters whichever list is currently showing in whichever mode tab is active. **It clears in one action**: a × appears inside the field once there is anything to clear, and Esc does the same for anyone whose hands are already on the keyboard — which, in a bind editor, is most of them. Clearing restores the full list and returns focus to the field. Controller Mode is picture-based, not a list, so how (or whether) Search applies there is still open.
- A **Show Anomalies Only** checkbox/toggle, on the same line as Search — shared shell state, not owned by any one mode, but what it filters *to* depends on which mode tab is active: in Game Mode it filters the grid to conflicting rows only (see [Conflict Display](#conflict-display)); in Action Groups it filters the group list to only groups containing at least one binding that conflicts with something in a *different* group (see [Action Groups — Conflict Filtering](#action-groups)); in Input Mode it filters the same way Game Mode does, since Input Mode's list is already the same row format with conflicts already shown inline. It has no meaningful effect on the Conflicts tab, since that tab's entire content already is the anomaly list — not undefined, just not applicable. Control Types and Controller Mode don't need a defined behavior either, since both are confirmed back-burner and out of scope.
- Mode tabs: **Game Mode**, **Action Groups**, **Control Types**, **Controller Mode**, **Input Mode**, **Anomalies** (see below for each mode's status).
- A Context Bar of buttons (All / General / Ship / SRV / On Foot) filtering the grid to one section.
- A collapsible-group binding grid: left-click toggles one group; right-click opens Expand All / Collapse All, scoped to the currently visible groups.
- A roughly 75/25 split between the binding grid and the Binding Editor panel.

## Game Mode

**Status: exists as Krondor's Binding Profile, and grows in phase 3** — corrected 2026-09-13; this line
used to say *built*, which overstated it. Elite-Intel's `BindingProfilePanel` already organizes bindings to
match the in-game controls UI — the same four top-level sections and the same group structure the game
uses — with the game's own row names (`BindingDisplayNames`), search, and a show-conflicts-only filter.
See [Binding Zone Map (domain knowledge)](domain-knowledge/EliteDangerous-ActionCatalog.md) for the full,
cross-verified section/subgroup/action inventory.

### What Game Mode still needs

| Addition | Where it is specified |
|---|---|
| Capture for **every device**, through the capture dialog | [Capture Dialog](#capture-dialog--settled-2026-09-13) |
| The **ASSISTANT** column | [The mechanism: an ASSISTANT column](#the-mechanism-an-assistant-column-in-the-grid--settled-2026-09-12) |
| Conflict colouring on the **slot cell**, not the whole row | [FN-1](#fn-1-in-detail--scoped-2026-09-12) |
| The in-game name and the XML tag in the capture dialog | [What the dialog shows](#what-the-dialog-shows) |

### Conflict Display

Carried forward from a proven, already-real pattern, not invented fresh:
- Conflicting rows render in a distinct warning color.
- Hovering a conflicting row shows a **persistent** popup (not an auto-dismissing tooltip, which vanishes too fast to read a full explanation) listing every other action sharing that key.
- The shell's [Show Anomalies Only toggle](#shell-common-to-all-modes) filters the grid to rows carrying an anomaly while Game Mode is active.

### Capture Dialog — settled 2026-09-13

**BindForge captures through Krondor's existing dialog, grown — not a new one.**
`AssignKeyboardBindingDialog` already does the hard parts: it captures a chord by having the commander press
it, colours an on-screen keyboard free and used, and warns about conflicts, reserved chords and the game-menu
key *before* anything is kept — leaving all validation and writing to `BindingsWriter`. It already dims the
window behind it, too. The earlier design, a floating popup *"modeled directly on the base game's own rebind
popup"*, described a dialog nobody built while this one shipped. The class keeps its name under the
[package freeze](../../00-overview/v1.2-scope.md#the-code-stays-where-it-is--stated-by-krondor-2026-09-12);
what it shows and what it captures grows.

**One dialog for every slot, with tabs that grey out what cannot fill it** (Alan, 2026-09-13). Earlier the
same day this was split into a button dialog and an axis dialog; it was folded back into one, because a single
dialog that shows every input source — and says plainly which ones do not apply — is one thing to learn
rather than two. **No mode gets its own variant:** Game Mode, Action Groups, Input Mode and Anomalies all open
this dialog, and Control Types will too if it is picked back up.

#### Capture is the only way in

The dialog opens from a slot's **Capture** button, never from clicking the row. Browsing the grid is always
safe: **a stray click cannot start a rebind** (Alan, 2026-09-13). Krondor's editor currently opens its dialog
from a click on the slot, so this is a change to the existing behaviour, not a carry-over of it.

#### Tabs are input sources; sub-tabs are input kinds

```
[KEYBOARD]  MOUSE  VPCThrottle 82  VPCPanel 43  RVWAP 31
             BUTTONS 79   HATS 0   AXIS DIRECTIONS 6   AXES 3
```

**KEYBOARD** and **MOUSE** come first; every connected controller follows, one tab each. A controller's tab
is split by **input kind** — **BUTTONS**, **HATS**, **AXIS DIRECTIONS**, **AXES** — so every device has the
same four pages whatever its hardware. Inside BUTTONS, the device's own `.buttonMap` labels group the inputs
under headings (the throttle's right grip, left grip, base, toggles, encoders), so a large device stays
findable.

**What the slot enables:**

| Tab or sub-tab | Button slot | Axis slot |
|---|---|---|
| KEYBOARD | enabled, and the default | greyed — an axis row never takes a key |
| MOUSE | enabled | greyed — [mouse movement is not bindable](#mouse-is-three-different-element-shapes-and-only-one-of-them-is-a-binding) |
| controller › BUTTONS | enabled | greyed |
| controller › HATS | enabled when the device reports hats | greyed |
| controller › AXIS DIRECTIONS | enabled when the device reports axes | greyed |
| controller › AXES | greyed — a button slot cannot take a whole axis | enabled when the device reports axes; the default page |
| a controller with nothing enabled | the whole tab greyed | the whole tab greyed — for instance a device with no axes |

**Greyed, not hidden.** Every tab stays in view with a tooltip saying why it is unavailable. A commander
looking for their button box on an axis row learns *"this device reports no axes"* instead of wondering where
it went.

**The input pressed decides the tab.** Pressing a real key moves to KEYBOARD; pressing a controller input
moves to that device's tab and kind. Clicking an input captures it too, for anyone working from the list
rather than the hardware.

**The page has a fixed height and scrolls.** Splitting by kind does not shorten the largest page much — 79
of the throttle's 82 inputs are buttons — so the dialog keeps a constant size that fits the app window, and
the input list scrolls inside it.

#### What the dialog shows

The **title is the in-game name**, and the first rows say where it lives and what the file calls it:

```
GALAXY CAM SET Y-AXIS TO Z-AXIS              BUTTON / KEY  ×
ASSIGNMENT
CONTROL          General › Galaxy Map
XML BINDING      CamTranslateZHold
SLOT             Secondary
CURRENT VALUE    Not defined
NEW INPUT        [ NOT DEFINED ]
                 [ CLEAR BINDING ]
```

| Row | Source | Button slot | Axis slot |
|---|---|---|---|
| Title | `BindingDisplayNames.lookup(bindingId).name()` | yes | yes |
| CONTROL | the same record's section and group | yes | yes |
| XML BINDING | the raw `bindingId` | yes | yes |
| SLOT | Primary or Secondary | yes | *Axis (one binding)* — an axis element has one `<Binding>` |
| CURRENT VALUE | the slot as it stands in the draft | yes | yes |
| NEW INPUT | the capture field | yes | yes |
| INVERTED, DEADZONE | the axis row's settings | — | yes |

**Why both names.** The in-game name is what the commander recognises, and it is ambiguous on its own — the
game has four different rows called *Move Forward*, which is why CONTROL carries the section and group. The XML
tag is what the file, a support bundle and another commander's `.binds` all say. Today the dialog's
**SELECTED BINDING** row shows only the tag, so the first thing a commander reads is `CamTranslateZHold`.

#### Nothing is kept until SAVE

The commander presses the input, reads whatever the dialog says about it — in use, reserved, the game-menu
key, a conflict — and then presses **SAVE**. **SAVE writes to the draft**; Apply is still the only thing that
reaches the game. **CLEAR BINDING** empties the slot, and **BACK** or Esc leaves with nothing changed.

This is Krondor's flow as it ships, and it **replaces the earlier commit-on-press design**, where a clean
capture saved itself instantly. A warning about a reserved chord or the game-menu key is only useful if it can
be read before the binding lands.

#### MOUSE is chosen, not captured

The MOUSE tab lists mouse inputs to click: the buttons, as `Mouse_1` upward, and the wheel as the half-axes
`Pos_Mouse_ZAxis` (up) and `Neg_Mouse_ZAxis` (down). It never listens for mouse movement or clicks — see
[Mouse inputs are chosen, not captured](#mouse-inputs-are-chosen-not-captured) for why a stray scroll must not
bind anything. The button count is whatever the system reports, never a hardcoded four.

#### What counts as an axis — what the device reports, not what it looks like

A dial is not necessarily an axis. **Both kinds sit on the same VIRPIL panel**, in Frontier's own
[`VPCPanel.buttonMap`](reference-data/VPCPanel.buttonMap) and [`VPCThrottle.buttonMap`](reference-data/VPCThrottle.buttonMap):

| Physical control | Reports as | Frontier's label | Sub-tab |
|---|---|---|---|
| Rotary **encoder** — endless, clicky | **buttons**: press, clockwise, counter-clockwise | `E1`, `E1 - Clockwise`, `E1 - Counter Clockwise` | BUTTONS |
| The throttle's **dials** | **buttons** | `L Dial - Forward`, `R Dial - Backward` | BUTTONS |
| **Axis dial or slider** — with end stops | **an axis** | `Joy_UAxis` = A1, `Joy_VAxis` = A2 | AXES |
| Throttle levers, flaps | **axes** | `Joy_RXAxis` = Throttle L, `Joy_RZAxis` = Flaps | AXES |

So **a button box can have axes**. Elite-Intel already knows the count: `DeviceService` reads
`SDL_GetNumJoystickAxes` into `Device.axisCount`, and publishes live movement as `DeviceAxisEvent`, which is
what *move an axis to capture it* needs. What a control looks like cannot be the test, because vendor software
can remap a physical control to report either way.

#### Axis directions and hats are button inputs

**Axis directions.** Pushing an axis **one way** can fire a button row. The format writes this as `Pos_`/`Neg_`
on a **BUTTON** slot ([format §5.2](domain-knowledge/EliteDangerous-BindsFileFormat.md#52-directional-pseudo-axis-codes)),
and real files use it: `DualVirpilDawnTreader.4.1.binds` puts `Neg_Joy_YAxis` on `UpThrustButton`'s Secondary
slot and `Neg_Joy_RYAxis` on Toggle HUD. The AXIS DIRECTIONS page offers a *+* and a *−* for every axis the
device reports.

**Hats.** A hat is written `Joy_POV1Up`, `Joy_POV1Down`, `Joy_POV1Left`, `Joy_POV1Right`, and a diagonal sets two
of them at once ([format §5.4](domain-knowledge/EliteDangerous-BindsFileFormat.md#54-pov--hat-codes)). Real
files bind them to button rows — `FocusRightPanel` on `Joy_POV1Right`. Some hardware reports its hats as plain
buttons instead (the throttle's `R POV1 Forward` is `Joy_9`), and those appear under BUTTONS.

#### Axis slots take an axis only

**An axis row is never bound to a keyboard key** (Alan, 2026-09-13), and never to the mouse — which is why
both tabs grey out. Keys go on the matching button rows — *Yaw Left* and *Yaw Right* beside *Yaw Axis*. Axis
bindings take no modifiers.

**Inverted and Deadzone live in both places.** They stay inline on the axis row for a quick adjustment, and
they appear in the dialog beside the axis they belong to. It is one value in each case, so saving from either
leaves the other showing the same thing.

#### Capture-control colour, and no skip option

A row's Capture controls are coloured by slot state — empty, bound and clean, and bound and conflicting are
each distinct. There is deliberately **no "always confirm" or skip-warnings option**, even though the base game
has one: a skip option would defeat the purpose of warning at all.

#### Conflict warnings name one binding today

Krondor's wording is *"Conflicts with {0}; may not work"*, and `CandidateConflict` carries a single
`otherBinding`. **The target is every action sharing that exact chord**, which needs that record to hold a list
and needs [FN-1](#fn-1-in-detail--scoped-2026-09-12) fixed first — otherwise a binding in the discarded slot
cannot be named at all.

#### What the dialog cannot do yet — gaps in the code, not in the design

| Gap | Where | Closes when |
|---|---|---|
| **A HOTAS slot never opens the dialog.** It shows *"This slot uses an advanced or unsupported binding. The basic editor will not modify it."* | `BindingProfilePanel.openAssignKeyboardBindingDialog` via `isBasicEditableSlot` | [`BindingsWriter` widens](overview.md#two-narrow-boundaries--one-stays-one-widens) |
| **The dialog says keyboard throughout** — *Assign keyboard key*, *Keyboard Assignment*, *New key* | `bindings.assign.*` in the nine `gui*.properties` bundles | the source tabs arrive; the text changes, the class name does not |
| **Hats cannot be captured.** The device layer has no hat handling at all | `elite.intel.devices` | hat events are read and mapped to `Joy_POV` tokens |
| **U and V axes cannot be captured.** `AXIS_TOKENS` is exactly X, Y, Z, RX, RY, RZ, so a panel's A1/A2 dials make `axisToBindsToken` throw | `ButtonInputMapper` | the token list grows to `Joy_UAxis` and `Joy_VAxis` |
| **SELECTED BINDING shows the raw tag** | `AssignKeyboardBindingDialog.buildUi` | the title and CONTROL row land |

### Open items — capture dialog

- *Settled 2026-09-13:* every controller gets a name and a `.buttonMap` through
  [onboarding](alias-designer.md#onboarding--every-controller-gets-a-name-and-a-buttonmap--settled-2026-09-13) — a name only, labels
  generated, skipping takes the default — and a connected controller that already has a name gets a
  generated `.buttonMap` at Elite-Intel startup.
- **Colour collision: the capture button's "empty" state versus the existing "recommendation"
  indicator.** Both land on a similar light blue. **Explicitly parked by Alan — do not resolve without
  raising it directly**, since it touches an existing convention rather than only BindForge's own.
- *Resolved:* how a slot opens the dialog — its [Capture button](#capture-is-the-only-way-in), to prevent
  accidental clicks. Where mouse input is chosen — the [MOUSE tab](#mouse-is-chosen-not-captured). Whether
  the other slot's Capture control stays live while one is edited — moot, because the dialog is modal.

### Modifiers, Hold, Inverted, Deadzone

All of the following are confirmed by direct in-game testing, not just inferred from the file format:

- Capturing a compound combination (main input plus modifiers) is a single motion — whatever is held when the completing input arrives becomes the modifier set, and the completing input becomes the slot's value. No separate "now capture the modifier" step exists.
- Cross-device modifiers are real and confirmed working in-game — for example, a keyboard key combined with two separate joystick buttons in one binding.
- **A bind slot can combine a maximum of four inputs total** — one main input plus up to three modifiers.
- **Axis bindings can never take a modifier at all**, confirmed by direct testing — attempting to add a modifier to an axis binding silently drops it and produces a plain single-input rebind instead. The only two things that vary on an axis slot are Inverted and Deadzone.
- **Hold** stays an inline toggle on a button slot. **Inverted** and **Deadzone** are inline on the axis row and
  also in the [capture dialog](#axis-slots-take-an-axis-only) when an axis slot is captured (settled 2026-09-13).
- **An axis row takes an axis only — never a keyboard key** (Alan, 2026-09-13).
- Axis rows have a different column shape than button rows: one value slot plus Capture, Inverted, and Deadzone controls, rather than a Primary/Secondary pair.

## Action Groups

*(Named "Purpose Mode" in earlier design generations — same underlying concept, renamed.)*

**Status: in scope for V1.2** (see [v1.2-scope.md](../../00-overview/v1.2-scope.md)). Groups bindings by **player intent** rather than game category — for example, "Move Left" spans Ship, SRV, On Foot, and General Controls simultaneously; Game Mode shows these in four separate sections, Action Groups shows them together in one group.

- **Action Groups** — each binding belongs to at most one action group. (A binding can't logically serve two physical intents at once — if it did, both groups would require the same key, which is itself a conflict.)
- **Default Groups** — ship with BindForge, read-only, no rename/edit/delete. The exact default set is intentionally left undefined until BindForge's binds-file audit is complete.
- **User Groups** — fully editable and deletable, stored persistently.
- **Ungrouped bindings are hidden in Action Groups** (they remain visible in Game Mode) — a deliberate curated-view choice, not a bug.

**View Groups tab:** a left panel lists groups (default groups with a lock indicator first, then user groups); a right panel shows only that group's bindings with its own real-time search. Clicking a row opens the same Binding Editor panel Game Mode uses.

**Manage Groups tab:** a three-column layout — a source list of all bindings, add/remove controls, and a target list of the selected group's members — plus a group list. A fourth control, "Assign Control to Group," applies one capture to every member binding in a group at once.

**Assign Control to Group — confirmation semantics (resolved):** the capture is attempted against every member binding in the group. For any member where applying it would either overwrite an existing, different value, or create a new conflict with a binding *outside* the group, that member gets its own row in one consolidated confirmation dialog — modeled on the base game's own "already bound" warning, but covering every affected member at once rather than one dialog per binding. Each row shows whatever is actually at stake for that specific member (the existing value being overwritten, the new conflict it would create, or both together when both apply) with its own toggle so the player can confirm or skip that member individually — applying to the rest of the group is never all-or-nothing because one member happens to be contested. Members with no existing value and no new conflict apply immediately, with nothing to confirm.

**Conflict prevention:** adding a binding already in another group is blocked, with an explicit warning naming both groups.

**Conflict Filtering:** conflicts here are a group-level concern, not a per-row one the way Game Mode shows them — a single binding inside a group either has a real conflict or it doesn't, but what a player scanning the View Groups list actually wants to know is *which groups contain any conflict with something outside them*. When the shell's [Show Conflicts Only toggle](#shell-common-to-all-modes) is active in Action Groups, the group list filters down to only groups with at least one member binding conflicting with a binding in a *different* group — group-internal duplicates aren't possible in the first place, since a binding belongs to at most one group.

**Persistence — resolved by direct testing:** an early design considered marking group membership using comments inside the `.binds` XML file itself. Testing confirmed the game silently strips every XML comment on load/rewrite (while every actual binding value survives intact) — a well-behaved normalize, not a lossy one, but one that rules out this storage approach. **Consequence: group-membership metadata must live in BindForge's own data store, never in `.binds` file comments.**

**Prerequisites:** the binds-file audit complete; Game Mode stable; a persistence design for user groups.

### Open items — carried from the punch list, 2026-09-09

Design questions this section raises and does not answer. Preserved when the punch lists were retired;
all five are Action Groups' own, and none blocks building the mode.

- **"Assign one control to every group member at once" — behaviour undesigned.** The layout is confirmed;
  what the button *does* is not. Overwrite every member's binding, or fill only empty slots? And does it
  validate that the result creates no new conflicts?
- **Ungrouped actions after a game update.** Frontier ships new content, new bindable actions appear, and
  they land ungrouped — where [ungrouped bindings are hidden](#action-groups). A commander could gain
  controls they never see here. Needs a notification design, not just a rule.
- **Can default groups update independently of an app release?** It would let group definitions follow the
  game's content without shipping a whole new Elite-Intel. Untouched.
- **Completely unbound bindings — shown in their group, or hidden?** Distinct from *ungrouped*: this is a
  binding that belongs to a group but has neither Primary nor Secondary assigned.
- **Storage schema for user groups.** Falls out during implementation, alongside the assign-to-all
  semantics above. A new table means a new `011XX` migration, and an applied migration is never edited.

## Input Mode

**Status: in scope for V1.2, and now designed.** Input Mode is an input-first reverse lookup — press a physical control, and see everything bound to it across every section — the mirror image of Game Mode's action-first view.

**Layout:** two columns. The left column, "Input Detected," is a live readout that lights up with the device and control name (e.g., "RHVCAP — Joy_4 (Button)") the moment the player presses a button or moves an axis. The right column lists every existing binding that uses that exact input, one row per binding, tagged with its section (Ship/SRV/General/On Foot) and action name. If nothing is currently bound to the detected input, the list says so plainly rather than showing an empty grid.

**Rows render exactly like Game Mode's grid, not a simplified list** — the same Primary/Secondary slot columns (with Hold/Tap and modifier chords shown), the same axis column shape (value, Inverted, Deadzone), and the same conflict red-highlighting. A player scanning what's bound to a physical input needs the same information density they'd get browsing Game Mode directly; a plain text list would be a step down.

**Editing from here:** clicking a row opens the same [capture dialog](#capture-dialog--settled-2026-09-13) used everywhere else in BindForge, right in place — the player never has to leave Input Mode and flip to Game Mode to rebind or clear something they found this way.

This is distinct from [Control Types](#control-types) below, which groups by input *category* (all axes together, all buttons together) rather than looking up one *specific* physical input. Input Mode absorbed what was judged the one genuinely useful idea from Control Types' original concept when Control Types itself was parked.

## Anomalies

*(Named "Conflicts" until 2026-09-07. Renamed because the mode outgrew the word — see
[Four kinds, one question](#four-kinds-one-question) below. "Conflict" remains exactly right for the kind
of anomaly it originally named, and for the detection service.)*

### Four kinds, one question

**Settled 2026-09-07.** Four different causes produce the same experience — the commander presses
something and it does not do what they expected. They belong in one place because the question is the
same in every case: *why didn't that work?*

| Kind | What it is | Cause |
|---|---|---|
| **Reserved** | the key can never work, whatever is bound | claimed by the OS, or by Elite's own Game Menu control |
| **Missing** | a control EliteIntel drives that it cannot press — **two shapes**, see below | absence, not breakage |
| **Conflicts** | two actions sharing one input | the original category |
| **Invalid** | bound, unique, uncontested, and still will not fire | something else consumes the input first in a given game state |

#### Missing has two shapes — added 2026-09-12

**Elite-Intel can only simulate keyboard input.** Krondor, 2026-09-12: *"the app can't use / simulate
any input except keyboard."* So from the assistant's side there are two ways a control it drives can
be undriveable, and they look completely different to the commander:

| Shape | The file says | The commander sees | Remedy |
|---|---|---|---|
| **Nothing bound** | no assignment in either slot | an empty row | bind it |
| **Bound, but not to a keyboard** | a HOTAS, joystick, gamepad or mouse assignment | **a perfectly normal binding** | add a keyboard binding in the free slot |

The second is the dangerous one, because **nothing looks wrong.** The commander bound it, the game
honours it, their hardware works — and the assistant silently cannot use that control. Elite-Intel
already detects exactly this (`KeyBindingsParser.isBoundToNonKeyboardDeviceOnly`) and its only output
today is a line in the log, which no commander reads.

**This must not become noise for HOTAS commanders.** It applies *only to controls Elite-Intel
actually drives*, exactly as the first shape already does. A commander who flies entirely on a stick
has hundreds of non-keyboard bindings and almost none of them matter here; flagging them all would
bury the handful that stop the assistant working. The existing detector makes the same distinction
for the same reason — its `// WHY:` records an earlier version that warned on 345 of 352 actions and
was worse than silent.

**The remedy is additive, which is why this is comfortable.** `.binds` gives every control two slots.
A control held on a HOTAS in Primary can take a keyboard binding in Secondary without the commander
losing anything — both fire. So the fix BindForge offers is *"add a keyboard binding here so the
assistant can use it too"*, never *"replace your stick binding"*. See
[FN-1](#known-scanner-defects--all-still-open), which currently makes the scanner blind to the very
Secondary slot this remedy writes to.

**Why not "Conflicts", "Warnings", "Errors" or "Problems".** *Conflicts* was the original and only
describes one of the four. *Warnings* understates — a reserved binding will never fire and a blocking
conflict stops the assistant working, and calling those advisory teaches commanders to ignore the tab.
*Errors* overstates at the other end — a plain overlap is legal and often deliberate, and labelling a
working setup broken earns the same shrug by the opposite route. The set genuinely spans "can never work"
to "probably fine, just so you know", so **any single-severity word is wrong at one end.** *Anomaly* is
severity-neutral by construction — it says *look at this* and lets the row say how much to care — and it
is already Elite's own vocabulary. *Problems* was accurate but reads like an IDE rather than a cockpit.

### Structure

A second tab row inside the mode: **All / Reserved / Missing / Conflicts / Invalid**, each carrying its
own count. **All** is the default.

**Severity is banding and sort order, never a third tab level.** Blocking, curated and plain-overlap are
properties of a row, shown as a chip on its group header. Putting them behind tabs would bury the blocking
ones behind a click, which is the precise failure the severity model exists to prevent — and it would put
a binding four levels deep.

**All is ordered by certainty of failure, not by kind:**

| | Rank | Why |
|---|---|---|
| Reserved | 1 | bound, and can never fire |
| Conflict — blocking | 2 | stops EliteIntel driving the game |
| Invalid | 3 | fails only in a particular state |
| Missing | 4 | nothing bound |
| Conflict — curated | 5 | works, with a known side effect |
| Conflict — plain overlap | 6 | legal, possibly deliberate |

**Bound-but-broken ranks above not-bound**, because the commander believes those already work. An unbound
control at least fails honestly.

### Intentionally Unbound — a state, not an anomaly

**Settled 2026-09-08.** Some controls are **meant** to have nothing on them. They are not Missing, they are
finished, and BindForge must not report them as a problem or offer to fix them.

The state has two sources.

**Shipped defaults — agreed between Alan and Krondor, and already enforced in Elite-Intel:**

| Control | Why | Kind of argument |
|---|---|---|
| `Pause` (Game Menu) | `Esc` opens that menu whether or not it is bound, so a key there buys a second route to a screen already reachable and takes a key off the board for everything else | **economy** |
| `EjectAllCargo`, `EjectAllCargo_Buggy` | the hold empties into space; it cannot be undone and cannot be done by halves. Neither the commander's finger nor a misheard phrase to the assistant should be able to trigger it | **safety** |

**The two reasons are different and both should survive.** Game Menu is about a key being wasted; cargo
ejection is about what the control does when it fires. Collapsing them into one "skip list" loses the
argument — and the argument is what tells a future maintainer whether some third control belongs here.
Elite-Intel keeps them apart too, as `GAME_MENU_LEFT_UNBOUND` and `LEFT_UNBOUND_ON_PURPOSE`.

**Player-marked — the same state, chosen rather than shipped.** A commander must be able to mark a control
*leave this empty* so it stops being reported. Confirmed needed 2026-07-12; storage resolved to the database
on 2026-07-17, because `.binds` does not preserve comments and there is nowhere in the file to put a marker.

#### The mechanism: an ASSISTANT column in the grid — settled 2026-09-12

The mark lives in the editing grid rather than behind a context menu or a dialog, and it is **one state of
a column rather than a feature of its own.** The column answers a single question about each control:
**can Elite-Intel drive this?**

```
CONTROL                PRIMARY     SECONDARY   ASSISTANT
Yaw Left               A           —
Move Right             Joy_4       —           CAN'T PRESS
Roll Left              —           —           NOT BOUND
Toggle HUD             —           —           ON PURPOSE
```

| State | Meaning |
|---|---|
| *(blank)* | **The common case.** Either Elite-Intel can drive the control, or it never needs to. |
| **NOT BOUND** | Elite-Intel drives this control and nothing is assigned — [Missing, first shape](#missing-has-two-shapes--added-2026-09-12). |
| **CAN'T PRESS** | Assigned, but only to a device Elite-Intel cannot send input to — [Missing, second shape](#missing-has-two-shapes--added-2026-09-12). |
| **ON PURPOSE** | The commander has said stop reporting this. |

**Why a column rather than a menu item.** A context menu hides the state as well as the action: nothing in
the grid would distinguish *empty* from *empty on purpose*, which is the whole distinction being drawn. A
column shows it on every row at a glance, and the same width then carries the two Missing shapes, which
otherwise needed somewhere of their own. **One column, three things that all answer the same question.**

**The mark is per control, not per slot.** *"This control should have nothing on it"* is what the two
shipped defaults actually say — `EjectAllCargo` is unbound entirely, not unbound in Primary — and it is
what Missing reports, which is action-level. A per-slot mark would also have no clear meaning when the
other slot is bound. This is the one place BindForge does **not** use the slot as its grain; everywhere
else — merging, conflicts, capture — [the slot is the unit](overview.md#merge-grain-the-slot-not-the-action--settled-2026-09-07).

**ON PURPOSE silences both shapes, and that is deliberate.** A commander who flies on a stick and does not
want Elite-Intel touching their landing gear marks it once. Whether the slot is empty or holds `Joy_12`,
their intent is the same — *stop asking* — so it is one mark and one database table rather than two states
a commander would have to tell apart. **Marking never changes a binding:** a control marked while bound to
a stick stays bound to the stick, and the game goes on using it. Only the reporting stops.

**Reversible, and reversible the same way it was set.** Clearing the mark returns the control to whichever
state it would otherwise have had — blank, NOT BOUND, or CAN'T PRESS. The mark is a row in the database
([storage settled 2026-07-17](#intentionally-unbound--a-state-not-an-anomaly)), so clearing it is a delete
and nothing in `.binds` is touched in either direction.

**Hovering a mark gives the reason**, in the commander's terms rather than the file's: *"Bound to Joy_4.
Elite-Intel can only send keyboard input, so it cannot use this control. Add a keyboard binding in the free
slot and both will work."* That sentence is the remedy as well as the explanation — the
[additive fix](#missing-has-two-shapes--added-2026-09-12), never a replacement.

**In the mockup, measured rather than assumed.** The column is 104px and the row is flex, so the width
comes out of the control name. At the app's 1184px client width the name column still gets 462px and no
name clips — including the longest in the set. It only crowds below roughly 900px, which is narrower than
the window ships at. The chips reuse the existing `an-sev` severity classes rather than introducing a
parallel set, so the amber of a Missing row and the amber of this column are the same amber by
construction.

#### What the state changes

| | Behaviour |
|---|---|
| **Anomalies — Missing** | never listed. It is not missing; it is done. |
| **Any automatic assignment** | never offered a key, and **the skip is reported with its reason** rather than passed over silently |
| **Manual binding** | **still allowed** |

**"The assistant will not bind it" is not "you may not bind it."** A commander who deliberately puts a key on
cargo ejection has, by that act, decided they want it — which is the whole distinction. BindForge warns on
that path and does not block it. Blocking would be BindForge overruling the person whose bindings these are.

**Reporting the skip matters more than it looks.** A commander who runs an auto-fix and still sees a control
listed as unassigned will assume the fix missed one. Saying *why* it was skipped is the difference between a
deliberate design and an apparent bug.

**A note on how this currently holds.** Neither control carries the `DRIVEN` flag on `Bindings.GameCommand`,
so today they fall outside the Missing scan by side effect rather than by intent. That is the right outcome
reached by the wrong route: it would evaporate the moment either flag changed, or the moment BindForge
widened Missing beyond the controls Elite-Intel drives. The state should be explicit.

### Open items — carried from the punch list, 2026-09-09

- **Narrow versus broad missing.** Missing is scoped to the controls Elite-Intel drives — roughly 80
  actions — while a commander's file typically has around 129 controls simply unbound. Both numbers are
  true and they answer different questions. Whether the broad count is shown at all, and how it is kept
  visually subordinate to the narrow one, is undecided.
- **Which voice command depends on this binding?** Selecting a missing control ought to name the
  command(s) that stop working without it — far more useful than the action's own name. No design, and it
  reaches into territory `BindingsMonitor` deliberately avoids: it does not consult the custom-command
  registry, precisely so `ai.hands` never points back at `ai.brain`. Any design here has to respect that
  boundary or move the lookup somewhere that legitimately sees both.

### Conflicts (the kind)

**Status: in scope for V1.2, and designed.** Surfaces the [Shared Conflict Detection](#shared-conflict-detection) service's output directly, rather than requiring the player to spot conflict coloring row-by-row inside Game Mode. The mode tab itself carries a live badge showing the current conflict count.

**Layout:** conflicts are grouped by the shared input causing them — each group header names the shared key/chord and how many binds share it, expandable/collapsible the same way Game Mode's grid groups work. Inside a group, each row is tagged with its section plus the action name, exactly like Input Mode's list.

**Editing from here:** clicking a row opens the same [capture dialog](#capture-dialog--settled-2026-09-13) used everywhere else, right in place — resolving a conflict never requires leaving the Conflicts tab and jumping to Game Mode.

## Mouse Inputs Are Chosen, Not Captured

**Settled 2026-09-07.** Keyboard and controller inputs are captured by pressing them. **Mouse inputs are
picked from a list instead**, and the game itself sets that precedent — Elite's own Mouse Controls screen
assigns mouse behaviour through dropdowns and sliders, not by asking the commander to waggle anything.

### The reason capture does not work here

A capture dialog can safely swallow every keystroke, because keyboard is not how the dialog is operated.
It cannot safely swallow every **click**, because clicking is how a commander would cancel. Elite resolves
this by making its whole popup a capture surface with `Esc` as the only exit; BindForge does not need to,
because the mouse token space is tiny and completely fixed:

```
Mouse_1  Mouse_2  Mouse_3  Mouse_4  …
Neg_Mouse_ZAxis   Pos_Mouse_ZAxis      wheel down / up, as half-axes
```

There is nothing to discover by capturing, and one thing to lose: **a mouse scrolls by accident in a way
it does not click by accident**, and the capture window is precisely when a hand is resting on it. A stray
scroll would bind something nobody chose.

**Do not cap the button count.** Accept whatever the file already contains and offer whatever the system
reports, rather than hardcoding `Mouse_1..4`. Four is what one commander's file happens to use, not a
limit.

### Mouse is three different element shapes, and only one of them is a binding

Confirmed against a real `.binds` and the in-game Mouse Controls screen, 2026-09-07:

| In the game's UI | In the file | Shape | Where it belongs in BindForge |
|---|---|---|---|
| Mouse buttons on an action | `Device="Mouse" Key="Mouse_1"` | a slot on a BUTTON element | the bind grid — chosen from the list above |
| **Mouse X-Axis → Yaw** | `<MouseXMode Value="Bindings_MouseYaw" />` | STANDALONE SETTING, **enumerated** | a dropdown, not a bind slot |
| Mouse Sensitivity, Deadzone, Power Curve | `<MouseSensitivity Value="1.527" />` | STANDALONE SETTING, float | a slider, not a bind slot |
| Reset Mouse, Disable Relative Mouse | `<MouseReset>` with `<Primary>`/`<Secondary>` | ordinary BUTTON element | the bind grid, bindable to *anything* |

**Mouse movement is not bindable.** There is no `Mouse_XAxis` key token — steering is configured by
choosing what the axis *does* (`Bindings_MouseYaw`, `Bindings_MousePitch`) from an enumerated setting. So
BindForge needs that value list, and it is a list rather than free text.

The last row is worth noticing: **`MouseReset` is a normal control that happens to be about the mouse.**
It can be bound to a HOTAS button like anything else. "Mouse" in the game's UI groups by *subject*, not by
*input device*, and BindForge should not confuse the two.

## Control Types

*(Named "Type Mode" in earlier design generations — same underlying concept, renamed.)*

**Status: parked, confirmed out of v1 scope** (see [v1.2-scope.md](../../00-overview/v1.2-scope.md)) — the least developed mode, with foundational open questions still unanswered. A planned mode grouping bindings by **input type**.

**Primary intended grouping: axis vs. button** — Control Types exists mainly to split bind slots by whether they're axis-type or button-type, mirroring the same fundamental distinction the [binding schema](../../03-data-models/binding-schema.md#binding-element--three-distinct-kinds) already uses at the data-model level. A finer breakdown within that (Keyboard, Joystick Axis, Joystick Button, POV/Hat, Mouse, Unbound) was also considered, but the core motivation is the axis/button split specifically.

### Open items — carried from the punch list, 2026-09-09

Both are parked with the mode itself, recorded so picking it up does not start from nothing:

- **A visual controller diagram** — a picture of the physical device with bound actions labelled beside
  each control, rather than a list.
- **Keyboard modifier plus joystick button in one binding** — confirmed real and working in-game; how a
  type-oriented view should present it is undesigned.

## Controller Mode

**Status: back-burner concept, confirmed out of v1 scope** (see [v1.2-scope.md](../../00-overview/v1.2-scope.md)). Rather than a list of bindings, this mode shows a picture of the player's actual controller with each physical button/axis numbered; each numbered callout would be a searchable dropdown to assign a bind slot to that specific physical control, directly on the image. Licensing and per-device diagram sourcing (where the reference images for a given controller model come from) are both unresolved — see BindForge_Spec.md's roadmap.

Motivation: help players verify axis assignments make physical sense across a complex multi-device HOTAS setup — for example, confirming pitch/roll/yaw/thrust map to axes that feel natural, and that no axis is accidentally inverted relative to another.

It may function as a reverse-lookup of either Game Mode (input-first: "I pressed this button — what's bound to it?") or Action Groups (start from an input type, see which action groups use it) — the exact relationship is undetermined.

**Unresolved, blocking questions (never answered in any design generation):**
- Standalone mode, or a filter within another mode?
- Is there real user value here beyond axis review, or would a simpler dedicated "Axis Review" panel serve better?
- How would it handle a binding using both a keyboard modifier and a joystick button?
- Should it show a visual controller diagram?

**Prerequisites:** the binds-file audit complete; Game Mode stable; Action Groups complete or explicitly deferred.

---

## Mockup Completeness

**Carried from the punch list, 2026-09-09.** Not a design question — a known gap between the mockup and
the data that already exists.

The sub-group grid is built out for two demo groups only. The full mapping lives in
[`BindForge_Binding_Zone_Map.xlsx`](reference-data/BindForge_Binding_Zone_Map.xlsx) and in the
[Action Catalog](domain-knowledge/EliteDangerous-ActionCatalog.md). **Build it from those sources, never
from memory** — an earlier pass invented a group that did not exist, which is why this warning is
written down rather than assumed.

## Shared Conflict Detection

Conflict-detection data and logic are shared across every Bind Editor mode — Game Mode, Action Groups, Conflicts, and Input Mode all read from the same underlying conflict state, rather than each mode running its own check. Each mode only differs in how it *displays* that shared state (see [Game Mode — Conflict Display](#conflict-display) and [Architecture Status](#architecture-status) below).

### How the Game Actually Resolves Conflicts (confirmed by direct in-game testing)

- **The modifier-vs-main-key label is not semantically meaningful to the game.** The game does not track which physical key is "really" a modifier — it tracks press order: whatever is held down when the last input arrives becomes the recorded modifier set, and the last-pressed input becomes the slot's recorded main value, regardless of which key is conceptually the "real" action key. **Practical consequence for capture:** hold intended modifiers first, press the intended main key last, to get the expected result.
- Modifier order does not affect the game's own conflict recognition — two captures of the same key set in different orderings are correctly recognized as the same combination.
- A binding matches by its exact chord (main key plus exactly its modifier set). Holding extra modifiers does not trigger a binding with fewer of them, and a bare key and a modified chord on the same key are two fully independent bindings that both fire — they do not suppress each other.
- Elite's own conflict-checking is genuinely context-aware, operating at two levels:
  - **Macro level (top-level sections):** the four sections (General/Ship/SRV/On Foot) are *assumed* isolated from each other for conflict purposes, but this is **not fully confirmed** — there is at least one unconfirmed observation of an apparent cross-section conflict between Ship and SRV during testing. If real, a cross-section conflict matrix would be needed in addition to the four per-section matrices that exist today. This is the single most important open item in BindForge's conflict-detection design — see [conflicts-and-open-questions.md](../../00-overview/conflicts-and-open-questions.md).
  - **Micro level (within a section):** conflict relationships between subgroups are **not predictable from category alone** and had to be tested pair by pair. Two confirmed examples of the game's own conflict-checking being wrong relative to real gameplay exclusivity: Multi-Crew conflicts with nearly the entire main Ship cluster even though a player can never simultaneously be in their own cockpit and a crewmate's seat — a confirmed Frontier bug that BindForge's own scanner should **not** mirror; conversely, SRV's Driving Turret Controls subgroup is correctly isolated (only conflicts with Driving Mode Switches), matching the same real-world exclusivity logic correctly. On Foot Controls, among the subgroups tested so far, is fully connected — every tested subgroup conflicts with every other one.
- **A real conflict, for BindForge's own scanner, means:** the same key/chord **and** both actions sit in subgroups the tested conflict matrices mark as conflicting (or, for an untested pair, treated conservatively as a conflict until tested). This is deliberately neither "same key anywhere in the file" (too broad — would flag Multi-Crew-style false positives project-wide) nor "same key within the same top-level section" (too coarse — would miss subgroups that are correctly exempt from their section's main cluster).
- **The base game's own rebind dialog is a confirmed-incomplete source of truth:** when a key is shared by three actions, the dialog names only the most recently bound one, never the others — direct proof the game's own conflict UI only reports the most recent collision, not every existing one. This justifies BindForge's own scanner listing every conflict, once the scanner itself is proven correct (see below).
- **A collision between one action's Secondary slot and another action's Primary slot is a real, confirmed conflict** — any scanner comparing only each action's single "winning" slot will systematically miss these.
- **The game does not consider a hold-bound action and a tap-bound action sharing a key to be conflicting at all.** Hold/Tap identity must be folded into whatever defines "the same combination" for conflict-comparison purposes — an earlier scanner that ignored this produced a confirmed false positive.
- **FSS-mode-entry nuance:** the action that enters FSS scanning mode genuinely conflicts with other ship actions sharing its key — a blanket rule that suppresses every scan-related action as "always safe" is wrong for at least this one action, even though the FSS subgroup as a whole is correctly isolated from the rest of Ship Controls at the macro level. The nuance is that subgroup-level isolation does not guarantee every individual action within it is exempt from every possible collision.
- **UI-action-vs-ship-action assignment is allowed by the game with no warning, but this only answers "can this be assigned," not "is it behaviorally safe when both are live simultaneously."** This distinction was tested but not cleanly resolved — a second, behavior-focused round of testing (rather than assignment-focused) is still needed. See [conflicts-and-open-questions.md](../../00-overview/conflicts-and-open-questions.md).

See [BindForge's conflict-testing domain knowledge](domain-knowledge/EliteDangerous-ConflictRules.md) and the [interactive conflict-matrix test pages](domain-knowledge/ConflictMatrix-General.html) for the full, per-subgroup empirical results this section summarizes.

### A second category this design does not yet model

Everything above is **pairwise** — two actions competing for one input. There is a second failure mode that no
pairwise check can detect: **a single binding that is unique, uncontested, and still does not work.**

The confirmed case is UI navigation bound to a key that types a character. With the galaxy map search box
focused, Elite swallows any printable keystroke as text and never consults the `UI_*` bindings, so `UI_Down` on
`S` — or `Shift+S` — silently fails, while `Ctrl+S` works. See
[Contextually Invalid Bindings](domain-knowledge/EliteDangerous-ConflictRules.md#3b-a-second-category-contextually-invalid-bindings).

**As specified today, BindForge would show such a binding as perfectly healthy.** The four conflict matrices,
the shared conflict-detection service and the Conflicts *kind* are all built on pairs, and this has nothing to
pair with.

Elite-Intel already detects the one confirmed case: `BindingsMonitor.textTrappedUiNavigation()` is a pure read
over the parsed bindings, backed by the side-effect-free `UiNavigationTextTrap`. **Both are directly reusable**,
and doing so would be cheaper and more honest than BindForge growing a second implementation of the same check.

**Settled 2026-09-07: it is the `Invalid` kind under [Anomalies](#anomalies).** The presentation question
that was deferred here dissolved rather than being answered — it only existed because the mode was called
Conflicts, and this is not a conflict. Widening the container removed the need to choose between forcing it
under a wrong label and inventing a second home for it.

### A third category: reserved keys — and this one BindForge can actively cause

**Added 2026-09-06**, from `elite.intel.ai.hands.ReservedKeyChords`, which landed upstream on 2026-09-05.
Full rules in
[Reserved Keys](domain-knowledge/EliteDangerous-ConflictRules.md#3c-a-third-category-reserved-keys).

A reserved key **cannot be used for anything, whatever else is or is not bound to it.** No competing
action, no game state — the key is off the board. Two sources:

- **The operating system.** `Alt+F4` closes the game window; on Linux `Ctrl+Alt+F1..F12` drops the
  commander to a TTY. Fixed, matched on the whole key-set.
- **Elite's own game-menu key.** Whatever the commander has on `Pause` is unusable for every other
  control — **matched on the key alone, ignoring modifiers.** With the menu on `P`, then `P`, `Shift+P`
  and `Alt+P` all pause the game and open the options screen. Read from the commander's file, both slots,
  so it is a parameter rather than a constant.

**This is the category BindForge is most likely to inflict on someone**, and it is the reason to treat it
ahead of the other two. The first two are conditions BindForge *finds*; this one it can *create*. Input
Mode capturing a chord that ends in the game-menu key produces a binding that will never fire, and the
commander has nothing to look at to work out why — there is no second action to blame. Elite's own
controls screen has no such rule, so a file written there can already contain one.

Three obligations, all satisfiable with existing code rather than new detection:

| Where | Obligation | Existing call |
|---|---|---|
| **Input Mode** | never accept a reserved chord; say which of the two rules refused it | `isReserved(mainKey, modifiers, gameMenuKeys)` |
| **Anomalies — Reserved** | report reserved bindings already in the file, **each with its own remedy** | `ReservedKeyChords.scan(bindings)` |
| **Keyboard map** | colour reserved keys distinctly, with no modifiers held | `isOsReserved` plus `gameMenuKeysFromSlots` |

**Updated 2026-09-08 — the remedy is per row, and the two are not interchangeable.** `ReservedBinding` now
carries a `Rule` alongside its reason — `ReservedBinding(action, chord, reason, rule)` — because only the
scan knows which rule claimed a chord:

| Rule | The fix |
|---|---|
| `GAME_MENU` | **clear the Game Menu binding** — `Esc` opens that menu anyway, and this one change clears every reserved binding on that key at once |
| `OS_CLAIMED` | **move the control to another key** — nothing can free `Alt+F4` |

A single generic *"change it in the game's controls"* was actively wrong for the game-menu case: it points at
rebinding every control on that key, which is several changes and leaves the key spent regardless. **So
Anomalies — Reserved shows the per-row remedy, not a shared footnote.**

**And BindForge should leave `Pause` unbound rather than filling it.** `Esc` opens that menu whether or
not `Pause` is bound, so a key there buys a second route into a screen already reachable and costs a key
everywhere else. Elite-Intel's auto-assigner already does exactly this — skips the control *and* pulls
its key from the pool — and reports the skip rather than passing over it silently, so a commander who
runs it and still sees the control listed as missing is told why.

### Conflicts have severity, and Anomalies shows it

The current design treats a conflict as present or absent. Elite-Intel sorts them into three tiers — see
[§3d](domain-knowledge/EliteDangerous-ConflictRules.md#3d-conflicts-have-severity-not-just-existence):

| Tier | Meaning |
|---|---|
| **Blocking** | stops the assistant driving the game at all — e.g. `UI_Select` sharing a key with quick comms |
| **Curated** | a known pair with a consequence worth spelling out — *"deploying hardpoints will also toggle landing gear"* |
| **Plain overlap** | two actions, one chord, no known consequence |

**A flat list treats all three the same, and the evidence says that fails.** One commander reassigning
controls generated fifty-one plain-overlap warnings in a single burst, which buried the handful that
mattered. That is the same failure the [conflict matrices](domain-knowledge/EliteDangerous-ConflictMatrix.md)
exist to prevent in the other direction — crying wolf until the signal is ignored.

So the Conflicts kind needs a rank, not just a count: blocking first and always visible, curated with their
own wording, plain overlaps grouped. **Reserved keys sort above all three**, since a reserved binding
cannot work at all while a conflicted one at least sometimes does.

### Architecture Status

**Resolved.** One consolidated conflict-detection service feeds every mode's UI, rather than each mode running its own check.

**What publishes conflict state:** the service recomputes the *entire* current conflict list from scratch, in memory, every time the working copy's bindings change (a capture, a clear, or a fresh file load) — nothing is persisted to disk or patched incrementally. This is a deliberately simple choice, not a performance compromise: the whole binding set tops out at 482 actions (see [`BindForge_ConsolidatedActionTable.xlsx`](reference-data/BindForge_ConsolidatedActionTable.xlsx)), small enough that a full recompute is trivial work, so there's no real benefit to the added complexity of incremental updates. The computation itself is exactly the "real conflict" definition already established above: two actions collide only when they share the same key/chord *and* both sit in subgroups the tested conflict matrices mark as able to collide — never "same key anywhere in the file."

**What subscribes to it:** every mode that displays conflict status — Game Mode, Action Groups, Conflicts, Input Mode — reads from this one shared, freshly-recomputed result rather than maintaining its own.

### What consolidation replaces — three paths exist today

**Read from the code, 2026-09-09.** The decision above says consolidation is the target; this is what it
consolidates *from*. All three run the
same `BindingConflictRules.isSafeOverlap()` underneath and diverge in scope:

| # | Path | Scope |
|---|---|---|
| 1 | **Database / voice / log** — `BindingsMonitor.checkForConflictsAndPersist()` → `binding_conflicts` table, driven by `KeyBindCheck` | Narrow. Filters to pairs touching a control Elite-Intel drives, deliberately, to keep the spoken warning quiet. |
| 2 | **The Binding Profile UI** — `BindingProfilePanel` calls `BindingConflictScanner.scan()` directly | Broad. Every conflict the scanner finds, unfiltered. |
| 3 | **The assign dialog and keyboard map** — `candidateConflict()` | Per-candidate. Colours each key while a commander is choosing one. |

**The screen a commander actually looks at does not read the database at all.** Only `BindingsMonitor`,
`BindingConflictDao` and `BindingConflictManager` touch `binding_conflicts` — no UI class does, confirmed
by repository search in July and again in September. So "the persisted set" and "what the user sees" have
never been the same thing.

The consolidation target, confirmed 2026-07-13: **one service computes the full, accurate conflict set and
publishes it; the UI shows everything; the announcer applies its own *display* filter on top** — rather
than a second computation arriving at a different answer to "is this a conflict". The present split bakes
a filtering decision into the computation path, which is the SRP/DRY objection
Elite-Intel's own `CODING_STANDARD.md` raises directly.

### Known scanner defects — all still open

**All four are live work rather than history.** FN-1, FP-1 and FP-3 were each confirmed against the game
itself rather than suspected, and verified against the code on 2026-09-09. FN-6 was a suspicion until
2026-09-12, when it was confirmed by reading the same code — see below.

**All four re-checked 2026-09-12**, after Krondor's September conflict work landed. That work added a
[third blocking rule](domain-knowledge/EliteDangerous-ConflictRules.md#the-three-breaks-in-the-context-model),
twenty-odd scanner tests, and a better source for an action's vehicle context (see below) — but it did
not touch keyset extraction or the device gate. **The rules got sharper; what reaches them did not
change.** FN-6, never checked before, is now confirmed rather than suspected.

| ID | Defect | What the game actually does | Still open? |
|---|---|---|---|
| **FN-1** | **Secondary-slot blindness.** One slot per action is kept and the other discarded before any conflict check runs. | Treats a Secondary-vs-Primary collision as **a real conflict**. | **Yes.** `toKeysets()` still builds one keyset per action from a single `KeyBinding`. |
| **FP-1** | **Hold and tap are not part of combo identity.** | Does **not** warn when a hold-bound and a tap-bound action share a key. | **Yes.** `buildKeyset()` and `keysetOf()` read `key` and `modifiers` only — never `hold`, although `KeyBinding` carries it. |
| **FP-3** | **Sub-state over-suppression.** Any `ExplorationFSS*` / `ExplorationSAA*` action is blanket-treated as safe. | **Does** warn — `ExplorationFSSEnter` sharing a key with `DeployHardpointToggle` is a genuine conflict. | **Yes.** `isSubStateModeAction()` still matches on those prefixes and `isSafeOverlap()` returns true for either side. |
| **FN-6** | **A non-keyboard modifier drops the whole slot.** Not just the modifier — the entire binding never reaches the conflict map. | Warns normally; the chord exists as far as the game is concerned. | **Yes, and confirmed 2026-09-12** — no longer a suspicion. `isKeyboardUsable()` requires the main key be `Keyboard` **and every modifier** be `Keyboard`, so one HOTAS modifier voids the slot. |

**FN-6 in full, since it was the unknown.** The gate is `isKeyboardUsable(device, key, modifiers)`:
a keyboard main key **plus** `modifiers.stream().allMatch(m -> "Keyboard".equals(m.device()))`. A slot
failing it becomes `null` in `toExecutableBinding()`, so `parseBindings()` falls through to the other
slot — or drops the action entirely when neither survives. **Shift+Joystick_1 on a keyboard key is not
a partially-read binding; it is an absent one.**

Both conflict paths apply it independently: `BindingsMonitor` through `parseBindings()`, and
`BindingProfilePanel.executableBinding()` with its own copy of the same check — which is itself worth
noting under [one writer, one way](overview.md#there-is-one-writer-and-it-already-exists).

**The gate is correct where it was written and wrong where BindForge needs it.** Elite-Intel cannot
press a HOTAS modifier, so excluding those chords from *command execution* is right. A bind editor is
not executing anything: it has to tell the commander their chord collides whether or not the assistant
could ever press it. **This is the same shape as
[widening `BindingsWriter`](overview.md#two-narrow-boundaries--one-stays-one-widens)** — a restriction
that protects the assistant, applied to an editor where it silently hides real problems. The remedy is
the same too: the editor reads through its own full-fidelity path, and the execution gate stays exactly
where it is.

**FN-1 and FN-6 compound.** FN-1 keeps one slot per action; FN-6 can be what empties the slot that was
kept. An action whose Primary is a HOTAS-modified chord and whose Secondary is plain keyboard is
scanned on its Secondary alone, with nothing reporting that the Primary was discarded.

**One thing did improve.** `contextOf()` now reads an action's vehicle from
`BindingDisplayNames.lookup(action).section()` — the game's own OPTIONS › CONTROLS screen — falling
back to substring-matching the tag only for `GENERAL` and `OTHER`. Context is the basis of every
`isSafeOverlap` decision, so sourcing it from the control screen rather than from whether a tag happens
to contain `Buggy` makes the whole safe/unsafe split more trustworthy than when these defects were
first recorded.

#### FN-1 in detail — scoped 2026-09-12

**It is a dependency, not a cleanup.** Two specified features are built on top of it and cannot be
correct without it:

- The capture dialog lists *every* action sharing a chord — see
  [Capture Flow](#capture-dialog--settled-2026-09-13). A scanner seeing one slot per action cannot
  produce that list.
- The remedy for [Missing's second shape](#missing-has-two-shapes--added-2026-09-12) is *add a keyboard
  binding in the free Secondary slot*. **The scanner cannot see the slot that remedy writes to**, so it
  cannot confirm its own fix worked.

**Where the slot is actually lost.** Not in the scanner — one layer above it. `parseBindingSlots()`
already returns **both** slots, correctly. `parseBindings()` then collapses each pair to a single
`KeyBinding`, primary-else-secondary, and everything downstream inherits the loss:

| Layer | State |
|---|---|
| `parseBindingSlots()` | **Both slots present.** The data exists. |
| `parseBindings()` | **Collapses to one** — primary if present, else secondary. This is the defect. |
| `toKeysets()` | one keyset per action, from that single binding |
| `scanKeysets()` | keyed by action name, so **the type cannot represent two slots** even if given them |
| `BindingProfilePanel.effectiveBindings()` | **a second copy** of primary-else-secondary, on the grid's own path. The fix removes it rather than patching both. |

**The question that sorts every caller.** The one-slot view answers *"what key does Elite-Intel
press?"* — and for that it is correct. The both-slots view answers *"what is bound in the file?"* — which is
what an editor needs. **Audited against the code 2026-09-12, including Krondor's latest merge:**

##### Needs both slots

| Call site | Feeds | What goes wrong today |
|---|---|---|
| `BindingConflictScanner.scan` — Game Mode grid, spoken warning, `binding_conflicts` | Conflicts | a clash in the discarded slot is invisible |
| `candidateConflict` — `AssignKeyboardBindingDialog` and `KeyboardAvailabilityView` | capture dialog, keyboard map | a key taken in another control's discarded slot shows green |
| `recommendVehicleTwins` — `BindingProfilePanel` | ship/SRV twin nudge | twins called mismatched when their other slots agree |
| `ReservedKeyChords.scan` | [Reserved](#four-kinds-one-question) | a chord Windows or the game menu swallows goes unreported if it sits in the discarded slot |
| `ReservedKeyChords.gameMenuKeys` | Reserved, keyboard map | with Game Menu bound in both slots, only one of its keys is treated as reserved |
| `UiNavigationTextTrap.scan` | [Invalid](#four-kinds-one-question) | a UI navigation key that types into the search box goes unreported if it sits in the discarded slot |

##### Correct with one slot — must not change

| Call site | Why one slot is right |
|---|---|
| `InputSequenceExecutor.resolveBinding` | it presses the key, and needs exactly one |
| `UINavigator.isBound` | *"is there something Elite-Intel can press?"* |
| `BindingsMonitor.checkForMissingBindings` | Missing asks whether Elite-Intel has *a* key, and falling back to Secondary is exactly what makes that answer right |
| `ToolGenerateBindings` | a developer tool that reads action names only |

**These must not be "fixed" along with the rest.** They look like the same defect and are not — each asks the
executor's question, and gets the executor's answer.

##### Two shapes of fix, chosen by who else depends on the caller

**Widen the scanner family** (the first three rows). The map key becomes a typed slot reference — action
plus `BindingSlotType`, which already exists — rather than a bare action string. That flows into
`Conflict`, `candidateConflict` and `recommendVehicleTwins`, and lets [Game Mode](#game-mode) colour the
offending *slot cell* rather than the whole row. One new rule is needed: **Primary and Secondary of the same
control on one chord is redundancy, not a conflict** — pressing it fires one action — so same-action pairs
are skipped. `BindingProfilePanel.effectiveBindings()` is deleted, not updated.

**Add alongside, for the two anomaly detectors** (the last three rows). `ReservedKeyChords` and
`UiNavigationTextTrap` also drive Elite-Intel's own spoken warnings, and **for those the one-slot view is
correct**: the text-trap warning exists because *Elite-Intel's* interface walk would type into the search
box, and Elite-Intel presses the surviving slot. Only BindForge's Anomalies tab, where the commander can
press either slot, needs both. So each gains a both-slots entry point for BindForge, and the existing
methods keep serving the voice path unchanged.

**This is not a second way of doing the same thing**, which `CODING_STANDARD.md` would rule out. The two
methods answer different questions — *"will the key I press be swallowed?"* and *"is anything in this file
swallowed?"* — and they live on the same class rather than a parallel one.

##### What it does not touch

- **No database migration.** `binding_conflicts` holds `conflict_key` and `description`, and
  `checkForConflictsAndPersist()` already funnels every conflict through
  `BindingConflictRules.makeKey(actionA, actionB)` into a `Set`. Several slot-level conflicts between one
  pair of actions collapse to one row **on their own**.
- **Krondor's tests keep their assertions.** Every scanner-family test — 32 `scanKeysets`, 6
  `candidateConflict` and 4 `recommendVehicleTwinsKeysets` calls — builds its input through the one private
  `bindings(Object...)` helper. Changing the key type means changing that helper plus the declared type of
  five local `existing` variables; **no test body's expectations change.** `ReservedKeyChordsTest` (11
  calls) and `UiNavigationTextTrapTest` (6 calls) are **not touched at all**, because those fixes add a
  method rather than change one. That matters because he adds tests to these files weekly — see
  [the package freeze](../../00-overview/v1.2-scope.md#the-code-stays-where-it-is--stated-by-krondor-2026-09-12).

##### What it does touch that is easy to miss

- **The spoken conflict warning can see more.** Widening `scan` means a clash in the *Secondary* slot of a
  control Elite-Intel presses by *Primary* would newly be announced — even though it cannot affect the key
  Elite-Intel sends. **That is Krondor's call, not BindForge's**: the persist path can keep the warning
  exactly as it is by filtering to the slot `getBindings()` returns. *Corrected 2026-09-12; an earlier
  version of this section said the spoken warning was unaffected.*
- **`ai/hands/PACKAGE.md` documents "primary slot wins over secondary".** That stays true of the executor's
  view and must gain a line describing the both-slots view **in the same change**, or it becomes the kind of
  half-true documentation the coding standard treats as an incomplete change.

**The blast radius, corrected 2026-09-12:** `BindingConflictScanner` and its three callers, one new method
each on `ReservedKeyChords` and `UiNavigationTextTrap`, one duplicate removed from `BindingProfilePanel`,
and one decision for Krondor about the spoken warning. An earlier version said *"the scanner and its
callers"*; the audit found the two detectors. **Still no schema change, still no test assertions
rewritten** — which is what keeps FN-1 a reasonable first slice rather than a refactor to be feared.

**FP-3's suppression list has grown since it was first recorded**, now also matching `Wheel`, `MultiCrew`,
`Store` and anything containing `Cam`. Each addition widens a rule already known to be too broad. The fix
is to narrow it to genuinely isolated sub-states, which is the same distinction
[§3d's severity model](domain-knowledge/EliteDangerous-ConflictRules.md#3d-conflicts-have-severity-not-just-existence)
makes elsewhere: a rule can be right in general and still need its exceptions named.

**Where filtering happens (resolved):** the shell owns one shared Show Anomalies Only toggle rather than each mode having its own, but what that toggle filters *to* is mode-specific — a row-level filter in Game Mode and Input Mode (same row format, so same filter), a group-level filter in Action Groups. It has no effect in the Conflicts tab, since that tab's content already is the conflict list. Control Types and Controller Mode don't need a defined behavior, being confirmed back-burner.
