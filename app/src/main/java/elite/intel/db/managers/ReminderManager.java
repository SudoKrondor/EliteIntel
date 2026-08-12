package elite.intel.db.managers;

import elite.intel.db.dao.DestinationReminderDao;
import elite.intel.db.util.Database;
import elite.intel.gameapi.ReminderContact;

public final class ReminderManager {
    private static volatile ReminderManager instance;

    private ReminderManager() {
    }

    public static ReminderManager getInstance() {
        ReminderManager result = instance;
        if (result == null) {
            synchronized (ReminderManager.class) {
                result = instance;
                if (result == null) {
                    instance = result = new ReminderManager();
                }
            }
        }
        return result;
    }


    public DestinationReminderDao.Reminder getReminder() {
        return Database.withDao(DestinationReminderDao.class, DestinationReminderDao::get);
    }

    /**
     * A reminder about a place: a system and the sentence to speak. Use
     * {@link #setReminder(String, String, String, ReminderContact)} whenever the port and who to see
     * there are known — the HUD overlay shows them as an objective card, and it cannot take them out
     * of the sentence.
     */
    public void setReminder(String text, String starSystem) {
        setReminder(text, starSystem, null, null);
    }

    /**
     * A reminder about a contact at a port.
     *
     * @param text        the sentence to speak, already localized
     * @param starSystem  destination system
     * @param stationName destination port, or null when there is not one
     * @param contact     who to see there, or null when the reminder is for a place
     */
    public void setReminder(String text, String starSystem, String stationName, ReminderContact contact) {
        Database.withDao(DestinationReminderDao.class, dao -> {
            DestinationReminderDao.Reminder data = new DestinationReminderDao.Reminder();
            data.setStarSystem(starSystem);
            data.setReminder(text);
            data.setStationName(stationName);
            data.setContact(ReminderContact.nameOrNull(contact));
            dao.save(data);
            return null;
        });
    }

    public void clear() {
        Database.withDao(DestinationReminderDao.class, dao -> {
            dao.clear();
            return null;
        });
    }

}
