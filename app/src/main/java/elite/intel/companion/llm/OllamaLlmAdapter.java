package elite.intel.companion.llm;

/**
 * Ollama (local) provider adapter. Ollama serves an OpenAI-compatible chat-completions endpoint
 * ({@code /v1/chat/completions}) alongside its native API, so it rides the shared
 * {@link OpenAiCompatibleLlmAdapter} exactly like LM Studio: the configured served model,
 * {@code tool_choice=required} (a forced function call, which Ollama's native {@code /api/chat} cannot
 * express), and no Mistral {@code prompt_cache_key}. The transport targets Ollama's OpenAI-compatible
 * endpoint (see {@code OllamaClient#sendOpenAiChatRequest}). The served model is the configured Ollama
 * command model (e.g. a locally pulled Gemma 4, matching the LM Studio setup).
 */
public final class OllamaLlmAdapter extends OpenAiCompatibleLlmAdapter {

    public OllamaLlmAdapter(String model) {
        super(model, "required", false, true); // local model accepts a custom temperature
    }
}
