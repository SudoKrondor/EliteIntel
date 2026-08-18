package elite.intel.ai;

import elite.intel.ai.mouth.TtsProvider;
import elite.intel.ai.mouth.edge.EdgeTTSImpl;
import elite.intel.ai.mouth.google.GoogleTTSImpl;
import elite.intel.ai.mouth.kokoro.KokoroTTS;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class ApiFactoryTest {

    private static final String GOOGLE_KEY = "AIzaSy123456789012345678901234567890123";

    @Test
    void theStoredProviderSelectsTheMouth() {
        assertSame(EdgeTTSImpl.getInstance(), ApiFactory.selectMouth(TtsProvider.EDGE, null));
        assertSame(GoogleTTSImpl.getInstance(), ApiFactory.selectMouth(TtsProvider.GOOGLE, GOOGLE_KEY));
        assertSame(KokoroTTS.getInstance(), ApiFactory.selectMouth(TtsProvider.KOKORO, GOOGLE_KEY));
    }

    /**
     * Edge is keyless, so a stored Google key must not pull the selection away from it.
     */
    @Test
    void edgeIsUnaffectedByAStoredGoogleKey() {
        assertSame(EdgeTTSImpl.getInstance(), ApiFactory.selectMouth(TtsProvider.EDGE, GOOGLE_KEY));
    }

    /**
     * Google without a usable key would start into silence, so the local engine stands in.
     */
    @Test
    void googleWithoutAUsableKeyFallsBackToKokoro() {
        assertSame(KokoroTTS.getInstance(), ApiFactory.selectMouth(TtsProvider.GOOGLE, null));
        assertSame(KokoroTTS.getInstance(), ApiFactory.selectMouth(TtsProvider.GOOGLE, ""));
        assertSame(KokoroTTS.getInstance(), ApiFactory.selectMouth(TtsProvider.GOOGLE, "not-a-key"));
    }
}
