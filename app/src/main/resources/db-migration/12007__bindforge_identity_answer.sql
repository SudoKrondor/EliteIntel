-- The identity question's answers that hold until the hardware changes. Added 2026-10-15 (Alan, A12).
--
-- When a named controller that bindings use is missing and an unnamed one is attached, BindForge asks which attached
-- controller it is - a firmware update or a replacement changes a controller's VID/PID. "Not now" and "None of these"
-- are remembered across restarts, so the question is not put again on every launch (alias-designer.md, What Changes
-- When Hardware Changes): re-asking turns a useful question into one dismissed without reading.
--
-- One row per missing device, the latest answer replacing the one before. candidates is the set of controllers the
-- question listed, as sorted upper-case VID:PID pairs joined by commas. "Until the hardware changes" is that set
-- changing: a controller plugged in or taken away gives another set, the row no longer matches, and the question is
-- asked again. A row is also forgotten once its device is attached again.
--
-- answer is NOT_NOW or NONE_OF_THESE. Both keep the question from being asked by itself. NOT_NOW also holds the
-- candidates back from onboarding's automatic offer, and CHECK AGAIN asks it again. NONE_OF_THESE frees them to be
-- named as new controllers.
CREATE TABLE IF NOT EXISTS bindforge_identity_answer
(
    device_name TEXT PRIMARY KEY COLLATE NOCASE,
    candidates  TEXT NOT NULL,
    answer      TEXT NOT NULL CHECK (answer IN ('NOT_NOW', 'NONE_OF_THESE')),
    answered_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
