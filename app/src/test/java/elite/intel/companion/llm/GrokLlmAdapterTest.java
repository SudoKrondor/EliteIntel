package elite.intel.companion.llm;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.inference.xai.GrokClient;
import elite.intel.companion.model.llm.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Grok (xAI, cloud) provider config over the shared OpenAI-compatible adapter: the Grok model,
 * {@code tool_choice=required}, no Mistral {@code prompt_cache_key}, and a per-profile {@code temperature}
 * (xAI accepts a custom temperature, unlike the OpenAI GPT-5 reasoning models).
 */
class GrokLlmAdapterTest {

    private final GrokLlmAdapter adapter = new GrokLlmAdapter();

    @Test
    void rendersGrokModelRequiredToolChoiceNoCacheKeyAndTemperature() {
        LlmRequest request = new LlmRequest("req-1",
                List.of(LlmMessage.of(LlmMessageRole.USER, "say hi")),
                List.of(new LlmToolDefinition("speak", "Speak", "",
                        List.of(new ActionParameterSpec("text", "string", true, "the words", List.of(), null)))),
                PromptCacheProfile.COMMANDER);

        JsonObject json = JsonParser.parseString(adapter.buildRequestBody(request)).getAsJsonObject();

        assertEquals(GrokClient.MODEL_GROK_NON_REASONING, json.get("model").getAsString());
        assertEquals("required", json.get("tool_choice").getAsString());
        assertFalse(json.has("prompt_cache_key"), "Mistral's cache key must not be sent to Grok");
        assertTrue(json.has("temperature"), "Grok accepts a custom temperature, so it must be sent");
    }
}
