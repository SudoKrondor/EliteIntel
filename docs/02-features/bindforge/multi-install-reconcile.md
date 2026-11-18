# BindForge — Making Several Game Copies Match (reconcile redesign)

**Status: proposal for Alan's review (Krondor, 2026-11-18).** Mockups:
[`mockups/reconcile/`](mockups/reconcile/) (PNG), built from `mockups/reconcile/src/build.py` (`python3 -I build.py`
re-renders them with headless Chrome). Nothing here changes *what* first setup or the check writes; it replaces *how
the user is asked*. Where it does change a rule, the change is listed in [Decisions for Alan](#decisions-for-alan).

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
  which dropped the other copy's labels. See [Decisions](#decisions-for-alan), D2.)
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

### 10 — Later: a copy changed outside BindForge ([`10-later-a-copy-changed.png`](mockups/reconcile/10-later-a-copy-changed.png))

The check (storage.md rule 14, S9) reuses the same pieces, with **SAVED IN BINDFORGE** (the working copy; *"Your
files, with the edits not applied"* at APPLY) as one side:

- *"Your Epic copy's controllers changed since BindForge last saved them. They were edited outside BindForge, by
  hand or by another tool. Which do you want both copies to have?"*
- **Put back what I had** / **Keep Epic's changes**, each card listing the effect; **Choose button by button…** (or,
  for element differences, the question screens 03–04) when the player wants to mix.
- **Check the result** → screen 07's layout, then the write.
- A **reset by a game update** keeps its own warning and *"Are you sure?"* (rule 14, *A reset is named*); this
  screen is for an ordinary edit.

## Edge cases

| Case | What the player sees |
|---|---|
| No questions, only things BindForge settles | Welcome says *0 questions*; *Let's fix it* goes Plug in → **Check** (no Choose step; the stepper shows it ticked) |
| Every copy identical | Today's flow: nothing to reconcile, nothing asked |
| Three or more copies | Cards sit side by side up to 3; from 4, a vertical list of the same cards. The badge tells two copies from one store apart by drive: `STEAM (D:)` |
| A copy with no `DeviceMappings.xml` | It has no side in any question, and is not offered in the shortcut (S2b rule). Welcome: *"Your Epic copy has no list of controllers yet; it gets the one you end up with."* |
| A file that cannot be read | Stops first setup and is named, as today. That stays a plain error dialog outside the wizard; it is not a question |
| Unknown controller (no product name) | *Unknown controller (3344:4130)*, with the "press a button" highlight as the way to find it |
| Controller with no button names in either copy | No button-names question; Check shows *—* |
| Answers clash on one VID/PID | Explained in place (screen 04); *Check the result* disabled until changed |

## Decisions for Alan

These change a settled rule or add a new one. Each needs a yes/no before building.

- **D1. The shortcut keeps entries only other copies have.** Today *use one installation for all* (2026-10-24)
  **overwrites**: it removes the other copies' own entries, with a warning when bindings use one. Proposed:
  *Make everything like my Steam copy* = `answersFavouring` (Steam wins every question, nothing one-sided is
  dropped). Why: the old warning (*"131 bindings … will stop working. Choose Merge to keep it."*) is a choice no
  casual player can make well, and the requirement is that every copy ends up the same, which this meets without
  losing anything. A player who really wants a controller gone removes it in Alias Designer afterwards.
- **D2. "Use Steam's names" for one controller keeps names only Epic has** (the 2026-10-08 per-device rule), rather
  than taking Steam's file whole (2026-10-24). Same reason as D1: labels are harmless to keep.
- **D3. The Plug in step is new.** It is read-only and never blocks. Alternatively it could be a note on Welcome,
  but the live state and the press-to-identify only help if the player sees them before the questions.
- **D4. The DANGER/WARNING/SUCCESS colouring and its legend go.** Every option now says in words what it does.
  Colour is kept for state only: plugged in (success), the not-plugged-in warning line (warning), a failure
  (danger).
- **D5. The "I HAVE READ AND UNDERSTOOD" gate goes.** Its two facts (backed up first; files go to the working
  copy) become the footer line and the Done screen's backup line.
- **D6. Nothing preselected, even where one option is clearly better** (e.g. the name 131 controls use vs. the one
  12 use). Kept as principle 6 requires. A *Recommended* tag on such an option, without selecting it, is possible
  (`.rec` exists in the mockup CSS) if Alan wants it.

## Implementation notes (for Claude)

**The engine is unchanged.** `FirstSetupPlan` already produces exactly the rows this design shows:

| `FirstSetupPlan.Kind` | Where it goes |
|---|---|
| `NAME_DIFFERS` | Question screen 03 |
| `HARDWARE_DIFFERS` | Question screen 04 |
| `LABELS_DIFFER` | Question screen 05 → 06 (`labelMerge(device)`, `labelAnswersFavouring`) |
| `ONLY_IN_SOME` | Check: *Was only in X, added to Y* |
| `IDENTICAL`, `BUILT_IN_LABELS` | Check: *Already the same* |
| `SHADOWED` | Check, *Also tidied up* |

Answers stay the `Map<String, String>` that `unanswered` / `resolve` take; the wizard is a new view over them. The
shortcut is `answersFavouring`; the write is today's `FirstTimeStartup` / `WritePath`; the later check passes the
working copy as one installation-like side, as `FirstSetupDialog` already does.

**UI (`ui.screen.bindings`):**

- Replace `FirstSetupDialog`'s grid with a `ReconcileWizard` dialog: `CardLayout` of Welcome, PlugIn, one
  `QuestionPanel` per question row, Check, Done. A `WizardStepper` header (four steps, painted). `FirstTimeStartupFlow`
  opens it where it opens `FirstSetupDialog` now; the later check (S9) opens it with the working copy as a side and
  without the Plug in step (D3 for the later check: open question 3).
- `ChoiceCard`: a `JToggleButton` in a `ButtonGroup`, painted as the card (border, fill and radio from palette roles:
  `frameBorder`, `tableCellBackground`; selected: `primaryAction` border, `accentFill`). Keyboard: arrows move between
  cards, Space selects. **Enter does not select** (S10).
- `LabelMergeDialog`: restyle rows as radio chips, human input names, the press-to-find highlight, *use all* links.
  Logic unchanged.
- Human input names: a small pure mapper `InputNames.spoken(code)` (`Joy_1` → *Button 1*, `Joy_POV2Up` → *Hat 2,
  up*, `Joy_RZAxis` → *Rotary axis* / per-axis words). Check `bind-editor.md` and `CaptureControllers` for an existing
  one first.
- Live connection and button presses: subscribe to `DeviceService` for the dialog's lifetime only, on the EDT via the
  UI bus; unsubscribe on close. Product names come from what the current *Device (Windows)* column uses.
- Control counts: `BindsDeviceReferences` across every binds file, as the current severity uses.
- Every colour is a `HudPalette` role (`ED_HUD_REFERENCE.md`); the mockup CSS names the role for each colour.
- **Every string is new i18n** in all bundles (`project_i18n_bundles_complete`); watch the MessageFormat apostrophe
  trap (*don't*, *Steam's*, *Let's* need `''`).

**Tests:** a pure presenter (`ReconcileQuestions`: rows → ordered question models with their consequence lines and
counts) is where the logic goes, and it gets unit tests: ordering, the *moved* counts per option, clash messages,
the 0-questions path, 3+ copies, a copy with no `DeviceMappings.xml`. The Swing panels stay thin.

## Copy deck (English source strings)

| Key (suggested) | Text |
|---|---|
| `reconcile.title` | Make your game copies match |
| `reconcile.safe` | Nothing changes until the last step. Everything is backed up first. |
| `reconcile.welcome.headline` | Your {0,choice,2#two\|2<{0}} copies of Elite Dangerous don''t agree about your controllers |
| `reconcile.welcome.body` | You have the game from {1}. They share one set of controls, but each copy keeps its own list of your controllers and button names. Right now the lists are different, so your controls may work in one copy and fall back to keyboard and mouse in another. |
| `reconcile.welcome.counts` | {0} questions for you · {1} things BindForge sorts out by itself · {2} controllers already match |
| `reconcile.welcome.after` | This takes about a minute. When it''s done, every copy will have exactly the same controllers, names and button names, and your controls will work the same whichever one you launch. |
| `reconcile.plugin.headline` | Plug in the controllers you fly with |
| `reconcile.plugin.body` | Elite only loads your controls when every controller they use is plugged in. If one is missing when the game starts, it quietly switches to keyboard and mouse, and it can look as if your bindings were lost. Plug yours in now. This list updates by itself. |
| `reconcile.plugin.tip` | Tip: press any button on a controller and its row lights up, so you know which one it is. A controller you no longer own can stay unplugged; you can carry on without it. |
| `reconcile.q.count` | Question {0} of {1} |
| `reconcile.name.headline` | Your {0} has a different name in each copy |
| `reconcile.name.sub` | Your controls find each controller by a short name. Pick the one to keep. |
| `reconcile.name.uses` | {0} of your controls already use this name |
| `reconcile.name.moves` | Your {0} controls that use "{1}" are moved to it |
| `reconcile.name.note` | Nothing stops working either way. Your controls follow the name you pick. This only decides what the controller is called. |
| `reconcile.hw.headline` | "{0}" means a different controller in each copy |
| `reconcile.hw.sub` | {1} of your controls are set up on {0}. Which controller should they work on? |
| `reconcile.hw.works` | Your {1} "{0}" controls work on this controller |
| `reconcile.hw.unplugged` | It isn''t plugged in. Plug it in before you play, or the game falls back to keyboard and mouse. |
| `reconcile.hw.note` | Not sure? It''s usually the one you still own and have plugged in. The other controller keeps working in the game, just without your "{0}" controls. BindForge offers it a name of its own the next time it is plugged in. |
| `reconcile.labels.headline` | Some buttons on your {0} are named differently |
| `reconcile.labels.sub` | Button names are what the game''s Controls screen shows, e.g. "Boost" instead of "Joy 1". |
| `reconcile.labels.useStore` | Use {0}''s names |
| `reconcile.labels.each` | Choose button by button |
| `reconcile.labels.note` | Button names never change what a button does. Names only one copy has are kept either way. |
| `reconcile.check.headline` | This is what {0,choice,2#both copies\|2<all {0} copies} will have |
| `reconcile.check.write` | Make {0,choice,2#both copies\|2<all copies} match |
| `reconcile.done.headline` | {0,choice,2#Both copies\|2<All copies} now match |
| `reconcile.done.beforePlay` | Before you play: plug in {0}. If a controller your controls use is missing when the game starts, the game switches to keyboard and mouse. |
| `reconcile.shortcut.link` | Make everything like my {0} copy… |

## Open questions

1. D1–D6 above.
2. Does Alias Designer show the product name for an unplugged controller today (the old screen did: *Logitech X56
   Stick (not connected)*), and from where? The design assumes it is always available.
3. Should the later check (screen 10) also get the Plug in step? Proposed: no. The player is mid-session and already
   knows their setup. The Check screen's plugged-in note covers it.
4. Two copies from the **same** store (two Steam libraries): is the drive letter the right way to tell them apart,
   or the folder name?
