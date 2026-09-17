-- Names learned from the commander's own journal.
--
-- A game update can add a material or commodity this build has never heard of. The journal names it
-- on every sighting (symbol plus Name_Localised in the client's language), so the row can be learned
-- on the spot instead of waiting for a curated migration. The learned display name goes into the
-- column of the GAME CLIENT'S language only - never guessed into any other.
--
-- The English column is NOT NULL UNIQUE on both tables, so a row learned on a non-English client
-- cannot leave it empty. It holds the bare symbol as a stand-in and this flag says so, so no reader
-- ever speaks the symbol or hands it to Spansh as a trade name. An English client clears the flag
-- by supplying the real name. A later curated migration overwrites the row BY SYMBOL and clears the
-- flag - it must never insert by name, or a learned row and the curated one would coexist.
ALTER TABLE material_names
    ADD COLUMN english_pending INTEGER NOT NULL DEFAULT 0;

ALTER TABLE commodities
    ADD COLUMN english_pending INTEGER NOT NULL DEFAULT 0;
