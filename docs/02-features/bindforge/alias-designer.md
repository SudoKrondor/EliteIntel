# BindForge — Alias Designer

**Purpose:** manage `DeviceMappings.xml` and `.buttonMap` files, giving connected controllers friendly names shown both in BindForge's own Bind Editor and in the game's own controls UI.

## Structure

A single flat section with no sub-tabs: a device list, with the editor expanding inline beneath whichever device is open.

Earlier drafts moved per-installation control into a separate "Installation Mirroring" screen, first inside Alias Designer and later inside [File Manager](file-manager.md). **Both are gone.** Which installations hold an entry for a device is a property *of that device*, so it belongs on the device's own row rather than on a screen the user has to go and find. See [Per-Installation Editing](#per-installation-editing).

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
| **Windows name** | what the hardware reports through the Device Service |
| **VID / PID** | the hardware |
| **Status** | `ATTACHED` or `MISSING` — whether the hardware is present right now |
| **Installations** | one in-row tab per *detected* installation |

The first three describe the hardware and are true no matter how many installations exist. **Everything else
is per installation** — the alias, whether an entry exists, whether a `.buttonMap` exists — and none of it can
honestly be reduced to a single cell. Two installations can disagree, and a list that averages them is lying.
So those facts live on the in-row installation tabs, and the row itself carries no alias column.

Each in-row tab shows the installation name plus one marker: **M** when that installation is mirrored, or
*not added* when it holds no entry for this device. Clicking a tab opens the device on that installation.

### Built-in Devices

Frontier's shipped default entries. Read-only; no Clear or Edit.

> **Open problem — this split is not derivable from the file.** `DeviceMappings.xml` is a flat list of device
> elements with nothing marking which are Frontier's and which the player added. Any list built by reading a
> real install will mix them. Resolving this needs a shipped reference copy of Frontier's stock file plus a
> provenance record — see [Device Provenance](../../03-data-models/device-provenance.md).

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

**The cause is firmware, not the commander.** VIRPIL's older *VPC Configuration Tool* exposed the PID as
a user-settable field; a firmware update between the first two snapshots changed it, and the newer *VPC
Configurator* forced another firmware change and **removed the ability to set it at all**. So the
commander cannot pin it, cannot predict it, and finds out afterwards.

### Why this matters more than a renumbering

**The failure is silent.** The `DeviceMappings.xml` entry still exists. `.binds` still says `RVWAP`. The
game simply finds no device matching that entry, so the bindings do nothing, with no error and nothing
to search for. A commander experiences it as *"my stick stopped working after an update."*

**And an aliased device is the recoverable case.** Because `.binds` references the alias rather than the
hex, the repair is one PID field per installation and every binding survives. A device with **no** entry
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

**Before the fact — devices referenced by hex.** A commander whose `.binds` names devices by raw hex is
one firmware update away from losing that layout and has no way to know it. BindForge can see the state
and count the exposure: *"Two attached devices are referenced by hardware ID rather than by name. 131
bindings depend on those IDs and would not survive a firmware update. Create aliases?"*

**Open:** how confident the after-the-fact match has to be before offering it. Same VID and an unmatched
device is suggestive, not proof — the commander could equally have unplugged one stick and plugged in
another from the same vendor. Suggest, never apply silently.

## Automatic Device Registration

Any controller connected to the system — whether already connected when BindForge starts, or plugged in later during a session (hot-plug) — is automatically added to the My Devices working copy with no user action required. This applies the same "detect and inform, don't ask" principle already used for [First Run](#first-run-and-hot-plug) below, extended to individual devices as they're seen.

- **Default alias:** the name the Device Service already reports for the hardware (e.g., "VPC Throttle"), used as-is until the user renames it.
- **VID/PID:** filled in immediately from the connected hardware, same as any other device entry.
- **No `.buttonMap` stub is created.** A button-map file is only created once the user actually enters button/axis names for that device — see [Device Editor](#device-editor) below.
- **This only touches the working copy, never a live game installation** — the same separation that already governs every other Alias Designer edit (see [Overview — Managed File Domains](overview.md#managed-file-domains)). Nothing about automatic registration writes to a game install; that only happens through File Manager's Installation Mirroring. This means automatic registration is safe to happen at any time.
- **Hot-plugging into BindForge is safe; hot-plugging into a running game is not — those are different things.** BindForge can register a newly-connected controller into its own working copy at any time without risk. Physically connecting or disconnecting a controller while Elite Dangerous itself is running is a separate, game-level risk (it can crash the game) that exists independently of BindForge and is outside BindForge's control — automatic registration does not cause or worsen this; it just means BindForge's own device list stays current regardless of when the user plugs something in.

### Rules automatic registration still needs

Three gaps, all found while working the design through and none of them yet decided:

- **Name sanitisation.** The default alias is the name the Device Service reports, but that name becomes an
  **XML element tag** in `DeviceMappings.xml` and a **filename stem** for `.buttonMap`. SDL-style names like
  `Virpil Controls 20220720` are neither a valid XML tag nor a safe filename. A sanitisation rule is required
  and does not exist.
- **Collision with Frontier's built-ins.** The alias-uniqueness rule in [Device Editor](#device-editor)
  forbids colliding with a built-in entry — but SDL will happily report `T-Rudder` for a device Frontier
  already ships a `<T-Rudder>` entry for. Automatic registration would violate BindForge's own validation rule
  for exactly the devices most likely to be recognised.
- **What reaches the game's file.** Registration only touches the draft, so nothing lands in a real install
  until **Apply to Game Installs** runs. **Settled 2026-09-06: applying pushes only what changed**, never the
  whole set. Pushing everything accumulates entries for hardware plugged in once, years ago, and rewrites
  rows no one touched; since both files are purely cosmetic, an entry for a device the player never named
  carries no benefit. A narrow write is also a smaller thing to get wrong, and a smaller diff to show the
  player when a merge has to be explained.

## Device Editor

The editor **expands inline beneath the device's row**, one device open at a time. Opening another closes the
first; clicking the open row closes it. There is no separate editor panel and no "no device selected" state —
a collapsed list is the resting state.

### Per-Installation Editing

`DeviceMappings.xml` and `.buttonMap` are genuinely duplicated per storefront installation, so **the editor
edits one installation at a time.** The expansion carries a tab per detected installation, and switching tabs
switches the whole record beneath it: alias, axis names, button names.

The header names the installation and shows its **folder path**, read-only. That path is what makes a tab
switch unambiguous — it is not merely a different data set, it is a different folder on disk.

**MIRROR**, in the action row, is what keeps the common case cheap:

- **Mirrored installations share one definition.** Edit any one of them and every other mirrored installation
  follows. Same hardware, same names, one edit — which is what nearly every player wants.
- **Turn MIRROR off** and that installation is edited on its own. This is the case that earlier designs could
  not express at all: they forced one definition on every installation.
- Turning MIRROR back on **adopts** whatever the other mirrored installations already say.

Together with whether an entry exists at all, that gives three per-installation states: *no entry*, *entry
edited independently*, and *entry shared with the other mirrored installations*.

### Fields and rules

- **VID/PID are always read-only**, always derived from the physical hardware — the game attaches a device by
  VID/PID, so hand-editing this would break the association. They are device-level, identical on every tab.
- **Edit Mode** activates the moment any field changes. A persistent label reminds the user this saves to the
  working copy only.
- **Alias name validation:** 3–50 characters; letters, numbers, spaces, and a specific set of punctuation
  characters only; no period; must not collide with any existing entry, including Frontier's built-ins.
- **The alias must be confirmed or changed before button/axis naming unlocks.** For an automatically-registered
  device still carrying its default hardware-reported alias, the Button & Axis Names tabs stay locked until the
  user has explicitly confirmed or edited the alias — a deliberate minimum touchpoint, since it ensures a device
  never gets fully configured under an alias the user never actually looked at. Confirmation does **not** reach
  the working copy until Save, so Discard can undo it.
- **Button and axis names** are edited in Axis/Buttons sub-tabs. The UI sizes dynamically to the device's real
  axis and button count — never hardcoded; a device with three axes shows three. A hard 18-character limit
  applies to every button/axis friendly name, confirmed by direct in-game testing. The alias field has its own
  rule (3–50 characters) and no in-game display slot, so truncation is not a concern there.
- **Live highlighting:** pressing a button or moving an axis on the open device highlights the corresponding
  row, letting the user identify raw input codes by feel. With the device list itself being the selection, there
  is no separate "which controller am I pressing" step to get wrong.

### Actions, and what each one reaches

| Action | Scope | Touches a game installation? |
|---|---|---|
| **SAVE** | the open installation's record | **No** — draft only |
| **DISCARD** | the open installation's record | No |
| **CLEAR** | the open installation's record | No — see below |
| **MIRROR** | the open installation | No |
| **APPLY TO GAME INSTALLS** | every installation with an entry | **Yes** |

**SAVE is deliberately the safe one.** It is the button people press by habit, so it must never be the button
that writes into a live game installation. Applying is a separate, deliberate action at the bottom of the
screen.

**CLEAR, not Delete.** A device cannot be removed from the list while its hardware is attached — it would
simply reappear, because the list is driven by what is plugged in. What CLEAR removes is *this installation's*
`DeviceMappings.xml` entry and its matching `.buttonMap`. The device stays listed, showing *not added* for that
installation. Clearing every installation returns the device to the state it had before it was ever configured.

This also replaces the old "delete and recreate to fix a wrong VID/PID" repair path, which never worked as
written: automatic registration would re-add a connected device immediately.

## Renaming a Device

A rename is **not** a single-field edit. The alias is the primary key of a three-link chain:

```
alias → <element tag> in DeviceMappings.xml → <alias>.buttonMap filename → possibly Device= in .binds
```

So renaming has to be handled as one transaction:

1. Rename the element tag in `DeviceMappings.xml`.
2. **Rename** the `.buttonMap` file — never create a second one. An orphaned `OldName.buttonMap` sits in the
   game's install folder where nothing will ever clean it up.
3. Scan `.binds` for `Device="<old name>"` and rewrite or refuse. This is the dangerous one: `.binds` can
   reference a device *by name* as well as by VID/PID hex, so a rename can silently orphan bindings.

**Why the alias-confirm gate matters more than it looks.** Requiring the alias to be settled before button and
axis naming unlocks means a `.buttonMap` is only ever created under a name the player has already looked at.
A rename *before* any file exists is free. This is also why no `.buttonMap` stub is created at registration
time — creating one early would reintroduce exactly the rename problem the sequencing avoids.

**Blocked on testing:** whether `.binds` uses the name or the hex form for devices defined in
`DeviceMappings.xml` is not settled — the format reference shows both. See
[testing-required.md](../../00-overview/testing-required.md).

## First Run and Hot-Plug

If no draft exists yet, BindForge auto-imports `DeviceMappings.xml` and all `.buttonMap` files from every
detected installation, with no prompt — a status message informs the user this is happening. If no
installation is detected, the draft starts empty and any connected devices are added via
[Automatic Device Registration](#automatic-device-registration) above.

**Settled 2026-09-06 — first import takes the player's files exactly as they are.** No mirroring, no
reconciliation, no adopting one installation's entry over another's. Where two installations hold
different entries for the same device, both are preserved as they were found and the device is left
**unmirrored**; BindForge does not decide which one was meant. Mirroring is one click away afterwards, and
is the player's call to make once they can see both.

The reasoning is that a first run has no way to tell a deliberate difference from an accidental one, and
the failure modes are not symmetric: preserving a difference that turns out to be accidental costs one
click, while flattening a difference that turns out to be deliberate destroys work the player cannot
recover from BindForge.

## Exit Prompt

If the user navigates away from BindForge (or exits Elite-Intel) with unsaved Alias Designer changes, the
same two-option dialog described in [Overview — Unsaved Work on Exit](overview.md#unsaved-work-on-exit)
interrupts: **Save** or **Discard**. Neither touches a game installation.

## Deferred

- **Recalibrate** — a feature to fix axis center-point drift during live highlighting. Out of scope for this iteration.
- **Duplicate-VID/PID handling — dropped from scope 2026-09-07; nothing is designed and nothing needs to be.**
  The existing `DeviceDuplicateWarningEvent` tells the player, and the alias-uniqueness rule below rejects the
  second automatic registration on its own. Retained for the reasoning: Two connected devices of the identical model genuinely can report the same VID and PID (this is normal USB behavior, not an edge case) — combined with [Automatic Device Registration](#automatic-device-registration), both would attempt to auto-register under the same default hardware-reported alias, which the alias-uniqueness rule would reject. Neither `.binds` nor `DeviceMappings.xml` has any field that could distinguish two entries sharing a VID/PID, and the game itself keys on VID/PID, so it could not act on a distinction even if BindForge drew one. That is why nothing is designed here rather than deferred: there is no design that would help. See [conflicts-and-open-questions.md](../../00-overview/conflicts-and-open-questions.md).

## Design Considerations — Closed

**The master-copy question is closed, and was already closed when this pointer was written.** Device
configuration is edited against a **single draft** pushed to installations on Apply. There is no two-tier
master-plus-shadow model and no persistent master copy competing with the live game file for the role of
source of truth — see
[Conflict 3.6](../../00-overview/conflicts-and-open-questions.md#36-working-copy-model--resolved-neither-original-option--a-third-sharper-model),
resolved before the port. Confirmed again 2026-09-06: the idea is an inherited artefact of the existing
Elite-Intel bind editor, not a live option.
