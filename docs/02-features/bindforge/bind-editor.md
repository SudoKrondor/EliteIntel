# BindForge — Bind Editor

The Bind Editor is where the player views and edits their actual key/button/axis bindings. It organizes the same underlying binding data through multiple **modes**, each offering a different lens on it. Conflict detection is shared infrastructure used by every mode, not owned by any single one — see [Shared Conflict Detection](#shared-conflict-detection) below.

## Shell (Common to All Modes)

- A Bindings File dropdown plus Load button, above everything. Load performs the standard [Live File Synchronization](overview.md#live-file-synchronization) freshness check for the Bind Domain before opening the file for editing.
- A Search box, above all mode tabs — filters whichever list is currently showing in whichever mode tab is active. Controller Mode is picture-based, not a list, so how (or whether) Search applies there is still open.
- A **Show Anomalies Only** checkbox/toggle, on the same line as Search — shared shell state, not owned by any one mode, but what it filters *to* depends on which mode tab is active: in Game Mode it filters the grid to conflicting rows only (see [Conflict Display](#conflict-display)); in Action Groups it filters the group list to only groups containing at least one binding that conflicts with something in a *different* group (see [Action Groups — Conflict Filtering](#action-groups)); in Input Mode it filters the same way Game Mode does, since Input Mode's list is already the same row format with conflicts already shown inline. It has no meaningful effect on the Conflicts tab, since that tab's entire content already is the anomaly list — not undefined, just not applicable. Control Types and Controller Mode don't need a defined behavior either, since both are confirmed back-burner and out of scope.
- Mode tabs: **Game Mode**, **Action Groups**, **Control Types**, **Controller Mode**, **Input Mode**, **Anomalies** (see below for each mode's status).
- A Context Bar of buttons (All / General / Ship / SRV / On Foot) filtering the grid to one section.
- A collapsible-group binding grid: left-click toggles one group; right-click opens Expand All / Collapse All, scoped to the currently visible groups.
- A roughly 75/25 split between the binding grid and the Binding Editor panel.

## Game Mode

**Status: built.** Organizes bindings to match the in-game controls UI exactly — the same four top-level sections and the same subgroup structure the game itself uses. See [Binding Zone Map (domain knowledge)](domain-knowledge/EliteDangerous-ActionCatalog.md) for the full, cross-verified section/subgroup/action inventory this is built from.

### Conflict Display

Carried forward from a proven, already-real pattern, not invented fresh:
- Conflicting rows render in a distinct warning color.
- Hovering a conflicting row shows a **persistent** popup (not an auto-dismissing tooltip, which vanishes too fast to read a full explanation) listing every other action sharing that key.
- The shell's [Show Anomalies Only toggle](#shell-common-to-all-modes) filters the grid to rows carrying an anomaly while Game Mode is active.

### Binding Editor Panel — Capture Dialog

Hovering a row reveals Capture Primary/Secondary controls — not a whole-row click, and not auto-armed by selection, so browsing rows to look at them is always a safe no-op. Clicking Capture opens a floating capture dialog centered over the content, modeled directly on the base game's own rebind popup (dimmed background, a box reading "Press [input] for... ACTION NAME," waiting for the next input) — a proven, already-real pattern, not invented fresh.

Capture-control color reflects slot state: an empty slot, a bound-and-clean slot, and a bound-and-conflicting slot are each visually distinct.

**Capture flow:**
1. The user presses a key/button.
2. If there's no conflict, it commits immediately to the working copy and the dialog closes.
3. If there is a conflict, the same dialog updates in place to list **every** action sharing that exact combination (not just one) with Confirm/Cancel only — no second, separate popup opens for this.

There is deliberately no "always confirm"/skip-warnings option, even though the base game has one — a skip option would defeat the purpose of warning the player at all.

**This capture dialog is the one and only input-capture mechanic anywhere in BindForge.** Every mode that lets the player rebind something — Game Mode, Action Groups, Input Mode, Anomalies, and Control Types whenever it's picked back up — opens this exact same dialog, never a mode-specific variant. A player rebinding from Input Mode's reverse-lookup list or from an Anomalies row sees the identical popup they'd see capturing from Game Mode's own grid.

### Open items — carried from the punch list, 2026-09-09

- **Does the non-active slot's Capture button stay live while the other slot is being edited?** Either
  answer is defensible; neither is chosen.
- **Colour collision: the capture button's "empty" state versus the existing "recommendation"
  indicator.** Both land on a similar light blue. **Explicitly parked by Alan — do not resolve without
  raising it directly**, since it touches an existing convention rather than only BindForge's own.

### Modifiers, Hold, Inverted, Deadzone

All of the following are confirmed by direct in-game testing, not just inferred from the file format:

- Capturing a compound combination (main input plus modifiers) is a single motion — whatever is held when the completing input arrives becomes the modifier set, and the completing input becomes the slot's value. No separate "now capture the modifier" step exists.
- Cross-device modifiers are real and confirmed working in-game — for example, a keyboard key combined with two separate joystick buttons in one binding.
- **A bind slot can combine a maximum of four inputs total** — one main input plus up to three modifiers.
- **Axis bindings can never take a modifier at all**, confirmed by direct testing — attempting to add a modifier to an axis binding silently drops it and produces a plain single-input rebind instead. The only two things that vary on an axis slot are Inverted and Deadzone.
- Hold (for buttons) and Inverted (for axes) are inline toggle controls; Deadzone (for axes) is an inline slider — none of the three use the capture-overlay mechanic.
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

**Editing from here:** clicking a row opens the same [capture dialog](#binding-editor-panel--capture-dialog) used everywhere else in BindForge, right in place — the player never has to leave Input Mode and flip to Game Mode to rebind or clear something they found this way.

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
| **Missing** | a control EliteIntel drives with nothing assigned | absence, not breakage |
| **Conflicts** | two actions sharing one input | the original category |
| **Invalid** | bound, unique, uncontested, and still will not fire | something else consumes the input first in a given game state |

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

**Player-marked — the same state, chosen rather than shipped.** A commander must be able to mark a slot
*leave this empty* so it stops being reported. Confirmed needed 2026-07-12; storage resolved to the database
on 2026-07-17, because `.binds` does not preserve comments and there is nowhere in the file to put a marker.
The mechanism — checkbox, context menu, something else — and whether it is reversible are still undesigned.

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

**Editing from here:** clicking a row opens the same [capture dialog](#binding-editor-panel--capture-dialog) used everywhere else, right in place — resolving a conflict never requires leaving the Conflicts tab and jumping to Game Mode.

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

**FN-1 is a dependency, not just a cleanup.** The capture dialog is specified to list *every* action
sharing a chord — see [Capture Flow](#binding-editor-panel--capture-dialog). A scanner that sees one slot
per action cannot produce that list, so the dialog cannot be built correctly on top of it.

**FP-3's suppression list has grown since it was first recorded**, now also matching `Wheel`, `MultiCrew`,
`Store` and anything containing `Cam`. Each addition widens a rule already known to be too broad. The fix
is to narrow it to genuinely isolated sub-states, which is the same distinction
[§3d's severity model](domain-knowledge/EliteDangerous-ConflictRules.md#3d-conflicts-have-severity-not-just-existence)
makes elsewhere: a rule can be right in general and still need its exceptions named.

**Where filtering happens (resolved):** the shell owns one shared Show Anomalies Only toggle rather than each mode having its own, but what that toggle filters *to* is mode-specific — a row-level filter in Game Mode and Input Mode (same row format, so same filter), a group-level filter in Action Groups. It has no effect in the Conflicts tab, since that tab's content already is the conflict list. Control Types and Controller Mode don't need a defined behavior, being confirmed back-burner.
