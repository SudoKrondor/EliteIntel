-- Star systems the commander has struck from every search that ends in a plotted route.
--
-- Spansh keeps a station on file after it is gone, and in a commander-colonised system a starport can be demolished
-- by its owner. Measured 2026-09-27: a Battle Weapons search named Oz Prospect in Hyades Sector WI-S b4-4 and the
-- commander arrived to find no starport there. The seven-day market window drops such a station within a week, and
-- this list is for the week before that, and for anything else the commander has seen with their own eyes.
--
-- Shared, not per-commander: a station that is not there is a fact about the galaxy, not about who flew to it.
--
-- NOTE: no semicolon may appear inside these comments. Migrations are split on a semicolon at end of
-- line before comments are stripped, so one here would cut the file mid-comment and hand SQLite a
-- statement with no SQL in it.
CREATE TABLE IF NOT EXISTS search_excluded_system
(
    starSystem
    TEXT
    PRIMARY
    KEY
    COLLATE
    NOCASE,
    excludedAt
    TEXT
    NOT
    NULL
);
