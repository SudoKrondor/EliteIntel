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
 * The massacre sets other commanders reported, as the nightly job checked and published them. Replaced wholesale
 * on every download. Callers go through {@code HuntingGroundManager} and {@code CommunityManager}.
 */
@RegisterRowMapper(CrowdMassacreSetDao.PairMapper.class)
public interface CrowdMassacreSetDao {

    @SqlUpdate("DELETE FROM crowd_massacre_set")
    void deleteAll();

    @SqlUpdate("""
            INSERT OR REPLACE INTO crowd_massacre_set
                (providerSystem, targetSystem, providerX, providerY, providerZ, providerFactions, stations,
                 targetFaction, stackDepth, reporters, lastSeen)
            VALUES (:providerSystem, :targetSystem, :x, :y, :z, :providerFactions, :stations,
                    :targetFaction, :stackDepth, :reporters, :lastSeen)
            """)
    void insert(@Bind("providerSystem") String providerSystem,
                @Bind("targetSystem") String targetSystem,
                @Bind("x") double x,
                @Bind("y") double y,
                @Bind("z") double z,
                @Bind("providerFactions") String providerFactions,
                @Bind("stations") String stations,
                @Bind("targetFaction") String targetFaction,
                @Bind("stackDepth") int stackDepth,
                @Bind("reporters") int reporters,
                @Bind("lastSeen") String lastSeen);

    @SqlQuery("SELECT COUNT(*) FROM crowd_massacre_set")
    int count();

    /**
     * How many stored sets {@link #findPairs} could offer at any distance: the same checks, without the range.
     */
    @SqlQuery("""
            SELECT COUNT(*)
              FROM crowd_massacre_set c
              JOIN hunting_ground g ON g.starSystem = c.targetSystem
             WHERE c.stackDepth > 0
               AND NOT EXISTS (SELECT 1 FROM hunting_ground_forgotten f WHERE f.starSystem = g.starSystem)
            """)
    int countUsable();

    /**
     * Reported sets whose target the commander's own ledger knows to have resource sites, deepest stack first, then
     * nearest.
     * <p>
     * WHY the join to {@code hunting_ground}: the nightly job cannot check resource sites (the galaxy dump it checks
     * against does not record them), so this app does, against what it has seen itself or heard over EDDN. A target
     * the commander forgot is left out, the same as for their own pairs. So is a set none of whose reported provider
     * factions is still in the provider system: there is nobody known there to take a contract from.
     *
     * @param targetSystem narrows the answer to one hunting ground, or null for any of them
     */
    @SqlQuery("""
            SELECT c.providerSystem, c.providerFactions, c.stations, c.targetSystem, c.targetFaction, c.stackDepth,
                   c.reporters,
                   g.resStandard, g.resLow, g.resHigh, g.resHazardous,
                   ((c.providerX - :x) * (c.providerX - :x)
                  + (c.providerY - :y) * (c.providerY - :y)
                  + (c.providerZ - :z) * (c.providerZ - :z)) AS distanceSq
              FROM crowd_massacre_set c
              JOIN hunting_ground g ON g.starSystem = c.targetSystem
             WHERE c.stackDepth > 0
               AND NOT EXISTS (SELECT 1 FROM hunting_ground_forgotten f WHERE f.starSystem = g.starSystem)
               AND (:targetSystem IS NULL OR c.targetSystem = :targetSystem)
               AND ((c.providerX - :x) * (c.providerX - :x)
                  + (c.providerY - :y) * (c.providerY - :y)
                  + (c.providerZ - :z) * (c.providerZ - :z)) <= :maxDistanceSq
             ORDER BY c.stackDepth DESC, distanceSq ASC
             LIMIT :limit
            """)
    List<Pair> findPairs(@Bind("x") double x,
                         @Bind("y") double y,
                         @Bind("z") double z,
                         @Bind("maxDistanceSq") double maxDistanceSq,
                         @Bind("targetSystem") String targetSystem,
                         @Bind("limit") int limit);

    class PairMapper implements RowMapper<Pair> {
        @Override
        public Pair map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new Pair(
                    rs.getString("providerSystem"),
                    rs.getString("providerFactions"),
                    rs.getString("stations"),
                    rs.getString("targetSystem"),
                    rs.getString("targetFaction"),
                    rs.getInt("stackDepth"),
                    rs.getInt("reporters"),
                    rs.getInt("resStandard"),
                    rs.getInt("resLow"),
                    rs.getInt("resHigh"),
                    rs.getInt("resHazardous"),
                    rs.getDouble("distanceSq")
            );
        }
    }

    /**
     * {@code providerFactions} and {@code stations} are joined with {@code |}, as in the commander's own ledger.
     */
    record Pair(String providerSystem,
                String providerFactions,
                String stations,
                String targetSystem,
                String targetFaction,
                int stackDepth,
                int reporters,
                int standard,
                int low,
                int high,
                int hazardous,
                double distanceSq) {
    }
}
