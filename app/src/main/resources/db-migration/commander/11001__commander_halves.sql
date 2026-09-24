-- The commander's half of the four mixed tables.
--
-- Each of these tables held galaxy facts every commander shares and, in one or two columns, what this commander did.
-- The shared half stays in the shared file. The commander's half lives here, keyed the same way as the shared row
-- it belongs to, and the DAOs join the two back together. The data is moved across once by CommanderSplit.

-- What the commander holds of each engineering material. material_names (shared) is the catalogue, keyed by symbol.
CREATE TABLE IF NOT EXISTS material_inventory
(
    symbol
    TEXT
    PRIMARY
    KEY,
    amount
    INTEGER
    NOT
    NULL
    DEFAULT
    0
);

-- Hunting grounds the commander told us to forget. hunting_ground (shared) is the ledger every commander fills.
CREATE TABLE IF NOT EXISTS hunting_ground_forgotten
(
    starSystem
    TEXT
    PRIMARY
    KEY
    COLLATE
    NOCASE
);

-- Exo-Mastery bodies the commander sampled out. The value is kept as it was on completion, so the harvested total
-- still reads after the shared catalogue is purged or replaced.
CREATE TABLE IF NOT EXISTS exo_mastery_harvest
(
    systemAddress
    INTEGER
    NOT
    NULL,
    bodyId
    INTEGER
    NOT
    NULL,
    value
    INTEGER
    NOT
    NULL
    DEFAULT
    0,
    completedAt
    TEXT,
    PRIMARY
    KEY
(
    systemAddress,
    bodyId
)
    );

-- Exo-Mastery species the commander has sampled.
CREATE TABLE IF NOT EXISTS exo_mastery_sample
(
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
    PRIMARY
    KEY
(
    systemAddress,
    bodyId,
    speciesSymbol
)
    );

-- What the commander did at a location: discovered it, mapped it, finished its bio survey, part-sampled it, lives
-- there. A JSON object of those LocationDto fields, merged over the shared location row (keyed by locationName)
-- when it is read. A location the commander did nothing at has no row.
CREATE TABLE IF NOT EXISTS location_visit
(
    locationName
    TEXT
    PRIMARY
    KEY,
    flags
    TEXT
    NOT
    NULL
);
