# BindForge — Making Several Game Copies Match (reconcile redesign)

**Status: approved by Alan and built, 2026-11-18 (Krondor; branch `V1.2-reconcile`).** Alan tests it and fixes what
needs fixing. Mockups: [`mockups/reconcile/`](mockups/reconcile/) (PNG), built from `mockups/reconcile/src/build.py`
(`python3 -I build.py` re-renders them with headless Chrome). It replaces *how the user is asked*, and, by Alan's
answers below, three rules of *what is written* - see [Decisions](#decisions-alan-2026-11-18). It supersedes the S22a
"one dialog" design on `origin/V1.2-BindForge` (alias-designer.md, *One dialog, no confirmation after it*), whose
settled engine rules it keeps: a dead entry's bindings move, and one controller's aliases share one label merge.

## Why the current screen fails

A user with Elite Dangerous from Steam **and** Epic opens BindForge and gets the grid in the old
`reconciliation-screen.png`. The maintainer needed several minutes to read it. A casual player has no chance:

| What the screen shows | What the player has to know to read it |
|---|---|
| Columns *DeviceMappings.xml entry*, *Button map* | That the game keeps a per-install XML file and per-device label files, and what they hold |
| `RVWAP` and `RightStick` on two red rows | That these are two names for **one** stick, and that the rows are a pair |
| `Throttle.buttonMap (14 labels)` / `(17 labels)` | That *labels* are button names, and that the numbers mean the lists differ |
| `[STEAM]`, `[EPIC]`, `[MERGE]` in a column | That these small boxes are buttons, that they are a choice between rows, and that rows without boxes need nothing |
| DANGER / WARNING / SUCCESS legend | That red means *"bindings use this"*, not *"something is wrong"*. Since A16 (bindings follow the winning name) picking **either** red option loses nothing, so the red is alarming and mostly false |
| *"I HAVE READ AND UNDERSTOOD THIS INFORMATION"* | Nothing: it is a gate, and the decisions are on the screen behind it |

The structure is the problem, not the wording: one table mixes **questions** (3 rows), **things BindForge does by
itself** (4 rows) and **things already fine** (2 rows), in file vocabulary, and the player has to work out which is which.

## Design in one paragraph

A four-step wizard: **Plug in → Choose → Check → Done**. The player sees one plain question at a time, each about **a
controller they own**, with two or three big choice cards that say **what happens** ("131 of your controls already use
this name") instead of what the file holds. Everything BindForge can settle by itself is **not asked**; it appears
once, in plain words, on the Check screen before anything is written. File names (`DeviceMappings.xml`, `.buttonMap`,
`.binds`) **never appear**. A shortcut, *Make everything like my Steam copy*, answers every question at once.

## Words: what the code says → what the player reads

Every string below is the player's. Code, logs and the spec keep their own words.

| Code / spec | On screen | Notes |
|---|---|---|
| installation | **copy** — *your Steam copy*, *your Epic copy* | The storefront as a plain text badge (`STEAM`, `EPIC`, `FRONTIER`, …). No logos. Two copies from one store: *Steam copy (D:)*, by drive |
| device element / element name / alias | **name** — *"the name your controls use for it"* | Shown in monospace (`RVWAP`) so it reads as a name, not a sentence |
| VID/PID, hardware | **controller**, by its product name — *VPC Throttle CM3* | The name the current screen's *Device* column shows. Unknown: *Unknown controller (3344:4130)* |
| `.buttonMap` labels | **button names** | Explained once: *"what the game's Controls screen shows, e.g. 'Boost' instead of 'Joy 1'"* |
| input code `Joy_1`, `Joy_POV2Up`, `Joy_RZAxis` | **Button 1**, **Hat 2, up**, **Rotary axis** | The code is kept small, grey, underneath, for the forum post |
| `.binds` bindings referencing the element | **your controls** — *"131 of your controls"* | Count from `BindsDeviceReferences`, across every binds file, as today |
| working copy | **saved in BindForge** | Only on the later check (screen 10), where it is a side |
| SHADOWED element | *"an old second name … that the game never used"* | |
| first-setup backup | *"Your files from before are kept forever"* | |

## The screens

Every screen of the wizard shares a footer line: **"Nothing changes until the last step. Everything is backed up
first."** (shield icon, success colour). It replaces the *Everything is backed up* banner and the *I have read*
gate, and it is true: first setup writes only at **Make both copies match** (storage.md rule 3, unchanged).

### 01 — Welcome ([`01-welcome.png`](mockups/reconcile/01-welcome.png))

Shown when the installations differ; with every copy identical there is nothing to ask and the flow is today's.

- **Headline:** *"Your two copies of Elite Dangerous don't agree about your controllers."* With three copies:
  *"Your 3 copies…"*.
- **One paragraph** of why: shared controls, each copy keeps its own list of controllers and button names, so the
  controls may work in one copy and fall back to keyboard and mouse in the other.
- **A small picture** of exactly that: one box per copy, each with its own list of names, both wired to one
  *Your controls* box in the middle. Built as three painted panels; no image asset.
- **Three counts:** *N questions for you*, *N things BindForge sorts out by itself*, *N controllers already match*.
  This tells the player up front that the job is small.
- **Not now** (closes; offered again next click, as today) · **Let's fix it**.

### 02 — Plug in ([`02-plug-in.png`](mockups/reconcile/02-plug-in.png))

**New.** This step deals with the support case *"I lost all my binds!"*: a binds file that names a controller with no
entry, or a controller that is not plugged in, makes the game fall back to KEYBOARD & MOUSE
([domain doc §1.2b](domain-knowledge/EliteDangerous-DeviceMappings-ButtonMap.md#12b-what-happens-when-a-device-name-has-no-entry--measured-2026-09-22)).
Making the copies match fixes the first cause, but not the second. The player needs to hear about the second here,
when they are already thinking about their controllers.

- Lists **every controller any copy knows** (the user's elements, not Frontier's stock ones), with a live
  **Plugged in / Not plugged in** state. It updates on hot-plug (`DeviceService`); no refresh button.
- **Press any button on a controller and its row lights up**, with *"← you just pressed a button on this one"*. This
  is how a player tells two sticks of the same model apart without knowing what a VID/PID is.
- **A *Known to* column** names the copies that hold each controller (Alan, 2026-11-18, D3), and a *Names* column
  its aliases, so a controller one copy alone knows is plain to see.
- **Never blocks.** *Next* is always enabled; a controller sold years ago can stay unplugged.
- Why this is a step of its own, not a note: the questions that follow show the plugged-in state on every
  controller, and question 2 (*which controller is "Yoke"*) is often answered by *"the one that's plugged in"*.

### 03–05 — Choose: one question per screen

`Question 1 of 3` above the headline. **Back** / **Next question** (last: **Check the result**). **Next is disabled
until the question is answered**; nothing is preselected (principle 6, *ask, don't guess*, and S10's *nothing is
selected and Enter chooses nothing*). The footer's left side carries the shortcut link
**Make everything like my Steam copy…** (screen 09).

Each question is a set of **choice cards**: big, bordered, with a radio mark in the corner. The selected card gets
the accent border and fill. Each card says **where the option comes from** (store badge), **what it is** (big), and
**what happens if you pick it** (✓ lines in success colour, • for neutral facts, ! in warning colour). Below the
cards, one **reassurance note** says what *cannot* go wrong, because that is what a worried player needs to hear.

Order of questions: **NAME_DIFFERS** and **HARDWARE_DIFFERS** first (they decide which devices exist), then
**LABELS_DIFFER** (cosmetic). Within each, by the number of controls involved, highest first.

#### 03 — Two names for one controller — `Kind.NAME_DIFFERS` ([`03-question-name.png`](mockups/reconcile/03-question-name.png))

- **Headline:** *"Your right stick has a different name in each copy"*. The controller's product name (and icon)
  under it, with *"Your controls find each controller by a short name. Pick the one to keep."*
- **One card per name** (`Row.options`): *STEAM calls it* **RVWAP** · ✓ *131 of your controls already use this name*
  · • *Your 12 controls that use "RightStick" are moved to it.*
- **Note:** *"Nothing stops working either way. Your controls follow the name you pick. This only decides what the
  controller is called."* This is true since A16, and it is why the old red colouring goes.
- A name used by **no** control says *"None of your controls use this name"* and has no move line.
- Case-only difference (`LVWAP`/`lvwap`): the same question; the note adds *"The game treats different capitals as
  different names, so your controls are all spelled the way you pick."*

#### 04 — One name, two controllers — `Kind.HARDWARE_DIFFERS` ([`04-question-controller.png`](mockups/reconcile/04-question-controller.png))

- **Headline:** *"'Yoke' means a different controller in each copy"* · *"48 of your controls are set up on Yoke.
  Which controller should they work on?"*
- **One card per controller**, each showing the controller (icon, product name, live plugged-in state). ✓ *Your 48
  "Yoke" controls work on this yoke.* A controller that is not plugged in adds the warning line: *"It isn't plugged
  in. Plug it in before you play, or the game falls back to keyboard and mouse."*
- **Pressing a button on a controller selects nothing**, but lights up its card (as on screen 02), for the same
  reason as there.
- **Note:** *"Not sure? It's usually the one you still own and have plugged in. The other controller keeps working in
  the game, just without your 'Yoke' controls; BindForge offers it a name of its own the next time it is plugged in."*
  The losing hardware is simply not in the working copy, and onboarding names it on next sight, as today.
- *No answer may leave two devices on one VID/PID* (FirstSetupPlan's `clashing`): when the player's answers clash, the
  Check button stays disabled and the clashing question shows a line in warning colour: *"This controller already has
  the name RVWAP from question 1. Pick the other one, or change question 1."*

#### 05 — Button names differ — `Kind.LABELS_DIFFER` ([`05-question-buttons.png`](mockups/reconcile/05-question-buttons.png))

- **Headline:** *"Some buttons on your throttle are named differently"*, with the one-line definition of a button name.
- **Three cards:** *Use Steam's names* (with three example names) · *Use Epic's names* · *Choose button by button*
  (*"7 buttons · 3 chosen"*, and a **Choose names…** button that opens screen 06).
- **Note:** *"Button names never change what a button does. Names only one copy has are kept either way: 11 more
  buttons are named in only one copy, so nothing is lost."*
- A store card is `labelAnswersFavouring`: that copy wins every **collision**, labels only another copy has are
  still taken. (This is the 2026-10-08 per-device rule. It is **not** the 2026-10-24 *use one installation for all*,
  which dropped the other copy's labels. See [Decisions](#decisions-alan-2026-11-18), D2.)
- The card is selected once the device's every input is answered, by whichever route; the button-by-button card
  shows the count while some are open.

#### 06 — Button by button ([`06-button-by-button.png`](mockups/reconcile/06-button-by-button.png))

The old merge view, made readable:

- Rows are **inputs in words** (*Button 1*, *Hat 2, up*, *Rotary axis*), with the code underneath in small monospace.
- Each name is a **radio chip**: a clear empty/filled dot, the selected chip bordered and filled with the accent. The
  old boxed labels looked the same chosen or not.
- **Press a button on the controller** and its row lights up and scrolls into view (*"← you pressed this"*).
  Axes and hats too, through `DeviceService`, as the capture dialogs do.
- Column heads: the store badge plus a small **use all** link (replaces the two big *USE STEAM'S LABELS* buttons).
- *"3 of 7 chosen"* top right; the line *"Buttons named the same in both copies, or in only one, are kept and not
  shown."* explains why the list is short.
- **Cancel** changes nothing; **Done** keeps a partial answer (as today).

### 07 — Check ([`07-check.png`](mockups/reconcile/07-check.png))

The summary step (alias-designer.md *The flow*, step 4), shown as the **result**, not the differences:

- *"This is what both copies will have"*: one row per controller the working copy will hold, with its **name**, how
  many **button names** it has, **why** it is there in plain words (*Your choice*, *Was only in Epic, added to
  Steam*, *Already the same*, *Your choice. 12 controls moved from "RightStick"*), and **plugged in now**.
- **Also tidied up**: shadowed names removed, a controller that lost a name (from question 2). The player meets these
  here, once, rather than as rows they might think they need to act on.
- **A plugged-in note** when any controller the controls use is not plugged in, naming those, and separately the
  ones no control uses (*"can stay unplugged"*).
- **Back** (to the last question) · **Make both copies match**. That button is today's write (`FirstTimeStartup`
  through `WritePath`): backup, working copy, every installation.

### 08 — Done ([`08-done.png`](mockups/reconcile/08-done.png))

- One line per copy (*✓ Updated*, or what failed and what to do; principle 9), and one for the controls (*12 controls
  moved to RVWAP*).
- **Before you play:** names every controller the controls use that is not plugged in. Shown even when the quiet box
  is ticked, as any report saying more than the plain success line is (storage.md rule 3, S21).
- Where the backup is: *File Manager → Backups → Before BindForge → Restore.*
- *Don't show this again when there is nothing to warn about*: S21's box, reworded.

### 09 — Shortcut: make everything like one copy ([`09-shortcut-one-copy.png`](mockups/reconcile/09-shortcut-one-copy.png))

From the footer link on any question. One card per copy, each listing in words what that copy would decide (*Right
stick is called RVWAP, "Yoke" is the Logitech Flight Yoke, Steam's button names on the throttle*) and what is kept
from the others anyway. **Check the result** goes to screen 07 with every question answered; *Back to the questions*
returns with the answers the player had.

This is `FirstSetupPlan.answersFavouring(installId)`, which already exists: it answers every question in that copy's
favour and keeps entries only the other copies have. Decision D1.

### 10 — Later: which setup is current ([`10-later-a-copy-changed.png`](mockups/reconcile/10-later-a-copy-changed.png))

The check (storage.md rule 14, S9) asks one thing (Alan, 2026-11-18, Q3): **which setup is current** - the one saved in
BindForge, or the one a copy of the game now holds. Whichever is picked, after the write every copy and the shared binds
files agree. No Plug in step: the player is mid-session.

- The wizard opens on that question: one card per side - **What I saved in BindForge** (the working copy; at APPLY,
  *"Your files, with the edits not applied"*) and **Like my Epic copy** for each copy that differs - each listing what it
  decides. *Choose controller by controller...* goes to the questions for a player who wants to mix.
- **Use the game's files** in the check's warning, with one copy differing, opens the wizard with that copy already
  chosen, on the result. It no longer takes that copy's files whole (`confirmUseOne` is gone): an alias only the working
  copy holds stays, so the shared binds files still find every device they name (D1).
- **The saved setup** is *Put mine back*, as before. A **reset by a game update** keeps its *"Are you sure?"*.

## Edge cases

| Case | What the player sees |
|---|---|
| No questions, only things BindForge settles | Welcome says *0 questions*; Plug in → **Check** |
| Every copy identical | Today's flow: nothing to reconcile, nothing asked |
| Three or more copies | Cards sit side by side up to 3; from 4, a vertical list of the same cards |
| Two copies from one store | Cannot happen (Alan, 2026-11-18, Q4): a second Steam or Epic account uses the same installation |
| A copy with no `DeviceMappings.xml` | It has no side in any question, and is not offered in the shortcut (S2b rule) |
| A file that cannot be read | Stops first setup and is named, as today - a plain error dialog, not a question |
| A controller not plugged in | Shown by its aliases: an aliased controller "exists" to the copies even unplugged, but the game cannot see it, and falls back to keyboard and mouse if the binds use it (Alan, 2026-11-18, Q2) |
| Controller with no button names in any copy | No button-names question; Check shows *—* |
| Answers clash on one VID/PID | Explained on the question; *Next* disabled until changed |

## Decisions (Alan, 2026-11-18)

- **D1. Aliases must agree everywhere, never merged.** A controller called FOO in Epic and BAR in Steam ends with one of
  the two, in every copy and in the binds files. The reconcile exists to clean up players' own attempts at running two
  copies. Built as: the name question (03) keeps one alias and moves the other's bindings to it; **a dead entry's
  bindings move too** (to the alias the game uses on that controller, or the one that beat it); the shortcut and the
  check's *use the game's files* answer every question one copy's way and keep controllers only another copy knows,
  since dropping one the binds use would get the whole binds file rejected.
- **D2. Labels are harmless to keep; aliases are not** (they go into the binds file as the device name). *Use Steam's
  names* keeps names only Epic has. One controller under two aliases has **one** button-names question, its names
  combined under whichever alias is kept. Two controllers under one alias never mix: the one chosen keeps its own.
- **D3. Plug in stays**, with a column saying which copies know each controller.
- **D4, D5. The colour legend and the "I have read and understood" gate go.**
- **D6. Nothing is preselected.** The player decides every question.
- **Q2.** An unplugged controller shows its aliases.
- **Q3.** The later check asks only which setup is current (screen 10).
- **Q4.** Two copies from one store cannot happen.

## Built (2026-11-18)

| Piece | Class |
|---|---|
| The wizard: Welcome, Plug in, questions, shortcut / "which is current", Check | `ui.screen.bindings.ReconcileWizard` (replaces `FirstSetupDialog`, removed) |
| What to ask, in what order, the counts and the result's reasons - pure, tested | `ReconcileQuestions` (`ReconcileQuestionsTest`) |
| Button by button | `LabelMergeDialog`, rebuilt: radio chips, inputs in words, *use all*, press to find |
| Plugged-in state, Windows' names, presses | `LiveControllers` (`DeviceBus`, for the dialog's lifetime) |
| Input codes in words | `InputNames` (`InputNamesTest`) |
| Cards, chips, wrapped text | `ReconcileWidgets` |
| Dead entries' moves; one label merge per controller; a name on two controllers keeps the chosen one's labels | `FirstSetupPlan` (`FirstSetupPlanTest`) |

- **Callers:** `FirstTimeStartupFlow` (Mode `firstSetup`), `RestoreFlow` (`restore`, its trailer on the Check step),
  `CheckFlow` (`check`, opening on "which is current", or with the one differing copy already chosen).
- **Done** is the existing report dialog, which now ends with *Before you play: plug in ...* when a controller the binds
  use is unplugged; such a report always shows.
- **Strings:** `bindings.reconcile.*` in all nine `gui` bundles (`_pt` seeded in English for its localizer), checked by
  `ReconcileTextTest` (every string formats, both sides of each choice). The old grid's 34 strings are removed.
- **Not built:** S22a's *Keep as* (keeping a controller that lost a shared name under a new name) - onboarding names it
  the next time it is plugged in; and a pressed **axis** does not light a row, only buttons and hats.
