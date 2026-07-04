package elite.intel.companion.llm;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.inference.anthropic.AnthropicClient;
import elite.intel.companion.model.llm.*;
import elite.intel.util.json.GsonFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Native {@link LlmProviderAdapter} for Anthropic's Messages API ({@code /v1/messages}), which is <em>not</em>
 * OpenAI-compatible. The differences this adapter bridges:
 * <ul>
 *   <li>the system prompt is a top-level {@code system} string, not a {@code system}-role message;</li>
 *   <li>{@code max_tokens} is required;</li>
 *   <li>an assistant tool-call is a {@code content} block {@code {type:"tool_use", id, name, input}}, and a
 *       tool result is a {@code {type:"tool_result", tool_use_id, content}} block carried in a <em>user</em>
 *       turn - so consecutive companion {@code TOOL} messages are coalesced into one user turn (the API
 *       forbids two user turns in a row);</li>
 *   <li>tools are {@code {name, description, input_schema}} and a forced call is {@code tool_choice:{type:"any"}};</li>
 *   <li>the response is a top-level {@code content} block array ({@code tool_use} blocks, or {@code text}).</li>
 * </ul>
 * The parameter {@code input_schema} is the same standard JSON-Schema object the OpenAI-compatible adapter
 * uses, so it reuses {@link ToolParameterSchema}.
 * <p>
 * Note: implemented to Anthropic's documented wire shapes; not yet exercised against the live endpoint.
 */
public final class AnthropicLlmAdapter implements LlmProviderAdapter {

    private static final int MAX_TOKENS = 1024;

    @Override
    public String buildRequestBody(LlmRequest request) {
        JsonObject body = new JsonObject();
        body.addProperty("model", AnthropicClient.MODEL_COMMAND_MODEL);
        body.addProperty("max_tokens", MAX_TOKENS);
        body.addProperty("temperature", request.profile().temperature());

        String system = systemPrompt(request.messages());
        if (!system.isBlank()) {
            body.addProperty("system", system);
        }
        body.add("messages", renderMessages(request.messages()));

        if (!request.tools().isEmpty()) {
            body.add("tools", renderTools(request.tools()));
            JsonObject toolChoice = new JsonObject();
            toolChoice.addProperty("type", "any"); // require some tool call (Anthropic's "must call a function")
            body.add("tool_choice", toolChoice);
        }
        // Shared Gson (HTML escaping disabled) to match how the rest of the app serializes provider bodies.
        return GsonFactory.getGson().toJson(body);
    }

    /**
     * Joins every SYSTEM message into the single top-level {@code system} field Anthropic expects.
     */
    private String systemPrompt(List<LlmMessage> messages) {
        StringBuilder system = new StringBuilder();
        for (LlmMessage m : messages) {
            if (m.role() == LlmMessageRole.SYSTEM && m.content() != null) {
                if (system.length() > 0) {
                    system.append("\n\n");
                }
                system.append(m.content());
            }
        }
        return system.toString();
    }

    /**
     * Renders the user/assistant turns (SYSTEM is hoisted out). Consecutive TOOL results are merged into one
     * user turn of {@code tool_result} blocks, because Anthropic requires strictly alternating roles.
     */
    private JsonArray renderMessages(List<LlmMessage> messages) {
        JsonArray array = new JsonArray();
        JsonArray pendingToolResults = null; // accumulates a run of tool results into a single user turn
        for (LlmMessage m : messages) {
            if (m.role() == LlmMessageRole.SYSTEM) {
                continue;
            }
            if (m.role() == LlmMessageRole.TOOL) {
                if (pendingToolResults == null) {
                    pendingToolResults = new JsonArray();
                }
                pendingToolResults.add(toolResultBlock(m));
                continue;
            }
            // A non-tool message ends any pending tool-result run; flush it as one user turn first.
            if (pendingToolResults != null) {
                array.add(userTurn(pendingToolResults));
                pendingToolResults = null;
            }
            array.add(m.role() == LlmMessageRole.ASSISTANT ? assistantTurn(m) : userTextTurn(m.content()));
        }
        if (pendingToolResults != null) {
            array.add(userTurn(pendingToolResults));
        }
        return array;
    }

    private JsonObject userTextTurn(String content) {
        JsonObject turn = new JsonObject();
        turn.addProperty("role", "user");
        turn.addProperty("content", content == null ? "" : content);
        return turn;
    }

    private JsonObject userTurn(JsonArray contentBlocks) {
        JsonObject turn = new JsonObject();
        turn.addProperty("role", "user");
        turn.add("content", contentBlocks);
        return turn;
    }

    /**
     * Assistant turn: optional leading text block, then one {@code tool_use} block per invocation.
     */
    private JsonObject assistantTurn(LlmMessage m) {
        JsonArray content = new JsonArray();
        if (m.content() != null && !m.content().isBlank()) {
            JsonObject text = new JsonObject();
            text.addProperty("type", "text");
            text.addProperty("text", m.content());
            content.add(text);
        }
        for (LlmToolInvocation call : m.toolCalls()) {
            JsonObject block = new JsonObject();
            block.addProperty("type", "tool_use");
            if (call.id() != null) {
                block.addProperty("id", call.id());
            }
            block.addProperty("name", call.name());
            block.add("input", call.arguments() == null ? new JsonObject() : call.arguments());
            content.add(block);
        }
        JsonObject turn = new JsonObject();
        turn.addProperty("role", "assistant");
        turn.add("content", content);
        return turn;
    }

    private JsonObject toolResultBlock(LlmMessage m) {
        JsonObject block = new JsonObject();
        block.addProperty("type", "tool_result");
        if (m.toolCallId() != null) {
            block.addProperty("tool_use_id", m.toolCallId());
        }
        block.addProperty("content", m.content() == null ? "" : m.content());
        return block;
    }

    private JsonArray renderTools(List<LlmToolDefinition> tools) {
        JsonArray array = new JsonArray();
        for (LlmToolDefinition tool : tools) {
            JsonObject entry = new JsonObject();
            entry.addProperty("name", tool.name());
            if (tool.description() != null && !tool.description().isBlank()) {
                entry.addProperty("description", tool.description());
            }
            entry.add("input_schema", ToolParameterSchema.jsonSchemaObject(tool.parameters()));
            array.add(entry);
        }
        return array;
    }

    @Override
    public LlmResult parse(JsonObject response) {
        try {
            JsonArray content = contentOf(response);
            if (content == null) {
                return invalid();
            }
            List<LlmToolInvocation> invocations = new ArrayList<>();
            for (JsonElement element : content) {
                JsonObject block = element.getAsJsonObject();
                if (!"tool_use".equals(typeOf(block))) {
                    continue;
                }
                if (!block.has("name") || block.get("name").isJsonNull()) {
                    return invalid();
                }
                String name = block.get("name").getAsString();
                if (name.isBlank()) {
                    return invalid();
                }
                String id = block.has("id") && !block.get("id").isJsonNull() ? block.get("id").getAsString() : null;
                JsonObject input = block.has("input") && block.get("input").isJsonObject()
                        ? block.getAsJsonObject("input") : new JsonObject();
                invocations.add(new LlmToolInvocation(id, name, input));
            }
            return invocations.isEmpty() ? invalid() : new LlmResult(LlmResult.Status.OK, invocations);
        } catch (RuntimeException malformed) {
            return invalid();
        }
    }

    @Override
    public String parseText(JsonObject response) {
        try {
            JsonArray content = contentOf(response);
            if (content == null) {
                return null;
            }
            for (JsonElement element : content) {
                JsonObject block = element.getAsJsonObject();
                if ("text".equals(typeOf(block)) && block.has("text") && !block.get("text").isJsonNull()) {
                    String text = block.get("text").getAsString();
                    if (!text.isBlank()) {
                        return text.strip();
                    }
                }
            }
            return null;
        } catch (RuntimeException malformed) {
            return null;
        }
    }

    private JsonArray contentOf(JsonObject response) {
        if (response == null || !response.has("content") || !response.get("content").isJsonArray()) {
            return null;
        }
        return response.getAsJsonArray("content");
    }

    private String typeOf(JsonObject block) {
        return block.has("type") && !block.get("type").isJsonNull() ? block.get("type").getAsString() : null;
    }

    private static LlmResult invalid() {
        return new LlmResult(LlmResult.Status.INVALID_RESPONSE, List.of());
    }
}
