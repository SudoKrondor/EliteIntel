package elite.intel.db.managers;

import elite.intel.db.FuzzySearch;
import elite.intel.db.util.Database;
import elite.intel.gameapi.GameLanguage;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.util.Cypher;
import org.jdbi.v3.core.Handle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A good the seed does not know is learned from the journal in the language the game client wrote it in,
 * and in that language only. The English name - the one Spansh matches - arrives from an English client
 * or a curated migration, never from a guess.
 */
class CommodityCatalogueTest {

    /**
     * A refinery ore no seed knows, in every spelling the journal uses for the same symbol.
     */
    private static final String MARKET_NAME = "$rhodiumore_name;";
    private static final String CARGO_NAME = "rhodiumore";
    private static final String PROSPECTOR_NAME = "RhodiumOre";

    @BeforeAll
    static void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
    }

    @AfterEach
    void forgetTheLearnedGood() {
        try (Handle handle = Database.init()) {
            handle.execute("DELETE FROM commodities WHERE LOWER(symbol) = LOWER(?)", CARGO_NAME);
            handle.execute("UPDATE commodities SET commodity_fr = 'Or' WHERE commodity = 'Gold'");
        }
        gameClient("English/UK");
    }

    @Test
    void aGermanClientTeachesTheGermanWordAndNoTradeName() {
        gameClient("German/DE");
        CommodityCatalogue.getInstance().learn(MARKET_NAME, "Rhodiumerz");

        withLanguage(Language.DE, () -> {
            String canonical = FuzzySearch.fuzzyCommodityMatch("Rhodiumerz", 3);
            assertEquals(CARGO_NAME, canonical, "the symbol stands in for the English name until one is known");
            assertEquals("Rhodiumerz", FuzzySearch.localizedCommodityName(canonical));
            assertEquals("Rhodiumerz", FuzzySearch.localizedCommodityNameForSymbol(PROSPECTOR_NAME),
                    "the prospector's spelling of the symbol resolves to the same row");
            assertEquals(CARGO_NAME, FuzzySearch.commoditySymbol(canonical));
            assertFalse(FuzzySearch.hasTradeName(canonical), "no English name, so nothing for Spansh to match");
            assertNull(FuzzySearch.commodityNameForSymbol(CARGO_NAME), "the stand-in is never handed out as English");
        });
    }

    @Test
    void theWordOnTheHudIsUnderstoodWhateverLanguageTheCommanderSpeaks() {
        gameClient("German/DE");
        CommodityCatalogue.getInstance().learn(MARKET_NAME, "Rhodiumerz");

        withLanguage(Language.EN, () -> {
            assertEquals(CARGO_NAME, FuzzySearch.fuzzyCommodityMatch("Rhodiumerz", 3));
            assertEquals("Rhodiumerz", FuzzySearch.localizedCommodityName(CARGO_NAME),
                    "with no English name, the German word the client shows is the one to speak");
        });
        withLanguage(Language.UK, () ->
                assertEquals(CARGO_NAME, FuzzySearch.fuzzyCommodityMatch("Rhodiumerz", 3)));
    }

    @Test
    void anEnglishClientSuppliesTheTradeNameAndKeepsTheGerman() {
        gameClient("German/DE");
        CommodityCatalogue.getInstance().learn(MARKET_NAME, "Rhodiumerz");
        gameClient("English/UK");
        CommodityCatalogue.getInstance().learn(CARGO_NAME, "Rhodium Ore");

        withLanguage(Language.EN, () -> {
            assertEquals("Rhodium Ore", FuzzySearch.fuzzyCommodityMatch("rhodium ore", 3));
            assertTrue(FuzzySearch.hasTradeName("Rhodium Ore"));
            assertEquals("Rhodium Ore", FuzzySearch.commodityNameForSymbol(PROSPECTOR_NAME));
        });
        withLanguage(Language.DE, () ->
                assertEquals("Rhodiumerz", FuzzySearch.localizedCommodityName("Rhodium Ore")));
    }

    @Test
    void aSightingNeverOverwritesACuratedNameOrDuplicatesASeededRow() {
        gameClient("French/FR");
        CommodityCatalogue.getInstance().learn("$gold_name;", "Or fin");

        withLanguage(Language.FR, () ->
                assertEquals("Or", FuzzySearch.localizedCommodityName("Gold"), "the seed's translation stands"));
        assertEquals("Gold", FuzzySearch.commodityNameForSymbol("gold"));
        try (Handle handle = Database.init()) {
            int rows = handle.createQuery("SELECT COUNT(*) FROM commodities WHERE LOWER(symbol) = 'gold'")
                    .mapTo(Integer.class).one();
            assertEquals(1, rows, "the seeded row must not gain a lower-case twin");
        }
    }

    @Test
    void anUnknownClientLanguageKeepsTheGoodButFilesNoName() {
        gameClient("Klingon/KL");
        CommodityCatalogue.getInstance().learn(MARKET_NAME, "Rhodiumerz");

        assertEquals(CARGO_NAME, FuzzySearch.commoditySymbol(CARGO_NAME), "the row exists, keyed by symbol");
        assertFalse(FuzzySearch.hasTradeName(CARGO_NAME));
        withLanguage(Language.DE, () ->
                assertNull(FuzzySearch.fuzzyCommodityMatch("Rhodiumerz", 3),
                        "a name is never filed under a language it may not belong to"));
    }

    @Test
    void aWholeMarketBoardIsLearnedInOneVisit() {
        gameClient("English/UK");
        CommodityCatalogue.getInstance().learnAll(List.of(
                new CommodityCatalogue.Sighting("$gold_name;", "Gold"),
                new CommodityCatalogue.Sighting(MARKET_NAME, "Rhodium Ore"),
                new CommodityCatalogue.Sighting("$tritium_name;", "Tritium")));

        assertEquals("Rhodium Ore", FuzzySearch.commodityNameForSymbol(CARGO_NAME));
        assertEquals("Gold", FuzzySearch.commodityNameForSymbol("gold"));
    }

    private static void gameClient(String headerLanguage) {
        GameLanguage.getInstance().onGameSessionStarted(headerLanguage);
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
