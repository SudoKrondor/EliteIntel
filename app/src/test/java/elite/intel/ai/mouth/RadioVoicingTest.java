package elite.intel.ai.mouth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins which engine voices radio. The transmission is the game client's prose, so the engine follows the
 * client - never the commander's own language and never the main mouth: Kokoro everywhere, except Supertonic
 * for a Russian client, whose Cyrillic Kokoro cannot read.
 */
class RadioVoicingTest {

    @Test
    void aRussianClientIsVoicedBySupertonicAndEveryOtherByKokoro() {
        assertEquals(TtsProvider.SUPERTONIC, RadioVoicing.engineFor(true));
        assertEquals(TtsProvider.KOKORO, RadioVoicing.engineFor(false));
    }

    @Test
    void radioIsAlwaysLocalAndNeverBillsGoogle() {
        for (boolean russianClient : new boolean[]{true, false}) {
            TtsProvider radio = RadioVoicing.engineFor(russianClient);
            assertTrue(radio.isLocal(), "radio is chatter, not narration");
            assertNotEquals(TtsProvider.GOOGLE, radio);
        }
    }
}
