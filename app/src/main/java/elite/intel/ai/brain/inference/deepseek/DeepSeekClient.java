package elite.intel.ai.brain.inference.deepseek;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.BaseAiClient;
import elite.intel.ai.brain.Client;
import elite.intel.eventbus.UiBus;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.ui.event.LlmUsageEvent;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.LlmMetadata;

import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class DeepSeekClient extends BaseAiClient implements Client {

    public static final String MODEL = "deepseek-v4-flash";
    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final DeepSeekClient instance = new DeepSeekClient();

    private DeepSeekClient() {
    }

    public static DeepSeekClient getInstance() {
        return instance;
    }

    // not used
    @Override
    public JsonObject createPrompt(int model, float temp) {
        return null;
    }

    @Override
    public JsonObject createPrompt(String model, float temp) {
        JsonObject header = new JsonObject();
        header.addProperty("model", model);
        header.addProperty("temperature", temp);
        return header;
    }

    @Override
    public JsonObject createErrorResponse(String message) {
        JsonObject error = new JsonObject();
        error.addProperty("text_to_speech_response", message);
        return error;
    }

    @Override
    public JsonObject sendJsonRequest(String request) {
        long t0 = System.nanoTime();
        JsonObject response = super.sendJsonRequest(buildRequest(request));
        long elapsed = System.nanoTime() - t0;
        LlmMetadata meta = GsonFactory.getGson().fromJson(response, LlmMetadata.class);
        UiBus.publish(new AppLogEvent("LLM: " + meta));
        if (meta != null && meta.usage() != null) {
            int cached = meta.usage().promptDetails() != null ? meta.usage().promptDetails().cachedTokens() : 0;
            UiBus.publish(new LlmUsageEvent("DeepSeek",
                    meta.model() != null ? meta.model() : MODEL,
                    meta.usage().promptTokens(), meta.usage().completionTokens(), cached, 0,
                    wallClockTps(elapsed, meta.usage().completionTokens())));
        }
        return response;
    }

    HttpRequest buildRequest(String body) {
        return HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + SystemSession.getInstance().getAiApiKey())
                .timeout(Duration.ofSeconds(60))
                .build();
    }
}
