# Proposal: Curated Subscriber Layer as the Sole Driver of Companion Vocalization

**Status:** Proposal / discussion
**Author:** (commander)
**Date:** 2026-06-25
**Affects:** `elite.intel.companion.*`, `elite.intel.gameapi.journal.subscribers.*`
**Related:** `docs/COMPANION_ARCHITECTURE.md` (§2.2 importance taxonomy)

## TL;DR

In companion mode, vocalization currently happens **at the LLM's discretion** for raw journal events classified
`HIGH` importance — often with wrong content, verbosity, and bad timing. Meanwhile the hand-curated logic in
`gameapi.journal.subscribers` (e.g.
`ProspectorSubscriber`, `ShipTargetedEventSubscriber`), which already decides *exactly*
what is worth saying and when, is partially bypassed and partially duplicated.

This proposal separates **knowing** from **barking**:

- **Knowing** — every accepted raw journal event flows into companion *memory
  only*. The companion stays aware of game state (can answer questions, build context) but cannot speak on its own.
- **Barking** — the curated subscriber layer is the **single authority
  ** that decides what gets vocalized. It publishes curated narration; the LLM is demoted to
  *phrasing* that narration in character, never deciding whether to speak.

The architecture already encodes this distinction (importance taxonomy). This is a re-routing and re-classification, *
*not** a rewrite.

---

## 1. How it works today

In companion mode there are two independent channels into the companion's EVENT lane, and they make the speak/don't-speak decision in two different places.

### Channel 1 — raw journal events (the "barking at a whim" source)

```
CompanionSubsystemGate.onGameEvent(BaseEvent)
  -> GameEventFilter            (allow-list + importance != LOW + 5s per-type cooldown)
  -> ThoughtDispatcher.submitEvent()
  -> Thought.event(GAME_EVENT, importance)
```

In `Thought.run()`:

| event `importance()` | behaviour |
|---|---|
| `LOW`    | dropped by `GameEventFilter` (never reaches a thought) |
| `NORMAL` | **recorded to memory, LLM never engaged, never speaks** — i.e. *knowing* |
| `HIGH`   | **full LLM thinking loop; the LLM freely chooses `speak` vs `nothing_to_do`** — i.e. *barking at a whim* |

The `HIGH` branch is the root cause: an automatic heuristic (`BaseEvent.importance()`)
hands the LLM an open mic.

Events currently classified `HIGH` (each `importance()` override returning `Importance.HIGH`):
`CarrierBuyEvent`, `CodexEntryEvent`, `MissionAcceptedEvent`, `MissionCompletedEvent`,
`MissionFailedEvent`, `MissionRedirectedEvent`, `PromotionEvent`, `ProspectedAsteroidEvent`,
`ResurrectEvent`, `ScanOrganicEvent`, `ShipyardNewEvent`.

### Channel 2 — the curated subscriber layer (the model we want)

```
gameapi.journal.subscribers.*  (hand-tuned filtering/calculations)
  -> publish SensorDataEvent
  -> CompanionSensorDataBridge.onSensorData()
  -> ThoughtDispatcher.submitSensorData()
  -> Thought.sensorNarration()   (URGENT, no query tools, bypasses verbosity gate)
```

Here the subscriber already decided "this is worth saying"; the LLM only
*phrases* the provided content. This is exactly the behaviour we want everywhere.

### The duplication problem

`BaseEvent.importance()` does double duty as a *speech
trigger*, and it duplicates the subscriber's own logic. Clearest example — mining:

- `ProspectedAsteroidEvent.importance()` runs a mining-target check and returns `HIGH`
  when a target material is present.
- `ProspectorSubscriber` runs the **same** mining-target check and publishes a curated
  `MiningAnnouncementEvent`.

So the same asteroid can be narrated twice by two layers making two independent decisions:
the curated announcement (good) *and* a raw `HIGH` thought where the LLM ad-libs (bad).

### A third path that bypasses the companion entirely

The other curated announcements are published by subscribers but consumed only by
`VocalisationRouter` -> TTS, gated by per-feature user toggles:

`MiningAnnouncementEvent`, `DiscoveryAnnouncementEvent`, `RouteAnnouncementEvent`,
`RadarContactAnnouncementEvent`, `NavigationVocalisationEvent`, `RadioTransmissionEvent`.

Only
`SensorDataEvent` is bridged into the companion. The rest never reach it, so in companion mode they are voiced by the legacy router rather than the companion's voice, and they are not recorded in companion memory.

---

## 2. Proposal

**Principle: separate the two channels by *intent*, not by a heuristic.**

### 2.1 Knowledge channel — raw events become memory-only, never speech

Make the `GAME_EVENT` kind always memory-only in `Thought.run()` — apply the existing
`NORMAL` short-circuit to *all* `GAME_EVENT` thoughts, regardless of
`importance()`. Raw journal events flow into memory so the companion
*knows* the game state, but can never trigger speech on their own. This removes the "barking at a whim" path entirely.

Consequently, `BaseEvent.importance()` is demoted to a pure **memory-relevance filter**:

- `LOW` — not worth remembering (filtered out, unchanged)
- otherwise — record to memory

It stops being a speak signal. The duplicated mining-target logic in
`ProspectedAsteroidEvent.importance()` can be simplified away (the subscriber owns that decision).

### 2.2 Narration channel — the curated subscriber layer is the only thing that speaks

A subscriber that wants the companion to talk publishes a curated narration event; that becomes a
`SENSOR_NARRATION`-style thought (LLM phrases it, does not decide it). This is the existing
`CompanionSensorDataBridge` pattern — generalize it.

### 2.3 Close the gap — bridge the other curated announcements into the companion

Route the remaining curated announcement events (`MiningAnnouncementEvent`,
`DiscoveryAnnouncementEvent`, `RouteAnnouncementEvent`, `RadarContactAnnouncementEvent`,
`NavigationVocalisationEvent`, `RadioTransmissionEvent`) into the companion the same way
`SensorDataEvent` is bridged today, so in companion mode they:

- come out **in the companion's voice** (phrased in character), and
- **land in companion memory**.

Keep the on/off decision where it already lives (the subscriber / session-flag layer), so the companion narrates only what the curated layer hands it.

### Net effect

| Concern | Owner |
|---|---|
| What to say / when to say it | curated subscribers (`gameapi.journal.subscribers`) |
| How to phrase it in character | companion LLM (phraser only) |
| Awareness of everything else | companion memory (raw events, memory-only) |
| Whether `importance()` can make it talk | **no longer possible** |

"Knowing" and "barking" become two physically separate pipes. The subscriber layer is the single, hand-tuned decision point for vocalization; the LLM never opens its own mic.

---

## 3. Sketch of the changes

> Indicative only — final shape is the developer's call.

1. **`Thought.run()`** — short-circuit *all* `EventInputKind.GAME_EVENT` thoughts to memory-only (extend the current
   `NORMAL` short-circuit to ignore `importance()` for
   `GAME_EVENT`). No LLM, no speech for raw events.
2. **`BaseEvent.importance()` overrides** — re-read as a memory-relevance filter; drop
   `HIGH` usages that exist only to trigger speech (e.g. simplify
   `ProspectedAsteroidEvent`). Keep `LOW` for high-frequency telemetry.
3. **New curated-narration bridge(s)** — mirror
   `CompanionSensorDataBridge` for the other announcement events, routing them to a
   `sensorNarration`-style thought. Register them in
   `CompanionSubsystemGate.start()` alongside `sensorDataBridge`.
4. **Toggles** — ensure per-feature user toggles (
   `isMiningAnnouncementOn`, etc.) remain authoritative on the companion path (keep them in the subscriber/session layer; the companion only narrates what it is handed).

No changes to the event bus, the journal parser, or the subscriber filtering logic itself — that hand-curated logic is the point and stays as-is.

---

## 4. Open questions

1. **Should any raw event ever speak unprompted?
   ** This proposal says no — all spontaneous speech goes through a subscriber. If a rare exception is wanted (e.g. "under attack"), the clean way is a dedicated subscriber that publishes curated narration, not a revived
   `HIGH`-importance LLM path.
2. **Verbosity vs curated speech.** Curated narration currently bypasses the verbosity gate
   (`EventSpeechPolicy`). Confirm that is still desired once
   *all* spontaneous speech is curated — or decide which curated channels honour `QUIET`.
3. **Double-recording.
   ** A curated narration and its underlying raw event will both land in memory (the spoken line + the data). Believed desirable; confirm.