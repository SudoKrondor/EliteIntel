package elite.intel.ai;

import java.util.Optional;

/**
 * The cloud language-model providers the app can talk to - exactly the ones with a wired client, so every
 * value is runnable. The commander picks one from the AI services tab and it is stored as a setting of its own
 * ({@code game_session.llmProvider}); it is never inferred from the shape of the API key, because providers
 * change their key formats without notice.
 * <p>
 * The label is the vendor as the commander knows it from the page they got the key on, with the model family
 * beside it. Brand names read the same in every language, so they are not localized.
 */
public enum ProviderEnum {
    ANTHROPIC("Anthropic (Claude)"),
    OPENAI("OpenAI"),
    GEMINI("Google (Gemini)"),
    GROK("xAI (Grok)"),
    DEEPSEEK("DeepSeek"),
    MISTRAL("Mistral");

    private final String displayName;

    ProviderEnum(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    /**
     * Resolves a stored setting value, or empty for none: null, blank, or a provider this build does not know
     * (one written by a newer version, or one since retired). Empty means "not selected", never a guess.
     */
    public static Optional<ProviderEnum> fromStored(String stored) {
        if (stored == null || stored.isBlank()) {
            return Optional.empty();
        }
        for (ProviderEnum provider : values()) {
            if (provider.name().equalsIgnoreCase(stored.trim())) {
                return Optional.of(provider);
            }
        }
        return Optional.empty();
    }
}
