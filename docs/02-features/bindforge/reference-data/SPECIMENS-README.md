# Specimen Files — What Each One Proves

**Moved here 2026-09-09** from the project root, where they were sitting loose. These are real Elite Dangerous
files, not examples written for the documentation, and several settled questions that had been open for
months. Keep them: a claim in the specs is only as good as the file it came from.

---

## `.binds` specimens — format evolution

| File | Version | Elements | Devices referenced |
|---|---|---|---|
| `Custom.3.0.binds` | `3.0` | 401 | Keyboard, Mouse |
| `DualVirpilDawnTreader.4.1.binds` | `4.1` | 486 | **hex** — `3344C3F3`, `334403F3`, `15320244` — plus `vJoy`, `T-Rudder` |
| `Custom.4.2.binds` | `4.2` | 515 | `SaitekX56Joystick`, `SaitekX56Throttle` — **Frontier built-ins, by name** |
| `Custom.4.2 (2).binds` | `4.2` | 515 | Keyboard, Mouse only |
| `20250624_003828_...binds.backup` | `4.2` | — | **hex** — `334483F3`, `334443F4`, `15320244` |

*Three byte-identical copies of that backup existed (same SHA, 67,193 bytes); two were deleted 2026-09-09
and one kept. Nothing was lost — they differed only in filename.*

### What they settled

**Device naming — [testing item 9](../../../00-overview/testing-required.md).** `Custom.4.2.binds` names
Frontier built-ins (`SaitekX56Joystick`); the live profile names user-defined aliases (`RVWAP`, `LVWAP`); the
2024 and 2025 files name the *same two devices* in **hex**, before their `DeviceMappings.xml` entries existed.
One commander's files, before and after — which is the migration Alias Designer performs, documented by
accident.

**VID+PID order — [testing item 2](../../../00-overview/testing-required.md).** Every Virpil hex value begins
`3344`, which is VIRPIL's vendor ID; the Razer device reads `15320244`, VID `1532` first. Vendor half leads,
confirmed rather than asserted.

**VID/PID is not stable.** Two devices, six PIDs across three snapshots — `C3F3`/`03F3` (2024-08),
`83F3`/`43F4` (2025-05), `83F4`/`03F5` (current). The Razer device and the two Frontier-recognised devices
never move in the same files. See
[VID/PID Is Not a Stable Identity](../alias-designer.md#vidpid-is-not-a-stable-identity).

**The format is strictly additive.** 3.0 ⊂ 4.1 ⊂ 4.2, with **zero elements removed** at either step — 85
added in 4.1, 29 in 4.2. A parser written for the newest version reads every older one.

---

## `StartPreset` specimens — two generations, and a split preset in the wild

**`StartPreset.start`** — no version number, and a **single line**:

```
Custom
```

The pre-Odyssey generation: one preset for everything, before the file grew to four sections.

**`StartPreset.4.start`** — four lines, and **not all the same**:

```
KeyboardMouseOnly     ← General
Custom                ← Ship
Custom                ← SRV
Custom                ← On Foot
```

**This is a real split-preset configuration.** [Preset Editor](../preset-editor.md) describes the split
preset as "unusual but valid" and supports it on that basis; this is a specimen of one, with General on a
different preset from the other three. It also confirms the four lines are General / Ship / SRV / On Foot in
that fixed order.

---

## Controller reports — `playstationcontroller{black,blue}.txt`

USB Device Tree Viewer output for two Sony controllers, captured 2026-09-06.

| | VID | PID | Enumerates as |
|---|---|---|---|
| DualShock 4 v1 (black) | `054C` | `05C4` | 1 interface, HID only |
| DualShock 4 v2 (blue) | `054C` | `09CC` | 4 interfaces, composite — includes Audio |

**What they settled:**

**Hat switches are not booleans.** The HID report descriptor shows a hat as **one 4-bit field**, logical `0–7`
mapping to 0°–315°, with a null state for centred. The boolean-per-direction model everything is built on is a
*translation*, not a reading. That closed a question open since the port.

**One device entry can cover several controllers.** Both match Frontier's single `<DualShock4>` element
through its `<Alternative>` pairs — which is why the provenance key could not be `(install_id, vid, pid)`.

**There is no device-unique identifier here.** Both report `iSerialNumber 0x00`. Their Windows instance IDs
(`8&1F31A475&0&4`, `...&0&3`) share the hub portion and differ only in the trailing port number, so even the
OS identity is port-derived for a serial-less device.

**A DualShock 4 has exactly six axes** — X, Y, Z, Rx, Ry, Rz. `ButtonInputMapper.axisToBindsToken` throws at
index ≥ 6, so an ordinary gamepad sits exactly on that ceiling.

---

## The other files here

`FrontierStock-DeviceMappings.xml`, `VPCPanel.buttonMap` and `VPCThrottle.buttonMap` are Frontier's own
shipped files, captured separately — see [FrontierStock-README.md](FrontierStock-README.md).

`ActionCatalog.json`, `BindForge_Binding_Zone_Map.xlsx` and `BindForge_ConsolidatedActionTable.xlsx` are the
seed data BindForge is built from, not specimens.
