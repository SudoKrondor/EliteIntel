package elite.intel.ai.brain.inference.lmstudio.install;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.ai.brain.inference.lmstudio.LmStudioDefaults;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * LM Studio's server on this PC, as the install uses it: the download, whether the model is loaded, and one test
 * request. The chat traffic itself still goes through {@code LMStudioClient}.
 * <p>
 * REST rather than {@code lms} where REST has the answer, because only the REST download reports its progress.
 */
final class LmStudioServer {

    private static final Duration QUICK = Duration.ofSeconds(5);
    /**
     * A first request can wait on the model being paged back in, so the test is given longer than a status call.
     */
    private static final Duration TEST_REQUEST = Duration.ofMinutes(2);

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(QUICK).build();

    // -------------------------------------------------------------------------
    // Server
    // -------------------------------------------------------------------------

    /**
     * Whether the server answers at all.
     */
    boolean isAnswering() {
        try {
            send(get("/v1/models", QUICK));
            return true;
        } catch (InstallException notYet) {
            return false;
        }
    }

    /**
     * Whether {@code model} has a loaded instance.
     */
    boolean isLoaded(String model) throws InstallException {
        return isLoaded(send(get("/api/v1/models", QUICK)), model);
    }

    /**
     * The context length {@code model} is loaded with, or empty when it is not loaded.
     */
    OptionalInt loadedContextLength(String model) throws InstallException {
        return loadedContextLength(send(get("/api/v1/models", QUICK)), model);
    }

    // -------------------------------------------------------------------------
    // Download
    // -------------------------------------------------------------------------

    /**
     * How a download request was answered.
     */
    sealed interface DownloadStart {
        record AlreadyDownloaded() implements DownloadStart {
        }

        record Started(String jobId) implements DownloadStart {
        }
    }

    enum DownloadState {DOWNLOADING, PAUSED, COMPLETED, FAILED}

    /**
     * Where a download stands. {@code bytesPerSecond} is 0 and {@code estimatedCompletion} empty unless it is
     * downloading.
     */
    record DownloadStatus(DownloadState state, long downloadedBytes, long totalBytes, double bytesPerSecond,
                          Optional<Instant> estimatedCompletion) {
    }

    DownloadStart startDownload(String model) throws InstallException {
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        return parseDownloadStart(send(post("/api/v1/models/download", body, QUICK)));
    }

    DownloadStatus downloadStatus(String jobId) throws InstallException {
        return parseDownloadStatus(send(get("/api/v1/models/download/status/" + jobId, QUICK)));
    }

    // -------------------------------------------------------------------------
    // Test
    // -------------------------------------------------------------------------

    /**
     * Sends one request shaped as {@code LmStudioLlmAdapter} sends VEGA's - a required tool call, reasoning off, no
     * streaming - and reports whether a tool call came back.
     */
    boolean answersWithToolCall(String model) throws InstallException {
        return hasToolCall(send(post("/v1/chat/completions", toolCallTest(model), TEST_REQUEST)));
    }

    static JsonObject toolCallTest(String model) {
        JsonObject function = new JsonObject();
        function.addProperty("name", "deploy_landing_gear");
        function.addProperty("description", "Lower the ship's landing gear.");
        JsonObject parameters = new JsonObject();
        parameters.addProperty("type", "object");
        parameters.add("properties", new JsonObject());
        function.add("parameters", parameters);
        JsonObject tool = new JsonObject();
        tool.addProperty("type", "function");
        tool.add("function", function);
        JsonArray tools = new JsonArray();
        tools.add(tool);

        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", "Deploy the landing gear.");
        JsonArray messages = new JsonArray();
        messages.add(message);

        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.add("messages", messages);
        body.add("tools", tools);
        body.addProperty("tool_choice", "required");
        body.addProperty("parallel_tool_calls", false);
        body.addProperty("reasoning_effort", "none");
        body.addProperty("stream", false);
        return body;
    }

    // -------------------------------------------------------------------------
    // Parsing (pure, so the shapes the smoke tests saw can be tested)
    // -------------------------------------------------------------------------

    static DownloadStart parseDownloadStart(String json) throws InstallException {
        JsonObject answer = object(json);
        String status = text(answer, "status");
        if ("already_downloaded".equals(status)) return new DownloadStart.AlreadyDownloaded();
        String jobId = text(answer, "job_id");
        if (jobId == null) {
            throw new InstallException("LM Studio did not start the download: " + json);
        }
        return new DownloadStart.Started(jobId);
    }

    static DownloadStatus parseDownloadStatus(String json) throws InstallException {
        JsonObject status = object(json);
        String state = text(status, "status");
        if (state == null) throw new InstallException("LM Studio's download status has no status: " + json);
        DownloadState parsed;
        try {
            parsed = DownloadState.valueOf(state.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            throw new InstallException("LM Studio reported an unknown download status: " + state);
        }
        return new DownloadStatus(parsed, number(status, "downloaded_bytes"), number(status, "total_size_bytes"),
                status.has("bytes_per_second") ? status.get("bytes_per_second").getAsDouble() : 0,
                instant(text(status, "estimated_completion")));
    }

    static boolean isLoaded(String modelsJson, String model) throws InstallException {
        return loadedInstance(modelsJson, model).isPresent();
    }

    /**
     * The context length of {@code model}'s first loaded instance, or empty when none is loaded or it does not say.
     */
    static OptionalInt loadedContextLength(String modelsJson, String model) throws InstallException {
        Optional<JsonObject> instance = loadedInstance(modelsJson, model);
        if (instance.isEmpty()) return OptionalInt.empty();
        JsonObject config = instance.get().getAsJsonObject("config");
        if (config == null || !config.has("context_length")) return OptionalInt.empty();
        return OptionalInt.of(config.get("context_length").getAsInt());
    }

    private static Optional<JsonObject> loadedInstance(String modelsJson, String model) throws InstallException {
        JsonArray models = object(modelsJson).getAsJsonArray("models");
        if (models == null) throw new InstallException("LM Studio's model list has no models: " + modelsJson);
        for (JsonElement entry : models) {
            JsonObject listed = entry.getAsJsonObject();
            if (!model.equalsIgnoreCase(text(listed, "key"))) continue;
            JsonArray instances = listed.getAsJsonArray("loaded_instances");
            if (instances == null || instances.isEmpty()) return Optional.empty();
            return Optional.of(instances.get(0).getAsJsonObject());
        }
        return Optional.empty();
    }

    static boolean hasToolCall(String completionJson) throws InstallException {
        JsonArray choices = object(completionJson).getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) return false;
        JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
        if (message == null) return false;
        JsonArray toolCalls = message.getAsJsonArray("tool_calls");
        return toolCalls != null && !toolCalls.isEmpty();
    }

    private static JsonObject object(String json) throws InstallException {
        try {
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (RuntimeException unreadable) {
            throw new InstallException("LM Studio answered with something that is not JSON: " + json, unreadable);
        }
    }

    private static String text(JsonObject object, String field) {
        JsonElement value = object.get(field);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }

    private static long number(JsonObject object, String field) {
        JsonElement value = object.get(field);
        return value == null || value.isJsonNull() ? 0 : value.getAsLong();
    }

    private static Optional<Instant> instant(String iso) {
        if (iso == null) return Optional.empty();
        try {
            return Optional.of(Instant.parse(iso));
        } catch (DateTimeParseException unreadable) {
            return Optional.empty();
        }
    }

    // -------------------------------------------------------------------------
    // HTTP
    // -------------------------------------------------------------------------

    private static HttpRequest get(String path, Duration timeout) {
        return HttpRequest.newBuilder(URI.create(LmStudioDefaults.SERVER_ROOT + path)).timeout(timeout).GET().build();
    }

    private static HttpRequest post(String path, JsonObject body, Duration timeout) {
        return HttpRequest.newBuilder(URI.create(LmStudioDefaults.SERVER_ROOT + path))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
    }

    private String send(HttpRequest request) throws InstallException {
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new InstallException("LM Studio did not answer " + request.uri() + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InstallException("Interrupted while waiting on LM Studio", e);
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new InstallException("LM Studio answered " + request.uri() + " with HTTP " + response.statusCode()
                    + ": " + response.body());
        }
        return response.body();
    }
}
