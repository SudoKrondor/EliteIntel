package elite.intel.ai.mouth.kokoro;

import elite.intel.ai.mouth.TtsProvider;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The other side of curating the cast: a commander who was already using a voice that has since been
 * removed has that name stored in the database, and gets the default rather than an exception.
 */
class KokoroVoicesTest {

    @Test
    void aVoiceThatHasLeftTheCastFallsBackToTheDefault() {
        assertEquals(KokoroVoices.DEFAULT_VOICE, KokoroVoices.voiceOrDefault("ZH_YUNYANG"));
        assertEquals(KokoroVoices.DEFAULT_VOICE, KokoroVoices.voiceOrDefault(null));
    }

    /**
     * Nicole whispers: a ship voice the commander may pick, never a stranger on the comms link.
     */
    @Test
    void aShipOnlyVoiceIsPickableForAShipButNeverSpeaksOverTheRadio() {
        assertEquals(KokoroVoices.NICOLE, KokoroVoices.voiceOrDefault("NICOLE"));
        assertTrue(TtsProvider.KOKORO.voiceRoster().anyMatch("NICOLE"::equals), "offered as a ship voice");

        assertFalse(Arrays.asList(KokoroVoices.radioCast()).contains(KokoroVoices.NICOLE));
        assertFalse(TtsProvider.KOKORO.radioVoiceRoster().anyMatch("NICOLE"::equals), "not offered to a carrier");
        for (int i = 0; i < 500; i++) {
            assertNotEquals(KokoroVoices.NICOLE, KokoroVoices.randomRadioVoice(null, Set.of()));
        }
        assertNotEquals(KokoroVoices.NICOLE, KokoroVoices.radioVoiceFor("Dave Knowles", null, Set.of()));
    }
}
