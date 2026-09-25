package elite.intel.ai;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The frozen, upgrade-only detector: each historical key shape still names its provider, so an install
 * upgraded with one keeps working, and anything else - including a guess between two - names none.
 */
class KeyDetectorTest {

    @Test
    void everyOpenAiKeyShapeIsDetected() {
        assertEquals(Optional.of(ProviderEnum.OPENAI), KeyDetector.detectLegacyProvider("sk-" + "a".repeat(48)));
        assertEquals(Optional.of(ProviderEnum.OPENAI), KeyDetector.detectLegacyProvider("sk-proj-" + "aB9_-".repeat(30)));
        assertEquals(Optional.of(ProviderEnum.OPENAI), KeyDetector.detectLegacyProvider("sk-svcacct-" + "aB9_-".repeat(30)));
        assertEquals(Optional.of(ProviderEnum.OPENAI), KeyDetector.detectLegacyProvider("sk-admin-" + "aB9_-".repeat(30)));
    }

    @Test
    void aDeepSeekKeyStaysUnambiguous() {
        assertEquals(Optional.of(ProviderEnum.DEEPSEEK),
                KeyDetector.detectLegacyProvider("sk-" + "0123456789abcdef".repeat(2)));
    }

    @Test
    void anAnthropicKeyDoesNotMatchTheOpenAiPattern() {
        assertEquals(Optional.of(ProviderEnum.ANTHROPIC),
                KeyDetector.detectLegacyProvider("sk-ant-api03-" + "aB9_-".repeat(19)));
    }

    @Test
    void bothMistralKeyShapesAreDetected() {
        assertEquals(Optional.of(ProviderEnum.MISTRAL), KeyDetector.detectLegacyProvider("aB3dE6gH9jK2mN5pQ8sT1vW4yZ7bC0eF"));
        assertEquals(Optional.of(ProviderEnum.MISTRAL),
                KeyDetector.detectLegacyProvider("mstrl_aB3dE6gH9jK2mN5pQ8sT1vW4yZ7bC0eF_x9Yz-Q1"));
    }

    @Test
    void bothGeminiKeyShapesAreDetected() {
        assertEquals(Optional.of(ProviderEnum.GEMINI), KeyDetector.detectLegacyProvider("AIzaSy123456789012345678901234567890123"));
        assertEquals(Optional.of(ProviderEnum.GEMINI), KeyDetector.detectLegacyProvider("AQ." + "aB9_.-".repeat(8)));
    }

    @Test
    void aGrokKeyIsDetected() {
        assertEquals(Optional.of(ProviderEnum.GROK), KeyDetector.detectLegacyProvider("xai-" + "aB9_-".repeat(10)));
    }

    /**
     * A stored key read back with stray whitespace is still the same key.
     */
    @Test
    void surroundingWhitespaceDoesNotHideTheProvider() {
        assertEquals(Optional.of(ProviderEnum.GROK), KeyDetector.detectLegacyProvider("  xai-" + "aB9_-".repeat(10) + "\n"));
    }

    @Test
    void nothingRecognisableNamesNoProvider() {
        assertEquals(Optional.empty(), KeyDetector.detectLegacyProvider(null));
        assertEquals(Optional.empty(), KeyDetector.detectLegacyProvider(""));
        assertEquals(Optional.empty(), KeyDetector.detectLegacyProvider("   "));
        assertEquals(Optional.empty(), KeyDetector.detectLegacyProvider("not-a-key"));
        assertEquals(Optional.empty(), KeyDetector.detectLegacyProvider("edge://"));
    }
}
