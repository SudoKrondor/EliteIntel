-- Exo-Mastery: a downloaded catalogue of star systems whose planets carry high-value exobiology,
-- and the commander's progress through it.
--
-- WHY a catalogue at all: a commander new to exobiology has no way to know where the money is.
-- The game shows biological signals one body at a time, after a detailed surface scan, and says
-- nothing about which of the four hundred billion systems is worth the trip. Spansh holds the
-- crowd-sourced answer, and a reduced copy of it (the species worth more than ten million credits,
-- within a thousand light years of Sol) is published at www.elite-intel.org. Nothing here is loaded
-- until the commander presses Enable on the Commander tab, because most commanders will never want
-- it and the ledger is not small.
--
-- WHY three tables rather than one JSON blob: the route command wants the highest-paying system
-- that still has an unsampled body in it, the stats panel wants sums, and the scan subscriber wants
-- to tick off one species on one body. All three are one indexed query against normalised rows and
-- a scan of a blob otherwise.
--
-- WHY the completed flag lives on the body and survives a purge: a body's organics can be sampled
-- exactly once, ever. The game remembers, the journal does not report it, and a commander who did
-- the sampling before installing the app has no journal for it at all. So the flag is written from
-- three places - the survey-complete latch on the location row, the third organic scan of the last
-- listed species, and the commander saying so - and Disable keeps every completed body so that a
-- later Enable does not send the commander back to a planet the game will not let them scan.
--
-- systemAddress is the journal's SystemAddress (Spansh id64), bodyId is the journal BodyID decoded
-- from the Spansh body id64, so both join directly to the location table and to ScanOrganic. The
-- species symbol is the Frontier stem (Stratum_07), never the localised name, for the same reason.
--
-- NOTE: no semicolon may appear inside these comments. Migrations are split on a semicolon at end of
-- line before comments are stripped, so one here would cut the file mid-comment and hand SQLite a
-- statement with no SQL in it.
CREATE TABLE IF NOT EXISTS exo_mastery_system
(
    systemAddress
    INTEGER
    PRIMARY
    KEY,
    starSystem
    TEXT
    NOT
    NULL
    COLLATE
    NOCASE,
    x
    DOUBLE
    PRECISION
    NOT
    NULL,
    y
    DOUBLE
    PRECISION
    NOT
    NULL,
    z
    DOUBLE
    PRECISION
    NOT
    NULL
);

CREATE TABLE IF NOT EXISTS exo_mastery_body
(
    id
    INTEGER
    PRIMARY
    KEY
    AUTOINCREMENT,
    systemAddress
    INTEGER
    NOT
    NULL,
    bodyId
    INTEGER
    NOT
    NULL,
    starSystem
    TEXT
    NOT
    NULL
    COLLATE
    NOCASE,
    bodyName
    TEXT
    NOT
    NULL,
    bodyType
    TEXT,
    value
    INTEGER
    NOT
    NULL
    DEFAULT
    0,
    completed
    BOOLEAN
    NOT
    NULL
    DEFAULT
    FALSE,
    completedAt
    TEXT,
    UNIQUE
(
    systemAddress,
    bodyId
)
    );

CREATE INDEX IF NOT EXISTS idx_exo_mastery_body_system ON exo_mastery_body (systemAddress, completed);

CREATE TABLE IF NOT EXISTS exo_mastery_species
(
    id
    INTEGER
    PRIMARY
    KEY
    AUTOINCREMENT,
    systemAddress
    INTEGER
    NOT
    NULL,
    bodyId
    INTEGER
    NOT
    NULL,
    speciesSymbol
    TEXT
    NOT
    NULL,
    speciesName
    TEXT
    NOT
    NULL,
    colonies
    INTEGER
    NOT
    NULL
    DEFAULT
    0,
    value
    INTEGER
    NOT
    NULL
    DEFAULT
    0,
    sampled
    BOOLEAN
    NOT
    NULL
    DEFAULT
    FALSE,
    UNIQUE
(
    systemAddress,
    bodyId,
    speciesSymbol
)
    );
