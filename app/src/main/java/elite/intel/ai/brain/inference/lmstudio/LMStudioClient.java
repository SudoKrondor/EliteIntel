package elite.intel.ai.brain.inference.lmstudio;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class LMStudioClient extends BaseAiClient implements Client {

    private static final Logger log = LogManager.getLogger(LMStudioClient.class);

    public static final Integer MODEL_COMMANDS = 1;
    public static final Integer MODEL_QUERIES = 2;

    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";
    private static final Duration MODEL_LIST_TIMEOUT = Duration.ofSeconds(3);

    private static final LMStudioClient INSTANCE = new LMStudioClient();
    private final SystemSession systemSession = SystemSession.getInstance();

    private LMStudioClient() {
    }

    public static LMStudioClient getInstance() {
        return INSTANCE;
    }

    @Override
    public JsonObject createPrompt(int model, float temp) {
        boolean isQueryModel = model == MODEL_QUERIES;

        JsonObject request = new JsonObject();
        request.addProperty("model", systemSession.getLmStudioCommandModel().trim());
        request.addProperty("temperature", temp);
        request.addProperty("max_tokens", isQueryModel ? 1024 : 512);
        request.addProperty("stream", false);
        request.addProperty("reasoning_effort", "none");
        return request;
    }

    // not used
    @Override
    public JsonObject createPrompt(String model, float temp) {
        return null;
    }

    @Override
    public JsonObject createErrorResponse(String message) {
        JsonObject err = new JsonObject();
        err.addProperty("text_to_speech_response", message);
        return err;
    }

    /**
     * Sends a VEGA request without converting a transport failure into legacy speech JSON.
     */
    public synchronized AiTransportResult sendVegaRequest(String request) {
        long t0 = System.nanoTime();
        UiBus.publish(new AppLogEvent("LM Studio request -> model: " + requestedModel(request)));
        AiTransportResult outcome = sendTransportRequest(buildRequest(request));
        if (outcome instanceof AiTransportResult.Success success) {
            reportResponse(success.response(), System.nanoTime() - t0);
        }
        return outcome;
    }

    @Override
    public synchronized JsonObject sendJsonRequest(String request) {
        long t0 = System.nanoTime();
        // Diagnostic: surface the model we actually put on the wire (LM Studio silently serves the loaded
        // model when the requested name is unknown, so the response's model name alone can't confirm this).
        UiBus.publish(new AppLogEvent("LM Studio request -> model: " + requestedModel(request)));
        JsonObject response = super.sendJsonRequest(buildRequest(request));
        reportResponse(response, System.nanoTime() - t0);
        return response;
    }

    /**
     * Asks LM Studio which models it can serve. This is the server's own answer, so it can tell "not running"
     * apart from "running without the model", which a chat reply cannot: LM Studio silently serves whatever is
     * loaded when the requested name is unknown or empty.
     * <p>
     * It bypasses {@link #sendTransportRequest} on purpose: a model list is not an AI answer, and recording its
     * millisecond round-trip would drag the reply time a connection check reports toward zero.
     */
    public ModelListing listModels() {
        Optional<URI> modelsUri = modelsUri(systemSession.getLmStudioAddress());
        if (modelsUri.isEmpty()) {
            return new ModelListing.Unknown();
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(modelsUri.get())
                .GET()
                .timeout(MODEL_LIST_TIMEOUT)
                .build();
        HttpResponse<String> response;
        try {
            response = sendAsync(request).get();
        } catch (ExecutionException noAnswer) {
            log.debug("LM Studio model list got no answer: {}", noAnswer.getMessage());
            return new ModelListing.NotAnswering();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ModelListing.Unknown();
        }
        return parseModelList(response.statusCode(), response.body());
    }

    /**
     * Where the model list lives, derived from the stored chat address, or empty when that address is not the
     * standard {@code .../v1/chat/completions} shape and the list's location would only be a guess.
     */
    static Optional<URI> modelsUri(String chatAddress) {
        if (chatAddress == null || !chatAddress.strip().endsWith(CHAT_COMPLETIONS_PATH)) {
            return Optional.empty();
        }
        try {
            String base = chatAddress.strip();
            return Optional.of(URI.create(base.substring(0, base.length() - CHAT_COMPLETIONS_PATH.length()) + "/models"));
        } catch (IllegalArgumentException malformed) {
            log.warn("LM Studio address is not a valid URI: {}", chatAddress);
            return Optional.empty();
        }
    }

    /**
     * Reads the model ids out of an OpenAI-style {@code {"data":[{"id":...}]}} list. Any HTTP answer at all
     * means the server is running, so one this cannot read is {@link ModelListing.Unknown}, never "not answering".
     */
    static ModelListing parseModelList(int statusCode, String body) {
        if (statusCode < 200 || statusCode >= 300) {
            log.warn("LM Studio model list answered HTTP {}", statusCode);
            return new ModelListing.Unknown();
        }
        try {
            JsonArray data = JsonParser.parseString(body).getAsJsonObject().getAsJsonArray("data");
            if (data == null) {
                return new ModelListing.Unknown();
            }
            Set<String> ids = new HashSet<>();
            for (JsonElement model : data) {
                ids.add(model.getAsJsonObject().get("id").getAsString());
            }
            return new ModelListing.Listed(ids);
        } catch (RuntimeException unreadable) {
            log.warn("LM Studio model list is unreadable: {}", unreadable.getMessage());
            return new ModelListing.Unknown();
        }
    }

    /**
     * What LM Studio said about the models it can serve.
     */
    public sealed interface ModelListing {

        /**
         * No HTTP answer: the server is not running, or not at the stored address.
         */
        record NotAnswering() implements ModelListing {
        }

        /**
         * The server is up, but what it serves could not be learned.
         */
        record Unknown() implements ModelListing {
        }

        /**
         * The server is up and named the models it can serve.
         */
        record Listed(Set<String> ids) implements ModelListing {

            public Listed {
                ids = Set.copyOf(ids);
            }

            /**
             * True when {@code model} is one of them. LM Studio ids carry a publisher prefix
             * ({@code google/gemma-4-e4b}) that a commander may leave off, so a match on the part after the slash
             * counts too.
             */
            public boolean includes(String model) {
                String wanted = model.strip();
                return ids.stream().anyMatch(id -> id.equalsIgnoreCase(wanted)
                        || id.toLowerCase(Locale.ROOT).endsWith("/" + wanted.toLowerCase(Locale.ROOT)));
            }
        }
    }

    private void reportResponse(JsonObject response, long elapsed) {
        LlmMetadata meta = GsonFactory.getGson().fromJson(response, LlmMetadata.class);
        UiBus.publish(new AppLogEvent("LM Studio: " + LlmMetadata.describe(meta)));
        if (meta != null && meta.usage() != null) {
            UiBus.publish(new LlmUsageEvent("LM Studio",
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

    private HttpRequest buildRequest(String body) {
        String url = systemSession.getLmStudioAddress();
        log.info("LM Studio connecting to: {}", url);
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(1_100))
                .build();
    }
}
