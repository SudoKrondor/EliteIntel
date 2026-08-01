-- Index the location lookup that every "what is in this system" query uses.
--
-- systemAddress was added to the location table without one, so each of those queries scanned the
-- whole table. That table only grows - a well-travelled commander has tens of thousands of rows -
-- and it is read on the hot journal path (arrival, scan and signal subscribers all resolve a body
-- by system) and now once a second by the HUD overlay poll, on the EDT.
--
-- Index only; no data or schema change.
CREATE INDEX IF NOT EXISTS idx_location_system_address ON location (systemAddress);
