-- Power conflict zones: where the powers are fighting each other, for commanders pledged to one.
--
-- A different fight from conflict_zone. A faction war is two minor factions and pays combat bonds to anyone
-- who picks a side. A power conflict zone ($Warzone_Powerplay_*) is a power's war and is fought for merits
-- by its pledged commanders, so the question asked of this ledger is not "where is a war" but "where is MY
-- power in one". conflict_zone keeps counting these zones in czPowerplay but never offers them.
--
-- WHY keyed by systemAddress: the FSS signal that announces a zone carries the system address and no system
-- name. The name is kept for plotting the route.
--
-- The zone count comes from FSS sweeps, the commander's own and the EDDN relay's, counted by distinct #index
-- the way conflict_zone counts them. The powers come from the arrival (FSDJump, Location) in the same
-- system: Powers is every power present, ControllingPower the one holding it, if any. They are kept as a
-- JSON array of the names the journal spells, and a pledged power is matched against that text.
--
-- The two halves age apart on purpose. lastSeen is the last sweep that saw zones, and only it decides whether
-- they are current (the powerplay cycle is weekly, so a week unseen is over). powersSeen is the last arrival
-- that named the powers, and a later arrival always wins - an arrival says nothing about whether zones are
-- still there, only who is in the system, so it never keeps old zones alive.
--
-- Seeded from conflict_zone rows that already counted powerplay zones, so the ledger starts with what the
-- app has learned. They have no powers yet and are not offered until an arrival names them.
--
-- NOTE: no semicolon may appear inside these comments. Migrations are split on a semicolon at end of
-- line before comments are stripped, so one here would cut the file mid-comment and hand SQLite a
-- statement with no SQL in it.
CREATE TABLE IF NOT EXISTS power_conflict_zone
(
    systemAddress    INTEGER PRIMARY KEY,
    starSystem       TEXT    NOT NULL COLLATE NOCASE,
    x                DOUBLE PRECISION,
    y                DOUBLE PRECISION,
    z                DOUBLE PRECISION,
    zones            INTEGER NOT NULL DEFAULT 0,
    controllingPower TEXT,
    powers           TEXT,
    powerplayState   TEXT,
    firstSeen        TEXT,
    lastSeen         TEXT,
    powersSeen       TEXT
);

INSERT OR IGNORE INTO power_conflict_zone (systemAddress, starSystem, x, y, z, zones, firstSeen, lastSeen)
SELECT systemAddress,
       starSystem,
       x,
       y,
       z,
       czPowerplay,
       firstSeen,
       lastSeen
FROM conflict_zone
WHERE czPowerplay > 0
  AND systemAddress IS NOT NULL;
