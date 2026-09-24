-- The user's own preferences, lifted out of the player row ahead of the commander split.
--
-- The player row is about to move into each commander's own file (commander tree, 11000__baseline). These columns
-- belong to the person rather than to any one commander, so they stay in the shared database: the announcement
-- toggles are one set of preferences for the human, and the journal folder has to be known before any commander
-- is, because reading the journals is how the commander is found.
--
-- Column names match the player table's, so PlayerDao reads both halves back as one row.
--
-- This file runs exactly once on every database, before the Java split step moves the player table out, so
-- reading player here is safe. No later shared migration may touch a commander table.

CREATE TABLE IF NOT EXISTS user_preferences
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
    is_discovery_announcement_on BOOLEAN NOT NULL DEFAULT 1,
    is_mining_announcement_on BOOLEAN NOT NULL DEFAULT 1,
    is_navigation_announcement_on BOOLEAN NOT NULL DEFAULT 1,
    is_radio_transmission_on BOOLEAN,
    is_route_announcement_on BOOLEAN NOT NULL DEFAULT 1,
    is_planetary_approach_announcement_on BOOLEAN NOT NULL DEFAULT 1,
    is_address_me_on BOOLEAN NOT NULL DEFAULT 1,
    is_cargo_scoop_pickup_announcement_on BOOLEAN NOT NULL DEFAULT 1,
    radarAnnouncementOn BOOLEAN DEFAULT FALSE,
    journal_dir TEXT,
    bindings_dir TEXT
    );

INSERT
OR IGNORE INTO user_preferences (id, is_discovery_announcement_on, is_mining_announcement_on,
                                        is_navigation_announcement_on, is_radio_transmission_on,
                                        is_route_announcement_on, is_planetary_approach_announcement_on,
                                        is_address_me_on, is_cargo_scoop_pickup_announcement_on,
                                        radarAnnouncementOn, journal_dir, bindings_dir)
SELECT 1,
       is_discovery_announcement_on,
       is_mining_announcement_on,
       is_navigation_announcement_on,
       is_radio_transmission_on,
       is_route_announcement_on,
       is_planetary_approach_announcement_on,
       is_address_me_on,
       is_cargo_scoop_pickup_announcement_on,
       radarAnnouncementOn,
       journal_dir,
       bindings_dir
FROM player
WHERE id = 1;

INSERT
OR IGNORE INTO user_preferences (id) VALUES (1);

-- An empty copy of the location table left behind by a table rebuild in an external database tool. Nothing reads it.
DROP TABLE IF EXISTS location_dg_tmp;
