# BindForge — File Manager

**File Manager is BindForge's primary feature — the main reason BindForge exists.** Backing up the binds
and configuration files is the most important thing this tool does: it safeguards the configuration the
commander is using *right now*, and makes it survive the three things that take it away — **game updates,
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

### Retention

Age-based: "keep backups for N days," default 30.

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
| `DeviceMappings.xml` | whole domain, **per installation** | One file per installation — the meaningful choice is *which installation*, and that selector is needed regardless. |
| `.buttonMap` | whole domain, **per installation** | Travels with `DeviceMappings.xml`. Offering these individually is a trap rather than a feature — see below. |

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

### Restore is not evenly scoped across the four domains, and the UI must not pretend otherwise

`.binds` and `StartPreset.#.start` live in the single shared user-config folder. **However many `.binds`
files a player keeps — and they may keep as many as they like — that folder is not duplicated per
installation**, so restoring them never asks *which installation*. `DeviceMappings.xml` and `.buttonMap`
are duplicated per installation, so restoring those always does.

### Installation identity — the storefront, and it is derived rather than assigned

**Settled 2026-09-07.** A backup stamps each per-installation file with the **storefront** it came from —
`steam`, `epic`, `frontier`. Nothing else, and nothing generated.

This works because **a machine can hold only one copy per storefront.** A Steam or Epic account binds to a
single Frontier account holding a single copy of the game, so the storefront name is already unique on any
given machine — there is nothing to disambiguate.

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

### Restoring when nothing matches — ask, do not guess

**Settled 2026-09-07.** If a backup's storefront matches no installation on this machine, BindForge **asks
the player which installation to restore into**, showing what the archive says about where it came from.

It does not skip the device domains silently, and it does not refuse. **A new PC is the normal case for a
portable single-file backup, not an error** — that is most of what the format is for. The `.binds` half
restores regardless, since it goes to one shared folder that has no installation to match.

**This is a separate check from the compatibility one**, and they can both fire on the same archive:

| Check | Question | Failure |
|---|---|---|
| Storefront match | *which installation does this go into?* | ask the player |
| Hardware match | *do these device entries describe hardware this player has?* | refuse the device domains as not compatible — see [Browse for Backup File](#browse-for-backup-file) |

Worth keeping distinct: a commander's own backup from their old PC fails the first check and passes the
second. Another commander's backup can pass the first and fail the second. Conflating them would either
refuse a legitimate restore or wave through someone else's hardware.

### Archive Format — ZIP

**Settled 2026-09-06.** One archive file per backup, not a folder tree.

Chosen for portability rather than compression: a single file can be copied, kept, or handed to someone
else when a commander's bindings break and they need to send what they had. RAR was considered and
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
cloud folder, or sent by another commander. Same scope selection and same draft-only destination as any
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
| 2 | **Another commander's backup — the `.binds` half** | Restore it. This is the `.binds` file sharing the punchlist meant, and it is genuinely useful: a bindings layout is worth copying. |
| 3 | **Another commander's backup — `DeviceMappings.xml` and `.buttonMap`** | **Refused.** BindForge says the device data is not compatible and restores the rest. |

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

#### Open: how BindForge tells case 1 from case 3

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

Unresolved: how much overlap counts as "theirs". A player who upgraded one stick between the backup and
the restore has a partial match, and that is case 1 with a wrinkle rather than case 3.

## Installation Mirroring — moved, not removed

This screen used to list every detected installation with a Mirror checkbox and an "Apply to Selected Game
Installations" button. **It is gone**, and the capability moved to
[Alias Designer](alias-designer.md#per-installation-editing).

The reason is that mirroring is a property *of a device*, not of a screen. A checkbox list forced one decision
across every device at once; the per-device installation tabs let one controller be shared across installations
while another stays local to one. That was impossible to express here.

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

Presenting these as one flat list of "locations" would imply the four domains live in comparable places and
carry comparable risk. They do not: losing the first is a gameplay problem, losing the second is cosmetic.

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

## Edit History

**Confirmed in V1.2 (2026-09-09).** The reason it earns its place rather than being deferred: **it removes
the need to take a full backup before every small experiment.** Without it, a commander who wants to try
one change either backs up the whole configuration first or risks not getting back. Neither is a fair
price for adjusting one binding, and the second is how people end up with a layout they cannot recover.

A bounded, per-file undo system — distinct in purpose from Player Backups. Where a Player Backup is a manually- or launch-triggered snapshot of *everything at once*, Edit History is automatic, per-file, and granular: it lets the user step back through recent versions of one specific file, one edit at a time.

**How it's populated:** every time a write actually lands on a live game file — which since 2026-09-08 means an **Apply**, from any section, since Apply is the only thing in BindForge that writes one — the version being replaced is moved into that file's own History folder before the new version is written, as part of the same [Data Integrity Principle](overview.md#data-integrity-principle) write sequence already used everywhere else (nothing new is required at the write-mechanics level; this changes what happens to the pre-write backup, not how safely it's taken).

**Retention:** count-based, not age-based (unlike Player Backups) — configurable per the BindForge Settings tab (see [Settings](#settings) below), 1–30 entries, default 10, per file. Once the limit is reached, the oldest entry is dropped as a new one is added.

**Browsing and restoring:** the Edit History sub-tab lists entries per managed file domain, each with a timestamp. Selecting an entry shows what it contains and offers a Restore action, which goes through the same confirmation-and-Controlled-Replace path as a Player Backup restore (see [Restore](#restore) above) — the current live version is itself backed up before the historical version is written over it, so restoring never destroys the ability to undo the restore itself.

**Scope:** applies to all four managed file domains uniformly — Bind, Device Identity, Button Map, and Active Preset — since every one of them goes through a write path that can now feed Edit History.

## Settings

BindForge's settings tab (see [Settings Storage](../../01-host-integration/elite-intel-platform-map.md#settings-storage) for how feature settings panels work in Elite-Intel) contains:

| Setting | Default | Description |
|---|---|---|
| Auto-backup on Elite-Intel launch | Enabled | Toggles [Player Backups — Auto-Backup on App Launch](#auto-backup-on-app-launch) |
| Backup destination | Elite-Intel's default backup path | Where Player Backup ZIP archives are written |
| Edit History retention | 10 (range 1–30) | How many historical versions [Edit History](#edit-history) keeps per file before dropping the oldest |

## Export / Import — Considered, Then Cut

A dedicated Export/Import feature for device configuration was considered and explicitly cut. Its main justified use case — remapping an alias configuration for a recipient's different hardware — doesn't hold up, because VID/PID is read-only and hardware-derived by design; an imported alias would still require the same manual re-association that Alias Designer's normal setup already requires. "Browse for backup file" already supports restoring a backup onto another machine, which is what a genuine transfer between a player's own machines actually needs. Nothing a dedicated Export/Import feature would add beyond that was judged worth its own feature.

## Community Sharing of `.binds` Presets — Not a Feature

Unlike StarVizion's Vizlets (see [StarVizion — HoloFrames and Persistence, Export and Import](../starvizion/holoframes-and-persistence.md#export-and-import)), BindForge does not have a community-sharing story for `.binds` presets, and this is a deliberate decision rather than a gap. A `.binds` file is tightly coupled to the exact physical hardware it was authored on — specific device VID/PIDs, exact axis/button counts, and often a specific keyboard-layout preference (e.g. ESDF instead of WASD). Handing a `.binds` file to another player is technically trivial (it's a plain file), but practically nearly useless unless the recipient happens to own an unusually similar setup — a mismatch made worse, not better, by anything BindForge could automate, since the device identifiers in the file are hardware-derived and cannot be usefully remapped without the recipient already owning matching hardware. File Manager's backup/restore is the only file-movement feature BindForge needs — it exists to protect one player's own configuration, not to distribute it.
