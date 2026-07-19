package elite.intel.companion.llm;

import com.google.gson.JsonObject;

/**
 * LM Studio (local, OpenAI-compatible) provider adapter: the {@link OpenAiCompatibleLlmAdapter} protocol with
 * the configured served model, {@code tool_choice=required}, a single tool call, and no Mistral
 * {@code prompt_cache_key}. The model name (e.g. a loaded Gemma) comes from the LM Studio settings.
 */
public final class LmStudioLlmAdapter extends OpenAiCompatibleLlmAdapter {

    public LmStudioLlmAdapter(String model) {
        super(model, "required", false, true); // local models accept a custom temperature
    }

    @Override
    protected void addToolRequestParameters(JsonObject body) {
        body.addProperty("parallel_tool_calls", false);
    }
}
