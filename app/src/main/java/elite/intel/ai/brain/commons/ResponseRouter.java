package elite.intel.ai.brain.commons;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.AIConstants;
import elite.intel.ai.brain.AIRouterInterface;
import elite.intel.ai.brain.actions.CommandOutcome;
import elite.intel.ai.brain.actions.IntelAction;
import elite.intel.ai.brain.actions.command.builtin.IgnoreNonsensicalInputCommand;
import elite.intel.ai.brain.actions.handlers.CommandHandlerFactory;
import elite.intel.ai.brain.actions.handlers.QueryHandlerFactory;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.util.StringUtls;
import elite.intel.ws.WebSocketBroadcaster;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.Map;

import static elite.intel.ai.brain.commons.AiEndPoint.CONNECTION_CHECK_COMMAND;
import static elite.intel.util.json.JsonUtils.nullSaveJsonObject;


public class ResponseRouter implements AIRouterInterface {

    private static final Logger log = LogManager.getLogger(ResponseRouter.class);
    private static final ResponseRouter INSTANCE = new ResponseRouter();
    private final Map<String, IntelAction> commandHandlers;
    private final Map<String, IntelQuery> queryHandlers;
    private final SystemSession systemSession;
    private final WebSocketBroadcaster webSocketBroadcaster;
    private boolean dryRun = false;

    /**
     * When true the router publishes {@link HandlerDispatchedEvent} but skips handler execution.
     * Use from test harnesses only - default is false.
     */
    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    private ResponseRouter() {
        try {
            commandHandlers = CommandHandlerFactory.getInstance().registerCommandHandlers();
            queryHandlers = QueryHandlerFactory.getInstance().registerQueryHandlers();
            webSocketBroadcaster = WebSocketBroadcaster.getInstance();
            this.systemSession = SystemSession.getInstance();
        } catch (Exception e) {
            log.error("Failed to initialize ResponseRouter", e);
            throw new RuntimeException("ResponseRouter initialization failed", e);
        }
    }

    public static ResponseRouter getInstance() {
        return INSTANCE;
    }

    @Override public void processAiResponse(JsonObject jsonResponse, @Nullable String userInput) {
        if (jsonResponse == null) {
            log.error("Null LLM response received");
            return;
        }
        webSocketBroadcaster.broadcast(jsonResponse);
        try {
            String responseText = getAsStringOrEmpty(jsonResponse, AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE);
            String action = resolveActionId(getAsStringOrEmpty(jsonResponse, AIConstants.TYPE_ACTION));

            JsonObject params = getAsObjectOrEmpty(jsonResponse);

            // Only user-originated commands carry a started timer; sensor-driven
            // calls pass a null userInput and are not timed.
            if (userInput != null) {
                BrainTimer.stopAndLog(log, action);
            }

            if (!responseText.isEmpty() && action.isEmpty()) {
                GameEventBus.publish(new AiVoxResponseEvent(responseText));
                log.info("Response Sent to vocalization: {}", responseText);
                return;
            } else {
                systemSession.clearChatHistory();
            }

            String paramsForLogging = action + (params == null ? "" : " params " + params);
            if (systemSession.useLocalCommandLlm()) {
                UiBus.publish(new AppLogEvent("Local LLM Action: " + paramsForLogging));
            } else {
                UiBus.publish(new AppLogEvent("Cloud LLM Action: " + paramsForLogging));
            }

            if (getCommandHandlers().containsKey(action)) {
                handleCommand(action, params, responseText);
            } else if (getQueryHandlers().containsKey(action)) {
                handleQuery(action, params, userInput);
            } else if (!action.isEmpty()) {
                log.warn("Unknown action '{}' - LLM invented an action name not in registry", action);
                UiBus.publish(new AppLogEvent("Unknown action: " + action));
                log.warn("LLM Hallucinated action that does not exist." + action);
            } else {
                handleChat(responseText);
            }
        } catch (Exception e) {
            log.error("Failed to process LLM response: {}", e.getMessage(), e);
            GameEventBus.publish(new AiVoxResponseEvent("Error processing response."));
        } finally {
            UiBus.publish(new AppLogEvent(""));
        }
    }

    private void handleQuery(String action, JsonObject params, String userInput) {
        IntelQuery handler = getQueryHandlers().get(action);
        if (handler == null) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent("infer query action"));
            return;
        }

        //AudioPlayer.getInstance().playBeep(AudioPlayer.BEEP_1);
        UiBus.publish(new AppLogEvent("Query handler: " + handler.getClass().getSimpleName()));
        if (action == null || action.isEmpty()) {
            GameEventBus.publish(new AiVoxResponseEvent("No query action found"));
        }

        try {
            GameEventBus.publish(new HandlerDispatchedEvent(action, handler.getClass().getSimpleName(), false));
            if (dryRun) return;
            JsonObject dataJson = handler.handle(action, params, userInput);
            if (dataJson == null) return;
            String responseTextToUse = dataJson.has(AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE) ? dataJson.get(AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE).getAsString() : "";
            if (responseTextToUse != null && !responseTextToUse.isEmpty()) {
                GameEventBus.publish(new AiVoxResponseEvent(responseTextToUse));
                log.info("Spoke final query response (action: {}): {}", action, responseTextToUse);
            }
        } catch (Exception e) {
            log.error("Query handling failed for action {}: {}", action, e.getMessage(), e);
            handleChat("Error processing request");
        }
    }


    /**
     * Resolves the LLM-returned action id to a registered handler id, tolerating benign echo
     * differences. Custom command keys may be non-ASCII (e.g. Cyrillic), and a small local model can
     * echo them in a different Unicode normalization form or letter case than the registry stored.
     * <p>
     * An exact match wins immediately. Otherwise the raw id is matched against the command and query
     * keys after Unicode (NFC) + case normalization; a single normalized match is adopted as the
     * canonical id. Ambiguous or absent matches return the raw id unchanged so genuinely invented
     * actions still fall through to the "unknown action" path. This deliberately does not do fuzzy /
     * edit-distance matching, which could silently route to the wrong command.
     */
    private String resolveActionId(String rawAction) {
        if (rawAction == null || rawAction.isEmpty()) {
            return rawAction;
        }
        if (getCommandHandlers().containsKey(rawAction) || getQueryHandlers().containsKey(rawAction)) {
            return rawAction;
        }
        String normalized = normalizeId(rawAction);
        String match = matchNormalized(normalized, getCommandHandlers().keySet());
        if (match == null) {
            match = matchNormalized(normalized, getQueryHandlers().keySet());
        }
        if (match != null && !match.equals(rawAction)) {
            log.debug("Resolved LLM action '{}' to registered id '{}' via normalization", rawAction, match);
        }
        return match != null ? match : rawAction;
    }

    /**
     * Returns the single key whose normalized form equals {@code normalized}, or null if none/ambiguous.
     */
    static String matchNormalized(String normalized, java.util.Set<String> keys) {
        String found = null;
        for (String key : keys) {
            if (normalizeId(key).equals(normalized)) {
                if (found != null) {
                    return null; // ambiguous - refuse to guess
                }
                found = key;
            }
        }
        return found;
    }

    static String normalizeId(String value) {
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFC)
                .trim()
                .toLowerCase(java.util.Locale.ROOT);
    }

    protected Map<String, IntelAction> getCommandHandlers() {
        return commandHandlers;
    }

    protected Map<String, IntelQuery> getQueryHandlers() {
        return queryHandlers;
    }

    /**
     * Executes a GUI-selected command through the same post-LLM command dispatch path.
     * <p>
     * This intentionally bypasses STT and LLM classification: the UI already provides
     * the resolved action id and any explicit params collected from the commander.
     */
    public void executeCommandFromGUI(String action, JsonObject params) {
        executeCommandFromGUI(action, params, true);
    }

    /**
     * Executes a GUI-selected command and optionally suppresses the standard affirmative voice preamble.
     */
    public void executeCommandFromGUI(String action, JsonObject params, boolean speakAffirmation) {
        handleCommand(action, params == null ? new JsonObject() : params, "", speakAffirmation);
    }


    protected void handleChat(String responseText) {
        if (!responseText.isEmpty()) {
            GameEventBus.publish(new AiVoxResponseEvent(responseText));
            log.info("Sent to VoiceGenerator: {}", responseText);
        }
    }


    protected void handleCommand(String action, JsonObject params, String responseText) {
        handleCommand(action, params, responseText, true);
    }

    private void handleCommand(String action, JsonObject params, String responseText, boolean speakAffirmation) {
        log.info("Command dispatch: action=[{}] params=[{}]", action, params);
        UiBus.publish(new AppLogEvent("Processing action: " + action + " with params: " + params.toString()));
        if (IgnoreNonsensicalInputCommand.ID.equalsIgnoreCase(action)) {
            /// do nothing and return.
            return;
        }

        if (speakAffirmation && !CONNECTION_CHECK_COMMAND.equalsIgnoreCase(action)) {
            GameEventBus.publish(new AiVoxResponseEvent("%s".formatted(StringUtls.affirmative())));
        }

        IntelAction handler = getCommandHandlers().get(action);
        if (handler == null) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent("command not found"));
            return;
        }

        UiBus.publish(new AppLogEvent("Command handler: " + handler.getClass().getSimpleName()));
        new Thread(() -> {
            try {
                GameEventBus.publish(new HandlerDispatchedEvent(action, handler.getClass().getSimpleName(), true));
                if (dryRun) return;
                speakOutcome(handler.handle(action, params, responseText));
            } catch (Exception e) {
                GameEventBus.publish(new AiVoxResponseEvent("Error processing command for action " + action + " see logs."));
                log.error("Command handling failed for action {}: {}", action, e.getMessage(), e);
            }
        }).start();
        log.debug("Handled command action: {}", action);
    }

    /**
     * Renders a command's returned {@link CommandOutcome} to voice: a normal outcome to an interruptible
     * {@link AiVoxResponseEvent}, a critical one to a {@link MissionCriticalAnnouncementEvent}. Additive
     * and safe during the handler migration: a not-yet-migrated command still self-narrates and returns
     * {@code null} (or an outcome with no spoken text), which speaks nothing here - no double narration.
     */
    private void speakOutcome(JsonObject outcome) {
        String text = CommandOutcome.spokenText(outcome);
        if (text.isEmpty()) {
            return;
        }
        if (CommandOutcome.isCritical(outcome)) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(text));
        } else {
            GameEventBus.publish(new AiVoxResponseEvent(text));
        }
        log.info("Spoke command outcome: {}", text);
    }


    protected String getAsStringOrEmpty(JsonObject obj, String key) {
        if (obj == null || key == null) return "";
        if (!obj.has(key)) return "";
        var el = obj.get(key);
        if (el == null || el.isJsonNull()) return "";
        if (el.isJsonPrimitive()) {
            try {
                return el.getAsString();
            } catch (UnsupportedOperationException ignored) {
                // fallthrough
            }
        }
        log.debug("Expected string for key '{}' but got {}", key, el);
        return "";
    }

    protected JsonObject getAsObjectOrEmpty(JsonObject obj) {
        return nullSaveJsonObject(obj, AIConstants.PARAMS, log);
    }

}
