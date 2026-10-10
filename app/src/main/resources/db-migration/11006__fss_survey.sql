-- How many bodies the discovery scanner counted in a system, and whether the FSS has found them all.
--
-- The honk (FSSDiscoveryScan) is the only place the journal says how many bodies a system holds, and
-- FSSAllBodiesFound the only place it says the count has been reached. Neither leaves a trace on any body
-- row, so without this table the explorer card lost its scan progress on every restart.
--
-- Shared, not per-commander: the number of bodies in a system is a fact about the galaxy, and the body
-- rows the progress is counted against are shared too.
--
-- NOTE: no semicolon may appear inside these comments. Migrations are split on a semicolon at end of
-- line before comments are stripped, so one here would cut the file mid-comment and hand SQLite a
-- statement with no SQL in it.
CREATE TABLE IF NOT EXISTS fss_survey
(
    systemAddress
        INTEGER
        PRIMARY
            KEY,
    bodyCount
        INTEGER
        NOT
            NULL,
    allBodiesFound
        INTEGER
        NOT
            NULL
        DEFAULT
            0
);
