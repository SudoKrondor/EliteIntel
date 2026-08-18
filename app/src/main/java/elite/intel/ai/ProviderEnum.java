package elite.intel.ai;

/**
 * Represents an enumeration of AI service providers categorized by their primary functionality.
 * Each provider is associated with a specific category, such as LLM (Large Language Model),
 * STT (Speech-to-Text), TTS (Text-to-Speech), or UNKNOWN for unclassified providers.
 * <p>
 * The enum provides a mechanism to check if a specific provider supports a given category.
 */
public enum ProviderEnum {
    GROK("LLM"),
    DEEPSEEK("LLM"),
    MISTRAL("LLM"),
    GOOGLE_STT("STT"),
    GOOGLE_TTS("TTS"),
    GEMINI("LLM"),
    OPENAI("LLM"),
    ANTHROPIC("LLM"),
    AWS_LLM("LLM"),
    AWS_STT("STT"),
    AWS_TTS("TTS"),
    AZURE_LLM("LLM"),
    AZURE_STT("STT"),
    AZURE_TTS("TTS"),
    ELEVEN_LABS("TTS"),
    DEEP_GRAM("STT"),
    IBM_WATSON_STT("STT"),
    IBM_WATSON_TTS("TTS"),
    UNKNOWN("UNKNOWN");

    private final String category;

    public boolean supportsCategory(String category) {
        return this.category.equals(category);
    }

    ProviderEnum(String category) {
        this.category = category;
    }
}
