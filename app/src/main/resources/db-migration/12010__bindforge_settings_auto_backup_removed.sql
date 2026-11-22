-- The "Auto-backup on Elite-Intel launch" setting is gone (Alan, 2026-10-24, file-manager.md, Settings).
--
-- The backup taken when Elite-Intel starts is the safety net and cannot be switched off (storage.md rule 12), so
-- the switch has nothing left to control. Nothing ever read it.
--
-- edit_history_retention stays until Edit History itself is removed (Storage S4), because EditHistory and
-- StartPresetApply still read it.

ALTER TABLE bindforge_settings DROP COLUMN auto_backup_on_launch;
