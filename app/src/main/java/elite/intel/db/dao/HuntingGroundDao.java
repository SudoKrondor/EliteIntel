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
 * Star systems the commander has seen resource extraction sites in.
 * <p>
 * The row's existence is the fact "this system has resource sites". The four counts say which
 * grades and how many, and are allowed to be zero on a row carried over from the old
 * hand-confirmation workflow, which recorded the system but never its grades.
 * <p>
 * Callers go through {@code HuntingGroundManager} rather than touching this directly.
 */
@RegisterRowMapper(HuntingGroundDao.GroundMapper.class)
@RegisterRowMapper(HuntingGroundDao.NearbyMapper.class)
public interface HuntingGroundDao {

    /**
     * Records that this system exists as a hunting ground and that we were just in it.
     * <p>
     * Coordinates and the system address are filled in only when the caller has them and the row
     * does not, so a seeded row gains its address the first time it is flown to, and a later visit
     * without coordinates to hand never blanks the ones already stored.
     */
    @SqlUpdate("""
            INSERT INTO hunting_ground (starSystem, systemAddress, x, y, z, firstSeen, lastSeen)
                 VALUES (:starSystem, :systemAddress, :x, :y, :z, :seenAt, :seenAt)
            ON CONFLICT(starSystem) DO UPDATE SET
                    systemAddress = COALESCE(hunting_ground.systemAddress, excluded.systemAddress),
                    x             = COALESCE(hunting_ground.x, excluded.x),
                    y             = COALESCE(hunting_ground.y, excluded.y),
                    z             = COALESCE(hunting_ground.z, excluded.z),
                    lastSeen      = excluded.lastSeen
            """)
    void touch(@Bind("starSystem") String starSystem,
               @Bind("systemAddress") Long systemAddress,
               @Bind("x") Double x,
               @Bind("y") Double y,
               @Bind("z") Double z,
               @Bind("seenAt") String seenAt);

    /**
     * Raises each grade count to what this sweep saw, never lowering one.
     * <p>
     * WHY MAX rather than assignment: a sweep interrupted part way, or one flown at a range where
     * only some sites are in scanner reach, reports fewer sites than the system holds. WHY MAX
     * rather than addition: every arrival re-emits the whole set, and summing turns Sol's five low
     * sites into forty.
     */
    @SqlUpdate("""
            UPDATE hunting_ground
               SET resStandard  = MAX(resStandard, :standard),
                   resLow       = MAX(resLow, :low),
                   resHigh      = MAX(resHigh, :high),
                   resHazardous = MAX(resHazardous, :hazardous)
             WHERE starSystem = :starSystem
            """)
    void recordSweep(@Bind("starSystem") String starSystem,
                     @Bind("standard") int standard,
                     @Bind("low") int low,
                     @Bind("high") int high,
                     @Bind("hazardous") int hazardous);

    /**
     * The shared ledger row, with whether this commander told us to forget it. The ledger is every commander's;
     * forgetting a ground is one commander's choice and lives in their own {@code hunting_ground_forgotten}.
     */
    @SqlQuery("""
            SELECT g.*,
                   EXISTS (SELECT 1 FROM hunting_ground_forgotten f WHERE f.starSystem = g.starSystem) AS forgotten
              FROM hunting_ground g
             WHERE g.starSystem = :starSystem
            """)
    Ground findByName(@Bind("starSystem") String starSystem);

    /**
     * The hunting grounds worth flying to, best first.
     * <p>
     * Ordering is by grade tier and then by distance, so within the range the commander asked for
     * they are offered the best fighting available rather than the closest ring. A row whose grades
     * were never recorded ranks as standard - it is known to have sites, just not which.
     */
    @SqlQuery("""
            SELECT starSystem, resStandard, resLow, resHigh, resHazardous,
                   ((x - :x) * (x - :x) + (y - :y) * (y - :y) + (z - :z) * (z - :z)) AS distanceSq
              FROM hunting_ground
             WHERE NOT EXISTS (SELECT 1 FROM hunting_ground_forgotten f WHERE f.starSystem = hunting_ground.starSystem)
               AND x IS NOT NULL
               AND ((x - :x) * (x - :x) + (y - :y) * (y - :y) + (z - :z) * (z - :z)) <= :maxDistanceSq
             ORDER BY CASE WHEN resHazardous > 0 THEN 4
                           WHEN resHigh > 0      THEN 3
                           WHEN resStandard > 0  THEN 2
                           WHEN resLow > 0       THEN 1
                           ELSE 2 END DESC,
                      distanceSq ASC
             LIMIT :limit
            """)
    List<Nearby> findNearby(@Bind("x") double x,
                            @Bind("y") double y,
                            @Bind("z") double z,
                            @Bind("maxDistanceSq") double maxDistanceSq,
                            @Bind("limit") int limit);

    /**
     * Forgets a ground for this commander only. Returns 1 when the ledger knows the system, as the old in-place
     * update did, so callers can still tell "forgotten" from "never heard of it".
     */
    @SqlUpdate("""
            INSERT OR IGNORE INTO hunting_ground_forgotten (starSystem)
            SELECT starSystem FROM hunting_ground WHERE starSystem = :starSystem
            """)
    int forgetForCommander(@Bind("starSystem") String starSystem);

    @SqlQuery("SELECT COUNT(*) FROM hunting_ground WHERE starSystem = :starSystem")
    int countNamed(@Bind("starSystem") String starSystem);

    default int forget(String starSystem) {
        forgetForCommander(starSystem);
        return countNamed(starSystem);
    }

    @SqlQuery("""
            SELECT COUNT(*) FROM hunting_ground g
             WHERE NOT EXISTS (SELECT 1 FROM hunting_ground_forgotten f WHERE f.starSystem = g.starSystem)
            """)
    int count();

    class GroundMapper implements RowMapper<Ground> {
        @Override
        public Ground map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new Ground(
                    rs.getString("starSystem"),
                    rs.getInt("resStandard"),
                    rs.getInt("resLow"),
                    rs.getInt("resHigh"),
                    rs.getInt("resHazardous"),
                    rs.getBoolean("forgotten")
            );
        }
    }

    class NearbyMapper implements RowMapper<Nearby> {
        @Override
        public Nearby map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new Nearby(
                    rs.getString("starSystem"),
                    rs.getInt("resStandard"),
                    rs.getInt("resLow"),
                    rs.getInt("resHigh"),
                    rs.getInt("resHazardous"),
                    rs.getDouble("distanceSq")
            );
        }
    }

    record Ground(String starSystem, int standard, int low, int high, int hazardous, boolean forgotten) {
    }

    record Nearby(String starSystem, int standard, int low, int high, int hazardous, double distanceSq) {
    }
}
