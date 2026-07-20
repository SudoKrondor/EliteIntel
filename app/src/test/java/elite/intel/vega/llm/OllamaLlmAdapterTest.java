package elite.intel.vega.llm;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.vega.model.llm.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Ollama (local) provider config over the shared OpenAI-compatible adapter, using Ollama's
 * {@code /v1/chat/completions} endpoint: the configured served model, {@code tool_choice=required} (a forced
 * function call, which the native API cannot express), a custom temperature, and no Mistral cache key.
 */
class OllamaLlmAdapterTest {

    private final OllamaLlmAdapter adapter = new OllamaLlmAdapter("gemma3");

    @Test
    void rendersConfiguredModelRequiredToolChoiceCustomTemperatureAndNoCacheKey() {
        LlmRequest request = new LlmRequest("req-1",
                List.of(LlmMessage.of(LlmMessageRole.USER, "say hi")),
                List.of(new LlmToolDefinition("speak", "Speak", "",
                        List.of(new ActionParameterSpec("text", "string", true, "the words", List.of(), null)))),
                PromptCacheProfile.COMMANDER);

        JsonObject json = JsonParser.parseString(adapter.buildRequestBody(request)).getAsJsonObject();

        assertEquals("gemma3", json.get("model").getAsString());
        assertEquals("required", json.get("tool_choice").getAsString());
        assertFalse(json.has("prompt_cache_key"), "Mistral's cache key must not be sent to Ollama");
        // Local models accept a custom temperature, so it must be sent (like LM Studio, unlike OpenAI GPT-5).
        assertTrue(json.has("temperature"), "a custom temperature must be sent to Ollama");
    }
}
