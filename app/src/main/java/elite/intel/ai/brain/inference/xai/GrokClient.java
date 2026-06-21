package elite.intel.ai.brain.inference.xai;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.BaseAiClient;
import elite.intel.ai.brain.Client;
import elite.intel.eventbus.UiBus;
import elite.intel.session.PlayerSession;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.ui.event.LlmUsageEvent;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.LlmMetadata;

import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class GrokClient extends BaseAiClient implements Client {

    /// use the same non-reasoning model for commands and queries for now.
    public static final String MODEL_GROK_NON_REASONING = "grok-4-1-fast-non-reasoning";
    public static final String MODEL_GROK_REASONING = "grok-4-1-fast-non-reasoning";

    ///
    public static final boolean IS_STREAM = false;
    private static final String API_URL = "https://api.x.ai/v1/chat/completions";
    private static final GrokClient instance = new GrokClient();
    private static final PlayerSession playerSession = PlayerSession.getInstance();


    private GrokClient() {
        //
    }

    public static GrokClient getInstance() {
        return instance;
    }

    //not used
    @Override public JsonObject createPrompt(int model, float temp) {
        return null;
    }

    @Override public JsonObject createPrompt(String model, float temp) {
        JsonObject header = new JsonObject();
        header.addProperty("model", model);
        header.addProperty("temperature", temp);
        header.addProperty("stream", IS_STREAM);
        return header;
    }

    public static JsonObject buildSensorResponseFormat() {
        JsonObject ttsProp = new JsonObject();
        ttsProp.addProperty("type", "string");
        JsonObject properties = new JsonObject();
        properties.add("text_to_speech_response", ttsProp);
        JsonArray required = new JsonArray();
        required.add("text_to_speech_response");
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", properties);
        schema.add("required", required);
        schema.addProperty("additionalProperties", false);
        JsonObject jsonSchema = new JsonObject();
        jsonSchema.addProperty("name", "sensor_response");
        jsonSchema.addProperty("strict", true);
        jsonSchema.add("schema", schema);
        JsonObject responseFormat = new JsonObject();
        responseFormat.addProperty("type", "json_schema");
        responseFormat.add("json_schema", jsonSchema);
        return responseFormat;
    }

    public static JsonObject buildCommandResponseFormat() {
        JsonObject actionProp = new JsonObject();
        actionProp.addProperty("type", "string");
        JsonObject paramsProp = new JsonObject();
        paramsProp.addProperty("type", "object");
        paramsProp.addProperty("additionalProperties", false);
        JsonObject properties = new JsonObject();
        properties.add("action", actionProp);
        properties.add("params", paramsProp);
        JsonArray required = new JsonArray();
        required.add("action");
        required.add("params");
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", properties);
        schema.add("required", required);
        schema.addProperty("additionalProperties", false);
        JsonObject jsonSchema = new JsonObject();
        jsonSchema.addProperty("name", "command_response");
        jsonSchema.addProperty("strict", true);
        jsonSchema.add("schema", schema);
        JsonObject responseFormat = new JsonObject();
        responseFormat.addProperty("type", "json_schema");
        responseFormat.add("json_schema", jsonSchema);
        return responseFormat;
    }

    @Override public JsonObject createErrorResponse(String message) {
        JsonObject error = new JsonObject();
        error.addProperty("text_to_speech_response", message);
        return error;
    }

    @Override public JsonObject sendJsonRequest(String request) {
        long t0 = System.nanoTime();
        JsonObject response = super.sendJsonRequest(buildRequest(request));
        long elapsed = System.nanoTime() - t0;
        LlmMetadata meta = GsonFactory.getGson().fromJson(response, LlmMetadata.class);
        UiBus.publish(new AppLogEvent("LLM: " + meta));
        if (meta != null && meta.usage() != null) {
            int cached = meta.usage().promptDetails() != null ? meta.usage().promptDetails().cachedTokens() : 0;
            UiBus.publish(new LlmUsageEvent("Grok",
                    meta.model() != null ? meta.model() : MODEL_GROK_NON_REASONING,
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
                .header("x-grok-conv-id", playerSession.getUUD())
                .timeout(Duration.ofSeconds(60))
                .build();
    }
}