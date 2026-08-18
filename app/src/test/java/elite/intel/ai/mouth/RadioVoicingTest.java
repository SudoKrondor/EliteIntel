package elite.intel.ai.mouth;

import elite.intel.i18n.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Pins which engine voices radio. Kokoro's phonemizer has no Cyrillic front end, so a Russian or Ukrainian
 * commander would hear either silence or gibberish from it; Edge is keyless and speaks both, so it takes the
 * channel there and nowhere else.
 */
class RadioVoicingTest {

    @Test
    void cyrillicLanguagesAreVoicedByEdgeAndEveryOtherLanguageByKokoro() {
        for (Language language : Language.values()) {
            assertEquals(
                    language.isCyrillicScript() ? TtsProvider.EDGE : TtsProvider.KOKORO,
                    RadioVoicing.engineFor(language),
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
