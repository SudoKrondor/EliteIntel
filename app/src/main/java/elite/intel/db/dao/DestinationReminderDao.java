package elite.intel.db.dao;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.ResultSet;
import java.sql.SQLException;

@RegisterRowMapper(DestinationReminderDao.DestinationMapper.class)
public interface DestinationReminderDao {


    @SqlUpdate("""
            INSERT OR REPLACE INTO destination_reminder (id, starSystem, reminder, stationName, contact)
            VALUES (1, :starSystem, :reminder, :stationName, :contact)
            """)
    void save(@BindBean Reminder data);

    @SqlQuery("SELECT * FROM destination_reminder WHERE id = 1")
    Reminder get();

    @SqlUpdate("DELETE FROM destination_reminder")
    void clear();

    class DestinationMapper implements RowMapper<Reminder> {

        @Override
        public Reminder map(ResultSet rs, StatementContext ctx) throws SQLException {
            Reminder destination = new Reminder();
            destination.setReminder(rs.getString("reminder"));
            destination.setStarSystem(rs.getString("starSystem"));
            destination.setStationName(rs.getString("stationName"));
            destination.setContact(rs.getString("contact"));
            return destination;
        }
    }


    class Reminder {
        public Reminder() {
        }

        private String starSystem;
        private String reminder;
        private String stationName;
        private String contact;

        /**
         * The spoken sentence. Prose for the voice, in the language it was created in.
         */
        public String getReminder() {
            return reminder;
        }

        public void setReminder(String reminder) {
            this.reminder = reminder;
        }

        public String getStarSystem() {
            return starSystem;
        }

        public void setStarSystem(String starSystem) {
            this.starSystem = starSystem;
        }

        /**
         * The port, when the reminder is for one; null for a reminder about a place.
         */
        public String getStationName() {
            return stationName;
        }

        public void setStationName(String stationName) {
            this.stationName = stationName;
        }

        /**
         * The {@code ReminderContact} constant name, or null. Stored as the enum name so the label can
         * be looked up in the reader's language rather than frozen at write time.
         */
        public String getContact() {
            return contact;
        }

        public void setContact(String contact) {
            this.contact = contact;
        }
    }
}
