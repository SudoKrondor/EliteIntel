-- The commander's shopping list for a colonisation construction site.
--
-- A site is not necessarily the commander's own - anyone can haul to anyone's depot, and the site this
-- was built against belongs to another architect entirely. So the list is keyed by the depot's MarketID
-- and nothing about it is assumed to be ours.
--
-- ColonisationConstructionDepot is republished every 15-30 seconds for as long as the ship sits on the
-- pad, always in full, so these tables are overwritten from the event rather than accumulated: the
-- journal is the authority on what is still needed and we are only ever caching its last word. That
-- also means the numbers move without us: other commanders deliver to the same depot while we are away,
-- so requiredAmount and providedAmount are a record of the last visit, not a running total of our own
-- hauling.

create table if not exists construction_site
(
    marketId
    integer
    primary
    key,
    stationName
    text,
    starSystem
    text,
    systemAddress
    integer,
    -- Provided tonnes over required tonnes, as the journal reports it. Verified against the resource
    -- rows: 70 of 6721 tonnes reads as 0.010415, so it is a flat tonnage ratio and not payment-weighted.
    progress
    real
    not
    null
    default
    0,
    complete
    integer
    not
    null
    default
    0,
    failed
    integer
    not
    null
    default
    0,
    -- When we last stood on the pad. Decides which site the shopping commands mean when the commander
    -- has hauled to more than one.
    visitedAt
    text
);

create table if not exists construction_requirement
(
    marketId
    integer
    not
    null,
    -- Bare lower-case journal symbol, the join key onto the cargo hold and the commodities table.
    -- The event's Name_Localised is in the language the GAME runs in, which is not necessarily the
    -- language the app speaks, so it is no use as a key.
    symbol
    text
    not
    null,
    -- The game's own localised name, kept only as a last resort for a good the commodities table
    -- carries no symbol for.
    gameName
    text,
    requiredAmount
    integer
    not
    null
    default
    0,
    providedAmount
    integer
    not
    null
    default
    0,
    payment
    integer
    not
    null
    default
    0,
    primary
    key
(
    marketId,
    symbol
)
    );

create index if not exists idx_construction_requirement_market on construction_requirement (marketId);
