-- Names the cloud language-model provider outright instead of deducing it from the shape of the stored key.
--
-- Providers change their key formats without notice (Mistral's mstrl_ keys, Gemini's AQ. keys), and every
-- change broke detection for the commanders holding the new shape. The commander now picks the provider on the
-- AI services tab and this column carries the choice. The key column is for keys.
--
-- NULL means not selected. An existing install is filled in on its next start by LlmProviderUpgrade, in Java,
-- because the key is encrypted and only the retired key patterns can name its provider. A key none of them
-- recognises stays NULL, and the setup check asks the commander to pick a provider.
--
-- NOTE: no semicolon may appear inside these comments. Migrations are split on a semicolon at end of
-- line before comments are stripped, so one here would cut the file mid-comment and hand SQLite a
-- statement with no SQL in it.
ALTER TABLE game_session
    ADD COLUMN llmProvider TEXT;
