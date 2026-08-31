-- The jukebox: a playlist of music files, and where the commander was in it when they last quit.
--
-- V1.2 opens a new migration band. 001XX was V1.0 and 010XX was V1.1, so V1.2 numbers from 01100.
--
-- WHY two tables rather than columns on game_session: a playlist is a list, and a list does not fit in
-- a settings row. The transport state IS settings-shaped and could have gone on game_session, but it is
-- kept beside the playlist it describes so the whole feature is one pair of tables that lifts out
-- wholesale. A music player is not Elite-specific, and the project is slowly separating game-specific
-- code from the rest.
--
-- WHY path is UNIQUE: re-picking a folder, or importing a playlist that overlaps one already loaded,
-- must not double every track. Adding is therefore idempotent. The cost is that the same file cannot
-- deliberately appear twice in one playlist, which no one has asked for.
--
-- WHY AUTOINCREMENT: jukebox_state.currentTrackId names a row in this table, and a plain INTEGER
-- PRIMARY KEY re-uses the rowid of a deleted row. Remove the playing track, add another, and the saved
-- position would silently resume inside a DIFFERENT song. AUTOINCREMENT never re-issues an id, so a
-- stale currentTrackId resolves to nothing and is discarded, which is the honest outcome.
--
-- WHY ordinal is dense and separate from id: the commander drags rows to reorder them, so playlist
-- order is theirs to set. It cannot be id order, and it cannot be any sort of the tags.
--
-- WHY the tag columns are nullable: a folder of ten thousand files cannot be read for tags before the
-- list can be shown, so rows are inserted from the file path alone and a background pass fills in
-- title, artist, album and duration afterwards. tagsScannedAt marks a row as having been through that
-- pass, so an interrupted scan resumes where it stopped instead of starting over. A file whose tags
-- are empty is still marked scanned, so it is not retried forever.
--
-- WHY missing rather than deleting on sight: a drive that is not mounted yet, or a network share slow
-- to come back, is not a deleted file. The row is flagged so the commander sees which tracks are
-- unavailable and chooses to drop them, rather than the app quietly emptying their playlist.
--
-- WHY playbackOrder is TEXT and not a boolean shuffle flag: it is stored as the enum constant name,
-- the way ttsProvider is, so the value reads plainly in the database and a third order later is not a
-- schema change.
--
-- NOTE: no semicolon may appear inside these comments. Migrations are split on a semicolon at end of
-- line before comments are stripped, so one here would cut the file mid-comment and hand SQLite a
-- statement with no SQL in it.
CREATE TABLE IF NOT EXISTS jukebox_track
(
    id
    INTEGER
    PRIMARY
    KEY
    AUTOINCREMENT,
    path
    TEXT
    NOT
    NULL
    UNIQUE,
    ordinal
    INTEGER
    NOT
    NULL,
    title
    TEXT,
    artist
    TEXT,
    album
    TEXT,
    trackNumber
    INTEGER,
    durationMs
    INTEGER,
    tagsScannedAt
    TEXT,
    missing
    INTEGER
    NOT
    NULL
    DEFAULT
    0
);

CREATE INDEX IF NOT EXISTS idx_jukebox_track_ordinal ON jukebox_track (ordinal);

-- Partial index: the tag scanner asks only for the rows it has not read yet, and once the library is
-- scanned that set is empty, so a full index on tagsScannedAt would be paid for on every write and
-- read by nobody.
CREATE INDEX IF NOT EXISTS idx_jukebox_track_unscanned ON jukebox_track (ordinal) WHERE tagsScannedAt IS NULL;

CREATE TABLE IF NOT EXISTS jukebox_state
(
    id
    INTEGER
    PRIMARY
    KEY,
    musicFolder
    TEXT,
    volume
    INTEGER
    NOT
    NULL
    DEFAULT
    70,
    playbackOrder
    TEXT
    NOT
    NULL
    DEFAULT
    'SEQUENTIAL',
    currentTrackId
    INTEGER,
    positionMs
    INTEGER
    NOT
    NULL
    DEFAULT
    0
);

INSERT
OR IGNORE INTO jukebox_state (id) VALUES (1);
