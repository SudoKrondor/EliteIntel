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
 * Every pirate massacre contract the commander has accepted, one row per contract.
 * <p>
 * WHY a ledger and not a table of provider/target pairs: the pair, the stack depth and the list of
 * stations are all questions about the contracts actually taken, and answering them with GROUP BY
 * costs nothing at this size. Keeping the raw rows makes re-reading a journal harmless, because the
 * game's own {@code MissionID} is the primary key and the same contract lands on the same row.
 * <p>
 * Callers go through {@code HuntingGroundManager} rather than touching this directly.
 */
@RegisterRowMapper(MassacreMissionDao.PairMapper.class)
public interface MassacreMissionDao {

    /**
     * @return 1 when the contract was new, 0 when it was already on file
     */
    @SqlUpdate("""
            INSERT OR IGNORE INTO massacre_mission
                (missionId, providerSystem, providerX, providerY, providerZ, providerStation,
                 providerFaction, targetSystem, targetFaction, killCount, reward, acceptedAt)
            VALUES (:missionId, :providerSystem, :x, :y, :z, :providerStation,
                    :providerFaction, :targetSystem, :targetFaction, :killCount, :reward, :acceptedAt)
            """)
    int record(@Bind("missionId") long missionId,
               @Bind("providerSystem") String providerSystem,
               @Bind("x") Double x,
               @Bind("y") Double y,
               @Bind("z") Double z,
               @Bind("providerStation") String providerStation,
               @Bind("providerFaction") String providerFaction,
               @Bind("targetSystem") String targetSystem,
               @Bind("targetFaction") String targetFaction,
               @Bind("killCount") Integer killCount,
               @Bind("reward") Long reward,
               @Bind("acceptedAt") String acceptedAt);

    @SqlUpdate("""
            UPDATE massacre_mission
               SET completedAt = :completedAt
             WHERE missionId = :missionId
               AND completedAt IS NULL
            """)
    int markCompleted(@Bind("missionId") long missionId, @Bind("completedAt") String completedAt);

    /**
     * Forgets every contract that sent the commander to this hunting ground.
     */
    @SqlUpdate("DELETE FROM massacre_mission WHERE targetSystem = :targetSystem")
    int deleteForTarget(@Bind("targetSystem") String targetSystem);

    /**
     * Provider systems paired with hunting grounds that have resource sites, deepest stack first.
     * <p>
     * The join to {@code hunting_ground} is what makes a pair a pair: a contract against a system
     * the commander has never seen sites in produces no row here, which is the whole point. Ordering
     * puts stack depth ahead of distance because an extra provider faction is worth more than a few
     * jumps - it is a whole contract that costs no extra kills. Completed contracts break the tie next,
     * because a pairing the commander has actually finished work on is proven and one merely tried is not.
     *
     * @param targetSystem narrows the answer to one hunting ground, or null for any of them
     */
    @SqlQuery("""
            SELECT m.providerSystem,
                   m.targetSystem,
                   m.targetFaction,
                   GROUP_CONCAT(DISTINCT m.providerFaction) AS providerFactions,
                   GROUP_CONCAT(DISTINCT m.providerStation) AS stations,
                   COUNT(DISTINCT m.providerFaction)        AS stackDepth,
                   SUM(CASE WHEN m.completedAt IS NULL THEN 0 ELSE 1 END) AS missionsCompleted,
                   g.resStandard, g.resLow, g.resHigh, g.resHazardous,
                   ((MAX(m.providerX) - :x) * (MAX(m.providerX) - :x)
                  + (MAX(m.providerY) - :y) * (MAX(m.providerY) - :y)
                  + (MAX(m.providerZ) - :z) * (MAX(m.providerZ) - :z)) AS distanceSq
              FROM massacre_mission m
              JOIN hunting_ground g ON g.starSystem = m.targetSystem
             WHERE g.forgotten = 0
               AND m.providerX IS NOT NULL
               AND (:targetSystem IS NULL OR m.targetSystem = :targetSystem)
             GROUP BY m.providerSystem, m.targetSystem, m.targetFaction
            HAVING ((MAX(m.providerX) - :x) * (MAX(m.providerX) - :x)
                  + (MAX(m.providerY) - :y) * (MAX(m.providerY) - :y)
                  + (MAX(m.providerZ) - :z) * (MAX(m.providerZ) - :z)) <= :maxDistanceSq
             ORDER BY stackDepth DESC, missionsCompleted DESC, distanceSq ASC
             LIMIT :limit
            """)
    List<Pair> findPairs(@Bind("x") double x,
                         @Bind("y") double y,
                         @Bind("z") double z,
                         @Bind("maxDistanceSq") double maxDistanceSq,
                         @Bind("targetSystem") String targetSystem,
                         @Bind("limit") int limit);

    @SqlQuery("SELECT COUNT(*) FROM massacre_mission")
    int count();

    class PairMapper implements RowMapper<Pair> {
        @Override
        public Pair map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new Pair(
                    rs.getString("providerSystem"),
                    rs.getString("providerFactions"),
                    rs.getString("stations"),
                    rs.getString("targetSystem"),
                    rs.getString("targetFaction"),
                    rs.getInt("resStandard"),
                    rs.getInt("resLow"),
                    rs.getInt("resHigh"),
                    rs.getInt("resHazardous"),
                    rs.getInt("missionsCompleted"),
                    rs.getDouble("distanceSq")
            );
        }
    }

    /**
     * One provider/target grouping straight out of SQL. {@code providerFactions} and
     * {@code stations} are comma-separated because that is what GROUP_CONCAT returns - the manager
     * splits them before anything else sees them.
     */
    record Pair(String providerSystem,
                String providerFactions,
                String stations,
                String targetSystem,
                String targetFaction,
                int standard,
                int low,
                int high,
                int hazardous,
                int missionsCompleted,
                double distanceSq) {
    }
}
