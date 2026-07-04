package elite.intel.companion.llm;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.inference.deepseek.DeepSeekClient;
import elite.intel.companion.model.llm.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The DeepSeek (cloud) provider config over the shared OpenAI-compatible adapter: the DeepSeek model,
 * {@code tool_choice=auto} (its thinking model rejects a forced tool_choice), no Mistral
 * {@code prompt_cache_key}, and a per-profile {@code temperature} (DeepSeek accepts a custom temperature).
 */
class DeepSeekLlmAdapterTest {

    private final DeepSeekLlmAdapter adapter = new DeepSeekLlmAdapter();

    @Test
    void rendersDeepSeekModelAutoToolChoiceNoCacheKeyAndTemperature() {
        LlmRequest request = new LlmRequest("req-1",
                List.of(LlmMessage.of(LlmMessageRole.USER, "say hi")),
                List.of(new LlmToolDefinition("speak", "Speak", "",
                        List.of(new ActionParameterSpec("text", "string", true, "the words", List.of(), null)))),
                PromptCacheProfile.COMMANDER);

        JsonObject json = JsonParser.parseString(adapter.buildRequestBody(request)).getAsJsonObject();

        assertEquals(DeepSeekClient.MODEL, json.get("model").getAsString());
        assertEquals("auto", json.get("tool_choice").getAsString(),
                "DeepSeek's thinking model rejects a forced tool_choice; auto is the supported value");
        assertFalse(json.has("prompt_cache_key"), "Mistral's cache key must not be sent to DeepSeek");
        assertTrue(json.has("temperature"), "DeepSeek accepts a custom temperature, so it must be sent");
    }
}
