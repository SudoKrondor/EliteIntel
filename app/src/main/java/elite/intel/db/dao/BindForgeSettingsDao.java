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
                                                       auto_backup_on_launch,
                                                       backup_destination,
                                                       backup_retention_days,
                                                       edit_history_retention)
            VALUES (1,
                    :autoBackupOnLaunch,
                    :backupDestination,
                    :backupRetentionDays,
                    :editHistoryRetention)
            """)
    void save(@BindBean BindForgeSettings settings);


    class BindForgeSettingsMapper implements RowMapper<BindForgeSettings> {
        @Override
        public BindForgeSettings map(ResultSet rs, StatementContext ctx) throws SQLException {
            BindForgeSettings entity = new BindForgeSettings();
            entity.setAutoBackupOnLaunch(rs.getBoolean("auto_backup_on_launch"));
            entity.setBackupDestination(rs.getString("backup_destination"));
            entity.setBackupRetentionDays(rs.getInt("backup_retention_days"));
            entity.setEditHistoryRetention(rs.getInt("edit_history_retention"));
            return entity;
        }
    }


    class BindForgeSettings {
        boolean autoBackupOnLaunch;
        String backupDestination;
        int backupRetentionDays;
        int editHistoryRetention;

        public boolean isAutoBackupOnLaunch() {
            return autoBackupOnLaunch;
        }

        public void setAutoBackupOnLaunch(boolean onOff) {
            this.autoBackupOnLaunch = onOff;
        }

        /**
         * Where Player Backup archives are written, or {@code null} to use Elite-Intel's default backup path.
         */
        public String getBackupDestination() {
            return backupDestination;
        }

        public void setBackupDestination(String backupDestination) {
            this.backupDestination = backupDestination;
        }

        /**
         * How many days a Player Backup is kept before it is pruned. Distinct from
         * {@link #getEditHistoryRetention()}, which counts versions rather than days.
         */
        public int getBackupRetentionDays() {
            return backupRetentionDays;
        }

        public void setBackupRetentionDays(int backupRetentionDays) {
            this.backupRetentionDays = backupRetentionDays;
        }

        public int getEditHistoryRetention() {
            return editHistoryRetention;
        }

        public void setEditHistoryRetention(int editHistoryRetention) {
            this.editHistoryRetention = editHistoryRetention;
        }
    }
}
