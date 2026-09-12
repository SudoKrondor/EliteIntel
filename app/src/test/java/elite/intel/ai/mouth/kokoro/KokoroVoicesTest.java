package elite.intel.ai.mouth.kokoro;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
