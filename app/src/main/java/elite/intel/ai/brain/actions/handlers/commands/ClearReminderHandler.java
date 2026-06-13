package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.managers.MonetizeRouteManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.db.managers.TimedReminderManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.util.StringUtls;

public class ClearReminderHandler implements CommandHandler {

    private final ReminderManager destinationReminder = ReminderManager.getInstance();
    private final MonetizeRouteManager monetizeRouteManager = MonetizeRouteManager.getInstance();

    @Override public void handle(String action, JsonObject params, String responseText) {
        destinationReminder.clear();
        monetizeRouteManager.clear();
        TimedReminderManager.getInstance().clearAll();
        EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.reminder.cleared")));
    }
}
