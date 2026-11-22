# Data Model — Telemetry (Game State as a StarVizion Data Source)

> **Ported note.** In the EDO StellarCore design this document described a hard problem: two isolated,
> out-of-process plugins that were forbidden to talk to each other, needing a sanctioned bridge invented
> for them. Inside Elite-Intel that premise does not survive contact with the code. Elite-Intel is a single
> in-process Java application with shared event buses and an existing parsed game-state layer, so what was
> once an architectural obstacle is now mostly a matter of deciding which fields to expose. This document
> has been rewritten accordingly. See [PORTING-NOTES.md](../PORTING-NOTES.md).

Telemetry is the concept of letting live Elite Dangerous game state — not just controller input — drive a
StarVizion Vizlet.

## The Concept

StarVizion's expression engine (StarCalc) has a documented extension point for referencing fields from a data
source other than a live controller input — see
[StarVizion's node binding model](../02-features/starvizion/vizlet-and-node-system.md#binding-a-neuronode).
Telemetry fills that extension point with game state, so a Vizlet can show a live readout — current fuel,
hull health, cargo fill, ship name, current system, landing-gear or night-vision state — alongside, or
combined with, the controller inputs a Vizlet already visualizes.

## Why This Is Now Straightforward

Elite-Intel already does the hard parts, for its own reasons:

| What telemetry needs | What Elite-Intel already has |
|---|---|
| Journal parsing into structured state | `elite.intel.gameapi.JournalParser` and the `gameapi.journal.events` event classes |
| Live flight/status state | `elite.intel.session.Status` / `StatusFlags` (the game's `Status.json` — fuel, hull, gear, lights, flags) |
| Session and location state | `elite.intel.session.PlayerSession`, `SystemSession`, `LocationData`, `SuitInventory` |
| Structured DTOs and subscribers | `elite.intel.gameapi.gamestate.dtos` / `status_events` / `subscribers` |
| A delivery mechanism | `elite.intel.eventbus.GameEventBus` (Guava `EventBus`, same-thread dispatch) |
| Durable history | the SQLite cache under `elite.intel.db` |

There is no isolation boundary to cross and no new inter-process transport to invent. A StarVizion telemetry
binding is a subscriber on an existing bus, or a read against existing session state.

## The Real Design Question

The open question is no longer *how* to bridge, but *what the field registry looks like* — the naming, typing,
and stability contract for the fields StarCalc expressions are allowed to reference.

This matters because a StarCalc expression written by a player is effectively a public API against Elite-Intel's
internal state. Exposing `PlayerSession` fields directly would couple every saved Vizlet to Elite-Intel's
internal refactoring. A deliberate, named, versioned field registry — a stable façade over the session and
status objects — is the piece that actually needs designing.

**Open items:**
- Which fields make the v1 registry, and what each is named in StarCalc.
- Update cadence per field: `Status.json` refreshes far more often than most journal-derived values, and a
  Vizlet redrawing at frame rate should not imply re-reading everything at frame rate.
- Behaviour when the game is not running, or state is not yet known: does a field read as stale, zero, or
  explicitly undefined? StarCalc needs one answer, not a per-field convention.
- Whether telemetry fields participate in the same deadzone/smoothing machinery as axis inputs, or bypass it.

## Status

Out of scope for StarVizion v1. In v1, StarCalc operates on controller input only. This document records the
shape of the idea so the field registry is designed once, deliberately, rather than accreted field by field
the first time someone wants a fuel gauge.

## Frontier CAPI — Not Required

The StellarCore version of this document treated Frontier's CAPI (and its OAuth2 flow) as part of telemetry,
because a third plugin was to be the data provider and CAPI was in its scope. That plugin is not being built
([FlightDeck](../04-other-ideas/FlightDeck.md)), and none of the telemetry fields listed above require CAPI — they all come from the
journal and `Status.json`, which Elite-Intel already reads locally. CAPI is therefore out of scope here, and
the `FrontierAuthService.md` reference was deliberately not carried over.
