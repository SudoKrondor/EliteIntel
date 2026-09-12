-- Hunting grounds, learned from the commander's own journal instead of a crowd-sourced service.
--
-- WHY this replaces pirate_hunting_grounds and mission_provider: those two were filled by the INTRA
-- API, which has returned nothing for two months. Everything they held is in the journal already. A
-- system has resource sites because FSSSignalDiscovered said so on arrival, and a provider is paired
-- with a target because the commander stood at a board in one and took a massacre contract against
-- the other. No network call is involved in either.
--
-- WHY massacre_mission is a ledger keyed on the game's own MissionID rather than a table of pairs:
-- the pair, the stack depth and the list of stations are all GROUP BY results over the contracts
-- actually taken. Keeping the raw rows means re-reading a journal cannot double-count anything - the
-- same MissionID lands on the same row - so the backfill scan is idempotent by construction and needs
-- no counters, no read-modify-write, and no de-duplication pass.
--
-- WHY the four res columns are counts and not flags: the grades are not interchangeable. A Low site
-- is beginner work with small bounties and a Hazardous one is engineered ships and the best payouts,
-- so a commander choosing where to hunt wants to know that this system has two Hazardous sites and
-- that one has five Low. The counts come from the SignalName symbol, never the localised name, so
-- they mean the same thing whatever language the game client runs in.
--
-- The counts are the largest seen in a single FSS sweep, not a running total. Every arrival re-emits
-- the whole signal set, so summing across visits reports forty Low sites in Sol where there are five.
--
-- WHY forgotten is kept rather than the row deleted: the commander forgets a hunting ground because
-- it is poor - low spawn rate, thin bounties, or a ring an hour out from the star - and none of that
-- is visible in the journal. Deleting the row would let the next arrival record it again and the app
-- would recommend it right back. The flag records the verdict, and the sightings underneath stay
-- honest.
--
-- Coordinates are on both tables because both are always known at the moment of writing. A hunting
-- ground is recorded from inside it, and a contract is accepted while docked in the provider system.
--
-- NOTE: no semicolon may appear inside these comments. Migrations are split on a semicolon at end of
-- line before comments are stripped, so one here would cut the file mid-comment and hand SQLite a
-- statement with no SQL in it.
CREATE TABLE IF NOT EXISTS hunting_ground
(
    id
    INTEGER
    PRIMARY
    KEY
    AUTOINCREMENT,
    starSystem
    TEXT
    NOT
    NULL
    UNIQUE
    COLLATE
    NOCASE,
    systemAddress
    INTEGER,
    x
    DOUBLE
    PRECISION,
    y
    DOUBLE
    PRECISION,
    z
    DOUBLE
    PRECISION,
    resStandard
    INTEGER
    NOT
    NULL
    DEFAULT
    0,
    resLow
    INTEGER
    NOT
    NULL
    DEFAULT
    0,
    resHigh
    INTEGER
    NOT
    NULL
    DEFAULT
    0,
    resHazardous
    INTEGER
    NOT
    NULL
    DEFAULT
    0,
    forgotten
    INTEGER
    NOT
    NULL
    DEFAULT
    0,
    firstSeen
    TEXT,
    lastSeen
    TEXT
);

CREATE INDEX IF NOT EXISTS idx_hunting_ground_address ON hunting_ground (systemAddress);

CREATE TABLE IF NOT EXISTS massacre_mission
(
    missionId
    INTEGER
    PRIMARY
    KEY,
    providerSystem
    TEXT
    NOT
    NULL
    COLLATE
    NOCASE,
    providerX
    DOUBLE
    PRECISION,
    providerY
    DOUBLE
    PRECISION,
    providerZ
    DOUBLE
    PRECISION,
    providerStation
    TEXT,
    providerFaction
    TEXT,
    targetSystem
    TEXT
    NOT
    NULL
    COLLATE
    NOCASE,
    targetFaction
    TEXT,
    killCount
    INTEGER,
    reward
    INTEGER,
    acceptedAt
    TEXT,
    completedAt
    TEXT
);

CREATE INDEX IF NOT EXISTS idx_massacre_mission_target ON massacre_mission (targetSystem);

-- Where the journal backfill stopped, so a re-run reads only what is new. One row, id 1.
CREATE TABLE IF NOT EXISTS hunting_ground_scan
(
    id
    INTEGER
    PRIMARY
    KEY,
    lastJournal
    TEXT,
    lastScanAt
    TEXT
);

INSERT
OR IGNORE INTO hunting_ground_scan (id) VALUES (1);

-- Carry across the systems the commander confirmed by hand under the old workflow. Their grade counts
-- are unknown and stay at zero until the system is visited again or the journal scan is run.
INSERT
OR IGNORE INTO hunting_ground (starSystem, x, y, z, forgotten)
SELECT starSystem, x, y, z, ignored
FROM pirate_hunting_grounds
WHERE hasResSite = 1;

DROP TABLE IF EXISTS pirate_hunting_grounds;

DROP TABLE IF EXISTS mission_provider;
