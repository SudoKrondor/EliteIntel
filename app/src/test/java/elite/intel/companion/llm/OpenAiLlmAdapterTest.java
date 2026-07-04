package elite.intel.companion.llm;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.inference.openai.OpenAiClient;
import elite.intel.companion.model.llm.LlmMessage;
import elite.intel.companion.model.llm.LlmMessageRole;
import elite.intel.companion.model.llm.LlmRequest;
import elite.intel.companion.model.llm.LlmToolDefinition;
import elite.intel.companion.model.llm.PromptCacheProfile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The OpenAI (cloud) provider config over the shared OpenAI-compatible adapter: the OpenAI model,
 * {@code tool_choice=required}, no Mistral {@code prompt_cache_key}, and - critically - no {@code temperature}.
 * The GPT-5 reasoning models reject any non-default temperature, so the request must omit it.
 */
class OpenAiLlmAdapterTest {

    private final OpenAiLlmAdapter adapter = new OpenAiLlmAdapter();

    @Test
    void rendersOpenAiModelRequiredToolChoiceNoCacheKeyAndNoTemperature() {
        LlmRequest request = new LlmRequest("req-1",
                List.of(LlmMessage.of(LlmMessageRole.USER, "say hi")),
                List.of(new LlmToolDefinition("speak", "Speak", "",
                        List.of(new ActionParameterSpec("text", "string", true, "the words", List.of(), null)))),
                PromptCacheProfile.COMMANDER);

        JsonObject json = JsonParser.parseString(adapter.buildRequestBody(request)).getAsJsonObject();

        assertEquals(OpenAiClient.MODEL_GPT, json.get("model").getAsString());
        assertEquals("required", json.get("tool_choice").getAsString());
        assertFalse(json.has("prompt_cache_key"), "Mistral's cache key must not be sent to OpenAI");
        assertFalse(json.has("temperature"), "temperature must be omitted for OpenAI GPT-5 reasoning models");
    }
}
