# BindForge — File Manager

**File Manager is BindForge's primary feature — the main reason BindForge exists.** Backing up the game input
configuration files safeguards the configuration the user is using *right now*, and makes it survive the three
things that take it away — **game updates, computer upgrades, and hardware failure.** Editing bindings is the
visible feature; not losing them is the one that matters.

How backups are taken, kept and restored is [storage.md](storage.md) (rules 10–12). There is one backup system: a
backup is a date-stamped zip of the live copies, of one of four kinds, and there is no history kept per file.

## Structure

Two sub-tabs: **Game Install Locations** and **Player Backups**. *Edit History, a third, was folded into Player
Backups on 2026-10-22.*

## Player Backups

### Four kinds of backup — settled 2026-10-24 (Alan)

| Kind | Taken | Kept | Folder |
|---|---|---|---|
| **Daily** | the first Elite-Intel start each day, silently | by age — 30, 60, 90 or 180 days, 30 by default | `Daily\` |
| **Before changes** | before every write: APPLY; RESTORE, of what is there now; the check, of the working copy before a live copy replaces it | by count — the newest 100 by default, 10 to 1000 | `BeforeChanges\` |
| **User** | Backup Now | until the user deletes them | `User\` |
| **First setup** | first-time startup, before the working copy is created | forever | `FirstSetup\` |

Every one is a zip holding the same files. **Why four** (Krondor's review, 2026-10-24): age alone deletes every good
backup when a user is away longer than the limit; a count alone loses last week's good copy to one evening of applies.
Daily backups cover the first, before-changes backups the second, and the two the user cares most about — their own and
the one from before BindForge wrote anything — are never deleted by either rule. Alan: *"i think that will probably
cover our butts better."*

### Daily backup — when Elite-Intel starts

The first time Elite-Intel starts each day, silently, in the background ([storage.md](storage.md) rule 12) — tied to
Elite-Intel's startup, not the game's, so a backup is not missed when the user launches the game first, and it never
holds startup up. It covers every installation. If it fails, the next start tries again, the same day included.
*Until 2026-10-23 startup said what it had backed up; it is silent now (Alan). Until Storage S1b (2026-10-24) one was taken at every
start.*

**Built 2026-10-24 (Storage S1), one at every start; once a day since S1b, later the same day:** `bindforge.storage.StartupBackup`,
started from `App.main` once the database is open — not with the services, which need credentials to start. It runs
installation detection first, so it covers every installation before first-time startup has ever run. **Retention
runs only after the backup succeeds:** when today's backup failed, the older ones are the newest there are. **Nothing
else BindForge does at startup writes to a game file until it is done** — `ButtonMapStartup` waits for it, and writes
nothing that run if it failed (principle 3; Alan, 2026-10-24).

**Whether today's is taken is read from `Daily\`** — a zip stamped with today's date, Partial included — not from a
table. A start that finds one takes none, counts as taken for `ButtonMapStartup`, and **still runs retention**: the
backup succeeded earlier that day, so the older ones are not the newest there are (2026-10-24, S1b).

### What a backup holds

Always every game input configuration file, from every installation ([principle 1](principles.md)) — the full set
from each installation, even when they match, with no comparison first (2026-10-24, Alan): the backup matters most
when installations differ, and code that decides what to back up can miss the file that mattered. *A checklist that
narrowed it was dropped 2026-10-24.*

| Status | Meaning |
|---|---|
| Full | every file from every installation |
| Partial | something could not be read — an installation on an unplugged drive, or a file that failed to open — and the backup says which |

### Retention

When Elite-Intel starts, after the daily backup ([storage.md](storage.md) rule 11): **daily** backups older than the
setting are deleted, and **before-changes** backups beyond the newest N. **User** and **first setup** backups are never
deleted by retention. Both limits are set in Player Backups' [retention settings](#settings).

**Delete is for user backups only** (2026-10-24, Alan): the user made them, so they decide when they go; it asks
first (`HudConfirmDialog`). Daily, before-changes and first setup backups have no Delete — retention looks after the
first two, and the third is the one from before BindForge wrote anything. *Removed 2026-10-24 with the folder format,
back 2026-10-24 for user backups.*

### Backup Now

Takes a **user** backup immediately — every file, as above — kept until the user deletes it.

### How the backup folder is laid out

```
<backup folder>\
  Daily\2026-10-25_09-12-40.zip
  BeforeChanges\2026-10-25_21-40-02.zip
  User\2026-10-25_21-45-10.zip
  FirstSetup\2026-10-13_20-11-08.zip

inside every zip:
  backup-info.txt                         the kind, what it was taken before (APPLY, RESTORE, the check),
                                          Full or Partial, each installation's storefront and folder,
                                          and what could not be read
  Bindings\                               every <name>.<major>.<minor>.binds and StartPreset.N.start
  Installations\1-STEAM\ControlSchemes\   DeviceMappings.xml and DeviceButtonMaps\*.buttonMap
  Installations\2-EPIC\ControlSchemes\
```

**One list, newest first**, with a *Taken* column — Daily, before APPLY, before RESTORE, before the check, User, First
setup — read from each zip's `backup-info.txt`; the date stamp in the name orders it, and file dates are never read. A
zip is written to a temporary file and renamed into place once complete, so a half-written backup never shows in the
list. On Linux, an installation whose own bindings folder is not the one Elite-Intel is set to has its `.binds` and
`StartPreset` under its own `Installations\<n>-<storefront>\Bindings\`. *Storage S1 built this as dated folders
directly in the backup folder; S1b made it zips in the four kind folders.*

**Built 2026-10-24 (Storage S1b; Alan approved):** the zip is built in memory — a full backup is a few megabytes at
most — and written with `elite.intel.io.AtomicFiles`, the one write path, so its temporary file is
`.<stamp>.zip.elite-intel-<id>.tmp` beside the target; retention's sweep removes one a crash left. Two backups of one
kind in one second are `<stamp>.zip` and `<stamp>-2.zip`. The list's columns are **Created, Taken, Status** — the
*Kept* column ("Forever") went, since *Taken* says First setup, and the mockup's *Files* column was not built.

**A backup with no file at all is not written.** If nothing could be read, the startup backup logs it and tries again
at the next start; Backup Now says so. **It is not a fault** — a new player, or a game not yet installed, has no files
yet — so it is logged as a warning, not an error (Alan, 2026-10-24).

**A new player is also told at every start, spoken** — the one exception to the silent backup ([storage.md](storage.md)
rule 12; Alan, 2026-10-24). It is Elite-Intel's existing setup check (`setup.SetupCheck`) that speaks, not the backup,
so the player hears one warning rather than two: when the bindings folder is the game's default and is missing or
holds no `StartPreset`, it says, in Alan's words (`speech.setup.noBindingsYet`): *"Commander, your control input <!-- terminology-ok: quotes the spoken line verbatim -->
configuration files have not been created yet. Complete your commander training, then in Options, Controls, set the <!-- terminology-ok: quotes the spoken line verbatim -->
galaxy map UI movement to the arrow keys and apply. Once you do, I can read your controls. Until then I cannot operate
your ship."* Until 2026-10-24 that case was told to set the bindings folder, which was already right. A folder the user
chose that is missing or empty is still told to fix the setting; the game's folder with a `StartPreset` and no `.binds`
is still told to change a binding in the game. **Backup Now's "Nothing to Back Up Yet" dialog and the startup log line
say the same words** (Alan, 2026-10-24).

### Old folder backups — V1.1's, and the old BindForge ones (settled 2026-10-24, Alan)

V1.1's Backup Now wrote `<backup folder>\<yyyy-MM-dd_HH-mm-ss>\` folders holding only `.binds` and `StartPreset`;
BindForge before Storage S1 wrote `…_before-bindforge` and `…_first-setup` folders; and Storage S1 itself, until S1b,
wrote dated folders holding a `backup-info.txt` (2026-10-24, Alan). **They stay where they are, untouched: not listed,
and never deleted by retention** — the list reads only zips in the four kind folders. The user can still open them in
their file manager. *Restore to Editing Slot and Restore to Live read the `.binds` from the zip's `Bindings\` until
RESTORE replaces them (S3). Old folders are no longer read since S1b — they are not listed, so none can be chosen.*

### Restore

**RESTORE puts the whole backup back: every `.binds`, `StartPreset.4.start`, `DeviceMappings.xml` and `.buttonMap`,
in every installation — no choices** (2026-10-24, Alan), so nothing can be mixed and matched by accident. A backup of
what is there now is taken first, and the working copy is replaced too ([storage.md](storage.md) rule 10). Its
confirmation says that everything will be replaced, and that any edits on screen are thrown away. *Until 2026-10-22 a
restore only landed in the draft and needed Apply; until 2026-10-24 the user could pick files.*

### Restore never asks *which installation* — reworked 2026-09-26

The device files go to every installation on this machine now, as one set — the backup's, or, **if the backup holds
different device files for different installations** (taken before first-time startup, or after a patch reset one),
the set the user reconciles exactly as in first-time startup ([storage.md](storage.md) rule 2). When they all match,
the normal case, nothing is asked (2026-10-24, Alan).

### Installation identity — the storefront, and it is derived rather than assigned

**Settled 2026-09-07; folder added 2026-10-24 (Alan).** A backup labels each installation's files with where they
came from — the **storefront** (`steam`, `epic`, `frontier`) and the **folder** — for a person reading the backup.
Nothing generated.

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

### Restoring onto a machine whose installations differ

**Device files are one set, so a restore needs only one** (2026-10-24, Alan): the backup's, when its installations all
held the same device files, or the reconciled set when they did not ([Restore](#restore)). That set goes to **every
installation on this machine now**, however many there are and whichever storefronts — so a backup from a PC with
Steam and Epic restores onto one with only Steam with nothing to ask. The `.binds` and `StartPreset.4.start` go to the
bindings folder. Whether the device entries describe *this user's* hardware is
[Browse for Backup File's](#browse-for-backup-file) check.

### Backup Format — every backup is a zip (2026-10-24, Alan)

**One zip per backup, of every kind**, so restore has one format to read, and a backup is one file to copy to a new PC
or send for support. Java ships `java.util.zip`; RAR was rejected for needing a third-party library.

*Settled 2026-09-06 as a zip; changed to a plain folder on 2026-10-24 to match V1.1 (Alan: "lets just use folder, that
stays consistent with version 1.1"); back to a zip later that day so every backup restores the same way.*

**Open for S3:** a zip made with `java.util.zip` does not keep Windows' read-only flag, which players set to stop the
game resetting a binds file. A restored file comes back writable unless RESTORE records and puts the flag back (Krondor's
review, 2026-10-24).

### Browse for Backup File

**Restore from a backup file that is not in the list** — one copied off the user's old PC, or out of a cloud folder.
It is [Restore](#restore) in every way: everything, no choices (2026-10-24, Alan). **It is for the user's own
backups**: its confirmation says *"Only restore your own backups. A backup from another player holds their
controllers, not yours."*

*Restoring part of another player's backup — their `.binds`, never their device files — is [deferred](#deferred).*

## Game Install Locations

**The one place BindForge shows installations** (2026-10-24, Alan; [principle 12](principles.md)) — the editors care
what is being edited; this sub-tab is where the files go. It lists:

- **the bindings folder** — its path, and whether it was found;
- **each installation** — storefront, folder, and **whether its live files match the working copy**: *matches*,
  *differs*, or *not found* — read from the same comparison as [the check](storage.md) (rule 14), which is what
  offers to fix a difference. This is the at-a-glance confirmation that every installation is in step, shown once
  per installation rather than on every device row;
- **Add missing** — browse to an installation's root folder, checked for `EliteDangerous64.exe` and the other files
  that confirm it — and **Remove** beside each row: the same list and buttons as first-time startup
  ([storage.md](storage.md) rule 1). Moving the game to another drive is Remove, then Add missing. An installation
  added later gets the working copy at the next APPLY.

*Until 2026-10-24 (morning) it had Rescan, Add, Relocate and Remove; for a few hours that day it was an Installations…
button instead of a sub-tab, before Alan settled that File Manager is where installations live.*

**Settled with Storage S2's sources (Alan, 2026-10-24), built in S2b:**

- **Rescan and Relocate are removed.** Detection runs at every Elite-Intel start, before the daily backup, so Rescan
  has nothing to add; moving the game is Remove, then Add missing.
- **A removed installation stays removed.** Detection at the next start would otherwise put a detected installation
  straight back, so the stored list records that the user removed it, and detection skips it. **Add missing** on its
  folder brings it back.

**Settled with Storage S2b's sources (Alan, 2026-10-25):**

- **A removed installation is neither backed up nor written** — removing it says it is not one of the user's.
  **Remove's confirmation says so:** *"Remove Steam (D:\SteamLibrary\…)? BindForge will no longer back up this
  installation or write to it. Add missing brings it back."*
- **Add missing checks for `EliteDangerous64.exe`** in the installation's Odyssey product folder, as well as its
  `ControlSchemes` folder.

**Run first-time startup again — for testing, and to be hidden or removed before V1.2 ships** (2026-10-24, Alan). A
button on this sub-tab that clears the "first-time startup complete" flag ([storage.md](storage.md) rule 1) and
nothing else — no backup is deleted, no game file is touched, and the working copy stays until first-time startup
replaces it. It asks first (`HudConfirmDialog`). The next click on the BindForge tab runs first-time startup again,
from the live copies, with its backup first. **It is shown to every user**, because Elite-Intel has no way to tell a
tester from anyone else; the build plan carries the reminder to take it out.

**A missing installation is never removed on its own.** A row whose folder has gone stays in the list, marked
missing, because an unplugged drive and an uninstall look identical from here.

**An installation folder can hold more than one product, and only Odyssey is in scope.** Under
`Products\` a Steam installation may carry both `elite-dangerous-64` (Horizons) and
`elite-dangerous-odyssey-64`, **each with its own `ControlSchemes` folder**, and they genuinely differ —
observed 2026-09-07, Horizons ships 49 device entries against Odyssey's 51 and has no `DeviceButtonMaps`
folder at all. **BindForge targets `elite-dangerous-odyssey-64` only** (Krondor's decision; Elite-Intel is
an Odyssey application), so the product is a constant rather than part of the identity.

**How installations are found — built.** `WindowsGameInstallationProvider` (Steam, Epic, Frontier and Oculus
defaults) and, 2026-10-14 at Krondor's request, `LinuxGameInstallationProvider`, chosen by
`GameInstallationProvider.forThisPlatform`. Linux reads every place a Steam client can live — `~/.steam/steam`,
`~/.local/share/Steam`, Snap's `~/snap/steam/common/.local/share/Steam` and Flatpak's
`~/.var/app/com.valvesoftware.Steam/.local/share/Steam` — since several can be installed at once, and each one's
`libraryfolders.vdf` (`SteamLibraries`, shared with Windows). A client or game reached twice through a link is
reported once. Each installation carries its own bindings folder, in the Proton prefix of the library holding it
(`steamapps/compatdata/359320/pfx/...`). Epic and Frontier copies under Lutris, Heroic or a hand-made Wine prefix
record nothing reliable and come in through **Add missing**.

## What a `.binds` File Might Not Be

**Added 2026-09-17, measured rather than imagined.** Across 48,354 player-shared files, **445 carry a
`.binds` extension and are not bindings files** — 363 of them a preset file picked by mistake from the
same folder, the rest other games' configs, device profiles, truncated files and stray XML. Full breakdown in
[Bind Editor](bind-editor.md#a-binds-name-does-not-make-it-a-binds-file--2026-09-17).

**What that asks of this screen:** the bindings-file dropdown lists what is *in* the folder, so it will list
those files too. Selecting one has to produce a sentence naming what the file appears to be, never a stack
trace and never an empty editor — and **no restore, backup or apply path may overwrite a file BindForge
could not identify.**

## Edit History

**Folded into Player Backups, 2026-10-22 (Alan).** It was a per-file undo — the last ten versions of each file, with
its own screen — so that a small experiment did not need a full backup first. Every APPLY now takes a full backup
first ([storage.md](storage.md) rule 12), which meets that.

**Two of its rules carry over to backups** (2026-10-24, Alan):

- **The backup folder is the record — no database table** (settled 2026-10-03). The list of backups is the folder's
  listing, sorted by the date stamp in each name; a table would have to be kept in step with the folder, and the two
  could disagree.
- **Backups live in Elite-Intel's own data folder, never in a game folder** — a game update replaces those
  wholesale, so a backup kept there would be deleted by the event it exists for.

## Settings

**A retention settings section on Player Backups** (2026-10-24, Alan) — beside the list, where the mockup already has
*Backups go to* and *Keep backups for*:

| Setting | Default | Description |
|---|---|---|
| Backups go to | Elite-Intel's default backup folder | Where backups are written. Never inside a game installation folder, where a game update would delete them (2026-10-24, Alan). If it cannot be written, the backup goes to the default rather than failing. |
| Keep daily backups for | 30 days | 30, 60, 90 or 180 days ([Retention](#retention)) |
| Keep before-changes backups | the newest 100 | 10 to 1000 — never so few that one evening of applies empties it |

*Removed 2026-10-24 (Alan): "Auto-backup on Elite-Intel launch" — the startup backup is the safety net and cannot be
switched off ([storage.md](storage.md) rule 12); "Edit History retention" — Edit History is gone.* **Built 2026-10-24
(Storage S1):** migration `12010` drops the `auto_backup_on_launch` column, and `edit_history_retention` stays in the
table until Edit History's code is removed (S4), because it still reads it. *Until S1b (2026-10-24) the settings were meant
for a BindForge panel on Elite-Intel's Settings screen, and there was one retention, 30 days with a floor of one day.*

**Built 2026-10-24 (Storage S1b; Alan approved):** a *Backup Settings* section above the list. **Backups go to** shows
the folder backups are actually written to — the chosen one, or the default when it is unset or unusable — with
**Browse…** and **Use Default**, which clears the choice. **Keep daily backups for** is a dropdown of the four
choices; **Keep newest before-changes backups** a stepper, 10 to 1000 in tens. Each saves when changed and applies at
the next Elite-Intel start. Migration `12011` adds `daily_backup_retention_days` (30) and `before_changes_backups_kept`
(100) and drops `backup_retention_days` — **its old value is not carried over**: no screen ever set it, V1.2 has not
shipped, and it need not be one of the four choices.

**A folder Browse… picks that would not be used is not saved, and the user is told why** (Alan, 2026-10-24, from the
S1b review; principle 9): *inside a game folder*, *could not be created* or *cannot be written to*, with backups still
going to the folder shown. Saved, it would have fallen back to the default without a word, and Browse… would have looked
broken. The check is the same one a backup uses (`BackupDestination.check`), so the two cannot disagree. A stored
folder that becomes unusable later — an unplugged drive — still falls back silently at backup time, as above. The
settings are read and saved one at a time, in order, and the manager's read-modify-write is locked, so quick stepper
clicks can never leave a stored value the screen does not show (review, 2026-10-24).

**Where they are stored — built 2026-09-28:** a `bindforge_settings` table of their own, one column each; the
destination is rejected at the setter if the platform cannot represent it, and each retention is checked against its
choices. **Resolving an unset destination to a real folder is not done in the settings manager** —
`bindforge.storage.BackupDestination` reads the stored value and is the only place that decision is made (moved from
`PlayerBackupService` 2026-10-24, Storage S1). A destination that cannot be read, created or written, or that lies
inside an installation or the bindings folder, falls back to the default rather than failing the backup: the backup
is the thing being protected. See [Settings Storage](../../01-host-integration/elite-intel-platform-map.md#a-bindforge_settings-table--built-2026-09-28).

## What exists in code today — checked 2026-10-21, updated for Storage S1 2026-10-24

From the [simplification review](../../../bindforge-simplification-review.md), chunk 6, set against
[storage.md](storage.md):

| Today's code | Against `storage.md` |
|---|---|
| ~~**Backup Now** copies `.binds` and `StartPreset` only, **no device files**, and finds `StartPreset` with the old `findStartPresetFile`~~ **Built 2026-10-24 (Storage S1):** every file from every installation, as a date-stamped folder (`bindforge.storage.Backups`); a zip in one of four kind folders since 2026-10-24 (S1b) | every file from every installation, as a date-stamped zip (rules 11–12) |
| ~~**No backup when Elite-Intel starts**~~ **Built 2026-10-24 (Storage S1):** `StartupBackup`, every start, silent; the switch is gone; once a day since S1b | a silent daily backup at the first start each day, which cannot be switched off (rule 12) |
| ~~**Nothing is ever deleted**~~ **Built 2026-10-24 (Storage S1):** retention at start, after a successful backup; two rules since S1b | daily backups kept by age, before-changes backups by count, user backups until deleted, first-time startup's backup forever (rule 11) |
| **Restore covers `.binds` only, one file at a time**, to the editing copy or straight to the game | everything, no choices, to the live copies and the working copy (rule 10) |
| **The "before BindForge" backup** holds everything, but its restore covers `.binds` only | part of the one backup system |
| **Edit History is written three ways** — device files into `bindings\history\<installation>\` (last 10), `.binds` copies into the backup folder (never deleted), `StartPreset` copies into that same folder (last 10) — and a `.bak` is left beside the working copy on every save | all of it goes: one backup system, nothing per file (rule 11) |
| **Game Install Locations** ~~shows no match states and no Add missing~~ **Built 2026-10-25 (Storage S2b):** Add missing, Remove (stays removed) and Run first-time startup again; Rescan and Relocate removed. Match states wait for S9 | the list with match states, Add missing and Remove (rule 1) |

**Auto-backup and retention are one change, not two.** Today nothing prunes, and that is harmless because
every backup is a deliberate button press. Add the launch trigger on its own and the folder grows every
time Elite-Intel starts, on a machine where the user never asked for a single backup. Whichever lands
first, the other has to land with it.

**BindForge owns migrations `12000–12499`**, commander and galaxy work `12500–12999`, per Krondor's <!-- terminology-ok: names the real db-migration/commander/ tree and Krondor's per-commander work -->
[proposal §4](../../multi-install-proposal.md#4-migrations-one-tree-per-file), accepted 2026-09-24, in the
shared top-level `db-migration/` tree and never `db-migration/commander/`, which is Krondor's alone. <!-- terminology-ok: names the real db-migration/commander/ tree and Krondor's per-commander work -->
The range held one file when this was written; it now holds `12000` to `12011` — look in `db-migration/` for the
current list rather than trusting a count here. [An applied migration is never edited](../../../CLAUDE.md).

## Export / Import — Considered, Then Cut

A dedicated Export/Import feature for device configuration was considered and explicitly cut. Its main justified use case — remapping an alias configuration for a recipient's different hardware — doesn't hold up, because VID/PID is read-only and hardware-derived by design; an imported alias would still require the same manual re-association that Alias Designer's normal setup already requires. "Browse for backup file" already supports restoring a backup onto another machine, which is what a genuine transfer between a player's own machines actually needs. Nothing a dedicated Export/Import feature would add beyond that was judged worth its own feature.

## Community Sharing of Binds Files — Not a Feature

Unlike StarVizion's Vizlets (see [StarVizion — HoloFrames and Persistence, Export and Import](../starvizion/holoframes-and-persistence.md#export-and-import)), BindForge does not have a community-sharing story for binds files, and this is a deliberate decision rather than a gap. A `.binds` file is tightly coupled to the exact physical hardware it was authored on — specific device VID/PIDs, exact axis/button counts, and often a specific keyboard-layout preference (e.g. ESDF instead of WASD). Handing a `.binds` file to another player is technically trivial (it's a plain file), but practically nearly useless unless the recipient happens to own an unusually similar setup — a mismatch made worse, not better, by anything BindForge could automate, since the device identifiers in the file are hardware-derived and cannot be usefully remapped without the recipient already owning matching hardware. File Manager's backup/restore is the only file-movement feature BindForge needs — it exists to protect one player's own configuration, not to distribute it.

## Deferred

**Restoring part of another player's backup** — deferred 2026-10-24 (Alan): this version restores everything, with no
choices, and is for the user's own backups. The design below is kept for a later version.

### Three use cases, and the third is refused

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

### How BindForge tells case 1 from case 3 — settled 2026-09-12

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
