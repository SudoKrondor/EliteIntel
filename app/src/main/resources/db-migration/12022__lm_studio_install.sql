-- The LM Studio the app installed on this PC, if it installed one (docs/LM_STUDIO_INSTALL_ASSISTANCE.md, "The install
-- record"). No row means the app installed nothing, and so starts, stops and uninstalls nothing. At most one row, at
-- id = 1.
--
-- In the shared database, because it describes the PC, not a commander.
--
-- install_id is also written into a marker file inside home_dir when the install starts. The record counts only
-- while that file holds the same id. A record restored from another PC never drives an uninstall here, even when this
-- PC happens to have the same folder, because that folder holds no marker with this id.
--
-- installed_at is ISO-8601 text, as SQLite has no date type.
CREATE TABLE IF NOT EXISTS lm_studio_install
(
    id              INTEGER PRIMARY KEY CHECK (id = 1),
    install_id      TEXT    NOT NULL,
    home_dir        TEXT    NOT NULL,
    pointer_written BOOLEAN NOT NULL,
    setup_complete  BOOLEAN NOT NULL DEFAULT 0,
    installed_at    TEXT    NOT NULL
);
