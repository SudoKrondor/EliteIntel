package elite.intel.db.dao;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@RegisterRowMapper(SubSystemDao.SubSystemMapper.class)
public interface SubSystemDao {

    @SqlQuery("SELECT LOWER(subsystem) FROM sub_system ORDER BY subsystem")
    List<String> getAllNamesLowerCase();

    @SqlQuery("SELECT subsystem FROM sub_system WHERE LOWER(subsystem) = LOWER(:subsystem) LIMIT 1")
    String getOriginalCase(@Bind("subsystem") String subsystem);

    /**
     * Resolves the journal machine_key (e.g. "int_powerplant", "ext_drive", "hpt_beamlaser")
     * for a canonical subsystem name. machine_key is reliably present in the journal's raw
     * Subsystem field, unlike Subsystem_Localised, so targeting matches on it.
     */
    @SqlQuery("SELECT machine_key FROM sub_system WHERE LOWER(subsystem) = LOWER(:subsystem) AND machine_key IS NOT NULL LIMIT 1")
    String getMachineKeyBySubsystem(@Bind("subsystem") String subsystem);


    class SubSystemMapper implements RowMapper<SubSystem> {

        @Override public SubSystem map(ResultSet rs, StatementContext ctx) throws SQLException {
            SubSystem system = new SubSystem();
            system.setSubsystem(rs.getString("subsystem"));
            return system;
        }
    }

    class SubSystem {
        private String subsystem;

        public String getSubsystem() {
            return subsystem;
        }

        public void setSubsystem(String subsystem) {
            this.subsystem = subsystem;
        }
    }
}
