package elite.intel.junit.brain.command;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.builtin.ClearRemindersCommand;
import elite.intel.ai.brain.actions.command.builtin.SetReminderCommand;
import elite.intel.db.dao.DestinationReminderDao;
import elite.intel.db.managers.ReminderManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReminderCommandTest {

    private final SetReminderCommand setReminder = new SetReminderCommand();
    private final ClearRemindersCommand clearReminders = new ClearRemindersCommand();
    private final ReminderManager reminderManager = ReminderManager.getInstance();

    @BeforeEach
    void clearState() {
        reminderManager.clear();
    }

    // ── SetReminderCommand ─────────────────────────────────────────────────

    @Test
    void setReminderStoresTextInDb() {
        JsonObject params = new JsonObject();
        params.addProperty("key", "check fuel at next station");

        setReminder.execute(params, null);

        DestinationReminderDao.Reminder r = reminderManager.getReminder();
        assertNotNull(r);
        assertEquals("check fuel at next station", r.getReminder());
    }

    @Test
    void setReminderOverwritesPreviousReminder() {
        JsonObject first = new JsonObject();
        first.addProperty("key", "buy limpets");
        setReminder.execute(first, null);

        JsonObject second = new JsonObject();
        second.addProperty("key", "repair hull");
        setReminder.execute(second, null);

        assertEquals("repair hull", reminderManager.getReminder().getReminder());
    }

    @Test
    void missingKeyLeavesDbUnchanged() {
        // no "key" property → command publishes an event but must not write to DB
        setReminder.execute(new JsonObject(), null);

        assertNull(reminderManager.getReminder());
    }

    // ── ClearRemindersCommand ──────────────────────────────────────────────

    @Test
    void clearReminderErasesStoredReminder() {
        reminderManager.setReminder("dock at Jameson", "Sol");

        clearReminders.execute(new JsonObject(), null);

        assertNull(reminderManager.getReminder());
    }

    @Test
    void clearReminderIsIdempotentWhenAlreadyEmpty() {
        // should not throw when the table is already empty
        assertDoesNotThrow(() -> clearReminders.execute(new JsonObject(), null));
        assertNull(reminderManager.getReminder());
    }
}
