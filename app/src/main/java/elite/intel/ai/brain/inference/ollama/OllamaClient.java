package elite.intel.ai.brain.inference.ollama;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.BaseAiClient;
import elite.intel.ai.brain.Client;
import elite.intel.eventbus.UiBus;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.ui.event.LlmUsageEvent;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.OllamaMetadata;

import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class OllamaClient extends BaseAiClient implements Client {
    public static final Integer MODEL_COMMANDS = 1;
    public static final Integer MODEL_QUERIES = 2;
    private static final OllamaClient INSTANCE = new OllamaClient();
    private final SystemSession systemSession = SystemSession.getInstance();

    private OllamaClient() {
        //
    }

    public static OllamaClient getInstance() {
        return INSTANCE;
    }

    private String getBaseUrl() {
        return systemSession.getOllamaAddress();
    }

    @Override
    public JsonObject createPrompt(int model, float temp) {

        String localLlmCommandModel = systemSession.getOllamaCommandModel().trim();
        String localLlmQueryModel = systemSession.getOllamaQueryModel().trim();

        boolean isQueryModel = model == MODEL_QUERIES;

        JsonObject request = new JsonObject();
        request.addProperty("model", isQueryModel ? localLlmQueryModel : localLlmCommandModel);
        request.addProperty("temperature", temp);
        request.addProperty("stream", false);
        request.addProperty("think", false);

        if (isQueryModel) {
            // Query model: larger context for data payloads, capped output to prevent generation loops
            request.addProperty("num_ctx", 8192);
            request.addProperty("num_predict", 512);  // hard cap - prevents infinite generation with structured output
        } else {
            // Command model: large system prompt (commands + queries list) needs real headroom, short output
            request.addProperty("num_ctx", 8192);
            request.addProperty("num_predict", 200);  // command responses are tiny JSON, 200 is plenty
        }

        /// OLLAMA tricks.
        /// Strongest "follow instructions + output clean JSON" preset for ~8–13B models
        request.addProperty("mirostat", 2);           // mirostat 2.0 - best for controlled output
        request.addProperty("mirostat_tau", 3.5f);    // 3.0–4.5 range; 3.5 is a common sweet spot
        request.addProperty("mirostat_eta", 0.1f);    // default
        request.addProperty("repeat_penalty", 1.12f); // 1.08–1.15 → prevents repeating keys or structure

        if (isQueryModel) {
            request.addProperty("top_k", 80);         // higher for query - allows natural language generation
        } else {
            request.addProperty("top_k", 10);         // low for commands - forces strict JSON structure
        }

        return request;
    }

    //not used
    @Override public JsonObject createPrompt(String model, float temp) {
        return null;
    }

    @Override
    public JsonObject createErrorResponse(String message) {
        JsonObject err = new JsonObject();
        err.addProperty("text_to_speech_response", message);
        return err;
    }

    @Override
    public synchronized JsonObject sendJsonRequest(String request) {
        JsonObject response = super.sendJsonRequest(buildRequest(request));
        OllamaMetadata metadata = GsonFactory.getGson().fromJson(response, OllamaMetadata.class);
        UiBus.publish(new AppLogEvent("Model " + metadata));
        if (metadata != null) {
            UiBus.publish(new LlmUsageEvent("Ollama",
                    metadata.model() != null ? metadata.model() : "local",
                    metadata.promptTokens(), metadata.completionTokens(), 0, 0,
                    metadata.tokensPerSecond()));
        }
        return response;
    }

    private HttpRequest buildRequest(String body) {
        return HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl()))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(1_100))
                .build();
    }
}