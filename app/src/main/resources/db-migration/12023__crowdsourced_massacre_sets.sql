-- Crowd-sourced massacre sets. Design in docs/CROWDSOURCED_MASSACRE_SETS.md.
--
-- shareMassacreSets - the commander's opt-in. Off by default. On, the app uploads the massacre sets it has
-- learned and uses the sets other commanders reported. Off, it does neither (give to get).
--
-- community_sync - this install's anonymous identity and when each upload and download last happened. One
-- row. It lives in the shared file because it belongs to the install, not to a commander. refreshToken is
-- encrypted with the app key like the API keys. A token this computer cannot read means a new anonymous
-- account, which costs nothing but a fresh uid.
--
-- crowd_massacre_set - the checked sets the nightly job published, replaced wholesale on every download.
-- Only sets the job could check against the galaxy index are kept, so every row has coordinates.
-- providerFactions is joined on the same separator the hunting-ground ledger uses, a character no faction
-- name carries.
--
-- NOTE: no semicolon may appear inside these comments. Migrations are split on a semicolon at end of
-- line before comments are stripped, so one here would cut the file mid-comment and hand SQLite a
-- statement with no SQL in it.
alter table global_settings
    add column shareMassacreSets boolean default false;

CREATE TABLE IF NOT EXISTS community_sync
(
    id               INTEGER PRIMARY KEY CHECK (id = 1),
    uid              TEXT,
    refreshToken     TEXT,
    lastPingAt       TEXT,
    lastReportAt     TEXT,
    lastReportDigest TEXT,
    lastDownloadAt   TEXT,
    setsEtag         TEXT
);

INSERT OR IGNORE INTO community_sync (id) VALUES (1);

CREATE TABLE IF NOT EXISTS crowd_massacre_set
(
    providerSystem   TEXT    NOT NULL COLLATE NOCASE,
    targetSystem     TEXT    NOT NULL COLLATE NOCASE,
    providerX        DOUBLE PRECISION NOT NULL,
    providerY        DOUBLE PRECISION NOT NULL,
    providerZ        DOUBLE PRECISION NOT NULL,
    providerFactions TEXT    NOT NULL,
    stations         TEXT,
    targetFaction    TEXT    NOT NULL,
    stackDepth       INTEGER NOT NULL,
    reporters        INTEGER NOT NULL,
    lastSeen         TEXT,
    PRIMARY KEY (providerSystem, targetSystem)
);
