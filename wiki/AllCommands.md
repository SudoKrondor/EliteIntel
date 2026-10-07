# Elite Intel Commands & Queries

Hey Commander! This is a reference for the kinds of things you can ask or tell **Vega**, your
ship's AI. **You don't need to memorise any of it** — speak naturally and Vega works out what you
mean. The phrases below are examples, not scripts: what matters is the meaning, not the exact
words.

> **The definitive, always-current list is in the app.** The
> [Actions tab → Built-in Commands](UI-Actions-Tab) lists every command and query this build has,
> filtered to what you can use where you are right now, with the training phrases in your own
> language. You can run any of them from there with a click.

---

## Talking to Vega

**Speak naturally.** *"Gear down"*, *"lower the landing gear"* and *"prepare for landing"* all do
the same thing. The more clearly you say what you want, the more reliably it happens.

**Addressing her by name is optional.** *"Vega, gear down"* works exactly like *"gear down"*.

**Sleep and wake.**

- *"Sleep"* / *"ignore me"* / *"do not monitor"* — Vega stops listening.
- *"Wake up"* — she listens again.
- *"Listen up, …"* — while asleep, gets **one** order through without waking her:
  *"Listen up, jump to hyperspace."*

**Interrupt.** Say *"interrupt"* to cut Vega off mid-sentence. With
[push-to-talk](UI-Settings-Tab) on, simply pressing the button does the same.

**Destructive commands ask first.** Clearing reminders, mining targets, active missions, a
trade, neutron or carrier route, deleting a codex entry, forgetting a hunting ground, dismissing a
construction site, excluding a system from searches, or setting a new home system — Vega asks
you to confirm. Answer *yes* to go ahead; *no*, or anything else, cancels it.

---

## Typing commands in the game chat

Speech recognition will always mangle some words — system names, commodity names, *Tritium*.
For those, **type the order into the in-game chat** instead. Start the line with `@Vega`:

```
@Vega find where to buy tritium within 200 light years
@Vega add mining target painite
@vega, gear down
```

- Any capitalisation works, and a comma or colon after the name is fine.
- The line is handled exactly as if you had said it, and it interrupts Vega if she is speaking.
- Use the **local** channel. Only your own sent lines are read — nothing another commander types
  can ever give Vega an order.
- In Russian and Ukrainian the Cyrillic spelling (`@Вега`) works too.

---

## ⚙️ App & Session

- Sleep / wake up / *"listen up, [command]"* — see above.
- *"Interrupt"* — stop speaking.
- *"Run a diagnostic"* / *"are you working properly?"* — Vega checks herself and reports.
- *"What time is it?"* — real-world UTC time.
- *"Set reminder [text]"* — a standing note; *"what was the reminder?"* reads it back.
- *"Remind me in 10 minutes to check the carrier"* — a countdown timer.
- *"Clear reminders."*

### Announcements on/off

- *"Turn off radar announcements"* / *"radar announcements on"*
- *"Discovery announcements on / off"*
- *"Route announcements on / off"*
- *"Planetary approach announcements on / off"*
- *"Mining announcements on / off"*
- *"Announce cargo scoop pickups"* / *"stop announcing cargo scoop pickups"*
- *"Radio on / off"* — radio transmissions
- *"All announcements off"* / *"turn on all announcements"*

Every one of these is also a toggle on the [Commander tab → Announcements](UI-Commander-Tab).

---

## 🎮 Ship Controls

- **Landing gear:** *"gear down"*, *"prepare for landing"* / *"gear up"*
- **Hardpoints:** *"hardpoints"*, *"weapons hot"*, *"weapons free"* / *"retract hardpoints"*,
  *"weapons cold"*, *"stand down"*
- **Cargo scoop:** *"open / close cargo scoop"*
- **Lights / night vision:** *"lights on"*, *"headlights off"*, *"night vision"*
- **HUD mode:** *"combat mode"* / *"analysis mode"*
- **Defensive:** *"heat sink"*, *"deploy shield cell"*, *"chaff"* / *"flares"*
- **Fire groups:** *"fire group bravo"*, *"select fire group 3"*
- **Power:** *"power to shields / engines / weapons / systems"* (*"max shields"*, *"pips to
  engines"*), *"equalize power"*
- **Head look:** *"look ahead"* / *"recenter view"*
- **Activate:** *"activate"* — presses whatever is selected in the open panel.

### Throttle

- *"Full stop"* / *"kill engines"* / *"all stop"*
- *"Quarter throttle"*, *"half speed"*, *"three quarters throttle"*, *"full throttle"*
- *"Increase speed by 2"* / *"slow down by 1"*
- *"Optimal speed"* — sets the throttle for a supercruise approach.

### Flight

- *"Launch"* / *"undock"* / *"take off"* — leave the pad.
- *"Request docking"* / *"request landing permission"*
- *"Taxi"* / *"auto dock"* — hand the landing to the docking computer.
- *"Enter supercruise"* / *"supercruise"*
- *"Jump"* / *"enter hyperspace"* / *"let's go"* / *"next waypoint"* — the actual jump.
- *"Drop out"* / *"drop here"* / *"leave supercruise"*
- *"Target destination"* — selects the next system on your plotted route.
- *"Honk"* / *"discovery scan"* — fires the discovery scanner (see the per-ship honk setting on
  the [Commander tab](UI-Commander-Tab)).
- *"Open FSS"* / *"full spectrum scan"* — opens the FSS and scans.

---

## 🚙 SRV, Fighter & On Foot

- *"Deploy SRV"* / *"launch SRV"* — opens the right hangar bay (set your bays under each ship's
  ⚙ settings on the [Commander tab](UI-Commander-Tab)).
- *"Launch nomad"* / *"deploy planetary scout"*
- *"Board ship"* / *"recover SRV"* / *"SRV dock"* — from the SRV.
- *"Drive assist on / off"*
- *"Disembark"* / *"step outside"*
- *"Dismiss ship"* / *"go play"* — send the ship to orbit.
- *"Pick me up"* / *"return to surface"* — call it back.
- *"Station services"* — the services panel, when docked in an SRV.

### Fighter orders

- *"Deploy fighter"*
- *"Fighter defend"* · *"Fighter attack my target"* · *"Fire at will"* · *"Fighter hold fire"* ·
  *"Recall fighter"*

---

## ⚔️ Combat & Missions

- **Targets:** *"target highest threat"*, *"next enemy"*, *"select hostile"*
- **Subsystems:** *"target FSD"*, *"target power plant"*, *"target drive"*, *"target power
  distributor"*, *"target life support"*, *"target shield"*
- **Wing:** *"target wingman one / two / three"* (or *alpha / bravo / charlie*),
  *"wing nav lock"* / *"follow wingman"*
- *"What are our missions?"* / *"mission status"* — everything on your board.
- *"Navigate to active mission"* / *"take me to mission"*
- *"Find mission cargo"* — where to buy what an active mission still needs, and plot it.
- *"Clear active missions"*
- *"Total bounties"* — bounties collected.

### Pirate massacre stacking

- *"Find pirate massacre missions"* / *"where can I stack massacre missions?"*
- *"Navigate to pirate mission provider"*
- *"Navigate to pirate mission target"*
- *"How many kills left?"* / *"massacre progress"*

### Bounty hunting & conflict zones

- *"Find hunting grounds within 100 light years"* — a system with resource extraction sites,
  from systems you have already flown through.
- *"Scan journals for hunting grounds"* — learn RES systems and massacre providers from your old
  journals in one go. Run it once after installing.
- *"Forget this hunting ground"*
- *"Find a conflict zone"* / *"where is the nearest war?"*

See [Pirate Missions](Pirate-Massacre-Mission-Tracking).

---

## 🧭 Navigation

Vega plots routes to **the result of a search**, to places she already knows, or to surface
coordinates. She **cannot** navigate to a system you simply name aloud — names are where speech
recognition fails most, and a wrong guess sends you to the wrong side of the bubble. Use
*navigate from memory*, or — for a place you visit often — build a
[custom command that plots the route for you](UI-Actions-Tab).

- *"Navigate from memory"* / *"paste from memory"* — copy a system name (from INARA, Spansh, a
  chat message…) with Ctrl+C first; Vega opens the galaxy map and plots to it.
- *"Take me home"* / *"set home system"*
- *"Navigate to fleet carrier"* / *"navigate to squadron carrier"*
- *"Navigate to next trade stop"*
- *"Cancel navigation"*

### On a planet

- *"Navigate to coordinates latitude 12.5 longitude -40.2"* — guidance from orbit to the spot.
- *"Navigate to landing zone"* / *"back to LZ"* — back to where your ship landed.
- *"Navigate to next bio sample"* / *"go to codex entry"* — the nearest tagged organic.
- *"Delete this codex entry"*

### Neutron highway

- *"Calculate neutron route"* — copy the destination name from the galaxy map first.
  Options: *"…efficiency 60"*, *"…with supercharge"*.
- *"Next neutron star"* — plot to the next neutron waypoint (or let the *auto plot next neutron
  jump* setting do it on the [Commander tab](UI-Commander-Tab)).
- *"Clear neutron route"*

### Finding places

Every *find* command plots a route to what it finds, and puts it on the
[HUD overlay](UI-HUD-Overlay).

- *"Find raw / encoded / manufactured material trader"*
- *"Find human / guardian tech broker"*
- *"Find nearest Vista Genomics"*
- *"Find nearest interstellar factor"* / *"where can I pay off my bounty?"*
- *"Find a fuel station"* / *"I need fuel"*
- *"Find nearest fleet carrier"*
- *"Find brain trees within 500 light years"*
- *"Find where to mine painite within 200 light years"* / *"find mining site"*
- *"Exclude this system from searches"* (or *"that system"* — your destination) — when a search
  keeps sending you somewhere that does not work. *"Allow this system in searches again"*, said
  from inside it, undoes it.

---

## 💰 Trade & Markets

- *"Where can I buy [commodity] within 100 light years?"* — also works for ship modules. Say
  *nearest* or *best price*.
- *"Where can I sell [commodity]?"*
- *"Calculate trade route"* — uses this ship's trade profile.
- *"Monetize route"* — a profitable cargo for the journey you are already on.
- *"What is our trade route?"* / *"trade legs"*
- *"Navigate to next trade stop"*
- *"Cancel trade route"*
- *"Local markets"* · *"Station details"* / *"what services here?"* · *"Outfitting"* ·
  *"Shipyard"* / *"ships for sale"*
- *"What are we carrying?"* — cargo hold.

### Trade profile

Also editable per ship on the [Commander tab](UI-Commander-Tab) ⚙ settings.

- *"Trade profile"* — describe the current one.
- *"Change trade profile starting budget 5 million"*
- *"Change trade profile max stops 4"*
- *"Change trade profile max distance 1000"*
- *"Allow / block prohibited cargo"*
- *"Allow / block planetary ports"*
- *"Allow / block permit systems"*
- *"Allow / block strongholds"*

See [Trade & Profit](TradeRoutePlotting) and [Commodity Searching](Search-galaxy-with-EliteIntel).

---

## 🏗️ Colonisation

- *"Find construction cargo"* — what the construction site still needs, where to buy it, and
  how to fill the hold.
- *"Construction site progress"* / *"how is the build going?"*
- *"Take me back to the construction site"*
- *"Dismiss the construction site"* — stop tracking it.

---

## 🛰️ Fleet Carrier

Say *carrier* — or *squadron carrier* — or Vega may think you mean the ship.

- *"Carrier status"* — fuel, range with current tritium, finances, how long you can operate.
- *"Set carrier fuel reserve 200"* — tritium kept in reserve.
- *"Calculate fleet carrier route"* — copy the destination name from the galaxy map first.
- *"Enter carrier destination"* — with the carrier's galaxy map open, Vega types the next leg in
  and confirms it.
- *"Carrier route"* / *"jumps left on carrier"*
- *"Carrier ETA"* / *"when does the carrier arrive?"*
- *"Distance to carrier"*
- *"Cancel carrier route"*
- *"Carriers in system"*

---

## 🌠 Exploration & Exobiology

- *"Where are we?"* — current location.
- *"Distance to the bubble"* / *"how far from Sol?"*
- *"How far to [planet / moon / station]?"*
- *"Last scan"* — the most recent body you scanned.
- *"Planets in system"* / *"landable planets"* / *"rings in system"*
- *"Signals in system"* · *"Geo signals"*
- *"System security"* / *"who controls this system?"*
- *"FSD target info"* / *"next jump"* — analyse the system you are about to jump to.
- *"Route analysis"* / *"jumps left"* / *"is the next star scoopable?"*
- *"Exploration profits"* — what your scans are worth.
- *"Planet materials"* — what is on this body.
- *"Bio signals in system"* · *"What's left to scan?"* · *"Analyze biome"*
- *"Distance to last bio sample"*
- *"We have already sampled this body"* — mark it done if you sampled it before installing.

### Exo-Mastery

Once the catalogue is enabled on the [Commander tab](UI-Commander-Tab):

- *"Take me to the next exobiology site"* — the richest system you have not sampled out yet.
- *"Flag this system as harvested"* — write off a whole system you did before.

See [Discovery](Discovery-Assistance).

---

## ⛏️ Mining

- *"Add mining target painite"* / *"remove mining target painite"* / *"clear mining targets"*
- *"Mining announcements on"* — prospector hits for your targets.
- *"Find where to mine [material]"*

With targets set and a refinery fitted, the [HUD overlay](UI-HUD-Overlay) shows a mining card.

---

## 👤 Commander & Ship

- *"Player profile"* — ranks and progress.
- *"Ship loadout"* / *"what am I flying?"*
- *"Material inventory"* / *"how much [material] do we have?"*

---

## 📺 Panels & Maps

Say **show**, **open**, or just the panel name:

- *Navigation* · *Transactions* · *Contacts* · *Comms / chat* · *Email inbox* · *Social* ·
  *History* · *Squadron* · *Status* · *Radar*
- *Commander panel* (*knee board*) · *Crew* · *Internal* (*home*) · *Modules* · *Fire groups* ·
  *Inventory* · *Storage* · *Fighter panel*
- *Carrier management*
- *Galaxy map* · *System map*
- *"Next / previous panel"*, *"next / previous page"* — cycle tabs inside a panel.
- *"Close"* / *"exit"* / *"close map"* — back to the HUD.

---

## 🎵 Music

*"Play music"*, *"pause the music"*, *"next track"*, *"previous track"*, *"restart the
playlist"*, *"shuffle the music"*, *"play the song [title]"*. See the
[Jukebox tab](UI-Jukebox-Tab).

---

## Your own commands

Anything missing, you can build yourself: [Actions → Custom Commands](UI-Actions-Tab). Custom
commands are triggered by your own phrases, spoken or typed in chat, exactly like the built-in
ones.

---

Fly Dangerous, Commander! o7

----
Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈 | Open Source [**GitHub**](https://github.com/SudoKrondor/EliteIntel) | [YouTube](https://www.youtube.com/@SudoKrondor) | [Twitch](https://www.twitch.tv/sudokrondor) | Creative Commons License |
