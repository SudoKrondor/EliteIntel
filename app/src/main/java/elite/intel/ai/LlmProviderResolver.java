package elite.intel.ai;

import elite.intel.session.SystemSession;

/**
 * Single source of truth for resolving the active cloud LLM provider from configuration. Both
 * {@link ApiFactory} (for the legacy command/query endpoints) and companion mode use this, so the
 * detection logic is written once.
 * <p>
 * The local host (LM Studio) is not resolved here: the local toggle differs by role
 * ({@code useLocalQueryLlm} vs {@code useLocalCommandLlm}), so it stays at the call site.
 */
public final class LlmProviderResolver {

    private LlmProviderResolver() {
    }

    /** The cloud LLM provider detected from the configured API key (LLM category). */
    public static ProviderEnum detectCloudProvider() {
        return KeyDetector.detectProvider(SystemSession.getInstance().getAiApiKey(), "LLM");
    }
}
