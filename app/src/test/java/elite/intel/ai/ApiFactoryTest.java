package elite.intel.ai;

import elite.intel.ai.mouth.edge.EdgeTTSImpl;
import elite.intel.ai.mouth.google.GoogleTTSImpl;
import elite.intel.ai.mouth.kokoro.KokoroTTS;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class ApiFactoryTest {
    @Test
    void cloudProviderSelectsTheMatchingMouth() {
        assertSame(EdgeTTSImpl.getInstance(), ApiFactory.selectMouth(false, ProviderEnum.EDGE_TTS));
        assertSame(GoogleTTSImpl.getInstance(), ApiFactory.selectMouth(false, ProviderEnum.GOOGLE_TTS));
    }

    @Test
    void localAndUnrecognizedConfigurationsPreserveKokoroFallback() {
        assertSame(KokoroTTS.getInstance(), ApiFactory.selectMouth(true, ProviderEnum.EDGE_TTS));
        assertSame(KokoroTTS.getInstance(), ApiFactory.selectMouth(false, ProviderEnum.UNKNOWN));
    }
}
