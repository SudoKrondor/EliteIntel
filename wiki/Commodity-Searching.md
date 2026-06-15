# Commodity Searching

This guide explains how to use EliteIntel (EI) to locate commodities in the market. EI queries [EDSM](https://www.edsm.net) to find markets within range. The data is not real-time, but EI is a reliable tool for identifying trade opportunities.

## Finding a Commodity

To locate a specific commodity, use the following process:

1. **Request a Search**: Speak naturally to EliteIntel. For example: "Where can I buy Tritium?" or "Find Bromellite." EliteIntel queries EDSM for the best market prices within 250 light years of your current position. When results are found, EliteIntel stores the market data in the session and confirms: "Found 5 markets for Tritium, Commander."
2. **Plot the Route**: Once markets are loaded, say "Navigate to the best market." EliteIntel opens the Galaxy Map, plots a route to the system with the lowest price, and reports the station name upon arrival.
3. **Station Reminder**: If you have jumped to the system and need to recall the station name, ask "Where is the market located?" EliteIntel retrieves the stored station information.

## Known Limitations

- **Speech-to-Text Errors**: Elite Dangerous includes commodities with names that are difficult to recognize via speech-to-text. For example, "Grandidierite" may be transcribed as "grande," causing the search to fail. A correction dictionary is available in the EliteIntel installation directory. Add entries in the format `"grande"="grandidierite"` to resolve recurring errors. Submit new corrections to the project so they can be included in the default dictionary.

- **EDSM Data Accuracy**: EDSM relies on data submitted by commanders running EDMC and is not real-time. A commodity may be sold out or repriced by the time you arrive. EliteIntel uses the best available data from EDSM, but discrepancies should be expected.

## Best Practices

- **Speak Naturally**: EliteIntel understands natural language. Use phrases like "Find me some Low Temperature Diamonds" without formal syntax.
- **Verify on Arrival**: EDSM data can be outdated. If the market is depleted, request a new search or check a nearby system.
- **Contribute to the Dictionary**: If a commodity name causes a consistent speech-to-text error, add it to the dictionary and share the entry with the project to benefit other users.

----
Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
