-- BindForge's own settings, in a table of their own.
--
-- Not columns on game_session, which is where most settings live. That file is one of the busiest in the
-- codebase and is edited by several people in the same week, so every BindForge setting added there would
-- touch the same DAO, the same session class and the same long INSERT statement somebody else is editing.
-- A table of its own touches nobody: the migration, DAO and manager are all new.
--
-- Shape set by Krondor 2026-09-22: a singleton manager in front of its own DAO and its own table, modelled on
-- global_settings - one row at id = 1 inserted here, one typed column per setting, each with a default.
--
-- backup_destination is deliberately nullable. The specified default is "Elite-Intel's default backup path",
-- which is machine-specific, so writing an absolute path into a migration that runs on every installation
-- would be wrong. NULL means "the user has not chosen one". Resolving that to a real folder stays in
-- PlayerBackupService.resolvePlayerBackupsDir(), which already makes that decision - one resolver, not two.
--
-- The two retentions are different things and both are settings (Alan, 2026-09-28). Player backups age
-- out: keep them for N days, 30 by default. Edit History counts instead: keep N versions per file, 10 by
-- default. Naming them apart is deliberate - one folder pruned by date, one history pruned by depth.

CREATE TABLE IF NOT EXISTS bindforge_settings
(
    id                     INTEGER PRIMARY KEY CHECK (id = 1),
    auto_backup_on_launch  BOOLEAN NOT NULL DEFAULT 1,
    backup_destination     TEXT,
    backup_retention_days  INTEGER NOT NULL DEFAULT 30,
    edit_history_retention INTEGER NOT NULL DEFAULT 10
);

INSERT OR IGNORE INTO bindforge_settings (id) VALUES (1);
