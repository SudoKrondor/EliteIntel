package elite.intel.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeyDetectorTest {
    @Test
    void exactEdgeSentinelSelectsEdgeOnlyForTts() {
        assertEquals(ProviderEnum.EDGE_TTS, KeyDetector.detectProvider("edge://", "TTS"));
        assertEquals(ProviderEnum.UNKNOWN, KeyDetector.detectProvider("edge://", "LLM"));
        assertEquals(ProviderEnum.UNKNOWN, KeyDetector.detectProvider("EDGE://", "TTS"));
        assertEquals(ProviderEnum.UNKNOWN, KeyDetector.detectProvider(" edge://", "TTS"));
        assertEquals(ProviderEnum.UNKNOWN, KeyDetector.detectProvider("edge:///", "TTS"));
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
