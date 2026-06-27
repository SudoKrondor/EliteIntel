package elite.intel.companion.llm;

import elite.intel.ai.brain.inference.mistral.MistralClient;

/**
 * Mistral (cloud) provider adapter: the {@link OpenAiCompatibleLlmAdapter} protocol with the Mistral model,
 * {@code tool_choice=any} (Mistral's "must call a function" value), and the Mistral {@code prompt_cache_key}.
 */
public final class MistralLlmAdapter extends OpenAiCompatibleLlmAdapter {

    public MistralLlmAdapter() {
        super(MistralClient.MODEL, "any", true);
    }
}
