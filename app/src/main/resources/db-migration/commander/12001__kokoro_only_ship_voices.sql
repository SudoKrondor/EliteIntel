-- Kokoro is gone (2026-10-28, see the shared migration 12015). Supertonic's cast carries the Kokoro voices under
-- the same names, so almost every ship keeps its voice as stored.
--
-- Three Kokoro voices were never fitted for Supertonic: ES_DORA, IT_SARA and PT_DORA. Supertonic already reads
-- a name it does not carry as its default voice, FR_SIWIS, which is female like all three. This makes the row
-- say so, so the fleet grid and the support bundle show the voice that is actually speaking.
--
-- No Google voice carries these names, so a Google commander's ships are untouched.

UPDATE ship
SET voice = 'FR_SIWIS'
WHERE voice IN ('ES_DORA', 'IT_SARA', 'PT_DORA');
