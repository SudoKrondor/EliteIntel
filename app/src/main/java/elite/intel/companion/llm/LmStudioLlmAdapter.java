package elite.intel.companion.llm;

/**
 * LM Studio (local, OpenAI-compatible) provider adapter: the {@link OpenAiCompatibleLlmAdapter} protocol with
 * the configured served model, {@code tool_choice=required} (the OpenAI "must call a function" value), and no
 * Mistral {@code prompt_cache_key}. The model name (e.g. a loaded Gemma) comes from the LM Studio settings.
 */
public final class LmStudioLlmAdapter extends OpenAiCompatibleLlmAdapter {

    public LmStudioLlmAdapter(String model) {
        super(model, "required", false, true); // local models accept a custom temperature
    }
}
