# EliteIntel AI Queries & Commands Guide

Hey Commander! This is a reference for the kinds of things you can ask or tell your **Elite Intel** sidekick.
**Ideally - You don't need to memorize any of these** - just speak naturally, and the app figures out what you mean. This
list exists so you know what's possible, not so you can recite scripts.

## First thing first. Having trouble?

- If app is firing random commands for no reason: brain problem (LLM is weak or configuration is wonky)
- If app is firing commands, but they do not activate: hands problem (Key bindings)
- If app can't hear you well: ears problem (noisy room low noise-to-rms ratio, audio not calibrated etc)
- If app does not speak: mouth problem. Audio connection is not routed properly or routed to an audio end point that you are not monitoring check audio routing at OS level.

### [Full Wiki is here](https://www.elite-intel.org/de)

## Audio Input

**Calibrate the audio in the app**. If the difference between Noise Floor and RMS is too low (e.g. less than 400) the
app will have hard time understanding you. A good ratio is at least 800-1000. Speakers and mic do not go together.
Headphones and mic are recommended.

## App

- Check missing or unbound key bindings.
- Sleep / Wake Up (Ignores user voice input in sleep mode, by pass by saying 'Listen Up', or switch back by saying 'Wake up')

## Exploration & Location

- Enable / Disable discovery announcements.
- Where are we right now?
- How far are we from the Bubble / our fleet carrier / last bio-sample?
- How far to a specific planet, moon, or station?
- What materials are available on this planet / moon?
- Analyze the most recent scan / body data.
- What's the ETA for our fleet carrier jump?
- What time is it / current UTC time? (real-time clock query)
- Analyze the biome for this star system.
- Which planets still need bio or organic scans?
- What bio scans have we completed?
- What organics do we still have to scan?
- What landable planets or moons are in this system?
- What signals are in this system?
- Which planets have geo signals?
- Open FSS and scan. / Perform filtered spectrum scan.
- System security, who controls this system, faction control?
- What is our player profile / ranks / stats?

## Exobiology

- What bio scans have we completed? / current star system (completed, partial, remaining).
- Exploration profit potential in this system.
- Last bio-sample location and distance. (requires codex entry scan with composition analyzer)
- Navigate to next bio-sample / organic / codex entry. (requires codex entry scan with composition analyzer)
- What organics or biology is on this planet / moon?
- Navigate to next organic / sample / codex entry.
- Biome analysis for [star system / planet / moon name].

## Fleet Carrier

Make sure you mention carrier, else it might think you are talking about the ship!

- What is our carrier range (includes reserve fuel if manually set) / stats / fuel.
- Set carrier fuel reserve [amount] (sets the fleet carrier tritium reserve)
- Where is our carrier jumping next?
- How long until carrier arrival? (Fleet carrier ETA)
- What's my fleet carrier fuel status / jump range / fuel reserve?
- How long can we operate on current funds?
- How far can carrier jump with current tritium?
- What's on the carrier route? / How many jumps remaining on carrier route?
- Distance from the fleet carrier?
- Fleet carriers in this system?

## Ship & Systems

- Enable / Disable route announcements.
- Is the next star scoopable?
- Analyze FSD target (allegiance, traffic, security, geo/bio data, etc.).
- What's in my cargo hold?
- What's your loadout?
- Analyze our route. (summary of the route, fuel availability)
- Fuel availability on route / next fuel stop.
- Do we have [material]? / How much [material] do we have? / Material inventory.
- What rank are we / player profile?

## Stations & Markets

- What services are at local stations?
- Outfitting / ship parts / modules for sale here?
- Any ships for sale at this station?
- Monetize my route (requires a trade profile set see Trade Profile Setup)
- Calculate trade route (requires a trade profile set see Trade Profile Setup)
- Where do I need to go to buy/sell [commodity]?
- What's in the local market?
- Data for stations, ports, and settlements in system.
- Remind me [blah] (reads reminders set during trade route run, tells you what station to go to etc.)
- What is our current trade plan / trade route / trade legs?

## Trade Profile Setup

- Change trade profile starting budget [amount]. (sets the starting capital for the trade route)
- Change trade profile max distance [X]. (will filter stations for this range from entry)
- Change trade profile max stops [N]. (route will be no longer than N stops)
- Change trade profile allow prohibited cargo [on/off].
- Change trade profile allow planetary port [on/off].
- Change trade profile allow permit systems [on/off].
- Change trade profile allow strongholds [on/off].
- Trade profile / trade settings / trade configuration. (describe current profile)
- List trade route parameters.

## ⚔️ Combat & Missions

- Radar contact on/off (turns the radar announcements on and off)
- Find hunting grounds (Will find target/mission provider pairs. Will tell you to fly to a target system and confirm the presence of the resource extraction site)
- Recon hunting ground / navigate to hunting ground (navigate to the target star system to scout it)
- Ignore hunting ground (skip the current hunting ground candidate)
- Confirm hunting ground (will manually confirm the presence of the resource extraction site, might be needed if we didn't detect it from beacon or auto-discovery)
- Navigate to mission provider system. / Navigate to pirate mission provider.
- Navigate to an active mission / plot route to active mission. (will navigate to the active mission star system)
- How many pirate kills left? (calculates the stack)
- Massacre mission progress / kills remaining / bounty hunt progress.
- What active missions do I have? (any missions, not just pirate)
- Total bounties collected this session.
- Target Subsystems commands: 'target drive', 'target fsd', 'target power distributor', 'target powerplant', 'target life support'. NOTE: optional subsystems are not included in the list.
- Target wingman 1 (wingman alpha) / wingman 2 (wingman bravo) / wingman 3 (wingman charlie).
- Priority target / target highest threat / next enemy / select hostile.

## 🧭 Navigation Commands

- Navigate to coordinates [latitude] [longitude]. (will guide you from orbit to location within 1 km, once landed within 100m)
- Navigate to landing zone. (will give you GPS nav back to where your ship landed last time)
- Navigate to next bio sample / codex entry.
- Navigate to nearest Fleet Carrier.
- Navigate to carrier / go to carrier / return to carrier.
- Navigate to / go to / head to / fly to / take me to / set course to / guide me to / plot route to [destination].
- Navigate to next trade stop / go to next trade stop.
- Calculate Fleet Carrier route. *(Open galaxy map, select star, copy name to clipboard first.)*
- Enter carrier destination / set carrier destination. *(Open Fleet Carrier galaxy map first.)*
- Calculate trade route / plot profitable trade route.
- Find nearest Vista Genomics.
- Find nearest human / guardian tech broker.
- Find nearest raw / encoded / manufactured material trader.
- Find / locate / search for brain trees [within X light years].
- Find mining location for [material] [within X light years].
- Find where we can mine Tritium [within X light years].
- Find where we can buy [commodity] [within X light years].
- Find hunting grounds.
- Cancel navigation.
- Plot route to Fleet Carrier.
- Take me home / navigate to home system.
- Set as home system.
- Set optimal speed. *(Sets throttle to 75% for supercruise approach.)*
- Increase / Decrease speed by [amount]. 1-10
- Set carrier fuel reserve to [amount]. (sets the fleet carrier tritium reserve)
- Select FDS destination / target destination. (selects the next system in the plotted route)
- Wing nav lock / lock wingman nav.
- Select / target highest threat / next hostile / target enemy.
- Navigate from memory. (This will open the galaxy map and navigate to the location you have copied from somewhere via Ctrl+C.)

## 🎮 Ship Controls

- Gear up / down | deploy / retract landing gear.
- Deploy / retract hardpoints / weapons.
- Weapons Hot! (same as deploy hardpoints)
- Deploy heat sink.
- Deploy / recover SRV / car / buggy.
- Board ship / return SRV / SRV dock. (recovers SRV while in SRV mode)
- Disembark. (exit ship on foot)
- Open / close cargo scoop, cargo bay, cargo hatch.
- Request docking. / Contact tower, get a landing pad, parking spot etc.
- Launch ship / leave station / detach from station.
- Taxi to landing / auto land / autopilot landing.
- Engage Supercruise / enter supercruise / go supercruise.
- Jump to hyperspace / enter hyperspace / lets go / next waypoint. (actual jump, not supercruise)
- Drop out / drop here / drop ftl / leave supercruise / drop from supercruise.
- Full stop / engine stop / set speed zero / kill engines.
- Set speed to: quarter / half / three-quarter / full throttle.
- Set speed plus / minus [amount].
- All power to shields / engines / weapons. Equalize power. / max shields, max engines, max weapons.
- Combat mode / switch to combat mode → switches to combat HUD.
- Analysis mode / switch to analysis mode / explorer mode → switches to analysis HUD.
- Night vision (on / off).
- Headlights (on / off).
- Drive assist on / off. (SRV mode)
- Dismiss ship / go to orbit / go play.
- Return to the surface / pick me up.
- Open FSS and scan / honk / scan system / run a scan / discovery scan / system scan / full spectrum scan. (Will open the full FSS UI and honk)
- Show / display - galaxy map / local map / close map.
- Exit / close (Exits menus / tabs / sub-menus and drops you to HUD).
- Interrupt / shut up / silence / cancel [stops TTS mid-sentence].
- Activate (activates whatever is selected in the UI).

## 🎙️ Fighter Commands

- Deploy fighter / launch fighter / send out fighter.
- Order fighters to defend the ship.
- Order fighter to focus on my target / attack my target / fire at will / fighter open orders.
- Order fighter to hold fire / fighter cease fire.
- Order fighter to return to mothership / recall fighter / fighter dock.

## 📺 UI Panels

Say **show**, **open**, or **display** followed by the panel name:

- Navigation panel
- Transactions panel
- Contacts panel
- Chat panel / comms panel
- Email inbox panel
- Social panel
- History panel
- Squadron panel
- Status panel
- Commander panel / role panel / knee board
- Crew panel
- Home panel (internal panel)
- Modules panel
- Fire groups
- Inventory panel
- Storage panel
- Fighter panel
- Carrier management panel
- Galaxy map
- Local / system map
- Services panel (SRV docked at station)

Other panel commands:
- Exit / close panel (exits menus and returns to HUD).

## ⚙️ App & Session Commands

- **"Ignore me" / "do not monitor" / "sleep"** → puts the app to sleep (ignores all input).
- **"Wake up"** → resumes normal listening.
- **"Listen up [command]"** → bypass: passes a single command or query through while the app stays asleep. The "listen up" prefix is stripped before the command reaches the AI. Example: *"Listen up, jump to hyperspace"* executes the jump without waking the app.
- Set reminder [text]. / Remind me [text].
- Clear reminders.
- Toggle route announcements on / off.
- Toggle discovery announcements on / off.
- Toggle mining and material announcements on / off.
- Toggle radio chatter / radio traffic on / off.
- Disable all announcements.
- Radar contact announcement on / off.
- Add mining target [material name].
- Remove mining target [material name].
- Clear mining targets.
- Delete codex entry. (Deletes the codex entry you are GPS tracking)

## 💬 General Chat (Conversation Mode must be ON)

By default the app runs in **Strict Mode**: if input doesn't match a known command or query it is silently ignored.
This is intentional — it prevents STT noise and background chatter from triggering random actions mid-flight.

Enable **Conversation Mode** in the Settings tab to turn on free-form chat. When on, anything that doesn't match a
command falls back to general conversation — game lore, real-world topics, ship builds, whatever. The AI is not just
a command parser when you want it to be more.

Local LLMs will respond but will be stiff. Cloud LLMs (Claude, OpenAI, xAI, Mistral, Deepseek) are recommended for conversation.

---

[More commands here](https://www.elite-intel.org/)

Fly Dangerous, Commander! o7

----
Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈 | Open Source [**GitHub**](https://github.com/SudoKrondor/EliteIntel) | [YouTube](https://www.youtube.com/@SudoKrondor) | [Twitch](https://www.twitch.tv/sudokrondor) | Cretive Commons License |