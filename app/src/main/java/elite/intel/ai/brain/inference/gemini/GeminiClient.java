package elite.intel.ai.brain.inference.gemini;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.BaseAiClient;
import elite.intel.ai.brain.Client;
import elite.intel.eventbus.UiBus;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.ui.event.LlmUsageEvent;

import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class GeminiClient extends BaseAiClient implements Client {

    public static final String MODEL_FLASH = "gemini-3.1-flash-lite-preview";
    private static final String API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final GeminiClient instance = new GeminiClient();

    private volatile String currentModel = MODEL_FLASH;

    private GeminiClient() {
    }

    public static GeminiClient getInstance() {
        return instance;
    }

    // not used
    @Override
    public JsonObject createPrompt(int model, float temp) {
        return null;
    }

    @Override
    public JsonObject createPrompt(String model, float temp) {
        this.currentModel = model;
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", temp);
        generationConfig.addProperty("maxOutputTokens", 1024);
        JsonObject prompt = new JsonObject();
        prompt.add("generationConfig", generationConfig);
        return prompt;
    }

    @Override
    public JsonObject createErrorResponse(String message) {
        JsonObject error = new JsonObject();
        error.addProperty("text_to_speech_response", message);
        return error;
    }

    /**
     * Companion-mode transport entry: pins {@code model} into the request URL for this call, then sends.
     * Companion builds the request body itself (native tool-calling) and never runs the legacy
     * {@link #createPrompt} path that sets {@link #currentModel}, so it must select the model here.
     */
    public JsonObject sendJsonRequest(String request, String model) {
        this.currentModel = model;
        return sendJsonRequest(request);
    }

    @Override
    public JsonObject sendJsonRequest(String request) {
        long t0 = System.nanoTime();
        JsonObject response = super.sendJsonRequest(buildRequest(request));
        long elapsed = System.nanoTime() - t0;
        if (response != null && response.has("usageMetadata")) {
            JsonObject usage = response.getAsJsonObject("usageMetadata");
            int promptTokens = usage.has("promptTokenCount") ? usage.get("promptTokenCount").getAsInt() : 0;
            int candidateTokens = usage.has("candidatesTokenCount") ? usage.get("candidatesTokenCount").getAsInt() : 0;
            int cachedTokens = usage.has("cachedContentTokenCount") ? usage.get("cachedContentTokenCount").getAsInt() : 0;
            UiBus.publish(new AppLogEvent(
                    "LLM Gemini [" + currentModel + "] in=" + promptTokens + " out=" + candidateTokens));
            UiBus.publish(new LlmUsageEvent("Gemini", currentModel, promptTokens, candidateTokens, cachedTokens, 0,
                    wallClockTps(elapsed, candidateTokens)));
        }
        return response;
    }

    private HttpRequest buildRequest(String body) {
        String apiKey = SystemSession.getInstance().getAiApiKey();
        URI uri = URI.create(API_BASE_URL + currentModel + ":generateContent?key=" + apiKey);
        return HttpRequest.newBuilder()
                .uri(uri)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    /**
     * Extracts the generated text from a Gemini API response.
     * Response shape: candidates[0].content.parts[0].text
     */
    public String extractText(JsonObject response) {
        return response
                .getAsJsonArray("candidates")
                .get(0).getAsJsonObject()
                .getAsJsonObject("content")
                .getAsJsonArray("parts")
                .get(0).getAsJsonObject()
                .get("text").getAsString();
    }
}