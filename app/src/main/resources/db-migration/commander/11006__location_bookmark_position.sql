-- Where each bookmark sits on the list, so the commander can arrange it on the Bookmarks page of the
-- Commander tab. The list is read back by position, lowest first, and the overlay card pages it in that
-- order.
--
-- A new bookmark goes to the bottom. Positions are not renumbered when one is deleted, so they may have
-- gaps - only their order matters, and a bookmark's number is its place in that order.
--
-- Existing bookmarks keep the order they were shown in until now (newest first) so no list changes on
-- upgrade.
--
-- NOTE: no semicolon may appear inside these comments. Migrations are split on a semicolon at end of
-- line before comments are stripped, so one here would cut the file mid-comment and hand SQLite a
-- statement with no SQL in it.
ALTER TABLE location_bookmark
    ADD COLUMN position INTEGER;

UPDATE location_bookmark
SET position = (SELECT COUNT(*) FROM location_bookmark newer WHERE newer.id >= location_bookmark.id);
