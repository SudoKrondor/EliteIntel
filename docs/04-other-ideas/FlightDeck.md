# FlightDeck — Not Being Built

**Status:** parked, 2026-09-08. Not part of Elite-Intel, not scheduled, not being designed further. This
document exists so the idea and the reasoning survive in one place instead of as scattered mentions in
documents about work that *is* happening.

---

## What it was

The third plugin of the **EDO StellarCore** design, alongside BindForge and StarVizion. Where those two are
about *controls*, FlightDeck was about **commander and session data** — reading the journal into a durable
store and presenting the commander's own history and current state back to them.

Its full specification was written for the StellarCore set and not carried across. That set is superseded,
so treat this document as what survives: the idea, and the reason it is parked. If FlightDeck is ever
revisited it should be designed against Elite-Intel as it actually is, not resumed from a specification
written for a host that was never built.

## Why it is not being built

**Elite-Intel already covers most of its ground.** Elite-Intel parses journals into a SQLite cache and tracks
commander and session state for its own purposes — the LLM needs that state to answer questions, so the data
layer FlightDeck would have built already exists and is already in use.

That makes FlightDeck the one plugin of the three whose core value the new host *duplicates* rather than
*enables*. BindForge and StarVizion came to Elite-Intel because the host supplies what they need. FlightDeck
would have arrived to find its job already done.

## What went with it

Three things left the documentation set because FlightDeck was their only consumer:

| Dropped | Why it belonged to FlightDeck |
|---|---|
| **Frontier CAPI, and its OAuth2 flow** (`FrontierAuthService.md`) | CAPI supplies market prices, fleet-carrier status and ship loadout — data outside the journal. Only FlightDeck needed it. Neither BindForge nor StarVizion has any CAPI dependency, and [telemetry](../03-data-models/telemetry.md) confirmed every field it wants comes from the journal or `Status.json`. |
| **Ship-type lookup data models** (`ship-type-lookup.md`, `EliteDangerous-ShipTypes.md`, `ShipType_DisplayName_Seed_Data.md`) | A canonical map from journal ship identifiers to display names. |
| **Journal and commander data models** (`journal-and-commander-data.md`) | The store FlightDeck would have read from. |

**CAPI is worth a second look only if something else ever needs it.** It was deferred indefinitely rather than
rejected — the note in the original scope was that it would be revisited "once FlightDeck is picked up." With
FlightDeck parked, that trigger is gone, and the honest position is: no current feature wants it.

## Two ideas worth keeping, if anyone returns

### The ship-type lookup self-heal layer

The canonical ship-type display-name lookup was designed as **a static seed table plus a live self-healing
capture** — new ship types learned from journal data over time, so a game update adding a hull does not
require a documentation update to display its name.

**Only the static seed layer was ever specified.** The self-heal half is a documented intent with no design
detail beyond "an already-wired-but-empty event handler." The idea is sound and general — it would apply to
any lookup table fed by a game that keeps adding to itself — but it is an idea, not a design.

### The two design generations agreed

Recorded because it is unusual and says something about the source material's quality: FlightDeck's two design
generations — the "EDO Project" plugin specification and the later `FlightDeck-Spec.md` — were compared for
contradictions and **none were found.** They are near-identical, the later one differing only by removing
technology-specific framing and consolidating open-item numbering.

Every other part of this documentation set produced real conflicts between generations that had to be
reconciled. FlightDeck produced none.

## If it is ever revisited

The question to ask first is not *"can we build it"* but **"what does it add that Elite-Intel does not already
do?"** That question is what parked it, and it is the one that has to be answered before anything else is
worth designing.
