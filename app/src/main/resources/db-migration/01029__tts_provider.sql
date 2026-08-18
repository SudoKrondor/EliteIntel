-- Names the TTS engine outright instead of deducing it from the boolean local/cloud flag plus the shape of
-- the stored cloud key.
--
-- Adding Microsoft Edge Read Aloud made the old pair unable to express the choice: Edge is keyless but not
-- local, so it fitted neither side of useLocalTTS, and it was selected by typing the reserved string
-- "edge://" into the Google TTS key field. The key column is for keys. This column carries the selection,
-- and the settings tab offers Edge as its own control.
--
-- KOKORO is the shipped default, so a fresh install still speaks with no account configured. Existing
-- installs carry their local/cloud choice over unchanged.
--
-- useLocalTTS is deliberately KEPT. Nothing reads it any more - SystemSession.useLocalTTS() now derives from
-- ttsProvider - but SystemSession.setTtsProvider keeps writing it, so a commander who installs this release
-- and then rolls back to an earlier jar still finds the local/cloud flag that jar expects to read. Dropping
-- the column would turn that rollback into a startup failure on the first session read.
--
-- A commander running a pre-release build of the Edge branch has an encrypted "edge://" in the key column,
-- which SQL cannot recognise. They land on GOOGLE with a key that is not one, and re-select Microsoft Edge
-- on the settings tab. Edge never shipped, so this can only affect branch builds.
--
-- NOTE: no semicolon may appear inside these comments. Migrations are split on a semicolon at end of
-- line before comments are stripped, so one here would cut the file mid-comment and hand SQLite a
-- statement with no SQL in it.
ALTER TABLE game_session
    ADD COLUMN ttsProvider TEXT NOT NULL DEFAULT 'KOKORO';

UPDATE game_session
SET ttsProvider = CASE WHEN useLocalTTS THEN 'KOKORO' ELSE 'GOOGLE' END;
