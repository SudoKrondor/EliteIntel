package elite.intel.companion.llm;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.companion.model.llm.*;
import elite.intel.util.json.GsonFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared {@link LlmProviderAdapter} for the OpenAI-compatible chat-completions protocol: renders the message
 * flow with native tools, assistant tool-calls and tool results, forces a function call on a consciousness
 * turn ({@code tool_choice}), and parses the response's tool-calls (or plain text for a compression turn).
 * <p>
 * Both Mistral (cloud) and an LM Studio local endpoint speak this protocol, so provider subclasses only
 * supply the served model, the "must call a function" {@code tool_choice} value, and whether to send
 * Mistral's {@code prompt_cache_key}. {@code response_format} is omitted (that is the legacy JSON-mode path,
 * not tool-calling).
 */
abstract class OpenAiCompatibleLlmAdapter implements LlmProviderAdapter {

    private final String model;
    private final String toolChoice;
    private final boolean sendCacheKey;
    private final boolean sendsTemperature;

    /**
     * @param model            the served model name
     * @param toolChoice       the "must call a function" value (Mistral {@code any}, OpenAI/LM Studio {@code required})
     * @param sendCacheKey     whether to send Mistral's {@code prompt_cache_key} (omit for other endpoints)
     * @param sendsTemperature whether the model accepts a custom {@code temperature}; false for OpenAI GPT-5
     *                         reasoning models, which reject any non-default value (the request is sent without it)
     */
    protected OpenAiCompatibleLlmAdapter(String model, String toolChoice, boolean sendCacheKey, boolean sendsTemperature) {
        this.model = model;
        this.toolChoice = toolChoice;
        this.sendCacheKey = sendCacheKey;
        this.sendsTemperature = sendsTemperature;
    }

    @Override
    public final String buildRequestBody(LlmRequest request) {
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        // Per-profile sampling temperature (COMMANDER runs warmer for livelier conversation; narration and
        // compression stay cooler for fidelity). Omitted for models that reject a custom temperature (OpenAI
        // GPT-5 reasoning models): the request then uses the API default.
        if (sendsTemperature) {
            body.addProperty("temperature", request.profile().temperature());
        }
        body.add("messages", renderMessages(request.messages()));
        if (!request.tools().isEmpty()) {
            body.add("tools", renderTools(request.tools()));
            body.addProperty("tool_choice", toolChoice); // require a function call
        }
        if (sendCacheKey) {
            body.addProperty("prompt_cache_key", request.profile().cacheKey());
        }
        // Serialize via the shared Gson (HTML escaping disabled), matching how the rest of the app sends
        // bodies to the provider; the default body.toString() would unicode-escape characters like '=' and
        // the apostrophe, bloating the body.
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
            // Replay the assistant tool-call turn so its tool results are a protocol-valid pair.
            if (m.role() == LlmMessageRole.ASSISTANT && !m.toolCalls().isEmpty()) {
                msg.add("tool_calls", renderToolCalls(m.toolCalls()));
            }
            array.add(msg);
        }
        return array;
    }

    /** Renders an assistant turn's tool invocations; the protocol expects {@code arguments} as a JSON string. */
    private JsonArray renderToolCalls(List<LlmToolInvocation> toolCalls) {
        JsonArray array = new JsonArray();
        for (LlmToolInvocation call : toolCalls) {
            JsonObject function = new JsonObject();
            function.addProperty("name", call.name());
            function.addProperty("arguments", GsonFactory.getGson().toJson(call.arguments()));

            JsonObject entry = new JsonObject();
            if (call.id() != null) {
                entry.addProperty("id", call.id());
            }
            entry.addProperty("type", "function");
            entry.add("function", function);
            array.add(entry);
        }
        return array;
    }

    private JsonArray renderTools(List<LlmToolDefinition> tools) {
        JsonArray array = new JsonArray();
        for (LlmToolDefinition tool : tools) {
            JsonObject function = new JsonObject();
            function.addProperty("name", tool.name());
            if (tool.description() != null && !tool.description().isBlank()) {
                function.addProperty("description", tool.description()); // optional field: omit when empty
            }
            // Standard JSON-Schema object, shared with the Anthropic adapter (see ToolParameterSchema).
            function.add("parameters", ToolParameterSchema.jsonSchemaObject(tool.parameters()));

            JsonObject entry = new JsonObject();
            entry.addProperty("type", "function");
            entry.add("function", function);
            array.add(entry);
        }
        return array;
    }

    @Override
    public final LlmResult parse(JsonObject response) {
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

    @Override
    public final String parseText(JsonObject response) {
        try {
            JsonObject message = messageOf(response);
            if (message == null || !message.has("content") || message.get("content").isJsonNull()) {
                return null;
            }
            String content = message.get("content").getAsString();
            return content == null || content.isBlank() ? null : content.strip();
        } catch (RuntimeException malformed) {
            return null;
        }
    }

    private JsonArray toolCallsOf(JsonObject response) {
        JsonObject message = messageOf(response);
        if (message == null || !message.has("tool_calls")) {
            return null;
        }
        return message.getAsJsonArray("tool_calls");
    }

    private JsonObject messageOf(JsonObject response) {
        if (response == null || !response.has("choices")) {
            return null;
        }
        JsonArray choices = response.getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        return choices.get(0).getAsJsonObject().getAsJsonObject("message");
    }

    /** The protocol sends arguments as a JSON string; tolerate an already-parsed object too. */
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
