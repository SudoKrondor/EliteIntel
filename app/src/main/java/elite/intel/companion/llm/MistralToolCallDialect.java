package elite.intel.companion.llm;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.inference.mistral.MistralClient;
import elite.intel.companion.model.llm.LlmMessage;
import elite.intel.companion.model.llm.LlmMessageRole;
import elite.intel.companion.model.llm.LlmRequest;
import elite.intel.companion.model.llm.LlmResult;
import elite.intel.companion.model.llm.LlmToolDefinition;
import elite.intel.companion.model.llm.LlmToolInvocation;
import elite.intel.util.json.GsonFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Mistral implementation of {@link CompanionLlmDialect} (OpenAI-compatible chat-completions with native
 * tool-calling). Forces a function call ({@code tool_choice: "any"}) to honor the tool-calling-only
 * contract, sets {@code prompt_cache_key} from the profile, and omits {@code response_format} (that is
 * the legacy JSON-mode path, not tool-calling).
 */
public final class MistralToolCallDialect implements CompanionLlmDialect {

    /** Low temperature for stable tool selection. */
    private static final double TEMPERATURE = 0.3;

    @Override
    public String buildRequestBody(LlmRequest request) {
        JsonObject body = new JsonObject();
        body.addProperty("model", MistralClient.MODEL);
        body.addProperty("temperature", TEMPERATURE);
        body.add("messages", renderMessages(request.messages()));
        if (!request.tools().isEmpty()) {
            body.add("tools", renderTools(request.tools()));
            body.addProperty("tool_choice", "any"); // require a function call
        }
        body.addProperty("prompt_cache_key", request.profile().cacheKey());
        // Serialize via the shared Gson (HTML escaping disabled), matching how the rest of the app
        // sends bodies to Mistral; the default body.toString() would unicode-escape characters like
        // '=' and the apostrophe, bloating the body.
        return GsonFactory.getGson().toJson(body);
    }

    private JsonArray renderMessages(List<LlmMessage> messages) {
        JsonArray array = new JsonArray();
        for (LlmMessage m : messages) {
            JsonObject msg = new JsonObject();
            msg.addProperty("role", m.role().wireValue());
            msg.addProperty("content", m.content());
            if (m.role() == LlmMessageRole.TOOL && m.toolCallId() != null) {
                msg.addProperty("tool_call_id", m.toolCallId());
            }
            array.add(msg);
        }
        return array;
    }

    private JsonArray renderTools(List<LlmToolDefinition> tools) {
        JsonArray array = new JsonArray();
        for (LlmToolDefinition tool : tools) {
            JsonObject function = new JsonObject();
            function.addProperty("name", tool.name());
            function.addProperty("description", tool.description());
            function.add("parameters", renderParameterSchema(tool.parameters()));

            JsonObject entry = new JsonObject();
            entry.addProperty("type", "function");
            entry.add("function", function);
            array.add(entry);
        }
        return array;
    }

    /** JSON-Schema object for a tool's parameters; {@code ActionParameterSpec} types are already JSON types. */
    private JsonObject renderParameterSchema(List<ActionParameterSpec> parameters) {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject properties = new JsonObject();
        JsonArray required = new JsonArray();
        for (ActionParameterSpec p : parameters) {
            JsonObject prop = new JsonObject();
            prop.addProperty("type", p.getType());
            prop.addProperty("description", p.getDescription());
            properties.add(p.getName(), prop);
            if (p.isRequired()) {
                required.add(p.getName());
            }
        }
        schema.add("properties", properties);
        if (!required.isEmpty()) {
            schema.add("required", required);
        }
        return schema;
    }

    @Override
    public LlmResult parse(JsonObject response) {
        try {
            JsonArray toolCalls = toolCallsOf(response);
            if (toolCalls == null || toolCalls.isEmpty()) {
                return invalid();
            }
            List<LlmToolInvocation> invocations = new ArrayList<>();
            for (JsonElement element : toolCalls) {
                JsonObject call = element.getAsJsonObject();
                JsonObject function = call.getAsJsonObject("function");
                if (function == null || !function.has("name")) {
                    return invalid();
                }
                String name = function.get("name").getAsString();
                if (name == null || name.isBlank()) {
                    return invalid();
                }
                String id = call.has("id") && !call.get("id").isJsonNull() ? call.get("id").getAsString() : null;
                invocations.add(new LlmToolInvocation(id, name, parseArguments(function.get("arguments"))));
            }
            return new LlmResult(LlmResult.Status.OK, invocations);
        } catch (RuntimeException malformed) {
            return invalid();
        }
    }

    private JsonArray toolCallsOf(JsonObject response) {
        if (response == null || !response.has("choices")) {
            return null;
        }
        JsonArray choices = response.getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
        if (message == null || !message.has("tool_calls")) {
            return null;
        }
        return message.getAsJsonArray("tool_calls");
    }

    /** Mistral sends arguments as a JSON string; tolerate an already-parsed object too. */
    private JsonObject parseArguments(JsonElement arguments) {
        if (arguments == null || arguments.isJsonNull()) {
            return new JsonObject();
        }
        if (arguments.isJsonObject()) {
            return arguments.getAsJsonObject();
        }
        return JsonParser.parseString(arguments.getAsString()).getAsJsonObject();
    }

    private static LlmResult invalid() {
        return new LlmResult(LlmResult.Status.INVALID_RESPONSE, List.of());
    }
}
