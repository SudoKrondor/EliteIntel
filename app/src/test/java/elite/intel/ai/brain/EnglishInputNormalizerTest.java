package elite.intel.ai.brain;

import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Guards the ordered English HUD-mode synonym rules used by companion command matching. */
class EnglishInputNormalizerTest {

    private final InputNormalizer normalizer = InputNormalizer.getInstance();
    private Language previousLanguage;

    @BeforeEach
    void useEnglish() {
        previousLanguage = SystemSession.getInstance().getLanguage();
        SystemSession.getInstance().setLanguage(Language.EN);
    }

    @AfterEach
    void restoreLanguage() {
        SystemSession.getInstance().setLanguage(previousLanguage);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "combat mode, switch to combat mode",
            "change to combat mode, switch to combat mode",
            "swap to combat mode, switch to combat mode",
            "enter combat mode, switch to combat mode",
            "Analysis mode, switch to analysis mode",
            "Change to analysis mode, switch to analysis mode",
            "swap to analysis mode, switch to analysis mode",
            "enter analysis mode, switch to analysis mode"
    })
    void normalizesSpecificHudModePhrasesBeforeTheirShorterSubstrings(String input, String expected) {
        assertEquals(expected, normalizer.normalize(input));
    }
}
