# Colonisation

EliteIntel turns a colonisation construction site into a live shopping list. It tracks what the build still
needs, finds where to buy it, and plots the route there. You stop alt-tabbing between market websites and
spreadsheets.

It does **not** decide what to build or where. That part is yours, architect. It helps you gather the pile of
commodities the build needs.

[[youtube:PnIlVZRdhKE]]

## Start Tracking a Build

**Land on the construction site.** That is the trigger. When you touch down at the depot, the game sends the
site's full manifest and that site becomes the one EliteIntel tracks. Open the construction screen and the
[HUD overlay](UI-HUD-Overlay) shows the **CONSTRUCTION SITE** card: progress, what is outstanding, and what
to load on the next run.

No tracking of the whole galaxy happens behind your back. The app only knows the state of the build **as of
your last landing**. Other commanders can haul to the same depot while you are away, so the card and Vega
mention how old the data is once it is more than an hour old.

## Find the Next Commodity

Ask ``Find construction cargo`` (or *find construction materials*, *find construction commodity*, etc...).

1. **Your carrier first.** If your fleet or squadron carrier holds goods the build needs, Vega sends you there
   before any market. A carrier in your current system always wins. One further away has to be worth the
   jumps.
2. **Then the nearest market.** EliteIntel searches markets within two jumps of your ship (using your
   ship's jump range), then four jumps if nothing turns up. Starports come first, planetary settlements
   second. Some goods, such as CMM Composite, are only sold at planetary settlements. Yes, you have to land.
3. **Largest shortfall first.** The search is anchored on the commodity you are shortest on (steel, titanium,
   aluminium, whatever the biggest hole is), and it prefers the market that fills most of your hold with
   other goods the build needs.

Once a market is found, EliteIntel opens the galaxy map on the star. It does **not** commit the route. That
is on purpose: you decide whether to lock it in, or drill into the system map to find the actual settlement.

**New install?** Spansh searches need a starting point. The app first looks for stations you have docked at
in its local database. If there are none, it asks Spansh for the nearest station, which needs your galactic
coordinates, and the app only learns those after an FSD jump. So fly around with the app running and dock at
a few stations before you start a construction project. The more you use it, the more it knows.

## At the Market

When you dock, the overlay reorders the list so the goods **this station sells** come first.

- A commodity partially loaded shows in green with the tonnes aboard (for example ``16 T +44``).
- Once you have enough of a commodity it drops off the list, and the next outstanding good takes its line.
- When you have bought everything this station can supply, the station name disappears from the card. You
  still have goods to buy, just not here. Ask ``Find construction cargo`` again and Vega sends you to the
  next suitable market.

Deliver or stash on your carrier, then repeat until the build is done.

## Back to the Site

Say ``Take me back to the construction site``. If the build is in another system, the route is plotted
there. If you are already in its system, no route is needed.

Ask ``How is the build going?`` or ``How much do we still need to buy?`` for a spoken progress report.

## Fleet Carrier Caveat

Frontier does not expose fleet carrier cargo to third-party tools. The only way EliteIntel can see your
stockpile is through the carrier's **commodity market**. To make the carrier visible:

1. **Lock the carrier down** so nobody else can dock and buy your construction pile.
2. **List the commodities for sale** in the carrier market.
3. **Open the carrier market** from the panel. That is the moment the app reads it.

After that EliteIntel keeps count as you transfer cargo between the carrier and your ship, and as you buy
or sell at your own carrier. It is a workaround, but there is little else the game allows.

## Several Construction Sites

The last site you landed on is the current one. EliteIntel tracks one build at a time on the overlay. Land
on another depot and that one takes over. Land on the first one again and it is current again.

Want a break from construction? Say ``Dismiss the construction site``. Vega asks you to confirm, then the
card goes quiet. Nothing is deleted. Landing on the site brings it back.

## What the AI Does (and Doesn't)

The AI turns what you say into actions and decides what Vega says back. The tracking, searching and
route plotting are plain code reading your game journal. It does not run the colonisation loop for you, and
it does not see anything the game does not write down. It is still you flying the ship. The app saves you
the web browsing.

See [All Commands](AllCommands) for the full list of colonisation phrases.
