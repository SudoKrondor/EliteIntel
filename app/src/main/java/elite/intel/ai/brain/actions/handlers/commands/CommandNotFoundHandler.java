package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.gameapi.EventBusManager;
import elite.intel.util.StringUtls;

public class CommandNotFoundHandler implements CommandHandler {

    @Override
    public void handle(String action, JsonObject params, String responseText) {
        EventBusManager.publish(new AiVoxResponseEvent(StringUtls.localizedLlm("handler.common.commandNotFound")));
    }
}
