package elite.intel.ai.brain.inference.mistral;

import com.google.gson.*;
import elite.intel.ai.ApiFactory;
import elite.intel.ai.brain.AIConstants;
import elite.intel.ai.brain.AiAnalysisInterface;
import elite.intel.ai.brain.actions.handlers.query.struct.AiData;
import elite.intel.ai.brain.commons.AiEndPoint;
import elite.intel.util.json.GsonFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MistralAnalysisEndpoint extends AiEndPoint implements AiAnalysisInterface {
    private static final Logger log = LogManager.getLogger(MistralAnalysisEndpoint.class);
    private final Gson gson = GsonFactory.getGson();
    private static final MistralAnalysisEndpoint instance = new MistralAnalysisEndpoint();
    ApiFactory apiFactory = ApiFactory.getInstance();

    private MistralAnalysisEndpoint() {
    }

    public static MistralAnalysisEndpoint getInstance() {
        return instance;
    }

    @Override
    public JsonObject analyzeData(String originalUserInput, AiData struct) {
        try {
            MistralClient client = MistralClient.getInstance();

            String systemPrompt = apiFactory.getAiPromptFactory().generateAnalysisPrompt();

            JsonObject prompt = client.createPrompt(MistralClient.MODEL, 0.8f);

            JsonObject systemMessage1 = new JsonObject();
            systemMessage1.addProperty("role", AIConstants.ROLE_SYSTEM);
            systemMessage1.addProperty("content", systemPrompt);

            JsonObject systemMessage2 = new JsonObject();
            systemMessage2.addProperty("role", AIConstants.ROLE_SYSTEM);
            systemMessage2.addProperty("content", "INSTRUCTIONS: " + struct.getInstructions());

            JsonObject messageUser = new JsonObject();
            messageUser.addProperty("role", AIConstants.ROLE_USER);
            messageUser.addProperty("content", "User intent: " + originalUserInput + "\nData: " + struct.getData().toYaml());

            prompt.add("messages", gson.toJsonTree(new Object[]{systemMessage1, systemMessage2, messageUser}));

            String jsonString = gson.toJson(prompt);

            JsonObject response = processAiPrompt(jsonString, client);

            JsonArray choices = response.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) {
                log.error("No choices in API response:\n{}", response);
                return client.createErrorResponse("Analysis error.");
            }

            JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
            if (message == null) {
                log.error("No message in API response choices:\n{}", response);
                return client.createErrorResponse("Analysis error.");
            }

            String content = message.get("content").getAsString();
            if (content == null) {
                log.error("No content in API response message:\n{}", response);
                return client.createErrorResponse("Analysis error.");
            }

            log.debug("API response content:\n{}", content);

            String jsonContent = extractJsonFromContent(content);
            if (jsonContent == null) {
                log.error("Could not extract JSON from content:\n{}", content);
                return client.createErrorResponse("Analysis error.");
            }

            log.info("Extracted JSON content:\n\n{}\n\n", jsonContent);

            try {
                return JsonParser.parseString(jsonContent).getAsJsonObject();
            } catch (JsonSyntaxException e) {
                log.error("Failed to parse API response content:\n{}", jsonContent, e);
                return client.createErrorResponse("Analysis error.");
            }
        } catch (Exception e) {
            log.error("AI API call fatal error: {}", e.getMessage(), e);
            return MistralClient.getInstance().createErrorResponse("Analysis error. Check logs.");
        }
    }

    @Override
    public boolean verifyConnection() {
        MistralClient client = MistralClient.getInstance();
        return probeChatStyle(client.createPrompt(MistralClient.MODEL, 0.8f), client,
                root -> root.has("choices") && !root.getAsJsonArray("choices").isEmpty());
    }
}
