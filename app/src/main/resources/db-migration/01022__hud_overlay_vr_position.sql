-- Remember where in the headset the commander hung the HUD.
--
-- In VR there is no window to drag, so this is the only record of a placement the commander chose.
-- Losing it on restart means re-picking it while wearing a headset that cannot see the settings
-- window, which is worse than losing a dragged window position on the desktop.
--
-- Stored as text for the same reason the display mode is: the value says what it means when somebody
-- reads the row, and an older build meeting a placement it does not know reads something it can fall
-- back from rather than a number that silently means a different direction.
--
-- BOTTOM is the default because it is where the VR overlay sat before the setting existed, so an
-- existing installation upgrading into this migration sees no change.
--
-- NOTE: no semicolon may appear inside these comments. Migrations are split on a semicolon at end of
-- line before comments are stripped, so one here would cut the file mid-comment and hand SQLite a
-- statement with no SQL in it.
ALTER TABLE game_session
    ADD COLUMN overlayVrPosition TEXT NOT NULL DEFAULT 'BOTTOM';
