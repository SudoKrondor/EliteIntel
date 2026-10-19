package elite.intel.ai.mouth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The stored engine setting: which engines are local, and what an unknown stored value reads as.
 */
class TtsProviderTest {

    /**
     * All offline providers are local; Google is the only cloud engine.
     */
    @Test
    void localAndCloudProvidersAreClassifiedCorrectly() {
        assertTrue(TtsProvider.KOKORO.isLocal());
        assertTrue(TtsProvider.SUPERTONIC.isLocal());
        assertTrue(TtsProvider.PIPER.isLocal());
        assertEquals(TtsProvider.PIPER, TtsProvider.fromStored("PIPER_UPMC"));
        assertEquals(TtsProvider.PIPER, TtsProvider.fromStored("PIPER_SIWIS"));
        assertFalse(TtsProvider.GOOGLE.isLocal());
    }

    /**
     * A stored value this build does not know - or none at all - is Kokoro, the shipped default, so a commander
     * who rolls back from a build with more engines still has a voice.
     */
    @Test
    void unknownStoredValuesFallBackToKokoro() {
        assertEquals(TtsProvider.KOKORO, TtsProvider.fromStored(null));
        assertEquals(TtsProvider.KOKORO, TtsProvider.fromStored(""));
        assertEquals(TtsProvider.KOKORO, TtsProvider.fromStored("ELEVENLABS"));
        assertEquals(TtsProvider.KOKORO, TtsProvider.fromStored("EDGE"), "Edge was removed in v1.1.0026");
        assertEquals(TtsProvider.SUPERTONIC, TtsProvider.fromStored("supertonic"));
    }
}
