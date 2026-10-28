# BindForge — Alias Designer

**Purpose:** manage `DeviceMappings.xml` and `.buttonMap` files, giving connected controllers friendly names shown both in BindForge's own Bind Editor and in the game's own controls UI.

## Structure

A single flat section: a device list, and the [Device Editor](#device-editor) opened as a dialog from a gear at
the end of each device's row. ~~A single flat section with no sub-tabs: a device list, with the editor expanding
inline beneath whichever device is open.~~ *Changed 2026-10-10 (Alan): Swing has no row that opens beneath
itself, and a dialog has room for tabs — see [the Device Editor](#a-dialog-opened-from-the-row--2026-10-10-alan).*

## Device List

Two views, toggled via a tab.

### My Devices

**The list is the union of live hardware and the working copy's `DeviceMappings.xml`**, correlated by VID/PID:

- every controller the Device Service currently reports, whether or not the game has ever heard of it, **and**
- every non-Frontier entry in the working copy's `DeviceMappings.xml`, whether or not that hardware is currently
  plugged in.

`DeviceMappings.xml` supplies *names*. It never supplies *existence*. A brand-new stick appears the moment it
is plugged in, unnamed; an entry for a controller sold last year keeps appearing until it is cleared.

| Column | Source |
|---|---|
| **Hardware name** | what the hardware reports through the Device Service, on any OS — *renamed from "Windows name" 2026-10-15 (Alan): it is the controller's own USB name on Linux too* |
| **VID / PID** | the hardware |
| **Status** | `ATTACHED` or `MISSING` — whether the hardware is present right now |
| **Alias** | the name in the working copy — or the new name, while it is an edit not applied — or *not added* |
| *(gear)* | opens the [Device Editor](#device-editor) for this device — on every row *(added 2026-10-10)* |

**No installations on this screen** (2026-10-24, Alan; [principle 12](principles.md)). *The Installations columns —
one marker per installation, M / differs / not added / not found — were removed: the editors care what is being
edited, and only File Manager's [Game Install Locations](file-manager.md#game-install-locations) cares where the
files are, and shows whether each installation matches the working copy.*

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

- **The two views are `COMPACT` tabs, My Devices and Built-in Devices**, in place of the My Devices heading.
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
  Frontier's shipped list and the user's working copy, so [the check](overview.md#what-the-check-tells-the-user--2026-10-23)
  finds it and offers to put the working copy back.
- *"Renaming one renames every controller it covers."* True for a rename, which is why **shadowing** is
  offered beside it: a user element at the top of the file, carrying only the attached controller's VID/PID,
  wins for that controller and leaves Frontier's element in place for the rest. For a single-pair entry like
  `<T-Rudder>` the two give the same result.

**Either one is offered only for a controller the user has attached** — the VID/PID comes from the hardware,
as it does for any device. Both are ordinary user elements from then on: kept in the working copy, written
[at the top of `<Root>`](#the-users-entries-come-first-in-devicemappingsxml--settled-2026-10-04), and a rename goes through the
[rename transaction](#renaming-a-device), rewriting every `.binds` that names the old element — Frontier's
name included. *The same test showed what skipping that costs: an inactive binds file still naming `T-Rudder`
logged six `Failed to find GUID` lines.* ~~How the device editor presents rename versus shadow is not designed
yet.~~ *Designed and built 2026-10-16 (Alan, A15): the editor offers one name box, and it always shadows - see
[below](#built--2026-10-16-alan-a15).*

~~**Its name and VID/PID are locked** (Alan, 2026-09-13). A built-in element is Frontier's **definition**, not a
label on one stick: `<DualShock4>` matches both DualShock 4 revisions, and `<GamePad>` carries 80 VID/PID pairs,
so renaming one renames every controller it covers. And a game update that restores Frontier's file would put
the old name back, leaving `.binds` pointing at a name that no longer exists.~~

**Its button and axis labels are editable.** They are saved as a `.buttonMap` under Frontier's name —
`T-Rudder.buttonMap` — which changes nothing about how the game recognises the device. Labels unlock straight
away, because there is no name to confirm.

**RESET LABELS replaces CLEAR.** [CLEAR](#actions-and-what-each-one-reaches) removes an installation's entry,
and on a built-in that entry is Frontier's. RESET LABELS removes only the `.buttonMap` the user or BindForge
created, and the next APPLY writes [default labels](#default-labels-for-every-connected-named-controller)
for it while it is plugged in. A `.buttonMap` Frontier itself shipped — `VPCPanel`, `VPCThrottle` — is never removed.
Like SAVE and CLEAR, it reaches **every installation** on the next APPLY — there is one working copy, so a reset
is a reset everywhere.

| | The user's entry | Built-in |
|---|---|---|
| Name | editable, as the [rename transaction](#renaming-a-device) | ~~locked~~ ~~renamed or shadowed~~ shadowed, when the controller is attached — the entry then becomes the user's (2026-10-04, above; *shadow only, 2026-10-16, A15*) |
| VID / PID | read-only | read-only |
| Button and axis labels | editable once the device has a name — ~~once the name is confirmed~~ (2026-10-10, [no confirm step](#fields-and-rules)) | editable straight away |
| Undo (reaches every install) | **CLEAR** — the entry and `.buttonMap` | **RESET LABELS** — only a `.buttonMap` BindForge or the user made |
| [Onboarding](#onboarding--every-controller-gets-a-name-and-a-buttonmap--settled-2026-09-13) | asked for a name | never asked |
| What APPLY writes | the entry and its `.buttonMap` | the `.buttonMap` only |

#### Built — 2026-10-16 (Alan, A15)

*Built against the master/draft model; moved onto the working copy in Storage S4a (2026-10-26) - see [Built on the working copy](#built-on-the-working-copy--2026-10-26-storage-s4a). The rules above still hold; the master, draft and Apply named below are history.*

Built in `bindforge.devices.DeviceRename` (who may rename, and `backToFrontier`), `AliasDesignerReader.openingFor`
and the name box in `DeviceEditorDialog`, `BindForgeDeviceDraftManager.saveDevice(…, BuiltIn)`, and at Apply in
`DeviceFilesPush` (no element rename for a shadow; the old `.buttonMap`), `DeviceApply.moveRenamedReferences` and
`InstallationHoldings.mayHoldOldName`. Alan answered five questions, one at a time:

- **One name box, and it always shadows** (option 1 of three). On an attached built-in the name box is editable,
  and a new name puts the user's element ahead of Frontier's in every installation; Frontier's stays. Renaming
  Frontier's element in place is not offered. *Why:* on an element covering several controllers - `<GamePad>` 80,
  `<DualShock4>` 3 - Apply brings an element's own VID/PID into line with the master, so a pad matching one of its
  `<Alternative>` pairs would overwrite Frontier's primary pair and unname the other controllers; and after an
  in-place rename, CLEAR would leave the hardware with no entry at all, since Apply never puts Frontier's elements
  back. For a single-pair element such as `<T-Rudder>` the two give the same result in the game - the first
  element in the file wins - and a shadow leaves only Frontier's element unused beneath the user's.
- **The bindings move, as a rename's do** (option 1 of three). Every `Device=` naming Frontier's element becomes the
  new name at Apply, the game's `.binds` last, retried by the next Apply if it stops. Left on Frontier's name, a
  binds file could end up spelling the controller two ways once the game saves it, which it rejects. Where Frontier's
  element covers other controllers the bindings name the element, not the pad, so they all move to this one - the
  editor says so beneath the name box, where the choice is made, rather than in Apply's confirm.
- **The rename is finished when the user's element wins everywhere** (option 1 of two): in every installation the
  first element claiming the controller's VID/PID is the new name - as the game picks - no binds file names Frontier's
  name, and the old `.buttonMap` is gone or is Frontier's own as shipped. Frontier's element staying beneath does
  not count against it. Until then the name box is read-only and CLEAR refused, as for any rename under way.
- **Labels move with the name** (option 1 of three). They are written under the new name - from the master, or the
  old file's own content when it holds none - and the old `.buttonMap` is deleted in every installation, **except a
  file Frontier ships** (`VPCPanel`, `VPCThrottle`), which is put back as Frontier shipped it. ~~Another controller
  still on Frontier's element gets a fresh file from [startup generation](#default-labels-for-every-connected-named-controller).~~
  *Overtaken by the review of 2026-10-17 (below): the old file stays while another device is still called by
  Frontier's name, and is neither deleted nor copied.*
- **Going back is typing Frontier's name** (option 1 of three) - the name of the element covering the controller,
  spelled exactly as Frontier spells it. It is a rename like any other: at Apply the user's element is removed where
  Frontier's is in the file, the bindings move back to Frontier's name, and the labels go to its `.buttonMap`. The
  device is a built-in again, with RESET LABELS.

*Also settled while building it:*

- **A built-in's Frontier name is always on disk**, so a shadow keeps it as the old name whether or not the master
  held the controller. The draft had kept no old name for a device the master did not hold - nothing on disk yet -
  which would have left the bindings on Frontier's name.
- **A shadow is the user's device on My Devices.** `MyDevice.builtIn` is now true only while the master holds the
  controller under Frontier's name, or not at all; otherwise the Installations markers would read **M** wherever
  Frontier's element is there, whether or not the user's is.
- **Only Frontier's exact spelling goes back.** Every other spelling of it stays taken - `t-rudder` ahead of
  `T-Rudder` would be two elements the game cannot tell apart, since it finds an element ignoring case - so a
  shadow can never be a case-only rename of Frontier's name.
- **The shadow's element is new**, written by the merge at the top of `<Root>` with only the controller's VID/PID;
  Frontier's element keeps every child it has.

*After review — 2026-10-17 (`code-integrity-review.md`, Alan: all eight):*

- **Frontier's names stay taken, even a device's own.** A built-in opens under Frontier's name and a shadow's old name
  is Frontier's; the clash check had dropped both as the device's own names, so `t-rudder` got through. Only going back
  (`backToFrontier`, Frontier's exact spelling) accepts one now.
- **A shadow is its own controller's row.** One Frontier element covers several controllers - `<DualShock4>` three -
  and the draft row under its name may be another controller's, labelled there earlier. Shadowing pad B had renamed pad
  A's row, with A's hardware and labels. When the row under Frontier's name has other hardware, the shadow is a new row
  with this controller's VID/PID, and the other controller keeps its own.
- **The old `.buttonMap` stays while another device is still called by Frontier's name** - pad A, labelled under
  `DualShock4`. Apply writes it from the master and never deletes it for pad B's shadow, and the rename does not wait
  for it to go. ~~*Known limit:* going back to Frontier's name while that other device holds it is refused as a taken
  name in the draft; the editor says the device changed.~~ *Second review below: refused as it is typed, saying why.*
- **Who may rename is told by enums**, `Owner` and `Presence`, not three booleans in a row.

*Second review — 2026-10-17 (`code-integrity-review.md`, Alan: all eleven):*

- **A shadow with no labels does not take the other controller's file.** The old `.buttonMap` was copied to the new
  name before the push checked whether another device still held it, so pad B saved without labels got pad A's
  `A CROSS`. The check now comes first: the file is the other device's, neither deleted nor copied.
- **Going back to Frontier's name is refused as it is typed while another controller holds it**
  (`DeviceRename.frontierNameHeld`, a row by that name in the master or the draft), with a line saying another controller
  is called that. It had been accepted in the box and then refused on SAVE as *"the device changed"*.
- **SAVE's Frontier argument is `FrontierCover`**, for every controller Frontier covers - a built-in, a shadow renamed
  again or going back - and needs the hardware as well as the name.

## First setup — reconciling the installs — settled 2026-09-23

**The user is asked nothing until they open BindForge**; until then BindForge only backs up, silently
([storage.md](storage.md) rule 12). First setup — [storage.md](storage.md) rules 1–4 — is the one time it asks: it
turns whatever the installations hold into one working copy. **It can be abandoned**: nothing is written until its
last button, and the next opening of the tab offers it again.

### Activation — 2026-10-13 (Alan)

*Reworked 2026-10-24 (Alan).*

- **The installation list is always shown first** — Confirm / Add missing ([storage.md](storage.md) rule 1). *Until
  2026-10-24 identical installations activated silently, with no prompt; the list now always comes first, so a
  missed installation can be caught.*
- **"Agree" means identical.** If every installation's `DeviceMappings.xml` and `.buttonMap`s are the same, there is
  nothing to reconcile and nothing is asked. Any difference opens the reconcile (rule 2).
- **A file that is there but cannot be read stops first setup, and is named** — it could hold the one entry the
  working copy must not lose.
- **An installation whose folder is gone** is left out, and checked like any other when it comes back.
- **The "first-time startup complete" flag** is stored, and set in the same step that creates the working copy. A
  user who owns nothing of their own is still complete.
- **The backup before the working copy is created** covers every file in every installation, is **never deleted**,
  is listed with the other backups, and the user is told once — spoken and logged (rule 3). *Until 2026-10-24 first
  setup also took a second, labelled backup of its own; rule 3's is that backup.*

#### Built on the working copy — 2026-10-25 (Storage S2b)

First-time startup runs from a click on the BindForge tab (`ui.screen.bindings.FirstTimeStartupFlow`): the installation
list (`InstallationListDialog`), the reconcile when the installations differ (`FirstSetupDialog`), and the write
(`bindforge.storage.FirstTimeStartup`, through `WritePath` with the first setup backup). The files are read once
(`FirstSetupReading`, `InstallationFiles`) and the working copy is made from them (`FirstSetupFiles`); `FirstSetupPlan`
and `LabelMerge` still decide the merge. The master is not filled, SET UP is gone, and the A1b and 2026-10-07 notes below
are history.

#### Built — 2026-10-13 (Alan, A1b)

*Removed in Storage S2b (2026-10-25); kept as history. The rules above still hold.*

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
  `<time>_before-bindforge` in the default player backups folder: each installation's device files, and the bindings folder's `.binds` and `StartPreset.4.start` files under `bindings\`. Only the configured bindings folder is backed up today. Linux detection (2026-10-14) finds each installation's own Proton bindings folder, but the installation list does not store it, so the backup does not yet cover one per installation.
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
**`.binds`** — every binds file in the folder — which decides severity: a device is red when bindings use it, so
losing its entry would break them.

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
| Present in one install only | the working copy takes it |
| Same name, **different VID/PID** | a real conflict — pick one |
| Same VID/PID, **different name** | a real conflict — and it is a rename, see below |

**Rules the list follows** (from first setup's build, 2026-10-07/08; kept 2026-10-24, Alan):

- Two names for one VID/PID inside one installation are not a question — the game uses the first in the file, so a
  later one is dead: shown, not kept. A conflict is asked only where *different* installations disagree.
- No answer may leave two devices on one VID/PID; such a combination stays unanswered until changed. A name that
  wins any conflict is kept, even where it loses another.
- An orphan `.buttonMap` — no device entry behind it — is ignored.
- Nothing of the user's means nothing to set up: the dialog says so, and the working copy is created from what is
  there.
- A failure is named: if the working copy cannot be saved, nothing was written and first setup can run again; if an
  installation cannot be written afterwards, first setup is finished and that installation is reported, for the next
  APPLY or the check ([storage.md](storage.md) rule 8).

**A name conflict is not a device-file edit.** The shared `.binds` names devices by their element name, and so
does the `.buttonMap` filename. Choosing between `RVWAP` and `RightStick` therefore moves three things
together — the element, every `Device="..."` in the shared `.binds`, and the `.buttonMap` file. First setup
routes that into the existing [rename transaction](#renaming-a-device) rather than treating it as a simple
choice.

**`.buttonMap` files merge at the label, and that needs a merge view.** Unlike an entry, two label sets for
one device combine: twenty labels in one install and five in another give twenty-five. But where both files
name the *same* input differently, only the user can say which belongs in the working copy — so overwrite-or-merge
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

The same view serves the later reconcile, when an installation's device files change after first setup — with the
working copy as one side ([the check for device files](overview.md#the-check-for-device-files--2026-10-23)).

**First setup offers two choices for the device files** (2026-10-24, Alan): **use one installation for all** — the
user picks which; its `DeviceMappings.xml` and `.buttonMap`s become the working copy — or **merge** every
installation, deciding each conflict by the four cases above and each label collision per input. If "use one
installation" would remove a device entry that bindings use, its confirmation says so before anything is written,
naming the binds files and the count — *"Epic has an entry Steam does not — LVWAP. 131 bindings in
DualVirpilDawnTreader.4.2.binds use it. Overwriting removes it, and those bindings will stop working. Choose Merge to
keep it."* An entry no bindings use is simply overwritten. Binds files and `StartPreset.4.start` are not part of this
choice: binds files sit side by side in the one folder, and there is one preset file.

**How the reconciled files are made — settled with Storage S2b's sources (Alan, 2026-10-25):**

- **One `DeviceMappings.xml`, from one installation's file.** Frontier's entries are the same everywhere, so the
  first installation holding a `DeviceMappings.xml` gives the file; the user's elements are taken out of it and the
  kept ones put at the top of `<Root>`, each copied whole from the installation whose version won — its
  `<Alternative>` and `<SupportsIcons>` with it — and given the hardware the user chose. A losing or shadowed name is
  not in the working copy.
- **A `.buttonMap` keeps its bytes when its labels came through unchanged**; only one whose labels were merged is
  written again. `VPCPanel` and `VPCThrottle` stay as Frontier ships them.
- **A case-only name conflict rewrites every binds file to the winner's exact spelling**; a reference already spelled
  that way is not counted as moved.
- **"Use one installation for all" is not offered for an installation with no `DeviceMappings.xml`** — the working
  copy would then hold none, and the write leaves every installation's alone.

**First setup never asks which binds file to use** (2026-10-24, Alan). `StartPreset.4.start` names the active one, for
every installation; every binds file goes into the working copy; and the overwrite warning counts bindings across all
of them, since any can be made active later and the game checks them all at startup.

### The flow

1. **"Your files are backed up as of &lt;time&gt;"** — said first, because it is what makes the rest safe to touch
   ([storage.md](storage.md) rule 3's backup, kept forever).
2. **The list** — every difference shown, conflicts sorted to the top, red where bindings use the device.
3. **Two routes through:** **use one installation for all**, or **merge** and answer the conflicts (see above).
4. **A summary before anything is written:** *"Your working copy will hold 6 devices and 5 button maps, and be
   written to 2 installations."*
5. **The last button works like an APPLY** (2026-10-24, Alan): the working copy is created and written to every
   installation, and the report is per installation — *"updated 2 of 2"*, or which one failed and why. No separate
   APPLY is needed after first setup.

**Show everything, ask about little.** Every difference appears in the list, but only the two genuine
conflicts above require an answer. A device present in just one install, or an orphan `.buttonMap`, has one
sensible outcome and is resolved without a question.

*Until 2026-10-24 first setup also took a second, labelled backup of its own and kept an Edit History entry per
installation; rule 3's one backup replaces both.*

### Settled while building it — 2026-10-07 (Alan)

*Removed in Storage S2b (2026-10-25); kept as history. The rules above still hold.*

Built as `bindforge.devicefiles.FirstSetup` (the flow), `FirstSetupPlan` (the decision, pure), `FirstSetupReader`
and `DeviceFilesSnapshot`, with `ui.screen.bindings.FirstSetupDialog` opened from **SET UP** on the Alias Designer
screen, which is offered only while the master is empty.

- ~~**A name conflict is answered only where no binding is lost.** The [rename transaction](#renaming-a-device) and
  its `.binds` rewrite are not built, so a name may be chosen only when the names it beats are not in `.binds`;
  any other choice is shown but disabled, *needs Rename*.~~ *Every name may win since A16 (2026-10-17) - see
  [Names that needed Rename](#names-that-needed-rename--2026-10-17-alan-a16) below.* The winner is written at the top of every installation,
  where [the first match wins](domain-knowledge/EliteDangerous-DeviceMappings-ButtonMap.md#12d-two-entries-for-one-vidpid-the-first-in-the-file-wins--measured-2026-10-03);
  each loser's element and `.buttonMap` stay on disk beneath it as a **pending removal**, for CLEAR's cleanup. When
  `.binds` uses both names, first setup cannot finish until the rename exists.
- ~~**Colliding labels are answered per device, by an installation** — the spec's *use Steam's labels for this
  device*. That installation's label wins every collision; a label only another installation holds is still
  taken.~~ *Kept as the whole-file route; the per-input rows came on 2026-10-08 — see
  [the label merge](#the-buttonmap-label-merge--2026-10-08-alan) below.*
- **"Use one installation as the master"** answers every question in its favour and still takes the elements only
  the others hold, because first setup never drops a custom entry. A question where that installation has no side,
  or its side needs Rename, is left to answer. *Changed 2026-10-24 (Alan): "use one installation for all"
  overwrites, with a warning when bindings use an entry it removes — see
  [Reconciliation](#reconciliation-is-per-device-element-never-per-file).*
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
  [Elite-Intel startup](#default-labels-for-every-connected-named-controller) are BindForge's,
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

### Names that needed Rename — 2026-10-17 (Alan, A16)

**When a name conflict is settled, the bindings follow the winner** — otherwise they would name a device that no
longer exists, and the game would reject the binds file. Reworked 2026-10-24 (Alan):

- **Either name can win.**
- **Bindings using a losing name move to the winner, in every binds file** — a second losing name too, when three
  installations disagree.
- **The confirmation lists each move** — *"Bindings naming RightStick move to RVWAP."* — and the report says how many
  bindings moved.
- **When the names differ only in case, every binds file is rewritten to the winning spelling, exactly.** In
  `.binds`, `LVWAP` and `lvwap` are not the same name, and a binds file holding two spellings of one device is
  rejected ([format §4.3](domain-knowledge/EliteDangerous-BindsFileFormat.md#43-case-only-binds-cares-and-it-cares-about-one-spelling-per-device--measured-2026-10-14)).
  So after first setup every binds file spells each device one way: the way the user picked.

*Until 2026-10-24 this needed a stored old name (`previous_name`), held-back removals and moves retried by later
Applies; with the binds files in the working copy, first setup rewrites them in its one write.*

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
  it gives counts inputs. **"Use one installation for all" takes that installation's labels whole** (2026-10-24,
  Alan); labels only another installation has are dropped — no warning, since labels never break bindings.
- **The same view serves the later reconcile**, with the working copy as one side
  ([the check for device files](overview.md#the-check-for-device-files--2026-10-23)).

### An install that appears later

- **An installation added later** — Add missing, in File Manager's
  [Game Install Locations](file-manager.md#game-install-locations) — **or one that was unreachable at first setup and
  comes back**, is compared with the working copy. If its device files match, nothing is done.
- **If they differ**, the user reconciles them the same way as first setup, before anything is written to it
  ([storage.md](storage.md) rules 1 and 14). It is never overwritten automatically (Alan, 2026-10-25, from the S2b
  review: *"who knows what the user is doing to that other installation"*).

### The user's entries come first in `DeviceMappings.xml` — settled 2026-10-04

When two elements claim one VID/PID,
[the first in the file wins](domain-knowledge/EliteDangerous-DeviceMappings-ButtonMap.md#12d-two-entries-for-one-vidpid-the-first-in-the-file-wins--measured-2026-10-03),
so a user's entry written after Frontier's is silently ignored. The working copy's `DeviceMappings.xml` keeps the
user's elements at the top of `<Root>`, Frontier's beneath them in their own order. *Until 2026-10-24 the device
master was the user's element set rather than a whole file.*

## When an installation's device files differ

**The check finds it and the user reconciles it the same way as first setup**
([the check for device files](overview.md#the-check-for-device-files--2026-10-23)); File Manager's
[Game Install Locations](file-manager.md#game-install-locations) shows which installations match. A reset or missing
`DeviceMappings.xml` is one problem, not one per device — a missing one is always serious, since the game then loads
no bindings. **An unreadable one is named, and putting the working copy back is offered** — writing it replaces
whatever could not be read (2026-10-24, Alan). *Until 2026-10-24 this was a per-device list ranked by colour, each
row with its own ways out — revert, adopt one device, compare labels.*

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
| **Yes** | the alias is pointed at the attached device, as an edit not applied. Every binding follows, because they reference the name — the one-field fix [described above](#why-this-matters-more-than-a-renumbering). |
| **No** | nothing is written. The bindings stay orphaned, and BindForge **says so plainly** rather than falling silent, since that is the state the user just confirmed. |
| **Not now** | nothing is written and **the question is not asked again until the hardware situation changes.** Deferral is not a soft no, and re-asking on every launch turns a useful prompt into something to dismiss reflexively. |

**This is the same shape as [Restore](file-manager.md#restoring-onto-a-machine-whose-installations-differ)
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

**Rules the questions follow** (from A12 and A17, 2026-10-15/17; kept 2026-10-24, Alan):

- Asked only when bindings use the missing controller, under any of its names; Frontier's names are never asked
  about.
- One question per missing controller, listing every candidate plugged in, and **None of these**.
- **Not now** and **None of these** are remembered across restarts, until the set of plugged-in controllers changes or
  the missing one comes back.
- While a question waits, onboarding does not offer to name its candidates.
- A controller the bindings know only by its hardware ID: **yes** opens the naming page; the name, and moving its
  bindings from the old ID to the name, are one edit not applied, written at APPLY.
- A device whose name looks like a hardware ID is never taken for a missing controller.

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
worse trade. Keeping them is also **reversible** — it is an edit not applied, and DISCARD puts everything back. Starting unbound destroys work that no later step can recover.

**BindForge does not try to detect whether the model differs.** It has no map of which physical button is
`Joy_5` on arbitrary hardware, and guessing would produce a confident wrong answer. The warning is shown
whenever a retarget crosses to a device the user picked from the candidate list, and a firmware
update that keeps the same layout costs them one dismissed dialog.

### Built — 2026-10-15 (Alan, A12)

*Built against the master/draft model; moved onto the working copy in Storage S4b (2026-10-28) - see [On the working copy](#on-the-working-copy--2026-10-28-storage-s4b) below. The rules above still hold; the master, draft and Apply named below are history.*

Built as `bindforge.devices.IdentityQuestions` (who is asked, about what - pure), `ui.screen.bindings.IdentityDialog`,
`BindForgeDeviceDraftManager.retargetByName`, and the remembered answers in `bindforge_identity_answer` (migration
`12007`, `BindForgeIdentityAnswerManager`), run by `AliasDesignerController`. Alan answered four questions, one at a time:

- **Named controllers only.** A12 asks about a controller the master names. One the bindings know only by its hardware
  id - the spec's first sketch, *"Your .binds references 334483F3"* - is **slice A17**: a yes there must name the new
  controller *and* move bindings from the **old** hex to the name, which Apply's hex move cannot find (it looks for a
  device's current VID/PID), so it needs a stored "old id → name" record of its own. *Built 2026-10-17 -
  [below](#built--2026-10-17-alan-a17).*
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

### Built — 2026-10-17 (Alan, A17)

*Built against the master/draft model. Since Storage S4a (2026-10-26) a yes opens onboarding, whose FINISH moves the bindings from the old id to the name in the same edit - no record waits for Apply; the record's table (`12008`) was dropped in S4b (2026-10-28). The rules above still hold.*

**A controller the bindings know only by its hardware id.** A stick that never had an entry in `DeviceMappings.xml`
is named in `.binds` by its id - `334483F3` - and a firmware update or a replacement gives it a new one, leaving every
binding on an id nothing has. Built in `bindforge.devices.IdentityQuestions` (who is asked, and what is forgotten -
pure), `IdentityDialog`, `Onboarding.pages` and `OnboardingDialog`, `DeviceOnboarding` and
`BindForgeDeviceDraftManager.onboard` (the record stored with the name), `DeviceApply.moveHardwareIds` (the move), and
the record itself: `bindforge_hardware_id_move` (migration `12008`, `BindForgeHardwareIdMoveManager`). Alan answered
four questions, one at a time:

- **A yes opens onboarding's name page for the picked controller** (option 1 of two), filled with the default name and
  saying *"131 bindings name the hardware ID 334483F3, which you said is this controller. They move to <name> when
  you apply."* Skip takes the default; BACK gives the yes up, and the question comes back next run. *Why:* naming it
  with the default and no page would be the silent creation under a name nobody saw that
  [onboarding rules out](#it-is-the-alias-confirm-gate-moved-earlier).
- **The entry and the move both wait for Apply** (option 1 of two). FINISH puts the name in the draft only, as
  onboarding does for a controller `.binds` names by id; Apply writes the entry, then moves the bindings, and reports
  both together. DISCARD takes the whole yes back with nothing left in the game folders. *Why:* the bindings are dead
  either way while the old stick is gone, so writing the entry early would only show the name in the game's controls
  screen sooner, and onboarding's rule is that such writes
  [land together, in one Apply](#what-onboarding-writes-and-when).
- **When the record is forgotten** (option 1 of two for the one real choice): once Apply has moved the bindings or
  found none left on the old id; once no device in the master or the draft has the hardware they were to move to -
  DISCARD, or CLEAR before Apply; and **once the old controller is attached again**, before the move - its bindings
  then work again and stay with it, while the new controller keeps its name and gets its entry at Apply with nothing
  moved to it. That last is A12's rule that a controller attached again forgets the answer. Kept while no installation
  took the name, or the rewrite stopped, for the next Apply. **A rename before Apply** does not lose it: the record is
  keyed by the hardware the controller has now, not by its name.
- **Not now and None of these are stored under the id** (option 1 of two), in the same `bindforge_identity_answer`
  table, written `3344:83F3`. No name BindForge accepts holds a colon; a name first setup or adopt took from an
  installation is not checked, and XML allows a colon in an element name, but it would have to be exactly `VID:PID` to
  meet one. The id alone would meet a name - `ABCDEF12` is a valid device name. No migration was needed for it, and every A12 rule about an answer holds as it is.

*Also settled while building it (Alan approved them with the sources):*

- **Who is asked:** an id in `.binds` that no controller My Devices lists has - attached, or named in an installation -
  no device in the master or the draft has (those are Apply's own move), Frontier does not cover, and no earlier yes
  already claims. The same id written in both orders is one controller. The candidates, the hold-back from onboarding,
  CHECK AGAIN and the once-per-run rule are A12's.
- **The question is A12's list form** with its first line changed: *"334483F3 is not attached. Your bindings know this
  controller only by its hardware ID - it has no name."* The id is shown as `.binds` writes it. The spec's first
  sketch above was a yes / no / not now question; A12 replaced it with the list, and A17 keeps the list.
- **The old id rides in the device's one rewrite** with any bindings on its current id - all or nothing, as A16's
  handed-over names ride in a rename's.
- **Catch-up moves it too**, since catch-up runs the same move. A controller named only in the draft is not in the
  master yet, so its record waits for the Apply that promotes it.
- **One old id is one controller's**: recording it for a second controller takes it from the first.

*After review — 2026-10-17 (`code-integrity-review.md`, Alan: 1–10 and P2; P1 left for its own question):*

- **A device named like an id is not asked about.** A name may be eight hex characters - `DEADBEEF` - which `.binds`
  is read as giving an id, so its bindings looked like a missing controller's, and a yes would have moved them off a
  working one. An id is now skipped when any device in the master, the draft or an installation has it as a name,
  old names included. *Left (P1):* whether the naming rule should refuse such names at all.
- **Every way out of the name page reads the screen again** - BACK, a page that could not open, nobody left to name -
  not only FINISH. The same batch of answers may have written a retarget or a Not now.
- **The old id is shown as `.binds` writes it, once**, by the question and by the name page after it - not as the
  `VID:PID` the answer is stored under. With the id in both orders, the one in the game's order, VID then PID.
- **One read of `.binds` per refresh** for both counts the question needs (`BindsDeviceReferences.counts`).
- *Also:* `Question.answerKey()` and `hardwareSetKey(…)`, so no accessor shadows a static; each question is built once;
  the manager's write is only the draft's, in its transaction; one form for an id in the logs, `VID 3344 PID 83F3`.

### On the working copy — 2026-10-28 (Storage S4b)

The identity question reads the working copy with the edits on it (`bindforge.storage.Edits`), as the rest of the
screen has since S4a. Alan approved the sources on 2026-10-28:

- **The named controllers are the user's elements in the edits' `DeviceMappings.xml`** - every element Frontier does
  not ship, with its own VID/PID. A controller named or retargeted in the edits is spoken for, so it is no candidate.
- **The bindings are counted in the edits' binds files** (Alan, option 1 of two), as onboarding counts them - so a
  rename or a hardware-id move not yet applied is counted the way APPLY will write it. Until S4b the question counted
  the live bindings folder.
- **A yes for a named controller is an edit not applied** (`DeviceEdits.retarget`, `DeviceMappingsMerge.retarget`):
  the element keeps its name, its place in the file and every other child, and only its `<VID>` and `<PID>` change.
  The binds files and the `.buttonMap` are untouched - the bindings name the device. Several answers are one edit. A
  device whose element is gone, or no longer has the hardware the question saw, changes nothing and is reported as
  changed, as before.
- **No record of a yes is kept.** A12's answers still go to `bindforge_identity_answer`; A17's waiting moves went with
  their table, since onboarding's FINISH moves the bindings in its edit.

## Automatic Device Registration

Any controller connected to the system — whether already connected when BindForge starts, or plugged in later during a session (hot-plug) — is listed in My Devices with no user action required; nothing is stored until it is named. This applies the same "detect and inform, don't ask" principle already used for [First Run](#first-run-and-hot-plug) below, extended to individual devices as they're seen.

- **Default alias:** the name the Device Service reports, **cleaned into a valid name** by the [default-name rule](#the-default-name-rule) — `Virpil Controls 20220720` becomes `VirpilControls20220720`. [Onboarding](#onboarding--every-controller-gets-a-name-and-a-buttonmap--settled-2026-09-13) shows it to the user before anything is written.
- **VID/PID:** filled in immediately from the connected hardware, same as any other device entry.
- **Its default labels are shown on screen once the device has a name, and its `.buttonMap` is written at APPLY**
  ([writes outside Apply — none](overview.md#writes-outside-apply--none), 2026-10-23). *Until then a `.buttonMap` was
  created straight away, at onboarding or at Elite-Intel startup.*
- **Registration is the row appearing, and writes nothing** — ~~touches only the working copy~~ *(reworded
  2026-10-15, A7: since [the list became the union](#my-devices) of live hardware and file entries, a detected
  controller is listed without being stored anywhere, draft included).* Detecting a controller writes nothing. A name and its labels are edits not applied, written at APPLY; nothing reaches the game outside APPLY and RESTORE.
- **Rules the offer follows** (from A7, 2026-10-15; kept 2026-10-24, Alan): a plugged-in controller is offered a name
  once per run — CHECK AGAIN, the gear and the next run still ask; plugging in or out updates the list at once while
  it is showing; the offer never opens over another dialog; several controllers plugged in together are one dialog.
- **Hot-plugging into BindForge is safe; hot-plugging into a running game is not — those are different things.** BindForge can list a newly-connected controller at any time without risk. Physically connecting or disconnecting a controller while Elite Dangerous itself is running is a separate, game-level risk (it can crash the game) that exists independently of BindForge and is outside BindForge's control — automatic registration does not cause or worsen this; it just means BindForge's own device list stays current regardless of when the user plugs something in.

### Built — 2026-10-15 (Alan, A7)

*Built against the master/draft model; moved onto the working copy in Storage S4a (2026-10-26) - see [Built on the working copy](#built-on-the-working-copy--2026-10-26-storage-s4a). The rules above still hold; the master, draft and Apply named below are history.*

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
  offered silently and to no one; its `.buttonMap` is [startup generation's](#default-labels-for-every-connected-named-controller),
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
- **What reaches the game's file.** Registration writes nothing *(2026-10-15, A7)*; a name and its labels are edits
  not applied, written at APPLY. **Only devices the user has named are in the working copy** (2026-10-24, Alan), so
  entries never pile up for hardware that was merely plugged in — the reason behind the 2026-09-06 rule that Apply
  pushes only what changed, now met without it, since APPLY writes the whole working copy.

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
| The first time BindForge is opened, alongside [First-Time Startup](overview.md#create--a-new-players-first-binds-file--2026-10-24) | every connected controller with **no `DeviceMappings.xml` entry** |
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

**A name given at onboarding is an edit not applied**: its `DeviceMappings.xml` entry and `.buttonMap` are written at
APPLY (2026-10-23, Alan). **A controller the bindings name by hardware ID** — `Device="334483F3"` — has its bindings
moved to the new name **in the same APPLY**, never one without the other: once an entry exists, `.binds` is expected
to name the device ([format §4.0](domain-knowledge/EliteDangerous-BindsFileFormat.md#40-the-rule-confirmed-2026-09-08)),
and there is no evidence the game honours the old hex references meanwhile, so an entry without the move, or the move
without the entry, can leave those bindings dead ([testing item 13](../../00-overview/testing-required.md)).
Onboarding says so: *"131 bindings will move to LVWAP when you apply."* That is the
[before-the-fact offer](#detecting-it-and-getting-ahead-of-it), folded into naming. *Until 2026-10-23 a controller
the bindings did not name by ID was written straight away.*

**Rules onboarding follows** (from A6b, 2026-10-14; kept 2026-10-24, Alan):

- **Three ways in:** automatically, once per run, after first setup; **NAME** in an unnamed device's editor; and
  **CHECK AGAIN**, which offers every controller still unnamed — and says so when none is (Krondor, 2026-10-14: a
  silent CHECK AGAIN produced support tickets).
- **Nothing happens until FINISH.** Skip and Next only move between pages; BACK, the close glyph or Esc leave with
  nothing changed.
- **APPLY tidies up after itself:** any named device the bindings still name by hardware ID has those bindings moved
  to its name — whoever named it, an entry added by hand outside BindForge included.
- **Two unnamed controllers on one VID/PID:** only the first one's page promises the binding move.
- **A binds file that cannot be read stops onboarding** — otherwise a controller the bindings name by ID could be
  named while its bindings stayed on the ID.
- **If an installation cannot be written** at that APPLY, the others still are and the binding move still happens
  ([storage.md](storage.md) rule 8); the missed installation is reported, for the next APPLY or the check.

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
  yet, for the next Apply. *The [rename](#how-a-rename-is-done--2026-10-24) keeps its own
  all-or-nothing rule for now; whether it follows this one is A11's question.*

#### Built — 2026-10-14 (Alan, A6b)

*Built against the master/draft model; moved onto the working copy in Storage S4a (2026-10-26) - see [Built on the working copy](#built-on-the-working-copy--2026-10-26-storage-s4a). The rules above still hold; the master, draft and Apply named below are history.*

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

### Default labels for every connected, named controller

A controller that **has a name** but no `.buttonMap` gets default labels — `Button 1`, `X Axis`, `Hat 1 Up` — with no
prompt, because there is nothing to decide. That covers Frontier's built-ins, which almost never ship one.

**The default labels are shown on BindForge's screens straight away, and written whenever the user next presses
APPLY for any reason** (2026-10-24, Alan). They do not light the APPLY bar on their own: they are not the user's
edits. A user who never applies anything is no worse off than without BindForge — the game shows `Joy_3`, as it
always did. *Until 2026-10-24 a `.buttonMap` was written into every installation at Elite-Intel startup and on every
connect, once BindForge was activated (Krondor, 2026-10-14).*

- **Connected controllers only.** A device plugged in once, years ago, gets nothing.
- **Never overwrites** a `.buttonMap` that already exists, whether Frontier's or the user's.
- **A device the game draws itself is skipped** — one with Frontier's button icons (`<SupportsIcons>`: the X52, X52
  Pro, T.Flight HOTAS 4, T.Flight Hotas One) and `<GamePad>`, where `Button 3` would replace an icon.
- **Only the axes the controller has**, and **four labels per hat**; where the axes cannot be read, all eight.
- **Labels are English and fixed** — file content the game displays, not Elite-Intel's interface text.
- **Nothing is named here:** a controller with no name waits for onboarding.
- **After RESET LABELS** on a built-in, the defaults come back the same way.

*Removed in Storage S2b (2026-10-25); kept as history. The rules above still hold.*

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
  is drift for [the startup check](overview.md#the-check-for-device-files--2026-10-23) to report and
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

### A gamepad is not a joystick — found 2026-10-23 (Krondor)

**Not built. A design to settle before [A18](../../05-build-plan/alias-designer.md#slices) and
[E7](../../05-build-plan/bind-editor-editing.md#slices).** A tester reported in the dev chat that the Device Editor *"
doesn't recognize my Xbox controller
triggers"*. Krondor has no Xbox pad; this was worked out from the code and from Frontier's shipped presets, not on hardware.

**SDL is not the
problem.** SDL3 reads Xbox pads well and reports each trigger as an axis of its own. What fails is A8b's description, on Windows, in one of two ways:

- **Through
  XInput** (the usual driver), SDL gives no HID device path, so `WindowsHidUsages.read` returns nothing and the axes are
  *unknown*.
- **Through HID**, Windows' description of an Xbox pad puts both triggers on
  **one** shared Z axis — DirectInput's long-standing limitation — so it counts five axes where SDL counts six, `agreesWith` fails, and the axes are
  *unknown* again.

Either way `axisAt` is empty and no axis is lit, sticks included. On Linux the `xpad` driver is not HID, so there is no `report_descriptor` to read either.
*Expected from the code, not seen.*

**The bigger problem is the token
vocabulary.** The game does not bind a pad on `<GamePad>` by `Joy_` tokens: it writes `Device="GamePad"` with tokens of its own, `Pad_A`, `GamePad_FaceDown`, `Pos_Pad_LTrigger` and the rest ([`.binds` format §5.6](domain-knowledge/EliteDangerous-BindsFileFormat.md#56-gamepad-codes--read-from-frontiers-presets-2026-10-23)). BindForge treats the pad like a stick:

| Where | What happens with a pad on `<GamePad>` | Cost |
|---|---|---|
| Capture dialog (E1) | a button press is captured as `Joy_N`, the D-pad as `Joy_POVnUp`… and written under `GamePad` | **a binding the game most likely ignores** — not measured |
| Capture dialog, triggers | axes are not captured at all until E2/E3 | a trigger cannot be bound as a button |
| Device Editor (A9) | `Joy_` label boxes for a device the game draws itself ([`gameDrawsIt`](#default-labels-for-every-connected-named-controller)) | labels that never show in game |
| Device Editor lights | axes unknown, so nothing is lit | the reported bug |

**Settled 2026-10-27 (Alan, from Krondor's storage review; Storage S4R): a GamePad's sticks are not captured.** Andrea's
gamepad work, merged from `V1.2-i18n-IT`, already captures buttons, the D-pad and the triggers under `GamePad_` tokens. It
captured a stick pushed one way as `Neg_Joy_XAxis` and the like, but a pad never uses `Joy_` tokens, and no preset shows
which of its own tokens the game writes for half a stick. So the capture dialog ignores a pad's sticks
(`ControllerCapture.gamepadAxis`), and nothing is guessed into a binds file. The answers to the open questions below
decide what replaces this.

**Proposed (Krondor, 2026-10-23) — to settle with Alan before building:**

- **Read a gamepad as a
  gamepad.** When `SDL_IsGamepad` holds, `DeviceService` also opens it with SDL's Gamepad API and polls its named inputs (`SDL_GetGamepadAxis`, `SDL_GetGamepadButton`): south/east/west/north, the bumpers, both sticks and thumbs, both triggers, the D-pad, back and start. They are published as new gamepad events keyed by a `devices.model` enum. The raw joystick events go on as now, so the input monitor and push-to-talk are unchanged. The flag is a lookup, `DeviceService.isGamepad(id)`, like `description(id)` — not a widened `Device`. In this API a trigger runs 0 to 32767 and rests at 0.
- **Map the named inputs to the game's
  tokens** in one pure table, with the `Pos_` form for a trigger used as a button, as the presets bind it. **Which
  family the game writes today is not known** — see the open questions.
- **Capture on `GamePad` offers and writes gamepad
  tokens**, never `Joy_N` or hats. A trigger pulled past a threshold is a press of its `Pos_` token and can be held as a modifier, by `DeviceEditorLights`' rest rule.
- **The Device Editor shows a `<GamePad>`
  read-only:** its inputs by name, lit live through the gamepad events, and a note that the game draws them itself. No label boxes, so nothing for SAVE.

**Open — the tester's answers decide them** ([testing item 27](../../00-overview/testing-required.md#bindforge)):

1. **Which family the game writes when a pad is bound in its own controls screen** — `Pad_` or `GamePad_`. Frontier's
   `ConsoleX360` preset uses both.
2. **Which pad, by VID/PID.** Frontier's `<GamePad>` lists the Microsoft ids `028E`, `028F`, `02FF`, `02A1`, `0291`,
   `0719`, `02EA`, `0B13` and `02D1`, but **not `0B12`, the wired Xbox Series pad**. A wired Series pad would not be on
   `<GamePad>`, BindForge would treat it as the user's own controller, and this section would not be its bug.
3. **Whether the game also sees the pad as a
   joystick**, which would make `Joy_` bindings on its VID/PID real. The answer to 1 likely shows it.

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
- **DEVICE** holds the name, VID and PID, the hardware name and whether it is attached. *Its installations block
  was removed 2026-10-24 — [no installations in the Device Editor](#no-installations-in-the-device-editor--2026-10-24).*
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
- **APPLY is not in the dialog.** It is the one
  [APPLY above BindForge's tabs](overview.md#apply-and-discard--one-pair-above-bindforges-tabs-2026-10-24-alan), and
  reaches every installation.
- **SAVE keeps the changes as edits not applied** (2026-10-24, Alan): it closes the dialog and lights the APPLY bar;
  nothing is written until APPLY. The word stays SAVE — what users expect of a dialog — since the lit bar and the
  leave prompt show the work is not in the game yet.

**Rules the dialog follows** (from A9, 2026-10-10; kept 2026-10-24, Alan):

- **SAVE keeps every box, defaults included**, so a controller's labels are complete when written — except on a
  device the game draws icons for (`<SupportsIcons>`, `<GamePad>`), where only boxes the user changed are kept, so a
  `Button 3` never replaces an icon.
- **SAVE is dimmed until a box differs** from what the dialog opened with.
- **No default button** — Enter in a label box must not save and close mid-edit.
- **Where the axes cannot be read, all eight are offered**, with a line saying so.
- **An unplugged controller with no saved labels** shows DEVICE alone, with *"plug it in to see every input."*

#### Built as a dialog — 2026-10-10 (Alan, A9)

*Built against the master/draft model; moved onto the working copy in Storage S4a (2026-10-26) - see [Built on the working copy](#built-on-the-working-copy--2026-10-26-storage-s4a). The rules above still hold; the master, draft and Apply named below are history.*

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
- **The labels shown are the draft's** when the draft holds the device, and the master's otherwise.
- **A box holds at most 18 characters** as it is typed; a longer saved label is cut to 18 on SAVE.
- **No default button** — Enter in a label box would otherwise save and close mid-edit.
- **Hat movement is polled by the Device Service** — `DeviceHatEvent`, SDL's direction bits, on change only.
- **An unattached controller with no saved labels** shows DEVICE alone, and says to plug it in.

### One record, and where it lands — reworked 2026-09-26

**One record per device, in the working copy.** One name, one set of labels; APPLY writes them to every
installation ([principle 8](principles.md)). *Until 2026-10-24 the user edited a master, and before 2026-09-26 the
editor had a tab per installation.*

#### No installations in the Device Editor — 2026-10-24

**The DEVICE tab shows no installations** (Alan; [principle 12](principles.md)): the editor is about the device being
edited, and where its files go is File Manager's [Game Install Locations](file-manager.md#game-install-locations). The
space is kept for whatever the editor needs later. *Until 2026-10-24 it carried an install strip — each installation,
its folder and M / differs — and before that a tab per installation.*

### Fields and rules

- **VID/PID are always read-only**, always derived from the physical hardware — the game attaches a device by
  VID/PID, so hand-editing this would break the association. They are device-level ~~, identical on every
  tab~~ — there are no per-installation tabs any more (2026-10-10).
- **Edit Mode** activates the moment any field changes. A persistent label reminds the user that SAVE keeps the
  changes until APPLY.
- **Alias name validation — tightened 2026-09-13.** 3–50 characters; **must start with a letter**; then
  letters, digits, `-` and `_` only; **no spaces and no period**; must not collide with any existing entry,
  including Frontier's built-ins, compared [ignoring case, `-` and `_`](#the-default-name-rule). *The earlier
  rule allowed spaces and a wider set of punctuation. But the alias becomes an XML element tag and a
  filename stem, and a space is valid in neither — so that rule would have accepted a name the game
  cannot read.*
- **No confirm step — 2026-10-10 (Alan).** The confirm was the gate for a name nobody had looked at, from when
  registration named controllers silently. [Onboarding moved that gate earlier](#it-is-the-alias-confirm-gate-moved-earlier),
  and every other way a device gets a name — first setup, the reconcile — shows the name
  too, so every named device has been seen. The editor has **no CONFIRM button and no lock**. Its labels are
  editable once the device has a name; a device with none has its input tabs disabled until onboarding names
  it. A built-in's name is Frontier's, so its labels are editable straight away. *`alias_confirmed` is no longer read by
  anything.*
- **The name box renames the device** (built in A11). A rename is an edit not applied: the `DeviceMappings.xml`
  element, its `.buttonMap` and every binds file naming it change together at APPLY (see
  [Renaming a Device](#renaming-a-device)).
- **Button and axis names** ~~are edited in Axis/Buttons sub-tabs~~ are edited on the dialog's input tabs
  *(2026-10-10)*. The UI sizes dynamically to the device's real inputs — never hardcoded; a device with three
  axes shows three, **and shows the three it has**: an X, Y and RZ stick shows `Joy_XAxis`, `Joy_YAxis` and
  `Joy_RZAxis`, read by [A8b](../../05-build-plan/alias-designer.md#slices), never the first three by position. A hard 18-character limit
  applies to every button/axis friendly name, confirmed by direct in-game testing. The alias field has its own
  rule (3–50 characters) and no in-game display slot, so truncation is not a concern there.
- **Every input gets a label — 2026-10-10 (Alan).** A box is never empty: one with no saved label shows the
  default, and clearing a box puts the default back. The defaults are [the generated ones](#default-labels-for-every-connected-named-controller)
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

| Action | What it changes | Touches the game? |
|---|---|---|
| **SAVE** | the device's changes, kept as edits not applied — the dialog's right-hand button | No |
| **BACK** | throws away the dialog's unsaved changes, asking first when there are any | No |
| **CLEAR** | removes the device's entry and its `.buttonMap`, as an edit not applied — not offered on a built-in. The dialog's middle button | No, until APPLY |
| **RESET LABELS** | a built-in's labels back to Frontier's, as an edit not applied. In CLEAR's place on a built-in; see [built-ins](#built-in-controllers-in-my-devices--settled-2026-09-13) | No, until APPLY |
| **APPLY** | everything, to every installation — the one [APPLY above BindForge's tabs](overview.md#apply-and-discard--one-pair-above-bindforges-tabs-2026-10-24-alan) | **Yes** |

**SAVE is deliberately the safe one.** It is the button people press by habit, so it must never be the button
that writes into a live game installation.

**CLEAR, not Delete.** A device cannot be removed from the list while its hardware is attached — it would simply
reappear, because the list is driven by what is plugged in. What CLEAR removes is the device's `DeviceMappings.xml`
entry and its matching `.buttonMap`, returning the device to the state it had before it was ever configured. The
device stays listed, showing *not added*. This also replaces the old "delete and recreate to fix a wrong VID/PID"
repair path, which never worked: automatic registration would re-add a connected device immediately.

**CLEAR asks first, naming no installations** — *"Clear RHVCAPDirection? APPLY removes its entry and its labels
from your game files."* — and RESET LABELS the same way, using [`HudConfirmDialog`](ui-component-map.md), never
`JOptionPane` (Alan, 2026-10-27, from the S4a review: editors show no installations - principle 12 - and since APPLY
leaves alone an installation that differs, *"all 3"* could be wrong). *Until 2026-10-27 it named the count - "Clear
RHVCAPDirection from all 3 installations?"*

**What APPLY does to each installation's device files** (2026-10-24, Alan):

- **They are written whole from the working copy**, so labels the working copy no longer has leave the file.
- **A missing `DeviceMappings.xml` is simply written** from the working copy, and the report says so — without it
  the game loads no bindings.
- **A file that already matches is not rewritten**, so an APPLY with nothing new for an installation writes nothing
  there.
- **Each installation's device files are made to match the working copy:** a `.buttonMap` the working copy no
  longer has — after CLEAR, or a rename — is removed from every installation, **except Frontier's own shipped ones**
  (`VPCPanel`, `VPCThrottle`), which are never removed. A label file added by hand inside an installation is never
  removed unasked: the check runs first at APPLY, shows it as a difference, and the user decides.
- **An installation that cannot be written** is reported, and the others are still written ([storage.md](storage.md)
  rule 8).

*Until 2026-10-24 edits sat in a database draft beside a database master, Apply never removed anything — CLEAR and
renames kept lists of what to remove — and one installation's files changed all or nothing; the working copy and
whole-file APPLY replaced all of it.*

#### Built on the working copy — 2026-10-26 (Storage S4a)

The screen reads the working copy with the edits on screen (`bindforge.storage.Edits`), never an installation or a
database table, and every action is a change to those edits (`bindforge.storage.DeviceEdits`); the one APPLY above
BindForge's tabs writes them ([storage.md](storage.md#how-the-files-are-written--2026-10-24)). Removed with the old
model: the screen's own APPLY and DISCARD, the divergence list and its ways out, the Installations columns and the
Device Editor's install strip, catch-up, Edit History and the device-file poll.

- **My Devices** is live hardware with the edits' `DeviceMappings.xml`: the alias is the edits' name, so a name given
  or changed shows at once. A device cleared in the edits stays listed under the working copy's name, *not added*, so
  its editor opens with UNDO CLEAR.
- **SAVE** writes the device's whole `.buttonMap`; with the name changed, a rename (`DeviceMappingsMerge.rename`, the
  map moved, every binds file rewritten), a shadow, or going back to Frontier's name - each one edit.
- **CLEAR** takes out the element and its map; **RESET LABELS** the map, or Frontier's own back for `VPCPanel` and
  `VPCThrottle`; **UNDO** puts back what the working copy holds - the element copied whole, where it led the file.
- **Onboarding's FINISH** is an edit (Alan, 2026-10-26): the element after the user's others, ahead of Frontier's;
  the default labels unless a map is already under the name; every binding naming it by hardware id - and an old id it
  replaces (A17) - moved to the name. Nothing reaches the game until APPLY.
- **Default labels** are worked out by APPLY as it runs, for every connected, named controller with no map in the
  edits it writes (`Apply.DefaultLabels`, `AliasDesignerReader.defaultLabels`), and are never part of the edits, so they
  never light the bar (Alan, 2026-10-27, from the S4a review: worked out at each refresh, they went stale after a
  rename applied before the refresh, or a controller plugged in while the screen was not showing).
- **No rename is ever "under way"**, so a renamed device can be renamed again before APPLY, and CLEAR no longer waits
  for one. No chains holds: every name the working copy holds stays taken until APPLY.

*Decided while building it (2026-10-26), confirmed by Alan 2026-10-27:*

- **A Frontier element covering several controllers keeps its `.buttonMap` when one of them is shadowed** - another
  controller may still be called by it - and **going back to its name is refused while a map of the user's is under
  it**, since this controller's labels would go over it. There is no record any more of which controller a shared
  element's labels were made for. For an element covering one controller (`T-Rudder`) the map goes with the name.

#### An edit `.binds` depends on updates `.binds` — 2026-10-11 (Alan)

**When the user changes a `DeviceMappings.xml` entry, or its `.buttonMap`, that the shared `.binds` depends on,
BindForge updates `.binds` to match**, so that no edit of the user's leaves bindings pointing at a device the game no
longer knows. **The rewrite is part of the same edit not applied**, in the working copy's binds files, and APPLY
writes everything together (2026-10-24, Alan).

- **Every binds file, every kind of line.** Every `Device="…"` is rewritten — on `<Primary>`, `<Secondary>`,
  `<Binding>` and `<Modifier>` alike — in every `.binds` in the folder, not only the active one, since the game
  validates them all.
- **Matching finds every spelling; the rewrite writes exactly one** — the new name, as the user spelled it — so a
  binds file ends up with one spelling per device ([format §4.3](domain-knowledge/EliteDangerous-BindsFileFormat.md#43-case-only-binds-cares-and-it-cares-about-one-spelling-per-device--measured-2026-10-14)).
- **A hardware-ID reference is matched VID then PID only**, the order the game writes, so a second device whose
  VID and PID are this one's swapped is never renamed by mistake; it is read in either order, as
  [format §4.2](domain-knowledge/EliteDangerous-BindsFileFormat.md#42-vidpid-derived-hex-identifiers) asks.

What it reaches:

- **A rename** rewrites every binding naming the old name ([Renaming a Device](#renaming-a-device)).
- **A retarget** keeps the name, so `.binds` does not change.
- **RESET LABELS and every label edit** are not reached: `.binds` names a device by its element and its inputs by
  token (`Joy_5`), never by label.
- **CLEAR on a device the bindings use moves those bindings to its hardware ID** — VID then PID, upper case, as the
  game itself writes a device with no entry ([format §4.0](domain-knowledge/EliteDangerous-BindsFileFormat.md#40-the-rule-confirmed-2026-09-08))
  — as part of the same edit (2026-10-24, Alan). Its confirmation says so: *"142 bindings name LVWAP. Clearing it
  moves them to its hardware ID, 3344:83F4 — they keep working while the stick is plugged in, but lose its name."*
  *Built today: the move to a hardware ID is not, so CLEAR is refused for such a device and says how many bindings
  name it — a build state, for the build plan.*

**Rules from A10** (2026-10-11; kept 2026-10-24, Alan):

- **After CLEAR or RESET LABELS the dialog says so** — *"Cleared. APPLY removes it from every installation."* — its
  input tabs are disabled, and the middle button becomes **UNDO CLEAR** (or **UNDO RESET**), which puts it back with
  no question.
- **A bindings folder that cannot be read counts as unreadable, not empty**, so a typo in settings or an unplugged
  drive never reads as "nothing uses this device" and lets CLEAR strip an entry the game needs.

*Until 2026-10-24 the rewrite was ordered across a draft, its baseline and the game's file — the game's `.binds` last,
all or nothing — to protect a separate draft (F2, 2026-10-12); with the binds files in the working copy it is part of
the edit.*

#### Built — 2026-10-11 (Alan, A10)

*Built against the master/draft model; moved onto the working copy in Storage S4a (2026-10-26) - see [Built on the working copy](#built-on-the-working-copy--2026-10-26-storage-s4a). The rules above still hold; the master, draft and Apply named below are history.*

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
  built-in it deletes the file in every installation, and [startup generation](#default-labels-for-every-connected-named-controller)
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
  folder is an answer: no custom binds file names anything. Severity stays lenient — there it only turns findings
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

*Removed in Storage S4a (2026-10-26): an installation that missed a write differs from the working copy, so APPLY leaves it alone and the check reconciles it (storage.md, How the files are written; Storage S9). Kept as history.*

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

Rename one link without the others and the bindings break: an installation whose `DeviceMappings.xml` lacks a name
the binds file uses
[rejects the entire binds file and falls back to KEYBOARD & MOUSE in all four sections](domain-knowledge/EliteDangerous-DeviceMappings-ButtonMap.md#12b-what-happens-when-a-device-name-has-no-entry--measured-2026-09-22)
— measured, not predicted. The only symptom is a line in `BindingLoadingErrors.log`.

### How a rename is done — 2026-10-24

**A rename is one edit not applied** (Alan): the element, the `.buttonMap` and every binds file change together in
the working copy, and APPLY writes them all at once. If an installation cannot be written, it is reported and the
next APPLY or the check catches it ([storage.md](storage.md) rule 8) — the partial rule chosen for renames on
2026-10-14.

1. **The element is renamed in place**, keeping its position in the file and every child it has (`<Alternative>`,
   `<SupportsIcons>`).
2. **The `.buttonMap` is renamed, never copied.** The old-named file is removed from every installation at APPLY
   ([what APPLY does](#actions-and-what-each-one-reaches)).
3. **Every binds file is rewritten** to the new name ([An edit `.binds` depends on](#an-edit-binds-depends-on-updates-binds--2026-10-11-alan)).
   **A rename that changes only case does this too**: in `.binds`, `LVWAP` and `lvwap` are not the same name.
4. **A device's own names never count as taken by itself**, so `Lvwap` → `LVWAP` is allowed.
5. **No chains:** a name one device gives up cannot be taken by another device until the next APPLY, so no two
   rewrites depend on their order (`LVWAP` → `LeftStick`, then another device → `LVWAP`).
6. **Linux with several installations:** APPLY's confirmation says controls are updated only for the installation
   Elite-Intel uses (2026-10-14).

*Until 2026-10-24 a rename ran installation by installation against a draft: `.binds` last and only if every
installation succeeded, rolled back on failure, then a stored old name (`previous_name`) and moves retried by later
Applies, with CLEAR refused while a rename was under way. The working copy and whole-file APPLY replaced all of it.*

#### Settled before building it — 2026-10-14 (Alan, A11)

*Built against the master/draft model; moved onto the working copy in Storage S4a (2026-10-26) - see [Built on the working copy](#built-on-the-working-copy--2026-10-26-storage-s4a). The rules above still hold; the master, draft and Apply named below are history.*

- **~~All or nothing~~ — a rename follows onboarding's partial rule** (Alan, option 3 of three). The user's Apply
  comes first, as for [onboarding's hex move](#settled-before-building-it--2026-10-14-alan-a6b): every installation
  that can be written is renamed, and `.binds` moves to the new name once **at least one** took it. One that missed
  it is named in the report and caught up later — by the next Apply, by **Revert**, or by
  [A14's catch-up](#actions-and-what-each-one-reaches) — since the push renames wherever the old name is still
  found. Only when **no** installation took it does `.binds` stay on the old name, and the rename is reported as not
  applied yet. *This knowingly accepts the cost measured above: until it is caught up, an installation that missed
  the rename rejects the binds file.* The four paragraphs above are kept for that reasoning.
- **The `.binds` move is derived, not recorded**, as A6b's is: at each Apply, a master device that still carries a
  `previous_name` which `.binds` uses has its bindings moved. A move that failed is retried by the next Apply.
  `previous_name` is cleared once every installation has lost the old name and `.binds` no longer uses it.
- **A rename that changes only case touches no `.binds`** — measured: a `Device=` name finds its element ignoring
  case ([`.binds` format §4.3](domain-knowledge/EliteDangerous-BindsFileFormat.md#43-case-only-binds-cares-and-it-cares-about-one-spelling-per-device--measured-2026-10-14)),
  and a binds file spelling one device two ways is rejected, so rewriting only some references would be worse than
  none. The element and the `.buttonMap` are renamed.
  *Corrected 2026-10-24: a case-only rename rewrites every binds file to the new spelling — `LVWAP` and `lvwap`
  are not the same name in `.binds` (Alan).*
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
  names that `.binds` uses, and first setup that cannot finish because `.binds` uses both names. *Built 2026-10-17 -
  [Names that needed Rename](#names-that-needed-rename--2026-10-17-alan-a16).*
- **A11 renames the user's own devices only** (Alan, 2026-10-14). On a built-in still under Frontier's name the
  name field stays read-only, as today. Renaming or shadowing an attached built-in
  ([above](#built-in-controllers-in-my-devices--settled-2026-09-13)) needs its presentation designed first, and is
  slice A15. *Built 2026-10-16 as a shadow only - [A15](#built--2026-10-16-alan-a15).*
- **Linux with several installations is told, not fixed.** Apply's confirm says, when BindForge has found more than
  one Linux installation, that controls are updated only for the installation Elite-Intel uses. Until the later slice,
  a rename can leave another installation's binds file naming the old device, and that installation falls back to
  keyboard and mouse.

#### Decided while building it — 2026-10-15 (A11)

*Built against the master/draft model; moved onto the working copy in Storage S4a (2026-10-26) - see [Built on the working copy](#built-on-the-working-copy--2026-10-26-storage-s4a). The rules above still hold; the master, draft and Apply named below are history.*

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

*Built against the master/draft model; moved onto the working copy in Storage S4a (2026-10-26) - see [Built on the working copy](#built-on-the-working-copy--2026-10-26-storage-s4a). The rules above still hold; the master, draft and Apply named below are history.*

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

The first opening of BindForge is first-time startup ([Activation](#activation--2026-10-13-alan), [storage.md](storage.md) rules 1–4).

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

*Built against the master/draft model; moved onto the working copy in Storage S4a (2026-10-26) - see [Built on the working copy](#built-on-the-working-copy--2026-10-26-storage-s4a). The rules above still hold; the master, draft and Apply named below are history.*

Built as `bindforge.devices.MissingControllers` (who is named — pure) and `MissingControllerWarning` (the one warning, spoken and logged), triggered by the Alias Designer screen (`AliasDesignerController.onShown`). Alan answered three questions, one at a time:

- **Only controllers `.binds` names.** A named controller that is not attached is mentioned only when a binding
  names it (`BindsDeviceReferences.referenceCounts`, ignoring case) — the reason to warn at all is bindings that will
  not load, and an entry for hardware sold years ago would otherwise be announced on every run. "Named" is the
  working copy's name, or before first setup the installation's entry name. A Frontier name is never mentioned; that is
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
[version-bump merge](overview.md#what-the-check-tells-the-user--2026-10-23).*

## Exit Prompt

**Leaving BindForge with edits not applied asks the one leave prompt** — Go back / Discard / Apply
([Unsaved Work on Exit](overview.md#unsaved-work-on-exit)).

**Inside the Device Editor — 2026-10-10 (Alan).** Closing the [dialog](#a-dialog-opened-from-the-row--2026-10-10-alan)
with unsaved changes — BACK, its close glyph or Esc — asks **Save**, **Discard** or **Keep editing**. The third
answer exists because the user is still on the screen and may only have pressed BACK by mistake. Save there keeps
the changes as edits not applied. *Until 2026-10-24 every action wrote a draft as it was pressed, so the screen had no
exit prompt of its own (Core C6, 2026-10-12); edits now stay on screen until APPLY, so the leave prompt applies.*

## Deferred

- **Recalibrate** — a feature to fix axis centre-point drift during live highlighting. Out of scope for this iteration.
- **Duplicate-VID/PID handling — dropped from scope 2026-09-07; nothing is designed and nothing needs to be.**
  The existing `DeviceDuplicateWarningEvent` tells the player, and the alias-uniqueness rule below rejects the
  second automatic registration on its own. Retained for the reasoning: Two connected devices of the identical model genuinely can report the same VID and PID (this is normal USB behaviour, not an edge case) — combined with [Automatic Device Registration](#automatic-device-registration), both would attempt to auto-register under the same default hardware-reported alias, which the alias-uniqueness rule would reject. Neither `.binds` nor `DeviceMappings.xml` has any field that could distinguish two entries sharing a VID/PID, and the game itself keys on VID/PID, so it could not act on a distinction even if BindForge drew one. That is why nothing is designed here rather than deferred: there is no design that would help. See [conflicts-and-open-questions.md](../../99-archive/conflicts-and-open-questions.md).

## Design Considerations — Closed

**The master-copy question is closed — in favour of the working copy (2026-10-22, Alan).** Device configuration is
one working copy, edited on screen and written to every installation at APPLY. *From 2026-09-24 to 2026-10-22 it was a
draft edited against a separate master.* See [storage.md](storage.md).
