-- The commander's own name for a bookmark, set on the Bookmarks page of the Commander tab.
--
-- NULL until they rename it, and the card then names the place as it always has. A name is stored only
-- when the commander typed one, so a bookmark left alone follows the app language for its coordinates.
--
-- NOTE: no semicolon may appear inside these comments. Migrations are split on a semicolon at end of
-- line before comments are stripped, so one here would cut the file mid-comment and hand SQLite a
-- statement with no SQL in it.
ALTER TABLE location_bookmark
    ADD COLUMN displayName TEXT;
