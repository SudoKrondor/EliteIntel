-- A sold ship is flagged, not deleted.
--
-- Deleting a ship took its settings and trade profile with it (ON DELETE CASCADE), so a ship wrongly taken off
-- the fleet came back with every setting the commander had made for it gone. Now a sale stamps soldAt with the
-- sale's journal timestamp and the fleet leaves the row out. A Loadout or StoredShips that names the ship again
-- clears the stamp, which can only happen to a ship wrongly marked, because the game never reuses a ShipID.
--
-- NOTE: no semicolon may appear inside these comments. Migrations are split on a semicolon at end of line before
-- comments are stripped.
ALTER TABLE ship ADD COLUMN soldAt TEXT DEFAULT NULL;
