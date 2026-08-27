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

    /**
     * The OpenAI pattern used to demand exactly 161 characters after "sk-", which is one key's
     * length rather than a format. A legacy key is 48 characters and a project key has no fixed
     * length, so both fell through to UNKNOWN and companion mode refused to start.
     */
    @Test
    void everyOpenAiKeyShapeIsDetected() {
        assertEquals(ProviderEnum.OPENAI, KeyDetector.detectProvider("sk-" + "a".repeat(48), "LLM"));
        assertEquals(ProviderEnum.OPENAI, KeyDetector.detectProvider("sk-proj-" + "aB9_-".repeat(30), "LLM"));
        assertEquals(ProviderEnum.OPENAI, KeyDetector.detectProvider("sk-svcacct-" + "aB9_-".repeat(30), "LLM"));
        assertEquals(ProviderEnum.OPENAI, KeyDetector.detectProvider("sk-admin-" + "aB9_-".repeat(30), "LLM"));
    }

    /**
     * A DeepSeek key is "sk-" and 32 hex characters, which the widened OpenAI pattern must not also
     * match - two matches resolve to UNKNOWN, which would trade one broken provider for two.
     */
    @Test
    void aDeepSeekKeyStaysUnambiguous() {
        assertEquals(ProviderEnum.DEEPSEEK, KeyDetector.detectProvider("sk-" + "0123456789abcdef".repeat(2), "LLM"));
    }

    /**
     * A real Anthropic key must not also match the widened OpenAI pattern: two matches resolve to
     * UNKNOWN, which is the failure this change exists to remove, arriving from the other side.
     */
    @Test
    void anAnthropicKeyDoesNotMatchTheWidenedOpenAiPattern() {
        assertEquals(ProviderEnum.ANTHROPIC,
                KeyDetector.detectProvider("sk-ant-api03-" + "aB9_-".repeat(19), "LLM"));
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
