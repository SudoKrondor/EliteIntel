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
 * Star systems seen to hold conflict zones, and who is fighting there.
 * <p>
 * The twin of {@link HuntingGroundDao}. The row's existence is the fact "this system has had a war
 * in it", the counts say how many zones of each intensity the last sweep saw, and {@code lastSeen}
 * says whether that is still current - a war lasts days, not for ever.
 * <p>
 * Callers go through {@code ConflictZoneManager} rather than touching this directly.
 */
@RegisterRowMapper(ConflictZoneDao.ZoneMapper.class)
@RegisterRowMapper(ConflictZoneDao.NearbyMapper.class)
public interface ConflictZoneDao {

    /**
     * Records that this system exists and that a war was just sighted in it.
     * <p>
     * Coordinates and the system address are filled in only when the caller has them and the row
     * does not, so a later sighting without coordinates to hand never blanks the ones already stored.
     */
    @SqlUpdate("""
            INSERT INTO conflict_zone (starSystem, systemAddress, x, y, z, firstSeen, lastSeen)
                 VALUES (:starSystem, :systemAddress, :x, :y, :z, :seenAt, :seenAt)
            ON CONFLICT(starSystem) DO UPDATE SET
                    systemAddress = COALESCE(conflict_zone.systemAddress, excluded.systemAddress),
                    x             = COALESCE(conflict_zone.x, excluded.x),
                    y             = COALESCE(conflict_zone.y, excluded.y),
                    z             = COALESCE(conflict_zone.z, excluded.z),
                    lastSeen      = MAX(COALESCE(conflict_zone.lastSeen, ''), excluded.lastSeen)
            """)
    void touch(@Bind("starSystem") String starSystem,
               @Bind("systemAddress") Long systemAddress,
               @Bind("x") Double x,
               @Bind("y") Double y,
               @Bind("z") Double z,
               @Bind("seenAt") String seenAt);

    /**
     * Forgets the zones and the sides of a war that is over, keeping the row so the system's
     * coordinates survive for the next one.
     * <p>
     * Called both when an arrival reports no active war and, by the manager, before a sweep lands
     * on a row that has not been sighted for a week - so an old war never inflates the new one
     * through the MAX in {@link #recordSweep}.
     */
    @SqlUpdate("""
            UPDATE conflict_zone
               SET czLow = 0, czMedium = 0, czHigh = 0, czPowerplay = 0,
                   warType = NULL, faction1 = NULL, faction2 = NULL
             WHERE starSystem = :starSystem
               AND (lastSeen IS NULL OR lastSeen < :seenBefore)
            """)
    int clearIfNotSeenSince(@Bind("starSystem") String starSystem, @Bind("seenBefore") String seenBefore);

    /**
     * The same, for an arrival that found no war: everything sighted up to and including that moment is
     * over, so the bound is inclusive.
     */
    @SqlUpdate("""
            UPDATE conflict_zone
               SET czLow = 0, czMedium = 0, czHigh = 0, czPowerplay = 0,
                   warType = NULL, faction1 = NULL, faction2 = NULL
             WHERE starSystem = :starSystem
               AND (lastSeen IS NULL OR lastSeen <= :seenAt)
            """)
    int clearIfNotSeenAfter(@Bind("starSystem") String starSystem, @Bind("seenAt") String seenAt);

    /**
     * Raises each intensity count to what this sweep saw, never lowering one.
     * <p>
     * WHY MAX rather than assignment: a sweep interrupted part way reports fewer zones than the
     * system holds. WHY MAX rather than addition: every arrival re-emits the whole set.
     */
    @SqlUpdate("""
            UPDATE conflict_zone
               SET czLow       = MAX(czLow, :low),
                   czMedium    = MAX(czMedium, :medium),
                   czHigh      = MAX(czHigh, :high),
                   czPowerplay = MAX(czPowerplay, :powerplay)
             WHERE starSystem = :starSystem
            """)
    void recordSweep(@Bind("starSystem") String starSystem,
                     @Bind("low") int low,
                     @Bind("medium") int medium,
                     @Bind("high") int high,
                     @Bind("powerplay") int powerplay);

    @SqlUpdate("""
            UPDATE conflict_zone
               SET warType = :warType, faction1 = :faction1, faction2 = :faction2
             WHERE starSystem = :starSystem
            """)
    void recordSides(@Bind("starSystem") String starSystem,
                     @Bind("warType") String warType,
                     @Bind("faction1") String faction1,
                     @Bind("faction2") String faction2);

    @SqlQuery("SELECT * FROM conflict_zone WHERE starSystem = :starSystem")
    Zone findByName(@Bind("starSystem") String starSystem);

    /**
     * The war zones worth flying to, best first: sighted since {@code seenSince}, holding at least
     * one faction-war zone, ordered by the day of the last sighting, then the hardest intensity
     * present, then distance. Within the range the commander asked for they are offered the war
     * most likely to still be on, and the best fighting in it, rather than the nearest.
     */
    @SqlQuery("""
            SELECT starSystem, czLow, czMedium, czHigh, czPowerplay, warType, faction1, faction2,
                   ((x - :x) * (x - :x) + (y - :y) * (y - :y) + (z - :z) * (z - :z)) AS distanceSq
              FROM conflict_zone
             WHERE x IS NOT NULL
               AND lastSeen >= :seenSince
               AND (czLow + czMedium + czHigh) > 0
               AND ((x - :x) * (x - :x) + (y - :y) * (y - :y) + (z - :z) * (z - :z)) <= :maxDistanceSq
             ORDER BY substr(lastSeen, 1, 10) DESC,
                      CASE WHEN czHigh > 0   THEN 3
                           WHEN czMedium > 0 THEN 2
                           ELSE 1 END DESC,
                      distanceSq ASC
             LIMIT :limit
            """)
    List<Nearby> findNearby(@Bind("x") double x,
                            @Bind("y") double y,
                            @Bind("z") double z,
                            @Bind("maxDistanceSq") double maxDistanceSq,
                            @Bind("seenSince") String seenSince,
                            @Bind("limit") int limit);

    class ZoneMapper implements RowMapper<Zone> {
        @Override
        public Zone map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new Zone(
                    rs.getString("starSystem"),
                    rs.getInt("czLow"),
                    rs.getInt("czMedium"),
                    rs.getInt("czHigh"),
                    rs.getInt("czPowerplay"),
                    rs.getString("warType"),
                    rs.getString("faction1"),
                    rs.getString("faction2"),
                    rs.getString("lastSeen")
            );
        }
    }

    class NearbyMapper implements RowMapper<Nearby> {
        @Override
        public Nearby map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new Nearby(
                    rs.getString("starSystem"),
                    rs.getInt("czLow"),
                    rs.getInt("czMedium"),
                    rs.getInt("czHigh"),
                    rs.getInt("czPowerplay"),
                    rs.getString("warType"),
                    rs.getString("faction1"),
                    rs.getString("faction2"),
                    rs.getDouble("distanceSq")
            );
        }
    }

    record Zone(String starSystem, int low, int medium, int high, int powerplay,
                String warType, String faction1, String faction2, String lastSeen) {
    }

    record Nearby(String starSystem, int low, int medium, int high, int powerplay,
                  String warType, String faction1, String faction2, double distanceSq) {
    }
}
