# These are two separate problems

There are three identities here, and the design depends on not mixing them up:

```
  ┌──────────────┬──────────┬──────────────────────────────────────────────────────────────┬───────────────────────────────────────────────────────────────────┐
  │   Identity   │ How many │                             Key                              │                        What belongs to it                         │
  ├──────────────┼──────────┼──────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────────────────────┤
  │ User         │ always 1 │ (the machine / OS account)                                   │ hardware, .binds, StartPreset, app settings, BindForge as a whole │
  ├──────────────┼──────────┼──────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────────────────────┤
  │ Installation │ 1..N     │ storefront + root path (Dawn's derived identity)             │ DeviceMappings.xml, .buttonMap                                    │
  ├──────────────┼──────────┼──────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────────────────────┤
  │ Commander    │ 1..M     │ FID from the Commander/LoadGame journal events, not the name │ ships, carrier, cargo, missions, exo progress, and so on          │
  └──────────────┴──────────┴──────────────────────────────────────────────────────────────┴───────────────────────────────────────────────────────────────────┘
```

Neither problem depends on the other:

- BindForge only works with User-scoped and Installation-scoped data. .binds already lives in one shared folder, so bindings don't care which commander is playing.
- Separating commanders only works with journal and aux data, which all sits in one shared folder and doesn't care which install wrote it.

So they can be designed, built and shipped separately. Don't tie a commander to an installation. They aren't 1:1. I'm fairly confident, but Dawn should verify:

- The Frontier launcher can log out and back in as another account on the same install.
- Two Steam accounts on one PC share one Steam library, so one install folder.

So "install X means commander A" will sometimes be wrong. Always take the commander from the journal.

## Problem 1: several installs, and which one is primary

Only DeviceMappings.xml and .buttonMap are per-install, so "sync bindings" really means syncing that one file family. Dawn has already settled that mirroring is decided per device, not per install.

Recommendation: no install is primary, and "last edited wins" is off the table. The master copy of each mirrored device definition lives in Elite-Intel's database (next to Dawn's provenance table), and installs only receive copies.

The reason: the incident that started BindForge was the game patcher wiping the file. By modification time, a patch-wipe looks like the newest edit, so "last edited wins" would copy Frontier's stock file over every other install and wipe them all. A "primary install" is no better, because it can be wiped by a patch just the same. Nothing that lives inside a game folder can be the source of truth.

How it plays out:

1. Push: BindForge APPLY writes the master copy to every install where that device is mirrored.
2. Check: at startup, and when an install's file changes, compare each install's file against the master copy.
    - It matches Frontier's stock reference (Dawn already ships one): the file was wiped by a patch. Offer to restore it.
    - Any other difference: someone edited it by hand. Ask whether to adopt that version (and mirror it) or revert it. Never adopt it automatically. This is the same "ask, don't guess" rule Dawn already uses for restore.
3. A default install for the UI (which tab opens first) is fine as a convenience. It just has no authority over the data.

Optional: which install is running right now. This can be worked out from files alone, so it stays within the journal-only rule. Each install writes Products\<product>\Logs\netLog.*.log. When a new journal opens, the install whose netLog appeared at the same time is the one running. That lets us note "commander A played on Epic" as observed history (useful in the UI), but nothing should depend on it. Dawn would need to confirm the netLog timing on his machine.

## Problem 2: keeping commanders apart

### 2a. Which commander each journal file belongs to

- Each journal file belongs to one commander, read from the Commander/LoadGame event near the top of the file. A continuation part inherits the commander of the part before it. A file with no commander event (the game went to the menu and quit) belongs to no one and is ignored.
- Keep this as a small index: file → FID. Journals never change once closed, so each file is parsed once.
- This fixes a bug that exists today. JournalPreScanner and the other pre-scans read the newest N journals by filename (JournalFiles.newest). If you swap commanders, the pre-scan mixes their data. Filtering pre-scans by FID is the cheapest part of this proposal and doesn't need a schema change.

### 2b. Aux files (your "latest journal owns the aux files" rule)

Your rule is correct when commanders are played one at a time, with one refinement: split the aux files into two groups.

- Galaxy data, same for every commander: Market.json, Outfitting.json, Shipyard.json. The file itself names the station. No attribution is needed, and station_markets stays shared.
- Commander state: Status.json, Cargo.json, NavRoute.json, ModulesInfo.json, ShipLocker.json, Backpack.json, FCMaterials.json. These belong to the commander of the journal being tailed, and only count while that journal is live. An aux file last written before the current journal was opened belongs to the previous session, so ignore it until the game rewrites it.

### 2c. Splitting the data

Every table falls into one of three scopes:

- User: global_settings, bindings*, jukebox_*, the name/alias catalogues, ship_make
- Galaxy (shared, and every commander benefits): station_markets, commodity_mean_price, hunting_ground*, conflict_zone, pirate_*, the EDDN ledgers
- Commander: player*, ranks_and_progress, reputation, ship*, fleet_carrier*, cargo, materials, missions, bounties, combat_bond, bio_samples, trade_route, destination_reminder, game_session, the routes, deferred_notifications, chat_history, …
- Mixed tables need splitting either way: location, exo_mastery_* and codex_entries hold galaxy facts next to "what this commander did there". Your exo-mastery example is exactly this case: the catalogue is shared, "harvested by me"
  is per commander.

There are two ways to store it:

```
  ┌────────────────────────────────────────────────────┬─────────────────────────────────────────────────────────────────────────────────────────────────┬─────────────────────────────────────────────────────────────────────────────┐
  │                                                    │                        A. commander_fid column on every commander table                         │       B. One SQLite file per commander, attached next to a shared DB        │
  ├────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────┤
  │ Leaking one commander's data into another's        │ one missing WHERE leaks data, across ~40 tables                                                 │ can't happen by design                                                      │
  ├────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────┤
  │ DAOs                                               │ every commander query changes                                                                   │ no changes: SQLite resolves table names across attached databases           │
  ├────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────┤
  │ Primary keys                                       │ have to be rebuilt (ShipID 1 exists for every commander), which means a table-rebuild migration │ unchanged                                                                   │
  │                                                    │  per table                                                                                      │                                                                             │
  ├────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────┤
  │ Migrator                                           │ unchanged                                                                                       │ needs two migration trees (shared / commander) and a one-time move of       │
  │                                                    │                                                                                                 │ existing data                                                               │
  ├────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────┤
  │ Queries across commanders ("where's my alt's       │ easy                                                                                            │ possible, by attaching both                                                 │
  │ carrier?")                                         │                                                                                                 │                                                                             │
  ├────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────┤
  │ Switching commander                                │ change a context value                                                                          │ rebuild the connection pool, which happens rarely and at a quiet moment     │
  │                                                    │                                                                                                 │ (LoadGame)                                                                  │
  └────────────────────────────────────────────────────┴─────────────────────────────────────────────────────────────────────────────────────────────────┴─────────────────────────────────────────────────────────────────────────────┘

```

I lean towards B. For a single-commander user (almost everyone) nothing changes except one extra file, and leaks between commanders become impossible rather than something tests have to catch. It costs DatabaseMigrator work, which is contained. A still works, but it needs an architecture test that fails when a commander-scoped table or query is missing the FID.

2d. Switching commander at runtime

This is the riskiest part, and it's in memory rather than on disk. When a LoadGame arrives with a different FID:

1. Stop taking game events and drain the queue.
2. Switch the commander store.
3. Reset PlayerSession and any singletons that cache state.
4. Reload.
5. Publish LoadSessionEvent.

The current CommanderChangedEvent only updates the UI. The first time a new FID is seen, run the pre-scan on that commander's journals only. With the game closed, the default commander is the one from the newest journal that has one, which is your rule.

The question that decides the scope: are both clients running at the same time?

You said people buy the game again to have more commanders. My understanding, which Dawn should confirm, is that many of them also run both clients at once: carrier operations, wing mining, hauling for an alt. If so:

- two journals are written into the same folder at the same time, and "latest journal" stops meaning anything;

2. Switch the commander store.
3. Reset PlayerSession and any singletons that cache state.
2. Switch the commander store.
3. Reset PlayerSession and any singletons that cache state.
4. Reload.
5. Publish LoadSessionEvent.

The current CommanderChangedEvent only updates the UI. The first time a new FID is seen, run the pre-scan on that commander's journals only. With the game closed, the default commander is the one from the newest journal that has one, which is your rule.

The question that decides the scope: are both clients running at the same time?

You said people buy the game again to have more commanders. My understanding, which Dawn should confirm, is that many of them also run both clients at once: carrier operations, wing mining, hauling for an alt. If so:

- two journals are written into the same folder at the same time, and "latest journal" stops meaning anything;
- Status.json switches between the two clients with nothing to say which one wrote it. Without reading game memory, which we don't allow, this can't be fully solved;
- Cargo.json and NavRoute.json can be matched up, because each write comes with an event in the writing client's journal. Accept an aux update only when the followed journal has a matching event within about a second;
- simulated keystrokes go to whichever game window has focus.

Suggested scope for V1.2: commanders played one at a time are fully supported. For two clients at once: detect it (two journals both live), warn the user once, follow the commander they pick, and treat Status.json-based features as unreliable.

Testing when you only have one install

- Problem 2 can be tested fully on Linux. Journal fixtures with two FIDs cover the file-to-commander index, pre-scan filtering, commander switching, and journals written at the same time. Ask Dawn for a real, cleaned-up journal folder from his multi-install machine to use as a fixture.
- Problem 1 (install detection, netLog timing, the patch-wipe check) needs Dawn's machine. That's already his area.

Suggested order

1. Journal-to-commander index and FID-filtered pre-scans. This is small, fixes a real bug, and changes no schema.
2. Commander storage split (B) and the runtime switch.
3. BindForge's master copy and pushing it to installs (Dawn).

Open questions for Dawn

1. Can a Frontier-launcher install switch between accounts?
2. Does a journal continuation part repeat the Commander event?
3. Does netLog creation line up with the journal's start time?
