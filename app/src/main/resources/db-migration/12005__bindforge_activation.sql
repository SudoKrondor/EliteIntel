-- Whether BindForge has been activated, and where its "before BindForge" backup went. Added 2026-10-13 (A1b).
--
-- A stored fact, not "the master has rows". Until now an empty master was read as "first setup has not run",
-- so a user who owns nothing - Frontier's device files only, or a custom .binds with stock device files - could
-- never get past SET UP: activated with nothing to master, they still looked unset. See alias-designer.md,
-- Activation.
--
-- A table of its own, not a column on bindforge_settings: this is state BindForge reached, not a choice the user
-- makes, and that table's DAO writes every column on every save.
--
-- activated_at is NULL until activation: filled with no prompt when every installation holds identical device
-- files, or when first setup finishes. It is written in the same transaction that fills the master.
--
-- backup_folder is the "before BindForge" backup, taken once before anything is written and never pruned. It is
-- recorded so it is taken only once - first setup can be abandoned and reopened - and so File Manager's retention
-- can tell it from the backups it ages out.
CREATE TABLE IF NOT EXISTS bindforge_activation
(
    id            INTEGER PRIMARY KEY CHECK (id = 1),
    activated_at  TEXT,
    backup_folder TEXT
);

INSERT OR IGNORE INTO bindforge_activation (id) VALUES (1);

-- A master that already has rows counts as activated: first setup filled it before this fact existed.
UPDATE bindforge_activation
SET activated_at = CURRENT_TIMESTAMP
WHERE id = 1
  AND EXISTS (SELECT 1 FROM bindforge_device_master);
