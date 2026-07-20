package elite.intel.ai.brain.actions.handlers.queries;

import com.google.gson.JsonObject;
import elite.intel.db.dao.DestinationReminderDao;
import elite.intel.db.managers.ReminderManager;


@RegisterQuery
public class RemindTargetDestinationQuery extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = "query_reminder";

    @Override
    public String llmDescription() {
        return "Report the commander's active reminder, especially a saved target or destination reminder.";
    }


    @Override public String id() { return ID; }


    private final ReminderManager destinationReminder = ReminderManager.getInstance();

    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        DestinationReminderDao.Reminder reminder = destinationReminder.getReminder();
        return process(reminder == null ? "no reminders set" : reminder.getReminder());
    }
}