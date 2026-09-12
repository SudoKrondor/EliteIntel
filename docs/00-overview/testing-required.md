# Testing Required

A consolidated checklist of everything in this documentation package that's confirmed as a real, standing question — not a design decision — waiting on hands-on testing against real hardware and/or a real Elite Dangerous install. Each item links back to its full write-up in [conflicts-and-open-questions.md](conflicts-and-open-questions.md) and the relevant plugin doc. Nothing here blocks writing more documentation; these are implementation/verification tasks for later.

---

## BindForge

**1. ~~Duplicate-VID/PID controllers~~ — DROPPED FROM SCOPE 2026-09-07. No longer a testing item.**
BindForge designs nothing for two identical controllers; the existing duplicate warning and the
alias-uniqueness rule already give defined behaviour, and Elite Dangerous cannot act on the distinction
regardless. See [Conflict 3.7](conflicts-and-open-questions.md#37-duplicate-vidpid-devices--resolved-confirmed-real-but-the-fix-needs-hands-on-testing-not-a-design-decision).
The two DualShock 4 controllers acquired 2026-09-06 would not have covered it anyway — their PIDs differ
(`05C4` vs `09CC`) — though they did settle the neighbouring case of two devices sharing one entry, see
item 9.

*Original question, for the record:* Connect two controllers of the identical model (VID and PID both match) and determine what Elite Dangerous actually does with two `DeviceMappings.xml` entries pointing at the same VID/PID: does it support both usefully, or silently conflate them? Neither `.binds` nor `DeviceMappings.xml` has any field that could distinguish two such entries, so this determines whether BindForge's auto-registration/disambiguation behavior (see [Alias Designer — Automatic Device Registration](../02-features/bindforge/alias-designer.md#automatic-device-registration)) is even useful at the file-format level, not just how to present it.
→ [Conflict 3.7](conflicts-and-open-questions.md#37-duplicate-vidpid-devices--resolved-confirmed-real-but-the-fix-needs-hands-on-testing-not-a-design-decision)

**2. ~~VID/PID-to-device-identifier derivation~~ — half answered 2026-09-08; the button/axis offset is
still open.** The **VID+PID order is confirmed**: a 2025-05 specimen carries `334483F3` and `334443F4`
for two VIRPIL devices, and `3344` is VIRPIL's vendor ID, so the vendor half leads; a Razer device in the
same file reads `15320244`. BindForge should still **read both orders** — fixed-width halves make it one
extra comparison — and never writes hex at all. What remains is the numbering offset only.

*Original wording:* confirm the VID+PID hex-concatenation rule for device identifiers, and confirm the 0-based (live input) vs. 1-based (`Joy_N` in `.binds`) numbering offset, both stated confidently in some source documents but flagged unverified in others with no bridging confirmation.
→ [Conflict 3.15](conflicts-and-open-questions.md#315-vidpid-device-id-derivation-and-the-buttonaxis-index-off-by-one--resolved-logged-as-testing-required) · [Binding Schema](../03-data-models/binding-schema.md#live-input-to-file-format-numbering-offset-needs-independent-testing)

**3. Cross-section conflict isolation** — confirm whether BindForge's four top-level sections (General/Ship/SRV/On Foot) are genuinely conflict-isolated from each other, or whether there's a real cross-section conflict (an unconfirmed observation during testing suggested a possible Ship↔SRV collision). This determines whether a cross-section conflict matrix is needed in addition to the four per-section matrices that already exist — the single most consequential open empirical question in BindForge's conflict-detection design.
→ [Conflict 3.16](conflicts-and-open-questions.md#316-cross-section-conflict-isolation-may-be-wrong--merged-into-38) · [Bind Editor — Shared Conflict Detection](../02-features/bindforge/bind-editor.md#how-the-game-actually-resolves-conflicts-confirmed-by-direct-in-game-testing)

**4. UI-action-vs-ship-action behavioral safety** — confirmed assignable to the same key with no in-game warning, but whether it's actually safe when both are live simultaneously (not just whether the assignment is *allowed*) needs a second, behavior-focused round of testing.
→ [Bind Editor — Shared Conflict Detection](../02-features/bindforge/bind-editor.md#how-the-game-actually-resolves-conflicts-confirmed-by-direct-in-game-testing)

**5. On Foot conflict coverage** — only three On Foot subgroups have been identified/tested so far; real on-foot play likely has more untested contexts (ship-interior actions while on foot, SRV boarding, taxi/Apex travel).
→ [BindForge Roadmap](../02-features/bindforge/roadmap.md)

**6. Consolidated bind data table — done, but still carries unresolved spot-checks.** [`BindForge_ConsolidatedActionTable.xlsx`](../02-features/bindforge/reference-data/BindForge_ConsolidatedActionTable.xlsx) now merges all three sources with no data discarded; the reconciliation itself is complete. What's still testing-required: 62 rows where `ActionCatalog.json` disagreed with the other two sources on an action's name — spot-check any still-uncertain ones against real screenshots the same way "UI Panel Select" was confirmed, per the table's own Row Notes.
→ [BindForge Roadmap](../02-features/bindforge/roadmap.md#formerly-needed-before-v1-can-be-built--now-resolved)

**7. First-Time Startup's file copy** — confirm whether a `ControlSchemes` factory preset file can be copied byte-for-byte into the player's bindings folder as a valid starting `.binds` file, or whether the game's own first-customization save does something to the file beyond a plain copy (e.g. a header/version rewrite) that a plain copy would miss.
→ [BindForge Overview — First-Time Startup](../02-features/bindforge/overview.md#first-time-startup)

**9. ~~Does `.binds` reference a `DeviceMappings.xml` device by name or by hex?~~ ANSWERED 2026-09-08 —
both, and the rule is now known.** *No longer a testing item.*

> `.binds` names a device by its `DeviceMappings.xml` element name, matched on VID/PID. With no matching
> entry it falls back to VID+PID hex.

Confirmed from five specimens spanning 2024-08 to 2026-09, including the same commander's file before and
after adding entries for the same two devices — the two VIRPIL sticks appear as `334483F3`/`334443F4` in
the 2025 file and as `RVWAP`/`LVWAP` today. See
[§4.0 of the format reference](../02-features/bindforge/domain-knowledge/EliteDangerous-BindsFileFormat.md#40-the-rule-confirmed-2026-09-08).

**Device rename is therefore a multi-file operation** — the element name, every `Device=` in `.binds`
using it, and the `.buttonMap` filename, together. That was the thing this item blocked, and it is now
designable.

*Original question, for the record:* §4.1 of the format reference
lists plain names (`T-Rudder`, `RVWAP`, `LVWAP`) as valid `Device=` values; §4.2 says devices configured through
`DeviceMappings.xml` use the derived 8-hex-character form. Both occur in the wild and the rule for which is
undocumented. **This blocks device rename**, because if `.binds` can hold the name, renaming a device orphans
every binding that used it.

**A discriminating test is now available (2026-09-06).** Two DualShock 4 controllers on the developer's
machine share one `DeviceMappings.xml` entry — `<DualShock4>` — through its `<Alternative>` pairs, while
carrying different PIDs (`05C4` and `09CC`). That is exactly the pair needed: **same name, different hex.**

Bind one action to a button on each controller, then read the resulting `.binds`:

- both slots say `DualShock4` → **by name**, and the two controllers are indistinguishable to the game
- the slots differ (`054C05C4` versus `054C09CC`) → **by hex**, and the `DeviceMappings.xml` name plays no
  part in binding identity

Either answer settles device rename. The first also answers what the game does when one entry covers two
attached devices, which is item 1's question in a form that can actually be run today.
→ [Alias Designer — Renaming a Device](../02-features/bindforge/alias-designer.md#renaming-a-device)

**10. What does a game update actually do to the four file domains?** The next weekly tick is expected to add a
vehicle, which may add bindable actions — a rare scheduled chance to observe all four domains at once with a
baseline captured beforehand. Specifically:
- Does the patcher replace `DeviceMappings.xml` wholesale, and do player-added entries survive? *(This is the
  incident that motivated BindForge, and it also settles the built-in-vs-user provenance question empirically —
  see [Device Provenance](../03-data-models/device-provenance.md).)*
- **Does a new vehicle add a fifth binding section?** `StartPreset.#.start` has exactly four lines — General,
  Ship, SRV, On Foot — and that four-ness is load-bearing across Preset Editor's UI, the four conflict
  matrices, and First-Time Startup's per-section checks. **Lower risk than it first appears:** binding sections
  are *control contexts, not vehicles*. A Ship Launched Vessel such as the Nomad, like a Ship Launched Fighter,
  is flown with **Ship** bindings and needs no section of its own. So a new vehicle of that class should add
  actions, not a section. Still worth confirming, because a genuinely new control context (as On Foot was)
  would be a different matter.
- Are `.buttonMap` files touched at all, or only `DeviceMappings.xml`? An asymmetry there would mean labels
  survive updates while the device registry does not.
- Do new actions appear in `.binds` immediately, or only after the in-game Controls menu is opened?

Method: object-access auditing (SACL) on the install's `ControlSchemes` folder gives the process identity in
Security event 4663; Process Monitor covers a known window. A file-hash baseline of all four domains must be
captured **before** the tick or the after-state is much less informative.

**11. ~~Do two installations on the same machine ever legitimately hold different device entries?~~ ANSWERED
2026-09-06 — yes, demonstrably.** A Steam and an Epic installation on the developer's own machine differ
right now: Steam's `DeviceMappings.xml` carries two hand-added elements, Epic's is Frontier stock. Epic also
holds an `LVWAP.buttonMap` with **no matching element in its own `DeviceMappings.xml`** — a live orphan,
left by an earlier BindForge attempt.

**The mechanism is worth more than the answer.** Epic was reinstalled on 2026-07-01. That reinstall restored
`DeviceMappings.xml` to stock — reverting the edit — while leaving the added `.buttonMap` untouched. A
reinstall reverts the files the manifest ships and ignores files the player added. That is the motivating
incident, observed directly rather than reported: **the edit is lost, the orphan survives.** It also means
orphaned `.buttonMap` files exist in the wild, which
[File Manager's restore scope](../02-features/bindforge/file-manager.md#restore-scope) has to tolerate
rather than treat as corruption.

*Original question, for the record:* The
per-installation model in [Alias Designer](../02-features/bindforge/alias-designer.md#per-installation-editing)
assumes they can, and builds the UI around allowing it. Worth confirming against real multi-storefront setups
whether that ever actually happens in practice, or whether every real user mirrors everything — which would
make mirrored the sensible default rather than a choice.

**12. Does the game tolerate a `DeviceMappings.xml` entry for hardware that is not attached?** Entries outlive
hardware, and BindForge lists and preserves them deliberately. Confirm the game ignores a stale entry rather
than erroring or discarding the file.

## Core Platform

**8. Linux/Proton path resolution — Krondor's, not a BindForge testing item.** *Reassigned 2026-09-06.* Not a one-time check but ongoing, given how much Linux path resolution varies by distro and by how an individual user has their Steam library configured — which is exactly why it is owned by the person running it. BindForge builds and tests Windows storefront detection; Krondor makes the edits his platform needs. Listed here so the dependency stays visible, not as work waiting on Alan. The journal path is confirmed against independent precedent (EDMarketConnector); the bindings-folder path is inferred from the same structure but not independently confirmed.
→ [Conflict 1.5](conflicts-and-open-questions.md#15-linuxproton-bindings-folder-path--resolved-reframed-as-ongoing-testing-feedback-not-a-one-time-check) · [Path Resolution](../01-host-integration/elite-intel-platform-map.md#path-resolution)
