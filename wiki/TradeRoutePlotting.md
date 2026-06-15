# Trade Route Plotting

Elite Intel calculates trade routes and guides cargo operations leg by leg. Say "Plot me a profitable trade route" to begin.

### How Trade Route Calculation Works

1. **Cargo capacity is required.**
   If the current ship has no cargo capacity, the app will prompt you to switch to a ship with at least one cargo rack.

2. **Trading Profile (per ship)**
   Each ship has its own trading profile. Set it once. Elite Intel retains the profile for future sessions.

3. **Setting or Changing the Profile** (voice commands only)
   Say "Alter trading profile" followed by the parameter to change.

   **Initial setup example** (minimum required):
    - "Alter trading profile, starting capital twenty million"
    - "Alter trading profile, maximum distance from star six thousand light seconds"
    - "Alter trading profile, maximum stops eight"

   > Note: say "maximum stops eight," not "to eight." Speech-to-text may transcribe "to" as "two," resulting in an incorrect stop count.

   Optional parameters:
    - "Alter trading profile, allow permit systems"
    - "Alter trading profile, allow planetary ports"
    - "Alter trading profile, allow fleet carriers" (note: carrier position may change before arrival)
    - "Alter trading profile, allow prohibited commodities"

4. **Calculate the Route**
   Say "Calculate trade route" or "Plot profitable trade route."
   Calculation typically takes 30 to 60 seconds.

5. **Navigate Leg by Leg**
   Say "Plot route to next trade stop" when ready. Synonyms for the station type are accepted. The command requires **Plot** + **trade** + a station synonym.

6. **On Arrival**
   Elite Intel announces the docking station and the commodity to buy or sell.
   If unsure, ask "What am I buying here?" or "Where am I selling this cargo?" Specify buy or sell to avoid ambiguity.

7. **After Loading Cargo**
   Say "Plot route to next trade stop" again. Elite Intel routes to the sell location with the highest profit margin.

8. **Route Persistence**
   The route persists until completed, overridden, or canceled. To start a new route, empty the cargo hold first.
   (Planned feature: automatic cargo liquidation to the highest bidder.)

----
Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
