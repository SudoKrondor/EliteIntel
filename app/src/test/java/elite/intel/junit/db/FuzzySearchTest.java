package elite.intel.junit.db;

import elite.intel.db.FuzzySearch;
import elite.intel.db.managers.MaterialManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FuzzySearchTest {

    // material_names is migration-seeded reference data covering all 147 materials, so nothing needs
    // seeding here — only the held amounts belong to the commander, and those are reset afterwards.
    @AfterEach
    void clearAmounts() {
        MaterialManager.getInstance().clear();
    }

    // ── levenshteinDistance ────────────────────────────────────────────────

    @Test
    void levenshteinExactMatchIsZero() {
        assertEquals(0, FuzzySearch.levenshteinDistance("carbon", "carbon"));
    }

    @Test
    void levenshteinOneSubstitution() {
        // "cardon" differs from "carbon" by one char swap
        assertEquals(1, FuzzySearch.levenshteinDistance("carbon", "cardon"));
    }

    @Test
    void levenshteinOneDeletion() {
        assertEquals(1, FuzzySearch.levenshteinDistance("carbon", "carbn"));
    }

    @Test
    void levenshteinOneInsertion() {
        assertEquals(1, FuzzySearch.levenshteinDistance("iron", "irons"));
    }

    @Test
    void levenshteinEmptyVsWord() {
        assertEquals(4, FuzzySearch.levenshteinDistance("", "iron"));
    }

    // ── fuzzyMaterialNameSearch (material_names is migration-seeded ref data) ──

    @Test
    void materialNameSearchExactInputReturnsCanonicalCase() {
        assertEquals("Carbon", FuzzySearch.fuzzyMaterialNameSearch("carbon", 8));
    }

    @Test
    void materialNameSearchPrefixReturnsShortestMatch() {
        // "iro" is an unambiguous prefix of "Iron" only
        assertEquals("Iron", FuzzySearch.fuzzyMaterialNameSearch("iro", 8));
    }

    @Test
    void materialNameSearchOneTypoStillMatches() {
        // "carbn" — one deletion from "carbon"; Levenshtein distance = 1
        assertEquals("Carbon", FuzzySearch.fuzzyMaterialNameSearch("carbn", 8));
    }

    @Test
    void materialNameSearchTotallyUnknownReturnsNull() {
        assertNull(FuzzySearch.fuzzyMaterialNameSearch("xxxxxxxxxx", 2));
    }

    // ── fuzzyCommodityMatch (commodities is migration-seeded ref data) ─────

    @Test
    void commodityMatchExactInputReturnsCanonicalCase() {
        assertEquals("Gold", FuzzySearch.fuzzyCommodityMatch("gold", 3));
    }

    @Test
    void commodityMatchPrefixReturnsShortest() {
        // "trit" is an unambiguous prefix of "Tritium"
        assertEquals("Tritium", FuzzySearch.fuzzyCommodityMatch("trit", 3));
    }

    @Test
    void commodityMatchOneTypoStillMatches() {
        // "platnum" → "Platinum": insert 'i' → distance 1
        assertEquals("Platinum", FuzzySearch.fuzzyCommodityMatch("platnum", 3));
    }

    @Test
    void commodityMatchUnknownReturnsNull() {
        assertNull(FuzzySearch.fuzzyCommodityMatch("xxxxxxxxxx", 3));
    }

    // ── commoditySymbol (non-localized FDevIDs symbol for cargo matching) ──

    @Test
    void commoditySymbolReturnsCamelCaseSymbolForMultiWordName() {
        // Cargo Inventory 'Name' is the symbol, so a multi-word display name must resolve to
        // the single-token symbol — this is the case the old display-name match got wrong.
        assertEquals("AtmosphericExtractors", FuzzySearch.commoditySymbol("Atmospheric Processors"));
    }

    @Test
    void commoditySymbolMatchesLowerCasedJournalNameCaseInsensitively() {
        // The journal writes the symbol lower-cased ("atmosphericextractors"); matching must
        // be case-insensitive against the CamelCase DB symbol.
        String symbol = FuzzySearch.commoditySymbol("Water Purifiers");
        assertEquals("WaterPurifiers", symbol);
        org.junit.jupiter.api.Assertions.assertTrue("waterpurifiers".equalsIgnoreCase(symbol));
    }

    @Test
    void commoditySymbolUnknownNameReturnsNull() {
        assertNull(FuzzySearch.commoditySymbol("Definitely Not A Commodity"));
    }

    // ── fuzzyMaterialSymbol (spoken name → the journal's non-localized Name) ──

    @Test
    void materialSymbolExactInputReturnsSymbol() {
        assertEquals("carbon", FuzzySearch.fuzzyMaterialSymbol("carbon", 8));
    }

    @Test
    void materialSymbolPrefixMatchesShortestCandidate() {
        // "iro" prefixes "Iron" but not "Iron ..." anything else
        assertEquals("iron", FuzzySearch.fuzzyMaterialSymbol("iro", 8));
    }

    @Test
    void materialSymbolOneTypoStillMatches() {
        // "nickl" → "Nickel": distance 1
        assertEquals("nickel", FuzzySearch.fuzzyMaterialSymbol("nickl", 8));
    }

    @Test
    void materialSymbolResolvesMultiWordDisplayNameToSingleTokenSymbol() {
        // This is the case the old display-name keying got wrong: the spoken words and the
        // journal's Name share no spelling at all.
        assertEquals("focuscrystals", FuzzySearch.fuzzyMaterialSymbol("focus crystals", 8));
        assertEquals("unknownenergysource", FuzzySearch.fuzzyMaterialSymbol("sensor fragment", 8));
    }

    @Test
    void materialSymbolCommodityIsNotAMaterialReturnsNull() {
        // "Gold" is a commodity, never an engineering material
        assertNull(FuzzySearch.fuzzyMaterialSymbol("gold", 8));
    }

    @Test
    void materialSymbolUnknownReturnsNull() {
        assertNull(FuzzySearch.fuzzyMaterialSymbol("xxxxxxxxxx", 2));
    }

    // ── aliases (spoken forms the game itself does not display) ───────────

    @Test
    void communityThargoidPrefixResolvesViaAlias() {
        // The game calls it "Weapon Parts"; EDDI, Inara and most commanders say "Thargoid Weapon
        // Parts". Levenshtein cannot bridge that on its own (distance 9, over the budget).
        assertEquals("tg_weaponparts", FuzzySearch.fuzzyMaterialSymbol("thargoid weapon parts", 8));
    }

    @Test
    void blueprintSegmentWordingResolvesToTheGamesFragment() {
        assertEquals("guardian_weaponblueprint",
                FuzzySearch.fuzzyMaterialSymbol("guardian weapon blueprint segment", 8));
    }

    // ── localizedMaterialName (what we speak back) ────────────────────────

    @Test
    void localizedNameInEnglishIsTheCanonicalName() {
        assertEquals("Focus Crystals", FuzzySearch.localizedMaterialName("focuscrystals"));
    }

    @Test
    void localizedNameStripsTheExportToolsDisambiguationSuffix() {
        // FDev's localization export and EDDI each append their own "(Guardian)"/"(Biological)"
        // marker; real journals show the game string carries neither.
        assertEquals("Pattern Alpha Obelisk Data",
                FuzzySearch.localizedMaterialName("ancientbiologicaldata"));
    }

    @Test
    void localizedNameDegradesToAReadablePhraseRatherThanTheRawSymbol() {
        // The result is spoken aloud, so an unregistered symbol must not surface the journal token.
        String spoken = FuzzySearch.localizedMaterialName("no_such_material_symbol");
        assertEquals("unknown material", spoken);
        assertFalse(spoken.contains("_"), "a raw journal symbol must never reach speech");
    }

    @Test
    void localizedNameHandlesMissingSymbol() {
        assertEquals("unknown material", FuzzySearch.localizedMaterialName(null));
        assertEquals("unknown material", FuzzySearch.localizedMaterialName("  "));
    }
}
