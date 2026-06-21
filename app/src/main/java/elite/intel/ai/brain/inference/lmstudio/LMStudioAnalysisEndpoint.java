package elite.intel.ai.brain.inference.lmstudio;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.ai.ApiFactory;
import elite.intel.ai.brain.AIConstants;
import elite.intel.ai.brain.AiAnalysisInterface;
import elite.intel.ai.brain.actions.handlers.query.struct.AiData;
import elite.intel.ai.brain.commons.AiEndPoint;
import elite.intel.gameapi.SensorDataEvent;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.JsonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LMStudioAnalysisEndpoint extends AiEndPoint implements AiAnalysisInterface {

    private static final Logger log = LogManager.getLogger(LMStudioAnalysisEndpoint.class);
    private static final LMStudioAnalysisEndpoint INSTANCE = new LMStudioAnalysisEndpoint();
    private final Gson gson = GsonFactory.getGson();
    private final ApiFactory apiFactory = ApiFactory.getInstance();

    private LMStudioAnalysisEndpoint() {
    }

    public static LMStudioAnalysisEndpoint getInstance() {
        return INSTANCE;
    }

    private JsonObject buildResponseFormat() {
        JsonObject responseTextProp = new JsonObject();
        responseTextProp.addProperty("type", "string");
        JsonObject properties = new JsonObject();
        properties.add("text_to_speech_response", responseTextProp);
        JsonArray required = new JsonArray();
        required.add("text_to_speech_response");
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", properties);
        schema.add("required", required);
        schema.addProperty("additionalProperties", false);
        JsonObject jsonSchema = new JsonObject();
        jsonSchema.addProperty("name", "analysis_response");
        jsonSchema.addProperty("strict", true);
        jsonSchema.add("schema", schema);
        JsonObject responseFormat = new JsonObject();
        responseFormat.addProperty("type", "json_schema");
        responseFormat.add("json_schema", jsonSchema);
        return responseFormat;
    }

    @Override
    public JsonObject analyzeData(String originalUserInput, AiData struct) {
        try {
            LMStudioClient client = LMStudioClient.getInstance();
            JsonObject prompt = client.createPrompt(LMStudioClient.MODEL_QUERIES, 0.70f);

            JsonObject systemMsg = new JsonObject();
            systemMsg.addProperty("role", AIConstants.ROLE_SYSTEM);
            systemMsg.addProperty("content", apiFactory.getAiPromptFactory().generateAnalysisPrompt() + "\n " + struct.getInstructions());

            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", AIConstants.ROLE_USER);
            userMsg.addProperty("content", "User say: [" + originalUserInput + "] data: [" + struct.getData().toYaml() + "]");

            JsonArray messages = new JsonArray();
            messages.add(systemMsg);
            messages.add(userMsg);
            prompt.add("messages", messages);
            prompt.add("response_format", buildResponseFormat());

            log.debug("LM Studio analysis call:\n{}", gson.toJson(prompt));

            JsonObject root = processAiPrompt(gson.toJson(prompt), client);
            StructuredResponse sr = checkResponse(root);
            if (!sr.isSuccessful()) {
                JsonObject err = new JsonObject();
                err.addProperty("text_to_speech_response", "LLM error: no response from LM Studio");
                return err;
            }

            log.debug("LM Studio analysis raw response:\n{}", sr.content());
            return JsonUtils.sanitizeTtsResponse(JsonParser.parseString(JsonUtils.repairLlmJson(sr.content())).getAsJsonObject());

        } catch (Exception e) {
            log.error("LM Studio analysis failed", e);
            JsonObject err = new JsonObject();
            err.addProperty("text_to_speech_response", "LLM Returned malformed response. Analysis failed – check logs");
            return err;
        }
    }

    public JsonObject processSensor(SensorDataEvent event) {
        try {
            LMStudioClient client = LMStudioClient.getInstance();
            JsonObject prompt = client.createPrompt(LMStudioClient.MODEL_QUERIES, 0.70f);

            JsonArray messages = new JsonArray();

            JsonObject sensorPrompt = new JsonObject();
            sensorPrompt.addProperty("role", AIConstants.ROLE_SYSTEM);

            /// combine base prompt with event specific instructions
            sensorPrompt.addProperty("content", ApiFactory.getInstance().getAiPromptFactory().generateSensorPrompt() + "\n\n" + event.getInstructions());
            messages.add(sensorPrompt);
/*
            JsonObject instructions = new JsonObject();
            instructions.addProperty("role", AIConstants.ROLE_SYSTEM);
            instructions.addProperty("content", "EVENT SPECIFIC INSTRUCTIONS: " + event.getInstructions());
            messages.add(instructions);*/

            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", AIConstants.ROLE_USER);
            userMsg.addProperty("content", event.getSensorData());
            messages.add(userMsg);

            prompt.add("messages", messages);
            prompt.add("response_format", buildResponseFormat());

            JsonObject root = processAiPrompt(gson.toJson(prompt), client);
            log.debug("LM Studio sensor raw response:\n{}", gson.toJson(root));

            StructuredResponse sr = checkResponse(root);
            if (!sr.isSuccessful()) {
                JsonObject err = new JsonObject();
                err.addProperty("text_to_speech_response", "Sensor analysis failed – check logs");
                return err;
            }

            log.debug("LM Studio sensor content: {}", sr.content());
            JsonObject parsed = JsonParser.parseString(JsonUtils.repairLlmJson(sr.content())).getAsJsonObject();
            log.debug("LM Studio sensor parsed: {}", parsed);
            JsonObject sanitized = JsonUtils.sanitizeTtsResponse(parsed);
            log.debug("LM Studio sensor sanitized: {}", sanitized);
            return sanitized;

        } catch (Exception e) {
            log.error("LM Studio sensor processing failed", e);
            JsonObject err = new JsonObject();
            err.addProperty("text_to_speech_response", "Sensor analysis failed – check logs");
            return err;
        }
    }
}
