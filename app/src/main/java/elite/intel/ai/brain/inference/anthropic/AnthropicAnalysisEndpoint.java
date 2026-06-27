package elite.intel.ai.brain.inference.anthropic;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.ai.ApiFactory;
import elite.intel.ai.brain.AIConstants;
import elite.intel.ai.brain.AiAnalysisInterface;
import elite.intel.ai.brain.actions.handlers.query.struct.AiData;
import elite.intel.ai.brain.commons.AiEndPoint;
import elite.intel.util.json.GsonFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Analysis endpoint for the Anthropic (Claude) backend.
 * <p>
 * Mirrors OllamaAnalysisEndpoint, adapting the request body to Claude's shape:
 * - System messages are collected into a single top-level "system" field
 * - Content is read from response.content[0].text
 * <p>
 * Uses MODEL_OPUS for analysis (higher capability) and a slightly higher
 * temperature than commands to allow more nuanced analytical responses.
 */
public class AnthropicAnalysisEndpoint extends AiEndPoint implements AiAnalysisInterface {

    private static final Logger log = LogManager.getLogger(AnthropicAnalysisEndpoint.class);
    private static final AnthropicAnalysisEndpoint INSTANCE = new AnthropicAnalysisEndpoint();

    private final Gson gson = GsonFactory.getGson();
    private final ApiFactory apiFactory = ApiFactory.getInstance();

    private AnthropicAnalysisEndpoint() {
        // singleton
    }

    public static AnthropicAnalysisEndpoint getInstance() {
        return INSTANCE;
    }

    @Override
    public JsonObject analyzeData(String originalUserInput, AiData struct) {
        try {
            AnthropicClient client = AnthropicClient.getInstance();
            JsonObject prompt = client.createPrompt(AnthropicClient.MODEL_ANALYSIS_MODEL, 0.65f);

            // Check if this is general conversation - use minimal prompt to avoid Anthropic safety refusals
            boolean isGeneralConversation = struct.getInstructions() != null &&
                                            struct.getInstructions().contains("General Chat");

            String baseSystemPrompt;
            if (isGeneralConversation) {
                // Minimal prompt for general conversation to avoid safety refusals
                baseSystemPrompt = """
                        AI assistant.
                        Provide brief, casual conversational responses.
                        Output ONLY this exact JSON structure {"text_to_speech_response": "YOUR ANSWER HERE"} - no markdown, no extra text.
                        Keep responses to one short sentence.
                        """;
            } else {
                baseSystemPrompt = apiFactory.getAiPromptFactory().generateAnalysisPrompt();
            }

            String instructionsPrompt = "INSTRUCTIONS: " + struct.getInstructions();

            JsonArray systemArray = new JsonArray();

            JsonObject systemBlock1 = new JsonObject();
            systemBlock1.addProperty("type", "text");
            systemBlock1.addProperty("text", baseSystemPrompt);
            systemArray.add(systemBlock1);

            // Cache covers base + instructions together a larger prefix increases odds
            // of meeting Anthropic's minimum token threshold for cache writes.
            // Cache hits on consecutive calls of the same query type.
            JsonObject systemBlock2 = new JsonObject();
            systemBlock2.addProperty("type", "text");
            systemBlock2.addProperty("text", instructionsPrompt);
            JsonObject cacheControl = new JsonObject();
            cacheControl.addProperty("type", "ephemeral");
            systemBlock2.add("cache_control", cacheControl);
            systemArray.add(systemBlock2);

            prompt.add("system", systemArray);

            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", AIConstants.ROLE_USER);

            // Format data section - if it's just empty/minimal chat history, omit the data preamble
            String dataYaml = struct.getData().toYaml();
            String content;
            if (dataYaml == null || dataYaml.trim().isEmpty() || dataYaml.contains("coPilotBrief: \"Connection successful.\"")) {
                // For general conversation with minimal/no real data
                content = originalUserInput;
            } else {
                // For data queries
                content = originalUserInput + "\n\nData:\n" + dataYaml;
            }
            userMsg.addProperty("content", content);

            JsonArray messages = new JsonArray();
            messages.add(userMsg);
            prompt.add("messages", messages);

            log.debug("Anthropic analysis call:\n{}", gson.toJson(prompt));

            JsonObject root = processAiPrompt(gson.toJson(prompt), client);
            if (root == null) {
                return buildError("No response from Claude analysis endpoint");
            }

            log.debug("Anthropic analysis raw response:\n{}", gson.toJson(root));

            // Check for API-level error envelope
            if ("error".equals(root.has("type") ? root.get("type").getAsString() : null)) {
                log.error("Anthropic API error during analysis: {}", root);
                return buildError("Claude API error – check logs");
            }

            String text = client.extractText(root);
            if (text == null || text.isBlank()) {
                log.error("Empty analysis response from Claude:\n{}", root);
                return buildError("Empty analysis response");
            }

            // Strip any accidental markdown fences
            text = stripJsonFences(text);

            return JsonParser.parseString(text).getAsJsonObject();

        } catch (Exception e) {
            log.error("Anthropic analysis failed", e);
            return buildError("Analysis failed – check logs");
        }
    }

    @Override
    public boolean verifyConnection() {
        AnthropicClient client = AnthropicClient.getInstance();
        return probeChatStyle(client.createPrompt(AnthropicClient.MODEL_ANALYSIS_MODEL, 0.65f), client,
                root -> root.has("content")
                        && !(root.has("type") && "error".equals(root.get("type").getAsString())));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private JsonObject buildError(String message) {
        JsonObject err = new JsonObject();
        err.addProperty("text_to_speech_response", message);
        return err;
    }

    private String stripJsonFences(String text) {
        String t = text.trim();
        if (t.startsWith("```")) {
            int firstNewline = t.indexOf('\n');
            if (firstNewline != -1) t = t.substring(firstNewline + 1);
            if (t.endsWith("```")) t = t.substring(0, t.lastIndexOf("```"));
            t = t.trim();
        }
        return t;
    }
}