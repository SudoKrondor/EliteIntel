package elite.intel.ai.brain.inference.openai;

import com.google.gson.*;
import elite.intel.ai.ApiFactory;
import elite.intel.ai.brain.AIConstants;
import elite.intel.ai.brain.AiAnalysisInterface;
import elite.intel.ai.brain.actions.handlers.query.struct.AiData;
import elite.intel.ai.brain.commons.AiEndPoint;
import elite.intel.util.json.GsonFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

import static elite.intel.ai.brain.AIConstants.PROPERTY_CONTENT;
import static elite.intel.ai.brain.AIConstants.PROPERTY_MESSAGE;

public class OpenAiAnalysisEndPoint extends AiEndPoint implements AiAnalysisInterface {
    private static final Logger log = LogManager.getLogger(OpenAiAnalysisEndPoint.class);
    private static OpenAiAnalysisEndPoint instance;

    private OpenAiAnalysisEndPoint() {
    }

    public static synchronized OpenAiAnalysisEndPoint getInstance() {
        if (instance == null) {
            instance = new OpenAiAnalysisEndPoint();
        }
        return instance;
    }

    @Override public JsonObject analyzeData(String originalUserInput, AiData struct) {
        try {
            OpenAiClient client = OpenAiClient.getInstance();
            String systemPrompt = ApiFactory.getInstance().getAiPromptFactory().generateAnalysisPrompt();

            JsonObject request = client.createPrompt(OpenAiClient.MODEL_GPT, 1);

            JsonObject systemMessaage1 = new JsonObject();
            systemMessaage1.addProperty("role", AIConstants.ROLE_SYSTEM);
            systemMessaage1.addProperty("content", systemPrompt);

            JsonObject systemMessaage2 = new JsonObject();
            systemMessaage2.addProperty("role", AIConstants.ROLE_SYSTEM);
            systemMessaage2.addProperty("content", "INSTRUCTIONS: " + struct.getInstructions());


            JsonObject messageUser = new JsonObject();
            messageUser.addProperty("role", AIConstants.ROLE_USER);
            messageUser.addProperty("content", "User Input: " + originalUserInput + ". Data: " + struct.getData().toYaml()+". ");

            JsonArray messages = new JsonArray();
            messages.add(systemMessaage1);
            messages.add(systemMessaage2);
            messages.add(messageUser);
            request.add("messages", messages);

            String jsonString = GsonFactory.getGson().toJson(request);
            log.debug("Analysis call:\n\n{}\n\n", jsonString);

            JsonObject response = processAiPrompt(jsonString, client);

            JsonElement jsonElement = response.get("usage");
            if(jsonElement != null) {
                log.info("API usage:\n{}", ("Prompt Tokens: "+jsonElement.getAsJsonObject().get("prompt_tokens")+"  Total Tokens:"+ jsonElement.getAsJsonObject().get("total_tokens")));
            }

            JsonArray choices = response.getAsJsonArray("choices");

            if (choices == null || choices.isEmpty()) {
                log.error("No choices in API response:\n{}", response);
                return client.createErrorResponse("Analysis error.");
            }

            JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject(PROPERTY_MESSAGE);
            if (message == null) {
                log.error("No message in API response choices:\n{}", response);
                return client.createErrorResponse("Analysis error.");
            }

            log.debug("AI Response: \n\n{}\n\n", GsonFactory.getGson().toJson(message));

            String content = message.get(PROPERTY_CONTENT).getAsString();
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

            try {
                return JsonParser.parseString(jsonContent).getAsJsonObject();
            } catch (JsonSyntaxException e) {
                log.error("Failed to parse API response content:\n{}", jsonContent, e);
                return client.createErrorResponse("Analysis error.");
            }
        } catch (IOException e) {
            log.error("Open AI API call failed: {}", e.getMessage(), e);
            return OpenAiClient.getInstance().createErrorResponse("Analysis error. Check logs.");
        } catch (Exception e) {
            log.error("AI API call fatal error: {}", e.getMessage(), e);
            return OpenAiClient.getInstance().createErrorResponse("Analysis error. Check logs.");
        }
    }

    @Override
    public boolean verifyConnection() {
        OpenAiClient client = OpenAiClient.getInstance();
        return probeChatStyle(client.createPrompt(OpenAiClient.MODEL_GPT, 1), client,
                root -> root.has("choices") && !root.getAsJsonArray("choices").isEmpty());
    }
}