# BindForge — File Manager

**File Manager is BindForge's primary feature — the main reason BindForge exists.** Backing up the binds
and configuration files is the most important thing this tool does: it safeguards the configuration the
user is using *right now*, and makes it survive the three things that take it away — **game updates,
computer upgrades, and hardware failure.** Editing bindings is the visible feature; not losing them is the
one that matters.

That holds even though part of File Manager already exists as Elite-Intel's Binding Management tab. What
exists covers two of the four file domains; the value is in covering all four, and in the restore side
being trustworthy. It provides backup and restore of all four [managed file domains](overview.md#managed-file-domains) packaged together, and owns Edit History, a bounded per-file undo system distinct from the ZIP archive backups.

## Structure

Three sub-tabs: **Game Install Locations**, **Player Backups**, and **Edit History**.

## Player Backups

### Backup Scope Selection

A checklist: "Everything" (checked by default, forcing all four domain checkboxes checked and disabled) or individually toggled `.binds` / `StartPreset.#.start` / `DeviceMappings.xml` / `.buttonMap` files.

### Auto-Backup on App Launch

Triggered by Elite-Intel's own startup — deliberately, not tied to the game's own launch, so a backup isn't missed if the user launches the game first. Runs in the background without blocking startup.

**It covers every detected install, and says so** (2026-09-23). The device files are per install, so a backup
that silently covered one of three would be worth less than it appears. Startup reports what it protected —
*"BindForge has backed up all your &lt;install names&gt; input configuration files"* — which is also how a user
who never opens BindForge learns that their second install exists. See
[Offered, never forced](overview.md#offered-never-forced--settled-2026-09-23).

### Retention

Age-based: **keep backups for N days, default 30** — a [setting](#settings) since 2026-09-28, with a floor of
one day.

**Not the same retention as [Edit History's](#edit-history)**, which counts versions per file rather than
days. One folder pruned by date, one history pruned by depth; they are separate settings and separate
columns.

### Backup Status Values

| Status | Meaning |
|---|---|
| Full | All four domains present |
| Partial | Some domain missing, not by user choice (e.g., no device alias existed yet at backup time, or a domain could not be read) |
| Custom | Scope deliberately reduced via the checklist — distinct from Partial so a deliberate choice never reads as a failure |

### Backup Now

Creates a backup immediately, using the current scope selection.

### Restore

**Restore writes into the draft. Always. There is no restore-to-live.** *Settled 2026-09-06.*

A restored backup lands in each domain's draft, where the player can look at it, edit it further, or throw
it away. If they want it live, they press **Apply** — the same button, the same pipeline, the same
confirmation as any other change they made by hand.

**This removes a shortcut, not a capability.** Restore-to-live was already implemented as restore-to-draft
followed by the ordinary apply pipeline — the two options shared one mechanism and differed only in whether
they stopped. Dropping the second costs one extra click in the disaster case and buys the property that
matters: **exactly one thing in BindForge writes to a live game file, and it is Apply.** A second door
round the side is how that guarantee stops being true.

It also makes partial restore safe (below). Writing a mixture of old and current state straight into a game
installation, unseen, is the most dangerous operation the feature could offer; into a draft it is
reviewable.

Restoring over a draft that holds unapplied edits destroys them, so it keeps its confirmation dialog.

### Restore Scope

The backup's own contents still bound what is possible, but the player chooses within them.

| Domain | Granularity | Why |
|---|---|---|
| `.binds` | **per file** | The only domain where the choice means anything. A player keeps several presets, and *"restore my Ship preset from June, leave the rest"* is a real thing to want. |
| `StartPreset.#.start` | whole domain | One file. Nothing to choose between. |
| `DeviceMappings.xml` | whole domain, **restored to every installation** | *Reworked 2026-09-26.* This row used to read "per installation", with a selector asking *which installation*. [Every install now holds the same files](overview.md#every-install-gets-the-same-files--settled-2026-09-23), so restoring into one and not the others would **create** the divergence BindForge exists to remove. |
| `.buttonMap` | whole domain, **restored to every installation** | Travels with `DeviceMappings.xml`. Offering these individually is a trap rather than a feature — see below. |

So file-level selection applies to exactly one domain. That is not a compromise; it falls out of the shape
of the domains.

**Files in a backup are not independent of each other, and partial restore is where that bites.**

- A `StartPreset.#.start` names a `.binds` file. Restore the preset without the file it points at — or the
  file without the preset — and the set is internally inconsistent.
- A `.buttonMap` is named for its device's element in `DeviceMappings.xml` (see
  [Device Provenance](../../03-data-models/device-provenance.md)). Restored without that element it is an
  orphan; restored alongside a *renamed* element it no longer matches.

**BindForge validates the resulting set and says so before Apply, rather than blocking the selection.** The
hazard exists at whole-domain granularity too, so the validation is owed either way; and because the result
lands in a draft, an inconsistent set is a thing to be shown, not a thing to be prevented.

### Restore never asks *which installation* — reworked 2026-09-26

**It used to, for the device domains.** `.binds` and `StartPreset.#.start` live in one shared folder, so they
never had an installation to choose; `DeviceMappings.xml` and `.buttonMap` are duplicated per installation, so
restoring them asked which one.

**That question is gone.** Under
[standardisation](overview.md#every-install-gets-the-same-files--settled-2026-09-23) every installation holds
the same device files, so there is no correct answer to *which one* — restoring into a single installation
manufactures exactly the drift the model exists to remove. A restore lands in the draft, and Apply carries it
to every installation, the same path an edit takes.

**The asymmetry did not disappear, it moved.** It is no longer about where a restore *goes*; it is about what
the archive *holds*. A backup captures each installation's device files **as they were found**, which means an
archive taken before standardisation, or after a patch wiped one installation, can contain several versions of
the same file.

**When those copies disagree, the restore asks the reconciliation question — not the routing one.** Which
captured version becomes the master? That is the same question
[first setup](alias-designer.md#first-setup--reconciling-the-installs--settled-2026-09-23) asks, with the same
answer shape, and once answered the result goes everywhere. When the captured copies all agree — the normal
case once a machine is standardised — there is nothing to ask.

### Installation identity — the storefront, and it is derived rather than assigned

**Settled 2026-09-07.** A backup stamps each per-installation file with the **storefront** it came from —
`steam`, `epic`, `frontier`. Nothing else, and nothing generated.

**~~This works because a machine can hold only one copy per storefront.~~ That premise was wrong, corrected
2026-09-26.** It reasoned that a Steam or Epic account binds to one Frontier account holding one copy, so the
storefront name is unique on any machine. Two pieces of evidence since say otherwise:

- **Windows.** A user can keep a second Frontier-launcher install — Alan listed exactly that among the
  possibilities — and the Frontier launcher can be run as
  [several copies, one per account](../../01-host-integration/domain-knowledge/EliteDangerous-InstallPaths.md).
- **Linux.** Native, Flatpak and Snap Steam are independent of one another and can all be installed at once,
  each with its own login and its own game copy. Krondor, 2026-09-26: *"I had all three Steams installed."*
  All three are storefront `steam`.

So **the storefront is a label, not a key.** Where a machine holds more than one installation of the same
storefront, the label alone cannot tell them apart, and its folder path is what distinguishes them.

**What it is still for.** The label records **where a captured file came from**, so an archive can be read
by a human and so a multi-installation capture is self-describing. What it no longer does is **route a
restore** — nothing needs to match a backup's installation to an installation on this machine, because the
device domains now go to all of them.

That makes the correction above cheap. A duplicate storefront label was only ever a problem for routing, and
routing is gone; two installations both labelled `steam` in an archive are now just two captured copies, which
the reconciliation question above already knows how to handle.

**The property that matters is that it is derived, not assigned.** Both machines independently arrive at
the same label from their own detection, rather than one minting an identifier the other has to trust. So
it survives everything that would defeat a generated ID:

| Event | Why the identity holds |
|---|---|
| **Relocate** | the installation row keeps its storefront; only the path changed |
| **Database loss** | detection re-derives it on the next run |
| **A different machine entirely** | `steam` means the same thing there, with nothing carried across |

So there is **no UUID, and no marker file written into the game folder.** A marker file was considered as
redundancy against database loss and rejected: it writes into an installation BindForge otherwise treats as
somebody else's property, and a game update can erase it — which is the exact incident that motivated this
project, so it fails precisely when it would be needed.

**The one gap: a manually added installation has no detected storefront.** It degrades into the case below
rather than breaking — an unrecognised label simply finds no match, and the player is asked. Whether such
an installation should carry a user-supplied label is undecided and cheap to defer.

### Restoring onto a machine whose installations differ — reworked 2026-09-26

**~~Settled 2026-09-07: if a backup's storefront matches no installation, ask which installation to restore
into.~~** That check existed to route a restore, and
[routing is gone](#restore-never-asks-which-installation--reworked-2026-09-26): the device domains go to every
installation, so nothing in the archive has to be matched to anything on this machine.

**The case it was written for still happens, and is now simpler.** A backup made on a PC with Steam and Epic,
restored onto one with only Steam, holds two captured copies of `DeviceMappings.xml` and the machine has one
installation to fill. If the captured copies agree, that is the master and it goes in. If they disagree — the
likely case for an archive predating standardisation — the
[reconciliation question](#restore-never-asks-which-installation--reworked-2026-09-26) picks which one wins,
then it goes in. Either way the count of installations either side is irrelevant.

**A new PC is the normal case for a portable single-file backup, not an error** — that is most of what the
format is for. It does not skip the device domains silently and it does not refuse.

**One check survives, and it is the one that was always doing the real work:**

| Check | Question | Failure |
|---|---|---|
| Hardware match | *do these device entries describe hardware this user has?* | refuse the device domains as not compatible — see [Browse for Backup File](#browse-for-backup-file) |

**Why that one is different in kind.** The storefront check asked a question about *this machine's layout*,
which standardisation answered permanently. The hardware check asks whether the archive describes **somebody
else's controllers**, which no amount of model-tidying answers — a user's own backup from their old PC passes
it, another player's backup may not. Keeping them separate was right; what changed is that only one of them
was ever about safety.

### Archive Format — ZIP

**Settled 2026-09-06.** One archive file per backup, not a folder tree.

Chosen for portability rather than compression: a single file can be copied, kept, or handed to someone
else when a user's bindings break and they need to send what they had. RAR was considered and
rejected for a dull reason — Java ships `java.util.zip`, while RAR needs a third-party library and is
proprietary.

**The layout inside the archive has to support extracting one domain, one file, or one installation without
unpacking the whole thing** — that is what Restore Scope above depends on, and it is a structure decision
rather than a format one.

**This diverges from what exists.** `PlayerBackupService` writes timestamped *folders* under
`playerbackups`, uncompressed, and covers only `.binds` and `StartPreset.*.start` — neither
`DeviceMappings.xml` nor `.buttonMap`, because it had no list of installations to walk. Migrating or
reading existing folder-format backups is an open item.

### Browse for Backup File

Restores from an archive that is **not** in the list — one copied off another machine, pulled out of a
cloud folder, or sent by another player. Same scope selection and same draft-only destination as any
other restore.

*Recorded 2026-09-06:* this button appeared in the mockup with no specification behind it. It was named
once, in a punchlist, as part of the justification for cutting Export/Import — *"`.binds` file sharing and
'Browse for Backup File' already cover what's actually needed"* — and never designed. The single-file
archive format is what makes it worth having.

#### Three use cases, and the third is refused

**Settled 2026-09-06.**

| # | Case | Behaviour |
|---|---|---|
| 1 | **The player's own backup, from elsewhere** — an old machine, a cloud folder, before a reinstall | Restore everything. Same hardware, same VID/PIDs, all four domains apply. |
| 2 | **Another player's backup — the `.binds` half** | Restore it. This is the `.binds` file sharing the punchlist meant, and it is genuinely useful: a bindings layout is worth copying. |
| 3 | **Another player's backup — `DeviceMappings.xml` and `.buttonMap`** | **Refused.** BindForge says the device data is not compatible and restores the rest. |

**Why the third is refused rather than merely warned about.** Those files describe *their* hardware.
Importing them writes device entries for controllers the player does not own — and nothing objects,
because `DeviceMappings.xml` tolerates entries for absent hardware perfectly well. The damage is quiet:
the device list fills with hardware that was never attached, `.buttonMap` files arrive naming buttons on
controllers that are not there, and the player has no way to tell which entries were theirs. It breaks
their setup while appearing to succeed.

This is the same reasoning that cut Export/Import on 2026-07-12 — *"an imported alias would still need
manual re-association with the recipient's hardware"* — which was never carried across to this feature
even though it applies identically.

**Later, not now: a reconciling importer.** Sharing device configuration is a reasonable thing to want,
and the honest version of it maps the donor's entries onto the recipient's actual hardware rather than
copying them wholesale — *"their Throttle entry, onto your throttle."* That is a feature in its own right,
not a flag on this one. Until it exists, refusing is the correct answer, because a refusal a player
understands beats an import they cannot unpick.

#### How BindForge tells case 1 from case 3 — settled 2026-09-12

Harder than it first appears, and an earlier note in this document got it wrong by suggesting the source
installation IDs would settle it.

**"Came from a different machine" is not the test**, because that is exactly what case 1 *is* — the
player's own backup from their old PC. Machine identity separates local from foreign; it does not
separate the player's hardware from someone else's.

The real question is **"do these device entries describe hardware this player has?"** Which suggests
comparing the archive's device entries against what BindForge already knows locally — the entries in the
local `DeviceMappings.xml` and the rows in
[device provenance](../../03-data-models/device-provenance.md), rather than against what happens to be
plugged in at that moment, since a player may restore before reattaching a controller.

**It never decides. It lists the device entries and the user ticks.** The archive is a bag of
device entries, not one indivisible thing — which is how `DeviceMappings.xml` already works — so
"case 1 or case 3" was the wrong shape of question. **A single archive can be both at once.**

```
Restore which device entries?
  [x] RVWAP         matches attached hardware
  [x] LVWAP         matches attached hardware
  [ ] X56 Throttle  not seen on this machine
```

**BindForge's assessment sets the tick; the user sets the outcome.** Entries matching hardware
it can see are pre-ticked, entries it cannot place are not. That keeps the common cases to a glance —
a straight own-backup restore is all ticked, another player's archive is all clear — while the
partial case, the one that started this question, needs exactly one correction.

**Why the user can answer this when BindForge cannot.** They know whether they still own the
X56. BindForge knows only that no attached device matches it, which is equally consistent with the
stick being sold, unplugged, or on a desk in another room. This is the same conclusion reached for
[device identity in Alias Designer](alias-designer.md#device-identity-is-the-users-to-confirm--settled-2026-09-12),
and for the same reason: there is no reliable hardware identity to compute from.

**The refusal in case 3 stands, and this is how it is enforced** — not by classifying the archive, but
by leaving foreign entries unticked by default, so importing someone else's hardware takes a
deliberate act rather than an unnoticed one. **Matching is compared against what BindForge knows
locally** — the local `DeviceMappings.xml` plus the
[provenance](../../03-data-models/device-provenance.md) rows — **not against what is plugged in right
now**, since a user may restore before reattaching a controller.

**`.binds` is unaffected by any of this.** It is restored per case 2 regardless of which device
entries are ticked; a bindings layout is worth copying even when none of the hardware is yours.

## Installation Mirroring — moved, not removed

This screen used to list every detected installation with a Mirror checkbox and an "Apply to Selected Game
Installations" button. **It is gone**, and the capability moved to
[Alias Designer](alias-designer.md#one-record-and-where-it-lands--reworked-2026-09-26).

~~The reason is that mirroring is a property *of a device*, not of a screen.~~ **Superseded 2026-09-23:**
mirroring is neither a screen nor a per-device property — it is the model. Every install holds the same device
files, so there is nothing to choose on any screen. See
[Every install gets the same files](overview.md#every-install-gets-the-same-files--settled-2026-09-23).

Installation *management* — adding, relocating or removing an installation when detection gets it wrong —
**now has a home: [Game Install Locations](#game-install-locations), below.** Alias Designer continues to
show each installation's path read-only, which is context rather than configuration; the editable list
lives here, in the same screen that needs it for Restore.

## Game Install Locations

**Added 2026-09-06.** The first of File Manager's three sub-tabs, ahead of Player Backups and Edit History.
It answers one question: *where does BindForge look for the files it manages?* Backup, Restore and Apply
all work from this list, so when detection is wrong every one of them is wrong.

### The screen states the domain split, because that is what people get wrong

Two sections, not one list, matching the split in
[Managed File Domains](overview.md#managed-file-domains):

**User configuration folder — one, shared by every installation.** Holds the `.binds` files and
`StartPreset.#.start`. It exists once no matter how many storefronts the player owns, and it is the folder
that decides what the controls actually do. Shown as a single read-only path with a found/missing state.

**Game installations — one per storefront.** Each holds its own `DeviceMappings.xml` and `.buttonMap` files,
which may legitimately differ between installations. Shown as a table: storefront, location, whether it was
auto-detected or added by hand, what device files were found there, and a status.

**An installation folder can hold more than one product, and only Odyssey is in scope.** Under
`Products\` a Steam installation may carry both `elite-dangerous-64` (Horizons) and
`elite-dangerous-odyssey-64`, **each with its own `ControlSchemes` folder**, and they genuinely differ —
observed 2026-09-07, Horizons ships 49 device entries against Odyssey's 51 and has no `DeviceButtonMaps`
folder at all. **BindForge targets `elite-dangerous-odyssey-64` only** (Krondor's decision; Elite-Intel is
an Odyssey application), so the product is a constant rather than part of the identity. Recorded because
the folder is visibly there, and because it is why the shipped reference file is Odyssey's specifically.

Presenting these as one flat list of "locations" would imply the four domains live in comparable places. They
do not: one folder is shared, the other is one per storefront, and that difference is what the two sections
show. ~~They do not carry comparable risk: losing the first is a gameplay problem, losing the second is
cosmetic.~~ *Corrected 2026-09-21:* the two sections differ in **where** the files live, not in **how much they
matter** — every managed file is protected alike, per
[Protection is uniform](overview.md#protection-is-uniform--settled-2026-09-21).

### Actions

| Action | Scope | Notes |
|---|---|---|
| **Rescan** | all | Re-runs storefront detection. Existing rows keep their device records; a row whose path has disappeared is marked missing rather than deleted. |
| **Add installation** | one | Folder chooser. Validated before acceptance — an arbitrary folder is not an installation. |
| **Relocate** | one row | Repoints an existing row at a new path, **carrying its device records with it**. The reinstall-to-another-drive case. |
| **Remove** | one row | Drops the row from the list. |

### Scope: a moved installation is the player's to fix, with the controls above

**Settled 2026-09-06.** Players install the game and leave it where it lands. Moving an installation is
rare, and a player who uninstalls and reinstalls elsewhere can put BindForge straight with **Relocate**, or
with **Remove** and **Add installation**. BindForge is not going to model that journey any further than
providing those three controls.

This is a deliberate limit, not an oversight. The obligation BindForge takes on is **finding the
installations and getting a reliable backup** — that is what protects the thing a player cannot recreate.
What happens afterwards because they moved the game is recoverable by hand in under a minute, and building
machinery to anticipate it would buy very little at the cost of complicating the part that actually
matters.

Consequences of that limit, so they are chosen rather than discovered:

- **Remove discards the records held against that installation.** No orphan-retention scheme, no undo
  beyond re-adding and letting import run again. Device records are cheap to rebuild; backups are not, and
  backups are not held here.
- **A missing installation is never auto-removed.** A row whose folder has gone stays in the list marked
  missing, because an unmounted drive and an uninstall look identical from here, and silently dropping a
  row would take its device records with it.
- **Relocate is still worth having** — it is one control, it keeps the device records, and it turns the
  most common version of this into a single click. It is the cheap half of the problem, and the only half
  being solved.

### Open

- **Detection itself is not built.** `AppPaths` resolves Elite-Intel's own folders only and has no notion
  of where Elite Dangerous is installed, let alone per storefront. This screen specifies what detection has
  to produce; it does not provide it. **Windows storefront detection is BindForge's to build; Linux/Proton
  resolution is Krondor's** — see below.

## What a `.binds` File Might Not Be

**Added 2026-09-17, measured rather than imagined.** Across 48,354 player-shared files, **445 carry a
`.binds` extension and are not bindings files** — 363 of them a `StartPreset.start` picked by mistake from the
same folder, the rest other games' configs, device profiles, truncated files and stray XML. Full breakdown in
[Bind Editor](bind-editor.md#a-binds-name-does-not-make-it-a-binds-file--2026-09-17).

**What that asks of this screen:** the bindings-file dropdown lists what is *in* the folder, so it will list
those files too. Selecting one has to produce a sentence naming what the file appears to be, never a stack
trace and never an empty editor — and **no restore, backup or apply path may overwrite a file BindForge
could not identify.**

## Edit History

**Confirmed in V1.2 (2026-09-09).** The reason it earns its place rather than being deferred: **it removes
the need to take a full backup before every small experiment.** Without it, a user who wants to try
one change either backs up the whole configuration first or risks not getting back. Neither is a fair
price for adjusting one binding, and the second is how people end up with a layout they cannot recover.

A bounded, per-file undo system — distinct in purpose from Player Backups. Where a Player Backup is a manually- or launch-triggered snapshot of *everything at once*, Edit History is automatic, per-file, and granular: it lets the user step back through recent versions of one specific file, one edit at a time.

**How it's populated:** every time a write actually lands on a live game file — which since 2026-09-08 means an **Apply**, from any section, since Apply is the only thing in BindForge that writes one — the version being replaced is moved into that file's own History folder before the new version is written, as part of the same [Data Integrity Principle](overview.md#data-integrity-principle) write sequence already used everywhere else (nothing new is required at the write-mechanics level; this changes what happens to the pre-write backup, not how safely it's taken).

**Retention:** count-based, not age-based (unlike Player Backups) — configurable per the BindForge Settings tab (see [Settings](#settings) below), 1–30 entries, default 10, per file. Once the limit is reached, the oldest entry is dropped as a new one is added.

**Storage: the files alone, no database table — settled 2026-10-03 (Alan).** This section described a History
folder holding the replaced versions, while [What exists in code today](#what-exists-in-code-today--checked-2026-09-18)
said Edit History needed a migration. Those are two designs, and only one is needed. **The folder is the
record**: each kept version is a file whose name carries its timestamp, so the folder listing *is* the
history, sorted. Counting to ten and dropping the oldest needs no table.

A table would have to be kept in step with the folder, and the two could then disagree — a version listed but
not on disk, or on disk and invisible. Since the folder is what a restore actually reads from, the table could
only ever be a second opinion about it. The entries are also throwaway by design: capped at thirty, replaced
constantly, and worth nothing once restored from. That is not what a migration — which can never be edited
afterwards — is for.

**Where the folder is: under Elite-Intel's own data folder — settled 2026-10-03 (Alan).**
`%LOCALAPPDATA%\elite-intel` on Windows, and whatever `AppPaths.getAppDataBase()` resolves to elsewhere —
the same root already holding the database, the working copies and the player backups.

**The path, for device files — built 2026-10-04:** `elite-intel\bindings\history\<installation id>\`, with
`.buttonMap` copies in a `DeviceButtonMaps\` folder beneath it. Keyed by installation id rather than storefront,
because two installations' files share a name and the id survives a relocate. Each copy is named
`<file>.<yyyyMMdd-HHmmss>.bak` by `elite.intel.io.TimestampedBackups`, so the listing sorts as the history.
`AppPaths.getBindingsWorkingDir()` supplies the root, which kept `AppPaths` — shared with V1.1 — unchanged.

*This replaces "that file's own History folder", which read as **beside the file**. For `DeviceMappings.xml`
and `.buttonMap` that would be inside the game installation, which the patcher replaces wholesale on every
update — so the history would be deleted by precisely the event a user most wants to step back from. It is
also the event this whole feature exists because of.*

**What gets kept: whatever is unique — settled 2026-10-03 (Alan).** Before overwriting a file, its current
content is kept; a file whose content is identical to one already kept during the same operation is not kept
again.

*This replaces "an Edit History entry per file written, so a single install's `DeviceMappings.xml` or one
`.buttonMap` can be walked back on its own". The goal was never one-entry-per-install, it was losing nothing
unique — and per-install was how that was reached when installations were still allowed to differ.
[Standardisation](overview.md#every-install-gets-the-same-files--settled-2026-09-23) ended that, so an entry
per installation would now be several byte-identical copies of one file.*

The rule falls out the same way without a special case for first setup:

| When | What differs | What is kept |
|---|---|---|
| [First setup](alias-designer.md#first-setup--reconciling-the-installs--settled-2026-09-23) | the installations disagree — that is why it is running | one entry per installation, which is the state worth returning to |
| Every later Apply | nothing; they all hold the master's last output | one entry |
| An installation edited outside BindForge, or wiped by a patch | that one installation | its unique content, before it is overwritten |

**Note what is *not* kept for device identity: the master.** For `.binds` and `StartPreset` the master is a
whole file and can be copied. For device identity [the master is the user's element set](alias-designer.md#what-the-master-actually-is)
— rows, not a file — so there is nothing to copy, and the file being overwritten is the only file in the
transaction. That is why the rule is phrased around what is written rather than around the master.

**Browsing and restoring:** the Edit History sub-tab lists entries per managed file domain, each with a timestamp. Selecting an entry shows what it contains and offers a Restore action, which goes through the same confirmation-and-Controlled-Replace path as a Player Backup restore (see [Restore](#restore) above) — the current live version is itself backed up before the historical version is written over it, so restoring never destroys the ability to undo the restore itself.

**Scope:** applies to all four managed file domains uniformly — Bind, Device Identity, Button Map, and Active Preset — since every one of them goes through a write path that can now feed Edit History.

## Settings

BindForge's settings tab (see [Settings Storage](../../01-host-integration/elite-intel-platform-map.md#settings-storage) for how feature settings panels work in Elite-Intel) contains:

| Setting | Default | Description |
|---|---|---|
| Auto-backup on Elite-Intel launch | Enabled | Toggles [Player Backups — Auto-Backup on App Launch](#auto-backup-on-app-launch) |
| Backup destination | Elite-Intel's default backup path | Where Player Backup ZIP archives are written |
| Backup retention | 30 days (minimum 1) | How long a [Player Backup](#retention) is kept before it is pruned |
| Edit History retention | 10 (range 1–30) | How many historical versions [Edit History](#edit-history) keeps per file before dropping the oldest |

**Where they are stored — built 2026-09-28:** a `bindforge_settings` table of their own, one column each;
retention is range-checked (1–30) and the destination is rejected at the setter if the platform cannot
represent it. **Resolving an unset destination to a real folder is not done in the settings manager** —
`PlayerBackupService.resolvePlayerBackupsDir()` reads the stored value and is the only place that decision is
made. A destination that cannot be read falls back to the default rather than failing the backup: the backup
is the thing being protected. See [Settings Storage](../../01-host-integration/elite-intel-platform-map.md#a-bindforge_settings-table--built-2026-09-28).
**Settled 2026-09-28: the age limit is a fourth setting**, defaulting to 30 days (Alan). It has a floor of
one day and no ceiling — zero would make every backup older than the limit the moment it was written, so
pruning would delete the backup it had just taken, while keeping backups longer only costs disk, which is the
user's to spend. One open point from that proposal still belongs here: that *"BindForge's settings tab"* means a panel on Elite-Intel's Settings
screen — not the Bind Editor's [Settings mode](bind-editor.md#settings--settled-2026-09-19), which edits the
game's own settings inside the `.binds` file.

## What exists in code today — checked 2026-09-18

`PlayerBackupService` and `BindingManagementPanel` already carry the backup half of this screen. What is
here, and what is not:

| Piece | State |
|---|---|
| Create a backup | **exists** — `createBackup()`, into a timestamped folder with real filenames ([format divergence](#archive-format--zip)) |
| List backups | **exists** — `listBackups()`, newest first, as `PlayerBackup(folder, timestamp, fileNames)` |
| Delete a backup | **exists**, wired to a button |
| Restore to the editing slot | **exists** — `restoreToWorkingCopy(folder, presetFileName)` |
| Restore to live | **exists** — `restoreToLive(folder, presetFileName, gameBindsFile)`, **one file at a time**, which is what [Restore Scope](#restore-scope) widens |
| [Auto-backup on launch](#auto-backup-on-app-launch) | **does not exist.** `createBackup()` has exactly one caller: the Backup Now button |
| [Retention](#retention) | **does not exist.** Nothing prunes anything; backups accumulate until deleted by hand |
| [Backup status values](#backup-status-values) | **not reachable yet** — Full / Partial / Custom needs the installation list that [detection](#open) would supply |
| [Game Install Locations](#game-install-locations) | **not built** — see the section's own [Open](#open) note |
| [Edit History](#edit-history) | **writing built for device files** (2026-10-04, `bindforge.devicefiles.DeviceFilesPush` — see [the path](#edit-history)); `.binds` and `StartPreset` not yet, and no browsing or restore |

**Auto-backup and retention are one change, not two.** Today nothing prunes, and that is harmless because
every backup is a deliberate button press. Add the launch trigger on its own and the folder grows every
time Elite-Intel starts, on a machine where the user never asked for a single backup. Whichever lands
first, the other has to land with it.

**~~Edit History needs a migration in BindForge's range.~~ Superseded 2026-10-03** — it needs no table at
all. The kept versions are files whose names carry their timestamps, and the folder listing is the history;
see [Storage](#edit-history). The band rule that paragraph described still stands and still applies to
everything else BindForge stores: **BindForge owns `12000–12499`**, commander and galaxy work `12500–12999`, per Krondor's <!-- terminology-ok: names the real db-migration/commander/ tree and Krondor's per-commander work -->
[proposal §4](../../multi-install-proposal.md#4-migrations-one-tree-per-file), accepted 2026-09-24, in the
shared top-level `db-migration/` tree and never `db-migration/commander/`, which is Krondor's alone. <!-- terminology-ok: names the real db-migration/commander/ tree and Krondor's per-commander work -->
The range held one file when this was written and now holds three — `12000__bindforge_settings.sql`,
`12001__bindforge_installations.sql` and `12002__bindforge_devices.sql`. [An applied migration is never edited](../../../CLAUDE.md).

## Export / Import — Considered, Then Cut

A dedicated Export/Import feature for device configuration was considered and explicitly cut. Its main justified use case — remapping an alias configuration for a recipient's different hardware — doesn't hold up, because VID/PID is read-only and hardware-derived by design; an imported alias would still require the same manual re-association that Alias Designer's normal setup already requires. "Browse for backup file" already supports restoring a backup onto another machine, which is what a genuine transfer between a player's own machines actually needs. Nothing a dedicated Export/Import feature would add beyond that was judged worth its own feature.

## Community Sharing of `.binds` Presets — Not a Feature

Unlike StarVizion's Vizlets (see [StarVizion — HoloFrames and Persistence, Export and Import](../starvizion/holoframes-and-persistence.md#export-and-import)), BindForge does not have a community-sharing story for `.binds` presets, and this is a deliberate decision rather than a gap. A `.binds` file is tightly coupled to the exact physical hardware it was authored on — specific device VID/PIDs, exact axis/button counts, and often a specific keyboard-layout preference (e.g. ESDF instead of WASD). Handing a `.binds` file to another player is technically trivial (it's a plain file), but practically nearly useless unless the recipient happens to own an unusually similar setup — a mismatch made worse, not better, by anything BindForge could automate, since the device identifiers in the file are hardware-derived and cannot be usefully remapped without the recipient already owning matching hardware. File Manager's backup/restore is the only file-movement feature BindForge needs — it exists to protect one player's own configuration, not to distribute it.
