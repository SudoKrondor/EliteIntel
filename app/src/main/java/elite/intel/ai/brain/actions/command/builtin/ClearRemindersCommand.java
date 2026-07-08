package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.managers.MonetizeRouteManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.db.managers.TimedReminderManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

/**
 * Owns its own execution: body migrated 1:1 from the legacy ClearReminderHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class ClearRemindersCommand implements IntelCommand {
    public static final String ID = "clear_reminders";

    @Override public String llmDescription() { return "Use this when the commander asks to clear, delete, remove, or forget all active reminders."; }


    private final ReminderManager destinationReminder = ReminderManager.getInstance();
    private final MonetizeRouteManager monetizeRouteManager = MonetizeRouteManager.getInstance();

    @Override
    public String id() {
        return ID;
    }

    /** App-side bookkeeping (no game input); executable in any location. */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        destinationReminder.clear();
        monetizeRouteManager.clear();
        TimedReminderManager.getInstance().clearAll();
        GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.reminder.cleared")));
    }
}
