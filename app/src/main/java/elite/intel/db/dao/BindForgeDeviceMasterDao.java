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
 * The master device set - what the user owns, and what Apply writes out to every installation. Callers go
 * through {@link elite.intel.db.managers.BindForgeDeviceMasterManager} rather than using this directly.
 * <p>
 * Covers the labels table too, because a label has no life apart from its device: it is read, written and
 * deleted with the device it belongs to.
 */
@RegisterRowMapper(BindForgeDeviceMasterDao.DeviceRowMapper.class)
public interface BindForgeDeviceMasterDao {

    @SqlQuery("SELECT * FROM bindforge_device_master ORDER BY device_name")
    List<DeviceRow> findAll();

    @SqlQuery("SELECT * FROM bindforge_device_master WHERE device_name = :deviceName")
    DeviceRow findByName(@Bind("deviceName") String deviceName);

    /**
     * The master entry for a piece of hardware. Matched case-insensitively by the column collation, because
     * Frontier's file has no case convention and a VID read from hardware need not match the case a file used.
     */
    @SqlQuery("SELECT * FROM bindforge_device_master WHERE vid = :vid AND pid = :pid")
    DeviceRow findByHardware(@Bind("vid") String vid, @Bind("pid") String pid);

    @SqlUpdate("""
            INSERT INTO bindforge_device_master (device_name, vid, pid, alias_confirmed)
            VALUES (:deviceName, :vid, :pid, :aliasConfirmed)
            """)
    void insert(@Bind("deviceName") String deviceName,
                @Bind("vid") String vid,
                @Bind("pid") String pid,
                @Bind("aliasConfirmed") boolean aliasConfirmed);

    @SqlUpdate("UPDATE bindforge_device_master SET alias_confirmed = :confirmed WHERE id = :id")
    void setAliasConfirmed(@Bind("id") long id, @Bind("confirmed") boolean confirmed);

    /**
     * Renames a device, keeping the name it had so the old {@code .buttonMap} can be found and removed in
     * every installation afterwards.
     */
    @SqlUpdate("""
            UPDATE bindforge_device_master
            SET previous_name = device_name, device_name = :deviceName
            WHERE id = :id
            """)
    void rename(@Bind("id") long id, @Bind("deviceName") String deviceName);

    @SqlUpdate("UPDATE bindforge_device_master SET previous_name = NULL WHERE id = :id")
    void clearPreviousName(@Bind("id") long id);

    /**
     * Points an existing entry at different hardware - the one-field repair for a vendor firmware update,
     * which every binding follows because {@code .binds} references the name rather than the ID.
     */
    @SqlUpdate("UPDATE bindforge_device_master SET vid = :vid, pid = :pid WHERE id = :id")
    void retarget(@Bind("id") long id, @Bind("vid") String vid, @Bind("pid") String pid);

    @SqlUpdate("DELETE FROM bindforge_device_master WHERE id = :id")
    void delete(@Bind("id") long id);

    @SqlQuery("SELECT * FROM bindforge_device_labels WHERE device_id = :deviceId ORDER BY input_token")
    @RegisterRowMapper(LabelRowMapper.class)
    List<LabelRow> labelsOf(@Bind("deviceId") long deviceId);

    @SqlUpdate("""
            INSERT INTO bindforge_device_labels (device_id, input_token, label)
            VALUES (:deviceId, :inputToken, :label)
            ON CONFLICT (device_id, input_token) DO UPDATE SET label = excluded.label
            """)
    void putLabel(@Bind("deviceId") long deviceId,
                  @Bind("inputToken") String inputToken,
                  @Bind("label") String label);

    @SqlUpdate("DELETE FROM bindforge_device_labels WHERE device_id = :deviceId")
    void deleteLabels(@Bind("deviceId") long deviceId);


    class DeviceRowMapper implements RowMapper<DeviceRow> {
        @Override
        public DeviceRow map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new DeviceRow(
                    rs.getLong("id"),
                    rs.getString("device_name"),
                    rs.getString("vid"),
                    rs.getString("pid"),
                    rs.getBoolean("alias_confirmed"),
                    rs.getString("previous_name"));
        }
    }

    class LabelRowMapper implements RowMapper<LabelRow> {
        @Override
        public LabelRow map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new LabelRow(
                    rs.getLong("device_id"),
                    rs.getString("input_token"),
                    rs.getString("label"));
        }
    }

    /**
     * @param deviceName     the alias - the XML element tag, and the {@code .buttonMap} filename stem
     * @param aliasConfirmed whether the user has looked at this name, which gates button and axis naming
     * @param previousName   the name before a rename, for orphan cleanup, and null at every other time
     */
    record DeviceRow(long id, String deviceName, String vid, String pid, boolean aliasConfirmed,
                     String previousName) {
    }

    /**
     * @param inputToken a {@code .binds} input token - {@code Joy_1}, {@code Joy_XAxis}, {@code Joy_POV1Up}
     */
    record LabelRow(long deviceId, String inputToken, String label) {
    }
}
