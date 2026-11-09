-- The local copy of Spansh's brain tree sites, replaced by asking Spansh on every request.
--
-- WHY it is dropped: the table was meant to hold every known brain tree site, and it held 250 bodies picked
-- at random from more than 10000 (Spansh caps its count there, and 6877 sit within 1000 ly of Sol alone,
-- measured 2026-11-08). The request carried no sort, so the nearest site with the material asked for was
-- usually not in it. Its one-row-per-system key threw away a second brain tree body in the same system,
-- and its upsert wrote z into the x column, so a row refreshed twice sorted by the wrong distance.
-- "Nearest" depends on where the commander is when they ask, so no fixed sample can answer it. Spansh
-- filters by material and sorts by distance itself, which answers the question exactly.
--
-- Shared file. Nothing else reads the table, and what it held is all still on Spansh.
--
-- NOTE: no semicolon may appear inside these comments. Migrations are split on a semicolon at end of
-- line before comments are stripped, so one here would cut the file mid-comment and hand SQLite a
-- statement with no SQL in it.
DROP INDEX IF EXISTS idx_brain_trees_star_system;
DROP TABLE IF EXISTS brain_trees;
