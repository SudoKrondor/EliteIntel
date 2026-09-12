# BindForge & StarVizion — Documentation

Design documentation for two features being built into
[Elite-Intel](https://github.com/SudoKrondor/EliteIntel), SudoKrondor's LLM side-kick and data analyst for
Elite Dangerous: Odyssey.

- **BindForge** — safe external editor and backup tool for control bindings across keyboard, mouse and
  controllers (`.binds`,
  `DeviceMappings.xml`, `.buttonMap`, `StartPreset.#.start`), with live device-input monitoring and
  controller-layout views.
- **StarVizion** — live controller-input visualization overlays for desktop and VR.

Ported from the EDO StellarCore documentation set on 2026-08-30. If you are reading any of this for the first
time, **start with [PORTING-NOTES.md](PORTING-NOTES.md)** — it explains what changed in the move and lists the
open questions the port created.

---

## Start Here

| If you want to… | Read |
|---|---|
| Understand what changed in the port | [PORTING-NOTES.md](PORTING-NOTES.md) |
| Know what the host provides and what's missing | [Elite-Intel Platform Map](01-host-integration/elite-intel-platform-map.md) |
| Understand why these tools exist | [Vision](00-overview/vision.md) |
| Know what v1 means | [V1 Scope](00-overview/v1.2-scope.md) |
| Look up a term | [Glossary](00-overview/glossary.md) |
| See what was considered and parked | [Other Ideas](04-other-ideas/) |
| See the mockups | [Elite-Intel shell](01-host-integration/mockups/EliteIntel_Shell_Mockup.html) — open this first; its tab bar links to both feature mockups |

## Layout

```
00-overview/            Vision, scope, glossary, open questions, testing backlog
01-host-integration/    What Elite-Intel provides, + Elite Dangerous path domain knowledge
    mockups/            Elite-Intel shell mockup (entry point to the two feature mockups)
02-features/
    bindforge/          Specs, domain knowledge, reference data, mockup, punchlists
    starvizion/         Specs, mockup, punchlists
03-data-models/         Binding schema, device provenance, telemetry
04-other-ideas/         Considered and parked - not scheduled, kept so the reasoning survives
```

## BindForge

Four top-level sections, in priority order — backup and restore first, because protecting configuration from
loss matters more than editing it.

- [Overview](02-features/bindforge/overview.md) — managed file domains, live file sync, data integrity
- [Alias Designer](02-features/bindforge/alias-designer.md) — device naming, button/axis labels, and
  per-installation mirroring
- [Bind Editor](02-features/bindforge/bind-editor.md) — Game Mode, Action Groups, Input Mode, Control Types,
  shared conflict detection
- [Preset Editor](02-features/bindforge/preset-editor.md) — which `.binds` file loads per binding section
- [File Manager](02-features/bindforge/file-manager.md) — backup and restore of all four file domains
- [Roadmap](02-features/bindforge/roadmap.md)
- [Domain knowledge](02-features/bindforge/domain-knowledge/) — `.binds` format, action catalog, conflict
  rules, `DeviceMappings.xml`/`.buttonMap`, and the interactive conflict-matrix pages
- [Reference data](02-features/bindforge/reference-data/) — `ActionCatalog.json` and the binding-zone
  spreadsheets
- [Mockup](02-features/bindforge/mockups/BindForge_Mockup.html)
- [Device Provenance](03-data-models/device-provenance.md) — why device ownership must live in the database

## StarVizion

- [Overview](02-features/starvizion/overview.md)
- [Vizlets and the Node System](02-features/starvizion/vizlet-and-node-system.md) — Vizlets, NeuroNodes, the
  eight primitives
- [StarCalc Expression Engine](02-features/starvizion/starcalc-expression-engine.md)
- [HoloFrames and Persistence](02-features/starvizion/holoframes-and-persistence.md)
- [UI Layout](02-features/starvizion/ui-layout.md) — editor UI and mode-based visibility
- [VR Overlay](02-features/starvizion/vr-overlay.md)
- [Roadmap](02-features/starvizion/roadmap.md)
- [Mockup](02-features/starvizion/mockups/StarVizion_Mockup.html)

## Status

Design documentation. Neither feature is implemented in Elite-Intel yet.

**BindForge is Elite-Intel's existing bind editor, upgraded in place.** It is not a second section built
beside the current one, and nothing is retired — the Bindings section is grown, tab by tab, until it is
what the BindForge name says. See [Phased rollout](00-overview/v1.2-scope.md#phased-rollout--revised-2026-09-08).

That answers what used to be the blocking question before code — which component owns the on-disk write
path — by removing the second component: the existing pipeline is BindForge's pipeline. What remains is
which of `elite.intel.ai.hands`'s deliberate restrictions grow with it — see
[PORTING-NOTES §7](PORTING-NOTES.md#7-open-questions-this-port-creates) and
[Upgrading the Existing Bind Editor](02-features/bindforge/overview.md#upgrading-the-existing-bind-editor).

*(An earlier blocker — no game-running detection — was cleared by removing Dormant Mode in August 2026.)*

## Where This Came From

Ported in August 2026 from the EDO StellarCore documentation set — a platform-agnostic design for a
standalone application that is not being built.

**That set is superseded, and this one is the source of truth.** Anything in it that mattered to
Elite-Intel was carried across; what was not is recorded in [PORTING-NOTES.md](PORTING-NOTES.md) with the
reason, so the decision is visible without needing the original. The host specifications went because
Elite-Intel *is* the host; the third plugin went for the reasons in
[Other Ideas](04-other-ideas/FlightDeck.md).
