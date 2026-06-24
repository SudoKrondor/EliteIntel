package elite.intel.ai.brain.inference.openai;

import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import elite.intel.ai.brain.AiCommandInterface;
import elite.intel.ai.brain.commons.BrainTimer;
import elite.intel.ai.brain.commons.CommandEndPoint;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.SensorDataEvent;
import elite.intel.gameapi.UserInputEvent;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.util.json.GsonFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.logging.log4j.util.Strings.trimToNull;

public class OpenAiCommandEndPoint extends CommandEndPoint implements AiCommandInterface {
    private static final Logger log = LogManager.getLogger(OpenAiCommandEndPoint.class);
    private static OpenAiCommandEndPoint instance;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ExecutorService executor;

    private OpenAiCommandEndPoint() {
        GameEventBus.register(this);
    }

    public static OpenAiCommandEndPoint getInstance() {
        if (instance == null) {
            instance = new OpenAiCommandEndPoint();
        }
        return instance;
    }

    private static String serverErrorMessage(IOException e) {
        String aiServerError = "AI Internal server error.";
        if (e.getMessage().contains("500") || e.getMessage().contains("402") || e.getMessage().contains("401")) {
            aiServerError = "AI Internal server crash.";
        }
        if (e.getMessage().contains("404")) {
            aiServerError = "AI API URL is invalid";
        }
        if (e.getMessage().contains("403")) {
            aiServerError = "AI API key is invalid, or not authorized for this endpoint.";
        }
        return aiServerError;
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            this.executor = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "OpenAiCommandEndPoint-Worker");
                t.setDaemon(true);
                return t;
            });
            GameEventBus.register(this);
            log.info("OpenAiCommandEndPoint started");
        } else {
            log.debug("OpenAiCommandEndPoint already started");
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            GameEventBus.unregister(this);
            if (executor != null) {
                OpenAiClient.getInstance().cancelCurrentRequest();
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException ie) {
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                } finally {
                    executor = null;
                }
            }
            log.info("OpenAiCommandEndPoint stopped");
        } else {
            log.debug("OpenAiCommandEndPoint already stopped");
        }
    }

    @Subscribe
    @Override
    public void onUserInput(UserInputEvent event) {
        if (!running.get()) {
            log.debug("Ignoring onUserInput: endpoint not running");
            return;
        }
        long entryNanos = System.nanoTime();
        if (executor == null) {
            log.warn("Executor is null; running onUserInput on caller thread");
            BrainTimer.start(entryNanos);
            processVoiceCommand(event.getUserInput());
            return;
        }
        executor.submit(() -> {
            try {
                BrainTimer.start(entryNanos);
                processVoiceCommand(event.getUserInput());
            } catch (Exception e) {
                log.error("Error processing user input", e);
            }
        });
    }

    private void processVoiceCommand(String userInput) {
        if (userInput == null || userInput.isEmpty()) {
            getRouter().processAiResponse(createError("Sorry, I couldn't process that."), userInput);
            return;
        }

        log.info("Sanitized voice userInput:\n{}", userInput);

        if (tryProcessExactCustomCommandCommand(userInput)) {
            return;
        }

        JsonArray messages = buildVoiceCommandMessages(userInput);

        JsonObject apiResponse = callOpenAiApi(messages);
        if (apiResponse == null) {
            getRouter().processAiResponse(createError("Sorry, I couldn't process that."), userInput);
            return;
        }

        getRouter().processAiResponse(apiResponse, userInput);
    }

    @Subscribe
    @Override
    public void onSensorDataEvent(SensorDataEvent event) {
        if (!running.get()) return;
        if (trimToNull(event.getSensorData()) == null) return;

        UiBus.publish(new AppLogEvent("Processing Sensor event"));
        JsonArray messages = buildSensorMessages(event);

        executor.submit(() -> {
            try {
                JsonObject apiResponse = callOpenAiApi(messages);
                if (apiResponse == null) {
                    GameEventBus.publish(new AiVoxResponseEvent("Failure processing system request. Check programming"));
                    return;
                }
                getRouter().processAiResponse(apiResponse, null);
            } catch (JsonSyntaxException e) {
                log.error("JSON parsing failed for sensor event", e);
                throw e;
            }
        });
    }

    private JsonObject callOpenAiApi(JsonArray messages) {
        try {
            OpenAiClient client = OpenAiClient.getInstance();
            JsonObject prompt = client.createPrompt(OpenAiClient.MODEL_GPT, 0.10f);
            prompt.add("messages", messages);
            String jsonString = GsonFactory.getGson().toJson(prompt);
            JsonObject response = processAiPrompt(jsonString, client);

            // Extract content
            JsonArray choices = response.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) {
                log.error("No choices in API response:\n{}", response);
                return client.createErrorResponse("No choices in API response");
            }

            JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
            if (message == null) {
                log.error("No message in API response choices:\n{}", response);
                return client.createErrorResponse("No message in API response");
            }

            String content = message.get("content").getAsString();
            if (content == null) {
                log.error("No content in API response message:\n{}", response);
                return client.createErrorResponse("No content in API response");
            }

            log.debug("API response content:\n{}", content);

            String jsonContent = extractJsonFromContent(content);
            if (jsonContent == null) {
                log.error("Could not extract JSON from content:\n{}", content);
                return client.createErrorResponse("No JSON object in content");
            }

            log.debug("Extracted JSON content:\n\n{}\n\n", jsonContent);

            try {
                return JsonParser.parseString(jsonContent).getAsJsonObject();
            } catch (JsonSyntaxException e) {
                log.error("Failed to parse API response content:\n{}", jsonContent, e);
                return client.createErrorResponse("Failed to parse API content");
            }
        } catch (IOException e) {
            log.error("Open AI API call failed: {}", e.getMessage(), e);
            String aiServerError = serverErrorMessage(e);
            return OpenAiClient.getInstance().createErrorResponse("API call failed: " + aiServerError);
        }
    }
}
