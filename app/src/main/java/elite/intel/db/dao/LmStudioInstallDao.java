package elite.intel.db.dao;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * The LM Studio the app installed on this PC: at most one row, at {@code id = 1}, in the table
 * {@code 12022__lm_studio_install.sql} creates. No row means the app installed nothing.
 * <p>
 * Callers go through {@link elite.intel.db.managers.LmStudioInstallManager} rather than using this directly.
 */
@RegisterRowMapper(LmStudioInstallDao.LmStudioInstallMapper.class)
public interface LmStudioInstallDao {

    /**
     * The row, or {@code null} when the app has installed nothing.
     */
    @SqlQuery("SELECT * FROM lm_studio_install WHERE id = 1")
    LmStudioInstallRow find();

    /**
     * Fails on the primary key when a row already exists: there is one install record at most.
     */
    @SqlUpdate("""
            INSERT INTO lm_studio_install (id, install_id, home_dir, pointer_written, setup_complete, installed_at)
            VALUES (1, :installId, :homeDir, :pointerWritten, :setupComplete, :installedAt)
            """)
    void insert(@Bind("installId") String installId,
                @Bind("homeDir") String homeDir,
                @Bind("pointerWritten") boolean pointerWritten,
                @Bind("setupComplete") boolean setupComplete,
                @Bind("installedAt") String installedAt);

    /**
     * @return the number of rows changed, 0 when there is no record
     */
    @SqlUpdate("UPDATE lm_studio_install SET setup_complete = 1 WHERE id = 1")
    int markSetupComplete();

    @SqlUpdate("DELETE FROM lm_studio_install")
    void delete();

    record LmStudioInstallRow(String installId, String homeDir, boolean pointerWritten, boolean setupComplete,
                              String installedAt) {
    }

    class LmStudioInstallMapper implements RowMapper<LmStudioInstallRow> {
        @Override
        public LmStudioInstallRow map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new LmStudioInstallRow(
                    rs.getString("install_id"),
                    rs.getString("home_dir"),
                    rs.getBoolean("pointer_written"),
                    rs.getBoolean("setup_complete"),
                    rs.getString("installed_at"));
        }
    }
}
