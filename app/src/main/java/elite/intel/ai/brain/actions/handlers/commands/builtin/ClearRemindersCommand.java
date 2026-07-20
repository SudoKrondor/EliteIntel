package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.db.managers.MonetizeRouteManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.db.managers.TimedReminderManager;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

/**
 * Owns its own execution: body migrated 1:1 from the legacy ClearReminderHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class ClearRemindersCommand implements IntelCommand {
    public static final String ID = "clear_reminders";

    @Override
    public String llmDescription() {
        return "Delete every reminder: the saved destination/target reminder, all timed reminders, and the monetize-route reminder.";
    }


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
    public String execute(JsonObject params, String responseText) {
        destinationReminder.clear();
        monetizeRouteManager.clear();
        TimedReminderManager.getInstance().clearAll();
        return StringUtls.localizedLlm("handler.reminder.cleared");
    }
}
