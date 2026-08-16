-- Persist the optional Google WaveNet pitch beside the other app-wide audio settings.
--
-- Zero preserves the provider voice's native pitch for existing and new installations. The UI and
-- session boundary constrain configured values to the Google API's supported -20 to 20 semitone range.
--
-- NOTE: no semicolon may appear inside these comments. Migrations are split on a semicolon at end of
-- line before comments are stripped, so one here would cut the file mid-comment and hand SQLite a
-- statement with no SQL in it.
ALTER TABLE game_session
    ADD COLUMN googleWaveNetPitch INTEGER NOT NULL DEFAULT 0;
