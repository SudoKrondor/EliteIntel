# EliteIntel AI Queries & Commands Guide

This document is a reference for queries and commands supported by **Elite Intel**.
Memorizing specific phrases is not required. Speak naturally and the app determines your intent.
This list shows what is possible, not a script to recite.

## Troubleshooting

- Random commands fire without cause: brain problem (the LLM is weak or the configuration is incorrect)
- Commands fire but do not activate: hands problem (key bindings)
- Poor speech recognition: ears problem (noisy environment, low noise-to-RMS ratio, audio not calibrated)
- App does not speak: mouth problem. The audio is not routed correctly, or is routed to an endpoint you are not monitoring. Check audio routing at the OS level.

### [Full Wiki](https://github.com/stone-alex/EliteIntel/wiki)

## Audio Input

**Calibrate audio in the app.** If the difference between Noise Floor and RMS is below 400, the app will struggle to recognize speech. A ratio of 800 to 1000 or higher is recommended. Speakers and microphone used together cause interference. Use headphones with a dedicated microphone.

## App

- Check missing or unbound key bindings.
- Sleep / Wake Up (Sleep mode ignores voice input. Say "Listen Up" to pass a single command, or say "Wake up" to resume.)

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

Include the word "carrier" to distinguish from ship commands.

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
- Remind me \<blah\> (reads reminders set during trade route run, tells you what station to go to etc.)
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
- **"Listen up \<command\>"** → bypass: passes a single command or query through while the app stays asleep. The "listen up" prefix is stripped before the command reaches the AI. Example: *"Listen up, jump to hyperspace"* executes the jump without waking the app.
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
This is intentional. It prevents speech-to-text noise and background chatter from triggering random actions mid-flight.

Enable **Conversation Mode** in the Settings tab to allow free-form chat. When enabled, any input that does not match a command falls back to general conversation, including game lore, real-world topics, ship builds, and other subjects. The AI functions as more than a command parser when this mode is active.

Local LLMs will respond but with limited conversational quality. Cloud LLMs (Claude, OpenAI, xAI) are recommended for conversation.

---

[More commands here](https://github.com/stone-alex/EliteIntel/wiki/Obscure-System-Commands)

Fly Dangerous, Commander! o7

----
Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈 | Open Source [**GitHub
**](https://github.com/stone-alex/EliteIntel) | [YouTube](https://www.youtube.com/@SudoKrondor) | [Twitch](https://www.twitch.tv/sudokrondor) | Cretive Commons License |
