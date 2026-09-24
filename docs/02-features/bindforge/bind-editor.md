# BindForge — Bind Editor

The Bind Editor is where the player views and edits their actual key/button/axis bindings. It organises the same underlying binding data through multiple **modes**, each offering a different lens on it. Conflict detection is shared infrastructure used by every mode, not owned by any single one — see [Shared Conflict Detection](#shared-conflict-detection) below.

**Three of the seven modes ship in V1.2: [Game Mode](#game-mode), [Anomalies](#anomalies) and
[Settings](#settings--settled-2026-09-19).** Action Groups,
Input Mode, Control Types and Controller Mode are [deferred to a later release](../../00-overview/v1.2-scope.md#the-v12-release-is-trimmed--settled-2026-09-16),
and their tabs are not in the mode bar at all for V1.2. Their designs stay in this document, which is what
makes them cheap to build when they come back — each mode's own section below carries its status.

## Shell (Common to All Modes)

- A Bindings File dropdown plus Load button, above everything. Load performs the standard [Live File Synchronization](overview.md#live-file-synchronization) freshness check for the Bind Domain before opening the file for editing.
- A Search box, above all mode tabs — filters whichever list is currently showing in whichever mode tab is active. **It clears in one action**: a × appears inside the field once there is anything to clear, and Esc does the same for anyone whose hands are already on the keyboard — which, in a bind editor, is most of them. Clearing restores the full list and returns focus to the field. Controller Mode is picture-based, not a list, so how (or whether) Search applies there is still open.
- A **Show Anomalies Only** checkbox, on the same line as Search. **It belongs to Game Mode and appears only there** (settled 2026-09-15). It filters the grid to rows carrying an anomaly — any of the [four kinds](#four-kinds-one-question), not conflicts alone. Leaving Game Mode hides it **and clears it**, so a filter is never left applied where nothing on screen explains it. **Why it exists when [Anomalies](#anomalies) has its own tab:** the tab answers *what is wrong*, gathering every anomaly by kind and severity away from the game's own layout; the checkbox answers *what is wrong here*, keeping the sections and groups the commander is already working in. *Until 2026-09-15 this was shared shell state that meant something different in each mode. [Action Groups](#action-groups) has no filter of its own as a result, and needs none: it is not where conflicts get fixed.*
- Mode tabs: **Game Mode**, **Anomalies** and **[Settings](#settings--settled-2026-09-19)** in V1.2 (settled 2026-09-16,
  Settings added 2026-09-19). **Action Groups**, **Control Types**, **Controller Mode** and **Input Mode** are
  documented below but [are not in the mode bar](../../00-overview/v1.2-scope.md#the-v12-release-is-trimmed--settled-2026-09-16)
  for that release — not greyed out, not labelled “coming soon”, not present. Nothing on screen promises a
  view that cannot be opened.
- A Context Bar of buttons (All / General / Ship / SRV / On Foot) filtering the grid to one section.
- A collapsible-group binding grid: left-click toggles one group; right-click opens Expand All / Collapse All, scoped to the currently visible groups.
- A roughly 75/25 split between the binding grid and the Binding Editor panel.

## Game Mode

**Status: exists as Krondor's Binding Profile, and grows in phase 3** — corrected 2026-09-13; this line
used to say *built*, which overstated it. Elite-Intel's `BindingProfilePanel` already organises bindings to
match the in-game controls UI — the same four top-level sections and the same group structure the game
uses — with the game's own row names (`BindingDisplayNames`), search, and a show-conflicts-only filter.
See [Binding Zone Map (domain knowledge)](domain-knowledge/EliteDangerous-ActionCatalog.md) for the full,
cross-verified section/subgroup/action inventory.

### What Game Mode still needs

| Addition | Where it is specified |
|---|---|
| Capture for **every device**, through the capture dialog | [Capture Dialog](#capture-dialog--settled-2026-09-13) |
| The 93 **settings entries**, which appear nowhere today | [Settings entries are rows too](#settings-entries-are-rows-too--settled-2026-09-16) |
| The **ASSISTANT** column | [The mechanism: an ASSISTANT column](#the-mechanism-an-assistant-column-in-the-grid--settled-2026-09-12) |
| Conflict colouring on the **slot cell**, not the whole row | [FN-1](#fn-1-in-detail--scoped-2026-09-12) |
| The in-game name and the XML tag in the capture dialog | [What the dialog shows](#what-the-dialog-shows) |

### Settings entries are rows too — settled 2026-09-16

**BindForge is a bind manager, not an assistant accessory.** Alan: *“we are not just facilitating Elite
Intel's need for bindings anymore, this is a full bind management system. that means we need the user to
have access to all the binds the game offers so that they can edit the binds outside of the game. This
includes the “binds” that are just settings, like deadzones and toggles for inverted and other “not an
input” “bind”*.

Two different things wear the word *settings*, and only one of them was already built:

- **Properties of an axis row** — Inverted and Deadzone, which belong to one axis binding and already sit on
  that row in the grid. Unchanged by this.
- **[Standalone-setting-type](../../03-data-models/binding-schema.md#binding-element--three-distinct-kinds)
  entries** — a bare value attached to no control at all, which had nowhere to appear. These are what
  this section adds.

**Measured against `reference-data/Custom.4.2.binds`**, a real 4.2 file holds **515 elements: 352
button-type, 70 axis-type and 93 standalone settings.** The settings are not a rounding error — they are
nearly a fifth of the file, and until now not one of them was reachable outside the game.

**They also have a tab of their own** — [Settings](#settings--settled-2026-09-19) lists every one of them in a single place,
for the commander who knows the game has an option somewhere without knowing which section hid it. Same rows,
same editors, same values.

**Where a settings row goes: with its own subgroup, at the foot of it.** The
[Action Catalog](domain-knowledge/EliteDangerous-ActionCatalog.md) already files every settings entry under a
subgroup — `YawToRollMode` is in **Flight Rotation** beside Yaw Axis, `FreeCamMouseSensitivity` is in
**Free Camera** — so BindForge puts it where the game's own taxonomy already has it, gathered below that
subgroup's bindings under a **SETTINGS** divider rather than mixed among them. Gathered, because a row with
no capture affordance sitting between rows that have one invites a click that cannot work; in its own band,
because moving `Yaw Into Roll` away from the yaw bindings would be BindForge inventing a layout the game
does not use.

**A settings row has no slots, so it has no capture, no Primary/Secondary, no hold chip and no conflict
state.** It cannot collide with anything, and it is never [an anomaly](#anomalies): a value is either set or
left at the game's default, and neither is a fault.

#### The three value shapes, measured

**The complete observed vocabulary of every choice field is
[tabulated separately](reference-data/settings-choice-fields.md)**, generated from 47,898 real files rather
than written by hand: 34 token choices, 3 numeric choices, 45 booleans and 21 free numbers.

| Shape | Count | Editor | Examples |
|---|---|---|---|
| **Boolean** | 36 | checkbox | `DeployHardpointsOnFire` (Firing Deploys Hardpoints), `EnableRumbleTrigger` — every one holds `0` or `1` |
| **Float** | 25 | numeric field | `MouseSensitivity` `1.00000000`, `MouseDeadzone` `0.05000000`, `FreeCamMouseSensitivity` `5.00000000` |
| **Enum** | 31 | dropdown | `YawToRollMode` `Bindings_YawIntoRollNone`, `MouseYMode` `Bindings_MousePitch`; **23 of the 31 hold an empty value**, which is the game's default rather than a missing one |

`KeyboardLayout` is the one entry with no `Value` attribute at all — it carries the layout as element text.
It is read-only: the game writes it from the OS keyboard layout, and nothing a commander does in a bind
editor should claim to change that.

**Floats are a plain numeric field, not a slider.** A slider has to know its range, and nothing in any of the
five specimen files tells us what the maximum sensitivity is. A field writes exactly what the commander
typed, which is the same contract the rest of BindForge keeps with the file.

**Except that three of them are not really floats — found 2026-09-16.** The game's options screen shows
**Headlook Button Increments** as a four-choice dropdown — CONTINUOUS, SMALL INCREMENTS, MEDIUM INCREMENTS,
LARGE INCREMENTS — and the file stores it as `HeadlookIncrement` = `0.00000000`. `ThrottleIncrement` and
`BuggyThrottleIncrement` have the same shape. **A numeric field would let a commander type a value the game
never offers**, so these three are dropdowns like any other enumerated setting, and the number behind each
label is captured by the same testing. *Nothing in the file distinguishes them from a genuine float — the
difference is only visible on the options screen, which is why it took looking.*

**The other 22 are safe to treat as numbers.** Every one of them is a sensitivity, a deadzone or a power
curve — `MouseSensitivity`, `FSSMouseDeadzone`, `HumanoidPitchSensitivity` and their kin — and the options
screen renders that family as a **slider**, confirmed twice on screen — Headlook Sensitivity, and the SRV
Turret Mouse Sensitivity, Deadzone and Power Curve trio.
A slider is a number chosen freely, so a numeric field writes nothing the game would not accept. Only the
**increments** family turned out to be a list wearing a number.

#### What the options screen showed — 2026-09-16

Alan walked the game's own options screen. **Two assumptions did not survive it.**

**A label does not tell you its token.** *UI Focus Mode* reads **DIRECTION** on screen and is stored as
`Bindings_FocusModeHold`. Anything that builds a token from a label, or a label from a token, is
guessing — both directions have to be read from a file the game wrote.

**And one token can wear different labels.** `Bindings_MouseYaw` reads **YAW** under Ship Controls and under
the SRV turret, and **ROTATE** under On Foot Controls — same token, same vocabulary, different word on
screen, because rotating on foot is not called yawing. **So the label table is keyed by element and token
together, never by token alone**, and a shared vocabulary does not imply shared labels.

**An empty value is a choice, not an absence — and which choice depends on the element.** The four
panel-focus entries hold `""` in `Custom.4.2.binds` and `FocusOption_Nothing` in another specimen, and the
screen shows **DOES NOTHING** for both. But `ThrottleRange` is empty and reads **FULL RANGE**, while
`MouseBuggyYMode` is empty and reads **OFF**. Three elements, three different meanings for the same empty
string:

| Element | Value in the file | What the screen reads |
|---|---|---|
| `CommsPanelFocusOptions` | `""` | **FOCUSES THE PANEL** |
| the same field | `FocusOption_Nothing` | DOES NOTHING |
| `ThrottleRange` | `""` | FULL RANGE |
| `MouseBuggyYMode`, `MouseBuggyRollingXMode` | `""` | OFF |

**Empty is sometimes the only way to say a thing.** The panel-focus fields offer three choices on screen and
have only two tokens: *Focuses the panel* is written as `Value=""` and nothing else, proved 2026-09-17 by
setting exactly that option in-game and watching the game replace `FocusOption_Nothing` with a blank while
the three sibling fields stayed as they were. **So writing that choice means writing an empty value, not
removing the element** — an editor that “tidied away” empty attributes would silently change the setting.

**So there is no single rule for empty**, and nothing may normalise it. An empty value is that element's
default choice, whatever the game decided it should be, and BindForge writes back exactly the form the file
already held unless the commander picks something else. *This is also the second reason the vocabularies
have to be captured per element rather than per family: the family tells you the tokens, not the default.*

| Element | In-game name | Choices on screen | Tokens known |
|---|---|---|---|
| `MouseXMode` | Mouse X-Axis | OFF, ROLL, YAW | `Bindings_MouseYaw` = YAW, `Bindings_MouseRoll` = ROLL |
| `MouseYMode` | Mouse Y-Axis | OFF, PITCH, PITCH INVERTED | `Bindings_MousePitch` = PITCH, `Bindings_MousePitchInverted` = PITCH INVERTED |
| `UIFocusMode` | UI Focus Mode | DIRECTION, and CYCLE per the help text | `Bindings_FocusModeHold` = **DIRECTION** |
| `CommsPanelFocusOptions` and its three siblings | Looking at Comms / External / Role / Internal Panel | FOCUSES THE PANEL, SHOWS THE PANEL, DOES NOTHING | `FocusOption_Nothing` = DOES NOTHING, `FocusOption_Show` = SHOWS THE PANEL, **`""` = FOCUSES THE PANEL** — *settled by experiment 2026-09-17; see [the vocabulary table](reference-data/settings-choice-fields.md)* |
| `HeadlookMode` | Headlook Axis Mode | ACCUMULATE, DIRECT | `Bindings_HeadlookModeAccumulate` = ACCUMULATE |
| `HeadlookIncrement` | Headlook Button Increments | CONTINUOUS, SMALL, MEDIUM, LARGE INCREMENTS | stored as a float; `0.00000000` is one of the four |
| `MuteButtonMode` | Mute Button Mode | TOGGLE, PUSH TO TALK, **PUSH TO MUTE** | `mute_toggle`, `mute_pushToTalk`; the third has never appeared in a file |
| `CqcMuteButtonMode` | Microphone State Mode (CQC) | the same three | `mute_pushToTalk` |
| `EnableMenuGroups` | Enable Context Menu In Ship | OFF | a plain `0`/`1`, no capture needed |
| `ThrottleRange` | Throttle Axis Range | FULL RANGE, FORWARD ONLY | **`""` = FULL RANGE** — the empty value is the default choice, not an unset one |
| `BuggyThrottleRange` | Throttle Axis Range (SRV) | the same two | `Bindings_BuggyThrottleForewardOnly` = FORWARD ONLY |
| `BuggyThrottleIncrement` | SRV Throttle Increments | CONTINUOUS, 10%, 12.5%, 16.7%, 25%, and more below the fold | `0.00000000` = CONTINUOUS |
| `MouseTurretYMode` | Turret Mouse Y-Axis | OFF, PITCH, PITCH INVERTED | `Bindings_MousePitchInverted` = PITCH INVERTED |
| `MouseTurretXMode` | Turret Mouse X-Axis | — | `Bindings_MouseYaw` = YAW |
| `MouseBuggyYMode` | SRV Pitch Mouse Y-Axis | OFF, PITCH, PITCH INVERTED | `""` = **OFF** |
| `MouseBuggyRollingXMode` | SRV Rolling Mouse X-Axis | — | `""` = **OFF** |
| `MouseHumanoidXMode` | Mouse X-Axis (On Foot) | — | `Bindings_MouseYaw` = **ROTATE** |
| `MouseHumanoidYMode` | Mouse Y-Axis (On Foot) | OFF, PITCH, PITCH INVERTED | `Bindings_MousePitch` = PITCH |

**Six in-game names came free with it** — Mouse X-Axis and Y-Axis were already in the catalog, but UI Focus
Mode, Headlook Axis Mode, Headlook Button Increments, Mute Button Mode, Microphone State Mode (CQC) and the
four *Looking at* panel entries were not. **Which panel element is which is still inferred**, not observed:
left/right/role/comms against external/internal/role/comms is the obvious mapping and the cheapest thing in
the world to confirm by setting one and reading the file.

**The increments are percentages, and that is the shape of the vocabulary.** *SRV Throttle Increments* offers
CONTINUOUS, 10%, 12.5%, 16.7%, 25% and more below the crop — a series of simple divisions (1/10, 1/8, 1/6,
1/4), with `0.00000000` standing for CONTINUOUS. **So the stored number is almost certainly the fraction the
label names**, which would make these dropdowns cheap to populate and to label. *Almost certainly is not
captured*, though: selecting two of them and reading the file settles it, and until then the reading stays a
hypothesis rather than something BindForge writes.

**Two values confirm each other across the two throttle families.** `BuggyThrottleRange` holds
`Bindings_BuggyThrottleForewardOnly` and the SRV screen reads FORWARD ONLY, while `ThrottleRange` is empty and
the ship screen reads FULL RANGE — so the ship's *Forward Only* token is the one thing still missing from
that family, and the guess `Bindings_ThrottleForewardOnly` is exactly the kind of guess this section exists to
refuse.

**More remain.** Alan: *“here is a bunch of things i have seen that are choices instead of a number or input
capture, there are more”* — the sweep in
[testing-required.md](../../00-overview/testing-required.md) is what finishes the set.

#### An in-game apply re-emits the whole file — observed 2026-09-17

Four saves on Alan's live file in five minutes, watched from the outside:

| Time | What he did | What the file did |
|---|---|---|
| 22:29:14 | set Comms panel focus to *Shows the panel* | that field changed, its three siblings did not |
| 22:30:33 | pressed apply with **nothing changed** | **the file was rewritten anyway**, with every value identical |
| 22:31:40 | set it back to *Does nothing* | that field changed, siblings again untouched |
| 22:34:50 | pressed apply with a field **already** on the value he wanted | file rewritten; that field read `Bindings_MouseYaw` |

**The 22:30 save is the useful one.** The game rewrote a file it had no changes for, which means an apply
**serialises the game's own model** rather than patching the text in place. Two consequences:

- **Every value in the file after an apply is what the current build writes**, not a value that happens to
  have survived. That is what settled the On Foot token without changing anything: the field was re-emitted,
  and it came out `Bindings_MouseYaw`.
- **A single-field experiment is conclusive**, because the siblings prove the rewrite is faithful rather than
  wholesale-defaulting: three panel-focus fields kept their values through all four saves.

*This is the same normalise-on-save behaviour that [strips XML comments](#action-groups) — seen from a different
angle, and it is the reason BindForge's own
[freshness check](overview.md#freshness-checks) cannot trust a file's modification time.*

#### Frontier documents most of them, in the game folder — found 2026-09-16

**`ControlSchemes\Help.txt` ships with the game**, beside the preset files, and its *Options* section lists legal values.
An identical 6,230-byte copy sits in every install on Alan's machine — Steam Horizons, Steam Odyssey and
Epic Odyssey — so it is not a stray file or a mod. This is the answer to *“is this written down anywhere”*:
**it was in the game folder the whole time.**

| Setting | Values Frontier lists |
|---|---|
| `YawToRollMode`, `YawToRollMode_Landing` | `Bindings_YawIntoRollNone`, `Bindings_YawIntoRollTime`, `Bindings_YawIntoRollLowRoll` |
| `ThrottleRange` | `""` and `Bindings_ThrottleForewardOnly` — **confirming empty is FULL RANGE** |
| `UIFocusMode` | `Bindings_FocusModeHold`, `Bindings_FocusModeToggle` — so CYCLE is *Toggle* |
| `HeadlookMode` | `Bindings_HeadlookModeDirect`, `Bindings_HeadlookModeAccumulate` |
| `GunsightSystem` | `Bindings_TraditionalGunsights`, `Bindings_TrailingGunsights` |
| `MouseSensitivity` | any number greater than 0.0 |
| `MouseDeadzone` | any number between 0.0 and 1.0 |
| `ThrottleIncrement` | **any number between 0.0 and 1.0** |

**A second source, in the same folders: 90 shipped preset files.** Scanning every `.binds` Frontier ships
(`SaitekX52.binds`, `KeyboardMouseOnly.binds`, `DualShock4Controller.binds` and the rest) yields tokens no
commander file held — `FocusOption_Show`, `Bindings_YawIntoRollTime`, `Bindings_ThrottleForewardOnly` and
`Bindings_TraditionalGunsights`. **Frontier's own files are the cheapest evidence there is**, and BindForge's
testing should start there before anyone touches the options screen.

**What the file does not cover, because it predates the settings it is missing.** It documents an older
`MouseMode` with values like `Bindings_MouseAbsPitchRoll` that no current file uses — today's `MouseXMode` and
`MouseYMode` are not in it, nor are the panel-focus family, the mute modes, or the headlook increments.
*Which is a warning about the file as much as a gift: it is documentation the game has outgrown in places, so a
value it lists is evidence, not a guarantee, and a setting it omits is not thereby invalid.*

**The mouse family looked settled, and was not — corrected 2026-09-17.** The ship's X dropdown offers exactly
OFF, ROLL, YAW and its Y dropdown OFF, PITCH, PITCH INVERTED, each with a known token or the empty default.
But On Foot carries **tokens of its own** — `Bindings_MouseHumanoidYawRotate` and
`Bindings_MouseHumanoidPitchRotate`, found in six files out of 48,354, beside the plain `Bindings_MouseYaw`
that 33,847 files hold. **Two tokens reach the same choice.**

**Today's build writes `Bindings_MouseYaw` — settled 2026-09-17.** With On Foot's *Mouse X-Axis* showing
ROTATE, Alan applied in-game; the game rewrote the file and that field came back `Bindings_MouseYaw`. Because
[an apply re-emits the whole file](#an-in-game-apply-re-emits-the-whole-file--observed-2026-09-17), the value
after a save is what the current build writes rather than a leftover — so `Bindings_MouseHumanoidYawRotate` is
a legacy or short-lived token, not current, which is also what six files in forty-eight thousand suggests.
**The vocabularies are now closed.**

**`ThrottleIncrement` is the interesting disagreement.** Frontier's file says *any number between 0.0 and 1.0*,
while the modern options screen offers a fixed list — CONTINUOUS, 10%, 12.5%, 16.7%, 25% and more. Both are
true: the field accepts any fraction, and the UI offers a chosen few. **BindForge offers the UI's list**, because
matching what the commander sees in the game is the entire point of the editor — and the list's values are
what the remaining testing captures.

#### A third source: EDRefCard's repository — 2026-09-16

[EDRefCard](https://github.com/richardbuckle/EDRefCard), the community reference-card generator, keeps **152
`.binds` files in its public repository** — Frontier's shipped defaults for four game versions (3.3, 3.5, 4.0a
and Odyssey patch 8) plus around two dozen real commander files kept as test cases. Scanning them closed
almost everything that was left, and **one result would have made a reasonable guess wrong**.

| Finding | Why it matters |
|---|---|
| `ThrottleRangeFreeCam` = **`Bindings_ThrottleForewardOnlyFreeCam`** | it has its **own token**, not the `Bindings_ThrottleForewardOnly` its siblings use. A family does not guarantee a shared vocabulary — the exact assumption this section refuses to make |
| `ThrottleIncrement` = `0.10000000`, `HeadlookIncrement` = `0.25000000` | **the increments are the fraction the label names** — 10% and 25%. The percentage reading is now observed, not hypothesised |
| `UIFocusMode` holds both `Bindings_FocusModeHold` and `Bindings_FocusModeToggle` | confirms [Help.txt](#frontier-documents-most-of-them-in-the-game-folder--found-2026-09-16) against files in the wild |
| `HeadlookMode` holds both `...Accumulate` and `...Direct` | same |
| panel focus: `FocusOption_Nothing`, `FocusOption_Show` | two of the three; **FOCUSES THE PANEL** is still unseen |
| mute: `mute_toggle`, `mute_pushToTalk` on both settings | two of the three; **PUSH TO MUTE** is still unseen |

**What that leaves is two tokens.** The value behind *Focuses the panel*, and the value behind *Push to mute*.
Both are one selection each in the game, and until they are observed BindForge offers the options it can
write and says why — `FocusOption_Focus` and `mute_pushToMute` are the obvious guesses, and
`Bindings_ThrottleForewardOnlyFreeCam` is why obvious guesses are not written to a commander's file.

**Where these came from matters.** All three sources so far — the game folder, Frontier's 90 shipped presets,
and a public source repository — were free to read. [edrefcard.info](https://edrefcard.info/list) lists 3,888
commander-shared configs and would be a far richer sample, but **its `robots.txt` allows `/list` and disallows
everything else, including `/configs/`**, so those files are not ours to crawl. Recorded here so the question
is settled rather than rediscovered.

#### A fourth source: 48,354 community configs — 2026-09-17

Alan holds a corpus of **48,354 commander-shared `.binds` files**, scanned read-only and diffed against
every value the earlier sources held.

**The version attributes name the writer, not the game — corrected 2026-09-17.** An earlier draft of this
section read a *4.5* in the corpus as a game version newer than any specimen. Alan challenged it, and he was
right: **116 of the 117 files reading `MajorVersion="4" MinorVersion="5"` are `PresetName="GameGlass"`**, a
third-party touchscreen controller app that writes its own preset and stamps its own number. Every version
above 4.2 is the same story — `4.4` is one file named `EliteDangerousBinds`, `6.1` is two named `Virpil`,
`3.1` and `3.2` are `HCS ...` files from a voice-pack vendor, and `5.0` is a handful of hand-named customs.
**So the version attribute identifies whatever tool last wrote the file, and BindForge must not gate any
behaviour on it.**

| Version reading | Files | What it actually is |
|---|---|---|
| 4.2 | 25,681 | the current game |
| 4.1, 4.0 | 17,688 | recent game versions |
| 3.0, 2.x, 1.x | 3,100-odd | genuinely old game files, back to `1.0` |
| 4.3, 4.4, 4.5, 5.0, 6.1 | 141 | **third-party tools and hand edits** |
| *absent entirely* | **1,225** | 1,078 using the older `<Root PresetName=… SortOrder="0">` schema, and 147 a bare `<Root>` |

**The missing-version case is the one that matters**, being nine times more common than every odd version put
together: a file legitimately has no `MajorVersion` or `MinorVersion` at all, so reading them must be
optional rather than assumed.

| What it found | Detail |
|---|---|
| **`mute_pushToMute`** | 191 files on `MuteButtonMode`, 122 on `CqcMuteButtonMode` — the last unknown token, and the guess was right |
| **`Bindings_ThrottleFullRange`** (23) and **`Bindings_ThrottleFullRangeFreeCam`** (6) | full range has an **explicit token as well as empty**, so empty is a default rather than the only representation |
| **`Bindings_MouseHumanoidYawRotate`**, **`Bindings_MouseHumanoidPitchRotate`** (6 each) | On Foot carries its **own tokens** beside the shared `Bindings_MouseYaw` / `MousePitch` |
| `FocusOption_Show` appears **only on `CommsPanelFocusOptions`** (4,679) | the left, right and role panels never hold it: **vocabularies differ inside a family** |
| `Bindings_YawIntoRollLowRoll` (271), `...Time` (2,540) | all three yaw-into-roll values confirmed in the wild, on `_FAOff` and `_Landing` too |
| `UIFocusMode`, `HeadlookMode` | both values of each, at scale — 1,862 and 3,262 files hold the non-default |

**The increment vocabularies are now complete, and they differ per element:**

| Element | Values in the corpus | The labels they match |
|---|---|---|
| `ThrottleIncrement`, `BuggyThrottleIncrement` | `0`, `0.10000000`, `0.12500000`, `0.16666667`, `0.25000000` | CONTINUOUS, 10%, 12.5%, 16.7%, 25% |
| `HeadlookIncrement` | `0`, `0.25000000`, `0.33333334`, `0.50000000` | CONTINUOUS, SMALL, MEDIUM, LARGE — exactly four |

**Two things written on 2026-09-16 were wrong, and are corrected above.** *Empty does not mean DOES NOTHING*
on a panel-focus entry: the preset in the screenshots holds `FocusOption_Nothing`, which is what the screen
was reading, and no `FocusOption_Focus` exists anywhere in 48,354 files — so empty is most likely **Focuses
the panel**, and is the one thing still worth one selection to confirm. *And the mouse family was not closed*:
On Foot has tokens of its own, found in six files out of forty-eight thousand.

##### Real files are messier than any specification

The corpus is the first evidence of what BindForge will actually be handed, and it is not clean:

- **Values the game never wrote.** `Bindings_ThrottleRangeFullRange`, `Bindings_ThrottleRange_ForwardOnly`,
  `Bindings_ThrustForwardOnly`, plain `Forward`, and `Bindings_MouseRole` — a typo of *Roll* — one file each.
  Hand-edited, and the game evidently tolerated them.
- **A binding token in a settings field.** Panel-focus entries holding `1`, or `Joy_POV1Up` — someone pasted a
  button where a choice belongs.
- **Eight files carry the game's *other* settings** — `Language`, `PilotIsFemale`, market filters, route
  plotting, HUD marker toggles — under a `.binds` extension.
- **437 files have no `<Root>` element at all.** Measured: **363 are plain-text lists of preset names**, the
  shape of a `StartPreset` file rather than a binds file, saved with the wrong extension; 47 are some other
  XML; and 27 are HTML pages, which are artifacts of however the corpus was collected rather than anything a
  commander's game produced. **A bind editor will be handed files that are not bind files** — by far the most
  likely being the `StartPreset` that sits beside them in the same folder.
- **Structurally wrong files**: `Deadzone`, `Inverted`, `ToggleOn`, `Hold` and `Modifier1` appearing as top-level
  elements, where they belong inside a binding.
- **Decimal formatting varies**: `0.00`, `0.0000`, `0.10000` alongside the usual eight places.

**What that requires of BindForge**, none of which was written down before: a file that contains a value no
vocabulary knows **still opens**; the unknown value is **shown as it is and preserved on write**, never silently
corrected to the nearest legal token; an element BindForge does not recognise is **left alone rather than
dropped**; and none of this counts as damage, because the commander's game has been reading these files
quite happily.

##### A `.binds` name does not make it a binds file — 2026-09-17

**445 of the 48,354 files — 0.9% — carry a `.binds` extension and hold something else entirely.**

| What is inside | Files | What it really is |
|---|---|---|
| Plain text, a few preset names per line | **363** | a **`StartPreset.start`** — the same shape as the specimen in `reference-data/` |
| `<ActionMaps>` | 10 | **another game's** keybindings (Star Citizen's format) |
| `<Bindings>`, `<FastEventsMapping>`, `<inputmap>`, `<profile>` | 16 | other tools' input configs and device profiles |
| `<KeyboardLayout>` | 2 | a real binds file whose `<Root>` element has been truncated away |
| `<plist>`, `<FilesMatch>`, `<li>`, hand-made XML | ~14 | a macOS property list, an Apache config snippet, HTML fragments, one-offs |
| `<!DOCTYPE html>` | 27 | artifacts of how the corpus was collected, not commander files |

**The 363 are the ones that matter, and they are not a mystery:** `StartPreset.start` lives in the **same
folder** as the `.binds` files, with a similar name and a similar job, so it is the obvious wrong file to
grab. A commander sharing their bindings picks it by accident — 363 times out of 48,354.

**So BindForge identifies a file by its content, not its name.** On load it checks for a `<Root>` element
carrying binding children, and when that is absent it **says what the file looks like instead** — *“this is a
Start Preset list, not a bindings file”* beats *“could not parse file”, and beats an editor that opens empty. The two
truncated binds files argue the same point from the other side: a file can be *nearly* right and still not be
loadable.

**And it never writes over one.** A file BindForge cannot identify is left exactly as it is: not repaired, not
normalised, not replaced with a binds file wearing the same name. Someone's `StartPreset`, or another
game's configuration, is not BindForge's to rewrite.

*Open, and Alan's call: whether an unrecognised settings value earns a row in [Anomalies](#anomalies).* It fits
the *[Invalid](#four-kinds-one-question)* kind by meaning — it will not do what the commander expects — but
[settings are currently never anomalies](#settings-entries-are-rows-too--settled-2026-09-16), and the whole
class is five files in forty-eight thousand.

#### Enums need their vocabularies captured first — settled 2026-09-16

An enum's value is a **token naming one choice from a fixed set**, the way the game writes the option the
commander picked from a dropdown: `Bindings_MouseYaw` means the mouse's X axis yaws, `Bindings_MouseRoll`
means it rolls. **The problem is that a file only ever contains the token it happens to hold.** Across all
five specimens, `YawToRollMode` is `Bindings_YawIntoRollNone` every single time — whatever the game calls its
on-states, no file we have has ever contained one.

| Element | In-game name | Tokens observed |
|---|---|---|
| `MouseXMode`, and the whole mouse-axis family | Mouse X-Axis | `Bindings_MouseYaw`, `Bindings_MouseRoll` |
| `MouseYMode`, and its family | Mouse Y-Axis | `Bindings_MousePitch`, `Bindings_MousePitchInverted` |
| `MuteButtonMode` | — | `mute_pushToTalk`, `mute_toggle` |
| `YawToRollMode` | Yaw Into Roll | `Bindings_YawIntoRollNone` **only** |
| `LeftPanelFocusOptions` and the other panel-focus entries | — | `FocusOption_Nothing` **only** |

**So the dropdowns are populated from testing, not from invention** — see
[testing-required.md](../../00-overview/testing-required.md). Walk each dropdown in the game's options screen,
save, and read the token back out of the file. **If that testing has not happened by release, enums ship
read-only** with their current value shown and the game's own options screen named as where to change them;
booleans and floats do not depend on it. Writing a token nobody has ever seen the game write is the one
outcome to avoid: the game would discard it silently, with BindForge's fingerprints on the file.

### Conflict Display

Carried forward from a proven, already-real pattern, not invented fresh:
- Conflicting rows render in a distinct warning colour.
- Hovering a conflicting row shows a **persistent** popup (not an auto-dismissing tooltip, which vanishes too fast to read a full explanation) listing every other action sharing that key.
- The [**Show Anomalies Only** checkbox](#shell-common-to-all-modes) sits beside Search and **appears only
  here**, filtering the grid to rows carrying an anomaly — any of the [four kinds](#four-kinds-one-question),
  not conflicts alone.

### Capture Dialog — settled 2026-09-13

**BindForge captures through Krondor's existing dialog, grown — not a new one.**
`AssignKeyboardBindingDialog` already does the hard parts: it captures a chord by having the commander press
it, colours an on-screen keyboard free and used, and warns about conflicts, reserved chords and the game-menu
key *before* anything is kept — leaving all validation and writing to `BindingsWriter`. It already dims the
window behind it, too. The earlier design, a floating popup *"modelled directly on the base game's own rebind
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
cannot be named at all. *FN-1 was fixed 2026-09-20, so a binding in either slot can now be found; the record
still holds one name, and making it a list is what remains.*

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

**Status: designed in full, and deferred to a later release (2026-09-16)** — see [The V1.2 release is
trimmed](../../00-overview/v1.2-scope.md#the-v12-release-is-trimmed--settled-2026-09-16). It was promoted into V1.2 scope on 2026-09-08 and designed out from there,
including the open items settled 2026-09-16 below; **its tab is not in the V1.2 mode bar**. Everything
recorded here stands and is what gets built when the mode returns. Groups bindings by **player intent** rather than game category — for example, "Move Left" spans Ship, SRV, On Foot, and General Controls simultaneously; Game Mode shows these in four separate sections, Action Groups shows them together in one group.

- **Action Groups** — each binding belongs to at most one action group. (A binding can't logically serve two physical intents at once — if it did, both groups would require the same key, which is itself a conflict.)
- **Default Groups** — **ship with the app** (settled 2026-09-16): an application resource, read-only, no
  rename/edit/delete, versioned with Elite-Intel like any other shipped resource. The exact default set is
  intentionally left undefined until BindForge's binds-file audit is complete.
- **User Groups** — fully editable and deletable, stored persistently.
- **Ungrouped is the default state of every binding** (settled 2026-09-16). Nothing is grouped until someone
  groups it; the shipped default groups are the only exception, and a control Frontier adds in a game
  update arrives ungrouped exactly as every existing control started out. Ungrouped bindings do not appear
  in **View Groups**, which lists groups — but they are all in **Manage Groups'** ALL BINDINGS list,
  which is complete, and in [Game Mode](#game-mode). *Earlier wording called this “hidden in Action
  Groups”, which read as though some bindings were unreachable.*

**View Groups tab:** a left panel lists groups (default groups with a lock indicator first, then user groups); a right panel shows only that group's bindings with its own real-time search. Clicking a row opens the same Binding Editor panel Game Mode uses.

**Manage Groups tab:** a three-column layout — **ALL BINDINGS**, add/remove controls, and the group
list with the selected group's members — plus "Assign Control to Group," which applies one capture to
every member of a group at once.

**ALL BINDINGS is Game Mode's list, minus the editing columns — settled 2026-09-16.** Alan: *“the
'all bindings' list should be exactly the same as the list on the game mode tab, minus all the extra
columns”*. Same sections, same groups, same rows, same order, showing each control's name and its Primary
and Secondary values; the shell's Search reaches it. What comes out is everything belonging to *editing* a
binding — the [ASSISTANT column](#game-mode), the capture buttons, the tap/hold chips, the axis inverted
and deadzone controls — because the list has one job: *“selecting an individual binding and putting it
in to a player designed group”*. Anomaly decoration comes out too, for the same reason anomaly filtering
is not offered here. A binding already in a group is shown dimmed, naming that group, so the one-group
rule is visible before the add button has to refuse it. **One list, one model:** Game Mode and this list
read the same binding set, so they cannot drift.

**Assign Control to Group — it writes Primary, and confirms any overwrite (settled 2026-09-16).**
The capture lands in each member's **Primary** slot, always — never Secondary, and never
“whichever slot is free”. A group is a statement about intent, so its members should hold the control in the
same place; picking a slot per member would make what the group did depend on what each member happened to
have. **Any existing Primary value is an overwrite and must be confirmed**, with the value at stake named. A
commander who wants the group's control in Secondary slots moves it there per binding in
[Game Mode](#game-mode) — which also means the *old* Primary is never silently displaced.

**Confirmation semantics (resolved):** the capture is attempted against every member binding in the group. For any member where applying it would either overwrite an existing, different value, or create a new conflict with a binding *outside* the group, that member gets its own row in one consolidated confirmation dialog — modelled on the base game's own "already bound" warning, but covering every affected member at once rather than one dialog per binding. Each row shows whatever is actually at stake for that specific member (the existing value being overwritten, the new conflict it would create, or both together when both apply) with its own toggle so the player can confirm or skip that member individually — applying to the rest of the group is never all-or-nothing because one member happens to be contested. Members with no existing value and no new conflict apply immediately, with nothing to confirm.

**Conflict prevention:** adding a binding already in another group is blocked, with an explicit warning
naming both groups — and the ALL BINDINGS row is dimmed beforehand, so the refusal is not the first the
commander hears of it.

**A member with nothing bound is shown (settled 2026-09-16)** — listed in its group with an empty value,
not hidden. Hiding it would make the group misreport itself: a commander reading “Move Left” wants to know
that the On Foot member has nothing on it, and that is precisely the member Assign Control to Group applies
to with nothing to confirm. It is also [an anomaly worth reporting](#four-kinds-one-question) when
Elite-Intel drives the control — reported in Game Mode and [Anomalies](#anomalies), where anomalies are
handled.

**No anomaly filtering here — settled 2026-09-15.** Alan: *"action groups aren't really a place to fix conflicts."* This is a curated view of **intent**, while a conflict is a property of a **chord**, and the commander resolves one in [Game Mode](#game-mode) or [Anomalies](#anomalies) where the chord and its rivals are both in view. So the shell's [Show Anomalies Only checkbox](#shell-common-to-all-modes) does not appear in this mode, and no equivalent of its own is planned.

**Conflict state still shows here**, from the [shared detection service](#shared-conflict-detection): a group holding a binding that clashes with something outside it is still marked, because noticing is useful even where fixing is not. *An earlier design filtered the group list down to exactly those groups, driven by the shell checkbox; that went with the checkbox.* Group-internal duplicates cannot arise in any case, since a binding belongs to at most one group.

**Persistence — resolved by direct testing:** an early design considered marking group membership using comments inside the `.binds` XML file itself. Testing confirmed the game silently strips every XML comment on load/rewrite (while every actual binding value survives intact) — a well-behaved normalise, not a lossy one, but one that rules out this storage approach. **Consequence: group-membership metadata must live in BindForge's own data store, never in `.binds` file comments.**

**Prerequisites:** the binds-file audit complete; Game Mode stable; a persistence design for user groups.

### Open items — carried from the punch list, 2026-09-09

Five design questions this section used to raise and not answer. **Four were settled 2026-09-16** and are
written up above; this list keeps the record of what was asked and what the answer was.

- ~~**“Assign one control to every group member at once” — behaviour undesigned.**~~ **Settled:** it writes
  **Primary** on every member and confirms any overwrite, and it validates conflicts against bindings outside
  the group.
- ~~**Ungrouped actions after a game update.**~~ **Settled, and the premise was wrong:** ungrouped is the
  default state of every binding, not a state a game update pushes things into, and ALL BINDINGS lists every
  binding there is. New Frontier controls arrive ungrouped like everything else started out, and are
  reachable in exactly the same place. No notification is needed for a control that is where all controls
  begin.
- ~~**Can default groups update independently of an app release?**~~ **Settled: no.** Default groups ship
  with the app as a versioned application resource, so a change to them is an Elite-Intel release. Fetching
  group definitions out of band is not planned.
- ~~**Completely unbound bindings — shown in their group, or hidden?**~~ **Settled: shown**, with an empty
  value.
- **Storage schema for user groups — still open.** Falls out during implementation. A new table means a new
  `02XXX` migration (v1.2's block, corrected 2026-09-22), and an applied migration is never edited. What has to persist is now settled: group
  name, ordered membership, and whether the group is a shipped default or the commander's own.
## Input Mode

**Status: designed in full, and deferred to a later release (2026-09-16)** — see [The V1.2 release is
trimmed](../../00-overview/v1.2-scope.md#the-v12-release-is-trimmed--settled-2026-09-16). **Its tab is not in the V1.2 mode bar.** The design below is complete and
unchanged: it is the cheapest of the deferred modes to build, since its rows are Game Mode's rows and its
capture is the shared dialog. Input Mode is an input-first reverse lookup — press a physical control, and see everything bound to it across every section — the mirror image of Game Mode's action-first view.

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
| **Missing** | nothing is bound to it — or it is bound only to something Elite-Intel cannot press. **Two shapes**, see below | absence, not breakage |
| **Conflicts** | two actions sharing one input | the original category |
| **Invalid** | bound, unique, uncontested, and still will not fire | something else consumes the input first in a given game state |

#### Missing has two shapes — added 2026-09-12

**The two shapes are scoped differently, settled 2026-09-16.** Shape one is **every control in the file with
nothing bound** — BindForge manages the commander's bindings, not Elite-Intel's subset of them, so an
unbound control is a fact about their file whoever was going to press it. Shape two stays scoped to the
controls Elite-Intel drives, because *“Elite-Intel cannot press this”* means nothing about a control it
never presses.

**Measured, so the size is known rather than assumed:** `DualVirpilDawnTreader.4.1.binds` has **128**
controls with nothing bound out of 396; `Custom.4.2.binds` has **245** out of 422. That is the length of the
MISSING list on a real file, and the [ASSISTANT chips](#the-mechanism-an-assistant-column-in-the-grid--settled-2026-09-12)
are what separate the handful that stop the assistant working from the rest of it. *This replaces the
“narrow versus broad” open item, which asked whether the broad count should be shown at all.*

**[Settings entries](#settings-entries-are-rows-too--settled-2026-09-16) are never Missing.** They hold a value or the game's default, and
neither is an absence — 23 of the 31 enums sit empty in a perfectly healthy file.

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
assistant can use it too"*, never *"replace your stick binding"*. Until 2026-09-20,
[FN-1](#fn-1-in-detail--scoped-2026-09-12) made the scanner blind to the very Secondary slot this remedy
writes to. It is fixed, so the remedy can now confirm its own work: a keyboard binding added in Secondary is
scanned like any other.

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

**The mode tab's badge counts every anomaly, all four kinds** (settled 2026-09-16) — the number on the tab
is the number of rows behind it, which is the only version of the badge that never lies. **It is a large
number, and that is honest:** with Missing covering every unbound control, `Custom.4.2.binds` produces 245
Missing beside 1 Reserved, 4 conflict groups and 2 Invalid — a badge of 252. A commander who has bound
their whole file sees a small one. *An earlier draft had this counting conflicts alone, which predates the
rename.*

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

**This ordering is what keeps the ALL tab usable now that Missing is the whole file.** Missing outnumbers
everything else several hundred to one, and sits fourth regardless — so a reserved key and a blocking
conflict are still the first things on screen, with the long tail below them. The ordering existed before
the MISSING list grew; growing it is what makes it load-bearing.

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

### What a row offers — settled 2026-09-16

Anomalies is where the commander reads the list, so it is where they act on it. Every row opens the
[capture dialog](#capture-dialog--settled-2026-09-13) in place, and a **MISSING** row can also be marked
**ON PURPOSE** from here.

**Marking from the tab rather than only from the grid** is the difference between silencing a control and
hunting for it: a commander working down a list of 245 unbound controls, deciding which ones they are
never going to bind, would otherwise have to find each one in Game Mode to say so. The mark is [the same
one the ASSISTANT column sets](#the-mechanism-an-assistant-column-in-the-grid--settled-2026-09-12) — one
state, one table, two places it can be set — and the row leaves the MISSING list as soon as it is set,
which is the feedback that the mark took.

**Marking never writes to `.binds`**, here or anywhere: it is a row in BindForge's own database, and clearing
it puts the control back on the list.

### Open items — carried from the punch list, 2026-09-09
- **Which voice command depends on this binding?** Selecting a missing control ought to name the
  command(s) that stop working without it — far more useful than the action's own name. No design, and it
  reaches into territory `BindingsMonitor` deliberately avoids: it does not consult the custom-command
  registry, precisely so `ai.hands` never points back at `ai.brain`. Any design here has to respect that
  boundary or move the lookup somewhere that legitimately sees both.

### Conflicts (the kind)

**Status: in scope for V1.2, and designed.** Surfaces the [Shared Conflict Detection](#shared-conflict-detection) service's output directly, rather than requiring the player to spot conflict colouring row-by-row inside Game Mode. The conflict count appears on the CONFLICTS sub-tab; the mode tab's own badge [counts every anomaly](#structure).

**Layout:** conflicts are grouped by the shared input causing them — each group header names the shared key/chord and how many binds share it, expandable/collapsible the same way Game Mode's grid groups work. Inside a group, each row is tagged with its section plus the action name — the same row shape Game Mode uses, so a conflict reads the same wherever the commander meets it. *Until 2026-09-16 this sentence pointed at Input Mode's list, which is now [deferred](../../00-overview/v1.2-scope.md#the-v12-release-is-trimmed--settled-2026-09-16).*

**Editing from here:** clicking a row opens the same [capture dialog](#capture-dialog--settled-2026-09-13) used everywhere else, right in place — resolving an anomaly never requires leaving the Anomalies tab and jumping to Game Mode.

## Settings — settled 2026-09-19

**Status: in scope for V1.2.** A third mode tab, after Anomalies, listing **every [settings entry](#settings-entries-are-rows-too--settled-2026-09-16) in the file at once** — all 93 of
them in a real 4.2 file, grouped the way the game groups them. Alan: *“in the game mode the settings show
inline with the binds. but we could also make it easy to see them all at once”*.

**Both places, and the split is one this editor already makes.** [Anomalies](#anomalies) gathers every
anomaly away from the game's layout while the [Show Anomalies Only checkbox](#shell-common-to-all-modes)
answers the same question inside it; settings divide on exactly that line. **Inline rows answer *what is set
here*** while the commander is working in a group — mouse sensitivity sitting with the mouse bindings it
affects. **This tab answers *what can I set*** — which is the question a commander arrives with when they know
the game has an option somewhere and do not know which section hid it.

**One model, two views.** The tab renders the same rows with the same editors — checkbox, numeric field,
dropdown — reading the same values. Nothing is editable here that is not editable there, and a change in
either is the same change. *This is the same relationship [ALL BINDINGS](#action-groups) has with Game Mode's
grid, for the same reason: two lists that can drift are two bugs waiting.*

**What it does not carry.** No [ASSISTANT column](#the-mechanism-an-assistant-column-in-the-grid--settled-2026-09-12) — Elite-Intel
drives controls, not sliders. No capture button, no Primary/Secondary, no conflict state: a setting has no
input to collide with. The shell's Search reaches it like every other list.

**It fits a trimmed release because it costs almost nothing.** The editors, the value shapes and the
[vocabularies](reference-data/settings-choice-fields.md) are all built for the inline rows already. This tab is
a second view over them — which is the only kind of tab worth adding to a mode bar that was just cut to
two.

### Where an unrecognised value shows — and what it settles

**A settings value no vocabulary knows shows *here*, on its own row, marked as unrecognised and preserved
exactly as written.** That was left open on 2026-09-17, when the only candidate home was [Anomalies](#anomalies):
it fits the *[Invalid](#four-kinds-one-question)* kind by meaning — the game will discard a token it does not
know — but putting five files in forty-eight thousand into the tab a commander opens to find broken bindings
would be a poor trade. **A Settings tab gives it the obvious home instead:** beside the value, where the
commander can see what it is and choose a real option, rather than in a list of things that stop the game
working.

The rule underneath is unchanged: BindForge **shows the unknown value and writes it back untouched** unless
the commander picks something else — see
[real files are messier than any specification](#real-files-are-messier-than-any-specification).

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
- Modifier order does not affect the game's own conflict recognition — two captures of the same key set in different orderings are correctly recognised as the same combination.
- A binding matches by its exact chord (main key plus exactly its modifier set). Holding extra modifiers does not trigger a binding with fewer of them, and a bare key and a modified chord on the same key are two fully independent bindings that both fire — they do not suppress each other.
- Elite's own conflict-checking is genuinely context-aware, operating at two levels:
  - **Macro level (top-level sections):** the four sections (General/Ship/SRV/On Foot) are *assumed* isolated from each other for conflict purposes, but this is **not fully confirmed** — there is at least one unconfirmed observation of an apparent cross-section conflict between Ship and SRV during testing. If real, a cross-section conflict matrix would be needed in addition to the four per-section matrices that exist today. This is the single most important open item in BindForge's conflict-detection design — see [conflicts-and-open-questions.md](../../00-overview/conflicts-and-open-questions.md).
  - **Micro level (within a section):** conflict relationships between subgroups are **not predictable from category alone** and had to be tested pair by pair. Two confirmed examples of the game's own conflict-checking being wrong relative to real gameplay exclusivity: Multi-Crew conflicts with nearly the entire main Ship cluster even though a player can never simultaneously be in their own cockpit and a crewmate's seat — a confirmed Frontier bug that BindForge's own scanner should **not** mirror; conversely, SRV's Driving Turret Controls subgroup is correctly isolated (only conflicts with Driving Mode Switches), matching the same real-world exclusivity logic correctly. On Foot Controls, among the subgroups tested so far, is fully connected — every tested subgroup conflicts with every other one.
- **A real conflict, for BindForge's own scanner, means:** the same key/chord **and** both actions sit in subgroups the tested conflict matrices mark as conflicting (or, for an untested pair, treated conservatively as a conflict until tested). This is deliberately neither "same key anywhere in the file" (too broad — would flag Multi-Crew-style false positives project-wide) nor "same key within the same top-level section" (too coarse — would miss subgroups that are correctly exempt from their section's main cluster).
- **The base game's own rebind dialog is a confirmed-incomplete source of truth:** when a key is shared by three actions, the dialog names only the most recently bound one, never the others — direct proof the game's own conflict UI only reports the most recent collision, not every existing one. This justifies BindForge's own scanner listing every conflict, once the scanner itself is proven correct (see below).
- **A collision between one action's Secondary slot and another action's Primary slot is a real, confirmed conflict** — any scanner comparing only each action's single "winning" slot will systematically miss these.
- **The game does not consider a hold-bound action and a tap-bound action sharing a key to be conflicting at all.** Hold/Tap identity must be folded into whatever defines "the same combination" for conflict-comparison purposes — an earlier scanner that ignored this produced a confirmed false positive.
- **FSS-mode-entry nuance:** the action that enters FSS scanning mode genuinely conflicts with other ship actions sharing its key — a blanket rule that suppresses every scan-related action as "always safe" is wrong for at least this one action, even though the FSS subgroup as a whole is correctly isolated from the rest of Ship Controls at the macro level. The nuance is that subgroup-level isolation does not guarantee every individual action within it is exempt from every possible collision.
- **UI-action-vs-ship-action assignment is allowed by the game with no warning, but this only answers "can this be assigned," not "is it behaviorally safe when both are live simultaneously."** This distinction was tested but not cleanly resolved — a second, behaviour-focused round of testing (rather than assignment-focused) is still needed. See [conflicts-and-open-questions.md](../../00-overview/conflicts-and-open-questions.md).

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

*Widened 2026-09-20 into three classes — Game-Claimed, OS-Claimed and Self-Sabotaging — which add `Esc`,
`PrintScreen`, the Windows and Copilot keys, and `NumLock`. See
[§3c](domain-knowledge/EliteDangerous-ConflictRules.md#3c-a-third-category-reserved-keys), and
[`Esc`](#esc-and-the-difference-between-a-key-you-can-bind-and-a-key-you-can-press) below for what it means
for the editor.*

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

### `Esc`, and the difference between a key you can bind and a key you can press

**Added 2026-09-20.** `Esc` is [Game-Claimed](domain-knowledge/EliteDangerous-ConflictRules.md#3c-a-third-category-reserved-keys)
outright, and it is the one the current editor gets wrong: `EliteKeyboardKeys.ASSIGNABLE_KEYS` lists
`Key_Escape`, so the assign dropdown offers it. A commander who takes the offer gets a binding that can never
fire — and when Elite-Intel drives that control, it presses `Esc` and opens the game menu instead.

**The fix is one entry, because the two jobs are already two lists.** Traced through the code 2026-09-20:

| | Bindable | Pressable |
|---|---|---|
| Source | `EliteKeyboardKeys.ASSIGNABLE_KEYS` | every `KEY_*` field on `KeyProcessor`, via `KeyBindingExecutor.knownEliteKeyNames()` |
| Token form | `Key_Escape` | `KEY_ESCAPE` |
| Read by | assign dropdown, `BindingsWriter`'s guard, keyboard map | the custom-command `RAW_KEY` picker and executor |

No class reads both. So removing `Key_Escape` from the bindable list:

- stops the dropdown offering it;
- makes `BindingsWriter` refuse to write it, so the rule holds even if the UI is bypassed;
- greys it out on the keyboard map;
- **still lets a commander clear an existing `Esc` binding** — the writer skips its key check for a clear;
- **still displays one**, since nothing on the display path reads the list;
- **leaves custom commands untouched.** A `RAW_KEY` step resolves through the pressable list, so a custom
  command that exits the game still reaches `Esc`.

**BindForge must keep the two lists apart.** They look like duplication and are not: one answers *"may a
game control be bound to this?"*, the other *"can Elite-Intel send this?"* Merging them would either put
`Esc` back in the dropdown or take it away from custom commands. **A guard test pins it** —
`knownEliteKeyNames()` contains `KEY_ESCAPE`, and `isAssignable("Key_Escape")` is false — so a future
tidy-up fails loudly instead of silently breaking a commander's exit command.

**An `Esc` binding already in a file is shown**, flagged reserved, with the remedy *clear it* (Alan,
2026-09-20). Reporting one at startup — a Game-Claimed rule in `ReservedKeyChords` — is **deferred**: the game
cannot write one, so it would only ever catch a hand-edited file.

**The other classes, briefly.** `PrintScreen`, the Windows key and the Copilot key are already absent from
`ASSIGNABLE_KEYS`, so nothing changes for them. **`NumLock` stays assignable, with a warning** (Alan,
2026-09-20): it fires fine, but pressing it changes what every numpad binding sends, and refusing it would
overrule a commander who knows that. The warning belongs in Input Mode when it is captured, and on the
keyboard map.

**Rolled into BindForge rather than patched in V1.1 first — decided 2026-09-20.** It only bites a commander
who deliberately picks `Esc` from the dropdown and finds out on the first press, which is small next to the
cost of changing the same editor twice.

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

### Known scanner defects — FN-1 fixed, three still open

**Updated 2026-09-20: FN-1 is fixed** — shipped in the V1.1 maintenance line and merged into
`V1.2-BindForge` the same day. See [FN-1 in detail](#fn-1-in-detail--scoped-2026-09-12) for what shipped and
where the build departed from the design. **FP-1, FP-3 and FN-6 are still open**, and nothing below about
them has changed.

*As recorded before the fix:* **All four are live work rather than history.** FN-1, FP-1 and FP-3 were each confirmed against the game
itself rather than suspected, and verified against the code on 2026-09-09. FN-6 was a suspicion until
2026-09-12, when it was confirmed by reading the same code — see below.

**All four re-checked 2026-09-12**, after Krondor's September conflict work landed. That work added a
[third blocking rule](domain-knowledge/EliteDangerous-ConflictRules.md#the-three-breaks-in-the-context-model),
twenty-odd scanner tests, and a better source for an action's vehicle context (see below) — but it did
not touch keyset extraction or the device gate. **The rules got sharper; what reaches them did not
change.** FN-6, never checked before, is now confirmed rather than suspected.

| ID | Defect | What the game actually does | Still open? |
|---|---|---|---|
| **FN-1** | **Secondary-slot blindness.** One slot per action is kept and the other discarded before any conflict check runs. | Treats a Secondary-vs-Primary collision as **a real conflict**. | **No — fixed 2026-09-20.** The scan keys each chord by action *and* slot, so both slots are compared. `toKeysets()` is gone. |
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

*Updated 2026-09-20:* the FN-1 fix deleted `BindingProfilePanel`'s copy. Both conflict paths now reach the
gate through `KeyBindingsParser.toExecutableSlots()`, so FN-6 has one place to be fixed in on the conflict
side. The gate itself is untouched, and FN-6 is exactly as open as before. (`MissingBindingAutoAssigner`
keeps a slot-level check of its own, outside the conflict path.)

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

*Updated 2026-09-20:* with FN-1 fixed they no longer compound — every surviving slot is scanned. FN-6 still
voids a HOTAS-modified slot on its own, so in that example the Secondary is scanned and the Primary is
still dropped without a word. The example still holds; only its cause got simpler.

**One thing did improve.** `contextOf()` now reads an action's vehicle from
`BindingDisplayNames.lookup(action).section()` — the game's own OPTIONS › CONTROLS screen — falling
back to substring-matching the tag only for `GENERAL` and `OTHER`. Context is the basis of every
`isSafeOverlap` decision, so sourcing it from the control screen rather than from whether a tag happens
to contain `Buggy` makes the whole safe/unsafe split more trustworthy than when these defects were
first recorded.

#### FN-1 in detail — scoped 2026-09-12

> **Shipped 2026-09-20.** Built in the V1.1 maintenance line rather than as BindForge's first slice: a
> blind spot in shipped conflict detection is a V1.1 bug, so it went out with V1.1 and reached
> `V1.2-BindForge` through Krondor's merge. Commits `220271aa2` (the fix) and `bf1ad44ef`…`38188f9da` (the
> cleanup after review) by Alan; `ac24c5de3` (the two detectors) by Krondor; merged to `V1.1-Release` as
> `2d0367be7` and to `V1.2-BindForge` as `02e9c0ab7`.
>
> **The rest of this section is the design as scoped, kept as the record.** The build followed it, except:
>
> | The design said | What shipped | Why |
> |---|---|---|
> | The two anomaly detectors **gain** a both-slots method; the one-slot methods keep serving the spoken warnings | `ReservedKeyChords.scan` and `UiNavigationTextTrap.scan` were **switched** to both slots, and the startup warnings with them | Elite fires either slot. A Secondary on `Alt+F4` still closes the game, and the trap bites the commander's own keypresses — *"in our hands or theirs"*, per `UiNavigationTextTrap`'s own javadoc — not only Elite-Intel's |
> | Whether the spoken conflict warning sees more is **Krondor's call** | It does. Conflicts are scanned on both slots, still filtered to pairs touching a control Elite-Intel drives | Settled by what he merged |
> | `ReservedKeyChordsTest` and `UiNavigationTextTrapTest` **not touched**; no test expectations change | Both suites were migrated to slot-map fixtures, the untyped helper this change added to the conflict tests became a typed builder, and each detector gained Secondary-slot cases | Replacing the detectors, rather than adding to them, made it unavoidable — and the typed builder cleared the `@SuppressWarnings` review had flagged repeatedly |
> | `Conflict` gains the slot, so Game Mode can colour the **slot cell** | `Conflict` still names actions and carries the **chord**; the panel finds the slot by matching it. Rows are still coloured whole | The action stayed the unit of judgement. Slot-cell colouring is **still to build**, and the chord is enough to build it from |
>
> **Found in review, not foreseen here:** `getBindingSlots()` returns `null` until a parse succeeds, and the
> first version of the both-slot conflict path would have thrown on a fresh install. Each scanner now guards
> `null` itself; Krondor chose that over changing the accessor.
>
> **Known debt left by the build:** the public trio is named unevenly — `scanSlots`,
> `recommendVehicleTwinsFromSlots`, `candidateConflictInSlots` — because the suffixes once told them apart from
> the action-keyed methods they replaced. Those methods are gone; four test-only adapters remain, each with a
> note saying why, until ~45 older tests move to slot-map fixtures. The older `bindings(Object...)` helper
> those tests build through keeps its `@SuppressWarnings("unchecked")` until then, and goes with them.

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

**Where filtering happens (resolved 2026-07, revised 2026-09-15):** there is no longer a shared toggle meaning something different in each mode. **Show Anomalies Only belongs to Game Mode**, filters rows there, and is hidden and cleared everywhere else. [Anomalies](#anomalies) needs no filter, since its content already is the list; [Action Groups](#action-groups) has no filter at all, by design; Control Types and Controller Mode need no behaviour, being confirmed back-burner.
