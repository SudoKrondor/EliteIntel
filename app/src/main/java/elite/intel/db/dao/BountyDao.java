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

@RegisterRowMapper(BountyDao.BountyMapper.class)
public interface BountyDao {

    @SqlUpdate("""
            INSERT OR REPLACE INTO bounties (key, bounty)
            VALUES(:key, :bounty)
            ON CONFLICT(key) DO UPDATE SET
            bounty = excluded.bounty
            """)
    void upsert(@BindBean Bounty bounty);

    @SqlQuery("SELECT * FROM bounties WHERE key = :key")
    Bounty get(@BindBean Bounty bounty);

    @SqlUpdate("DELETE FROM bounties")
    void clear();

    @SqlUpdate("DELETE FROM bounties WHERE key = :key")
    void delete(@Bind("key") String key);

    @SqlQuery("SELECT * FROM bounties")
    Bounty[] listAll();

    class BountyMapper implements RowMapper<Bounty> {

        @Override public Bounty map(ResultSet rs, StatementContext ctx) throws SQLException {
            Bounty bounty = new Bounty();
            bounty.setKey(rs.getString("key"));
            bounty.setBounty(rs.getString("bounty"));
            return bounty;
        }
    }


    class Bounty{
        public Bounty() {
        }
        private String key;
        private String bounty;

        public String getKey() {
            return key;
        }
        public void setKey(String key) {
            this.key = key;
        }
        public String getBounty() {
            return bounty;
        }
        public void setBounty(String bounty) {
            this.bounty = bounty;
        }

    }
}
