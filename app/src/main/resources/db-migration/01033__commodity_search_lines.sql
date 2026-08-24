-- Everything a commodity search says to buy at the market it found, not just the good that was asked for.
--
-- A search anchored on one commodity already learns, for free, what else the candidate markets stock: the
-- Spansh stations index returns each station's entire market alongside the row. When the caller is working
-- from a standing list - a colonisation manifest, a stack of source-and-return missions - the answer is
-- "load steel, polymers and copper here", and one row of commodity_search_result cannot hold that.
--
-- The anchor is written here too, as the first line, so the card has one place to read from rather than a
-- header plus a list. commodity_search_result keeps its own copy of the anchor as the headline good.

create table if not exists commodity_search_line
(
    -- Position in the list; 0 is the anchor the search was built around.
    position
    integer
    primary
    key,
    commodity
    text
    not
    null,
    -- Bare journal symbol, or null for a good the commodities table carries none for.
    symbol
    text,
    price
    integer
    not
    null
    default
    0,
    -- Units on sale, already corrected by our own last look at that market.
    supply
    integer
    not
    null
    default
    0,
    -- What the hold has room for, capped by the supply and by what is still wanted.
    unitsToBuy
    integer
    not
    null
    default
    0
);
