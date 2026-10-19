-- Keep the voice selected before any active ship is known.
-- Kokoro and Google already have their own preference columns.
ALTER TABLE game_session ADD COLUMN supertonicVoice TEXT;
ALTER TABLE game_session ADD COLUMN piperVoice TEXT;
