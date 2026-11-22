-- BindForge's device draft: the user's saved but unapplied device edits. Settled 2026-10-04 (Alan).
--
-- The master (12002) is what was last applied, and what every installation is compared against. The draft is
-- what the user is editing. SAVE writes here, and APPLY copies the draft into the master, empties the draft, and
-- then pushes the master out. The copy comes first, so a push that fails for one installation loses nothing:
-- the edits are in the master, the installation stops matching it, and the next Apply tries again.
--
-- Two stores rather than one, because a comparison against a half-edited master is meaningless - the startup
-- check and the install strip would fire against edits nobody has applied. See docs/02-features/bindforge/
-- overview.md, "The master and the draft".
--
-- The draft is a whole copy of the master's set, taken on the first edit, not a list of changes. Apply is then
-- a plain copy, and the master afterwards holds exactly what the user saw.

-- Whether a draft exists. One row or none.
--
-- WHY a marker rather than "the draft table has rows": a user who clears every device holds a draft with no
-- devices in it, and that has to promote to an empty master rather than be mistaken for no edits at all.
CREATE TABLE IF NOT EXISTS bindforge_device_draft_state
(
    id INTEGER PRIMARY KEY CHECK (id = 1)
);

-- The draft's devices, the same columns as bindforge_device_master.
--
-- master_id is the master row this one was copied from, and null for a device added in the draft. It is what
-- tells a rename from a removal plus an addition: a master device no draft row points at was removed, and its
-- name is reported when the draft is applied, because Apply itself adds and updates only.
CREATE TABLE IF NOT EXISTS bindforge_device_draft
(
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    master_id       INTEGER REFERENCES bindforge_device_master (id) ON DELETE SET NULL,
    device_name     TEXT    NOT NULL UNIQUE,
    vid             TEXT    NOT NULL COLLATE NOCASE,
    pid             TEXT    NOT NULL COLLATE NOCASE,
    alias_confirmed BOOLEAN NOT NULL DEFAULT 0,
    previous_name   TEXT
);

-- The draft's button and axis labels, the same shape as bindforge_device_labels.
CREATE TABLE IF NOT EXISTS bindforge_device_draft_labels
(
    device_id   INTEGER NOT NULL REFERENCES bindforge_device_draft (id) ON DELETE CASCADE,
    input_token TEXT    NOT NULL,
    label       TEXT    NOT NULL,
    PRIMARY KEY (device_id, input_token)
);
