-- BindForge's device records: the master the user edits, its labels, and what each installation holds.
--
-- Three tables because there are two different facts here. The master records INTENT - the user's device
-- element set, which Apply pushes out to every installation. The per-installation rows record REALITY - what
-- each installation's DeviceMappings.xml actually contains right now. Drift is the difference between them.
--
-- Collapsing the two would remove the only copy of reality. A game patch resets these files per installation,
-- which is the incident BindForge exists for. With one row per device there is nothing to compare a patched
-- installation against, so a wipe becomes indistinguishable from a device that was never configured. The
-- per-installation rows are what let the M marker say "this installation matches the master" and the
-- divergence list say where it does not. See docs/03-data-models/device-provenance.md.
--
-- Frontier's own entries are deliberately NOT stored here. The stock DeviceMappings.xml ships as an
-- application resource instead: seeding its 51 elements as rows would need a new migration every time
-- Frontier adds a device, and an applied migration can never be edited. Anything present in a player's file
-- but absent from that resource was not shipped by Frontier.
--
-- VID and PID are COLLATE NOCASE. Frontier's file has no case convention at all - audited across all 136
-- tuples, and <DualShock4> carries VID 054C uppercase beside PID 05c4 lowercase on the same line - so roughly
-- half the file would miss a case-sensitive comparison. The stored text keeps whatever the file said, and
-- BindForge never rewrites Frontier's casing to normalise it.

-- The user's device element set: one row per device, and what Apply writes outward.
--
-- device_name is the alias. It becomes the XML element tag in DeviceMappings.xml and the filename stem of
-- that device's .buttonMap, which is why a rename is a multi-file transaction rather than a field edit.
--
-- UNIQUE here catches an exact duplicate only. The real alias-collision rule compares ignoring case, '-' and
-- '_', and must also check Frontier's built-in names, which are not in this table - so that rule lives in
-- Java where the whole of it can be applied. This constraint is a backstop, not the rule.
--
-- alias_confirmed gates button and axis naming: a .buttonMap is only ever created under a name the user has
-- looked at. It sits here rather than on the per-installation row because there is one record per device now
-- - confirming a name is not something that can be true of one installation and false of another.
--
-- previous_name is left behind by a rename so the old .buttonMap can be found and cleaned up in every
-- installation. Null at every other time.
CREATE TABLE IF NOT EXISTS bindforge_device_master
(
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    device_name     TEXT    NOT NULL UNIQUE,
    vid             TEXT    NOT NULL COLLATE NOCASE,
    pid             TEXT    NOT NULL COLLATE NOCASE,
    alias_confirmed BOOLEAN NOT NULL DEFAULT 0,
    previous_name   TEXT
);

-- The master's button and axis labels - the content of a .buttonMap, held as rows.
--
-- input_token is the .binds vocabulary: Joy_1, Joy_XAxis, Joy_POV1Up. ButtonInputMapper produces them from
-- the device's reported counts, and an existing .buttonMap is parsed into the same tokens.
--
-- One label per input per device, so the composite key is the identity. Labels cascade with their device:
-- clearing a device removes its labels, which is the whole of what CLEAR means.
CREATE TABLE IF NOT EXISTS bindforge_device_labels
(
    device_id   INTEGER NOT NULL REFERENCES bindforge_device_master (id) ON DELETE CASCADE,
    input_token TEXT    NOT NULL,
    label       TEXT    NOT NULL,
    PRIMARY KEY (device_id, input_token)
);

-- What each installation's DeviceMappings.xml holds, read from disk rather than chosen by the user.
--
-- The key is (install_id, device_name), approved 2026-09-06. A device element owns a SET of VID/PID pairs,
-- not one: <GamePad> alone carries a primary pair plus 79 alternatives, so keying on VID/PID would turn one
-- entry into eighty rows. The element tag is the identity the file itself uses, and it doubles as the
-- .buttonMap filename stem.
--
-- device_name is stored exactly as the file spells it, with no NOCASE collation: this records what is on
-- disk, and two installations may legitimately disagree - that disagreement is the drift being measured.
--
-- provenance answers "may BindForge touch this entry". 'unknown' exists because the shipped stock reference
-- goes stale: without it, a device Frontier adds after our capture would be recorded as the player's, after
-- which BindForge could offer to rename or clear something it does not own. 'unknown' means do not claim it
-- and do not touch it, and costs nothing.
--
-- Rows cascade with their installation. Removing an installation discards what was held against it, which is
-- deliberate - device records are rebuilt by a rescan, and backups are not kept here.
CREATE TABLE IF NOT EXISTS bindforge_device_installs
(
    install_id     INTEGER NOT NULL REFERENCES bindforge_installations (id) ON DELETE CASCADE,
    device_name    TEXT    NOT NULL,
    vid            TEXT COLLATE NOCASE,
    pid            TEXT COLLATE NOCASE,
    provenance     TEXT    NOT NULL DEFAULT 'unknown'
        CHECK (provenance IN ('frontier', 'user_preexisting', 'bindforge', 'unknown')),
    has_button_map BOOLEAN NOT NULL DEFAULT 0,
    PRIMARY KEY (install_id, device_name)
);
