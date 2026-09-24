-- Commander tree baseline. Builds every commander-scoped table at the shape the shared tree had left it in
-- by v1.1.0019 (the last file before the split is 01058__ship_modules.sql).
--
-- Each commander gets a file of their own, cmdr_<FID>.db, built from this tree and attached next to the shared
-- database. Leaking one commander's rows into another's is therefore impossible, and the DAOs stay unqualified
-- because SQLite resolves a table name in whichever attached file holds it.
--
-- The player table is the one exception to "same shape as before": the user's own preferences (announcement
-- toggles, the journal and bindings folders) moved to the shared user_preferences table in shared 11000, because
-- they belong to the person rather than to one commander. The journal folder in particular has to be known before
-- any commander is, since reading the journals is how the commander is found.

CREATE TABLE IF NOT EXISTS bio_samples
(
    id
    INTEGER
    PRIMARY
    KEY
    AUTOINCREMENT,
    key
    TEXT
    NOT
    NULL
    UNIQUE,
    json
    TEXT
);
CREATE INDEX IF NOT EXISTS bio_samples_key_index ON bio_samples (key);

CREATE TABLE IF NOT EXISTS bounties
(
    id
    INTEGER
    PRIMARY
    KEY
    AUTOINCREMENT,
    key
    TEXT
    NOT
    NULL
    UNIQUE,
    bounty
    TEXT
);
CREATE INDEX IF NOT EXISTS bounty_key_index ON bounties (key);

CREATE TABLE IF NOT EXISTS cargo
(
    id
    INTEGER
    PRIMARY
    KEY
    AUTOINCREMENT,
    json
    TEXT
);

CREATE TABLE IF NOT EXISTS chat_history
(
    id
    INTEGER
    PRIMARY
    KEY
    AUTOINCREMENT,
    json
    TEXT
    NOT
    NULL,
    timestamp
    TEXT
    NOT
    NULL
    DEFAULT (
    datetime
(
    'now'
))
    );

CREATE TABLE IF NOT EXISTS codex_entries
(
    id
    INTEGER
    PRIMARY
    KEY
    AUTOINCREMENT,
    subCategory
    TEXT
    NOT
    NULL,
    starSystem
    TEXT
    NOT
    NULL,
    bodyId
    INTEGER
    NOT
    NULL,
    latitude
    REAL
    NOT
    NULL,
    longitude
    REAL
    NOT
    NULL,
    entryName
    TEXT
    NOT
    NULL,
    voucherAmount
    BIGINT
    NOT
    NULL
    DEFAULT
    0,
    entrySymbol
    TEXT
);

CREATE TABLE IF NOT EXISTS combat_bond
(
    id
    INTEGER
    PRIMARY
    KEY
    AUTOINCREMENT,
    systemAddress
    INTEGER,
    awardingFaction
    TEXT,
    victimFaction
    TEXT,
    reward
    INTEGER
    NOT
    NULL
    DEFAULT
    0,
    earnedAt
    TEXT
    NOT
    NULL,
    UNIQUE
(
    earnedAt,
    awardingFaction,
    victimFaction,
    reward
)
    );

CREATE TABLE IF NOT EXISTS commodity_search_line
(
    position
    INTEGER
    PRIMARY
    KEY,
    commodity
    TEXT
    NOT
    NULL,
    symbol
    TEXT,
    price
    INTEGER
    NOT
    NULL
    DEFAULT
    0,
    supply
    INTEGER
    NOT
    NULL
    DEFAULT
    0,
    unitsToBuy
    INTEGER
    NOT
    NULL
    DEFAULT
    0
);

CREATE TABLE IF NOT EXISTS commodity_search_result
(
    id
    INTEGER
    PRIMARY
    KEY
    CHECK
(
    id =
    1
),
    commodity TEXT NOT NULL,
    starSystem TEXT,
    stationName TEXT,
    stationType TEXT,
    price INTEGER NOT NULL DEFAULT 0,
    supply INTEGER NOT NULL DEFAULT 0,
    fleetCarrier INTEGER NOT NULL DEFAULT 0,
    foundAt TEXT,
    side TEXT NOT NULL DEFAULT 'BUY'
    );

CREATE TABLE IF NOT EXISTS construction_requirement
(
    marketId
    INTEGER
    NOT
    NULL,
    symbol
    TEXT
    NOT
    NULL,
    gameName
    TEXT,
    requiredAmount
    INTEGER
    NOT
    NULL
    DEFAULT
    0,
    providedAmount
    INTEGER
    NOT
    NULL
    DEFAULT
    0,
    payment
    INTEGER
    NOT
    NULL
    DEFAULT
    0,
    PRIMARY
    KEY
(
    marketId,
    symbol
)
    );
CREATE INDEX IF NOT EXISTS idx_construction_requirement_market ON construction_requirement (marketId);

CREATE TABLE IF NOT EXISTS construction_site
(
    marketId
    INTEGER
    PRIMARY
    KEY,
    stationName
    TEXT,
    starSystem
    TEXT,
    systemAddress
    INTEGER,
    progress
    REAL
    NOT
    NULL
    DEFAULT
    0,
    complete
    INTEGER
    NOT
    NULL
    DEFAULT
    0,
    failed
    INTEGER
    NOT
    NULL
    DEFAULT
    0,
    visitedAt
    TEXT,
    isCurrent
    INTEGER
    NOT
    NULL
    DEFAULT
    0
);

CREATE TABLE IF NOT EXISTS deferred_notifications
(
    id
    INTEGER
    PRIMARY
    KEY
    AUTOINCREMENT,
    key
    TEXT
    NOT
    NULL,
    timeToNotify
    BIG
    INT
    NOT
    NULL,
    notification
    TEXT
    NOT
    NULL
);

CREATE TABLE IF NOT EXISTS destination_reminder
(
    id
    INTEGER
    PRIMARY
    KEY
    AUTOINCREMENT,
    starSystem
    TEXT
    DEFAULT
    NULL,
    reminder
    TEXT
    DEFAULT
    NULL,
    stationName
    TEXT
    DEFAULT
    NULL,
    contact
    TEXT
    DEFAULT
    NULL
);

CREATE TABLE IF NOT EXISTS fleet_carrier
(
    id
    INTEGER
    PRIMARY
    KEY
    AUTOINCREMENT,
    json
    TEXT
);

CREATE TABLE IF NOT EXISTS fleet_carrier_route
(
    id
    INTEGER
    PRIMARY
    KEY
    AUTOINCREMENT,
    leg
    INTEGER
    NOT
    NULL
    UNIQUE,
    systemName
    TEXT
    NOT
    NULL
    UNIQUE,
    distance
    DOUBLE
    NOT
    NULL,
    fuelUsed
    INTEGER
    NOT
    NULL,
    remainingFuel
    INTEGER
    NOT
    NULL,
    hasIcyRing
    BOOLEAN
    NOT
    NULL,
    isPristine
    BOOLEAN
    NOT
    NULL,
    x
    DOUBLE
    NOT
    NULL,
    y
    DOUBLE
    NOT
    NULL,
    z
    DOUBLE
    NOT
    NULL
);
CREATE INDEX IF NOT EXISTS carrier_route_key_index ON fleet_carrier_route (leg);

CREATE TABLE IF NOT EXISTS fsd_target
(
    id
    INTEGER
    PRIMARY
    KEY
    AUTOINCREMENT,
    json
    TEXT
);

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

CREATE TABLE IF NOT EXISTS mining_targets
(
    id
    INTEGER
    PRIMARY
    KEY
    AUTOINCREMENT,
    target
    TEXT
    NOT
    NULL
    UNIQUE
);

CREATE TABLE IF NOT EXISTS missions
(
    id
    INTEGER
    PRIMARY
    KEY
    AUTOINCREMENT,
    key
    BIGINT
    NOT
    NULL
    UNIQUE,
    mission
    TEXT,
    missionType
    TEXT
    NOT
    NULL
    DEFAULT
    'PIRATES',
    keywords
    TEXT
);
CREATE INDEX IF NOT EXISTS mission_key_index ON missions (key);

CREATE TABLE IF NOT EXISTS neutron_star_route
(
    id
    INTEGER
    PRIMARY
    KEY
    AUTOINCREMENT,
    leg
    INTEGER
    NOT
    NULL
    UNIQUE,
    systemAddress
    INTEGER
    NOT
    NULL
    UNIQUE,
    systemName
    TEXT
    NOT
    NULL,
    distanceJumped
    DOUBLE
    NOT
    NULL,
    distanceLeft
    DOUBLE
    NOT
    NULL,
    jumps
    INTEGER
    NOT
    NULL,
    neutronStar
    BOOLEAN
    NOT
    NULL,
    x
    DOUBLE
    NOT
    NULL,
    y
    DOUBLE
    NOT
    NULL,
    z
    DOUBLE
    NOT
    NULL
);
CREATE INDEX IF NOT EXISTS neutron_route_leg_index ON neutron_star_route (leg);

CREATE TABLE IF NOT EXISTS player
(
    id
    INTEGER
    PRIMARY
    KEY
    CHECK
(
    id =
    1
),
    current_primary_star TEXT NOT NULL DEFAULT '',
    carrier_departure_time TEXT NOT NULL DEFAULT '',
    crew_wags_payout INTEGER NOT NULL DEFAULT 0,
    current_ship TEXT NOT NULL DEFAULT '',
    current_ship_name TEXT NOT NULL DEFAULT '',
    current_location_id INTEGER,
    current_wealth INTEGER NOT NULL DEFAULT 0,
    final_destination TEXT NOT NULL DEFAULT '',
    game_version TEXT NOT NULL DEFAULT '',
    goods_sold_this_session INTEGER NOT NULL DEFAULT 0,
    highest_single_transaction INTEGER NOT NULL DEFAULT 0,
    in_game_name TEXT NOT NULL DEFAULT '',
    insurance_claims INTEGER NOT NULL DEFAULT 0,
    last_known_carrier_location TEXT NOT NULL DEFAULT '',
    last_scan_id INTEGER NOT NULL DEFAULT -1,
    market_profits INTEGER NOT NULL DEFAULT 0,
    personal_credits_available INTEGER NOT NULL DEFAULT 0,
    player_highest_military_rank TEXT NOT NULL DEFAULT '',
    player_name TEXT NOT NULL DEFAULT '',
    ships_owned INTEGER NOT NULL DEFAULT 0,
    species_first_logged INTEGER NOT NULL DEFAULT 0,
    total_bounty_claimed INTEGER NOT NULL DEFAULT 0,
    total_distance_traveled REAL NOT NULL DEFAULT 0.0,
    total_hyperspace_distance INTEGER NOT NULL DEFAULT 0,
    total_profits_from_exploration INTEGER NOT NULL DEFAULT 0,
    total_systems_visited INTEGER NOT NULL DEFAULT 0,
    exobiology_profits INTEGER NOT NULL DEFAULT 0,
    alternative_name TEXT,
    game_build TEXT,
    bounty_collected_lifetime BIGINT DEFAULT 0,
    homeSystemId BIGINT DEFAULT 0,
    systemAddress BIG INT NOT NULL DEFAULT 0,
    currentGenus TEXT
    );
INSERT
OR IGNORE INTO player (id) VALUES (1);

CREATE TABLE IF NOT EXISTS player_status
(
    id
    INTEGER
    PRIMARY
    KEY
    AUTOINCREMENT,
    timestamp
    TEXT
    NOT
    NULL
    DEFAULT
    CURRENT_TIMESTAMP,
    event
    TEXT
    NOT
    NULL
    DEFAULT
    '',
    flags
    INTEGER
    NOT
    NULL
    DEFAULT
    0,
    flags2
    INTEGER
    NOT
    NULL
    DEFAULT
    0,
    fireGroup
    INTEGER
    NOT
    NULL
    DEFAULT
    0,
    guiFocus
    INTEGER
    NOT
    NULL
    DEFAULT
    0,
    cargo
    DOUBLE
    NOT
    NULL
    DEFAULT
    0,
    latituge
    DOUBLE
    NOT
    NULL
    DEFAULT
    0,
    longitude
    DOUBLE
    NOT
    NULL
    DEFAULT
    0,
    heading
    INTEGER
    NOT
    NULL
    DEFAULT
    0,
    altitude
    DOUBLE
    NOT
    NULL
    DEFAULT
    0,
    balance
    BIGINT
    NOT
    NULL
    DEFAULT
    0,
    planetRadius
    DOUBLE
    NOT
    NULL
    DEFAULT
    0,
    pips
    TEXT
    DEFAULT
    NULL,
    legalState
    TEXT
    DEFAULT
    NULL,
    destination
    TEXT
    DEFAULT
    NULL,
    oxygen
    REAL
    DEFAULT
    NULL,
    health
    REAL
    DEFAULT
    NULL,
    temperature
    REAL
    DEFAULT
    NULL,
    selectedWeapon
    TEXT
    DEFAULT
    NULL,
    gravity
    REAL
    DEFAULT
    NULL
);
INSERT
OR IGNORE INTO player_status (id, timestamp, event, flags, flags2, fireGroup, guiFocus, cargo, latituge,
                                     longitude, heading, altitude, balance, planetRadius)
VALUES (1, '1984-01-01 00:00:00', 'init', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

CREATE TABLE IF NOT EXISTS ranks_and_progress
(
    id
    INTEGER
    PRIMARY
    KEY
    AUTOINCREMENT,
    json
    TEXT
);

CREATE TABLE IF NOT EXISTS reputation
(
    id
    INTEGER
    PRIMARY
    KEY
    AUTOINCREMENT,
    json
    TEXT
);

CREATE TABLE IF NOT EXISTS ship
(
    id
    INTEGER
    PRIMARY
    KEY
    AUTOINCREMENT,
    shipName
    TEXT
    NOT
    NULL,
    shipId
    INTEGER
    NOT
    NULL
    UNIQUE,
    shipIdentifier
    TEXT,
    cargoCapacity
    INTEGER,
    voice
    TEXT
    NOT
    NULL
    DEFAULT
    'EMMA',
    personality
    VARCHAR
(
    255
) NOT NULL DEFAULT 'PROFESSIONAL',
    commanderName TEXT
    );

CREATE TABLE IF NOT EXISTS ship_loadout
(
    id
    INTEGER
    PRIMARY
    KEY
    AUTOINCREMENT,
    json
    TEXT
);
CREATE INDEX IF NOT EXISTS ship_loadout_index ON ship_loadout (id);

CREATE TABLE IF NOT EXISTS ship_route
(
    id
    INTEGER
    PRIMARY
    KEY
    AUTOINCREMENT,
    leg
    INTEGER
    NOT
    NULL
    UNIQUE,
    x
    DOUBLE
    NOT
    NULL,
    y
    DOUBLE
    NOT
    NULL,
    z
    DOUBLE
    NOT
    NULL,
    remainingJumps
    INTEGER
    NOT
    NULL,
    starClass
    TEXT
    NOT
    NULL,
    systemName
    TEXT
    NOT
    NULL,
    scoopable
    BOOLEAN
    NOT
    NULL
);
CREATE INDEX IF NOT EXISTS ship_route_key_index ON ship_route (leg);

CREATE TABLE IF NOT EXISTS ship_scans
(
    id
    INTEGER
    PRIMARY
    KEY
    AUTOINCREMENT,
    key
    TEXT
    NOT
    NULL
    UNIQUE,
    scan
    TEXT
);
CREATE INDEX IF NOT EXISTS scan_key_index ON ship_scans (key);

CREATE TABLE IF NOT EXISTS ship_settings
(
    id
    INTEGER
    PRIMARY
    KEY
    AUTOINCREMENT,
    shipId
    INTEGER
    NOT
    NULL
    UNIQUE,
    honkTrigger
    INTEGER
    NOT
    NULL
    DEFAULT
    1,
    honkFireGroup
    TEXT
    DEFAULT
    'A',
    honkOnJump
    BOOLEAN
    DEFAULT
    FALSE,
    hgeAlerts
    BOOLEAN
    NOT
    NULL
    DEFAULT
    FALSE,
    vehicleBay1
    TEXT
    DEFAULT
    NULL,
    vehicleBay2
    TEXT
    DEFAULT
    NULL,
    vehicleBay3
    TEXT
    DEFAULT
    NULL,
    vehicleBay4
    TEXT
    DEFAULT
    NULL,
    FOREIGN
    KEY
(
    shipId
) REFERENCES ship
(
    shipId
) ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS squadron_carrier
(
    id
    INTEGER
    PRIMARY
    KEY
    AUTOINCREMENT,
    json
    TEXT
);

CREATE TABLE IF NOT EXISTS squadron_carrier_route
(
    id
    INTEGER
    PRIMARY
    KEY
    AUTOINCREMENT,
    leg
    INTEGER
    NOT
    NULL
    UNIQUE,
    systemName
    TEXT
    NOT
    NULL
    UNIQUE,
    distance
    DOUBLE
    NOT
    NULL,
    fuelUsed
    INTEGER
    NOT
    NULL,
    remainingFuel
    INTEGER
    NOT
    NULL,
    hasIcyRing
    BOOLEAN
    NOT
    NULL,
    isPristine
    BOOLEAN
    NOT
    NULL,
    x
    DOUBLE
    NOT
    NULL,
    y
    DOUBLE
    NOT
    NULL,
    z
    DOUBLE
    NOT
    NULL
);
CREATE INDEX IF NOT EXISTS squadron_carrier_route_key_index ON squadron_carrier_route (leg);

CREATE TABLE IF NOT EXISTS target_location
(
    id
    INTEGER
    PRIMARY
    KEY
    AUTOINCREMENT,
    json
    TEXT
);

CREATE TABLE IF NOT EXISTS trade_profile
(
    id
    INTEGER
    PRIMARY
    KEY
    AUTOINCREMENT,
    shipId
    INTEGER
    NOT
    NULL
    UNIQUE,
    padSize
    TEXT
    CHECK (
    padSize
    IN
(
    'S',
    'M',
    'L'
)),
    allowPlanetary INTEGER NOT NULL DEFAULT 0,
    allowProhibited INTEGER NOT NULL DEFAULT 0,
    allowPermit INTEGER NOT NULL DEFAULT 0,
    allowFleetCarrier INTEGER NOT NULL DEFAULT 0,
    startingBudget INTEGER NOT NULL DEFAULT 0,
    maxDistanceLs INTEGER,
    maxJumps INTEGER NOT NULL DEFAULT 15,
    allowStrongHold BOOLEAN DEFAULT FALSE,
    FOREIGN KEY
(
    shipId
) REFERENCES ship
(
    shipId
) ON DELETE CASCADE
    );
CREATE INDEX IF NOT EXISTS trade_profile_ship_id_index ON trade_profile (shipId);

CREATE TABLE IF NOT EXISTS trade_route
(
    id
    INTEGER
    PRIMARY
    KEY
    AUTOINCREMENT,
    legNumber
    INTEGER
    NOT
    NULL
    UNIQUE,
    json
    TEXT
    NOT
    NULL,
    totalLegs
    INTEGER
    DEFAULT
    NULL
);

CREATE TABLE IF NOT EXISTS trade_tuple
(
    id
    INTEGER
    PRIMARY
    KEY
    AUTOINCREMENT,
    sourceCommodity
    TEXT
    NOT
    NULL,
    sourceStarSystem
    TEXT
    NOT
    NULL,
    sourceStationName
    TEXT
    NOT
    NULL,
    sourceStationType
    TEXT
    NOT
    NULL,
    sourceBuyPrice
    INTEGER
    NOT
    NULL,
    sourceSupply
    BIGINT
    NOT
    NULL,
    destinationCommodity
    TEXT
    NOT
    NULL,
    destinationStarSystem
    TEXT
    NOT
    NULL,
    destinationStationName
    TEXT
    NOT
    NULL,
    destinationStationType
    TEXT
    NOT
    NULL,
    destinationSellPrice
    INTEGER
    NOT
    NULL,
    destinationDemand
    BIGINT
    NOT
    NULL
);
