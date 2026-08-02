-- Remember where the commander wants the HUD drawn: the monitor, the headset, or both.
--
-- Stored as text rather than an integer so the value in the database says what it means when
-- somebody reads the row, and so an older build that does not know a newer mode reads something
-- it can fall back from rather than a number that silently means the wrong thing.
--
-- DESKTOP is the default because it is what every version before VR support did, so an existing
-- installation upgrading into this migration keeps the overlay exactly where it already was.
--
-- NOTE: no semicolon may appear inside these comments. Migrations are split on a semicolon at end of
-- line before comments are stripped, so one here would cut the file mid-comment and hand SQLite a
-- statement with no SQL in it.
ALTER TABLE game_session
    ADD COLUMN overlayDisplayMode TEXT NOT NULL DEFAULT 'DESKTOP';
