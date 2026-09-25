package elite.intel.ai;

import elite.intel.session.SystemSession;

import java.util.Optional;

/**
 * Single source of truth for the active cloud LLM provider. Both {@link ApiFactory} (the analysis endpoint)
 * and VEGA read it here, so there is one answer to "which cloud are we talking to".
 * <p>
 * The provider is the commander's stored selection, nothing else: the key is what a provider needs, never what
 * picks it. The local host (LM Studio) is not resolved here: the local toggle differs by role
 * ({@code useLocalQueryLlm} vs {@code useLocalCommandLlm}), so it stays at the call site.
 */
public final class LlmProviderResolver {

    private LlmProviderResolver() {
    }

    /**
     * The cloud LLM provider the commander selected, or empty when none is.
     */
    public static Optional<ProviderEnum> cloudProvider() {
        return SystemSession.getInstance().getLlmProvider();
    }

    /**
     * True when the commander is set up for a cloud language model but has not said which one - nothing can
     * answer them until they pick a provider.
     */
    public static boolean isCloudProviderMissing() {
        return !SystemSession.getInstance().useLocalCommandLlm() && cloudProvider().isEmpty();
    }
}
