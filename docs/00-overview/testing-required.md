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

*Original question, for the record:* Connect two controllers of the identical model (VID and PID both match) and determine what Elite Dangerous actually does with two `DeviceMappings.xml` entries pointing at the same VID/PID: does it support both usefully, or silently conflate them? Neither `.binds` nor `DeviceMappings.xml` has any field that could distinguish two such entries, so this determines whether BindForge's auto-registration/disambiguation behaviour (see [Alias Designer — Automatic Device Registration](../02-features/bindforge/alias-designer.md#automatic-device-registration)) is even useful at the file-format level, not just how to present it.
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

**4. UI-action-vs-ship-action behavioural safety** — confirmed assignable to the same key with no in-game warning, but whether it's actually safe when both are live simultaneously (not just whether the assignment is *allowed*) needs a second, behaviour-focused round of testing.
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

**13. Does the game honour hex device references once a `DeviceMappings.xml` entry exists for that
device?** `.binds` names a device by its entry once one exists, and by VID+PID hex until then. What is untested
is the moment in between: add an entry for a device an existing `.binds` references by hex, start the game, and
see whether those bindings still work — and whether the game rewrites them to the name on its next save.
**The only evidence points the other way:** when Alan added `RVWAP` and `LVWAP`, the hex references were
replaced by hand. So onboarding writes the entry and the `.binds` rewrite together, in one Apply. If the game
turns out to cope on its own, the entry could be written straight away like any other name.
→ [Alias Designer — What onboarding writes, and when](../02-features/bindforge/alias-designer.md#what-onboarding-writes-and-when)

**14. What are the legal values of each settings enum?** *Added 2026-09-16, and it gates part of the
release.* 31 of a `.binds` file's 93 [standalone settings](../03-data-models/binding-schema.md#binding-element--three-distinct-kinds)
hold an enum token — `Bindings_MouseYaw`, `mute_toggle`, `FocusOption_Nothing` — and **a file contains the one
token it holds, never the set it was chosen from.** Across all five specimens, `YawToRollMode` reads
`Bindings_YawIntoRollNone` every time; whatever its on-states are called, we have never seen one. The
mouse-axis family is the exception — sibling elements between them show `Bindings_MouseYaw`,
`Bindings_MouseRoll`, `Bindings_MousePitch` and `Bindings_MousePitchInverted`.

**The test, and why reading the screen is not enough (revised 2026-09-16).** *UI Focus Mode* reads
**DIRECTION** on the options screen and is stored as `Bindings_FocusModeHold` — so the label does not give you
the token in either direction. **Each option has to be selected, saved, and the file read back**, one at a
time, recording the label and the token together. Roughly twenty minutes of clicking for the whole set,
and it is the difference between a working dropdown and BindForge writing a value the game discards.

**Three of them are not enums at all in the file.** *Headlook Button Increments* is a four-choice
dropdown (CONTINUOUS, SMALL, MEDIUM, LARGE) stored as `HeadlookIncrement` = `0.00000000`; `ThrottleIncrement`
and `BuggyThrottleIncrement` share the shape. Nothing in the file marks them as enumerated, so they need
the same walk — recording which number each label writes.

**Most of this was answered on 2026-09-16 without touching the game** — `ControlSchemes\Help.txt` ships with
Elite and lists the legal values for `YawToRollMode` (None, Time, LowRoll), `ThrottleRange` (`""` and
`Bindings_ThrottleForewardOnly`), `UIFocusMode` (Hold, Toggle), `HeadlookMode` (Direct, Accumulate) and
`GunsightSystem`, and the 90 shipped preset files supply `FocusOption_Show` on top. See
[Frontier documents most of them](../02-features/bindforge/bind-editor.md#frontier-documents-most-of-them-in-the-game-folder--found-2026-09-16).
**What is left is the short list below**, and the mouse family is closed entirely.

**Partly captured already**, from Alan's pass over the options screen on 2026-09-16 — see
[What the options screen showed](../02-features/bindforge/bind-editor.md#what-the-options-screen-showed--2026-09-16)
for the labels and the tokens known so far. What is left is the token behind every option **not currently
selected** — OFF and ROLL on the mouse X axis, CYCLE on UI Focus Mode, DIRECT on Headlook Axis Mode,
**one confirmation** (2026-09-17). `mute_pushToMute` was found in 313 community files, the increment vocabularies
are complete, and the panel-focus question was **settled in-game**: setting *Looking at Comms Panel* to
*Focuses the panel* rewrote `CommsPanelFocusOptions` to `Value=""`, so the empty string is that choice and
there is no third token.

**Nothing remains open.** The On Foot question closed the same evening: with *Mouse X-Axis* on ROTATE, an
in-game apply re-emitted the field as `Bindings_MouseYaw`, so that is what the current build writes and
`Bindings_MouseHumanoidYawRotate` is not current. **Item 14 is complete** — every choice field's vocabulary is
[tabulated](../02-features/bindforge/reference-data/settings-choice-fields.md), and the method that closed it is
recorded under
[an in-game apply re-emits the whole file](../02-features/bindforge/bind-editor.md#an-in-game-apply-re-emits-the-whole-file--observed-2026-09-17).

Everything else closed on 2026-09-16 from sources that cost nothing: Frontier's `Help.txt`, the 90
presets Elite ships, and the 152 `.binds` files in [EDRefCard's repository](https://github.com/richardbuckle/EDRefCard).
**The increments are the fraction the label names** — `ThrottleIncrement` = `0.10000000` for 10%,
`HeadlookIncrement` = `0.25000000` for 25% — and `ThrottleRangeFreeCam` turned out to carry its own token,
`Bindings_ThrottleForewardOnlyFreeCam`, rather than sharing its family's. **That last one is the argument for
this whole testing item:** the obvious guess was wrong, in the one place nobody would have checked.

**Do not crawl edrefcard.info for more.** Its 3,888 shared configs would be a richer sample, but the site's
`robots.txt` allows `/list` and disallows everything else, `/configs/` included.

**Second pass, 2026-09-16, ended early — the game crashed.** Nothing was lost: every value captured matched
the preset file exactly, so the pairs already recorded stand. Worth confirming the `.binds` file survived the
crash intact before the next pass, since a crash during an options change is precisely the case
[Player Backups](../02-features/bindforge/file-manager.md) exist for.
**If it does not happen before release, enums ship read-only** — booleans and floats do not depend on it.

**The 31 entries are 6 families**, and the members of a family share a vocabulary, so the work is about
**eight dropdowns**, not thirty-one. Tokens below were read out of the specimen files; none is inferred from an
element's name.

| Family | Entries | Tokens already observed | What the walk has to find |
|---|---|---|---|
| **Mouse axis mode** | 16 — `MouseXMode`/`MouseYMode` and the FSS, SAA, Multicrew, Turret, SRV steering, SRV rolling and On Foot copies, plus `YawCameraMouse` and `PitchCameraMouse` | `Bindings_MouseYaw`, `Bindings_MouseRoll`, `Bindings_MousePitch`, `Bindings_MousePitchInverted` | whether X offers yaw and roll only, whether each has an inverted form, whether an off/none exists |
| **Panel focus** | 4 — `LeftPanelFocusOptions`, `RightPanelFocusOptions`, `RolePanelFocusOptions`, `CommsPanelFocusOptions` | `FocusOption_Nothing` **only** | every other `FocusOption_*` |
| **Yaw into roll** | 3 — `YawToRollMode`, `YawToRollMode_FAOff`, `YawToRollMode_Landing` | `Bindings_YawIntoRollNone` **only** | the on-states |
| **Throttle range** | 3 — `ThrottleRange`, `ThrottleRangeFreeCam`, `BuggyThrottleRange` | `Bindings_BuggyThrottleForewardOnly` = FORWARD ONLY (note the game's own spelling of *Foreward*); `""` = FULL RANGE | **the ship's FORWARD ONLY token**, and whether `ThrottleRangeFreeCam` uses the same vocabulary |
| **Mute mode** | 2 — `MuteButtonMode`, `CqcMuteButtonMode` | `mute_pushToTalk`, `mute_toggle` | **there is a third** — the screen offers PUSH TO MUTE, whose token no file has ever held |
| **Enumerated numerics** | 3 — `HeadlookIncrement`, `ThrottleIncrement`, `BuggyThrottleIncrement` | `0.00000000` = CONTINUOUS on all three | the SRV list reads CONTINUOUS, 10%, 12.5%, 16.7%, 25%, … — confirm the stored number is the fraction, and get the full list including whatever sits below the fold |
| **Singletons** | 2 — `UIFocusMode`, `HeadlookMode` | `Bindings_FocusModeHold`, `Bindings_HeadlookModeAccumulate` | the other modes of each |

**The observed vocabularies are tabulated** in
[settings-choice-fields.md](../02-features/bindforge/reference-data/settings-choice-fields.md) — what this
testing adds is the labels, and any value the files have never held.

**Capture what empty means, per element.** An empty value is that element's default *choice*, and the choice
differs: `""` reads DOES NOTHING on a panel-focus entry, FULL RANGE on `ThrottleRange` and OFF on
`MouseBuggyYMode`. Record it alongside the tokens — it is what BindForge shows for a setting the commander
has never touched.

**Confirm which panel element is which.** `LeftPanelFocusOptions`, `RightPanelFocusOptions`, `RolePanelFocusOptions`
and `CommsPanelFocusOptions` line up with *Looking at External / Internal / Role / Comms Panel* by inference,
not by observation. Setting one of them to a distinct value and reading the file settles all four.

**Capture the missing display names in the same pass.** Eleven of the 31 have no row in the
[Action Catalog](../02-features/bindforge/domain-knowledge/EliteDangerous-ActionCatalog.md) at all —
`HeadlookMode`, `UIFocusMode`, `MuteButtonMode`, `CqcMuteButtonMode`, the four panel-focus entries,
`YawCameraMouse`, `PitchCameraMouse` and `BuggyThrottleRange`. **Six of the eleven were named on 2026-09-16** —
UI Focus Mode, Headlook Axis Mode, Mute Button Mode, Microphone State Mode (CQC) and the four *Looking at*
panel entries — leaving `YawCameraMouse`, `PitchCameraMouse` and `BuggyThrottleRange`. The element is known, the label the game puts
on it is not, and [every grid row needs a name](../02-features/bindforge/bind-editor.md#settings-entries-are-rows-too--settled-2026-09-16).
Noting where each one appears on screen costs nothing extra while the options screen is already open.

→ [Bind Editor — Enums need their vocabularies captured first](../02-features/bindforge/bind-editor.md#enums-need-their-vocabularies-captured-first--settled-2026-09-16)

**15. What does the game do when two files share one preset name?** *Added 2026-09-17.* `Custom.3.0.binds`
and `Custom.4.2.binds` both reduce to the preset name `Custom`, which is what `StartPreset.#.start` stores —
so the name in that file cannot distinguish them. Presumably the game loads the one matching its own version
and shows a single entry, but that is an assumption, and
[Preset Editor](../02-features/bindforge/preset-editor.md#a-name-does-not-identify-a-file) is built on it. Put
two versions of one name in the folder, open the game's Controls screen, and see what the preset list shows and
which file a save writes back to.
→ [Preset Editor — what a line actually holds](../02-features/bindforge/preset-editor.md#what-a-line-actually-holds--the-preset-name-not-the-file-name)

## Core Platform

**8. Linux/Proton path resolution — Krondor's, not a BindForge testing item.** *Reassigned 2026-09-06.* Not a one-time check but ongoing, given how much Linux path resolution varies by distro and by how an individual user has their Steam library configured — which is exactly why it is owned by the person running it. BindForge builds and tests Windows storefront detection; Krondor makes the edits his platform needs. Listed here so the dependency stays visible, not as work waiting on Alan. The journal path is confirmed against independent precedent (EDMarketConnector); the bindings-folder path is inferred from the same structure but not independently confirmed.
→ [Conflict 1.5](conflicts-and-open-questions.md#15-linuxproton-bindings-folder-path--resolved-reframed-as-ongoing-testing-feedback-not-a-one-time-check) · [Path Resolution](../01-host-integration/elite-intel-platform-map.md#path-resolution)
