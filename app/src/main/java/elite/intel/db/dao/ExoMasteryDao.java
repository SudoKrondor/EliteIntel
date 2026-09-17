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
 * The Exo-Mastery catalogue and the commander's progress through it.
 * <p>
 * Three tables, one ledger: a system holds bodies, a body holds species, and the body carries the one
 * flag that matters - {@code completed}, meaning the game will not let this body be sampled again.
 * The catalogue rows are replaceable (they come from a file), the flags are not: every upsert here
 * leaves {@code completed} and {@code sampled} alone, and the purge keeps every completed body.
 * <p>
 * Callers go through {@code ExoMasteryManager} rather than touching this directly.
 */
@RegisterRowMapper(ExoMasteryDao.SiteMapper.class)
@RegisterRowMapper(ExoMasteryDao.BodyMapper.class)
@RegisterRowMapper(ExoMasteryDao.StatsMapper.class)
public interface ExoMasteryDao {

    // -------------------------------------------------------------- catalogue

    @SqlUpdate("""
            INSERT INTO exo_mastery_system (systemAddress, starSystem, x, y, z)
                 VALUES (:systemAddress, :starSystem, :x, :y, :z)
            ON CONFLICT(systemAddress) DO UPDATE SET
                    starSystem = excluded.starSystem,
                    x = excluded.x, y = excluded.y, z = excluded.z
            """)
    void upsertSystem(@Bind("systemAddress") long systemAddress,
                      @Bind("starSystem") String starSystem,
                      @Bind("x") double x,
                      @Bind("y") double y,
                      @Bind("z") double z);

    /**
     * The catalogue half of a body row. {@code completed} is deliberately not in the update list.
     */
    @SqlUpdate("""
            INSERT INTO exo_mastery_body (systemAddress, bodyId, starSystem, bodyName, bodyType, value)
                 VALUES (:systemAddress, :bodyId, :starSystem, :bodyName, :bodyType, :value)
            ON CONFLICT(systemAddress, bodyId) DO UPDATE SET
                    starSystem = excluded.starSystem,
                    bodyName   = excluded.bodyName,
                    bodyType   = excluded.bodyType,
                    value      = excluded.value
            """)
    void upsertBody(@Bind("systemAddress") long systemAddress,
                    @Bind("bodyId") long bodyId,
                    @Bind("starSystem") String starSystem,
                    @Bind("bodyName") String bodyName,
                    @Bind("bodyType") String bodyType,
                    @Bind("value") long value);

    /**
     * The catalogue half of a species row. {@code sampled} is deliberately not in the update list.
     */
    @SqlUpdate("""
            INSERT INTO exo_mastery_species (systemAddress, bodyId, speciesSymbol, speciesName, colonies, value)
                 VALUES (:systemAddress, :bodyId, :speciesSymbol, :speciesName, :colonies, :value)
            ON CONFLICT(systemAddress, bodyId, speciesSymbol) DO UPDATE SET
                    speciesName = excluded.speciesName,
                    colonies    = excluded.colonies,
                    value       = excluded.value
            """)
    void upsertSpecies(@Bind("systemAddress") long systemAddress,
                       @Bind("bodyId") long bodyId,
                       @Bind("speciesSymbol") String speciesSymbol,
                       @Bind("speciesName") String speciesName,
                       @Bind("colonies") int colonies,
                       @Bind("value") long value);

    /**
     * Adopts what the location table already knows: a body whose survey-complete latch is set was
     * sampled out before the catalogue arrived, so it is completed on arrival rather than offered.
     * <p>
     * A read of another manager's table, in SQL, because the alternative is loading every location
     * row's JSON to look at one boolean. The latch is stored as JSON true, which SQLite's {@code ->>}
     * reads back as 1.
     */
    @SqlUpdate("""
            UPDATE exo_mastery_body
               SET completed = TRUE, completedAt = :completedAt
             WHERE completed = FALSE
               AND EXISTS (SELECT 1 FROM location l
                            WHERE l.systemAddress = exo_mastery_body.systemAddress
                              AND l.json ->> '$.bodyId' = exo_mastery_body.bodyId
                              AND l.json ->> '$.bioScansCompleted' = 1)
            """)
    int adoptCompletedLocations(@Bind("completedAt") String completedAt);

    @SqlUpdate("DELETE FROM exo_mastery_system")
    void deleteSystems();

    @SqlUpdate("DELETE FROM exo_mastery_body WHERE completed = FALSE")
    void deleteUncompletedBodies();

    /**
     * Species rows belong to their body: once the uncompleted bodies are gone, so are theirs.
     */
    @SqlUpdate("""
            DELETE FROM exo_mastery_species
             WHERE NOT EXISTS (SELECT 1 FROM exo_mastery_body b
                                WHERE b.systemAddress = exo_mastery_species.systemAddress
                                  AND b.bodyId = exo_mastery_species.bodyId)
            """)
    void deleteOrphanSpecies();

    // --------------------------------------------------------------- progress

    @SqlUpdate("""
            UPDATE exo_mastery_body
               SET completed = :completed,
                   completedAt = CASE WHEN :completed THEN :completedAt ELSE NULL END
             WHERE systemAddress = :systemAddress AND bodyId = :bodyId AND completed != :completed
            """)
    int setBodyCompleted(@Bind("systemAddress") long systemAddress,
                         @Bind("bodyId") long bodyId,
                         @Bind("completed") boolean completed,
                         @Bind("completedAt") String completedAt);

    @SqlUpdate("""
            UPDATE exo_mastery_body
               SET completed = TRUE, completedAt = :completedAt
             WHERE systemAddress = :systemAddress AND completed = FALSE
            """)
    int completeSystem(@Bind("systemAddress") long systemAddress, @Bind("completedAt") String completedAt);

    @SqlUpdate("""
            UPDATE exo_mastery_species
               SET sampled = TRUE
             WHERE systemAddress = :systemAddress AND bodyId = :bodyId AND speciesSymbol = :speciesSymbol
            """)
    int markSampled(@Bind("systemAddress") long systemAddress,
                    @Bind("bodyId") long bodyId,
                    @Bind("speciesSymbol") String speciesSymbol);

    @SqlQuery("""
            SELECT COUNT(*) FROM exo_mastery_species
             WHERE systemAddress = :systemAddress AND bodyId = :bodyId AND sampled = FALSE
            """)
    int countUnsampledSpecies(@Bind("systemAddress") long systemAddress, @Bind("bodyId") long bodyId);

    // ------------------------------------------------------------------ reads

    @SqlQuery("SELECT COUNT(*) FROM exo_mastery_system")
    int countSystems();

    @SqlQuery("SELECT COUNT(*) FROM exo_mastery_body WHERE systemAddress = :systemAddress AND bodyId = :bodyId")
    int countBody(@Bind("systemAddress") long systemAddress, @Bind("bodyId") long bodyId);

    @SqlQuery("SELECT COUNT(*) FROM exo_mastery_body WHERE systemAddress = :systemAddress AND completed = FALSE")
    int countRemainingBodies(@Bind("systemAddress") long systemAddress);

    /**
     * The catalogued systems with something left to sample, richest first. Value is what is still on
     * offer, not what the catalogue says the system was worth: a system half sampled keeps its other
     * half, and a system sampled out drops off the list.
     */
    @SqlQuery("""
            SELECT s.systemAddress, s.starSystem, s.x, s.y, s.z, SUM(b.value) AS remainingValue
              FROM exo_mastery_system s
              JOIN exo_mastery_body b ON b.systemAddress = s.systemAddress
             WHERE b.completed = FALSE
             GROUP BY s.systemAddress
             ORDER BY remainingValue DESC, s.starSystem ASC
             LIMIT :limit
            """)
    List<Site> richestRemaining(@Bind("limit") int limit);

    @SqlQuery("""
            SELECT * FROM exo_mastery_body
             WHERE systemAddress = :systemAddress
             ORDER BY completed ASC, value DESC, bodyName ASC
            """)
    List<Body> bodiesIn(@Bind("systemAddress") long systemAddress);

    /**
     * The catalogue's size and the commander's progress through it, in one row. Only catalogued
     * bodies count towards the totals, so after a purge the totals go to zero while the harvested
     * side - the completed bodies kept through it - still reads.
     */
    @SqlQuery("""
            SELECT (SELECT COUNT(*) FROM exo_mastery_system) AS systems,
                   (SELECT COUNT(*) FROM exo_mastery_body b
                     WHERE EXISTS (SELECT 1 FROM exo_mastery_system s WHERE s.systemAddress = b.systemAddress)) AS bodies,
                   (SELECT COALESCE(SUM(b.value), 0) FROM exo_mastery_body b
                     WHERE EXISTS (SELECT 1 FROM exo_mastery_system s WHERE s.systemAddress = b.systemAddress)) AS totalValue,
                   (SELECT COUNT(*) FROM exo_mastery_body WHERE completed = TRUE) AS completedBodies,
                   (SELECT COALESCE(SUM(value), 0) FROM exo_mastery_body WHERE completed = TRUE) AS harvestedValue
            """)
    Stats stats();

    // ---------------------------------------------------------------- entities

    /**
     * A catalogued system and what is still on offer in it.
     */
    record Site(long systemAddress, String starSystem, double x, double y, double z, long remainingValue) {
    }

    record Body(long systemAddress, long bodyId, String starSystem, String bodyName, String bodyType,
                long value, boolean completed) {
    }

    record Stats(int systems, int bodies, long totalValue, int completedBodies, long harvestedValue) {
    }

    class SiteMapper implements RowMapper<Site> {
        @Override
        public Site map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new Site(rs.getLong("systemAddress"), rs.getString("starSystem"),
                    rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"), rs.getLong("remainingValue"));
        }
    }

    class BodyMapper implements RowMapper<Body> {
        @Override
        public Body map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new Body(rs.getLong("systemAddress"), rs.getLong("bodyId"), rs.getString("starSystem"),
                    rs.getString("bodyName"), rs.getString("bodyType"), rs.getLong("value"), rs.getBoolean("completed"));
        }
    }

    class StatsMapper implements RowMapper<Stats> {
        @Override
        public Stats map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new Stats(rs.getInt("systems"), rs.getInt("bodies"), rs.getLong("totalValue"),
                    rs.getInt("completedBodies"), rs.getLong("harvestedValue"));
        }
    }
}
