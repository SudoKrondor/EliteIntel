# Data Model — Device Provenance

**Status:** Design decision taken 2026-09-01. **Built 2026-10-01** as
`db-migration/12002__bindforge_devices.sql`, which carries this table alongside the two the master needs —
see [Migration](#migration).

BindForge needs to know things about a device that **cannot be stored in any of the files it manages**:

- Which `DeviceMappings.xml` entries did BindForge create, versus Frontier shipping them, versus the player
  having added them by hand before BindForge ever ran?
- Has the player confirmed this device's alias yet? (The Device Editor gates button/axis naming on it.)
- Does a `.buttonMap` exist for it, and therefore must a rename also rename a file?

This document records why that state has to live in Elite-Intel's database, and what it has to carry.

---

## Why not in the files

Three separate reasons, each sufficient on its own.

**`.binds` strips anything the game does not model.** Confirmed by live testing on 2026-09-01: a comment
placed in the active `.binds` was gone after an in-game binding change. The game reconstructs the file from its
own model rather than round-tripping it. See
[`.binds` format reference §10](../02-features/bindforge/domain-knowledge/EliteDangerous-BindsFileFormat.md).

**`DeviceMappings.xml` and `.buttonMap` live in the game install folder**, which the patcher replaces wholesale
during updates. That is the exact incident that motivated BindForge in the first place. A marker inside a file
that gets replaced is a marker that disappears when it is most needed.

**`DeviceMappings.xml` has no built-in/user distinction to read.** Its schema is a flat list of elements, one
per device, with no attribute or grouping separating Frontier's shipped entries from the player's own. This was
found the hard way: the BindForge mockup's "Built-in Devices" list was seeded from a real
`DeviceMappings.xml` and contains `LVWAP` and `RVWAP` — the developer's own hand-added devices — sitting
indistinguishably among Frontier's.

**Corrected 2026-09-06, then corrected again 2026-09-08 — and the second correction restores the first
figures as also true.** The current entries are `LVWAP` (VID `3344`, PID `83F4`) and `RVWAP` (VID `3344`,
PID `03F5`). Earlier revisions gave `83F3` and `43F4`, which were not wrong: they are what the *same two
devices* reported in 2025, recorded from a `.binds` file of that era. **Neither figure was an error; the
hardware changed.** See
[VID/PID Is Not a Stable Identity](../02-features/bindforge/alias-designer.md#vidpid-is-not-a-stable-identity). The complete difference between the edited file and Frontier's stock is three lines — those two
elements and one XML comment. `VPCPanel` and `VPCThrottle`, which look equally like personal additions,
are **Frontier's**: they are present in the stock file.

## What the record carries

Sketch, not final:

**The grain is one row per device per installation**, not one row per device — **and this survives
standardisation, for a different reason than it was written for.**

It was justified by installations being allowed to disagree. They are not any more:
[every install gets the same files](../02-features/bindforge/overview.md#every-install-gets-the-same-files--settled-2026-09-23),
and the user edits one master. The obvious inference is that one row per device would now do.

**It would not.** The rows do not describe what the user *chose* — the master holds that. They describe
**what each installation actually contains**, which is a different fact and the only one that can detect
drift. A game patch resets these files per installation; that is the incident BindForge exists for. With one
row per device there is nothing to compare a patched installation against, so a wipe is indistinguishable
from a device that was never configured. Per-installation rows are what let the
[**M** marker](../02-features/bindforge/alias-designer.md#one-record-and-where-it-lands--reworked-2026-09-26)
say *this installation matches the master* and the
[divergence list](../02-features/bindforge/alias-designer.md#divergence-between-installs--one-list-ranked-by-consequence)
say where it does not.

So: **the master records intent, these rows record reality, and drift is the difference between them.**
Collapsing the rows would remove the only copy of reality. *Do not "simplify" this to one row per device on
the grounds that installations now match — knowing they match is exactly what the rows are for.*

| Field | Purpose |
|---|---|
| `install_id` | which installation this row describes |
| `vid`, `pid` | hardware identity **as currently reported** — survives a rename, but **not a vendor firmware update**; see [VID/PID Is Not a Stable Identity](../02-features/bindforge/alias-designer.md#vidpid-is-not-a-stable-identity). Correlates to `.binds` `Device=` only for devices with no entry. **Compare case-insensitively** — see [Hex case](#hex-case-is-not-a-convention-in-frontiers-file). |
| `device_name` | the XML element tag for this installation, which is also its `.buttonMap` filename stem |
| `provenance` | `frontier` \| `user_preexisting` \| `bindforge` \| `unknown` — **approved 2026-09-06**, see [below](#the-table-cannot-solve-first-import) |
| ~~`mirrored`~~ | **Superseded 2026-09-23** — every install holds the same files, so there is no per-device switch to record. See [Every install gets the same files](../02-features/bindforge/overview.md#every-install-gets-the-same-files--settled-2026-09-23). |
| ~~`alias_confirmed`~~ | **Moved to the master 2026-10-01.** It gates button/axis naming per [Device Editor](../02-features/bindforge/alias-designer.md#device-editor), and that is a fact about the device, not about one installation: since standardisation there is [one record per device](../02-features/bindforge/alias-designer.md#one-record-and-where-it-lands--reworked-2026-09-26), so a name cannot be confirmed in Steam and unconfirmed in Epic. It sits on `bindforge_device_master`. |
| `has_button_map` | whether a rename must also rename a file |
| ~~`previous_name`~~ | **Moved to the master 2026-10-01**, for the same reason. A rename is one operation on the master that then renames a `.buttonMap` in *every* installation, so the name being renamed away from is one fact, not one per installation. It sits on `bindforge_device_master` and is cleared when the rename completes. |

### The key is `(install_id, device_name)` — approved 2026-09-06

**Found 2026-09-06 by parsing Frontier's stock file.** A device element owns a *set* of VID/PID pairs, not
one. The stock file holds **51 device elements carrying 136 (VID, PID) tuples** — `<GamePad>` alone has a
primary pair plus **79** `<Alternative>` pairs, and `<DualShock4>` has three in total. Keyed on
`(install_id, vid, pid)`, GamePad would become eighty rows describing one entry.

**The grain of a provenance record is one entry, and an entry is named by its XML element.** So the key is
`(install_id, device_name)`, which is also what the file itself uses — the element tag is the identity, and
it doubles as the `.buttonMap` filename stem. A rename updates each installation's row, with the master
holding the `previous_name` the orphan cleanup works from; a device present in two installations has two rows
that may legitimately carry different names, which is the divergence the design exists to allow.

**VID/PID stays on the row rather than moving to a child table**, because the multi-pair entries are all
Frontier's, and Frontier's entries live in the shipped reference file rather than in this table. This table
records what BindForge knows about entries it might *touch*, and a player-added entry carries one pair.

~~**Settled 2026-09-13 for Frontier's entries: BindForge never edits one.**~~ **Reversed 2026-10-04:** a
user with the controller attached may rename a Frontier entry or shadow it with one of their own, and from then
on it is the user's element like any other. Measured safe, and recoverable because BindForge carries Frontier's
shipped list — see [Built-in controllers in My Devices](../02-features/bindforge/alias-designer.md#built-in-controllers-in-my-devices--settled-2026-09-13)
and [the first match wins](../02-features/bindforge/domain-knowledge/EliteDangerous-DeviceMappings-ButtonMap.md#12d-two-entries-for-one-vidpid-the-first-in-the-file-wins--measured-2026-10-03).
**Provenance still governs what BindForge does unasked:** an entry it has not been told is the user's stays
`FRONTIER` or `UNKNOWN`, and is never touched without the user choosing to.

**Still open, and cheap to defer:** what happens if a player adds `<Alternative>` pairs by hand. Not observed;
recorded so the single-pair assumption is a decision rather than an oversight.

### Confirmed with real hardware: two controllers, one entry

**2026-09-06.** Two Sony controllers were attached and read with USB Device Tree Viewer:

| Controller | VID | PID | Enumerates as |
|---|---|---|---|
| DualShock 4 v1 (black) | `054C` | `05C4` | 1 interface, HID only |
| DualShock 4 v2 (blue) | `054C` | `09CC` | 4 interfaces, composite — includes Audio |

Both match `<DualShock4>` in Frontier's stock file — one through each of its two `<Alternative>` pairs. The
element's *primary* PID `0BA0` is the DualShock 4 USB Wireless Adaptor, which is neither of them.

**So two physically distinct controllers resolve to a single device entry, and therefore to a single
provenance row.** This is the multi-VID/PID structure confirmed against hardware rather than inferred from
the file, and it is why the key had to move off `(install_id, vid, pid)`.

Neither controller reports a serial number (`iSerialNumber 0x00`). The only thing separating them at the OS
level is the Windows instance path (`...&0&4` versus `...&0&3`), which encodes the **USB port** rather than
the device — replug into a different port and it changes. There is no stable hardware-unique identifier to
lean on.

**This is not the duplicate-VID/PID case** in [testing item 1](../00-overview/testing-required.md) — the PIDs
differ. It is a neighbouring case that is probably more common: *distinct devices sharing one entry.*

### Hex case is not a convention in Frontier's file

**Audited 2026-09-06 across all 136 tuples in the stock file:**

| | uppercase | lowercase | digits only |
|---|---|---|---|
| PID | 39 | 35 | 62 |
| VID | 51 | 55 | 30 |

There is no rule, and the inconsistency occurs *within a single line* — `<DualShock4>` carries VID `054C`
uppercase beside PID `05c4` lowercase. Roughly half the file would mismatch a case-sensitive comparison, so
**every VID/PID comparison, lookup and key derivation must fold case**, and BindForge must not "normalise"
Frontier's entries by rewriting them.

### Two decisions, one settled and one overtaken

**~~Scope: working copy, or per install?~~ Settled 2026-09-02: per install — and it still holds.** The
original argument (a single canonical copy cannot represent installations that deliberately differ) expired
with standardisation. The conclusion did not: see the grain above. Rows multiply by installation count, and
that is still the correct cost, because recording what each installation holds is the only way to notice one
has been wiped.

**~~Which installation seeds a device on first import?~~ Settled 2026-09-06: neither — both preserved,
device left unmirrored. Overtaken 2026-09-23.** There is no `mirrored` flag to set any more, and leaving
installations permanently disagreeing is the state standardisation exists to end. The replacement is
[first setup](../02-features/bindforge/alias-designer.md#first-setup--reconciling-the-installs--settled-2026-09-23):
BindForge reads every installation, shows what disagrees, and asks — once, per element.

**What carried over is the reasoning, which was always the valuable part.** *A first import cannot
distinguish a deliberate difference from an accidental one, and the two mistakes do not cost the same:
preserving an accidental difference costs one click to fix, while flattening a deliberate one destroys work
BindForge cannot give back.* That is still why nothing is adopted automatically. The rows still record both
installations exactly as found; what changed is that the user is asked to resolve it at setup rather than
left with the difference indefinitely.

### The table cannot solve first import

It records what BindForge does from the moment it runs. For a
`DeviceMappings.xml` that already contains the player's edits — the common case, and exactly the
LVWAP/RVWAP situation — there is still nothing to separate theirs from Frontier's.

The answer is to **ship a reference copy of Frontier's stock `DeviceMappings.xml`** and treat anything absent
from it as not-Frontier's. It is needed *alongside* this table, not instead of it.

**Captured 2026-09-06.** See
[reference-data/FrontierStock-README.md](../02-features/bindforge/reference-data/FrontierStock-README.md).
Taken from an Epic installation reinstalled on 2026-07-01 and never played since, and byte-identical to an
independent reconstruction made by deleting known personal entries from an edited Steam copy — two routes,
same bytes.

Two consequences for this table:

- **Ship it as an application resource, not as seeded rows.** Database seeding would need a new migration
  every time Frontier adds a device, and an applied migration can never be edited.
- **`provenance` gains a fourth value, `unknown`** (approved 2026-09-06). The reference will go stale, and under
  `frontier | user_preexisting | bindforge` a device Frontier ships after our capture is recorded as the
  player's — after which BindForge would offer to rename or clear something it does not own. `unknown`
  means *do not claim it, do not touch it*, and costs nothing.

## Migration

**BindForge writes in `12000–12499`.** The first two digits of a migration number are the version — `11XXX` =
V1.1, `12XXX` = V1.2 — and V1.2's shared band is split between its two writers, BindForge taking
`12000–12499` and commander/galaxy work `12500–12999`. Krondor's <!-- terminology-ok: names the real db-migration/commander/ tree and Krondor's per-commander work -->
[proposal §4](../multi-install-proposal.md#4-migrations-one-tree-per-file), accepted 2026-09-24. Files written before the band
rule keep their numbers (`000XX` = V1.0, `010XX` = V1.1) and sort before `11000`, so filename order still
holds.

So this becomes **`db-migration/<next free number in 12000–12499>__device_provenance.sql`**.

**Built 2026-10-01 as `12002__bindforge_devices.sql`** — the next free number, and named for devices rather
than provenance because one file carries all three tables: `bindforge_device_master` (the user's element set),
`bindforge_device_labels` (its `.buttonMap` content) and `bindforge_device_installs` (this table). They are
one change — the master is meaningless without something to compare it against — and splitting them across
three migrations would only mean three numbers for one schema.

**The device draft followed on 2026-10-04 as `12003__bindforge_device_draft.sql`** — `bindforge_device_draft`
and `bindforge_device_draft_labels`, the same shape as the master's two tables, plus a one-row marker saying a
draft exists. Saved edits live there until Apply copies them into the master. **`12004` adds
`bindforge_device_pending_removal`**: devices an Apply took out of the master whose entries are still in the
installations, kept until whatever removes the files clears them. See
[Alias Designer — where the draft lives](../02-features/bindforge/alias-designer.md#actions-and-what-each-one-reaches).

*Deliberately not a specific number.* Krondor's §4 named 12000 as the example; `12000__bindforge_settings.sql`
took it on 2026-09-28, and `12001__bindforge_installations.sql` took the renumbered one the same day. A
migration number is never reused or renamed, so whichever file lands first owns the number — which means a
doc cannot reserve one, and naming one here only creates a correction later. Take the next free number when
the file is actually written. Note the tree: the shared top level, never `db-migration/commander/`, which <!-- terminology-ok: names the real db-migration/commander/ tree and Krondor's per-commander work -->
is his alone by the §5 contract. Out-of-order
arrival between the two ranges is safe, because migrations are recorded by filename rather than by highest
number applied.

Migrations are applied once at application startup and **an applied migration is never edited** — a new
numbered script is added instead. The schema above should settle before it becomes a numbered file.

## Secondary marker — optional, not the mechanism

A `<!-- BindForge -->` comment in `DeviceMappings.xml` and `.buttonMap` would let BindForge re-derive ownership
if its database is lost (new machine, restore from backup without the DB). Worth having as redundancy.
**Not** worth relying on: it does not survive a patcher replacement, and it is untested whether the game
preserves comments in those two files at all.
