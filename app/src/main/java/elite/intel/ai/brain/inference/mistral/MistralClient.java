package elite.intel.ai.brain.inference.mistral;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.AiTransportResult;
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
import java.util.concurrent.TimeUnit;

public class MistralClient extends BaseAiClient implements Client {

    public static final String MODEL = "mistral-small-2506";
    private static final String API_URL = "https://api.mistral.ai/v1/chat/completions";
    private static final MistralClient instance = new MistralClient();

    /**
     * Minimum spacing between two sends to Mistral, over its free tier's one request per second. The margin
     * covers the round trip's own jitter; {@link SendRateGate} explains why the companion needs the gate at all.
     */
    private static final long MIN_SEND_INTERVAL_MILLIS = 1_100;

    /**
     * Every send from this client passes the gate, so the companion and the query analyser share one budget.
     */
    private final SendRateGate sendGate = new SendRateGate(MIN_SEND_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);

    private MistralClient() {
    }

    public static MistralClient getInstance() {
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
        JsonObject responseFormat = new JsonObject();
        responseFormat.addProperty("type", "json_object");
        header.add("response_format", responseFormat);
        return header;
    }

    @Override
    public JsonObject createErrorResponse(String message) {
        JsonObject error = new JsonObject();
        error.addProperty("text_to_speech_response", message);
        return error;
    }

    /** Sends a companion request without converting a transport failure into legacy speech JSON. */
    public AiTransportResult sendCompanionRequest(String request) {
        if (!sendGate.awaitSlot()) {
            return AiTransportResult.failure(AiTransportResult.FailureKind.CANCELLED, null,
                    "Request cancelled while waiting for the Mistral send slot");
        }
        long t0 = System.nanoTime();
        AiTransportResult outcome = sendTransportRequest(buildRequest(request));
        if (outcome instanceof AiTransportResult.Success success) {
            reportResponse(success.response(), System.nanoTime() - t0);
        }
        return outcome;
    }

    @Override
    public JsonObject sendJsonRequest(String request) {
        if (!sendGate.awaitSlot()) {
            return createErrorResponse("LLM Call Failed");
        }
        long t0 = System.nanoTime();
        JsonObject response = super.sendJsonRequest(buildRequest(request));
        reportResponse(response, System.nanoTime() - t0);
        return response;
    }

    private void reportResponse(JsonObject response, long elapsed) {
        LlmMetadata meta = GsonFactory.getGson().fromJson(response, LlmMetadata.class);
        UiBus.publish(new AppLogEvent("LLM: " + meta));
        if (meta != null && meta.usage() != null) {
            int cached = meta.usage().promptDetails() != null ? meta.usage().promptDetails().cachedTokens() : 0;
            UiBus.publish(new LlmUsageEvent("Mistral",
                    meta.model() != null ? meta.model() : MODEL,
                    meta.usage().promptTokens(), meta.usage().completionTokens(), cached, 0,
                    wallClockTps(elapsed, meta.usage().completionTokens())));
        }
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
