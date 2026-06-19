package elite.intel.ai.brain.commons;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.AIConstants;
import elite.intel.ai.brain.AIRouterInterface;
import elite.intel.ai.brain.actions.handlers.CommandHandlerFactory;
import elite.intel.ai.brain.actions.handlers.QueryHandlerFactory;
import elite.intel.ai.brain.actions.command.CommandHandler;
import elite.intel.ai.brain.actions.handlers.query.QueryHandler;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.gameapi.EventBusManager;
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
    private final Map<String, CommandHandler> commandHandlers;
    private final Map<String, QueryHandler> queryHandlers;
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
            String action = getAsStringOrEmpty(jsonResponse, AIConstants.TYPE_ACTION);

            JsonObject params = getAsObjectOrEmpty(jsonResponse);

            if (!responseText.isEmpty() && action.isEmpty()) {
                EventBusManager.publish(new AiVoxResponseEvent(responseText));
                log.info("Response Sent to vocalization: {}", responseText);
                return;
            } else {
                systemSession.clearChatHistory();
            }

            String paramsForLogging = action + (params == null ? "" : " params " + params);
            if (systemSession.useLocalCommandLlm()) {
                EventBusManager.publish(new AppLogEvent("Local LLM Action: " + paramsForLogging));
            } else {
                EventBusManager.publish(new AppLogEvent("Cloud LLM Action: " + paramsForLogging));
            }

            if (getCommandHandlers().containsKey(action)) {
                handleCommand(action, params, responseText);
            } else if (getQueryHandlers().containsKey(action)) {
                handleQuery(action, params, userInput);
            } else if (!action.isEmpty()) {
                log.warn("Unknown action '{}' - LLM invented an action name not in registry", action);
                EventBusManager.publish(new AppLogEvent("Unknown action: " + action));
                log.warn("LLM Hallucinated action that does not exist." + action);
            } else {
                handleChat(responseText);
            }
        } catch (Exception e) {
            log.error("Failed to process LLM response: {}", e.getMessage(), e);
            EventBusManager.publish(new AiVoxResponseEvent("Error processing response."));
        } finally {
            EventBusManager.publish(new AppLogEvent(""));
        }
    }

    private void handleQuery(String action, JsonObject params, String userInput) {
        QueryHandler handler = getQueryHandlers().get(action);
        if (handler == null) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent("infer query action"));
            return;
        }

        //AudioPlayer.getInstance().playBeep(AudioPlayer.BEEP_1);
        EventBusManager.publish(new AppLogEvent("Query handler: " + handler.getClass().getSimpleName()));
        if (action == null || action.isEmpty()) {
            EventBusManager.publish(new AiVoxResponseEvent("No query action found"));
        }

        try {
            EventBusManager.publish(new HandlerDispatchedEvent(action, handler.getClass().getSimpleName(), false));
            if (dryRun) return;
            JsonObject dataJson = handler.handle(action, params, userInput);
            if (dataJson == null) return;
            String responseTextToUse = dataJson.has(AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE) ? dataJson.get(AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE).getAsString() : "";
            if (responseTextToUse != null && !responseTextToUse.isEmpty()) {
                EventBusManager.publish(new AiVoxResponseEvent(responseTextToUse));
                log.info("Spoke final query response (action: {}): {}", action, responseTextToUse);
            }
        } catch (Exception e) {
            log.error("Query handling failed for action {}: {}", action, e.getMessage(), e);
            handleChat("Error processing request");
        }
    }


    protected Map<String, CommandHandler> getCommandHandlers() {
        return commandHandlers;
    }

    protected Map<String, QueryHandler> getQueryHandlers() {
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
            EventBusManager.publish(new AiVoxResponseEvent(responseText));
            log.info("Sent to VoiceGenerator: {}", responseText);
        }
    }


    protected void handleCommand(String action, JsonObject params, String responseText) {
        handleCommand(action, params, responseText, true);
    }

    private void handleCommand(String action, JsonObject params, String responseText, boolean speakAffirmation) {
        log.info("Command dispatch: action=[{}] params=[{}]", action, params);
        EventBusManager.publish(new AppLogEvent("Processing action: " + action + " with params: " + params.toString()));
        if (CommandIds.IGNORE_NONSENSICAL_INPUT.equalsIgnoreCase(action)) {
            /// do nothing and return.
            return;
        }

        if (speakAffirmation && !CONNECTION_CHECK_COMMAND.equalsIgnoreCase(action)) {
            EventBusManager.publish(new AiVoxResponseEvent("%s".formatted(StringUtls.affirmative())));
        }

        CommandHandler handler = getCommandHandlers().get(action);
        if (handler == null) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent("command not found"));
            return;
        }

        EventBusManager.publish(new AppLogEvent("Command handler: " + handler.getClass().getSimpleName()));
        new Thread(() -> {
            try {
                EventBusManager.publish(new HandlerDispatchedEvent(action, handler.getClass().getSimpleName(), true));
                if (dryRun) return;
                handler.handle(action, params, responseText);
            } catch (Exception e) {
                EventBusManager.publish(new AiVoxResponseEvent("Error processing command for action " + action + " see logs."));
                log.error("Command handling failed for action {}: {}", action, e.getMessage(), e);
            }
        }).start();
        log.debug("Handled command action: {}", action);
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
