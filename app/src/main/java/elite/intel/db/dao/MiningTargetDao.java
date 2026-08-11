package elite.intel.db.dao;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.ResultSet;
import java.sql.SQLException;

@RegisterRowMapper(MiningTargetDao.MiningTargetMapper.class)
public interface MiningTargetDao {


    @SqlUpdate("INSERT OR IGNORE INTO mining_targets (target) VALUES (:target)")
    void add(@BindBean MiningTarget target);

    @SqlQuery("SELECT * FROM mining_targets")
    MiningTarget[] listAll();

    @SqlUpdate("DELETE FROM mining_targets")
    void clear();

    @SqlUpdate("DELETE FROM mining_targets WHERE target = :target")
    void remove(@Bind("target") String target);


    class MiningTargetMapper implements RowMapper<MiningTarget> {

        @Override public MiningTarget map(ResultSet rs, StatementContext ctx) throws SQLException {
            MiningTarget miningTarget = new MiningTarget();
            miningTarget.target = rs.getString("target");
            return miningTarget;
        }
    }


    class MiningTarget {

        private String target;

        public MiningTarget() {
        }

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }
    }
}
