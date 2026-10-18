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
 * Bindings waiting to move from a controller's old hardware id to its name, from
 * {@code 12008__bindforge_hardware_id_move.sql} (Alan, 2026-10-17, A17). Callers go through
 * {@link elite.intel.db.managers.BindForgeHardwareIdMoveManager}.
 */
@RegisterRowMapper(BindForgeHardwareIdMoveDao.MoveRowMapper.class)
public interface BindForgeHardwareIdMoveDao {

    @SqlQuery("SELECT * FROM bindforge_hardware_id_move ORDER BY vid, pid")
    List<MoveRow> findAll();

    /** Takes the old id away from any other controller first - one old id is one controller's. */
    @SqlUpdate("DELETE FROM bindforge_hardware_id_move WHERE old_vid = :oldVid AND old_pid = :oldPid")
    void deleteByOldId(@Bind("oldVid") String oldVid, @Bind("oldPid") String oldPid);

    /** Records the old id for a controller, replacing the one recorded before for it. */
    @SqlUpdate("""
            INSERT INTO bindforge_hardware_id_move (vid, pid, old_vid, old_pid)
            VALUES (:vid, :pid, :oldVid, :oldPid)
            ON CONFLICT (vid, pid) DO UPDATE
                SET old_vid = excluded.old_vid,
                    old_pid = excluded.old_pid,
                    recorded_at = CURRENT_TIMESTAMP
            """)
    void put(@Bind("vid") String vid, @Bind("pid") String pid,
             @Bind("oldVid") String oldVid, @Bind("oldPid") String oldPid);

    @SqlUpdate("DELETE FROM bindforge_hardware_id_move WHERE vid = :vid AND pid = :pid")
    void delete(@Bind("vid") String vid, @Bind("pid") String pid);

    class MoveRowMapper implements RowMapper<MoveRow> {
        @Override
        public MoveRow map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new MoveRow(
                    rs.getString("vid"),
                    rs.getString("pid"),
                    rs.getString("old_vid"),
                    rs.getString("old_pid"));
        }
    }

    /**
     * @param vid    the controller's hardware now, as the master and the draft hold it - upper case
     * @param pid    as {@code vid}
     * @param oldVid the hardware {@code .binds} still names it by - upper case
     * @param oldPid as {@code oldVid}
     */
    record MoveRow(String vid, String pid, String oldVid, String oldPid) {
    }
}
