-- The colour theme the app window is drawn with.
--
-- The themes themselves are JSON files in the themes folder beside the databases, so a commander can export them,
-- share them and import other commanders' themes. This column only says which one is active, and it belongs to
-- the user rather than to a commander: it is the same eyes whichever commander is loaded.
--
-- Empty means the Default theme. builtin: followed by a name is a theme shipped with the app, and anything else
-- is the file name of a theme in the themes folder. A theme that cannot be loaded falls back to Default at
-- startup and this column is left as it is, so a file that is only missing for now comes back on the next start.
--
-- NOTE: no semicolon may appear inside these comments. Migrations are split on a semicolon at end of line before
-- comments are stripped, so one here would cut the file mid-comment and hand SQLite a statement with no SQL in it.
ALTER TABLE game_session
    ADD COLUMN uiTheme TEXT NOT NULL DEFAULT '';
