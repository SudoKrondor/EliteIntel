package elite.intel.ai.mouth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The stored engine setting: which engines are local, and what an unknown stored value reads as.
 */
class TtsProviderTest {

    /**
     * Supertonic is local and Google is not: the settings panel's LOCAL segment and the radio rule
     * ({@code RadioVoicing}) both read this.
     */
    @Test
    void onlySupertonicIsLocal() {
        assertTrue(TtsProvider.SUPERTONIC.isLocal());
        assertFalse(TtsProvider.GOOGLE.isLocal());
    }

    /**
     * A stored value this build does not know - or none at all - is Supertonic, the default, so a commander
     * who rolls back from a build with more engines still has a voice.
     */
    @Test
    void unknownStoredValuesFallBackToSupertonic() {
        assertEquals(TtsProvider.SUPERTONIC, TtsProvider.fromStored(null));
        assertEquals(TtsProvider.SUPERTONIC, TtsProvider.fromStored(""));
        assertEquals(TtsProvider.SUPERTONIC, TtsProvider.fromStored("ELEVENLABS"));
        assertEquals(TtsProvider.SUPERTONIC, TtsProvider.fromStored("EDGE"), "Edge was removed in v1.1.0026");
        assertEquals(TtsProvider.SUPERTONIC, TtsProvider.fromStored("KOKORO"), "Kokoro was removed in V1.2");
        assertEquals(TtsProvider.SUPERTONIC, TtsProvider.fromStored("supertonic"));
    }
}
