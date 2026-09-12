# Conflicts and Open Questions

> ## Ported to Elite-Intel — read this first
>
> This document is preserved **as a historical record** and has deliberately not been rewritten. Its value is in
> showing *why* each decision was reached; editing the entries to match the new host would destroy that.
>
> Read it with these standing corrections in mind:
>
> - **References to "EDO StellarCore", the host shell, the plugin system, the `Plugins/` folder, plugin
>   isolation, and the full service set are superseded.** The host is now Elite-Intel — see
>   [Elite-Intel Platform Map](../01-host-integration/elite-intel-platform-map.md).
> - **Conflict entries about plugin packaging, out-of-process contracts, and inter-process communication are
>   moot.** BindForge and StarVizion are in-process packages in a single Java application.
> - **Dormant Mode was removed in August 2026.** Entry 3.8 and every reference to a write-block while the game
>   is running are superseded — BindForge no longer blocks writes and no longer detects whether the game is
>   running. See
>   [BindForge — Writing While the Game Is Running](../02-features/bindforge/overview.md#writing-while-the-game-is-running).
> - **Entries about Elite Dangerous itself — file formats, conflict resolution, device identity, install paths —
>   remain fully valid.** These are the majority of the document and the reason it was carried over.
> - `scope.md` is referenced in places but was **not ported**, and is not coming: it described the
>   StellarCore documentation pass itself, not Elite Dangerous or the features. A reference to it is a
>   dead end by design.
>
> New open questions created *by* this port are tracked in [PORTING-NOTES.md](../PORTING-NOTES.md), not here.

---

Every place the curated source material disagreed with itself, or left a genuine question unanswered, is logged here rather than silently resolved. Where this documentation package had to pick *something* in order to write a coherent spec, the choice made is stated, along with what the alternative was — so it can be revisited.

**General pattern across this whole corpus:** the source material spans several chronological design generations, roughly: an early Elite-Intel-era pass, a standalone-tool concept, a Windows-only host-shell rebuild, and a later cross-platform host-shell rewrite that explicitly supersedes the Windows-only one. Unless noted otherwise below, **this documentation package used the chronologically latest generation's resolution** wherever one generation's own text explicitly says it supersedes or was adapted from an earlier one. Genuine disagreements *within* the same generation, or ideas dropped without an explicit reason, are what's actually logged as open below.

---

## 1. Core Platform

### 1.1 Windows-only vs. Windows+Linux host shell — RESOLVED

**Decision (confirmed):** the Windows+Linux design stands as the standard for all six items below. No further action needed; documented here for the record.

An earlier design iteration (`EDO Stellar Core — Application Specification.md`, the "EDO Project" copy) specified a Windows-only host shell. The current design (`StellarCore-Spec.md`) explicitly supersedes it with a Windows+Linux design. This package follows the current design throughout. Specific details that differed as a direct consequence, each individually confirmed:

- **Tray icon vs. minimize-to-taskbar** — the earlier spec kept a persistent system tray icon as the "keep running" mechanism; the current spec replaces it with minimize-to-taskbar, specifically because tray-icon support is inconsistent across Linux desktop environments.
- **Nav badge states** — the earlier spec had three states (None/Warning/Error); the current spec adds a fourth (Running), used by StarVizion to show it has active background overlays.
- **Plugin manifest fields** — the earlier spec's manifest lacked an Id field (separate from display Name), an Execution Model field, and a Stays-Active-In-Background flag. All three exist in the current spec and are load-bearing for real plugin behavior (StarVizion depends on Stays-Active-In-Background; the Id field is what keeps a plugin's on-disk data folder stable across a display-name change).
- **Device Service axis range** — the earlier spec left this genuinely undecided (one section of that same document even states two different ranges in two places). The current spec settles on −1.0 to +1.0 consistently, matching StarVizion's own spec. Treated as resolved in this package, but flagged here since the earlier spec's internal inconsistency was never explained, only superseded.
- **Window minimum size** — unspecified in the earlier spec; 1200×900 in the current one.
- **Configuration store format** — the earlier spec left this as an open item with a stated leaning toward one flat format; the current spec resolves it as a three-tier approach based on each piece of data's own shape (see [Services — Configuration Store](../01-host-integration/elite-intel-platform-map.md#configuration-store)).

### 1.2 Root vision document's original open questions — RESOLVED

**Decision (confirmed):** see [v1.2-scope.md](v1.2-scope.md) for the full breakdown. Summary:
- **Smallest useful v1 release** — EDO StellarCore (full service set) ships together with a fully-featured BindForge (Alias Designer, Bind Editor's Game/Purpose/Input Modes plus shared conflict detection, Preset Editor, File Manager) and StarVizion. StarVizion's own v1 feature list is still undefined and remains open — see [v1.2-scope.md](v1.2-scope.md).
- **CAPI** — deferred indefinitely, revisited only once there's an actual need. Not a factor for BindForge or StarVizion. See [FlightDeck](../04-other-ideas/FlightDeck.md), which was its only consumer.

This resolution changed two things in the plugin docs themselves: BindForge's Purpose Mode moved from "deferred, post-v1" to in-scope for v1, and Input Mode moved from a rough roadmap idea to an in-scope-but-undesigned v1 requirement. See [Bind Editor](../02-features/bindforge/bind-editor.md) and [BindForge Roadmap](../02-features/bindforge/roadmap.md).

### 1.3 Inter-process communication for out-of-process plugins — RESOLVED (sequencing)

**Decision (confirmed):** this is not a v1 concern — both v1 plugins (BindForge, StarVizion) are in-process. The concrete transport is confirmed to be a technology-stack decision, deferred until the platform stack itself is chosen, expected sometime after v1 ships to the community. What *is* done now: a transport-independent functional sketch of the request/response/event contract, so the transport decision can be dropped in later without redesigning around it. See [Out-of-Process Plugin Contract](../01-host-integration/elite-intel-platform-map.md#in-process-not-out-of-process).

### 1.4 Plugin manifest file format — RESOLVED (sequencing)

**Decision (confirmed):** the folder-level shape is fixed and documented now — each plugin is a subfolder of the `Plugins/` subfolder under EDO StellarCore's own application root (see [Application Folder Structure](../01-host-integration/elite-intel-platform-map.md#application-folder-structure)). The exact on-disk filename/format of the manifest inside that folder is deferred until the technology stack is chosen, same reasoning as [1.3](#13-inter-process-communication-for-out-of-process-plugins--resolved-sequencing) — no functional sketch needed here since there's nothing stack-independent left to specify once the folder shape is pinned down.

### 1.5 Linux/Proton bindings-folder path — RESOLVED (reframed as ongoing testing-feedback, not a one-time check)

**Decision (confirmed):** this is not being treated as a single verification task to tick off before Linux ship. Linux distro and configuration diversity (non-default Steam library locations, different distro conventions, users running things through Flatpak or other non-standard setups) means one successful test on one machine would not actually close this out — new edge cases are expected to keep surfacing after ship regardless of how much pre-release testing happens. This is logged as a standing **testing-feedback item**: real-world path resolution on Linux (both the confirmed journal path and the inferred bindings path) needs an ongoing channel for users to report when automatic detection gets it wrong, not just a pre-ship confirmation pass.

This directly reinforces why the Path Service's manual-override requirement is treated as load-bearing rather than a fallback of last resort — see [Services — Path Service](../01-host-integration/elite-intel-platform-map.md#path-resolution) and [Elite Dangerous install-path discovery](../01-host-integration/domain-knowledge/EliteDangerous-InstallPaths.md), §6, which already states this as a design rule for exactly this reason. The journal-file path remains confirmed against independent third-party precedent (EDMarketConnector's own documented Linux setup); the bindings-folder path remains inferred from the same directory structure one level over.

---

## 2. StarVizion

### 2.1 CortexUnit (reusable/shareable Vizlet template) — CONFIRMED WANTED, confirmed low-priority

**Decision (confirmed):** CortexUnit is a real, wanted feature — not an abandoned idea. Its purpose: let a Vizlet, or a reusable piece of one, be packaged and shared with the community, so v1 doesn't ship with every single Vizlet personally authored by one person. "CortexUnit" is confirmed as the working name for this concept going forward.

**Prerequisite now substantially satisfied; CortexUnit's own design still not started, and deliberately staying that way for now.** CortexUnit's actual design (what exactly gets templated — a whole Vizlet, a single node, a reusable group of nodes, possibly a whole HoloFrame; what's parameterized vs. fixed; how an instance relates back to its template after sharing) was originally deferred until StarVizion's own foundational question was answered first: **a complete breakdown of everything that makes up a Vizlet — every part, every layer, and how they all fit together.** That breakdown — every NeuroNode primitive, the Vizlet Editor's structure, Live/Static mode, HoloFrames (StarVizion's group concept, renamed from VizLoadout), frame-rate input ownership, global keyboard capture, and all four VR render modes — is now largely done, so the prerequisite is no longer what's holding CortexUnit back. It's simply confirmed not important enough to prioritize right now. See [Vizlets and the Node System](../02-features/starvizion/vizlet-and-node-system.md), [UI Layout](../02-features/starvizion/ui-layout.md), [HoloFrames and Persistence](../02-features/starvizion/holoframes-and-persistence.md), and [VR Overlay](../02-features/starvizion/vr-overlay.md). See [StarVizion's roadmap](../02-features/starvizion/roadmap.md#cortexunit-reusableshareable-vizlet-templates).

The older `StarVizion Docs/` documents also used different terminology for two other concepts that are likely just earlier names for current ideas, not separate open questions: **NeuroFrame** (vs. the current **NeuroNode**) and **VizCluster** (vs. the current **HoloFrame**, itself called **VizLoadout** for most of this documentation pass before being renamed). These are not being actively revisited — they read as the same underlying ideas renamed as the design matured, not a live disagreement — but are noted here since they came from the same source material as CortexUnit.

---

## 3. BindForge

BindForge's source material is the deepest and most heavily revised part of the whole corpus — four internal design generations were identified, several of which materially disagree rather than merely rephrase each other.

### 3.1 Foundational: standalone tool vs. hosted plugin — RESOLVED

**Decision (confirmed):** BindForge-as-hosted-plugin is correct and final, for the whole project, not just BindForge. Origin story, for the record: BindForge and StarVizion were originally separate, standalone app ideas; realizing they shared a lot of the same underlying needs (file management, input device reading) is exactly what led to the idea of a central host app with a plugin system in the first place — see [Vision — History](vision.md). Going forward, **EDO StellarCore is the only standalone application; BindForge, StarVizion and anything the community builds are all plugins.** No exceptions.

The earliest identified BindForge concept document (`BindForgeSpecification.md`) had described BindForge as a standalone tool with its own sidecar metadata files, contributor tagging/notes, exportable presets for community sharing, and speculative telemetry-assisted suggestions — confirmed to be an abandoned early pitch that predates the host+plugin model entirely, not a live alternative.

**Spun out separately, now also resolved:** BindForge will **not** support community sharing of `.binds` presets — unlike StarVizion's Vizlets. Reasoning: a `.binds` file is tightly coupled to the exact physical hardware and layout preferences it was authored on (specific device VID/PIDs, axis/button counts, often a specific keyboard layout like ESDF), so sharing one with the community would almost never be useful to the recipient — an unlikely-to-recur hardware/layout match, not something BindForge could fix by remapping. See [File Manager — Community Sharing of `.binds` Presets](../02-features/bindforge/file-manager.md#community-sharing-of-binds-presets--not-a-feature).

### 3.2 Relationship to a prior in-house binding editor — RESOLVED (not applicable)

**Decision (confirmed):** not applicable to this project. Origin, for the record: this conflict came from a period when BindForge was being developed specifically to replace Elite Intel's own half-built binding editor. That relationship no longer exists — BindForge is not migrating away from or absorbing any prior in-house editor as far as this project is concerned. No legacy editor to stage a replacement for; the two disagreeing document snapshots described a migration plan that simply doesn't apply anymore.

### 3.3 Naming of BindForge's four top-level sections — RESOLVED

**Decision (confirmed):** Alias Designer / Bind Editor / Preset Editor / File Manager — the names already used throughout this package. Two earlier naming sets existed (Device Mapping / Bind Editor / Preset Manager / Backup; and Profiles / Binds Editor / Devices with a separate Backup tab) but are not used going forward.

### 3.4 Installation-path discovery: BindForge-owned vs. shell-owned — RESOLVED

**Decision (confirmed):** the Path Service owns this entirely; BindForge never does its own filesystem discovery, no exceptions. This was really just a direct consequence of the shell/plugin boundary already confirmed in [1.1](#11-windows-only-vs-windowslinux-host-shell--resolved) — plugins never touch the filesystem directly, and BindForge is not a special case. Both earlier and current generations already agreed on the underlying real per-storefront path facts (see [Elite Dangerous install-path discovery](../01-host-integration/domain-knowledge/EliteDangerous-InstallPaths.md)) — only ownership was ever in question.

### 3.5 New-device-creation flow — RESOLVED (refined beyond any of the three original options)

**Decision (confirmed):** none of the three original flows — see below — is quite right. The actual model: **any connected device auto-registers into the working copy automatically**, at startup and on hot-plug, with no "+Add Device" click needed at all — a default alias (the hardware-reported name) and VID/PID are filled in immediately. The user must confirm or edit that alias before button/axis naming unlocks for that device. Editing anything still goes through the existing Edit-Mode-then-Save/Discard flow — nothing is written until the user explicitly commits. Full detail in [Alias Designer — Automatic Device Registration](../02-features/bindforge/alias-designer.md#automatic-device-registration) and [Device Editor](../02-features/bindforge/alias-designer.md#device-editor).

This also settled a related question: automatic registration only ever touches the working copy, never a live game install, so it's safe to happen at any time — including while Dormant Mode is active — since Dormant Mode only blocks writes to live game files. Physically hot-plugging a controller *into a running Elite Dangerous session* remains a separate, game-level crash risk outside BindForge's control; BindForge registering the device in its own working copy does not cause or worsen that.

**New follow-on question surfaced by this decision, not yet resolved:** two identical connected devices would now both attempt to auto-register under the same default hardware-reported alias, which the existing alias-uniqueness rule would reject — and there's no user present in the moment to react to a warning dialog the way [3.7](#37-duplicate-vidpid-devices--resolved-confirmed-real-but-the-fix-needs-hands-on-testing-not-a-design-decision) originally assumed. This needs an actual resolution mechanism (e.g., auto-disambiguating the default alias), not just a warning. See [Alias Designer — Deferred](../02-features/bindforge/alias-designer.md#deferred).

For the record, the three flows this replaced: (1) the previously-current spec's unified click-then-Edit-Mode-then-Save/Discard flow with an explicit "+Add Device" step; (2) an earlier draft's distinct multi-step flow — click a controller, type an alias, a **Create** button enables once validation passes; (3) a separate generation's UI-design document, where clicking **+ Add Device** inserted a placeholder row before any field was touched, combined with auto-save-as-you-work and an "Apply Changes" button serving mainly as a confidence signal rather than the actual save trigger.

### 3.6 Working-copy model — RESOLVED (neither original option — a third, sharper model)

**Decision (confirmed):** neither the single-tier working-copy model nor the master-plus-shadow-copy model, as originally described, is quite right. The actual model: **the live game file is unambiguously the source of truth**, full stop — there is no persistent "master copy" living in BindForge's own data folder that could compete with it for that role. BindForge's local working copy is a refreshable mirror of that truth, kept current via a freshness check (on load, and on live-file-change detection through the File Service's Watch Path) and edited locally before being written back.

This resolution also produced two new, load-bearing behaviors that didn't exist in either original option:

1. **Conflict handling when a live change lands on top of unsaved local edits** — surfaced to the user explicitly (keep the local draft, or refresh from the live file and lose the local edits), rather than either side silently winning.
2. **Destructive Change Detection** — the more important addition. Not every live-file change is legitimate; Elite Dangerous has, at least once in practice, overwritten a player's `DeviceMappings.xml` back to factory defaults during a game update. BindForge does not blindly sync its working copy to match a live file that looks like it lost data — it detects the loss, refuses to propagate it, and instead offers to restore the working copy (or the most recent File Manager backup) back onto the damaged live file.

Full detail in [BindForge Overview — Live File Synchronization](../02-features/bindforge/overview.md#live-file-synchronization). This applies to all four managed file domains — not just `DeviceMappings.xml`, where the original master/shadow-copy idea was scoped — since a future update could plausibly damage any of them, and there's nothing domain-specific about the detection principle.

### 3.7 Duplicate-VID/PID devices — RESOLVED (confirmed real, but the fix needs hands-on testing, not a design decision)

**Decision (confirmed):** the current spec's claim that this scenario "cannot actually occur" is wrong. VID/PID identifies a product model, not an individual physical unit — two off-the-shelf controllers of the identical model (unlike the user's own dual Virpils, which happen to use distinct PIDs per left/right grip variant) genuinely do report identical VID and PID. Combined with [3.5](#35-new-device-creation-flow--resolved-refined-beyond-any-of-the-three-original-options)'s automatic device registration, this is a real, reachable collision, not a theoretical one.

**Not yet resolved — and reframed as a testing-required item, not a design decision:** neither file format Elite Dangerous itself reads has any concept of "USB port" at all. An axis binding in `.binds` identifies its device purely by the VID+PID hex string; `DeviceMappings.xml` maps an alias to a VID/PID pair, nothing else. There is no field in either format that *could* distinguish two entries sharing the same VID/PID. This means the real open question is not "how should BindForge label the second device" — it's **whether the game can usefully support two aliases pointing at the same VID/PID at all, or silently conflates them** — and that can only be answered by connecting two identical-VID/PID controllers and testing directly against a real Elite Dangerous install (same standard of evidence already used for every other confirmed behavior in [Bind Editor — Shared Conflict Detection](../02-features/bindforge/bind-editor.md#shared-conflict-detection)). EDO StellarCore's own Device Service *can* tell two such devices apart at runtime via USB port path — that's real, useful information BindForge has — but it's a fact the game itself may simply have no way to receive through either file it reads.

**Dropped from scope 2026-09-07.** BindForge designs nothing for this. Two identical controllers on one
machine is rare enough that special-casing it costs more than it returns, and the ceiling is not ours to
raise anyway: Elite Dangerous keys on VID/PID and `DeviceMappings.xml` has no field that could carry more,
so even perfect identification on BindForge's side would hand the game a distinction it cannot act on.

**Dropping it leaves defined behaviour, not undefined behaviour** — which is why it is safe to drop.
Existing rules already cover it without a line of new code:

- `DeviceService.checkForDuplicate()` publishes `DeviceDuplicateWarningEvent` when two connected devices
  share VID/PID, so the player is told.
- BindForge's alias-uniqueness rule rejects the second automatic registration, because both units report
  the same hardware-derived name.

The player is informed and nothing breaks silently. Revisit only if it turns up in real use.

*Original wording, for the record:* BindForge's exact disambiguation behavior (auto-suffixing by port path, refusing a second alias, or something else) is deliberately left undesigned until this test is done, since the right answer depends entirely on what the game turns out to do.

### 3.8 Dormant Mode — RESOLVED

**Decision (confirmed):** Dormant Mode itself stays — full write-block for the entire time the game is running, no exceptions, for v1. This was reconsidered directly (whether Dormant Mode is even needed at all, given that the game only re-reads `.binds` when its Controls menu is opened, suggesting the true collision-risk window is narrower than "the whole session") and confirmed to stand as the conservative default, since protecting configuration from loss is BindForge's top priority and the downside of a wrong assumption here is silent data corruption. **New testing-required item, alongside [1.5](#15-linuxproton-bindings-folder-path--resolved-reframed-as-ongoing-testing-feedback-not-a-one-time-check) and [3.7](#37-duplicate-vidpid-devices--resolved-confirmed-real-but-the-fix-needs-hands-on-testing-not-a-design-decision):** whether the block window can be safely narrowed to something closer to the actual risk moment (matched to Controls-menu-open specifically, and confirmed to also hold for `DeviceMappings.xml`/`.buttonMap`/the Active Preset file, which may be read at different times than `.binds`) is a real post-v1 question, not something to design around yet. See [BindForge Overview — Dormant Mode](../02-features/bindforge/overview.md#writing-while-the-game-is-running).

**Now fully resolved — indicator uses both mechanisms together, not one or the other:** a persistent warning banner appears across whichever section is active (covering all four sections Dormant Mode actually affects), *and* the File Manager tab button additionally changes its own text to "Elite Dangerous is running" and greys out, as a second, more localized signal. See [BindForge Overview — Dormant Mode](../02-features/bindforge/overview.md#writing-while-the-game-is-running).

### 3.9 Per-file Edit History — RESOLVED (kept, and fully designed)

**Decision (confirmed):** kept, as a real feature — a bounded, per-file undo system, distinct from and in addition to Player Backups' ZIP archives. Every write to a managed file moves the version it replaces into that file's own History folder; retention is count-based (1–30, default 10, configurable in BindForge's settings tab) rather than age-based; entries are browsable and individually restorable, through the same confirmation-and-Controlled-Replace safety path as everything else. Applies uniformly to all four managed file domains.

This closes a real gap in the previous design, not just a naming question — the current spec's own application folder structure has carried an undefined `PluginData/BindForge/History/` folder since before this conflict was resolved, with nothing documented about what it was for. See [File Manager — Edit History](../02-features/bindforge/file-manager.md#edit-history) and [Application Folder Structure](../01-host-integration/elite-intel-platform-map.md#application-folder-structure).

### 3.10 Preset Editor scope — RESOLVED (pointer-only, confirmed correct by how the game actually works)

**Decision (confirmed):** the current pointer-only model is correct, and the alternative isn't just unwanted — it doesn't apply given how Elite Dangerous actually discovers `.binds` files. The game looks in exactly one shared bindings folder and lists every `.binds` file found there; there's no mechanism by which "hiding" a file from the game via relocation would be meaningful, since a `.binds` file has nowhere else useful to live. The only file movement anywhere in BindForge is the standard edit-and-apply flow: copying an edited working copy over the live file in the game's one bindings folder on Save/Apply (see [Live File Synchronization](../02-features/bindforge/overview.md#live-file-synchronization)). The earlier Active/Inactive managed-storage concept from a separate design generation is not applicable. See [Preset Editor — Scope, Confirmed](../02-features/bindforge/preset-editor.md#scope-confirmed).

### 3.11 Bind Editor mode set — RESOLVED

**Decision (confirmed):** four modes — **Game Mode**, **Action Groups**, **Input Mode**, **Control Types** — settling on names drawn from a mix of generations rather than any single one:
- "Action Groups" and "Control Types" reuse the naming from the separate generation that used "Game Mode / Action Groups / Control Types," in preference to the current spec's "Purpose Mode" / "Type Mode" naming — chosen because they're clearer to think in terms of, per direct feedback. Same underlying concepts as "Purpose Mode" and "Type Mode," renamed only.
- "Input Mode" is kept from the current spec (it doesn't exist in the "Action Groups / Control Types" generation at all).
- **Control Types' primary intended grouping is now clarified:** axis vs. button, first and foremost — matching the same fundamental split the [binding schema](../03-data-models/binding-schema.md#binding-element--three-distinct-kinds) already uses at the data-model level. A finer input-category breakdown (Keyboard, Joystick Axis, Joystick Button, POV/Hat, Mouse, Unbound) was also part of the original concept but is secondary to the axis/button split.
- **The old "Search" mode idea (a separate generation's third mode) is confirmed retired**, not replaced by anything mode-shaped — it became the shell-level Search box that already sits above all mode tabs in the current design and filters whichever list is showing, rather than being its own dedicated mode.

Every reference across this package (Bind Editor, Roadmap, v1.2-scope.md, Glossary) now uses the four confirmed names.

### 3.12 Action/subgroup taxonomy — RESOLVED (reconciliation complete)

**Decision (confirmed), and the reconciliation itself is now done.** While examining it, a third source turned out to be richer than either of the two originally compared: `BindForge_Binding_Zone_Map.xlsx` carries a **Mode Type** classification (Always Active / Conditional / Exclusive Overlay, with a note and confidence rating per row) that neither the domain-knowledge catalog nor `ActionCatalog.json` has — the raw per-action data behind [Bind Editor's context-aware conflict findings](../02-features/bindforge/bind-editor.md#how-the-game-actually-resolves-conflicts-confirmed-by-direct-in-game-testing). All three sources were merged into [`BindForge_ConsolidatedActionTable.xlsx`](../02-features/bindforge/reference-data/BindForge_ConsolidatedActionTable.xlsx) — 482 actions, no data discarded. Where sources disagreed on an action's name (62 cases, all `ActionCatalog.json` disagreeing with the other two, which always agreed with each other), the alternate name was kept as its own row directly beneath the primary one rather than silently dropped. See [BindForge Roadmap](../02-features/bindforge/roadmap.md#formerly-needed-before-v1-can-be-built--now-resolved).

### 3.13 Two specific action-name discrepancies — RESOLVED

**Decision (confirmed via direct screenshot):** `UI_Select`'s real in-game name is **"UI Panel Select"** — confirmed against a real screenshot of General Controls → Interface Mode, matching Primary `[SPACE]` / Secondary a joystick button in the `.binds` file. This also happens to match what `ActionCatalog.json` had independently, confirming that source was right on this specific row. The earlier "Select / Confirm" label was a placeholder guess, never a transcription. Fixed at the source in [`EliteDangerous-ActionCatalog.md`](../02-features/bindforge/domain-knowledge/EliteDangerous-ActionCatalog.md#3-needs-review-items--full-detail).

The "Enter FSS Mode" case remains as-documented context (not something that needed re-resolving) — it already illustrates the same lesson this section demonstrated live: a row that looked settled wasn't, until someone actually checked it against the real game.

### 3.14 Binding-element data-model shape — RESOLVED

**Decision (confirmed):** distinct-per-kind stands — button-type, axis-type, and standalone-setting-type binding elements remain structurally distinct shapes, as already documented in [Binding Schema](../03-data-models/binding-schema.md). The earlier flat-shape alternative (`BindForgeParserModelSpecification.md`) is not used.

### 3.15 VID/PID device-ID derivation and the button/axis-index off-by-one — RESOLVED (logged as testing-required)

**Decision (confirmed):** logged alongside [1.5](#15-linuxproton-bindings-folder-path--resolved-reframed-as-ongoing-testing-feedback-not-a-one-time-check) and [3.7](#37-duplicate-vidpid-devices--resolved-confirmed-real-but-the-fix-needs-hands-on-testing-not-a-design-decision) as a standing testing-required item, not a design decision.

**What the numbering-offset fact actually is, in plain terms** (worth restating here since it's easy to lose track of): two different systems number a controller's buttons starting from different numbers. The live-input system BindForge reads from starts at **0** (first button = index 0, second = index 1, third = index 2...). Elite Dangerous's own `.binds` file format starts at **1** (`Joy_1`, `Joy_2`, `Joy_3`...). So the third physical button is index `2` live, but `Joy_3` on disk — a consistent one-off gap that any conversion between the two must account for, or every button gets captured/highlighted one off from the one actually pressed.

Both this offset and the separate VID/PID-derivation fact are stated as settled-with-worked-examples in some documents and flagged as still-needing-verification in others, with no visible "confirmed by testing" bridging the two the way the game's conflict-resolution behavior was verified elsewhere in this project. Full detail (kept in sync with this entry) now lives in [Binding Schema — Live-Input-to-File-Format Numbering Offset](../03-data-models/binding-schema.md#live-input-to-file-format-numbering-offset-needs-independent-testing).

### 3.16 Cross-section conflict isolation may be wrong — MERGED into 3.8

**Merged (confirmed):** this is the same open item already covered under [3.8](#38-dormant-mode--resolved)'s testing-required note and detailed in [Bind Editor — Shared Conflict Detection](../02-features/bindforge/bind-editor.md#how-the-game-actually-resolves-conflicts-confirmed-by-direct-in-game-testing) — whether BindForge's four top-level sections are truly conflict-isolated from each other, or whether a cross-section matrix is needed on top of the four per-section ones that already exist. Tracked once, in the [consolidated testing checklist](testing-required.md), rather than in two places.

### 3.17 ~~Three punchlist files exist, at three points in time~~ — all retired 2026-09-09

**Retired 2026-09-09, along with the StarVizion punch list and the executed conflict test plan.** Three
BindForge punch lists existed at three points in time — `BindForge_Punch_List.md` (earliest, deep
engineering-decision tracking), `BindForge_Punch_List_Working.md` (mid-point, organised by section), and
`BindForge-PunchList.html` (most current, with conflict detection reorganised as a cross-mode concern).
They never contradicted each other on a claim, only on organisation, and were kept as-is per an
instruction to preserve punch lists rather than merge them.

That instruction outlived its purpose. Of the items still marked open across all four files, three had
since been resolved in the specs, several referenced a stack (Godot), a host (the plugin shell) or spec
sections that no longer exist, and the genuinely live remainder was small. **Those were extracted into the
specs that own them** — [Action Groups](../02-features/bindforge/bind-editor.md#action-groups),
[Anomalies](../02-features/bindforge/bind-editor.md#anomalies), the
[capture dialog](../02-features/bindforge/bind-editor.md#binding-editor-panel--capture-dialog),
[Control Types](../02-features/bindforge/bind-editor.md#control-types),
[StarVizion's open questions](../02-features/starvizion/roadmap.md#open-questions) and
[V1.2 Scope](v1.2-scope.md#host-prerequisites-elite-intel) — and the punch lists deleted. An open item is
more likely to be acted on beside the design it constrains than in a list of its own.

---

## 5. Cross-Cutting

### 5.1 "Commands" vs. "actions" terminology

The task brief for this rewrite flagged "commands not actions" as an example of established project terminology to preserve. A full search of the curated source material found **no place where the project expresses a preference for "command" over "action"** for the concept of a bindable game control — "action" is the sole, consistently-used term for that concept throughout (`ActionCatalog.json`, every BindForge spec generation, the domain-knowledge action catalog). Every appearance of "command" in the corpus refers to something else: the player's "Commander" identity, Elite-Intel's own spoken commands — which the StellarCore pass excluded and which are now the host — or a distinct, barely-specified "custom command" macro feature mentioned only in passing in a couple of files, which does not have enough detail anywhere in the corpus to document as a current feature. Flagging this explicitly in case "commands not actions" refers to a preference from outside the curated source material that should be applied going forward regardless.

### 5.2 Elite Intel material excluded from this pass

A body of material describing a separate, related predecessor application — with its own voice-assistant
feature set — was excluded from the functional rewrite, as it was not part of StellarCore's plugins.

**Recorded here for the irony, and because it is worth knowing.** That predecessor was Elite Intel. The
pass that built this documentation set deliberately scoped it *out*; Elite-Intel is now the host
everything is being built into. Nothing needs recovering — the application itself is right there, and
[the platform map](../01-host-integration/elite-intel-platform-map.md) reads it directly rather than
relying on any description of it.

### 5.3 ShipType lookup self-heal layer — designed but not built

The canonical ship-type display-name lookup is meant to work as a static seed table plus a live self-healing capture of new ship types from journal data over time. Only the static seed layer exists in the source material; the self-heal layer is a documented intent with no design detail beyond "an already-wired-but-empty event handler." Moved to [FlightDeck](../04-other-ideas/FlightDeck.md), which owned the ship-type lookup and is not being built.
