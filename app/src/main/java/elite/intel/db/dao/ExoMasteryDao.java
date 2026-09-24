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
 * The catalogue is three shared tables: a system holds bodies, a body holds species. It comes from a file and is
 * replaceable. The progress is the commander's own and lives in their file: {@code exo_mastery_harvest} holds the
 * bodies they sampled out ({@code completed}, meaning the game will not let this body be sampled again) and
 * {@code exo_mastery_sample} the species they have scanned. A harvest keeps the body's value as it was, so the
 * harvested total survives the catalogue being purged or replaced.
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
     * A catalogue body row. Whether the commander completed it is theirs, in {@code exo_mastery_harvest}.
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
     * A catalogue species row. Whether the commander sampled it is theirs, in {@code exo_mastery_sample}.
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
     * A read of another manager's tables, in SQL, because the alternative is loading every location
     * row's JSON to look at one boolean. The body is matched on the location row's own {@code inGameId}
     * column, which is the journal BodyID the location manager keys on. The latch is the commander's, so it
     * comes out of their {@code location_visit} flags, where it is stored as true and read back by SQLite's
     * {@code ->>} as 1.
     */
    @SqlUpdate("""
            INSERT OR IGNORE INTO exo_mastery_harvest (systemAddress, bodyId, value, completedAt)
            SELECT b.systemAddress, b.bodyId, b.value, :completedAt
              FROM exo_mastery_body b
             WHERE EXISTS (SELECT 1 FROM location l
                             JOIN location_visit v ON v.locationName = l.locationName
                            WHERE l.systemAddress = b.systemAddress
                              AND l.inGameId = b.bodyId
                              AND v.flags ->> '$.bioScansCompleted' = 1)
            """)
    int adoptCompletedLocations(@Bind("completedAt") String completedAt);

    @SqlUpdate("DELETE FROM exo_mastery_system")
    void deleteSystems();

    /**
     * The body catalogue, except the bodies the open commander completed, which stay listed as the record of
     * what they sampled. Another commander's completed bodies go with the catalogue, but their harvest (and so
     * their harvested total) is kept in their own file, value and all.
     */
    @SqlUpdate("""
            DELETE FROM exo_mastery_body
             WHERE NOT EXISTS (SELECT 1 FROM exo_mastery_harvest h
                                WHERE h.systemAddress = exo_mastery_body.systemAddress
                                  AND h.bodyId = exo_mastery_body.bodyId)
            """)
    void deleteUnharvestedBodies();

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

    /**
     * Records that a body is sampled out, or that it is not.
     *
     * @return 1 when that changed anything, 0 when it was already so or the catalogue has no such body
     */
    default int setBodyCompleted(long systemAddress, long bodyId, boolean completed, String completedAt) {
        return completed ? harvest(systemAddress, bodyId, completedAt) : unharvest(systemAddress, bodyId);
    }

    @SqlUpdate("""
            INSERT OR IGNORE INTO exo_mastery_harvest (systemAddress, bodyId, value, completedAt)
            SELECT systemAddress, bodyId, value, :completedAt
              FROM exo_mastery_body
             WHERE systemAddress = :systemAddress AND bodyId = :bodyId
            """)
    int harvest(@Bind("systemAddress") long systemAddress,
                @Bind("bodyId") long bodyId,
                @Bind("completedAt") String completedAt);

    @SqlUpdate("DELETE FROM exo_mastery_harvest WHERE systemAddress = :systemAddress AND bodyId = :bodyId")
    int unharvest(@Bind("systemAddress") long systemAddress, @Bind("bodyId") long bodyId);

    @SqlUpdate("""
            INSERT OR IGNORE INTO exo_mastery_harvest (systemAddress, bodyId, value, completedAt)
            SELECT systemAddress, bodyId, value, :completedAt
              FROM exo_mastery_body
             WHERE systemAddress = :systemAddress
            """)
    int completeSystem(@Bind("systemAddress") long systemAddress, @Bind("completedAt") String completedAt);

    /**
     * Records a species as sampled.
     *
     * @return 1 when the catalogue lists this species on this body (sampled before or not), 0 when it does not
     */
    default int markSampled(long systemAddress, long bodyId, String speciesSymbol) {
        recordSample(systemAddress, bodyId, speciesSymbol);
        return countSpecies(systemAddress, bodyId, speciesSymbol);
    }

    @SqlUpdate("""
            INSERT OR IGNORE INTO exo_mastery_sample (systemAddress, bodyId, speciesSymbol)
            SELECT systemAddress, bodyId, speciesSymbol
              FROM exo_mastery_species
             WHERE systemAddress = :systemAddress AND bodyId = :bodyId AND speciesSymbol = :speciesSymbol
            """)
    int recordSample(@Bind("systemAddress") long systemAddress,
                     @Bind("bodyId") long bodyId,
                     @Bind("speciesSymbol") String speciesSymbol);

    @SqlQuery("""
            SELECT COUNT(*) FROM exo_mastery_species
             WHERE systemAddress = :systemAddress AND bodyId = :bodyId AND speciesSymbol = :speciesSymbol
            """)
    int countSpecies(@Bind("systemAddress") long systemAddress,
                     @Bind("bodyId") long bodyId,
                     @Bind("speciesSymbol") String speciesSymbol);

    @SqlQuery("""
            SELECT COUNT(*) FROM exo_mastery_species s
             WHERE s.systemAddress = :systemAddress AND s.bodyId = :bodyId
               AND NOT EXISTS (SELECT 1 FROM exo_mastery_sample x
                                WHERE x.systemAddress = s.systemAddress AND x.bodyId = s.bodyId
                                  AND x.speciesSymbol = s.speciesSymbol)
            """)
    int countUnsampledSpecies(@Bind("systemAddress") long systemAddress, @Bind("bodyId") long bodyId);

    // ------------------------------------------------------------------ reads

    @SqlQuery("SELECT COUNT(*) FROM exo_mastery_system")
    int countSystems();

    @SqlQuery("SELECT COUNT(*) FROM exo_mastery_body WHERE systemAddress = :systemAddress AND bodyId = :bodyId")
    int countBody(@Bind("systemAddress") long systemAddress, @Bind("bodyId") long bodyId);

    @SqlQuery("""
            SELECT COUNT(*) FROM exo_mastery_body b
             WHERE b.systemAddress = :systemAddress
               AND NOT EXISTS (SELECT 1 FROM exo_mastery_harvest h
                                WHERE h.systemAddress = b.systemAddress AND h.bodyId = b.bodyId)
            """)
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
             WHERE NOT EXISTS (SELECT 1 FROM exo_mastery_harvest h
                                WHERE h.systemAddress = b.systemAddress AND h.bodyId = b.bodyId)
             GROUP BY s.systemAddress
             ORDER BY remainingValue DESC, s.starSystem ASC
             LIMIT :limit
            """)
    List<Site> richestRemaining(@Bind("limit") int limit);

    @SqlQuery("""
            SELECT b.*,
                   EXISTS (SELECT 1 FROM exo_mastery_harvest h
                            WHERE h.systemAddress = b.systemAddress AND h.bodyId = b.bodyId) AS completed
              FROM exo_mastery_body b
             WHERE b.systemAddress = :systemAddress
             ORDER BY completed ASC, b.value DESC, b.bodyName ASC
            """)
    List<Body> bodiesIn(@Bind("systemAddress") long systemAddress);

    /**
     * The catalogue's size and the commander's progress through it, in one row. Only catalogued
     * bodies count towards the totals, so after a purge the totals go to zero while the harvested
     * side - the commander's own harvest, value kept - still reads.
     */
    @SqlQuery("""
            SELECT (SELECT COUNT(*) FROM exo_mastery_system) AS systems,
                   (SELECT COUNT(*) FROM exo_mastery_body b
                     WHERE EXISTS (SELECT 1 FROM exo_mastery_system s WHERE s.systemAddress = b.systemAddress)) AS bodies,
                   (SELECT COALESCE(SUM(b.value), 0) FROM exo_mastery_body b
                     WHERE EXISTS (SELECT 1 FROM exo_mastery_system s WHERE s.systemAddress = b.systemAddress)) AS totalValue,
                   (SELECT COUNT(*) FROM exo_mastery_harvest) AS completedBodies,
                   (SELECT COALESCE(SUM(value), 0) FROM exo_mastery_harvest) AS harvestedValue
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
