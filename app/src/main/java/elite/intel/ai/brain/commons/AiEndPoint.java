package elite.intel.ai.brain.commons;

import com.google.gson.*;
import elite.intel.ai.brain.AIConstants;
import elite.intel.ai.brain.Client;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.ws.WebSocketBroadcaster;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public abstract class AiEndPoint {

    public static final String CONNECTION_CHECK_COMMAND = "command_verify_connection";

    private static final Logger log = LogManager.getLogger(AiEndPoint.class);

    protected JsonArray sanitizeJsonArray(JsonArray messages) {
        if (messages == null) {
            return new JsonArray();
        }
        JsonArray sanitized = new JsonArray();
        for (int i = 0; i < messages.size(); i++) {
            JsonObject original = messages.get(i).getAsJsonObject();
            JsonObject sanitizedObj = new JsonObject();
            sanitizedObj.addProperty("role", original.get("role").getAsString());
            JsonElement content = original.get("content");
            if (content != null) {
                sanitizedObj.addProperty("content", content.getAsString());
            }
            sanitized.add(sanitizedObj);
        }
        return sanitized;
    }


    public JsonObject processAiPrompt(String jsonString, Client client) throws IOException {
        log.debug("AI API call:\n{}", jsonString);
        WebSocketBroadcaster.getInstance().broadcast(jsonString);
        return client.sendJsonRequest(jsonString);
    }

    public StructuredResponse checkResponse(JsonObject response) {
        JsonArray choices = response.getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) {
            log.error("No choices in API response:\n{}", response);
            return new StructuredResponse(null, null, null, false);
        }

        JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
        if (message == null) {
            log.error("No message in API response choices:\n{}", response);
            return new StructuredResponse(null, null, null, false);
        }

        JsonElement element = message.get("content");
        if (element == null) {
            GameEventBus.publish(new AiVoxResponseEvent("No content in API response message: " + response.toString().replace("\n", "")));
            throw new RuntimeException("No content in API response message");
        }
        String content = element.getAsString();

        if (content == null) {
            log.error("No content in API response message:\n{}", response);
            return new StructuredResponse(null, null, null, false);
        }
        return new StructuredResponse(choices, message, content, true);
    }

    public record StructuredResponse(JsonArray choices, JsonObject message, String content, boolean isSuccessful) {
    }

    /**
     * Returns true when the response is a BaseAiClient HTTP-error sentinel
     * (has text_to_speech_response, no choices). The error was already logged
     * and a TTS event already published by BaseAiClient - callers should return
     * null silently rather than logging a second misleading error.
     */
    protected boolean isHttpErrorResponse(JsonObject response) {
        return response.has(AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE) && !response.has("choices");
    }

    /**
     * Extracts the first valid JSON object from a model response string.
     * Handles ```json code fences, leading preamble, and uses brace-counting
     * to find the correct closing brace. Returns null if no valid JSON is found.
     */
    protected String extractJsonFromContent(String content) {
        content = content.trim();

        int start = content.indexOf('{');
        if (start == -1) return null;

        int fenceStart = content.indexOf("```json");
        if (fenceStart != -1) {
            start = content.indexOf('{', fenceStart + 7);
        }

        int braceCount = 0;
        int end = -1;
        for (int i = start; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{') braceCount++;
            else if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    end = i + 1;
                    break;
                }
            }
        }

        if (end == -1) return null;

        String candidate = content.substring(start, end);
        try {
            JsonParser.parseString(candidate);
            return candidate;
        } catch (JsonSyntaxException ignored) {
            return null;
        }
    }

}
