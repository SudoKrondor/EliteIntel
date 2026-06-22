package elite.intel.companion.llm;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.companion.model.llm.LlmMessage;
import elite.intel.companion.model.llm.LlmMessageRole;
import elite.intel.companion.model.llm.LlmRequest;
import elite.intel.companion.model.llm.LlmResult;
import elite.intel.companion.model.llm.LlmToolDefinition;
import elite.intel.companion.model.llm.PromptCacheProfile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Render and parse coverage for the Mistral (OpenAI-compatible) tool-calling dialect. */
class MistralToolCallDialectTest {

    private final MistralToolCallDialect dialect = new MistralToolCallDialect();

    private static LlmToolDefinition speakTool() {
        return new LlmToolDefinition("speak", "Speak to the commander", "",
                List.of(new ActionParameterSpec("text", "string", true, "the words", List.of(), null)));
    }

    private static LlmRequest request(List<LlmToolDefinition> tools) {
        return new LlmRequest("req-1",
                List.of(LlmMessage.of(LlmMessageRole.SYSTEM, "rules"),
                        LlmMessage.of(LlmMessageRole.USER, "say hi")),
                tools, PromptCacheProfile.COMMANDER);
    }

    @Test
    void buildsToolCallingBodyWithSchemaChoiceAndCacheKey() {
        String body = dialect.buildRequestBody(request(List.of(speakTool())));
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();

        assertEquals("any", json.get("tool_choice").getAsString());
        assertEquals(PromptCacheProfile.COMMANDER.cacheKey(), json.get("prompt_cache_key").getAsString());
        assertFalse(json.has("response_format"), "tool-calling must not use legacy JSON mode");
        assertTrue(json.has("model"));

        JsonObject function = json.getAsJsonArray("tools").get(0).getAsJsonObject().getAsJsonObject("function");
        assertEquals("speak", function.get("name").getAsString());
        JsonObject schema = function.getAsJsonObject("parameters");
        assertEquals("object", schema.get("type").getAsString());
        assertEquals("string", schema.getAsJsonObject("properties").getAsJsonObject("text").get("type").getAsString());
        assertEquals("text", schema.getAsJsonArray("required").get(0).getAsString());

        JsonArray messages = json.getAsJsonArray("messages");
        assertEquals("system", messages.get(0).getAsJsonObject().get("role").getAsString());
        assertEquals("user", messages.get(1).getAsJsonObject().get("role").getAsString());
    }

    @Test
    void omitsToolsAndChoiceWhenNoToolsOffered() {
        String body = dialect.buildRequestBody(request(List.of()));
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();

        assertFalse(json.has("tools"));
        assertFalse(json.has("tool_choice"));
        assertTrue(json.has("prompt_cache_key"));
    }

    @Test
    void parsesToolCallsIntoInvocations() {
        LlmResult result = dialect.parse(responseWithToolCall("speak", "{\"text\":\"hi\"}"));

        assertTrue(result.isValid());
        assertEquals(1, result.toolInvocations().size());
        assertEquals("speak", result.toolInvocations().get(0).name());
        assertEquals("hi", result.toolInvocations().get(0).arguments().get("text").getAsString());
    }

    @Test
    void rejectsResponseWithoutToolCalls() {
        JsonObject message = new JsonObject();
        message.addProperty("content", "just text, no tool call");
        JsonObject choice = new JsonObject();
        choice.add("message", message);
        JsonArray choices = new JsonArray();
        choices.add(choice);
        JsonObject response = new JsonObject();
        response.add("choices", choices);

        assertEquals(LlmResult.Status.INVALID_RESPONSE, dialect.parse(response).status());
    }

    @Test
    void rejectsResponseWithoutChoices() {
        assertEquals(LlmResult.Status.INVALID_RESPONSE, dialect.parse(new JsonObject()).status());
    }

    private static JsonObject responseWithToolCall(String name, String argumentsJson) {
        JsonObject function = new JsonObject();
        function.addProperty("name", name);
        function.addProperty("arguments", argumentsJson);
        JsonObject call = new JsonObject();
        call.addProperty("id", "call-1");
        call.add("function", function);
        JsonArray calls = new JsonArray();
        calls.add(call);
        JsonObject message = new JsonObject();
        message.add("tool_calls", calls);
        JsonObject choice = new JsonObject();
        choice.add("message", message);
        JsonArray choices = new JsonArray();
        choices.add(choice);
        JsonObject response = new JsonObject();
        response.add("choices", choices);
        return response;
    }
}
