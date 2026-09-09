-- Whether a jet cone boost automatically plots the route to the next neutron waypoint.
--
-- Flying a neutron highway is the same three actions at every waypoint: arrive, scoop the cone, ask for
-- a route to the next one. Commanders asked for the third to happen on its own. It is off by default
-- because it opens the galaxy map without being asked, and a commander who does not expect that is
-- flying blind for a few seconds -- so it is opted into, and the boost is announced before the map opens.
--
-- autoFighterOutFighterDocking, added in 00051, is deliberately left in place and unused. The automation
-- it gated has been broken since the Nomad update, and its checkbox and accessors are gone, so nothing
-- reads or writes the column any more. Dropping it would rewrite a table every installation already has
-- for no gain.
--
-- NOTE: no semicolon may appear inside these comments. Migrations are split on a semicolon at end of
-- line before comments are stripped, so one here would cut the file mid-comment and hand SQLite a
-- statement with no SQL in it.
alter table global_settings
    add column autoPlotNextNeutronJump boolean default false;
