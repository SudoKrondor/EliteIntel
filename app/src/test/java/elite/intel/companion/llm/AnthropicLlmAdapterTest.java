package elite.intel.companion.llm;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.inference.anthropic.AnthropicClient;
import elite.intel.companion.model.llm.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Render and parse coverage for the native Anthropic Messages-API adapter.
 */
class AnthropicLlmAdapterTest {

    private final AnthropicLlmAdapter adapter = new AnthropicLlmAdapter();

    private static LlmToolDefinition speakTool() {
        return new LlmToolDefinition("speak", "Speak to the commander", "",
                List.of(new ActionParameterSpec("text", "string", true, "the words", List.of(), null)));
    }

    private static LlmRequest request(List<LlmMessage> messages, List<LlmToolDefinition> tools) {
        return new LlmRequest("req-1", messages, tools, PromptCacheProfile.COMMANDER);
    }

    @Test
    void hoistsSystemToTopLevelAndKeepsUserInMessages() {
        JsonObject json = JsonParser.parseString(adapter.buildRequestBody(request(
                List.of(LlmMessage.of(LlmMessageRole.SYSTEM, "rules"),
                        LlmMessage.of(LlmMessageRole.USER, "say hi")),
                List.of(speakTool())))).getAsJsonObject();

        assertEquals(AnthropicClient.MODEL_COMMAND_MODEL, json.get("model").getAsString());
        assertTrue(json.has("max_tokens"), "Anthropic requires max_tokens");
        assertEquals("rules", json.get("system").getAsString());

        JsonArray messages = json.getAsJsonArray("messages");
        assertEquals(1, messages.size(), "the system message must not appear in messages");
        assertEquals("user", messages.get(0).getAsJsonObject().get("role").getAsString());
    }

    @Test
    void buildsToolsWithInputSchemaAndForcedToolChoice() {
        JsonObject json = JsonParser.parseString(adapter.buildRequestBody(request(
                List.of(LlmMessage.of(LlmMessageRole.USER, "hi")), List.of(speakTool())))).getAsJsonObject();

        assertEquals("any", json.getAsJsonObject("tool_choice").get("type").getAsString());
        JsonObject tool = json.getAsJsonArray("tools").get(0).getAsJsonObject();
        assertEquals("speak", tool.get("name").getAsString());
        JsonObject schema = tool.getAsJsonObject("input_schema");
        assertEquals("object", schema.get("type").getAsString());
        assertEquals("string", schema.getAsJsonObject("properties").getAsJsonObject("text").get("type").getAsString());
        assertEquals("text", schema.getAsJsonArray("required").get(0).getAsString());
    }

    @Test
    void omitsToolsAndChoiceWhenNoToolsOffered() {
        JsonObject json = JsonParser.parseString(adapter.buildRequestBody(request(
                List.of(LlmMessage.of(LlmMessageRole.USER, "summarize")), List.of()))).getAsJsonObject();

        assertFalse(json.has("tools"));
        assertFalse(json.has("tool_choice"));
    }

    @Test
    void rendersAssistantToolUseAndCoalescesConsecutiveToolResults() {
        JsonObject argsA = new JsonObject();
        argsA.addProperty("text", "hi");
        String firstId = "da2ea247-86cc-45d2-a272-9e99af14a88d";
        String secondId = "gateway-classify-1";
        LlmRequest req = request(List.of(
                LlmMessage.of(LlmMessageRole.USER, "go"),
                LlmMessage.assistantToolCalls(List.of(
                        new LlmToolInvocation(firstId, "speak", argsA),
                        new LlmToolInvocation(secondId, "scan", new JsonObject()))),
                LlmMessage.toolResult(firstId, "{\"status\":\"spoken\"}"),
                LlmMessage.toolResult(secondId, "{\"status\":\"scanned\"}")), List.of());

        JsonArray messages = JsonParser.parseString(adapter.buildRequestBody(req)).getAsJsonObject()
                .getAsJsonArray("messages");

        // user(go), assistant(2 tool_use), user(2 tool_result coalesced) - three turns, alternating roles.
        assertEquals(3, messages.size());
        JsonObject assistant = messages.get(1).getAsJsonObject();
        assertEquals("assistant", assistant.get("role").getAsString());
        JsonArray blocks = assistant.getAsJsonArray("content");
        assertEquals("tool_use", blocks.get(0).getAsJsonObject().get("type").getAsString());
        String firstWireId = blocks.get(0).getAsJsonObject().get("id").getAsString();
        String secondWireId = blocks.get(1).getAsJsonObject().get("id").getAsString();
        assertTrue(firstWireId.matches("[A-Za-z0-9]{9}"));
        assertTrue(secondWireId.matches("[A-Za-z0-9]{9}"));
        assertNotEquals(firstWireId, secondWireId);
        assertEquals("hi", blocks.get(0).getAsJsonObject().getAsJsonObject("input").get("text").getAsString());

        JsonObject toolTurn = messages.get(2).getAsJsonObject();
        assertEquals("user", toolTurn.get("role").getAsString());
        JsonArray results = toolTurn.getAsJsonArray("content");
        assertEquals(2, results.size(), "consecutive tool results must coalesce into one user turn");
        assertEquals("tool_result", results.get(0).getAsJsonObject().get("type").getAsString());
        assertEquals(firstWireId, results.get(0).getAsJsonObject().get("tool_use_id").getAsString());
        assertEquals(secondWireId, results.get(1).getAsJsonObject().get("tool_use_id").getAsString());
    }

    @Test
    void parsesToolUseBlocksIntoInvocations() {
        JsonObject input = new JsonObject();
        input.addProperty("text", "hi");
        LlmResult result = adapter.parse(responseWithToolUse("tool-1", "speak", input));

        assertTrue(result.isValid());
        assertEquals(1, result.toolInvocations().size());
        assertEquals("speak", result.toolInvocations().get(0).name());
        assertEquals("tool-1", result.toolInvocations().get(0).id());
        assertEquals("hi", result.toolInvocations().get(0).arguments().get("text").getAsString());
    }

    @Test
    void rejectsResponseWithoutToolUse() {
        assertEquals(LlmResult.Status.INVALID_RESPONSE, adapter.parse(responseWithText("just chatting")).status());
        assertEquals(LlmResult.Status.INVALID_RESPONSE, adapter.parse(new JsonObject()).status());
    }

    @Test
    void parsesTextBlockForCompression() {
        assertEquals("a compact summary", adapter.parseText(responseWithText("  a compact summary  ")));
    }

    @Test
    void parseTextReturnsNullForMissingText() {
        assertNull(adapter.parseText(new JsonObject()));
    }

    private static JsonObject responseWithText(String text) {
        JsonObject block = new JsonObject();
        block.addProperty("type", "text");
        block.addProperty("text", text);
        return wrap(block);
    }

    private static JsonObject responseWithToolUse(String id, String name, JsonObject input) {
        JsonObject block = new JsonObject();
        block.addProperty("type", "tool_use");
        block.addProperty("id", id);
        block.addProperty("name", name);
        block.add("input", input);
        return wrap(block);
    }

    private static JsonObject wrap(JsonObject block) {
        JsonArray content = new JsonArray();
        content.add(block);
        JsonObject response = new JsonObject();
        response.add("content", content);
        return response;
    }
}
