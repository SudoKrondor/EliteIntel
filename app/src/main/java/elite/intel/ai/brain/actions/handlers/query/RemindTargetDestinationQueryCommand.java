package elite.intel.ai.brain.actions.handlers.query;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.ai.brain.actions.query.RegisterQuery;

import com.google.gson.JsonObject;
import elite.intel.db.dao.DestinationReminderDao;
import elite.intel.db.managers.ReminderManager;


@RegisterQuery
public class RemindTargetDestinationQueryCommand extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = "query_reminder";


    @Override public String id() { return ID; }


    private final ReminderManager destinationReminder = ReminderManager.getInstance();

    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        DestinationReminderDao.Reminder reminder = destinationReminder.getReminder();
        return process(reminder == null ? "no reminders set" : reminder.getReminder());
    }
}