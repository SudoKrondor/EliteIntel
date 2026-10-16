# BindForge — Alias Designer

**Purpose:** manage `DeviceMappings.xml` and `.buttonMap` files, giving connected controllers friendly names shown both in BindForge's own Bind Editor and in the game's own controls UI.

## Structure

A single flat section: a device list, and the [Device Editor](#device-editor) opened as a dialog from a gear at
the end of each device's row. ~~A single flat section with no sub-tabs: a device list, with the editor expanding
inline beneath whichever device is open.~~ *Changed 2026-10-10 (Alan): Swing has no row that opens beneath
itself, and a dialog has room for tabs — see [the Device Editor](#a-dialog-opened-from-the-row--2026-10-10-alan).*

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
| **Hardware name** | what the hardware reports through the Device Service, on any OS — *renamed from "Windows name" 2026-10-15 (Alan): it is the controller's own USB name on Linux too* |
| **VID / PID** | the hardware |
| **Status** | `ATTACHED` or `MISSING` — whether the hardware is present right now |
| **Alias** | the name in the master, or *not added* |
| **Installations** | one read-only marker per *detected* installation |
| *(gear)* | opens the [Device Editor](#device-editor) for this device — on every row, last, after the Installations columns *(added 2026-10-10)* |

The first three describe the hardware and are true no matter how many installations exist. **The alias is now
a column**, because there is one of them: the master's. *Under the original design there was no alias column,
because each installation could hold a different name and a single cell would have been averaging them.
[Standardisation](overview.md#every-install-gets-the-same-files--settled-2026-09-23) removed that, so the
honest cell is the master's name.*

**What stays per installation is whether each one has caught up.** Each marker names an installation and
shows **M** when that installation matches the master for this device, *not added* when it holds no entry, or
the [divergence colour](#divergence-between-installs--one-list-ranked-by-consequence) when it differs. The
markers are a readout — clicking one does not open a per-installation editor, because there is no such thing;
the row's gear opens [the one record](#one-record-and-where-it-lands--reworked-2026-09-26).

#### The Installations markers — 2026-10-08 (Alan)

Built as `bindforge.devices.InstallationMarkers` (pure), shown by `AliasDesignerPanel`.

- **One column per installation**, headed with its name, one marker per cell — not every marker in one cell,
  which would need a painter of its own ([GAP 5](ui-component-map.md#alias-designer)).
- **The colour is the [divergence list](#against-the-master--2026-10-08-alan)'s**, read from its findings, so a
  marker and the row explaining it cannot disagree. **M** is green. A difference reads **differs** — a word as well as
  a colour.
- **The text says what the installation holds; the colour says what it costs.** An entry the master holds and an
  installation lacks reads *not added* in the finding's colour — red when a binding names it. Plain *not added*
  means neither the master nor that installation holds the device. An entry only the installation holds — the master
  lacks it — is never **M**.
- **A built-in is M** where Frontier's entry is there, and *not added* where it is not: the master expects exactly
  Frontier's entry.
- **A reset `DeviceMappings.xml`** marks every device the master holds, in that one row's colour — so after a reset
  even a built-in the master labels shows the reset's colour. **An unreadable one** marks every device: nothing is
  known about any of them.
- **An installation whose folder is gone** keeps its column and reads **not found**, uncoloured.
- **Before first setup there are no Installations columns.** M, *not added* and the divergence colour are all
  defined against the master.
- **They do not read `bindforge_device_installs`**: those rows carry no labels and no severity. The colour and the
  differences come from the check against the master; which names an installation gives the hardware comes from the
  same refresh's read of its file. A file changed between the two reads can give one stale marker, which the next
  refresh corrects. The rows stay the stored record of what each installation holds — see
  [Device Provenance](../../03-data-models/device-provenance.md#what-the-record-carries).
- *After review:* **a built-in reads M only where Frontier's entry is there**, whether or not the master labels it.
  The check compares only the user's elements, so a Frontier entry gone from an installation raises no finding and
  would otherwise read as a match.

### Built-in Devices

Frontier's shipped default entries. **The reference list is read-only**, and BindForge never changes an
entry on it. A built-in controller that is actually attached also appears under **My Devices, marked
BUILT-IN**, where its button labels can be edited — see
[Built-in controllers in My Devices](#built-in-controllers-in-my-devices--settled-2026-09-13).

> ~~**Open problem — this split is not derivable from the file.** `DeviceMappings.xml` is a flat list of device
> elements with nothing marking which are Frontier's and which the player added. Any list built by reading a
> real install will mix them. Resolving this needs a shipped reference copy of Frontier's stock file plus a
> provenance record — see [Device Provenance](../../03-data-models/device-provenance.md).~~ *Resolved: the list
> is the [shipped reference](reference-data/FrontierStock-README.md) itself, carried in the jar, so no
> installation's file is read for it — see below.*

#### Built as the stock reference — 2026-10-08 (Alan)

Built as `bindforge.devices.BuiltInDeviceList` (pure) and `BuiltInDevice`, shown by `AliasDesignerPanel`.

- **The two views are `COMPACT` tabs, My Devices and Built-in Devices**, in place of the My Devices heading. The
  [divergence list](#against-the-master--2026-10-08-alan) stays beneath both: it is not a view of the device list.
- **One row per Frontier element, in the order Frontier's file gives them**, read from the reference in the jar
  (`FrontierStockDevices`) — never from an installation, whose file mixes Frontier's entries with the user's.
- **Four columns: Device, VID / PID, Also covers, Status.** VID / PID is the element's own pair. *Also covers*
  counts its `<Alternative>` pairs — `<GamePad>` reads *+79 more* — and lists every one as the cell's tooltip.
  **Status reads *Attached*** where a controller plugged in now resolves to the entry, through the first entry
  claiming its pair, as the game resolves it — so it agrees with My Devices marking the same controller BUILT-IN.
  With the Device Service stopped, the list says so in a row, as My Devices does.
- **The list says when it was captured** — *"Frontier's list as captured on 2026-09-06"* — because the reference
  belongs to one game version, and a controller Frontier added since is not on it. The date is
  `FrontierStockDevices.CAPTURED`, changed with every re-capture.
- **Not shown:** whether Frontier ships a `.buttonMap` for the entry. Two of 51 do.
- **Read-only:** nothing on the view acts. Renaming or shadowing a built-in starts from My Devices, where the
  attached controller is.
- **Found while building it:** the parser handed back an entry's pairs through `Set.copyOf`, whose order is
  salted per run, so "the first pair" of `<GamePad>` or `<DualShock4>` was a different pair on every launch — and
  `InstallationDeviceScanner` stored that pair as the element's VID/PID in `bindforge_device_installs`. The pairs
  now keep file order, primary first.

*After review — 2026-10-08 (Alan):*

- **The primary is the element's own pair, held on the entry** (`DeviceEntry.primary()`), not read as "the first
  pair". An element carrying only `<Alternative>` pairs has no primary: VID / PID is blank and every pair is counted
  under *Also covers*, and the installation scan records no pair for it rather than an alternative. No shipped
  element has that shape; a re-capture or a user's file could.
- **Dates are ISO in every language** — *2026-09-06*, not a localised form (Alan).
- **The capture date is checked against the stock README** by a test, so a re-capture that forgets
  `FrontierStockDevices.CAPTURED` fails rather than showing the old date.

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
| Button and axis labels | editable once the device has a name — ~~once the name is confirmed~~ (2026-10-10, [no confirm step](#fields-and-rules)) | editable straight away |
| Undo (reaches every install) | **CLEAR** — the entry and `.buttonMap` | **RESET LABELS** — only a `.buttonMap` BindForge or the user made |
| [Onboarding](#onboarding--every-controller-gets-a-name-and-a-buttonmap--settled-2026-09-13) | asked for a name | never asked |
| What APPLY writes | the entry and its `.buttonMap` | the `.buttonMap` only |

## First setup — reconciling the installs — settled 2026-09-23

**The handover.** Until a user opens Alias Designer, BindForge only backs up and reports
([offered, never forced](overview.md#offered-never-forced--settled-2026-09-23)). First setup is the one time
it asks anything: it turns whatever the installs currently hold into a single master set, after which every
install matches it. *"Adopt"* stays reserved for what happens later, when someone edits in the game.

**It can be opened and abandoned.** Nothing is written until the user presses the button at the end.

*How first setup is reached changed 2026-10-13 (Alan): it is no longer a step every user starts. BindForge
**activates** itself the first time its tab is opened, and first setup opens only when the installations
disagree — see [Activation](#activation--2026-10-13-alan).*

### Activation — 2026-10-13 (Alan)

**Found while planning A6b.** The 2026-09-06 ruling took the user's files with no prompt the first time BindForge
was opened. The 2026-09-23 ruling replaced it for installations that **disagree**, but the case where they agree
was never carried over, and A1 built first setup as a button every user had to press. With nothing of the user's
to set up, the master stayed empty, and an empty master was read as *not set up*. So a new player (Frontier's
files only) and a player with a custom `.binds` and stock device files could never get past **SET UP**: no APPLY,
no Device Editor, no onboarding. The second is exactly the user whose `.binds` names their sticks by hex. The
fix is activation:

- **Activation is automatic the first time the BindForge tab is opened**, with no prompt, **when the installations
  agree**. The master is filled from what they hold, which may be nothing.
- **"Agree" means identical: filling the master would write no game file.** Any difference at all opens
  [first setup](#first-setup--reconciling-the-installs--settled-2026-09-23) instead, even one with nothing to ask.
  An entry only one installation holds is shown as decided and confirmed by the dialog's final button, so no game
  file is written until the user asks ([offered, never forced](overview.md#offered-never-forced--settled-2026-09-23)).
- **When they disagree, first setup opens straight away** on that first opening. Opening BindForge is the user
  asking. The dialog can still be abandoned, it writes nothing until its last button, and until it finishes
  BindForge is not activated and **SET UP** reopens it.
- **No installation detected:** activate with an empty master. One found later is handled as
  [an install that appears later](#an-install-that-appears-later).
- **An installation whose folder is gone** is left out of "identical" and activation goes ahead, as first setup
  already skips it. When it comes back, it is judged against the master like any other installation.
- **A file that is there but cannot be read** stops silent activation, and first setup opens and names it. An
  unread file could hold the one entry the master must not lose. *As built (2026-10-13): when first setup's own read
  fails there is no list to show, so the user gets first setup's "could not read your files" message naming the
  file; when only the identical test fails to read one, first setup opens with the file named at the top.*
- **Activation is its own stored fact**, not "the master has rows". A user who owns nothing is still activated.
  Every test that read an empty master as *not set up* becomes *not activated*: the gear, APPLY, **SET UP**, and
  [the startup check's](overview.md#for-the-device-files--settled-2026-10-05-alan) "an empty master means first
  setup has not run". A master that already has rows counts as activated. One left empty by an earlier **SET UP**
  that found nothing activates the next time the tab is opened.

**Every user gets a "before BindForge" backup**, whichever path activation takes:

- **It covers all of the user's input data:** every reachable installation's `DeviceMappings.xml` and
  `DeviceButtonMaps\`, and every `.binds` and `StartPreset.#.start` in the bindings folder. On Linux that is every
  bindings folder BindForge knows of, since each Proton installation has its own
  ([input environment §7](../../00-overview/input-environment.md#7-linux-the-bindings-folder-is-not-shared)).
- **It is taken before anything is written**: at silent activation, or when first setup opens. First setup still
  takes its own labelled backup just before it writes, because the user may abandon the dialog and come back after
  the files have changed.
- **It is never pruned.** Edit History keeps a limited number of copies per file, so after enough Applies the
  files as they were before BindForge would otherwise age out.
- **It goes in the player backups folder**, as first setup's backup does, so it is listed under Player Backups.
  The restore there handles `.binds` presets only; restoring device files is
  [File Manager's](file-manager.md) work.

**The user is told once, when the backup is made** — on the first attempt to activate, which may open first setup
rather than finish: a spoken line (the AI says it) and a log line, as the [missing-controller warning](#a-missing-controller-is-worth-exactly-one-warning--settled-2026-09-24)
does. Never again after that. *Was "on first activation only"; reconciled with the build after review
(2026-10-13): the fact told is the backup, and the backup is taken once.*

**What this changes for onboarding:** it always runs after activation, so a controller `.binds` names by hex can be
named and wait for Apply from the first visit. A6b's planned "not before first setup" rule for the hex case is
withdrawn.

#### Built — 2026-10-13 (Alan, A1b)

Built as `bindforge.devicefiles.Activation`, run by `AliasDesignerController.activateOnFirstShowing()` (through the panel's), which the
outer BindForge tab (`BindingsTabPanel`) calls the first time it is shown. The fact is the `bindforge_activation`
table (migration `12005`, `BindForgeActivationManager`).

- **The BindForge tab is the outer one**, not the Alias Designer sub-tab (Alan). First setup may open over
  whichever sub-tab is showing.
- **Tried once per run.** First setup abandoned is then **SET UP**'s, and the next run's first opening offers it
  again. A backup that cannot be taken stops activation, logged, for the next run to retry.
- **"Identical" is the push's own test:** nothing for first setup to ask, nothing shadowed, and
  `DeviceFilesPush.plan` would write nothing to any reachable installation. An installation with no
  `DeviceMappings.xml` is therefore not identical — the push would seed it — and opens first setup with nothing to
  ask, whose **Continue** now stays offered so that write can be confirmed.
- **The installations are detected again before activation reads them** (`InstallationRegistry.rescan()`), since
  on a fresh database nothing is stored until detection runs, and the startup scan may still be running.
- **Activated is written in the same transaction that fills the master**
  (`BindForgeDeviceDraftManager.establish`), which now refuses once activated as well as when the master has rows.
  The migration marks a master that already has rows as activated.
- **The backup is taken once ever**, its folder recorded in `bindforge_activation.backup_folder`, as
  `<time>_before-bindforge` in the default player backups folder: each installation's device files, and the bindings folder's `.binds` and `StartPreset` files under `bindings\`. Only the configured bindings folder is backed up today. Linux detection (2026-10-14) finds each installation's own Proton bindings folder, but the installation list does not store it, so the backup does not yet cover one per installation.
- **Told when the backup is taken** — that is the one time — spoken (`speech.bindforgeBackedUp`) and logged with
  the folder.
- **Every "empty master means not set up" test reads the fact now:** the gear, APPLY and **SET UP** on the screen,
  and `DeviceFilesCheck` (`Report.activated()`).
- **Nothing prunes player backups yet.** When File Manager adds retention, the folder recorded in `backup_folder`
  must be exempt.

*After review — 2026-10-13 (`code-integrity-review.md`, Alan: all thirteen):*

- **SET UP takes the "before BindForge" backup too** (`BeforeBindForgeBackup.ensureTaken`, shared with activation,
  one at a time). Otherwise a user whose first attempt could not take it, and who then pressed SET UP, would be
  activated without it — and every later run would skip the step.
- **SET UP waits while activation runs**, so the two cannot open first setup twice.
- **A failed activation is said in the application log**, with its reason, as the backup's success is.
- **An activated user pays for no detection:** the fact is checked before the rescan. Detection itself runs one at
  a time (`InstallationRegistry.rescan()`), which closes the known collision between two scans.
- **A refresh that fails keeps the last answer** rather than reading as *not activated*.
- **The only way to activate is `establish`**, in the master's transaction; the manager has no `activate()`.
- **The resolution judged identical is the one saved** (`Activation.fillWithoutWriting` returns it).
- **A backup copied but not recorded is left in place** — a complete restore point; the next attempt takes and
  records another.

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

### Settled while building it — 2026-10-07 (Alan)

Built as `bindforge.devicefiles.FirstSetup` (the flow), `FirstSetupPlan` (the decision, pure), `FirstSetupReader`
and `DeviceFilesSnapshot`, with `ui.screen.bindings.FirstSetupDialog` opened from **SET UP** on the Alias Designer
screen, which is offered only while the master is empty.

- **A name conflict is answered only where no binding is lost.** The [rename transaction](#renaming-a-device) and
  its `.binds` rewrite are not built, so a name may be chosen only when the names it beats are not in `.binds`;
  any other choice is shown but disabled, *needs Rename*. The winner is written at the top of every installation,
  where [the first match wins](domain-knowledge/EliteDangerous-DeviceMappings-ButtonMap.md#12d-two-entries-for-one-vidpid-the-first-in-the-file-wins--measured-2026-10-03);
  each loser's element and `.buttonMap` stay on disk beneath it as a **pending removal**, for CLEAR's cleanup. When
  `.binds` uses both names, first setup cannot finish until the rename exists.
- ~~**Colliding labels are answered per device, by an installation** — the spec's *use Steam's labels for this
  device*. That installation's label wins every collision; a label only another installation holds is still
  taken.~~ *Kept as the whole-file route; the per-input rows came on 2026-10-08 — see
  [the label merge](#the-buttonmap-label-merge--2026-10-08-alan) below.*
- **"Use one installation as the master"** answers every question in its favour and still takes the elements only
  the others hold, because first setup never drops a custom entry. A question where that installation has no side,
  or its side needs Rename, is left to answer.
- **The labelled backup is the device files only**, every reachable installation's `DeviceMappings.xml` and
  `DeviceButtonMaps\`, in the **default** player backups folder as `<time>_first-setup`. First setup writes no
  `.binds`, and a backup destination the user chose is resolved only inside `PlayerBackupService`, which V1.1
  shares. The opening line says the files *will be* backed up, since the launch backup it was meant to quote is
  not built either.
- **Edit History needs nothing new:** the push keeps each replaced file's content once per unique content, which
  at first setup is one entry per installation.
- **The master is filled in one transaction, only if it is still empty, and any draft is thrown away with it.**
  An empty master can hold no saved edits, and a draft left started would replace the new master on the next
  Apply.
- **Frontier's elements are set aside by name**, as the provenance rule does. A `.buttonMap` beside one is
  mastered as labels on a built-in unless it is Frontier's own: `VPCPanel` and `VPCThrottle` now travel in the jar
  (`FrontierStockButtonMaps`) and are recognised by their labels. Generated maps from
  [Elite-Intel startup](#at-elite-intel-startup-a-buttonmap-for-every-connected-named-controller) are BindForge's,
  so they are mastered like any other.
- **An orphan `.buttonMap` is not read** — nothing resolves through it, so it gives the master nothing — and an
  unreachable installation is named and skipped. An installation whose files cannot be read stops first setup:
  an unread file could hold the one entry the master must not lose.
- **Nothing of the user's means nothing to set up.** With no custom element and no labels, the master stays empty
  and the dialog says so.

*After review — 2026-10-08 (Alan):*

- **Two names for one VID/PID inside one installation are not a question.** The game uses the first in the file,
  so a later one is a dead entry: it is shown, not taken, and becomes a pending removal. A name conflict is asked
  only where *different* installations disagree — which is what the four cases above describe. A name shadowed
  in one installation but first in another is still in play.
- **A name that wins any name conflict is kept**, even where it loses another. A device under one name on a stale
  PID in one installation, with each PID also held under a second name, must not lose the user the device — and
  "use one installation as the master" must never drop that installation's own element. Answers that would leave
  two kept devices on one VID/PID are treated as unanswered until changed.
- **A failure after the backup is named for what it is.** If the master cannot be saved, nothing was written and
  first setup can run again; if the push fails after the master is saved, first setup is over and the
  installations that missed it stop matching the master, for Apply to retry. A half-written backup is removed
  rather than left to pass for a restore point.

### The `.buttonMap` label merge — 2026-10-08 (Alan)

Built as `bindforge.devicefiles.LabelMerge` (pure) and `ui.screen.bindings.LabelMergeDialog`, opened from first
setup.

- **The device keeps one row in first setup**, *Button labels disagree*, whose answer reads *"2 of 5 chosen"*.
  Selecting it offers **Compare N inputs**, which opens the merge view, and *use Steam's labels* for each
  installation — the whole-file route — beside it, so a user who would rather not walk the list never opens it.
  The inputs were not put in the first-setup list itself: its columns are not the merge table's, and a thirty-button
  stick would bury the device questions.
- **The merge view is the table above**: Input, one column per installation holding labels, and what is kept.
  Clicking an installation's label keeps it; an installation with no label for that input shows **-** and is not a
  choice. The whole-file buttons are repeated beneath it. Cancelling changes nothing; DONE keeps a partial answer.
- **An answer is the label, not the installation.** Two installations holding the same label are one choice, and a
  third that disagrees is the other.
- **Every disagreeing input is its own question.** First setup cannot finish until each is answered, and the count
  it gives counts inputs. *Use one installation as the master* answers every input it holds a label for and leaves
  the rest — as it already did for every other question where it has no side.
- **The re-merge is not built.** `LabelMerge` takes its sides by name, not by installation, so A3's re-merge can
  pass the master as one side and reuse the dialog.

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

### Against the master — 2026-10-08 (Alan)

Built as `DeviceDivergence.againstMaster` (pure), fed by [the startup check](overview.md#for-the-device-files--settled-2026-10-05-alan)
(`DeviceFilesCheck`) through `DeviceDivergenceScanner.scanAgainstMaster`, and shown on the Alias Designer screen.

- **Before first setup the installations are still compared with each other.** With the master empty there is
  nothing to compare against, and that comparison is what shows the user why first setup is worth running. Once the
  master is filled, every row is an installation against the master.
- **One row per device and issue**, naming every installation it concerns.
- **Red when a binding names the device, yellow otherwise**, for an entry the installation lacks, one on other
  hardware than the master's, one **out of place** — below Frontier's entry for the same hardware, which
  [the game uses instead](domain-knowledge/EliteDangerous-DeviceMappings-ButtonMap.md#12d-two-entries-for-one-vidpid-the-first-in-the-file-wins--measured-2026-10-03)
  (inferred from that measurement, not measured with bindings) — and one **the master does not hold**, which is red
  when named because Apply will never put it in the installations that lack it. That entry's own labels are not
  listed: its row says it already, and they are often a generated map's thirty-odd.
- **A reset `DeviceMappings.xml` is one row** — not one per entry it lost, and not one per `.buttonMap` beside it,
  since the repair is one revert that rewrites both. *After review:* **reset to Frontier's file** it is red only
  when a binding names one of the user's own devices — the entries it lost; a binding naming a Frontier device
  the master labels still resolves. **No file at all** is always red, because the game then
  [loads no bindings](overview.md#why-devicemappingsxml-is-not-cosmetic--reclassified-2026-09-08). The check says
  which it found (`InstallationCheck.fileGone`).
- **An unreadable `DeviceMappings.xml` is one row**, red when a binding names any master device —
  [the whole preset is rejected](domain-knowledge/EliteDangerous-DeviceMappings-ButtonMap.md#12b-what-happens-when-a-device-name-has-no-entry--measured-2026-09-22).
- **Labels are one yellow row per device**, however many inputs differ; a missing or unreadable `.buttonMap` is
  yellow too. A wrong label breaks no binding.
- **One cause, one row** *(after review)*: the `.buttonMap` of an entry an installation lacks is not listed as an
  orphan as well, and a reset installation's maps are not listed at all.
- **Not listed:** an installation that matches, one whose folder is gone, and a `.buttonMap` that differs only in
  layout — nothing there for the user to judge.
- ~~**The list only reports.** The ways out — adopt, revert, and the label re-merge with the master as one side of
  [the merge view](#the-buttonmap-label-merge--2026-10-08-alan) — are the next slice.~~ *Built — see
  [the ways out](#the-ways-out--2026-10-08-alan) below.*

### The ways out — 2026-10-08 (Alan)

Built as `bindforge.devicefiles.DivergenceWaysOut` (which ways a row gets, pure), `DeviceFilesAdopt.adopt(installId,
deviceName)` and `adoptLabels`, and `LabelReMerge`, shown beneath the list on the Alias Designer screen. Selecting a
row shows its ways out as buttons, as first setup shows its choices; each asks before it runs.

| Row | Ways out |
|---|---|
| Entry missing, out of place, `.buttonMap` missing or unreadable, reset to Frontier's or gone | **Revert** |
| Same name, other hardware | **Revert**, or **adopt** from one installation — one button each, naming the VID/PID it holds (the firmware-update case) |
| An entry the master does not hold | **Adopt** from one installation. Revert would leave it where it is: the push never removes |
| Labels differ | **Compare labels** — the re-merge — or **revert** |
| `DeviceMappings.xml` unreadable | **None.** The push cannot read it either; the row says to restore it or fix it by hand |
| Orphan `.buttonMap` | **None.** CLEAR acts on a device, and an orphan has none: the row says to remove it by hand or restore it (2026-10-11, [A10](#built--2026-10-11-alan-a10)) |

- **Adopt from a row takes that device and nothing else.** Adopting the whole installation, as the startup check's
  adopt does, would also drop from the master every entry that installation lacks — fixing one stick's PID could
  lose another stick. So a missing entry is never adopted from a row: removing a device is CLEAR's, and CLEAR
  confirms it by name.
- **Revert is the whole installation**, as [C3 settled](overview.md#for-the-device-files--settled-2026-10-05-alan):
  the push brings everything there in line with the master. The confirm says how many rows of the list that reaches.
  A row naming several installations reverts each, reported per installation — *"updated 1 of 2"*, and why.
- **The backup before revert is Edit History.** The push keeps each replaced file before writing and stops if it
  cannot; no labelled snapshot is taken, since those land in the Player Backups list (a known issue). Adopt and the
  re-merge write no game file.
- **Adopt and the re-merge write the master only** — and a draft three-way, as adopt does. The other installations
  then differ from the master and appear as rows of their own, to be reverted; with no Apply button yet, that is how
  they are brought in line.
- **The re-merge's sides are the master and each installation the row names.** A label only one side holds is kept,
  as in first setup — so a label the master holds and an installation's file lacks stays in the master. Where the
  sides only add to each other there is nothing to ask, and the merge view is skipped. Every collision must be
  answered before anything is written; a `.buttonMap` that cannot be read stops the re-merge.
- **Before first setup the rows have no ways out** — there is no master to revert to or adopt into. The row says
  SET UP resolves them.

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

### Built — 2026-10-15 (Alan, A12)

Built as `bindforge.devices.IdentityQuestions` (who is asked, about what - pure), `ui.screen.bindings.IdentityDialog`,
`BindForgeDeviceDraftManager.retargetByName`, and the remembered answers in `bindforge_identity_answer` (migration
`12007`, `BindForgeIdentityAnswerManager`), run by `AliasDesignerController`. Alan answered four questions, one at a time:

- **Named controllers only.** A12 asks about a controller the master names. One the bindings know only by its hardware
  id - the spec's first sketch, *"Your .binds references 334483F3"* - is **slice A17**: a yes there must name the new
  controller *and* move bindings from the **old** hex to the name, which Apply's hex move cannot find (it looks for a
  device's current VID/PID), so it needs a stored "old id → name" record of its own.
- **Asked only when bindings use the missing controller**, by its current, master or old name - the
  [missing-controller warning's](#built--2026-10-11-alan-a13) rule. The question exists to save bindings; an entry for a
  stick sold years ago is not worth one whenever a new controller is plugged in. Frontier's names are never asked about.
- **Not now and None of these are remembered across restarts**, keyed on the missing controller and the set of
  candidates the question listed. *"Until the hardware situation changes"* is that set changing - a controller plugged
  in or taken away - or the missing controller being attached again, which forgets the answer.
- **While a question is deferred, onboarding leaves its candidates alone.** Naming a candidate would end the question
  unasked. The automatic offer and the hot-plug offer skip them (a hot-plugged one keeps waiting); **CHECK AGAIN** asks
  the deferred questions first, then onboards whatever is left; **NAME** from the Device Editor names the controller,
  since that is the user choosing - and naming it takes it out of every question's candidates, which is None of these
  without a stored row.

*Also settled while building it (Alan approved them with the sources):*

- **The list form, not Yes / No / Not now.** One dialog per missing controller, a row per candidate marked *same vendor,
  different product ID* where it is, and a last row **None of these**. **OK** takes the selected row; **Not now**, the
  close glyph and Esc defer.
- **A candidate** is an attached controller onboarding would ask about (not Frontier's, no entry, not in the master)
  that the draft does not already hold - a controller named for Apply, or one a retarget points at, is spoken for. A
  controller picked for one question drops out of the next; a question left with no candidate is not put.
- **Yes retargets in the draft** (`retargetByName`: name and labels kept, the VID/PID changed, in one transaction with
  starting the draft). Apply writes the new `<VID>`/`<PID>` to every installation, as it brings any element into line
  with the master; `.binds` is not touched, since the name does not change. A device whose hardware changed between the
  question and the answer writes nothing and says so.
- **The notice says the bindings follow when you apply**, not that they moved - nothing reaches the game until then -
  and names the risk: bindings and labels keep their button numbers. **Review in Bind Editor** opens the Binding
  Profile tab.
- **Where it runs:** in the refresh, after catch-up and before onboarding, under onboarding's conditions - activated,
  the hardware listed, the user on the screen, no write running, no other dialog open. None of these asks for
  onboarding's offer on the next refresh. A question put once in a run is not put again in that run unless CHECK AGAIN
  asks, so a write that fails cannot bring the dialog straight back.
- **The automatic onboarding offer no longer opens over another dialog**, as the hot-plug offer already did - a refresh
  can land while the identity questions are showing.

## Automatic Device Registration

Any controller connected to the system — whether already connected when BindForge starts, or plugged in later during a session (hot-plug) — is automatically added to the My Devices working copy with no user action required. This applies the same "detect and inform, don't ask" principle already used for [First Run](#first-run-and-hot-plug) below, extended to individual devices as they're seen.

- **Default alias:** the name the Device Service reports, **cleaned into a valid name** by the [default-name rule](#the-default-name-rule) — `Virpil Controls 20220720` becomes `VirpilControls20220720`. [Onboarding](#onboarding--every-controller-gets-a-name-and-a-buttonmap--settled-2026-09-13) shows it to the user before anything is written.
- **VID/PID:** filled in immediately from the connected hardware, same as any other device entry.
- **A `.buttonMap` is created once the device has a name** — at [onboarding](#onboarding--every-controller-gets-a-name-and-a-buttonmap--settled-2026-09-13) for a controller with no entry, or at [Elite-Intel startup](#at-elite-intel-startup-a-buttonmap-for-every-connected-named-controller) for a connected controller that already has one. *Changed 2026-09-13: this used to say no `.buttonMap` stub is created until the user labels something — see [why the rule moved](#it-is-the-alias-confirm-gate-moved-earlier).*
- **Registration is the row appearing, and writes nothing** — ~~touches only the working copy~~ *(reworded
  2026-10-15, A7: since [the list became the union](#my-devices) of live hardware and file entries, a detected
  controller is listed without being stored anywhere, draft included).* Detecting a controller writes nothing. The writes that follow it — a confirmed name, a generated `.buttonMap` — are among the few that reach a game installation outside Apply, and each is justified in [What onboarding writes, and when](#what-onboarding-writes-and-when) and in [Overview — writes outside Apply](overview.md#writes-outside-apply--the-complete-list).
- **Hot-plugging into BindForge is safe; hot-plugging into a running game is not — those are different things.** BindForge can register a newly-connected controller into its own working copy at any time without risk. Physically connecting or disconnecting a controller while Elite Dangerous itself is running is a separate, game-level risk (it can crash the game) that exists independently of BindForge and is outside BindForge's control — automatic registration does not cause or worsen this; it just means BindForge's own device list stays current regardless of when the user plugs something in.

### Built — 2026-10-15 (Alan, A7)

Built as `bindforge.devices.HotPlugOffers` (who is still owed an offer — in memory, tested) and the Device Bus
subscription in `AliasDesignerController`. Alan approved these as the sources' defaults and answered one question:

- **Once per run per controller** (Alan). A controller offered once — by the automatic offer after activation, by
  hot-plug, or from NAME — is not offered again when it is plugged back in, whatever the answer: a loose cable or a
  hub resetting would otherwise put the dialog up again and again. **CHECK AGAIN**, NAME and the next run still ask,
  as [A6b](#settled-before-building-it--2026-10-14-alan-a6b) set out.
- **Plugging in or out reads the screen again while the user is looking**, so the row appears or turns `MISSING`
  straight away. While the screen is hidden it is marked stale and read again when next shown — nothing reads the
  disk for a plug nobody sees.
- **The offer follows the automatic offer's rules:** only once activated, once the hardware is listed, while the
  user is on the screen; never while catch-up is asking or a write is running. **It never opens over another dialog**
  — the Device Editor included — and waits for a later refresh instead.
- **Several plugged in together are one dialog**; one unplugged before its offer stops waiting.
- **Every connect waits; onboarding's own rule decides who needs a name.** A named controller, or a built-in, is
  offered silently and to no one; its `.buttonMap` is [startup generation's](#at-elite-intel-startup-a-buttonmap-for-every-connected-named-controller),
  which already acts on every connect.
- **The controllers present at startup are the automatic offer's.** The screen registers on the Device Bus when it is
  built, and the offer after activation asks about every attached controller, those plugged in meanwhile included.
- **Nothing is done on the Device Bus's thread** but recording the plug — no file or database read — since it also
  carries push-to-talk.

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
- **What reaches the game's file.** Registration ~~only touches the draft~~ writes nothing *(2026-10-15, A7)*; the
  name onboarding then gives is written [as onboarding writes it](#what-onboarding-writes-and-when), and anything else
  lands in a real install only when **Apply** runs. **Settled 2026-09-06: applying pushes only what changed**, never the
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

#### Settled before building it — 2026-10-14 (Alan, A6b)

- **Onboarding runs only once BindForge is [activated](#activation--2026-10-13-alan).** There is no "before first
  setup" case, so a controller `.binds` names by hex is offered from the first visit and waits for Apply.
- **Three ways in.** Automatically, once per run, on the first showing of the BindForge tab right after activation,
  when any attached controller needs a name — those attached at that moment; one plugged in later is
  [hot-plug's](#first-run-and-hot-plug). From the gear: an unnamed device's editor offers
  **NAME**, which opens onboarding for that one controller. And from **CHECK
  AGAIN**, which offers it again for every attached controller still needing a name, and says nothing when none does *(
  changed 2026-10-14, Krondor: with only the first two, a user who backed out found CHECK AGAIN doing nothing visible —
  a support ticket each time)*.
- **Everything is written at the end.** Skip and Next only move between pages; **FINISH** writes every name in one
  pass and reports per installation, listing each controller that waits for Apply and how many bindings will move.
  BACK, the close glyph or Esc leave having written nothing, without asking — only typed names are lost — and the controllers stay unnamed until
  **CHECK AGAIN**, the gear, or the next run.
- **A partial Apply does not hold the `.binds` rewrite back.** The installations that took the entry have it, and
  `.binds` moves to the name; one that missed it is named in the report and
  [caught up when it comes back](#actions-and-what-each-one-reaches). Only when **no** installation took the entry is
  `.binds` left on the hex — nothing was applied for it to follow — and the controller is reported as not named
  yet, for the next Apply. *The [rename](#binds-goes-last-and-only-if-every-installation-succeeded) keeps its own
  all-or-nothing rule for now; whether it follows this one is A11's question.*

#### Built — 2026-10-14 (Alan, A6b)

Built as `bindforge.devices.Onboarding` (the pages, pure), `ui.screen.bindings.OnboardingDialog`,
`bindforge.devicefiles.DeviceOnboarding` (the writes), `BindForgeDeviceDraftManager.onboard` (one transaction),
`DeviceMappingsMerge.insert`, `BindsDeviceReferences.hardwareIdCounts` and `hardwareIdsFor`, and Apply's move in
`DeviceApply` through `BindsDeviceRewrite`. Edit History for the device files is now one class, `EditHistory`,
shared by the push and onboarding.

- **Apply moves bindings for any master device of the user's that `.binds` still names by hardware id**, not only
  one onboarding named. The test is derived — a master device whose VID and PID appear as hex in `.binds` — so a
  controller named for Apply needs no record of its own, a move that could not run is retried by the next Apply,
  and a hand-added entry whose bindings were never moved is put right the same way. Once an entry exists `.binds`
  is expected to name it ([format §4.0](domain-knowledge/EliteDangerous-BindsFileFormat.md#40-the-rule-confirmed-2026-09-08)).
  One of Frontier's names is never a target. Two master devices on one VID/PID: the first in the master's order
  takes the bindings, as it leads every file and is the one the game resolves.
- **The insert changes nothing but the new element.** Unlike the push it does not bring another element's VID/PID
  into line or move it — that would be a revert the user did not ask for. The new element takes its place in the
  master's order among the user's elements leading the file, so a file that matched the master still does.
- **A `.buttonMap` already under the name is kept**, as startup generation keeps one, and the report says so; the
  divergence list shows the difference.
- **A `.binds` that cannot be read stops onboarding** before it opens: without it, a controller the bindings name by
  hex would be named straight away and its bindings left on the id.
- **The automatic offer waits for the hardware to be listed**, as the missing-controller warning does — before then
  every controller reads absent, and the run's one offer would be spent on nobody.
- **NAME is offered for a controller onboarding would ask about**: attached, not Frontier's, and named in no
  installation. One an installation names is given its name by adopting that entry.
- **A controller named for Apply opens under its draft name** from the gear, rather than offering NAME again, and is
  not asked about again until Apply makes it the master's.
- **Labels are what startup generation gives the controller**, read from it at the moment it is named.

*After review (2026-10-14, `code-integrity-review.md`; Alan: eleven fixed, two left):*

- **One rewrite per device, back to back** (Alan, option 1 of three). Each ends on the game's `.binds`, which the
  bindings monitor reloads, so a second device's rewrite in the same Apply can meet a working copy mid-reload and
  stop as a changed file. That device is reported, its bindings stay on the hardware id, and the next Apply moves
  them. One pass for several devices would need a rewrite with several targets in `bindforge.io`, a foundation-branch
  change; one device per Apply was the third option.
- **A move that found nothing says so.** When the game saved `.binds` between the count and the rewrite, the report
  says no binding named the controller any more, not that its bindings moved; a move reports the bindings it moved
  in the game's own files.
- **A move that stopped gives its reason in words**; the status is in the log.
- **The automatic offer also waits for the user to be on the screen**, and coming back to it asks again — a refresh
  the user did not start no longer opens the dialog over another screen.
- **Two unnamed controllers on one VID/PID:** only the first page promises the bindings; the second is named
  straight away.
- **NAME uses onboarding's own rule** (`DeviceNames.needingName`), and one onboarding keeps one Edit History copy per
  content across identical installations, as the push does.

### At Elite-Intel startup: a `.buttonMap` for every connected, named controller

A controller that **already has a name** but no `.buttonMap` gets a generated one when Elite-Intel starts, with no
prompt, because there is nothing to decide. That covers Frontier's built-ins, which almost never ship one.

- **Connected controllers only.** A device plugged in once, years ago, gets nothing.
- **Every detected installation**, since `.buttonMap` files live inside each one.
- **Never overwrites** a `.buttonMap` that exists, whether Frontier's or the user's.
- **After RESET LABELS** on a built-in, the next startup generates its `.buttonMap` afresh.

It runs at Elite-Intel startup rather than when BindForge opens, so the labels are already there the first time
the capture dialog is used. **Startup never names anything:** a controller with no name waits for onboarding.

**Only once BindForge is activated — 2026-10-14 (
Krondor).** Before activation it writes nothing into a game folder. Run on every start, it put generated files into every installation before the
["before BindForge" backup](#activation--2026-10-13-alan) was taken, so that backup did not show the files as they were before BindForge — and it wrote into the game folders of users who had never opened BindForge. Activation is the first opening of the tab, before any capture dialog, so the labels are still there in time: when activation happens (`BindForgeActivatedEvent`, published by `Activation.establish` for silent activation and first setup alike), the controllers connected then are handled.

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
- **Only the axes the controller has — 2026-10-10 (Alan, A8b).** An X, Y and RZ stick gets `X Axis`, `Y Axis`
  and `RZ Axis`, read from [what the controller has](#which-axes-and-hats-a-controller-has--2026-10-10-alan), so
  an unplugged stick's saved labels name the axes it really has. An axis the game cannot see gets no label. Maps
  already on disk keep their eight, since generation never replaces.
  ~~**A controller with any axis gets all eight axis labels, `X Axis` through `V Axis`** *(after review; replaces
  labelling by position)*.~~ *Kept for a controller whose HID description cannot be read, or counts other than
  SDL does (after review, 2026-10-10):* SDL numbers a
  controller's axes without gaps, while the game names them by which axis they are: a stick reporting X, Y and RZ
  is axes 0, 1 and 2, and nothing says which is which, so labelling by position would label Z and leave the twist
  raw. A label for an axis the device lacks is never shown, so all eight miss nothing. *"Sized to what the device
  reports"* above holds for buttons only.
- **Four labels per hat — 2026-10-10 (Alan, A8b):** `Hat 1 Up`, `Hat 1 Right`, `Hat 1 Down`, `Hat 1 Left`, after
  the buttons and axes, for each hat SDL reports. A hat the firmware reports as buttons is buttons, and gets
  `Button N`. ~~**No hat labels.** The Device Service reads no hats, and in Elite Dangerous a hat mostly reports
  as buttons, which get `Button N` like any other.~~ *"Mostly" is not "always" — a stick can be set to report a
  true hat.*
- **Labels are English and fixed** — file content the game displays, not Elite-Intel's interface text.
- **Started as a service**, `BUTTON_MAP_STARTUP` in `AppController.buildServices`, after `DEVICE`. Diagnostics
  mode stubs it, as it does the other services touching game files.
- **After RESET LABELS it regenerates only if the file is gone.** Generation never replaces, so RESET LABELS
  (the [Actions](#actions-and-what-each-one-reaches) slice) must delete the `.buttonMap` in every installation
  for the line above about a fresh file to hold.

### Which axes and hats a controller has — 2026-10-10 (Alan)

Built in A8b as `elite.intel.devices.model.DeviceDescription`, read by `elite.intel.devices.hid.DeviceDescriber`
as each controller connects and held by `DeviceService.description(deviceId)`. SDL gives an axis count and
numbers the axes without gaps; it cannot say which axis is which. The Device Editor, live highlighting and the
generated labels all need to.

- **SDL3 for what the controller is doing, HID for what it is.** SDL keeps live input, hot-plug and push-to-talk.
  The HID description is read once, at connect, for one fact SDL lacks. Reading live input through HID instead
  would rebuild all of SDL, and on Linux `/dev/hidraw` needs a udev rule a user would have to install.
- **Read from the HID description on both platforms** — `hid.dll` on Windows (opened by the device path SDL's
  DirectInput backend gives), `report_descriptor` under `/sys` on Linux, which every user can read. DirectInput
  itself was the more correct source on Windows but is COM, called by hand through JNA, where one wrong offset
  crashes the whole app, push-to-talk with it. A failed HID read is a status code.
- **An axis is named by the game slot it takes** — X, Y, Z, RX, RY, RZ, U, V, `Joy_XAxis` to `Joy_VAxis`, which
  are DirectInput's eight slots — not by its HID name. DirectInput fills them by kind: each of X…Rz takes the
  first axis of its kind, and U and V the first two slider-kind axes, in the order the description lists them.
  The kinds are Wine's (`dlls/dinput/joystick_hid.c`, read 2026-10-10), which Proton runs and which models
  Windows: **Slider, Dial and Simulation Throttle are sliders**, Wheel is Z, Steering X, Accelerator Y, Rudder and
  Brake Rz. *Not measured — [testing items 17, 18 and 22](../../00-overview/testing-required.md#bindforge).*
- **The game sees at most eight.** An axis whose slot another took, or past the second slider, is counted as one
  the game cannot see: no label, and the Device Editor can say *"2 more axes the game cannot use"* rather than
  seem to have missed them.
- **SDL's axis k is the k-th axis in SDL's order, and only when nothing is in doubt.** SDL's DirectInput backend
  sorts axes by slot (`SDL_dinputjoystick.c`, `SortDevObjects`); its evdev backend lists them by the kernel's code,
  where Slider is `ABS_THROTTLE` and Dial `ABS_RUDDER` whatever the description's order (`hid-input.c`). The
  numbering is matched only when every axis has a slot, no two share a kernel code, and SDL counts exactly those
  axes; otherwise it is left unmatched and no axis is lit by identity. *All four of the user's controllers come
  through SDL's DirectInput backend — their GUIDs end in its zero signature —
  [testing item 21](../../00-overview/testing-required.md#bindforge).*
- **When the description cannot be read, nothing is guessed.** The axes are *unknown*, and every consumer does
  what it did before A8b — generation labels all eight.
- **Hats come from SDL**, `SDL_GetNumJoystickHats`, the same source that will report their movement, so count and
  movement cannot disagree. On Windows SDL lists them in DirectInput's order, the game's `POV1`, `POV2` —
  [testing item 23](../../00-overview/testing-required.md#bindforge).
- **`Device` and its events are unchanged.** The description is a separate record looked up by device id, so
  `DeviceService` — which V1.1 also changes — gains one short block and nothing else.
- ~~**Not built here: hat movement.** Lighting a hat needs `DeviceService` to poll hat state, which is
  [A9's](../../05-build-plan/alias-designer.md#slices) to add when it lights one.~~ *Built in A9:* `DeviceHatEvent`,
  published on change.

*After review — 2026-10-10 (Alan):*

- **A description that counts other than SDL is not trusted for labels** (`DeviceDescription.agreesWith`): one
  finding no axis where SDL counts three has missed something, such as a vendor-defined usage, and leaving those
  axes unlabelled costs more than labelling all eight. A9's AXES tab should ask the same question.
- **The Windows path is matched ignoring case.** SDL upper-cases it (`QueryDevicePath` in `SDL_dinputjoystick.c`),
  but Windows itself often gives them in lower case, and a mismatch would silently leave every controller unknown.
- **The Linux descriptor is read as the kernel reads it** (`hid-core.c`): an Input gives as many fields as the
  larger of its report count and its usages, and of delimited alternative usages only the first set is kept —
  since what the kernel makes of it is what SDL then counts.
- **A description's axes are held in the game's order, each once**, whoever builds it.
- **Reading the description runs on the device thread at connect**, before the connect is published — which is what
  makes `description()` race-free. It is a few system calls; nothing may add per-poll HID work there.


## Device Editor

~~The editor **expands inline beneath the device's row**, one device open at a time. Opening another closes the
first; clicking the open row closes it. There is no separate editor panel and no "no device selected" state —
a collapsed list is the resting state.~~ *Replaced 2026-10-10 — see below.*

### A dialog, opened from the row — 2026-10-10 (Alan)

**The editor is a dialog**, opened by the **gear** at the end of a My Devices row — the gear the fleet list uses
to open a ship's settings, so the two lists open their editors the same way. *The inline expansion was dropped
because a Swing table row cannot host a panel beneath itself, and a dialog has room for tabs and for features
still to come.*

- **The gear is on every row**, a device not added included. Where the dialog cannot do something yet it says
  so rather than the gear being missing: a device with **no name yet** opens with its input tabs disabled and a
  line saying it needs a name, which [onboarding](#onboarding--every-controller-gets-a-name-and-a-buttonmap--settled-2026-09-13)
  gives it.
- **Modal to Elite-Intel only**, as every Elite-Intel dialog is: the main window waits, Windows does not. The
  game and every other program carry on.
- **As large as its content needs.** Dialogs in the app are not one size.
- **Tabs: DEVICE, AXES, BUTTONS, HATS — each only when the controller has that kind of input.** The dialog is
  built from what the controller reports, not from a fixed layout: a pedal set with three axes and no buttons
  shows DEVICE and AXES. The axes and hats a controller has are
  [read as it connects](#which-axes-and-hats-a-controller-has--2026-10-10-alan) (A8b); SDL alone gives only an
  axis count, and two VIRPIL sticks put their twist on Z and on RZ. ~~Where the axes cannot be read, the AXES tab
  needs A9 to decide what to show.~~ *Decided in A9 — [below](#built-as-a-dialog--2026-10-10-alan-a9).*
- **DEVICE** holds the name, VID and PID, the hardware name, whether it is attached, and the
  [install strip](#the-install-strip--a-readout-not-a-switch).
- **AXES** has one label box per axis the controller reports, named by the axis it is (`Joy_XAxis`,
  `Joy_RZAxis`), six to a column and a second column when there are more.
- **BUTTONS** has one label box per button, in groups of **32** on sub-tabs numbered as the game numbers them —
  `1–32`, `33–64` — when there are more than 32.
- **HATS** has four label boxes per hat — `Joy_POV1Up`, `Joy_POV1Right`, `Joy_POV1Down`, `Joy_POV1Left`. A hat a
  controller reports as buttons is buttons, and needs no tab. *An 8-way hat's diagonal is assumed to be two
  directions at once — [testing item 20](../../00-overview/testing-required.md#bindforge).*
- **A controller that is not attached** cannot report its inputs, so its tabs show the inputs its saved labels
  name, with a line: *"plug it in to see every input."*
- **Half-axis labels are not shown.** `Pos_Joy_…` and `Neg_Joy_…` label one direction of an axis; the AXES tab
  offers one box per whole axis. A half-axis label already saved is kept and written out unchanged —
  [testing item 19](../../00-overview/testing-required.md#bindforge) asks whether the game shows one at all.
- **The buttons along the bottom: BACK on the left, CLEAR in the middle, SAVE on the right.** CLEAR is in the
  middle so it is not hit by reaching for either of the others; on a built-in, RESET LABELS takes its place. The
  dialog footer has no middle place today, so one is added to the shared dialog scaffold (`HudModalSpec`) —
  see the [UI component map](ui-component-map.md#alias-designer).
- **BACK is DISCARD.** Leaving with unsaved edits — BACK, the close glyph or Esc — asks **Save**, **Discard** or
  **Keep editing**. *That is the [exit prompt](#exit-prompt) narrowed to one device, with a way back in.*
- **APPLY is not in the dialog.** It is the screen's button, beneath the list, and reaches every installation.

#### Built as a dialog — 2026-10-10 (Alan, A9)

Built as `ui.screen.bindings.DeviceEditorDialog`, opened from the gear on `AliasDesignerPanel`'s My Devices rows,
with `bindforge.devices.DeviceEditorInputs` (which boxes, their defaults, what SAVE writes — pure) and
`DeviceEditorLights` (live highlighting — pure). SAVE is `BindForgeDeviceDraftManager.saveLabels`, one transaction.
Alan answered five questions, one at a time:

- **Before first setup the gear does not open the editor.** It says first setup comes first and offers **SET UP**,
  which starts it. First setup throws any draft away as it fills the master, so labels saved before it would be lost
  without a word.
- **Axes that cannot be read are all eight** — when the description could not be read, or counts other than SDL
  does (`agreesWith`) — with a line saying which axes the controller has could not be read. This is what generation
  labels in the same case, and a label for an axis the controller lacks is never shown in the game. None is lit,
  since SDL's numbering is not matched.
- **An axis rests where it is first seen** after the dialog opens. The Device Service reports an axis only when it
  moves, in raw steps, so the first report arrives within a hair of where the axis sat; a throttle parked at half
  rests at half. Lit beyond 5% of its travel — 0.1 of the −1…1 range.
- **SAVE writes every box, defaults included** — once the master holds labels for a device, startup generation skips
  it, so a default left unwritten would never reach the game, which would show a raw `Joy_14`. **Except on a device
  the game draws itself** (`<SupportsIcons>`, `<GamePad>` — `FrontierStockDevices.gameDrawsIt`): there a box is
  written only when it already had a saved label or was changed, so a `Button 3` never replaces an icon nobody
  touched. A saved label with no box — a half-axis, or an input the controller does not report today — is kept.
- **CLEAR, or RESET LABELS on a built-in, is in the footer's middle place, disabled** until the Actions slice
  (A10), so the layout is final and A10 only wires it.

*Also settled while building it:*

- **SAVE closes the dialog** once the draft holds the labels, and is dimmed until a box differs from what it opened
  with — compared as SAVE would keep it, so trimming spaces is not an edit. `HudUnsavedHint` sits beside it.
- **A built-in the master holds no labels for is saved under Frontier's name**, added to the draft with the attached
  controller's VID/PID. Apply writes its `.buttonMap` only.
- **The labels shown are the draft's** when the draft holds the device, and the master's otherwise. The install strip
  is the device's Installations markers, against the saved master.
- **A box holds at most 18 characters** as it is typed; a longer saved label is cut to 18 on SAVE.
- **No default button** — Enter in a label box would otherwise save and close mid-edit.
- **Hat movement is polled by the Device Service** — `DeviceHatEvent`, SDL's direction bits, on change only.
- **An unattached controller with no saved labels** shows DEVICE alone, and says to plug it in.

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

The strip ~~along the top of the expansion~~ on the dialog's **DEVICE** tab *(2026-10-10)* stays, and it is
**read-only**. It no longer selects which installation you are editing; it shows **which installations the user
has, and whether each one matches the master for this device** — one line per installation, then one sentence:

```
 STEAM     M         E:\SteamLibrary\steamapps\common\Elite Dangerous\
 EPIC      M         C:\Program Files\Epic\EliteDangerous\
 FRONTIER  differs   D:\Games\Frontier\EDLaunch\
 2 of 3 installations match.
```

*The marker's words and colours are the [Installations column's](#the-installations-markers--2026-10-08-alan) —
**M**, *differs*, *not added*, *not found* — so the strip and the row say the same thing. The sketch used `•`
before that column was built (2026-10-10).*

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
  VID/PID, so hand-editing this would break the association. They are device-level ~~, identical on every
  tab~~ — there are no per-installation tabs any more (2026-10-10).
- **Edit Mode** activates the moment any field changes. A persistent label reminds the user this saves to the
  working copy only.
- **Alias name validation — tightened 2026-09-13.** 3–50 characters; **must start with a letter**; then
  letters, digits, `-` and `_` only; **no spaces and no period**; must not collide with any existing entry,
  including Frontier's built-ins, compared [ignoring case, `-` and `_`](#the-default-name-rule). *The earlier
  rule allowed spaces and a wider set of punctuation. But the alias becomes an XML element tag and a
  filename stem, and a space is valid in neither — so that rule would have accepted a name the game
  cannot read.*
- ~~**The alias must be confirmed or changed before button/axis naming unlocks.** [Onboarding](#onboarding--every-controller-gets-a-name-and-a-buttonmap--settled-2026-09-13)
  is where that happens. Confirming the offered name, typing another, or skipping — which accepts the
  default — all count, because in each case the user has been shown the name. A device is never fully
  configured under a name nobody looked at. A later rename is the [multi-file transaction](#renaming-a-device).
  **A built-in has no name to confirm:** its labels unlock straight away, and its name stays
  [Frontier's](#built-in-controllers-in-my-devices--settled-2026-09-13).~~
- **No confirm step — 2026-10-10 (Alan).** The confirm was the gate for a name nobody had looked at, from when
  registration named controllers silently. [Onboarding moved that gate earlier](#it-is-the-alias-confirm-gate-moved-earlier),
  and every other way a device gets a name — first setup, adopting an installation's entry — shows the name
  too, so every named device has been seen. The editor has **no CONFIRM button and no lock**. Its labels are
  editable once the device has a name; a device with none has its input tabs disabled until onboarding names
  it. A built-in's name is Frontier's, so its labels are editable straight away. *`alias_confirmed` stays on the
  master and the draft; every path writes it `true`, and nothing reads it now.*
- **The name field is read-only until [Rename](#renaming-a-device) is built** (slice A11), with a line saying
  so. A changed name is a rename — every installation, then `.binds` — not a field edit.
- **Button and axis names** ~~are edited in Axis/Buttons sub-tabs~~ are edited on the dialog's input tabs
  *(2026-10-10)*. The UI sizes dynamically to the device's real inputs — never hardcoded; a device with three
  axes shows three, **and shows the three it has**: an X, Y and RZ stick shows `Joy_XAxis`, `Joy_YAxis` and
  `Joy_RZAxis`, read by [A8b](../../05-build-plan/alias-designer.md#slices), never the first three by position. A hard 18-character limit
  applies to every button/axis friendly name, confirmed by direct in-game testing. The alias field has its own
  rule (3–50 characters) and no in-game display slot, so truncation is not a concern there.
- **Every input gets a label — 2026-10-10 (Alan).** A box is never empty: one with no saved label shows the
  default, and clearing a box puts the default back. The defaults are [the generated ones](#at-elite-intel-startup-a-buttonmap-for-every-connected-named-controller)
  — `Button 14`, `X Axis` — so the dialog and the generated files agree, and hats get `Hat 1 Up`,
  `Hat 1 Right`, `Hat 1 Down`, `Hat 1 Left`. Spaces at either end of a label are trimmed.
- **Live highlighting:** pressing a button or moving an axis on the open device highlights the corresponding
  ~~row~~ box, letting the user identify raw input codes by feel. ~~With the device list itself being the
  selection, there is no separate "which controller am I pressing" step to get wrong.~~ The dialog is opened for
  one device, so only that device's input is shown. *Settled 2026-10-10 (Alan):*
  - **A box is lit only while its input is active**, with nothing lingering after: a button while it is held, a
    hat direction while it is pushed, an axis while it is **more than 5% of its travel** from where it rests —
    so jitter never shows.
  - **The dialog never changes tab for an input.** Most buttons are momentary, and a light on another tab shows
    nothing; a box lights only where the user is looking.
  - **A lit box is solid orange with dark
    text** (Alan, 2026-10-15). The half-transparent selection blue it used first was too faint to notice.
  - Axes are lit by which axis they are, so axis highlighting needs [A8b](../../05-build-plan/alias-designer.md#slices)
    — SDL numbers a controller's axes without gaps, and its axis 2 may be Z or RZ.

### Actions, and what each one reaches

| Action | Scope | Touches a game installation? |
|---|---|---|
| **SAVE** | the device's record in the draft — the dialog's right-hand button | **No** — draft only |
| **DISCARD** | ~~the draft — whether one device or all of it is for the Actions slice to settle, see below~~ the dialog's unsaved edits — it is **BACK**, which asks first when there are any (2026-10-10) | No |
| **CLEAR** | the device's entry and its `.buttonMap` — not offered on a built-in. The dialog's middle button | No — see below |
| **RESET LABELS** | a built-in's `.buttonMap` labels. In CLEAR's place on a built-in | No — draft only; see [built-ins](#built-in-controllers-in-my-devices--settled-2026-09-13) |
| **APPLY** ~~TO GAME INSTALLS~~ | **every installation**, and reports per installation. The screen's button, not the dialog's — renamed 2026-10-10 (Alan), since there is no choosing which installations | **Yes** |

*Where each button is — 2026-10-10 (Alan).* SAVE, CLEAR (or RESET LABELS) and BACK are the
[dialog's](#a-dialog-opened-from-the-row--2026-10-10-alan), because they act on one device: CLEAR on the main
screen would sit beside the whole list. APPLY stays on the screen, because it carries everything saved to every
installation at once. *Built:* SAVE comes with the dialog, in slice A9; CLEAR, RESET LABELS and APPLY in A10.

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

*Settled 2026-10-11 ([A10](#built--2026-10-11-alan-a10)): the screen's DISCARD is the second, the whole draft.* *Half settled 2026-10-10: the dialog's DISCARD is the first of the two below — BACK throws away the edits made
since the dialog opened. Whether anything puts the whole draft back to the master is still the Actions slice's.*
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

**The user's Apply comes first; an installation that missed it is caught up later — 2026-10-14 (Alan).** A
missing, locked or unavailable installation is rare, and it never holds back what the user applied: every
installation that can be written is, the report names the one that could not and why, and nothing is rolled back
to wait for it. **When that installation is found again** — at Elite-Intel startup, or whenever the check next
reaches it — BindForge asks: *"Found the Epic installation that was missing. Write your recent control
configuration changes to it?"* ~~Yes is the same push as **Revert**.~~ Yes is Apply's push to that installation
alone, removals and renames included — *changed 2026-10-15 (Alan), since Revert removes nothing.* Built in slice
A14 — see [Catch-up](#catch-up--2026-10-15-alan-a14).
*Alan: "when a user edits and applies, those should be the priority, and the rare, weird and highly unlikely
things get a message to the user."*

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

#### An edit `.binds` depends on updates `.binds` — 2026-10-11 (Alan)

**When the user changes a `DeviceMappings.xml` entry, or its `.buttonMap`, that the shared `.binds` depends on,
Alias Designer must update `.binds` to match**, so that no edit of the user's leaves bindings pointing at a device
the game no longer knows. Recorded while building A10; it had never been stated before. ~~**Undecided:** whether
Alias Designer writes `.binds` itself or hands the change to the Bind Editor as a task.~~

**Settled 2026-10-12 (Alan, Foundation F2):**

- **Alias Designer writes it, inside its own Apply.** A rename is all-or-nothing with `.binds` last, and only if
  every installation succeeded ([below](#binds-goes-last-and-only-if-every-installation-succeeded)) — an
  ordering only one operation can promise. A task handed to the Bind Editor could not.
- **A `.binds` draft is rewritten too**, by the same substitution, and its baseline re-recorded
  (`BindingsWorkingCopyRepository.recordGameFileBaseline`) — likewise the `.binds` master once it exists. The
  substitution is mechanical and the same on every copy, so it cannot lose an edit; refusing the rename instead
  would make the user apply or discard unrelated binding edits to rename a stick. So the rewrite takes a list of
  files — drafts, masters, and the game's `.binds` — and is **all or nothing across all of them**.
- **The game's `.binds` goes last in that list** (Krondor, 2026-10-13). Writing it makes the bindings monitor
  reload, and the reload copies the game's file over a working copy holding no draft; a working copy still to
  come would then read as changed, and the whole rewrite would be put back as `STALE_FILE`.
- **Matching ignores case. A hex reference is rewritten from the value found** (Krondor, 2026-10-13, accepted
  by Alan). `BindsDeviceReferences` still *reads* hex in either order, as
  [format §4.2](domain-knowledge/EliteDangerous-BindsFileFormat.md#42-vidpid-derived-hex-identifiers) asks of a
  reader, and reports the exact value it found; the rewrite is built from that value
  (`new DeviceReferenceRewrite(Set.of(found), name)`), so CLEAR's refusal count and the rewrite still agree.
  `DeviceReferenceRewrite.hardwareIdToName(vid, pid, name)` matches **VID then PID only**, the order the game
  writes: a rewrite writes, and matching the reverse could rename a second device whose VID and PID are this
  one's swapped. `DeviceMappings.xml` is unaffected — its `<VID>` and `<PID>` are read by tag, in any order.
- **CLEAR writes hex** — VID then PID, upper case, as the game does. It is the one place BindForge writes a hex
  reference ([format §4.2](domain-knowledge/EliteDangerous-BindsFileFormat.md#42-vidpid-derived-hex-identifiers)).
- Every `Device="…"` is rewritten — on `<Primary>`, `<Secondary>`, `<Binding>` and `<Modifier>` alike — and every
  `.binds` in the folder, not only the active preset, since the game validates them all. Built as
  `BindingsWriter`'s device rewrite on the [foundation branch](../../05-build-plan/foundation.md); merged into
  `V1.1-Release` and up to `V1.2-BindForge` 2026-10-13.

What it reaches today:

- **CLEAR** on a device `.binds` names: without the entry, `.binds` must name the device by its hardware ID
  instead, as the game itself writes a device with no entry ([format §4.0](domain-knowledge/EliteDangerous-BindsFileFormat.md#40-the-rule-confirmed-2026-09-08)).
  That rewrite is `bindforge.io` and not built, so CLEAR is **refused** for such a device until it is, and says how
  many bindings name it. When it exists, CLEAR does both, `.binds` last, as a [rename](#binds-goes-last-and-only-if-every-installation-succeeded) does.
- **RESET LABELS and every label edit** are not reached: `.binds` names a device by its element and its inputs by
  token (`Joy_5`), never by label.
- **A rename** already carries its `.binds` rewrite ([below](#renaming-a-device)); **a retarget** keeps the name,
  so `.binds` does not change.

#### Built — 2026-10-11 (Alan, A10)

Built as the Device Editor's middle button (`bindforge.devices.DeviceEditorMiddle`, pure), APPLY and DISCARD on
the Alias Designer screen, `bindforge.devicefiles.DeviceApply` (now with its first caller), removals in
`DeviceFilesPush.push(Set)` and `DeviceMappingsMerge.remove`, and `bindforge.devicefiles.DraftChanges` (pure).
Alan answered five questions, one at a time:

- **CLEAR is refused while `.binds` names the device** — the rule above — with the count: *"142 bindings name
  LVWAP. Clearing it means moving them to its hardware ID, which is not built yet."* Compared ignoring case.
  **The Apply cleanup never removes an entry of the user's that `.binds` names either**, whoever made it pending —
  first setup's shadowed and losing names included: it stays pending, and the report says so. When `.binds` cannot
  be read, no entry of the user's is removed. *A built-in's reset is never held back* — see *After review* below.
- **RESET LABELS on `VPCPanel` or `VPCThrottle` puts Frontier's map back**, byte for byte from the jar
  (`FrontierStockButtonMaps.shippedContent`), since a `.buttonMap` Frontier shipped is never removed. On any other
  built-in it deletes the file in every installation, and [startup generation](#at-elite-intel-startup-a-buttonmap-for-every-connected-named-controller)
  makes a fresh one the next time the controller connects. Frontier's element is never touched.
- **A device cleared or reset in the draft, not yet applied, says so** — *"Cleared. Apply removes it from every
  installation."* — its input tabs are disabled, and the middle button becomes **UNDO CLEAR** (or **UNDO RESET**),
  which puts it back as the master holds it. The undo asks nothing: it changes no game file.
- **APPLY and DISCARD are in the screen's footer.** APPLY is the primary, on the right. **It is offered only when it has
  work** (Alan, 2026-10-15, reversing *"whenever the master
  exists"*). Work means the draft changes something, a removal or rename is still on its way, or an installation differs from the master in a way the push repairs:
  the rows that offer Revert (`ApplyWork`). The last case is still the retry after a partial Apply, since the installation it failed on still differs. Dimmed, its tooltip says there is nothing to apply. A button that answers every press, with nothing to do, had users pressing it again and again. It asks
  first, naming the installation count, and reports per installation: *"Updated 2 of 3"*, each failure's reason,
  what was removed, what was held back and why. **DISCARD throws the whole draft away** — the second DISCARD
  [above](#one-record-and-where-it-lands--reworked-2026-09-26), now settled; the dialog's BACK is the first. It is
  offered only when the draft changes something, asks first naming the devices, and touches no installation.
  Beside it, *"Saved, not applied: LVWAP, T-Rudder"*.
- **An orphan `.buttonMap` no device owns is left alone.** Apply's cleanup removes the ones BindForge recorded as
  pending; a stray file nobody recorded stays a yellow row that says to remove it by hand or restore from a backup.
  A **Remove** way out for it is for a later slice.

*Also settled while building it:*

- **"The draft changes something" is one definition** (`DraftChanges`): a device added, removed, or with a
  different name, hardware or label. A started draft that matches the master changes nothing — which the
  [unsaved-work prompt](#exit-prompt) will need too.
- **A removal leaves every installation in the same unit as Apply's updates** — one installation's
  `DeviceMappings.xml` and `.buttonMap` files change together or not at all, and each removed file is kept in
  [Edit History](file-manager.md#edit-history) first. Every element with the name goes, a second copy included.
- **A name stays pending until every installation has lost it**, so one on an unplugged drive is removed by the
  next Apply. Revert removes nothing.
- **Found:** a device cleared and then saved again came back as a new draft row, and Apply would have stored it as a
  pending removal while it was in the master. A name the draft holds is now never pending.
- **CLEAR and RESET LABELS confirm, naming the installation count**; both write the draft only.

*After review — 2026-10-11 (`code-integrity-review.md`, Alan):*

- **A reset built-in is applied whatever `.binds` says.** Apply held back every pending name `.binds` mentioned,
  Frontier's included — so RESET LABELS on a stick the user flies with was reported as left in place, and stayed
  pending for good while the master and the installations disagreed. A built-in loses only its `.buttonMap`, and
  `.binds` names inputs by token, so nothing a binding needs goes; only the user's names are checked now.
- **A bindings folder that is not there is unreadable, not empty**, for the two places that decide a removal:
  CLEAR's refusal and Apply's hold-back (`BindsDeviceReferences.referenceCounts`). So is one whose `.binds` files
  all fail to parse. Otherwise a typo in settings, or an unmounted drive, would read as "nothing names this device"
  and let CLEAR strip an entry the game needs. The editor still opens, with CLEAR refused and saying why. An empty
  folder is an answer: no custom preset names anything. Severity stays lenient — there it only turns findings
  yellow — and a skipped `.binds` is now logged by name.
- **A CLEAR or undo that changed nothing says so** rather than closing as if it worked: the device changed since the
  dialog opened.
- **The gear asks whether the master is empty**, not whether the refresh produced a report — a failed refresh no
  longer sends the user to first setup.
- **An unattached controller offers hats only up to the fourth**, the most DirectInput reports (`Joy_POV1`–`Joy_POV4`).
  A saved label naming a higher one is kept and written out, like a half-axis label, and builds no boxes.

*Second review — 2026-10-11 (`code-integrity-review.md`, Alan):*

- **Apply reads `.binds` only when a pending removal is the user's.** Frontier's names never consult it and most
  Applies remove nothing, so the read was wasted — and when the folder was missing, its warning described a hold-back
  that never happened.
- **A stale CLEAR or undo shows its own message**, not wrapped in *"This could not be done"*.

#### Catch-up — 2026-10-15 (Alan, A14)

Built as `bindforge.devicefiles.MissedWrites` (the flag, and who is found again), `DeviceFilesPush.push(installIds,
removals)`, `DeviceApply.catchUp`, `InstallationHoldings` (what settles), `CatchUpAnnouncement` (the startup line)
and the question in `AliasDesignerController`. Alan answered four questions, one at a time:

- **A missed write is a stored fact** — `missed_write` on `bindforge_installations`, migration `12006`. The check
  alone cannot tell an installation that missed an Apply from one edited in the game: both differ from the master,
  and they need different questions. **Any write that misses an installation marks it** — Apply, first setup,
  Revert, onboarding, catch-up itself. **Only a push carrying everything Apply carries clears it** — Apply, first
  setup and catch-up. Revert removes nothing and onboarding writes one element, so either can land on an installation
  that still holds a device the others lost to a CLEAR.
- **Yes writes what Apply would, to that installation alone** (option 3 of three). The master, the pending removals
  `.binds` does not hold back, and the renames; the bindings follow a rename or a hardware id as Apply's would, so a
  catch-up that is the first to take a rename moves them. **The draft is never promoted** — what the user saved and
  has not applied stays theirs to apply — and **no other installation is written**: one edited in the game meanwhile
  is not this question's to revert. ~~Revert's push only~~ would have left a CLEAR undone, and ~~Apply again without
  the draft~~ would have reverted the other installations unasked.
- **Settled from the files.** A removal stops being pending, and a rename's old name is forgotten, once no
  installation may still hold the name, read as the push would find it. Catch-up's own report covers one
  installation, and cannot say whether the others took the removal earlier. An installation that cannot be read
  counts as holding it — **unless it has missed no write**: it took every write that carried everything. *The one
  case this misjudges is a removal held back for `.binds` when that installation was last written and released since;
  it lingers there as a leftover the check already expects.*
- **No asks again next run** (option 1 of three). Nothing is written, the flag stays, and the question is not
  repeated in this run. The divergence list's Revert row and APPLY stay available meanwhile.
- **Spoken at startup, asked on the screen** (option 2 of three). At startup the user is often already in the game
  with Elite-Intel's window behind it, so a dialog would block the window unseen. `CatchUpAnnouncement` — after
  activation and the run's one detection — says once and logs: *"The Epic installation is back, and it missed your
  last control changes. Open BindForge to update it."* The question opens the first time the Alias Designer screen is
  shown in the run, and whenever a refresh there finds one later — the "whenever the check next reaches it" above.

*Also settled while building it (Alan approved them as defaults with the sources):*

- **Several at once are one question**, naming them all; Yes writes each and reports per installation.
- **Catch-up before onboarding.** It finishes writes the user already made; onboarding's offer waits for the refresh
  after the answer, so the two never write one installation at once.
- **The report is Apply's** — updated N of M, what was removed, held back, moved, renamed.
- **A write missed before this slice is not asked about**: existing rows start unflagged. APPLY stays lit for such an
  installation ([`ApplyWork`](#built--2026-10-11-alan-a10)), which repairs it all the same.

*After review — 2026-10-15 (`code-integrity-review.md`, Alan: all but the record constructors of #6, and P2):*

- **The run's one detection is waited for.** A second reader arriving while it ran got the list straight away, with a
  folder that came back still marked missing — so the startup line could go silent, and a screen's first refresh
  could draw a stale marker. `InstallationRegistry.currentWithStartupScan()` now holds every caller until it ends.
- **The first showing asks from the stored flag**, not from the last refresh, which may have run before the folder
  came back. The refresh it starts still decides whether the installation can be reached.
- **A write that starts between the offer and the dialog gives the question back**, for the refresh that ends the
  write to offer again — two writes to one installation at once would each keep an Edit History copy of a file the
  other replaces.
- **One push loop for every kind** (Apply, catch-up, revert), so the missed-write rule is written once; and **one
  meaning of "landed"** (`Outcome.landed()` on the push's and onboarding's outcomes), so a new outcome counts as a
  miss on every path.
- *Left as built (Alan):* the short `Target` and `InstallationRow` constructors the existing tests use; and a Revert
  that lands leaves the flag, so catch-up still asks about that installation (review P2) — as ruled above.

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

#### Settled before building it — 2026-10-14 (Alan, A11)

- **~~All or nothing~~ — a rename follows onboarding's partial rule** (Alan, option 3 of three). The user's Apply
  comes first, as for [onboarding's hex move](#settled-before-building-it--2026-10-14-alan-a6b): every installation
  that can be written is renamed, and `.binds` moves to the new name once **at least one** took it. One that missed
  it is named in the report and caught up later — by the next Apply, by **Revert**, or by
  [A14's catch-up](#actions-and-what-each-one-reaches) — since the push renames wherever the old name is still
  found. Only when **no** installation took it does `.binds` stay on the old name, and the rename is reported as not
  applied yet. *This knowingly accepts the cost measured above: until it is caught up, an installation that missed
  the rename rejects the preset.* The four paragraphs above are kept for that reasoning.
- **The `.binds` move is derived, not recorded**, as A6b's is: at each Apply, a master device that still carries a
  `previous_name` which `.binds` uses has its bindings moved. A move that failed is retried by the next Apply.
  `previous_name` is cleared once every installation has lost the old name and `.binds` no longer uses it.
- **A rename that changes only case touches no `.binds`** — measured: a `Device=` name finds its element ignoring
  case ([`.binds` format §4.3](domain-knowledge/EliteDangerous-BindsFileFormat.md#43-case-only-binds-cares-and-it-cares-about-one-spelling-per-device--measured-2026-10-14)),
  and a preset spelling one device two ways is rejected, so rewriting only some references would be worse than
  none. The element and the `.buttonMap` are renamed.
- **One bindings folder, on Linux too — for now** (Alan, 2026-10-14). On Linux each Proton installation has its own
  `.binds` folder ([input environment §7](../../00-overview/input-environment.md#7-linux-the-bindings-folder-is-not-shared)),
  but the rename, like every other `.binds` read and write in BindForge, uses the one folder Elite-Intel is set to
  (`PlayerSession.getBindingsDir()`, which on Linux is the prefix the launcher's `ed-bindings` link points at). That is
  Elite-Intel's behaviour already, and correct on Windows and on a Linux machine with one installation. *Why not per
  installation:* which installations should share controls is the user's to say, not BindForge's to infer — a
  commander owns no input file, so "same commander" does not answer it — and asking belongs to a later slice that <!-- terminology-ok: the in-game commander, contrasted with the user -->
  moves every `.binds` consumer together, not the rename alone.
- **Several renames per Apply, no chains** (Alan, 2026-10-14). Each device's `.binds` move is its own rewrite, run one
  after another as onboarding's hex moves are, and one that stops (the bindings monitor reloading under it) is
  retried by the next Apply. **A name still waiting as some device's old name** — a `previous_name` in the master or
  the draft — **is taken** until every installation has lost it, so it cannot be a new name. That rules out chains
  (`LVWAP` → `LeftStick`, then another device → `LVWAP`), so no two moves depend on their order.
- **First setup's *needs Rename* choices stay disabled in A11** (Alan, 2026-10-14). A11 renames from the Device
  Editor only. Opening those choices — recording the losing name as the winner's `previous_name` so the first Apply
  moves its bindings — is slice A16, which also designs the two cases one `previous_name` cannot cover: two losing
  names that `.binds` uses, and first setup that cannot finish because `.binds` uses both names.
- **A11 renames the user's own devices only** (Alan, 2026-10-14). On a built-in still under Frontier's name the
  name field stays read-only, as today. Renaming or shadowing an attached built-in
  ([above](#built-in-controllers-in-my-devices--settled-2026-09-13)) needs its presentation designed first, and is
  slice A15.
- **Linux with several installations is told, not fixed.** Apply's confirm says, when BindForge has found more than
  one Linux installation, that controls are updated only for the installation Elite-Intel uses. Until the later slice,
  a rename can leave another installation's preset naming the old device, and that installation falls back to
  keyboard and mouse.

#### Decided while building it — 2026-10-15 (A11)

- **One rename at a time per device.** The master keeps one old name per device, so a device whose last rename has
  not reached every installation and `.binds` yet keeps its name field read-only, saying so, until an Apply finishes
  it. A second rename would overwrite the first old name, and the installations still holding it would never be
  renamed.
- **A device's own names are never "taken" for itself.** The clash check leaves out its current name, its master
  name and its old name, matched exactly — so changing only case is a rename, and so is going back to the name the
  installations still hold (which the draft then records as no rename at all).
- **The element is renamed in place**, keeping its place in the file and every child it has (`<Alternative>`,
  `<SupportsIcons>`). A second copy under the old name is removed, as CLEAR removes one; when the new name is already
  in the file the old copies are removed and that element is kept.
- **The `.buttonMap` is renamed, never copied.** The master's labels are written under the new name and the old file
  deleted; a device with no labels in the master takes the old file's own content to the new name. **On a file system
  that ignores case** (Windows) a case-only rename finds the old and new name to be one file, and nothing is deleted —
  the game finds the file under either spelling ([format §2.2](domain-knowledge/EliteDangerous-DeviceMappings-ButtonMap.md)).
- **Renaming is in the Device Editor's name box**, checked as it is typed by onboarding's rule and words. SAVE writes
  the name and the labels in one transaction; a name another device took meanwhile writes nothing and says the device
  changed. A device renamed in the draft opens under its new name, and CLEAR counts the bindings on every name it
  has — draft, master and old.

#### After review — 2026-10-15 (Alan, A11)

- **CLEAR is refused while a rename is still under way.** Its old name may still be in `.binds` or in an
  installation that missed the rename, and clearing would drop the old name the next Apply needs to finish it —
  leaving bindings on a device with no entry, which the game rejects. The tooltip says so; Apply finishes the
  rename, and CLEAR is offered again.
- **A device that leaves the master mid-rename keeps its old name pending**, by any route. The old name is then
  removed from the installations that still hold it — or held back while `.binds` names it — like any other
  pending removal, rather than forgotten with the row.
- **The editor's CLEAR count and Apply's rename move use one count**
  (`BindsDeviceReferences.bindingsNaming`), so they cannot disagree about how many bindings name a device.
- **The rename finds the old element ignoring case**, as the game does: an installation holding `<lvwap>` is still
  the device whose old name is `LVWAP`. The element already spelled exactly as the new name is never taken for an
  old copy.
- **The Linux note on Apply's confirm speaks of every bindings move**, onboarding's hex move as well as a rename,
  since both are made in the one bindings folder Elite-Intel uses.

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

~~If no draft exists yet, BindForge auto-imports `DeviceMappings.xml` and all `.buttonMap` files from every
detected installation, with no prompt — a status message informs the user this is happening. If no
installation is detected, the draft starts empty and any connected devices are added via
[Automatic Device Registration](#automatic-device-registration) above.~~ *Replaced 2026-10-13 (Alan) by
[Activation](#activation--2026-10-13-alan): no prompt when the installations agree, first setup when they do not,
and an empty master when none is detected.*

### A missing controller is worth exactly one warning — settled 2026-09-24

**Elite-Intel says once, ~~at startup~~ the first time Alias Designer is opened in a run *(2026-10-11, below)*, which named controllers are not attached** — *"controller such and such
is missing"* — and then waits and sees. Nothing is changed, nothing is repaired, and the message is not
repeated.

**Why so light a touch.** The user is expected to connect their controllers before starting the game;
if they do not, the game will not load those bindings and they find out immediately in the cockpit. BindForge
cannot fix that from outside, and a missing stick is usually a cable, not a configuration problem.

**What it must not do** is treat an absent controller as a reason to change anything. An entry for hardware
that is not plugged in stays exactly where it is — entries deliberately outlive hardware, which is what makes
a stick work again when it is plugged back in. The
[`ATTACHED` / `MISSING` column](#my-devices) is the standing version of this, and the ~~startup line~~ one-time warning is the
once-per-run version.

#### Built — 2026-10-11 (Alan, A13)

Built as `bindforge.devices.MissingControllers` (who is named — pure) and `MissingControllerWarning` (the one warning, spoken and logged), triggered by the Alias Designer screen (`AliasDesignerController.onShown`). Alan answered three questions, one at a time:

- **Only controllers `.binds` names.** A named controller that is not attached is mentioned only when a binding
  names it (`BindsDeviceReferences.referenceCounts`, ignoring case) — the reason to warn at all is bindings that will
  not load, and an entry for hardware sold years ago would otherwise be announced on every run. "Named" is the
  master's name, or before first setup the installation's entry name. A Frontier name is never mentioned; that is
  judged by name, not by VID/PID, so a user's element shadowing a built-in still counts as the user's.
- **Not at Elite-Intel startup — the first time the user opens Alias Designer** in the run. The Device Service has
  been listing controllers since launch; BindForge says nothing until the user comes to it. The screen's first showing
  starts a refresh of its own, and the warning judges that one: the screen's refresh at application start can run
  before any hardware has been listed.
- **Spoken and logged**, as the other one-time checks are: one spoken line naming the controllers, and one log line
  each with how many bindings name it — *"Controller LVWAP is not connected - 142 bindings name it"*.

*Also settled while building it:*

- **Once per run is once per Elite-Intel process.** Nothing missing uses the warning up too: a stick unplugged later
  is the `MISSING` column's to show.
- **With the Device Service not running nothing is said, and the warning is kept** for the next time the screen is
  shown — with nothing looking, every controller reads absent.
- **When `.binds` cannot be read nothing is said, and the warning is used up**: showing the screen again does not make
  the folder readable, and without the counts every unplugged entry would be announced.
- **Two rows under one name** — before first setup, an installation can still hold a stale PID — are one controller,
  and it is not missing when either is attached.

*After review — 2026-10-11 (`code-integrity-review.md`, Alan):*

- **"Running" is not "has listed the hardware."** The Device Service reports itself available before its first look at
  the controllers, so a screen opened in that moment — just after launch, or after the services are restarted — would
  have announced every bound stick as missing, and the one warning would have been the wrong one. The warning now
  waits for `DeviceService.hasEnumerated()`, set after the poll loop's first pass and cleared at stop; until then it
  says nothing and is kept, as with the service stopped.

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

**Inside the Device Editor — 2026-10-10 (Alan).** Closing the [dialog](#a-dialog-opened-from-the-row--2026-10-10-alan)
with unsaved edits — BACK, its close glyph or Esc — asks **Save**, **Discard** or **Keep editing**. The third
answer exists because the user is still on the screen and may only have pressed BACK by mistake. The dialog is
modal, so it cannot be left open while the user navigates away; the prompt above covers what is left after it
closes.

**Nothing is left after it closes — 2026-10-12 (Alan, Core C6).** Every action on this screen writes the draft as it
is pressed, and saved-but-not-applied is not unsaved, so the editor's prompt is the whole of the exit prompt here
and the screen adds none. [The ruling](overview.md#built-as-a-ruling-not-code--2026-10-12-alan-core-c6).

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
