# Glossary

> **For the player's own files, accounts and installations — what a *user*, *commander*, *installation*,
> *alias*, *`.buttonMap`*, *binds file*, *preset file* or *BindName* actually is — see [The Input Environment](input-environment.md).** That page
> defines the real things on disk. This page defines **our** terms for what we build on top of them.

Established project terminology. Where a term was named differently across design generations, the
current/preferred term is listed first with prior names noted — see
[conflicts-and-open-questions.md](../99-archive/conflicts-and-open-questions.md) for full detail on any naming change flagged
as unresolved.

## Naming Normalisation Applied Throughout This Package

| Current name | Prior name(s) found in source material |
|---|---|
| Elite-Intel | *(the host; not a prior name — see [Vision](vision.md))* |
| BindForge | Bind Forge, Bind Editor (as a plugin name) |
| StarVizion | (unchanged) |
| ~~EDO StellarCore~~ | EDO NexusCore, NexusCore, Nexus Core, Stellar Core, CoreDock — **the former host, replaced by Elite-Intel** |
| binds file | preset, `.binds` preset; shipped / stock / factory preset (now *factory binds file*) — **settled 2026-10-19**, see [The Input Environment §2](input-environment.md#binds-file-preset-file-bindname--settled-2026-10-19-alan) |
| preset file | Active Preset file, `StartPreset`, `StartPreset.#.start` (the number is the game's version — `StartPreset.4.start` today, and it changes when Frontier bumps it; see [The Input Environment §2](input-environment.md#binds-file-preset-file-bindname--settled-2026-10-19-alan)) |
| BindName | preset name |

## Host Terms

- **Elite-Intel** — the host application. A Java 21 / Swing / Gradle desktop app for Elite Dangerous; see
  [Elite-Intel Platform Map](../01-host-integration/elite-intel-platform-map.md).
- **Feature** — a top-level capability of Elite-Intel, surfaced as a tab. BindForge and StarVizion are features.
  They are **not** plugins: there is no plugin framework, manifest, or isolation boundary in Elite-Intel.
- **Event bus** — Elite-Intel's Guava-`EventBus`-based dispatch. Six exist under `elite.intel.eventbus`;
  `DeviceBus` and `GameEventBus` are the two these features care about. Subscribers register with `@Subscribe`.
- **Device Service** — `elite.intel.devices.DeviceService`. Shared, read-only joystick/HOTAS/gamepad/pedal
  input at ~60 Hz. The successor to StellarCore's service of the same name; see
  [Device Input](../01-host-integration/elite-intel-platform-map.md#device-input).
- **Data Integrity Principle** — no write may leave a corrupt or partially-written file; every operation either
  completes fully or rolls back cleanly. In Elite-Intel this is a **discipline, not a structural guarantee** —
  see [Data Integrity Principle](../01-host-integration/elite-intel-platform-map.md#data-integrity-principle).
- **Dirty state** — a feature's self-reported status of having unsaved work. Note that Elite-Intel has no
  lifecycle hook that acts on this; a feature must wire its own shutdown and tab-switch handling.

### Retired Host Terms

These appeared throughout the StellarCore documentation and have **no equivalent** in Elite-Intel. They are
listed so that older notes and mockups can be read correctly:

- **Plugin / Manifest / Plugin isolation / Staged load (Stage 1–3)** — no plugin framework exists.
- **Controlled Replace** — StellarCore's File Service write-safety pattern. No central file broker exists; the
  pattern itself must still be implemented inside BindForge's own write path.
- **Nav badge** — the None/Running/Warning/Error indicator on a left-nav button. Elite-Intel is tabbed and has
  no such affordance; see [Status Badges](../01-host-integration/elite-intel-platform-map.md#status-badges).

## BindForge Terms

- **Action** — the established term for a bindable Elite Dangerous control (e.g. "Boost," "Deploy Hardpoints").
  The project does not use "command" for this concept — in Elite-Intel, "command" already means a spoken voice
  command, which makes keeping this distinction more important here than it was under StellarCore.
- **Binding element** — the parsed representation of one action's key/button/axis assignment. See
  [Binding Schema](../03-data-models/binding-schema.md).
- **Game input configuration files, working copy, live copy, backup, edits not applied** — defined in
  [BindForge — Principles](../02-features/bindforge/principles.md#words) (2026-10-22, Alan), which every BindForge
  document follows. In short: the **working copy** is in Elite-Intel's BindForge folder and is the source of truth;
  the **live copy** is where the game reads the files; a **backup** is a date-stamped zip of the live copies, of one of four kinds — daily, before changes, user, first setup (2026-10-24).
- **Draft** — **retired 2026-10-22.** In older documents it means the editor screens' edits not applied.
  (Until then it named the editable copy in Elite-Intel's folder, beside a separate master.)
- **Master** — **retired 2026-10-22.** In older documents read it as the working copy. (From 2026-09-24 it named a
  third copy, separate from the draft, that game files were compared against and adopted into; that model was
  replaced by [storage.md](../02-features/bindforge/storage.md).)
- **Local master** — not a concept in this design. Used once in discussion on 2026-09-06 to mean *draft*.
- **Alias Designer / Bind Editor / Preset Editor / File Manager** — BindForge's four top-level sections. See
  [BindForge Overview](../02-features/bindforge/overview.md#top-level-structure).
- **Game Mode / Action Groups / Control Types / Controller Mode / Input Mode / Anomalies / Settings** — the
  seven Bind Editor modes. **Game Mode, Anomalies and Settings ship in V1.2**; the other four are
  [deferred](v1.2-scope.md#the-v12-release-is-trimmed--settled-2026-09-16), with their tabs absent from the mode bar rather than greyed out
  (2026-09-16). **Settings was added 2026-09-19**, after the trim, because it is a second view over rows the
  grid already renders rather than a mode's worth of new work. Action Groups and Control Types were named "Purpose Mode" and "Type Mode" in earlier design
  generations; **Anomalies was named "Conflicts" until 2026-09-07**, when it grew to cover reserved keys,
  missing controls and contextually invalid bindings alongside conflicts. See
  [Bind Editor](../02-features/bindforge/bind-editor.md).
- **Action Group** — an Action-Groups-mode grouping of bindings by player intent rather than game category.
- **Binding section** — one of the four contexts a binds file is split into: General, Ship, SRV, On Foot.
  These are **control contexts, not vehicles.** A Ship Launched Fighter or Ship Launched Vessel (the Nomad) is
  flown with *Ship* bindings; only a genuinely new control mode would justify a fifth section. The four lines of
  `StartPreset.4.start` correspond one-to-one with these, in that order.

## StarVizion Terms

- **Vizlet** — a single overlay window; StarVizion's fundamental visual unit.
- **NeuroNode** — the single building block of a Vizlet; renders one visual primitive, Live or Static depending
  on whether it has input bindings.
- **Live mode / Static mode** — a NeuroNode's behaviour depending on whether it has input bindings (earlier
  design generations called these "Reactive mode" and "Decoration mode").
- **Primitive** — the visual shape a NeuroNode renders: Dot, Bar, Line, Arc, Text, Grid, Bitmap, or Parametric.
- **StarCalc** — StarVizion's expression language.
- **Local (expression)** — a named, reusable intermediate value within a NeuroNode's StarCalc expressions.
- **HoloFrame** — a named, activatable group of Vizlets (named VizLoadout in earlier design generations).
- **Vizlet library** — the catalog view of all saved Vizlets.

## Shared Data Concepts

- **FDevIDs** — the community-maintained project supplying the canonical internal-symbol-to-display-name lookup
  for ship types. *(The ship-type lookup document was not ported — see [FlightDeck](../04-other-ideas/FlightDeck.md). The reference is kept
  here because BindForge's action catalog work touches the same community data sources.)*
- **Telemetry** — exposing Elite-Intel's parsed journal and `Status.json` state as a live data source for
  StarVizion's expressions. Under StellarCore this was blocked by plugin isolation; in Elite-Intel it is
  straightforward and the open question is the field registry's shape. See
  [Telemetry](../03-data-models/telemetry.md).
