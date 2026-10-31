# Data Model — Device Provenance

**Rewritten 2026-10-28 (Storage S4b, Alan).** BindForge keeps **no database record of devices**. The device master,
the draft, the pending removals, the per-installation rows and the waiting hardware-id moves were dropped by
migration `12019`, with `bindforge_installations.missed_write`. *From 2026-10-01 to 2026-10-28 this page described
those tables; [the history](#history--the-tables-that-were) is kept below.*

## What answers each question now

The questions the tables existed to answer are answered from files, as [storage.md](../02-features/bindforge/storage.md)
and [the principles](../02-features/bindforge/principles.md) require: the working copy is the one source of truth.

| Question | Answered by |
|---|---|
| Is this element Frontier's or the user's? | **Frontier's stock list, shipped in the jar** (`FrontierStockDevices`, captured 2026-09-06 — [FrontierStock-README](../02-features/bindforge/reference-data/FrontierStock-README.md)). An element on it is Frontier's; any other element is the user's. |
| Which name, which hardware, which labels does a device have? | **The working copy** — its `DeviceMappings.xml` and `.buttonMap`s — **with the edits not applied on top** (`bindforge.storage.Edits`). The Alias Designer, the identity question and the Bind Editor's controller names and labels (`SavedDeviceLabels`, `CaptureControllers`) all read that one set. |
| Does an installation still hold what BindForge wrote? | **The check** compares each installation's files with the working copy by value ([storage.md](../02-features/bindforge/storage.md) rule 14, Storage S9). Until S9, APPLY leaves alone an installation that differs (`DeviceFilesComparison`). |
| Must a rename also rename a file, or move bindings? | **The edit does it in the working copy** — element, `.buttonMap` and every binds file together ([Renaming a Device](../02-features/bindforge/alias-designer.md#renaming-a-device)). Nothing waits for a later APPLY, so no old name is stored. |
| Bindings still on an old hardware id (A17)? | **Onboarding's FINISH moves them in the same edit** (Storage S4a). Nothing waits for APPLY, so no record is kept. |

## The one table left

**`bindforge_identity_answer`** (migration `12007`, A12/A17) remembers **Not now** and **None of these**, keyed by the
missing controller's name — or its old hardware id, written `VID:PID` — with the set of candidates the question
listed. It holds the user's answer, not a fact about the files, so no file can hold it; it is forgotten when the set
of candidates changes or the missing controller comes back
([alias-designer.md](../02-features/bindforge/alias-designer.md#device-identity-is-the-users-to-confirm--settled-2026-09-12)).

## What still holds from the old reasoning

- **`DeviceMappings.xml` has no built-in/user distinction to read.** It is a flat list of elements; only the shipped
  stock list separates Frontier's from the user's. The user's own `LVWAP` and `RVWAP` sit indistinguishably among
  Frontier's in a real file, and `VPCPanel` and `VPCThrottle`, which look like personal additions, are Frontier's.
- **A device element owns a set of VID/PID pairs**, not one: Frontier's file holds 51 elements carrying 136 pairs,
  `<GamePad>` alone 80. An element is named by its tag, which is also its `.buttonMap` file stem and what `.binds`
  names it by.
- **Two controllers can resolve to one element** — both DualShock 4 revisions match `<DualShock4>` through its
  `<Alternative>` pairs — and neither reports a serial number, so there is no stable hardware identity to lean on.
  That is why BindForge asks rather than guesses ([VID/PID Is Not a Stable Identity](../02-features/bindforge/alias-designer.md#vidpid-is-not-a-stable-identity)).
- **Hex case is not a convention in Frontier's file.** Across the 136 pairs, upper and lower case are mixed, even
  within one element (`<DualShock4>`: VID `054C`, PID `05c4`). Every VID/PID comparison folds case, and BindForge
  never rewrites Frontier's entries to "normalise" them (`DeviceEntry.HardwareId` folds case).
- **A stale stock list cannot tell a newer Frontier element from the user's.** A controller Frontier added after the
  2026-09-06 capture reads as the user's until the list is re-captured. The `unknown` provenance value was the old
  guard for this; it went with the table. The list's capture date is shown on the Built-in Devices tab.

## Migration band

**BindForge writes in `12000–12499`**; commander and galaxy work in `12500–12999` (Krondor's <!-- terminology-ok: names the real db-migration/commander/ tree and Krondor's per-commander work -->
[proposal §4](../multi-install-proposal.md#4-migrations-one-tree-per-file), accepted 2026-09-24) - until 2026-10-31,
when `V1.2`'s own `12013`–`12018` moved BindForge's unshipped `12013` and `12014` to `12019` and `12020` in the merge
([file-manager.md](../02-features/bindforge/file-manager.md), Alan, 2026-10-31). A shipped migration number is never
reused or renamed, and an applied migration is never edited — a new numbered script is added instead, as
`12019` drops the tables `12002`–`12004` and `12008` created.

## History — the tables that were

*Kept for the reasoning the specs cite. None of these exist after migration `12019`.*

- **`12002`** (2026-10-01): `bindforge_device_master` (the user's element set, with `alias_confirmed` and
  `previous_name`), `bindforge_device_labels` (its `.buttonMap` content) and `bindforge_device_installs` — one row per
  device per installation, keyed `(install_id, device_name)`, with a `provenance` of `frontier`, `user_preexisting`,
  `bindforge` or `unknown`. The argument for per-installation rows was that *the master records intent, these rows
  record reality, and drift is the difference*. The check now finds drift by comparing the files themselves with the
  working copy, which needs no stored copy of reality.
- **`12003`** (2026-10-04): the device draft — saved edits waiting for Apply. Replaced by the edits held in memory
  (Storage S4a).
- **`12004`**: pending removals — devices an Apply took out of the master whose entries were still in an
  installation. Replaced by whole-file APPLY, which removes what the working copy lacks.
- **`12006`**: `missed_write`, which marked an installation for catch-up (A14). Catch-up was removed in S4a.
- **`12008`** (2026-10-17, A17): `bindforge_hardware_id_move`, bindings waiting to move from an old hardware id.
  Replaced by the move in onboarding's edit.

**The secondary marker** — a `<!-- BindForge -->` comment in the files — was considered as redundancy for the
database record and never built. With no record to back up, there is nothing for it to re-derive.
