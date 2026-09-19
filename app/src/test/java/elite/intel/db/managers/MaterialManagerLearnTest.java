package elite.intel.db.managers;

import elite.intel.db.FuzzySearch;
import elite.intel.db.dao.MaterialNameDao;
import elite.intel.db.util.Database;
import elite.intel.gameapi.GameLanguage;
import elite.intel.gameapi.search.edsm.dto.MaterialsType;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.util.Cypher;
import org.jdbi.v3.core.Handle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A material the seed does not know is learned from the journal in the language the game client wrote
 * it in, and in that language only. Its count is kept whatever is known about its name, and its storage
 * cap - which the journal never states - is reported as unknown rather than as zero.
 */
class MaterialManagerLearnTest {

    private static final String NEW_SYMBOL = "rhodiumdust";
    private static final MaterialsType RAW = MaterialsType.GAME_RAW;

    @BeforeAll
    static void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
    }

    @AfterEach
    void forgetTheLearnedMaterial() {
        MaterialManager.getInstance().clear();
        try (Handle handle = Database.init()) {
            handle.execute("DELETE FROM material_names WHERE symbol IN (?, ?)", NEW_SYMBOL, "rhodium");
        }
        gameClient("English/UK");
    }

    @Test
    void aGermanClientTeachesTheGermanWordOnly() {
        gameClient("German/DE");
        MaterialManager.getInstance().collect(NEW_SYMBOL, RAW, 3, "Rhodiumstaub");

        MaterialNameDao.Material learned = MaterialManager.getInstance().find(NEW_SYMBOL);
        assertEquals(3, learned.getAmount(), "the count is kept whatever is known about the name");
        assertNull(learned.getName(), "no English client has named it, so there is no English name");
        assertEquals(0, learned.getMaxCapacity(), "the journal never states a grade, so the cap is unknown");
        withLanguage(Language.DE, () -> {
            assertEquals("Rhodiumstaub", FuzzySearch.localizedMaterialName(NEW_SYMBOL));
            assertEquals(NEW_SYMBOL, FuzzySearch.fuzzyMaterialSymbol("Rhodiumstaub", 8));
        });
    }

    @Test
    void theWordOnTheHudIsSpokenAndUnderstoodWhateverLanguageTheCommanderSpeaks() {
        gameClient("German/DE");
        MaterialManager.getInstance().collect(NEW_SYMBOL, RAW, 3, "Rhodiumstaub");

        withLanguage(Language.EN, () -> {
            assertEquals("Rhodiumstaub", FuzzySearch.localizedMaterialName(NEW_SYMBOL),
                    "with no English name, the word the client shows beats 'unknown material'");
            assertEquals(NEW_SYMBOL, FuzzySearch.fuzzyMaterialSymbol("Rhodiumstaub", 8));
        });
        withLanguage(Language.UK, () ->
                assertEquals("Rhodiumstaub", FuzzySearch.localizedMaterialName(NEW_SYMBOL),
                        "a Ukrainian speaker on a German client hears the German word, as their HUD shows it"));
    }

    @Test
    void anEnglishClientSuppliesTheEnglishNameAndKeepsTheGerman() {
        gameClient("German/DE");
        MaterialManager.getInstance().collect(NEW_SYMBOL, RAW, 3, "Rhodiumstaub");
        gameClient("English/UK");
        MaterialManager.getInstance().collect(NEW_SYMBOL, RAW, 1, "Rhodium Dust");

        MaterialNameDao.Material learned = MaterialManager.getInstance().find(NEW_SYMBOL);
        assertEquals("Rhodium Dust", learned.getName());
        assertEquals(4, learned.getAmount());
        withLanguage(Language.EN, () -> assertEquals(NEW_SYMBOL, FuzzySearch.fuzzyMaterialSymbol("rhodium dust", 8)));
        withLanguage(Language.DE, () -> assertEquals("Rhodiumstaub", FuzzySearch.localizedMaterialName(NEW_SYMBOL)));
    }

    @Test
    void anEnglishClientOmittingTheDisplayNameMeansTheCapitalisedSymbolIsTheName() {
        // Frontier omits Name_Localised only when it would equal the symbol, which is the norm for raw elements.
        gameClient("English/UK");
        MaterialManager.getInstance().collect("rhodium", RAW, 2, null);

        assertEquals("Rhodium", MaterialManager.getInstance().find("rhodium").getName());
    }

    @Test
    void aSightingNeverOverwritesACuratedTranslation() {
        gameClient("German/DE");
        MaterialManager.getInstance().collect("iron", RAW, 1, "Eisenstaub");

        withLanguage(Language.DE, () -> assertEquals("Eisen", FuzzySearch.localizedMaterialName("iron")));
        assertEquals("Iron", MaterialManager.getInstance().find("iron").getName());
    }

    @Test
    void anUnknownClientLanguageKeepsTheCountButFilesNoName() {
        gameClient("Klingon/KL");
        MaterialManager.getInstance().collect(NEW_SYMBOL, RAW, 3, "Rhodiumstaub");

        MaterialNameDao.Material learned = MaterialManager.getInstance().find(NEW_SYMBOL);
        assertEquals(3, learned.getAmount());
        assertNull(learned.getName());
        withLanguage(Language.DE, () ->
                assertNull(FuzzySearch.fuzzyMaterialSymbol("Rhodiumstaub", 8),
                        "a name is never filed under a language it may not belong to"));
    }

    @Test
    void theStartupRebuildLearnsBeforeItWritesTheCounts() {
        gameClient("German/DE");
        MaterialManager.getInstance().replaceAll(List.of(
                new MaterialManager.Holding(NEW_SYMBOL, RAW, 5, "Rhodiumstaub"),
                new MaterialManager.Holding("iron", RAW, 9, null)));

        assertEquals(5, MaterialManager.getInstance().find(NEW_SYMBOL).getAmount());
        assertEquals(9, MaterialManager.getInstance().find("iron").getAmount());
        withLanguage(Language.DE, () -> assertEquals("Rhodiumstaub", FuzzySearch.localizedMaterialName(NEW_SYMBOL)));
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
