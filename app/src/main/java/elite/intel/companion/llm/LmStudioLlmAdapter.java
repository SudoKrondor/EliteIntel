package elite.intel.companion.llm;

import com.google.gson.JsonObject;

/**
 * LM Studio (local, OpenAI-compatible) provider adapter: the {@link OpenAiCompatibleLlmAdapter} protocol with
 * the configured served model, {@code tool_choice=required}, parallel tool calls, and no Mistral
 * {@code prompt_cache_key}. The model name (e.g. a loaded Gemma) comes from the LM Studio settings.
 */
public final class LmStudioLlmAdapter extends OpenAiCompatibleLlmAdapter {

    public LmStudioLlmAdapter(String model) {
        super(model, "required", false, true); // local models accept a custom temperature
    }

    @Override
    protected void addToolRequestParameters(JsonObject body) {
        // Gemma 4's LM Studio PEG parser needs the multi-call grammar for classify_turn plus the settling call.
        body.addProperty("parallel_tool_calls", true);
    }
}
