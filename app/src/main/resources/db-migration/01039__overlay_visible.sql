-- Whether the desktop HUD overlay was on screen when the app last ran.
--
-- The overlay has always started hidden, so a commander who flies with it up had to reach for the
-- Show Overlay button every launch. It is not a preference with a checkbox of its own -- the button
-- IS the control -- so this column just remembers which way that button was left.
--
-- 0 is the behaviour every build so far has had, so an existing installation keeps starting hidden
-- until the commander turns the overlay on once. Restoring reads this column and never writes it: an
-- overlay that fails to start -- a missing binary, a broken install -- must not erase the preference
-- that would bring it back on the next launch.
--
-- NOTE: no semicolon may appear inside these comments. Migrations are split on a semicolon at end of
-- line before comments are stripped, so one here would cut the file mid-comment and hand SQLite a
-- statement with no SQL in it.
ALTER TABLE game_session
    ADD COLUMN overlayVisible INTEGER NOT NULL DEFAULT 0;
