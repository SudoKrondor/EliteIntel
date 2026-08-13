# HUD Overlay

**New in V1.1.** An always-on-top overlay that puts your current objectives on screen — in the
game window, or inside a VR headset.

![HUD overlay in game](images/ui-overlay-ingame.png)

It replaces the old OBS overlay window. The overlay runs out of process, so it does not
compete with the game or the app for the interface thread.

The card is drawn to the cockpit's geometry rather than square to your monitor, so it leans the
way the ship's own panels lean at that spot on screen — move it and the lean changes to suit. Its
rows are slanted lines, which is why a value can sit noticeably lower than the label it belongs
to: read each row along the slant, the same way you read the game's own readouts beside it. How
far a value appears to fall depends on **TEXT SIZE** as well as placement — the lean is fixed by
the cockpit, so smaller text means shorter rows and the same drop crosses more of them.

Turn it on with **DISPLAY OVERLAY** on the [Vega tab](UI-Vega-Tab), and configure it with
**OVERLAY SETUP** next to it.

> If the overlay binary is missing from the distribution, the toggle stays off and says so in
> the diagnostics log. It will not claim an overlay that is not there.

---

## What it shows

The overlay draws **cards** — one per live objective, derived from what you are actually doing.
Cards appear and disappear on their own; there is nothing to configure.

| Card | Appears when |
|------|--------------|
| **EXOBIOLOGY** | You are sampling organics — genus, and what is left to find |
| **MASSACRE CONTRACT** | You are running massacre missions — kills required, stack, reward |
| **MINING** | You are mining — hold, limpets, target commodity |
| **TRADE ROUTE** | A trade route is plotted — commodity, buy, sell, margin, leg *n* of *m* |
| **CARGO OPPORTUNITY** | A profitable cargo has been spotted for what you are carrying |
| **MISSION** | A featured mission — target, cargo or passengers, expiry, reward |
| **PLOTTED ROUTE** | A route is set — destination, next system, jumps remaining |
| **MATERIAL TRADER** · **TECHNOLOGY BROKER** · **INTERSTELLAR FACTORS** · **VISTA GENOMICS** | You have set a destination reminder to go see one |

The featured mission card picks its mission the way you would: the one at your route
destination first, then one in your current system, then the most recently accepted.

---

## Overlay setup

![Overlay settings](images/ui-overlay-settings.png)

**BACKGROUND TRANSPARENCY** (0–100%) and **TEXT SIZE** (75–200%) are two separate controls on
purpose. A single "opacity" slider would fade the text along with the backdrop, which is exactly
what makes a dimmed overlay unreadable over a bright planet surface. Fade the background; leave
the text alone.

### DISPLAY ON

| Mode | What it does |
|------|--------------|
| **Monitor** | A desktop window. The default, and what every version before V1.1 did. The card leans to match the cockpit, and the lean changes with where you place it — see below |
| **VR headset** | A SteamVR overlay. Needs SteamVR running. If VR cannot be had it falls back to a desktop window, so you are never left with nothing |
| **Monitor and headset** | Both at once, fed identical data. Useful if you fly in VR but stream or record from the monitor |
| **VR capture window** | A plain, flat, opaque window for a capture tool to pin |

### About VR capture window

This mode does **not** talk to SteamVR. Start your capture tool — Desktop+, OVR Toolkit, or
Virtual Desktop — and pick the window named **"EliteIntel HUD (VR capture)"**.

Why it exists: the SteamVR mode hands the compositor a full texture per typed character, and on
a streamed headset that has been reported as a real frame-rate cost. A capture tool takes the
window on the GPU on its own schedule, and gives you placement and curvature controls this app
does not have.

It is a separate mode rather than "point your capture tool at the Monitor window", because that
window leans, is see-through, and is a tool window — which capture pickers filter out entirely.

### POSITION IN HEADSET

Eight placements: **Above, Above right, Right, Below right, Below, Below left, Left,
Above left.**

> **The HUD is fixed in front of your seat and does not follow your head.** The direction you
> pick is measured from where you face after SteamVR's *Reset Seated Position* — so recentring
> your view moves the HUD along with the cockpit, which is what you want. Look away and the HUD
> stays where you left it, exactly like a physical panel.

---

## Reading it in another language

Card labels follow the app's language, and numbers are grouped the way that language groups
them. Names the game supplies — systems, stations, commodities — pass through untouched.

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
