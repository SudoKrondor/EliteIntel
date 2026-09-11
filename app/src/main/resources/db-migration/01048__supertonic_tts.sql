-- Kokoro -> Supertonic-3 local TTS engine migration.
--
-- The stored TTS provider selection and the per-ship/per-carrier voice names are both engine-specific
-- strings, and Supertonic's voice cast (SupertonicVoices: M1-M5, F1-F5) shares no names with Kokoro's
-- 53-voice cast, so any stored Kokoro voice name is meaningless to the new engine. Rather than leave
-- ships pinned to voices that no longer resolve (falling back silently to the default at read time -
-- see SystemSession/CommanderTabPanel), this migration clears them explicitly so every ship reverts
-- to the new engine's default voice, which the commander can then repick if they want variety.
alter table game_session
    rename column kokoroVoice to supertonicVoice;

update game_session
set supertonicVoice = null
where supertonicVoice is not null;

update game_session
set ttsProvider = 'SUPERTONIC'
where ttsProvider = 'KOKORO';

-- voice is NOT NULL, so ships revert straight to the new engine's default voice name
-- rather than to null (which the column has never allowed).
update ship
set voice = 'F1'
where voice is not null;
