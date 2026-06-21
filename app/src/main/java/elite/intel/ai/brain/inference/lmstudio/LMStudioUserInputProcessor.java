package elite.intel.ai.brain.inference.lmstudio;

import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.AiCommandInterface;
import elite.intel.ai.brain.commons.CommandEndPoint;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.SensorDataEvent;
import elite.intel.gameapi.UserInputEvent;
import elite.intel.ui.event.AppLogEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.apache.logging.log4j.util.Strings.trimToNull;

public class LMStudioUserInputProcessor extends CommandEndPoint implements AiCommandInterface {

    private static final Logger log = LogManager.getLogger(LMStudioUserInputProcessor.class);
    private static final LMStudioUserInputProcessor INSTANCE = new LMStudioUserInputProcessor();
    private ExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private LMStudioUserInputProcessor() {
    }

    public static LMStudioUserInputProcessor getInstance() {
        return INSTANCE;
    }

    @Override
    public void start() {
        GameEventBus.register(this);
        if (running.compareAndSet(false, true)) {
            this.executor = newSingleThreadExecutor(
                    r -> {
                        Thread t = new Thread(r, "LMStudioCommand-Worker");
                        t.setDaemon(true);
                        return t;
                    });
            log.info("LMStudioUserInputProcessor started");
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            GameEventBus.unregister(this);
            if (executor != null) {
                executor.shutdownNow();
                executor = null;
            }
            log.info("LMStudioUserInputProcessor stopped");
        }
    }

    @Subscribe
    @Override
    public void onUserInput(UserInputEvent event) {
        if (!running.get()) return;
        if (executor == null) {
            processVoiceCommand(event.getUserInput());
            return;
        }
        executor.submit(() -> {
            {
                try {
                    processVoiceCommand(event.getUserInput());
                } catch (Exception e) {
                    UiBus.publish(new AppLogEvent("Error processing user input. See logs."));
                    log.error("Error processing user input", e);
                }
            }
        });
    }

    @SuppressWarnings("DuplicatedCode")
    private void processVoiceCommand(String userInput) {
        if (userInput == null || userInput.isEmpty()) {
            getRouter().processAiResponse(createError("Sorry, I couldn't process that."), userInput);
            return;
        }

        log.info("LM Studio voice input: {}", userInput);

        if (CONNECTION_CHECK_COMMAND.equalsIgnoreCase(userInput)) {
            JsonObject direct = new JsonObject();
            direct.addProperty("action", CONNECTION_CHECK_COMMAND);
            direct.add("params", new JsonObject());
            getRouter().processAiResponse(direct, userInput);
            return;
        }

        if (tryProcessExactCustomCommandCommand(userInput)) {
            return;
        }

        JsonArray request = buildVoiceCommandMessages(userInput);
        JsonObject response = LMStudioCommandEndPoint.getInstance().processAiPrompt(request, 0.01f);
        if (response == null) {
            getRouter().processAiResponse(createError("LLM did not answer."), userInput);
            return;
        }

        getRouter().processAiResponse(response, userInput);
    }

    @Subscribe
    @Override
    public void onSensorDataEvent(SensorDataEvent event) {
        if (!running.get()) return;
        if (trimToNull(event.getSensorData()) == null) return;
        UiBus.publish(new AppLogEvent("Processing Sensor event"));
        JsonObject response = LMStudioAnalysisEndpoint.getInstance().processSensor(event);
        getRouter().processAiResponse(response, "");
    }

}
