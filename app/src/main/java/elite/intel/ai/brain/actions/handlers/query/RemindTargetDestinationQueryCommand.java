package elite.intel.ai.brain.actions.handlers.query;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.ai.brain.actions.query.QueryIds;
import elite.intel.ai.brain.actions.query.RegisterQuery;

import com.google.gson.JsonObject;
import elite.intel.db.dao.DestinationReminderDao;
import elite.intel.db.managers.ReminderManager;


@RegisterQuery
public class RemindTargetDestinationQueryCommand extends BaseQueryAnalyzer implements IntelQuery {

    @Override public String id() { return QueryIds.REMINDER; }


    private final ReminderManager destinationReminder = ReminderManager.getInstance();

    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        DestinationReminderDao.Reminder reminder = destinationReminder.getReminder();
        return process(reminder == null ? "no reminders set" : reminder.getReminder());
    }
}