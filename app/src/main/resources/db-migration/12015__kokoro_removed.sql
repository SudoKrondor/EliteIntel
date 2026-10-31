-- Kokoro is gone (2026-10-28). Supertonic 3 is the one local engine, for the main mouth and the radio, in every
-- language.
--
-- Its commanders move to Supertonic. Supertonic's cast carries the Kokoro voices under the same names, re-rendered
-- with the same timbre and sex, so each ship's stored voice carries over unchanged. A voice that was never in
-- that cast reads as Supertonic's default, the same as after any engine switch. The ship table belongs to the
-- commander file, which a shared migration may not touch, so it is left alone.
--
-- TtsProvider.fromStored already reads an unknown value as SUPERTONIC. This makes the row say so too, so the
-- settings tab and the support bundle show the engine that is actually speaking. The column default still says
-- KOKORO. Changing it would mean rebuilding the table, and every save writes the provider explicitly.
--
-- kokoroVoice is the app-wide Kokoro voice from before voices were per ship. Nothing has read it since.

UPDATE game_session
SET ttsProvider = 'SUPERTONIC'
WHERE ttsProvider = 'KOKORO';

ALTER TABLE game_session
    DROP COLUMN kokoroVoice;
