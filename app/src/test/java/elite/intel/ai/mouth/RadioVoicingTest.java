package elite.intel.ai.mouth;

import elite.intel.i18n.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Pins which engine voices radio. Supertonic is a single multilingual model that voices every language this
 * app ships, Cyrillic included, so it owns the radio channel everywhere - no language ever routes to Edge.
 */
class RadioVoicingTest {

    @Test
    void everyLanguageIsVoicedBySupertonic() {
        for (Language language : Language.values()) {
            assertEquals(TtsProvider.SUPERTONIC, RadioVoicing.engineFor(language),
                    "radio engine for " + language);
        }
    }

    @Test
    void googleIsNeverARadioEngine() {
        for (Language language : Language.values()) {
            assertNotEquals(TtsProvider.GOOGLE, RadioVoicing.engineFor(language),
                    "radio is chatter, not narration - it must never bill a Google key: " + language);
        }
    }
}
