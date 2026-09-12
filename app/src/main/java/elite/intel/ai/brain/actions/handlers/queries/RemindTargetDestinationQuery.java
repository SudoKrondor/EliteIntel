package elite.intel.ai.brain.actions.handlers.queries;

import com.google.gson.JsonObject;
import elite.intel.db.dao.DestinationReminderDao;
import elite.intel.db.managers.ReminderManager;


@RegisterQuery
public class RemindTargetDestinationQuery extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = "query_reminder";

    @Override
    public String llmDescription() {
        return "Report the commander's active reminder: the errand they saved for themselves - where to travel, "
                + "what to buy or pick up there, where to sell it. Use this for any question about the current "
                + "errand or plan.";
    }


    @Override public String id() { return ID; }


    private final ReminderManager destinationReminder = ReminderManager.getInstance();

    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        DestinationReminderDao.Reminder reminder = destinationReminder.getReminder();
        if (reminder == null) {
            return process("no reminders set");
        }
        // The stored sentence is the errand as it is read on arrival, so it names neither the system nor
        // (always) the port; asked before arrival, the commander wants both.
        String port = reminder.getStationName();
        return process("Destination system: " + reminder.getStarSystem()
                + (port == null || port.isBlank() ? "" : ". Port: " + port)
                + ". On arrival: " + reminder.getReminder());
    }
}