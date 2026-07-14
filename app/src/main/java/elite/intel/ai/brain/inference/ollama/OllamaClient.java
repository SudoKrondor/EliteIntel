package elite.intel.ai.brain.inference.ollama;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.ai.brain.AiTransportResult;
import elite.intel.ai.brain.BaseAiClient;
import elite.intel.ai.brain.Client;
import elite.intel.eventbus.UiBus;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.ui.event.LlmUsageEvent;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.LlmMetadata;
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

        boolean isQueryModel = model == MODEL_QUERIES;

        JsonObject request = new JsonObject();
        request.addProperty("model", localLlmCommandModel);
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

    /** Sends an OpenAI-compatible companion request without converting a transport failure into legacy speech JSON. */
    public synchronized AiTransportResult sendCompanionOpenAiChatRequest(String request) {
        long t0 = System.nanoTime();
        UiBus.publish(new AppLogEvent("Ollama request -> model: " + requestedModel(request)));
        AiTransportResult outcome = sendTransportRequest(buildOpenAiCompatibleRequest(request));
        if (outcome instanceof AiTransportResult.Success success) {
            reportOpenAiCompatibleResponse(success.response(), System.nanoTime() - t0);
        }
        return outcome;
    }

    /**
     * Sends an OpenAI-compatible chat-completions body to Ollama's {@code /v1/chat/completions} endpoint and
     * returns the raw OpenAI-shaped response. Companion mode uses this (not the native {@code /api/chat} path
     * that {@link #sendJsonRequest}) because only the compatibility endpoint honours {@code tool_choice}.
     * Usage is read from the OpenAI-style metadata, matching the LM Studio client's reporting.
     */
    public synchronized JsonObject sendOpenAiChatRequest(String request) {
        long t0 = System.nanoTime();
        // Diagnostic: surface the model we actually put on the wire (the response's model name alone can't
        // confirm the configured value was honoured).
        UiBus.publish(new AppLogEvent("Ollama request -> model: " + requestedModel(request)));
        JsonObject response = super.sendJsonRequest(buildOpenAiCompatibleRequest(request));
        reportOpenAiCompatibleResponse(response, System.nanoTime() - t0);
        return response;
    }

    private void reportOpenAiCompatibleResponse(JsonObject response, long elapsed) {
        LlmMetadata meta = GsonFactory.getGson().fromJson(response, LlmMetadata.class);
        UiBus.publish(new AppLogEvent("Ollama: " + LlmMetadata.describe(meta)));
        if (meta != null && meta.usage() != null) {
            UiBus.publish(new LlmUsageEvent("Ollama",
                    meta.model() != null ? meta.model() : "local",
                    meta.usage().promptTokens(), meta.usage().completionTokens(), 0, 0,
                    wallClockTps(elapsed, meta.usage().completionTokens())));
        }
    }

    /**
     * Reads the {@code model} field from an outgoing request body for the diagnostic log; tolerant of a malformed body.
     */
    private static String requestedModel(String body) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            return json.has("model") && !json.get("model").isJsonNull() ? json.get("model").getAsString() : "<none>";
        } catch (RuntimeException malformed) {
            return "<unparseable>";
        }
    }

    private HttpRequest buildOpenAiCompatibleRequest(String body) {
        return HttpRequest.newBuilder()
                .uri(URI.create(openAiCompatibleUrl(getBaseUrl())))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(1_100))
                .build();
    }

    /**
     * Derives Ollama's OpenAI-compatible endpoint from the configured native address. Ollama serves both
     * {@code /api/chat} (native) and {@code /v1/chat/completions} (OpenAI-compatible) off the same server
     * root, so the compatibility URL is that root plus the OpenAI path. Deriving it from the configured
     * host keeps a single Ollama address setting instead of adding a second one.
     */
    static String openAiCompatibleUrl(String configuredNativeUrl) {
        URI uri = URI.create(configuredNativeUrl.trim());
        return uri.getScheme() + "://" + uri.getAuthority() + "/v1/chat/completions";
    }
}
