-- Per-voice tuning for the Supertonic cast (2026-10-28). The voices differ a little in loudness and pace, so each
-- one carries its own speed, a volume trim on top of the Voice and Radio sliders, and a six-band EQ.
--
-- One row per voice the commander has touched, keyed by the SupertonicVoices constant name (the stored identity
-- of a voice, which never changes). A voice with no row plays at the defaults below. Shared, not per commander:
-- the voices belong to this installation, like the other audio settings.
--
-- speed is the pace on the scale the old global slider used: 1 + speed/100 times the voice's own pace.
-- volume is a percentage, 100 = unchanged. eq1 to eq6 are gains in dB, eq1 and eq6 being the shelves.
--
-- The global speech speed now drives Google alone (each Supertonic voice has its own), so its column is named
-- for it, like googleWaveNetPitch.
--
-- NOTE: no semicolon may appear inside these comments. Migrations are split on a semicolon at end of
-- line before comments are stripped, so one here would cut the file mid-comment and hand SQLite a
-- statement with no SQL in it.
CREATE TABLE IF NOT EXISTS supertonic_voice_tuning
(
    voice  TEXT PRIMARY KEY,
    speed  INTEGER NOT NULL DEFAULT 20,
    volume INTEGER NOT NULL DEFAULT 100,
    eq1    INTEGER NOT NULL DEFAULT 0,
    eq2    INTEGER NOT NULL DEFAULT 0,
    eq3    INTEGER NOT NULL DEFAULT 0,
    eq4    INTEGER NOT NULL DEFAULT 0,
    eq5    INTEGER NOT NULL DEFAULT 0,
    eq6    INTEGER NOT NULL DEFAULT 0
);

ALTER TABLE game_session
    RENAME COLUMN speechSpeed TO googleSpeechSpeed;
