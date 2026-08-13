# Commander Tab

<img src="images/controller.png" class="inline" height="20" alt="Commander"> Who you are, what
your ship does for you automatically, what Vega tells you about without being asked, and which
voice each hull in your fleet speaks with.

![Commander tab](images/ui-tab-commander.png)

---

## Commander Profile

**Commander Name** — overrides your in-game name for speech. Use it if Vega mangles your
handle, or if you simply want to be called something else. Saved when you press Enter or click
away.

> The **journal folder** moved to [Settings → Common](UI-Settings-Tab) in V1.1, and the
> **bindings folder** moved to the [Bindings tab](UI-Bindings-Tab).

---

## Ship Options

Automations Vega performs on your behalf. Each one is a plain toggle that writes through
immediately. Useful for everyone, and genuinely enabling for commanders with disabilities.

| Toggle | What it does |
|--------|--------------|
| **Auto speed up for FTL** | Throttles up before a jump |
| **Auto lights off for FTL** | Kills ship lights before a jump |
| **Auto night vision off for FTL** | Drops night vision before a jump |
| **Auto hardpoints retract for FTL** | Retracts hardpoints before a jump |
| **Auto landing gear up for FTL** | Raises gear before a jump |
| **Auto cargo scoop retract for FTL** | Retracts the scoop before a jump |
| **Auto gear up on take off** | Raises gear after lifting off |
| **Auto exit UI before opening another panel** | Closes the open panel before opening the next one, so panel commands do not collide |
| **Auto lights off for SRV deployment** | Kills lights when you deploy the SRV |
| **Request fighter dock on FTL / cancel if out** | *Currently disabled* — waiting on a Frontier fix for a Nomad-related bug |

---

## Announcements

Everything Vega volunteers without being asked. All eleven toggles now live in one place, so
there is a single screen to check when something is talking too much — or not enough.

![Announcements](images/ui-commander-announcements.png)

| Toggle | What you hear |
|--------|---------------|
| **Announce discoveries** | Notable bodies, first discoveries, biological signals |
| **Announce route progress** | Where you are along a plotted route |
| **Announce radar contacts** | Ships appearing on the scanner |
| **Announce mining** | Mining events and yields |
| **Announce navigation** | Navigation events and arrivals |
| **Radio transmissions** | In-character radio chatter, spoken in a distinct radio voice |
| **Announce jump destination** | What the next system is |
| **Announce destination traffic** | Traffic reports for where you are heading |
| **Announce destination fatalities** | Recent deaths in the destination system |
| **Announce remaining jumps** | Jumps left on the route |
| **Announce fuel star availability** | Whether the destination has a scoopable star |

The first six can also be flipped by voice, so this screen re-reads them whenever you open the
tab — a spoken `toggle all announcements` will be reflected here.

---

## Fleet Voice Configuration

One row per ship you own. Elite Intel discovers your fleet from the game journal; you do not
add ships by hand.

| Column | Notes |
|--------|-------|
| **Ship** | Your ship's given name |
| **Ship Make** | The hull type |
| **Voice** | Click to pick. Changing it immediately plays a demo line in that voice so you can audition it |
| **Personality** | `PROFESSIONAL` · `CASUAL` · `FRIENDLY` · `UNHINGED` · `ROGUE` |
| **⚙** | Opens that ship's settings |

**About the voice list.** Ship voices are female. Which voices appear depends on the speech
engine selected in [Settings → AI Services](UI-Settings-Tab):

- **Local (Kokoro)** — 53 voices, labelled `Name - accent`. No key, no download, no setup.
- **Cloud (Google)** — labelled `Name - accent · HD` or `· Standard`. In English the accent
  tells the voices apart. In every other language each voice is synthesized in that language,
  so the label shows gender and the quality tier instead of a misleading English accent.

> Switching the speech engine resets every ship's voice to the new engine's default. Your ship
> **personalities are kept**. The app warns you before doing it.

---

## Ship Settings (the ⚙ button)

Per-ship settings, because a mining Python and a combat Corvette do not want the same
behaviour.

![Ship settings](images/ui-ship-settings.png)

**Honk system on entry** — performs a discovery scan when you arrive in a system. Pick the
**Fire Group** (A–H) and **Trigger** (1 or 2) your discovery scanner is mounted on. If your HUD
is in Combat mode, Elite Intel swaps to Analysis, scans, and swaps back.

**High grade emissions material alert** — tells you when a High Grade Emissions signal in the
system carries materials worth stopping for.

**Trade Profile** — the constraints Elite Intel obeys when it plots a trade route for this
ship. Every one of these can also be set by voice:
*"alter trade profile, set max stops to four"*.

| Setting | Meaning |
|---------|---------|
| **Allow Planetary Ports** | Include surface ports in routes |
| **Allow Prohibited Cargo** | Include cargo that is illegal somewhere on the route |
| **Allow Permit-Locked Systems** | Include systems needing a permit |
| **Allow Fleet Carriers** | Include player fleet carriers as markets |
| **Allow Stronghold Systems** | Include Thargoid/power stronghold systems |
| **Max Ls From Arrival** | How far from the arrival star a station may sit |
| **Max Stops** | Number of legs in the route |
| **Starting Capital** | Credits the route planner may spend |

See [Trade & Profit](TradeRoutePlotting) for how routes are flown.

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
