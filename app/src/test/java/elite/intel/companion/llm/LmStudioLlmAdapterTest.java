package elite.intel.companion.llm;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.companion.model.llm.LlmMessage;
import elite.intel.companion.model.llm.LlmMessageRole;
import elite.intel.companion.model.llm.LlmRequest;
import elite.intel.companion.model.llm.LlmToolDefinition;
import elite.intel.companion.model.llm.PromptCacheProfile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The LM Studio (local) provider config over the shared OpenAI-compatible adapter: the configured served
 * model, {@code tool_choice=required}, and no Mistral {@code prompt_cache_key}.
 */
class LmStudioLlmAdapterTest {

    private final LmStudioLlmAdapter adapter = new LmStudioLlmAdapter("gemma-3");

    @Test
    void rendersConfiguredModelRequiredToolChoiceAndNoCacheKey() {
        LlmRequest request = new LlmRequest("req-1",
                List.of(LlmMessage.of(LlmMessageRole.USER, "say hi")),
                List.of(new LlmToolDefinition("speak", "Speak", "",
                        List.of(new ActionParameterSpec("text", "string", true, "the words", List.of(), null)))),
                PromptCacheProfile.COMMANDER);

        JsonObject json = JsonParser.parseString(adapter.buildRequestBody(request)).getAsJsonObject();

        assertEquals("gemma-3", json.get("model").getAsString());
        assertEquals("required", json.get("tool_choice").getAsString());
        assertFalse(json.has("prompt_cache_key"), "Mistral's cache key must not be sent to LM Studio");
        // Local models accept a custom temperature, so it must be sent (the inverse of the OpenAI case).
        assertTrue(json.has("temperature"), "a custom temperature must be sent to LM Studio");
    }

    @Test
    void closedValueParamRendersJsonSchemaEnumWhileFreeFormDoesNot() {
        LlmRequest request = new LlmRequest("req-2",
                List.of(LlmMessage.of(LlmMessageRole.USER, "rate it")),
                List.of(new LlmToolDefinition("classify_turn", "Rate", "",
                        List.of(
                                new ActionParameterSpec("importance", "string", true, "how important",
                                        List.of(), null, List.of("low", "normal", "high", "max")),
                                new ActionParameterSpec("note", "string", false, "free text", List.of(), null)))),
                PromptCacheProfile.COMMANDER);

        JsonObject json = JsonParser.parseString(adapter.buildRequestBody(request)).getAsJsonObject();
        JsonObject properties = json.getAsJsonArray("tools").get(0).getAsJsonObject()
                .getAsJsonObject("function").getAsJsonObject("parameters").getAsJsonObject("properties");

        // The closed-value param carries a JSON-Schema enum constraining the model to those values.
        JsonObject importance = properties.getAsJsonObject("importance");
        assertTrue(importance.has("enum"), "closed-value param must render an enum");
        assertEquals(List.of("low", "normal", "high", "max"),
                importance.getAsJsonArray("enum").asList().stream().map(e -> e.getAsString()).toList());
        // The free-form param has no enum so the model is not constrained.
        assertFalse(properties.getAsJsonObject("note").has("enum"), "free-form param must not render an enum");
    }
}
