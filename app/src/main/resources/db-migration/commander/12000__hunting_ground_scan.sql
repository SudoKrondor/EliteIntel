-- Where this commander's journal backfill stopped, so a re-run reads only what is new. One row, id 1.
--
-- Per-commander. It used to be one shared row, so a second commander's scan started where the first one's had
-- stopped and never read their own older journals. The scan now reads only the open commander's journals, and
-- the bookmark names the last of those. The shared row is dropped by shared 12009 rather than carried across:
-- there is no telling whose it was, and a missing bookmark only costs one full re-read, which learns nothing twice.
--
-- NOTE: no semicolon may appear inside these comments. Migrations are split on a semicolon at end of
-- line before comments are stripped, so one here would cut the file mid-comment and hand SQLite a
-- statement with no SQL in it.
CREATE TABLE IF NOT EXISTS hunting_ground_scan
(
    id          INTEGER PRIMARY KEY,
    lastJournal TEXT,
    lastScanAt  TEXT
);

INSERT OR IGNORE INTO hunting_ground_scan (id)
VALUES (1);
