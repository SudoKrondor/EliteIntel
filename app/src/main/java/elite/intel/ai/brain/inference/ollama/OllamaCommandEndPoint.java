package elite.intel.ai.brain.inference.ollama;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.ai.brain.AIChatInterface;
import elite.intel.ai.brain.commons.AiEndPoint;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.JsonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OllamaCommandEndPoint extends AiEndPoint implements AIChatInterface {

    private static final Logger log = LogManager.getLogger(OllamaCommandEndPoint.class);
    private static final OllamaCommandEndPoint INSTANCE = new OllamaCommandEndPoint();

    private OllamaCommandEndPoint() {
    }

    public static OllamaCommandEndPoint getInstance() {
        return INSTANCE;
    }

    @Override public JsonObject processAiPrompt(JsonArray messages, float temp) {
        String bodyString = null;
        try {
            OllamaClient client = OllamaClient.getInstance();
            JsonObject prompt = client.createPrompt(OllamaClient.MODEL_COMMANDS, temp);

            JsonArray sanitized = sanitizeJsonArray(messages);
            prompt.add("messages", sanitized);


            // Enforce output schema at the grammar level - works even for smaller/weaker models
            JsonObject actionProp = new JsonObject();
            actionProp.addProperty("type", "string");
            JsonObject paramsProp = new JsonObject();
            paramsProp.addProperty("type", "object");
            JsonObject properties = new JsonObject();
            properties.add("action", actionProp);
            properties.add("params", paramsProp);
            JsonArray required = new JsonArray();
            required.add("action");
            required.add("params");
            JsonObject format = new JsonObject();
            format.addProperty("type", "object");
            format.add("properties", properties);
            format.add("required", required);
            format.addProperty("additionalProperties", false);
            prompt.add("format", format);

            bodyString = prompt.toString();
            log.debug("LLM API call:\n{}", GsonFactory.getGson().toJson(prompt));

            JsonObject root = processAiPrompt(bodyString, client);

            JsonObject message = root.getAsJsonObject("message");
            if (message == null || !message.has("content")) {
                log.error("No message/content from LLM:\n{}", root);
                GameEventBus.publish(new AiVoxResponseEvent("LLM error: no message/content from Ollama"));
                return null;
            }

            String content = message.get("content").getAsString();
            log.debug("LLM raw response:\n{}", GsonFactory.getGson().toJson(message));

            return JsonParser.parseString(JsonUtils.repairLlmJson(content)).getAsJsonObject();

        } catch (Exception e) {
            log.error("Ollama chat call failed: {}", e.getMessage(), e);
            log.error("Request body was:\n{}", bodyString != null ? bodyString : "null");
            GameEventBus.publish(new AiVoxResponseEvent("LLM call failed, check logs"));
            return null;
        }
    }
}