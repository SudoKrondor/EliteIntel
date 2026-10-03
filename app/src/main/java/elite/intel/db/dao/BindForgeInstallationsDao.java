package elite.intel.db.dao;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * The game installations BindForge knows about. Callers go through
 * {@link elite.intel.db.managers.BindForgeInstallationsManager} rather than using this directly.
 */
@RegisterRowMapper(BindForgeInstallationsDao.InstallationRowMapper.class)
public interface BindForgeInstallationsDao {

    @SqlQuery("SELECT * FROM bindforge_installations ORDER BY id")
    List<InstallationRow> findAll();

    @SqlQuery("SELECT * FROM bindforge_installations WHERE root_path = :rootPath")
    InstallationRow findByPath(@Bind("rootPath") String rootPath);

    @SqlUpdate("""
            INSERT INTO bindforge_installations (storefront, root_path, added_by_hand, is_missing)
            VALUES (:storefront, :rootPath, :addedByHand, :missing)
            """)
    void insert(@Bind("storefront") String storefront,
                @Bind("rootPath") String rootPath,
                @Bind("addedByHand") boolean addedByHand,
                @Bind("missing") boolean missing);

    @SqlUpdate("UPDATE bindforge_installations SET is_missing = :missing WHERE id = :id")
    void setMissing(@Bind("id") long id, @Bind("missing") boolean missing);

    /** Repoints a row at a new folder, keeping the row - and everything keyed to it - intact. */
    @SqlUpdate("UPDATE bindforge_installations SET root_path = :rootPath, is_missing = 0 WHERE id = :id")
    void relocate(@Bind("id") long id, @Bind("rootPath") String rootPath);

    @SqlUpdate("DELETE FROM bindforge_installations WHERE id = :id")
    void delete(@Bind("id") long id);


    class InstallationRowMapper implements RowMapper<InstallationRow> {
        @Override
        public InstallationRow map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new InstallationRow(
                    rs.getLong("id"),
                    rs.getString("storefront"),
                    rs.getString("root_path"),
                    rs.getBoolean("added_by_hand"),
                    rs.getBoolean("is_missing"));
        }
    }

    /**
     * @param id          what device records are keyed by, so it outlives a relocate
     * @param addedByHand true when the user added this rather than detection finding it
     * @param missing     true when the folder is not there right now - a state, never a reason to delete
     */
    record InstallationRow(long id, String storefront, String rootPath, boolean addedByHand, boolean missing) {
    }
}
