-- The galactic average price of each commodity, harvested from the markets the commander has stood in.
--
-- Every Market.json the game writes carries a MeanPrice for every good on the board, and the app has been
-- storing those files in station_markets all along without ever reading that field. It is the one number
-- that says whether a price is any good: 57844 for Tritium means nothing on its own, and "6550 above the
-- galactic average of 51294" means a great deal.
--
-- WHY a table of its own rather than a column on commodities: that table is reference data, shipped and
-- seeded by migration, keyed by symbol and holding names. This is observed data with a sighting date on
-- it, learned as the commander flies, and absent for any good they have never seen listed anywhere.
--
-- WHY one row per commodity and not per station: MeanPrice is the GALACTIC average, so it is the same
-- number at every market that lists the good. One sighting anywhere teaches it everywhere, which is why
-- this fills up fast and why the newest sighting simply replaces the old one.
--
-- The backfill reads the 200-odd Market.json files already on disk. Name arrives as "$tritium_name;" and
-- has to be reduced to the bare journal symbol the way JournalSymbol.normalize does it in Java -- strip
-- the leading dollar, the trailing semicolon, then the trailing _name, then lower-case. A symbol this
-- gets wrong simply never matches a lookup, and the live harvester writes the right row the next time the
-- commander opens any market listing that good, so a mistake here costs a missing figure and never a
-- wrong one. MeanPrice is 0 on markets that do not report it -- fleet carriers, mostly -- and zero is not
-- an average.
--
-- NOTE: no semicolon may appear inside these comments. Migrations are split on a semicolon at end of
-- line before comments are stripped, so one here would cut the file mid-comment and hand SQLite a
-- statement with no SQL in it.
CREATE TABLE IF NOT EXISTS commodity_mean_price
(
    symbol
    TEXT
    PRIMARY
    KEY,
    meanPrice
    INTEGER
    NOT
    NULL,
    seenAt
    TEXT
);

INSERT
OR REPLACE INTO commodity_mean_price (symbol, meanPrice, seenAt)
SELECT lower(
               CASE
                   WHEN trim(trim(json_extract(item.value, '$.Name'), '$'), ';') LIKE '%\_name' ESCAPE '\'
                       THEN substr(trim(trim(json_extract(item.value, '$.Name'), '$'), ';'), 1,
                                   length(trim(trim(json_extract(item.value, '$.Name'), '$'), ';')) - 5)
                   ELSE trim(trim(json_extract(item.value, '$.Name'), '$'), ';')
                   END),
       json_extract(item.value, '$.MeanPrice'),
       json_extract(m.json, '$.timestamp')
FROM station_markets m,
     json_each(json_extract(m.json, '$.Items')) AS item
WHERE json_valid(m.json)
  AND json_extract(item.value, '$.Name') IS NOT NULL
  AND json_extract(item.value, '$.MeanPrice') > 0
ORDER BY json_extract(m.json, '$.timestamp') ASC;
