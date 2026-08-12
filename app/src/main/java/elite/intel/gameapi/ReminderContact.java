package elite.intel.gameapi;

/**
 * Who the commander is going to see at a reminder's destination.
 * <p>
 * An identifier, not a label: the constant name is what goes in the database, and the words shown to
 * the commander are looked up per language where they are drawn. Storing the label instead would
 * freeze a reminder in whatever language it was created in.
 * <p>
 * Absent ({@code null}) is normal — plenty of reminders are for a place rather than a contact, such
 * as a mining site or a stand of brain trees.
 */
public enum ReminderContact {

    MATERIAL_TRADER_RAW,
    MATERIAL_TRADER_MANUFACTURED,
    MATERIAL_TRADER_ENCODED,
    TECHNOLOGY_BROKER_HUMAN,
    TECHNOLOGY_BROKER_GUARDIAN,
    INTERSTELLAR_FACTORS,
    VISTA_GENOMICS;

    /**
     * The stored form. Null-safe so callers can pass an absent contact straight through.
     */
    public static String nameOrNull(ReminderContact contact) {
        return contact == null ? null : contact.name();
    }

    /**
     * Reads back a stored contact, tolerating anything unrecognised — a reminder written by a newer
     * build, or a column edited by hand, must not stop the rest of the reminder from being shown.
     */
    public static ReminderContact parseOrNull(String stored) {
        if (stored == null || stored.isBlank()) return null;
        try {
            return valueOf(stored.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
