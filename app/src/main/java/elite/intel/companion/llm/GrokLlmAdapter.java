package elite.intel.companion.llm;

import elite.intel.ai.brain.inference.xai.GrokClient;

/**
 * Grok (xAI, cloud) provider adapter: the {@link OpenAiCompatibleLlmAdapter} protocol with the Grok model,
 * {@code tool_choice=required} (xAI's OpenAI-compatible "must call a function" value), and no Mistral
 * {@code prompt_cache_key}. xAI accepts a custom temperature.
 */
public final class GrokLlmAdapter extends OpenAiCompatibleLlmAdapter {

    public GrokLlmAdapter() {
        super(GrokClient.MODEL_GROK_NON_REASONING, "required", false, true); // xAI accepts a custom temperature
    }
}
