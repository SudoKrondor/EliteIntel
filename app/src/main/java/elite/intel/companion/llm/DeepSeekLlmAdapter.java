package elite.intel.companion.llm;

import elite.intel.ai.brain.inference.deepseek.DeepSeekClient;

/**
 * DeepSeek (cloud) provider adapter: the {@link OpenAiCompatibleLlmAdapter} protocol with the DeepSeek model
 * and no Mistral {@code prompt_cache_key}. DeepSeek accepts a custom temperature.
 * <p>
 * {@code tool_choice} is {@code auto}, not {@code required}: the configured model ({@code deepseek-v4-flash})
 * is a thinking model, and DeepSeek rejects a forced {@code tool_choice} in thinking mode ("Thinking mode does
 * not support this tool_choice", HTTP 400). The consciousness prompt strongly instructs a tool call and the
 * gateway retries once on a non-tool response, so {@code auto} still yields a tool call in practice.
 */
public final class DeepSeekLlmAdapter extends OpenAiCompatibleLlmAdapter {

    public DeepSeekLlmAdapter() {
        super(DeepSeekClient.MODEL, "auto", false, true); // thinking model: forced tool_choice is rejected
    }
}
