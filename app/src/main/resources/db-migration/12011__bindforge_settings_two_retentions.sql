-- Two retention settings replace the one (Alan, 2026-10-25, file-manager.md, Four kinds of backup and Settings).
--
-- Daily backups are kept by age: 30, 60, 90 or 180 days, 30 by default. Before-changes backups are kept by count:
-- the newest 100 by default, 10 to 1000. User and first setup backups have no retention setting - the user deletes
-- their own, and first setup's is kept forever.
--
-- Why two rules (Krondor's review, 2026-10-25): age alone deletes every good backup when a user is away longer than
-- the limit, and a count alone loses last week's good copy to one evening of applies.
--
-- The old backup_retention_days value is not carried over: no screen ever set it, V1.2 has not shipped, and it need
-- not be one of the four day choices (Alan, 2026-10-25).

ALTER TABLE bindforge_settings ADD COLUMN daily_backup_retention_days INTEGER NOT NULL DEFAULT 30;

ALTER TABLE bindforge_settings ADD COLUMN before_changes_backups_kept INTEGER NOT NULL DEFAULT 100;

ALTER TABLE bindforge_settings DROP COLUMN backup_retention_days;
