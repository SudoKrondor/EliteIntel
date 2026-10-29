package elite.intel.db.dao;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * BindForge's settings row. One row, at {@code id = 1}, created by {@code 12000__bindforge_settings.sql}.
 * <p>
 * Callers go through {@link elite.intel.db.managers.BindForgeSettingsManager} rather than using this directly.
 */
@RegisterRowMapper(BindForgeSettingsDao.BindForgeSettingsMapper.class)
public interface BindForgeSettingsDao {

    @SqlQuery("SELECT * FROM bindforge_settings WHERE id = 1")
    BindForgeSettings get();

    // WHY: every column is listed. INSERT OR REPLACE writes a whole row, so a column left out of this
    // statement is silently reset to its default on the next save - which has already happened once in
    // global_settings, where two columns are missing. BindForgeSettingsRoundTripTest is what keeps this honest.
    @SqlUpdate("""
            INSERT OR REPLACE INTO bindforge_settings (id,
                                                       backup_destination,
                                                       daily_backup_retention_days,
                                                       before_changes_backups_kept)
            VALUES (1,
                    :backupDestination,
                    :dailyBackupRetentionDays,
                    :beforeChangesBackupsKept)
            """)
    void save(@BindBean BindForgeSettings settings);


    class BindForgeSettingsMapper implements RowMapper<BindForgeSettings> {
        @Override
        public BindForgeSettings map(ResultSet rs, StatementContext ctx) throws SQLException {
            BindForgeSettings entity = new BindForgeSettings();
            entity.setBackupDestination(rs.getString("backup_destination"));
            entity.setDailyBackupRetentionDays(rs.getInt("daily_backup_retention_days"));
            entity.setBeforeChangesBackupsKept(rs.getInt("before_changes_backups_kept"));
            return entity;
        }
    }


    class BindForgeSettings {
        String backupDestination;
        int dailyBackupRetentionDays;
        int beforeChangesBackupsKept;

        /**
         * Where backups are written, or {@code null} to use Elite-Intel's default backup path.
         */
        public String getBackupDestination() {
            return backupDestination;
        }

        public void setBackupDestination(String backupDestination) {
            this.backupDestination = backupDestination;
        }

        /**
         * How many days a daily backup is kept before retention deletes it.
         */
        public int getDailyBackupRetentionDays() {
            return dailyBackupRetentionDays;
        }

        public void setDailyBackupRetentionDays(int dailyBackupRetentionDays) {
            this.dailyBackupRetentionDays = dailyBackupRetentionDays;
        }

        /**
         * How many before-changes backups are kept, newest first - a count, not days.
         */
        public int getBeforeChangesBackupsKept() {
            return beforeChangesBackupsKept;
        }

        public void setBeforeChangesBackupsKept(int beforeChangesBackupsKept) {
            this.beforeChangesBackupsKept = beforeChangesBackupsKept;
        }
    }
}
