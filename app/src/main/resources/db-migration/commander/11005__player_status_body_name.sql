-- The body Status.json reports a surface position against.
--
-- The latitude and longitude were stored without it, so a reading on a planet said where on the planet but
-- not which planet. Bookmarking a spot on the ground needs both, and fell back to the star system.
--
-- NOTE: no semicolon may appear inside these comments. Migrations are split on a semicolon at end of
-- line before comments are stripped, so one here would cut the file mid-comment and hand SQLite a
-- statement with no SQL in it.
ALTER TABLE player_status
    ADD COLUMN bodyName TEXT;
