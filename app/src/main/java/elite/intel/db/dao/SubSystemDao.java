package elite.intel.db.dao;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.Define;
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
     * Candidate names for fuzzy matching in one language. Rows with no translation yet fall back to
     * the English name so they remain reachable; for translated rows the English term is covered by
     * {@code FuzzySearch#fuzzySubSystemSearch}'s retry against {@link #getAllNamesLowerCase()}.
     */
    @SqlQuery("SELECT LOWER(COALESCE(<col>, subsystem)) FROM sub_system ORDER BY COALESCE(<col>, subsystem)")
    List<String> getAllLocalizedNamesLowerCase(@Define("col") String col);

    /**
     * Resolves a localized subsystem name back to the canonical English name, which is what
     * {@link #getMachineKeyBySubsystem(String)} and the targeting cycle are keyed on.
     */
    @SqlQuery("SELECT subsystem FROM sub_system WHERE LOWER(COALESCE(<col>, subsystem)) = LOWER(:localizedName) LIMIT 1")
    String getEnglishByLocalizedName(@Define("col") String col, @Bind("localizedName") String localizedName);

    /**
     * The display name to speak for a canonical English subsystem name, falling back to English
     * when that language has no translation for the row.
     */
    @SqlQuery("SELECT COALESCE(<col>, subsystem) FROM sub_system WHERE LOWER(subsystem) = LOWER(:subsystem) LIMIT 1")
    String getLocalizedLabel(@Define("col") String col, @Bind("subsystem") String subsystem);

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
