-- The Edit History retention setting is gone (Storage S6, Alan, 2026-10-28 - file-manager.md, Settings).
--
-- The preset file's own master, draft and history were the last things that read it. Since S6 the preset file is
-- edited and written like every other file, through the edits and the one APPLY, and the before-APPLY backup is its
-- history, kept by before_changes_backups_kept.
ALTER TABLE bindforge_settings DROP COLUMN edit_history_retention;
