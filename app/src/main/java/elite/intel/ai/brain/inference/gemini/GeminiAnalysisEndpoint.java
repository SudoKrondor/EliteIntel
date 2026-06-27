package elite.intel.ai.brain.inference.gemini;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import elite.intel.ai.ApiFactory;
import elite.intel.ai.brain.AIConstants;
import elite.intel.ai.brain.AiAnalysisInterface;
import elite.intel.ai.brain.actions.handlers.query.struct.AiData;
import elite.intel.ai.brain.commons.AiEndPoint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GeminiAnalysisEndpoint extends AiEndPoint implements AiAnalysisInterface {
    private static final Logger log = LogManager.getLogger(GeminiAnalysisEndpoint.class);
    private static final GeminiAnalysisEndpoint instance = new GeminiAnalysisEndpoint();
    private final ApiFactory apiFactory = ApiFactory.getInstance();

    private GeminiAnalysisEndpoint() {
    }

    public static GeminiAnalysisEndpoint getInstance() {
        return instance;
    }

    @Override
    public JsonObject analyzeData(String originalUserInput, AiData struct) {
        try {
            GeminiClient client = GeminiClient.getInstance();
            JsonObject prompt = client.createPrompt(GeminiClient.MODEL_FLASH, 0.8f);
            JsonObject generationConfig = prompt.getAsJsonObject("generationConfig");
            generationConfig.addProperty("responseMimeType", "application/json");
            generationConfig.addProperty("maxOutputTokens", 8192);

            // System instruction: analysis prompt + query-specific instructions
            String systemPrompt = apiFactory.getAiPromptFactory().generateAnalysisPrompt();
            String instructions = "INSTRUCTIONS: " + struct.getInstructions();

            JsonObject systemInstruction = new JsonObject();
            JsonArray parts = new JsonArray();
            JsonObject part1 = new JsonObject();
            part1.addProperty("text", systemPrompt + "\n\n" + instructions);
            parts.add(part1);
            systemInstruction.add("parts", parts);
            prompt.add("system_instruction", systemInstruction);

            // User message: intent + YAML data
            JsonObject userPart = new JsonObject();
            userPart.addProperty("text", "User intent: " + originalUserInput + "\nData: " + struct.getData().toYaml());
            JsonArray userParts = new JsonArray();
            userParts.add(userPart);

            JsonObject userContent = new JsonObject();
            userContent.addProperty("role", "user");
            userContent.add("parts", userParts);

            JsonArray contents = new JsonArray();
            contents.add(userContent);
            prompt.add("contents", contents);

            String jsonString = prompt.toString();
            log.debug("Gemini analysis call:\n{}", jsonString);

            JsonObject rawResponse = processAiPrompt(jsonString, client);
            if (rawResponse == null) {
                log.error("Null response from Gemini analysis API");
                return client.createErrorResponse("Analysis error.");
            }

            if (!rawResponse.has("candidates")) {
                log.error("No candidates in Gemini analysis response:\n{}", rawResponse);
                return client.createErrorResponse("Analysis error.");
            }

            String finishReason = rawResponse.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .has("finishReason")
                    ? rawResponse.getAsJsonArray("candidates").get(0).getAsJsonObject().get("finishReason").getAsString()
                    : "UNKNOWN";
            if ("MAX_TOKENS".equals(finishReason)) {
                log.warn("Gemini analysis response truncated at token limit - increase maxOutputTokens");
            }

            String text = client.extractText(rawResponse);
            log.debug("Gemini analysis raw text:\n{}", text);

            // Strip markdown fences if present
            text = extractJson(text);

            try {
                return JsonParser.parseString(text).getAsJsonObject();
            } catch (JsonSyntaxException e) {
                log.warn("Gemini returned plain text instead of JSON - wrapping as TTS response");
                JsonObject fallback = new JsonObject();
                fallback.addProperty(AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE, text);
                return fallback;
            }

        } catch (Exception e) {
            log.error("Gemini analysis API fatal error: {}", e.getMessage(), e);
            return GeminiClient.getInstance().createErrorResponse("Analysis error. Check logs.");
        }
    }

    @Override
    public boolean verifyConnection() {
        GeminiClient client = GeminiClient.getInstance();
        JsonObject prompt = client.createPrompt(GeminiClient.MODEL_FLASH, 0.8f);
        JsonObject userPart = new JsonObject();
        userPart.addProperty("text", "ping");
        JsonArray parts = new JsonArray();
        parts.add(userPart);
        JsonObject userContent = new JsonObject();
        userContent.addProperty("role", "user");
        userContent.add("parts", parts);
        JsonArray contents = new JsonArray();
        contents.add(userContent);
        prompt.add("contents", contents);
        return probeConnection(prompt.toString(), client,
                root -> root.has("candidates") && !root.getAsJsonArray("candidates").isEmpty());
    }

    private String extractJson(String text) {
        if (text == null) return "{}";
        text = text.trim();
        int fenceStart = text.indexOf("```json");
        if (fenceStart != -1) {
            int start = text.indexOf('{', fenceStart + 7);
            int end = text.lastIndexOf('}');
            if (start != -1 && end > start) return text.substring(start, end + 1);
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start != -1 && end > start) return text.substring(start, end + 1);
        return text;
    }
}
