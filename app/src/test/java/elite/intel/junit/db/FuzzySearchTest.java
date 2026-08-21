package elite.intel.junit.db;

import elite.intel.db.FuzzySearch;
import elite.intel.db.managers.MaterialManager;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
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

    // ── commodity symbols (commodities is migration-seeded ref data) ──

    @Test
    void commoditySymbolResolvesToTheEnglishNameSpanshMatches() {
        // The mission journal writes "$HazardousEnvironmentSuits_Name;"; Spansh only matches "H.E. Suits",
        // and the commander's game may be running in any of six languages, so the symbol is the only bridge.
        assertEquals("H.E. Suits", FuzzySearch.commodityNameForSymbol("hazardousenvironmentsuits"));
        assertEquals("Haematite", FuzzySearch.commodityNameForSymbol("Haematite"));
    }

    @Test
    void commoditySymbolLookupRoundTrips() {
        String symbol = FuzzySearch.commoditySymbol("H.E. Suits");
        assertEquals("H.E. Suits", FuzzySearch.commodityNameForSymbol(symbol));
    }

    @Test
    void unknownCommoditySymbolIsNull() {
        assertNull(FuzzySearch.commodityNameForSymbol("notacommodity"));
        assertNull(FuzzySearch.commodityNameForSymbol(null));
        assertNull(FuzzySearch.commodityNameForSymbol("  "));
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

    // ── Portuguese commodity names (01018 section 2 adds the columns, section 5 fills them) ──

    @Test
    void commodityMatchResolvesABrazilianPortugueseNameToEnglish() {
        withLanguage(Language.PTBZ, () ->
                assertEquals("Advanced Medicines", FuzzySearch.fuzzyCommodityMatch("remédios avançados", 3)));
    }

    @Test
    void localizedCommodityNameSpeaksBrazilianPortuguese() {
        withLanguage(Language.PTBZ, () ->
                assertEquals("Tratamento Agronômico", FuzzySearch.localizedCommodityName("Agronomic Treatment")));
    }

    @Test
    void europeanPortugueseAlsoGetsAPortugueseCommodityName() {
        withLanguage(Language.PT, () ->
                assertEquals("Tratamento Agronômico", FuzzySearch.localizedCommodityName("Agronomic Treatment")));
    }

    @Test
    void untranslatedCommodityFallsBackToEnglishRatherThanGoingBlank() {
        // Not every row has an upstream translation; the DAO COALESCEs so speech stays intelligible.
        withLanguage(Language.PTBZ, () ->
                assertEquals("Chemicals", FuzzySearch.localizedCommodityName("Chemicals")));
    }

    @Test
    void ukrainianCommoditiesStillResolveViaTheEnglishFallback() {
        // commodity_uk has no data at all — the upstream file carries no Ukrainian column.
        withLanguage(Language.UK, () ->
                assertEquals("Gold", FuzzySearch.fuzzyCommodityMatch("gold", 3)));
    }

    @Test
    void commodityMatchStillAcceptsEnglishWhileAnotherLanguageIsActive() {
        // Same guard as the subsystem case: commodity_ptbz for Gold is "Ouro", so "gold" is absent
        // from the Portuguese candidate list and COALESCE does not rescue it. Adding the PT/PTBZ
        // columns would have silently cost these commanders English commodity names without the
        // English retry in fuzzyCommodityMatch.
        withLanguage(Language.PTBZ, () ->
                assertEquals("Gold", FuzzySearch.fuzzyCommodityMatch("gold", 3)));
        withLanguage(Language.DE, () ->
                assertEquals("Gold", FuzzySearch.fuzzyCommodityMatch("gold", 3)));
    }

    // ── fuzzySubSystemSearch (sub_system is migration-seeded ref data) ──
    //
    // Targeting is keyed on the canonical English name, which resolves to the journal machine key.
    // A localized utterance must therefore come back out as English, or SubSystemsManager cannot
    // look up a machine key and refuses to start cycling.

    @Test
    void subSystemSearchResolvesEnglishToCanonicalCase() {
        withLanguage(Language.EN, () ->
                assertEquals("Power Plant", FuzzySearch.fuzzySubSystemSearch("power plant", 4)));
    }

    @Test
    void subSystemSearchResolvesASpanishNameToTheEnglishCanonicalName() {
        // "Núcleo de Energía" is the label_es value seeded for Power Plant.
        withLanguage(Language.ES, () ->
                assertEquals("Power Plant", FuzzySearch.fuzzySubSystemSearch("núcleo de energía", 4)));
    }

    @Test
    void subSystemSearchResolvesARussianNameToTheEnglishCanonicalName() {
        withLanguage(Language.RU, () ->
                assertEquals("Power Plant", FuzzySearch.fuzzySubSystemSearch("силовая установка", 4)));
    }

    @Test
    void subSystemSearchStillAcceptsEnglishWhileAnotherLanguageIsActive() {
        // Guards the English retry specifically, NOT the DAO's COALESCE. label_es for Power Plant
        // is populated, so COALESCE yields "Núcleo de Energía" and "power plant" is absent from the
        // Spanish candidate list; this passes only because fuzzySubSystemSearch retries in English.
        // Delete that retry and this test fails.
        withLanguage(Language.ES, () ->
                assertEquals("Power Plant", FuzzySearch.fuzzySubSystemSearch("power plant", 4)));
    }

    @Test
    void subSystemSearchResolvesABrazilianPortugueseName() {
        // 01018 section 4 copies the Brazilian labels from label_pt into label_ptbz. PTBZ is the
        // locale that needs them: a pt-BR client shows translated module names, so that is what
        // gets spoken.
        withLanguage(Language.PTBZ, () ->
                assertEquals("Power Plant", FuzzySearch.fuzzySubSystemSearch("gerador de energia", 4)));
    }

    @Test
    void brazilianAndEuropeanPortugueseBothResolveAfterTheCopy() {
        // The copy left label_pt in place, so PT did not regress to English-only.
        withLanguage(Language.PT, () ->
                assertEquals("Power Plant", FuzzySearch.fuzzySubSystemSearch("gerador de energia", 4)));
        withLanguage(Language.PTBZ, () ->
                assertEquals("Gerador de Energia", FuzzySearch.localizedSubSystemName("Power Plant")));
    }

    @Test
    void subSystemSearchFallsBackToEnglishForALanguageWithNoLabels() {
        // label_de is added but unpopulated; a German commander must still be able to target.
        withLanguage(Language.DE, () ->
                assertEquals("Power Plant", FuzzySearch.fuzzySubSystemSearch("power plant", 4)));
    }

    @Test
    void localizedSubSystemNameSpeaksTheCommandersWording() {
        withLanguage(Language.ES, () ->
                assertEquals("Núcleo de Energía", FuzzySearch.localizedSubSystemName("Power Plant")));
    }

    @Test
    void localizedSubSystemNameFallsBackToEnglishWhenUntranslated() {
        withLanguage(Language.DE, () ->
                assertEquals("Power Plant", FuzzySearch.localizedSubSystemName("Power Plant")));
    }

    private static void withLanguage(Language language, Runnable body) {
        SystemSession session = SystemSession.getInstance();
        Language previous = session.getLanguage();
        session.setLanguage(language);
        try {
            body.run();
        } finally {
            session.setLanguage(previous);
        }
    }
}
