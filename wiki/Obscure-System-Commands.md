# Obscure System Commands

EliteIntel uses a minimal interface with no menus or complex settings panels. A full set of features is available via voice commands. All settings and functions are accessible through natural speech.

## Toggleable Settings

Toggle these settings using voice commands:

- **Route Announcements [On/Off]**: "Turn Route Announcements On." EliteIntel announces details about your next jump, including whether the star is scoopable, system security, and other relevant information. When disabled, announcements stop. Manual queries remain available. *Default: On*
- **Discovery Announcements [On/Off]**: "Turn Discovery Announcements On." EliteIntel announces first-mapped systems, high-value planets, bio signals, and similar discoveries. *Default: On*
- **Mining and Material Announcements [On/Off]**: "Turn Mining Announcements On." EliteIntel flags prospector limpet hits and material finds. *Default: Off*
- **Add Mining Target [Material Name]**: "Add Mining Target Painite." EliteIntel flags hits for that specific material. No effect if no target is set. *Default: Off*
- **Radio Chatter [On/Off]**: "Turn On the Radio." EliteIntel announces relevant radio transmissions such as pirate threats, while filtering routine traffic like commercial broadcasts. Useful for cargo runs. *Default: Off*
- **Streaming Mode [On/Off]**: "Set Streaming Mode On." EliteIntel only responds to commands prefixed with "Computer." Useful for streaming or when in a wing. Say "Computer, turn streaming mode off" to disable.
- **Night Vision [On/Off]**: "Night vision on." Toggles night vision in the ship or SRV.
- **Headlights [On/Off]**: "Headlights on."
- **Drive Assist [On/Off]**: "Drive assist off."

## Navigation & Finding Things

Beyond basic route plotting, EliteIntel supports targeted navigation:

- **Navigate to Coordinates**: "Navigate to latitude 41.4325 longitude -75.2309." EliteIntel guides you from orbit to within 50 meters. Dark side navigation requires instrument flight.
- **Navigate to Bio Sample / Codex Entry**: "Navigate to next bio sample" or "Navigate to nearest codex entry." EliteIntel navigates to the nearest tagged biological. It prioritizes the active genus if one is being tracked.
- **Navigate to Landing Zone**: "Navigate to landing zone." Navigates to your last known ground landing coordinates.
- **Navigate to Fleet Carrier**: "Navigate to carrier" or "Return to base." Plots a route to the carrier's last known system.
- **Take Me Home**: "Take me home." Plots a route to your configured home system.
- **Set Home System**: "Set as home system." Marks your current system as the home system.
- **Plot Fleet Carrier Route**: Open the galaxy map and select a star. Copy its name using the last button on the right in the Galaxy Map UI. Say "Calculate Fleet Carrier Route." EliteIntel builds the route if the system is in Spansh.
- **Enter Next Fleet Carrier Destination**: Open the Fleet Carrier galaxy map and click the top text field. Say "Enter next Fleet Carrier destination." EliteIntel pastes the next system name into the field.
- **Find Nearest Human/Guardian Tech Broker**: "Find nearest Human tech broker." EliteIntel plots a route. After arriving, say "Remind me where we need to go" for a station reminder.
- **Find Nearest Material Trader**: "Find nearest raw material trader" / "encoded" / "manufactured." EliteIntel finds and plots a route.
- **Find Brain Trees**: "Find brain trees within 500 light years." EliteIntel scouts Guardian biology locations.
- **Find Mining Location**: "Find where we can mine some Osmium within 200 light years." EliteIntel finds a system with suitable rings and plots the route.
- **Find Carrier Fuel (Tritium)**: "Find where we can mine some carrier fuel within 300 light years." EliteIntel searches for ring systems containing Tritium.
- **Find Commodity**: "Find where we can buy Bromellite within 150 light years." EliteIntel queries EDSM and plots a route to the best market.
- **Find Nearest Vista Genomics**: "Find nearest Vista Genomics." Plots a route to the nearest Vista Genomics for bio sample redemption.
- **Find Nearest Fleet Carrier**: "Find nearest Fleet Carrier." Useful when a Fleet Carrier is needed nearby.

## Combat & Mission Commands

- **Find Hunting Grounds**: "Find hunting grounds." EliteIntel scouts nearby Hazardous Resource Extraction Sites or combat zones matching mission parameters.
- **Plot Reconnaissance Route**: "Navigate / plot recon route to hunting ground." EliteIntel plots a scouting path to the target system.
- **Navigate to Mission Provider**: "Navigate to system with matching mission provider" or "Navigate to a confirmed pirate massacre mission provider." EliteIntel plots a route to the mission provider.
- **Navigate to Active Mission**: "Navigate to active mission." Navigates to the current mission objective.
- **Confirm / Ignore Hunting Ground**: "Confirm hunting ground" or "Ignore hunting ground." Tells EliteIntel whether to use or skip that location.

## Ship Control Shortcuts

- **Power Distribution**: "All power to shields," "engines," "weapons," or "Equalize power." One command rebalances all power pips. This is especially useful in VR, where cloud processing delays increase response time.
- **Exit to HUD**: "Exit to HUD." Exits nested menus in a single command.
- **FSS Scan / Honk**: "Open FSS and scan" or "Honk." Triggers the discovery scan.
- **Set Optimal Speed**: "Set optimal speed." Sets throttle to 75% for supercruise. Issue this command approximately 20 seconds from the target to avoid orbital looping.
- **Target Next System in Route**: "Target next system in route." EliteIntel selects the next waypoint on the plotted route.
- **Wing Nav Lock**: "Wing nav lock." Locks onto a wingman's navigation.
- **Target Subsystem**: "Target power plant." Locks subsystem targeting.
- **Dismiss Ship / Get Extracted**: "Dismiss ship" / "Go to orbit" / "Return to surface, requesting extraction."

## Utility & Session Commands

- **Set a Reminder**: "Set reminder, pick up Painite from Hutton Orbital." EliteIntel stores the reminder for the session. Say "Remind me" to retrieve it.
- **Clear Reminders**: "Clear reminders." Clears the reminder queue.
- **Clear Codex Entries**: "Clear codex entries." Clears all scanned bio entries. Use with caution.
- **Delete Codex Entry**: "Delete codex entry." Removes the most recent entry.
- **Clear Cache**: "Clear cache." Clears the entire session cache. Use only when the session is in an unrecoverable state.
- **Monetize Route**: "Monetize route." EliteIntel analyzes the plotted route for trade or exploration profit opportunities.
- **Verify LLM Connection**: "Verify LLM connection." Confirms that the AI backend is reachable and responding.
- **Interrupt / Silence**: "Interrupt," "Silence," or "Cancel." Stops active text-to-speech output.
- **Biome Analysis**: "Run biome analysis on [star system / planet name]." EliteIntel reports probable species at that location before landing.
- **Fleet Carrier Financials**: "Open fleet carrier data and tell me how long we can operate." EliteIntel calculates the number of weeks of operation from the current balance.
- **Fleet Carrier Range**: "What is the range of our fleet carrier?" EliteIntel calculates the worst-case range from tritium in the depot and carrier storage.
- **How Long Can the Carrier Operate?**: "Use reserve balance, how long can we run the carrier with 31 million credits per week?" Specify which balance to use. EliteIntel performs the calculation.

## Usage Notes

- **Natural Language**: Formal command syntax is not required. Natural speech is supported.
- **VR Users**: Power distribution and Exit to HUD commands reduce the impact of cloud processing delays in VR.
- **Dark Side Navigation**: Navigating to coordinates on a planet's dark side requires instrument flight.

----
Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
