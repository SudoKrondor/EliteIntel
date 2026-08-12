-- The overlay derived a route's length from the legs still in the table (flown legs are deleted as they go),
-- so anything that removed a row without it being flown changed the total the commander was reading. Record
-- the length once, when the route is stored, and read it back instead of inferring it.
--
-- Null on routes plotted before this migration; the overlay falls back to the old derivation for those.

alter table trade_route
    add column totalLegs integer default null;
