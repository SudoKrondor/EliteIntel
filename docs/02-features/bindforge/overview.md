# BindForge — Overview

**Elite-Intel feature:** BindForge
**Function:** A game control configuration editor for viewing and editing a player's Elite Dangerous bindings outside the game — `.binds` files, `DeviceMappings.xml`, and `.buttonMap` files.

---

## Purpose

BindForge exists to protect Elite Dangerous control configuration from loss and to let the player view and edit that configuration without leaving the game's own file conventions. It is the most design-complete of the two features in this documentation set.

### Scope: every control, every input device

**BindForge covers the whole of Elite's control set and every device that can drive it — keyboard, mouse, joystick, HOTAS, gamepad, pedals, and any combination of them.** It is not a controller-only tool. Where this documentation says "controller", read it as shorthand for *whatever device holds the binding*, never as an exclusion of keyboard and mouse.

This matters more than it looks, because Elite-Intel's existing binding code is deliberately narrower. `KeyBindingsParser` is a read-only, keyboard-only boundary feeding command execution, and `BindingsWriter` refuses to edit any slot holding a controller assignment. Both are correct for what they protect today, and because **BindForge is that same code grown**, neither can simply be routed around.

They do not get the same answer: the parser stays exactly as narrow as it is, and the writer widens — see [Two narrow boundaries](#two-narrow-boundaries--one-stays-one-widens).

**Priority order, in order:**
1. Back up and restore all managed file domains reliably — see [Managed File Domains](#managed-file-domains) and [File Manager](file-manager.md).
2. View and edit bindings — see [Bind Editor](bind-editor.md).
3. Manage device mappings and button names — see [Alias Designer](alias-designer.md).

BindForge does not interact with the running game process. It only touches files the game itself reads at its own startup or when it loads a binds file — see [Writing While the Game Is Running](#writing-while-the-game-is-running).

BindForge runs on both of Elite-Intel's target platforms. On Linux, there is effectively one installation (via Steam/Proton); on Windows, a player may have several storefront installations simultaneously.

## Managed File Domains

BindForge manages four related but distinct file domains, described fully (with real confirmed paths and format detail) in [BindForge's binds-file domain knowledge](domain-knowledge/EliteDangerous-BindsFileFormat.md) and [device-mapping/button-map domain knowledge](domain-knowledge/EliteDangerous-DeviceMappings-ButtonMap.md):

| Domain | File(s) | Location | Loss impact |
|---|---|---|---|
| Bind Domain | `.binds` | User configuration folder — one single shared location regardless of storefront | High — a lost or corrupted `.binds` file affects actual gameplay controls |
| Device Identity Domain | `DeviceMappings.xml` | Inside the game **installation** folder — genuinely duplicated per storefront install | **High — reclassified 2026-09-08, see below.** Previously read: cosmetic only — affects button-label display, not gameplay |
| Button Map Domain | `.buttonMap` (one per named device) | Inside the game installation folder, alongside `DeviceMappings.xml` | Labels only — **but protected like the others**, see below |
| Preset File Domain | `StartPreset.4.start` | Same user configuration folder as `.binds` | Governs which `.binds` file is loaded per binding section — **protected like the others**, see below |

### Protection is uniform — settled 2026-09-21

**Every managed file gets the same protection, whatever its loss impact.** The column above says what losing
a file *does*; it does not say how carefully the file is guarded. Alan:

> *"before a user/player edits devicemappings.xml and creates buttonmaps those 2 files mean nothing. the moment
> they do edit/create those losing them breaks the binds file. […] Once BindForge is used, to create even just an
> alias for a controller, just one, everything is important and needs to be protected at all cost. […] we should
> just assume that if a player uses Elite Intel, all files are now needing protection. thats the simplest path."*

**Both of the earlier readings were true, at different moments.** A factory `DeviceMappings.xml` and no
`.buttonMap` files carry nothing of the user's, and losing them costs nothing. The moment a user names
one controller, the files hold their work, and a reset to factory undoes it. Tracking which state each
installation is in would be a way to be wrong about it. **So BindForge assumes the second state from the first
run**: backup, restore, destructive-change detection and warnings treat all four domains alike, and no screen
ranks one as safer to lose.

### Every install gets the same files — settled 2026-09-23

**BindForge keeps one configuration and applies it everywhere** ([principle 8](principles.md)).
`DeviceMappings.xml` and the `.buttonMap`s exist once per installation; APPLY writes the working copy's one set to
every installation, so they are identical — the same names and the same labels everywhere. There is no
per-installation configuration. `.binds` and `StartPreset.4.start` are shared by every installation on Windows; on
Linux each installation has its own bindings folder, and BindForge uses the one Elite-Intel is set to, for now
(2026-10-14, Alan — see [input environment §7](../../00-overview/input-environment.md#7-linux-the-bindings-folder-is-not-shared)).

**Why, and it is measured rather than argued.** The shared `.binds` names devices by entries that live in a
per-install file, so an install whose `DeviceMappings.xml` lacks them
[rejects the whole binds file and falls back to a factory one](domain-knowledge/EliteDangerous-DeviceMappings-ButtonMap.md#12b-what-happens-when-a-device-name-has-no-entry--measured-2026-09-22).
Divergence between installs is not a configuration a user chose. It is a latent outage waiting for
whichever install they launch next, and the only symptom is a log file nobody opens.

**What this needs, and where it is:** one reconcile when installations already disagree
([storage.md](storage.md) rules 1–3); saying so when an installation cannot be written, *"updated 2 of 3"* (rule 8);
and a check after game updates, since a patch resets these files per installation (rules 14–15).

#### Offered, never forced — settled 2026-09-23

**Offered, never forced** ([principle 5](principles.md)). Alan: *"we build BindForge to help when asked. not force a
situation on the user… all we need to do at first is be the support mechanism."* BindForge asks nothing and writes
nothing to the game's files until the user opens its tab. First-time startup then runs ([storage.md](storage.md)
rules 1–3): where the installations are identical there is nothing to reconcile and nothing is asked; where they
differ, the user reconciles them. The user is told once, spoken and logged, when the backup before the working copy
is created is taken.

**Why not force it, when installations that differ are an outage waiting to happen?** Elite-Intel has warned about
binding conflicts for months without requiring anyone to act, and a user can run a long time with one harmless
conflict. Forcing a choice on a user who has not asked for one is how a backup tool turns into an obstacle.

**The one thing done unasked: a backup every time Elite-Intel starts** (2026-10-23, Alan; storage.md rule 12) —
silently, of every live copy in every installation, whether or not the user ever opens BindForge. A user who only
ever edits in the game is protected too, and a user who lost their binds can be told where their backups are.

The preset file has four lines, one per binding section, in a fixed order: General, Ship, SRV, On Foot. All four normally name the same BindName (the common case), but each line is independently settable — a valid, if unusual, split preset file. See [Preset Editor](preset-editor.md).

### Why `DeviceMappings.xml` is not cosmetic — reclassified 2026-09-08

It was filed as cosmetic because its visible effect is button labels. That is what it *shows*. What it *does* is decouple the user's bindings from a hardware ID they do not control.

`.binds` names a device by its `DeviceMappings.xml` element name, or by raw VID+PID hex when no entry matches ([§4.0](domain-knowledge/EliteDangerous-BindsFileFormat.md#40-the-rule-confirmed-2026-09-08)). So the entry is an **indirection layer**, and whether one exists decides what happens when the hardware ID changes:

| State | `.binds` holds | A firmware update changes the PID |
|---|---|---|
| **No entry** | `Device="334483F3"` | every binding on that device is orphaned, **silently** |
| **Entry exists** | `Device="RVWAP"` | edit one PID field; every binding keeps working |

**And hardware IDs do change.** See
[VID/PID is not stable](alias-designer.md#vidpid-is-not-a-stable-identity). One user's two VIRPIL devices carried six different PIDs across three snapshots between 2024-08 and 2026-09, while a Razer device and two Frontier-recognised devices in the same files never moved.

So the loss impact is **high, not cosmetic**: a `DeviceMappings.xml` lost to a game update takes the indirection with it, and the bindings that depended on it revert to depending on an ID the vendor can change. `.buttonMap` holds only labels — but under the [uniform protection rule](#protection-is-uniform--settled-2026-09-21) it is guarded exactly as carefully, because once a user has written labels, losing them is losing their work.

**Measured 2026-09-22, and it is worse than "those bindings stop working."** On a machine with two installs sharing one bindings folder, launching the install whose `DeviceMappings.xml` lacked the user's two entries produced 141 `Failed to find GUID for device` lines and **the game fell back to the factory KEYBOARD & MOUSE binds file in all four sections** — losing the keyboard and mouse bindings too, which referenced no device entry at all. The other install, reading the same `StartPreset.4.start` and the same `.binds`, loaded correctly. Nothing was rewritten on disk. Full method and numbers: [what happens when a `Device=` name has no entry](domain-knowledge/EliteDangerous-DeviceMappings-ButtonMap.md#12b-what-happens-when-a-device-name-has-no-entry--measured-2026-09-22).

A confirmed real-world incident — a game update that overwrote a user's `DeviceMappings.xml` — is the direct reason Alias Designer and File Manager were prioritised ahead of the rest of the BindForge suite. Game updates can overwrite or erase these files, but this is confirmed **not** universal or guaranteed — it happens on some updates for some users, not reliably reproducibly — so BindForge must always validate presence/correctness rather than assume either outcome.

**Confirmed by direct testing:** `.buttonMap` files cannot be relocated to the user configuration bindings folder alongside `.binds`, despite documentation suggesting otherwise — removing `DeviceMappings.xml` from the game install folder causes the game to load no bindings at all, not even defaults. The game installation folder is the only valid location for both `DeviceMappings.xml` and `.buttonMap` files.

## Live File Synchronization

How the working copy and the live copies stay in step is [storage.md](storage.md): the working copy is the source of truth (rule 4), APPLY writes it to the live copies (rule 7), and the check finds a live copy that changed (rule 14).

### The working copy and edits not applied — 2026-10-22

**One file, and edits held on screen.** The working copy is the last state BindForge wrote, and the check compares
the live copies against it ([storage.md](storage.md) rule 14). The user's changes stay on the editor screens, as
edits not applied, until APPLY saves them into the working copy and writes them out in one step (rules 6–7).

**Why the edits are not written into the working copy as they are made.** A comparison against a half-edited file
is meaningless. Someone midway through renaming a stick would hold a working copy matching neither the game nor
anything worth restoring, and the check would fire against it. *This was the reason for the separate master and
draft (2026-09-24). Holding edits on screen meets it with one file (2026-10-22, Alan).*

### What the working copy and backups cover — 2026-10-22

Alan: *"my intent behind the 'keep the users input data safe' motto is, EVERYTHING… any user data that lives
in this realm of input configurations."*

**Backups and the working copy cover the same files, and only these** (2026-10-23, Alan):

| Folder | Files |
|---|---|
| The bindings folder — on Windows `%LOCALAPPDATA%\Frontier Developments\Elite Dangerous\Options\Bindings\`, on Linux one per installation | **`StartPreset.4.start`** — exactly one; **every `<name>.<major>.<minor>.binds`** — one or more |
| Each installation's `Products\elite-dangerous-odyssey-64\ControlSchemes\` | **`DeviceMappings.xml`** — exactly one per installation; **every `DeviceButtonMaps\<device name>.buttonMap`** — none or more, the user's own, BindForge's generated ones and the two Frontier ships (`VPCPanel`, `VPCThrottle`) |

**Not backed up and not in the working copy:** the factory binds files in `ControlSchemes`, a leftover
`StartPreset.start`, `.binds.backup` copies, `BindingLoadingErrors.log`, and anything else in either folder.
*Until 2026-10-23 a backup took the whole bindings folder.*

**Why the factory binds files are excluded** (Alan, 2026-09-24): the game never edits them. Changing one in the
Controls screen writes a `Custom.#.#.binds` into the *user* folder instead, and a factory binds file hand-edited outside
the game is overwritten by the next update anyway. Backing them up would archive Frontier's files, which
Frontier already ships.

**They are still read**, as the templates [First-Time Startup](#create--a-new-players-first-binds-file--2026-10-24) copies a factory binds file
from. Whether that copy works byte for byte is [testing item 7](../../00-overview/testing-required.md).

**The working copy holds whole files.** APPLY writes `DeviceMappings.xml` and the `.buttonMap`s whole to every
installation, Frontier's entries in `DeviceMappings.xml` with them: every installation runs the same game version,
so those entries are the same everywhere. *Until 2026-10-22 only the user's own elements were kept, so that one
installation's Frontier entries were never pushed into another.*

**Every binds file can be edited, not only the active one** (2026-10-21, Alan). A user editing a binds file they
are not playing — *"edit the testing binds and use that for testing in game"* — gets the same protection as the one
`StartPreset.4.start` points at. The game validates every `.binds` in the folder at startup
([measured](domain-knowledge/EliteDangerous-DeviceMappings-ButtonMap.md#12b-what-happens-when-a-device-name-has-no-entry--measured-2026-09-22)),
and a binds file that disappears from the live copy is caught by the check ([storage.md](storage.md) rule 14).

### What the check tells the user — 2026-10-23

The check ([storage.md](storage.md) rule 14) runs the first time the BindForge tab is opened in a session, and at
APPLY. It compares content exactly: a modification time is not evidence of a change, and no similarity score is
used — a percentage cannot tell a reset from a user who rebinds a whole stick in one sitting (rejected 2026-09-24).

**What the user is shown is the change, not a score.** *"The game's file has 3 bindings yours does not: Ship ›
Landing Gear, …"* can be judged; *"90% similar"* cannot.

**A reset is named** (2026-10-23, Alan). When a live copy matches [Frontier's factory file](reference-data/FrontierStock-README.md),
the warning says a game update probably reset it, offers putting the working copy back first, and asks *"Are you
sure?"* before the reset file replaces the working copy.

**Nothing is decided automatically**, and the backup is taken before anything is written (rule 12).

**At APPLY, with edits on screen** (2026-10-23, Alan): the check settles the working copy first; APPLY then puts the
on-screen edits on top and writes everything out.

#### The check for device files — 2026-10-23

First setup reconciles every installation's `DeviceMappings.xml` and `.buttonMap`s into one working copy: the user
keeps one installation's files over the others, or merges them and decides each conflict — for example, device
3344 BFBF is named `LVWAP` in one and `VirpilLeft` in the other, so the user picks one. The result is the working
copy, the only copy that matters, and APPLY writes it to every installation.

After first setup, if any installation's `DeviceMappings.xml` or `.buttonMap`s no longer match the working copy —
because the user changed something in the game, or a game update changed them — BindForge tells the user what
changed and they reconcile it **the same way as first setup**, with the working copy as one of the sides: keep the
working copy, keep an installation's version, or merge and decide each conflict. The result becomes the working
copy, and goes to every installation after the backup (2026-10-23, Alan).

If the installation's file now matches Frontier's factory file, or is gone, BindForge says a game update probably
reset it, and offers putting the working copy back first. Without `DeviceMappings.xml` the game loads no bindings.

### When a game update brings a newer version — 2026-10-23

**A working copy is never written over a game file of a newer version** (Krondor, 2026-09-24; Alan, 2026-10-23). A
newer version — `DualVirpilDawnTreader.4.2.binds` becoming `.4.3.binds`, or a newer version on the file's XML root —
means a reconcile, never an overwrite. This holds for every one of the files: `.binds` certainly,
`DeviceMappings.xml` and `StartPreset.4.start` possibly one day, `.buttonMap`s probably never.

**The quick way, when nothing of the user's was lost.** The game carries the user's bindings into the new version
itself. So when the newer live copy holds every binding the working copy has, unchanged, the user is told: *"The
game's new version has all your bindings, plus N new ones. It should become your working copy. Approve?"* On
approval the working copy is backed up, and the game's new file becomes the working copy, under its new name.

**The full reconcile, when something was removed or renamed.** If the newer file lacks a binding the working copy
has, or changed one, the user reconciles: the new slots are listed and inserted into the working copy where they
belong, every other difference is decided line by line, and the working copy is renamed to the new version. That
becomes the working copy and goes out at APPLY.

**New slots usually need binding.** The reconcile shows the list of new slots, so the user knows to put keys,
buttons or axes on them.

**Frontier's factory reference belongs to one game version.** After an update, until the
[Frontier stock capture](reference-data/FrontierStock-README.md) is re-captured, BindForge cannot say a file was
reset; the user is still asked, without that wording.

### Freshness Checks

**A file's date is not evidence of a change** (observed 2026-09-17). Pressing Apply on the game's controls screen
with nothing altered still rewrites the whole `.binds` file, every value the same. So [the check](storage.md)
compares what is in the files — the values — never their dates, and never their exact bytes: a different line
ending or byte-order mark is not a change. Otherwise the user would be asked about a change every time they visited
the game's controls screen.

### Destructive Change Detection

**A game update can overwrite or delete the user's files** — it has happened to `DeviceMappings.xml`, and nothing
guarantees it cannot happen to the others. BindForge never takes a reset file as the new truth: a reset is named, and
putting the working copy back is offered first ([the check](#what-the-check-tells-the-user--2026-10-23),
[device files](#the-check-for-device-files--2026-10-23)).

**The user is told even if they never open BindForge** (2026-10-23, Alan). When Elite-Intel starts, once first setup
is complete, it reads the live copies and compares them with the working copy — reading only, writing nothing, no
backup. If anything differs it says once, spoken and logged: *"Your control files were changed outside BindForge.
Open BindForge to check them."*

**If the working copy cannot be read,** BindForge says so and offers the most recent backup to restore from
([storage.md](storage.md) rule 10).

### Create — a new player's first binds file — 2026-10-24

**A new player may have no binds file of their own.** The game runs on one of Frontier's factory binds files until the
player first rebinds something in the game, so there would be nothing of theirs to edit — for exactly the players
who would benefit most.

**CREATE sits at the left end of the [APPLY / DISCARD bar](#apply-and-discard--one-pair-above-bindforges-tabs-2026-10-24-alan),
shown only while the user has no custom binds file** (2026-10-24, Alan). Until it is pressed, Alias Designer, Bind
Editor and Preset Editor are greyed out — the sign to a new player that this is where to start. **File Manager stays
usable**, so a returning player on a new PC can restore a backup instead of creating a file they do not want.

**Pressing CREATE asks two things:**

- **Which of Frontier's factory binds files to start from** — a dropdown of the factory binds files in the
  installation's `ControlSchemes` folder (30 on Alan's: `KeyboardMouseOnly`, `SaitekX56`, `ThrustMasterHOTASWarthog`,
  the gamepad layouts and the rest), **`KeyboardMouseOnly` first and selected**, as the game's own default.
  BindForge does not guess which factory file fits which controller ([principle 6](principles.md)); Alias
  Designer's device list, right there, shows what is attached.
- **A name for the new binds file**, pre-filled `Custom` — the name the game itself gives the first binds file it
  makes — and held to the [binds-file naming rules](bind-editor.md#shell-common-to-all-modes).

**What CREATE does is an edit not applied:** the new binds file, copied from the chosen factory file under the chosen
name, and all four lines of `StartPreset.4.start` pointing at it. The greyed tabs come alive, CREATE greys out, and
APPLY and DISCARD light up. **APPLY** writes it, and CREATE disappears for good — the user now has a custom binds
file. **DISCARD** throws it away, and the user is back where they started: CREATE lit, the tabs greyed.

A user who already has a custom binds file never sees CREATE; a section still on a factory binds file is pointed at a
custom one in the Preset Editor. *Until 2026-10-24 this was a prompt asking per section — Create / Choose sections /
Not now — and before 2026-10-23 it wrote straight into the game's folders.*

Whether a factory file copied byte for byte is a valid custom binds file is still
[testing item 7](../../00-overview/testing-required.md).

#### Writes outside Apply — none

**Nothing writes to the game's files outside APPLY and RESTORE** (2026-10-23, Alan; [principle 4](principles.md)).
Until 2026-10-23 three writes did, on the grounds that each only created something new: a custom binds file from a
factory one (now [in the working copy](#create--a-new-players-first-binds-file--2026-10-24)), a controller's name at
onboarding, and a generated `.buttonMap` at Elite-Intel startup.

**A controller named at onboarding is an edit not applied**: its `DeviceMappings.xml` entry and `.buttonMap` reach the
working copy and every installation at the next APPLY.

**Default labels** — `Button 1`, `X Axis`, `Hat 1 Up` — are worked out by BindForge for every connected controller and
shown on its own screens; they become `.buttonMap` files at the next APPLY. The game's own controls screen shows them
from then.

## Top-Level Structure

BindForge's UI is organised into four top-level sections, in this order:

1. **Alias Designer** — device mapping and button/axis naming
2. **Bind Editor** — viewing and editing bindings; [three modes in V1.2](../../00-overview/v1.2-scope.md#the-v12-release-is-trimmed--settled-2026-09-16), Game Mode, Anomalies and Settings
3. **Preset Editor** — which `.binds` file loads for each binding section
4. **File Manager** — backup and restore of all four managed file domains; BindForge's primary feature

## Data Integrity Principle

BindForge follows Elite-Intel's [Data Integrity Principle](../../01-host-integration/elite-intel-platform-map.md#data-integrity-principle):
a backup first ([storage.md](storage.md) rule 12), and each file written to a temporary file and swapped in, so a
crash never leaves a half-written file.

**APPLY writes everything it can** (2026-10-23, Alan). If a file or an installation cannot be written, the others are
still written, and the user is told — *"updated 2 of 3: Epic — folder not found"*. Nothing is rolled back. The working
copy holds the edits, and the next APPLY writes whatever was missed.

## Reconciling BindForge Edits With In-Game Rebinds

**When the check finds a live copy that changed and the user chooses merge** ([storage.md](storage.md) rule 14), this
is the dialog. *Until 2026-10-23 a V1.1 mechanism decided instead — three fingerprints; an in-game rebind silently
taken when BindForge had no edits, and Apply refused when both sides had. Rule 14 replaced it: the user is always
asked.*

**One row per line item** — see [merge grain](#merge-grain-the-slot-not-the-action--settled-2026-09-07) — with the
working copy's value and the game's side by side; clicking one picks it. **Every line that differs is a row**
(2026-10-22, Alan). A full-file diff was rejected: a `.binds` holds 774 slots, and showing two versions of it asks a
user to think like a programmer about their joystick.

**Nothing is pre-selected.** Merge stays disabled until every row has an answer, and a count says how many are left:
either default silently destroys work in the case the user does not read the dialog. **Keep all mine** and **Keep
all in-game** set every row at once; pressing one is itself the decision. **Cancel resolves nothing.** **If the game
changes the file again** while the dialog is open, the comparison re-runs; answers on rows that did not change again
are kept.

**The result** becomes the working copy and is written to the live copies, backup first (rules 7 and 12).

### Merge grain: the slot, not the action — settled 2026-09-07

**Primary and Secondary are separate entities, not two halves of one.** A change to *Primary* and a change to *Secondary* of the same action are **two separate line items** in [the merge](#reconciling-bindforge-edits-with-in-game-rebinds).

It is not simply twice the element count, because the three element types carry different numbers of slots. From the 457-element reference specimen in [the format documentation](domain-knowledge/EliteDangerous-BindsFileFormat.md):

| Element type | Count | Slots each | Slots |
|---|---|---|---|
| **BUTTON** | 352 | 2 — Primary and Secondary | 704 |
| **AXIS** | 70 | **1** — never a Primary/Secondary pair | 70 |
| **STANDALONE SETTING** | 92 | **0** — a bare value; cannot conflict with anything | 0 |
| `KeyboardLayout` | 1 | 0 — the one text-content element | 0 |
| | 515 | | **774** |

*Counts refreshed 2026-09-08 against two independent `4.2` files, which agree exactly; an earlier 457-element specimen gave 268/72/117.*

So around 600 addressable slots on that file rather than the ~900 a flat doubling would suggest, and 117 elements with no slot to merge at all. Counts differ per file; the proportions are the point.

| Element type | Merge unit | Both sides changed it |
|---|---|---|
| Button | **per slot** | Primary vs Secondary — independent, no conflict |
| Axis | per element (its single slot) | conflict |
| Standalone setting | per element (its single value) | conflict — only one value can win |

**One exception, and it is a property rather than an input.** `ToggleOn` is **element-level, covering both Primary and Secondary as one shared behaviour** — explicitly unlike `Modifier` and `Hold`, which are per-slot. So on a button element the two input slots merge independently while the toggle flag sitting above them does not: both sides changing `ToggleOn` is a real conflict even when the slots either side of it merged cleanly.

The same reasoning covers file-level non-binding settings such as `KeyboardLayout`: one value, no slot, so both sides changing it is a conflict.

**Settled 2026-09-12:** [the merge dialog](#reconciling-bindforge-edits-with-in-game-rebinds) resolves per slot, which is why the grain above is the thing it needed to know.


## Writing While the Game Is Running

**BindForge does not block writes while Elite Dangerous is running, and does not detect whether it is.** An earlier
**Dormant Mode** blocked every write while the game was up; it was dropped in August 2026 because the game re-reads
`.binds` only when its own controls screen opens, so the risk is far narrower than "the game is running", and
because Elite-Intel cannot detect the game process.

**The risk that remains** (measured 2026-09-18): if the game's controls screen is open while BindForge applies, and
the user then applies in the game, the game
[writes back the whole file as it loaded it](bind-editor.md#an-in-game-apply-re-emits-the-whole-file--observed-2026-09-17),
and BindForge's write is gone. **Nothing is lost for good:** the working copy still holds the user's work, and the
next check — at Elite-Intel startup, when BindForge is next opened, or at the next APPLY ([storage.md](storage.md)
rules 14–15) — finds the live copy changed and asks.

**APPLY's confirmation always says** (2026-10-23, Alan): *"Close the game's controls screen before applying. Changes
made in BindForge and in the game at the same time can be lost."* — rule 13's warning, at the moment it matters.

Every write is backed up first (rule 12) and written atomically ([Data Integrity Principle](#data-integrity-principle)).

## Upgrading the Existing Bind Editor

Elite-Intel already ships a bind editor — `ui.screen.BindingsTabPanel`, backed by ~20 classes that moved from `elite.intel.ai.hands` to `elite.intel.bindforge.io`, `.model` and `.rules` on 2026-10-03.
**BindForge is that editor,
upgraded.** It is not a second section built beside it, and there is nothing to retire: anything BindForge was specified to do is built
*into* the existing editor and grown up to these specs. See [Phased rollout](../../00-overview/v1.2-scope.md#phased-rollout--revised-2026-09-08) for the order, and [why the build-alongside plan was replaced](../../00-overview/v1.2-scope.md#the-bindforge-name-is-returning-not-arriving) for the reasoning.

That settles what used to be this document's hardest open question — which component owns the write path — by removing the second component. What follows is what the decision obliges.

### There is one writer, and it already exists

`BindingsApplyService`, `BindingsWorkingCopyRepository`, `BindingsWriter`, `elite.intel.io.TimestampedBackups` and `AppPaths.getBindingsWorkingDir()` are **BindForge's pipeline already**. They are not borrowed from a neighbour; the neighbour is the thing being upgraded. The [working copy](#live-file-synchronization) this document describes is the working-copy repository that exists today, grown, with APPLY and RESTORE as the only paths to a live file — [no exceptions](#writes-outside-apply--none) since 2026-10-23.

**The hazard this replaces is worth naming, so that nobody rebuilds it.** A second editor would have meant two working copies and two writers over the same `.binds`. Each would have been individually safe — backup, atomic write, the [Data Integrity Principle](#data-integrity-principle) satisfied — and **jointly they would not have been:** one saves, the other is still holding a stale in-memory model, the player applies there, and the first editor's work is gone with no operation having misbehaved. The Data Integrity Principle does not
catch that, because it governs atomicity *within* an operation, not ownership *across* components.

**Under upgrade-in-place that failure cannot occur, because the second owner is never built.** The rule that keeps it that way is simple: BindForge adds no working copy, no writer and no working directory of its own. Where a capability is missing, the existing class grows it.

#### Where the working copy lives — 2026-10-23

The working copy is Elite-Intel's existing working folder, `AppPaths.getBindingsWorkingDir()`, grown to hold every
file — not a new folder beside it ([one writer](#there-is-one-writer-and-it-already-exists)). Inside, it matches the
game's own layout, so a user browsing to it recognises what they see: `Bindings\` holds every `.binds` and
`StartPreset.4.start`; `ControlSchemes\` holds the one `DeviceMappings.xml`, with every `.buttonMap` in
`ControlSchemes\DeviceButtonMaps\` (2026-10-23, Alan). *Until 2026-10-23 this section placed a separate master, its
Edit History and a Revert method; [storage.md](storage.md) replaced them.*

### APPLY and DISCARD — one pair, above BindForge's tabs (2026-10-24, Alan)

**APPLY and DISCARD sit once, above BindForge's four tabs — Alias Designer, Bind Editor, Preset Editor, File Manager —
and beneath Elite-Intel's main tabs.** They are the only APPLY and DISCARD in BindForge; no tab has its own. They act
on everything: APPLY pushes the whole working copy, with every edit not applied from any tab, to the live files in
every installation; DISCARD throws away every edit not applied, from every tab.

**They are lit only when there are edits not applied, and greyed out otherwise** — so their state is itself the hint
that there is work to apply. Placed under Elite-Intel's main tabs, they are where the mouse goes to leave BindForge,
so the user sees them before switching away. Leaving with edits not applied still asks
([Unsaved Work on Exit](#unsaved-work-on-exit)).

### Two narrow boundaries — one stays, one widens

`elite.intel.bindforge.io` holds two deliberate restrictions. Under the old plan BindForge sidestepped both by building its own. Under this one they have to be faced directly, and
**they do not have the same answer.**

**`KeyBindingsParser` stays exactly as narrow as it is.** It is a read-only, keyboard-only boundary feeding command execution — the path that presses real keys in the player's ship. Its narrowness is what stops a non-keyboard assignment from becoming executable by accident. BindForge reads `.binds` for editing through its own full-fidelity path and **must not widen the parser to do it**; the two readers answer different questions, and the parser's answer must stay small.

**`BindingsWriter` widens — settled 2026-09-12, reversing an earlier position.** It refuses today to edit any slot holding a controller assignment, allowing only an existing `Keyboard` slot or an empty `{NoDevice}` one (`isEditableMainSlot`). BindForge's remit is [every control on every device](#scope-every-control-every-input-device), so as the editor
becomes BindForge, the writer must be able to write what the editor can edit.

The refusal's stated reason survives the change, because it was never really about controllers:

> *The commander bound that device in the game and this application does not take it away.* <!-- terminology-ok: verbatim javadoc from BindingsWriter, quoted as written -->

That is exactly right for an **assistant acting on its own initiative**, and it stays right — nothing in the voice or command path may touch a controller binding. It does not describe a **user editing their own bindings on purpose**, which is the one thing a bind editor is for. A user who rebinds a HOTAS button in the capture dialog *is* taking it away, intentionally, having been shown what was there.

So the boundary moves from *device type* to *who initiated the write*: **deliberate, user-initiated edits may write any device; nothing automatic may write a controller slot.** Widening the writer does not widen the parser, so the protection the javadoc actually describes — keeping non-keyboard assignments out of command execution — is untouched.

#### Editing a binding is not the same as being able to press it

**Stated by Krondor 2026-09-12, and it is a hard capability limit rather than a policy:**

> *"While we may let users edit the HOTAS assignment as part of the binding management (which is the
> goal for BindForge) the app can't use / simulate any input except keyboard."*

So the writer widening is real but **one-directional**. BindForge gains the ability to *write* a
controller assignment into `.binds`; Elite-Intel gains no ability to *send* one. The reason the editor
refused controller slots in the first place was not caution about overwriting the user's work —
it was that **there was no support for those devices at all.**

Two consequences worth building for:

1. **`KeyBindingsParser` staying narrow is not a stylistic choice.** It is the shape of what the
   application can actually do. Widening it would not make a HOTAS binding executable; it would only
   let an unpressable one reach the executor.
2. **A control the assistant drives, bound only to a controller, is unusable by the assistant** even
   though the user sees it as bound. That is a real state with a real remedy, and it belongs in
   [Anomalies](bind-editor.md#four-kinds-one-question) rather than only in a log line.

### External change detection — 2026-10-23

**BindForge does not watch the files.** The check runs at three moments — when Elite-Intel starts, the first time the
BindForge tab is opened in a session, and at APPLY ([storage.md](storage.md) rules 5, 7, 14–15) — and the one at
APPLY catches anything that changed while BindForge was open, before anything is written (2026-10-23, Alan).
*Until 2026-10-23 `DeviceFilesMonitor` polled every installation's device files every two seconds to refresh the
Alias Designer screen.*

**`BindingsMonitor` stays, unchanged, and is not BindForge's.** It watches the bindings folder so the assistant
reloads the user's key bindings when `.binds` changes — see
[BindForge must not publish `BindingsUpdatedEvent`](#bindforge-must-not-publish-bindingsupdatedevent).

### BindForge must not publish `BindingsUpdatedEvent`

**Verified 2026-09-06 — and the conclusion is the opposite of what this section first assumed.**

The concern was that changing a binding without telling the command-execution path — `KeyBindingExecutor`,
`InputSequenceExecutor`, `CustomCommandHandler`, `SafeKeyboardKeys` — would make the assistant press the
wrong key mid-flight. Worse than a stale screen: a wrong action in the game.

**That path does not subscribe to the event, and does not need to.** `InputSequenceExecutor` calls
`BindingsMonitor.getBindings()` fresh on every step, and the monitor's watcher re-parses on change. **Reload
is driven by the filesystem, not by the event.** There is no staleness gap and no pre-existing bug.

`BindingsUpdatedEvent` is a UI notification — its subscribers are UI panels because that is all it is for. It
is published by `BindingsMonitor` (the watcher noticed a change) and `PlayerBackupService` (restore).
Applying to a live `.binds` trips the watcher, which publishes the event itself; **a manual publish on top of
that double-fires.**

What BindForge owes instead is **one write per apply.** The monitor de-duplicates repeat notifications by
fingerprinting the file it last parsed — last-modified time plus size — and a write delivered in pieces
defeats that.

## Unsaved Work on Exit

**Leaving BindForge with edits not applied** — switching to another Elite-Intel tab, or closing Elite-Intel — asks
first ([storage.md](storage.md) rule 9), with three choices: **Go back** (return with the edits still on screen; on
closing Elite-Intel it cancels the close), **Discard** (throw them away and leave), or **Apply** (apply, with its usual
confirmation and backup, then leave).

**Nothing is selected, and Enter chooses nothing** (2026-10-23, Alan): the user clicks one of the three, so no choice
is made by accident. **Esc and the close glyph are Go back**, which loses nothing. Writing to the game's files must
never be the incidental outcome of leaving a screen — the 2026-09-06 reason Apply was once left off this dialog
altogether. *Until 2026-10-23 the choices were Save and Discard, Save keeping the edits in the draft; with edits held
on screen there is nothing to save to. The 2026-10-12 ruling (Core C6) that saved-but-not-applied work needs no
prompt went with the draft.*

## Document Map

- [Alias Designer](alias-designer.md)
- [Bind Editor](bind-editor.md) — Game Mode, Anomalies, Settings and shared conflict detection in V1.2; Action Groups, Input Mode, Control Types and Controller Mode [deferred](../../00-overview/v1.2-scope.md#the-v12-release-is-trimmed--settled-2026-09-16)
- [Preset Editor](preset-editor.md)
- [File Manager](file-manager.md)
- [Roadmap and Open Items](roadmap.md)
- [Binding Schema (data model)](../../03-data-models/binding-schema.md)
- [Domain Knowledge](domain-knowledge/) — Elite Dangerous file formats, action catalog, conflict rules, and the interactive bind-conflict test pages
- [Reference Data](reference-data/) — the raw action catalog and binding-zone spreadsheet BindForge is seeded
  from, Frontier's stock files, and the real `.binds` specimens several findings rest on (see
  [SPECIMENS-README.md](reference-data/SPECIMENS-README.md))
