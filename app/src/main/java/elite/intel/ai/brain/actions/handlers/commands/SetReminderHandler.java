package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.db.managers.ReminderManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.util.StringUtls;

public class SetReminderHandler implements CommandHandler {


    @Override public void handle(String action, JsonObject params, String responseText) {
        ReminderManager reminderManager = ReminderManager.getInstance();
        JsonElement key = params.get("key");
        if (key != null) {
            reminderManager.setReminder(
                    key.getAsString(),
                    null
            );
        } else {
            EventBusManager.publish(new AiVoxResponseEvent(StringUtls.localizedLlm("handler.reminder.noKey")));
        }
    }
}
