package elite.intel.companion.llm;

import elite.intel.ai.brain.inference.openai.OpenAiClient;

/**
 * OpenAI (cloud) provider adapter: the {@link OpenAiCompatibleLlmAdapter} protocol with the OpenAI model,
 * {@code tool_choice=required} (OpenAI's "must call a function" value), and no Mistral prompt cache key.
 * Temperature is not sent: the GPT-5 reasoning models reject any non-default {@code temperature}, so the
 * request uses the API default.
 */
public final class OpenAiLlmAdapter extends OpenAiCompatibleLlmAdapter {

    public OpenAiLlmAdapter() {
        super(OpenAiClient.MODEL_GPT, "required", false, false);
    }
}
