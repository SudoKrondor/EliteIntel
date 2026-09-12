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
@RegisterRowMapper(BountyDao.PendingMapper.class)
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

    /**
     * What the commander is carrying and has not cashed in yet, as one row.
     * <p>
     * WHY an aggregate rather than reading {@link #listAll()} and adding it up: the HUD overlay polls
     * this on a timer (its own {@code hud-overlay-objectives} thread), and a session of bounty hunting is
     * hundreds of rows whose payload is a JSON blob. SQLite reads the two fields it needs out of the blob and hands back one row.
     * <p>
     * A row written before the cashed-in flag existed has no such key in its JSON, and COALESCE reads
     * that absence as "not cashed in" - which is what it meant.
     */
    @SqlQuery("""
            SELECT COUNT(*)                                              AS kills,
                   COALESCE(SUM(json_extract(bounty, '$.totalReward')), 0) AS credits
              FROM bounties
             WHERE COALESCE(json_extract(bounty, '$.cashedIn'), 0) = 0
            """)
    Pending pending();

    class PendingMapper implements RowMapper<Pending> {

        @Override
        public Pending map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new Pending(rs.getInt("kills"), rs.getLong("credits"));
        }
    }

    /**
     * @param kills   bounty vouchers held, which is one per kill the commander was paid for
     * @param credits what those vouchers are worth
     */
    record Pending(int kills, long credits) {
    }

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
