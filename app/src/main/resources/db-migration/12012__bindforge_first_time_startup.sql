-- First-time startup, rebuilt on the working copy (Storage S2b, Alan, 2026-10-25 - storage.md, First-time startup,
-- and file-manager.md, Game Install Locations).
--
-- is_removed: an installation the user removed from Game Install Locations stays removed. Detection at the next start
-- would otherwise put it straight back, so the row is kept and marked, detection skips it, and it is neither backed up
-- nor written. Add missing on its folder brings it back. Until now Remove deleted the row.
ALTER TABLE bindforge_installations ADD COLUMN is_removed BOOLEAN NOT NULL DEFAULT 0;

-- backup_folder recorded the "before BindForge" backup, a folder of its own. First-time startup's backup is now a
-- zip in the backup folder's FirstSetup folder, kept forever by its kind, so nothing needs recording.
ALTER TABLE bindforge_activation DROP COLUMN backup_folder;

-- activated_at is now the "first-time startup complete" flag. It is cleared once here, so every tester set up the old
-- way - into the database master - goes through first-time startup again, from their live files (Alan, 2026-10-24
-- and 2026-10-25). V1.2 has not shipped, so no user of it loses anything.
UPDATE bindforge_activation SET activated_at = NULL WHERE id = 1;
