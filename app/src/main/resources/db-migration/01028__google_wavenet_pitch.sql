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


-- Removes Ollama from the schema. LM Studio is now the only local LLM host.
--
-- Ollama was supported alongside LM Studio for the whole V1.1 cycle and effectively nobody ran it:
-- it was too slow to keep up with the companion pipeline, and every support answer about it ended
-- with "use LM Studio instead". Rather than keep a second local host that is only ever recommended
-- against, it is gone from the app as of this migration - client, settings tab and schema.
--
-- The columns dropped here have had their read paths removed in the same change set:
--
--   localLlmProvider   - chose between the two local hosts. With Ollama gone there is one host left,
--                        so the setting has nothing to choose between and the HOST control that drove
--                        it is off the AI Services settings tab.
--   ollamaAddress      - the Ollama endpoint, read only when that host was selected.
--   ollamaCommandModel - the model Ollama served, read only when that host was selected.
--
-- A commander who was running Ollama keeps their LM Studio address (the default endpoint, unless they
-- had also configured LM Studio) and lands with no local model named, which is the truth: they have
-- to install LM Studio and load a model. SetupCheck says exactly that on the next start. The Ollama
-- model name is deliberately not carried over - it names a model pulled into a different runtime.

ALTER TABLE game_session
DROP
COLUMN ollamaAddress;

ALTER TABLE game_session
DROP
COLUMN ollamaCommandModel;

ALTER TABLE game_session
DROP
COLUMN localLlmProvider;
