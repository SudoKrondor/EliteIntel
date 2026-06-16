# Pirate Massacre Mission Tracking

EliteIntel tracks Pirate Massacre Missions, including kill counts, target factions, and payout calculations. It 
monitors kills and payouts throughout the session.

## Getting Started

To efficiently complete Pirate Massacre Missions:

1. **Find a Hotspot**: Use [INARA](https://inara.cz) to find a system with a Hazardous Resource Extraction Site (Haz 
   RES) where multiple factions offer missions against the same pirate faction. Accept multiple missions against the 
   same faction. EliteIntel tracks the target faction, kill counts, and payout details.
2. **Query Mission Status**: After accepting missions, ask EliteIntel questions such as ``How many kills left on 
   pirate missions?`` or ``What is the pirate massacre payout?`` EliteIntel returns the total kills required, potential 
   credits, and a per-faction breakdown. Specify that the query is pirate-related to avoid confusion with other 
   mission types.

## Engaging Pirates in the Haz RES

1. **Travel to the Site**: Fly to the target system and enter the Haz RES. Scan nearby ships.
2. **Target Identification**: EliteIntel announces the following when ships are scanned:
    - **Non-Mission Pirates**: ``Legal Target, [ship type], [pilot rank], [bounty reward].`` These ships are valid 
      targets but do not count toward mission objectives.
    - **Mission Pirates**: ``Mission Target! [ship type], [pilot rank], [bounty reward].`` These are primary targets.
    - EliteIntel does not announce clean or friendly ships, keeping audio output relevant.
3. **Kill Confirmation**: When a pirate is eliminated, EliteIntel announces ``Kill Confirmed`` along with the bounty 
   earned. Mission target kills are announced as ``Mission Kill Confirmed`` with the associated payout.
4. **Progress Queries**: Ask ``How many kills left?`` at any time. EliteIntel returns total kills remaining and a 
   per-faction breakdown.


## Best Practices

_**redeem all bounties before picking up your private massacre missions**_ a confirmed kill is a bounty voucher 
against the faction

- **Mission Stacking**: Accepting multiple missions against the same faction increases credit efficiency. Use INARA 
  to identify systems with multiple qualifying mission providers.
- **Natural Language Queries**: Natural speech is accepted. ``How many pirates left to kill?`` and ``Pirate mission 
  score?`` are both valid. Specify that the query is pirate-related to avoid confusion with other active missions.
- **Personality Toggle**: Switch between personality modes as needed to match your current playstyle.


---

# Find pirate massacre missions 
## Experimental feature

At the moment manual search in INARA for pirate mission stacking remains the most effective way of bounty hunting. 
However, the app has an experimental feature based on IN**T**RA data (not IN**A**RA). This is a less efficient but 
more immersive way to play bounty hunting, though it depends on the data availability in IN**T**RA and their site 
being up and operational. 

To use this feature, board our bounty-hunter ship and ask ``Find us some hunting grounds (within X light years)`` the 
ship will attempt to a connection to IN**T**RA and fetch the pairs of target system / mission provider system. The 
success or failure depends on IN**T**RA API returning the data and the number of other bounty hunters running EDMC with
IN**T**RA plugin.

If and When that data is returned to you, the ship computer will tell you it found X number of hunting grounds. Ask 
``plot a route to hunting ground for reconnaissance``. Elite Intell will plot the route for the nearest hunting 
ground in the list. You have to fly there and confirm the presence of the Resource Site. If there is one, and the 
spawn rate is to your liking, confirm or reject this system as a potential hunting ground with a voice command.

Elite Intel may automatically confirm it as a hunting ground if it detects the RES sites in the journal on entry, or 
on nav beacon scan. There are cases when this does not happen because there are no records of RES sites dumped by 
the game in to the journal. Manual confirmation is required.

Once you are satisfied with the hunting ground, ask ``navigate to pirate massacre mission provider system`` or 
something close to that. The app will plot a route to the mission provider system. Fly there, land at ports, and pick 
up missions against pirates for the same faction and location of the hunting ground system. When you pick up your 
first massacre mission, this star system pair will be confirmed as hunting ground / mission provider. The app will 
tell you if there are other systems that it knows about that have missions against the same faction in the target 
system. This is, of course, if the IN**T**RA has that data. 

When you got your missions stacked ask ``plot route to active mission`` and pew-pew pirates as usual. When you are 
done with the missions same request ``plot route to active mission`` will take you to where the objective is, this 
time the port where you picked up the assignment. 

----
Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
