package elite.intel.ai.brain.inference.anthropic;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.BaseAiClient;
import elite.intel.ai.brain.Client;
import elite.intel.eventbus.UiBus;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.ui.event.LlmUsageEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class AnthropicClient extends BaseAiClient implements Client {

    private static final Logger log = LogManager.getLogger(AnthropicClient.class);

    public static final String MODEL_COMMAND_MODEL = "claude-haiku-4-5-20251001";
    public static final String MODEL_ANALYSIS_MODEL = "claude-haiku-4-5-20251001";

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String API_VERSION = "2023-06-01";
    private static final int DEFAULT_MAX_TOKENS = 1024;

    private static final AnthropicClient INSTANCE = new AnthropicClient();

    private AnthropicClient() {
        // singleton
    }

    public static AnthropicClient getInstance() {
        return INSTANCE;
    }

    @Override public JsonObject createPrompt(int model, float temp) {
        return createPrompt(model == 1 ? MODEL_COMMAND_MODEL : MODEL_ANALYSIS_MODEL, temp);
    }

    @Override
    public JsonObject createPrompt(String model, float temp) {
        JsonObject request = new JsonObject();
        request.addProperty("model", model);
        request.addProperty("max_tokens", DEFAULT_MAX_TOKENS);
        request.addProperty("temperature", temp);
        return request;
    }

    @Override
    public JsonObject createErrorResponse(String message) {
        JsonObject err = new JsonObject();
        err.addProperty("text_to_speech_response", message);
        return err;
    }

    @Override
    public JsonObject sendJsonRequest(String request) {
        try {
            long t0 = System.nanoTime();
            JsonObject response = super.sendJsonRequest(buildRequest(request));
            long elapsed = System.nanoTime() - t0;
            if (response != null && response.has("usage")) {
                JsonObject usage = response.getAsJsonObject("usage");
                int in = usage.has("input_tokens") ? usage.get("input_tokens").getAsInt() : 0;
                int out = usage.has("output_tokens") ? usage.get("output_tokens").getAsInt() : 0;
                int cached = usage.has("cache_read_input_tokens") ? usage.get("cache_read_input_tokens").getAsInt() : 0;
                int written = usage.has("cache_creation_input_tokens") ? usage.get("cache_creation_input_tokens").getAsInt() : 0;
                UiBus.publish(new AppLogEvent(
                        "Claude – in= " + in + " out= " + out +
                                (cached > 0 ? " cache_read= " + cached : "") +
                                (written > 0 ? " cache_written= " + written : "") +
                                " tokens"));
                String model = response.has("model") ? response.get("model").getAsString() : MODEL_COMMAND_MODEL;
                UiBus.publish(new LlmUsageEvent("Claude", model, in, out, cached, written, wallClockTps(elapsed, out)));
            }
            return response;
        } catch (Exception e) {
            log.error("Request failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    private HttpRequest buildRequest(String body) {
        return HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .header("Content-Type", "application/json")
                .header("x-api-key", SystemSession.getInstance().getAiApiKey())
                .header("anthropic-version", API_VERSION)
                .timeout(Duration.ofSeconds(120))
                .build();
    }

    public String extractText(JsonObject response) {
        try {
            return response
                    .getAsJsonArray("content")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();
        } catch (Exception e) {
            return null;
        }
    }
}