package elite.intel.ai.brain.inference.gemini;

import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static elite.intel.util.json.JsonUtils.getAsStringOrEmpty;
import static org.apache.logging.log4j.util.Strings.trimToNull;

public class GeminiCommandEndPoint extends CommandEndPoint implements AiCommandInterface {
    private static final Logger log = LogManager.getLogger(GeminiCommandEndPoint.class);
    private static GeminiCommandEndPoint instance;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ExecutorService executor;

    private GeminiCommandEndPoint() {
    }

    public static GeminiCommandEndPoint getInstance() {
        if (instance == null) {
            instance = new GeminiCommandEndPoint();
        }
        return instance;
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            this.executor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "GeminiCommandEndPoint-Worker");
                t.setDaemon(true);
                return t;
            });
            GameEventBus.register(this);
            log.info("GeminiCommandEndPoint started");
        } else {
            log.debug("GeminiCommandEndPoint already started");
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            GameEventBus.unregister(this);
            if (executor != null) {
                GeminiClient.getInstance().cancelCurrentRequest();
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException ie) {
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                } finally {
                    executor = null;
                }
            }
            log.info("GeminiCommandEndPoint stopped");
        } else {
            log.debug("GeminiCommandEndPoint already stopped");
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

        JsonObject apiResponse = GeminiChatEndPoint.getInstance().processAiPrompt(messages, 0.01f);
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
                JsonObject apiResponse = GeminiChatEndPoint.getInstance().processAiPrompt(messages, 0.01f);
                if (apiResponse == null) {
                    GameEventBus.publish(new AiVoxResponseEvent("Failure processing system request. Check programming"));
                    return;
                }
                getRouter().processAiResponse(apiResponse, null);
            } catch (JsonSyntaxException e) {
                log.error("JSON parsing failed for sensor event", e);
            }
        });
    }
}
