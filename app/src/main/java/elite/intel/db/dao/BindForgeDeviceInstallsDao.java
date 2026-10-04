package elite.intel.db.dao;

import elite.intel.bindforge.devicefiles.Provenance;
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
 * What each installation's {@code DeviceMappings.xml} actually holds, read from disk rather than chosen by
 * the user. Callers go through {@link elite.intel.db.managers.BindForgeDeviceInstallsManager}.
 * <p>
 * These rows record reality and the master records intent. Drift is the difference between them, which is why
 * they are separate tables rather than one.
 */
@RegisterRowMapper(BindForgeDeviceInstallsDao.InstallDeviceRowMapper.class)
public interface BindForgeDeviceInstallsDao {

    @SqlQuery("SELECT * FROM bindforge_device_installs ORDER BY install_id, device_name")
    List<InstallDeviceRow> findAll();

    @SqlQuery("""
            SELECT * FROM bindforge_device_installs
            WHERE install_id = :installId
            ORDER BY device_name
            """)
    List<InstallDeviceRow> findByInstall(@Bind("installId") long installId);

    @SqlQuery("""
            SELECT * FROM bindforge_device_installs
            WHERE install_id = :installId AND device_name = :deviceName
            """)
    InstallDeviceRow findOne(@Bind("installId") long installId, @Bind("deviceName") String deviceName);

    /**
     * Records what was found on disk, replacing whatever was recorded before for that entry.
     * <p>
     * An upsert rather than an insert because a scan re-reads every installation from scratch, and what it
     * finds is the truth however many times that entry has been seen before.
     */
    @SqlUpdate("""
            INSERT INTO bindforge_device_installs
                (install_id, device_name, vid, pid, provenance, has_button_map)
            VALUES (:installId, :deviceName, :vid, :pid, :provenance, :hasButtonMap)
            ON CONFLICT (install_id, device_name) DO UPDATE SET
                vid = excluded.vid,
                pid = excluded.pid,
                provenance = excluded.provenance,
                has_button_map = excluded.has_button_map
            """)
    void record(@Bind("installId") long installId,
                @Bind("deviceName") String deviceName,
                @Bind("vid") String vid,
                @Bind("pid") String pid,
                @Bind("provenance") String provenance,
                @Bind("hasButtonMap") boolean hasButtonMap);
    // The provenance bind is the stored lowercase form - Provenance.stored(). It is a String here because
    // this is the SQL seam: JDBI would bind an enum by name(), which is uppercase and fails the CHECK.

    @SqlUpdate("""
            DELETE FROM bindforge_device_installs
            WHERE install_id = :installId AND device_name = :deviceName
            """)
    void delete(@Bind("installId") long installId, @Bind("deviceName") String deviceName);

    /** Clears an installation's rows, so a rescan records what is there now rather than merging with what was. */
    @SqlUpdate("DELETE FROM bindforge_device_installs WHERE install_id = :installId")
    void deleteForInstall(@Bind("installId") long installId);


    class InstallDeviceRowMapper implements RowMapper<InstallDeviceRow> {
        @Override
        public InstallDeviceRow map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new InstallDeviceRow(
                    rs.getLong("install_id"),
                    rs.getString("device_name"),
                    rs.getString("vid"),
                    rs.getString("pid"),
                    Provenance.fromStored(rs.getString("provenance")),
                    rs.getBoolean("has_button_map"));
        }
    }

    /**
     * @param deviceName   the element tag exactly as that installation's file spells it
     * @param provenance   whether BindForge may touch this entry
     * @param hasButtonMap whether a {@code .buttonMap} exists under this name in that installation
     */
    record InstallDeviceRow(long installId, String deviceName, String vid, String pid, Provenance provenance,
                            boolean hasButtonMap) {
    }
}
