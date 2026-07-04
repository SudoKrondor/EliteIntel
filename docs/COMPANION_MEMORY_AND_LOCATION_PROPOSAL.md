# Companion: Durable Memory & Situational Awareness - Design Proposal

**Status:** Draft for discussion
**Audience:** Gnevko (companion subsystem owner) + Alex
**Scope:** Theoretical approach only - no implementation. This is a starting point for a conversation, not a spec.

---

## 1. Purpose

Two capabilities we want to add to the companion, discussed here as design direction:

1. **Persist the companion's "state of mind" / memory** across sessions, reusing the existing
   `db` package (JDBI3 + SQLite + DAO/Manager pattern).
2. **Give the companion real situational awareness** grounded in `gameapi` journal data and the DB - especially *
   *location** - in a form an **8B local LLM can actually digest**.

Design goals restated:

- A way to persist the companion's thoughts / interaction context beyond a single session.
- A way for the companion to be aware of the game session - location above all - so it can be interacted with meaningfully, using data we already receive from journal events and store in the DB.

Everything below is deliberately shaped to **wrap, not rewrite** the companion internals.
`SessionMemoryGateway` and the thought graph stay as they are; we add a persistence layer around the existing seams and a read-only world-state layer beside them.

---

## 2. Current state (as-built, for shared context)

**Memory** (`companion/memory`) is intentionally **session-only, in-RAM** -
`SessionMemoryGateway` says so explicitly. Three tiers:

- **Short-term** - hot, count/token-bounded timeline, inlined into the prompt (cache-friendly prefix).
- **Mid-term** - `EnumMap<ConversationTopic, List>`, per-topic bounded, importance-aware eviction.
- **Long-term** - one LLM-consolidated summary string + a verbatim archive of
  `MAX`-importance "pinned" facts. Both searched on demand, not force-fed.

`MemoryEntry = (timestamp, topic, source, content, importance, float[] embedding)`. Recall = word + cosine-semantic (
`MemorySearch`). Reached statically via `CompanionRuntime`; installed/cleared by `CompanionSubsystemGate` on start/stop.

**"State of mind"** today is thin: `CompanionState` holds only `globalTopic`.

**Game events already reach the companion** - `CompanionSubsystemGate` subscribes to `BaseEvent`/`SensorDataEvent` →
`GameEventFilter` → `EventThought`, which records the event's `memorySummary()` as
*prose* (LOW dropped upstream). But there is **no structured world-state
**: the "Visible context" is only recent conversation; `SystemSession` is touched only for language.

**Persistence infra** is mature and idiomatic: DAO+Manager pairs, and a recurring **"few indexed columns + a `json`
blob"** pattern (see `LocationDao`). Location is already authoritatively persisted by `LocationManager`/
`LocationDao`, fed by journal subscribers, independent of the companion.

**Constraint:** migrations are **additive-only until release** (shared tester DB) - no drops/schema breaks. New
`companion_*` tables are purely additive, so this is fine.

---

## 3. Part One - Durable memory & state of mind

### 3.1 Principle: persist the crystallized layers, not the hot working set

Short-term is ephemeral by design (cache-friendly prompt prefix, synchronized gateway). Putting the DB in its write path would fight the whole design. What deserves durability, in value order:

1. **Pinned `MAX` facts** - verbatim, "commander asked to remember." Highest value, lowest churn.
2. **Long-term summary** - single string, replaced atomically, rare.
3. **`CompanionState`** - the real "state of mind":
   `globalTopic` now, plus whatever mood / disposition / relationship-stance it grows into.
4. *(Optional, later)* mid-term topic memory - survives a crash mid-arc, but higher churn. Defer.

### 3.2 Persist at the transitions that already exist

The gateway already funnels the durable stuff through exactly two methods:

- `addLongTermPinned(entry)` → also persist.
- `replaceLongTermSummary(summary)` → also persist.

These are the only persistence hooks needed for phase one. **The hot path is never touched.**

### 3.3 Wrap, don't rewrite

`SessionMemoryGateway`'s "session-only" contract is explicit and the companion internals are Gnevko's. The clean move is a
**persisting decorator** implementing
`MemoryGateway` that delegates to the session gateway and mirrors the two durable mutations to the DB **asynchronously
** (off the gateway's `synchronized` lock, via `Database.withDao` on a small executor).
`SessionMemoryGateway` stays mechanical and untouched.

`CompanionSubsystemGate.start()` gains one step: **hydrate** the gateway from the DB (pinned facts + summary + state)
*before* registering event subscribers, so the companion boots with its memory intact.

### 3.4 Identity: closed

**One CMDR per session.
** The owner key is implicit - companion memory is simply "the CMDR's memory." No persona/fork question, no partitioning column needed.

### 3.5 Schema sketch (all additive, house idiom: columns + `json` blob)

- `companion_memory` -
  `id, tier, topic, source, importance, content, created_at, embedding BLOB, embed_model, embed_dim, system_address (nullable), body_id (nullable), json`. The
  `json` blob future-proofs the shape.
- `companion_state` - single row:
  `global_topic, long_term_summary, json` (evolving mood/disposition lives in the blob so `CompanionState` can grow with
  **zero migrations** - same trick as `LocationDto`).

### 3.6 The one real gotcha: embedding vectors are model-specific

Semantic vectors are only meaningful for the model that produced them. Store `embed_model` +
`embed_dim`; on hydrate, if the current matcher doesn't match, either **lazily re-embed** or **fall back to word-only
recall**. Otherwise stored vectors silently rot after any embedding-model swap.

### 3.7 Phase one recommendation

Persist **pinned facts + long-term summary + `CompanionState`
**. High value, low churn, maps to two existing hooks, no hot-path impact. Mid-term persistence is a possible later phase.

---

## 4. Part Two - Situational awareness (location is the load-bearing case)

### 4.1 The core reframe: two kinds of knowledge

- **Episodic memory** (subjective, "what happened / what you told me") → the existing tiered memory. Fine as-is.
- **World state** (current objective facts: location, docked?, ship, cargo, target) → **not memory.
  ** It's live current state and should be **pulled fresh into the prompt each turn
  **, not decayed through a prose timeline.

Today location leaks in as aging prose via
`EventThought`, so "where am I" scrolls out of short-term and only resurfaces if semantic recall happens to catch it - unreliable grounding. The fix is
**not more event plumbing** (ingestion already works); it's a structured read seam.

### 4.2 Location is a graph, not a value

`LocationDto` is one flat object that fans out into star / planet / station / market / outfitting / shipyard / rings / signals / materials / genus, persisted as a JSON blob per body.
`MarketDto` (and `StationMarketDao`) can hold hundreds of rows. **Location cannot be inlined into an 8B context.**

### 4.3 The rule: reference, never payload

The companion **never holds a `LocationDto`.** It holds a **handle** - a tiny, stable tuple - and dereferences it **on
demand** through a query that returns a **purpose-shaped projection** sized for an 8B model.

**The handle** (small enough to live in the prompt, in `CompanionState`, and as a memory stamp):

```
{ systemAddress, bodyId, marketID?, LocationType, displayName }
```

That is all that is ever always-on. It uniquely resolves against `LocationDao` (systemAddress + bodyId / name) and
`StationMarketDao` (marketID), and it is what we stamp onto episodic memories.

### 4.4 `LocationType` selects which projections are meaningful

The discriminator decides which projection tools the reducer even exposes (keeps the 8B tool list short):

| Handle type | On-demand projections (each a small, ranked/aggregated text block) |
|---|---|
| `STATION` / `FLEET_CARRIER` | services summary; market **top-N by relevance** (never the full list); outfitting/shipyard presence |
| `PLANET` / `MOON` | class, gravity, atmosphere, landable, bio/geo signal counts, materials, "we mapped it / our discovery" |
| `PLANETARY_RING` / `BELT_CLUSTER` | ring type + mining hotspots/materials |
| `STAR` / `PRIMARY_STAR` | class, scoopable/fuel star, distance |
| system-level | body count, notable bodies, allegiance/economy/security/power |

**Discipline:** a projection **selects, ranks, and aggregates - it never dumps.
** "What's the market here?" returns the top handful of relevant commodities with an "as of \<time\>" note, not 300 rows. The projection layer is DB-query + compact formatter keyed by the handle's IDs; heavy data stays in the existing DAOs. This is the same shape as the existing
`search_in_memory` tool and the main app's query handlers - new location-detail tools, not a new subsystem.

### 4.5 Market cardinality - the handle settles "is there a market?" for free

`station_markets` is keyed uniquely by `marketId` (`ON CONFLICT(marketId)`), and
`marketID` is 1:1 with a station-type location. Presence ⟺ market exists, so it's decidable **from the handle alone, no
query:**

- Handle has `marketID` → exactly one market; resolve it.
- Handle has no `marketID` → no market here; the companion answers the negative directly, zero cost.

Never N markets per location.

### 4.6 Market sourcing ladder - one projection contract, three sources

Local (your own docked) market data vs. other stations/systems (stale or absent unless we hit a third-party API) becomes a routing rule
*inside* the projection. The LLM calls one "market info" tool; the layer decides the source:

| Situation | Source | Freshness |
|---|---|---|
| Station handle **with** `marketID` | local `StationMarketDao.get(marketID)` | per-dock snapshot → label **"as of \<dock time\>"** |
| Station handle **without** `marketID` | none - answer "no market" | instant |
| System handle (N stations) / "where can I buy/sell X" / "best market for Y" | **external** - `SpanshMarketClient` / `EdsmCommoditySearch` / `TradeRouteClient` (search package) | API "as of", bounded top-N |

The projection **contract is identical regardless of source**: local `StationMarketDto`, Spansh
`StationMarketDto`, and EDSM `MarketDto`/`CommoditySearchResult` all normalize into the
*same* small ranked block. The 8B doesn't know or care where it came from.

Two consequences:

- **System-level market/trade questions are inherently external and slow.
  ** Those tools cross the network → on-demand only (never always-on), and the companion should acknowledge/narrate ("checking the trade data…") rather than block silently. The search package is already the async seam.
- **Local market is a per-dock snapshot** (`INSERT OR REPLACE` on `marketId`), so **"as of" is mandatory
  ** in the projection or the companion will quote stale prices as current.

### 4.7 How the two halves connect

- **Situational awareness (always-on):** the current-location **handle
  ** sits in the prompt's Visible context as one line - "You are at \<name\> (\<type\>), docked/in-supercruise." Cheap, always fresh, sourced from
  `PlayerSession` / `LocationManager`.
- **Depth on demand:
  ** when the conversation needs it, the LLM calls the matching projection tool with the handle → query (local or external) → digestible slice. Unbounded depth, near-zero always-on cost. This is exactly "any reference to location is followed by a query returning digestible data."
- **Memory (episodic):** entries are stamped with the **handle
  **, never the payload. "What did we find last time we were in this system?" = recall memory rows stamped with that
  `systemAddress`, then optionally dereference the handle for current facts. Companion tables store only tiny handles; *
  *no location data is duplicated into companion storage** - which also keeps the schema in §3.5 small.

### 4.8 Architectural-constraint check

All of this is **read-only pulls over existing seams** (`PlayerSession`/`SystemSession`,
`LocationManager`/DAOs, the search-package clients). No journal-memory reading, no process injection - the hard
`gameapi` constraint holds.

---

## 5. Open questions for discussion

1. **Where does the persisting decorator live and who owns it** - companion package (Gnevko) vs. a thin
   `db`-side manager the decorator calls? Proposal: decorator in companion, DAO/Manager in
   `db`, matching the house pattern.
2. **Mid-term persistence** - phase one skips it. Agree, or is crash-survival of mid-term worth the churn?
3. **Embedding-rot policy** - re-embed on hydrate vs. word-only fallback on model mismatch (§3.6).
4. **Projection budgets
   ** - hard caps (top-N, char/token ceiling) per projection, tuned for 8B. Market is the pressure test.
5. **First projection to build as proof-of-concept** - proposal: the **market projection
   **, since it exercises the local-vs-external routing and the ranking/cap discipline. Get it right and the rest are easy.
6. **Location stamping seam** - where does the handle get attached to a `MemoryEntry` (dispatcher /
   `ThoughtContext`), given that's where the current game state is known? This is the one place Part Two touches the thought graph.

---

## 6. Summary

- **Memory persistence:** wrap the gateway with an async persisting decorator; persist pinned facts + summary +
  `CompanionState` at the two existing hooks; hydrate on start. One CMDR → trivial identity. Watch embedding-model versioning.
- **Situational awareness:** location is a **handle + `LocationType`-driven projection queries
  **. The handle is tiny and always-on (and settles market presence by itself); depth is an on-demand projection that routes local vs. Spansh/EDSM. Nothing large ever enters the prompt or the companion's persisted tables.
- **Ownership:** shaped as additive tables + a wrapping decorator + a read-only world-state/projection layer, so
  `SessionMemoryGateway` and the thought graph stay intact.