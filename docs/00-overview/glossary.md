# Glossary

Established project terminology. Where a term was named differently across design generations, the
current/preferred term is listed first with prior names noted — see
[conflicts-and-open-questions.md](conflicts-and-open-questions.md) for full detail on any naming change flagged
as unresolved.

## Naming Normalization Applied Throughout This Package

| Current name | Prior name(s) found in source material |
|---|---|
| Elite-Intel | *(the host; not a prior name — see [Vision](vision.md))* |
| BindForge | Bind Forge, Bind Editor (as a plugin name) |
| StarVizion | (unchanged) |
| ~~EDO StellarCore~~ | EDO NexusCore, NexusCore, Nexus Core, Stellar Core, CoreDock — **the former host, replaced by Elite-Intel** |

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
- **Draft** — the copy a player edits; never the live game file directly. **The preferred term throughout
  this documentation as of 2026-09-06.** Elite-Intel already implements it
  (`ai.hands.BindingsWorkingCopyRepository`, `AppPaths.getBindingsWorkingDir()`), and carries a baseline
  fingerprint alongside each one so an in-game rebind can be told apart from a BindForge edit — see
  [Reconciling BindForge Edits With In-Game Rebinds](../02-features/bindforge/overview.md#reconciling-bindforge-edits-with-in-game-rebinds).
- **Working copy** — the same thing as a draft. Retained only where it names the actual code
  (`BindingsWorkingCopyRepository`); prefer *draft* in prose.
- **Local master** — not a concept in this design. Used once in discussion on 2026-09-06 to mean *draft*;
  recorded here so it is not mistaken for the dropped master-copy model below.
- **Master copy** — **dropped, and not an option.** There is no persistent master copy in BindForge's own
  data folder competing with the live game file for the role of source of truth. It is an artefact of the
  existing Elite-Intel bind editor's design generation; see
  [Conflict 3.6](conflicts-and-open-questions.md#36-working-copy-model--resolved-neither-original-option--a-third-sharper-model).
- **Mirror** — one-way propagation of device configuration from BindForge's draft out to a live game
  installation (deliberately not called "sync," since it is one-directional). Never applied automatically
  on first import — see [First Run](../02-features/bindforge/alias-designer.md#first-run-and-hot-plug).
- **Alias Designer / Bind Editor / Preset Editor / File Manager** — BindForge's four top-level sections. See
  [BindForge Overview](../02-features/bindforge/overview.md#top-level-structure).
- **Game Mode / Action Groups / Control Types / Controller Mode / Input Mode / Anomalies** — the six Bind
  Editor modes. Action Groups and Control Types were named "Purpose Mode" and "Type Mode" in earlier design
  generations; **Anomalies was named "Conflicts" until 2026-09-07**, when it grew to cover reserved keys,
  missing controls and contextually invalid bindings alongside conflicts. See
  [Bind Editor](../02-features/bindforge/bind-editor.md).
- **Action Group** — an Action-Groups-mode grouping of bindings by player intent rather than game category.
- **Binding section** — one of the four contexts a `.binds` preset is split into: General, Ship, SRV, On Foot.
  These are **control contexts, not vehicles.** A Ship Launched Fighter or Ship Launched Vessel (the Nomad) is
  flown with *Ship* bindings; only a genuinely new control mode would justify a fifth section. The four lines of
  `StartPreset.#.start` correspond one-to-one with these, in that order.

## StarVizion Terms

- **Vizlet** — a single overlay window; StarVizion's fundamental visual unit.
- **NeuroNode** — the single building block of a Vizlet; renders one visual primitive, Live or Static depending
  on whether it has input bindings.
- **Live mode / Static mode** — a NeuroNode's behavior depending on whether it has input bindings (earlier
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
