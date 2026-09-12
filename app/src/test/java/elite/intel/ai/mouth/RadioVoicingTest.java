package elite.intel.ai.mouth;

import elite.intel.i18n.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins which engine voices radio. The transmission is the game client's prose, so the engine follows the
 * language the client writes in - never the commander's own language and never the main mouth: Kokoro
 * wherever it can pronounce the script, Supertonic for the Cyrillic it cannot read.
 */
class RadioVoicingTest {

    @Test
    void aRussianClientIsVoicedBySupertonicAndEveryOtherByKokoro() {
        for (Language language : Language.values()) {
            assertEquals(
                    language.isCyrillicScript() ? TtsProvider.SUPERTONIC : TtsProvider.KOKORO,
                    RadioVoicing.engineFor(language),
                    "radio engine for a client writing " + language);
        }
    }

    @Test
    void radioIsAlwaysLocalAndNeverBillsGoogle() {
        for (Language language : Language.values()) {
            TtsProvider radio = RadioVoicing.engineFor(language);
            assertTrue(radio.isLocal(), "radio is chatter, not narration: " + language);
            assertNotEquals(TtsProvider.GOOGLE, radio);
            assertNotEquals(TtsProvider.EDGE, radio);
        }
    }

    /**
     * The one script Kokoro cannot read is the one Supertonic takes, and nothing else moves off Kokoro: its
     * cast is the wider one, and the channel lives on variety.
     */
    @Test
    void supertonicTakesOnlyWhatKokoroCannotPronounce() {
        for (Language language : Language.values()) {
            assertEquals(TtsProvider.KOKORO.canVoice(language), RadioVoicing.engineFor(language) == TtsProvider.KOKORO,
                    language.toString());
        }
    }
}
