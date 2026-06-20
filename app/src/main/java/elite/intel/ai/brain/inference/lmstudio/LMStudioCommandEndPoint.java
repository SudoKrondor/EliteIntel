package elite.intel.ai.brain.inference.lmstudio;

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

public class LMStudioCommandEndPoint extends AiEndPoint implements AIChatInterface {

    private static final Logger log = LogManager.getLogger(LMStudioCommandEndPoint.class);
    private static final LMStudioCommandEndPoint INSTANCE = new LMStudioCommandEndPoint();

    private LMStudioCommandEndPoint() {
    }

    public static LMStudioCommandEndPoint getInstance() {
        return INSTANCE;
    }

    @Override
    public JsonObject processAiPrompt(JsonArray messages, float temp) {
        String bodyString = null;
        try {
            LMStudioClient client = LMStudioClient.getInstance();
            JsonObject prompt = client.createPrompt(LMStudioClient.MODEL_COMMANDS, temp);

            JsonArray sanitized = sanitizeJsonArray(messages);
            prompt.add("messages", sanitized);

            // OpenAI-compatible structured output
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
            JsonObject schema = new JsonObject();
            schema.addProperty("type", "object");
            schema.add("properties", properties);
            schema.add("required", required);
            schema.addProperty("additionalProperties", false);
            JsonObject jsonSchema = new JsonObject();
            jsonSchema.addProperty("name", "command_response");
            jsonSchema.addProperty("strict", true);
            jsonSchema.add("schema", schema);
            JsonObject responseFormat = new JsonObject();
            responseFormat.addProperty("type", "json_schema");
            responseFormat.add("json_schema", jsonSchema);
            prompt.add("response_format", responseFormat);

            bodyString = prompt.toString();
            log.debug("LM Studio command API call:\n{}", GsonFactory.getGson().toJson(prompt));

            JsonObject root = processAiPrompt(bodyString, client);

            StructuredResponse sr = checkResponse(root);
            if (!sr.isSuccessful()) {
                return null;
            }

            log.debug("LM Studio command raw response:\n{}", sr.content());
            return JsonParser.parseString(JsonUtils.repairLlmJson(sr.content())).getAsJsonObject();

        } catch (Exception e) {
            log.error("LM Studio command call failed: {}", e.getMessage(), e);
            log.error("Request body was:\n{}", bodyString != null ? bodyString : "null");
            GameEventBus.publish(new AiVoxResponseEvent("LLM call failed, check logs"));
            return null;
        }
    }
}
