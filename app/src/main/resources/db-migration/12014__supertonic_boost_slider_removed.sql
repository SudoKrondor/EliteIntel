-- The "Supertonic 3 boost" slider is gone (2026-10-28). Tuning settled on 60%, which SupertonicBoost now applies
-- to every Supertonic utterance, so the stored percentage has nothing left to control.
--
-- supertonicBoost is the on/off switch 11004 turned into the percentage. Nothing has read it since.

ALTER TABLE game_session
    DROP COLUMN supertonicBoostPercent;
ALTER TABLE game_session
    DROP COLUMN supertonicBoost;
