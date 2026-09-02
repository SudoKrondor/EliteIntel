-- What the commander has loaded into each planetary vehicle hangar bay.
--
-- WHY this has to be stored at all: the journal reports the hangar module but never its contents, so
-- there is no way to learn which bay holds which vehicle by observation. The commander tells us once per
-- hull and the deploy command reads it back.
--
-- Per ship rather than global because the hangar is part of the hull. A commander flies an explorer with
-- one Scarab and a combat build with a Scorpion and a Rhino, and the bay numbers mean different things in
-- each.
--
-- All four default to NULL, meaning "not configured". That is a distinct state from any vehicle name, and
-- the deploy command refuses on it rather than guessing a Scarab: guessing wrong on a Rhino would deploy
-- from a landed ship, and on a Scarab would leave the commander hovering waiting for a drop.
--
-- Stored as the enum name rather than an ordinal so a row stays readable and reordering the enum cannot
-- silently repoint every commander's bays at a different vehicle.
--
-- NOTE: no semicolon may appear inside these comments. Migrations are split on a semicolon at end of
-- line before comments are stripped, so one here would cut the file mid-comment and hand SQLite a
-- statement with no SQL in it.
ALTER TABLE ship_settings
    ADD COLUMN vehicleBay1 TEXT DEFAULT NULL;
ALTER TABLE ship_settings
    ADD COLUMN vehicleBay2 TEXT DEFAULT NULL;
ALTER TABLE ship_settings
    ADD COLUMN vehicleBay3 TEXT DEFAULT NULL;
ALTER TABLE ship_settings
    ADD COLUMN vehicleBay4 TEXT DEFAULT NULL;
