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
 * The device draft - the user's saved but unapplied device edits. Callers go through
 * {@link elite.intel.db.managers.BindForgeDeviceDraftManager} rather than using this directly.
 * <p>
 * Also holds the statements that copy between the draft and the master, because both directions are the draft's
 * operations: starting one copies the master in, and applying one copies it back out.
 */
@RegisterRowMapper(BindForgeDeviceDraftDao.DraftRowMapper.class)
public interface BindForgeDeviceDraftDao {

    @SqlQuery("SELECT EXISTS (SELECT 1 FROM bindforge_device_draft_state)")
    boolean exists();

    @SqlUpdate("INSERT OR IGNORE INTO bindforge_device_draft_state (id) VALUES (1)")
    void markStarted();

    @SqlUpdate("DELETE FROM bindforge_device_draft_state")
    void clearStarted();

    @SqlQuery("SELECT * FROM bindforge_device_draft ORDER BY device_name")
    List<DraftRow> findAll();

    @SqlQuery("SELECT * FROM bindforge_device_draft WHERE device_name = :deviceName")
    DraftRow findByName(@Bind("deviceName") String deviceName);

    @SqlUpdate("""
            INSERT INTO bindforge_device_draft (master_id, device_name, vid, pid, alias_confirmed, previous_name)
            VALUES (:masterId, :deviceName, :vid, :pid, :aliasConfirmed, :previousName)
            """)
    void insert(@Bind("masterId") Long masterId,
                @Bind("deviceName") String deviceName,
                @Bind("vid") String vid,
                @Bind("pid") String pid,
                @Bind("aliasConfirmed") boolean aliasConfirmed,
                @Bind("previousName") String previousName);

    @SqlUpdate("UPDATE bindforge_device_draft SET alias_confirmed = :confirmed WHERE id = :id")
    void setAliasConfirmed(@Bind("id") long id, @Bind("confirmed") boolean confirmed);

    /**
     * Renames a draft device. The name kept is the one the installations hold, which is the master's: renaming
     * twice before an Apply must still find the {@code .buttonMap} under the first name. A device added in the
     * draft has no files anywhere yet, so it keeps no previous name.
     */
    @SqlUpdate("""
            UPDATE bindforge_device_draft
            SET previous_name = CASE WHEN master_id IS NULL THEN NULL ELSE COALESCE(previous_name, device_name) END,
                device_name   = :deviceName
            WHERE id = :id
            """)
    void rename(@Bind("id") long id, @Bind("deviceName") String deviceName);

    @SqlUpdate("UPDATE bindforge_device_draft SET vid = :vid, pid = :pid WHERE id = :id")
    void retarget(@Bind("id") long id, @Bind("vid") String vid, @Bind("pid") String pid);

    @SqlUpdate("DELETE FROM bindforge_device_draft WHERE id = :id")
    void delete(@Bind("id") long id);

    @SqlUpdate("DELETE FROM bindforge_device_draft WHERE master_id = :masterId")
    void deleteByMasterId(@Bind("masterId") long masterId);

    @SqlUpdate("DELETE FROM bindforge_device_draft")
    void deleteAll();

    @SqlQuery("SELECT * FROM bindforge_device_draft_labels WHERE device_id = :deviceId ORDER BY input_token")
    @RegisterRowMapper(BindForgeDeviceMasterDao.LabelRowMapper.class)
    List<BindForgeDeviceMasterDao.LabelRow> labelsOf(@Bind("deviceId") long deviceId);

    @SqlUpdate("""
            INSERT INTO bindforge_device_draft_labels (device_id, input_token, label)
            VALUES (:deviceId, :inputToken, :label)
            ON CONFLICT (device_id, input_token) DO UPDATE SET label = excluded.label
            """)
    void putLabel(@Bind("deviceId") long deviceId,
                  @Bind("inputToken") String inputToken,
                  @Bind("label") String label);

    @SqlUpdate("DELETE FROM bindforge_device_draft_labels WHERE device_id = :deviceId")
    void deleteLabels(@Bind("deviceId") long deviceId);

    // --- Copying between the draft and the master ---

    @SqlQuery("SELECT * FROM bindforge_device_master ORDER BY device_name")
    @RegisterRowMapper(BindForgeDeviceMasterDao.DeviceRowMapper.class)
    List<BindForgeDeviceMasterDao.DeviceRow> findAllMaster();

    @SqlQuery("SELECT * FROM bindforge_device_master WHERE id = :id")
    @RegisterRowMapper(BindForgeDeviceMasterDao.DeviceRowMapper.class)
    BindForgeDeviceMasterDao.DeviceRow findMasterById(@Bind("id") long id);

    @SqlQuery("SELECT * FROM bindforge_device_master WHERE device_name = :deviceName")
    @RegisterRowMapper(BindForgeDeviceMasterDao.DeviceRowMapper.class)
    BindForgeDeviceMasterDao.DeviceRow findMasterByName(@Bind("deviceName") String deviceName);

    @SqlUpdate("""
            INSERT INTO bindforge_device_draft_labels (device_id, input_token, label)
            SELECT :draftId, input_token, label FROM bindforge_device_labels WHERE device_id = :masterId
            """)
    void copyLabelsFromMaster(@Bind("draftId") long draftId, @Bind("masterId") long masterId);

    /** The master devices no draft row was copied from - what applying the draft removes from the master. */
    @SqlQuery("""
            SELECT device_name FROM bindforge_device_master
            WHERE id NOT IN (SELECT master_id FROM bindforge_device_draft WHERE master_id IS NOT NULL)
            ORDER BY device_name
            """)
    List<String> masterNamesNotInDraft();

    @SqlUpdate("DELETE FROM bindforge_device_master")
    void deleteAllMaster();

    @SqlUpdate("""
            INSERT INTO bindforge_device_master (device_name, vid, pid, alias_confirmed, previous_name)
            VALUES (:deviceName, :vid, :pid, :aliasConfirmed, :previousName)
            """)
    void insertMaster(@Bind("deviceName") String deviceName,
                      @Bind("vid") String vid,
                      @Bind("pid") String pid,
                      @Bind("aliasConfirmed") boolean aliasConfirmed,
                      @Bind("previousName") String previousName);

    @SqlUpdate("""
            INSERT INTO bindforge_device_labels (device_id, input_token, label)
            SELECT :masterId, input_token, label FROM bindforge_device_draft_labels WHERE device_id = :draftId
            """)
    void copyLabelsToMaster(@Bind("masterId") long masterId, @Bind("draftId") long draftId);


    class DraftRowMapper implements RowMapper<DraftRow> {
        @Override
        public DraftRow map(ResultSet rs, StatementContext ctx) throws SQLException {
            // WHY: wasNull() answers for the last column read, so it is asked straight after master_id.
            long read = rs.getLong("master_id");
            Long masterId = rs.wasNull() ? null : read;
            return new DraftRow(
                    rs.getLong("id"),
                    masterId,
                    rs.getString("device_name"),
                    rs.getString("vid"),
                    rs.getString("pid"),
                    rs.getBoolean("alias_confirmed"),
                    rs.getString("previous_name"));
        }
    }

    /**
     * @param masterId       the master row this was copied from, or {@code null} for a device added in the draft
     * @param deviceName     the alias - the XML element tag, and the {@code .buttonMap} filename stem
     * @param aliasConfirmed whether the user has looked at this name, which gates button and axis naming
     * @param previousName   the name the installations still hold after a rename, and null at every other time
     */
    record DraftRow(long id, Long masterId, String deviceName, String vid, String pid, boolean aliasConfirmed,
                    String previousName) {
    }
}
