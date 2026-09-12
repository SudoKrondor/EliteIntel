# Elite Dangerous — Binding Conflict Rules

**Scope:** What Elite Dangerous itself actually treats as a keybind conflict, and what that means
for BindForge's own conflict-detection algorithm (BindForge-Spec.md §5.2). Raw test data lives in
`EliteDangerous-ConflictMatrix.md` — this doc is the analysis built on top of it.

**Confidence:** The subgroup-level findings below are directly tested, in-game, via the rebind
dialog (2026-07-30) — not inferred. The one thing this doc does **not** confirm is whether the
top-level file sections (General/Ship/SRV/On Foot) are truly conflict-isolated from each other —
see §3. Treat that boundary as provisional until it's specifically retested.

---

## 1. The Core Finding: Elite's Own Conflict Check Is Not Flat

Earlier assumptions in this project went through three stages, each corrected by direct testing:

1. **First assumption:** the game's rebind dialog does a flat "is this raw input already used
   anywhere in the file" check, with no concept of context at all.
2. **Corrected:** the four top-level binding-file sections (General/Ship/SRV/On Foot) already
   don't cross-conflict — so there's at least some context-awareness.
3. **Corrected again:** within a section, the game *also* has real sub-context groupings — e.g.
   FSS Mode and Driving Turret Controls are both properly exempted from conflicting with the rest
   of their section, because you can't be in that sub-mode and the rest of the section's activity
   at the same time.

So the real picture is a two-level model:

- **Macro level** — the four file sections. Assumed isolated from each other (see §3 for the open
  question on this).
- **Micro level** — subgroups within a section. Some pairs conflict, some don't, and it is
  **not predictable from category alone** — it had to be tested pair by pair. Full results in
  `EliteDangerous-ConflictMatrix.md`.

---

## 2. Key Findings Per Section

**General Controls** — Interface Mode, Camera Suite, Free Camera, and the "photo booth" cluster
(Holo-Me, Playlist, Store Camera) mostly conflict with each other, consistent with all of them
reusing the same generic pan/zoom/navigation action set. Galaxy Map and System Colonization
Facility Placement are both largely isolated (Galaxy Map only conflicts with Playlist; SCFP
conflicts with nothing in this section at all).

**Ship Controls** — the biggest surprise here: almost the entire "active flight" cluster (Mouse
Controls, Flight Rotation, Flight Thrust, Alternate Flight Controls, Flight Throttle, Flight
Miscellaneous, Targeting, Weapons, Cooling, Miscellaneous, Mode Switches) behaves as **one shared
conflict domain** — nearly every pair within it conflicts. Three exceptions carve out real isolated
pockets:

- **Flight Landing Overrides** (the `_Landing`-suffixed bindings) is mostly exempt from the main
  cluster — no conflict with Rotation, Thrust, Throttle, Targeting, Weapons, Cooling, Misc, or
  Alternate Flight. It does conflict with Mouse Controls, Mode Switches, and Multi-Crew.
- **Multi-Crew is a confirmed Frontier bug** — it conflicts with nearly the entire main cluster
  even though you can't be in your own cockpit and in a crewmate's turret/helm seat at the same
  time. The two states are mutually exclusive in real play; the game's conflict check doesn't know
  that. This is the finding that started this whole investigation.
- **Full Spectrum System Scanner (FSS) is correctly isolated** — conflicts with nothing.
  **Detailed Surface Scanner (DSS) is not** — it conflicts with both Weapons and Mode Switches.
  This corrects an earlier assumption in this project that DSS would behave like FSS since they're
  sibling view-modes; they don't, and this had to be tested to catch.

**SRV Controls** — same shape as Ship, smaller: Driving, Driving Targeting, Drive Throttle, and
Driving Miscellaneous all conflict with each other freely. **Driving Turret Controls is correctly
isolated** (only conflicts with Driving Mode Switches) — the same real-world exclusivity as
Ship's Multi-Crew (can't drive and gun the turret at once), but here the game gets it right.

**On Foot Controls** — fully connected. Every subgroup tested conflicts with every other one, no
isolated pockets at all.

---

## 3. Open Question: Does Ship Ever Conflict With SRV? — Not Resolved

During testing, there was at least one unconfirmed observation of what looked like a conflict
between a Ship-section action and an SRV-section action. This directly contradicts the working
assumption (§1, macro level) that the four top-level file sections never cross-conflict — an
assumption every table in `EliteDangerous-ConflictMatrix.md` depends on.

**This is not confirmed either way yet.** If it turns out to be real:

- The four separate per-section matrices are no longer independently complete — a cross-section
  matrix (Ship subgroups × SRV subgroups, and potentially × General/On Foot too) would be needed
  on top of them.
- It would mean the game's context-awareness is even less predictable than §1 already shows —
  correct at some boundaries (FSS, Driving Turret Controls), buggy at others (Multi-Crew), and
  now possibly leaky at the macro/section level too, not just the micro/subgroup level.

**Standing flag:** retest the specific scenario that triggered this observation (which Ship
subgroup, which SRV subgroup, which key) before treating the macro-level section boundary as
settled. Until then, BindForge's conflict-detection algorithm (§4) should not hard-code an
assumption that cross-section pairs are always safe.

**What the implementation does in the meantime — recorded 2026-09-12.** `isSafeOverlap` treats a
cross-vehicle pair as safe, and now says why in terms of the commander rather than the file: a control
they think of as one thing but Elite binds per vehicle — *the cargo scoop, the fire groups, the
triggers, the panels, the maps, the lamps, night vision* — may sit on one key in all of them.
**Commanders lay it out that way on purpose, and only one vehicle is ever occupied.**

That does not resolve the observation above, which was about *the game's own* check. It does mean
flagging these pairs would be a false positive against real, deliberate layouts — so the open question
is whether the game disagrees, not whether BindForge should start warning.

---

## 3b. A Second Category: Contextually Invalid Bindings

Everything above concerns **pairwise** conflict — two actions competing for one input. There is a second,
structurally different failure that no pairwise check can find: **a single binding that is valid, unique,
uncontested, and still does not work in a particular game context.**

### The confirmed case: UI navigation swallowed by a focused text field

Confirmed by direct in-game testing on 2026-08-31, and implemented in Elite-Intel as
`elite.intel.ai.hands.UiNavigationTextTrap`.

While an Elite text field has focus — the galaxy map's system search box above all — the game treats any
keystroke that produces a **printable character** as typing, and never consults the `UI_*` bindings at all. A
commander with `UI_Down` on `S` who types a system name and then presses their own "down" key appends an "s"
to what they typed. Focus never leaves the box, and every keystroke after it is typed into the box too.

**The modifier decides it, not the binding:**

| Chord | Produces a character? | UI navigation works? |
|---|---|---|
| `S` | yes | **no** |
| `Shift+S` | yes — "S" | **no** |
| `Ctrl+S`, `Alt+S` | no | yes |

**Shift is not a character-suppressing modifier** — it changes the character rather than removing it. That is
why this layout silently works for some commanders and not others, and why the correct test is *"does this
chord type a character"*, never *"is this chord modified"*.

Character-suppressing modifiers, per the implementation: `Key_LeftControl`, `Key_RightControl`, `Key_LeftAlt`,
`Key_RightAlt`, `Key_LeftSuper`, `Key_RightSuper`, `Key_Apps`, `Key_Menu`.

### Why it is invisible to the player

A commander does not hit this by hand, because by hand they click the search result with the mouse. It only
bites automation. That is why Elite-Intel announces it on **every** start rather than once — there is no way
for the player to discover it through play, and it stops route plotting outright rather than degrading it.

### Two design lessons worth carrying into BindForge

**Fail toward silence, not false positives.** The implementation lists character-producing keys *positively*
rather than as "everything except the arrows", so an unrecognised token produces no warning. Its own comment
puts it best: an unrecognised token producing no warning *"is the right way to be wrong. A startup warning that
cries wolf about a working layout gets tuned out."* BindForge's own validation should default the same way.

**Scope the check to where it actually bites.** `UI_Select` is deliberately excluded from the scan even though
bare `Space` types a character: Frontier's default binds it that way, so including it would flag nearly every
commander alive, and Select is only ever pressed once focus has already left the text box.

### The general shape

Contextual invalidity is likely broader than this one case. The general form is: *an input is valid in the
abstract, but the game is in a state where something else consumes it first.* Anywhere Elite has a modal input
consumer — text entry, a rebind dialog, a chat box — the same class of problem can exist. Only the search-box
case is confirmed; the rest is unexplored.

## 3c. A Third Category: Reserved Keys

**Documented 2026-09-06 from `elite.intel.ai.hands.ReservedKeyChords`,** which Elite-Intel gained on
2026-09-05. Neither of the categories above covers it.

A reserved key is one that **cannot be used for anything, regardless of what else is bound to it.** There
is no competing action to name and no game state to qualify it — the key is simply off the board. A
pairwise scan finds nothing wrong, because nothing is wrong *between two bindings*.

Two sources, one fixed and one read out of the commander's own file.

### Source 1: the operating system

| Chord | What happens | Where |
|---|---|---|
| `Alt+F4` | closes the focused window — quits the game | every desktop OS |
| `Ctrl+Alt+F1`…`F12` | switches virtual terminal, dropping the commander to a TTY out of the running session | Linux only |

**Matched on the full key-set** — main key plus modifiers — so key order and extra modifiers held
alongside cannot let one slip through.

### Source 2: Elite itself — the game-menu key

This is the subtle one, and it is undocumented by Frontier.

Elite's `Pause` control — shown as **Game Menu** on the options screen — opens the menu and pauses the
game. **It is matched on the key alone, not on the chord.** With `Pause` on `Key_P`, the menu comes up on
`P`, on `Shift+P`, on `Alt+P`, on anything ending in P.

So **whatever key sits on `Pause` is unusable for every other control, with any modifiers or none.** A
second control sharing that key can never be pressed without dropping the commander out of the cockpit
and into the options screen.

**Which key that is comes from the commander's file.** It is a parameter, not a constant — and
`gameMenuKeysFromSlots` reads *both* slots, because a commander with a key in Primary and another in
Secondary has two keys that open the menu.

### How it was misdiagnosed, and why that matters

The auto-assigner handed out `Alt+P` on a file whose `Pause` was on `Key_P`, and the control opened the
options menu every time. The first reading blamed the `Alt+P` chord and added it as a fixed taboo.

**But P was never special.** The reserved key is whatever sits on `Pause`; on that one file it happened to
be P. A rule written from a single commander's file described that commander, not the game. Worth keeping
in view for every other rule in this document that rests on one observation.

### The corollary: the game-menu control is worth nothing and costs a key

`Esc` opens that same menu whether or not `Pause` is bound to anything. So a key on `Pause` buys a second
way into a screen the commander can already reach, and takes a key off the board for everything else.

Elite-Intel's auto-assigner therefore leaves `Pause` deliberately empty **and** pulls its key out of the
assignment pool entirely — pulling the whole key, not the exact chord, since with the menu on P it would
otherwise still hand out `Alt+P` and `Shift+P`.

## 3d. Conflicts Have Severity, Not Just Existence

Also from the same week's work, and a distinction this document previously did not make. Elite-Intel now
sorts conflicts into three tiers, and they are reported very differently:

| Tier | Meaning | How it is reported |
|---|---|---|
| **Blocking** | stops Elite-Intel driving the game at all, not merely degrading it | its own line, unconditionally, ahead of everything else |
| **Curated** | a known pair whose consequence is worth spelling out — *"Deploying hardpoints will also toggle landing gear"* | its own line, with the curated wording |
| **Plain overlap** | two actions on one chord, no special consequence known | collapsed into a single line listing the pairs |

The collapsing is not cosmetic: a commander reassigning their controls produced **fifty-one** separate
"... and may interfere" lines in one burst, which buried everything else.

**A worked example of promotion between tiers.** `UI_Select` sharing a key with the quick comms panel was
promoted from plain overlap to **blocking**, in all three vehicle variants (`QuickCommsPanel`,
`QuickCommsPanel_Buggy`, `QuickCommsPanel_Humanoid`). Select is how Elite-Intel commits every choice it
makes in the interface, so on a shared key each tap also drops a focused chat box on screen — which
swallows what is typed next and can broadcast it.

**The context model had cleared that pair**, because Select is "interface" and comms is "ship", and
[§1](#1-the-core-finding-elites-own-conflict-check-is-not-flat) establishes that contexts do not
cross-conflict. A correct general rule with real exceptions in it — which is worth more caution than
the rule being wrong would be.

### The three breaks in the context model

**Updated 2026-09-12.** "UI and vehicle controls are never live together" is right in general and wrong
three times, and each exception is **blocking** rather than merely worth a mention. They share one
shape: a control that stays live *while a panel or map is already open*, which is exactly when
Elite-Intel is walking the interface.

| # | The pair | Why the context model clears it, wrongly |
|---|---|---|
| 1 | **Map camera vs UI navigation** | camera actions read as map-overlay, navigation as interface — but inside the galaxy or system map both are live at once |
| 2 | **`UI_Select` vs quick comms** | Select is interface, comms is ship — but the comms panel is reachable with a panel already open |
| 3 | **Panel-focus and map-open keys vs UI navigation** | the focus keys read as ship — but they are how a commander moves *between* panels, so they never stop being live |

The third was added by Krondor in September 2026 and is the most consequential of the three, because
it breaks the walk rather than disturbing it. Every panel Elite-Intel opens it then steps through with
`UI_*` taps — the role panel to recover an SRV, the right panel to a module, the galaxy map to its
search field. On a shared chord each step *also* switches panel or throws the map up over what was
there, and **the walk runs on blind, every keystroke reporting success.**

From a support bundle of 2026-09-09: `UI_Down` and `GalaxyMapOpen_Buggy` both on `Ctrl+S`, so every
attempt to recover an SRV opened the galaxy map instead. **A commander doing it by hand never sees
this** — they are looking at the screen and simply stop when the map appears.

The rule covers fifteen named actions — the four panel-focus keys and the two map-open toggles, in
each vehicle context Elite names them for. **Named, not prefix-matched**, for the same reason the map
camera family is: the action set is Frontier's, so a control they add later has to be opted in by
someone who has decided it belongs. The remedy is separation — the interface keys and the panel/map
keys have to be different chords; which layout is the commander's to pick.

**Quick comms is deliberately absent from that list** even though it behaves the same way. It keeps its
own rule, which reports it against `UI_Select` alone and leaves it an ordinary overlap against the
direction keys.

## 4. Implication for BindForge's Own Conflict-Detection Algorithm

Real conflict = same key/chord **and** both actions sit in subgroups that the tables in
`EliteDangerous-ConflictMatrix.md` mark as conflicting (or an untested pair, treated conservatively
as a conflict until tested). This is not the same as "same key anywhere in the file" (too broad —
would falsely flag Multi-Crew-style pairs project-wide) and not the same as "same key within the
same top-level section" either (too coarse — would miss that FSS and Driving Turret Controls are
correctly exempt, and would incorrectly clear DSS since it looks like FSS's sibling).

Concretely: BindForge's scanner needs a subgroup tag per action (most already derivable from the
binding zone map reference) and a lookup against these matrices to decide whether a same-key hit
between two subgroups is a real conflict, before it ever reaches the pub/sub architecture already
decided in BindForge-Spec.md §5.2.1. The known Multi-Crew bug is a case where BindForge should
**not** mirror the game's own (wrong) answer — same principle as the FSS/Hardpoints and
UI-vs-ship suppression work in §5.2.2's algorithm-gaps table, generalized to a full lookup table
instead of a handful of special cases.

**Not yet resolved:**
- §3's Ship×SRV cross-section question.
- On Foot's subgroup list may not be exhaustive — only three subgroups have been identified/tested
  so far (On Foot, On Foot Mode Switches, On Foot Emotes); real on-foot play likely has more
  (ship-interior actions while on foot, SRV boarding, Apex/taxi) that haven't been tested yet.
- Whether the subgroup tag can be derived automatically from existing `.binds`/binding-zone-map
  structure, or needs hand-curation per action (BindForge-Spec.md §5.1's open item on a
  key-name mapping table is a related, not identical, question).
