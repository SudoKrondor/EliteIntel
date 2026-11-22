package elite.intel.db.dao;

import elite.intel.gameapi.missions.MassacreKill;
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
 * Combat bonds earned and not yet cashed in - the conflict-zone twin of {@link BountyDao}.
 * <p>
 * A bond cashed in while a war massacre contract is open is kept, flagged {@code cashedIn}, because it is
 * still one of that contract's kills: the card's held total leaves it out, the kill count does not.
 * <p>
 * Callers go through {@code CombatBondManager} rather than touching this directly.
 */
@RegisterRowMapper(CombatBondDao.PendingMapper.class)
public interface CombatBondDao {

    /**
     * WHY OR IGNORE: the table's unique key is the journal line itself, so a line seen twice - a
     * replay, a re-read - lands on the row it already has.
     */
    @SqlUpdate("""
            INSERT OR IGNORE INTO combat_bond (systemAddress, awardingFaction, victimFaction, reward, earnedAt)
                 VALUES (:systemAddress, :awardingFaction, :victimFaction, :reward, :earnedAt)
            """)
    void record(@Bind("systemAddress") Long systemAddress,
                @Bind("awardingFaction") String awardingFaction,
                @Bind("victimFaction") String victimFaction,
                @Bind("reward") long reward,
                @Bind("earnedAt") String earnedAt);

    /**
     * What the commander is carrying, as one row: the HUD overlay polls this on a timer, so SQLite
     * adds it up rather than the caller. The side is the faction that paid the latest bond, which is
     * the side the commander is fighting for.
     */
    @SqlQuery("""
            SELECT COUNT(*)                 AS kills,
                   COALESCE(SUM(reward), 0) AS credits,
                   (SELECT awardingFaction FROM combat_bond WHERE cashedIn = 0
                     ORDER BY earnedAt DESC, id DESC LIMIT 1) AS side
              FROM combat_bond
             WHERE cashedIn = 0
            """)
    Pending pending();

    /**
     * Every bond on record, cashed in or not, as the kill it paid for.
     */
    @SqlQuery("SELECT victimFaction, earnedAt FROM combat_bond ORDER BY earnedAt, id")
    @RegisterRowMapper(KillMapper.class)
    List<MassacreKill> kills();

    @SqlUpdate("UPDATE combat_bond SET cashedIn = 1")
    void markAllCashedIn();

    @SqlUpdate("DELETE FROM combat_bond")
    void clear();

    class KillMapper implements RowMapper<MassacreKill> {
        @Override
        public MassacreKill map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new MassacreKill(rs.getString("victimFaction"), rs.getString("earnedAt"));
        }
    }

    class PendingMapper implements RowMapper<Pending> {
        @Override
        public Pending map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new Pending(rs.getInt("kills"), rs.getLong("credits"), rs.getString("side"));
        }
    }

    /**
     * @param kills   bonds held, which is one per kill the commander was paid for
     * @param credits what those bonds are worth
     * @param side    the faction paying, or null when nothing is held
     */
    record Pending(int kills, long credits, String side) {
    }
}
