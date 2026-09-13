# BindForge — Alias Designer

**Purpose:** manage `DeviceMappings.xml` and `.buttonMap` files, giving connected controllers friendly names shown both in BindForge's own Bind Editor and in the game's own controls UI.

## Structure

A single flat section with no sub-tabs: a device list, with the editor expanding inline beneath whichever device is open.

Earlier drafts moved per-installation control into a separate "Installation Mirroring" screen, first inside Alias Designer and later inside [File Manager](file-manager.md). **Both are gone.** Which installations hold an entry for a device is a property *of that device*, so it belongs on the device's own row rather than on a screen the user has to go and find. See [Per-Installation Editing](#per-installation-editing).

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
| **Installations** | one in-row tab per *detected* installation |

The first three describe the hardware and are true no matter how many installations exist. **Everything else
is per installation** — the alias, whether an entry exists, whether a `.buttonMap` exists — and none of it can
honestly be reduced to a single cell. Two installations can disagree, and a list that averages them is lying.
So those facts live on the in-row installation tabs, and the row itself carries no alias column.

Each in-row tab shows the installation name plus one marker: **M** when that installation is mirrored, or
*not added* when it holds no entry for this device. Clicking a tab opens the device on that installation.

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
the device is not *not added* — but the entry is not the commander's either, and the grid says which. The match
is made against the [shipped stock reference](reference-data/FrontierStock-README.md): `Thrustmaster T-Rudder`
at `044F:B679` is Frontier's `<T-Rudder>`.

**Its name and VID/PID are locked** (Alan, 2026-09-13). A built-in element is Frontier's **definition**, not a
label on one stick: `<DualShock4>` matches both DualShock 4 revisions, and `<GamePad>` carries 80 VID/PID pairs,
so renaming one renames every controller it covers. And a game update that restores Frontier's file would put
the old name back, leaving `.binds` pointing at a name that no longer exists.

**Its button and axis labels are editable.** They are saved as a `.buttonMap` under Frontier's name —
`T-Rudder.buttonMap` — which changes nothing about how the game recognises the device. Labels unlock straight
away, because there is no name to confirm.

**RESET LABELS replaces CLEAR.** [CLEAR](#actions-and-what-each-one-reaches) removes an installation's entry,
and on a built-in that entry is Frontier's. RESET LABELS removes only the `.buttonMap` the commander or BindForge
created, and the next [Elite-Intel startup](#at-elite-intel-startup-a-buttonmap-for-every-connected-named-controller)
generates a fresh one. A `.buttonMap` Frontier itself shipped — `VPCPanel`, `VPCThrottle` — is never removed.
Like SAVE and CLEAR, it **follows MIRROR**: resetting one mirrored installation resets them all.

| | The commander's entry | Built-in |
|---|---|---|
| Name | editable, as the [rename transaction](#renaming-a-device) | **locked** — Frontier's |
| VID / PID | read-only | read-only |
| Button and axis labels | editable once the name is confirmed | editable straight away |
| Undo (follows MIRROR) | **CLEAR** — the entry and `.buttonMap` | **RESET LABELS** — only a `.buttonMap` BindForge or the commander made |
| [Onboarding](#onboarding--every-controller-gets-a-name-and-a-buttonmap--settled-2026-09-13) | asked for a name | never asked |
| What APPLY writes | the entry and its `.buttonMap` | the `.buttonMap` only |

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

**The cause is firmware, not the commander.** VIRPIL's older *VPC Configuration Tool* exposed the PID as
a user-settable field; a firmware update between the first two snapshots changed it, and the newer *VPC
Configurator* forced another firmware change and **removed the ability to set it at all**. So the
commander cannot pin it, cannot predict it, and finds out afterwards.

### Why this matters more than a renumbering

**The failure is silent.** The `DeviceMappings.xml` entry still exists. `.binds` still says `RVWAP`. The
game simply finds no device matching that entry, so the bindings do nothing, with no error and nothing
to search for. A commander experiences it as *"my stick stopped working after an update."*

**And an aliased device is the recoverable case.** Because `.binds` references the alias rather than the
hex, the repair is one PID field per installation and every binding survives. A device with **no** entry
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

**Before the fact — devices referenced by hex.** A commander whose `.binds` names devices by raw hex is
one firmware update away from losing that layout and has no way to know it. BindForge can see the state
and count the exposure: *"Two attached devices are referenced by hardware ID rather than by name. 131
bindings depend on those IDs and would not survive a firmware update. Create aliases?"*

### Device identity is the commander's to confirm — settled 2026-09-12

The question was how confident a match has to be before BindForge offers it. **The answer is that
BindForge does not score confidence at all.**

**Because it cannot.** There is no reliable device identity to compare:

- **No serial number.** Both DualShock 4 specimens report `iSerialNumber 0x00`, and their Windows
  instance IDs differ only by USB port — see [Specimens](reference-data/SPECIMENS-README.md).
- **VID/PID is provably unstable.** Two VIRPIL devices carried six PIDs across three snapshots, from
  firmware updates alone — [above](#vidpid-is-not-a-stable-identity).
- **One entry can cover several controllers.** Frontier's `<DualShock4>` matches both DS4 revisions
  through `<Alternative>` pairs.

Same VID with an unmatched device is suggestive, never proof: a commander may equally have unplugged
one stick and plugged in another from the same vendor. **A percentage threshold here would be fake
precision — a number defending a guess.**

**The commander knows, and is the only one who does.** They know whether they flashed firmware or
bought a new stick. So BindForge states what it found and asks:

```
Your .binds references 334483F3.
That device is not attached.

Attached instead: VIRPIL Constellation ALPHA (334483F4)
Same vendor, different product ID.

131 bindings point at the missing ID.

Is this the same device?      [ Yes ]  [ No ]  [ Not now ]
```

**What the dialog owes the commander** is evidence, not a verdict: the ID in the file, what is
attached instead, which halves match, and **how many bindings are at stake.** The binding count is
what makes the question answerable — 131 is worth a moment's thought, 2 is not.

| Answer | What happens |
|---|---|
| **Yes** | the alias is created and pointed at the attached device, in the draft. Every binding follows, because they reference the name — the one-field fix [described above](#why-this-matters-more-than-a-renumbering). |
| **No** | nothing is written. The bindings stay orphaned, and BindForge **says so plainly** rather than falling silent, since that is the state the commander just confirmed. |
| **Not now** | nothing is written and **the question is not asked again until the hardware situation changes.** Deferral is not a soft no, and re-asking on every launch turns a useful prompt into something to dismiss reflexively. |

**This is the same shape as [Restore](file-manager.md#restoring-when-nothing-matches--ask-do-not-guess)
and the [foreign-backup device list](file-manager.md#how-bindforge-tells-case-1-from-case-3--settled-2026-09-12):**
where hardware identity is genuinely unknowable, BindForge asks a question the commander can answer
instead of computing an answer it cannot.

## What Changes When Hardware Changes

**Settled 2026-09-12**, walking the scenarios a commander actually hits. They reduce to **two
operations**, and which one runs is the commander's answer rather than BindForge's deduction.

| What BindForge sees | What it usually means | Operation |
|---|---|---|
| A device appears, nothing missing | **Addition** — a stick added to the setup | **Create**: new `DeviceMappings.xml` entry, new `.buttonMap` |
| A device is missing, and something unrecognised is present | **Replacement, or a firmware update** | **Ask**, then **retarget**: the entry keeps its name, its VID/PID changes |
| Missing, and the commander says none of the candidates is it | **A genuinely different device** | **Create**, and the old bindings stay orphaned |
| A device is missing, nothing new present | **Unplugged, or sold** | **Nothing.** No prompt, no change |

**A firmware update is not a third case.** It presents exactly as a replacement — an ID that is gone and
an ID that is new — and BindForge cannot distinguish a reflash from a warranty swap. It does not need to:
*"yes, this is the same device"* produces the correct outcome for both.

**Addition is only simple when Frontier does not already know the device.** The stock
`DeviceMappings.xml` ships **139 device elements**
([the built-in list](#built-in-devices)). A controller already in there is already named by the game, and
there is nothing for BindForge to create.

**A missing device is not an error.** Hardware gets unplugged. The entry stays valid, the bindings stay
intact, and the device works again when it comes back. BindForge says nothing — there is no question to
ask and nothing to fix.

### Ask about one missing device at a time

**The one-missing-one-new pairing does not survive contact with reality.** This commander's own files:

| Snapshot | LVWAP | RVWAP |
|---|---|---|
| 2024-08 | `C3F3` | `03F3` |
| 2025-05 | `83F3` | `43F4` |
| current | `83F4` | `03F5` |

**Both sticks changed PID in the same session, twice.** That is the VPC Configuration Tool flashing both
devices at once, which is how firmware updates normally happen for a two-stick setup — not an edge case.
So BindForge routinely sees *two* missing and *two* new, all from one vendor, with **no way to know which
new ID is the left stick.** Pairing them by order or by proximity would silently swap a commander's
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
[the three answers](#device-identity-is-the-commanders-to-confirm--settled-2026-09-12).

**Deferred, not rejected: press-to-identify.** Asking the commander to press a button on the missing
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

**Why keep them rather than start clean.** A commander replacing a broken stick with a similar one has a
layout that mostly works, and rebuilding 131 bindings by hand to avoid a handful of misplaced ones is the
worse trade. Keeping them is also **reversible** — it lands in the draft, and Discard puts everything
back. Starting unbound destroys work that no later step can recover.

**BindForge does not try to detect whether the model differs.** It has no map of which physical button is
`Joy_5` on arbitrary hardware, and guessing would produce a confident wrong answer. The warning is shown
whenever a retarget crosses to a device the commander picked from the candidate list, and a firmware
update that keeps the same layout costs them one dismissed dialog.

## Automatic Device Registration

Any controller connected to the system — whether already connected when BindForge starts, or plugged in later during a session (hot-plug) — is automatically added to the My Devices working copy with no user action required. This applies the same "detect and inform, don't ask" principle already used for [First Run](#first-run-and-hot-plug) below, extended to individual devices as they're seen.

- **Default alias:** the name the Device Service reports, **cleaned into a valid name** by the [default-name rule](#the-default-name-rule) — `Virpil Controls 20220720` becomes `VirpilControls20220720`. [Onboarding](#onboarding--every-controller-gets-a-name-and-a-buttonmap--settled-2026-09-13) shows it to the commander before anything is written.
- **VID/PID:** filled in immediately from the connected hardware, same as any other device entry.
- **A `.buttonMap` is created once the device has a name** — at [onboarding](#onboarding--every-controller-gets-a-name-and-a-buttonmap--settled-2026-09-13) for a controller with no entry, or at [Elite-Intel startup](#at-elite-intel-startup-a-buttonmap-for-every-connected-named-controller) for a connected controller that already has one. *Changed 2026-09-13: this used to say no `.buttonMap` stub is created until the commander labels something — see [why the rule moved](#it-is-the-alias-confirm-gate-moved-earlier).*
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
  rows no one touched; since both files are purely cosmetic, an entry for a device the player never named
  carries no benefit. A narrow write is also a smaller thing to get wrong, and a smaller diff to show the
  player when a merge has to be explained.

## Onboarding — every controller gets a name and a `.buttonMap` — settled 2026-09-13

**Every connected controller ends up with a name and a `.buttonMap`** (Alan, 2026-09-13). The
[capture dialog](bind-editor.md#capture-dialog--settled-2026-09-13) labels a controller's inputs from its
`.buttonMap`, and Frontier ships one for only 2 of its 51 device entries
([stock reference](reference-data/FrontierStock-README.md)), so without this most controllers would show bare
`Joy_N`. A `.buttonMap` is named after its device's `DeviceMappings.xml` entry, so every file needs a name first
— and giving a controller a name is what onboarding does.

### It is the alias-confirm gate, moved earlier

This used to be ruled out: *no `.buttonMap` stub is created* until the commander labels something. The reason was
never the file. It was that a `.buttonMap` should only exist under **a name the commander has looked at**, because
renaming afterwards is a [three-file transaction](#renaming-a-device). Onboarding keeps that property and drops
the wait: the name is shown before anything is written. **Silent creation under a name nobody saw is still ruled
out.**

### When, and for which controllers

| Moment | Controllers |
|---|---|
| The first time BindForge is opened, alongside [First-Time Startup](overview.md#first-time-startup) | every connected controller with **no `DeviceMappings.xml` entry** |
| A controller detected later — [hot-plug](#first-run-and-hot-plug) | that controller, if it has no entry |

**A controller that already has a name is never asked** — a Frontier built-in such as `SaitekX56Joystick`, or an
entry the commander made. There is no decision to put to them.

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

**Skipping takes the default name.** A controller never leaves onboarding unnamed; skipping is how a commander
who wants to get on with it says *use yours*. That keeps onboarding to a few clicks however many controllers are
attached.

### The default-name rule

The name every controller is offered, and the constraint typed names are held to, because the name becomes an
**XML element tag** in `DeviceMappings.xml` and the **filename stem** of its `.buttonMap`:

1. Start from the name the device reports.
2. **Keep letters and digits only** — spaces and punctuation are dropped.
3. **It must start with a letter.** If it does not, prefix `Device`; if nothing is left, use `Controller`.
4. **It must not clash** with an existing entry or one of Frontier's built-in names, compared ignoring case, `-`
   and `_`. On a clash, add the lowest number that makes it unique.

| Reported | Default | Why |
|---|---|---|
| `Virpil Controls 20220720` | `VirpilControls20220720` | spaces dropped |
| `T-Rudder`, from a device that is not Frontier's T-Rudder | `TRudder2` | clashes with `T-Rudder` once `-` is ignored |
| `3Dconnexion SpaceMouse` | `Device3DconnexionSpaceMouse` | must start with a letter |

A name the commander **types** may also use `-` and `_` — see [alias validation](#fields-and-rules).

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
there is no evidence the game honours the old hex references in the meantime — when this commander added `RVWAP`
and `LVWAP`, the hex was replaced by hand. Writing the entry on its own could leave those bindings dead until
Apply. See [testing item 13](../../00-overview/testing-required.md).

Onboarding says so rather than leaving a surprise: *"131 bindings will move to LVWAP when you apply."* That is the
[before-the-fact offer](#detecting-it-and-getting-ahead-of-it), folded into naming.

### At Elite-Intel startup: a `.buttonMap` for every connected, named controller

A controller that **already has a name** but no `.buttonMap` gets a generated one when Elite-Intel starts, with no
prompt, because there is nothing to decide. That covers Frontier's built-ins, which almost never ship one.

- **Connected controllers only.** A device plugged in once, years ago, gets nothing.
- **Every detected installation**, since `.buttonMap` files live inside each one.
- **Never overwrites** a `.buttonMap` that exists, whether Frontier's or the commander's.
- **After RESET LABELS** on a built-in, the next startup generates its `.buttonMap` afresh.

It runs at Elite-Intel startup rather than when BindForge opens, so the labels are already there the first time
the capture dialog is used. **Startup never names anything:** a controller with no name waits for onboarding.

## Device Editor

The editor **expands inline beneath the device's row**, one device open at a time. Opening another closes the
first; clicking the open row closes it. There is no separate editor panel and no "no device selected" state —
a collapsed list is the resting state.

### Per-Installation Editing

`DeviceMappings.xml` and `.buttonMap` are genuinely duplicated per storefront installation, so **the editor
edits one installation at a time.** The expansion carries a tab per detected installation, and switching tabs
switches the whole record beneath it: alias, axis names, button names.

The header names the installation and shows its **folder path**, read-only. That path is what makes a tab
switch unambiguous — it is not merely a different data set, it is a different folder on disk.

**MIRROR**, in the action row, is what keeps the common case cheap:

- **Mirrored installations share one definition.** Edit any one of them and every other mirrored installation
  follows. Same hardware, same names, one edit — which is what nearly every player wants.
- **Turn MIRROR off** and that installation is edited on its own. This is the case that earlier designs could
  not express at all: they forced one definition on every installation.
- Turning MIRROR back on **adopts** whatever the other mirrored installations already say.

Together with whether an entry exists at all, that gives three per-installation states: *no entry*, *entry
edited independently*, and *entry shared with the other mirrored installations*.

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
  default — all count, because in each case the commander has been shown the name. A device is never fully
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
| **SAVE** | the open installation's record | **No** — draft only |
| **DISCARD** | the open installation's record | No |
| **CLEAR** | the open installation's record, **and every installation mirrored with it** — not offered on a built-in | No — see below |
| **RESET LABELS** | a built-in's `.buttonMap` in the open installation, **and every installation mirrored with it** | No — draft only; see [built-ins](#built-in-controllers-in-my-devices--settled-2026-09-13) |
| **MIRROR** | the open installation | No |
| **APPLY TO GAME INSTALLS** | every installation with an entry | **Yes** |

**SAVE is deliberately the safe one.** It is the button people press by habit, so it must never be the button
that writes into a live game installation. Applying is a separate, deliberate action at the bottom of the
screen.

**CLEAR, not Delete.** A device cannot be removed from the list while its hardware is attached — it would
simply reappear, because the list is driven by what is plugged in. What CLEAR removes is the open installation's
`DeviceMappings.xml` entry and its matching `.buttonMap` — **and, when that installation is mirrored, every
installation mirrored with it**, because MIRROR means one shared definition and SAVE already reaches all of them
(settled 2026-09-13). The device stays listed, showing *not added* for each installation cleared. Clearing every
installation returns the device to the state it had before it was ever configured. To clear just one of several
mirrored installations, turn MIRROR off on it first.

This also replaces the old "delete and recreate to fix a wrong VID/PID" repair path, which never worked as
written: automatic registration would re-add a connected device immediately.

## Renaming a Device

A rename is **not** a single-field edit. The alias is the primary key of a three-link chain:

```
alias → <element tag> in DeviceMappings.xml → <alias>.buttonMap filename → possibly Device= in .binds
```

So renaming has to be handled as one transaction:

1. Rename the element tag in `DeviceMappings.xml`.
2. **Rename** the `.buttonMap` file — never create a second one. An orphaned `OldName.buttonMap` sits in the
   game's install folder where nothing will ever clean it up.
3. Scan `.binds` for `Device="<old name>"` and rewrite or refuse. This is the dangerous one: `.binds` can
   reference a device *by name* as well as by VID/PID hex, so a rename can silently orphan bindings.

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

**Settled 2026-09-06 — first import takes the player's files exactly as they are.** No mirroring, no
reconciliation, no adopting one installation's entry over another's. Where two installations hold
different entries for the same device, both are preserved as they were found and the device is left
**unmirrored**; BindForge does not decide which one was meant. Mirroring is one click away afterwards, and
is the player's call to make once they can see both.

The reasoning is that a first run has no way to tell a deliberate difference from an accidental one, and
the failure modes are not symmetric: preserving a difference that turns out to be accidental costs one
click, while flattening a difference that turns out to be deliberate destroys work the player cannot
recover from BindForge.

## Exit Prompt

If the user navigates away from BindForge (or exits Elite-Intel) with unsaved Alias Designer changes, the
same two-option dialog described in [Overview — Unsaved Work on Exit](overview.md#unsaved-work-on-exit)
interrupts: **Save** or **Discard**. Neither touches a game installation.

## Deferred

- **Recalibrate** — a feature to fix axis center-point drift during live highlighting. Out of scope for this iteration.
- **Duplicate-VID/PID handling — dropped from scope 2026-09-07; nothing is designed and nothing needs to be.**
  The existing `DeviceDuplicateWarningEvent` tells the player, and the alias-uniqueness rule below rejects the
  second automatic registration on its own. Retained for the reasoning: Two connected devices of the identical model genuinely can report the same VID and PID (this is normal USB behavior, not an edge case) — combined with [Automatic Device Registration](#automatic-device-registration), both would attempt to auto-register under the same default hardware-reported alias, which the alias-uniqueness rule would reject. Neither `.binds` nor `DeviceMappings.xml` has any field that could distinguish two entries sharing a VID/PID, and the game itself keys on VID/PID, so it could not act on a distinction even if BindForge drew one. That is why nothing is designed here rather than deferred: there is no design that would help. See [conflicts-and-open-questions.md](../../00-overview/conflicts-and-open-questions.md).

## Design Considerations — Closed

**The master-copy question is closed, and was already closed when this pointer was written.** Device
configuration is edited against a **single draft** pushed to installations on Apply. There is no two-tier
master-plus-shadow model and no persistent master copy competing with the live game file for the role of
source of truth — see
[Conflict 3.6](../../00-overview/conflicts-and-open-questions.md#36-working-copy-model--resolved-neither-original-option--a-third-sharper-model),
resolved before the port. Confirmed again 2026-09-06: the idea is an inherited artefact of the existing
Elite-Intel bind editor, not a live option.
