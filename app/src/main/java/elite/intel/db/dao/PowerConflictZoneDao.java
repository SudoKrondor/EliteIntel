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
 * Star systems seen to hold power conflict zones, and the powers present in them.
 * <p>
 * The powerplay twin of {@link ConflictZoneDao}, keyed by system address. {@code lastSeen} is the
 * last sweep that saw zones and {@code powersSeen} the last arrival that named the powers - see the
 * migration for why the two age apart.
 * <p>
 * Callers go through {@code PowerConflictZoneManager} rather than touching this directly.
 */
@RegisterRowMapper(PowerConflictZoneDao.NearbyMapper.class)
public interface PowerConflictZoneDao {

    /**
     * Records that a sweep just saw power conflict zones in this system, raising the count to what it saw.
     * <p>
     * WHY MAX: a sweep interrupted part way reports fewer zones than the system holds, and every
     * arrival re-emits the whole set. Coordinates are filled in only when the row has none.
     */
    @SqlUpdate("""
            INSERT INTO power_conflict_zone (systemAddress, starSystem, x, y, z, zones, firstSeen, lastSeen)
                 VALUES (:systemAddress, :starSystem, :x, :y, :z, :zones, :seenAt, :seenAt)
            ON CONFLICT(systemAddress) DO UPDATE SET
                    starSystem = excluded.starSystem,
                    x          = COALESCE(power_conflict_zone.x, excluded.x),
                    y          = COALESCE(power_conflict_zone.y, excluded.y),
                    z          = COALESCE(power_conflict_zone.z, excluded.z),
                    zones      = MAX(power_conflict_zone.zones, excluded.zones),
                    lastSeen   = MAX(COALESCE(power_conflict_zone.lastSeen, ''), excluded.lastSeen)
            """)
    void recordSweep(@Bind("systemAddress") long systemAddress,
                     @Bind("starSystem") String starSystem,
                     @Bind("x") Double x,
                     @Bind("y") Double y,
                     @Bind("z") Double z,
                     @Bind("zones") int zones,
                     @Bind("seenAt") String seenAt);

    /**
     * Forgets the zones of a cycle that is over, before a new sweep lands on the row, so an old cycle's
     * count never inflates this one through the MAX in {@link #recordSweep}.
     */
    @SqlUpdate("""
            UPDATE power_conflict_zone
               SET zones = 0
             WHERE systemAddress = :systemAddress
               AND (lastSeen IS NULL OR lastSeen < :seenBefore)
            """)
    int clearIfNotSeenSince(@Bind("systemAddress") long systemAddress, @Bind("seenBefore") String seenBefore);

    /**
     * An arrival found no power in the system: every zone sighted up to that moment is gone.
     */
    @SqlUpdate("""
            UPDATE power_conflict_zone
               SET zones = 0, controllingPower = NULL, powers = NULL, powerplayState = NULL, powersSeen = :seenAt
             WHERE systemAddress = :systemAddress
               AND (lastSeen IS NULL OR lastSeen <= :seenAt)
            """)
    int clearIfNotSeenAfter(@Bind("systemAddress") long systemAddress, @Bind("seenAt") String seenAt);

    /**
     * Records the powers an arrival named, creating the row so a sweep that follows has the powers to
     * join. The newest arrival wins, whichever order the relay delivers them in.
     */
    @SqlUpdate("""
            INSERT INTO power_conflict_zone (systemAddress, starSystem, x, y, z, controllingPower, powers,
                                             powerplayState, firstSeen, powersSeen)
                 VALUES (:systemAddress, :starSystem, :x, :y, :z, :controllingPower, :powers,
                         :powerplayState, :seenAt, :seenAt)
            ON CONFLICT(systemAddress) DO UPDATE SET
                    starSystem       = excluded.starSystem,
                    x                = COALESCE(power_conflict_zone.x, excluded.x),
                    y                = COALESCE(power_conflict_zone.y, excluded.y),
                    z                = COALESCE(power_conflict_zone.z, excluded.z),
                    controllingPower = excluded.controllingPower,
                    powers           = excluded.powers,
                    powerplayState   = excluded.powerplayState,
                    powersSeen       = excluded.powersSeen
              WHERE power_conflict_zone.powersSeen IS NULL
                 OR power_conflict_zone.powersSeen <= excluded.powersSeen
            """)
    void recordPowers(@Bind("systemAddress") long systemAddress,
                      @Bind("starSystem") String starSystem,
                      @Bind("x") Double x,
                      @Bind("y") Double y,
                      @Bind("z") Double z,
                      @Bind("controllingPower") String controllingPower,
                      @Bind("powers") String powers,
                      @Bind("powerplayState") String powerplayState,
                      @Bind("seenAt") String seenAt);

    /**
     * Whether the system has a row at all. A read, so it takes no writer lock: the guard that keeps the
     * relay's arrivals in systems never seen to hold a power conflict zone from costing a write.
     */
    @SqlQuery("SELECT COUNT(*) > 0 FROM power_conflict_zone WHERE systemAddress = :systemAddress")
    boolean exists(@Bind("systemAddress") long systemAddress);

    /**
     * Power conflict zones the given power is in, best first: sighted since {@code seenSince}, ordered
     * by the day of the last sighting, then the most zones, then distance.
     *
     * @param quotedPower the power's name in double quotes, as it appears inside the stored JSON array,
     *                    so one name never matches inside another
     */
    @SqlQuery("""
            SELECT starSystem, zones, controllingPower, powers,
                   ((x - :x) * (x - :x) + (y - :y) * (y - :y) + (z - :z) * (z - :z)) AS distanceSq
              FROM power_conflict_zone
             WHERE x IS NOT NULL
               AND zones > 0
               AND lastSeen >= :seenSince
               AND instr(powers, :quotedPower) > 0
               AND ((x - :x) * (x - :x) + (y - :y) * (y - :y) + (z - :z) * (z - :z)) <= :maxDistanceSq
             ORDER BY substr(lastSeen, 1, 10) DESC,
                      zones DESC,
                      distanceSq ASC
             LIMIT :limit
            """)
    List<Nearby> findNearby(@Bind("x") double x,
                            @Bind("y") double y,
                            @Bind("z") double z,
                            @Bind("maxDistanceSq") double maxDistanceSq,
                            @Bind("seenSince") String seenSince,
                            @Bind("quotedPower") String quotedPower,
                            @Bind("limit") int limit);

    class NearbyMapper implements RowMapper<Nearby> {
        @Override
        public Nearby map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new Nearby(
                    rs.getString("starSystem"),
                    rs.getInt("zones"),
                    rs.getString("controllingPower"),
                    rs.getString("powers"),
                    rs.getDouble("distanceSq")
            );
        }
    }

    /**
     * @param powers the stored JSON array of power names
     */
    record Nearby(String starSystem, int zones, String controllingPower, String powers, double distanceSq) {
    }
}
