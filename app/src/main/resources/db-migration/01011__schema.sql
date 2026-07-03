-- App-global TTS voice, one per provider (V1.1). Dedicated columns rather than reusing the legacy
-- aiVoice/aiCadence fields, which are retired and dropped in the pre-release cleanup script.
-- Additive-only: V1.0 shares this DB and simply ignores the new columns.
alter table game_session
    add column kokoroVoice text;
alter table game_session
    add column googleVoice text;
