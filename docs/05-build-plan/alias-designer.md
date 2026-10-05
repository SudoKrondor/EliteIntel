# Alias Designer

**Naming controllers, and keeping every installation's device files identical.** The tab exists and shows real
data; most of what the spec describes is still ahead.

## Spec

- [alias-designer.md](../02-features/bindforge/alias-designer.md) — **read end to end at the start of every
  session.** It is the screen's definition, and building from the slice's name instead of the page is how this
  tab went wrong the first time — see `CLAUDE.md`.
- [device-provenance.md](../03-data-models/device-provenance.md) — the three tables, and why there are three.
- [EliteDangerous-DeviceMappings-ButtonMap.md](../02-features/bindforge/domain-knowledge/EliteDangerous-DeviceMappings-ButtonMap.md)
  — the file formats, and what has been measured on a live game.
- [ui-component-map.md — Alias Designer](../02-features/bindforge/ui-component-map.md#alias-designer).

## Built

| What | Where |
|---|---|
| The tab | `ui.screen.bindings.AliasDesignerPanel` — My Devices above the divergence list, in `BindingsTabPanel` |
| My Devices — four of its five columns | `bindforge.devices.MyDeviceList` (the union, a pure function), `MyDevice`. Built-ins show `BUILT-IN` in the Alias column |
| Hardware VID/PID | `bindforge.devices.DeviceIdentities` — from SDL's GUID; confirmed on four controllers, real GUIDs in its test |
| Frontier's shipped list | `bindforge.devicefiles.FrontierStockDevices`, resource `bindforge/FrontierStock-DeviceMappings.xml` |
| Divergence list | `DeviceDivergenceScanner`, `DeviceDivergence` (red if `.binds` names the device, yellow otherwise), `BindsDeviceReferences`, `ButtonMapAudit` |
| What each installation holds | `InstallationDeviceScanner`, run on every refresh; rows in `bindforge_device_installs` |
| Game Install Locations screen | `ui.screen.bindings.GameInstallLocationsPanel` — in Binding Management, where the spec puts it in File Manager |
| Master store | see [core.md](core.md) |

**The divergence list compares installations with each other.** The spec compares each one **with the master**.
That is A3.

## Settled — and where it is recorded

- **The list is the union of live hardware and file entries.** `DeviceMappings.xml` supplies names, never
  existence. [Spec, My Devices](../02-features/bindforge/alias-designer.md#my-devices)
- **The Installations markers wait for first setup.** `M`, *not added* and the divergence colour are all defined
  against a master record. With the master empty, an installation holding `LVWAP` is in none of those states,
  and the spec does not define one. Building markers first would mean inventing it.
- **"Device service is not running" is a row in the table**, not just a log line — an empty list because nothing
  is plugged in and one because nothing is looking are different states.
- **Duplicate-VID/PID handling is out of scope.** [Spec, Deferred](../02-features/bindforge/alias-designer.md#deferred)
- **DirectInput instance GUIDs are out of scope** — the game keys on VID/PID, and no file could carry more.
- **The game ignores `DeviceMappings.xml` formatting; it does not ignore missing children.**
  [Domain doc §1.2c](../02-features/bindforge/domain-knowledge/EliteDangerous-DeviceMappings-ButtonMap.md#12c-the-game-parses-it-as-xml-and-does-not-reformat-it--measured-2026-10-03)

## Known issues — fix when a slice passes them

- **[alias-designer.md](../02-features/bindforge/alias-designer.md) says Frontier ships "139 device elements".**
  It is 51 elements carrying 136 VID/PID pairs — the README of the stock capture, and the tests, both say so.
- **`AliasDesignerPanel`'s class javadoc says the panel is "not in the tab bar".** It is.
- **`ButtonInputMapper.axisToBindsToken` throws for axis index 6 and above.** Generating labels "sized to what
  the device reports" will hit it on any controller with seven or more axes.
- **Still open from the first code review:**
  - **#7 — paths are stored as typed**, so `E:\SteamLibrary\…` and `e:\steamlibrary\…\` become two rows for one
    installation. Discussed 2026-09-30 and leaning towards deleting **relocate** altogether, since Remove then
    Add covers every case and nothing yet depends on an installation keeping its id. **Undecided — ask Alan.**
  - **#12 — `WindowsGameInstallationProvider` reads `InstallPath` under `HKCU\Software\Valve\Steam`**, where the
    value is named `SteamPath`. The two `HKLM` lookups work, so detection works; the `HKCU` fallback never finds
    anything.

## Boundaries

- **Owns** `bindforge.devices`, `bindforge.devicefiles`, `bindforge.install` and `ui.screen.bindings.AliasDesignerPanel`.
- **Writes through Core, never around it.** No slice here writes a game file itself.
- **The Game Install Locations screen belongs to File Manager** — fix bugs in it here if a slice needs to, but its
  move into File Manager is that section's work.

## Slices

| # | Slice | Needs | Status |
|---|---|---|---|
| A1 | **First setup** — turn what the installations hold into the master: per element, the four cases, a name conflict routed into the rename, a labelled backup and Edit History before writing | Core C1, C2 (both done) | ready |
| A2 | **`.buttonMap` label merge** — one row per input that disagrees, nothing else | A1 | waiting |
| A3 | **Divergence against the master** — replace installation-against-installation | A1 | waiting |
| A4 | **Installations markers** — `M`, *not added*, severity colour | A1 | waiting |
| A5 | **Built-in Devices tab** — Frontier's list, read-only | — | ready |
| A6 | **Onboarding** — the default-name rule and alias validation | Core C1 (done) | ready |
| A7 | **Automatic registration and hot-plug** — `DeviceBus` connect and disconnect | A6 | waiting |
| A8 | **`.buttonMap` generation at Elite-Intel startup** — connected, named, none on disk | Core C1 (done) | ready — needs a create-only write, see core.md |
| A9 | **Device Editor** — inline expansion, install strip, labels, live highlighting | C2 (done) | ready |
| A10 | **Actions** — SAVE, DISCARD, CLEAR, RESET LABELS, APPLY, reported per installation | A9 | waiting |
| A11 | **Rename** — every installation first, `.binds` last, all or nothing | A10 | waiting |
| A12 | **When hardware changes** — a missing device, the identity question, retarget, one missing device at a time | A11 | waiting |
| A13 | **The missing-controller warning**, once per run | — | ready |

**A5 and A13 need nothing from Core** — either is a good slice for a session that wants something self-contained
while Core is in progress.

## Hand-off notes

*Newest first. Two or three lines each: what was decided and where it is recorded, what was found, what the
next slice needs.*

- **2026-10-03 — brief written.** Built so far: install detection, device-file parsing, the orphan check, severity,
  the divergence list, the master store, Frontier's stock reference, the hardware VID/PID derivation, My Devices
  and the installation scan.
