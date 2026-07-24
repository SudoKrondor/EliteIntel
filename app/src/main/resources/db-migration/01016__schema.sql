-- Add the non-localized FDev organic symbol (journal CodexEntry 'Name', e.g.
-- "$Codex_Ent_Tussocks_02_F_Name;") to codex_entries so bio codex rows can be matched
-- to a genus/species by symbol instead of by localized display name (which breaks on
-- non-English game clients). Additive, nullable: existing rows keep entrySymbol NULL and
-- fall back to the legacy localized-name match until re-logged.
ALTER TABLE codex_entries
    ADD COLUMN entrySymbol TEXT;
