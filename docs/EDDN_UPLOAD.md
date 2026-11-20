# EDDN Upload (Tentative)

**Status:** a *maybe* for the release after V1.2, written 2026-11-19 after a discussion. Nothing is built,
and nothing is promised. V1.2 does not get it. This document records what it would take, so the decision can be
made later with the facts in hand.

Today EDDN is **subscribe-only** in Elite Intel: `EddnListener` reads the relay and nothing is ever uploaded
(CLAUDE.md says so in as many words). This feature would reverse that, so it needs an explicit yes from the
commander before a single message leaves the machine.

---

## Why

- **We take, so we should give.** Spansh and INARA are fed by EDDN, and Elite Intel leans on Spansh for
  markets, bodies and routes. Their freshness comes from commanders who upload.
- **EDMC is the usual uploader, and on Linux it barely works.** The Flatpak build fails to start when it
  cannot find the game installation, which is most of the time (bug report sent with every known Linux install
  location, closed without a fix). The Windows build under Wine runs, but its plugins do not. Building from
  source means old Python dependencies on the machine.
- **For most Linux commanders EDMC is only an EDDN pusher**, and it is easy to forget to start. Elite Intel is
  already running and already reading the journal.

---

## What EDDN requires (and what it does not)

Read from the EDDN repository (`docs/Developers.md`, `schemas/*-README.md`), 2026-11-19.

**No account, no API key, no approval.** An upload is an HTTPS `POST` of a JSON body to
`https://eddn.edcd.io:4430/upload/` (port 4430, trailing slash required, HTTP/1.1, optional gzip with
`Content-Encoding: gzip`, otherwise `Content-Type: application/json`).

The rules a sender must follow:

| Rule | What it means for us |
|---|---|
| Unique, stable `softwareName` | `"Elite Intel"`, never changed. It is how the EDDN admins find us if our data is bad. |
| `softwareVersion` | Must change whenever the content of what we send changes. The app version does that. |
| `uploaderID` | The in-game commander name. The relay obfuscates it before listeners see it. Never spoken, so the "never speak the in-game name" rule is untouched. |
| `gameversion` / `gamebuild` | From `Fileheader` (`FileheaderEventSubscriber` already reads it). `gamebuild` passed through as-is, trailing space included. |
| `horizons` / `odyssey` | From `LoadGame`. When the key is absent from `LoadGame`, **leave it out** of the message, never `false`. |
| Live game only | Nothing from a beta or alpha client goes to the live schemas. |
| `/test` schemas while developing | Every `$schemaRef` gets `/test` appended in dev builds and tests. |
| No personal data | Remove the fields each schema lists (`Wanted`, `FuelLevel`, `FuelUsed`, `JumpDist`, `BoostUsed`, `MyReputation`, `HomeSystem`, `HappiestSystem`, `SquadronFaction`, `Latitude`/`Longitude`, `ActiveFine`, `CockpitBreach`, ...) and **every** `*_Localised` key. |
| Key renames | Several schemas use the old CAPI key names, not the journal's. The schema file is the authority. |
| Augmentation cross-check | `StarSystem` / `StarPos` / `SystemAddress` added to an event that lacks them must match the last `FSDJump` / `Location` / `CarrierJump`. **On a mismatch, drop the message.** The game sometimes stops writing the journal and resumes with events missing. |
| Retries | Wait at least a minute before a retry. Never retry a `400` or `426`. One bad message must not block the others. |

EDDN's own motto sums it up: *no data is better than bad data, and delayed good data is better than degrading
the service for others.*

---

## The trap specific to Elite Intel: never upload a replay

EDDN expects **live** data. Elite Intel reads plenty of journal lines that are not live:

- the startup pre-scan (`JournalPreScanner`, `@PreScanOnly` subscribers),
- the journal backup import (`gameapi.journalimport`), years of old journals,
- the diagnostics harness.

Uploading any of these would push stale markets and old locations to Spansh and INARA. The sender must sit
where only live events arrive. This is the first thing to design, and it gets a test that proves a pre-scan
or an import cannot reach it.

A good backstop as well: refuse any event whose journal timestamp is more than a few minutes old.

---

## Consent and EDMC

- **Opt-in, off by default**, like sharing massacre sets. This is consent to upload, not an expert toggle,
  so the "no expert toggles" rule does not apply.
- **Double posting.** A Windows commander who runs EDMC as well would send everything twice. The opt-in
  wording covers it: something like *"Share market and exploration data with EDDN (leave off if you run
  EDMC)"*. Detecting EDMC is not worth it.
- The commander can turn it off at any time. Nothing is queued to disk, so turning it off ends it.

---

## Scope, if it happens

**First cut** - the most value for Spansh and INARA, from data Elite Intel already reads:

| Schema | Source | Notes |
|---|---|---|
| `commodity/3` | `Market` event + `Market.json` | `StationMarketsManager` already parses it. Skip `NonMarketable` and non-empty `legality` rows; strip `$..._name;`; drop `Category`, `id`, `Producer`, `Rare`, `StationType`; no `economies`/`prohibited` keys. The single most useful schema. |
| `journal/1` | `FSDJump`, `Location`, `Docked`, `CarrierJump`, `Scan`, `SAASignalsFound` | The elisions and augmentations above. |
| `fsssignaldiscovered/1` | `FSSSignalDiscovered` | Also where resource sites and conflict zones come from, the data our own ledgers read. |
| `navroute/1` | `NavRoute` + `NavRoute.json` | Written at plot only. |
| `approachsettlement/1` | `ApproachSettlement` | Small. |

**Later**, once the first cut has proven itself: `outfitting`, `shipyard`, `codexentry`, `fssdiscoveryscan`,
`fssallbodiesfound`, `fssbodysignals`, `navbeaconscan`, `scanbarycentre`, `fcmaterials_journal`,
`dockinggranted`/`dockingdenied`.

**Never:** CAPI-sourced schemas. Elite Intel has no CAPI access and should not gain it for this.

---

## Shape of the code

- One sender in `gameapi.eddn`, next to `EddnListener`: one daemon thread, a small bounded in-memory queue.
  A full queue drops the oldest message. If the sender dies, the app is whole without it, the same as the
  listener.
- One builder per schema: takes the journal event, returns the finished message or nothing (cross-check
  failed, beta client, replay).
- Transport through the app's existing HTTP client.
- Announces nothing. Uploading is silent, like the ledgers.

**Reference implementation:** EDMC's `plugins/eddn.py` is the de facto spec, edge cases and all. Port its
rules, not its structure.

**Tests:** the EDDN repository ships every schema as JSON Schema (`schemas/*-v*.json`). Unit tests build
messages from real journal lines and check them against those files with a JSON Schema validator, plus
explicit checks that no personal field and no `_Localised` key survives. No live EDDN in unit tests. Manual
testing goes to the `/test` schemas.

---

## Contact with the EDDN admins

Not required to send, but polite, and useful if they ever need to reach us about bad data.

- The EDCD community lives on Discord (`#eddn`). Krondor stays off Discord, and Alan has offered to say hello
  on the project's behalf.
- In writing: issues on the EDCD/EDDN GitHub repository.

---

## Open questions

1. **Go or no go.** This whole document is a maybe.
2. Where the opt-in lives (Commander tab, beside the massacre-set sharing?) and its exact wording in all
   locales.
3. Which release, and its migration band, if the opt-in needs a stored setting.
4. Whether the site and the privacy text need a line saying what is shared, once it ships.
