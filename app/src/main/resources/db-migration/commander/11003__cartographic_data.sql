-- The exploration data on board, not yet sold to Universal Cartographics.
--
-- Per-commander, and per ship within that: the data lives in the ship that scanned it. A sale sells what the
-- docked ship carries and a death loses what the lost ship carried, so a combat ship's death leaves the
-- explorer parked on the carrier with its data intact.
--
-- cartographic_body holds what the scan said about each body and what the commander did to it since (mapped,
-- within the probe target). The value is never stored. It is worked out from these columns when it is
-- needed, so a correction to the formula reprices data already on board.
--
-- cartographic_system holds the honk: how many bodies it counted, and whether the FSS found them all. The
-- honk prices bodies nobody scanned, so it cannot be reconstructed from the body rows.
--
-- NOTE: no semicolon may appear inside these comments. Migrations are split on a semicolon at end of
-- line before comments are stripped, so one here would cut the file mid-comment and hand SQLite a
-- statement with no SQL in it.
CREATE TABLE IF NOT EXISTS cartographic_body
(
    shipId        INTEGER NOT NULL,
    bodyName      TEXT    NOT NULL,
    systemAddress INTEGER NOT NULL,
    starSystem    TEXT    NOT NULL,
    primaryStar   INTEGER NOT NULL,
    starType      TEXT,
    stellarMass   REAL    NOT NULL,
    planetClass   TEXT,
    terraformable INTEGER NOT NULL,
    massEM        REAL    NOT NULL,
    wasDiscovered INTEGER NOT NULL,
    wasMapped     INTEGER NOT NULL,
    mapped        INTEGER NOT NULL DEFAULT 0,
    efficient     INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (shipId, bodyName)
);

CREATE INDEX IF NOT EXISTS idx_cartographic_body_system ON cartographic_body (shipId, systemAddress);

CREATE TABLE IF NOT EXISTS cartographic_system
(
    shipId         INTEGER NOT NULL,
    systemAddress  INTEGER NOT NULL,
    starSystem     TEXT    NOT NULL,
    honkBodyCount  INTEGER NOT NULL,
    allBodiesFound INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (shipId, systemAddress)
);
