# Multiple Installs and Multiple Commanders — Architecture Proposal

Dawn, this is a proposal for how multiple installations and multiple commanders fit together, and where your BindForge work and my commander work meet.
**Revision
2** folds in your first answers: storage is now decided (B, a database file per commander, §3), migrations get a new layout and a new numbering rule (§4), the split ships in V1.1 as v1.1.0020 (§6), and the master copy of your files follows your answer to Q8 (§2). What is still open is at the bottom, numbered as before.

## 1. The model: one User, many Installations, many Commanders

- **User** — always exactly one. This is the human, their hardware and their muscle memory. `.binds`,
  `StartPreset`, app settings and all of BindForge belong here.
-
**Installation** — 1..N, one per storefront install. Identified the way your File Manager doc already settled: the storefront, derived rather than assigned. `DeviceMappings.xml` and `.buttonMap` belong here.
- **Commander** — 1..M, identified by the
  **FID** from the `Commander` / `LoadGame` journal events. We use the FID rather than the name, which is only for display. Ships (with their voices and personalities), carrier, cargo, missions, exo progress, reminders and the rest belong here.

**Commander != User.** Several commanders are always played by the same person on the same hardware.

**Commanders are never tied to
installations.** Only one game client can run at a time, and journals, aux files and `.binds` all sit in folders shared by every install. The commander always comes from the journal and never from the install. It is not a 1:1 relationship anyway: two Steam accounts share one Steam library, and (still to be checked, Q5) the Frontier launcher may switch accounts on a single install.

Because of this the two workstreams are almost independent. BindForge only ever deals with User-scoped and Installation-scoped data, and commander separation only ever deals with journal-derived data.

## 2. Installations (your side): Elite-Intel keeps the master copy

**Agreed (Q8):** Elite-Intel keeps the master copy of the user's `.binds`, `StartPreset`, `DeviceMappings.xml`
and `.buttonMap` files **in its own
folder**, and pushes to the game when the user makes a change. No install is primary, and there is no "last edited wins".

- **Why not "last edited
  wins":** the incident BindForge exists for is the patcher wiping these files. Judged by modification time, a patch wipe looks like the
  *newest edit*. Last-edited-wins would therefore copy Frontier's stock file over every other install.
- **Why not a primary
  install:** it lives in a game folder, so a patch can wipe it too. No file inside a game install can be the source of truth.
- **The folder is User-scoped.** It sits beside the shared database (§3), never with a commander.
- **Where a push
  lands:** `DeviceMappings.xml` and `.buttonMap` go to every install where that device is mirrored. `.binds` and `StartPreset` live in the one shared `Options/Bindings` folder on Windows, so for them a push is a restore into that folder, not a copy per install.

The flow:

1. **Push.** APPLY writes the master copy wherever it belongs (above).
2. **Check.** At startup, and when a file changes, compare the game's file with the master copy:
    - **It matches Frontier's stock reference** (which you already ship) → a patch wiped it → offer to restore.
    - **Any other difference** → someone edited it by hand → ask whether to *adopt* it or
      *revert* it. Never adopt it automatically. This is the same "ask, don't guess" rule as your Restore.
3. A **default
   install** for the UI (which tab opens first) is fine as a convenience, but it has no authority over the data.

**When FDev adds new controls (your open point in
Q8).** You are right that the version gives it away: it is in the file name (`Custom.4.2.binds`) and also in the version attributes on the XML root. Proposed rule:

- **Never push a master over a game file of a higher
  version.** Doing so would silently delete every control the new version added.
- **On a version bump, merge instead of
  restoring:** start from the game's new file and re-apply the user's value for every control the master already knows. New controls keep the game's default. The merged file becomes the new master, and the user is told what was carried over.
- This matches the existing auto-fix rule of never changing a binding the user already has.
- The stock reference used by the patch-wipe check is only valid for one game version, so it needs the same versioning.

## 3. Commanders (my side)

**Which commander each journal belongs to.** Every journal file belongs to one commander, read from the
`Commander` / `LoadGame` event near the top of the file. The exception is a continuation part (`.02.log`):
as far as I know it opens with only a `Fileheader`, so it inherits the commander of the part before it. The old part ends with a `Continued` event, which we do not handle anywhere yet. The fixture (Q7) will confirm this. A journal with no commander event (the game went to the menu and quit) is ignored. The result is kept in a small index: file → FID.

**Which commander the aux files belong
to.** Only one client runs at a time, so the commander of the live journal owns the aux files. The aux files fall into two groups:

- **Galaxy data, the same for every
  commander:** `Market.json`, `Outfitting.json`, `Shipyard.json`. The file names its own station, so there is nothing to attribute.
- **Commander state:** `Status.json`, `Cargo.json`, `NavRoute.json`, `ModulesInfo.json`,
  `ShipLocker.json`, `Backpack.json`, `FCMaterials.json`. These belong to the commander of the live journal. A file last written before that journal opened belongs to the previous session and is ignored until the game rewrites it.

**When the game is closed**, the current commander is the one in the newest journal that names a commander.

### Storage: decided — B, a shared file plus one file per commander

```
elite_intel.db     ← shared: user settings, BindForge, and ALL galaxy learning (EDDN, locations, markets)
cmdr_<FID>.db      ← one per commander: ships, cargo, missions, reminders...
cmdr_<FID2>.db
```

The shared file is always open. The current commander's file is `ATTACH`ed next to it on every pooled connection, so both look like one database. The DAOs stay as they are: SQLite resolves an unqualified table name in whichever file has it. A commander switch (at `LoadGame`, a quiet moment) rebuilds the pool with the new file attached.

We chose B over A (one file with a `commander_fid` column on every commander table) for three reasons:

- **Leaking is impossible.** Under A, one missing `WHERE commander_fid = ?` shows one commander's data to another.
- **No primary keys to
  rebuild.** Many tables assume one commander: `player` is pinned to `id = 1`, `ship` is keyed by the game's ShipID (`1` exists for every commander), and the carrier tables hold "the" carrier. Under B each commander's file has its own row.
- **Almost no DAO changes.** Under A every query on ~35 tables changes.

**Nothing learned is
lost.** Everything the app learns about the galaxy lives in the shared file, and every commander reads and fills it. Commander B knows every resource site, conflict zone and body that commander A's sessions learned.

**Rules that come with B:**

- **A table lives in exactly one file.** If a name exists in both, SQLite silently picks the shared one.
- **No foreign key crosses files** (SQLite cannot enforce one). Today's foreign keys (`ship_loadout` →
  `ship`, `trade_route` → `trade_profile`/`ship`) are all commander-side, so none crosses.
- **Views and triggers stay in their own file.** A view in one file cannot see tables in the other.

### Every table gets one of three scopes

- **User** (shared file): settings, bindings, jukebox, the name/alias catalogues, and **everything BindForge adds**.
- **Galaxy** (shared file): station markets, mean prices, the EDDN ledgers (`hunting_ground`,
  `conflict_zone`, `pirate_factions`, `pirate_hunting_grounds`), and the galaxy half of the mixed tables below.
-
**Commander** (commander file): player, ranks, reputation, ships (with voice and personality), carrier, cargo, materials, missions, bounties, bio samples, trade routes, reminders, routes, chat history.

**Mixed tables are split in
two.** The galaxy half stays shared, and a thin commander-side table holds "what this commander did there":

| Table | Stays shared | Moves to the commander file |
|---|---|---|
| `location` (a JSON blob per row) | body physics, signals, genus, materials, station, economy, faction, powerplay, market/outfitting/shipyard | `ourDiscovery`, `weMappedIt`, `bioScansCompleted`, `partialBioSamples`, `isHomeSystem` |
| `exo_mastery_*`, `codex_entries` | the catalogue | "harvested/logged by me" |
| `game_session` | mic RMS thresholds and other hardware/app settings (User) | whatever turns out to be about the commander's session |

`game_session` was listed as Commander in revision 1, but its mic thresholds describe the user's hardware. Every column needs a scope before the split. Option A would have needed the same splits, since a location row cannot be stamped with one commander without hiding it from the other.

**Two exceptions to "filter the pre-scans by FID":**

- **The hunting-ground journal scan reads every commander's
  journals.** A sighting is a galaxy fact, whoever flew past it. Its watermark (`hunting_ground_scan`) stays in the shared file.
- Only pre-scans that rebuild commander state (ships, cargo, missions, materials, ranks) are filtered.

## 4. Migrations: one tree per file

Today there is one tree, `db-migration/`, applied in filename order and recorded by filename in
`schema_migration`. With two kinds of file it becomes two trees:

```
db-migration/               ← shared tree (elite_intel.db): the existing 100+ files stay exactly where they
                              are, and all later shared/BindForge work goes here too
db-migration/commander/     ← commander tree (every cmdr_<FID>.db): starts with 11000__baseline.sql
```

**How it works:**

- **Each tree is applied to its own file, opened as `main`** (not attached), with its own `schema_migration`
  table. That matters: an unqualified `CREATE TABLE` goes to `main`, so a commander migration run through an attachment would create its tables in the shared file.
- **The existing files are not
  touched.** They are already recorded in `schema_migration` under the names they ran with, so none of them is renamed, renumbered or moved. The shared tree is simply the top level of
  `db-migration/`.
- **The commander tree starts from a
  baseline:** `commander/11000__baseline.sql` creates every commander table at its V1.1 shape. Each commander's file is built from it on first sight of that FID.
- **`DatabaseMigrator` needs one fix first.** It walks `db-migration/` recursively, so today it would pick up
  `commander/` files and apply them to the shared file. The shared tree must read only the top level, and the commander tree only `commander/`.

**Numbering: sequential, never overlapping.**

- **New band rule: the first two digits are the
  version.** `11XXX` = V1.1, `12XXX` = V1.2, so each release has 1000 numbers in each tree. The same band is used in both trees.
- **Files already written keep their
  numbers** (`000XX` = V1.0, `010XX` = V1.1 so far). They are never renamed, and they all sort before `11000`, so filename order stays correct. The next V1.1 migration is
  `11000`, not `01059`.
- **The commander tree is mine alone.** By the contract (§5), BindForge never touches it, so it needs no sub-ranges.
- **In V1.2 the shared tree has two writers, so its band is split: BindForge `12000–12499`, commander/galaxy
  work `12500–12999`.** Your provenance script becomes `db-migration/12000__device_provenance.sql`. V1.1 maintenance has one writer (me), so `11XXX` is not split.
- **Out-of-order arrival is
  safe.** Migrations are recorded by filename, not "highest number applied", so if your 12003 lands after my 12502 has run on a dev database it still gets applied. The only rule is that a migration must not depend on one from the other range without a heads-up.
- **The rule holds up to
  V1.9** (`19XXX`). A V2.0 would be `20XXX`. A V1.10 has no band, so that needs a decision before it happens.

**A guard test (`MigrationLayoutTest`) fails the build when:**

1. two files in one tree share a number;
2. a file sits outside a known band: a number below `11000` that is not one of the files already written, or a first-two-digit version that has not been released or opened yet;
3. the same table is created in both trees;
4. a migration has a semicolon inside a comment (the existing trap: `DatabaseMigrator` splits on `;` before stripping comments);
5. a foreign key references a table in the other tree.

**The one-time
split** (on the upgrade to the V1.1 release that carries it, §6) needs the FID, so it is a Java step, not an SQL file:

1. Take the owner FID from the newest journal that names a commander. The existing data is moved as it is, and leaked data is not repaired (§6). With no journals and no data (a fresh install), there is nothing to move: the first commander file is created at the first `LoadGame`.
2. Delete any half-built `cmdr_<FID>.db` left by an earlier crash, build it from the commander tree, attach it, and copy every commander table (and the commander half of the mixed tables) across.
3. Check that the row counts match, and record "split done" in the shared file.
4. Only then drop the moved tables and columns from the shared file.

The order is what makes it crash-safe. The app runs in WAL mode, where SQLite does
**not** make a transaction atomic across attached files, so nothing is dropped until the copy is verified and recorded. A crash at any point just repeats the step on the next start.

**Tests** keep the in-memory setup: the shared database stays `file:testdb?mode=memory&cache=shared`, and a second named in-memory database is attached as the commander file.

## 5. The contract between BindForge and commander work

1. **No link between commanders and
   installs**, in either direction. BindForge never subscribes to a commander switch. If it ever needs to, something is in the wrong scope.
2. **Every new table declares its
   scope** (User / Galaxy / Commander). BindForge tables are User-scoped and live in the shared tree.
3. **Migration
   numbers:** as in §4. BindForge writes only to the shared tree (top level of `db-migration/`), in `12000–12499`.
4. **Terminology (agreed, Q6):** **user** (or "player") for the human, and
   **commander** only for the in-game identity. The BindForge docs will be updated wherever "commander" means the person, and the glossary gets an entry for both words.
5. **Current commander while the game is closed** = the newest journal that names a commander.

## 6. Release plan: V1.1.0020

The split and commander switching ship in V1.1 maintenance, in **v1.1.0020**, not in V1.2. There are two reasons:

1. **Users who run several commanders today get the capability now.**
2. **V1.1 and V1.2 builds must be able to share one
   database.** That holds only while V1.2's migrations are purely additive. The split is the most destructive migration we will ever write: it drops tables from the main file. Shipped in V1.2, it would break the next V1.1 run on the same database. Shipped in V1.1, V1.2 inherits the two-file layout and only adds `12XXX` files, which V1.1 ignores. Developers and users trying a V1.2 beta are in the same position here.

**Branch:** the work happens on `V1.1-Release-multiple-commanders`, cut from `V1.1-Release`. We both test it there for a while before it merges back and rolls out in v1.1.0020 with other fixes.

**Build order on that branch:**

1. **The journal → FID index and pre-scans filtered by
   FID.** The index covers continuation parts. The hunting-ground scan is the exception (§3). This also fixes a real bug: the pre-scans read "the newest N journals", which mixes commanders after a swap.
2. **Two trees in `DatabaseMigrator`**, with the existing files left where they are. The guard test goes in here.
3. **The split and commander
   switching.** The upgrade step moves the existing data to the owner's file (§4). A new FID gets its own file at `LoadGame`, which the filtered pre-scan fills from that commander's own journals. The in-memory session is reset fully on a switch.

**Data that has already leaked is not
repaired.** V1.1 has not supported multiple commanders until now, and users running several know it. The upgrade moves the existing data as it is, mixed or not, to one owner: the commander in the newest journal. What that means for a multi-commander user:

- The owner's file keeps whatever the other commanders left in it: their ships (with the voices set on them), reminders, trade routes and so on.
- Every other commander starts with a fresh file, rebuilt from their own journals. Ships come back with default voices, and anything the user entered by hand for that commander stays with the owner.
- From v1.1.0020 on, nothing leaks again.

The release notes should say so, so a user can pick which commander to play last before upgrading.

**v1.1.0020 is a one-way
door.** Once a user runs it, an earlier V1.1.x build breaks on that database, because it expects the tables the split dropped. The release notes have to say that too. It is also the compatibility floor: V1.2, and with it BindForge, is built on V1.1 at v1.1.0020 or later.

**V1.2** keeps only BindForge's side: the master-copy folder and the version-bump merge (§2).

## 7. Testing

- **Commander separation can be tested on Linux with
  fixtures:** a journal folder containing two FIDs exercises attribution, filtering, switching and migration. You've agreed to send a cleaned-up journal folder from your multi-install machine (Q7). Ideally it includes a session long enough to roll into a
  `.02.log` part, which settles Q4.
- **Install detection, the patch-wipe check and the version-bump merge** need your machine. That is already your area.

## Answers so far

| # | Question | Answer |
|---|---|---|
| 1 | Does BindForge conflict with the model or the contract? | Pending: you'll check after work. |
| 2 | Storage A or B? | **B.** Ships and their voices go per commander (§3). |
| 3 | Migration numbers | Replaced by the layout in §4. Please confirm. |
| 4 | Continuation journals | Unknown. Assumed one commander per file. The fixture settles it. |
| 5 | Frontier launcher account switching | Pending: you're asking CMDR Burr. |
| 6 | Terminology | **Yes.** |
| 7 | Fixture | **Yes.** |
| 8 | Master copy | **Elite-Intel's folder**, pushed on change. Version bumps: see §2. |

## Open questions

1. **(
   Q1)** Does anything in BindForge conflict with "commanders are never tied to installs" or "BindForge is entirely User/Installation-scoped"?
2. **(
   Q3)** Does the layout in §4 work for you: the shared tree at the top of `db-migration/` and a `commander/` tree, the new `12XXX` band, you in `12000–12499` of the shared tree, me in `12500–12999`?
3. **(Q4)** When the fixture is ready: does a `.02.log` part repeat the `Commander` event, or only the
   `Fileheader`?
4. **(Q5)** Can one Frontier-launcher install log out and back in as a different account?
5. **(
   Q8)** Does the version-bump rule in §2 (never push over a newer file, merge the user's values into the game's new file) match what you had in mind?
