package elite.intel.companion.llm;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.companion.model.llm.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Render and parse coverage for the native Gemini generateContent adapter.
 */
class GeminiLlmAdapterTest {

    private final GeminiLlmAdapter adapter = new GeminiLlmAdapter();

    private static LlmToolDefinition speakTool() {
        return new LlmToolDefinition("speak", "Speak to the commander", "",
                List.of(new ActionParameterSpec("text", "string", true, "the words", List.of(), null)));
    }

    private static LlmRequest request(List<LlmMessage> messages, List<LlmToolDefinition> tools) {
        return new LlmRequest("req-1", messages, tools, PromptCacheProfile.COMMANDER);
    }

    @Test
    void hoistsSystemToSystemInstructionAndMapsUserRole() {
        JsonObject json = JsonParser.parseString(adapter.buildRequestBody(request(
                List.of(LlmMessage.of(LlmMessageRole.SYSTEM, "rules"),
                        LlmMessage.of(LlmMessageRole.USER, "say hi")),
                List.of(speakTool())))).getAsJsonObject();

        assertEquals("rules", json.getAsJsonObject("systemInstruction")
                .getAsJsonArray("parts").get(0).getAsJsonObject().get("text").getAsString());

        JsonArray contents = json.getAsJsonArray("contents");
        assertEquals(1, contents.size(), "the system message must not appear in contents");
        assertEquals("user", contents.get(0).getAsJsonObject().get("role").getAsString());
        assertTrue(json.has("generationConfig"));
    }

    @Test
    void buildsFunctionDeclarationsWithUppercaseSchemaAndAnyMode() {
        JsonObject json = JsonParser.parseString(adapter.buildRequestBody(request(
                List.of(LlmMessage.of(LlmMessageRole.USER, "hi")), List.of(speakTool())))).getAsJsonObject();

        assertEquals("ANY", json.getAsJsonObject("toolConfig")
                .getAsJsonObject("functionCallingConfig").get("mode").getAsString());

        JsonObject declaration = json.getAsJsonArray("tools").get(0).getAsJsonObject()
                .getAsJsonArray("functionDeclarations").get(0).getAsJsonObject();
        assertEquals("speak", declaration.get("name").getAsString());
        JsonObject schema = declaration.getAsJsonObject("parameters");
        assertEquals("OBJECT", schema.get("type").getAsString());
        assertEquals("STRING", schema.getAsJsonObject("properties").getAsJsonObject("text").get("type").getAsString());
        assertEquals("text", schema.getAsJsonArray("required").get(0).getAsString());
    }

    @Test
    void omitsToolConfigWhenNoToolsOffered() {
        JsonObject json = JsonParser.parseString(adapter.buildRequestBody(request(
                List.of(LlmMessage.of(LlmMessageRole.USER, "summarize")), List.of()))).getAsJsonObject();

        assertFalse(json.has("tools"));
        assertFalse(json.has("toolConfig"));
    }

    @Test
    void rendersFunctionCallAsModelRoleAndFunctionResponseByName() {
        JsonObject args = new JsonObject();
        args.addProperty("text", "hi");
        // Gemini issues no call id; companion carries the function name as the id (see parse()).
        LlmRequest req = request(List.of(
                LlmMessage.of(LlmMessageRole.USER, "go"),
                LlmMessage.assistantToolCalls(List.of(new LlmToolInvocation("speak", "speak", args))),
                LlmMessage.toolResult("speak", "{\"status\":\"spoken\"}")), List.of());

        JsonArray contents = JsonParser.parseString(adapter.buildRequestBody(req)).getAsJsonObject()
                .getAsJsonArray("contents");

        JsonObject model = contents.get(1).getAsJsonObject();
        assertEquals("model", model.get("role").getAsString());
        JsonObject functionCall = model.getAsJsonArray("parts").get(0).getAsJsonObject().getAsJsonObject("functionCall");
        assertEquals("speak", functionCall.get("name").getAsString());
        assertEquals("hi", functionCall.getAsJsonObject("args").get("text").getAsString());

        JsonObject toolTurn = contents.get(2).getAsJsonObject();
        assertEquals("user", toolTurn.get("role").getAsString());
        JsonObject functionResponse = toolTurn.getAsJsonArray("parts").get(0).getAsJsonObject()
                .getAsJsonObject("functionResponse");
        assertEquals("speak", functionResponse.get("name").getAsString(), "functionResponse links by function name");
        assertTrue(functionResponse.getAsJsonObject("response").has("result"));
    }

    @Test
    void parsesFunctionCallsCarryingNameAsId() {
        JsonObject args = new JsonObject();
        args.addProperty("text", "hi");
        LlmResult result = adapter.parse(responseWithFunctionCall("speak", args));

        assertTrue(result.isValid());
        assertEquals(1, result.toolInvocations().size());
        assertEquals("speak", result.toolInvocations().get(0).name());
        assertEquals("speak", result.toolInvocations().get(0).id(), "name is carried as the id for result linkage");
        assertEquals("hi", result.toolInvocations().get(0).arguments().get("text").getAsString());
    }

    @Test
    void roundTripsThoughtSignatureFromResponseBackIntoReplayedFunctionCall() {
        JsonObject args = new JsonObject();
        args.addProperty("text", "hi");
        // A thinking model returns a thoughtSignature on the functionCall part.
        JsonObject response = responseWithFunctionCall("speak", args);
        response.getAsJsonArray("candidates").get(0).getAsJsonObject()
                .getAsJsonObject("content").getAsJsonArray("parts").get(0).getAsJsonObject()
                .addProperty("thoughtSignature", "SIG-abc123==");

        LlmResult parsed = adapter.parse(response);
        assertTrue(parsed.isValid());
        // The neutral name is preserved (used for execution and result linkage)...
        assertEquals("speak", parsed.toolInvocations().get(0).name());

        // ...and when the parsed invocation is replayed, the signature reappears on the functionCall part,
        // while the tool result still links by the bare function name.
        LlmRequest replay = request(List.of(
                LlmMessage.assistantToolCalls(parsed.toolInvocations()),
                LlmMessage.toolResult(parsed.toolInvocations().get(0).id(), "{\"status\":\"ok\"}")), List.of());

        JsonArray contents = JsonParser.parseString(adapter.buildRequestBody(replay)).getAsJsonObject()
                .getAsJsonArray("contents");

        JsonObject modelPart = contents.get(0).getAsJsonObject().getAsJsonArray("parts").get(0).getAsJsonObject();
        assertEquals("speak", modelPart.getAsJsonObject("functionCall").get("name").getAsString());
        assertEquals("SIG-abc123==", modelPart.get("thoughtSignature").getAsString(),
                "the thought signature must be replayed verbatim or Gemini 3 rejects the request");

        JsonObject responsePart = contents.get(1).getAsJsonObject().getAsJsonArray("parts").get(0).getAsJsonObject();
        assertEquals("speak", responsePart.getAsJsonObject("functionResponse").get("name").getAsString(),
                "functionResponse must link by the bare function name, not the signature-bearing id");
    }

    @Test
    void omitsThoughtSignatureWhenResponseHadNone() {
        JsonObject args = new JsonObject();
        args.addProperty("text", "hi");
        LlmResult parsed = adapter.parse(responseWithFunctionCall("speak", args));
        LlmRequest replay = request(List.of(LlmMessage.assistantToolCalls(parsed.toolInvocations())), List.of());

        JsonObject modelPart = JsonParser.parseString(adapter.buildRequestBody(replay)).getAsJsonObject()
                .getAsJsonArray("contents").get(0).getAsJsonObject()
                .getAsJsonArray("parts").get(0).getAsJsonObject();
        assertFalse(modelPart.has("thoughtSignature"), "no signature must be sent when none was returned");
    }

    @Test
    void rejectsResponseWithoutFunctionCall() {
        assertEquals(LlmResult.Status.INVALID_RESPONSE, adapter.parse(responseWithText("just text")).status());
        assertEquals(LlmResult.Status.INVALID_RESPONSE, adapter.parse(new JsonObject()).status());
    }

    @Test
    void parsesTextPartForCompression() {
        assertEquals("a compact summary", adapter.parseText(responseWithText("  a compact summary  ")));
    }

    @Test
    void parseTextReturnsNullForMissingCandidates() {
        assertNull(adapter.parseText(new JsonObject()));
    }

    private static JsonObject responseWithText(String text) {
        JsonObject part = new JsonObject();
        part.addProperty("text", text);
        return wrap(part);
    }

    private static JsonObject responseWithFunctionCall(String name, JsonObject args) {
        JsonObject functionCall = new JsonObject();
        functionCall.addProperty("name", name);
        functionCall.add("args", args);
        JsonObject part = new JsonObject();
        part.add("functionCall", functionCall);
        return wrap(part);
    }

    private static JsonObject wrap(JsonObject part) {
        JsonArray parts = new JsonArray();
        parts.add(part);
        JsonObject content = new JsonObject();
        content.add("parts", parts);
        JsonObject candidate = new JsonObject();
        candidate.add("content", content);
        JsonArray candidates = new JsonArray();
        candidates.add(candidate);
        JsonObject response = new JsonObject();
        response.add("candidates", candidates);
        return response;
    }
}
