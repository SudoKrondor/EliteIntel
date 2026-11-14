# Pirate Massacre Mission Tracking

EliteIntel tracks Pirate Massacre Missions, including kill counts, target factions, and payout calculations. It 
monitors kills and payouts throughout the session.

## Getting Started

To efficiently complete Pirate Massacre Missions:

1. **Find a Set**: Ask ``Find pirate massacre missions`` (see *Finding Massacre Sets* below), or look one up on
   [INARA](https://inara.cz): a system where several factions offer missions against the same pirate faction, next to
   a system with a Resource Extraction Site (RES). Accept as many missions against that faction as you can. EliteIntel
   tracks the target faction, kill counts, and payout details.
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

- **Mission Stacking**: Accepting multiple missions against the same faction increases credit efficiency. Ask
  EliteIntel for the deepest stack nearby (see *Finding Massacre Sets* below).
- **Natural Language Queries**: Natural speech is accepted. ``How many pirates left to kill?`` and ``Pirate mission 
  score?`` are both valid. Specify that the query is pirate-related to avoid confusion with other active missions.
- **Personality Toggle**: Switch between personality modes as needed to match your current playstyle.


---

# Finding Massacre Sets

A **massacre set** is a mission provider and a hunting ground: factions in one system issue pirate massacre
missions against a pirate faction in a nearby system, and that system has a Resource Extraction Site (RES) to do the
killing in. The more factions issue in the provider system, the deeper the stack: their missions run side by side,
so the same kills pay out several times.

## How EliteIntel Learns Sets

EliteIntel learns sets from your own flying. No website or account is involved.

- **Missions you accept.** When you accept a pirate massacre mission, EliteIntel records the provider system, the
  station, the faction and the target.
- **Resource sites you see.** When you arrive in a system, the game lists its signal sources, Resource Extraction
  Sites included. EliteIntel also hears about the resource sites other commanders' games report to EDDN, the public
  Elite Dangerous data network, so it knows about systems you have never visited.

A set needs both halves. A mission against a system with no known resource site is kept on file, but it is not
offered until a resource site has been seen there.

**Start with your history.** Your old journals hold years of this. Use **Import Old Journals** on the
[Commander tab](UI-Commander-Tab.html), or say ``Scan journals for hunting grounds``.

## Asking for a Set

| Say | What happens |
|---|---|
| ``Find pirate massacre missions within 50 light years`` | Vega names the best set in range: the provider system and how many factions issue there, the pirate faction and the target system, its resource sites, and which stations to visit. Deeper stacks come first, then nearer ones. Without a range, it looks 100 light years out. |
| ``Navigate to pirate mission provider`` | Plots a route to a provider: first one for the missions you already carry, then one for the hunting ground you are in, then the best set nearby. A reminder names the target system. |
| ``Navigate to pirate mission target`` | Plots a route to the hunting ground of the missions you carry. |
| ``Find a bounty hunting ground`` | The nearest Resource Extraction Site worth hunting for the bounties alone, with no missions involved. |
| ``Forget this hunting ground`` | Said in a hunting ground you do not like (too easy, too hard, too far from the star, the wrong power). Vega asks first, then never offers it again, nor any set that sends you there. |

## Sharing Sets with Other Commanders

Sharing is off until you turn it on, on the **Massacre Sets** page of the [Commander tab](UI-Commander-Tab.html).

It works both ways: **while you share, you also get the sets other commanders have shared.** The more commanders
share, the more sets there are for everyone.

**What is shared:** for each set, the star system, faction and station names, how many resource sites of each grade
the target has, and the day the set was last seen. Every commander on your computer counts, including ones you
imported.

**What is never shared:** your commander name, your missions, rewards, or anything else that says who you are. See
the [Privacy Policy](Privacy.html).

What happens to the sets:

1. Your sets are uploaded when you turn sharing on, and after that at most once a day, when you have learned new ones.
2. Every night the sets from all commanders are checked against galaxy data. The systems must exist, the target
   must be within 20 light years of the provider, and the pirate faction must really be in the target system and be
   an Anarchy. Sets that fail are dropped. The rest are published, without saying who sent them.
3. Once a day EliteIntel downloads the published sets. When you ask for a set, they are offered alongside your own.

**A shared set is offered only when your own data shows a resource site in its target**, from your visits or from
EDDN. That is the one thing galaxy data cannot check. If a shared set does not show up, fly to its target system, or
wait for EDDN to report it.

**Vega tells you when a set comes from other commanders.** Treat it as a lead to check, not something you have flown
yourself. If it is not to your liking, ``Forget this hunting ground`` works the same as for your own sets.

**Your own record comes first.** When you know a set yourself, your version is used, and between sets of the same
depth, yours are offered before reported ones.

If you do not share and EliteIntel knows no set nearby, Vega tells you when other commanders have reported some in
the area.

Turning sharing off removes your sets from the shared data, and EliteIntel stops using everyone else's.

----
Community 👉[**Matrix**](https://app.element.io/#/room/#krondor:matrix.org)👈
