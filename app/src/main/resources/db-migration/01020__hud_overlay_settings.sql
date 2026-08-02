-- Persist the HUD overlay layout so it comes back where the commander left it.
--
-- Transparency, text size, width and screen position lived only in memory, so every launch reset
-- them and the overlay had to be re-tuned against the game each time. They sit in game_session with
-- the other app-wide settings (voices, audio devices, thresholds) rather than in a table of their own.
--
-- Defaults reproduce today's behaviour exactly. An overlayFontScale of 0 means "not chosen yet", so
-- the app keeps deriving text size from screen height, and overlayX/Y of -1 mean "leave the window
-- wherever it opens", which is what the overlay already does when no position is sent.
--
-- NOTE: no semicolon may appear inside these comments. Migrations are split on a semicolon at end of
-- line before comments are stripped, so one here would cut the file mid-comment and hand SQLite a
-- statement with no SQL in it.
ALTER TABLE game_session
    ADD COLUMN overlayAlpha REAL NOT NULL DEFAULT 0.25;

ALTER TABLE game_session
    ADD COLUMN overlayFontScale REAL NOT NULL DEFAULT 0;

ALTER TABLE game_session
    ADD COLUMN overlayWidth INTEGER NOT NULL DEFAULT 760;

ALTER TABLE game_session
    ADD COLUMN overlayX INTEGER NOT NULL DEFAULT -1;

ALTER TABLE game_session
    ADD COLUMN overlayY INTEGER NOT NULL DEFAULT -1;
