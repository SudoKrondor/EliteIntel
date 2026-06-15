# Navigation Assistance

EliteIntel (EI) provides navigation assistance in *Elite Dangerous*. It supports galactic coordinate routing for Fleet Carriers, surface coordinate navigation for planets and moons, and exobiology sample tracking.

## Fleet Carrier Route Planning

EliteIntel uses [Spansh](https://spansh.co.uk) to plan Fleet Carrier jump routes. To calculate a route:

1. **Open the Galaxy Map**: Any galaxy map view works. The Fleet Carrier map is not required for this step.
2. **Copy the Destination Name**: Select the target star system. Click the copy button (the last button on the right in the Galaxy Map UI). The system name is now on your clipboard.
3. **Request Route Calculation**: Say "Calculate Fleet Carrier Route." EliteIntel reads the system name from the clipboard, checks the carrier's current position, and queries Spansh to generate a route. The route is saved to the current session.

**Note**: Spansh's database may not include all systems. If the carrier's current system is unknown to Spansh, EliteIntel selects the nearest known star within jump range as the starting point. If the destination system is not in Spansh, a nearby alternative may be required.

### Executing the Route

After the route is calculated:

1. **Open the Fleet Carrier Galaxy Map**: Access the map from the carrier management menu and click the top text field.
2. **Retrieve the Next Destination**: Say "Enter next Fleet Carrier destination." EliteIntel pastes the next system name into the field.
3. **Schedule the Jump**: Confirm the jump in-game. Repeat for each leg of the route.

## Surface Coordinate Navigation

EliteIntel provides voice-guided navigation to specific coordinates on a planet or moon. No navigation marker is required.

1. **Begin Navigation**: Say "Navigate to latitude 41.4325 longitude -75.2309" (replace with your target coordinates). EliteIntel guides you from orbit to the surface.
2. **Orbital Approach**: Maintain a moderate speed in supercruise. There is a slight text-to-speech delay, so avoid excessive speed to ensure you can follow guidance accurately. Too slow, and the ship may drop out of supercruise prematurely. Navigating the dark side of a planet requires instrument flight.
3. **Glide Phase**: When approximately 400 km from the surface, EliteIntel provides glide angle updates such as "Steep glide angle, -40 degrees." The decision to glide or reposition for a better angle is the pilot's choice. EliteIntel provides guidance only.
4. **Surface Navigation**: After landing, EliteIntel guides you to within 1,000 meters of the target and prompts you to find a landing spot. From that point, EliteIntel continues directing you in the SRV or on foot until you are within 50 meters of the target.
5. **Cancel Navigation**: Say "Cancel navigation" at any time to stop. Natural phrasing is accepted.

## Exobiology Navigation

When collecting bio samples in difficult terrain, EliteIntel tracks tagged samples and guides you between them.

1. **Tag Samples**: Fly low and slow over the terrain to locate bio colonies. Approach each colony and scan it with the Composition Analyzer to create a Codex Entry. Remain close to the sample when scanning. This logs the ship's coordinates in EliteIntel and may earn a credit reward. Tag all visible species before landing.
2. **Land**: Find a landing spot within 2 km of the samples if possible. If no suitable spot is available within that range, EliteIntel will guide you via SRV.
3. **Navigate to Samples**: Say "Navigate to nearest codex entry" or "Navigate to next bio sample." EliteIntel routes you to the nearest tagged location. After scanning a species (for example, Frutexa), EliteIntel records the type. Subsequent navigation requests prioritize that species if additional tags of the same type exist.
4. **Continue**: Repeat until all scans are complete. EliteIntel provides continuous voice guidance during extended SRV traversal.

**Note**: EliteIntel uses coordinates from Codex tags. Remaining close to the sample during scanning improves coordinate accuracy. More tags provide more navigation options.

## Usage Notes

- **Natural Language**: Formal command syntax is not required. Natural speech is accepted.
- **Dark Side Navigation**: Navigating to coordinates on a planet's dark side requires instrument flight.
- **Spansh Limitations**: If Spansh does not recognize your current system, try an adjacent star to initiate the route.

----
Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
