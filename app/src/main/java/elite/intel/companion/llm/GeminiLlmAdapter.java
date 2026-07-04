package elite.intel.companion.llm;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.companion.model.llm.*;
import elite.intel.util.json.GsonFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Native {@link LlmProviderAdapter} for Google's Gemini {@code generateContent} API, which is <em>not</em>
 * OpenAI-compatible. The differences this adapter bridges:
 * <ul>
 *   <li>the system prompt is a top-level {@code systemInstruction}, not a system-role message;</li>
 *   <li>turns live in {@code contents} with roles {@code user}/{@code model} (no "assistant"/"tool" roles);</li>
 *   <li>an assistant tool-call is a {@code {functionCall:{name, args}}} part and a tool result is a
 *       {@code {functionResponse:{name, response}}} part - linked by function <em>name</em>, not an id, and
 *       consecutive companion {@code TOOL} results are coalesced into one user turn;</li>
 *   <li>a thinking model (Gemini 3) attaches a {@code thoughtSignature} to each {@code functionCall} part and
 *       <em>requires</em> it sent back verbatim when that call is replayed in history, else the next request is
 *       rejected (HTTP 400). The neutral {@link LlmToolInvocation} has no provider-opaque slot, so {@link #parse}
 *       packs both the function name and the signature into the invocation id ({@code name  signature});
 *       {@link #modelTurn} re-emits the signature and {@link #functionResponsePart} reads back the name. This is
 *       confined to this adapter - the id is opaque to the rest of companion (it only round-trips it as the
 *       {@code tool_call_id}).</li>
 *   <li>tools are {@code {functionDeclarations:[{name, description, parameters}]}}, the parameter schema uses
 *       <em>uppercase</em> OpenAPI type names ({@code STRING}/{@code OBJECT}), and a forced call is
 *       {@code toolConfig.functionCallingConfig.mode = "ANY"};</li>
 *   <li>the response is {@code candidates[0].content.parts[]} ({@code functionCall} parts, or {@code text}).</li>
 * </ul>
 * Note: implemented to Gemini's documented wire shapes; not yet exercised against the live endpoint.
 */
public final class GeminiLlmAdapter implements LlmProviderAdapter {

    private static final int MAX_OUTPUT_TOKENS = 1024;

    @Override
    public String buildRequestBody(LlmRequest request) {
        JsonObject body = new JsonObject();

        String system = systemPrompt(request.messages());
        if (!system.isBlank()) {
            JsonObject part = new JsonObject();
            part.addProperty("text", system);
            JsonArray parts = new JsonArray();
            parts.add(part);
            JsonObject instruction = new JsonObject();
            instruction.add("parts", parts);
            body.add("systemInstruction", instruction);
        }

        body.add("contents", renderContents(request.messages()));

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", request.profile().temperature());
        generationConfig.addProperty("maxOutputTokens", MAX_OUTPUT_TOKENS);
        body.add("generationConfig", generationConfig);

        if (!request.tools().isEmpty()) {
            body.add("tools", renderTools(request.tools()));
            JsonObject functionCallingConfig = new JsonObject();
            functionCallingConfig.addProperty("mode", "ANY"); // require a function call this turn
            JsonObject toolConfig = new JsonObject();
            toolConfig.add("functionCallingConfig", functionCallingConfig);
            body.add("toolConfig", toolConfig);
        }
        // Shared Gson (HTML escaping disabled) to match how the rest of the app serializes provider bodies.
        return GsonFactory.getGson().toJson(body);
    }

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
     * Renders user/model turns (SYSTEM is hoisted out). Consecutive TOOL results coalesce into one user turn
     * of {@code functionResponse} parts, keeping the user/model turns clean.
     */
    private JsonArray renderContents(List<LlmMessage> messages) {
        JsonArray contents = new JsonArray();
        JsonArray pendingResponses = null;
        for (LlmMessage m : messages) {
            if (m.role() == LlmMessageRole.SYSTEM) {
                continue;
            }
            if (m.role() == LlmMessageRole.TOOL) {
                if (pendingResponses == null) {
                    pendingResponses = new JsonArray();
                }
                pendingResponses.add(functionResponsePart(m));
                continue;
            }
            if (pendingResponses != null) {
                contents.add(turn("user", pendingResponses));
                pendingResponses = null;
            }
            contents.add(m.role() == LlmMessageRole.ASSISTANT ? modelTurn(m) : turn("user", textParts(m.content())));
        }
        if (pendingResponses != null) {
            contents.add(turn("user", pendingResponses));
        }
        return contents;
    }

    private JsonObject turn(String role, JsonArray parts) {
        JsonObject content = new JsonObject();
        content.addProperty("role", role);
        content.add("parts", parts);
        return content;
    }

    private JsonArray textParts(String text) {
        JsonObject part = new JsonObject();
        part.addProperty("text", text == null ? "" : text);
        JsonArray parts = new JsonArray();
        parts.add(part);
        return parts;
    }

    /**
     * Model turn: optional leading text part, then one {@code functionCall} part per invocation.
     */
    private JsonObject modelTurn(LlmMessage m) {
        JsonArray parts = new JsonArray();
        if (m.content() != null && !m.content().isBlank()) {
            JsonObject text = new JsonObject();
            text.addProperty("text", m.content());
            parts.add(text);
        }
        for (LlmToolInvocation call : m.toolCalls()) {
            JsonObject functionCall = new JsonObject();
            functionCall.addProperty("name", call.name());
            functionCall.add("args", call.arguments() == null ? new JsonObject() : call.arguments());
            JsonObject part = new JsonObject();
            part.add("functionCall", functionCall);
            // Replay the thinking model's thought signature for this call; Gemini 3 rejects its absence.
            String signature = signatureOf(call.id());
            if (signature != null) {
                part.addProperty("thoughtSignature", signature);
            }
            parts.add(part);
        }
        return turn("model", parts);
    }

    /**
     * Gemini links a result to its call by function name; companion carries that name in the tool-call id.
     */
    private JsonObject functionResponsePart(LlmMessage m) {
        JsonObject response = new JsonObject();
        // The result text is wrapped under a single field; Gemini requires response to be a JSON object.
        response.addProperty("result", m.content() == null ? "" : m.content());
        JsonObject functionResponse = new JsonObject();
        functionResponse.addProperty("name", nameOf(m.toolCallId()));
        functionResponse.add("response", response);
        JsonObject part = new JsonObject();
        part.add("functionResponse", functionResponse);
        return part;
    }

    private JsonArray renderTools(List<LlmToolDefinition> tools) {
        JsonArray declarations = new JsonArray();
        for (LlmToolDefinition tool : tools) {
            JsonObject declaration = new JsonObject();
            declaration.addProperty("name", tool.name());
            if (tool.description() != null && !tool.description().isBlank()) {
                declaration.addProperty("description", tool.description());
            }
            // Gemini omits parameters entirely for a no-arg function (an empty-properties object is rejected).
            if (!tool.parameters().isEmpty()) {
                declaration.add("parameters", parameterSchema(tool.parameters()));
            }
            declarations.add(declaration);
        }
        JsonObject entry = new JsonObject();
        entry.add("functionDeclarations", declarations);
        JsonArray tools0 = new JsonArray();
        tools0.add(entry);
        return tools0;
    }

    /**
     * Gemini's OpenAPI-subset schema: same shape as JSON-Schema but with uppercase type names.
     */
    private JsonObject parameterSchema(List<ActionParameterSpec> parameters) {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "OBJECT");
        JsonObject properties = new JsonObject();
        JsonArray required = new JsonArray();
        for (ActionParameterSpec p : parameters) {
            JsonObject prop = new JsonObject();
            prop.addProperty("type", geminiType(p.getType()));
            prop.addProperty("description", ToolParameterSchema.describeParameter(p));
            List<String> enumValues = p.getEnumValues();
            if (!enumValues.isEmpty()) {
                JsonArray enumArray = new JsonArray();
                enumValues.forEach(enumArray::add);
                prop.add("enum", enumArray);
            }
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

    /**
     * Maps the project's JSON parameter types to Gemini's uppercase OpenAPI type enum.
     */
    private String geminiType(String jsonType) {
        return switch (jsonType) {
            case "number" -> "NUMBER";
            case "boolean" -> "BOOLEAN";
            default -> "STRING";
        };
    }

    @Override
    public LlmResult parse(JsonObject response) {
        try {
            JsonArray parts = partsOf(response);
            if (parts == null) {
                return invalid();
            }
            List<LlmToolInvocation> invocations = new ArrayList<>();
            for (JsonElement element : parts) {
                JsonObject part = element.getAsJsonObject();
                if (!part.has("functionCall") || !part.get("functionCall").isJsonObject()) {
                    continue;
                }
                JsonObject functionCall = part.getAsJsonObject("functionCall");
                if (!functionCall.has("name") || functionCall.get("name").isJsonNull()) {
                    return invalid();
                }
                String name = functionCall.get("name").getAsString();
                if (name.isBlank()) {
                    return invalid();
                }
                JsonObject args = functionCall.has("args") && functionCall.get("args").isJsonObject()
                        ? functionCall.getAsJsonObject("args") : new JsonObject();
                // Gemini issues no call id; the id carries the function name (for result linkage) plus the
                // thinking model's per-call thought signature (which must be replayed verbatim next turn).
                String signature = part.has("thoughtSignature") && !part.get("thoughtSignature").isJsonNull()
                        ? part.get("thoughtSignature").getAsString() : null;
                invocations.add(new LlmToolInvocation(encodeCallId(name, signature), name, args));
            }
            return invocations.isEmpty() ? invalid() : new LlmResult(LlmResult.Status.OK, invocations);
        } catch (RuntimeException malformed) {
            return invalid();
        }
    }

    @Override
    public String parseText(JsonObject response) {
        try {
            JsonArray parts = partsOf(response);
            if (parts == null) {
                return null;
            }
            for (JsonElement element : parts) {
                JsonObject part = element.getAsJsonObject();
                if (part.has("text") && !part.get("text").isJsonNull()) {
                    String text = part.get("text").getAsString();
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

    private JsonArray partsOf(JsonObject response) {
        if (response == null || !response.has("candidates")) {
            return null;
        }
        JsonArray candidates = response.getAsJsonArray("candidates");
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        JsonObject content = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
        if (content == null || !content.has("parts")) {
            return null;
        }
        return content.getAsJsonArray("parts");
    }

    // ---- Call-id codec: packs the function name and optional thought signature into the neutral id ----
    // Function names are [A-Za-z0-9_] and signatures are base64; neither contains the SOH separator.

    private static final char CALL_ID_SEPARATOR = '\u0001';

    /**
     * Encodes the function name and (optional) thought signature into the opaque invocation id.
     */
    private static String encodeCallId(String name, String signature) {
        return signature == null || signature.isBlank() ? name : name + CALL_ID_SEPARATOR + signature;
    }

    /**
     * The function name packed into a call id (the whole id when no signature was attached).
     */
    private static String nameOf(String callId) {
        if (callId == null) {
            return "";
        }
        int separator = callId.indexOf(CALL_ID_SEPARATOR);
        return separator < 0 ? callId : callId.substring(0, separator);
    }

    /**
     * The thought signature packed into a call id, or {@code null} when none was attached.
     */
    private static String signatureOf(String callId) {
        if (callId == null) {
            return null;
        }
        int separator = callId.indexOf(CALL_ID_SEPARATOR);
        return separator < 0 ? null : callId.substring(separator + 1);
    }

    private static LlmResult invalid() {
        return new LlmResult(LlmResult.Status.INVALID_RESPONSE, List.of());
    }
}
