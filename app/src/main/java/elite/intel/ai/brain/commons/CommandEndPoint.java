package elite.intel.ai.brain.commons;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.ai.ApiFactory;
import elite.intel.ai.brain.AIConstants;
import elite.intel.ai.brain.AIRouterInterface;
import elite.intel.ai.brain.AiPromptFactory;
import elite.intel.ai.brain.AiActionsMap;
import elite.intel.ai.brain.InputNormalizer;
import elite.intel.ai.brain.actions.customcommand.CustomCommandRegistry;
import elite.intel.gameapi.SensorDataEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class CommandEndPoint extends AiEndPoint {

    private static final Logger log = LogManager.getLogger(CommandEndPoint.class);
    private final AiPromptFactory contextFactory;
    private final AIRouterInterface router;
    private final InputNormalizer inputNormalizer = InputNormalizer.getInstance();

    protected CommandEndPoint() {
        this.router = ApiFactory.getInstance().getAiRouter();
        this.contextFactory = ApiFactory.getInstance().getAiPromptFactory();
    }

    public AiPromptFactory getContextFactory() {
        return contextFactory;
    }

    public AIRouterInterface getRouter() {
        return router;
    }

    protected JsonObject createError(String text) {
        JsonObject err = new JsonObject();
        err.addProperty(AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE, text);
        return err;
    }

    /**
     * Dispatches an exact user custom command phrase before LLM classification.
     * <p>
     * User customCommands are explicit user-authored commands, so exact phrase matches should be deterministic.
     */
    protected boolean tryProcessExactCustomCommandCommand(String userInput) {
        String normalizedInput = normalizeForExactCustomCommandMatch(userInput);
        if (normalizedInput.isBlank()) {
            return false;
        }

        String action = AiActionsMap.getInstance().actionMap(false).get(normalizedInput);
        Set<String> customCommandIds = CustomCommandRegistry.getInstance().getCustomCommands().stream()
                .map(customCommand -> customCommand.getActionKey().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        if (action == null || !customCommandIds.contains(action.toLowerCase(Locale.ROOT))) {
            return false;
        }

        log.info("Exact custom command phrase matched STT input: [{}] -> {}", normalizedInput, action);
        JsonObject direct = new JsonObject();
        direct.addProperty(AIConstants.TYPE_ACTION, action);
        direct.add(AIConstants.PARAMS, new JsonObject());
        router.processAiResponse(direct, userInput);
        return true;
    }

    private String normalizeForExactCustomCommandMatch(String value) {
        String normalized = inputNormalizer.normalize(value == null ? "" : value);
        return normalized == null ? "" : normalized.trim().toLowerCase(Locale.ROOT);
    }

    protected JsonArray buildVoiceCommandMessages(String userInput) {
        JsonArray messages = new JsonArray();
        JsonObject system = new JsonObject();
        system.addProperty("role", AIConstants.ROLE_SYSTEM);
        system.addProperty("content", contextFactory.generateUserInputSystemPrompt(userInput));
        messages.add(system);
        JsonObject user = new JsonObject();
        user.addProperty("role", AIConstants.ROLE_USER);
        user.addProperty("content", contextFactory.normalizeInput(userInput));
        messages.add(user);
        return messages;
    }

    protected JsonArray buildSensorMessages(SensorDataEvent event) {
        JsonArray messages = new JsonArray();
        JsonObject system = new JsonObject();
        system.addProperty("role", AIConstants.ROLE_SYSTEM);
        system.addProperty("content", contextFactory.generateSensorPrompt());
        messages.add(system);
        JsonObject instructions = new JsonObject();
        instructions.addProperty("role", AIConstants.ROLE_SYSTEM);
        instructions.addProperty("content", event.getInstructions());
        messages.add(instructions);
        JsonObject user = new JsonObject();
        user.addProperty("role", AIConstants.ROLE_USER);
        user.addProperty("content", event.getSensorData());
        messages.add(user);
        return messages;
    }
}
