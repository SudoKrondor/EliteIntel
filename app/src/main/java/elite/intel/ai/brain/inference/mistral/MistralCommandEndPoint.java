package elite.intel.ai.brain.inference.mistral;

import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import elite.intel.ai.brain.AIConstants;
import elite.intel.ai.brain.AiCommandInterface;
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

import static elite.intel.util.json.JsonUtils.getAsStringOrEmpty;
import static org.apache.logging.log4j.util.Strings.trimToNull;

public class MistralCommandEndPoint extends CommandEndPoint implements AiCommandInterface {
    private static final Logger log = LogManager.getLogger(MistralCommandEndPoint.class);
    private static MistralCommandEndPoint instance;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ExecutorService executor;

    private MistralCommandEndPoint() {
    }

    public static MistralCommandEndPoint getInstance() {
        if (instance == null) {
            instance = new MistralCommandEndPoint();
        }
        return instance;
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            this.executor = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "MistralCommandEndPoint-Worker");
                t.setDaemon(true);
                return t;
            });
            GameEventBus.register(this);
            log.info("MistralCommandEndPoint started");
        } else {
            log.debug("MistralCommandEndPoint already started");
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            GameEventBus.unregister(this);
            if (executor != null) {
                MistralClient.getInstance().cancelCurrentRequest();
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
            log.info("MistralCommandEndPoint stopped");
        } else {
            log.debug("MistralCommandEndPoint already stopped");
        }
    }

    @Subscribe
    @Override
    public void onUserInput(UserInputEvent event) {
        if (!running.get()) {
            log.debug("Ignoring onUserInput: endpoint not running");
            return;
        }
        if (executor == null) {
            log.warn("Executor is null; running onUserInput on caller thread");
            processVoiceCommand(event.getUserInput());
            return;
        }
        executor.submit(() -> {
            try {
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

        JsonObject apiResponse = MistralChatEndPoint.getInstance().processAiPrompt(messages, 0.01f);
        if (apiResponse == null) {
            getRouter().processAiResponse(createError("Sorry, I couldn't process that."), userInput);
            return;
        }

        log.info("Processing Action: {}", getAsStringOrEmpty(apiResponse, AIConstants.TYPE_ACTION));
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
                JsonObject apiResponse = callMistralApi(messages);
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

    private JsonObject callMistralApi(JsonArray messages) {
        try {
            MistralClient client = MistralClient.getInstance();
            JsonObject prompt = client.createPrompt(MistralClient.MODEL, 1.00f);
            prompt.add("messages", messages);
            String jsonString = GsonFactory.getGson().toJson(prompt);

            JsonObject response = processAiPrompt(jsonString, client);

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

            log.debug("Raw API content:\n{}", content);

            String jsonContent = extractJsonFromContent(content);
            if (jsonContent == null) {
                log.error("Could not extract valid JSON from content:\n{}", content);
                return client.createErrorResponse("Could not extract JSON from model response");
            }

            log.debug("Extracted JSON:\n{}", jsonContent);

            return JsonParser.parseString(jsonContent).getAsJsonObject();

        } catch (IOException e) {
            log.error("Mistral API call failed: {}", e.getMessage(), e);
            String aiServerError = serverErrorMessage(e);
            return MistralClient.getInstance().createErrorResponse("API call failed: " + aiServerError);
        } catch (JsonSyntaxException e) {
            log.error("JSON parse failed after extraction", e);
            return MistralClient.getInstance().createErrorResponse("Invalid JSON structure from model");
        }
    }

    private String serverErrorMessage(Exception e) {
        String msg = e.getMessage();
        if (msg.contains("500") || msg.contains("402") || msg.contains("401")) {
            return "AI Internal server crash.";
        }
        if (msg.contains("404")) {
            return "AI API URL is invalid";
        }
        if (msg.contains("403")) {
            return "AI API key is invalid, or not authorized for this endpoint.";
        }
        return msg;
    }
}
