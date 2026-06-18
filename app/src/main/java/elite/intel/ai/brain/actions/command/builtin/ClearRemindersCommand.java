package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.managers.MonetizeRouteManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.db.managers.TimedReminderManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.util.StringUtls;

/**
 * Owns its own execution: body migrated 1:1 from the legacy ClearReminderHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class ClearRemindersCommand implements IntelCommand {

    private final ReminderManager destinationReminder = ReminderManager.getInstance();
    private final MonetizeRouteManager monetizeRouteManager = MonetizeRouteManager.getInstance();

    @Override
    public String id() {
        return CommandIds.CLEAR_REMINDERS;
    }

    @Override
    public boolean ownsExecution() {
        return true;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        destinationReminder.clear();
        monetizeRouteManager.clear();
        TimedReminderManager.getInstance().clearAll();
        EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.reminder.cleared")));
    }
}
