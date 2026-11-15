-- The notices the user has chosen not to see again. Added 2026-11-14 (Alan, Storage S21).
--
-- A dialog that only tells the user something they have read before - APPLY's confirmation, APPLY's report, first-time
-- startup's report - carries a "Don't show this again" box. Ticking it adds the notice's row, unticking it removes the
-- row, each the moment the box changes. Reset warnings, on Services' Push To Talk and Other Settings tab, deletes every
-- row. A report that names anything beyond its plain success line shows whatever is stored here (storage.md rule 13).
--
-- One row per notice, keyed by its name, so a dialog anywhere in Elite-Intel can join with a name of its own and no
-- migration. Not BindForge's table: the reset button is not in BindForge.
CREATE TABLE IF NOT EXISTS quieted_notices
(
    notice     TEXT PRIMARY KEY,
    quieted_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
