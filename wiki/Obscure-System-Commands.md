# System Commands — the Details

[All Commands](AllCommands) lists what you can say. This page covers the commands that need a
bit more explanation: what to do *before* you say them, what they actually do, and the gotchas.

Everything here works spoken, typed in the game chat as `@Vega …`, or clicked on the
[Actions tab](UI-Actions-Tab).

---

## Settings You Can Flip by Voice

Every spoken announcement has a voice toggle, and each one mirrors a switch on the
[Commander tab → Announcements](UI-Commander-Tab), so you can check what is on at a glance.

- **Route announcements**: *"Route announcements off."* Silences everything said around a jump —
  next system, traffic, fatalities, arrival, jumps left, scoopable star. Manual queries still work.
- **Discovery announcements**: *"Discovery announcements on."* First discoveries, valuable
  bodies, biological signals. Also controls the Exobiology card on the [HUD overlay](UI-HUD-Overlay).
- **Planetary approach announcements**: *"Planetary approach announcements on."*
- **Mining announcements**: *"Mining announcements on."* Prospector hits for your mining targets.
  Add targets first: *"Add mining target painite."* Without targets there is nothing to announce.
- **Cargo scoop pickups**: *"Announce cargo scoop pickups."*
- **Radar contacts**: *"Radar announcements off."*
- **Radio**: *"Radio on."* In-character radio traffic — pirate threats, traffic control — in its
  own radio voices. Its volume and effects are in [Settings → Audio](UI-Settings-Tab).
- **Everything at once**: *"All announcements off."*
- **Night vision / lights / drive assist**: *"Night vision on."* *"Lights off."* *"Drive assist
  off."*

> **Streaming or flying in a wing?** There is no special "streaming mode". To stop Vega reacting
> to other voices, put her to sleep (*"Sleep"*) and prefix the occasional order with *"Listen
> up, …"* — or use [push-to-talk](UI-Settings-Tab), which ignores everything unless the button is
> held.

---

## Navigation & Finding Things

Vega plots routes to the result of a search, to places she already knows, or to surface
coordinates. She does not plot to a system you name aloud — see
[a custom command for places you visit often](UI-Actions-Tab).

- **Navigate to coordinates**: *"Navigate to coordinates latitude 41.43 longitude -75.23."*
  Guidance from orbit to the spot on the current or approached body. On the dark side you will be
  flying on instruments.
- **Next bio sample / codex entry**: *"Navigate to next bio sample."* Guides you to the nearest
  saved organism location on this planet. *"Delete this codex entry"* drops the one you are
  tracking.
- **Landing zone**: *"Navigate to landing zone."* Back to where your ship last landed.
- **Your carrier**: *"Navigate to fleet carrier"* / *"navigate to squadron carrier."* Plots to the
  carrier's last known location — or to your home system if no carrier is known.
- **Home**: *"Set home system"* marks where you are now (it asks you to confirm); *"take me home"*
  plots back to it.
- **Navigate from memory**: copy a system name to the clipboard (Ctrl+C) from INARA, Spansh or a
  chat message, then say *"navigate from memory."* Vega opens the galaxy map and plots to it.
- **Carrier route**: open the galaxy map, select the destination and copy its name, then say
  *"calculate fleet carrier route."* The route comes from Spansh, so the system must be known
  there.
- **Enter next carrier destination**: open the *carrier's* galaxy map and say *"enter carrier
  destination."* Vega types the next leg of the stored route and confirms it — repeat after each
  jump.
- **Neutron route**: copy the destination name from the galaxy map and say *"calculate neutron
  route"* (optionally *"…efficiency 60"*, *"…with supercharge"*). Then *"next neutron star"* after
  each boost — or turn on *Auto plot next neutron jump on cone boost* on the
  [Commander tab](UI-Commander-Tab).
- **Traders and brokers**: *"Find raw / encoded / manufactured material trader."* *"Find human /
  guardian tech broker."* *"Find nearest Vista Genomics."* *"Find nearest interstellar factor."*
  Vega plots the route and leaves a reminder naming the station; ask *"what was the reminder?"*
  when you arrive.
- **Brain trees**: *"Find brain trees for [material] within 500 light years."* Finds a Guardian
  brain-tree site that yields that raw material.
- **Mining sites**: *"Where can I mine osmium within 200 light years?"* Works for tritium too.
- **Buying and selling**: *"Where can I buy bromellite within 150 light years?"* — add *nearest*
  or *best price*; it works for ship modules as well. *"Where can I sell gold?"*
- **Fuel**: *"Find a fuel station"* / *"I need fuel."*
- **Bad search results**: if a search keeps sending you somewhere that does not work, say
  *"exclude this system from searches"* (or *"exclude that system"* for your destination). Undo
  it from inside the system: *"allow this system in searches again."*

---

## Combat & Missions

- **Learn from your history first**: *"Scan journals for hunting grounds."* Reads your saved game
  journals and learns every system with resource extraction sites and every pirate massacre
  mission provider you have ever seen. Run it once after installing.
- **Hunting grounds**: *"Find hunting grounds within 100 light years."* A system with resource
  extraction sites, picked from systems you have flown through. *"Forget this hunting ground"*
  removes one.
- **Massacre stacking**: *"Find pirate massacre missions"*, *"navigate to pirate mission
  provider"*, *"navigate to pirate mission target"*, *"how many kills left?"*
- **Conflict zones**: *"Find a conflict zone."*
- **Missions**: *"Navigate to active mission."* *"Find mission cargo"* finds where to buy what an
  active mission still needs — the one expiring soonest whose cargo you do not already carry.
- **Subsystems**: *"Target power plant"* (also drive, FSD, power distributor, life support,
  shield).

---

## Ship Control Shortcuts

- **Power distribution**: *"Power to shields"*, *"max engines"*, *"equalize power."* One command
  sets all the pips.
- **Close / exit**: *"Close"* or *"exit"* backs out of the open panel or map.
- **Honk**: *"Honk"* fires the discovery scanner on the fire group you set per ship (Commander tab
  → ⚙). *"Open FSS"* opens the full spectrum scanner.
- **Optimal speed**: *"Set optimal speed."* Sets the throttle to 75% — the supercruise sweet spot.
  Say it about 20 seconds out from the target to avoid looping round it.
- **Target next system in route**: *"Target destination."*
- **Wing nav lock**: *"Wing nav lock."*
- **Fire groups**: *"Fire group bravo"* — NATO letters or numbers.
- **Dismiss / recall**: *"Dismiss ship"* sends it to orbit; *"pick me up"* brings it back.

---

## Utility & Session Commands

- **Reminders**: *"Set reminder, pick up painite at Hutton Orbital."* Saved until you clear it;
  *"what was the reminder?"* reads it back. *"Clear reminders"* asks you to confirm first.
- **Timers**: *"Remind me in 20 minutes to check the carrier."*
- **Monetize route**: *"Monetize route."* Finds one profitable buy/sell pair along the route you
  have plotted and saves it as a reminder — trade, not exploration. It shows on the
  [HUD overlay](UI-HUD-Overlay) as a *Cargo opportunity*.
- **Self diagnostic**: *"Run a diagnostic"* / *"are you working properly?"* Tests the connection to
  the language model and reports whether it answers and how quickly.
- **Interrupt**: *"Interrupt."* Stops Vega mid-sentence. With push-to-talk, pressing the button
  does the same.
- **Biome analysis**: *"Analyze biome"* (or name a planet). Reports what is likely to grow there
  before you land.
- **Carrier finances and range**: *"Carrier status"* / *"how long can we operate the carrier?"* /
  *"how far can the carrier jump?"* Fuel, reserve, jump range, balance and running time.
- **Construction sites**: *"Construction site progress"*, *"find construction cargo"*, *"take me
  back to the construction site"*, *"dismiss the construction site."*

---

## Usage Notes

- **Natural language**: no formal syntax. Say what you mean.
- **Names go in the chat**: system, station and commodity names are where speech recognition
  struggles most. Type those orders: `@Vega where can I buy tritium`.
- **Destructive commands ask first**: clearing reminders, mining targets, missions or a route,
  deleting a codex entry, forgetting a hunting ground, excluding a system or setting a new home.
  Answer *yes*; anything else cancels.
- **VR**: power distribution and close/exit save you from hunting through menus in the headset.

----
Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
