package elite.intel.ai.brain.inference.mistral;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import elite.intel.ai.brain.AIChatInterface;
import elite.intel.ai.brain.AIConstants;
import elite.intel.ai.brain.commons.AiEndPoint;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.ui.event.AiResponseLogEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MistralChatEndPoint extends AiEndPoint implements AIChatInterface {
    private static final Logger log = LogManager.getLogger(MistralChatEndPoint.class);
    private static final MistralChatEndPoint INSTANCE = new MistralChatEndPoint();

    private MistralChatEndPoint() {
    }

    public static MistralChatEndPoint getInstance() {
        return INSTANCE;
    }

    @Override
    public JsonObject processAiPrompt(JsonArray messages, float temp) {
        String bodyString = null;
        try {
            MistralClient client = MistralClient.getInstance();
            JsonObject prompt = client.createPrompt(MistralClient.MODEL, temp);

            JsonArray sanitizedMessages = sanitizeJsonArray(messages);
            prompt.add("messages", sanitizedMessages);

            bodyString = prompt.toString();
            log.debug("Mistral API chat call:\n{}", bodyString);

            JsonObject response = processAiPrompt(bodyString, client);

            JsonArray choices = response.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) {
                log.error("No choices in API response:\n{}", response);
                UiBus.publish(new AiResponseLogEvent(response.toString()));
                GameEventBus.publish(new AiVoxResponseEvent("Mistral Call failed."));
                return response;
            }

            JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
            if (message == null) {
                log.error("No message in API response choices:\n{}", response);
                return null;
            }

            String content = message.get("content").getAsString();
            if (content == null) {
                log.error("No content in API response message:\n{}", response);
                return null;
            }

            log.debug("API response content:\n{}", content);

            String jsonContent = extractJsonFromContent(content);
            if (jsonContent == null) {
                JsonObject result = new JsonObject();
                result.addProperty(AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE, content);
                return result;
            }

            log.debug("Extracted JSON content:\n\n{}\n\n", jsonContent);

            try {
                return JsonParser.parseString(jsonContent).getAsJsonObject();
            } catch (JsonSyntaxException e) {
                log.error("Failed to parse API response content:\n{}", jsonContent, e);
                JsonObject result = new JsonObject();
                result.addProperty(AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE, content);
                return result;
            }
        } catch (Exception e) {
            log.error("Mistral API chat call fatal error: {}", e.getMessage(), e);
            log.error("Input data:\n{}", bodyString != null ? bodyString : "null");
            return null;
        }
    }
}
