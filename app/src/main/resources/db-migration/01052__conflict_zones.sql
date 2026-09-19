-- Conflict zones, and the combat bonds earned fighting in them.
--
-- The other half of hunting_ground. A resource extraction site is where pirates are hunted for
-- bounties, a conflict zone is where two factions at war are fought for combat bonds. The commander
-- flies to one, drops in, picks a side and fights - there is no contract to take first, so this is
-- the "find a war zone" ledger the way hunting_ground is the "find a hunting ground" one.
--
-- WHY one row per star system with counts, like hunting_ground: a commander choosing where to fight
-- wants to know that this system has two high-intensity zones and that one has five low. The counts
-- come from the SignalName symbol ($Warzone_PointRace_Low/Med/High and $Warzone_Powerplay_*), never
-- the localised name, so they mean the same thing whatever language the game client runs in. Each
-- zone carries an index in its symbol and the same zone can be listed twice in one sweep, so a count
-- is the number of DISTINCT indices seen in one sweep, and the largest such count is kept.
--
-- WHY lastSeen matters here and barely does on hunting_ground: a ring keeps its resource sites for
-- ever, but a war lasts days. A zone not sighted for a week is over, and the row is treated as stale
-- rather than offered. Rows are never deleted for that - the next war in the same system re-sights
-- them and the row comes back to life.
--
-- The two sides and the kind of war come from the Conflicts block of FSDJump and Location, which is
-- keyed on the same system. They are optional: a zone can be sighted before the arrival that names
-- the sides is seen, and a war can be known before its zones are.
--
-- Rows are fed both from the commander's own journal and from the EDDN relay of other commanders'
-- journals, so the ledger fills in quietly the longer the app runs. Neither source is announced.
--
-- combat_bond is the tally of FactionKillBond rewards not yet cashed in, for the HUD card. It is the
-- combat-zone twin of the bounties table. Redeeming combat bonds (RedeemVoucher, type CombatBond)
-- empties it, which is how the card knows the fight is over without a session of its own. The
-- UNIQUE key makes replaying a journal line harmless. It is the whole line, because the event carries
-- nothing else to tell two bonds apart, and journal timestamps are whole seconds - so two bonds for the
-- same side, the same victim faction and the same reward in the same second are one row. That is two
-- identical ships killed within a second of each other, and the card counting one kill short of the
-- truth for it is accepted over a replay counting every kill twice.
--
-- NOTE: no semicolon may appear inside these comments. Migrations are split on a semicolon at end of
-- line before comments are stripped, so one here would cut the file mid-comment and hand SQLite a
-- statement with no SQL in it.
CREATE TABLE IF NOT EXISTS conflict_zone
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
    czLow
    INTEGER
    NOT
    NULL
    DEFAULT
    0,
    czMedium
    INTEGER
    NOT
    NULL
    DEFAULT
    0,
    czHigh
    INTEGER
    NOT
    NULL
    DEFAULT
    0,
    czPowerplay
    INTEGER
    NOT
    NULL
    DEFAULT
    0,
    warType
    TEXT,
    faction1
    TEXT,
    faction2
    TEXT,
    firstSeen
    TEXT,
    lastSeen
    TEXT
);
CREATE INDEX IF NOT EXISTS idx_conflict_zone_address ON conflict_zone (systemAddress);

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
