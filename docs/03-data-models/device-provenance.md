# Data Model — Device Provenance

**Status:** Design decision taken 2026-09-01. Schema not yet final; no migration written.

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

**The grain is one row per device per installation**, not one row per device. `DeviceMappings.xml` and
`.buttonMap` are duplicated per storefront installation and can legitimately disagree, so a single row per
device cannot represent the state — this is the same reasoning that killed the aggregate columns in
[Alias Designer's device list](../02-features/bindforge/alias-designer.md#my-devices).

| Field | Purpose |
|---|---|
| `install_id` | which installation this row describes |
| `vid`, `pid` | hardware identity **as currently reported** — survives a rename, but **not a vendor firmware update**; see [VID/PID Is Not a Stable Identity](../02-features/bindforge/alias-designer.md#vidpid-is-not-a-stable-identity). Correlates to `.binds` `Device=` only for devices with no entry. **Compare case-insensitively** — see [Hex case](#hex-case-is-not-a-convention-in-frontiers-file). |
| `device_name` | the XML element tag for this installation, which is also its `.buttonMap` filename stem |
| `provenance` | `frontier` \| `user_preexisting` \| `bindforge` \| `unknown` — **approved 2026-09-06**, see [below](#the-table-cannot-solve-first-import) |
| `mirrored` | whether this installation shares one definition with the other mirrored ones |
| `alias_confirmed` | gates button/axis naming per [Device Editor](../02-features/bindforge/alias-designer.md#device-editor) |
| `has_button_map` | whether a rename must also rename a file |
| `previous_name` | orphan cleanup after a rename |

### The key is `(install_id, device_name)` — approved 2026-09-06

**Found 2026-09-06 by parsing Frontier's stock file.** A device element owns a *set* of VID/PID pairs, not
one. The stock file holds **51 device elements carrying 136 (VID, PID) tuples** — `<GamePad>` alone has a
primary pair plus **79** `<Alternative>` pairs, and `<DualShock4>` has three in total. Keyed on
`(install_id, vid, pid)`, GamePad would become eighty rows describing one entry.

**The grain of a provenance record is one entry, and an entry is named by its XML element.** So the key is
`(install_id, device_name)`, which is also what the file itself uses — the element tag is the identity, and
it doubles as the `.buttonMap` filename stem. A rename updates the row and leaves `previous_name` behind for
orphan cleanup; a device present in two installations has two rows that may legitimately carry different
names, which is the divergence the design exists to allow.

**VID/PID stays on the row rather than moving to a child table**, because the multi-pair entries are all
Frontier's, and Frontier's entries live in the shipped reference file rather than in this table. This table
records what BindForge knows about entries it might *touch*, and a player-added entry carries one pair.

**Open, and cheap to defer:** what happens if a player adds `<Alternative>` pairs by hand, or edits one of
Frontier's multi-pair entries. Neither is common, and neither has been observed. Recorded so the single-pair
assumption is a decision rather than an oversight.

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

### Two decisions, both now settled

**~~Scope: working copy, or per install?~~ Settled 2026-09-02: per install.** The earlier assumption — one
canonical working copy mirrored outward — cannot represent two installations holding deliberately different
entries, which Alias Designer now supports. Rows multiply by installation count, and that is the correct cost.

**~~Which installation seeds a device on first import?~~ Settled 2026-09-06: neither — both are preserved.**
With per-installation rows nothing has to *win*, because each installation keeps its own row. When a device
is found in two installations with different entries, BindForge records both exactly as found and marks the
device **unmirrored**. It does not adopt one, and does not ask on first run.

A first import cannot distinguish a deliberate difference from an accidental one, and the two mistakes do
not cost the same: preserving an accidental difference costs one click to fix, while flattening a
deliberate one destroys work BindForge cannot give back. So `mirrored` starts `false` for any device whose
entries disagree, and `true` only where they already matched.

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

V1.2 work uses the `011XX` block (`000XX` = v1.0, `010XX` = v1.1). Highest applied as of 2026-09-02 is
`01045__ship_vehicle_bays.sql`, so the block is clean and the first script would be `01100__device_provenance.sql`.

Migrations are applied once at application startup and **an applied migration is never edited** — a new
numbered script is added instead. The schema above should settle before it becomes a numbered file.

## Secondary marker — optional, not the mechanism

A `<!-- BindForge -->` comment in `DeviceMappings.xml` and `.buttonMap` would let BindForge re-derive ownership
if its database is lost (new machine, restore from backup without the DB). Worth having as redundancy.
**Not** worth relying on: it does not survive a patcher replacement, and it is untested whether the game
preserves comments in those two files at all.
