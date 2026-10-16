package elite.intel.ai;

import elite.intel.ai.mouth.TtsProvider;
import elite.intel.ai.mouth.google.GoogleTTSImpl;
import elite.intel.ai.mouth.kokoro.KokoroTTS;
import elite.intel.ai.mouth.supertonic.SupertonicTTS;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class ApiFactoryTest {

    private static final String GOOGLE_KEY = "AIzaSy123456789012345678901234567890123";

    @Test
    void theStoredProviderSelectsTheMouth() {
        assertSame(GoogleTTSImpl.getInstance(), ApiFactory.selectMouth(TtsProvider.GOOGLE, GOOGLE_KEY));
        assertSame(KokoroTTS.getInstance(), ApiFactory.selectMouth(TtsProvider.KOKORO, GOOGLE_KEY));
        assertSame(SupertonicTTS.getInstance(), ApiFactory.selectMouth(TtsProvider.SUPERTONIC, GOOGLE_KEY));
    }

    /**
     * Google without a key would start into silence, so the local engine stands in.
     */
    @Test
    void googleWithoutAKeyFallsBackToKokoro() {
        assertSame(KokoroTTS.getInstance(), ApiFactory.selectMouth(TtsProvider.GOOGLE, null));
        assertSame(KokoroTTS.getInstance(), ApiFactory.selectMouth(TtsProvider.GOOGLE, ""));
        assertSame(KokoroTTS.getInstance(), ApiFactory.selectMouth(TtsProvider.GOOGLE, "   "));
    }

    /**
     * Whether Google has a key is a question of presence, never of format: a key shape Google introduces must
     * not read as "no key" and quietly hand the commander's voice to Kokoro.
     */
    @Test
    void aGoogleKeyInAnUnfamiliarFormatStillSelectsGoogle() {
        assertSame(GoogleTTSImpl.getInstance(),
                ApiFactory.selectMouth(TtsProvider.GOOGLE, "AQ.a-new-google-key-shape"));
    }
}
