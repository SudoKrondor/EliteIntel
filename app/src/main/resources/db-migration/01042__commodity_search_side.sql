-- Which way round the commodity search card reads: BUY or SELL.
--
-- The card was written when the only search was "where can I buy this", so every figure on it was
-- read one way: the price is what the commander pays, and supply is what the market has in stock.
-- The sell search asks the mirror question and fills the same single row with the mirror figures --
-- the price is what the commander is PAID, and supply is the tonnage the market WANTS. Without a
-- direction on the row the card cannot label either one, and would tell a commander flying out to
-- unload 300 tonnes of tritium that the station has 300 tonnes in stock.
--
-- BUY is the default because that is what every row written so far is, and the column has to mean
-- something for the card the commander may be looking at right now, mid-trip, across the upgrade.
--
-- The commodity_search_line rows keep their unitsToBuy column name: renaming a column in SQLite
-- means rebuilding the table, and the market row's side already says which way its lines read.
--
-- NOTE: no semicolon may appear inside these comments. Migrations are split on a semicolon at end of
-- line before comments are stripped, so one here would cut the file mid-comment and hand SQLite a
-- statement with no SQL in it.
alter table commodity_search_result
    add column side text not null default 'BUY';
