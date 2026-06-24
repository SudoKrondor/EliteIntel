package elite.intel.ai.brain.inference.anthropic;

import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.AIConstants;
import elite.intel.ai.brain.AiCommandInterface;
import elite.intel.ai.brain.commons.BrainTimer;
import elite.intel.ai.brain.commons.CommandEndPoint;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.SensorDataEvent;
import elite.intel.gameapi.UserInputEvent;
import elite.intel.ui.event.AppLogEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.logging.log4j.util.Strings.trimToNull;

/**
 * Event-driven user input processor for the Anthropic (Claude) backend.
 * <p>
 * Intentionally mirrors OllamaUserInputProcessor structure so that the two
 * implementations stay easy to diff and maintain together. The only real
 * differences are:
 * - Uses AnthropicCommandEndPoint / AnthropicClient instead of Ollama equivalents
 * - Thread name is "AnthropicCommand-Worker"
 */
public class AnthropicCommandEndPoint extends CommandEndPoint implements AiCommandInterface {

    private static final Logger log = LogManager.getLogger(AnthropicCommandEndPoint.class);
    private static final AnthropicCommandEndPoint INSTANCE = new AnthropicCommandEndPoint();

    private ExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private AnthropicCommandEndPoint() {
    }

    @Override
    public AnthropicPromptFactory getContextFactory() {
        return AnthropicPromptFactory.getInstance();
    }

    public static AnthropicCommandEndPoint getInstance() {
        return INSTANCE;
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    @Override
    public void start() {
        GameEventBus.register(this);
        if (running.compareAndSet(false, true)) {
            this.executor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "AnthropicCommand-Worker");
                t.setDaemon(true);
                return t;
            });
            log.info("AnthropicUserInputProcessor started");
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
            log.info("AnthropicUserInputProcessor stopped");
        }
    }

    // -----------------------------------------------------------------------
    // Event subscribers
    // -----------------------------------------------------------------------

    @Subscribe
    @Override
    public void onUserInput(UserInputEvent event) {
        if (!running.get()) return;
        long entryNanos = System.nanoTime();
        if (executor == null) {
            BrainTimer.start(entryNanos);
            processVoiceCommand(event.getUserInput());
            return;
        }
        executor.submit(() -> {
            BrainTimer.start(entryNanos);
            processVoiceCommand(event.getUserInput());
        });
    }

    @Subscribe
    @Override
    public void onSensorDataEvent(SensorDataEvent event) {
        if (!running.get()) return;
        if (trimToNull(event.getSensorData()) == null) return;

        UiBus.publish(new AppLogEvent("Processing Sensor event"));

        JsonArray messages = new JsonArray();

        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", AIConstants.ROLE_SYSTEM);
        systemMessage.addProperty("content", getContextFactory().generateSensorPrompt());
        messages.add(systemMessage);

        JsonObject instructions = new JsonObject();
        instructions.addProperty("role", AIConstants.ROLE_SYSTEM);
        instructions.addProperty("content", "EVENT SPECIFIC INSTRUCTIONS: " + event.getInstructions());
        messages.add(instructions);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", AIConstants.ROLE_USER);
        userMsg.addProperty("content", event.getSensorData());
        messages.add(userMsg);

        // Low temperature for deterministic sensor-driven responses
        executor.submit(() -> {
            JsonObject response = AnthropicUserEndPoint.getInstance().processAiPrompt(messages, 0.1f);
            if (response != null) getRouter().processAiResponse(response, null);
        });
    }

    // -----------------------------------------------------------------------
    // Core processing
    // -----------------------------------------------------------------------

    private void processVoiceCommand(String userInput) {
        if (userInput == null || userInput.isEmpty()) {
            getRouter().processAiResponse(createError("Sorry, I couldn't process that."), userInput);
            return;
        }

        log.info("Anthropic voice input: {}", userInput);

        if (tryProcessExactCustomCommandCommand(userInput)) {
            return;
        }

        JsonArray request = new JsonArray();

        // Static rules block first (cached by AnthropicUserEndPoint), then the
        // Reducer-filtered action list (uncached - unique per voice input).
        JsonObject staticSystem = new JsonObject();
        staticSystem.addProperty("role", AIConstants.ROLE_SYSTEM);
        staticSystem.addProperty("content", getContextFactory().generateFullUserInputPromptForAnthropicCaching());
        request.add(staticSystem);

        JsonObject actionSystem = new JsonObject();
        actionSystem.addProperty("role", AIConstants.ROLE_SYSTEM);
        actionSystem.addProperty("content", getContextFactory().generateUserInputActionListForAnthropicPrompt(userInput));
        request.add(actionSystem);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", AIConstants.ROLE_USER);
        userMsg.addProperty("content", getContextFactory().normalizeInput(userInput));
        request.add(userMsg);

        // Low temperature for command classification accuracy
        JsonObject response = AnthropicUserEndPoint.getInstance().processAiPrompt(request, 0.1f);
        if (response == null) {
            getRouter().processAiResponse(createError("Sorry, I couldn't reach Claude."), userInput);
            return;
        }

        getRouter().processAiResponse(response, userInput);
    }

}
