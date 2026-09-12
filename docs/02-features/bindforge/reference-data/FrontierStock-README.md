# Frontier Stock Reference Files

**Captured 2026-09-06.** These are Frontier's shipped files, unmodified. They exist so BindForge can answer a
question no file on disk can answer by itself: **which entries did Frontier ship, and which did the player
add?**

## Why this is needed

`DeviceMappings.xml` is a flat list of device elements with **no attribute, grouping or marker separating
Frontier's entries from the player's**. A device the player added by hand sits indistinguishably among the
136 Frontier ships. Nothing in the file's own schema recovers that distinction.

Neither does anything else:

- **The game install folder is replaced by updates**, which is the incident that motivated BindForge — a
  marker inside a replaced file disappears exactly when it is needed most.
- **`.binds` strips comments**, proven by live test. A comment marker is not a mechanism there.
- **File timestamps do not survive** a verify or reinstall.

The only reliable answer is a reference copy: anything present in the player's file but **absent from this
one** was not shipped by Frontier.

## Provenance of this capture

| File | Source |
|---|---|
| `FrontierStock-DeviceMappings.xml` | `E:\EpicLibrary\EliteDangerous\Products\elite-dangerous-odyssey-64\ControlSchemes\DeviceMappings.xml` |
| `VPCPanel.buttonMap` | same install, `ControlSchemes\DeviceButtonMaps\` |
| `VPCThrottle.buttonMap` | same install, `ControlSchemes\DeviceButtonMaps\` |

Taken from an Epic Games installation that had been reinstalled on 2026-07-01 and never played since, so its
`DeviceMappings.xml` had never been edited.

**Independently corroborated.** It is byte-identical to a reconstruction made separately, by deleting known
personal entries from an edited Steam copy. Two routes, same bytes.

```
sha256  d0a9ad8eda58e009f356fbccf8202a8e24593299c496840a4fb1f9261e72c7d0
size    9,643 bytes
```

## What the file contains

| | |
|---|---|
| Device elements | 51 |
| `<Alternative>` VID/PID pairs | 85 (79 of them on `GamePad` alone) |
| **Total (VID, PID) tuples** | **136** |

**A device element can hold many VID/PID pairs.** `<GamePad>` carries one primary pair plus 79 alternatives;
`<DualShock4>` carries three in total. This matters to
[Device Provenance](../../../03-data-models/device-provenance.md): a provenance record describes one *entry*,
and an entry owns a *set* of VID/PIDs rather than one.

Frontier ships `.buttonMap` files for some devices but not all — only `VPCPanel` and `VPCThrottle` here,
against 51 device entries. **A missing `.buttonMap` is the normal case, not a fault.**

## How BindForge should use it

**Ship it as an application resource, not as database rows.** A database-seeded list would need a new
migration every time Frontier adds a device, and an applied migration can never be edited — the numbers would
accumulate forever, each script correcting the last. A resource file is a swap, and it diffs in git.

**Treat "absent from the reference" as `unknown`, not as `user_preexisting`.** This copy will go stale:
Frontier adds hardware and BindForge will not always ship an update first. Under a two-state rule, a device
Frontier added after this capture is recorded as the player's, and BindForge would then offer to rename or
clear something it does not own. A third state costs nothing and fails safely — do not claim it, do not touch
it.

## Maintenance

Re-capture when Frontier adds device support. Comparing the shipped `.binds` presets across two installations
of different vintages found them identical, which suggests `ControlSchemes` changes rarely — but that is an
observation from one machine, not a guarantee. See
[testing-required.md](../../../00-overview/testing-required.md) item 10, which captures all four file domains
across a game update.

**Do not edit these files.** They are evidence. Their value is being exactly what Frontier shipped.
