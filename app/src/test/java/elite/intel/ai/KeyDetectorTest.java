package elite.intel.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeyDetectorTest {

    /**
     * The Edge branch shipped a reserved "edge://" selector in the TTS key field. Selecting an engine is a
     * setting of its own now, so this string is just an unrecognised key - the key detector must never route
     * a provider selection again.
     */
    @Test
    void theRetiredEdgeSelectorIsJustAnUnknownKey() {
        assertEquals(ProviderEnum.UNKNOWN, KeyDetector.detectProvider("edge://", "TTS"));
        assertEquals(ProviderEnum.UNKNOWN, KeyDetector.detectProvider("edge://", "LLM"));
    }

    @Test
    void existingAndFallbackDetectionRemainUnchanged() {
        assertEquals(ProviderEnum.GOOGLE_TTS,
                KeyDetector.detectProvider("AIzaSy123456789012345678901234567890123", "TTS"));
        assertEquals(ProviderEnum.UNKNOWN, KeyDetector.detectProvider(null, "TTS"));
        assertEquals(ProviderEnum.UNKNOWN, KeyDetector.detectProvider("", "TTS"));
        assertEquals(ProviderEnum.UNKNOWN, KeyDetector.detectProvider("not-a-key", "TTS"));
    }
}
