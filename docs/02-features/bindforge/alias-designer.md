# BindForge — Alias Designer

**Purpose:** manage `DeviceMappings.xml` and `.buttonMap` files, giving connected controllers friendly names shown both in BindForge's own Bind Editor and in the game's own controls UI.

## Structure

A single flat section with no sub-tabs: a device list, with the editor expanding inline beneath whichever device is open.

Earlier drafts moved per-installation control into a separate "Installation Mirroring" screen, first inside Alias Designer and later inside [File Manager](file-manager.md). **All of it is gone** — both screens, and the per-installation control itself, which [standardisation removed](overview.md#every-install-gets-the-same-files--settled-2026-09-23). What survives is the *reporting*: whether each installation matches the master for a device is a property of that device, so it sits on the device's own row rather than on a screen the user has to go and find. See [One record, and where it lands](#one-record-and-where-it-lands--reworked-2026-09-26).

## Device List

Two views, toggled via a tab.

### My Devices

**The list is the union of live hardware and file entries**, correlated by VID/PID:

- every controller the Device Service currently reports, whether or not the game has ever heard of it, **and**
- every non-Frontier entry found in any installation's `DeviceMappings.xml`, whether or not that hardware is
  currently plugged in.

`DeviceMappings.xml` supplies *names*. It never supplies *existence*. A brand-new stick appears the moment it
is plugged in, unnamed; an entry for a controller sold last year keeps appearing until it is cleared.

Columns, and why only these:

| Column | Source |
|---|---|
| **Windows name** | what the hardware reports through the Device Service |
| **VID / PID** | the hardware |
| **Status** | `ATTACHED` or `MISSING` — whether the hardware is present right now |
| **Alias** | the name in the master, or *not added* |
| **Installations** | one read-only marker per *detected* installation |

The first three describe the hardware and are true no matter how many installations exist. **The alias is now
a column**, because there is one of them: the master's. *Under the original design there was no alias column,
because each installation could hold a different name and a single cell would have been averaging them.
[Standardisation](overview.md#every-install-gets-the-same-files--settled-2026-09-23) removed that, so the
honest cell is the master's name.*

**What stays per installation is whether each one has caught up.** Each marker names an installation and
shows **M** when that installation matches the master for this device, *not added* when it holds no entry, or
the [divergence colour](#divergence-between-installs--one-list-ranked-by-consequence) when it differs. The
markers are a readout — clicking one does not open a per-installation editor, because there is no such thing;
opening the row opens [the one record](#one-record-and-where-it-lands--reworked-2026-09-26).

### Built-in Devices

Frontier's shipped default entries. **The reference list is read-only**, and BindForge never changes an
entry on it. A built-in controller that is actually attached also appears under **My Devices, marked
BUILT-IN**, where its button labels can be edited — see
[Built-in controllers in My Devices](#built-in-controllers-in-my-devices--settled-2026-09-13).

> **Open problem — this split is not derivable from the file.** `DeviceMappings.xml` is a flat list of device
> elements with nothing marking which are Frontier's and which the player added. Any list built by reading a
> real install will mix them. Resolving this needs a shipped reference copy of Frontier's stock file plus a
> provenance record — see [Device Provenance](../../03-data-models/device-provenance.md).

### Built-in controllers in My Devices — settled 2026-09-13

**A controller that matches one of Frontier's entries on VID/PID is shown as BUILT-IN**, in every
installation, because Frontier's entry sits in each installation's `DeviceMappings.xml`. The entry exists, so
the device is not *not added* — but the entry is not the user's either, and the grid says which. The match
is made against the [shipped stock reference](reference-data/FrontierStock-README.md): `Thrustmaster T-Rudder`
at `044F:B679` is Frontier's `<T-Rudder>`.

**Reversed 2026-10-04 — a built-in can be renamed, or shadowed by an entry of the user's** (Alan). Measured:
when two elements claim one VID/PID, [the first in the file wins](domain-knowledge/EliteDangerous-DeviceMappings-ButtonMap.md#12d-two-entries-for-one-vidpid-the-first-in-the-file-wins--measured-2026-10-03),
and renaming Frontier's `<T-Rudder>` in place loaded normally and survived a game save. The two reasons for
the lock below no longer hold:

- *"A game update restores Frontier's file."* It can, and that is a wipe like any other: BindForge carries
  Frontier's shipped list and the user's master, so [the startup check](overview.md#what-the-startup-check-actually-decides)
  detects it and Apply puts the user's version back.
- *"Renaming one renames every controller it covers."* True for a rename, which is why **shadowing** is
  offered beside it: a user element at the top of the file, carrying only the attached controller's VID/PID,
  wins for that controller and leaves Frontier's element in place for the rest. For a single-pair entry like
  `<T-Rudder>` the two give the same result.

**Either one is offered only for a controller the user has attached** — the VID/PID comes from the hardware,
as it does for any device. Both are ordinary user elements from then on: mastered, written
[at the top of `<Root>`](#what-the-master-actually-is), and a rename goes through the
[rename transaction](#renaming-a-device), rewriting every `.binds` that names the old element — Frontier's
name included. *The same test showed what skipping that costs: an inactive preset still naming `T-Rudder`
logged six `Failed to find GUID` lines.* How the device editor presents rename versus shadow is not designed
yet.

~~**Its name and VID/PID are locked** (Alan, 2026-09-13). A built-in element is Frontier's **definition**, not a
label on one stick: `<DualShock4>` matches both DualShock 4 revisions, and `<GamePad>` carries 80 VID/PID pairs,
so renaming one renames every controller it covers. And a game update that restores Frontier's file would put
the old name back, leaving `.binds` pointing at a name that no longer exists.~~

**Its button and axis labels are editable.** They are saved as a `.buttonMap` under Frontier's name —
`T-Rudder.buttonMap` — which changes nothing about how the game recognises the device. Labels unlock straight
away, because there is no name to confirm.

**RESET LABELS replaces CLEAR.** [CLEAR](#actions-and-what-each-one-reaches) removes an installation's entry,
and on a built-in that entry is Frontier's. RESET LABELS removes only the `.buttonMap` the user or BindForge
created, and the next [Elite-Intel startup](#at-elite-intel-startup-a-buttonmap-for-every-connected-named-controller)
generates a fresh one. A `.buttonMap` Frontier itself shipped — `VPCPanel`, `VPCThrottle` — is never removed.
Like SAVE and CLEAR, it reaches **every installation** on the next Apply — there is one record, so a reset is
a reset everywhere.

| | The user's entry | Built-in |
|---|---|---|
| Name | editable, as the [rename transaction](#renaming-a-device) | ~~locked~~ renamed or shadowed, when the controller is attached — the entry then becomes the user's (2026-10-04, above) |
| VID / PID | read-only | read-only |
| Button and axis labels | editable once the name is confirmed | editable straight away |
| Undo (reaches every install) | **CLEAR** — the entry and `.buttonMap` | **RESET LABELS** — only a `.buttonMap` BindForge or the user made |
| [Onboarding](#onboarding--every-controller-gets-a-name-and-a-buttonmap--settled-2026-09-13) | asked for a name | never asked |
| What APPLY writes | the entry and its `.buttonMap` | the `.buttonMap` only |

## First setup — reconciling the installs — settled 2026-09-23

**The handover.** Until a user opens Alias Designer, BindForge only backs up and reports
([offered, never forced](overview.md#offered-never-forced--settled-2026-09-23)). First setup is the one time
it asks anything: it turns whatever the installs currently hold into a single master set, after which every
install matches it. *"Adopt"* stays reserved for what happens later, when someone edits in the game.

**It can be opened and abandoned.** Nothing is written until the user presses the button at the end.

### What it reads

Every install's `DeviceMappings.xml` and `.buttonMap` files, plus two references that decide how rows are
judged: **Frontier's stock list**, to tell the user's entries from the shipped ones, and the shared
**`.binds`**, which decides [severity](#divergence-between-installs--one-list-ranked-by-consequence).

**Stock entries are never compared, never written, never offered as a choice.** Every install runs the same
game version — the live service forces an update before it will let anyone play — so their shipped entries are
identical by construction. Alan's machine: Steam 53 elements, Epic 51, **the same 51 stock in both**, the
difference being his two custom sticks.

### Reconciliation is per device element, never per file

The file is a container; the device element is the unit of meaning. **A file copy would delete any custom
entry that exists only in the install being written to**, which is the one thing first setup must never do. So
"overwrite Epic with Steam" means *replace Epic's custom elements with Steam's*, leaving everything else
in place.

Once stock is set aside, a custom element falls into one of four cases:

| Case | Resolution |
|---|---|
| Same name, same VID/PID in every install | identical — nothing to do |
| Present in one install only | the master takes it |
| Same name, **different VID/PID** | a real conflict — pick one |
| Same VID/PID, **different name** | a real conflict — and it is a rename, see below |

**A name conflict is not a device-file edit.** The shared `.binds` names devices by their element name, and so
does the `.buttonMap` filename. Choosing between `RVWAP` and `RightStick` therefore moves three things
together — the element, every `Device="..."` in the shared `.binds`, and the `.buttonMap` file. First setup
routes that into the existing [rename transaction](#renaming-a-device) rather than treating it as a simple
choice.

**`.buttonMap` files merge at the label, and that needs a merge view.** Unlike an entry, two label sets for
one device combine: twenty labels in one install and five in another give twenty-five. But where both files
name the *same* input differently, only the user can say which belongs in the master — so overwrite-or-merge
is not a single answer for the file, it is an answer per input.

**The merge view shows only the inputs that disagree**, one row each:

| Input | Steam | Epic | |
|---|---|---|---|
| `Joy_1` | LV MAIN TRIGGER | TRIGGER 1 | pick one |
| `Joy_4` | — | PINKY PADDLE | taken, nothing to ask |
| `Joy_XAxis` | LVWAP_XAXIS | LVWAP_XAXIS | identical, not shown |

- **Identical labels are not listed.** Showing what already agrees turns a short decision into a long one.
- **A label present in only one file is taken**, on the same "show everything, ask about little" rule as the
  element cases above — it appears in the result, not in the questions.
- **Only genuine collisions are rows**, and each row is one click.
- **Overwrite stays available for the whole file**, for a user who would rather not walk a list: *use Steam's
  labels for this device*.

The same view serves a later re-merge, when an install has drifted and its labels no longer match the master.

### The flow

1. **"Your files are backed up as of &lt;time&gt;"** — said first, because it is what makes the rest safe to touch.
2. **The list**, every row coloured by severity, conflicts sorted to the top.
3. **Two routes through:** resolve row by row, or **use one install as the master** — the blunt option, one
   click, for a user who does not want to think about it. The backup makes it reversible.
4. **A summary before any write:** *"The master will hold 6 devices and 5 buttonMaps. Apply to 2 installs?"*
5. **Write, and report per install** — *"updated 2 of 2"*, or which one failed and why.

**Two ways back, and both are taken — settled 2026-09-23.** First setup is the one operation that rewrites
every install at once, so the launch backup alone is too blunt to undo it:

- **A labelled backup immediately before the write**, so there is a restore point meaning *"just before I
  standardised"* rather than *"whatever the last launch happened to catch."* It covers every install the
  operation touches, because the operation spans them.
- **An [Edit History](file-manager.md#edit-history) entry for every file whose content is unique**, so one
  installation's `DeviceMappings.xml` or one `.buttonMap` can be walked back on its own without rolling back
  everything else. At first setup the installations disagree — that is why it is running — so this is one
  entry each, which is the state worth returning to. *Reworded 2026-10-03: this said "an entry per file
  written", which after [standardisation](overview.md#every-install-gets-the-same-files--settled-2026-09-23)
  would mean several byte-identical copies of one file on every later Apply. See
  [what gets kept](file-manager.md#edit-history).*

This makes Edit History a **dependency of first setup**, not a later nicety — worth knowing, since nothing of
it exists in code yet.

**Show everything, ask about little.** Every difference appears in the list, but only the two genuine
conflicts above require an answer. A device present in just one install, or an orphan `.buttonMap`, has one
sensible outcome and is resolved without a question.

### An install that appears later

It is backed up, then brought to the master. **Unless it already holds custom entries of its own** — then that
install goes through first setup rather than being silently overwritten.

### What the master actually is

**For device identity, the master is the user's element set — not a copy of a `DeviceMappings.xml`.**
Holding a whole file as the master would mean pushing one install's stock section into another, which is never
wanted even though the stock sets agree. `.binds` and `StartPreset` are different: they are shared, whole
files, and a file master is exactly right for them.

**The user's elements go at the top of `<Root>` — settled 2026-10-04.** When two elements claim one VID/PID,
[the first in the file wins](domain-knowledge/EliteDangerous-DeviceMappings-ButtonMap.md#12d-two-entries-for-one-vidpid-the-first-in-the-file-wins--measured-2026-10-03),
so a user's entry written after Frontier's is silently ignored. Writing the user's elements first means
anything they set up on purpose is what the game uses. Frontier's elements keep their order beneath them. *(A refinement to
[multi-install-proposal.md](../../multi-install-proposal.md) §2, which describes the master as a file for all
four domains.)*

## Divergence between installs — one list, ranked by consequence

**Added 2026-09-23.** Every install holds the same device files
([Every install gets the same files](overview.md#every-install-gets-the-same-files--settled-2026-09-23)), so
anywhere they disagree is something to resolve. Alias Designer is where that is shown and where the user
decides — it is [offered, never forced](overview.md#offered-never-forced--settled-2026-09-23).

**One list, not three.** Every kind of disagreement appears together, because splitting them would mean
BindForge deciding which kinds deserve the user's attention. What ranks them instead is **a severity, shown
as the row's colour**:

| | Meaning | Examples |
|---|---|---|
| **Green** | consistent — every install says the same thing | the device matches the master everywhere |
| **Yellow** | wrong, but nothing breaks | an orphan `.buttonMap` with no entry behind it; labels that differ between installs; an entry for a device no binding references |
| **Red** | will stop bindings working | a device named in `.binds` has no entry in some install; the same name pointing at different hardware in two installs |

**Severity is judged against the shared `.binds`, not against the device files alone.** That is what separates
red from yellow: a missing entry only costs the user something when a binding actually names that device.
The [measured case](domain-knowledge/EliteDangerous-DeviceMappings-ButtonMap.md#12b-what-happens-when-a-device-name-has-no-entry--measured-2026-09-22)
is red by that rule — 144 bindings naming two sticks one install had never heard of, and the whole preset
rejected — while the orphan `LVWAP.buttonMap` sitting beside it is yellow, because nothing resolves through it.

**Colour is the row's text colour, not a fill or a pill** — the HUD canon's rule for state, and the same
decision taken for the sync badge. See
[the UI component map](ui-component-map.md#2-statusbadge-draws-a-pill).

**The ways out**, offered per row: merge the installs' entries, overwrite one install from another, or take
one install as the master. Which of these is offered depends on the row's kind, and none of them runs until
the user picks one.

## VID/PID Is Not a Stable Identity

**Confirmed 2026-09-08 from five `.binds` specimens spanning 2024-08 to 2026-09.** This document and
[Device Provenance](../../03-data-models/device-provenance.md) both described VID/PID as *stable hardware
identity*. It is stable against renaming. **It is not stable against the vendor.**

| Snapshot | Device A | Device B |
|---|---|---|
| 2024-08, `.4.1` | `3344 C3F3` | `3344 03F3` |
| 2025-05, `.4.2` backup | `3344 83F3` | `3344 43F4` |
| 2026-09, current entries | `3344 83F4` (LVWAP) | `3344 03F5` (RVWAP) |

Two devices, six PIDs, none repeating. **The control group is in the same files:** a Razer device
(`1532 0244`), `T-Rudder` and `vJoy` are byte-identical across all three snapshots. Only the VIRPIL
devices moved.

**The cause is firmware, not the user.** VIRPIL's older *VPC Configuration Tool* exposed the PID as
a user-settable field; a firmware update between the first two snapshots changed it, and the newer *VPC
Configurator* forced another firmware change and **removed the ability to set it at all**. So the
user cannot pin it, cannot predict it, and finds out afterwards.

### Why this matters more than a renumbering

**The failure is silent.** The `DeviceMappings.xml` entry still exists. `.binds` still says `RVWAP`. The
game simply finds no device matching that entry, so the bindings do nothing, with no error and nothing
to search for. A user experiences it as *"my stick stopped working after an update."*

**And an aliased device is the recoverable case.** Because `.binds` references the alias rather than the
hex, the repair is **one PID field, edited once and applied everywhere**, and every binding survives.
*This used to read "one PID field per installation", from the design where each install was edited
separately.* A device with **no** entry
has its raw hex in every binding, and the same firmware update orphans all of them — in the specimen
above, 131 bindings across two devices.

This is the argument that
[reclassified `DeviceMappings.xml` as not cosmetic](overview.md#why-devicemappingsxml-is-not-cosmetic--reclassified-2026-09-08).

### Detecting it, and getting ahead of it

BindForge is the only component holding live hardware and file entries side by side, which makes both
halves of this its natural job.

**After the fact — an entry that matches nothing.** An entry whose VID/PID matches no attached device,
beside an attached device matching no entry, is the signature of exactly this event. Offering to move the
entry onto the new PID turns a silent breakage into one click.

**Before the fact — devices referenced by hex.** A user whose `.binds` names devices by raw hex is
one firmware update away from losing that layout and has no way to know it. BindForge can see the state
and count the exposure: *"Two attached devices are referenced by hardware ID rather than by name. 131
bindings depend on those IDs and would not survive a firmware update. Create aliases?"*

### Device identity is the user's to confirm — settled 2026-09-12

The question was how confident a match has to be before BindForge offers it. **The answer is that
BindForge does not score confidence at all.**

**Because it cannot.** There is no reliable device identity to compare:

- **No serial number.** Both DualShock 4 specimens report `iSerialNumber 0x00`, and their Windows
  instance IDs differ only by USB port — see [Specimens](reference-data/SPECIMENS-README.md).
- **VID/PID is provably unstable.** Two VIRPIL devices carried six PIDs across three snapshots, from
  firmware updates alone — [above](#vidpid-is-not-a-stable-identity).
- **One entry can cover several controllers.** Frontier's `<DualShock4>` matches both DS4 revisions
  through `<Alternative>` pairs.

Same VID with an unmatched device is suggestive, never proof: a user may equally have unplugged
one stick and plugged in another from the same vendor. **A percentage threshold here would be fake
precision — a number defending a guess.**

**The user knows, and is the only one who does.** They know whether they flashed firmware or
bought a new stick. So BindForge states what it found and asks:

```
Your .binds references 334483F3.
That device is not attached.

Attached instead: VIRPIL Constellation ALPHA (334483F4)
Same vendor, different product ID.

131 bindings point at the missing ID.

Is this the same device?      [ Yes ]  [ No ]  [ Not now ]
```

**What the dialog owes the user** is evidence, not a verdict: the ID in the file, what is
attached instead, which halves match, and **how many bindings are at stake.** The binding count is
what makes the question answerable — 131 is worth a moment's thought, 2 is not.

| Answer | What happens |
|---|---|
| **Yes** | the alias is created and pointed at the attached device, in the draft. Every binding follows, because they reference the name — the one-field fix [described above](#why-this-matters-more-than-a-renumbering). |
| **No** | nothing is written. The bindings stay orphaned, and BindForge **says so plainly** rather than falling silent, since that is the state the user just confirmed. |
| **Not now** | nothing is written and **the question is not asked again until the hardware situation changes.** Deferral is not a soft no, and re-asking on every launch turns a useful prompt into something to dismiss reflexively. |

**This is the same shape as [Restore](file-manager.md#restoring-onto-a-machine-whose-installations-differ--reworked-2026-09-26)
and the [foreign-backup device list](file-manager.md#how-bindforge-tells-case-1-from-case-3--settled-2026-09-12):**
where hardware identity is genuinely unknowable, BindForge asks a question the user can answer
instead of computing an answer it cannot.

## What Changes When Hardware Changes

**Settled 2026-09-12**, walking the scenarios a user actually hits. They reduce to **two
operations**, and which one runs is the user's answer rather than BindForge's deduction.

| What BindForge sees | What it usually means | Operation |
|---|---|---|
| A device appears, nothing missing | **Addition** — a stick added to the setup | **Create**: new `DeviceMappings.xml` entry, new `.buttonMap` |
| A device is missing, and something unrecognised is present | **Replacement, or a firmware update** | **Ask**, then **retarget**: the entry keeps its name, its VID/PID changes |
| Missing, and the user says none of the candidates is it | **A genuinely different device** | **Create**, and the old bindings stay orphaned |
| A device is missing, nothing new present | **Unplugged, or sold** | **Nothing.** No prompt, no change |

**A firmware update is not a third case.** It presents exactly as a replacement — an ID that is gone and
an ID that is new — and BindForge cannot distinguish a reflash from a warranty swap. It does not need to:
*"yes, this is the same device"* produces the correct outcome for both.

**Addition is only simple when Frontier does not already know the device.** The stock
`DeviceMappings.xml` ships **51 device elements carrying 136 VID/PID pairs**
([the built-in list](#built-in-devices); *corrected 2026-10-06 from "139 device elements"*). A controller already in there is already named by the game, and
there is nothing for BindForge to create.

**A missing device is not an error.** Hardware gets unplugged. The entry stays valid, the bindings stay
intact, and the device works again when it comes back. BindForge says nothing — there is no question to
ask and nothing to fix.

### Ask about one missing device at a time

**The one-missing-one-new pairing does not survive contact with reality.** This user's own files:

| Snapshot | LVWAP | RVWAP |
|---|---|---|
| 2024-08 | `C3F3` | `03F3` |
| 2025-05 | `83F3` | `43F4` |
| current | `83F4` | `03F5` |

**Both sticks changed PID in the same session, twice.** That is the VPC Configuration Tool flashing both
devices at once, which is how firmware updates normally happen for a two-stick setup — not an edge case.
So BindForge routinely sees *two* missing and *two* new, all from one vendor, with **no way to know which
new ID is the left stick.** Pairing them by order or by proximity would silently swap a user's
joystick and throttle bindings.

**So each missing device is presented on its own, listing every unmatched candidate:**

```
LVWAP (334483F3) is not attached.
131 bindings depend on it.

Which attached device is it?
  ( ) VIRPIL Constellation ALPHA   334483F4
  ( ) VIRPIL MongoosT-50CM3        334403F5
  ( ) None of these

                          [ OK ]  [ Not now ]
```

One mechanism covers one change, two, or five. **"None of these"** is what routes to *create* instead of
*retarget*, and **"Not now"** defers without recording a wrong answer — see
[the three answers](#device-identity-is-the-users-to-confirm--settled-2026-09-12).

**Deferred, not rejected: press-to-identify.** Asking the user to press a button on the missing
device would identify it with certainty rather than by inference, and Elite-Intel already has the input
capture to do it. It is a better answer where the hardware is attached and working, and it is worth
revisiting once the capture path is in place. It cannot be the *only* answer, because the device may be
absent — which is frequently why it is missing.

### Retargeting keeps the bindings, and says what it cannot promise

A retarget edits one field. Every binding follows, because `.binds` references the **name**, not the ID —
the whole reason the indirection is worth having
([above](#why-this-matters-more-than-a-renumbering)).

**That is complete and correct for a firmware update or an identical unit.** The hardware is the same, so
the buttons are where they were.

**It is not the whole story for a different model.** `.binds` records buttons **by index** — `Joy_5`, not
"the pinkie switch". `Joy_5` on a Constellation Alpha is not the same physical button as `Joy_5` on a
MongoosT-50CM3. The retarget succeeds, nothing errors, and some controls are simply in the wrong place.
The `.buttonMap` labels are wrong in the same way, for the same reason.

**So the bindings move, and BindForge names the risk rather than hiding or pre-empting it:**

```
Bindings moved to VIRPIL MongoosT-50CM3.

131 bindings kept. They refer to buttons by
number, and this device has a different layout,
so some may now be in unexpected places.

  [ Review in Bind Editor ]   [ Dismiss ]
```

**Why keep them rather than start clean.** A user replacing a broken stick with a similar one has a
layout that mostly works, and rebuilding 131 bindings by hand to avoid a handful of misplaced ones is the
worse trade. Keeping them is also **reversible** — it lands in the draft, and Discard puts everything
back. Starting unbound destroys work that no later step can recover.

**BindForge does not try to detect whether the model differs.** It has no map of which physical button is
`Joy_5` on arbitrary hardware, and guessing would produce a confident wrong answer. The warning is shown
whenever a retarget crosses to a device the user picked from the candidate list, and a firmware
update that keeps the same layout costs them one dismissed dialog.

## Automatic Device Registration

Any controller connected to the system — whether already connected when BindForge starts, or plugged in later during a session (hot-plug) — is automatically added to the My Devices working copy with no user action required. This applies the same "detect and inform, don't ask" principle already used for [First Run](#first-run-and-hot-plug) below, extended to individual devices as they're seen.

- **Default alias:** the name the Device Service reports, **cleaned into a valid name** by the [default-name rule](#the-default-name-rule) — `Virpil Controls 20220720` becomes `VirpilControls20220720`. [Onboarding](#onboarding--every-controller-gets-a-name-and-a-buttonmap--settled-2026-09-13) shows it to the user before anything is written.
- **VID/PID:** filled in immediately from the connected hardware, same as any other device entry.
- **A `.buttonMap` is created once the device has a name** — at [onboarding](#onboarding--every-controller-gets-a-name-and-a-buttonmap--settled-2026-09-13) for a controller with no entry, or at [Elite-Intel startup](#at-elite-intel-startup-a-buttonmap-for-every-connected-named-controller) for a connected controller that already has one. *Changed 2026-09-13: this used to say no `.buttonMap` stub is created until the user labels something — see [why the rule moved](#it-is-the-alias-confirm-gate-moved-earlier).*
- **Registration itself touches only the working copy.** Detecting a controller writes nothing. The writes that follow it — a confirmed name, a generated `.buttonMap` — are among the few that reach a game installation outside Apply, and each is justified in [What onboarding writes, and when](#what-onboarding-writes-and-when) and in [Overview — writes outside Apply](overview.md#writes-outside-apply--the-complete-list).
- **Hot-plugging into BindForge is safe; hot-plugging into a running game is not — those are different things.** BindForge can register a newly-connected controller into its own working copy at any time without risk. Physically connecting or disconnecting a controller while Elite Dangerous itself is running is a separate, game-level risk (it can crash the game) that exists independently of BindForge and is outside BindForge's control — automatic registration does not cause or worsen this; it just means BindForge's own device list stays current regardless of when the user plugs something in.

### Rules automatic registration still needs

Three gaps were found while working the design through. **Two were settled by onboarding on 2026-09-13**, and the third earlier:

- **Name sanitisation — settled 2026-09-13.** The reported name becomes an **XML element tag** in
  `DeviceMappings.xml` and a **filename stem** for `.buttonMap`, and SDL-style names like
  `Virpil Controls 20220720` are neither. [The default-name rule](#the-default-name-rule) cleans them, and
  [alias validation](#fields-and-rules) now holds typed names to the same constraint.
- **Collision with Frontier's built-ins — settled 2026-09-13.** A controller that *is* a built-in already
  has Frontier's entry, matched on VID/PID, so it is never onboarded and [never renamed](#built-in-controllers-in-my-devices--settled-2026-09-13). A *different*
  controller reporting a clashing name gets a number — `T-Rudder` from an unrelated device becomes
  `TRudder2`. See [the default-name rule](#the-default-name-rule).
- **What reaches the game's file.** Registration only touches the draft, so nothing lands in a real install
  until **Apply to Game Installs** runs. **Settled 2026-09-06: applying pushes only what changed**, never the
  whole set. Pushing everything accumulates entries for hardware plugged in once, years ago, and rewrites
  rows no one touched, and an entry for a device the player never named carries no benefit — no binding
  depends on it. *(This used to say "since both files are purely cosmetic"; the decision stands, but that
  reason did not — see [Protection is uniform](overview.md#protection-is-uniform--settled-2026-09-21).)* A narrow write is also a smaller thing to get wrong, and a smaller diff to show the
  player when a merge has to be explained.

## Onboarding — every controller gets a name and a `.buttonMap` — settled 2026-09-13

**Every connected controller ends up with a name and a `.buttonMap`** (Alan, 2026-09-13). The
[capture dialog](bind-editor.md#capture-dialog--settled-2026-09-13) labels a controller's inputs from its
`.buttonMap`, and Frontier ships one for only 2 of its 51 device entries
([stock reference](reference-data/FrontierStock-README.md)), so without this most controllers would show bare
`Joy_N`. A `.buttonMap` is named after its device's `DeviceMappings.xml` entry, so every file needs a name first
— and giving a controller a name is what onboarding does.

### It is the alias-confirm gate, moved earlier

This used to be ruled out: *no `.buttonMap` stub is created* until the user labels something. The reason was
never the file. It was that a `.buttonMap` should only exist under **a name the user has looked at**, because
renaming afterwards is a [three-file transaction](#renaming-a-device). Onboarding keeps that property and drops
the wait: the name is shown before anything is written. **Silent creation under a name nobody saw is still ruled
out.**

### When, and for which controllers

| Moment | Controllers |
|---|---|
| The first time BindForge is opened, alongside [First-Time Startup](overview.md#first-time-startup) | every connected controller with **no `DeviceMappings.xml` entry** |
| A controller detected later — [hot-plug](#first-run-and-hot-plug) | that controller, if it has no entry |

**A controller that already has a name is never asked** — a Frontier built-in such as `SaitekX56Joystick`, or an
entry the user made. There is no decision to put to them.

### What it asks: just a name

```
3 controllers need a name so BindForge and the game can label their buttons.

VIRPIL Controls 20220720      VID 3344  PID 83F4
NAME  [ VirpilControls20220720          ]

                                        [ Skip ]  [ Next ]
```

One field per controller, prefilled with the [default name](#the-default-name-rule). **Button labels are
generated**, not asked for — `Button 1`…`Button N`, `Hat 1 Up`/`Down`/`Left`/`Right`, `X Axis` — sized to
what the device reports. Labelling every button by pressing it would turn seconds into minutes for a thirty-button
stick. The generated labels can be renamed later in the [Device Editor](#device-editor) at no cost, because
renaming a label does not rename the file.

**Skipping takes the default name.** A controller never leaves onboarding unnamed; skipping is how a user
who wants to get on with it says *use yours*. That keeps onboarding to a few clicks however many controllers are
attached.

### The default-name rule

The name every controller is offered, and the constraint typed names are held to, because the name becomes an
**XML element tag** in `DeviceMappings.xml` and the **filename stem** of its `.buttonMap`:

1. Start from the name the device reports.
2. **Keep letters and digits only** — spaces and punctuation are dropped.
3. **It must start with a letter and be at least 3 characters.** If it does not start with a letter, or is
   shorter than 3, prefix `Device`; if nothing is left at all, use `Controller`.
4. **It must be at most 50 characters.** A longer name is cut to 50.
5. **It must not clash** with an existing entry or one of Frontier's built-in names, compared ignoring case, `-`
   and `_`. On a clash, add the lowest number that makes it unique — shortening the name first when that is
   what keeps it within 50.

*Steps 3 and 4 added 2026-09-13*, so the default always meets [alias validation](#fields-and-rules)'s 3–50
character limit instead of offering a name the user would then be told is invalid.

| Reported | Default | Why |
|---|---|---|
| `Virpil Controls 20220720` | `VirpilControls20220720` | spaces dropped |
| `T-Rudder`, from a device that is not Frontier's T-Rudder | `TRudder2` | clashes with `T-Rudder` once `-` is ignored |
| `3Dconnexion SpaceMouse` | `Device3DconnexionSpaceMouse` | must start with a letter |
| `G9` | `DeviceG9` | at least 3 characters |
| `Thrustmaster Hotas Warthog Flight Stick And Throttle Combined Edition` | `ThrustmasterHotasWarthogFlightStickAndThrottleComb` | cut to 50 |
| the same, when that name is taken | `ThrustmasterHotasWarthogFlightStickAndThrottleCom2` | shortened so the number fits |

A name the user **types** may also use `-` and `_` — see [alias validation](#fields-and-rules).

**Settled while building it — 2026-10-06 (Alan).** Built as `bindforge.devices.DeviceNames`.

- **"Letters" means ASCII `A–Z`, `a–z`, with accents folded first.** XML would accept `Contrôleur` as a tag, but
  the same text is a filename the game opens, and nobody has tested what it does with one. Folding keeps the
  name readable — `Contrôleur X` becomes `ControleurX` — and any other script is dropped, so `手柄 12` becomes
  `Device12`. A **typed** name is held to the same: a non-ASCII letter is refused.
- **Numbering starts at 2**, as `TRudder2` shows, and the lowest free number wins. The name is shortened by
  as many characters as the number has digits, so the tenth copy of the cut Warthog name ends `…Co10`.
- **A device's own current name is not a clash** when its rename is validated — `Lvwap` to `LVWAP` is allowed.
  The caller leaves it out of the names it passes. How renaming or shadowing a built-in treats Frontier's name
  is the [Device Editor](#device-editor) and [rename](#renaming-a-device) slices' to settle.

### What onboarding writes, and when

| The controller | Written | When |
|---|---|---|
| **Not referenced in `.binds`**, or referenced only by name | its `DeviceMappings.xml` entry and a generated `.buttonMap` | **straight away**, to every detected installation |
| **Referenced in `.binds` by VID+PID hex** | the entry, the `.buttonMap`, **and** a rewrite of every `Device="<hex>"` to the new name | **together, in one Apply** |

**Why the two cases differ.** The first creates things that did not exist and changes no existing entry or
binding — the same reasoning that lets
[First-Time Startup apply immediately](overview.md#it-applies-immediately--settled-2026-09-12). The second rewrites
existing bindings, which only Apply ever does.

**And the second case's writes have to land together.** Once an entry exists, `.binds` is expected to name the
device ([format §4.0](domain-knowledge/EliteDangerous-BindsFileFormat.md#40-the-rule-confirmed-2026-09-08)), and
there is no evidence the game honours the old hex references in the meantime — when this user added `RVWAP`
and `LVWAP`, the hex was replaced by hand. Writing the entry on its own could leave those bindings dead until
Apply. See [testing item 13](../../00-overview/testing-required.md).

Onboarding says so rather than leaving a surprise: *"131 bindings will move to LVWAP when you apply."* That is the
[before-the-fact offer](#detecting-it-and-getting-ahead-of-it), folded into naming.

### At Elite-Intel startup: a `.buttonMap` for every connected, named controller

A controller that **already has a name** but no `.buttonMap` gets a generated one when Elite-Intel starts, with no
prompt, because there is nothing to decide. That covers Frontier's built-ins, which almost never ship one.

- **Connected controllers only.** A device plugged in once, years ago, gets nothing.
- **Every detected installation**, since `.buttonMap` files live inside each one.
- **Never overwrites** a `.buttonMap` that exists, whether Frontier's or the user's.
- **After RESET LABELS** on a built-in, the next startup generates its `.buttonMap` afresh.

It runs at Elite-Intel startup rather than when BindForge opens, so the labels are already there the first time
the capture dialog is used. **Startup never names anything:** a controller with no name waits for onboarding.

**Settled while building it — 2026-10-06 (Alan).** Built as `bindforge.devicefiles.ButtonMapGeneration`
(`ButtonMapLabels` for the labels), run by `bindforge.devices.ButtonMapStartup`.

- **It acts on every connect, not only at startup.** The Device Service gives no signal that its first
  enumeration is done, but it reports every controller present then as a connect — so this covers startup
  exactly, and a named controller plugged in later meets the same rule. Naming an unnamed one is still
  onboarding's.
- **"Named" is per installation:** the first element in *that installation's* `DeviceMappings.xml` claiming the
  controller's VID/PID, primary or `<Alternative>` — the one the game uses
  ([domain doc §1.2d](domain-knowledge/EliteDangerous-DeviceMappings-ButtonMap.md#12d-two-entries-for-one-vidpid-the-first-in-the-file-wins--measured-2026-10-03)).
  Until first setup the installations can disagree, and each gets the file its own name needs.
- **A device the game draws itself is skipped** *(after review)*: an element carrying `<SupportsIcons>` — in
  Frontier's file the X52, X52 Pro, T.Flight HOTAS 4 and T.Flight Hotas One — and `<GamePad>`. Frontier's own
  [readme for `.buttonMap`](reference-data/Frontier-ButtonMap-Readme.txt) says a label may be an icon token such as `[x52b1]`
  ([domain doc §2.4](domain-knowledge/EliteDangerous-DeviceMappings-ButtonMap.md#24-label-values)), and these
  devices get the game's icons, so `Button 3` would put text where an icon was. A pad on `<GamePad>` is bound
  by the game's own `GamePad_` tokens, so `Joy_` labels on it would never show. *`<DualShock4>` is generated
  as usual — unverified whether the game binds it by `Joy_` tokens.*
- **A device the master holds labels for is skipped.** Apply writes its file from the master, so a missing one
  is drift for [the startup check](overview.md#for-the-device-files--settled-2026-10-05-alan) to report and
  revert. The master is never written here.
- **Create-only to the end:** the file is written beside the target and published under its name by a hard
  link, which refuses atomically when the file exists, on Windows and Linux alike; where a volume refuses hard
  links, a move that does not replace is used instead. A file that appears meanwhile is kept. Nothing is
  replaced, so nothing goes to Edit History.
- **A controller with any axis gets all eight axis labels, `X Axis` through `V Axis`** *(after review; replaces
  labelling by position)*. SDL numbers a controller's axes without gaps, while the game names them by which axis
  they are: a stick reporting X, Y and RZ is axes 0, 1 and 2, and nothing says which is which, so labelling by
  position would label Z and leave the twist raw. A label for an axis the device lacks is never shown, so all
  eight miss nothing. *"Sized to what the device reports"* above holds for buttons only.
- **No hat labels.** The Device Service reads no hats, and in Elite Dangerous a hat mostly reports as buttons,
  which get `Button N` like any other — so the `Hat 1 Up` labels [onboarding](#what-it-asks-just-a-name) mentions
  are not generated.
- **Labels are English and fixed** — file content the game displays, not Elite-Intel's interface text.
- **Started as a service**, `BUTTON_MAP_STARTUP` in `AppController.buildServices`, after `DEVICE`. Diagnostics
  mode stubs it, as it does the other services touching game files.
- **After RESET LABELS it regenerates only if the file is gone.** Generation never replaces, so RESET LABELS
  (the [Actions](#actions-and-what-each-one-reaches) slice) must delete the `.buttonMap` in every installation
  for the line above about a fresh file to hold.


## Device Editor

The editor **expands inline beneath the device's row**, one device open at a time. Opening another closes the
first; clicking the open row closes it. There is no separate editor panel and no "no device selected" state —
a collapsed list is the resting state.

### One record, and where it lands — reworked 2026-09-26

**There is one record per device, and the user edits the master.** `DeviceMappings.xml` and `.buttonMap` are
duplicated per installation, but BindForge keeps those copies identical, so there is nothing to choose between
— see [Every install gets the same files](overview.md#every-install-gets-the-same-files--settled-2026-09-23).
One alias, one set of axis and button labels, applied everywhere on Apply.

*This replaces the original design, where the expansion carried a **tab per installation** and a **MIRROR**
button decided which installations shared a definition. Both existed to express deliberate per-install
difference, which is no longer a state BindForge supports: divergence is not a configuration the user chose,
it is a latent outage. Reconciling installs that already disagree is
[a first-setup question asked once](#first-setup--reconciling-the-installs--settled-2026-09-23), not a
per-device switch.*

#### The install strip — a readout, not a switch

The strip along the top of the expansion stays, and it is **read-only**. It no longer selects which
installation you are editing; it shows **which installations the user has, and whether each one matches the
master for this device**:

```
 Steam  M     Epic  M     Frontier  •
        └─ matches the master        └─ does not
```

- **Each installation is named, with its folder path**, as before. The path is what makes a failed push
  comprehensible: when Apply reports *"updated 2 of 3"*, the user can see which one and where it lives.
- **The `M` indicator means "Matches"** — this installation's entry for this device is identical to the
  master's. It is a **fact being reported, not a setting being toggled**, which is the whole difference from
  the button it replaces.
- **It carries the same severity colours as the
  [divergence list](#divergence-between-installs--one-list-ranked-by-consequence)**, rather than inventing a
  second vocabulary: green where the installation matches, yellow where it differs in a way that breaks
  nothing, red where a binding names this device and the installation has no entry for it. Colour is the
  text's colour, never a fill or a pill, per the HUD canon.

**What the strip compares.** The installation against the **saved master** — never against the unsaved draft
in front of the user. Otherwise every keystroke would flip three indicators to "differs", which is true and
useless. Unsaved work is Edit Mode's job to report; the strip answers a different question: *what is actually
on disk out there.*

**Its scope is this device.** The whole-configuration view, ranked and with the ways out, remains the
[divergence list](#divergence-between-installs--one-list-ranked-by-consequence). The strip is not a second
home for drift reporting and offers no repairs — it is the same fact, narrowed to the device already open,
at the moment the user is editing it.

#### Two states, not three

A device is either **configured** in the master or **not added**. The original design's third state — "entry
edited independently of the other installations" — no longer exists. Where an installation's file disagrees
with the master, that is drift to be resolved, not a configuration to be preserved.

### Fields and rules

- **VID/PID are always read-only**, always derived from the physical hardware — the game attaches a device by
  VID/PID, so hand-editing this would break the association. They are device-level, identical on every tab.
- **Edit Mode** activates the moment any field changes. A persistent label reminds the user this saves to the
  working copy only.
- **Alias name validation — tightened 2026-09-13.** 3–50 characters; **must start with a letter**; then
  letters, digits, `-` and `_` only; **no spaces and no period**; must not collide with any existing entry,
  including Frontier's built-ins, compared [ignoring case, `-` and `_`](#the-default-name-rule). *The earlier
  rule allowed spaces and a wider set of punctuation. But the alias becomes an XML element tag and a
  filename stem, and a space is valid in neither — so that rule would have accepted a name the game
  cannot read.*
- **The alias must be confirmed or changed before button/axis naming unlocks.** [Onboarding](#onboarding--every-controller-gets-a-name-and-a-buttonmap--settled-2026-09-13)
  is where that happens. Confirming the offered name, typing another, or skipping — which accepts the
  default — all count, because in each case the user has been shown the name. A device is never fully
  configured under a name nobody looked at. A later rename is the [multi-file transaction](#renaming-a-device).
  **A built-in has no name to confirm:** its labels unlock straight away, and its name stays
  [Frontier's](#built-in-controllers-in-my-devices--settled-2026-09-13).
- **Button and axis names** are edited in Axis/Buttons sub-tabs. The UI sizes dynamically to the device's real
  axis and button count — never hardcoded; a device with three axes shows three. A hard 18-character limit
  applies to every button/axis friendly name, confirmed by direct in-game testing. The alias field has its own
  rule (3–50 characters) and no in-game display slot, so truncation is not a concern there.
- **Live highlighting:** pressing a button or moving an axis on the open device highlights the corresponding
  row, letting the user identify raw input codes by feel. With the device list itself being the selection, there
  is no separate "which controller am I pressing" step to get wrong.

### Actions, and what each one reaches

| Action | Scope | Touches a game installation? |
|---|---|---|
| **SAVE** | the device's record in the draft | **No** — draft only |
| **DISCARD** | the draft — whether one device or all of it is for the Actions slice to settle, see below | No |
| **CLEAR** | the device's entry and its `.buttonMap` — not offered on a built-in | No — see below |
| **RESET LABELS** | a built-in's `.buttonMap` labels | No — draft only; see [built-ins](#built-in-controllers-in-my-devices--settled-2026-09-13) |
| **APPLY TO GAME INSTALLS** | **every installation**, and reports per installation | **Yes** |

*The **MIRROR** action is gone, along with the per-installation scoping that qualified CLEAR and RESET LABELS.
There is one record, so every action reaches it and Apply carries it everywhere.*

*The table said SAVE's scope was "the device's record in the master" while marking it "draft only". The draft
is right; corrected 2026-10-04.*

**Where the draft lives — settled 2026-10-04 (Alan).** In two database tables shaped like the master's
(`12003__bindforge_device_draft.sql`, `BindForgeDeviceDraftManager`), not in a working folder of files: the
master is already rows, so Apply is a plain copy. The first edit copies the **whole** master set into the
draft; no draft means no saved edits. **Apply copies the draft into the master *before* pushing**
(`bindforge.devicefiles.DeviceApply`), so a write that fails loses nothing — the edits sit in the master, the
installation that missed them stops matching it, and the next Apply retries.

**A device the draft removed is kept as a pending removal** (`12004__bindforge_device_pending_removal.sql`,
added after review the same day). The push never removes an entry, so the device leaves the master while its
entry and `.buttonMap` stay in every installation; the list is stored in the same transaction as the Apply, so
a crash before the cleanup forgets nothing, and a name put back in the master stops being pending. Whatever
removes the files (CLEAR) works from that list. **A rename does not:** its old `.buttonMap` is found through
`previous_name`, which Apply carries into the master. **Master ids change on every Apply**, because the
master is emptied and refilled, so a device is identified by name across one.

**DISCARD is still two things in this spec**, and which the button does is the Actions slice's to settle:
throwing away edits not yet saved ([Unsaved Work on Exit](overview.md#unsaved-work-on-exit)), or putting the
draft back to the master (*"Discard puts everything back"* under
[retargeting](#retargeting-keeps-the-bindings-and-says-what-it-cannot-promise)). The draft supports both a
per-device revert and dropping the whole draft. **A started draft may hold no edits** — the editor starts one
before reading the rows it changes — so "a draft exists" is not "the user changed something".

**SAVE is deliberately the safe one.** It is the button people press by habit, so it must never be the button
that writes into a live game installation. Applying is a separate, deliberate action at the bottom of the
screen.

**APPLY reports per installation, and says so when it cannot finish.** *"Updated 3 of 3"* on the good day,
*"updated 2 of 3 — Frontier install not reachable"* on the bad one. Silent partial success is exactly the
drift this model exists to remove, so a push that half-lands must be visible
([what standardisation owes, #2](overview.md#every-install-gets-the-same-files--settled-2026-09-23)). After a
partial apply the install strip shows it: the installation that missed the write stops matching the master.

**What Apply does per installation — settled 2026-10-04 (Alan), built in `bindforge.devicefiles.DeviceFilesPush`:**

- **Partial success for every cause, each one named.** A missing folder and a failed write are treated alike:
  that installation is left as it was and reported with the reason — *"Epic: folder not found"*, *"Frontier:
  DeviceMappings.xml is locked by another program"* — while the others are written. *A rename is the exception,
  and needs all of them or none; see [below](#binds-goes-last-and-only-if-every-installation-succeeded).*
- **Within one installation, all or nothing.** Its `DeviceMappings.xml` and its `.buttonMap` files change
  together. If one write fails, the ones already written there are put back, so no installation holds a new
  entry with old labels.
- **No `DeviceMappings.xml` at all → rebuilt from Frontier's.** Without the file the game loads no bindings,
  so it is seeded from the stock copy BindForge carries, the user's elements are added, and the report says so.
- **A `.buttonMap` is written whole from the master.** Labels the master no longer holds — after RESET LABELS,
  or an axis the device stopped reporting — leave the file too. The replaced file is kept in
  [Edit History](file-manager.md#edit-history). A device with no labels gets no file.
- **A built-in still under Frontier's name gets its `.buttonMap` only.** Its element is left exactly where and
  as it is, per the table above.
- **Apply adds and updates; it does not remove.** CLEAR, a rename and RESET LABELS each remove something, and
  each brings its own list of what to remove — the master alone cannot tell a cleared device from an entry
  BindForge never owned.
- **A file already matching the master is not rewritten**, so a second Apply with nothing new writes nothing.

**CLEAR, not Delete.** A device cannot be removed from the list while its hardware is attached — it would
simply reappear, because the list is driven by what is plugged in. What CLEAR removes is the device's
`DeviceMappings.xml` entry and its matching `.buttonMap`, returning the device to the state it had before it
was ever configured. The device stays listed, showing *not added*.

**CLEAR now reaches every installation, so it asks first.** Under the original design it was scoped to one
installation and its mirrors, and a user could clear a single install by turning MIRROR off first. Neither is
true any more: there is one record, and clearing it clears the device everywhere on the next Apply. That is a
wider blast radius than the button used to have, so it takes a confirm that **names the count** — *"Clear
RHVCAPDirection from all 3 installations?"* — using
[`HudConfirmDialog`](ui-component-map.md), never `JOptionPane`.

This also replaces the old "delete and recreate to fix a wrong VID/PID" repair path, which never worked as
written: automatic registration would re-add a connected device immediately.

## Renaming a Device

A rename is **not** a single-field edit. The alias is the primary key of a three-link chain:

```
alias → <element tag> in DeviceMappings.xml → <alias>.buttonMap filename → possibly Device= in .binds
```

So renaming has to be handled as one all-or-nothing operation:

1. Rename the element tag in `DeviceMappings.xml`.
2. **Rename** the `.buttonMap` file — never create a second one. An orphaned `OldName.buttonMap` sits in the
   game's install folder where nothing will ever clean it up.
3. Scan `.binds` for `Device="<old name>"` and rewrite or refuse. This is the dangerous one: `.binds` can
   reference a device *by name* as well as by VID/PID hex, so a rename can silently orphan bindings.

### A rename spans every installation — added 2026-09-26

**Steps 1 and 2 happen once per installation.** `DeviceMappings.xml` and the `.buttonMap` files are
duplicated per install; only `.binds` is shared. So on a three-install machine a rename is not three file
operations, it is **seven**: two in each installation, plus the one shared `.binds`.

| File | Copies | Step |
|---|---|---|
| `DeviceMappings.xml` | one **per installation** | 1 |
| `<alias>.buttonMap` | one **per installation** | 2 |
| `.binds` | **one, shared by all of them** | 3 |

*The original three-step list was written when a rename meant one installation's files. The steps are
unchanged; what changed is how many times the first two run.*

#### `.binds` goes last, and only if every installation succeeded

**The shared file is the one that decides whether a half-finished rename is survivable.** Rename the entry in
two installations, rewrite `.binds` to the new name, then fail on the third, and that third installation is
now reading a `.binds` that names a device it has never heard of. It
[rejects the entire preset and falls back to KEYBOARD & MOUSE in all four sections](domain-knowledge/EliteDangerous-DeviceMappings-ButtonMap.md#12b-what-happens-when-a-device-name-has-no-entry--measured-2026-09-22)
— measured, not predicted. The only symptom is a line in `BindingLoadingErrors.log`.

So the order is fixed:

1. **Rename in every installation first.** All of them, or the rename does not proceed.
2. **Only then rewrite `.binds`.**

**If any installation cannot be written — a locked file, an unmounted drive, a permission refusal — nothing
is written at all.** Not the installations that would have succeeded, and certainly not `.binds`. The user is
told which installation blocked it and why, and the old name stays everywhere. A rename that never started
costs a retry; a rename that half-landed costs a working controller until someone reads a log file.

**This is the one place Apply's "updated 2 of 3" reporting is not good enough.** A partial *push* leaves an
installation stale, which the [install strip](#one-record-and-where-it-lands--reworked-2026-09-26) shows and
the next Apply fixes. A partial *rename* leaves it broken, because the shared file moved underneath it.
Partial success is acceptable for the first and not for the second.

**Roll back what was already done.** If installation three fails after one and two were renamed, the two
completed renames are undone before reporting the failure — the files were named for a backup-covered write,
so the previous names are recoverable. Leaving them renamed while `.binds` still says the old name is the
same breakage in the other direction.

**Why the alias-confirm gate matters more than it looks.** Requiring the alias to be settled before button and
axis naming unlocks means a `.buttonMap` is only ever created under a name the player has already looked at.
A rename *before* any file exists is free. [Onboarding](#it-is-the-alias-confirm-gate-moved-earlier) keeps
that property while creating the file straight away: the name is shown before anything is written.

**Answered 2026-09-08:** `.binds` names a device by its `DeviceMappings.xml` element name when an entry
matches, and falls back to VID+PID hex when none does
([format §4.0](domain-knowledge/EliteDangerous-BindsFileFormat.md#40-the-rule-confirmed-2026-09-08)). So step 3
is real: a rename that skips it orphans every binding on that device. *This line used to say the question
was blocked on testing.*

## First Run and Hot-Plug

If no draft exists yet, BindForge auto-imports `DeviceMappings.xml` and all `.buttonMap` files from every
detected installation, with no prompt — a status message informs the user this is happening. If no
installation is detected, the draft starts empty and any connected devices are added via
[Automatic Device Registration](#automatic-device-registration) above.

### A missing controller is worth exactly one warning — settled 2026-09-24

**Elite-Intel says once, at startup, which named controllers are not attached** — *"controller such and such
is missing"* — and then waits and sees. Nothing is changed, nothing is repaired, and the message is not
repeated.

**Why so light a touch.** The user is expected to connect their controllers before starting the game;
if they do not, the game will not load those bindings and they find out immediately in the cockpit. BindForge
cannot fix that from outside, and a missing stick is usually a cable, not a configuration problem.

**What it must not do** is treat an absent controller as a reason to change anything. An entry for hardware
that is not plugged in stays exactly where it is — entries deliberately outlive hardware, which is what makes
a stick work again when it is plugged back in. The
[`ATTACHED` / `MISSING` column](#my-devices) is the standing version of this, and the startup line is the
once-per-run version.

*Not to be confused with a **control** disappearing from the game itself — a different case, in the
[version-bump merge](overview.md#what-the-startup-check-actually-decides).*

**~~Settled 2026-09-06 — first import takes the player's files exactly as they are.~~ Superseded 2026-09-23
by [first setup](#first-setup--reconciling-the-installs--settled-2026-09-23).** That ruling said BindForge
would preserve both entries where two installations disagreed, leave the device **unmirrored**, and let the
user mirror it later.

**What replaced it, and what survived it.** Standardisation removed the end state it aimed at — a device can
no longer be left deliberately different per installation. What survived is its actual argument: *a first run
cannot tell a deliberate difference from an accidental one, and the two mistakes do not cost the same.*
First setup still never flattens silently. It reads every installation, shows what disagrees, and **asks**
— once, per element, with the user choosing which value wins. So nothing is adopted without being seen, which
is what the 2026-09-06 decision was protecting; it just gets resolved at setup instead of deferred
indefinitely.

## Exit Prompt

If the user navigates away from BindForge (or exits Elite-Intel) with unsaved Alias Designer changes, the
same two-option dialog described in [Overview — Unsaved Work on Exit](overview.md#unsaved-work-on-exit)
interrupts: **Save** or **Discard**. Neither touches a game installation.

## Deferred

- **Recalibrate** — a feature to fix axis centre-point drift during live highlighting. Out of scope for this iteration.
- **Duplicate-VID/PID handling — dropped from scope 2026-09-07; nothing is designed and nothing needs to be.**
  The existing `DeviceDuplicateWarningEvent` tells the player, and the alias-uniqueness rule below rejects the
  second automatic registration on its own. Retained for the reasoning: Two connected devices of the identical model genuinely can report the same VID and PID (this is normal USB behaviour, not an edge case) — combined with [Automatic Device Registration](#automatic-device-registration), both would attempt to auto-register under the same default hardware-reported alias, which the alias-uniqueness rule would reject. Neither `.binds` nor `DeviceMappings.xml` has any field that could distinguish two entries sharing a VID/PID, and the game itself keys on VID/PID, so it could not act on a distinction even if BindForge drew one. That is why nothing is designed here rather than deferred: there is no design that would help. See [conflicts-and-open-questions.md](../../99-archive/conflicts-and-open-questions.md).

## Design Considerations — Closed

**The master-copy question is closed — in favour of the master, reversing the earlier answer (2026-09-24).**
Device configuration is edited as a **draft against the master**, and Apply writes the master out to every
installation. There is still no two-tier master-plus-shadow model; what changed is which side is
authoritative. See [Master](../../00-overview/glossary.md#bindforge-terms) in the glossary for the reversal and
why, and [The master and the draft](overview.md#the-master-and-the-draft--settled-2026-09-24). The superseded
2026-09-06 reasoning is kept in the archive at
[Conflict 3.6](../../99-archive/conflicts-and-open-questions.md#36-working-copy-model--resolved-neither-original-option--a-third-sharper-model).
