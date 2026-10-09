-- Places the commander bookmarked by voice, to fly back to later.
--
-- Per-commander: what a commander chose to remember is theirs, not a fact about the galaxy.
--
-- What is filled depends on the kind of place. A system names only starSystem. A station adds stationName.
-- A planetary port adds planetName and stationName. A planet adds planetName. A spot on the surface adds
-- planetName, latitude and longitude. The rowid order is the order they were saved, which is the order the
-- list is read back in (newest first).
--
-- NOTE: no semicolon may appear inside these comments. Migrations are split on a semicolon at end of
-- line before comments are stripped, so one here would cut the file mid-comment and hand SQLite a
-- statement with no SQL in it.
CREATE TABLE IF NOT EXISTS location_bookmark
(
    id
        INTEGER
        PRIMARY
            KEY
        AUTOINCREMENT,
    kind
        TEXT
        NOT
            NULL,
    starSystem
        TEXT
        NOT
            NULL,
    stationName
        TEXT,
    planetName
        TEXT,
    latitude
        REAL,
    longitude
        REAL,
    createdAt
        INTEGER
        NOT
            NULL
);
