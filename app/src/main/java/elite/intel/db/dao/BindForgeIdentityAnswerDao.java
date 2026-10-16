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
 * The identity question's answers that hold until the hardware changes, from
 * {@code 12007__bindforge_identity_answer.sql} (Alan, 2026-10-15, A12). Callers go through
 * {@link elite.intel.db.managers.BindForgeIdentityAnswerManager}.
 */
@RegisterRowMapper(BindForgeIdentityAnswerDao.AnswerRowMapper.class)
public interface BindForgeIdentityAnswerDao {

    @SqlQuery("SELECT * FROM bindforge_identity_answer ORDER BY device_name")
    List<AnswerRow> findAll();

    /** Records an answer, replacing the one before for that device. */
    @SqlUpdate("""
            INSERT INTO bindforge_identity_answer (device_name, candidates, answer)
            VALUES (:deviceName, :candidates, :answer)
            ON CONFLICT (device_name) DO UPDATE
                SET candidates = excluded.candidates,
                    answer = excluded.answer,
                    answered_at = CURRENT_TIMESTAMP
            """)
    void put(@Bind("deviceName") String deviceName,
             @Bind("candidates") String candidates,
             @Bind("answer") String answer);

    @SqlUpdate("DELETE FROM bindforge_identity_answer WHERE device_name = :deviceName")
    void delete(@Bind("deviceName") String deviceName);

    class AnswerRowMapper implements RowMapper<AnswerRow> {
        @Override
        public AnswerRow map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new AnswerRow(
                    rs.getString("device_name"),
                    rs.getString("candidates"),
                    rs.getString("answer"));
        }
    }

    /**
     * @param candidates the controllers the question listed, as sorted upper-case {@code VID:PID} pairs joined by
     *                   commas
     * @param answer     {@code NOT_NOW} or {@code NONE_OF_THESE}
     */
    record AnswerRow(String deviceName, String candidates, String answer) {
    }
}
