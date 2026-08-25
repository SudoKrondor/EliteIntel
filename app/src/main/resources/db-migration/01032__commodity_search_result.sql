-- The last market a commodity search sent the commander to, so the overlay can show it.
--
-- Single row, like destination_reminder: only the most recent search is of any interest, and the card
-- has to survive a restart mid-trip. The overlay's rule is derive-never-remember - a source recomputes
-- its card from persisted state on every poll - so a result that lived only in the search's local
-- variable could not be shown at all.
--
-- A destination reminder is already written alongside this, but it stores prose for the voice plus a
-- system and a port; the stock and the unit price are in that sentence and nowhere a card can read them.

create table if not exists commodity_search_result
(
    id
    integer
    primary
    key
    check
(
    id =
    1
),
    -- English name as the commodities table spells it; localized for display at read time.
    commodity text not null,
    starSystem text,
    stationName text,
    stationType text,
    price integer not null default 0,
    -- Units on sale when Spansh last heard. Zero when it did not say.
    supply integer not null default 0,
    -- A carrier jumps, so the card says so rather than sending the commander to a dot on a map.
    fleetCarrier integer not null default 0,
    foundAt text
    );
