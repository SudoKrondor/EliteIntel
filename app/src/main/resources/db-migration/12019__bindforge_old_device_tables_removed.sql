-- The device master, the draft and their companions are gone (Storage S4b, Alan, 2026-10-28 - storage.md, and
-- device-provenance.md, which says what is left).
--
-- The working copy is the source of truth, and the edits not applied are held in memory until APPLY, so nothing reads
-- or writes these tables any more. Since Storage S4a the Alias Designer and the Device Editor work on the edits, and
-- since S4b the identity question and the Bind Editor's controller names and labels do too.
--
-- bindforge_identity_answer stays: the identity question still remembers Not now and None of these.
-- bindforge_settings.edit_history_retention stays until Storage S6, which removes the preset file's own history that
-- still reads it (Alan, 2026-10-28).
--
-- Children before their parents, since the labels and the draft reference the master.
DROP TABLE IF EXISTS bindforge_device_labels;
DROP TABLE IF EXISTS bindforge_device_draft_labels;
DROP TABLE IF EXISTS bindforge_device_draft;
DROP TABLE IF EXISTS bindforge_device_draft_state;
DROP TABLE IF EXISTS bindforge_device_pending_removal;
DROP TABLE IF EXISTS bindforge_device_installs;
DROP TABLE IF EXISTS bindforge_device_master;

-- The bindings waiting to move from an old hardware id (A17). Onboarding's FINISH moves them in the edit since S4a.
DROP TABLE IF EXISTS bindforge_hardware_id_move;

-- missed_write marked an installation for catch-up (A14), which S4a removed. An installation a write missed now
-- differs from the working copy, and the check reconciles it (Storage S9).
ALTER TABLE bindforge_installations DROP COLUMN missed_write;
