package elite.intel.ai.mouth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins which engine voices radio: Supertonic, for every game client, whatever the main mouth. It reads every
 * language the game ships in, so the client no longer decides the engine.
 */
class RadioVoicingTest {

    @Test
    void supertonicVoicesRadioForEveryClient() {
        assertEquals(TtsProvider.SUPERTONIC, RadioVoicing.engine());
        assertTrue(RadioVoicing.isRadioEngine(TtsProvider.SUPERTONIC));
        assertFalse(RadioVoicing.isRadioEngine(TtsProvider.KOKORO));
    }

    @Test
    void radioIsAlwaysLocalAndNeverBillsGoogle() {
        assertTrue(RadioVoicing.engine().isLocal(), "radio is chatter, not narration");
        assertFalse(RadioVoicing.isRadioEngine(TtsProvider.GOOGLE));
    }
}
