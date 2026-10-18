-- Bindings waiting to move from a controller's old hardware id to its name. Added 2026-10-17 (Alan, A17).
--
-- A controller with no entry in DeviceMappings.xml is named in .binds by its hardware id - VID then PID, eight hex
-- characters. A firmware update or a replacement gives it a new id, and every one of those bindings stops working.
-- When the user says an attached, unnamed controller is the missing one, it is named, and at Apply its bindings move
-- from the OLD id to the name (alias-designer.md, What Changes When Hardware Changes, Built - A17). Apply's own move
-- finds only a device's current VID/PID, so the old one is kept here until the move is done.
--
-- One row per controller, keyed by the hardware it has now, as the master and the draft hold it - so the row follows
-- the device through a rename. vid, pid, old_vid and old_pid are stored upper case.
--
-- A row is forgotten once Apply has moved the bindings or found none left to move, once no device in the master or
-- the draft has that hardware (DISCARD, CLEAR), and once the old controller is attached again - its bindings then
-- work again and stay with it.
CREATE TABLE IF NOT EXISTS bindforge_hardware_id_move
(
    vid         TEXT NOT NULL,
    pid         TEXT NOT NULL,
    old_vid     TEXT NOT NULL,
    old_pid     TEXT NOT NULL,
    recorded_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (vid, pid),
    UNIQUE (old_vid, old_pid)
);
