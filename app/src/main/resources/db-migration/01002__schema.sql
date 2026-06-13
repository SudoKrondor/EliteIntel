CREATE TABLE IF NOT EXISTS neutron_star_route
(
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    leg            INTEGER NOT NULL UNIQUE,
    systemAddress  INTEGER NOT NULL UNIQUE,
    systemName     TEXT    NOT NULL,
    distanceJumped DOUBLE  NOT NULL,
    distanceLeft   DOUBLE  NOT NULL,
    jumps          INTEGER NOT NULL,
    neutronStar    BOOLEAN NOT NULL,
    x              DOUBLE  NOT NULL,
    y              DOUBLE  NOT NULL,
    z              DOUBLE  NOT NULL
);
CREATE INDEX IF NOT EXISTS neutron_route_leg_index ON neutron_star_route (leg);
